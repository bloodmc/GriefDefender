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

    @Setting(value = "auto-chest-claim-block-radius", comment = "Radius used (in blocks) for auto-created claim when a chest is placed. Set to -1 to disable chest claim creation.")
    public int autoChestClaimBlockRadius = 4;
    @Setting(value = "border-block-radius", comment = "Set claim border of specified radius (in blocks), centered on claim. If set to 1, adds an additional 1 block protected radius around claim.\n" + 
            "Note: It is not recommended to set this value too high as performance can degrade due to deeper claim searches.")
    public int borderBlockRadius = 0;
    @Setting(value = "claim-list-max", comment = "Controls the max displayed claims when using the '/claimlist' command. Default: 200")
    public int claimListMax = 200;
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
    @Setting(value = "protect-tamed-entities", comment = "Whether tamed entities should be protected in claims. Default: true")
    public boolean protectTamedEntities = true;
    @Setting(value = "reserved-claim-names", comment = "A list of reserved claim names for use only by administrators."
        + "\nNote: Names support wildcards '?' and '*' by using Apache's wildcard matcher." 
        + "\nThe wildcard '?' represents a single character."
        + "\nThe wildcard '*' represents zero or more characters."
        + "\nFor more information on usage, see https://commons.apache.org/proper/commons-io/javadocs/api-2.5/org/apache/commons/io/FilenameUtils.html#wildcardMatch(java.lang.String,%20java.lang.String)")
    public List<String> reservedClaimNames = new ArrayList<>();
    @Setting(value = "bank-tax-system", comment = "Whether to enable the bank/tax system for claims. Set to true to enable.")
    public boolean bankTaxSystem = false;
    @Setting(value = "tax-apply-hour", comment = "The specific hour in day to apply tax to all claims.")
    public int taxApplyHour = 12;
    @Setting(value = "bank-transaction-log-limit")
    public int bankTransactionLogLimit = 60;

    public ClaimCategory() {

    }
}