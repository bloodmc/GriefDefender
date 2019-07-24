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
package com.griefdefender.registry;

import static com.google.common.base.Preconditions.checkNotNull;

import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.permission.flag.Flag;
import com.griefdefender.api.permission.flag.Flags;
import com.griefdefender.api.registry.CatalogRegistryModule;
import com.griefdefender.permission.GDFlag;
import com.griefdefender.util.RegistryHelper;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class FlagRegistryModule implements CatalogRegistryModule<Flag> {

    private static FlagRegistryModule instance;

    public static FlagRegistryModule getInstance() {
        return instance;
    }

    private final Map<String, Flag> flagMap = new HashMap<>();

    @Override
    public Optional<Flag> getById(String id) {
        if (id == null) {
            return Optional.empty();
        }

        id = id.replace("griefdefender.flag.", "");
        String[] parts = id.split("\\.");
        if (parts.length > 0) {
            id = parts[0];
        }

        return Optional.ofNullable(this.flagMap.get(checkNotNull(id)));
    }

    @Override
    public Collection<Flag> getAll() {
        return this.flagMap.values();
    }

    @Override
    public void registerDefaults() {
        RegistryHelper.mapFields(Flags.class, input -> {
            final String field = input.toLowerCase().replace("_", "-");
            final Flag flag = new GDFlag("griefdefender:" + field, field);
            this.flagMap.put(field.toLowerCase(Locale.ENGLISH), flag);
            return flag;
        });
    }

    @Override
    public void registerCustomType(Flag type) {
        this.flagMap.put(type.getName().toLowerCase(Locale.ENGLISH), type);
        GriefDefenderPlugin.getGlobalConfig().getConfig().permissionCategory.refreshFlags();
        GriefDefenderPlugin.getInstance().dataStore.setDefaultGlobalPermissions();
    }

    static {
        instance = new FlagRegistryModule();
    }
}
