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
package com.griefdefender.permission.flag;

import com.griefdefender.api.Tristate;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.flag.FlagData;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;

import java.util.Set;

public class GDActiveFlagData {

    public enum Type {
        CLAIM,
        DEFAULT,
        OVERRIDE,
        UNDEFINED
    }

    private final GDFlagDefinition flagDefinition;
    private final FlagData flagData;
    private final Tristate value;
    private final Type type;
    private final Set<Context> contexts;

    public GDActiveFlagData(GDFlagDefinition flagDefinition, FlagData flagData, Tristate value, Set<Context> contexts, Type type) {
        this.flagDefinition = flagDefinition;
        this.flagData = flagData;
        this.value = value;
        this.type = type;
        this.contexts = contexts;
    }

    public FlagData getFlagData() {
        return this.flagData;
    }

    public Tristate getValue() {
        return this.value;
    }

    public TextColor getColor() {
        if (this.type == Type.CLAIM) {
            return TextColor.YELLOW;
        }
        if (this.type == Type.OVERRIDE) {
            return TextColor.RED;
        }
        if (this.type == Type.DEFAULT) {
            return TextColor.LIGHT_PURPLE;
        }
        return TextColor.GRAY;
    }

    public Type getType() {
        return this.type;
    }

    public Component getComponent() {
        String descr = "Active Claim Result: ";
        final TextColor valueColor = this.getColor();
        TextComponent valueComponent = TextComponent.of(this.value.toString().toLowerCase()).color(valueColor);
        if (this.type == Type.OVERRIDE) {
            descr = "Active Override Result: ";
            valueComponent = TextComponent.of(this.value.toString().toLowerCase()).color(valueColor).decoration(TextDecoration.ITALIC, true).decoration(TextDecoration.UNDERLINED, true);
        } else if (this.type == Type.DEFAULT) {
            descr = "Active Default Result: ";
        } else {
            descr = "No Result: ";
        }
        TextComponent.Builder builder = TextComponent.builder();
        builder.append("\n" + descr, TextColor.AQUA);
        builder.append(valueComponent);
        builder.append("\nFlag: ")
                    .append(this.flagData.getFlag().getName(), TextColor.GREEN);

        if (!this.flagData.getContexts().isEmpty()) {
            builder.append("\nContexts: ");
        }
        for (Context context : this.contexts) {
            if (!this.flagDefinition.isAdmin() && context.getKey().contains("gd_claim")) {
                continue;
            }
            builder.append("\n");
            final String key = context.getKey();
            final String value = context.getValue();
            TextColor keyColor = TextColor.AQUA;
            builder.append(key, keyColor)
                    .append("=", TextColor.WHITE)
                    .append(value.replace("minecraft:", ""), TextColor.GRAY);
        }

        return builder.build();
    }
}
