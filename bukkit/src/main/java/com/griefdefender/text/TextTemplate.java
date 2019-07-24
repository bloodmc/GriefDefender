/*
 * This file is part of SpongeAPI, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.griefdefender.text;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

import javax.annotation.Nullable;

/**
 * Represents a re-usable template that produces a formatted
 * {@link TextComponent.Builder}. Elements will be appended to the result builder in the
 * order that they are specified in {@link #of(Object...)}.
 */
public final class TextTemplate implements Iterable<Object> {

    /**
     * Default "open" String for how arguments are contained within the
     * template.
     */
    public static final String DEFAULT_OPEN_ARG = "{";

    /**
     * Default "close" String for how arguments are contained within the
     * template.
     */
    public static final String DEFAULT_CLOSE_ARG = "}";

    /**
     * Empty representation of a {@link TextTemplate}. This is returned if the
     * array supplied to {@link #of(Object...)} is empty.
     */
    public static final TextTemplate EMPTY = new TextTemplate(DEFAULT_OPEN_ARG, DEFAULT_CLOSE_ARG, new Object[]{});

    final ImmutableList<Object> elements;
    final ImmutableMap<String, Arg> arguments;
    final Component text;
    final String openArg;
    final String closeArg;

    TextTemplate(String openArg, String closeArg, Object... elements) {
        this.openArg = openArg;
        this.closeArg = closeArg;

        // collect elements
        ImmutableList.Builder<Object> elementList = ImmutableList.builder();
        Map<String, Arg> argumentMap = new HashMap<>();
        TextColor color = null;
        for (Object element : elements) {
            if (element instanceof Arg.Builder) {
                element = ((Arg.Builder) element).build();
            }
            if (element instanceof Arg) {
                // check for non-equal duplicate argument
                Arg newArg = new Arg((Arg) element, this.openArg, this.closeArg);
                Arg oldArg = argumentMap.get(newArg.name);
                if (oldArg != null && !oldArg.equals(newArg)) {
                    //throw new TextTemplateArgumentException("Tried to supply an unequal argument with a duplicate name \""
                    //        + newArg.name + "\" to TextTemplate.");
                    continue;
                }
                argumentMap.put(newArg.name, newArg);
                element = newArg;
                elementList.add(element);
            } else if (element instanceof TextColor) {
                color = (TextColor) element;
            } else if (element instanceof String) {
                if (color != null) {
                    elementList.add(TextComponent.builder((String) element).color(color).build());
                } else {
                    elementList.add(element);
                }
                color = null;
            } else {
                elementList.add(element);
            }
        }
        this.elements = elementList.build();
        this.arguments = ImmutableMap.copyOf(argumentMap);

        // build text representation
        TextComponent.Builder builder = null;
        for (Object element : this.elements) {
            builder = apply(element, builder);
        }
        if (builder != null) {
            final TextComponent text = builder.build();
            this.text = text;
        } else {
            this.text = TextComponent.empty();
        }
    }

    /**
     * Returns the elements contained in this TextTemplate.
     *
     * @return The elements within the template
     */
    public List<Object> getElements() {
        return this.elements;
    }

    /**
     * Returns the arguments contained within the TextTemplate.
     *
     * @return The arguments within this TextTemplate
     */
    public Map<String, Arg> getArguments() {
        return this.arguments;
    }

    /**
     * Returns the string used for containing Args within the template.
     *
     * @return String containing args
     */
    public String getOpenArgString() {
        return this.openArg;
    }

    /**
     * Returns the string used for containing Args within the template.
     *
     * @return String containing args
     */
    public String getCloseArgString() {
        return this.closeArg;
    }

    /**
     * Concatenates the specified {@link TextTemplate} to this template and
     * returns the result. In the event that the two templates' open/close
     * argument containers vary, this template's argument containers will be
     * used.
     *
     * @param other To concatenate
     * @return Concatenated template
     */
    public TextTemplate concat(TextTemplate other) {
        List<Object> elements = new ArrayList<>(this.elements);
        elements.addAll(other.elements);
        return of(this.openArg, this.closeArg, elements.toArray(new Object[elements.size()]));
    }

    /**
     * Applies an empty map of parameters to this TextTemplate and returns the
     * result in a {@link Text.Builder}.
     *
     * @return Text builder containing result
     * @throws TextTemplateArgumentException if required parameters are missing
     */
    public TextComponent.Builder apply() {
        return apply(Collections.emptyMap());
    }

