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

import com.griefdefender.GDTimings;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.claim.GDClaimManager;
import com.griefdefender.internal.tracking.chunk.GDChunk;

import java.io.IOException;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public class WorldEventHandler implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onWorldUnload(WorldUnloadEvent event) {
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(event.getWorld().getUID())) {
            return;
        }

        GriefDefenderPlugin.getInstance().dataStore.removeClaimWorldManager(event.getWorld().getUID());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onWorldSave(WorldSaveEvent event) {
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(event.getWorld().getUID())) {
            return;
        }

        GDTimings.WORLD_SAVE_EVENT.startTiming();
        GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(event.getWorld().getUID());
        if (claimWorldManager == null) {
            GDTimings.WORLD_SAVE_EVENT.stopTiming();
            return;
        }

        claimWorldManager.save();

        GDTimings.WORLD_SAVE_EVENT.stopTiming();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(event.getWorld().getUID())) {
            return;
        }

        final GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(event.getWorld().getUID());
        final GDChunk gdChunk = claimWorldManager.getChunk(event.getChunk());
        if (gdChunk != null) {
            try {
                gdChunk.loadChunkTrackingData();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChunkUnload(ChunkUnloadEvent event) {
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(event.getWorld().getUID())) {
            return;
        }

        final GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(event.getWorld().getUID());
        final GDChunk gdChunk = claimWorldManager.getChunk(event.getChunk(), false);
        if (gdChunk != null) {
            if (gdChunk.getTrackedShortPlayerPositions().size() > 0) {
                gdChunk.saveChunkTrackingData();
            }
            claimWorldManager.removeChunk(gdChunk.getChunkKey());
        }
    }
}
