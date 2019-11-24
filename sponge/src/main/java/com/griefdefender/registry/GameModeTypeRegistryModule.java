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

import com.griefdefender.api.permission.option.type.GameModeType;
import com.griefdefender.api.permission.option.type.GameModeTypes;
import com.griefdefender.api.registry.CatalogRegistryModule;
import com.griefdefender.permission.option.type.GDGameModeType;
import com.griefdefender.util.RegistryHelper;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class GameModeTypeRegistryModule implements CatalogRegistryModule<GameModeType> {

    private static GameModeTypeRegistryModule instance;

    public static GameModeTypeRegistryModule getInstance() {
        return instance;
    }

    private final Map<String, GameModeType> registryMap = new HashMap<>();

    @Override
    public Optional<GameModeType> getById(String id) {
        if (id == null) {
            return Optional.empty();
        }

        if (id.contains("griefdefender.")) {
            id = id.replace("griefdefender.", "griefdefender:");
        }
        if (!id.contains(":")) {
            id = "griefdefender:" + id;
        }

        return Optional.ofNullable(this.registryMap.get(checkNotNull(id)));
    }

    @Override
    public Collection<GameModeType> getAll() {
        return this.registryMap.values();
    }

    @Override
    public void registerDefaults() {
        RegistryHelper.mapFields(GameModeTypes.class, input -> {
            final String name = input.toLowerCase().replace("_", "-");
            final String id = "griefdefender:" + name;
            final GameModeType type = new GDGameModeType(id, name);
            this.registryMap.put(id, type);
            return type;
        });
    }

    @Override
    public void registerCustomType(GameModeType type) {
        // TODO Auto-generated method stub
        
    }

    static {
        instance = new GameModeTypeRegistryModule();
    }
}
