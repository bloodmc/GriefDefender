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
import java.util.List;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class ClaimCategory extends ConfigCategory {

    @Setting(value = "explosion-block-surface-blacklist", comment = "A list of source id's that cannot cause explosion damage to blocks above sea level."
            + "\nEx. If you add 'minecraft:creeper' to the list, creepers would not be able to cause blocks to explode above sea level."
            + "\nNote: This will have higher priority than 'explosion-block' flag.")
    public List<String> explosionBlockSurfaceBlacklist = new ArrayList<>();
    @Setting(value = "explosion-entity-surface-blacklist", comment = "A list of id's that cannot cause explosion damage to entities above sea level."
            + "\nEx. If you add 'minecraft:creeper' to the list, creepers would not be able to hurt entities above sea level."
            + "\nNote: This will have higher priority than 'explosion-entity' flag.")
    public List<String> explosionEntitySurfaceBlacklist = new ArrayList<>();
    @Setting(value = "explosion-surface-block-level", comment = "The 'Y' block level that is considered the surface for explosions. (Default: 63)")
    public int explosionSurfaceBlockLevel = 63;
    @Setting(value = "claim-block-task-move-threshold", comment = "The minimum threshold of movement (in blocks) required to receive accrued claim blocks. (Default: 0)"
            + "\nNote: The claim block task runs every 5 minutes which is the time each player will get to move the required amount of blocks.")
    public int claimBlockTaskMoveThreshold = 0;
    @Setting(value = "claim-block-task", comment = "Whether claim block task should run to accrue blocks for players. (Default: True)"
            + "\nNote: If in economy-mode, use setting 'use-claim-block-task' under economy category."
            + "\nNote: To configure amount accrued, see 'blocks-accrued-per-hour' option at https://github.com/bloodmc/GriefDefender/wiki/Options-(Meta)#global-options")
    public boolean claimBlockTask = true;
    @Setting(value = "claim-create-radius-limit", comment = "The radius limit for the /claimcreate command. (Default: 256)")
    public int claimCreateRadiusLimit = 256;
    @Setting(value = "worldedit-schematics", comment = "Whether to use WorldEdit for schematics. Default: false"
            + "\nNote: If you were using schematics in older GD/GP versions and want old schematics to work then you should keep this setting disabled.")
    public boolean useWorldEditSchematics = false;
    @Setting(value = "auto-chest-claim-block-radius", comment = "Radius used (in blocks) for auto-created claim when a chest is placed. Set to -1 to disable chest claim creation.")
    public int autoChestClaimBlockRadius = 4;
    @Setting(value = "border-block-radius", comment = "Set claim border of specified radius (in blocks), centered on claim. If set to 1, adds an additional 1 block protected radius around claim.\n" + 
            "Note: It is not recommended to set this value too high as performance can degrade due to deeper claim searches.")
    public int borderBlockRadius = 0;
    @Setting(value = "restrict-world-max-height", comment = "Whether to restrict claiming to world max height. (Default: True")
    public boolean restrictWorldMaxHeight = true;
    @Setting(value = "expiration-cleanup-interval", comment = "The interval in minutes for cleaning up expired claims. Default: 0. Set to 0 to disable.")
    public int expirationCleanupInterval = 0;
    @Setting(value = "auto-nature-restore", comment = "Whether survival claims will be automatically restored to world generated state when expired. \nNote: This only supports world generated blocks. Consider using 'auto-schematic-restore' if using a custom world.")
    public boolean claimAutoNatureRestore = false;
    @Setting(value = "auto-schematic-restore", comment = "Whether survival claims will be automatically restored to its claim creation schematic on abandon/expiration. "
            + "\nNote: Enabling this feature will cause ALL newly created claims to automatically create a special schematic that will be used to restore claim on abandon/expiration."
            + "\nNote: Enabling this feature will disable ability to resize claims."
            + "\nNote: It is HIGHLY recommended to disable building in the wilderness before using this feature to avoid players exploiting."
            + "\nNote: It is also recommended to ONLY use this feature in newly created worlds where there is no existing player data."
            + "\nNote: This does NOT affect deletions. If admins want to restore back to original schematic, they can select '__restore__' by using /claimschematic command.")
    public boolean claimAutoSchematicRestore = false;
    @Setting(value = "investigation-tool", comment = "The item used to investigate claims with a right-click.\nNote: Set to empty quotes if you want to assign no item and use '/claim' mode exclusively.")
    public String investigationTool = "minecraft:stick";
    @Setting(value = "modification-tool", comment = "The item used to create/resize claims with a right click.\nNote: Set to empty quotes if you want to assign no item and use '/claim' mode exclusively.")
    public String modificationTool = "minecraft:golden_shovel";
    @Setting(value = "claims-enabled",
            comment = "Whether claiming is enabled or not. (0 = Disabled, 1 = Enabled)")
    public int claimsEnabled = 1;
    @Setting(value = "player-trapped-cooldown", comment = "The cooldown time, in seconds, when using the '/trapped' command. (Default: 300)")
    public int trappedCooldown = 300;
    @Setting(value = "protect-tamed-entities", comment = "Whether tamed entities should be protected in claims. Default: true")
    public boolean protectTamedEntities = true;
    @Setting(value = "reserved-claim-names", comment = "A list of reserved claim names for use only by administrators."
        + "\nNote: Names support wildcards '?' and '*' by using Apache's wildcard matcher." 
        + "\nThe wildcard '?' represents a single character."
        + "\nThe wildcard '*' represents zero or more characters."
        + "\nFor more information on usage, see https://commons.apache.org/proper/commons-io/javadocs/api-2.5/org/apache/commons/io/FilenameUtils.html#wildcardMatch(java.lang.String,%20java.lang.String)")
    public List<String> reservedClaimNames = new ArrayList<>();

    public ClaimCategory() {

    }
}