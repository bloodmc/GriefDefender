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
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.claim.GDClaimManager;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.event.GDRemoveClaimEvent;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.text.action.GDCallbackHolder;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.spongeapi.TextAdapter;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.world.storage.WorldProperties;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_ABANDON_WORLD_CLAIMS)
public class CommandClaimAbandonWorld extends BaseCommand {

    @CommandCompletion("@gdworlds @gddummy")
    @CommandAlias("abandonworld")
    @Description("%abandon-world")
    @Subcommand("abandon world")
    @Syntax("[<world>]")
    public void execute(Player player, @Optional String worldName) {
        WorldProperties worldProperties = player.getWorld().getProperties();
        if (worldName != null) {
            worldProperties = Sponge.getServer().getWorldProperties(worldName).orElse(null);
            if (worldProperties == null) {
                TextAdapter.sendComponent(player, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.COMMAND_WORLD_NOT_FOUND,
                        ImmutableMap.of("world", worldName)));
                return;
            }
        }
        final GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(player.getWorld().getUniqueId());
        if (claimWorldManager.getWorldClaims().size() == 0) {
            try {
                throw new CommandException(MessageCache.getInstance().CLAIM_NO_CLAIMS);
            } catch (CommandException e) {
                TextAdapter.sendComponent(player, e.getText());
                return;
            }
        }

        final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ABANDON_WORLD_WARNING, ImmutableMap.of(
                "world", TextComponent.of(worldProperties.getWorldName())));
        final Component confirmationText = TextComponent.builder()
                .append(message)
                .append(TextComponent.builder()
                    .append("\n[")
                    .append(MessageCache.getInstance().LABEL_CONFIRM.color(TextColor.GREEN))
                    .append("]\n")
                    .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(player, createConfirmationConsumer(player, worldProperties), true)))
                    .hoverEvent(HoverEvent.showText(MessageCache.getInstance().UI_CLICK_CONFIRM)).build())
                .build();
        TextAdapter.sendComponent(player, confirmationText);
    }

    private static Consumer<CommandSource> createConfirmationConsumer(Player source, WorldProperties worldProperties) {
        return confirm -> {
            final GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(worldProperties.getUniqueId());
            for (GDPlayerData playerData : claimWorldManager.getPlayerDataMap().values()) {
                final GDPermissionUser user = playerData.getSubject();
                if (user == null) {
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

                Set<Claim> allowedClaims = new HashSet<>();
                final Player player = user.getOnlinePlayer();
                for (Claim claim : playerData.getInternalClaims()) {
                    if (!claim.getWorldUniqueId().equals(worldProperties.getUniqueId())) {
                        continue;
                    }
                    allowedClaims.add(claim);
                }

                if (!allowedClaims.isEmpty()) {
                    GDCauseStackManager.getInstance().pushCause(user);
                    GDRemoveClaimEvent.Abandon event = new GDRemoveClaimEvent.Abandon(ImmutableList.copyOf(allowedClaims));
                    GriefDefender.getEventManager().post(event);
                    GDCauseStackManager.getInstance().popCause();
                    if (event.cancelled()) {
                        TextAdapter.sendComponent(source, event.getMessage().orElse(MessageCache.getInstance().PLUGIN_EVENT_CANCEL).color(TextColor.RED));
                        return;
                    }

                    double refund = 0;
                    // adjust claim blocks
                    for (Claim claim : allowedClaims) {
                        if (claim.isSubdivision() || claim.isAdminClaim() || claim.isWilderness()) {
                            continue;
                        }
                        final double abandonReturnRatio = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Double.class), user, Options.ABANDON_RETURN_RATIO, claim);
                        if (GriefDefenderPlugin.getInstance().isEconomyModeEnabled()) {
                            refund += claim.getClaimBlocks() * abandonReturnRatio;
                        } else {
                            playerData.setAccruedClaimBlocks(playerData.getAccruedClaimBlocks() - ((int) Math.ceil(claim.getClaimBlocks() * (1 - abandonReturnRatio))));
                        }
                    }

                    //playerData.useRestoreSchematic = event.isRestoring();
                    GriefDefenderPlugin.getInstance().dataStore.abandonClaimsForPlayer(user, allowedClaims);
                    //playerData.useRestoreSchematic = false;
                    playerData.onClaimDelete();

                    if (GriefDefenderPlugin.getInstance().isEconomyModeEnabled()) {
                        final Account playerAccount = GriefDefenderPlugin.getInstance().economyService.get().getOrCreateAccount(playerData.playerID).orElse(null);
                        if (playerAccount == null) {
                            continue;
                        }

                        final Currency defaultCurrency = GriefDefenderPlugin.getInstance().economyService.get().getDefaultCurrency();
                        final TransactionResult result = playerAccount.deposit(defaultCurrency, BigDecimal.valueOf(refund), Sponge.getCauseStackManager().getCurrentCause());
                        if (result.getResult() == ResultType.SUCCESS && player != null) {
                            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_ABANDON_SUCCESS_WORLD, ImmutableMap.of(
                                    "world", worldProperties.getWorldName(),
                                    "amount", TextComponent.of(String.valueOf(refund))));
                            TextAdapter.sendComponent(player, message);
                        }
                    } else if (player != null) {
                        int remainingBlocks = playerData.getRemainingClaimBlocks();
                        final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ABANDON_SUCCESS_WORLD, ImmutableMap.of(
                                    "world", worldProperties.getWorldName(),
                                    "amount", remainingBlocks));
                        TextAdapter.sendComponent(player, message);
                    }
                }
            }
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ABANDON_WORLD_SUCCESS, ImmutableMap.of(
                    "world", worldProperties.getWorldName()));
            TextAdapter.sendComponent(source, message);
        };
    }
}