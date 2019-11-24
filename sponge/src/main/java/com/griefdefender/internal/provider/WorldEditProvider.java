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
package com.griefdefender.internal.provider;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableMap;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GDTimings;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimResult;
import com.griefdefender.api.claim.ClaimResultType;
import com.griefdefender.api.permission.option.type.CreateModeType;
import com.griefdefender.api.permission.option.type.CreateModeTypes;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.claim.GDClaimManager;
import com.griefdefender.claim.GDClaimSchematic;
import com.griefdefender.command.CommandHelper;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.internal.provider.worldedit.cui.MultiSelectionColors;
import com.griefdefender.internal.provider.worldedit.cui.event.MultiSelectionClearEvent;
import com.griefdefender.internal.provider.worldedit.cui.event.MultiSelectionColorEvent;
import com.griefdefender.internal.provider.worldedit.cui.event.MultiSelectionCuboidEvent;
import com.griefdefender.internal.provider.worldedit.cui.event.MultiSelectionGridEvent;
import com.griefdefender.internal.provider.worldedit.cui.event.MultiSelectionPointEvent;
import com.griefdefender.internal.util.BlockUtil;
import com.griefdefender.storage.FileStorage;
import com.griefdefender.util.PlayerUtil;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.world.World;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.spongeapi.TextAdapter;
import net.kyori.text.format.TextColor;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.DataTranslators;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.schematic.Schematic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

public class WorldEditProvider {

    private final WorldEdit worldEditService;
    private Map<UUID, GDActor> worldEditPlayers = new HashMap<>();
    private final Map<UUID, Path> schematicWorldMap = new HashMap<>();

    public WorldEditProvider() {
        this.worldEditService = WorldEdit.getInstance();
    }

    public WorldEdit getWorldEditService() {
        return this.worldEditService;
    }

    public LocalSession getLocalSession(String playerName) {
        return WorldEdit.getInstance().getSessionManager().findByName(playerName);
    }

    public World getWorld(String playerName) {
        final LocalSession session = getLocalSession(playerName);
        if (session == null) {
            return null;
        }

        return session.getSelectionWorld();
    }

    public World getWorld(org.spongepowered.api.world.World spongeWorld) {
        for (World world : this.worldEditService.getServer().getWorlds()) {
            if (world.getName().equals(spongeWorld.getName())) {
                return world;
            }
        }

        return null;
    }

    public RegionSelector getRegionSelector(Player player) {
        final LocalSession session = getLocalSession(player.getName());
        if (session == null) {
            return null;
        }

        World world = session.getSelectionWorld();
        if (world == null) {
            world = this.getWorld(player.getWorld());
        }
        return session.getRegionSelector(world);
    }

    public Vector createVector(Vector3i point) {
        return new Vector(point.getX(), point.getY(), point.getZ());
    }

    public GDActor getOrCreateActor(Player player) {
        if (this.worldEditPlayers.containsKey(player.getUniqueId())) {
            return this.worldEditPlayers.get(player.getUniqueId());
        }

        final GDActor actor = new GDActor(player);
        this.worldEditPlayers.put(player.getUniqueId(), actor);
        return actor;
    }

    public void removePlayer(Player player) {
        this.worldEditPlayers.remove(player.getUniqueId());
    }

    public void visualizeClaim(Claim claim, Player player, GDPlayerData playerData, boolean investigating) {
        this.visualizeClaim(claim, claim.getLesserBoundaryCorner(), claim.getGreaterBoundaryCorner(), player, playerData, investigating);
    }

    public void visualizeClaim(Claim claim, Vector3i pos1, Vector3i pos2, Player player, GDPlayerData playerData, boolean investigating) {
        // revert any current visuals if investigating
        if (investigating) {
            this.revertVisuals(player, playerData, null);
        }
        final LocalSession session = this.getLocalSession(player.getName());
        if (session == null || !session.hasCUISupport()) {
            return;
        }
        final Vector point1 = this.createVector(pos1);
        final Vector point2 = this.createVector(pos2);
        final CuboidRegionSelector regionSelector = new CuboidRegionSelector(session.getSelectionWorld(), point1, point2);
        final GDActor actor = this.getOrCreateActor(player);
        session.setRegionSelector(this.getWorld(player.getWorld()), regionSelector);
        actor.dispatchCUIEvent(new MultiSelectionCuboidEvent(claim.getUniqueId()));
        actor.dispatchCUIEvent(new MultiSelectionPointEvent(0, point1, regionSelector.getArea()));
        if (playerData.claimResizing != null) {
            actor.dispatchCUIEvent(new MultiSelectionPointEvent(1));
        } else {
            actor.dispatchCUIEvent(new MultiSelectionPointEvent(1, point2, regionSelector.getArea()));
        }

        if (investigating || playerData.lastShovelLocation == null) {
            actor.dispatchCUIEvent(new MultiSelectionColorEvent(MultiSelectionColors.RED, MultiSelectionColors.getClaimColor(claim), "", ""));
        }
        actor.dispatchCUIEvent(new MultiSelectionGridEvent(10));
    }

