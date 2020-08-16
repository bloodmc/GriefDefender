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
import com.griefdefender.internal.util.NMSUtil;

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
        // check item
        String customItemId = NMSUtil.getInstance().getItemStackNBTString(stack, SLIME_COMPOUND_KEY, SLIME_ITEM_KEY);
        if (customItemId != null && !customItemId.isEmpty()) {
            String level = null;
            if (contexts != null && customItemId.length() > 2 && customItemId.matches("^.*\\_\\d$")) {
                level = customItemId.substring(customItemId.length() - 1, customItemId.length());
                customItemId = customItemId.substring(0, customItemId.length() - 2);
                if (contexts != null) {
                    contexts.add(new Context("slimefun_level", level));
                }
            }
            return "slimefun:" + customItemId.toLowerCase();
        }
        // check block
        String customItemBlockId = NMSUtil.getInstance().getItemStackNBTString(stack, SLIME_COMPOUND_KEY, SLIME_BLOCK_KEY);
        if (customItemBlockId != null && !customItemBlockId.isEmpty()) {
            String level = null;
            if (contexts != null && customItemBlockId.length() > 2 && customItemBlockId.matches("^.*\\_\\d$")) {
                level = customItemBlockId.substring(customItemBlockId.length() - 1, customItemBlockId.length());
                customItemBlockId = customItemBlockId.substring(0, customItemBlockId.length() - 2);
                contexts.add(new Context("slimefun_level", level));
            }
            return "slimefun:" + customItemBlockId.toLowerCase();
        }

        return "";
    }

    public String getSlimeBlockId(Block block) {
        return this.getSlimeBlockId(block, null);
    }

    public String getSlimeBlockId(Block block, Set<Context> contexts) {
        String customItemBlockId = NMSUtil.getInstance().getTileEntityNBTString(block, "PublicBukkitValues", "slimefun:slimefun_block");
        if (customItemBlockId != null && !customItemBlockId.isEmpty()) {
            String level = null;
            if (contexts != null && customItemBlockId.length() > 2 && customItemBlockId.matches("^.*\\_\\d$")) {
                level = customItemBlockId.substring(customItemBlockId.length() - 1, customItemBlockId.length());
                customItemBlockId = customItemBlockId.substring(0, customItemBlockId.length() - 2);
                contexts.add(new Context("slimefun_level", level));
            }
            return "slimefun:" + customItemBlockId.toLowerCase();
        }

        return "";
    }
}
