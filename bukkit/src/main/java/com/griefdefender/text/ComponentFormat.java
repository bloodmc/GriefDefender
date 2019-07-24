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

import com.google.common.base.Objects;
import com.google.common.reflect.TypeToken;

import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;

import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Represents a pair of {@link TextDecoration} and {@link TextColor}.
 */
public final class ComponentFormat {

    static {
        TypeSerializers.getDefaultSerializers().registerType(TypeToken.of(ComponentFormat.class), new TextFormatConfigSerializer());
    }

    /**
     * An empty {@link ComponentFormat} with no {@link TextColor} and no {@link TextDecoration}.
     */
    public static final ComponentFormat NONE = new ComponentFormat(null, null);

    /**
     * The text color.
     */
    private final TextColor color;

    /**
     * The text style.
     */
    private final Set<TextDecoration> styles;

    /**
     * Gets the {@link ComponentFormat} with the default style and color.
     *
     * @return The empty text format
     */
    public static ComponentFormat of() {
        return NONE;
    }

    /**
     * Constructs a new {@link ComponentFormat} with the specific style.
     *
     * @param style The style
     * @return The new text format
     */
    public static ComponentFormat of(TextDecoration style) {
        final Set<TextDecoration> styles = new HashSet<>();
        styles.add(style);
        return new ComponentFormat(null, styles);
    }

    /**
     * Constructs a new {@link ComponentFormat} with the specific color.
     *
     * @param color The color
     * @return The new text format
     */
    public static ComponentFormat of(TextColor color) {
        return new ComponentFormat(color, null);
    }

    /**
     * Constructs a new {@link ComponentFormat} with the specific color and style.
     *
     * @param color The color
     * @param style The style
     * @return The new text format
     */
    public static ComponentFormat of(TextColor color, TextDecoration style) {
        final Set<TextDecoration> styles = new HashSet<>();
        styles.add(style);
        return new ComponentFormat(color, styles);
    }

    /**
     * Constructs a new {@link ComponentFormat}.
     *
     * @param color The color
     * @param style The style
     */
    private ComponentFormat(TextColor color, Set<TextDecoration> styles) {
        this.color = color;
        this.styles = styles;
    }

    /**
     * Returns the {@link TextColor} in this format.
     *
     * @return The color
     */
    public TextColor getColor() {
        return this.color;
    }


    /**
     * Returns the {@link TextDecoration} in this format.
     *
     * @return The style
     */
    public Set<TextDecoration> getStyles() {
        return this.styles;
    }

    /**
     * Returns a new {@link ComponentFormat} with the given color.
     *
     * @param color The color
     * @return The new text format
     */
    public ComponentFormat color(TextColor color) {
        return new ComponentFormat(color, this.styles);
    }

    /**
     * Returns a new {@link ComponentFormat} with the given style.
     *
     * @param style The style
     * @return The new text format
     */
    public ComponentFormat style(TextDecoration style) {
        final Set<TextDecoration> styles = new HashSet<>();
        styles.add(style);
        return new ComponentFormat(this.color, styles);
    }

    /**
     * Returns a new {@link ComponentFormat} with the given style.
     *
     * @param style The style
     * @return The new text format
     */
    public ComponentFormat style(Set<TextDecoration> style) {
        return new ComponentFormat(this.color, style);
    }

    public void applyTo(TextComponent.Builder builder) {
        builder.color(this.color).decorations(this.styles, true);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ComponentFormat)) {
            return false;
        }

        ComponentFormat that = (ComponentFormat) o;
        return this.color.equals(that.color) && this.styles.equals(that.styles);

    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.color, this.styles);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", this.getClass().getSimpleName() + "[", "]")
                .add("color=" + this.color)
                .add("style=" + this.styles)
                .toString();
    }

}
