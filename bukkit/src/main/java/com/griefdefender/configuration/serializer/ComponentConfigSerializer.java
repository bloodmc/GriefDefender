/*
 * This file is part of GriefDefender, licensed under the MIT License (MIT).
 *
 * Copyright (c) bloodmc
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
package com.griefdefender.configuration.serializer;

import com.google.common.reflect.TypeToken;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.serializer.gson.GsonComponentSerializer;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.gson.GsonConfigurationLoader;
import ninja.leaping.configurate.loader.HeaderMode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;

public class ComponentConfigSerializer implements TypeSerializer<Component> {

    /**
     * Creates a new {@link ComponentConfigSerializer}. Normally this should not
     * need to be created more than once.
     */
    public ComponentConfigSerializer() {
    }

    @Override
    public Component deserialize(TypeToken<?> type, ConfigurationNode node) throws ObjectMappingException {
        if (node.getString() == null || node.getString().isEmpty()) {
            return TextComponent.empty();
        }
        if (node.getString().contains("text=")) {
            // Try sponge data
            StringWriter writer = new StringWriter();

            GsonConfigurationLoader gsonLoader = GsonConfigurationLoader.builder()
                    .setIndent(0)
                    .setSink(() -> new BufferedWriter(writer))
                    .setHeaderMode(HeaderMode.NONE)
                    .build();

            try {
                gsonLoader.save(node);
            } catch (IOException e) {
                throw new ObjectMappingException(e);
            }
            return GsonComponentSerializer.INSTANCE.deserialize(writer.toString());
        }

        return LegacyComponentSerializer.legacy().deserialize(node.getString(), '&');
    }

    @Override
    public void serialize(TypeToken<?> type, Component obj, ConfigurationNode node) throws ObjectMappingException {
        if (obj == TextComponent.empty()) {
            node.setValue("");
        } else {
            node.setValue(LegacyComponentSerializer.legacy().serialize(obj, '&'));
        }
    }

}
