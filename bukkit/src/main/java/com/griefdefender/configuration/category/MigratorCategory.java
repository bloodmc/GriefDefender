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

@ConfigSerializable
public class MigratorCategory extends ConfigCategory {

    @Setting(value = "classic", comment = "Set to true to enable the classic migrator." + 
            "\nNote: Migrates GP bukkit classic claim data and GPFlags data, if available, to current format." +
            "\nNote: It is recommended to backup data before using.")
    public boolean classicMigrator = false;

    @Setting(value = "worldguard", comment = 
            "Set to true to enable WorldGuard data migrator." +
            "\nNote: Only cuboid regions are supported." +
            "\nNote: It is recommended to backup data before using.")
    public boolean worldGuardMigrator = false;

    @Setting(value = "playerdata", comment = "Set to true to enable the legacy playerdata file migrator."
            + "\nNote: Migrates legacy playerdata file format to permissions storage such as LuckPerms json or mysql storage."
            + "\nNote: Before turning this on, make sure you properly set 'context-storage-type' in the the playerdata section of this config."
            + "\nNote: It is HIGHLY recommended to backup your permissions database before running this migrator as all local playerdata files will be migrated to it."
            + "\nNote: Do NOT run this migrator on more than one server if multiple servers share the same permissions database.")
    public boolean playerDataMigrator = false;
}
