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
import com.griefdefender.api.permission.option.Option;
import com.griefdefender.api.permission.option.Options;

public class GDOptions {

    public static boolean PLAYER_COMMAND_ENTER;
    public static boolean PLAYER_COMMAND_EXIT;
    public static boolean PLAYER_DENY_FLIGHT;
    public static boolean PLAYER_DENY_GODMODE;
    public static boolean PLAYER_DENY_HUNGER;
    public static boolean PLAYER_FLY_SPEED;
    public static boolean PLAYER_GAMEMODE;
    public static boolean PLAYER_HEALTH_REGEN;
    public static boolean PLAYER_ITEM_DROP_LOCK;
    public static boolean PLAYER_KEEP_INVENTORY;
    public static boolean PLAYER_KEEP_LEVEL;
    public static boolean PLAYER_TELEPORT_DELAY;
    public static boolean PLAYER_WALK_SPEED;
    public static boolean PLAYER_WEATHER;
    public static boolean PVP;
    public static boolean PVP_COMBAT_COMMAND;
    public static boolean PVP_COMBAT_TELEPORT;
    public static boolean PVP_COMBAT_TIMEOUT;
    public static boolean PVP_ITEM_DROP_LOCK;
    public static boolean SPAWN_LIMIT;

    public static void populateOptionStatus() {
        PLAYER_COMMAND_ENTER = GriefDefenderPlugin.getOptionConfig().getConfig().isOptionEnabled(Options.PLAYER_COMMAND_ENTER.getName().toLowerCase());
        PLAYER_COMMAND_EXIT = GriefDefenderPlugin.getOptionConfig().getConfig().isOptionEnabled(Options.PLAYER_COMMAND_EXIT.getName().toLowerCase());
        PLAYER_DENY_FLIGHT = GriefDefenderPlugin.getOptionConfig().getConfig().isOptionEnabled(Options.PLAYER_DENY_FLIGHT.getName().toLowerCase());
        PLAYER_DENY_GODMODE = GriefDefenderPlugin.getOptionConfig().getConfig().isOptionEnabled(Options.PLAYER_DENY_GODMODE.getName().toLowerCase());
        PLAYER_DENY_HUNGER = GriefDefenderPlugin.getOptionConfig().getConfig().isOptionEnabled(Options.PLAYER_DENY_HUNGER.getName().toLowerCase());
        PLAYER_FLY_SPEED = GriefDefenderPlugin.getOptionConfig().getConfig().isOptionEnabled(Options.PLAYER_FLY_SPEED.getName().toLowerCase());
        PLAYER_GAMEMODE = GriefDefenderPlugin.getOptionConfig().getConfig().isOptionEnabled(Options.PLAYER_GAMEMODE.getName().toLowerCase());
        PLAYER_HEALTH_REGEN = GriefDefenderPlugin.getOptionConfig().getConfig().isOptionEnabled(Options.PLAYER_HEALTH_REGEN.getName().toLowerCase());
        PLAYER_ITEM_DROP_LOCK = GriefDefenderPlugin.getOptionConfig().getConfig().isOptionEnabled(Options.PLAYER_ITEM_DROP_LOCK.getName().toLowerCase());
        PLAYER_KEEP_INVENTORY = GriefDefenderPlugin.getOptionConfig().getConfig().isOptionEnabled(Options.PLAYER_KEEP_INVENTORY.getName().toLowerCase());
        PLAYER_KEEP_LEVEL = GriefDefenderPlugin.getOptionConfig().getConfig().isOptionEnabled(Options.PLAYER_KEEP_LEVEL.getName().toLowerCase());
        PLAYER_TELEPORT_DELAY = GriefDefenderPlugin.getOptionConfig().getConfig().isOptionEnabled(Options.PLAYER_TELEPORT_DELAY.getName().toLowerCase());
        PLAYER_WALK_SPEED = GriefDefenderPlugin.getOptionConfig().getConfig().isOptionEnabled(Options.PLAYER_WALK_SPEED.getName().toLowerCase());
        PLAYER_WEATHER = GriefDefenderPlugin.getOptionConfig().getConfig().isOptionEnabled(Options.PLAYER_WEATHER.getName().toLowerCase());
        PVP = GriefDefenderPlugin.getOptionConfig().getConfig().isOptionEnabled(Options.PVP.getName().toLowerCase());
        PVP_COMBAT_COMMAND = GriefDefenderPlugin.getOptionConfig().getConfig().isOptionEnabled(Options.PVP_COMBAT_COMMAND.getName().toLowerCase());
        PVP_COMBAT_TELEPORT = GriefDefenderPlugin.getOptionConfig().getConfig().isOptionEnabled(Options.PVP_COMBAT_TELEPORT.getName().toLowerCase());
        PVP_COMBAT_TIMEOUT = GriefDefenderPlugin.getOptionConfig().getConfig().isOptionEnabled(Options.PVP_COMBAT_TIMEOUT.getName().toLowerCase());
        PVP_ITEM_DROP_LOCK = GriefDefenderPlugin.getOptionConfig().getConfig().isOptionEnabled(Options.PVP_ITEM_DROP_LOCK.getName().toLowerCase());
        SPAWN_LIMIT = GriefDefenderPlugin.getOptionConfig().getConfig().isOptionEnabled(Options.SPAWN_LIMIT.getName().toLowerCase());
    }

    public static boolean isOptionEnabled(Option option) {
        if (option == null) {
            return false;
        }
        if (!option.getName().toLowerCase().contains("player") && !option.getName().toLowerCase().contains("spawn") && !option.getName().toLowerCase().contains("pvp")) {
            return true;
        }
        return GriefDefenderPlugin.getOptionConfig().getConfig().isOptionEnabled(option.getName().toLowerCase());
    }
}
