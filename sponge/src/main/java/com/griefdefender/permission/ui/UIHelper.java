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
package com.griefdefender.permission.ui;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.option.Option;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.permission.ui.UIFlagData.FlagContextHolder;

import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.serializer.plain.PlainComponentSerializer;

public class UIHelper {

    public static Comparator<Component> PLAIN_COMPARATOR = (text1, text2) -> 
        PlainComponentSerializer.INSTANCE.serialize(text1).replace("true",  "").replace("false",  "")
            .compareTo(PlainComponentSerializer.INSTANCE.serialize(text2).replace("true",  "").replace("false",  ""));

    public static List<Component> stripeText(List<Component> texts) {
        Collections.sort(texts, PLAIN_COMPARATOR);

        ImmutableList.Builder<Component> finalTexts = ImmutableList.builder();
        for (int i = 0; i < texts.size(); i++) {
            Component text = texts.get(i);
            if (i % 2 == 0) { 
                text = text.color(TextColor.GREEN);
            } else {
                text = text.color(TextColor.AQUA);
            }

            finalTexts.add(text);
        }
        return finalTexts.build();
    }

    public static Component getPermissionMenuTypeHoverText(FlagContextHolder flagHolder, MenuType menuType) {
        if (flagHolder.getType() == MenuType.DEFAULT && menuType == MenuType.CLAIM) {
            return TextComponent.builder()
                    .append(MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.FLAG_NOT_SET, 
                            ImmutableMap.of("flag", TextComponent.of(flagHolder.getFlag().getName(), TextColor.GREEN),
                                            "value", TextComponent.of(flagHolder.getValue(), TextColor.LIGHT_PURPLE)))).build();
        }

        return getPermissionMenuTypeHoverText(menuType);
    }

    public static Component getPermissionMenuTypeHoverText(MenuType type) {
        Component hoverText = TextComponent.empty();
        if (type == MenuType.DEFAULT) {
            hoverText = TextComponent.builder("")
                    .append(MessageCache.getInstance().TITLE_DEFAULT.color(TextColor.LIGHT_PURPLE))
                    .append(" : ")
                    .append(MessageCache.getInstance().FLAG_UI_INFO_DEFAULT)
                    .build();
        } else if (type == MenuType.CLAIM) {
            hoverText = TextComponent.builder("")
                    .append(MessageCache.getInstance().TITLE_CLAIM.color(TextColor.GOLD))
                    .append(" : ")
                    .append(MessageCache.getInstance().FLAG_UI_INFO_CLAIM)
                    .build();
        } else if (type == MenuType.OVERRIDE) {
            hoverText = TextComponent.builder("")
                    .append(MessageCache.getInstance().TITLE_OVERRIDE.color(TextColor.RED))
                    .append(" : ")
                    .append(MessageCache.getInstance().FLAG_UI_INFO_OVERRIDE)
                    .build();
        } else if (type == MenuType.INHERIT) {
            hoverText = TextComponent.builder("")
                    .append(MessageCache.getInstance().TITLE_INHERIT.color(TextColor.AQUA))
                    .append(" : ")
                    .append(MessageCache.getInstance().FLAG_UI_INFO_INHERIT)
                    .build();
        }
        return hoverText;
    }

    public static Component getBaseOptionOverlayText(String option) {
        String baseFlag = option.replace(GDPermissions.OPTION_BASE + ".", "");
        int endIndex = baseFlag.indexOf(".");
        if (endIndex != -1) {
            baseFlag = baseFlag.substring(0, endIndex);
        }

        final Option<?> flag = GriefDefender.getRegistry().getType(Option.class, baseFlag).orElse(null);
        if (flag == null) {
            return MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.OPTION_NOT_FOUND, ImmutableMap.of(
                    "option", baseFlag));
        }

        return flag.getDescription();
    }

    public static Component getFriendlyContextString(Claim claim, Set<Context> contexts) {
        if (contexts.isEmpty()) {
            return TextComponent.of("[]", TextColor.WHITE);
        }

        TextComponent.Builder builder = TextComponent.builder();
        final Iterator<Context> iterator = contexts.iterator();
        while (iterator.hasNext()) {
            final Context context = iterator.next();
            builder.append("\n[", TextColor.WHITE)
                .append(context.getKey(), TextColor.GREEN)
                .append("=", TextColor.GRAY)
                .append(context.getValue(), TextColor.WHITE);

            if (iterator.hasNext()) {
                builder.append("], ");
            } else {
                builder.append("]");
            }
        }
        return builder.build();
    }

    public static TextColor getPermissionMenuTypeColor(MenuType type) {
        TextColor color = TextColor.LIGHT_PURPLE;
        if (type == MenuType.DEFAULT) {
            color = TextColor.LIGHT_PURPLE;
        } else if (type == MenuType.CLAIM) {
            color = TextColor.GOLD;
        } else if (type == MenuType.INHERIT) {
            color = TextColor.AQUA;
        } else {
            color = TextColor.RED;
        }

        return color;
    }

    public static boolean containsCustomContext(Set<Context> contexts) {
        for (Context context : contexts) {
            if (context.getKey().equals("gd_claim_default") || context.getKey().equals("server") || context.getKey().equals("gd_claim")) {
                continue;
            }

            return true;
        }
        return false;
    }

    public static Set<Context> getFilteredContexts(Set<Context> contexts) {
        Set<Context> filteredContexts = new HashSet<>(contexts);
        for (Context context : contexts) {
            if (context.getKey().contains("gd_claim") || context.getKey().equals("server")) {
                filteredContexts.remove(context);
            }
        }

        return filteredContexts;
    }
}
