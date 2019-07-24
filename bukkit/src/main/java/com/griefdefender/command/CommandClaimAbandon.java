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
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.claim.GDClaimManager;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.event.GDDeleteClaimEvent;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.util.PermissionUtil;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.format.TextColor;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_ABANDON_BASIC)
public class CommandClaimAbandon extends BaseCommand {

    protected boolean abandonTopClaim = false;

    @CommandAlias("abandonclaim")
    @Description("Abandons a claim")
    @Subcommand("abandon claim")
    public void execute(Player player) {
        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        final GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAt(player.getLocation());
        final UUID ownerId = claim.getOwnerUniqueId();

        final boolean isAdmin = playerData.canIgnoreClaim(claim);
        final boolean isTown = claim.isTown();
        if (claim.isWilderness()) {
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.commandAbandonClaimMissing.toText());
            return;
        } else if (!isAdmin && !player.getUniqueId().equals(ownerId) && claim.isUserTrusted(player, TrustTypes.MANAGER)) {
            if (claim.parent == null) {
                // Managers can only abandon child claims
                GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.claimNotYours.toText());
                return;
            }
        } else if (!isAdmin && (claim.allowEdit(player) != null || (!claim.isAdminClaim() && !player.getUniqueId().equals(ownerId)))) {
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.claimNotYours.toText());
            return;
        }

        if (!claim.isTown() && !claim.isAdminClaim() && claim.children.size() > 0 && !this.abandonTopClaim) {
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.commandAbandonTopLevel.toText());
            return;
        } else {
            if (this.abandonTopClaim && (claim.isTown() || claim.isAdminClaim()) && claim.children.size() > 0) {
                Set<Claim> invalidClaims = new HashSet<>();
                for (Claim child : claim.getChildren(true)) {
                    if (child.getOwnerUniqueId() == null || !child.getOwnerUniqueId().equals(ownerId)) {
                        //return CommandResult.empty();
                        invalidClaims.add(child);
                    }
                }

                if (!invalidClaims.isEmpty()) {
                    GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.commandAbandonTownChildren.toText());
                    CommandHelper.showClaims(player, invalidClaims, 0, true);
                    return;
                }
            }

            GDCauseStackManager.getInstance().pushCause(player);
            GDDeleteClaimEvent.Abandon event = new GDDeleteClaimEvent.Abandon(claim);
            GriefDefender.getEventManager().post(event);
            GDCauseStackManager.getInstance().popCause();
            if (event.cancelled()) {
                TextAdapter.sendComponent(player, event.getMessage().orElse(TextComponent.of("Could not abandon claim. A plugin has denied it.").color(TextColor.RED)));
                return;
            }

            if (!claim.isSubdivision() && !claim.isAdminClaim()) {
                if (GriefDefenderPlugin.getInstance().isEconomyModeEnabled()) {
                    final Economy economy = GriefDefenderPlugin.getInstance().getVaultProvider().getApi();
                    if (!economy.hasAccount(player)) {
                        return;
                    }
                }
            }

            GDClaimManager claimManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(player.getWorld().getUID());
            claimManager.deleteClaimInternal(claim, this.abandonTopClaim);
            // remove all context permissions
            PermissionUtil.getInstance().clearPermissions(player, claim.getContext());
            PermissionUtil.getInstance().clearPermissions(GriefDefenderPlugin.DEFAULT_HOLDER, claim.getContext());

            playerData.revertActiveVisual(player);
            if (isTown) {
                playerData.inTown = false;
                playerData.townChat = false;
            }

            if (!claim.isSubdivision() && !claim.isAdminClaim()) {
                final double abandonReturnRatio = GDPermissionManager.getInstance().getInternalOptionValue(player, Options.ABANDON_RETURN_RATIO, claim, playerData);
                if (GriefDefenderPlugin.getInstance().isEconomyModeEnabled()) {
                    final Economy economy = GriefDefenderPlugin.getInstance().getVaultProvider().getApi();
                    final double requiredClaimBlocks = claim.getClaimBlocks() * abandonReturnRatio;
                    final double refund = requiredClaimBlocks * claim.getOwnerEconomyBlockCost();
                    final EconomyResponse result = economy.depositPlayer(player, refund);
                    if (result.transactionSuccess()) {
                        final Component message = GriefDefenderPlugin.getInstance().messageData.economyClaimAbandonSuccess
                                .apply(ImmutableMap.of(
                                "refund", refund
                        )).build();
                        GriefDefenderPlugin.sendMessage(player, message);
                    }
                } else {
                    int newAccruedClaimCount = playerData.getAccruedClaimBlocks() - ((int) Math.ceil(claim.getClaimBlocks() * (1 - abandonReturnRatio)));
                    playerData.setAccruedClaimBlocks(newAccruedClaimCount);
                    int remainingBlocks = playerData.getRemainingClaimBlocks();
                    final Component message = GriefDefenderPlugin.getInstance().messageData.claimAbandonSuccess
                            .apply(ImmutableMap.of(
                            "remaining-blocks", remainingBlocks
                    )).build();
                    GriefDefenderPlugin.sendMessage(player, message);
                }
            }
        }
    }
}
