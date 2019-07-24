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
package com.griefdefender.configuration.type;

import com.griefdefender.configuration.category.BanCategory;
import com.griefdefender.configuration.category.DefaultPermissionCategory;
import com.griefdefender.configuration.category.EconomyCategory;
import com.griefdefender.configuration.category.MessageCategory;
import com.griefdefender.configuration.category.MigratorCategory;
import com.griefdefender.configuration.category.ModuleCategory;
import com.griefdefender.configuration.category.PlayerDataCategory;
import com.griefdefender.configuration.category.ThreadCategory;
import com.griefdefender.configuration.category.TownCategory;
import ninja.leaping.configurate.objectmapping.Setting;

public class GlobalConfig extends ConfigBase {

    @Setting
    public BanCategory bans = new BanCategory();
    @Setting
    public EconomyCategory economy = new EconomyCategory();
    @Setting
    public PlayerDataCategory playerdata = new PlayerDataCategory();
    @Setting
    public MessageCategory message = new MessageCategory();
    @Setting(comment = 
            "List of migrators that convert old or other protection data into the current GD claim data format." + 
            "\nNote: It is recommended to backup data before using.")
    public MigratorCategory migrator = new MigratorCategory();
    @Setting(value = "modules")
    public ModuleCategory modules = new ModuleCategory();
    @Setting(value = "default-global-permissions")
    public DefaultPermissionCategory permissionCategory = new DefaultPermissionCategory();
    @Setting
    public ThreadCategory thread = new ThreadCategory();

    @Setting
    public TownCategory town = new TownCategory();
}
