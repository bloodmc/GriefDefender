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
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.economy.TransactionResultType;
import com.griefdefender.api.economy.TransactionType;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.claim.GDClaimManager;
import com.griefdefender.economy.GDPaymentTransaction;
import com.griefdefender.event.GDTaxClaimEvent;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.util.EconomyUtil;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.world.World;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

public class TaxApplyTask implements Runnable {

    final EconomyService economyService;

    public TaxApplyTask() {
        this.economyService = GriefDefenderPlugin.getInstance().economyService.get();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void run() {
        for (World world : Sponge.getServer().getWorlds()) {
            if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUniqueId())) {
                continue;
            }

            GDClaimManager claimManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(world.getUniqueId());
            ArrayList<Claim> claimList = (ArrayList<Claim>) new ArrayList<>(claimManager.getWorldClaims());
            if (claimList.size() == 0) {
                return;
            }
    
            Iterator<GDClaim> iterator = ((ArrayList) claimList.clone()).iterator();
            while (iterator.hasNext()) {
                GDClaim claim = iterator.next();
                final GDPlayerData playerData = claim.getOwnerPlayerData();
                if (claim.isWilderness()) {
                    continue;
                }
                if (playerData == null) {
                    continue;
                }
    
                if (!playerData.dataInitialized) {
                    continue;
                }
    
                if (claim.isAdminClaim()) {
                    // search for town
                    final Set<Claim> children = claim.getChildren(false);
                    for (Claim child : children) {
                        if (child.isTown()) {
                            handleTownTax((GDClaim) child, playerData);
                        } else if (child.isBasicClaim()) {
                            handleClaimTax((GDClaim) child, playerData, false);
                        }
                    }
                } else {
                    if (claim.isTown()) {
                        handleTownTax(claim, playerData);
                    } else if (claim.isBasicClaim()){
                        handleClaimTax(claim, playerData, false);
                    }
                }
            }
        }
    }

    private void handleClaimTax(GDClaim claim, GDPlayerData playerData, boolean inTown) {
        final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(playerData.getUniqueId());
        double taxRate = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Double.class), user, Options.TAX_RATE, claim);
        double taxOwed = claim.getEconomyData().getTaxBalance() + (claim.getClaimBlocks() * taxRate);
        try (final CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            Sponge.getCauseStackManager().pushCause(GriefDefenderPlugin.getInstance());
            GDTaxClaimEvent event = new GDTaxClaimEvent(claim, taxRate, taxOwed);
            GriefDefender.getEventManager().post(event);
            if (event.cancelled()) {
                return;
            }
            final double taxBalance = claim.getEconomyData().getTaxBalance();
            taxRate = event.getTaxRate();
            taxOwed = taxBalance + (claim.getClaimBlocks() * taxRate);
            final TransactionResult result = EconomyUtil.getInstance().withdrawTax(claim, user.getUniqueId(), taxOwed);
            if (result.getResult() != ResultType.SUCCESS) {
                final Instant localNow = Instant.now();
                Instant taxPastDueDate = claim.getEconomyData().getTaxPastDueDate();
                if (taxPastDueDate == null) {
                     claim.getEconomyData().setTaxPastDueDate(Instant.now());
                } else {
                    final int taxExpirationDays = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), user, Options.TAX_EXPIRATION, claim);
                    if (taxExpirationDays > 0) {
                        claim.getInternalClaimData().setExpired(true);
                        if (taxExpirationDays == 0) {
                            claim.getInternalClaimData().setExpired(true);
                            claim.getData().save();
                        } else if (taxPastDueDate.plus(Duration.ofDays(taxExpirationDays)).isBefore(localNow)) {
                            claim.getInternalClaimData().setExpired(true);
                            claim.getData().save();
                        }
                    }
                }
                final double totalTaxOwed = taxBalance + taxOwed;
                claim.getEconomyData().setTaxBalance(totalTaxOwed);
                claim.getEconomyData().addPaymentTransaction(new GDPaymentTransaction(TransactionType.TAX, TransactionResultType.FAIL, Instant.now(), taxOwed));
            } else {
                claim.getEconomyData().addPaymentTransaction(new GDPaymentTransaction(TransactionType.TAX, TransactionResultType.SUCCESS, Instant.now(), taxOwed));
                claim.getEconomyData().setTaxPastDueDate(null);
                claim.getEconomyData().setTaxBalance(0);
                claim.getInternalClaimData().setExpired(false);

                if (inTown) {
                    final GDClaim town = claim.getTownClaim();
                    town.getData()
                        .getEconomyData()
                        .addPaymentTransaction(new GDPaymentTransaction(TransactionType.TAX, TransactionResultType.SUCCESS, Instant.now(), taxOwed));
                    if (town.getEconomyAccountId().isPresent()) {
                        EconomyUtil.depositFunds(town.getEconomyAccountId().get(), taxOwed);
                    }
                }
                claim.getData().save();
            }
        }
    }

    private void handleTownTax(GDClaim town, GDPlayerData playerData) {
        Account townAccount = town.getEconomyAccount();
        if (townAccount == null) {
            // Virtual Accounts not supported by Economy Plugin so ignore
            return;
        }
        Set<Claim> children = town.getChildren(true);
        for (Claim child : children) {
            // resident tax
            if (child.isBasicClaim()) {
                handleClaimTax((GDClaim) child, playerData, true);
            }
        }
        if (town.getOwnerUniqueId().equals(playerData.playerID)) {
            handleClaimTax(town, playerData, false);
        }
    }
}
