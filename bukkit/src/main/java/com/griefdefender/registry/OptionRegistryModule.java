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

import com.griefdefender.api.Tristate;
import com.griefdefender.api.permission.option.Option;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.api.permission.option.type.CreateModeType;
import com.griefdefender.api.permission.option.type.GameModeType;
import com.griefdefender.api.permission.option.type.WeatherType;
import com.griefdefender.api.registry.CatalogRegistryModule;
import com.griefdefender.permission.option.GDOption;
import com.griefdefender.util.RegistryHelper;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings("rawtypes")
public class OptionRegistryModule implements CatalogRegistryModule<Option> {

    private static OptionRegistryModule instance;

    public static OptionRegistryModule getInstance() {
        return instance;
    }

    static {
        instance = new OptionRegistryModule();
    }

    protected final Map<String, Option> registryMap = new HashMap<>();
    protected final Map<String, Option> customMap = new HashMap<>();

    @Override
    public Optional<Option> getById(String id) {
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
    public Collection<Option> getAll() {
        return this.registryMap.values();
    }

    public Map<String, Option> getCustomAdditions() {
        return this.customMap;
    }

    @Override
    public void registerDefaults() {
        this.createKey("griefdefender:abandon-delay", "abandon-delay", Integer.class);
        this.createKey("griefdefender:abandon-return-ratio", "abandon-return-ratio", Double.class);
        this.createKey("griefdefender:create-limit", "create-limit", Integer.class);
        this.createKey("griefdefender:expiration", "expiration", Integer.class);
        this.createKey("griefdefender:max-size-x", "max-size-x", Integer.class);
        this.createKey("griefdefender:max-size-y", "max-size-y", Integer.class);
        this.createKey("griefdefender:max-size-z", "max-size-z", Integer.class);
        this.createKey("griefdefender:min-size-x", "min-size-x", Integer.class);
        this.createKey("griefdefender:min-size-y", "min-size-y", Integer.class);
        this.createKey("griefdefender:min-size-z", "min-size-z", Integer.class);
        this.createKey("griefdefender:tax-expiration", "tax-expiration", Integer.class);
        this.createKey("griefdefender:tax-expiration-days-keep", "tax-expiration-days-keep", Integer.class);
        this.createKey("griefdefender:tax-rate", "tax-rate", Double.class);
        this.createKey("griefdefender:blocks-accrued-per-hour", "blocks-accrued-per-hour", Integer.class);
        this.createKey("griefdefender:chest-expiration", "chest-expiration", Integer.class);
        this.createKey("griefdefender:create-mode", "create-mode", CreateModeType.class);
        this.createKey("griefdefender:economy-block-cost", "economy-block-cost", Double.class);
        this.createKey("griefdefender:economy-block-sell-return", "economy-block-sell-return", Double.class);
        this.createKey("griefdefender:initial-blocks", "initial-blocks", Integer.class);
        this.createKey("griefdefender:max-accrued-blocks", "max-accrued-blocks", Integer.class);
        this.createKey("griefdefender:max-level", "max-level", Integer.class);
        this.createKey("griefdefender:min-level", "min-level", Integer.class);
        this.createKey("griefdefender:radius-list", "radius-list", Integer.class);
        this.createKey("griefdefender:radius-inspect", "radius-inspect", Integer.class);
        this.createKey("griefdefender:raid", "raid", Boolean.class);
        this.createKey("griefdefender:spawn-limit", "spawn-limit", Integer.class);
        this.createKey("griefdefender:player-command", "player-command", String.class);
        this.createKey("griefdefender:player-deny-flight", "player-deny-flight", Boolean.class);
        this.createKey("griefdefender:player-deny-godmode", "player-deny-godmode", Boolean.class);
        this.createKey("griefdefender:player-deny-hunger", "player-deny-hunger", Boolean.class);
        this.createKey("griefdefender:player-gamemode", "player-gamemode", GameModeType.class);
        this.createKey("griefdefender:player-health-regen", "player-health-regen", Double.class);
        this.createKey("griefdefender:player-keep-inventory", "player-keep-inventory", Tristate.class);
        this.createKey("griefdefender:player-keep-level", "player-keep-level", Tristate.class);
        this.createKey("griefdefender:player-walk-speed", "player-walk-speed", Integer.class);
        this.createKey("griefdefender:player-weather", "player-weather", WeatherType.class);

        RegistryHelper.mapFields(Options.class, input -> {
            final String name = input.replace("_", "-");
            return this.registryMap.get("griefdefender:" + name.toLowerCase(Locale.ENGLISH));
        });
    }

    private void createKey(String id, String name, Class<?> clazz) {
        this.registryMap.put(id, new GDOption<>(id, name, clazz));
    }

    @Override
    public void registerCustomType(Option type) {
        this.customMap.put(type.getId(), type);
    }
}
