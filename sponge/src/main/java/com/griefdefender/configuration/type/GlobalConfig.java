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

import com.griefdefender.configuration.category.CustomFlagGroupDefinitionCategory;
import com.griefdefender.configuration.category.DefaultPermissionCategory;
import com.griefdefender.configuration.category.DynmapCategory;
import com.griefdefender.configuration.category.EconomyCategory;
import com.griefdefender.configuration.category.MessageCategory;
import com.griefdefender.configuration.category.MigratorCategory;
import com.griefdefender.configuration.category.ModuleCategory;
import com.griefdefender.configuration.category.PlayerDataCategory;
import com.griefdefender.configuration.category.ThreadCategory;
import com.griefdefender.configuration.category.TownCategory;
import ninja.leaping.configurate.objectmapping.Setting;

public class GlobalConfig extends ConfigBase {

    @Setting(value = "custom-flags", comment = "Used to define a group of custom flags for players/admins."
            + "\nEach group defined will be displayed in the flag GUI for users."
            + "\nGroups can have the following settings : "
            + "\n    enabled=<true|false>: Whether the group is enabled."
            + "\n    admin-group=<true|false>: Whether this group is considered for admin use only."
            + "\n    hover=<text>: The hover text to be displayed when hovering over group name in GUI."
            + "\n    title=<text>: The title text to be used for TAB display."
            + "\n    value=<true|false>: This is used to set a default value for the flag definition. It is only used in conjunction with 'override=<type>, default=<type> settings."
            + "\n    contexts=[\"key=value\"]: A list of optional definition contexts that will be applied to all permissions."
            + "\nNote: This is primary used with 'default' and 'override' contexts. Ex. contexts=[\"default=global\"]"
            + "\nEach group will have an associated permission in order to be viewable."
            + "\nThe 'user' group will use the permission : 'griefdefender.custom.flag.group.user'"
            + "\nThe 'admin' group will use the permission : 'griefdefender.custom.flag.group.admin'"
            + "\nWithin each group, you can define flag definitions."
            + "\nEach flag definition must be defined in the following format:"
                    + "\nenabled: Controls whether the definition is enabled. Accepts a value of 'true' or 'false'"
                    + "\ndescription: The flag description to display on hover. Uses the legacy text format."
                    + "\npermissions: The list of permissions to link to definition. Each permission accepts the following contexts :"
                    + "\n    flag=<linked-flag>: This context is used to link the permission to a GD specific flag. Ex. 'flag=block-break' would link permission to GD's block-break flag"
                    + "\n    source=<id>: This context is used to specify a source id such as 'minecraft:creeper'."
                    + "\n    target=<id>: This context is used to specify a target id such as 'minecraft:chest'."
                    + "\n    state=<properties>: This context is used to specify a blockstate property such as 'state=lit:true'."
                    + "\nNote: Required if no source or target context is specified, the permission will default to ALL."
                    + "\nNote: Available contexts are : flag, source, target, state, used_item, item_name"
                    + "\nThese contexts may change, See https://github.com/bloodmc/GriefDefender/wiki for latest information.")
    public CustomFlagGroupDefinitionCategory customFlags = new CustomFlagGroupDefinitionCategory();
    @Setting
    public DynmapCategory dynmap = new DynmapCategory();
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
    @Setting(value = "default-permissions")
    public DefaultPermissionCategory permissionCategory = new DefaultPermissionCategory();
    @Setting
    public ThreadCategory thread = new ThreadCategory();

    @Setting
    public TownCategory town = new TownCategory();
}
