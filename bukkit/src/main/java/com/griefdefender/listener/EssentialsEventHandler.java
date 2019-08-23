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

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.google.common.reflect.TypeToken;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissions;

import net.ess3.api.events.FlyStatusChangeEvent;

public class EssentialsEventHandler implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onFlyStatusChange(FlyStatusChangeEvent event) {
        final Player player = event.getAffected().getBase();
        final Location location = player.getLocation();
        final GameMode gameMode = player.getGameMode();
        if (gameMode == GameMode.CREATIVE || gameMode == GameMode.SPECTATOR) {
            return;
        }

        if (event.getValue()) {
            final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
            final GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(location, playerData, true);
            final Boolean noFly = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Boolean.class), playerData.getSubject(), Options.PLAYER_DENY_FLIGHT, claim);
            final boolean adminFly = player.hasPermission(GDPermissions.BYPASS_OPTION);
            final boolean ownerFly = claim.isBasicClaim() ? player.hasPermission(GDPermissions.USER_OPTION_PERK_OWNER_FLY_BASIC) : claim.isTown() ? player.hasPermission(GDPermissions.USER_OPTION_PERK_OWNER_FLY_TOWN) : false;
            if (player.getUniqueId().equals(claim.getOwnerUniqueId()) && ownerFly) {
                return;
            }
            if (!adminFly && noFly) {
                event.setCancelled(true);
            }
        }
    }
}
