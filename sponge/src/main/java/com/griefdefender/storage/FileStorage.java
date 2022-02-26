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
package com.griefdefender.storage;

import com.flowpowered.math.vector.Vector3i;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimBlockSystem;
import com.griefdefender.api.claim.ClaimResult;
import com.griefdefender.api.claim.ClaimResultType;
import com.griefdefender.api.claim.ClaimSchematic;
import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.claim.GDClaimManager;
import com.griefdefender.claim.GDClaimResult;
import com.griefdefender.claim.GDSpongeClaimSchematic;
import com.griefdefender.configuration.ClaimStorageData;
import com.griefdefender.configuration.ClaimTemplateStorage;
import com.griefdefender.configuration.GriefDefenderConfig;
import com.griefdefender.configuration.TownStorageData;
import com.griefdefender.configuration.type.ConfigBase;
import com.griefdefender.event.GDLoadClaimEvent;
import com.griefdefender.migrator.GPBukkitMigrator;
import com.griefdefender.migrator.PlayerDataMigrator;
import com.griefdefender.migrator.WorldGuardMigrator;
import com.griefdefender.util.PermissionUtil;
import org.apache.commons.io.FileUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.DataTranslators;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.world.DimensionType;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.schematic.Schematic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

public class FileStorage extends BaseStorage {

    private final static Path migrationVersionFilePath = dataLayerFolderPath.resolve("_migrationVersion");
    private final static Path worldsConfigFolderPath = dataLayerFolderPath.resolve("worlds");
    public final static Path claimDataPath = Paths.get("GriefDefenderData", "ClaimData");
    public final static Path claimTemplatePath = claimDataPath.resolve("Templates");
    private final static Map<UUID, Task> cleanupClaimTasks = new HashMap<>();
    private final Path rootConfigPath = GriefDefenderPlugin.getInstance().getConfigPath().resolve("worlds");
    public static Path rootWorldSavePath;
    private int claimLoadCount = 0;

    @Override
    public void initialize() throws Exception {
        // ensure data folders exist
        File worldsDataFolder = worldsConfigFolderPath.toFile();

        if (!worldsDataFolder.exists()) {
            worldsDataFolder.mkdirs();
        }

        rootWorldSavePath = Sponge.getGame().getSavesDirectory().resolve(Sponge.getServer().getDefaultWorldName());

        super.initialize();
    }

