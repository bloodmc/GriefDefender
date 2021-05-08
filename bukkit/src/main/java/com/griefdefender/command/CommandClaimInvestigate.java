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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.api.permission.option.type.CreateModeTypes;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.internal.provider.GDWorldEditProvider;
import com.griefdefender.internal.visual.GDClaimVisual;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.util.BlockUtil;
import com.griefdefender.util.PlayerUtil;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import net.kyori.text.Component;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_CLAIM_INVESTIGATE)
public class CommandClaimInvestigate extends BaseCommand {

    @CommandAlias("claiminvestigate")
    @Description("%claim-investigate")
    @Syntax("[area|hide|hideall]")
    @Subcommand("claim investigate")
    public void execute(Player player, @Optional String cmd) {
        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        if (!playerData.queuedVisuals.isEmpty()) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.VISUAL_UPDATE_IN_PROGRESS,
                    ImmutableMap.of(
                    "count", playerData.queuedVisuals.size()));
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }

        final GDWorldEditProvider worldEditProvider = GriefDefenderPlugin.getInstance().getWorldEditProvider();
        if (cmd != null && cmd.equalsIgnoreCase("hideall")) {
            if (worldEditProvider != null) {
                worldEditProvider.revertVisuals(player, playerData, null);
            }
            playerData.revertAllVisuals();
            return;
        }
        final boolean hideTargetClaimVisual = cmd != null && cmd.equalsIgnoreCase("hide");
        final boolean checkArea = cmd != null && cmd.equalsIgnoreCase("area");
        GDClaim claim = null;
        Location location = null;
        if (hideTargetClaimVisual || checkArea) {
            final int maxDistance = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), player, Options.RADIUS_INSPECT);
            claim = PlayerUtil.getInstance().findNearbyClaim(player, playerData, maxDistance, hideTargetClaimVisual);
            if (checkArea) {
                if (!playerData.canIgnoreClaim(claim) && !player.hasPermission(GDPermissions.VISUALIZE_CLAIMS_NEARBY)) {
                    GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().PERMISSION_VISUAL_CLAIMS_NEARBY);
                    return;
                }

                Location nearbyLocation = playerData.lastValidInspectLocation != null ? playerData.lastValidInspectLocation : player.getLocation();
                Set<Claim> claims = BlockUtil.getInstance().getNearbyClaims(nearbyLocation, maxDistance, true);
                List<Claim> visualClaims = new ArrayList<>();
                for (Claim nearbyClaim : claims) {
                    if (!((GDClaim) nearbyClaim).hasActiveVisual(player)) {
                        visualClaims.add(nearbyClaim);
                    }
                }
                int height = (int) (playerData.lastValidInspectLocation != null ? playerData.lastValidInspectLocation.getBlockY() : PlayerUtil.getInstance().getEyeHeight(player));

                boolean hideBorders = worldEditProvider != null &&
                                      worldEditProvider.hasCUISupport(player) &&
                                      GriefDefenderPlugin.getActiveConfig(player.getWorld().getUID()).getConfig().visual.hideBorders;
                if (!hideBorders) {
                    for (Claim visualClaim : visualClaims) {
                        final GDClaimVisual visual = ((GDClaim) visualClaim).getVisualizer();
                        visual.createClaimBlockVisuals(playerData.getClaimCreateMode() == CreateModeTypes.VOLUME ? height : PlayerUtil.getInstance().getEyeHeight(player), player.getLocation(), playerData);
                        visual.apply(player);
                    }
                }

                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_SHOW_NEARBY,
                        ImmutableMap.of(
                        "amount", claims.size()));
                GriefDefenderPlugin.sendMessage(player, message);
                if (!claims.isEmpty()) {
                    if (worldEditProvider != null && !visualClaims.isEmpty()) {
                         worldEditProvider.visualizeClaims(visualClaims, player, playerData, true);
                     }
                    CommandHelper.showClaims(player, claims);
                }
                return;
            }
            if (claim != null && claim.isWilderness()) {
                playerData.lastValidInspectLocation = null;
                GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.BLOCK_NOT_CLAIMED));
                return;
            }
        } else {
            boolean ignoreAir = false;
            if (worldEditProvider != null) {
                // Ignore air so players can use client-side WECUI block target which uses max reach distance
                if (worldEditProvider.hasCUISupport(player) && playerData.getClaimCreateMode() == CreateModeTypes.VOLUME && playerData.lastShovelLocation != null) {
                    ignoreAir = true;
                }
            }
            final int distance = !ignoreAir ? 100 : 5;
            location = BlockUtil.getInstance().getTargetBlock(player, playerData, distance, ignoreAir).orElse(null);
            if (location == null) {
                return;
            }

            claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(location, playerData, true);
            if (claim.isWilderness()) {
                GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.BLOCK_NOT_CLAIMED));
                return;
            }
        }

        // Handle left-click visual revert
        if (claim != null && !claim.isWilderness() && hideTargetClaimVisual) {
            if (claim.hasActiveVisual(player)) {
                final int maxDistance = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), player, Options.RADIUS_INSPECT);
                if (!((GDClaim) claim).children.isEmpty()) {
                    claim = PlayerUtil.getInstance().findNearbyClaim(player, playerData, maxDistance, true);
                }
                if (!claim.hasActiveVisual(player) && claim.parent != null) {
                    GDClaim parent = claim.parent;
                    while (parent != null) {
                        if (parent.hasActiveVisual(player)) {
                            claim = parent;
                            parent = null;
                        } else {
                            parent = parent.parent;
                        }
                    }
                }
                if (claim != null && claim.hasActiveVisual(player)) {
                   playerData.revertClaimVisual(claim);
                }
                return;
            }
        }

        int height = PlayerUtil.getInstance().getEyeHeight(player);
        if (playerData.lastValidInspectLocation != null || location != null) {
            height = playerData.lastValidInspectLocation != null ? playerData.lastValidInspectLocation.getBlockY() : location.getBlockY();
        }

        if (claim != null) {
            // always show visual borders for resize purposes
            final GDClaimVisual visual = claim.getVisualizer();
            playerData.isInvestigating = true;
            visual.createClaimBlockVisuals(playerData.getClaimCreateMode() == CreateModeTypes.VOLUME ? height : PlayerUtil.getInstance().getEyeHeight(player), player.getLocation(), playerData);
            visual.apply(player);
            playerData.isInvestigating = false;
            Set<Claim> claims = new HashSet<>();
            claims.add(claim);
            playerData.showNoClaimsFoundMessage = false;
            CommandHelper.showClaims(player, claims);
            Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.BLOCK_CLAIMED,
                    ImmutableMap.of(
                    "player", claim.getOwnerDisplayName()));
            GriefDefenderPlugin.sendMessage(player, message);
        }
    }
}
