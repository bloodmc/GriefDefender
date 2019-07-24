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

import com.google.common.reflect.TypeToken;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import net.kyori.text.format.TextFormat;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;

import java.util.HashSet;
import java.util.Set;

/**
 * An implementation of {@link TypeSerializer} to allow serialization of
 * {@link TextFormat}s directly to a configuration file.
 */
public class TextFormatConfigSerializer implements TypeSerializer<ComponentFormat> {

    private static final String NODE_COLOR = "color";
    private static final String NODE_STYLE = "style";

    @Override
    public ComponentFormat deserialize(TypeToken<?> type, ConfigurationNode value) throws ObjectMappingException {
        TextColor color = TextColor.WHITE;
        String colorId = value.getNode(NODE_COLOR).getString();
        if (colorId != null) {
            color = TextColor.valueOf(colorId);
        }

        ConfigurationNode styleNode = value.getNode(NODE_STYLE);
        final Set<TextDecoration> decorations = new HashSet<>();
        for (TextDecoration decoration : TextDecoration.values()) {
            if (styleNode.getNode(decoration.name().toLowerCase()).getBoolean()) {
                decorations.add(decoration);
            }
        }

        return ComponentFormat.NONE.color(color).style(decorations);
    }

    @Override
    public void serialize(TypeToken<?> type, ComponentFormat obj, ConfigurationNode value) throws ObjectMappingException {
        value.getNode(NODE_COLOR).setValue(obj.getColor().name());
        ConfigurationNode styleNode = value.getNode(NODE_STYLE);
        Set<TextDecoration> decorations = obj.getStyles();
        for (TextDecoration decoration : TextDecoration.values()) {
            styleNode.getNode(decoration.name().toLowerCase()).setValue(decorations.contains(decoration));
        }
    }

}
