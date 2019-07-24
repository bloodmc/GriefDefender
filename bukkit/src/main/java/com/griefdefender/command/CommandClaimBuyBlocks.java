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
package com.griefdefender.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;

import com.google.common.collect.ImmutableMap;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissions;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.format.TextColor;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_BUY_CLAIM_BLOCKS)
public class CommandClaimBuyBlocks extends BaseCommand {

    @CommandAlias("buyblocks")
    @Description("Purchases additional claim blocks with server money.\nNote: Requires economy plugin.")
    @Syntax("[<amount>]")
    @Subcommand("buy blocks")
    public void execute(Player player, @Optional Integer blockCount) {
        if (GriefDefenderPlugin.getInstance().isEconomyModeEnabled()) {
            TextAdapter.sendComponent(player, TextComponent.of("This command is not available while server is in economy mode.", TextColor.RED));
            return;
        }

        if (GriefDefenderPlugin.getInstance().getVaultProvider() == null) {
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.economyNotInstalled.toText());
            return;
        }

        final Economy economy = GriefDefenderPlugin.getInstance().getVaultProvider().getApi();
        if (!economy.hasAccount(player)) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.economyUserNotFound
                    .apply(ImmutableMap.of(
                    "user", player.getName())).build();
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }

        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        final GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAt(player.getLocation());
        final double economyBlockCost = GDPermissionManager.getInstance().getGlobalInternalOptionValue(player, Options.ECONOMY_BLOCK_COST, playerData);
        final double economyBlockSell = GDPermissionManager.getInstance().getGlobalInternalOptionValue(player, Options.ECONOMY_BLOCK_SELL_RETURN, playerData);
        if (economyBlockCost == 0 && economyBlockSell == 0) {
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.economyBuySellNotConfigured.toText());
            return;
        }

        if (economyBlockCost == 0) {
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.economyOnlySellBlocks.toText());
            return;
        }

        final double balance = economy.getBalance(player);
        if (blockCount == null) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.economyBlockPurchaseCost
                    .apply(ImmutableMap.of(
                    "cost", economyBlockCost,
                    "balance", balance)).build();
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        } else {
            if (blockCount <= 0) {
                GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.economyBuyInvalidBlockCount.toText());
                return;
            }

            final double totalCost = blockCount * economyBlockCost;
            final int newClaimBlockTotal = playerData.getAccruedClaimBlocks() + blockCount;
            if (newClaimBlockTotal > playerData.getMaxAccruedClaimBlocks()) {
                final Component message = GriefDefenderPlugin.getInstance().messageData.claimBlockPurchaseLimit
                        .apply(ImmutableMap.of(
                            "new_total", newClaimBlockTotal,
                            "block_limit", playerData.getMaxAccruedClaimBlocks())).build();
                    GriefDefenderPlugin.sendMessage(player, message);
                    return;
            }

            final EconomyResponse result = economy.withdrawPlayer(player, totalCost);

            if (!result.transactionSuccess()) {
                final Component message = GriefDefenderPlugin.getInstance().messageData.economyWithdrawError
                    .apply(ImmutableMap.of(
                        "reason", result.errorMessage)).build();
                GriefDefenderPlugin.sendMessage(player, message);
                return;
            }

            playerData.addAccruedClaimBlocks(blockCount);
            playerData.getStorageData().save();

            final Component message = GriefDefenderPlugin.getInstance().messageData.economyBlocksPurchaseConfirmation
                    .apply(ImmutableMap.of(
                    "cost", totalCost,
                    "remaining-blocks", playerData.getRemainingClaimBlocks())).build();
            GriefDefenderPlugin.sendMessage(player, message);
        }
    }
}
