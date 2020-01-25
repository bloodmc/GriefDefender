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
package com.griefdefender.permission.option;

import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.permission.option.Option;
import com.griefdefender.api.permission.option.type.CreateModeType;
import com.griefdefender.api.permission.option.type.CreateModeTypes;
import com.griefdefender.api.permission.option.type.GameModeType;
import com.griefdefender.api.permission.option.type.GameModeTypes;
import com.griefdefender.api.permission.option.type.WeatherType;
import com.griefdefender.api.permission.option.type.WeatherTypes;

import net.kyori.text.Component;
import net.kyori.text.TextComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.Nullable;

public class GDOption<T> implements Option<T> {

    private static final List<String> GLOBAL_OPTIONS = Arrays.asList(
            "abandon-return-ratio", "accrued-blocks", "bonus-blocks", "blocks-accrued-per-hour", "chest-expiration", "economy-block-cost", 
            "economy-block-sell-return", "expiration", "initial-blocks", "max-accrued-blocks", "radius-list", 
            "radius-list");
    private static final List<String> ADMIN_OPTIONS = Arrays.asList(
            "player-command", "player-deny-godmode", "player-deny-hunger", "player-gamemode",
            "player-health-regen", "player-keep-inventory", "player-keep-level", "player-walk-speed",
            "radius-inspect", "radius-list");

    private final String id;
    private final String name;
    private final Class<T> allowed;
    private Set<String> requiredContextKeys = new HashSet<>();
    private Component description;
    private boolean multiValued;
    private Boolean isGlobal;
    private Boolean isAdmin;

    GDOption(OptionBuilder<T> builder) {
        this(builder.id, builder.name, builder.description, builder.multiValued, builder.requiredContextKeys, builder.typeClass);
    }

    public GDOption(String id, String name, Component description, boolean multiValued, Set<String> requiredContexts, Class<T> allowed) {
        this.id = id;
        this.name = name;
        this.allowed = allowed;
        this.description = description;
        this.multiValued = multiValued;
        this.requiredContextKeys = requiredContexts;
        this.isAdmin = ADMIN_OPTIONS.contains(name);
        this.isGlobal = GLOBAL_OPTIONS.contains(name);
    }

    @Override
    public String getId() {
        return this.id;
    }

    public String getPermission() {
        return "griefdefender." + this.name.toLowerCase();
    }

    public String getName() {
        return this.name;
    }

    public boolean isGlobal() {
        return this.isGlobal;
    }

    public boolean isAdmin() {
        return this.isAdmin;
    }

    @Override
    public boolean multiValued() {
        return this.multiValued;
    }

    @Override
    public Set<String> getRequiredContextKeys() {
        return this.requiredContextKeys;
    }

    @Override
    public Class<T> getAllowedType() {
        return this.allowed;
    }

    @Override
    public Component getDescription() {
        if (this.description == null) {
            this.description = this.createDescription();
        }
        return this.description;
    }

    @Override
    public String toString() {
        return this.name;
    }

    private Component createDescription() {
        final Component description = GriefDefenderPlugin.getInstance().messageData.getMessage("option-description-" + this.name.toLowerCase());
        if (description != null) {
            return description;
        }
        return TextComponent.of("Not defined.");
    }

    public void reloadDescription() {
        this.description = null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T getDefaultValue() {
        if (this.allowed.isAssignableFrom(Tristate.class)) {
            return (T) Tristate.UNDEFINED;
        }
        if (this.allowed.isAssignableFrom(String.class)) {
            return (T) "undefined";
        }
        if (this.allowed.isAssignableFrom(Integer.class)) {
            return (T) Integer.valueOf(-1);
        }
        if (this.allowed.isAssignableFrom(Double.class)) {
            return (T) Double.valueOf(0);
        }
        if (this.allowed.isAssignableFrom(Boolean.class)) {
            return (T) Boolean.FALSE;
        }
        if (this.allowed.isAssignableFrom(List.class)) {
            return (T) new ArrayList<>();
        }
        if (this.allowed.isAssignableFrom(CreateModeType.class)) {
            return (T) CreateModeTypes.AREA;
        }
        if (this.allowed.isAssignableFrom(GameModeType.class)) {
            return (T) GameModeTypes.UNDEFINED;
        }
        if (this.allowed.isAssignableFrom(WeatherType.class)) {
            return (T) WeatherTypes.UNDEFINED;
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Option)) {
            return false;
        }
        return this.id.equals(((Option<?>) o).getId());
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Nullable
    public boolean validateStringValue(String value, boolean log) {
        if (value.equalsIgnoreCase("undefined")) {
            return false;
        } else if (this.allowed == List.class) {
            return true;
        } else if (this.allowed == Integer.class) {
            try {
                Integer.parseInt(value);
            } catch (NumberFormatException e) {
                if (log) {
                    GriefDefenderPlugin.getInstance().getLogger().warning("Invalid Integer value '" + value + "', entered for option " + this.getName() 
                        + ".\nYou must use a valid number. Skipping...");
                }
                return false;
            }
            return true;
        } else if (this.allowed == Boolean.class) {
            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                return true;
            }

            if (log) {
                GriefDefenderPlugin.getInstance().getLogger().warning("Invalid Boolean value '" + value + "', entered for option " + this.getName() 
                    + ".\nAcceptable values are : true, or false. Skipping...");
            }
            return false;
        } else if (this.allowed == Double.class) {
            try {
                Double.parseDouble(value);
            } catch (NumberFormatException e) {
                if (log) {
                    GriefDefenderPlugin.getInstance().getLogger().warning("Invalid Double value '" + value + "', entered for option " + this.getName() 
                        + ".\nYou must use a valid number. Skipping...");
                }
                return false;
            }
            return true;
        } else if (this.allowed == Tristate.class) {
            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                return true;
            }
            if (log) {
                GriefDefenderPlugin.getInstance().getLogger().warning("Invalid Tristate value '" + value + "', entered for option " + this.getName() 
                    + ".\nAcceptable values are : true, false, or undefined. Skipping...");
            }
        } else if (this.allowed == CreateModeType.class) {
            if (value.equalsIgnoreCase("area") || value.equalsIgnoreCase("volume")) {
                return true;
            }
            if (log) {
                GriefDefenderPlugin.getInstance().getLogger().warning("Invalid CreateModeType value '" + value + "', entered for option " + this.getName() 
                    + ".\nAcceptable values are : area, volume, or undefined. Skipping...");
            }
        } else if (this.allowed == GameModeType.class) {
            if (value.equalsIgnoreCase("survival") || value.equalsIgnoreCase("adventure") || value.equalsIgnoreCase("creative") || value.equalsIgnoreCase("spectator")) {
                return true;
            }
            if (log) {
                GriefDefenderPlugin.getInstance().getLogger().warning("Invalid GameModeType value '" + value + "', entered for option " + this.getName() 
                    + ".\nAcceptable values are : adventure, creative, survival, spectator, or undefined. Skipping...");
            }
        } else if (this.allowed == WeatherType.class) {
            if (value.equalsIgnoreCase("clear") || value.equalsIgnoreCase("downfall")) {
                return true;
            }
            if (log) {
                GriefDefenderPlugin.getInstance().getLogger().warning("Invalid WeatherType value '" + value + "', entered for option " + this.getName() 
                    + ".\nAcceptable values are : clear, downfall, or undefined. Skipping...");
            }
        }
 
        return false;
    }
}
