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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FilenameUtils;
import org.bukkit.entity.Player;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class ModCategory {

    @Setting(value = "fakeplayer-identifiers", comment = "Contains a list of strings used to identify a fakeplayer by UUID or name. To use, add the fakeplayer UUID or name."
            + "\nNote: Strings support wildcards '?' and '*' by using Apache's wildcard matcher." 
            + "\nThe wildcard '?' represents a single character."
            + "\nThe wildcard '*' represents zero or more characters."
            + "\nFor more information on usage, see https://commons.apache.org/proper/commons-io/javadocs/api-2.5/org/apache/commons/io/FilenameUtils.html#wildcardMatch(java.lang.String,%20java.lang.String)")
    public List<String> fakePlayerIdentifiers = new ArrayList<>();
    @Setting(value = "mod-id-map", comment = "Used to map an unknown mod item/block/entity to a mod id. To use, add the mod package with a mapping to a mod id."
            + "\nEx. 'com.pixelmonmod.*', 'pixelmon' would map an entity containing class name 'com.pixelmonmod.*' to 'pixelmon'."
            + "\nNote: Strings support wildcards '?' and '*' by using Apache's wildcard matcher." 
            + "\nThe wildcard '?' represents a single character."
            + "\nThe wildcard '*' represents zero or more characters."
            + "\nFor more information on usage, see https://commons.apache.org/proper/commons-io/javadocs/api-2.5/org/apache/commons/io/FilenameUtils.html#wildcardMatch(java.lang.String,%20java.lang.String)")
    public Map<String, String> modIdMap = new HashMap<>();
    @Setting(value = "block-id-convert-list", comment = "Used to override generic block id's to their actual id during TE and item usage if available. Add the target block id to list if you want to force a conversion when detected."
            + "\nNote: This is useful for mods such as IC2 which uses the generic id 'ic2:te' for its multi-block.")
    public List<String> blockIdConvertList = new ArrayList<>();
    @Setting(value = "tile-id-nbt-map", comment = "Used to override generic tileentity id's to their actual id during TE usage. Add the target TE id as key and NBT key where ID is stored as value."
            + "\nNote: This is useful for mods such as Gregtech which uses the generic id 'gregtech:machine' for its TE and NBT key 'MetaId' to store the actual ID.")
    public Map<String, String> tileIdNbtMap = new HashMap<>();

    public ModCategory() {
        this.blockIdConvertList.add("gregtech:machine");
        this.blockIdConvertList.add("ic2:te");
        this.fakePlayerIdentifiers.add("41C82C87-7AfB-4024-BA57-13D2C99CAE77"); // Forge FakePlayer
        this.fakePlayerIdentifiers.add("BFC3377F-C3C9-3382-9DA6-79B50A9AFE57"); // OpenMods
        this.fakePlayerIdentifiers.add("0D0C4CA0-4FF1-11E4-916C-0800200C9A66"); // ComputerCraft
        this.fakePlayerIdentifiers.add("[Minecraft]"); // Forge FakePlayer name
        this.fakePlayerIdentifiers.add("OpenModsFakethis*");
        this.modIdMap.put("com.pixelmonmod.*", "pixelmon");
        this.modIdMap.put("net.minecraftforge.*", "forge");
        this.modIdMap.put("openblocks.*", "openblocks");
        this.modIdMap.put("openmods.*", "openmods");
        this.tileIdNbtMap.put("gregtech:machine", "MetaId");
    }

    public boolean isFakePlayer(Player player) {
        for (String str : this.fakePlayerIdentifiers) {
            if (FilenameUtils.wildcardMatch(player.getUniqueId().toString().toLowerCase(), str.toLowerCase())) {
                return true;
            }
            if (FilenameUtils.wildcardMatch(player.getName().toLowerCase(), str.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public boolean convertBlockId(String id) {
        if (this.blockIdConvertList.contains(id)) {
            return true;
        }
        return false;
    }

    public String getModId(String clazz) {
        for (Entry<String, String> entry : this.modIdMap.entrySet()) {
            final String modPackage = entry.getKey();
            final String modId = entry.getValue();
            if (FilenameUtils.wildcardMatch(clazz, modPackage)) {
                return modId.toLowerCase();
            }
        }
        return null;
    }

    public String getNbtKey(String tileId) {
        return this.tileIdNbtMap.get(tileId);
    }
}
