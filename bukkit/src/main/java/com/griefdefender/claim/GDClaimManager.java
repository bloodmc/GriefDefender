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
package com.griefdefender.claim;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimBlockSystem;
import com.griefdefender.api.claim.ClaimManager;
import com.griefdefender.api.claim.ClaimResult;
import com.griefdefender.api.claim.ClaimResultType;
import com.griefdefender.api.claim.ClaimTypes;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.configuration.ClaimDataConfig;
import com.griefdefender.configuration.ClaimStorageData;
import com.griefdefender.configuration.GriefDefenderConfig;
import com.griefdefender.configuration.PlayerStorageData;
import com.griefdefender.event.GDRemoveClaimEvent;
import com.griefdefender.internal.tracking.PlayerIndexStorage;
import com.griefdefender.internal.tracking.chunk.GDChunk;
import com.griefdefender.internal.util.BlockUtil;
import com.griefdefender.internal.util.VecHelper;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.storage.BaseStorage;
import com.griefdefender.util.Direction;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.serializer.plain.PlainComponentSerializer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class GDClaimManager implements ClaimManager {

    private static final BaseStorage DATASTORE = GriefDefenderPlugin.getInstance().dataStore;
    private UUID worldUniqueId;
    private String worldName;
    private GriefDefenderConfig<?> activeConfig;

    // Player UUID -> player data
    private Map<UUID, GDPlayerData> playerDataList = Maps.newHashMap();
    // World claim list
    private Set<Claim> worldClaims = new HashSet<>();
    // Claim UUID -> Claim
    private Map<UUID, Claim> claimUniqueIdMap = Maps.newHashMap();
    // String -> Claim
    private Map<Long, Set<Claim>> chunksToClaimsMap = new Long2ObjectOpenHashMap<>(4096);
    // Entity Index
    public PlayerIndexStorage playerIndexStorage;
    private Map<Long, GDChunk> chunksToGpChunks = new Long2ObjectOpenHashMap<>(4096);

    private GDClaim theWildernessClaim;

    public GDClaimManager(World world) {
        this.worldUniqueId = world.getUID();
        this.worldName = world.getName();
        this.activeConfig = GriefDefenderPlugin.getActiveConfig(this.worldUniqueId);
        this.playerIndexStorage = new PlayerIndexStorage(world);
    }

    public GDPlayerData getOrCreatePlayerData(UUID playerUniqueId) {
        GDPlayerData playerData = this.getPlayerDataMap().get(playerUniqueId);
        if (playerData == null) {
            return createPlayerData(playerUniqueId);
        } else {
            return playerData;
        }
    }

    private GDPlayerData createPlayerData(UUID playerUniqueId) {
        Set<Claim> claimList = this.createPlayerClaimList(playerUniqueId);
        GDPlayerData playerData = new GDPlayerData(this.worldUniqueId, this.worldName, playerUniqueId, claimList);
        this.getPlayerDataMap().put(playerUniqueId, playerData);
        return playerData;
    }

    private Set<Claim> createPlayerClaimList(UUID playerUniqueId) {
        Set<Claim> claimList = new HashSet<>();
        if (BaseStorage.USE_GLOBAL_PLAYER_STORAGE) {
            for (World world : Bukkit.getServer().getWorlds()) {
                GDClaimManager claimmanager = DATASTORE.getClaimWorldManager(world.getUID());
                for (Claim claim : claimmanager.worldClaims) {
                    GDClaim gpClaim = (GDClaim) claim;
                    if (gpClaim.isAdminClaim()) {
                        continue;
                    }
                    if (gpClaim.parent != null) {
                       if (gpClaim.parent.getOwnerUniqueId().equals(playerUniqueId)) {
                           claimList.add(claim);
                       }
                    } else {
                        if (gpClaim.getOwnerUniqueId().equals(playerUniqueId)) {
                            claimList.add(claim);
                        }
                    }
                }
            }
        } else {
            for (Claim claim : this.worldClaims) {
                GDClaim gpClaim = (GDClaim) claim;
                if (gpClaim.isAdminClaim()) {
                    continue;
                }
                if (gpClaim.parent != null) {
                   if (gpClaim.parent.getOwnerUniqueId().equals(playerUniqueId)) {
                       claimList.add(claim);
                   }
                } else {
                    if (gpClaim.getOwnerUniqueId().equals(playerUniqueId)) {
                        claimList.add(claim);
                    }
                }
            }
        }

        return claimList;
    }

    public void removePlayer(UUID playerUniqueId) {
        this.getPlayerDataMap().remove(playerUniqueId);
    }

    public ClaimResult addClaim(Claim claim) {
        GDClaim newClaim = (GDClaim) claim;
        // ensure this new claim won't overlap any existing claims
        ClaimResult result = newClaim.checkArea(false);
        if (!result.successful()) {
            return result;
        }

        // validate world
        if (!this.worldUniqueId.equals(newClaim.getWorld().getUID())) {
            World world = Bukkit.getServer().getWorld(this.worldUniqueId);
            newClaim.setWorld(world);
        }

        // otherwise add this new claim to the data store to make it effective
        this.addClaim(newClaim, true);
        if (result.getClaims().size() > 1) {
            newClaim.migrateClaims(new ArrayList<>(result.getClaims()));
        }
        return result;
    }

    public void addClaim(Claim claimToAdd, boolean writeToStorage) {
        GDClaim claim = (GDClaim) claimToAdd;
        if (claim.parent == null && this.worldClaims.contains(claimToAdd)) {
            return;
        }

        if (writeToStorage) {
            DATASTORE.writeClaimToStorage(claim);
        }

        // We need to keep track of all claims so they can be referenced by children during server startup
        this.claimUniqueIdMap.put(claim.getUniqueId(), claim);

        if (claim.isWilderness()) {
            this.theWildernessClaim = claim;
            return;
        }

        if (claim.parent != null) {
            claim.parent.children.add(claim);
            this.worldClaims.remove(claim);
            this.deleteChunkHashes((GDClaim) claim);
            if (!claim.isAdminClaim() && (!claim.isInTown() || !claim.getTownClaim().getOwnerUniqueId().equals(claim.getOwnerUniqueId()))) {
                final GDPlayerData playerData = this.getPlayerDataMap().get(claim.getOwnerUniqueId());
                Set<Claim> playerClaims = playerData.getInternalClaims();
                if (!playerClaims.contains(claim)) {
                    playerClaims.add(claim);
                }
            }
            return;
        }

        if (!this.worldClaims.contains(claim)) {
            this.worldClaims.add(claim);
        }
        final UUID ownerId = claim.getOwnerUniqueId();
        final GDPlayerData playerData = this.getPlayerDataMap().get(ownerId);
        if (playerData != null) {
            Set<Claim> playerClaims = playerData.getInternalClaims();
            if (!playerClaims.contains(claim)) {
                playerClaims.add(claim);
            }
        } else if (!claim.isAdminClaim()) {
            this.createPlayerData(ownerId);
        }

        this.updateChunkHashes(claim);
        return;
    }

    public void updateChunkHashes(GDClaim claim) {
        this.deleteChunkHashes(claim);
        Set<Long> chunkHashes = claim.getChunkHashes(true);
        for (Long chunkHash : chunkHashes) {
            Set<Claim> claimsInChunk = this.getInternalChunksToClaimsMap().get(chunkHash);
            if (claimsInChunk == null) {
                claimsInChunk = new HashSet<Claim>();
                this.getInternalChunksToClaimsMap().put(chunkHash, claimsInChunk);
            }

            claimsInChunk.add(claim);
        }
    }

    // Used when parent claims becomes children
    public void removeClaimData(Claim claim) {
        this.worldClaims.remove(claim);
        this.deleteChunkHashes((GDClaim) claim);
    }

    @Override
    public ClaimResult deleteClaim(Claim claim, boolean deleteChildren) {
        GDRemoveClaimEvent event = new GDRemoveClaimEvent(claim);
        GriefDefender.getEventManager().post(event);
        if (event.cancelled()) {
            return new GDClaimResult(claim, ClaimResultType.CLAIM_EVENT_CANCELLED, event.getMessage().orElse(null));
        }

        return this.deleteClaimInternal(claim, deleteChildren);
    }

    public ClaimResult deleteClaimInternal(Claim claim, boolean deleteChildren) {
        final GDClaim gpClaim = (GDClaim) claim;
        Set<Claim> subClaims = claim.getChildren(false);
        for (Claim child : subClaims) {
            if (deleteChildren || (gpClaim.parent == null && child.isSubdivision())) {
                this.deleteClaimInternal(child, true);
                continue;
            }

            final GDClaim parentClaim = (GDClaim) claim;
            final GDClaim childClaim = (GDClaim) child;
            if (parentClaim.parent != null) {
                migrateChildToNewParent(parentClaim.parent, childClaim);
            } else {
                // move child to parent folder
                migrateChildToNewParent(null, childClaim);
            }
        }

        resetPlayerClaimVisuals(claim);
        // transfer bank balance to owner
        final UUID bankAccount = claim.getEconomyAccountId().orElse(null);
        if (bankAccount != null) {
            final Economy economy = GriefDefenderPlugin.getInstance().getVaultProvider().getApi();
            final GDPlayerData playerData = ((GDClaim) claim).getOwnerPlayerData();
            if (playerData != null) {
                final OfflinePlayer vaultPlayer = playerData.getSubject().getOfflinePlayer();
                if (vaultPlayer != null && !economy.hasAccount(vaultPlayer)) {
                    final double bankBalance = economy.bankBalance(claim.getUniqueId().toString()).amount;
                    economy.depositPlayer(vaultPlayer, bankBalance);
                }
            }
            economy.deleteBank(claim.getUniqueId().toString());
        }
        this.worldClaims.remove(claim);
        this.claimUniqueIdMap.remove(claim.getUniqueId());
        this.deleteChunkHashes((GDClaim) claim);
        if (gpClaim.parent != null) {
            gpClaim.parent.children.remove(claim);
        }

        return DATASTORE.deleteClaimFromStorage((GDClaim) claim);
    }

    // Migrates children to new parent
    private void migrateChildToNewParent(GDClaim parentClaim, GDClaim childClaim) {
        childClaim.parent = parentClaim;
        String fileName = childClaim.getClaimStorage().filePath.getFileName().toString();
        Path newPath = null;
        if (parentClaim == null) {
            newPath = childClaim.getClaimStorage().folderPath.getParent().getParent().resolve(childClaim.getType().getName().toLowerCase()).resolve(fileName);
        } else {
            // Only store in same claim type folder if not admin.
            // Admin claims are currently the only type that can hold children of same type within
            if (childClaim.getType().equals(parentClaim.getType()) && (!parentClaim.isAdminClaim())) {
                newPath = parentClaim.getClaimStorage().folderPath.resolve(fileName);
            } else {
                newPath = parentClaim.getClaimStorage().folderPath.resolve(childClaim.getType().getName().toLowerCase()).resolve(fileName);
            }
        }

        try {
            if (Files.notExists(newPath.getParent())) {
                Files.createDirectories(newPath.getParent());
            }
            Files.move(childClaim.getClaimStorage().filePath, newPath);
            if (childClaim.getClaimStorage().folderPath.toFile().listFiles().length == 0) {
                Files.delete(childClaim.getClaimStorage().folderPath);
            }
            childClaim.setClaimStorage(new ClaimStorageData(newPath, this.worldUniqueId, (ClaimDataConfig) childClaim.getInternalClaimData()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Make sure to update new parent in storage
        final UUID parentUniqueId = parentClaim == null ? null : parentClaim.getUniqueId();
        childClaim.getInternalClaimData().setParent(parentUniqueId);
        this.addClaim(childClaim, true);
        for (Claim child : childClaim.children) {
            migrateChildToNewParent(childClaim, (GDClaim) child);
        }
    }

    private void resetPlayerClaimVisuals(Claim claim) {
        // player may be offline so check is needed
        GDPlayerData playerData = this.getPlayerDataMap().get(claim.getOwnerUniqueId());
        if (playerData != null) {
            playerData.getInternalClaims().remove(claim);
            if (playerData.lastClaim != null) {
                playerData.lastClaim.clear();
            }
        }

        // revert visuals for all players watching this claim
        List<UUID> playersWatching = new ArrayList<>(((GDClaim) claim).playersWatching);
        for (UUID playerUniqueId : playersWatching) {
            Player player = Bukkit.getServer().getPlayer(playerUniqueId);
            if (player != null) {
                playerData = this.getOrCreatePlayerData(playerUniqueId);
                playerData.revertActiveVisual(player);
                if (playerData.lastClaim != null) {
                    playerData.lastClaim.clear();
                }
                if (GriefDefenderPlugin.getInstance().getWorldEditProvider() != null) {
                    GriefDefenderPlugin.getInstance().getWorldEditProvider().revertVisuals(player, playerData, claim.getUniqueId());
                }
            }
        }
    }

    private void deleteChunkHashes(GDClaim claim) {
        Set<Long> chunkHashes = claim.getChunkHashes(false);
        if (chunkHashes == null) {
            return;
        }

        for (Long chunkHash : chunkHashes) {
            Set<Claim> claimsInChunk = this.getInternalChunksToClaimsMap().get(chunkHash);
            if (claimsInChunk != null) {
                claimsInChunk.remove(claim);
            }
        }
    }

    @Nullable
    public Optional<Claim> getClaimByUUID(UUID claimUniqueId) {
        return Optional.ofNullable(this.claimUniqueIdMap.get(claimUniqueId));
    }

    public Set<Claim> getInternalPlayerClaims(UUID playerUniqueId) {
        final GDPlayerData playerData = this.getPlayerDataMap().get(playerUniqueId);
        if (playerData == null) {
            return new HashSet<>();
        }
        return playerData.getInternalClaims();
    }

    @Nullable
    public Set<Claim> getPlayerClaims(UUID playerUniqueId) {
        final GDPlayerData playerData = this.getPlayerDataMap().get(playerUniqueId);
        if (playerData == null) {
            return ImmutableSet.of();
        }
        return ImmutableSet.copyOf(this.getPlayerDataMap().get(playerUniqueId).getInternalClaims());
    }

    public void createWildernessClaim(World world) {
        final Vector3i lesserCorner = new Vector3i(-30000000, 0, -30000000);
        final Vector3i greaterCorner = new Vector3i(29999999, 255, 29999999);
        // Use world UUID as wilderness claim ID
        GDClaim wilderness = new GDClaim(world, lesserCorner, greaterCorner, world.getUID(), ClaimTypes.WILDERNESS, null, false);
        wilderness.setOwnerUniqueId(GriefDefenderPlugin.WORLD_USER_UUID);
        wilderness.initializeClaimData(null);
        wilderness.claimData.save();
        wilderness.claimStorage.save();
        this.theWildernessClaim = wilderness;
        this.claimUniqueIdMap.put(wilderness.getUniqueId(), wilderness);
    }

    @Override
    public GDClaim getWildernessClaim() {
        if (this.theWildernessClaim == null) {
            World world = Bukkit.getServer().getWorld(this.worldUniqueId);
            this.createWildernessClaim(world);
        }
        return this.theWildernessClaim;
    }

    @Override
    public Set<Claim> getWorldClaims() {
        return this.worldClaims;
    }

    public Map<UUID, GDPlayerData> getPlayerDataMap() {
        if (BaseStorage.USE_GLOBAL_PLAYER_STORAGE) {
            return BaseStorage.GLOBAL_PLAYER_DATA;
        }
        return this.playerDataList;
    }

    public Set<Claim> findOverlappingClaims(Claim claim) {
        Set<Claim> claimSet = new HashSet<>();
        for (Long chunkHash : claim.getChunkHashes()) {
            final Set<Claim> chunkClaims = this.chunksToClaimsMap.get(chunkHash);
            if (chunkClaims == null) {
                continue;
            }
            for (Claim chunkClaim : chunkClaims) {
                if (!chunkClaim.equals(claim) && (claim.overlaps(chunkClaim) || chunkClaim.overlaps(claim))) {
                    claimSet.add(chunkClaim);
                }
            }
        }
        return claimSet;
    }

    @Override
    public Map<Long, Set<Claim>> getChunksToClaimsMap() {
        return ImmutableMap.copyOf(this.chunksToClaimsMap);
    }

    public Map<Long, Set<Claim>> getInternalChunksToClaimsMap() {
        return this.chunksToClaimsMap;
    }

    public void save() {
        for (Claim claim : this.worldClaims) {
            GDClaim gpClaim = (GDClaim) claim;
            gpClaim.save();
        }
        this.getWildernessClaim().save();
    }

    public void unload() {
        this.playerDataList.clear();
        this.worldClaims.clear();
        this.claimUniqueIdMap.clear();
        this.chunksToClaimsMap.clear();
        if (this.theWildernessClaim != null) {
            this.theWildernessClaim.unload();
            this.theWildernessClaim = null;
        }
        this.worldUniqueId = null;
    }

    public Claim getClaimAt(Location location, boolean useBorderBlockRadius) {
        return this.getClaimAt(VecHelper.toVector3i(location), null, null, useBorderBlockRadius);
    }

    public Claim getClaimAtPlayer(Location location, GDPlayerData playerData) {
        return this.getClaimAt(VecHelper.toVector3i(location), (GDClaim) playerData.lastClaim.get(), playerData, false);
    }

    public Claim getClaimAtPlayer(Location location, GDPlayerData playerData, boolean useBorderBlockRadius) {
        return this.getClaimAt(VecHelper.toVector3i(location), (GDClaim) playerData.lastClaim.get(), playerData, useBorderBlockRadius);
    }

    @Override
    public Claim getClaimAt(Vector3i pos) {
        return this.getClaimAt(pos, null, null, false);
    }

    public Claim getClaimAt(Vector3i pos, GDClaim cachedClaim, GDPlayerData playerData, boolean useBorderBlockRadius) {
        if (cachedClaim != null && !cachedClaim.isWilderness() && cachedClaim.contains(pos, true)) {
            return cachedClaim;
        }

        Set<Claim> claimsInChunk = this.getInternalChunksToClaimsMap().get(BlockUtil.getInstance().asLong(pos.getX() >> 4, pos.getZ() >> 4));
        if (useBorderBlockRadius && (playerData != null && !playerData.bypassBorderCheck)) {
            final int borderBlockRadius = GriefDefenderPlugin.getActiveConfig(this.worldUniqueId).getConfig().claim.borderBlockRadius;
            // if borderBlockRadius > 0, check surrounding chunks
            if (borderBlockRadius > 0) {
                for (Direction direction : BlockUtil.getInstance().ORDINAL_SET) {
                    Vector3i currentPos = pos;
                    for (int i = 0; i < borderBlockRadius; i++) { // Handle depth
                        currentPos = BlockUtil.getInstance().getBlockRelative(currentPos, direction); 
                        Set<Claim> relativeClaims = this.getInternalChunksToClaimsMap().get(BlockUtil.getInstance().asLong(currentPos.getX() >> 4, currentPos.getZ() >> 4));
                        if (relativeClaims != null) {
                            if (claimsInChunk == null) {
                                claimsInChunk = new HashSet<>();
                            }
                            claimsInChunk.addAll(relativeClaims);
                        }
                    }
                }
            }
        }
        if (claimsInChunk == null) {
            return this.getWildernessClaim();
        }

        for (Claim claim : claimsInChunk) {
            GDClaim foundClaim = findClaim((GDClaim) claim, pos, playerData, useBorderBlockRadius);
            if (foundClaim != null) {
                return foundClaim;
            }
        }

        // if no claim found, return the world claim
        return this.getWildernessClaim();
    }

    private GDClaim findClaim(GDClaim claim, Vector3i pos, GDPlayerData playerData, boolean useBorderBlockRadius) {
        if (claim.contains(pos, playerData, useBorderBlockRadius)) {
            // when we find a top level claim, if the location is in one of its children,
            // return the child claim, not the top level claim
            for (Claim childClaim : claim.children) {
                GDClaim child = (GDClaim) childClaim;
                if (!child.children.isEmpty()) {
                    GDClaim innerChild = findClaim(child, pos, playerData, useBorderBlockRadius);
                    if (innerChild != null) {
                        return innerChild;
                    }
                }
                // check if child has children (Town -> Basic -> Subdivision)
                if (child.contains(pos, playerData, useBorderBlockRadius)) {
                    return child;
                }
            }
            return claim;
        }
        return null;
    }

    @Override
    public List<Claim> getClaimsByName(String name) {
        List<Claim> claimList = new ArrayList<>();
        for (Claim worldClaim : this.getWorldClaims()) {
            Component claimName = worldClaim.getName().orElse(null);
            if (claimName != null && claimName != TextComponent.empty()) {
                if (PlainComponentSerializer.INSTANCE.serialize(claimName).equalsIgnoreCase(name)) {
                    claimList.add(worldClaim);
                }
            }
            // check children
            for (Claim child : ((GDClaim) worldClaim).getChildren(true)) {
                if (child.getUniqueId().toString().equals(name)) {
                    claimList.add(child);
                }
            }
        }
        return claimList;
    }

    public void resetPlayerData() {
        // migrate playerdata to new claim block system
        final int migration3dRate = GriefDefenderPlugin.getGlobalConfig().getConfig().playerdata.migrateVolumeRate;
        final int migration2dRate = GriefDefenderPlugin.getGlobalConfig().getConfig().playerdata.migrateAreaRate;
        final boolean resetClaimBlockData = GriefDefenderPlugin.getGlobalConfig().getConfig().playerdata.resetAccruedClaimBlocks;

        if (migration3dRate <= -1 && migration2dRate <= -1 && !resetClaimBlockData) {
            return;
        }
        if (GriefDefenderPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.VOLUME && migration2dRate >= 0) {
            return;
        }
        if (GriefDefenderPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.AREA && migration3dRate >= 0) {
            return;
        }

        for (GDPlayerData playerData : this.getPlayerDataMap().values()) {
            final int accruedBlocks = playerData.getAccruedClaimBlocks();
            int newAccruedBlocks = accruedBlocks;
            // first check reset
            if (resetClaimBlockData) {
                newAccruedBlocks = playerData.getTotalClaimsCost();
                playerData.setBonusClaimBlocks(0);
            } else if (migration3dRate > -1) {
                newAccruedBlocks = accruedBlocks * migration3dRate;
            } else if (migration2dRate > -1) {
                newAccruedBlocks = accruedBlocks / migration2dRate;
            }
            if (newAccruedBlocks < 0) {
                newAccruedBlocks = 0;
            }
            final int maxAccruedBlocks = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), playerData.getSubject(), Options.MAX_ACCRUED_BLOCKS);
            if (newAccruedBlocks > maxAccruedBlocks) {
                newAccruedBlocks = maxAccruedBlocks;
            }
            playerData.setAccruedClaimBlocks(newAccruedBlocks);
        }
    }

    @Override
    public UUID getWorldId() {
        return this.worldUniqueId;
    }

    public GDChunk getChunk(Chunk chunk) {
        GDChunk gpChunk = this.chunksToGpChunks.get(getChunkKey(chunk));
        if (gpChunk == null) {
            gpChunk = new GDChunk(chunk);
            this.chunksToGpChunks.put(getChunkKey(chunk), gpChunk);
        }
        return gpChunk;
    }

    public void removeChunk(Chunk chunk) {
        this.chunksToGpChunks.remove(getChunkKey(chunk));
    }

    private long getChunkKey(Chunk chunk) {
        return (long) chunk.getX() & 0xffffffffL | ((long) chunk.getZ() & 0xffffffffL) << 32;
    }
}