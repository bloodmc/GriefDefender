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
package com.griefdefender.configuration.category;

import com.google.common.collect.Maps;
import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.api.claim.ClaimTypes;
import com.griefdefender.api.permission.flag.Flag;
import com.griefdefender.registry.ClaimTypeRegistryModule;
import com.griefdefender.registry.FlagRegistryModule;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.util.HashMap;
import java.util.Map;

@ConfigSerializable
public class DefaultFlagCategory extends ConfigCategory {

    @Setting(value = "default-claim-flags", comment = "The default flag settings used by claims. The group name represents the claim type."
            + "\nEx: The group admin will ONLY affect admin claims."
            + "\nSupported groups are : global, admin, basic, subdivision, town, and wilderness."
            + "\nNote: Global represents all claim types."
            + "\nNote: Specific types, such as wilderness, have higher priority than global."
            + "\nNote: Defaults do not force flags onto user claims. A newly created claim will have no flags set and use these default settings until a claim owner sets flags.")
    private Map<String, Map<String, Boolean>> defaultClaimFlags = Maps.newHashMap();

    public DefaultFlagCategory() {
        Map<String, Boolean> globalFlagMap = new HashMap<>();
        for (Flag flag : FlagRegistryModule.getInstance().getAll()) {
            globalFlagMap.put(flag.getName(), flag.getDefaultClaimTypeValue(null));
        }
        this.defaultClaimFlags.put("global", globalFlagMap);
        Map<String, Boolean> wildernessFlagMap = new HashMap<>();
        for (Flag flag : FlagRegistryModule.getInstance().getAll()) {
            wildernessFlagMap.put(flag.getName(), flag.getDefaultClaimTypeValue(ClaimTypes.WILDERNESS));
        }
        this.defaultClaimFlags.put(ClaimTypes.WILDERNESS.getName().toLowerCase(), wildernessFlagMap);
    }

    public void refreshFlags() {
        for (ClaimType type : ClaimTypeRegistryModule.getInstance().getAll()) {
            final Map<String, Boolean> flagTypeMap = this.defaultClaimFlags.get(type.getName().toLowerCase());
            if (flagTypeMap != null) {
                for (Flag flag : FlagRegistryModule.getInstance().getAll()) {
                    if (!flagTypeMap.containsKey(flag.getName())) {
                        flagTypeMap.put(flag.getName(), flag.getDefaultClaimTypeValue(type));
                    }
                }
            }
        }
        final Map<String, Boolean> globalFlagMap = this.defaultClaimFlags.get("global");
        for (Flag flag : FlagRegistryModule.getInstance().getAll()) {
            if (!globalFlagMap.containsKey(flag.getName())) {
                globalFlagMap.put(flag.getName(), flag.getDefaultClaimTypeValue(null));
            }
        }
    }

    public Map<String, Boolean> getFlagDefaults(String type) {
        return this.defaultClaimFlags.get(type.toLowerCase());
    }
}
