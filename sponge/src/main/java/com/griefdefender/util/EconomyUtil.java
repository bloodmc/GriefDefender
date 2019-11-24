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
package com.griefdefender.util;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.griefdefender.GDBootstrap;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimResult;
import com.griefdefender.api.claim.ClaimResultType;
import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.claim.GDClaimResult;
import com.griefdefender.command.CommandHelper;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.internal.provider.WorldEditProvider;
import com.griefdefender.internal.util.BlockUtil;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.text.action.GDCallbackHolder;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.format.TextColor;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.world.World;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class EconomyUtil {

    private static EconomyUtil instance;

    public static EconomyUtil getInstance() {
        return instance;
    }

    static {
        instance = new EconomyUtil();
    }

    public void economyCreateClaimConfirmation(Player player, GDPlayerData playerData, int height, Vector3i point1, Vector3i point2, ClaimType claimType, boolean cuboid, Claim parent) {
        GDClaim claim = new GDClaim(player.getWorld(), point1, point2, claimType, player.getUniqueId(), cuboid);
        claim.parent = (GDClaim) parent;
        final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(player);
        final int claimCost = BlockUtil.getInstance().getClaimBlockCost(player.getWorld(), claim.lesserBoundaryCorner, claim.greaterBoundaryCorner, claim.cuboid);
        final Double economyBlockCost = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Double.class), user, Options.ECONOMY_BLOCK_COST, claim);
        final double requiredFunds = claimCost * economyBlockCost;
        final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_BUY_CONFIRMATION,
                ImmutableMap.of("amount", String.valueOf("$" + requiredFunds)));
        final Component buyConfirmationText = TextComponent.builder()
                .append(message)
                .append(TextComponent.builder()
                    .append("\n[")
                    .append(MessageCache.getInstance().LABEL_CONFIRM.color(TextColor.GREEN))
                    .append("]\n")
                    .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(economyClaimBuyConfirmed(player, playerData, height, requiredFunds, point1, point2, claimType, cuboid, parent)))).build())
                .build();
        GriefDefenderPlugin.sendMessage(player, buyConfirmationText);
    }

    private static Consumer<CommandSource> economyClaimBuyConfirmed(Player player, GDPlayerData playerData, int height, double requiredFunds, Vector3i lesserBoundaryCorner, Vector3i greaterBoundaryCorner, ClaimType claimType, boolean cuboid, Claim parent) {
        return confirm -> {
            // try to create a new claim
            ClaimResult result = null;
            GDCauseStackManager.getInstance().pushCause(player);
            result = GriefDefenderPlugin.getInstance().dataStore.createClaim(
                    player.getWorld(),
                    lesserBoundaryCorner,
                    greaterBoundaryCorner,
                    claimType, player.getUniqueId(), cuboid);
            GDCauseStackManager.getInstance().popCause();

            GDClaim gdClaim = (GDClaim) result.getClaim().orElse(null);
            // if it didn't succeed, tell the player why
            if (!result.successful()) {
                if (result.getResultType() == ClaimResultType.OVERLAPPING_CLAIM) {
                    GDClaim overlapClaim = (GDClaim) result.getClaim().get();
                    GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CREATE_OVERLAP_SHORT);
                    Set<Claim> claims = new HashSet<>();
                    claims.add(overlapClaim);
                    CommandHelper.showClaims(player, claims, height, true);
                } else {
                    GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CREATE_FAILED_RESULT,
                            ImmutableMap.of("reason", result.getResultType())));
                }
                return;
            }

            // otherwise, advise him on the /trust command and show him his new claim
            else {
                Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_BUY_CONFIRMED, ImmutableMap.of(
                            "amount", requiredFunds));
                GriefDefenderPlugin.sendMessage(player, message);
                playerData.lastShovelLocation = null;
                message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CREATE_SUCCESS, ImmutableMap.of(
                        "type", gdClaim.getFriendlyNameType(true)));
                GriefDefenderPlugin.sendMessage(player, message);
                final WorldEditProvider worldEditProvider = GriefDefenderPlugin.getInstance().worldEditProvider;
                if (worldEditProvider != null) {
                    worldEditProvider.stopVisualDrag(player);
                    worldEditProvider.visualizeClaim(gdClaim, player, playerData, false);
                }
                gdClaim.getVisualizer().createClaimBlockVisuals(height, player.getLocation(), playerData);
                gdClaim.getVisualizer().apply(player, false);
            }
        };
    }

    public static TransactionResult depositFunds(UUID uuid, double amount) {
        final Account playerAccount = GriefDefenderPlugin.getInstance().economyService.get().getOrCreateAccount(uuid).orElse(null);
        final Currency defaultCurrency = GriefDefenderPlugin.getInstance().economyService.get().getDefaultCurrency();
        return playerAccount.deposit(defaultCurrency, BigDecimal.valueOf(amount), Sponge.getCauseStackManager().getCurrentCause());
    }

    public static TransactionResult withdrawFunds(UUID uuid, double amount) {
        final Account playerAccount = GriefDefenderPlugin.getInstance().economyService.get().getOrCreateAccount(uuid).orElse(null);
        final Currency defaultCurrency = GriefDefenderPlugin.getInstance().economyService.get().getDefaultCurrency();
        return playerAccount.withdraw(defaultCurrency, BigDecimal.valueOf(amount), Sponge.getCauseStackManager().getCurrentCause());
    }

    public GDClaimResult checkEconomyFunds(GDClaim claim, GDPlayerData newPlayerData, boolean withdrawFunds) {
        if (!GriefDefenderPlugin.getInstance().isEconomyModeEnabled()) { 
            return new GDClaimResult(claim, ClaimResultType.ECONOMY_ACCOUNT_NOT_FOUND);
        }

        final Object root = GDCauseStackManager.getInstance().getCurrentCause().root();
        final Player player = root instanceof Player ? (Player) root : null; 
        final World world = claim.getWorld();
        final int claimCost = BlockUtil.getInstance().getClaimBlockCost(world, claim.lesserBoundaryCorner, claim.greaterBoundaryCorner, claim.cuboid);
        final GDPermissionUser targetPlayer = newPlayerData.getSubject();
        final Account playerAccount = GriefDefenderPlugin.getInstance().economyService.get().getOrCreateAccount(targetPlayer.getUniqueId()).orElse(null);
        if (playerAccount == null) {
            return new GDClaimResult(claim, ClaimResultType.ECONOMY_ACCOUNT_NOT_FOUND);
        }

        final Currency defaultCurrency = GriefDefenderPlugin.getInstance().economyService.get().getDefaultCurrency();
        double requiredFunds = claimCost * claim.getOwnerEconomyBlockCost();
        final BigDecimal currentFunds = playerAccount.getBalance(defaultCurrency);
        if (currentFunds.doubleValue() < requiredFunds) {
            Component message = null;
            if (player != null) {
                message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_NOT_ENOUGH_FUNDS, ImmutableMap.of(
                        "balance", String.valueOf("$" + currentFunds.doubleValue()),
                        "amount", String.valueOf("$" + requiredFunds)));
                GriefDefenderPlugin.sendMessage(player, message);
            }

            //playerData.lastShovelLocation = null;
           // playerData.claimResizing = null;
            return new GDClaimResult(claim, ClaimResultType.ECONOMY_NOT_ENOUGH_FUNDS, message);
        }

        if (withdrawFunds) {
            final TransactionResult result = playerAccount.withdraw(defaultCurrency, BigDecimal.valueOf(requiredFunds), Sponge.getCauseStackManager().getCurrentCause());
            if (result.getResult() != ResultType.SUCCESS) {
                Component message = null;
                if (player != null) {
                    message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_WITHDRAW_ERROR, ImmutableMap.of(
                            "reason", result.getResult().name()));
                    GriefDefenderPlugin.sendMessage(player, message);
                }

                //playerData.lastShovelLocation = null;
                //playerData.claimResizing = null;
                return new GDClaimResult(claim, ClaimResultType.ECONOMY_WITHDRAW_FAIL, message);
            }
        }

        return new GDClaimResult(claim, ClaimResultType.SUCCESS);
    }
}
