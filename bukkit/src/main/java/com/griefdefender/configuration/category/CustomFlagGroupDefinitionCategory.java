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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.flag.FlagData;
import com.griefdefender.api.permission.flag.FlagDefinition;
import com.griefdefender.permission.flag.GDFlagDefinition;
import com.griefdefender.registry.FlagDefinitionRegistryModule;
import com.griefdefender.util.PermissionUtil;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class CustomFlagGroupDefinitionCategory extends ConfigCategory {

    @Setting
    Map<String, CustomFlagGroupCategory> groups = new HashMap<>();

    public Map<String, CustomFlagGroupCategory> getGroups() {
        return this.groups;
    }

    public void initDefaults() {
        CustomFlagGroupCategory userGroup = this.groups.get("user");
        CustomFlagGroupCategory adminGroup = this.groups.get("admin");
        if (userGroup == null) {
            userGroup = new CustomFlagGroupCategory();
        }
        if (userGroup.isEnabled() && userGroup.getFlagDefinitions().isEmpty()) {
            for (FlagDefinition definition : FlagDefinitionRegistryModule.getInstance().getAll()) {
                if (definition.getGroupName().equalsIgnoreCase("user")) {
                    userGroup.getFlagDefinitions().put(definition.getName(), (GDFlagDefinition) definition);
                }
            }
            this.groups.put("user", userGroup);
        }
        if (adminGroup == null) {
            adminGroup = new CustomFlagGroupCategory();
        }
        if (adminGroup.isEnabled() && adminGroup.getFlagDefinitions().isEmpty()) {
            for (FlagDefinition definition : FlagDefinitionRegistryModule.getInstance().getAll()) {
                if (definition.getGroupName().equalsIgnoreCase("admin")) {
                    adminGroup.getFlagDefinitions().put(definition.getName(), (GDFlagDefinition) definition);
                }
            }
            adminGroup.isAdmin = true;
            this.groups.put("admin", adminGroup);
        }

        for (CustomFlagGroupCategory group : this.groups.values()) {
            if (!group.isEnabled()) {
                continue;
            }
            for (FlagDefinition flagDefinition : group.definitions.values()) {
                if (!flagDefinition.isEnabled()) {
                    continue;
                }
                Set<Context> contexts = new HashSet<>(flagDefinition.getContexts());
                for (FlagData flagData : flagDefinition.getFlagData()) {
                    Set<Context> permissionContexts = new HashSet<>(contexts);
                    permissionContexts.addAll(flagData.getContexts());
                    boolean shouldApply = false;
                    boolean isOverride = false;
                    for (Context context : permissionContexts) {
                        if (context.getKey().equalsIgnoreCase("gd_claim_default")) {
                            shouldApply = true;
                            break;
                        } else if (context.getKey().equalsIgnoreCase("gd_claim_override")) {
                            shouldApply = true;
                            isOverride = true;
                        }
                    }
                    if (shouldApply) {
                        if (isOverride) {
                            PermissionUtil.getInstance().setPermissionValue(GriefDefenderPlugin.DEFAULT_HOLDER, flagData.getFlag().getPermission(), flagDefinition.getDefaultValue(), permissionContexts);
                        } else {
                            PermissionUtil.getInstance().setTransientPermission(GriefDefenderPlugin.DEFAULT_HOLDER, flagData.getFlag().getPermission(), flagDefinition.getDefaultValue(), permissionContexts);
                        }
                    }
                }
            }
        }
    }
}