    @Override
    public void loadClaimTemplates() {
        try {
            if (Files.exists(rootWorldSavePath.resolve(claimTemplatePath))) {
                File[] files = rootWorldSavePath.resolve(claimTemplatePath).toFile().listFiles();
                int count = 0;
                for (File file : files) {
                    ClaimTemplateStorage templateStorage = new ClaimTemplateStorage(file.toPath());
                    String templateName = templateStorage.getConfig().getTemplateName();
                    if (!templateName.isEmpty()) {
                        globalTemplates.put(templateName, templateStorage);
                        count++;
                    }
                }
                GriefDefenderPlugin.getInstance().getLogger().info(count + " total claim templates loaded.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void registerWorld(World world) {
        if (this.claimWorldManagers.get(world.getUniqueId()) != null) {
            return;
        }

        final UUID worldUniqueId = world.getUniqueId();
        DimensionType dimType = world.getProperties().getDimensionType();
        String[] parts = dimType.getId().split(":");
        Path dimPath = rootConfigPath.resolve(parts[0]).resolve(dimType.getName());
        if (!Files.exists(dimPath.resolve(world.getName()))) {
            try {
                Files.createDirectories(dimPath.resolve(world.getName()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        GDClaimManager claimWorldManager = new GDClaimManager(world);
        this.claimWorldManagers.put(world.getUniqueId(), claimWorldManager);
        // create/load configs
        GriefDefenderConfig<ConfigBase> dimConfig = new GriefDefenderConfig<>(ConfigBase.class, dimPath.resolve("dimension.conf"), BaseStorage.globalConfig);
        GriefDefenderConfig<ConfigBase> worldConfig = new GriefDefenderConfig<>(ConfigBase.class, dimPath.resolve(world.getName()).resolve("world.conf"), dimConfig);
        BaseStorage.dimensionConfigMap.put(worldUniqueId, dimConfig);
        BaseStorage.worldConfigMap.put(worldUniqueId, worldConfig);

        if (GriefDefenderPlugin.getGlobalConfig().getConfig().migrator.gpBukkitMigrator) {
            final File migrateFile = dimPath.resolve(world.getName()).resolve("_bukkitMigrated").toFile();
            if (!migrateFile.exists()) {
                try {
                    final Path path = Paths.get("plugins", "GriefPreventionData", "ClaimData");
                    if (path.toFile().exists()) {
                        GPBukkitMigrator.migrate(world, path);
                        final Path bukkitMigratedFile = dimPath.resolve(world.getName()).resolve("_bukkitMigrated");
                        if (Files.notExists(bukkitMigratedFile.getParent())) {
                            Files.createDirectories(bukkitMigratedFile.getParent());
                        }
                        if (Files.notExists(bukkitMigratedFile)) {
                            Files.createFile(bukkitMigratedFile);
                        }
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (GriefDefenderPlugin.getGlobalConfig().getConfig().migrator.worldGuardMigrator) {
            final File migrateFile = dimPath.resolve(world.getName()).resolve("_wgMigrated").toFile();
            if (!migrateFile.exists()) {
                try {
                    final Path path = Paths.get("plugins", "WorldGuard", "worlds", world.getName());
                    if (path.toFile().exists()) {
                        WorldGuardMigrator.migrate(world);
                        Files.createFile(dimPath.resolve(world.getName()).resolve("_wgMigrated"));
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        Path newWorldDataPath = dimPath.resolve(world.getName());

        try {
            // Create data folders if they do not exist
            if (Files.notExists(newWorldDataPath.resolve("ClaimData"))) {
                Files.createDirectories(newWorldDataPath.resolve("ClaimData"));
            }
            if (Files.notExists(newWorldDataPath.resolve("ClaimData").resolve("wilderness"))) {
                Files.createDirectories(newWorldDataPath.resolve("ClaimData").resolve("wilderness"));
            }
            if (Files.notExists(newWorldDataPath.resolve("SchematicData"))) {
                Files.createDirectories(newWorldDataPath.resolve("SchematicData"));
            }

            GriefDefenderPlugin.getInstance().getSchematicWorldMap().put(world.getUniqueId(), newWorldDataPath.resolve("SchematicData"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void loadWorldData(World world) {
        final UUID worldUniqueId = world.getUniqueId();
        final DimensionType dimType = world.getProperties().getDimensionType();
        final String[] parts = dimType.getId().split(":");
        final Path dimPath = rootConfigPath.resolve(parts[0]).resolve(dimType.getName());
        final Path newWorldDataPath = dimPath.resolve(world.getName());
        GDClaimManager claimWorldManager = this.claimWorldManagers.get(worldUniqueId);
        if (claimWorldManager == null) {
            this.registerWorld(world);
            claimWorldManager = this.claimWorldManagers.get(worldUniqueId);
        }

        // Load wilderness claim first
        final Path wildernessFilePath = newWorldDataPath.resolve("ClaimData").resolve("wilderness").resolve(worldUniqueId.toString());
        if (Files.exists(wildernessFilePath)) {
            try {
                this.loadClaim(wildernessFilePath.toFile(), world, world.getUniqueId());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            claimWorldManager.createWildernessClaim(world);
        }

        // Load Claim Data
        try {
            File[] files = newWorldDataPath.resolve("ClaimData").toFile().listFiles();
            if (files != null && files.length > 0) {
                this.loadClaimData(files, world);
                GriefDefenderPlugin.getInstance().getLogger().info("[" + world.getName() + "] " + this.claimLoadCount + " total claims loaded.");
            }

            if (GriefDefenderPlugin.getGlobalConfig().getConfig().playerdata.useWorldPlayerData()) {
                // migrate player data
                PlayerDataMigrator.migrateWorld(world, newWorldDataPath.resolve("PlayerData"));
            }

            // If a wilderness claim was not loaded, create a new one
            if (claimWorldManager.getWildernessClaim() == null) {
                claimWorldManager.createWildernessClaim(world);
            }

            // Load schematics
            if (GriefDefenderPlugin.getInstance().getWorldEditProvider() != null && GriefDefenderPlugin.getGlobalConfig().getConfig().claim.useWorldEditSchematics) {
                GriefDefenderPlugin.getInstance().getLogger().info("Loading schematics for world " + world.getName() + "...");
                GriefDefenderPlugin.getInstance().getWorldEditProvider().loadSchematics(world);
            } else {
                GriefDefenderPlugin.getInstance().getLogger().info("Loading sponge schematics for world " + world.getName() + "...");
                this.loadSpongeSchematics(world);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.claimLoadCount = 0;
    }

    public void loadSpongeSchematics(org.spongepowered.api.world.World world) {
        GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(world.getUniqueId());
        GriefDefenderPlugin.getInstance().executor.execute(() -> {
            for (Claim claim : claimWorldManager.getWorldClaims()) {
                Path path = GriefDefenderPlugin.getInstance().getSchematicWorldMap().get(claim.getWorldUniqueId()).resolve(claim.getUniqueId().toString());
                if (!Files.exists(path)) {
                    continue;
                }

                File files[] = path.toFile().listFiles();
                for (File file : files) {
                    DataContainer schematicData = null;
                    Schematic schematic = null;
                    final String fileName = file.getName().replaceFirst("[.][^.]+$", "");
                    try {
                        schematicData = DataFormats.NBT.readFrom(new GZIPInputStream(new FileInputStream(file)));
                        schematic = DataTranslators.SCHEMATIC.translate(schematicData);
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }

                    Instant creationDate = null;
                    BasicFileAttributes attr;
                    try {
                        attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                        creationDate = attr.creationTime().toInstant();
                    } catch (IOException e) {
                        e.printStackTrace();
                        creationDate = Instant.now();
                    }

                    final GDSpongeClaimSchematic claimSchematic = new GDSpongeClaimSchematic(claim, schematic, fileName, creationDate);
                    ((GDClaim) claim).schematics.put(fileName, claimSchematic);
                }
            }
        });
    }

    public void unloadWorldData(World world) {
        final UUID worldUniqueId = world.getUniqueId();
        GDClaimManager claimWorldManager = this.getClaimWorldManager(worldUniqueId);
        for (Claim claim : claimWorldManager.getWorldClaims()) {
            ((GDClaim) claim).unload();
        }
        // Task must be cancelled before removing the claimWorldManager reference to avoid a memory leak
        Task cleanupTask = cleanupClaimTasks.get(worldUniqueId);
        if (cleanupTask != null) {
           cleanupTask.cancel();
           cleanupClaimTasks.remove(worldUniqueId);
        }

        claimWorldManager.unload();
        this.claimWorldManagers.remove(worldUniqueId);
        BaseStorage.dimensionConfigMap.remove(worldUniqueId);
        BaseStorage.worldConfigMap.remove(worldUniqueId);
    }

    void loadClaimData(File[] files, World world) throws Exception {
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isFile()) {
                this.loadClaimFile(file, world);
            }
        }
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isDirectory()) {
                this.loadClaimData(file.listFiles(), world);
            }
        }
    }

    void loadClaimFile(File file, World world) {
        if (file.isFile()) // avoids folders
        {
            // the filename is the claim ID. try to parse it
            UUID claimId;

            try {
                final String fileName = file.getName();
                // UUID's should always be 36 in length
                if (fileName.length() != 36) {
                    return;
                }

                claimId = UUID.fromString(fileName);
            } catch (Exception e) {
                GriefDefenderPlugin.getInstance().getLogger().error("Could not read claim file " + file.getAbsolutePath());
                return;
            }

            try {
               this.loadClaim(file, world, claimId);
            }

            catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("World not found")) {
                    file.delete();
                } else {
                    GriefDefenderPlugin.getInstance().getLogger().error(file.getName() + " is corrupted.");
                    e.printStackTrace();
                }
            }
        }
    }

    void loadPlayerData(World world, File[] files) throws Exception {
        final boolean resetMigration = GriefDefenderPlugin.getGlobalConfig().getConfig().playerdata.resetMigrations;
        final boolean resetClaimData = GriefDefenderPlugin.getGlobalConfig().getConfig().playerdata.resetAccruedClaimBlocks;
        final int migration2dRate = GriefDefenderPlugin.getGlobalConfig().getConfig().playerdata.migrateAreaRate;
        final int migration3dRate = GriefDefenderPlugin.getGlobalConfig().getConfig().playerdata.migrateVolumeRate;
        boolean migrate = false;
        if (resetMigration || resetClaimData || (migration2dRate > -1 && GriefDefenderPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.AREA) 
                || (migration3dRate > -1 && GriefDefenderPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.VOLUME)) {
            // load all player data if migrating
            migrate = true;
        }
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile())
            {
                UUID playerUUID;

                try {
                    final String fileName = files[i].getName();
                    // UUID's should always be 36 in length
                    if (fileName.length() != 36) {
                        return;
                    }

                    playerUUID = UUID.fromString(fileName);
                } catch (Exception e) {
                    GriefDefenderPlugin.getInstance().getLogger().error("Could not read player file " + files[i].getAbsolutePath());
                    continue;
                }

                if (!migrate && !Sponge.getServer().getPlayer(playerUUID).isPresent()) {
                    continue;
                }

                try {
                    this.getOrCreatePlayerData(world, playerUUID);
                } catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().contains("World not found")) {
                        files[i].delete();
                    } else {
                        GriefDefenderPlugin.getInstance().getLogger().error(files[i].getName() + " is corrupted.");
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public GDClaim loadClaim(File claimFile, World world, UUID claimId)
            throws Exception {
        GDClaim claim;

        final GDClaimManager claimManager = this.getClaimWorldManager(world.getUniqueId());
        if (claimManager.getWildernessClaim() != null && claimManager.getWildernessClaim().getUniqueId().equals(claimId)) {
            return null;
        }
        boolean isTown = claimFile.toPath().getParent().endsWith("town");
        boolean writeToStorage = false;
        ClaimStorageData claimStorage = null;
        if (isTown) {
            claimStorage = new TownStorageData(claimFile.toPath(), world.getUniqueId());
        } else {
            claimStorage = new ClaimStorageData(claimFile.toPath(), world.getUniqueId());
        }

        final ClaimType type = claimStorage.getConfig().getType();
        final UUID parent = claimStorage.getConfig().getParent().orElse(null);
        final String fileName = claimFile.getName();
        //final World world = Sponge.getServer().loadWorld(worldProperties).orElse(null);
        //if (world == null) {
        //    throw new Exception("World [Name: " + worldProperties.getWorldName() + "][UUID: " + worldUniqueId.toString() + "] is not loaded.");
        //}

        if (claimFile.getParentFile().getName().equalsIgnoreCase("claimdata")) {
            final Path newPath = claimStorage.filePath.getParent().resolve(type.getName().toLowerCase());
            if (Files.notExists(newPath)) {
                Files.createDirectories(newPath);
            }
            Files.move(claimStorage.filePath, newPath.resolve(fileName));
            claimStorage.filePath = newPath.resolve(fileName);
            claimStorage = new ClaimStorageData(claimStorage.filePath, world.getUniqueId());
        }

        // identify world the claim is in
        UUID claimWorldUniqueId = claimStorage.getConfig().getWorldUniqueId();
        if (!claimWorldUniqueId.equals(world.getUniqueId())) {
            GriefDefenderPlugin.getInstance().getLogger().info("Found mismatch world UUID in " + type.getName().toLowerCase() + " claim file " + claimFile + ". Expected " + world.getUniqueId() + ", found " + claimWorldUniqueId + ". Updating file with correct UUID...");
            claimStorage.getConfig().setWorldUniqueId(world.getUniqueId());
            writeToStorage = true;
        }

        // boundaries
        final boolean cuboid = claimStorage.getConfig().isCuboid();
        Vector3i lesserCorner = claimStorage.getConfig().getLesserBoundaryCornerPos();
        Vector3i greaterCorner = claimStorage.getConfig().getGreaterBoundaryCornerPos();
        if (lesserCorner == null || greaterCorner == null) {
            throw new Exception("Claim file '" + claimFile.getName() + "' has corrupted data and cannot be loaded. Skipping...");
        }

        UUID ownerID = claimStorage.getConfig().getOwnerUniqueId();
        claim = new GDClaim(world, lesserCorner, greaterCorner, claimId, claimStorage.getConfig().getType(), ownerID, cuboid);
        claim.setClaimStorage(claimStorage);
        claim.setClaimData(claimStorage.getConfig());
        GDLoadClaimEvent.Pre preEvent = new GDLoadClaimEvent.Pre(claim);
        GriefDefender.getEventManager().post(preEvent);

        // add parent claim first
        if (parent != null) {
            GDClaim parentClaim = null;
            try {
                parentClaim = (GDClaim) claimManager.getClaimByUUID(parent).orElse(null);
            } catch (Throwable t) {
                t.printStackTrace();
            }
            if (parentClaim == null) {
                throw new Exception("Unable to load claim file '" + claimFile.getAbsolutePath() + "'. Required parent claim '" + parent + "' no longer exists. Skipping...");
            }
            claim.parent = parentClaim;
        }

        claimManager.addClaim(claim, writeToStorage);
        this.claimLoadCount++;
        GDLoadClaimEvent.Post postEvent = new GDLoadClaimEvent.Post(claim);
        GriefDefender.getEventManager().post(postEvent);
        return claim;
    }

    @Override
    public ClaimResult writeClaimToStorage(GDClaim claim) {
        try {
            ClaimStorageData claimStorage = claim.getClaimStorage();
            claim.updateClaimStorageData();
            claimStorage.save();
            return new GDClaimResult(claim, ClaimResultType.SUCCESS);
        } catch (Exception e) {
            GriefDefenderPlugin.getInstance().getLogger().error(claim.getUniqueId() + " could not save properly.");
            e.printStackTrace();
        }

        return new GDClaimResult(claim, ClaimResultType.FAILURE);
    }

    @Override
    public ClaimResult deleteClaimFromStorage(GDClaim claim) {
        final GDPlayerData ownerData = claim.getOwnerPlayerData();
        try {
            if (claim.getClaimStorage().filePath.toFile().exists()) {
                Files.delete(claim.getClaimStorage().filePath);
            }
            final Path schematicPath = GriefDefenderPlugin.getInstance().getSchematicWorldMap().get(claim.getWorldUniqueId());
            if (schematicPath != null && Files.exists(schematicPath.resolve(claim.getUniqueId().toString()))) {
                if (ownerData != null && ownerData.useRestoreSchematic) {
                    final ConfigBase activeConfig = GriefDefenderPlugin.getActiveConfig(claim.getWorldUniqueId()).getConfig();
                    if (GriefDefenderPlugin.getInstance().getWorldEditProvider() != null && activeConfig.claim.claimAutoSchematicRestore) {
                        final ClaimSchematic schematic = claim.getSchematics().get("__restore__");
                        if (schematic != null) {
                            schematic.apply();
                        }
                    }
                }
                FileUtils.deleteDirectory(schematicPath.resolve(claim.getUniqueId().toString()).toFile());
            }

            PermissionUtil.getInstance().clearPermissions((GDClaim) claim);
            return new GDClaimResult(claim, ClaimResultType.SUCCESS);
        } catch (Throwable e) {
            e.printStackTrace();
            GriefDefenderPlugin.getInstance().getLogger().error("Error: Unable to delete claim file \"" + claim.getClaimStorage().filePath + "\".");
        }

        return new GDClaimResult(claim, ClaimResultType.FAILURE);
    }

    @Override
    GDPlayerData getPlayerDataFromStorage(UUID playerID) {
        return null;
    }

    @Override
    void overrideSavePlayerData(UUID playerID, GDPlayerData playerData) {
    }

}
