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
package com.griefdefender.internal.visual;

import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.CatalogType;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockType;

public class GDClaimVisualType implements CatalogType {

    private final String id;
    private final String name;
    public BlockType visualAccentBlock;
    public BlockType visualCornerBlock;
    public BlockType visualFillerBlock;

    public GDClaimVisualType(String id, String name) {
        this.id = id.toLowerCase();
        this.name = name.toLowerCase();
    }

    public BlockType getVisualAccentBlock() {
        if (this.visualAccentBlock == null) {
            this.initVisualBlocks();
        }
        return this.visualAccentBlock;
    }

    public BlockType getVisualCornerBlock() {
        if (this.visualCornerBlock == null) {
            this.initVisualBlocks();
        }
        return this.visualCornerBlock;
    }

    public BlockType getVisualFillerBlock() {
        if (this.visualFillerBlock == null) {
            this.initVisualBlocks();
        }
        return this.visualFillerBlock;
    }

    private void initVisualBlocks() {
        if (this == ClaimVisual.ADMIN) {
            this.visualAccentBlock = Sponge.getRegistry().getType(BlockType.class, GriefDefenderPlugin.getGlobalConfig().getConfig().visual.visualAdminAccentBlock).orElse(null);
            if (this.visualAccentBlock == null) {
                this.visualAccentBlock = Sponge.getRegistry().getType(BlockType.class, "minecraft:pumpkin").get();
            }
            this.visualCornerBlock = Sponge.getRegistry().getType(BlockType.class, GriefDefenderPlugin.getGlobalConfig().getConfig().visual.visualAdminCornerBlock).orElse(null);
            if (this.visualCornerBlock == null) {
                this.visualCornerBlock = Sponge.getRegistry().getType(BlockType.class, "minecraft:glowstone").get();
            }
            this.visualFillerBlock = Sponge.getRegistry().getType(BlockType.class, GriefDefenderPlugin.getGlobalConfig().getConfig().visual.visualAdminFillerBlock).orElse(null);
            if (this.visualFillerBlock == null) {
                this.visualFillerBlock = Sponge.getRegistry().getType(BlockType.class, "minecraft:pumpkin").get();
            }
        } else if (this == ClaimVisual.BASIC) {
            this.visualAccentBlock = Sponge.getRegistry().getType(BlockType.class, GriefDefenderPlugin.getGlobalConfig().getConfig().visual.visualBasicAccentBlock).orElse(null);
            if (this.visualAccentBlock == null) {
                this.visualAccentBlock = Sponge.getRegistry().getType(BlockType.class, "minecraft:gold_block").get();
            }
            this.visualCornerBlock = Sponge.getRegistry().getType(BlockType.class, GriefDefenderPlugin.getGlobalConfig().getConfig().visual.visualBasicCornerBlock).orElse(null);
            if (this.visualCornerBlock == null) {
                this.visualCornerBlock = Sponge.getRegistry().getType(BlockType.class, "minecraft:glowstone").get();
            }
            this.visualFillerBlock = Sponge.getRegistry().getType(BlockType.class, GriefDefenderPlugin.getGlobalConfig().getConfig().visual.visualBasicFillerBlock).orElse(null);
            if (this.visualFillerBlock == null) {
                this.visualFillerBlock = Sponge.getRegistry().getType(BlockType.class, "minecraft:gold_block").get();
            }
        } else if (this == ClaimVisual.RESTORENATURE) {
            this.visualAccentBlock = Sponge.getRegistry().getType(BlockType.class, GriefDefenderPlugin.getGlobalConfig().getConfig().visual.visualNatureAccentBlock).orElse(null);
            if (this.visualAccentBlock == null) {
                this.visualAccentBlock = Sponge.getRegistry().getType(BlockType.class, "minecraft:diamond_block").get();
            }
            this.visualCornerBlock = Sponge.getRegistry().getType(BlockType.class, GriefDefenderPlugin.getGlobalConfig().getConfig().visual.visualNatureCornerBlock).orElse(null);
            if (this.visualCornerBlock == null) {
                this.visualCornerBlock = Sponge.getRegistry().getType(BlockType.class, "minecraft:diamond_block").get();
            }
            this.visualFillerBlock = Sponge.getRegistry().getType(BlockType.class, "minecraft:diamond_block").get();
        } else if (this == ClaimVisual.SUBDIVISION) {
            this.visualAccentBlock = Sponge.getRegistry().getType(BlockType.class, GriefDefenderPlugin.getGlobalConfig().getConfig().visual.visualSubdivisionAccentBlock).orElse(null);
            if (this.visualAccentBlock == null) {
                this.visualAccentBlock = Sponge.getRegistry().getType(BlockType.class, "minecraft:white_wool").orElse(null);
                if (this.visualAccentBlock == null) {
                    this.visualAccentBlock = Sponge.getRegistry().getType(BlockType.class, "minecraft:wool").get();
                }
            }
            this.visualCornerBlock = Sponge.getRegistry().getType(BlockType.class, GriefDefenderPlugin.getGlobalConfig().getConfig().visual.visualSubdivisionCornerBlock).orElse(null);
            if (this.visualCornerBlock == null) {
                this.visualCornerBlock = Sponge.getRegistry().getType(BlockType.class, "minecraft:iron_block").get();
            }
            this.visualFillerBlock = Sponge.getRegistry().getType(BlockType.class, GriefDefenderPlugin.getGlobalConfig().getConfig().visual.visualSubdivisionFillerBlock).orElse(null);
            if (this.visualFillerBlock == null) {
                this.visualFillerBlock = Sponge.getRegistry().getType(BlockType.class, "minecraft:white_wool").get();
                if (this.visualFillerBlock == null) {
                    this.visualFillerBlock = Sponge.getRegistry().getType(BlockType.class, "minecraft:wool").get();
                }
            }
        } else if (this == ClaimVisual.TOWN) {
            this.visualAccentBlock = Sponge.getRegistry().getType(BlockType.class, GriefDefenderPlugin.getGlobalConfig().getConfig().visual.visualTownAccentBlock).orElse(null);
            if (this.visualAccentBlock == null) {
                this.visualAccentBlock = Sponge.getRegistry().getType(BlockType.class, "minecraft:emerald_block").get();
            }
            this.visualCornerBlock = Sponge.getRegistry().getType(BlockType.class, GriefDefenderPlugin.getGlobalConfig().getConfig().visual.visualTownCornerBlock).orElse(null);
            if (this.visualCornerBlock == null) {
                this.visualCornerBlock = Sponge.getRegistry().getType(BlockType.class, "minecraft:glowstone").get();
            }
            this.visualFillerBlock = Sponge.getRegistry().getType(BlockType.class, GriefDefenderPlugin.getGlobalConfig().getConfig().visual.visualTownFillerBlock).orElse(null);
            if (this.visualFillerBlock == null) {
                this.visualFillerBlock = Sponge.getRegistry().getType(BlockType.class, "minecraft:emerald_block").get();
            }
        } else { // DEFAULT
            this.visualAccentBlock = Sponge.getRegistry().getType(BlockType.class, "minecraft:netherrack").get();
            this.visualCornerBlock = Sponge.getRegistry().getType(BlockType.class, "minecraft:redstone_ore").get();
            this.visualFillerBlock = this.visualAccentBlock;
        }
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public String toString() {
        return this.name;
    }
}