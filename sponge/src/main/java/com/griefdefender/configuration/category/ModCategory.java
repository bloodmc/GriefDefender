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

import org.apache.commons.io.FilenameUtils;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class ModCategory {

    @Setting(value = "block-id-convert-list", comment = "Used to override generic block id's to their actual id during TE and item usage if available. Add the target block id to list if you want to force a conversion when detected."
            + "\nNote: This is useful for mods such as IC2 which uses the generic id 'ic2:te' for its multi-block.")
    public List<String> blockIdConvertList = new ArrayList<>();
    @Setting(value = "tile-id-nbt-map", comment = "Used to override generic tileentity id's to their actual id during TE usage. Add the target TE id as key and NBT key where ID is stored as value."
            + "\nNote: This is useful for mods such as Gregtech which uses the generic id 'gregtech:machine' for its TE and NBT key 'MetaId' to store the actual ID.")
    public Map<String, String> tileIdNbtMap = new HashMap<>();
    @Setting(value = "item-interact-force-list", comment = "Used to force interact-item flag checks when a player left/right-clicks with an item in hand."
            + "\nBy default, GD will only check interact-item flags when the item is used."
            + "\nTo force an item, add the item 'modid:name' to list."
            + "\nNote: Names support wildcards '?' and '*' by using Apache's wildcard matcher." 
            + "\nThe wildcard '?' represents a single character."
            + "\nThe wildcard '*' represents zero or more characters."
            + "\nFor more information on usage, see https://commons.apache.org/proper/commons-io/javadocs/api-2.5/org/apache/commons/io/FilenameUtils.html#wildcardMatch(java.lang.String,%20java.lang.String)")
    public List<String> itemInteractForceList = new ArrayList<>();

    public ModCategory() {
        this.blockIdConvertList.add("gregtech:machine");
        this.blockIdConvertList.add("ic2:te");
        this.tileIdNbtMap.put("gregtech:machine", "MetaId");
    }

    public boolean convertBlockId(String id) {
        if (this.blockIdConvertList.contains(id)) {
            return true;
        }
        return false;
    }

    public String getNbtKey(String tileId) {
        return this.tileIdNbtMap.get(tileId);
    }

    public boolean forceItemInteract(String id) {
        for (String str : this.itemInteractForceList) {
            if (FilenameUtils.wildcardMatch(id, str)) {
                return true;
            }
        }
        return false;
    }
}