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
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.claim.GDClaimManager;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.internal.tracking.PlayerTracker;
import com.griefdefender.internal.tracking.chunk.GDChunk;
import com.griefdefender.internal.util.BlockUtil;
import com.griefdefender.internal.util.NMSUtil;
import com.griefdefender.internal.util.VecHelper;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.util.CauseContextHelper;
import com.griefdefender.util.Direction;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.UUID;

public class BlockEventTracker implements Listener {

    private static Direction[] NOTIFY_DIRECTIONS = {Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH};
    private int lastTick = -1;
    private GDChunk cacheChunk = null;

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
        final String targetBlockName = GDPermissionManager.getInstance().getPermissionIdentifier(event.getBlock());
        if (targetBlockName.equals("minecraft:observer")) {
            return;
        }

        final Block sourceBlock = NMSUtil.getInstance().getSourceBlock(event);
        final Location sourceLocation = sourceBlock != null ? sourceBlock.getLocation() : null;
        if (sourceLocation != null && sourceLocation.equals(event.getBlock().getLocation())) {
            return;
        }
        if (sourceBlock != null) {
            final String sourceBlockName = GDPermissionManager.getInstance().getPermissionIdentifier(sourceBlock);
            if (sourceBlockName.equals("minecraft:observer")) {
                return;
            }
        }

        final GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(event.getBlock().getWorld().getUID());
        final GDChunk gpChunk = claimWorldManager.getChunk(event.getBlock().getChunk());
        final GDPermissionUser user = CauseContextHelper.getEventUser(sourceLocation);
        final UUID uuid = user != null ? user.getUniqueId() : null;

        final Vector3i targetPos = VecHelper.toVector3i(event.getBlock().getLocation());
        //final Vector3i sourcePos = VecHelper.toVector3i(event.getSourceBlock().getLocation());
        //final Location targetLocation = event.getBlock().getLocation();
        if (uuid != null) {
            gpChunk.addTrackedBlockPosition(targetPos, uuid, PlayerTracker.Type.NOTIFIER);
            // Bukkit doesn't send surrounding events for performance reasons so we must handle it manually
            /*for (Direction direction : NOTIFY_DIRECTIONS) {
                final Vector3i directionPos = targetPos.add(direction.asBlockOffset());
                gpChunk.addTrackedBlockPosition(directionPos, uuid, PlayerTracker.Type.NOTIFIER);
            }*/
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteractBlockSecondary(PlayerInteractEvent event) {
        if (!event.isCancelled()) {
            final GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(event.getClickedBlock().getWorld().getUID());
            final GDChunk gpChunk = claimWorldManager.getChunk(event.getClickedBlock().getChunk());
            final Vector3i targetPos = VecHelper.toVector3i(event.getClickedBlock().getLocation());
            GDCauseStackManager.getInstance().pushCause(event.getPlayer());
            gpChunk.addTrackedBlockPosition(event.getClickedBlock(), event.getPlayer().getUniqueId(), PlayerTracker.Type.NOTIFIER);
            // We must track the position above clicked to block actions like water flow properly.
            final Vector3i blockAbovePos = VecHelper.toVector3i(BlockUtil.getInstance().getBlockRelative(event.getClickedBlock().getLocation(), BlockFace.UP));
            gpChunk.addTrackedBlockPosition(blockAbovePos, event.getPlayer().getUniqueId(), PlayerTracker.Type.NOTIFIER);
        }
    }
}
