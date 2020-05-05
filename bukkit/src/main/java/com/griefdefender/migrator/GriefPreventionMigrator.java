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
package com.griefdefender.migrator;

import com.flowpowered.math.vector.Vector3i;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.api.claim.ClaimTypes;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.ContextKeys;
import com.griefdefender.api.permission.flag.Flags;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.configuration.ClaimDataConfig;
import com.griefdefender.configuration.ClaimStorageData;
import com.griefdefender.internal.util.BlockUtil;
import com.griefdefender.permission.GDPermissionHolder;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.flag.FlagContexts;
import com.griefdefender.permission.option.OptionContexts;
import com.griefdefender.storage.BaseStorage;
import com.griefdefender.util.PermissionUtil;

import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import org.bukkit.World;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GriefPreventionMigrator {

    private static final File gpBukkitPlayerDataMigrated = BaseStorage.globalPlayerDataPath.resolve("_bukkitMigrated").toFile();
    private static final Path gpFlags = Paths.get("plugins", "GPFlags", "flags.yml");
    private static final Map<Integer, UUID> idToUUID = new ConcurrentHashMap<>();
    private static final Map<UUID, ClaimStorageData> claimStorageMap = new ConcurrentHashMap<>();
    private static final GDPermissionHolder DEFAULT_HOLDER = GriefDefenderPlugin.DEFAULT_HOLDER;
    private static int count;

    private static final String FLAG_CHANGE_BIOME = "changebiome";
    private static final String FLAG_COMMAND_BLACKLIST = "commandblacklist";
    private static final String FLAG_COMMAND_WHITELIST = "commandwhitelist";
    private static final String FLAG_ENTER_COMMAND = "entercommand";
    private static final String FLAG_ENTER_COMMAND_MEMBERS = "entercommandmembers";
    private static final String FLAG_ENTER_COMMAND_OWNER = "entercommandowner";
    private static final String FLAG_ENTER_MESSAGE = "entermessage";
    private static final String FLAG_ENTER_PLAYER_COMMAND = "enterplayercommand";
    private static final String FLAG_EXIT_COMMAND = "exitcommand";
    private static final String FLAG_EXIT_COMMAND_MEMBERS = "exitcommandmembers";
    private static final String FLAG_EXIT_COMMAND_OWNER = "exitcommandowner";
    private static final String FLAG_EXIT_MESSAGE = "exitmessage";
    private static final String FLAG_EXIT_PLAYER_COMMAND = "exitplayercommand";
    private static final String FLAG_HEALTH_REGEN = "healthregen";
    private static final String FLAG_INFINITE_ARROWS = "infinitearrows";
    private static final String FLAG_KEEP_INVENTORY = "keepinventory";
    private static final String FLAG_KEEP_LEVEL = "keeplevel";
    private static final String FLAG_NETHER_PORTAL_CONSOLE_COMMAND = "netherportalconsolecommand";
    private static final String FLAG_NETHER_PORTAL_PLAYER_COMMAND = "netherportalplayercommand";
    private static final String FLAG_NO_CHORUS_FRUIT = "nochorusfruit";
    private static final String FLAG_NO_COMBAT_LOOT = "nocombatloot";
    private static final String FLAG_NO_ENDER_PEARL = "noenderpearl";
    private static final String FLAG_NO_ENTER = "noenter";
    private static final String FLAG_NO_ENTER_PLAYER = "noenterplayer";
    private static final String FLAG_NO_EXPIRATION = "noexpiration";
    private static final String FLAG_NO_EXPLOSION_DAMAGE = "noexplosiondamage";
    private static final String FLAG_NO_FALL_DAMAGE = "nofalldamage";
    private static final String FLAG_NO_FIRE_DAMAGE = "nofiredamage";
    private static final String FLAG_NO_FIRE_SPREAD = "nofirespread";
    private static final String FLAG_NO_FLIGHT = "noflight";
    private static final String FLAG_NO_FLUID_FLOW = "nofluidflow";
    private static final String FLAG_NO_GROWTH = "nogrowth";
    private static final String FLAG_NO_HUNGER = "nohunger";
    private static final String FLAG_NO_ICE_FORM = "noiceform";
    private static final String FLAG_NO_ITEM_DAMAGE = "noitemdamage";
    private static final String FLAG_NO_ITEM_DROP = "noitemdrop";
    private static final String FLAG_NO_ITEM_PICKUP = "noitempickup";
    private static final String FLAG_NO_LEAF_DECAY = "noleafdecay";
    private static final String FLAG_NO_LOOT_PROTECTION = "nolootprotection";
    private static final String FLAG_NO_MOB_DAMAGE = "nomobdamage";
    private static final String FLAG_NO_MOB_SPAWNS = "nomobspawns";
    private static final String FLAG_NO_MOB_SPAWNS_TYPE = "nomobspawnstype";
    private static final String FLAG_NO_OPEN_DOORS = "noopendoors";
    private static final String FLAG_NO_PET_DAMAGE = "nopetdamage";
    private static final String FLAG_NO_PLAYER_DAMAGE = "noplayerdamage";
    private static final String FLAG_NO_SNOW_FORM = "nosnowform";
    private static final String FLAG_NO_VEHICLE = "novehicle";
    private static final String FLAG_NO_VINE_GROWTH = "novinegrowth";
    private static final String FLAG_NO_WEATHER_CHANGE = "noweatherchange";
    private static final String FLAG_OWNER_FLY = "ownerfly";
    private static final String FLAG_OWNER_MEMBER_FLY = "ownermemberfly";
    private static final String FLAG_PLAYER_GAMEMODE = "playergamemode";
    private static final String FLAG_PLAYER_TIME = "playertime";
    private static final String FLAG_PLAYER_WEATHER = "playerweather";
    private static final String FLAG_RESPAWN_LOCATION = "respawnlocation";
    private static final String FLAG_SPLEEF_ARENA = "spleefarena";
    private static final String FLAG_TRAPPED_DESTINATION = "trappeddestination";

    public static void migrate(World world, Path gpClassicDataPath) throws FileNotFoundException, ClassNotFoundException {
        count = 0;
        GriefDefenderPlugin.getInstance().getLogger().info("Starting GriefPrevention data migration for world " + world.getName() + "...");
        // Migrate playerdata first
        migratePlayerData(world);
        File[] files = gpClassicDataPath.toFile().listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                if (file.isFile()) {
                    try {
                        int claimId = Integer.parseInt(file.getName().replaceFirst("[.][^.]+$", ""));
                        idToUUID.put(claimId, UUID.randomUUID());
                    } catch (NumberFormatException e) {
                        if (file.getName().equalsIgnoreCase("_nextClaimID")) {
                            // GP keeps track of next claim ID in this file so we can safely ignore the exception
                            continue;
                        }
                        e.printStackTrace();
                        continue;
                    }
                }
            }
            migrateClaims(world, files, true);
            migrateClaims(world, files, false);
            GriefDefenderPlugin.getInstance().getLogger().info("Finished GriefPrevention data migration for world '" + world.getName() + "'."
                    + " Migrated a total of " + count + " claims.");
        }
        if (Files.exists(gpFlags)) {
            migrateGpFlags(world);
        }
    }

    private static void migrateGpFlags(World world) {
        YAMLConfigurationLoader flagsManager = YAMLConfigurationLoader.builder().setPath(gpFlags).build();
        ConfigurationNode root = null;
        try {
            root = flagsManager.load();
        } catch (IOException e1) {
            e1.printStackTrace();
            return;
        }

        int count = 0;
        for (Entry<Object, ? extends ConfigurationNode> claims : root.getChildrenMap().entrySet()) {
            Object key = claims.getKey();
            int bukkitClaimId = -1;
            try {
                bukkitClaimId = Integer.parseInt(key.toString());
            } catch (NumberFormatException e) {
                e.printStackTrace();
                continue;
            }

            final UUID claimUniqueId = idToUUID.get(bukkitClaimId);
            if (claimUniqueId == null) {
                GriefDefenderPlugin.getInstance().getLogger().info("Could not locate migrated GD claim for id '" + bukkitClaimId + "'. Skipping...");
                continue;
            }

            final Set<Context> contexts = new HashSet<>();
            final ClaimStorageData claimStorage = claimStorageMap.get(claimUniqueId);
            if (claimStorage == null) {
                // Different world
                continue;
            }
            count++;
            final UUID ownerUniqueId = claimStorage.getConfig().getOwnerUniqueId();
            GDPermissionUser claimOwner = null;
            if (ownerUniqueId != null) {
                claimOwner = PermissionHolderCache.getInstance().getOrCreateUser(ownerUniqueId);
            }
            contexts.add(new Context(ContextKeys.CLAIM, claimUniqueId.toString()));
            ConfigurationNode flags = claims.getValue();
            GriefDefenderPlugin.getInstance().getLogger().info("Starting flag migration for legacy claim id '" + bukkitClaimId + "'...");
            for (Entry<Object, ? extends ConfigurationNode> flagEntry : flags.getChildrenMap().entrySet()){
                final String flag = (String) flagEntry.getKey();
                final ConfigurationNode paramNode = flagEntry.getValue();
                final String param = paramNode.getNode("params").getString();
                final boolean value = paramNode.getNode("value").getBoolean();
                if (!value) {
                    continue;
                }
                GriefDefenderPlugin.getInstance().getLogger().info("Migrating flag '" + flag + "'...");
                switch (flag) {
                    case FLAG_COMMAND_BLACKLIST :
                    case FLAG_COMMAND_WHITELIST :
                        // TODO
                        break;
                    case FLAG_ENTER_MESSAGE :
                        claimStorage.getConfig().setGreeting(LegacyComponentSerializer.legacy().deserialize(param, '§'));
                        claimStorage.getConfig().setRequiresSave(true);
                        claimStorage.save();
                        break;
                    case FLAG_ENTER_COMMAND :
                        contexts.add(new Context(ContextKeys.FLAG, Flags.ENTER_CLAIM.getName()));
                        contexts.add(FlagContexts.TARGET_PLAYER);
                        contexts.add(OptionContexts.COMMAND_RUNAS_CONSOLE);
                        contexts.add(OptionContexts.COMMAND_RUNFOR_PUBLIC);
                        PermissionUtil.getInstance().setOptionValue(DEFAULT_HOLDER, Options.PLAYER_COMMAND_ENTER.getPermission(), param, contexts);
                        break;
                    case FLAG_ENTER_COMMAND_OWNER :
                        contexts.add(new Context(ContextKeys.FLAG, Flags.ENTER_CLAIM.getName()));
                        contexts.add(FlagContexts.TARGET_PLAYER);
                        contexts.add(OptionContexts.COMMAND_RUNAS_CONSOLE);
                        contexts.add(OptionContexts.COMMAND_RUNFOR_OWNER);
                        if (claimOwner == null) {
                            GriefDefenderPlugin.getInstance().getLogger().info("Could not locate owner legacy claim id '" + bukkitClaimId + "'. Skipping...");
                            break;
                        }
                        PermissionUtil.getInstance().setOptionValue(claimOwner, Options.PLAYER_COMMAND_ENTER.getPermission(), param, contexts);
                        break;
                    case FLAG_ENTER_COMMAND_MEMBERS : {
                        contexts.add(new Context(ContextKeys.FLAG, Flags.ENTER_CLAIM.getName()));
                        contexts.add(FlagContexts.TARGET_PLAYER);
                        contexts.add(OptionContexts.COMMAND_RUNAS_CONSOLE);
                        contexts.add(OptionContexts.COMMAND_RUNFOR_MEMBER);
                        List<UUID> members = new ArrayList<>();
                        members.addAll(claimStorage.getConfig().getAccessors());
                        members.addAll(claimStorage.getConfig().getBuilders());
                        members.addAll(claimStorage.getConfig().getContainers());
                        members.addAll(claimStorage.getConfig().getManagers());
                        for (UUID memberUniqueId : members) {
                            final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(memberUniqueId);
                            PermissionUtil.getInstance().setOptionValue(user, Options.PLAYER_COMMAND_ENTER.getPermission(), param, contexts);
                        }
                        break;
                    }
                    case FLAG_ENTER_PLAYER_COMMAND :
                        contexts.add(new Context(ContextKeys.FLAG, Flags.ENTER_CLAIM.getName()));
                        contexts.add(FlagContexts.TARGET_PLAYER);
                        contexts.add(OptionContexts.COMMAND_RUNAS_PLAYER);
                        contexts.add(OptionContexts.COMMAND_RUNFOR_PUBLIC);
                        PermissionUtil.getInstance().setOptionValue(DEFAULT_HOLDER, Options.PLAYER_COMMAND_ENTER.getPermission(), param, contexts);
                        break;
                    case FLAG_EXIT_COMMAND_MEMBERS : {
                        contexts.add(new Context(ContextKeys.FLAG, Flags.EXIT_CLAIM.getName()));
                        contexts.add(FlagContexts.TARGET_PLAYER);
                        contexts.add(OptionContexts.COMMAND_RUNAS_PLAYER);
                        contexts.add(OptionContexts.COMMAND_RUNFOR_MEMBER);
                        List<UUID> members = new ArrayList<>();
                        members.addAll(claimStorage.getConfig().getAccessors());
                        members.addAll(claimStorage.getConfig().getBuilders());
                        members.addAll(claimStorage.getConfig().getContainers());
                        members.addAll(claimStorage.getConfig().getManagers());
                        for (UUID memberUniqueId : members) {
                            final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(memberUniqueId);
                            PermissionUtil.getInstance().setOptionValue(user, Options.PLAYER_COMMAND_EXIT.getPermission(), param, contexts);
                        }
                        break;
                    }
                    case FLAG_EXIT_COMMAND_OWNER :
                        contexts.add(new Context(ContextKeys.FLAG, Flags.EXIT_CLAIM.getName()));
                        contexts.add(FlagContexts.TARGET_PLAYER);
                        contexts.add(OptionContexts.COMMAND_RUNAS_CONSOLE);
                        contexts.add(OptionContexts.COMMAND_RUNFOR_MEMBER);
                        if (claimOwner == null) {
                            GriefDefenderPlugin.getInstance().getLogger().info("Could not locate owner legacy claim id '" + bukkitClaimId + "'. Skipping...");
                            break;
                        }
                        PermissionUtil.getInstance().setOptionValue(claimOwner, Options.PLAYER_COMMAND_EXIT.getPermission(), param, contexts);
                        break;
                    case FLAG_EXIT_COMMAND :
                    case FLAG_EXIT_PLAYER_COMMAND :
                        contexts.add(new Context(ContextKeys.FLAG, Flags.EXIT_CLAIM.getName()));
                        contexts.add(FlagContexts.TARGET_PLAYER);
                        contexts.add(OptionContexts.COMMAND_RUNAS_PLAYER);
                        contexts.add(OptionContexts.COMMAND_RUNFOR_PUBLIC);
                        PermissionUtil.getInstance().setOptionValue(DEFAULT_HOLDER, Options.PLAYER_COMMAND_EXIT.getPermission(), param, contexts);
                        break;
                    case FLAG_EXIT_MESSAGE :
                        claimStorage.getConfig().setFarewell(LegacyComponentSerializer.legacy().deserialize(param, '§'));
                        claimStorage.getConfig().setRequiresSave(true);
                        claimStorage.save();
                        break;
                    case FLAG_HEALTH_REGEN :
                        PermissionUtil.getInstance().setOptionValue(DEFAULT_HOLDER, Options.PLAYER_HEALTH_REGEN.getPermission(), param, contexts);
                        break;
                    case FLAG_KEEP_INVENTORY :
                        PermissionUtil.getInstance().setOptionValue(DEFAULT_HOLDER, Options.PLAYER_KEEP_INVENTORY.getPermission(), "1", contexts);
                        break;
                    case FLAG_KEEP_LEVEL :
                        PermissionUtil.getInstance().setOptionValue(DEFAULT_HOLDER, Options.PLAYER_KEEP_LEVEL.getPermission(), "1", contexts);
                        break;
                    case FLAG_NO_CHORUS_FRUIT :
                        contexts.add(new Context(ContextKeys.TARGET, "chorus_fruit"));
                        PermissionUtil.getInstance().setPermissionValue(DEFAULT_HOLDER, Flags.ITEM_USE.getPermission(), Tristate.FALSE, contexts);
                        break;
                    case FLAG_NO_ENDER_PEARL :
                        contexts.add(new Context(ContextKeys.TARGET, "ender_pearl"));
                        PermissionUtil.getInstance().setPermissionValue(DEFAULT_HOLDER, Flags.INTERACT_ITEM_SECONDARY.getPermission(), Tristate.FALSE, contexts);
                        break;
                    case FLAG_NO_ENTER :
                        contexts.add(new Context(ContextKeys.TARGET, "player"));
                        PermissionUtil.getInstance().setPermissionValue(DEFAULT_HOLDER, Flags.ENTER_CLAIM.getPermission(), Tristate.FALSE, contexts);
                        break;
                    case FLAG_NO_ENTER_PLAYER :
                        // TODO
                        break;
                    case FLAG_NO_EXPIRATION :
                        claimStorage.getConfig().setExpiration(false);
                        claimStorage.getConfig().setRequiresSave(true);
                        claimStorage.save();
                        break;
                    case FLAG_NO_EXPLOSION_DAMAGE :
                        PermissionUtil.getInstance().setPermissionValue(DEFAULT_HOLDER, Flags.EXPLOSION_BLOCK.getPermission(), Tristate.FALSE, contexts);
                        PermissionUtil.getInstance().setPermissionValue(DEFAULT_HOLDER, Flags.EXPLOSION_ENTITY.getPermission(), Tristate.FALSE, contexts);
                        break;
                    case FLAG_NO_FALL_DAMAGE :
                        contexts.add(new Context(ContextKeys.SOURCE, "fall"));
                        contexts.add(FlagContexts.TARGET_PLAYER);
                        PermissionUtil.getInstance().setPermissionValue(DEFAULT_HOLDER, Flags.ENTITY_DAMAGE.getPermission(), Tristate.FALSE, contexts);
                        break;
                    case FLAG_NO_FIRE_DAMAGE :
                        contexts.add(new Context(ContextKeys.SOURCE, "fire"));
                        contexts.add(FlagContexts.TARGET_PLAYER);
                        PermissionUtil.getInstance().setPermissionValue(DEFAULT_HOLDER, Flags.ENTITY_DAMAGE.getPermission(), Tristate.FALSE, contexts);
                        break;
                    case FLAG_NO_FIRE_SPREAD :
                        contexts.add(new Context(ContextKeys.SOURCE, "fire"));
                        PermissionUtil.getInstance().setPermissionValue(DEFAULT_HOLDER, Flags.BLOCK_SPREAD.getPermission(), Tristate.FALSE, contexts);
                        break;
                    case FLAG_NO_FLIGHT :
                        PermissionUtil.getInstance().setOptionValue(DEFAULT_HOLDER, Options.PLAYER_DENY_FLIGHT.getPermission(), "true", contexts);
                        break;
                    case FLAG_NO_FLUID_FLOW :
                        PermissionUtil.getInstance().setPermissionValue(DEFAULT_HOLDER, Flags.LIQUID_FLOW.getPermission(), Tristate.FALSE, contexts);
                        break;
                    case FLAG_NO_GROWTH :
                        PermissionUtil.getInstance().setPermissionValue(DEFAULT_HOLDER, Flags.BLOCK_GROW.getPermission(), Tristate.FALSE, contexts);
                        break;
                    case FLAG_NO_HUNGER :
                        PermissionUtil.getInstance().setOptionValue(DEFAULT_HOLDER, Options.PLAYER_DENY_HUNGER.getPermission(), "true", contexts);
                        break;
                    case FLAG_NO_ICE_FORM :
                        contexts.add(FlagContexts.TARGET_ICE_FORM);
                        PermissionUtil.getInstance().setPermissionValue(DEFAULT_HOLDER, Flags.BLOCK_MODIFY.getPermission(), Tristate.FALSE, contexts);
                        break;
                    case FLAG_NO_ITEM_DROP :
                        contexts.add(FlagContexts.TARGET_PLAYER);
                        PermissionUtil.getInstance().setPermissionValue(DEFAULT_HOLDER, Flags.ITEM_DROP.getPermission(), Tristate.FALSE, contexts);
                        break;
                    case FLAG_NO_ITEM_PICKUP :
                        contexts.add(FlagContexts.TARGET_PLAYER);
                        PermissionUtil.getInstance().setPermissionValue(DEFAULT_HOLDER, Flags.ITEM_PICKUP.getPermission(), Tristate.FALSE, contexts);
                        break;
                    case FLAG_NO_LEAF_DECAY :
                        PermissionUtil.getInstance().setPermissionValue(DEFAULT_HOLDER, Flags.LEAF_DECAY.getPermission(), Tristate.FALSE, contexts);
                        break;
                    case FLAG_NO_MOB_DAMAGE :
                        contexts.add(FlagContexts.SOURCE_PLAYER);
                        contexts.add(new Context(ContextKeys.TARGET, "#monster"));
                        PermissionUtil.getInstance().setPermissionValue(DEFAULT_HOLDER, Flags.ENTITY_DAMAGE.getPermission(), Tristate.FALSE, contexts);
                        break;
                    case FLAG_NO_MOB_SPAWNS :
                        contexts.add(new Context(ContextKeys.TARGET, "#monster"));
                        PermissionUtil.getInstance().setPermissionValue(DEFAULT_HOLDER, Flags.ENTITY_SPAWN.getPermission(), Tristate.FALSE, contexts);
                        break;
                    case FLAG_NO_PLAYER_DAMAGE :
                        contexts.add(new Context(ContextKeys.TARGET, "player"));
                        PermissionUtil.getInstance().setPermissionValue(DEFAULT_HOLDER, Flags.ENTITY_DAMAGE.getPermission(), Tristate.FALSE, contexts);
                        break;
                    case FLAG_NO_SNOW_FORM :
                        if (GriefDefenderPlugin.getMajorMinecraftVersion() > 12) {
                            contexts.add(FlagContexts.TARGET_SNOW);
                        } else {
                            contexts.add(FlagContexts.TARGET_SNOW_1_12);
                        }
                        PermissionUtil.getInstance().setPermissionValue(DEFAULT_HOLDER, Flags.BLOCK_MODIFY.getPermission(), Tristate.FALSE, contexts);
                        break;
                    case FLAG_NO_VEHICLE :
                        contexts.add(FlagContexts.TARGET_TYPE_VEHICLE);
                        PermissionUtil.getInstance().setPermissionValue(DEFAULT_HOLDER, Flags.INTERACT_ENTITY_SECONDARY.getPermission(), Tristate.FALSE, contexts);
                        break;
                    case FLAG_NO_VINE_GROWTH :
                        contexts.add(FlagContexts.SOURCE_VINE);
                        PermissionUtil.getInstance().setPermissionValue(DEFAULT_HOLDER, Flags.BLOCK_GROW.getPermission(), Tristate.FALSE, contexts);
                        break;
                    case FLAG_OWNER_FLY : {
                        if (claimOwner == null) {
                            GriefDefenderPlugin.getInstance().getLogger().info("Could not locate owner legacy claim id '" + bukkitClaimId + "'. Skipping...");
                            break;
                        }
                        PermissionUtil.getInstance().setOptionValue(claimOwner, Options.PLAYER_DENY_FLIGHT.getPermission(), "1", contexts);
                        break;
                    }
                    case FLAG_OWNER_MEMBER_FLY : {
                        List<UUID> members = new ArrayList<>();
                        members.addAll(claimStorage.getConfig().getAccessors());
                        members.addAll(claimStorage.getConfig().getBuilders());
                        members.addAll(claimStorage.getConfig().getContainers());
                        members.addAll(claimStorage.getConfig().getManagers());
                        for (UUID memberUniqueId : members) {
                            final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(memberUniqueId);
                            PermissionUtil.getInstance().setOptionValue(user, Options.PLAYER_DENY_FLIGHT.getPermission(), "1", contexts);
                        }
                        break;
                    }

                    case FLAG_RESPAWN_LOCATION :
                        // TODO
                        break;

                    // NOT CURRENTLY SUPPORTED
                    case FLAG_CHANGE_BIOME :
                    case FLAG_INFINITE_ARROWS :
                    case FLAG_NETHER_PORTAL_CONSOLE_COMMAND :
                    case FLAG_NETHER_PORTAL_PLAYER_COMMAND :
                    case FLAG_NO_COMBAT_LOOT :
                    case FLAG_NO_ITEM_DAMAGE :
                    case FLAG_NO_LOOT_PROTECTION :
                    case FLAG_NO_MOB_SPAWNS_TYPE :
                    case FLAG_NO_OPEN_DOORS :
                    case FLAG_NO_PET_DAMAGE :
                    case FLAG_NO_WEATHER_CHANGE :
                    case FLAG_PLAYER_GAMEMODE :
                    case FLAG_PLAYER_TIME :
                    case FLAG_PLAYER_WEATHER :
                    case FLAG_SPLEEF_ARENA :
                    case FLAG_TRAPPED_DESTINATION :
                        GriefDefenderPlugin.getInstance().getLogger().info("Flag '" + flag + "' not currently supported! Skipping...");
                        break;
                }
            }
 
            GriefDefenderPlugin.getInstance().getLogger().info("Successfully migrated GPFlags for claim id '" + bukkitClaimId + "' to '" + claimUniqueId + "'");
        }
        GriefDefenderPlugin.getInstance().getLogger().info("Finished GPFlags data migration for world '" + world.getName() + "'."
                + " Migrated a total of " + count + " claims.");
    }

    private static void migratePlayerData(World world) {
        if (gpBukkitPlayerDataMigrated.exists()) {
            return;
        }

        final Path path = Paths.get("plugins", "GriefPreventionData", "PlayerData");
        if (!path.toFile().exists()) {
            return;
        }

        File[] files = path.toFile().listFiles();
        if (files != null) {
            GriefDefenderPlugin.getInstance().getLogger().info("Migrating " + files.length + " player data files...");
            for (int i = 0; i < files.length; i++) {
                final File file = files[i];
                GriefDefenderPlugin.getInstance().getLogger().info("Migrating playerdata " + file.getName() + "...");
                UUID uuid = null;
                try {
                    uuid = UUID.fromString(file.getName().replaceFirst("[.][^.]+$", ""));
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    continue;
                }
                List<String> lines;
                try {
                    lines = Files.readAllLines(file.toPath());
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }
                if (lines.size() < 3) {
                    continue;
                }
                try {
                    final int accruedBlocks = Integer.parseInt(lines.get(1));
                    final int bonusBlocks = Integer.parseInt(lines.get(2));
                    final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(world, uuid);
                    // Set directly in storage as subject data has not been initialized
                    playerData.setAccruedClaimBlocks(accruedBlocks);
                    playerData.setBonusClaimBlocks(bonusBlocks);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }

        try {
            Files.createFile(gpBukkitPlayerDataMigrated.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void migrateClaims(World world, File[] files, boolean parentsOnly) {
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isFile()) {
                createClaim(world, file, parentsOnly);
            }
        }
    }

    private static void createClaim(World world, File file, boolean parentsOnly) {
        int claimId;
        try {
            claimId = Integer.parseInt(file.getName().replaceFirst("[.][^.]+$", ""));
        } catch (NumberFormatException e) {
            return;
        }
        final UUID claimUniqueId = idToUUID.get(claimId);
        if (claimUniqueId == null) {
            return;
        }
        YAMLConfigurationLoader regionManager = YAMLConfigurationLoader.builder().setPath(file.toPath()).build();
        ConfigurationNode region = null;
        try {
            region = regionManager.load();
        } catch (IOException e1) {
            e1.printStackTrace();
            return;
        }
        if (region.getChildrenMap().isEmpty()) {
            GriefDefenderPlugin.getInstance().getLogger().info("Detected corrupted claim file '" + file + "'. Skipping...");
            return;
        }
        if (parentsOnly && region.getChildrenMap().get("Parent Claim ID").getInt() != -1) {
            return;
        }

        final ConfigurationNode lesserNode = region.getChildrenMap().get("Lesser Boundary Corner");
        if (lesserNode.isVirtual()) {
            return;
        }
        final String claimWorldName = getWorldName(lesserNode.getValue().toString());
        if (lesserNode != null && !world.getName().equalsIgnoreCase(claimWorldName)) {
            return;
        }
        Vector3i lesserBoundaryCorner = null;
        Vector3i greaterBoundaryCorner = null;
        List<UUID> builders = new ArrayList<>();
        List<UUID> containers = new ArrayList<>();
        List<UUID> accessors = new ArrayList<>();
        List<UUID> managers = new ArrayList<>();
        UUID parentClaimUniqueId = null;
        UUID ownerUniqueId = null;
        boolean inherit = true;
        for (Entry<Object, ? extends ConfigurationNode> mapEntry : region.getChildrenMap().entrySet()){
            Object key = mapEntry.getKey();
            ConfigurationNode value = mapEntry.getValue();
            if (key.equals("Lesser Boundary Corner")) {
                lesserBoundaryCorner = classicPosFromString(value.getString(), true);
            } else if (key.equals("Greater Boundary Corner")) {
                greaterBoundaryCorner = classicPosFromString(value.getString(), false);
            } else if (key.equals("Owner")) {
                final String owner = value.getString();
                if (owner != null && !owner.isEmpty()) {
                    try {
                        ownerUniqueId = UUID.fromString(value.getString());
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                }
            } else if (key.equals("Builders")) {
                for (String id : (List<String>) value.getValue()) {
                    if (id.equals("public")) {
                        builders.add(GriefDefenderPlugin.PUBLIC_UUID);
                    } else {
                        try {
                            final UUID uuid = UUID.fromString(id);
                            builders.add(uuid);
                        } catch (IllegalArgumentException e) {
                            
                        }
                    }
                }
            } else if (key.equals("Containers")) {
                for (String id : (List<String>) value.getValue()) {
                    if (id.equals("public")) {
                        containers.add(GriefDefenderPlugin.PUBLIC_UUID);
                    } else {
                        try {
                            final UUID uuid = UUID.fromString(id);
                            containers.add(uuid);
                        } catch (IllegalArgumentException e) {
                            
                        }
                    }
                }
            } else if (key.equals("Accessors")) {
                for (String id : (List<String>) value.getValue()) {
                    if (id.equals("public")) {
                        accessors.add(GriefDefenderPlugin.PUBLIC_UUID);
                    } else {
                        try {
                            final UUID uuid = UUID.fromString(id);
                            accessors.add(uuid);
                        } catch (IllegalArgumentException e) {
                            
                        }
                    }
                }
            } else if (key.equals("Managers")) {
                for (String id : (List<String>) value.getValue()) {
                    if (id.equals("public")) {
                        managers.add(GriefDefenderPlugin.PUBLIC_UUID);
                    } else {
                        try {
                            final UUID uuid = UUID.fromString(id);
                            managers.add(uuid);
                        } catch (IllegalArgumentException e) {
                            
                        }
                    }
                }
            } else if (key.equals("Parent Claim ID")) {
                if (value.getInt() != -1) {
                    parentClaimUniqueId = idToUUID.get(value.getInt());
                    if (parentClaimUniqueId == null) {
                        GriefDefenderPlugin.getInstance().getLogger().info("Detected corrupted subdivision claim file '" + file + "' with parent id " + value.getInt() + " that does NOT exist. Skipping...");
                        return;
                    }
                }
            } else if (key.equals("inheritNothing")) {
                inherit = !value.getBoolean();
            }
        }
        if (lesserBoundaryCorner == null) {
            return;
        }

        ClaimType type = null;
        if (parentClaimUniqueId != null) {
            type = ClaimTypes.SUBDIVISION;
        } else if (ownerUniqueId == null) {
            type = ClaimTypes.ADMIN;
        } else {
            type = ClaimTypes.BASIC;
        }
        Path claimDataFolderPath = null;
        if (parentClaimUniqueId != null) {
            final ClaimStorageData claimStorage = claimStorageMap.get(parentClaimUniqueId);
            claimDataFolderPath = claimStorage.filePath.getParent().resolve(type.getName().toLowerCase());
            if (ownerUniqueId == null) {
                ownerUniqueId = claimStorage.getConfig().getOwnerUniqueId();
            }
        } else {
            claimDataFolderPath = BaseStorage.worldConfigMap.get(world.getUID()).getPath().getParent().resolve("ClaimData").resolve(type.getName().toLowerCase());
        }
        Path claimFilePath = claimDataFolderPath.resolve(claimUniqueId.toString());
        if (!Files.exists(claimFilePath)) {
            claimFilePath.toFile().getParentFile().mkdirs();
            try {
                Files.createFile(claimFilePath);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }

        ClaimStorageData claimStorage = new ClaimStorageData(claimFilePath, world.getUID());
        claimStorageMap.put(claimUniqueId, claimStorage);
        ClaimDataConfig claimDataConfig = claimStorage.getConfig();
        if (ownerUniqueId != null) {
            claimDataConfig.setOwnerUniqueId(ownerUniqueId);
        }

        claimDataConfig.setLesserBoundaryCorner(BlockUtil.getInstance().posToString(lesserBoundaryCorner));
        claimDataConfig.setGreaterBoundaryCorner(BlockUtil.getInstance().posToString(greaterBoundaryCorner));
        claimDataConfig.setBuilders(builders);
        claimDataConfig.setContainers(containers);
        claimDataConfig.setAccessors(accessors);
        claimDataConfig.setManagers(managers);
        claimDataConfig.setInheritParent(inherit);
        claimDataConfig.setDateLastActive(Instant.now());
        claimDataConfig.setParent(parentClaimUniqueId);
        claimDataConfig.setWorldUniqueId(world.getUID());
        claimDataConfig.setType(type);
        claimDataConfig.setRequiresSave(true);
        claimStorage.save();
        GriefDefenderPlugin.getInstance().getLogger().info("Successfully migrated GriefPrevention claim file '" + file.getName() + "' to '" + claimFilePath + "'");
        count++;
    }

    private static String getWorldName(String string) {
        String[] elements = string.split(";");
        return elements[0];
    }

    private static Vector3i classicPosFromString(String string, boolean lesser) {
        // split the input string on the space
        String[] elements = string.split(";");

        String worldName = elements[0];
        String xString = elements[1];
        String zString = elements[3];

        // convert those numerical strings to integer values
        int x = Integer.parseInt(xString);
        int y = lesser ? 0 : 255;
        int z = Integer.parseInt(zString);

        return new Vector3i(x, y, z);
    }
}