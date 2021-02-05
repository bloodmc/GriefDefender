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

import com.google.common.reflect.TypeToken;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.permission.GDPermissionManager;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.manipulator.mutable.entity.VehicleData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.math.BigDecimal;
import java.util.Optional;

public class ClaimBlockTask implements Runnable {

    public ClaimBlockTask() {
    }

    @Override
    public void run() {
        for (World world : Sponge.getServer().getWorlds()) {
            final int blockMoveThreshold = GriefDefenderPlugin.getActiveConfig(world.getProperties()).getConfig().claim.claimBlockTaskMoveThreshold;
            for (Player player : world.getPlayers()) {
                final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
                final GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(playerData, player.getLocation());
                final int accrualPerHour = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), player, Options.BLOCKS_ACCRUED_PER_HOUR, claim);
                if (accrualPerHour > 0) {
                    final Location<World> lastLocation = playerData.lastAfkCheckLocation;
                    if (!player.get(VehicleData.class).isPresent() &&
                            (lastLocation == null || lastLocation.getPosition().distanceSquared(player.getLocation().getPosition()) >= (blockMoveThreshold * blockMoveThreshold))) {
                        int accruedBlocks = playerData.getBlocksAccruedPerHour() / 12;
                        if (accruedBlocks < 0) {
                            accruedBlocks = 1;
                        }

                        if (GriefDefenderPlugin.getInstance().isEconomyModeEnabled()) {
                            final Account playerAccount = GriefDefenderPlugin.getInstance().economyService.get().getOrCreateAccount(player.getUniqueId()).orElse(null);
                            if (playerAccount == null) {
                                continue;
                            }

                            final Currency defaultCurrency = GriefDefenderPlugin.getInstance().economyService.get().getDefaultCurrency();
                            playerAccount.deposit(defaultCurrency, BigDecimal.valueOf(accruedBlocks), Sponge.getCauseStackManager().getCurrentCause());
                        } else {
                            int currentTotal = playerData.getAccruedClaimBlocks();
                            if ((currentTotal + accruedBlocks) > playerData.getMaxAccruedClaimBlocks()) {
                                playerData.setAccruedClaimBlocks(playerData.getMaxAccruedClaimBlocks());
                                playerData.lastAfkCheckLocation = player.getLocation();
                                continue;
                            }

                            playerData.setAccruedClaimBlocks(playerData.getAccruedClaimBlocks() + accruedBlocks);
                        }
                    }

                    playerData.lastAfkCheckLocation = player.getLocation();
                }
            }
        }
    }
}
