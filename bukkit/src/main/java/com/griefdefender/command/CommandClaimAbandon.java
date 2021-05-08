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
import co.aikar.commands.annotation.Subcommand;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimResult;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
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
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_ABANDON_BASIC)
public class CommandClaimAbandon extends BaseCommand {

    protected boolean abandonTopClaim = false;

    @CommandAlias("abandon|abandonclaim")
    @Description("%abandon-claim")
    @Subcommand("abandon claim")
    public void execute(Player player) {
        final GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAt(player.getLocation());
        final UUID ownerId = claim.getOwnerUniqueId();
        final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(player.getUniqueId());
        final GDPlayerData playerData = user.getInternalPlayerData();

        final boolean isAdmin = playerData.canIgnoreClaim(claim);
        if (claim.isWilderness()) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().ABANDON_CLAIM_MISSING);
            return;
        } else if (!isAdmin && !player.getUniqueId().equals(ownerId) && claim.isUserTrusted(player, TrustTypes.MANAGER)) {
            if (claim.parent == null) {
                // Managers can only abandon child claims
                GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CLAIM_NOT_YOURS);
                return;
            }
        } else if (!isAdmin && (claim.allowEdit(player) != null || (!claim.isAdminClaim() && !player.getUniqueId().equals(ownerId)))) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CLAIM_NOT_YOURS);
            return;
        } else {
            if (this.abandonTopClaim && claim.children.size() > 0) {
                if (claim.isTown() || claim.isAdminClaim()) {
                    Set<Claim> invalidClaims = new HashSet<>();
                    for (Claim child : claim.getChildren(true)) {
                        if (child.getOwnerUniqueId() == null || !child.getOwnerUniqueId().equals(ownerId)) {
                            invalidClaims.add(child);
                        }
                    }
    
                    if (!invalidClaims.isEmpty()) {
                        GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().ABANDON_TOWN_CHILDREN);
                        CommandHelper.showClaims(player, invalidClaims, 0, true);
                        return;
                    }
                }
            }
        }
        if (!isAdmin && (claim.isTown() && claim.children.size() > 0)) {
            Set<Claim> childClaims = new HashSet<>();
            for (Claim child : claim.getChildren(true)) {
                if (!child.isBasicClaim()) {
                    continue;
                }
                if (child.getOwnerUniqueId().equals(user.getUniqueId())) {
                    childClaims.add(child);
                }
            }
            if (!childClaims.isEmpty()) {
                GriefDefenderPlugin.sendMessage(player, TextComponent.of("Abandon town failed! You must abandon all basic children claims owned by you first.", TextColor.RED));
                CommandHelper.showClaims(player, childClaims, 0, true);
                return;
            }
        }

        if (!isAdmin) {
            final int abandonDelay = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), user, Options.ABANDON_DELAY, claim);
            if (abandonDelay > 0) {
                final Instant localNow = Instant.now();
                final Instant dateCreated = claim.getInternalClaimData().getDateCreated();
                final Instant delayExpires = dateCreated.plus(Duration.ofDays(abandonDelay));
                final boolean delayActive = !delayExpires.isBefore(localNow);
                if (delayActive) {
                    TextAdapter.sendComponent(player, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.ABANDON_CLAIM_DELAY_WARNING, 
                            ImmutableMap.of("date", Date.from(delayExpires))));
                    return;
                }
            }
        }

        final boolean autoSchematicRestore = GriefDefenderPlugin.getActiveConfig(player.getWorld().getUID()).getConfig().claim.claimAutoSchematicRestore;
        final Component confirmationText = TextComponent.builder()
                .append(autoSchematicRestore ? MessageCache.getInstance().SCHEMATIC_ABANDON_RESTORE_WARNING : MessageCache.getInstance().ABANDON_WARNING)
                    .append(TextComponent.builder()
                    .append("\n[")
                    .append(MessageCache.getInstance().LABEL_CONFIRM.color(TextColor.GREEN))
                    .append("]\n")
                    .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(player, createConfirmationConsumer(player, user, claim, this.abandonTopClaim), true)))
                    .hoverEvent(HoverEvent.showText(MessageCache.getInstance().UI_CLICK_CONFIRM)).build())
                .build();
        TextAdapter.sendComponent(player, confirmationText);
    }

    private static Consumer<CommandSender> createConfirmationConsumer(Player source, GDPermissionUser user, GDClaim claim, boolean abandonTopClaim) {
        return confirm -> {

            GDCauseStackManager.getInstance().pushCause(source);
            GDRemoveClaimEvent.Abandon event = new GDRemoveClaimEvent.Abandon(claim);
            GriefDefender.getEventManager().post(event);
            GDCauseStackManager.getInstance().popCause();
            if (event.cancelled()) {
                TextAdapter.sendComponent(source, event.getMessage().orElse(MessageCache.getInstance().PLUGIN_EVENT_CANCEL));
                return;
            }

            if (!claim.isSubdivision() && !claim.isAdminClaim()) {
                if (GriefDefenderPlugin.getInstance().isEconomyModeEnabled()) {
                    final Economy economy = GriefDefenderPlugin.getInstance().getVaultProvider().getApi();
                    if (!economy.hasAccount(user.getOfflinePlayer())) {
                        return;
                    }
                }
            }

            final GDPlayerData playerData = user.getInternalPlayerData();
            GDClaimManager claimManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(source.getWorld().getUID());
            playerData.useRestoreSchematic = event.isRestoring();
            final ClaimResult claimResult = claimManager.deleteClaimInternal(claim, abandonTopClaim);
            playerData.useRestoreSchematic = false;
            if (!claimResult.successful()) {
                TextAdapter.sendComponent(source, event.getMessage().orElse(GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ABANDON_FAILED,
                        ImmutableMap.of("result", claimResult.getMessage().orElse(TextComponent.of(claimResult.getResultType().toString()))))));
                return;
            }

            playerData.onClaimDelete();
            playerData.revertClaimVisual(claim);

            if (claim.isTown()) {
                playerData.inTown = false;
                playerData.townChat = false;
            }

            if (claim.isSubdivision()) {
                int remainingBlocks = playerData.getRemainingClaimBlocks();
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ABANDON_SUCCESS, ImmutableMap.of(
                        "amount", remainingBlocks));
                GriefDefenderPlugin.sendMessage(source, message);
                return;
            }

            if (!claim.isAdminClaim()) {
                // check parent
                final Claim parentClaim = claim.getParent().orElse(null);
                if (parentClaim != null && parentClaim.isTown()) {
                    if (parentClaim.isUserTrusted(user.getUniqueId(), TrustTypes.MANAGER)) {
                        int remainingBlocks = playerData.getRemainingClaimBlocks();
                        final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ABANDON_SUCCESS, ImmutableMap.of(
                                "amount", remainingBlocks));
                        GriefDefenderPlugin.sendMessage(source, message);
                        return;
                    }
                }
                final double abandonReturnRatio = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Double.class), user, Options.ABANDON_RETURN_RATIO, claim);
                if (GriefDefenderPlugin.getInstance().isEconomyModeEnabled()) {
                    final Economy economy = GriefDefenderPlugin.getInstance().getVaultProvider().getApi();
                    final double requiredClaimBlocks = claim.getClaimBlocks() * abandonReturnRatio;
                    final double refund = requiredClaimBlocks * claim.getOwnerEconomyBlockCost();
                    final EconomyResponse result = economy.depositPlayer(user.getOfflinePlayer(), refund);
                    if (result.transactionSuccess()) {
                        final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_ABANDON_SUCCESS, ImmutableMap.of(
                                "amount", refund));
                        GriefDefenderPlugin.sendMessage(source, message);
                    }
                } else {
                    int newAccruedClaimCount = playerData.getAccruedClaimBlocks() - ((int) Math.ceil(claim.getClaimBlocks() * (1 - abandonReturnRatio)));
                    playerData.setAccruedClaimBlocks(newAccruedClaimCount);
                    int remainingBlocks = playerData.getRemainingClaimBlocks();
                    final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ABANDON_SUCCESS, ImmutableMap.of(
                            "amount", remainingBlocks));
                    GriefDefenderPlugin.sendMessage(source, message);
                }
            }
        };
    }
}
