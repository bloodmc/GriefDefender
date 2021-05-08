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
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import net.kyori.text.Component;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import com.google.common.collect.ImmutableMap;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.economy.TransactionResultType;
import com.griefdefender.api.economy.TransactionType;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.economy.GDPaymentTransaction;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.util.EconomyUtil;

import java.time.Instant;
import java.util.Date;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_CLAIM_TAX)
public class CommandClaimTax extends BaseCommand {

    @CommandCompletion("@gdtaxcommands @gddummy")
    @CommandAlias("claimtax")
    @Description("%claim-tax")
    @Syntax("balance|pay <amount>")
    @Subcommand("claim tax")
    public void execute(Player player, String[] args) throws CommandException {
        if (!GriefDefenderPlugin.getGlobalConfig().getConfig().economy.taxSystem) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().TAX_SYSTEM_DISABLED);
            return;
        }

        if (GriefDefenderPlugin.getInstance().getEconomyService() == null) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().ECONOMY_NOT_INSTALLED);
            return;
        }

        if (args.length == 0) {
            throw new InvalidCommandArgument();
        }

        final GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAt(player.getLocation());
        if (claim.isSubdivision() || claim.isAdminClaim()) {
            return;
        }

        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        if (!claim.getOwnerUniqueId().equals(player.getUniqueId()) && !playerData.canIgnoreClaim(claim) && claim.allowEdit(player) != null) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().PERMISSION_TAX);
            return;
        }

        final Account ownerAccount = GriefDefenderPlugin.getInstance().economyService.get().getOrCreateAccount(player.getUniqueId()).orElse(null);
        if (ownerAccount == null) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_PLAYER_NOT_FOUND, ImmutableMap.of(
                    "player", player.getName()));
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }

        final String command = args[0];
        double amount = args.length > 1 ? Double.parseDouble(args[1]) : 0;

        if (command.equalsIgnoreCase("balance")) {
            final double taxBalance = claim.getEconomyData().getTaxBalance();
            Component message = null;
            if (taxBalance > 0) {
                message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.TAX_PAST_DUE,
                        ImmutableMap.of(
                            "balance", taxBalance,
                            "date", Date.from(claim.getEconomyData().getTaxPastDueDate())));
            } else {
                message = MessageCache.getInstance().TAX_NO_BALANCE;
            }
            GriefDefenderPlugin.sendMessage(player, message);
        } else if (command.equalsIgnoreCase("pay")) {
            final double taxBalance = claim.getEconomyData().getTaxBalance();
            if (taxBalance <= 0 || amount <= 0) {
                return;
            }

            if (amount > taxBalance) {
                amount = taxBalance;
            }

            final TransactionResult result = EconomyUtil.withdrawFunds(player.getUniqueId(), amount);
            if (result.getResult() == ResultType.SUCCESS) {
                double depositAmount = amount;
                    depositAmount -= claim.getEconomyData().getTaxBalance();
                if (depositAmount >= 0) {
                    claim.getEconomyData().addPaymentTransaction(new GDPaymentTransaction(TransactionType.TAX, TransactionResultType.SUCCESS, Instant.now(), taxBalance));
                    claim.getEconomyData().setTaxPastDueDate(null);
                    claim.getEconomyData().setTaxBalance(0);
                    claim.getInternalClaimData().setExpired(false);
                    final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.TAX_PAID_BALANCE,
                            ImmutableMap.of(
                                "amount", taxBalance));
                    GriefDefenderPlugin.sendMessage(player, message);
                    return;
                } else {
                    final double newTaxBalance = Math.abs(depositAmount);
                    claim.getEconomyData().setTaxBalance(newTaxBalance);
                    final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.TAX_PAID_PARTIAL,
                            ImmutableMap.of(
                                "amount", depositAmount,
                                "balance", newTaxBalance));
                    GriefDefenderPlugin.sendMessage(player, message);
                    return;
                }
            } else {
                GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.TAX_PAY_NO_FUNDS));
                claim.getData().getEconomyData()
                    .addPaymentTransaction(new GDPaymentTransaction(TransactionType.TAX, TransactionResultType.FAIL, playerData.playerID, Instant.now(), amount));
                return;
            }
        }
    }
}
