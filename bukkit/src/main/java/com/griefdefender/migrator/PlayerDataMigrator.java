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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;

import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.configuration.PlayerStorageData;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.storage.BaseStorage;
import com.griefdefender.util.PermissionUtil;

public class PlayerDataMigrator {

    private static int count;

    public static void migrateGlobal() throws FileNotFoundException, ClassNotFoundException {
        if (!GriefDefenderPlugin.getGlobalConfig().getConfig().migrator.playerDataMigrator) {
            return;
        }
        if (BaseStorage.globalPlayerDataPath.resolve("_migrated").toFile().exists()) {
            return;
        }
        count = 0;
        GriefDefenderPlugin.getInstance().getLogger().info("Starting GriefDefender GlobalPlayerData migration...");
        migratePlayerData(null);
        try {
            Files.createFile(BaseStorage.globalPlayerDataPath.resolve("_migrated"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        GriefDefenderPlugin.getInstance().getLogger().info("Finished GriefDefender GlobalPlayerData migration. " 
                + "Migrated a total of " + count + " playerdata files.");
    }

    public static void migrateWorld(World world, Path path) throws FileNotFoundException, ClassNotFoundException {
        if (!GriefDefenderPlugin.getGlobalConfig().getConfig().migrator.playerDataMigrator) {
            return;
        }
        if (path.resolve("_migrated").toFile().exists()) {
            return;
        }
        count = 0;
        GriefDefenderPlugin.getInstance().getLogger().info("Starting GriefDefender PlayerData migration for world " + world.getName() + "...");
        migratePlayerData(world);
        try {
            Files.createFile(path.resolve("_migrated"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        GriefDefenderPlugin.getInstance().getLogger().info("Finished GriefDefender PlayerData migration for world " + world.getName() + ". "
                + "Migrated a total of " + count + " playerdata files.");
    }

    private static void migratePlayerData(World world) {
        Path path = null;
        if (world == null) {
            path = BaseStorage.globalPlayerDataPath;
        } else {
            path = BaseStorage.worldConfigMap.get(world.getUID()).getPath().getParent().resolve("PlayerData");
        }

        if (!path.toFile().exists()) {
            return;
        }
        if (path.resolve("_migrated").toFile().exists()) {
            return;
        }
        File[] files = path.toFile().listFiles();
        if (files != null) {
            GriefDefenderPlugin.getInstance().getLogger().info("Migrating " + files.length + " player data files...");
            for (int i = 0; i < files.length; i++) {
                final File file = files[i];
                if (file.getName().startsWith("_")) {
                    continue;
                }
                GriefDefenderPlugin.getInstance().getLogger().info("Migrating playerdata " + file.getName() + "...");
                UUID uuid = null;
                try {
                    uuid = UUID.fromString(file.getName().replaceFirst("[.][^.]+$", ""));
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    continue;
                }
                // ignore internal player uuid's
                if (uuid.equals(GriefDefenderPlugin.PUBLIC_UUID) || uuid.equals(GriefDefenderPlugin.ADMIN_USER_UUID) || uuid.equals(GriefDefenderPlugin.WORLD_USER_UUID)) {
                    continue;
                }
                final PlayerStorageData playerStorage = createPlayerStorageData(world, uuid);
                if (playerStorage == null) {
                    continue;
                }
                final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(uuid);
                final String accruedClaimBlocks = String.valueOf(playerStorage.getConfig().getAccruedClaimBlocks());
                final String bonusClaimBlocks = String.valueOf(playerStorage.getConfig().getBonusClaimBlocks());
                final Set<Context> contexts = new HashSet<>();
                if (world != null) {
                    contexts.add(new Context("server", PermissionUtil.getInstance().getServerName()));
                    contexts.add(new Context("world", world.getName().toLowerCase()));
                } else {
                    final String contextType = GriefDefenderPlugin.getGlobalConfig().getConfig().playerdata.contextType;
                    if (contextType.equalsIgnoreCase("world")) {
                        // ignore
                    } else if (contextType.equalsIgnoreCase("global")) {
                        contexts.add(new Context("server", "global"));
                    } else {
                        contexts.add(new Context("server", PermissionUtil.getInstance().getServerName()));
                    }
                }
                GriefDefenderPlugin.getInstance().getPermissionProvider().setOptionValue(user, Options.ACCRUED_BLOCKS.getPermission(), accruedClaimBlocks, contexts);
                GriefDefenderPlugin.getInstance().getPermissionProvider().setOptionValue(user, Options.BONUS_BLOCKS.getPermission(), bonusClaimBlocks, contexts);
                count++;
            }
        }
    }

    private static PlayerStorageData createPlayerStorageData(World world, UUID playerUniqueId) {
        Path playerFilePath = null;
        if (BaseStorage.USE_GLOBAL_PLAYER_STORAGE) {
            playerFilePath = BaseStorage.globalPlayerDataPath.resolve(playerUniqueId.toString());
        } else {
            playerFilePath = BaseStorage.worldConfigMap.get(world.getUID()).getPath().getParent().resolve("PlayerData").resolve(playerUniqueId.toString());
        }

        PlayerStorageData playerStorage = new PlayerStorageData(playerFilePath);
        return playerStorage;
    }
}
