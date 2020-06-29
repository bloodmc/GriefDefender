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

import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaimManager;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.internal.tracking.EntityTracker;
import com.griefdefender.internal.tracking.PlayerTracker;
import com.griefdefender.internal.tracking.chunk.GDChunk;
import com.griefdefender.internal.tracking.entity.GDEntity;
import com.griefdefender.internal.util.NMSUtil;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.util.BlockUtil;
import com.griefdefender.util.CauseContextHelper;
import com.griefdefender.util.SignUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.UUID;

public class BlockEventTracker implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreakMonitor(BlockBreakEvent event) {
        final GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(event.getBlock().getWorld().getUID());
        final GDChunk gpChunk = claimWorldManager.getChunk(event.getBlock().getChunk());
        gpChunk.updateBreakPosition(event.getBlock(), event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        final GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(event.getBlock().getWorld().getUID());
        final GDChunk gpChunk = claimWorldManager.getChunk(event.getBlock().getChunk());
        final GDPermissionUser notifier = gpChunk.getBlockNotifier(event.getBlock().getLocation());
        if (notifier != null) {
            gpChunk.addTrackedBlockPosition(event.getBlock(), notifier.getUniqueId(), PlayerTracker.Type.NOTIFIER);
            return;
        }
        final GDPermissionUser owner = gpChunk.getBlockOwner(event.getBlock().getLocation());
        if (owner != null) {
            gpChunk.addTrackedBlockPosition(event.getBlock(), owner.getUniqueId(), PlayerTracker.Type.OWNER);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFalling(EntitySpawnEvent event) {
        final Entity entity = event.getEntity();
        final World world = entity.getWorld();
        if (entity instanceof FallingBlock) {
            // add owner
            final Location location = entity.getLocation();
            final Block block = world.getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            final GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(location.getWorld().getUID());
            final GDChunk gdChunk = claimWorldManager.getChunk(location.getChunk());
            final UUID ownerUniqueId = gdChunk.getBlockOwnerUUID(block.getLocation());
            if (ownerUniqueId != null) {
                final GDEntity gdEntity = new GDEntity(event.getEntity().getEntityId());
                gdEntity.setOwnerUUID(ownerUniqueId);
                gdEntity.setNotifierUUID(ownerUniqueId);
                EntityTracker.addTempEntity(gdEntity);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockSpawnFalling(EntityChangeBlockEvent event) {
        final Entity entity = event.getEntity();
        if (entity instanceof FallingBlock) {
            final GDEntity gdEntity = EntityTracker.getCachedEntity(event.getEntity().getEntityId());
            if (gdEntity != null) {
                final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(gdEntity.getOwnerUUID());
                final GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(event.getBlock().getWorld().getUID());
                final GDChunk gdChunk = claimWorldManager.getChunk(event.getBlock().getChunk());
                gdChunk.addTrackedBlockPosition(event.getBlock(), user.getUniqueId(), PlayerTracker.Type.OWNER);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        //event
        final GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(event.getBlock().getWorld().getUID());
        final GDChunk gpChunk = claimWorldManager.getChunk(event.getBlock().getChunk());
        if (event.getPlayer() != null) {
            gpChunk.addTrackedBlockPosition(event.getBlock(), event.getPlayer().getUniqueId(), PlayerTracker.Type.NOTIFIER);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlaceMonitor(BlockPlaceEvent event) {
        if (!event.isCancelled()) {
            final GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(event.getBlock().getWorld().getUID());
            final GDChunk gpChunk = claimWorldManager.getChunk(event.getBlock().getChunk());
            gpChunk.addTrackedBlockPosition(event.getBlock(), event.getPlayer().getUniqueId(), PlayerTracker.Type.OWNER);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPhysicsMonitor(BlockPhysicsEvent event) {
        if (NMSUtil.getInstance().isBlockObserver(event.getBlock())) {
            return;
        }
        // Check if sign broke
        final Block block = event.getBlock();
        if (GriefDefenderPlugin.getInstance().getVaultProvider() != null && (GriefDefenderPlugin.getGlobalConfig().getConfig().economy.isRentSignEnabled() || GriefDefenderPlugin.getGlobalConfig().getConfig().economy.isSellSignEnabled())) {
            if (SignUtil.isSign(block) && block.getState().getData() instanceof org.bukkit.material.Sign) {
                final org.bukkit.material.Sign sign = (org.bukkit.material.Sign) block.getState().getData();
                final BlockFace face = sign.getAttachedFace();
                if (face != null) {
                    final Block attachedBlock = block.getRelative(face);
                    if (attachedBlock.getType() == Material.AIR && (SignUtil.isRentSign(block) || SignUtil.isSellSign(block))) {
                       final GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(event.getBlock().getWorld().getUID());
                       final Claim claim = claimWorldManager.getClaimAt(block.getLocation(), false);
                       claim.getEconomyData().setRentSignPosition(null);
                    }
                }
            }
        }

        final Block sourceBlock = NMSUtil.getInstance().getSourceBlock(event);
        final Location sourceLocation = sourceBlock != null ? sourceBlock.getLocation() : null;
        if (sourceLocation != null && sourceLocation.equals(block.getLocation())) {
            return;
        }
        if (sourceBlock != null && NMSUtil.getInstance().isBlockObserver(sourceBlock)) {
            return;
        }

        final GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(block.getWorld().getUID());
        final GDChunk gpChunk = claimWorldManager.getChunk(block.getChunk());
        final GDPermissionUser user = CauseContextHelper.getEventUser(sourceLocation);
        final UUID uuid = user != null ? user.getUniqueId() : null;

        //final Vector3i sourcePos = VecHelper.toVector3i(event.getSourceBlock().getLocation());
        //final Location targetLocation = event.getBlock().getLocation();
        if (uuid != null) {
            gpChunk.addTrackedBlockPosition(block, uuid, PlayerTracker.Type.NOTIFIER);
            // Bukkit doesn't send surrounding events for performance reasons so we must handle it manually
            /*for (Direction direction : NOTIFY_DIRECTIONS) {
                final Vector3i directionPos = targetPos.add(direction.asBlockOffset());
                gpChunk.addTrackedBlockPosition(directionPos, uuid, PlayerTracker.Type.NOTIFIER);
            }*/
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteractBlockSecondary(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }
        final GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(event.getClickedBlock().getWorld().getUID());
        final GDChunk gpChunk = claimWorldManager.getChunk(event.getClickedBlock().getChunk());
        GDCauseStackManager.getInstance().pushCause(event.getPlayer());
        gpChunk.addTrackedBlockPosition(event.getClickedBlock(), event.getPlayer().getUniqueId(), PlayerTracker.Type.NOTIFIER);
        // We must track the position above clicked to block actions like water flow properly.
        final Location aboveLocation = BlockUtil.getInstance().getBlockRelative(event.getClickedBlock().getLocation(), BlockFace.UP);
        gpChunk.addTrackedBlockPosition(aboveLocation.getBlock(), event.getPlayer().getUniqueId(), PlayerTracker.Type.NOTIFIER);
    }
}
