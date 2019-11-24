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
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.internal.visual.ClaimVisual;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.entity.living.player.Player;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class ClaimVisualApplyTask implements Runnable {

    private ClaimVisual visualization;
    private Player player;
    private GDPlayerData playerData;
    private boolean resetActive;

    public ClaimVisualApplyTask(Player player, GDPlayerData playerData, ClaimVisual visualization) {
        this(player, playerData, visualization, true);
    }

    public ClaimVisualApplyTask(Player player, GDPlayerData playerData, ClaimVisual visualization, boolean resetActive) {
        this.visualization = visualization;
        this.playerData = playerData;
        this.player = player;
        this.resetActive = resetActive;
    }

    @Override
    public void run() {
        // Only revert active visual if we are not currently creating a claim
        if (!this.playerData.visualBlocks.isEmpty() && this.playerData.lastShovelLocation == null) {
            if (this.resetActive) {
                this.playerData.revertActiveVisual(this.player);
            }
        }

        for (int i = 0; i < this.visualization.getVisualTransactions().size(); i++) {
            BlockSnapshot snapshot = this.visualization.getVisualTransactions().get(i).getFinal();
            this.player.sendBlockChange(snapshot.getPosition(), snapshot.getState());
        }

        if (this.visualization.getClaim() != null) {
            this.playerData.visualClaimId = this.visualization.getClaim().getUniqueId();
            this.visualization.getClaim().playersWatching.add(this.player.getUniqueId());
        }
        // If we still have active visuals to revert, combine with new
        if (!this.playerData.visualBlocks.isEmpty()) {
            this.playerData.visualBlocks.addAll(this.visualization.getVisualTransactions());
        } else {
            this.playerData.visualBlocks = new ArrayList<>(this.visualization.getVisualTransactions());
        }

        if (playerData.lastShovelLocation == null) {
            this.playerData.visualRevertTask = Sponge.getGame().getScheduler().createTaskBuilder().delay(1, TimeUnit.MINUTES)
                    .execute(new ClaimVisualRevertTask(this.player, this.playerData)).submit(GDBootstrap.getInstance());
        }
    }
}
