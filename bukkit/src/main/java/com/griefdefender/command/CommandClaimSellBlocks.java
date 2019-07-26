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
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.permission.GDPermissions;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.format.TextColor;
import org.bukkit.entity.Player;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_SELL_CLAIM_BLOCKS)
public class CommandClaimSellBlocks extends BaseCommand {

    @CommandAlias("sellblocks")
    @Description("Sell your claim blocks for server money.\nNote: Requires economy plugin.")
    @Syntax("[<amount>]")
    @Subcommand("sell blocks")
    public void execute(Player player, @Optional Integer blockCount) {
        if (GriefDefenderPlugin.getGlobalConfig().getConfig().economy.economyMode) {
            TextAdapter.sendComponent(player, TextComponent.of("This command is not available while server is in economy mode.", TextColor.RED));
            return;
        }

        // if economy is disabled, don't do anything
        if (GriefDefenderPlugin.getInstance().getVaultProvider() == null) {
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_NOT_INSTALLED));
            return;
        }

        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        if (playerData.getEconomyClaimBlockCost() == 0 && playerData.getEconomyClaimBlockReturn() == 0) {
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_BUY_SELL_DISABLED));
            return;
        }

        // if selling disabled, send error message
        if (playerData.getEconomyClaimBlockReturn() == 0) {
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_BLOCK_ONLY_BUY));
            return;
        }

        int availableBlocks = playerData.getRemainingClaimBlocks();
        if (blockCount == null) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.economyBlockPurchaseCost
                    .apply(ImmutableMap.of(
                    "cost", playerData.getEconomyClaimBlockReturn(),
                    "balance", availableBlocks)).build();
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        } else {
            // try to parse number of blocks
            if (blockCount <= 0) {
                GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.economyBuyInvalidBlockCount.toText());
                return;
            } else if (blockCount > availableBlocks) {
                GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_BLOCK_NOT_AVAILABLE));
                return;
            }

            // attempt to compute value and deposit it
            double totalValue = blockCount * playerData.getEconomyClaimBlockReturn();
            /*TransactionResult
                transactionResult =
                GriefDefenderPlugin.getInstance().economyService.get().getOrCreateAccount(player.getUniqueId()).get().deposit
                    (GriefDefenderPlugin.getInstance().economyService.get().getDefaultCurrency(), BigDecimal.valueOf(totalValue),
                        Sponge.getCauseStackManager().getCurrentCause());

            if (transactionResult.getResult() != ResultType.SUCCESS) {
                final Component message = GriefDefenderPlugin.getInstance().messageData.economyBlockSellError
                    .apply(ImmutableMap.of(
                        "reason", transactionResult.getResult().name())).build();
                GriefDefenderPlugin.sendMessage(player, message);
                return CommandResult.success();
            }
            // subtract blocks
            playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks() - blockCount);
            playerData.getStorageData().save();*/

            final Component message = GriefDefenderPlugin.getInstance().messageData.economyBlockSaleConfirmation
                    .apply(ImmutableMap.of(
                    "deposit", totalValue,
                    "remaining-blocks", playerData.getRemainingClaimBlocks())).build();
            // inform player
            GriefDefenderPlugin.sendMessage(player, message);
        }
    }
}
