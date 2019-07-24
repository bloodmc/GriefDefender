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
import com.griefdefender.api.permission.flag.Flag;
import com.griefdefender.api.permission.flag.Flags;
import com.griefdefender.registry.ClaimTypeRegistryModule;
import com.griefdefender.registry.FlagRegistryModule;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

@ConfigSerializable
public class BlacklistCategory extends ConfigCategory {

    @Setting(value = "flag-id-blacklist", comment = "A list of id's ignored by flags.")
    public Map<String, List<String>> flagIdBlacklist = new HashMap<>();

    @Setting(value = "global-source", comment = "A global list of source id's that are ignored by events. \nNote: This only affects events where the id specified is the source.")
    public List<String> globalSourceBlacklist = new ArrayList<>();

    @Setting(value = "global-target", comment = "A global list of target id's that are ignored by events. \nNote: This only affects events where the id specified is the target.")
    public List<String> globalTargetBlacklist = new ArrayList<>();

    public List<String> getGlobalSourceBlacklist() {
        return this.globalSourceBlacklist;
    }

    public List<String> getGlobalTargetBlacklist() {
        return this.globalTargetBlacklist;
    }

    public BlacklistCategory() {
        for (Flag flag : FlagRegistryModule.getInstance().getAll()) {
            this.flagIdBlacklist.put(flag.getId().toLowerCase(), new ArrayList<>());
        }
        this.flagIdBlacklist.put("block-pre", new ArrayList<>());
    }

    // Used by API
    @Nullable
    public List<String> getFlagBlacklist(String flag) {
        return this.flagIdBlacklist.get(flag);
    }
}
