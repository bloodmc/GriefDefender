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
import com.griefdefender.internal.block.BlockSnapshot;
import com.griefdefender.internal.util.NMSUtil;
import com.griefdefender.internal.visual.ClaimVisual;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;

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
        if (this.playerData.visualBlocks != null) {
            if (this.resetActive) {
                this.playerData.revertActiveVisual(this.player);
            }
        }

        for (int i = 0; i < this.visualization.visualTransactions.size(); i++) {
            BlockSnapshot snapshot = this.visualization.visualTransactions.get(i).getFinal();
            NMSUtil.getInstance().sendBlockChange(this.player, snapshot);
        }

        if (this.visualization.getClaim() != null) {
            this.playerData.visualClaimId = this.visualization.getClaim().getUniqueId();
            this.visualization.getClaim().playersWatching.add(this.player.getUniqueId());
        }
        this.playerData.visualBlocks = new ArrayList<>(this.visualization.visualTransactions);

        if (playerData.lastShovelLocation == null) {
            this.playerData.visualRevertTask = Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(GDBootstrap.getInstance(), new ClaimVisualRevertTask(this.player, this.playerData), 1200);
        }
    }
}