    /**
     * Applies the specified parameters to this TextTemplate and returns the
     * result in a {@link Text.Builder}.
     *
     * @param params Parameters to apply
     * @return Text builder containing result
     * @throws TextTemplateArgumentException if required parameters are missing
     */
    public TextComponent.Builder apply(Map<String, ?> params) {
        return apply(null, params);
    }

    private TextComponent.Builder apply(@Nullable TextComponent.Builder result, Map<String, ?> params) {
        checkNotNull(params, "params");
        for (Object element : this.elements) {
            result = apply(element, result, params);
        }
        return Optional.ofNullable(result).orElse(TextComponent.builder(""));
    }

    @Nullable
    private TextComponent.Builder apply(Object element, @Nullable TextComponent.Builder builder, Map<String, ?> params) {
        // Note: The builder is initialized as null to avoid unnecessary Text nesting
        if (element instanceof Arg) {
            Arg arg = (Arg) element;
            Object param = params.get(arg.name);
            if (param == null) {
                arg.checkOptional();
                if (arg.defaultValue != null) {
                    builder = applyArg(arg.defaultValue, arg, builder);
                }
            } else {
                builder = applyArg(param, arg, builder);
            }
        } else {
            builder = apply(element, builder);
        }
        return builder;
    }

    private TextComponent.Builder apply(Object element, @Nullable TextComponent.Builder builder) {
        TextComponent component = null;
        if (element instanceof TextComponent) {
            component = (TextComponent) element;
        } else if (element instanceof Arg) {
            final Arg arg = (Arg) element;
            if (arg.format != null) {
                TextComponent.Builder argBuilder = TextComponent.builder(arg.openArg + arg.name + arg.closeArg);
                if (arg.format.getStyles() != null) {
                    argBuilder.decorations(arg.format.getStyles(), true);
                }
                if (arg.format.getColor() != null) {
                    argBuilder.color(arg.format.getColor());
                }
                component = argBuilder.build();
            } else {
                component = TextComponent.builder(arg.openArg + arg.name + arg.closeArg).build();
            }
        } else {
            String str = element.toString();
            component = TextComponent.of(str);
        }
        if (builder == null) {
            builder = TextComponent.builder(component.content()).mergeColor(component).mergeDecorations(component).mergeEvents(component);
        } else {
            builder.append(component);
        }
        return builder;
    }

    private TextComponent.Builder applyArg(Object param, Arg arg, @Nullable TextComponent.Builder builder) {
        if (builder == null) {
            builder = TextComponent.builder("");
        }
        // wrap the parameter in the argument format
        TextComponent.Builder wrapper = TextComponent.builder("");
        if (arg.format.getColor() != null) {
            wrapper.color(arg.format.getColor());
        }
        if (arg.format.getStyles() != null) {
            wrapper.decorations(arg.format.getStyles(), true);
        }
        apply(param, wrapper);
        builder.append(wrapper.build());
        return builder;
    }

    /**
     * Constructs a new TextTemplate for the given elements. The order of the
     * elements is the order in which they will be appended to the result
     * builder via {@link #apply(Map)}.
     *
     * <p>The provided elements may be of any type.</p>
     *
     * <p>In the case that an element is a {@link TextElement},
     * {@link TextElement#applyTo(Text.Builder)} will be used to append the
     * element to the builder.</p>
     *
     * <p>In the case that an element is an {@link Arg} the argument will be
     * replaced with the {@link TextElement} provided by the corresponding
     * parameter supplied by {@link #apply(Map)}</p>
     *
     * <p>In the case that an element is any other type, the parameter value's
     * {@link Object#toString()} method will be used to create a {@link Text}
     * object.</p>
     *
     * @param elements Elements to append to builder
     * @param openArg String to use for beginning of Arg containers
     * @param closeArg String to use for end of Arg containers
     * @return Newly constructed TextTemplate
     */
    public static TextTemplate of(String openArg, String closeArg, Object... elements) {
        checkNotNull(openArg, "open arg");
        checkArgument(!openArg.isEmpty(), "open arg cannot be empty");
        checkNotNull(closeArg, "close arg");
        checkArgument(!closeArg.isEmpty(), "close arg cannot be empty");
        checkNotNull(elements, "elements");
        if (elements.length == 0) {
            return of();
        }
        return new TextTemplate(openArg, closeArg, elements);
    }

