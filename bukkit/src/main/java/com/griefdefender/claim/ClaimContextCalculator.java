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

import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;

import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;

public class ClaimContextCalculator implements ContextCalculator<Player> {

    @Override
    public void calculate(@NonNull Player player, @NonNull ContextConsumer contextSet) {
        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
        if (playerData == null) {
            return;
        }
        if (playerData.ignoreActiveContexts) {
            playerData.ignoreActiveContexts = false;
            return;
        }

        GDClaim sourceClaim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(playerData, player.getLocation());
        if (sourceClaim != null) {
            if (playerData == null || playerData.canIgnoreClaim(sourceClaim)) {
                return;
            }

            if (sourceClaim.parent != null && sourceClaim.getData().doesInheritParent()) {
                contextSet.accept(sourceClaim.parent.getContext().getKey(), sourceClaim.parent.getContext().getValue());
            } else {
                contextSet.accept(sourceClaim.getContext().getKey(), sourceClaim.getContext().getValue());
            }
            contextSet.accept("server", GriefDefenderPlugin.getInstance().getPermissionProvider().getServerName());
        }
    }
}
