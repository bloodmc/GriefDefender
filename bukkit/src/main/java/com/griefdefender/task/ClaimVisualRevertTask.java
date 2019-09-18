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

import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import org.bukkit.entity.Player;

class ClaimVisualRevertTask implements Runnable {

    private Player player;
    private GDPlayerData playerData;

    public ClaimVisualRevertTask(Player player, GDPlayerData playerData) {
        this.playerData = playerData;
        this.player = player;
    }

    @Override
    public void run() {
        // don't do anything if the player's current visualization is different
        // from the one scheduled to revert
        if (this.playerData.visualBlocks.isEmpty()) {
            return;
        }

        // check for any active WECUI visuals
        if (GriefDefenderPlugin.getInstance().getWorldEditProvider() != null) {
            GriefDefenderPlugin.getInstance().getWorldEditProvider().revertVisuals(this.player, this.playerData, this.playerData.visualClaimId);
        }
        this.playerData.revertActiveVisual(this.player);
    }
}
