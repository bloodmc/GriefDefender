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

import java.util.HashMap;
import java.util.Map;

import com.griefdefender.permission.flag.GDFlagDefinition;

import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class CustomFlagGroupCategory extends ConfigCategory {

    @Setting(value = "enabled", comment = "Whether flag definition group is enabled.")
    boolean enabled = true;
    @Setting(value = "admin-group", comment = "Set to true if this flag group is for admin use only."
            + "\nNote: If admin group, the permission is 'griefdefender.admin.custom.flag.<groupname>"
            + "\nNote: If user group (admin set false), the permission is 'griefdefender.user.custom.flag.<groupname>")
    boolean isAdmin = false;
    @Setting(value = "title", comment = "The title text to be used for TAB display.")
    Component titleText = TextComponent.empty();
    @Setting(value = "hover", comment = "The hover text to be displayed when hovering over group name in GUI.")
    Component hoverText = TextComponent.empty();
    @Setting
    Map<String, GDFlagDefinition> definitions = new HashMap<>();

    public Map<String, GDFlagDefinition> getFlagDefinitions() {
        return this.definitions;
    }

    public Component getTitleComponent() {
        return this.titleText;
    }

    public Component getHoverComponent() {
        return this.hoverText;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public boolean isAdminGroup() {
        return this.isAdmin;
    }
}
