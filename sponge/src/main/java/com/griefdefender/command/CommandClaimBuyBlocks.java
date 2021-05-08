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
import com.google.common.reflect.TypeToken;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissions;
import net.kyori.text.Component;
import net.kyori.text.adapter.spongeapi.TextAdapter;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;

import java.math.BigDecimal;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_BUY_CLAIM_BLOCKS)
public class CommandClaimBuyBlocks extends BaseCommand {

    @CommandAlias("buyclaim|buyclaimblocks|buyblocks")
    @Description("%buy-blocks")
    @Syntax("[<amount>]")
    @Subcommand("buy blocks")
    public void execute(Player player, @Optional Integer blockCount) {
        if (GriefDefenderPlugin.getInstance().isEconomyModeEnabled()) {
            TextAdapter.sendComponent(player, MessageCache.getInstance().COMMAND_NOT_AVAILABLE_ECONOMY);
            return;
        }

        if (!GriefDefenderPlugin.getInstance().economyService.isPresent()) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().ECONOMY_NOT_INSTALLED);
            return;
        }

        Account playerAccount = GriefDefenderPlugin.getInstance().economyService.get().getOrCreateAccount(player.getUniqueId()).orElse(null);
        if (playerAccount == null) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_PLAYER_NOT_FOUND, ImmutableMap.of(
                    "player", player.getName()));
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }

        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        final double economyBlockCost = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Double.class), player, Options.ECONOMY_BLOCK_COST);
        if (economyBlockCost <= 0) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().ECONOMY_BLOCK_BUY_SELL_DISABLED);
            return;
        }

        double balance = playerAccount.getBalance(GriefDefenderPlugin.getInstance().economyService.get().getDefaultCurrency()).doubleValue();
        if (blockCount == null) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_BLOCK_PURCHASE_COST, ImmutableMap.of(
                    "amount", economyBlockCost,
                    "balance", String.valueOf("$" + balance)));
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        } else {
            if (blockCount <= 0) {
                GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().ECONOMY_BLOCK_BUY_INVALID);
                return;
            }

            final double totalCost = blockCount * economyBlockCost;
            final int newClaimBlockTotal = playerData.getAccruedClaimBlocks() + blockCount;
            if (newClaimBlockTotal > playerData.getMaxAccruedClaimBlocks()) {
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_BLOCK_PURCHASE_LIMIT, ImmutableMap.of(
                            "total", newClaimBlockTotal,
                            "limit", playerData.getMaxAccruedClaimBlocks()));
                    GriefDefenderPlugin.sendMessage(player, message);
                    return;
            }

            try (final CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
                Sponge.getCauseStackManager().addContext(GriefDefenderPlugin.PLUGIN_CONTEXT, GriefDefenderPlugin.getInstance());
                TransactionResult transactionResult = playerAccount.withdraw
                    (GriefDefenderPlugin.getInstance().economyService.get().getDefaultCurrency(), BigDecimal.valueOf(totalCost),
                        Sponge.getCauseStackManager().getCurrentCause());

                if (transactionResult.getResult() != ResultType.SUCCESS) {
                    final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_WITHDRAW_ERROR, ImmutableMap.of(
                            "reason", transactionResult.getResult()));
                    GriefDefenderPlugin.sendMessage(player, message);
                    return;
                }
            }

            final int bonusTotal = playerData.getBonusClaimBlocks();
            playerData.setBonusClaimBlocks(bonusTotal + blockCount);
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_BLOCK_PURCHASE_CONFIRMATION, ImmutableMap.of(
                    "amount", totalCost,
                    "balance", playerData.getRemainingClaimBlocks()));
            GriefDefenderPlugin.sendMessage(player, message);
        }
    }
}
