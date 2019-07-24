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

import me.lucko.luckperms.api.PermissionHolder;
import me.lucko.luckperms.api.context.ContextCalculator;

public abstract class ClaimContextCalculator implements ContextCalculator<PermissionHolder> {

    /*@Override
    public void accumulateContexts(PermissionHolder calculable, Set<Context> accumulator) {
        if (calculable.getCommandSource().isPresent() && calculable.getCommandSource().get() instanceof Player) {
            Player player = (Player) calculable.getCommandSource().get();
            GPPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
            if (playerData == null) {
                return;
            }
            if (playerData.ignoreActiveContexts) {
                playerData.ignoreActiveContexts = false;
                return;
            }

            GPClaim sourceClaim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(playerData, player.getLocation());
            if (sourceClaim != null) {
                if (playerData == null || playerData.canIgnoreClaim(sourceClaim)) {
                    return;
                }

                if (sourceClaim.parent != null && sourceClaim.getData().doesInheritParent()) {
                    accumulator.add(sourceClaim.parent.getContext());
                } else {
                    accumulator.add(sourceClaim.getContext());
                }
            }
        }

    }

    @Override
    public boolean matches(Context context, Subject subject) {
        if (context.equals("gd_claim")) {
            if (subject.getCommandSource().isPresent() && subject.getCommandSource().get() instanceof Player) {
                Player player = (Player) subject.getCommandSource().get();
                GPPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
                if (playerData == null) {
                    return false;
                }

                GPClaim playerClaim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(playerData, player.getLocation());
                if (playerClaim != null && playerClaim.id.equals(UUID.fromString(context.getValue()))) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public @NonNull MutableContextSet giveApplicableContext(@NonNull PermissionHolder arg0,
            @NonNull MutableContextSet arg1) {
        // TODO Auto-generated method stub
        return null;
    }*/
}
