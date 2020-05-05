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

import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.claim.ClaimBlockSystem;
import com.griefdefender.api.permission.option.Options;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.util.HashMap;
import java.util.Map;

@ConfigSerializable
public class DefaultOptionCategory extends ConfigCategory {

    @Setting(value = "default-user-options", comment = "The default user options for all players.\nNote: Setting default claim type options will override this.")
    private Map<String, String> defaultUserOptions = new HashMap<>();

    @Setting(value = "default-user-basic-options", comment = "The default options applied to users for basic claims.\nNote: These options override default global options.")
    private Map<String, String> defaultBasicOptions = new HashMap<>();

    @Setting(value = "default-user-subdivision-options", comment = "The default options applied to users for subdivisions.\nNote: These options override default global options.")
    private Map<String, String> defaultSubdivisionOptions = new HashMap<>();

    @Setting(value = "default-user-town-options", comment = "The default options applied to users for towns.\nNote: These options override default global options.")
    private Map<String, String> defaultTownOptions = new HashMap<>();

    public DefaultOptionCategory() {
        final int maxAccruedBlocks = GriefDefenderPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.VOLUME ? 20480000 : 80000;
        this.defaultUserOptions.put(Options.ABANDON_DELAY.getName(), "0");
        this.defaultUserOptions.put(Options.ABANDON_RETURN_RATIO.getName(), "1.0");
        this.defaultUserOptions.put(Options.ACCRUED_BLOCKS.getName(), "0");
        this.defaultUserOptions.put(Options.BONUS_BLOCKS.getName(), "0");
        this.defaultUserOptions.put(Options.BLOCKS_ACCRUED_PER_HOUR.getName(), "120");
        this.defaultUserOptions.put(Options.CHEST_EXPIRATION.getName(), "7");
        this.defaultUserOptions.put(Options.CREATE_LIMIT.getName(), "-1");
        this.defaultUserOptions.put(Options.CREATE_MODE.getName(), "undefined");
        this.defaultUserOptions.put(Options.ECONOMY_BLOCK_COST.getName(), "0.0");
        this.defaultUserOptions.put(Options.ECONOMY_BLOCK_SELL_RETURN.getName(), "0.0");
        this.defaultUserOptions.put(Options.EXPIRATION.getName(), "14");
        this.defaultUserOptions.put(Options.INITIAL_BLOCKS.getName(), "120");
        this.defaultUserOptions.put(Options.MAX_ACCRUED_BLOCKS.getName(), Integer.toString(maxAccruedBlocks));
        this.defaultUserOptions.put(Options.MIN_LEVEL.getName(), "0");
        this.defaultUserOptions.put(Options.MAX_LEVEL.getName(), "255");
        this.defaultUserOptions.put(Options.PLAYER_DENY_FLIGHT.getName(), "false");
        this.defaultUserOptions.put(Options.PLAYER_DENY_GODMODE.getName(), "false");
        this.defaultUserOptions.put(Options.PLAYER_DENY_HUNGER.getName(), "false");
        this.defaultUserOptions.put(Options.PLAYER_GAMEMODE.getName(), "undefined");
        this.defaultUserOptions.put(Options.PLAYER_HEALTH_REGEN.getName(), "-1.0");
        this.defaultUserOptions.put(Options.PLAYER_KEEP_INVENTORY.getName(), "undefined");
        this.defaultUserOptions.put(Options.PLAYER_KEEP_LEVEL.getName(), "undefined");
        this.defaultUserOptions.put(Options.PLAYER_TELEPORT_DELAY.getName(), "0");
        this.defaultUserOptions.put(Options.PLAYER_WALK_SPEED.getName(), "-1");
        this.defaultUserOptions.put(Options.PLAYER_WEATHER.getName(), "undefined");
        this.defaultUserOptions.put(Options.PVP.getName(), "undefined");
        this.defaultUserOptions.put(Options.PVP_COMBAT_COMMAND.getName(), "false");
        this.defaultUserOptions.put(Options.PVP_COMBAT_TELEPORT.getName(), "false");
        this.defaultUserOptions.put(Options.PVP_COMBAT_TIMEOUT.getName(), "15");
        this.defaultUserOptions.put(Options.RAID.getName(), "true");
        this.defaultUserOptions.put(Options.RADIUS_INSPECT.getName(), "100");
        this.defaultUserOptions.put(Options.SPAWN_LIMIT.getName(), "-1");
        this.defaultUserOptions.put(Options.TAX_EXPIRATION.getName(), "7");
        this.defaultUserOptions.put(Options.TAX_EXPIRATION_DAYS_KEEP.getName(), "7");
        this.defaultUserOptions.put(Options.TAX_RATE.getName(), "1.0");

        this.defaultBasicOptions.put(Options.MIN_SIZE_X.getName(), "5");
        this.defaultBasicOptions.put(Options.MIN_SIZE_Y.getName(), "5");
        this.defaultBasicOptions.put(Options.MIN_SIZE_Z.getName(), "5");
        this.defaultBasicOptions.put(Options.MAX_SIZE_X.getName(), "0");
        this.defaultBasicOptions.put(Options.MAX_SIZE_Y.getName(), "256");
        this.defaultBasicOptions.put(Options.MAX_SIZE_Z.getName(), "0");

        this.defaultSubdivisionOptions.put(Options.MIN_SIZE_X.getName(), "1");
        this.defaultSubdivisionOptions.put(Options.MIN_SIZE_Y.getName(), "1");
        this.defaultSubdivisionOptions.put(Options.MIN_SIZE_Z.getName(), "1");
        this.defaultSubdivisionOptions.put(Options.MAX_SIZE_X.getName(), "1000");
        this.defaultSubdivisionOptions.put(Options.MAX_SIZE_Y.getName(), "256");
        this.defaultSubdivisionOptions.put(Options.MAX_SIZE_Z.getName(), "1000");

        this.defaultTownOptions.put(Options.CREATE_LIMIT.getName(), "1");
        this.defaultTownOptions.put(Options.MIN_LEVEL.getName(), "0");
        this.defaultTownOptions.put(Options.MIN_SIZE_X.getName(), "32");
        this.defaultTownOptions.put(Options.MIN_SIZE_Y.getName(), "32");
        this.defaultTownOptions.put(Options.MIN_SIZE_Z.getName(), "32");
        this.defaultTownOptions.put(Options.MAX_LEVEL.getName(), "255");
        this.defaultTownOptions.put(Options.MAX_SIZE_X.getName(), "0");
        this.defaultTownOptions.put(Options.MAX_SIZE_Y.getName(), "256");
        this.defaultTownOptions.put(Options.MAX_SIZE_Z.getName(), "0");
        this.defaultTownOptions.put(Options.TAX_EXPIRATION.getName(), "7");
        this.defaultTownOptions.put(Options.TAX_EXPIRATION_DAYS_KEEP.getName(), "7");
        this.defaultTownOptions.put(Options.TAX_RATE.getName(), "1.0");
    }

