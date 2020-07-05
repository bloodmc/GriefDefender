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
package com.griefdefender.listener;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GDTimings;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimResult;
import com.griefdefender.api.claim.ClaimTypes;
import com.griefdefender.api.claim.ClaimVisualTypes;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.api.economy.PaymentType;
import com.griefdefender.api.permission.flag.Flags;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.cache.EventResultCache;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.claim.GDClaimManager;
import com.griefdefender.configuration.GriefDefenderConfig;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.internal.tracking.PlayerTracker;
import com.griefdefender.internal.tracking.chunk.GDChunk;
import com.griefdefender.internal.util.NMSUtil;
import com.griefdefender.internal.util.VecHelper;
import com.griefdefender.internal.visual.GDClaimVisual;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.permission.flag.GDFlags;
import com.griefdefender.storage.BaseStorage;
import com.griefdefender.util.BlockUtil;
import com.griefdefender.util.CauseContextHelper;
import com.griefdefender.util.Direction;
import com.griefdefender.util.PlayerUtil;
import com.griefdefender.util.SignUtil;
import net.kyori.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

public class BlockEventHandler implements Listener {

    private final BaseStorage storage;

    public BlockEventHandler(BaseStorage dataStore) {
        this.storage = dataStore;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryMoveItemEvent(InventoryMoveItemEvent event) {
        if (!GDFlags.INVENTORY_ITEM_MOVE || !GriefDefenderPlugin.getGlobalConfig().getConfig().economy.rentSystem) {
            return;
        }
        final World world = event.getSource().getLocation().getWorld();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUID()) || GriefDefenderPlugin.getInstance().getVaultProvider() == null) {
            return;
        }