    public void visualizeClaims(Set<Claim> claims, Player player, GDPlayerData playerData, boolean investigating) {
        for (Claim claim : claims) {
            if (((GDClaim) claim).children.size() > 0) {
                visualizeClaims(claim.getChildren(true), player, playerData, investigating);
            }
            final LocalSession session = this.getLocalSession(player.getName());
            if (session == null || !session.hasCUISupport()) {
                return;
            }
            final Vector point1 = this.createVector(claim.getLesserBoundaryCorner());
            final Vector point2 = this.createVector(claim.getGreaterBoundaryCorner());
            final CuboidRegionSelector regionSelector = new CuboidRegionSelector(session.getSelectionWorld(), point1, point2);
            final GDActor actor = this.getOrCreateActor(player);
            //session.setRegionSelector(this.getWorld(player.getWorld()), regionSelector);
            actor.dispatchCUIEvent(new MultiSelectionCuboidEvent(claim.getUniqueId()));
            actor.dispatchCUIEvent(new MultiSelectionPointEvent(0, point1, regionSelector.getArea()));
            if (playerData.claimResizing != null) {
                actor.dispatchCUIEvent(new MultiSelectionPointEvent(1));
            } else {
                actor.dispatchCUIEvent(new MultiSelectionPointEvent(1, point2, regionSelector.getArea()));
            }
            if (investigating) {
                actor.dispatchCUIEvent(new MultiSelectionColorEvent(MultiSelectionColors.RED, MultiSelectionColors.getClaimColor(claim), "", ""));
            }
            actor.dispatchCUIEvent(new MultiSelectionGridEvent(10));
        }
    }

    public void revertVisuals(Player player, GDPlayerData playerData, UUID claimUniqueId) {
        final LocalSession session = this.getLocalSession(player.getName());
        if (session == null || !session.hasCUISupport()) {
            return;
        }
        final World world = this.getWorld(player.getWorld());
        final RegionSelector region = session.getRegionSelector(world);
        final GDActor actor = this.getOrCreateActor(player);
        region.clear();
        session.dispatchCUISelection(actor);
        if (claimUniqueId != null) {
            actor.dispatchCUIEvent(new MultiSelectionClearEvent(claimUniqueId));
        } else {
            actor.dispatchCUIEvent(new MultiSelectionClearEvent());
        }
    }

    public void stopVisualDrag(Player player) {
        final GDActor actor = this.getOrCreateActor(player);
        actor.dispatchCUIEvent(new MultiSelectionClearEvent(player.getUniqueId()));
    }

    public void sendVisualDrag(Player player, GDPlayerData playerData, Vector3i pos) {
        final LocalSession session = this.getLocalSession(player.getName());
        if (session == null || !session.hasCUISupport()) {
            return;
        }

        final Location<org.spongepowered.api.world.World> location = BlockUtil.getInstance().getTargetBlock(player, playerData, 60, true).orElse(null);
        Vector point1 = null;
        if (playerData.claimResizing != null) {
            // get opposite corner
            final GDClaim claim = playerData.claimResizing;
            final int x = playerData.lastShovelLocation.getBlockX() == claim.lesserBoundaryCorner.getX() ? claim.greaterBoundaryCorner.getX() : claim.lesserBoundaryCorner.getX();
            final int y = playerData.lastShovelLocation.getBlockY() == claim.lesserBoundaryCorner.getY() ? claim.greaterBoundaryCorner.getY() : claim.lesserBoundaryCorner.getY();
            final int z = playerData.lastShovelLocation.getBlockZ() == claim.lesserBoundaryCorner.getZ() ? claim.greaterBoundaryCorner.getZ() : claim.lesserBoundaryCorner.getZ();
            point1 = new Vector(x, y, z);
        } else {
            point1 = this.createVector(pos);
        }
        Vector point2 = null;
        if (location == null) {
            point2 = new Vector(player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ());
        } else {
            point2 = this.createVector(location.getBlockPosition());
        }

        final CuboidRegionSelector regionSelector = new CuboidRegionSelector(session.getSelectionWorld(), point1, point2);
        final GDActor actor = this.getOrCreateActor(player);
        actor.dispatchCUIEvent(new MultiSelectionCuboidEvent(player.getUniqueId()));
        actor.dispatchCUIEvent(new MultiSelectionPointEvent(0, point1, regionSelector.getArea()));
        actor.dispatchCUIEvent(new MultiSelectionPointEvent(1));
    }

