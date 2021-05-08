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

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.util.Direction;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableMap;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimBlockSystem;
import com.griefdefender.api.claim.ClaimResult;
import com.griefdefender.api.claim.ClaimResultType;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.internal.visual.GDClaimVisual;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.util.PlayerUtil;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_CLAIM_EXPAND)
public class CommandClaimExpand extends BaseCommand {

    @CommandCompletion("@gddummy @gdDirections @gddummy")
    @CommandAlias("claimexpand|expandclaim")
    @Description("%claim-expand")
    @Syntax("<amount> [direction]")
    @Subcommand("claim expand")
    public void execute(Player player, int amount, @Optional String direction) {
        final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(player);
        final GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(user.getInternalPlayerData(), player.getLocation());
        final GDPlayerData playerData = user.getInternalPlayerData();

        if (claim.isWilderness()) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CLAIM_NOT_YOURS);
            return;
        }
        if (!claim.getInternalClaimData().isResizable() || (!claim.getOwnerUniqueId().equals(player.getUniqueId()) && !playerData.canIgnoreClaim(claim) && claim.allowEdit(player) != null)) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().PERMISSION_CLAIM_RESIZE);
            return;
        }

        final Vector3i lesser = claim.lesserBoundaryCorner;
        final Vector3i greater = claim.greaterBoundaryCorner;
        Vector3i point1 = null;
        Vector3i point2 = null;
        if (direction == null || !direction.equalsIgnoreCase("all")) {
            final Direction face = direction == null ? PlayerUtil.getInstance().getBlockFace(player) : PlayerUtil.getInstance().getBlockFace(direction);
            if (face == null || amount <= 0) {
                GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().COMMAND_INVALID);
                return;
            }
    
            if ((face == Direction.UP || face == Direction.DOWN) && !claim.cuboid) {
                GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().COMMAND_INVALID);
                return;
            }
            if (face == Direction.EAST) {
                point1 = new Vector3i(lesser.getX(), lesser.getY(), lesser.getZ());
                point2 = new Vector3i(greater.getX() + amount, greater.getY(), greater.getZ());
            } else if (face == Direction.WEST) {
                point1 = new Vector3i(lesser.getX() - amount, lesser.getY(), lesser.getZ());
                point2 = new Vector3i(greater.getX(), greater.getY(), greater.getZ());
            } else if (face == Direction.NORTH) {
                point1 = new Vector3i(lesser.getX(), lesser.getY(), lesser.getZ() - amount);
                point2 = new Vector3i(greater.getX(), greater.getY(), greater.getZ());
            } else if (face == Direction.SOUTH) {
                point1 = new Vector3i(lesser.getX(), lesser.getY(), lesser.getZ());
                point2 = new Vector3i(greater.getX(), greater.getY(), greater.getZ() + amount);
            } else if (face == Direction.UP) {
                point1 = new Vector3i(lesser.getX(), lesser.getY(), lesser.getZ());
                point2 = new Vector3i(greater.getX(), greater.getY() + amount, greater.getZ());
            }  else if (face == Direction.DOWN) {
                point1 = new Vector3i(lesser.getX(), lesser.getY() - amount, lesser.getZ());
                point2 = new Vector3i(greater.getX(), greater.getY(), greater.getZ());
            }
        } else {
            point1 = new Vector3i(
                    lesser.getX() - amount,
                    lesser.getY(),
                    lesser.getZ() - amount);
            point2 = new Vector3i(
                greater.getX() + amount,
                greater.getY(),
                greater.getZ() + amount);
        }

        GDCauseStackManager.getInstance().pushCause(player);
        final ClaimResult result = claim.resize(point1, point2);
        GDCauseStackManager.getInstance().popCause();
        if (!result.successful()) {
            if (result.getResultType() == ClaimResultType.OVERLAPPING_CLAIM) {
                GDClaim overlapClaim = (GDClaim) result.getClaim().get();
                GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().RESIZE_OVERLAP);
                Set<Claim> claims = new HashSet<>();
                claims.add(overlapClaim);
                CommandHelper.showOverlapClaims(player, claims, PlayerUtil.getInstance().getEyeHeight(player));
            } else {
                // TODO add to lang
                GriefDefenderPlugin.sendMessage(player, TextComponent.of("Could not resize claim. Reason : " + result.getResultType()).color(TextColor.RED));
            }
        } else {
            int claimBlocksRemaining = playerData.getRemainingClaimBlocks();;
            if (!claim.isAdminClaim()) {
                UUID ownerID = claim.getOwnerUniqueId();
                if (claim.parent != null) {
                    ownerID = claim.parent.getOwnerUniqueId();
                }

                if (ownerID.equals(player.getUniqueId())) {
                    claimBlocksRemaining = playerData.getRemainingClaimBlocks();
                } else {
                    GDPlayerData ownerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), ownerID);
                    claimBlocksRemaining = ownerData.getRemainingClaimBlocks();
                    final Player owner = Sponge.getServer().getPlayer(ownerID).orElse(null);
                    if (owner == null || !owner.isOnline()) {
                        GriefDefenderPlugin.getInstance().dataStore.clearCachedPlayerData(player.getWorld().getUniqueId(), ownerID);
                    }
                }
            }
            if (GriefDefenderPlugin.getInstance().isEconomyModeEnabled()) {
                final Account playerAccount = GriefDefenderPlugin.getInstance().economyService.get().getOrCreateAccount(player.getUniqueId()).orElse(null);
                if (playerAccount != null) {
                    final Currency defaultCurrency = GriefDefenderPlugin.getInstance().economyService.get().getDefaultCurrency();
                    final BigDecimal currentFunds = playerAccount.getBalance(defaultCurrency);
                    if (GriefDefenderPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.VOLUME) {
                        final double claimableChunks = claimBlocksRemaining / 65536.0;
                        final Map<String, Object> params = ImmutableMap.of(
                                "balance", String.valueOf("$" + currentFunds.intValue()),
                                "chunk-amount", Math.round(claimableChunks * 100.0)/100.0, 
                                "block-amount", claimBlocksRemaining);
                        GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_MODE_RESIZE_SUCCESS_3D, params));
                    } else {
                        final Map<String, Object> params = ImmutableMap.of(
                                "balance", String.valueOf("$" + currentFunds.intValue()),
                                "block-amount", claimBlocksRemaining);
                        GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_MODE_RESIZE_SUCCESS_2D, params));
                    }
                }
            } else {
                if (GriefDefenderPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.VOLUME) {
                    final double claimableChunks = claimBlocksRemaining / 65536.0;
                    final Map<String, Object> params = ImmutableMap.of(
                            "chunk-amount", Math.round(claimableChunks * 100.0)/100.0, 
                            "block-amount", claimBlocksRemaining);
                    GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.RESIZE_SUCCESS_3D, params));
                } else {
                    final Map<String, Object> params = ImmutableMap.of(
                            "block-amount", claimBlocksRemaining);
                    GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.RESIZE_SUCCESS_2D, params));
                }
            }
            playerData.revertClaimVisual(claim);
            claim.getVisualizer().resetVisuals();
            final GDClaimVisual visual = claim.getVisualizer();
            if (visual.getVisualTransactions().isEmpty()) {
                visual.createClaimBlockVisuals(PlayerUtil.getInstance().getEyeHeight(player), player.getLocation(), playerData);
            }
            visual.apply(player);
        }
    }
}
