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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.flag.FlagDefinition;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.permission.GDPermissionHolder;
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
                Set<Context> defaultContexts = new HashSet<>();
                Set<Context> overrideContexts = new HashSet<>();
                String groupStr = null;
                final Iterator<Context> iterator = contexts.iterator();
                while (iterator.hasNext()) {
                    final Context context = iterator.next();
                    if (context.getKey().equalsIgnoreCase("gd_claim_default")) {
                        defaultContexts.add(context);
                    } else if (context.getKey().equalsIgnoreCase("gd_claim_override")) {
                        if (context.getValue().equalsIgnoreCase("claim")) {
                            iterator.remove();
                            continue;
                        }
                        overrideContexts.add(context);
                    } else if (context.getKey().equalsIgnoreCase("group")) {
                        groupStr = context.getValue();
                    }
                }
                GDPermissionHolder holder = GriefDefenderPlugin.GD_DEFINITION_HOLDER;
                if (groupStr != null) {
                    if (PermissionUtil.getInstance().hasGroupSubject(groupStr)) {
                        holder = PermissionHolderCache.getInstance().getOrCreateGroup(groupStr);
                        if (holder == null) {
                            holder = GriefDefenderPlugin.GD_DEFINITION_HOLDER;
                        }
                    }
                }

                if (!defaultContexts.isEmpty()) {
                    PermissionUtil.getInstance().setFlagDefinition(holder, flagDefinition, flagDefinition.getDefaultValue(), defaultContexts, false);
                }
                if (!overrideContexts.isEmpty()) {
                    PermissionUtil.getInstance().setFlagDefinition(holder, flagDefinition, flagDefinition.getDefaultValue(), overrideContexts, false);
                }
            }
        }
    }
}