    /**
     * Constructs a new TextTemplate for the given elements. The order of the
     * elements is the order in which they will be appended to the result
     * builder via {@link #apply(Map)}.
     *
     * <p>The provided elements may be of any type.</p>
     *
     * <p>In the case that an element is a {@link TextElement},
     * {@link TextElement#applyTo(Text.Builder)} will be used to append the
     * element to the builder.</p>
     *
     * <p>In the case that an element is an {@link Arg} the argument will be
     * replaced with the {@link TextElement} provided by the corresponding
     * parameter supplied by {@link #apply(Map)}</p>
     *
     * <p>In the case that an element is any other type, the parameter value's
     * {@link Object#toString()} method will be used to create a {@link Text}
     * object.</p>
     *
     * @param elements Elements to append to builder
     * @return Newly constructed TextTemplate
     */
    public static TextTemplate of(Object... elements) {
        return of(DEFAULT_OPEN_ARG, DEFAULT_CLOSE_ARG, elements);
    }

    /**
     * Returns the empty representation of a TextTemplate.
     *
     * @return Empty TextTemplate
     */
    public static TextTemplate of() {
        return EMPTY;
    }

    /**
     * Constructs a new {@link Arg} to be supplied to {@link #of(Object...)}.
     * This argument expects a {@link TextElement} parameter.
     *
     * @param name name of argument
     * @return argument builder
     */
    public static Arg.Builder arg(String name) {
        return new Arg.Builder(name);
    }

    public Component toText() {
        return this.text;
    }

