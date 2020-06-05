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
package com.griefdefender.task;

import com.griefdefender.GDBootstrap;
import com.griefdefender.GDPlayerData;
import com.griefdefender.internal.visual.GDClaimVisual;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ClaimVisualApplyTask implements Runnable {

    private GDClaimVisual visualization;
    private Player player;
    private GDPlayerData playerData;
    private boolean resetActive;

    public ClaimVisualApplyTask(Player player, GDPlayerData playerData, GDClaimVisual visualization) {
        this(player, playerData, visualization, true);
    }

    public ClaimVisualApplyTask(Player player, GDPlayerData playerData, GDClaimVisual visualization, boolean resetActive) {
        this.visualization = visualization;
        this.playerData = playerData;
        this.player = player;
        this.resetActive = resetActive;
    }

    @Override
    public void run() {
        if (!this.player.isOnline()) {
            this.playerData.revertAllVisuals();
            return;
        }
        // Only revert active visual if we are not currently creating a claim
        if (!this.playerData.visualClaimBlocks.isEmpty() && this.playerData.lastShovelLocation == null) {
            if (this.resetActive) {
                this.playerData.revertAllVisuals();
            }
        }

        for (Transaction<BlockSnapshot> transaction : this.visualization.getVisualTransactions()) {
            this.playerData.queuedVisuals.add(transaction.getFinal());
        }

        if (this.visualization.getClaim() != null) {
            this.visualization.getClaim().playersWatching.add(this.player.getUniqueId());
        }

        UUID visualUniqueId = null;
        if (this.visualization.getClaim() == null) {
            visualUniqueId = UUID.randomUUID();
            playerData.tempVisualUniqueId = visualUniqueId;
        } else {
            visualUniqueId = this.visualization.getClaim().getUniqueId();
        }

        final List<Transaction<BlockSnapshot>> blockTransactions = this.playerData.visualClaimBlocks.get(visualUniqueId);
        if (blockTransactions == null) {
            this.playerData.visualClaimBlocks.put(visualUniqueId, new ArrayList<>(this.visualization.getVisualTransactions()));
        } else {
            // support multi layer visuals i.e. water
            blockTransactions.addAll(this.visualization.getVisualTransactions());
            // cancel existing task
            final Task task = this.playerData.claimVisualRevertTasks.get(visualUniqueId);
            if (task != null) {
                task.cancel();
                this.playerData.claimVisualRevertTasks.remove(visualUniqueId);
            }
        }

        if (this.playerData.lastShovelLocation == null) {
            this.playerData.claimVisualRevertTasks.put(visualUniqueId, Sponge.getGame().getScheduler().createTaskBuilder().delay(1, TimeUnit.MINUTES)
                    .execute(new ClaimVisualRevertTask(visualUniqueId, this.player, this.playerData)).submit(GDBootstrap.getInstance()));
        }
    }
}
