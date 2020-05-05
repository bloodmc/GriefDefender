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
package com.griefdefender.configuration;

import com.griefdefender.configuration.category.ConfigCategory;
import com.griefdefender.configuration.category.CustomFlagGroupDefinitionCategory;
import com.griefdefender.configuration.category.DefaultFlagCategory;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class FlagStorage extends ConfigCategory {

    @Setting(value = "default-flags")
    public DefaultFlagCategory defaultFlagCategory = new DefaultFlagCategory();
    @Setting(value = "custom-flags", comment = "Used to define a group of custom flags for players/admins."
            + "\nEach group defined will be displayed in the flag GUI for users."
            + "\nGroups can have the following settings : "
            + "\n    enabled=<true|false>: Whether the group is enabled."
            + "\n    admin-group=<true|false>: Whether this group is considered for admin use only."
            + "\n    Note: GUI toggles in admin groups will always use configured definition contexts."
            + "\n          However, non-admin groups, such as 'user', will always apply to current claim only."
            + "\n    Note: If you assign users the permission 'griefdefender.admin.advanced-flags', they will be able to access admin presets within the claim tab."
            + "\n          It is recommended not to assign this permission to users for best experience."
            + "\n    hover=<text>: The hover text to be displayed when hovering over group name in GUI."
            + "\n    title=<text>: The title text to be used for TAB display."
            + "\n    value=<true|false>: This is used to set a default value for the flag definition. It is only used in conjunction with 'override=<type>, default=<type> settings."
            + "\n    contexts=[\"key=value\"]: A list of definition contexts that will be applied to all permissions."
            + "\nNote: This is primary used with 'default' and 'override' contexts. Ex. contexts=[\"default=global\"]"
            + "\nNote: You must specify one of the following contexts :'gd_claim_default=<type>' , 'gd_claim_override=<type>', or 'gd_claim=claim'"
            + "\nEach group will have an associated permission in order to be viewable."
            + "\nThe 'user' group will use the permission : 'griefdefender.custom.flag.group.user'"
            + "\nThe 'admin' group will use the permission : 'griefdefender.custom.flag.group.admin'"
            + "\nWithin each group, you can define flag definitions."
            + "\nEach flag definition must be defined in the following format:"
                    + "\nenabled: Controls whether the definition is enabled. Accepts a value of 'true' or 'false'"
                    + "\ndefault-value: The default value to assign flag definition."
                    + "\ndescription: The flag description to display on hover. Uses the legacy text format."
                    + "\npermissions: The list of permissions to link to definition. Each permission accepts the following contexts :"
                    + "\n    flag=<linked-flag>: This context is used to link the permission to a GD specific flag. Ex. 'flag=block-break' would link permission to GD's block-break flag"
                    + "\n    source=<id>: This context is used to specify a source id such as 'minecraft:creeper'."
                    + "\n    target=<id>: This context is used to specify a target id such as 'minecraft:chest'."
                    + "\n    state=<properties>: This context is used to specify a blockstate property such as 'state=lit:true'."
                    + "\nNote: All flag definitions that contain a definition context of 'gd_claim_default' or 'gd_claim_override' will be applied to permissions during server startup."
                    + "\nNote: Required if no source or target context is specified, the permission will default to ALL."
                    + "\nNote: Available contexts are : flag, source, target, state, used_item, item_name"
                    + "\nThese contexts may change, See https://github.com/bloodmc/GriefDefender/wiki for latest information.")
    public CustomFlagGroupDefinitionCategory customFlags = new CustomFlagGroupDefinitionCategory();
}
