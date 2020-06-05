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
package com.griefdefender.configuration;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.Maps;
import com.griefdefender.api.permission.option.Option;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.configuration.category.ConfigCategory;
import com.griefdefender.configuration.category.DefaultOptionCategory;
import com.griefdefender.registry.OptionRegistryModule;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class OptionStorage extends ConfigCategory {

    @Setting(value = "option-control", comment = "Controls which options are enabled.\nNote: To enable an option, set the value to 'true'."
            + "\nSee https://github.com/bloodmc/GriefDefender/wiki/Advanced-Options for info on how each option works.")
    private Map<String, Boolean> control = Maps.newHashMap();

    @Setting(value = "default-options")
    public DefaultOptionCategory defaultOptionCategory = new DefaultOptionCategory();

    @Setting(value = "vanilla-fallback-values", comment = "Controls the default vanilla fallback values used to for resetting options such as player walking and flying speed."
            + "\nFor example: If no option value is found for a player, the values set here will be used instead.")
    public Map<String, String> vanillaFallbackMap = new HashMap<>();

    public OptionStorage() {
        for (Option option : OptionRegistryModule.getInstance().getAll()) {
            if (!option.getName().contains("player") && !option.getName().contains("spawn") && !option.getName().contains("pvp")) {
                continue;
            }
            this.control.put(option.getName().toLowerCase(), false);
        }
        this.vanillaFallbackMap.put(Options.PLAYER_FLY_SPEED.getName().toLowerCase(), "0.1");
        this.vanillaFallbackMap.put(Options.PLAYER_WALK_SPEED.getName().toLowerCase(), "0.2");
    }

    public boolean isOptionEnabled(String option) {
        final Boolean result = this.control.get(option);
        if (result == null) {
            return false;
        }

        return result;
    }
}
