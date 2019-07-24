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
import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.api.claim.ClaimTypes;
import com.griefdefender.configuration.ClaimDataConfig;
import com.griefdefender.configuration.ClaimStorageData;
import com.griefdefender.internal.util.BlockUtil;
import com.griefdefender.storage.BaseStorage;

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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GriefPreventionMigrator {

    private final static File gpBukkitPlayerDataMigrated = BaseStorage.globalPlayerDataPath.resolve("_bukkitMigrated").toFile();
    private static final Map<Integer, UUID> idToUUID = new ConcurrentHashMap<>();
    private static final Map<UUID, ClaimStorageData> claimStorageMap = new ConcurrentHashMap<>();
    private static int count;

    public static void migrate(World world, Path gpClassicDataPath) throws FileNotFoundException, ClassNotFoundException {
        if (!GriefDefenderPlugin.getGlobalConfig().getConfig().migrator.classicMigrator) {
            return;
        }
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
                    playerData.getStorageData().getConfig().setAccruedClaimBlocks(accruedBlocks);
                    playerData.getStorageData().getConfig().setBonusClaimBlocks(bonusBlocks);
                    playerData.saveAllData();
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
        if (parentsOnly && region.getChildrenMap().get("Parent Claim ID").getInt() != -1) {
            return;
        }
        GriefDefenderPlugin.getInstance().getLogger().info("Migrating claim '" + file + "'...");
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
                final String worldName = getWorldName(value.getString());
                if (!(worldName.equals(world.getName()))) {
                    GriefDefenderPlugin.getInstance().getLogger().info("Detected different world " + worldName + ", skipping...");
                    return;
                }

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
            claimDataFolderPath = claimStorageMap.get(parentClaimUniqueId).filePath.getParent().resolve(type.getName().toLowerCase());
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