    @Override
    public Iterator<Object> iterator() {
        return this.elements.iterator();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", this.getClass().getSimpleName() + "[", "]")
                .add("elements=" + this.elements)
                .add("arguments=" + this.arguments)
                .add("text=" + this.text)
                .add("openArg=" + this.openArg)
                .add("closeArg=" + this.closeArg)
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.elements, this.openArg, this.closeArg);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TextTemplate)) {
            return false;
        }
        TextTemplate that = (TextTemplate) obj;
        return that.elements.equals(this.elements)
                && that.openArg.equals(this.openArg)
                && that.closeArg.equals(this.closeArg);
    }

    /**
     * Represents a variable element within a TextTemplate. Arguments are
     * replaced by parameters in {@link #apply(Map)}.
     */
    @ConfigSerializable
    public static final class Arg { //implements TextRepresentable {

        @Setting final boolean optional;
        @Setting @Nullable final Component defaultValue;
        final String name; // defined by node name
        final ComponentFormat format; // defined in "content" node
        final String openArg;
        final String closeArg;

        Arg(String name, boolean optional, @Nullable Component defaultValue, ComponentFormat format, String openArg, String closeArg) {
            this.name = name;
            this.optional = optional;
            this.defaultValue = defaultValue;
            this.format = format;
            this.openArg = openArg;
            this.closeArg = closeArg;
        }

        Arg(String name, boolean optional, @Nullable Component defaultValue, ComponentFormat format) {
            this(name, optional, defaultValue, format, DEFAULT_OPEN_ARG, DEFAULT_CLOSE_ARG);
        }

        Arg(Arg arg, String openArg, String closeArg) {
            this(arg.name, arg.optional, arg.defaultValue, arg.format, openArg, closeArg);
        }

        void checkOptional() {
            if (!this.optional) {
                //throw new TextTemplateArgumentException("Missing required argument in TextTemplate \"" + this.name + "\".");
            }
        }

        /**
         * Returns the name of this argument to be matched with incoming
         * parameters.
         *
         * @return Argument name
         */
        public String getName() {
            return this.name;
        }

        /**
         * Returns true if this Arg is optional. If a parameter is missing for
         * a non-optional Arg, a {@link TextTemplateArgumentException} will be
         * thrown.
         *
         * @return True if optional
         */
        public boolean isOptional() {
            return this.optional;
        }

        /**
         * Returns the default value to use if the Arg {@link #isOptional()}
         * and no parameter is supplied.
         *
         * @return Default value
         */
        public Optional<Component> getDefaultValue() {
            return Optional.ofNullable(this.defaultValue);
        }

        /**
         * Returns the base format to be applied to this Arg.
         *
         * @return Base format
         */
        public ComponentFormat getFormat() {
            return this.format;
        }

        /**
         * Returns the beginning string of the Arg's container.
         *
         * @return Open string
         */
        public String getOpenArgString() {
            return this.openArg;
        }

        /**
         * Returns the end string of the Arg's container.
         *
         * @return Close string
         */
        public String getCloseArgString() {
            return this.closeArg;
        }

        public Component toText() {
            TextComponent.Builder builder = TextComponent.builder(this.openArg + this.name + this.closeArg);
            if (this.format.getColor() != null) {
                builder.color(this.format.getColor());
            }
            if (this.format.getStyles() != null) {
                builder.decorations(this.format.getStyles(), true);
            }
            return builder.build();
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", this.getClass().getSimpleName() + "[", "]")
                    .add("optional=" + this.optional)
                    .add("defaultValue=" + this.defaultValue)
                    .add("name=" + this.name)
                    .add("format=" + (this.format == null ? null : this.format))
                    .add("openArg=" + this.openArg)
                    .add("closeArg=" + this.closeArg)
                    .toString();
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this.name, this.optional, this.defaultValue, this.openArg, this.closeArg);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Arg)) {
                return false;
            }
            Arg that = (Arg) obj;
            return that.name.equals(this.name)
                    && that.optional == this.optional
                    && (that.defaultValue != null ? that.defaultValue.equals(this.defaultValue) : this.defaultValue == null)
                    && that.openArg.equals(this.openArg)
                    && that.closeArg.equals(this.closeArg);
        }

        /**
         * Represents a builder for {@link Arg}s.
         */
        public static final class Builder {

            final String name;
            boolean optional = false;
            @Nullable Component defaultValue;
            ComponentFormat format = ComponentFormat.NONE;

            Builder(String name) {
                this.name = name;
            }

            /**
             * Builds a new {@link Arg}. Note that it is not necessary to call
             * this method when supplying an argument to a template. You may
             * pass the builder to {@link TextTemplate#of(Object...)} directly.
             *
             * @return Newly created Arg
             */
            public Arg build() {
                return new Arg(this.name, this.optional, this.defaultValue, this.format);
            }

            /**
             * Sets whether the Arg should be optional (false by default).
             *
             * @param optional True if should be optional
             * @return This builder
             */
            public Builder optional(boolean optional) {
                this.optional = optional;
                return this;
            }

            /**
             * Makes the Arg optional.
             *
             * @return This builder
             */
            public Builder optional() {
                return optional(true);
            }

            /**
             * Sets the default value for the Argument. The argument must by
             * optional in order for this value to be used.
             *
             * @param defaultValue Default value
             * @return This builder
             */
            public Builder defaultValue(Component defaultValue) {
                this.defaultValue = defaultValue;
                return this;
            }

            /**
             * Sets the "base" format of the Arg. This acts as a default format
             * when no formatting data is provided by the parameter.
             *
             * @param format Base format of Arg
             * @return This builder
             */
            public Builder format(ComponentFormat format) {
                this.format = format;
                return this;
            }

            /**
             * Sets the "base" color of the Arg. This acts as a default color
             * when no color data is provided by the parameter.
             *
             * @param color Base color of Arg
             * @return This builder
             */
            public Builder color(TextColor color) {
                this.format = this.format.color(color);
                return this;
            }

            /**
             * Sets the "base" style of the Arg. This acts as a default style
             * when no style data is provided by the parameter.
             *
             * @param style Base style of Arg
             * @return This builder
             */
            public Builder styles(Set<TextDecoration> styles) {
                this.format = this.format.style(styles);
                return this;
            }

            /**
             * Sets the "base" style of the Arg. This acts as a default style
             * when no style data is provided by the parameter.
             *
             * @param style Base style of Arg
             * @return This builder
             */
            public Builder style(TextDecoration style) {
                this.format = this.format.style(style);
                return this;
            }

            @Override
            public String toString() {
                return new StringJoiner(", ", this.getClass().getSimpleName() + "[", "]")
                        .add("name=" + this.name)
                        .add("optional=" + this.optional)
                        .add("defaultValue=" + this.defaultValue)
                        .add("format=" + (this.format == null ? null : this.format))
                        .toString();
            }
        }
    }

}
