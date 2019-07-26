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
import com.griefdefender.GDPlayerData;
import com.griefdefender.GDTimings;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimResult;
import com.griefdefender.api.claim.ClaimTypes;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.api.permission.flag.Flags;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.claim.GDClaimManager;
import com.griefdefender.configuration.GriefDefenderConfig;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.internal.tracking.chunk.GDChunk;
import com.griefdefender.internal.util.BlockUtil;
import com.griefdefender.internal.util.NMSUtil;
import com.griefdefender.internal.util.VecHelper;
import com.griefdefender.internal.visual.ClaimVisual;
import com.griefdefender.permission.GDFlags;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.storage.BaseStorage;
import com.griefdefender.util.Direction;
import com.griefdefender.visual.ClaimVisualType;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class BlockEventHandler implements Listener {

    private int lastBlockPreTick = -1;
    private boolean lastBlockPreCancelled = false;

    private final BaseStorage storage;

    public BlockEventHandler(BaseStorage dataStore) {
        this.storage = dataStore;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {
        if (lastBlockPreTick == NMSUtil.getInstance().getRunningServerTicks()) {
            // IGNORE
            event.setCancelled(lastBlockPreCancelled);
            return;
        }
        lastBlockPreTick = NMSUtil.getInstance().getRunningServerTicks();
        final World world = event.getBlock().getWorld();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUID())) {
            return;
        }

        final Object root = GDCauseStackManager.getInstance().getCurrentCause().root();
        Player player = null;
        GDPermissionUser user = null;
        if (root instanceof Player) {
            player = (Player) root;
            user = PermissionHolderCache.getInstance().getOrCreateUser(player);
        } else {
            final GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(event.getBlock().getWorld().getUID());
            final GDChunk gpChunk = claimWorldManager.getChunk(event.getBlock().getChunk());
            user = gpChunk.getBlockOwner(event.getBlock().getLocation());
            if (user == null) {
                lastBlockPreCancelled = false;
                return;
            }
        }

        GDClaim targetClaim = null;
        if (event.getBlocks().isEmpty()) {
            Location location = BlockUtil.getInstance().getBlockRelative(event.getBlock().getLocation(), event.getDirection());
            targetClaim = this.storage.getClaimAt(location, targetClaim);
            if (targetClaim.isWilderness()) {
                return;
            }

            if (handleBlockBreak(event, location, targetClaim, event.getBlock(), location.getBlock(), user, false)) {
                event.setCancelled(true);
                lastBlockPreCancelled = true;
                return;
            }
        }

        for (Block block : event.getBlocks()) {
            Location location = BlockUtil.getInstance().getBlockRelative(block.getLocation(), event.getDirection());
            targetClaim = this.storage.getClaimAt(location, targetClaim);
            if (targetClaim.isWilderness()) {
                continue;
            }

            if (handleBlockBreak(event, location, targetClaim, event.getBlock(), block, user, false)) {
                event.setCancelled(true);
                lastBlockPreCancelled = true;
                return;
            }
        }
        lastBlockPreCancelled = false;
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
        final GDPermissionUser user = gpChunk.getBlockOwner(location);
        if (user != null) {
            final BlockFace face = NMSUtil.getInstance().getFacing(block);
            final Location faceLocation = BlockUtil.getInstance().getBlockRelative(location, face);
            final GDClaim targetClaim = this.storage.getClaimAt(faceLocation);
            final ItemStack activeItem = user != null && user.getOnlinePlayer() != null ? NMSUtil.getInstance().getActiveItem(user.getOnlinePlayer()) : null;
            final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, GDPermissions.INTERACT_BLOCK_SECONDARY, activeItem, event.getBlock(), user, TrustTypes.BUILDER, true);
            if (result != Tristate.TRUE) {
                event.setCancelled(true);
            } else {
                GDCauseStackManager.getInstance().pushCause(user);
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

        final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, GDPermissions.BLOCK_GROW, null, event.getBlock(), (GDPermissionUser) null, TrustTypes.BUILDER, false);
        if (result != Tristate.TRUE) {
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
    
            final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, GDPermissions.BLOCK_GROW, null, blockstate, event.getPlayer(), TrustTypes.BUILDER, true);
            if (result != Tristate.TRUE) {
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

        final GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(fromBlock.getWorld().getUID());
        final GDChunk gpChunk = claimWorldManager.getChunk(fromBlock.getChunk());
        GDPermissionUser user = gpChunk.getBlockNotifier(fromBlock.getLocation());

        if (user == null) {
            Object root = GDCauseStackManager.getInstance().getCurrentCause().root();
            if (root != null && root instanceof Player) {
                user = PermissionHolderCache.getInstance().getOrCreateUser((Player) root);
            }

            if (user == null) {
                return;
            }
        }

        Location location = toBlock.getLocation();
        GDClaim targetClaim = this.storage.getClaimAt(location);
        if (targetClaim.isWilderness()) {
            return;
        }

        if (fromBlock.isLiquid()) {
            final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, GDPermissions.LIQUID_FLOW, fromBlock, toBlock, user, TrustTypes.BUILDER, true);
            if (result != Tristate.TRUE) {
                event.setCancelled(true);
                return;
            }
        } else if (handleBlockBreak(event, location, targetClaim, event.getBlock(), event.getToBlock(), user, false)) {
            event.setCancelled(true);
        }
    }

    private boolean handleBlockBreak(BlockEvent event, Location location, GDClaim claim, Object source, Object target, GDPermissionUser user, boolean sendDenyMessage) {
        // check overrides
        final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, claim, GDPermissions.BLOCK_BREAK, source, target, user, TrustTypes.BUILDER, true);
        if (result != Tristate.TRUE) {
            if (sendDenyMessage && user != null) {
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_BUILD,
                        ImmutableMap.of(
                        "player", claim.getOwnerName()
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

        final Object source = event.getIgnitingBlock() != null ? event.getIgnitingBlock() : event.getIgnitingEntity();
        GDPermissionUser user = null;
        if (event.getPlayer() != null) {
            user = PermissionHolderCache.getInstance().getOrCreateUser(event.getPlayer());
        } else {
            final Player player = GDCauseStackManager.getInstance().getCurrentCause().first(Player.class).orElse(null);
            if (player != null) {
                user = PermissionHolderCache.getInstance().getOrCreateUser(player);
            } else if (event.getIgnitingBlock() != null) {
                final GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(event.getBlock().getWorld().getUID());
                final GDChunk gpChunk = claimWorldManager.getChunk(event.getBlock().getChunk());
                user = gpChunk.getBlockOwner(event.getIgnitingBlock().getLocation());
            }
        }
        Location location = event.getBlock().getLocation();
        GDClaim targetClaim = this.storage.getClaimAt(location);

        // check overrides
        final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, GDPermissions.BLOCK_MODIFY, source, event.getBlock(), user, TrustTypes.BUILDER, false);
        if (result != Tristate.TRUE) {
            event.setCancelled(true);
        }
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
        final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, GDPermissions.LEAF_DECAY, event.getBlock().getWorld(), event.getBlock(), (GDPermissionUser) null);
        if (result != Tristate.TRUE) {
            event.setCancelled(true);
        }
    }

    // Handle fluids flowing into claims
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockNotify(BlockPhysicsEvent event) {
        final Block source = NMSUtil.getInstance().getSourceBlock(event);
        if (source != null && GriefDefenderPlugin.isSourceIdBlacklisted("block-notify", source, event.getBlock().getWorld().getUID())) {
            return;
        }

        final Location sourceLocation = source != null ? source.getLocation() : null;
        if (sourceLocation != null && sourceLocation.equals(event.getBlock().getLocation())) {
            return;
        }

        GDPermissionUser user = null;
        final GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(event.getBlock().getWorld().getUID());
        final GDChunk gpChunk = claimWorldManager.getChunk(event.getBlock().getChunk());
        final Object root = GDCauseStackManager.getInstance().getCurrentCause().root();
        if (root instanceof Player) {
            user = PermissionHolderCache.getInstance().getOrCreateUser(((Player) root));
        } else if (sourceLocation != null) {
            user = gpChunk.getBlockNotifier(sourceLocation);
        }
        Location location = event.getBlock().getLocation();
        if (user == null) {
            return;
        }

        final World world = event.getBlock().getWorld();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUID())) {
            return;
        }

        GDTimings.BLOCK_NOTIFY_EVENT.startTiming();
        GDPlayerData playerData =  GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(world, user.getUniqueId());
        GDClaim sourceClaim = null;
        if (source != null) {
            sourceClaim = this.storage.getClaimAt(sourceLocation, playerData.lastClaim.get());
        }
        Vector3i pos = VecHelper.toVector3i(location);
        GDClaim targetClaim = this.storage.getClaimAt(location);
        if (sourceClaim != null && sourceClaim.isWilderness() && targetClaim.isWilderness()) {
            if (playerData != null) {
                playerData.setLastInteractData(targetClaim);
            }
            GDTimings.BLOCK_NOTIFY_EVENT.stopTiming();
            return;
        } else if (sourceClaim != null && !sourceClaim.isWilderness() && targetClaim.isWilderness()) {
            if (playerData != null) {
                playerData.setLastInteractData(targetClaim);
            }
            GDTimings.BLOCK_NOTIFY_EVENT.stopTiming();
            return;
        } // Redstone sources can end up in target
        else if (sourceClaim != null && sourceClaim.getUniqueId().equals(targetClaim.getUniqueId())) {
            if (playerData != null) {
                playerData.setLastInteractData(targetClaim);
            }
            GDTimings.BLOCK_NOTIFY_EVENT.stopTiming();
            return;
        } else {
            if (playerData.checkLastInteraction(targetClaim, user)) {
                GDTimings.BLOCK_NOTIFY_EVENT.stopTiming();
                return;
            }
            // Needed to handle levers notifying doors to open etc.
            if (targetClaim.isUserTrusted(user, TrustTypes.ACCESSOR)) {
                if (playerData != null) {
                    playerData.setLastInteractData(targetClaim);
                }
                GDTimings.BLOCK_NOTIFY_EVENT.stopTiming();
                return;
            }
        }
        event.setCancelled(true);
        GDTimings.BLOCK_NOTIFY_EVENT.stopTiming();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onExplosionEvent(BlockExplodeEvent event) {
        final World world = event.getBlock().getLocation().getWorld();
        if (!GDFlags.EXPLOSION_BLOCK || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUID())) {
            return;
        }

        final Object root = GDCauseStackManager.getInstance().getCurrentCause().root();
        GDPermissionUser user = null;
        if (root instanceof Player) {
            user = PermissionHolderCache.getInstance().getOrCreateUser((Player) root);
        } else {
            final GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(event.getBlock().getWorld().getUID());
            final GDChunk gpChunk = claimWorldManager.getChunk(event.getBlock().getChunk());
            user = gpChunk.getBlockOwner(event.getBlock().getLocation());
        }

        Block source = event.getBlock();
        if (GriefDefenderPlugin.isSourceIdBlacklisted(Flags.EXPLOSION_BLOCK.toString(), source, world.getUID())) {
            return;
        }

        if (user == null) {
            // Cancel if no user found
            event.setCancelled(true);
            return;
        }

        GDTimings.EXPLOSION_EVENT.startTiming();
        GDClaim targetClaim = null;
        final List<Block> filteredLocations = new ArrayList<>();
        for (Block block : event.blockList()) {
            final Location location = block.getLocation();
            targetClaim =  GriefDefenderPlugin.getInstance().dataStore.getClaimAt(location, targetClaim);
            /*if (GDFlags.EXPLOSION_SURFACE && location.getPosition().getY() > ((net.minecraft.world.World) world).getSeaLevel()) {
                result = GPPermissionHandler.getClaimPermission(event, location, targetClaim, GPPermissions.EXPLOSION_SURFACE, source, location.getBlock(), user, true);
            } else {*/
            Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, GDPermissions.EXPLOSION_BLOCK, source, location.getBlock(), user, true);
            if (result == Tristate.FALSE) {
                // Avoid lagging server from large explosions.
                if (event.blockList().size() > 100) {
                    event.setCancelled(true);
                    break;
                }
                filteredLocations.add(block);
            }
        }
        // Workaround for SpongeForge bug
        if (event.isCancelled()) {
            event.blockList().clear();
        } else if (!filteredLocations.isEmpty()) {
            event.blockList().removeAll(filteredLocations);
        }
        GDTimings.EXPLOSION_EVENT.stopTiming();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBurn(BlockBurnEvent event) {
        
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!GDFlags.BLOCK_BREAK) {
            return;
        }

        final Player player = event.getPlayer();
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
        if (targetClaim.isWilderness()) {
            GDTimings.BLOCK_BREAK_EVENT.stopTiming();
            return;
        }
        if (location == null || event.getBlock().getState().getType() == Material.AIR) {
            GDTimings.BLOCK_BREAK_EVENT.stopTiming();
            return;
        }

        // check overrides
        final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, GDPermissions.BLOCK_BREAK, player, event.getBlock(), player, TrustTypes.BUILDER, true);
        if (result != Tristate.TRUE) {
            if (player != null) {
                Component message = GDPermissionManager.getInstance().getEventMessage();
                if (message == null) {
                    message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_BUILD,
                            ImmutableMap.of(
                            "player", targetClaim.getOwnerName()
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
        for (Direction direction : BlockUtil.getInstance().CARDINAL_SET) {
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
            final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, GDPermissions.BLOCK_PLACE, player, block, player, TrustTypes.BUILDER, true);
            if (result != Tristate.TRUE) {
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

            final int minClaimLevel = GDPermissionManager.getInstance().getGlobalInternalOptionValue(player, Options.MIN_LEVEL, playerData).intValue();
            final int maxClaimLevel = GDPermissionManager.getInstance().getGlobalInternalOptionValue(player, Options.MAX_LEVEL, playerData).intValue();
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
                    final ClaimResult result = GriefDefender.getRegistry().createClaimBuilder()
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
                        GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_CHEST_CONFIRMATION));
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
                        ClaimResult result = GriefDefender.getRegistry().createClaimBuilder()
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
                            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_AUTOMATIC_NOTIFICATION));

                            // show the player the protected area
                            GDClaim newClaim = this.storage.getClaimAt(block.getLocation());
                            ClaimVisual visualization = new ClaimVisual(newClaim, ClaimVisualType.CLAIM);
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
}
