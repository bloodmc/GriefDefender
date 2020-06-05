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

import com.google.common.collect.ImmutableMap;
import com.griefdefender.GDBootstrap;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.economy.TransactionResultType;
import com.griefdefender.api.economy.TransactionType;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.claim.GDClaimManager;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.economy.GDPaymentTransaction;
import com.griefdefender.event.GDRentClaimEvent;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.util.EconomyUtil;
import com.griefdefender.util.TaskUtil;

import net.kyori.text.Component;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.world.World;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public class RentDelinquentApplyTask implements Runnable {

    final EconomyService economyService;

    public RentDelinquentApplyTask() {
        this.economyService = GriefDefenderPlugin.getInstance().economyService.get();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void run() {
        for (World world : Sponge.getServer().getWorlds()) {
            GDClaimManager claimManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(world.getUniqueId());
            ArrayList<Claim> claimList = (ArrayList<Claim>) new ArrayList<>(claimManager.getWorldClaims());
            if (claimList.size() == 0) {
                return;
            }

            Iterator<GDClaim> iterator = ((ArrayList) claimList.clone()).iterator();
            while (iterator.hasNext()) {
                GDClaim claim = iterator.next();
                if (claim.isWilderness()) {
                    continue;
                }

                for (UUID uuid : claim.getEconomyData().getDelinquentRenters()) {
                    final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(uuid);
                    if (claim.getEconomyData().isUserRenting(user.getUniqueId())) {
                        // past due payments are handled in RentApplyTask
                        continue;
                    }
                    handleClaimRent(claim, user);
                }
                final Set<Claim> children = claim.getChildren(true);
                for (Claim child : children) {
                    for (UUID uuid : child.getEconomyData().getDelinquentRenters()) {
                        if (child.getEconomyData().isUserRenting(uuid)) {
                            // past due payments are handled in RentApplyTask
                            continue;
                        }
                        final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(uuid);
                        handleClaimRent((GDClaim) child, user);
                    }
                }
            }
        }
    }

    private void handleClaimRent(GDClaim claim, GDPermissionUser renter) {
        final GDPlayerData playerData = claim.getOwnerPlayerData();
        double rentBalance = claim.getEconomyData().getRentBalance(playerData.playerID);
        if (rentBalance <= 0) {
            return;
        }

        GDRentClaimEvent event = new GDRentClaimEvent(claim, renter, 0, rentBalance);
        GriefDefender.getEventManager().post(event);
        if (event.cancelled()) {
            return;
        }

        final double totalrentOwed = rentBalance;
        final Player player = renter.getOnlinePlayer();
        final Account playerAccount = this.economyService.getOrCreateAccount(renter.getUniqueId()).orElse(null);
        final Currency defaultCurrency = GriefDefenderPlugin.getInstance().economyService.get().getDefaultCurrency();
        final TransactionResult result = EconomyUtil.withdrawFunds(renter.getUniqueId(), totalrentOwed);
        if (result.getResult() != ResultType.SUCCESS) {
            claim.getEconomyData().addPaymentTransaction(new GDPaymentTransaction(TransactionType.RENT, TransactionResultType.FAIL, Instant.now(), rentBalance));
            if (player != null) {
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_RENT_PAYMENT_FAILURE, ImmutableMap.of(
                        "balance", totalrentOwed));
                GriefDefenderPlugin.sendMessage(player, message);
            }
        } else {
            claim.getEconomyData().addPaymentTransaction(new GDPaymentTransaction(TransactionType.RENT, TransactionResultType.SUCCESS, Instant.now(), totalrentOwed));
            claim.getEconomyData().getDelinquentRenters().remove(renter.getUniqueId());
            claim.getEconomyData().setRentBalance(playerData.playerID, 0);
            claim.getData().save();
            if (player != null) {
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_RENT_PAYMENT_SUCCESS, ImmutableMap.of(
                        "total-funds", playerAccount.getBalance(defaultCurrency).doubleValue(),
                        "amount", rentBalance,
                        "balance", totalrentOwed,
                        "days-remaining", 5
                        ));
                GriefDefenderPlugin.sendMessage(player, message);
            }
        }
    }
}
