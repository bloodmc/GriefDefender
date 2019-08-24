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

import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

@ConfigSerializable
public class BanCategory extends ConfigCategory {

    @Setting(value = "blocks")
    private Map<String, Component> blocks = new HashMap<>();
    @Setting(value = "entities")
    private Map<String, Component> entities = new HashMap<>();
    @Setting(value = "items")
    private Map<String, Component> items = new HashMap<>();
    

    public Map<String, Component> getBlockMap() {
        return this.blocks;
    }

    public Map<String, Component> getEntityMap() {
        return this.entities;
    }

    public Map<String, Component> getItemMap() {
        return this.items;
    }

    public boolean isBlockBanned(String id) {
        if (id == null) {
            return false;
        }
        if (!id.contains(":")) {
            id = "minecraft:" + id;
        }
        return this.blocks.containsKey(id);
    }

    public void addBlockBan(String id, Component reason) {
        if (id == null) {
            return;
        }
        if (reason == null) {
            reason = TextComponent.empty();
        }
        if (!id.contains(":")) {
            id = "minecraft:" + id;
        }
        this.blocks.put(id, reason);
    }

    public void removeBlockBan(String id) {
        if (id == null) {
            return;
        }
        if (!id.contains(":")) {
            id = "minecraft:" + id;
        }
        this.blocks.remove(id);
    }

    public Component getBlockBanReason(String id) {
        if (id == null) {
            return null;
        }
        if (!id.contains(":")) {
            id = "minecraft:" + id;
        }
        return this.blocks.get(id);
    }

    public boolean isEntityBanned(String id) {
        if (id == null) {
            return false;
        }
        if (!id.contains(":")) {
            id = "minecraft:" + id;
        }
        return this.entities.containsKey(id);
    }

    public void addEntityBan(String id, Component reason) {
        if (id == null) {
            return;
        }
        if (reason == null) {
            reason = TextComponent.empty();
        }
        this.entities.put(id, reason);
    }

    public void removeEntityBan(String id) {
        if (id == null) {
            return;
        }
        if (!id.contains(":")) {
            id = "minecraft:" + id;
        }
        this.entities.remove(id);
    }

    public Component getEntityBanReason(String id) {
        if (id == null) {
            return null;
        }
        if (!id.contains(":")) {
            id = "minecraft:" + id;
        }
        return this.entities.get(id);
    }

    public boolean isItemBanned(String id) {
        if (id == null) {
            return false;
        }
        if (!id.contains(":")) {
            id = "minecraft:" + id;
        }
        return this.items.containsKey(id);
    }

    public void addItemBan(String id, Component reason) {
        if (id == null) {
            return;
        }
        if (reason == null) {
            reason = TextComponent.empty();
        }
        if (!id.contains(":")) {
            id = "minecraft:" + id;
        }
        this.items.put(id, reason);
    }

    public void removeItemBan(String id) {
        if (id == null) {
            return;
        }
        if (!id.contains(":")) {
            id = "minecraft:" + id;
        }
        this.items.remove(id);
    }

    public Component getItemBanReason(String id) {
        if (id == null) {
            return null;
        }
        if (!id.contains(":")) {
            id = "minecraft:" + id;
        }
        return this.items.get(id);
    }
}
