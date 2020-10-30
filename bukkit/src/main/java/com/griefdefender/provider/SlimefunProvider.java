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
package com.griefdefender.provider;

import java.util.Set;

import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import com.griefdefender.api.permission.Context;

import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;

public class SlimefunProvider {

    private static final String SLIME_COMPOUND_KEY = "PublicBukkitValues";
    private static final String SLIME_BLOCK_KEY = "slimefun:slimefun_block";
    private static final String SLIME_ITEM_KEY = "slimefun:slimefun_item";

    public SlimefunProvider() {

    }

    public String getSlimeItemId(ItemStack stack) {
        return this.getSlimeItemId(stack, null);
    }

    public String getSlimeItemId(ItemStack stack, Set<Context> contexts) {
        final SlimefunItem slimefunItem = SlimefunItem.getByItem(stack);
        if (slimefunItem != null) {
            String level = null;
            String id = slimefunItem.getId();
            if (id.length() > 2 && id.matches("^.*\\_\\d$")) {
                System.out.println("match!");
                level = id.substring(id.length() - 1, id.length());
                id = id.substring(0, id.length() - 2);
                if (contexts != null) {
                    contexts.add(new Context("slimefun_level", level));
                }
            }
            return "slimefun:" + id.toLowerCase();
        }

        return "";
    }

    public String getSlimeBlockId(Block block) {
        return this.getSlimeBlockId(block, null);
    }

    public String getSlimeBlockId(Block block, Set<Context> contexts) {
        final SlimefunItem slimefunBlock = BlockStorage.check(block);
        if (slimefunBlock != null) {
            String level = null;
            String id = slimefunBlock.getId();
            if (id.length() > 2 && id.matches("^.*\\_\\d$")) {
                level = id.substring(id.length() - 1, id.length());
                id = id.substring(0, id.length() - 2);
                if (contexts != null) {
                    contexts.add(new Context("slimefun_level", level));
                }
            }
            return "slimefun:" + id.toLowerCase();
        }

        return "";
    }

    public String getSlimeItemDisplayName(ItemStack stack) {
        final SlimefunItem slimefunItem = SlimefunItem.getByItem(stack);
        if (slimefunItem != null) {
            return "slimefun:" + slimefunItem.getItemName();
        }

        return "";
    }

    public boolean isInventory(Block block) {
        final SlimefunItem slimefunItem = BlockStorage.check(block);
        if (slimefunItem != null) {
            return BlockMenuPreset.isInventory(slimefunItem.getId());
        }
        return false;
    }
}