        final Inventory sourceInventory = event.getSource();
        final Inventory targetInventory = event.getDestination();
        final Location sourceLocation = sourceInventory.getLocation();
        final Location targetLocation = targetInventory.getLocation();
        final GDClaim sourceClaim = GriefDefenderPlugin.getInstance().dataStore.getClaimAt(sourceLocation);
        final GDClaim targetClaim = GriefDefenderPlugin.getInstance().dataStore.getClaimAt(targetLocation);
        if (sourceClaim.isWilderness() && targetClaim.isWilderness() || (GriefDefenderPlugin.getInstance().getVaultProvider() == null)) {
            return;
        }
        if (sourceClaim.getEconomyData().isRented() || targetClaim.getEconomyData().isRented()) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockFadeEvent(BlockFadeEvent event) {
        CommonBlockEventHandler.getInstance().handleBlockModify(event, event.getBlock(), event.getNewState());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockForm(BlockFormEvent event) {
        CommonBlockEventHandler.getInstance().handleBlockModify(event, event.getBlock(), event.getNewState());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockSpreadEvent(BlockSpreadEvent event) {
        CommonBlockEventHandler.getInstance().handleBlockSpread(event, event.getSource(), event.getNewState());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBurn(BlockBurnEvent event) {
        final Block fromBlock = NMSUtil.getInstance().getIgnitingBlock(event);
        final Block toBlock = event.getBlock();
        CommonBlockEventHandler.getInstance().handleBlockModify(event, fromBlock, toBlock.getState());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {
        GDCauseStackManager.getInstance().pushCause(event.getBlock());
        final World world = event.getBlock().getWorld();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUID())) {
            return;
        }

        final Block sourceBlock = event.getBlock();
        final GDPermissionUser user = CauseContextHelper.getEventUser(sourceBlock.getLocation());
        if (user == null) {
            return;
        }

        final GDClaim sourceClaim = this.storage.getClaimAt(sourceBlock.getLocation());
        for (Block block : event.getBlocks()) {
            // always check next block in direction
            final Location location = BlockUtil.getInstance().getBlockRelative(block.getLocation(), event.getDirection());
            final GDClaim targetClaim = this.storage.getClaimAt(location);
            if (targetClaim.isWilderness() || sourceClaim.getUniqueId().equals(targetClaim.getUniqueId())) {
                continue;
            }

            if (location.getBlock().isEmpty()) {
                if (handleBlockPlace(event, location, targetClaim, sourceBlock, block, user, false)) {
                    event.setCancelled(true);
                    return;
                }
            } else {
                if (handleBlockBreak(event, location, targetClaim, sourceBlock, location.getBlock(), user, false)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {
        final World world = event.getBlock().getWorld();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUID())) {
            return;
        }

        final Block sourceBlock = event.getBlock();
        final GDPermissionUser user = CauseContextHelper.getEventUser(sourceBlock.getLocation());
        if (user == null) {
            return;
        }

        final GDClaim sourceClaim = this.storage.getClaimAt(sourceBlock.getLocation());
        for (Block block : event.getBlocks()) {
            final Location location = block.getLocation();
            final GDClaim targetClaim = this.storage.getClaimAt(location);
            if (sourceClaim.getUniqueId().equals(targetClaim.getUniqueId())) {
                continue;
            }

            if (location.getBlock().isEmpty()) {
                if (handleBlockPlace(event, location, targetClaim, sourceBlock, block, user, false)) {
                    event.setCancelled(true);
                    return;
                }
            } else {
                if (handleBlockBreak(event, location, targetClaim, sourceBlock, location.getBlock(), user, false)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockDispense(BlockDispenseEvent event) {
        final Block block = event.getBlock();
        final World world = event.getBlock().getWorld();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUID())) {
            return;
        }

        final Location location = block.getLocation();
        final GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(world.getUID());
        final GDChunk gpChunk = claimWorldManager.getChunk(block.getChunk());
        final GDPermissionUser user = gpChunk.getBlockNotifier(location);
        if (user != null) {
            final BlockFace face = NMSUtil.getInstance().getFacing(block);
            final Location faceLocation = BlockUtil.getInstance().getBlockRelative(location, face);
            final GDClaim targetClaim = this.storage.getClaimAt(faceLocation);
            final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, Flags.ITEM_SPAWN, event.getBlock(), event.getItem(), user, TrustTypes.BUILDER, true);
            if (result == Tristate.FALSE) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockGrow(BlockGrowEvent event) {
        final Block block = event.getBlock();
        final World world = event.getBlock().getWorld();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUID())) {
            return;
        }

        final Location location = block.getLocation();
        final GDClaim targetClaim = this.storage.getClaimAt(location);
        if (targetClaim.isWilderness()) {
            return;
        }

        final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, Flags.BLOCK_GROW, null, event.getBlock(), (GDPermissionUser) null, TrustTypes.BUILDER, false);
        if (result == Tristate.FALSE) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onStructureGrow(StructureGrowEvent event) {
        final World world = event.getLocation().getWorld();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUID())) {
            return;
        }

        for (BlockState blockstate : event.getBlocks()) {
            final Location location = blockstate.getLocation();
            final GDClaim targetClaim = this.storage.getClaimAt(location);

            if (targetClaim.isWilderness()) {
                continue;
            }
    
            final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, Flags.BLOCK_GROW, null, blockstate, event.getPlayer(), TrustTypes.BUILDER, true);
            if (result == Tristate.FALSE) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockFromTo(BlockFromToEvent event) {
        final Block fromBlock = event.getBlock();
        final Block toBlock = event.getToBlock();
        final World world = fromBlock.getWorld();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUID())) {
            return;
        }

        final GDPermissionUser user = CauseContextHelper.getEventUser(fromBlock.getLocation(), PlayerTracker.Type.NOTIFIER);
        if (user == null) {
            return;
        }

        Location location = toBlock.getLocation();
        GDClaim targetClaim = this.storage.getClaimAt(location);
        if (targetClaim.isWilderness()) {
            return;
        }

        if (fromBlock.isLiquid()) {
            final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, Flags.LIQUID_FLOW, fromBlock, toBlock, user, TrustTypes.BUILDER, true);
            if (result == Tristate.FALSE) {
                event.setCancelled(true);
                return;
            }
        } else if (handleBlockBreak(event, location, targetClaim, event.getBlock(), event.getToBlock(), user, false)) {
            event.setCancelled(true);
        }
    }

    private boolean handleBlockBreak(BlockEvent event, Location location, GDClaim claim, Object source, Object target, GDPermissionUser user, boolean sendDenyMessage) {
        // check overrides
        final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.BLOCK_BREAK, source, target, user, TrustTypes.BUILDER, true);
        if (result == Tristate.FALSE) {
            if (sendDenyMessage && user != null) {
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_BUILD,
                        ImmutableMap.of(
                        "player", claim.getOwnerDisplayName()
                ));
                final Player player = Bukkit.getPlayer(user.getUniqueId());
                if (player != null && player.isOnline()) {
                    GriefDefenderPlugin.sendClaimDenyMessage(claim, (Player) player, message);
                }
            }

            return true;
        }

