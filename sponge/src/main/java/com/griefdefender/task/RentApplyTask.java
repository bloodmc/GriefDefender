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
import com.google.common.reflect.TypeToken;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimSchematic;
import com.griefdefender.api.economy.TransactionResultType;
import com.griefdefender.api.economy.TransactionType;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.claim.GDClaimManager;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.economy.GDPaymentTransaction;
import com.griefdefender.event.GDRentClaimEvent;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.util.EconomyUtil;
import com.griefdefender.util.SignUtil;

import net.kyori.text.Component;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.World;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class RentApplyTask implements Runnable {

    final EconomyService economyService;

    public RentApplyTask() {
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
                if (claim.isWilderness()) {
                    continue;
                }
    
                final List<UUID> renters = new ArrayList<>(claim.getEconomyData().getRenters());
                for (UUID uuid : renters) {
                    final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(uuid);
                    handleClaimRent(claim, user);
                }
                final Set<Claim> children = claim.getChildren(true);
                for (Claim child : children) {
                    if (child.getEconomyData().isRented()) {
                        for (UUID uuid : child.getEconomyData().getRenters()) {
                            final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(uuid);
                            handleClaimRent((GDClaim) child, user);
                        }
                    }
                }
            }
        }
    }

    private void handleClaimRent(GDClaim claim, GDPermissionUser renter) {
        final GDPlayerData playerData = claim.getOwnerPlayerData();
        final GDPlayerData ownerPlayerData = claim.getOwnerPlayerData();

        double rentRate = claim.getEconomyData().getRentRate();
        final Instant localNow = Instant.now();
        Duration duration = Duration.between(localNow, claim.getEconomyData().getRentPaymentDueDate());
        if (!duration.isNegative()) {
            // Payment not due yet
            return;
        }

        double rentBalance = claim.getEconomyData().getRentBalance(renter.getUniqueId());
        double rentOwed = rentBalance + rentRate;
        GDRentClaimEvent event = new GDRentClaimEvent(claim, renter, rentRate, rentOwed);
        GriefDefender.getEventManager().post(event);
        if (event.cancelled()) {
            return;
        }

        final Account playerAccount = this.economyService.getOrCreateAccount(renter.getUniqueId()).orElse(null);
        final Currency defaultCurrency = GriefDefenderPlugin.getInstance().economyService.get().getDefaultCurrency();
        final double totalrentOwed = rentBalance + event.getRentRate();
        boolean rentRestore = false;
        if (claim.isAdminClaim()) {
            rentRestore = GriefDefenderPlugin.getGlobalConfig().getConfig().economy.rentSchematicRestoreAdmin;
        } else {
            rentRestore = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Boolean.class), ownerPlayerData.getSubject(), Options.RENT_RESTORE, claim).booleanValue();
        }
        final Player player = renter.getOnlinePlayer();
        if (playerAccount == null) {
            return;
        }

        if (totalrentOwed > 0) {
            final TransactionResult result = EconomyUtil.withdrawFunds(renter.getUniqueId(), totalrentOwed);
            if (result.getResult() != ResultType.SUCCESS) {
                Instant rentPastDue = claim.getEconomyData().getRentPastDueDate();
                if (rentPastDue == null) {
                    claim.getEconomyData().setRentPastDueDate(localNow);
                    rentPastDue = localNow;
                }
    
                final Duration pastDueDuration = Duration.between(rentPastDue, localNow);
                final int rentExpirationDays = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), ownerPlayerData.getSubject(), Options.RENT_EXPIRATION, claim).intValue();
                final int expireDaysToKeep = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), ownerPlayerData.getSubject(), Options.RENT_EXPIRATION_DAYS_KEEP, claim).intValue();
                if (pastDueDuration.toDays() > rentExpirationDays) {
                    claim.getInternalClaimData().setExpired(false);
                    final int keepDays = (int) (expireDaysToKeep - pastDueDuration.toDays());
                    if (player != null) {
                        final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_RENTED_EXPIRED, ImmutableMap.of(
                                "balance", totalrentOwed,
                                "time", keepDays
                                ));
                        GriefDefenderPlugin.sendMessage(player, message);
                    }
                    claim.getData().save();
                    if (rentRestore && rentPastDue.plus(Duration.ofDays(rentExpirationDays + expireDaysToKeep)).isBefore(localNow)) {
                        // expiration days keep is up
                        // restore schematic and remove renter rights
                        final ClaimSchematic schematic = claim.getSchematics().get("__rent__");
                        if (schematic != null) {
                            if (schematic.apply()) {
                                if (ownerPlayerData.getSubject().getOnlinePlayer() != null) {
                                    final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_RENT_END_RESTORE, ImmutableMap.of(
                                            "claim", claim.getFriendlyName()));
                                    GriefDefenderPlugin.sendMessage(ownerPlayerData.getSubject().getOnlinePlayer(), message);
                                }
                                // end rent
                                claim.getEconomyData().setForRent(false);
                                claim.getEconomyData().getDelinquentRenters().add(renter.getUniqueId());
                                claim.removeAllTrustsFromUser(renter.getUniqueId());
                                final Sign rentSign = SignUtil.getSign(claim.getWorld(), claim.getEconomyData().getRentSignPosition());
                                if (rentSign != null) {
                                    rentSign.getLocation().setBlockType(BlockTypes.AIR);
                                }
                    
                                SignUtil.resetRentData(claim);
                                if (player != null) {
                                    GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().ECONOMY_CLAIM_RENT_CANCELLED);
                                }
                                claim.getEconomyData().getRenters().clear();
                                claim.getData().save();
                                return;
                            }
                        }
                    }
                }
    
                claim.getEconomyData().setRentBalance(playerData.playerID, totalrentOwed);
                claim.getEconomyData().addPaymentTransaction(new GDPaymentTransaction(TransactionType.RENT, TransactionResultType.FAIL, Instant.now(), rentOwed));
                if (player != null) {
                    final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_RENT_PAYMENT_FAILURE, ImmutableMap.of(
                            "total-funds", playerAccount.getBalance(defaultCurrency).doubleValue(),
                            "amount", rentBalance,
                            "balance", totalrentOwed,
                            "days-remaining", 5
                            ));
                    GriefDefenderPlugin.sendMessage(player, message);
                }
            } else {
                claim.getEconomyData().addPaymentTransaction(new GDPaymentTransaction(TransactionType.RENT, TransactionResultType.SUCCESS, Instant.now(), totalrentOwed));
                claim.getEconomyData().setRentPastDueDate(null);
                claim.getEconomyData().setRentBalance(playerData.playerID, 0);
                claim.getInternalClaimData().setExpired(false);
                claim.getData().save();
                if (player != null) {
                    final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_RENT_PAYMENT_SUCCESS, ImmutableMap.of(
                            "balance", totalrentOwed));
                    GriefDefenderPlugin.sendMessage(player, message);
                }
            }
        } else {
            claim.getEconomyData().setRentPastDueDate(null);
            claim.getEconomyData().setRentBalance(playerData.playerID, totalrentOwed); // new balance
            claim.getData().save();
        }

        // check max days
        final int rentMax = claim.getEconomyData().getRentMaxTime();
        if (rentMax <= 0) {
            return;
        }

        final Instant endDate = claim.getEconomyData().getRentEndDate();
        duration = Duration.between(localNow, endDate);
        if (duration.isNegative()) {
            // end rent
            claim.getEconomyData().setForRent(false);
            claim.removeAllTrustsFromUser(renter.getUniqueId());
            final Sign rentSign = SignUtil.getSign(claim.getWorld(), claim.getEconomyData().getRentSignPosition());
            if (rentSign != null) {
                rentSign.getLocation().setBlockType(BlockTypes.AIR);
            }

            SignUtil.resetRentData(claim);
            if (renter != null && renter.getOnlinePlayer() != null) {
                GriefDefenderPlugin.sendMessage(renter.getOnlinePlayer(), MessageCache.getInstance().ECONOMY_CLAIM_RENT_CANCELLED);
            }
            if (rentRestore) {
                final ClaimSchematic schematic = claim.getSchematics().get("__rent__");
                if (schematic != null) {
                    if (schematic.apply()) {
                        if (ownerPlayerData.getSubject().getOnlinePlayer() != null) {
                            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_RENT_END_RESTORE, ImmutableMap.of(
                                    "claim", claim.getFriendlyName()));
                            GriefDefenderPlugin.sendMessage(ownerPlayerData.getSubject().getOnlinePlayer(), message);
                        }
                    }
                }
            } else {
                if (ownerPlayerData.getSubject().getOnlinePlayer() != null) {
                    final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_RENT_END, ImmutableMap.of(
                            "claim", claim.getFriendlyName()));
                    GriefDefenderPlugin.sendMessage(ownerPlayerData.getSubject().getOnlinePlayer(), message);
                }
            }
            claim.getEconomyData().getRenters().clear();
            claim.getData().save();
        } else {
            if (duration.toDays() <= GriefDefenderPlugin.getGlobalConfig().getConfig().economy.rentRestoreDayWarning && rentRestore) {
                if (player != null) {
                    final long durationSeconds = duration.getSeconds();
                    final int days = (int)TimeUnit.SECONDS.toDays(durationSeconds);        
                    final long hours = TimeUnit.SECONDS.toHours(durationSeconds) - (days *24);
                    final long minutes = TimeUnit.SECONDS.toMinutes(durationSeconds) - (TimeUnit.SECONDS.toHours(durationSeconds)* 60);
                    final long seconds = TimeUnit.SECONDS.toSeconds(durationSeconds) - (TimeUnit.SECONDS.toMinutes(durationSeconds) *60);
                    final String remainingTime = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                    final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_RENTED_TIME_WARNING, ImmutableMap.of(
                            "time", days < 1 ? 
                                    (remainingTime) : 
                                        days + " " + LegacyComponentSerializer.legacy().serialize(MessageCache.getInstance().LABEL_HOUR, '&') + "s"));
                    GriefDefenderPlugin.sendMessage(renter.getOnlinePlayer(), message);
                }
            }
        }
    }
}
