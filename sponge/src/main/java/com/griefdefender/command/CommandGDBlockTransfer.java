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

import com.google.common.collect.ImmutableMap;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaimManager;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.storage.BaseStorage;
import com.griefdefender.text.action.GDCallbackHolder;

import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.spongeapi.TextAdapter;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.world.storage.WorldProperties;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_ECONOMY_BLOCK_TRANSFER)
public class CommandGDBlockTransfer extends BaseCommand {

    @CommandAlias("gdblocktransfer")
    @Description("%economy-block-transfer")
    @Subcommand("economy blocktransfer")
    public void execute(CommandSource source, @Optional Integer blockCount) {
        // if economy is disabled, don't do anything
        if (!GriefDefenderPlugin.getInstance().economyService.isPresent()) {
            GriefDefenderPlugin.sendMessage(source, MessageCache.getInstance().ECONOMY_NOT_INSTALLED);
            return;
        }
        if (!GriefDefenderPlugin.getInstance().isEconomyModeEnabled()) {
            GriefDefenderPlugin.sendMessage(source, MessageCache.getInstance().ECONOMY_MODE_NOT_ENABLED);
            return;
        }

        final Component confirmationText = TextComponent.builder()
                .append(MessageCache.getInstance().ECONOMY_BLOCK_TRANSFER_WARNING)
                .append(TextComponent.builder()
                    .append("\n[")
                    .append(MessageCache.getInstance().LABEL_CONFIRM.color(TextColor.GREEN))
                    .append("]\n")
                    .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(source, createConfirmationConsumer(source), true)))
                    .hoverEvent(HoverEvent.showText(MessageCache.getInstance().UI_CLICK_CONFIRM)).build())
                .build();
        TextAdapter.sendComponent(source, confirmationText);
    }

    public Consumer<CommandSource> createConfirmationConsumer(CommandSource source) {
        return confirm -> {
            Set<GDPlayerData> playerDataSet = new HashSet<>();
            if (BaseStorage.USE_GLOBAL_PLAYER_STORAGE) {
                final UUID worldUniqueId = Sponge.getServer().getDefaultWorld().get().getUniqueId();
                final GDClaimManager claimManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(worldUniqueId);
                playerDataSet.addAll(claimManager.getPlayerDataMap().values());
            } else {
                for (WorldProperties worldProperties : Sponge.getServer().getAllWorldProperties()) {
                    final GDClaimManager claimManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(worldProperties.getUniqueId());
                    playerDataSet.addAll(claimManager.getPlayerDataMap().values());
                }
            }

            int count = 0;
            for (GDPlayerData playerData : playerDataSet) {
                final double economyBlockCost = playerData.getEconomyClaimBlockCost();
                if (economyBlockCost <= 0) {
                    continue;
                }
                if (playerData.playerID.equals(GriefDefenderPlugin.ADMIN_USER_UUID)) {
                    continue;
                }
                if (playerData.playerID.equals(GriefDefenderPlugin.PUBLIC_UUID)) {
                    continue;
                }
                if (playerData.playerID.equals(GriefDefenderPlugin.WORLD_USER_UUID)) {
                    continue;
                }

                final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(playerData.getUniqueId());
                if (user == null) {
                    continue;
                }
                System.out.println("Migrating user " + user.getFriendlyName() + " remaining claimblocks...");
                final Account playerAccount = GriefDefenderPlugin.getInstance().economyService.get().getOrCreateAccount(user.getUniqueId()).orElse(null);
                if (playerAccount == null) {
                    final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_PLAYER_NOT_FOUND, ImmutableMap.of(
                            "player", user.getFriendlyName()));
                    GriefDefenderPlugin.sendMessage(source, message);
                    continue;
                }
    
                int availableBlocks = playerData.getAccruedClaimBlocks() + playerData.getBonusClaimBlocks();
                int totalClaimCost = playerData.getTotalClaimsCost();
                final int remainingBlocks = availableBlocks - totalClaimCost;
                if (remainingBlocks <= 0) {
                    System.out.println("User " + user.getFriendlyName() + " has " + remainingBlocks + " remaining claim blocks. Skipping...");
                    continue;
                }

                // attempt to compute value and deposit it
                double economyTotalValue = economyBlockCost * remainingBlocks;
                final Currency defaultCurrency = GriefDefenderPlugin.getInstance().economyService.get().getDefaultCurrency();
                final TransactionResult result = playerAccount.deposit(defaultCurrency, BigDecimal.valueOf(economyTotalValue), Sponge.getCauseStackManager().getCurrentCause());
                if (result.getResult() != ResultType.SUCCESS) {
                    final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_DEPOSIT_ERROR, ImmutableMap.of(
                            "reason", result.getResult()));
                    GriefDefenderPlugin.sendMessage(source, message);
                    continue;
                }

                final double balance = playerAccount.getBalance(GriefDefenderPlugin.getInstance().economyService.get().getDefaultCurrency()).doubleValue();
                Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_MODE_BLOCK_SALE_CONFIRMATION,
                            ImmutableMap.of(
                            "deposit", "$" + String.format("%.2f", economyTotalValue),
                            "balance", "$" + String.format("%.2f", balance),
                            "amount", playerData.getRemainingClaimBlocks()));
                playerData.setAccruedClaimBlocks(0);
                playerData.setBonusClaimBlocks(0);
                if (user.getOnlinePlayer() != null) {
                    GriefDefenderPlugin.sendMessage(user.getOnlinePlayer(), message);
                }
                count++;
            }
            if (count > 0) {
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_BLOCK_TRANSFER_SUCCESS,
                        ImmutableMap.of(
                        "count", count));
                GriefDefenderPlugin.sendMessage(source, message);
            } else {
                GriefDefenderPlugin.sendMessage(source, MessageCache.getInstance().ECONOMY_BLOCK_TRANSFER_CANCEL);
            }
        };
    }
}
