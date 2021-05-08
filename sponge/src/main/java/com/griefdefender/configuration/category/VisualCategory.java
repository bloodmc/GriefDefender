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

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.MinecraftVersion;
import org.spongepowered.api.Sponge;

@ConfigSerializable
public class VisualCategory extends ConfigCategory {

    @Setting(value = "hide-borders-when-using-wecui", comment = "Whether to hide the glowstone/gold block borders when using WECUI.")
    public boolean hideBorders = false;
    @Setting(value = "hide-fillers-when-using-wecui", comment = "Whether to hide the block fillers when using WECUI.")
    public boolean hideFillers = false;
    @Setting(value = "hide-wecui-drag-visuals-2d", comment = "Whether drag visuals should be shown while creating a claim in 2D mode.")
    public boolean hideDrag2d = true;
    @Setting(value = "cuboid-level-visuals-2d", comment = "Whether to use cuboid visuals, in 2D claims, during inspection with owner min/max claim levels between 0 and 255."
            + "\nNote: WECUI visuals are not associated to this option."
            + "\nNote: If enabled, this will send many block updates to players causing more client strain depending on size of claim. Use with caution.")
    public boolean cuboidLevelVisuals = false;
    @Setting(value = "client-visuals-per-tick", comment = "The amount of block visuals a client can receive per tick when showing/hiding claims. Default: 12")
    public int clientVisualsPerTick = 12;
    @Setting(value = "claim-create-block", comment = "The visual block used during claim creation. (Default: minecraft:diamond_block)")
    public String claimCreateStartBlock = "minecraft:diamond_block";
    @Setting(value = "filler-spacing", comment = "The space between each filler visual block.")
    public int fillerSpacing = 10;
    @Setting(value = "active-claim-visual-time", comment = "The active time, in seconds, to keep a claim's visuals shown to a player. (Default: 60)"
            + "\nNote: If value is <= 0, GD will use the default value.")
    public int claimVisualTime = 60;
    @Setting(value = "active-create-block-visual-time", comment = "The active time, in seconds, to keep a claim's create block visual shown to a player. (Default: 180)"
            + "\nNote: This only applies during claim creation."
            + "\nNote: If value is <= 0, GD will use the default value.")
    public int createBlockVisualTime = 180;

    @Setting(value = "admin-accent-block", comment = "The visual accent block used for admin claims. (Default: minecraft:pumpkin)")
    public String visualAdminAccentBlock = "minecraft:pumpkin";
    @Setting(value = "admin-corner-block", comment = "The visual corner block used for admin claims. (Default: minecraft:glowstone)")
    public String visualAdminCornerBlock = "minecraft:glowstone";
    @Setting(value = "admin-filler-block", comment = "The visual filler block used for admin claims. (Default: minecraft:pumpkin)")
    public String visualAdminFillerBlock = "minecraft:pumpkin";

    @Setting(value = "basic-accent-block", comment = "The visual accent block used for basic claims. (Default: minecraft:gold_block)")
    public String visualBasicAccentBlock = "minecraft:gold_block";
    @Setting(value = "basic-corner-block", comment = "The visual corner block used for basic claims. (Default: minecraft:glowstone)")
    public String visualBasicCornerBlock = "minecraft:glowstone";
    @Setting(value = "basic-filler-block", comment = "The visual filler block used for basic claims. (Default: minecraft:gold_block)")
    public String visualBasicFillerBlock = "minecraft:gold_block";

    @Setting(value = "error-accent-block", comment = "The visual accent block used to visualize an error in a claim. (Default: minecraft:netherrack)")
    public String visualErrorAccentBlock = "minecraft:netherrack";
    @Setting(value = "error-corner-block", comment = "The visual corner block used to visualize an error in a claim. (Default: minecraft:redstone_ore)")
    public String visualErrorCornerBlock = "minecraft:redstone_ore";
    @Setting(value = "error-filler-block", comment = "The visual filler block used to visualize an error in a claim. (Default: minecraft:diamond_block)")
    public String visualErrorFillerBlock = "minecraft:diamond_block";

    @Setting(value = "subdivision-accent-block", comment = "The visual accent block used for subdivision claims. (Default: minecraft:white_wool or minecraft:wool for legacy versions)")
    public String visualSubdivisionAccentBlock;
    @Setting(value = "subdivision-corner-block", comment = "The visual corner block used for subdivision claims. (Default: minecraft:iron_block)")
    public String visualSubdivisionCornerBlock = "minecraft:iron_block";
    @Setting(value = "subdivision-filler-block", comment = "The visual filler block used for subdivision claims. (Default: minecraft:white_wool or minecraft:wool for legacy versions)")
    public String visualSubdivisionFillerBlock;

    @Setting(value = "town-accent-block", comment = "The visual accent block used for town claims. (Default: minecraft:emerald_block)")
    public String visualTownAccentBlock = "minecraft:emerald_block";
    @Setting(value = "town-corner-block", comment = "The visual corner block used for town claims. (Default: minecraft:glowstone)")
    public String visualTownCornerBlock = "minecraft:glowstone";
    @Setting(value = "town-filler-block", comment = "The visual filler block used for town claims. (Default: minecraft:emerald_block)")
    public String visualTownFillerBlock = "minecraft:emerald_block";

    @Setting(value = "nature-accent-block", comment = "The visual accent block used while in restore nature mode. (Default: minecraft:diamond_block)")
    public String visualNatureAccentBlock = "minecraft:diamond_block";
    @Setting(value = "nature-corner-block", comment = "The visual corner block used while in restore nature mode. (Default: minecraft:diamond_block)")
    public String visualNatureCornerBlock = "minecraft:diamond_block";

    public VisualCategory() {
        final MinecraftVersion version = Sponge.getPlatform().getMinecraftVersion();
        if (version.getName().contains("1.8.8") || version.getName().contains("1.12")) {
            this.visualSubdivisionAccentBlock = "minecraft:wool";
            this.visualSubdivisionFillerBlock = "minecraft:wool";
        } else {
            this.visualSubdivisionAccentBlock = "minecraft:white_wool";
            this.visualSubdivisionFillerBlock = "minecraft:white_wool";
        }
    }

}
