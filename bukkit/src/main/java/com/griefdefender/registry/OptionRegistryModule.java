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

import com.griefdefender.api.permission.option.Option;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.api.registry.CatalogRegistryModule;
import com.griefdefender.permission.GDOption;
import com.griefdefender.util.RegistryHelper;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class OptionRegistryModule implements CatalogRegistryModule<Option> {

    private static OptionRegistryModule instance;

    public static OptionRegistryModule getInstance() {
        return instance;
    }

    private final Map<String, Option> optionMap = new HashMap<>();
    private final Map<String, Option> customMap = new HashMap<>();

    @Override
    public Optional<Option> getById(String id) {
        if (id == null) {
            return Optional.empty();
        }

        id = id.replace("griefdefender.", "");
        String[] parts = id.split("\\.");
        if (parts.length > 0) {
            id = parts[0];
        }
 
        return Optional.ofNullable(this.optionMap.get(checkNotNull(id)));
    }

    @Override
    public Collection<Option> getAll() {
        return this.optionMap.values();
    }

    public Map<String, Option> getCustomAdditions() {
        return this.customMap;
    }

    @Override
    public void registerDefaults() {
        RegistryHelper.mapFields(Options.class, input -> {
            final String field = input.toLowerCase().replace("_", "-");
            final Option option = new GDOption("griefdefender:" + field, field);
            this.optionMap.put(field.toLowerCase(Locale.ENGLISH), option);
            return option;
        });
    }

    @Override
    public void registerCustomType(Option type) {
        this.optionMap.put(type.getName().toLowerCase(Locale.ENGLISH), type);
        this.customMap.put(type.getName().toLowerCase(Locale.ENGLISH), type);
    }

    static {
        instance = new OptionRegistryModule();
    }
}