    public boolean hasCUISupport(Player player) {
        return hasCUISupport(player.getName());
    }

    public boolean hasCUISupport(String name) {
        final LocalSession session = this.getLocalSession(name);
        if (session == null || !session.hasCUISupport()) {
            return false;
        }

        return true;
    }

    public void createClaim(Player player) {
        RegionSelector regionSelector = null;
        Region region = null;
        try {
            regionSelector = GriefDefenderPlugin.getInstance().worldEditProvider.getRegionSelector(player);
            region = regionSelector.getRegion();
        } catch (IncompleteRegionException e) {
            TextAdapter.sendComponent(player, TextComponent.of("Could not find a worldedit selection.", TextColor.RED));
            return;
        }

        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        final int minY = playerData.getClaimCreateMode() == CreateModeTypes.VOLUME ? region.getMinimumPoint().getBlockY() : 0;
        final int maxY = playerData.getClaimCreateMode() == CreateModeTypes.VOLUME ? region.getMaximumPoint().getBlockY() : 255;
        final Vector3i lesser = new Vector3i(region.getMinimumPoint().getBlockX(), minY, region.getMinimumPoint().getBlockZ());
        final Vector3i greater = new Vector3i(region.getMaximumPoint().getBlockX(), maxY, region.getMaximumPoint().getBlockZ());
        GDCauseStackManager.getInstance().pushCause(player);
        final ClaimResult result = GriefDefender.getRegistry().createBuilder(Claim.Builder.class)
            .bounds(lesser, greater)
            .cuboid(playerData.getClaimCreateMode() == CreateModeTypes.VOLUME)
            .owner(player.getUniqueId())
            .sizeRestrictions(true)
            .type(PlayerUtil.getInstance().getClaimTypeFromShovel(playerData.shovelMode))
            .world(player.getWorld().getUniqueId())
            .build();
        GDCauseStackManager.getInstance().popCause();
        if (result.successful()) {
            final Claim claim = result.getClaim().get();
            final GDClaimManager claimManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(player.getWorld().getUniqueId());
            claimManager.addClaim(claim, true);
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CREATE_SUCCESS, ImmutableMap.of(
                    "type", ((GDClaim) claim).getFriendlyNameType(true)));
            GriefDefenderPlugin.sendMessage(player, message);
        } else {
            if (result.getResultType() == ClaimResultType.OVERLAPPING_CLAIM) {
                GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CREATE_OVERLAP_SHORT);
                Set<Claim> claims = new HashSet<>();
                claims.add(result.getClaim().get());
                CommandHelper.showClaims(player, claims, 0, true);
                GDTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
            }
        }
    }

    public Map<UUID, Path> getSchematicWorldMap() {
        return this.schematicWorldMap;
    }

    public void loadSchematics(org.spongepowered.api.world.World world) {
        GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(world.getUniqueId());
        GriefDefenderPlugin.getInstance().executor.execute(() -> {
            for (Claim claim : claimWorldManager.getWorldClaims()) {
                Path path = this.schematicWorldMap.get(claim.getWorldUniqueId()).resolve(claim.getUniqueId().toString());
                if (!Files.exists(path)) {
                    try {
                        Files.createDirectories(path);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return;
                }
        
                File files[] = path.toFile().listFiles();
                for (File file : files) {
                    DataContainer schematicData = null;
                    Schematic schematic = null;
                    try {
                        schematicData = DataFormats.NBT.readFrom(new GZIPInputStream(new FileInputStream(file)));
                        schematic = DataTranslators.SCHEMATIC.translate(schematicData);
                    } catch (Exception e) {
                        e.printStackTrace();
                        continue;
                    }
                    GDClaimSchematic claimSchematic = new GDClaimSchematic(claim, schematic, schematic.METADATA_NAME);
                    ((GDClaim) claim).schematics.put(file.getName(), claimSchematic);
                }
            }
        });
    }
}
