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

import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;

public class GDActiveFlagData {

    public enum Type {
        CLAIM,
        DEFAULT,
        OVERRIDE,
        UNDEFINED
    }

    private final CustomFlagData flagData;
    private final Tristate value;
    private final Type type;

    public GDActiveFlagData(CustomFlagData flagData, Tristate value, Type type) {
        this.flagData = flagData;
        this.value = value;
        this.type = type;
    }

    public CustomFlagData getFlagData() {
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

    public Component getComponent() {
        TextComponent.Builder contextBuilder = TextComponent.builder();
        int count = 0;
        for (Context context : this.flagData.getContexts()) {
            if (count > 0) {
                contextBuilder.append(", ");
            }
            contextBuilder.append(context.getKey().replace("gd_claim_", "").replace("gd_claim", ""), TextColor.GREEN)
                .append("=")
                .append(context.getValue(), TextColor.GRAY);
        }
        TextComponent.Builder builder = TextComponent.builder();
        builder
            .append(this.flagData.getFlag().getName().toLowerCase(), this.getColor())
            .append("=", TextColor.WHITE)
            .append(this.value.toString().toLowerCase(), TextColor.GOLD)
            .append(" ")
            .append(contextBuilder.build());
        return builder.build();
    }
}