    public void checkOptions() {
        Map<String, String> options = new HashMap<>(this.defaultUserOptions);
        for (Map.Entry<String, String> mapEntry : options.entrySet()) {
            if (mapEntry.getKey().equalsIgnoreCase(Options.CREATE_LIMIT.getName()) && mapEntry.getValue().equals("0")) {
                this.defaultUserOptions.put(mapEntry.getKey(), "undefined");
                break;
            }
        }
        options = new HashMap<>(this.defaultBasicOptions);
        for (Map.Entry<String, String> mapEntry : options.entrySet()) {
            if (mapEntry.getKey().equalsIgnoreCase(Options.CREATE_LIMIT.getName()) && mapEntry.getValue().equals("0")) {
                this.defaultBasicOptions.put(mapEntry.getKey(), "undefined");
                break;
            }
        }
        options = new HashMap<>(this.defaultSubdivisionOptions);
        for (Map.Entry<String, String> mapEntry : options.entrySet()) {
            if (mapEntry.getKey().equalsIgnoreCase(Options.CREATE_LIMIT.getName()) && mapEntry.getValue().equals("0")) {
                this.defaultSubdivisionOptions.put(mapEntry.getKey(), "undefined");
                break;
            }
        }
        options = new HashMap<>(this.defaultTownOptions);
        for (Map.Entry<String, String> mapEntry : options.entrySet()) {
            if (mapEntry.getKey().equalsIgnoreCase(Options.CREATE_LIMIT.getName()) && mapEntry.getValue().equals("0")) {
                this.defaultTownOptions.put(mapEntry.getKey(), "undefined");
                break;
            }
        }
    }

    public Map<String, String> getBasicOptionDefaults() {
        return this.defaultBasicOptions;
    }

    public Map<String, String> getSubdivisionOptionDefaults() {
        return this.defaultSubdivisionOptions;
    }

    public Map<String, String> getTownOptionDefaults() {
        return this.defaultTownOptions;
    }

    public Map<String, String> getUserOptionDefaults() {
        return this.defaultUserOptions;
    }
}