        return false;
    }

    private boolean handleBlockPlace(BlockEvent event, Location location, GDClaim claim, Object source, Object target, GDPermissionUser user, boolean sendDenyMessage) {
        // check overrides
        final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.BLOCK_PLACE, source, target, user, TrustTypes.BUILDER, true);
        if (result == Tristate.FALSE) {
            if (sendDenyMessage && user != null) {
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_BUILD,
                        ImmutableMap.of(
                        "player", claim.getOwnerDisplayName()
                ));
                final Player player = Bukkit.getPlayer(user.getUniqueId());
                if (player != null && player.isOnline()) {
                    GriefDefenderPlugin.sendClaimDenyMessage(claim, (Player) player, message);
                }
            }

            return true;
        }

        return false;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (!GDFlags.BLOCK_MODIFY) {
            return;
        }

        final World world = event.getBlock().getWorld();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUID())) {
            return;
        }

        if (event.getPlayer() != null) {
            GDCauseStackManager.getInstance().pushCause(event.getPlayer());
        }
        final Object source = event.getIgnitingBlock() != null ? event.getIgnitingBlock() : event.getIgnitingEntity();
        CommonBlockEventHandler.getInstance().handleBlockModify(event, source, event.getBlock().getState());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockDecay(LeavesDecayEvent event) {
        if (!GDFlags.LEAF_DECAY) {
            return;
        }

        final World world = event.getBlock().getWorld();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUID())) {
            return;
        }

        Location location = event.getBlock().getLocation();
        GDClaim targetClaim = this.storage.getClaimAt(location);

        // check overrides
        final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, Flags.LEAF_DECAY, event.getBlock().getWorld(), event.getBlock(), (GDPermissionUser) null);
        if (result == Tristate.FALSE) {
            event.setCancelled(true);
        }
    }

    // Handle fluids flowing into claims
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockNotify(BlockPhysicsEvent event) {
        final Block source = NMSUtil.getInstance().getSourceBlock(event);
        if (source == null) {
            return;
        }

        final Location sourceLocation = source.getLocation();
        if (sourceLocation != null && sourceLocation.equals(event.getBlock().getLocation())) {
            return;
        }

        final GDPermissionUser user = CauseContextHelper.getEventUser(sourceLocation);
        final Location location = event.getBlock().getLocation();
        if (user == null) {
            return;
        }

        final World world = event.getBlock().getWorld();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUID())) {
            return;
        }

        final GDPlayerData playerData =  GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(world, user.getUniqueId());
        final GDClaim sourceClaim = this.storage.getClaimAt(sourceLocation);
        final GDClaim targetClaim = this.storage.getClaimAt(location);
        if (sourceClaim.isWilderness() && targetClaim.isWilderness()) {
            if (playerData != null) {
                playerData.eventResultCache = new EventResultCache(targetClaim, "block-notify", Tristate.TRUE);
            }

            return;
        } else if (!sourceClaim.isWilderness() && targetClaim.isWilderness()) {
            if (playerData != null) {
                playerData.eventResultCache = new EventResultCache(targetClaim, "block-notify", Tristate.TRUE);
            }

            return;
        } // Redstone sources can end up in target
        else if (sourceClaim.getUniqueId().equals(targetClaim.getUniqueId())) {
            if (playerData != null) {
                playerData.eventResultCache = new EventResultCache(targetClaim, "block-notify", Tristate.TRUE);
            }

            return;
        } else {
            if (playerData.eventResultCache != null && playerData.eventResultCache.checkEventResultCache(targetClaim) == Tristate.TRUE) {
                return;
            }
            // Needed to handle levers notifying doors to open etc.
            if (targetClaim.isUserTrusted(user, TrustTypes.ACCESSOR)) {
                if (playerData != null) {
                    playerData.eventResultCache = new EventResultCache(targetClaim, "block-notify", Tristate.TRUE);
                }
                return;
            }
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onExplosionEvent(BlockExplodeEvent event) {
        final World world = event.getBlock().getLocation().getWorld();
        if (!GDFlags.EXPLOSION_BLOCK || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUID())) {
            return;
        }

        Block source = event.getBlock();
        GDCauseStackManager.getInstance().pushCause(source);
        if (GriefDefenderPlugin.isSourceIdBlacklisted(Flags.EXPLOSION_BLOCK.toString(), source, world.getUID())) {
            return;
        }

        final GDPermissionUser user = CauseContextHelper.getEventUser(event.getBlock().getLocation(), PlayerTracker.Type.OWNER);
        GDTimings.EXPLOSION_EVENT.startTiming();
        GDClaim targetClaim = null;
        final List<Block> filteredLocations = new ArrayList<>();
        final String sourceId = GDPermissionManager.getInstance().getPermissionIdentifier(source);
        final int cancelBlockLimit = GriefDefenderPlugin.getGlobalConfig().getConfig().claim.explosionCancelBlockLimit;
        boolean denySurfaceExplosion = GriefDefenderPlugin.getActiveConfig(world.getUID()).getConfig().claim.explosionBlockSurfaceBlacklist.contains(sourceId);
        if (!denySurfaceExplosion) {
            denySurfaceExplosion = GriefDefenderPlugin.getActiveConfig(world.getUID()).getConfig().claim.explosionBlockSurfaceBlacklist.contains("any");
        }
        for (Block block : event.blockList()) {
            final Location location = block.getLocation();
            if (location.getBlock().isEmpty()) {
                continue;
            }
            targetClaim =  GriefDefenderPlugin.getInstance().dataStore.getClaimAt(location);
            if (denySurfaceExplosion && block.getWorld().getEnvironment() != Environment.NETHER && location.getBlockY() >= location.getWorld().getSeaLevel()) {
                filteredLocations.add(block);
                GDPermissionManager.getInstance().processEventLog(event, location, targetClaim, Flags.EXPLOSION_BLOCK.getPermission(), source, block, user, "explosion-surface", Tristate.FALSE);
                continue;
            }
            Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, Flags.EXPLOSION_BLOCK, source, block, user, true);
            if (result == Tristate.FALSE) {
                // Avoid lagging server from large explosions.
                if (event.blockList().size() > cancelBlockLimit) {
                    event.setCancelled(true);
                    break;
                }
                filteredLocations.add(block);
            }
        }

        if (event.isCancelled()) {
            event.blockList().clear();
        } else if (!filteredLocations.isEmpty()) {
            event.blockList().removeAll(filteredLocations);
        }
        GDTimings.EXPLOSION_EVENT.stopTiming();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!GDFlags.BLOCK_BREAK) {
            return;
        }

        final Player player = event.getPlayer();
        GDCauseStackManager.getInstance().pushCause(player);
        final World world = event.getPlayer().getWorld();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUID())) {
            return;
        }

        GDTimings.BLOCK_BREAK_EVENT.startTiming();

        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.BLOCK_BREAK.getName(), event.getBlock(), world.getUID())) {
            GDTimings.BLOCK_BREAK_EVENT.stopTiming();
            return;
        }

        Location location = event.getBlock().getLocation();
        GDClaim targetClaim = this.storage.getClaimAt(location);
        if (location == null || event.getBlock().getState().getType() == Material.AIR) {
            GDTimings.BLOCK_BREAK_EVENT.stopTiming();
            return;
        }

        // check overrides
        final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, Flags.BLOCK_BREAK, player, event.getBlock(), player, TrustTypes.BUILDER, true);
        if (result == Tristate.FALSE) {
            if (!PlayerUtil.getInstance().isFakePlayer(player)) {
                Component message = GDPermissionManager.getInstance().getEventMessage();
                if (message == null) {
                    message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_BUILD,
                            ImmutableMap.of(
                            "player", targetClaim.getOwnerDisplayName()
                    ));
                }
                GriefDefenderPlugin.sendClaimDenyMessage(targetClaim, player, message);
            }
            event.setCancelled(true);
        } else {
            targetClaim.markVisualDirty = true;
        }

        GDTimings.BLOCK_BREAK_EVENT.stopTiming();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {

        final Player player = event.getPlayer();
        GDCauseStackManager.getInstance().pushCause(player);
        final Block block = event.getBlock();
        final World world = event.getPlayer().getWorld();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUID())) {
            return;
        }

        GDTimings.BLOCK_PLACE_EVENT.startTiming();
        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(world.getUID(), player.getUniqueId());
        final GriefDefenderConfig<?> activeConfig = GriefDefenderPlugin.getActiveConfig(world.getUID());

        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.BLOCK_PLACE.getName(), block, world.getUID())) {
            GDTimings.BLOCK_PLACE_EVENT.stopTiming();
            return;
        }

        final Location location = event.getBlock().getLocation();
        if (location == null) {
            GDTimings.BLOCK_PLACE_EVENT.stopTiming();
            return;
        }

        final GDClaim targetClaim = this.storage.getClaimAtPlayer(location, playerData, true);
        // check surroundings for chest in protected claim
        for (Direction direction : BlockUtil.CARDINAL_SET) {
            final Location relative = BlockUtil.getInstance().getBlockRelative(location, direction);
            final GDClaim claim = this.storage.getClaimAtPlayer(relative, playerData, true);
            if (!claim.equals(targetClaim)) {
                final Block claimBlock = relative.getBlock();
                if (claimBlock.getType() == Material.CHEST && !claim.isUserTrusted(player, TrustTypes.CONTAINER)) {
                    event.setCancelled(true);
                    GDTimings.BLOCK_PLACE_EVENT.stopTiming();
                    return;
                }
            }
        }

        if (GDFlags.BLOCK_PLACE) {
            // check overrides
            final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, Flags.BLOCK_PLACE, player, block, player, TrustTypes.BUILDER, true);
            if (result == Tristate.FALSE) {
                if (!PlayerUtil.getInstance().isFakePlayer(player)) {
                    Component message = GDPermissionManager.getInstance().getEventMessage();
                    if (message == null) {
                        message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_BUILD,
                                ImmutableMap.of(
                                "player", targetClaim.getOwnerDisplayName()
                        ));
                    }
                    GriefDefenderPlugin.sendClaimDenyMessage(targetClaim, player, message);
                }
                event.setCancelled(true);
                GDTimings.BLOCK_PLACE_EVENT.stopTiming();
                return;
            }
        }

        final Vector3i blockPos = VecHelper.toVector3i(block.getLocation());

        if (targetClaim.isWilderness() && activeConfig.getConfig().claim.autoChestClaimBlockRadius > -1) {
            if (block.getType() != Material.CHEST) {
                GDTimings.BLOCK_PLACE_EVENT.stopTiming();
                return;
            }

            final int minClaimLevel = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), player, Options.MIN_LEVEL);
            final int maxClaimLevel = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), player, Options.MAX_LEVEL);
            if (blockPos.getY() < minClaimLevel || blockPos.getY() > maxClaimLevel) {
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_CHEST_OUTSIDE_LEVEL,
                        ImmutableMap.of(
                        "min-level", minClaimLevel,
                        "max-level", maxClaimLevel));
                GriefDefenderPlugin.sendMessage(player, message);
                GDTimings.BLOCK_PLACE_EVENT.stopTiming();
                return;
            }

            int radius = activeConfig.getConfig().claim.autoChestClaimBlockRadius;

            if (playerData.getInternalClaims().size() == 0) {
                if (activeConfig.getConfig().claim.autoChestClaimBlockRadius == 0) {
                    final ClaimResult result = GriefDefender.getRegistry().createBuilder(Claim.Builder.class)
                            .bounds(blockPos, blockPos)
                            .cuboid(false)
                            .owner(player.getUniqueId())
                            .sizeRestrictions(false)
                            .type(ClaimTypes.BASIC)
                            .world(world.getUID())
                            .build();
                    if (result.successful()) {
                        final Claim claim = result.getClaim().get();
                        final GDClaimManager claimManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(world.getUID());
                        claimManager.addClaim(claim, true);
                        GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CLAIM_CHEST_CONFIRMATION);
                        GDTimings.BLOCK_PLACE_EVENT.stopTiming();
                        return;
                    }
                } else {
                    Vector3i lesserBoundary = new Vector3i(
                        blockPos.getX() - radius,
                        minClaimLevel,
                        blockPos.getZ() - radius);
                    Vector3i greaterBoundary = new Vector3i(
                        blockPos.getX() + radius,
                        maxClaimLevel,
                        blockPos.getZ() + radius);
                    while (radius >= 0) {
                        ClaimResult result = GriefDefender.getRegistry().createBuilder(Claim.Builder.class)
                                .bounds(lesserBoundary, greaterBoundary)
                                .cuboid(false)
                                .owner(player.getUniqueId())
                                .sizeRestrictions(false)
                                .type(ClaimTypes.BASIC)
                                .world(world.getUID())
                                .build();
                        if (!result.successful()) {
                            radius--;
                        } else {
                            // notify and explain to player
                            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CLAIM_AUTOMATIC_NOTIFICATION);

                            // show the player the protected area
                            GDClaim newClaim = this.storage.getClaimAt(block.getLocation());
                            GDClaimVisual visualization = new GDClaimVisual(newClaim, ClaimVisualTypes.BASIC);
                            visualization.createClaimBlockVisuals(blockPos.getY(), player.getLocation(), playerData);
                            visualization.apply(player);

                            GDTimings.BLOCK_PLACE_EVENT.stopTiming();
                            return;
                        }
                    }
                }

                if (player.hasPermission(GDPermissions.CLAIM_SHOW_TUTORIAL)) {
                    GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.TUTORIAL_CLAIM_BASIC));
                }
            }
        }

        GDTimings.BLOCK_PLACE_EVENT.stopTiming();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSignChangeEvent(SignChangeEvent event) {
        if (GriefDefenderPlugin.getInstance().getVaultProvider() == null) {
            return;
        }

        final GriefDefenderConfig<?> activeConfig = GriefDefenderPlugin.getActiveConfig(event.getBlock().getWorld().getUID());
        if (!activeConfig.getConfig().economy.rentSystem || (!activeConfig.getConfig().economy.isRentSignEnabled() && !activeConfig.getConfig().economy.isSellSignEnabled())) {
            return;
        }

        final Sign sign = (Sign) event.getBlock().getState();
        final Player player = event.getPlayer();
        final GDClaim claim = this.storage.getClaimAt(event.getBlock().getLocation());
        if (claim.isWilderness()) {
            return;
        }

        final String[] lines = event.getLines();
        final String header = lines[0];
        if (header == null || (!header.equalsIgnoreCase("gd") && !header.equalsIgnoreCase("griefdefender"))) {
            return;
        }

        final String line1 = lines[1];
        final String line2 = lines[2];
        final String line3 = lines[3];
        if (line1.equalsIgnoreCase("sell") && activeConfig.getConfig().economy.isSellSignEnabled()) {
            if (!player.hasPermission(GDPermissions.USER_SELL_SIGN)) {
                return;
            }

            // handle sell
            // check price
            Double price = null;
            try {
                price = Double.valueOf(line2);
            } catch (NumberFormatException e) {
                return;
            }


            SignUtil.setClaimForSale(claim, player, sign, price);
        } else if (line1.equalsIgnoreCase("rent") && activeConfig.getConfig().economy.isRentSignEnabled()) {
            if (!player.hasPermission(GDPermissions.USER_RENT_SIGN)) {
                return;
            }

            Double rate = null;
            try {
                rate = Double.valueOf(line2.substring(0, line2.length() - 1));
            } catch (NumberFormatException e) {
                return;
            }

            int rentMin = 0;
            int rentMax = 0;
            if (line3 != null) {
                rentMin = SignUtil.getRentMinTime(line3);
                rentMax = SignUtil.getRentMaxTime(line3);
            }

            String rentType = line2;
            final PaymentType paymentType = SignUtil.getPaymentType(rentType);
            if (paymentType == PaymentType.UNDEFINED) {
                // invalid
                return;
            }

            SignUtil.setClaimForRent(claim, player, sign, rate, rentMin, rentMax, paymentType);
        }
    }
}
