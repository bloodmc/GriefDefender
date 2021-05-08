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

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimResult;
import com.griefdefender.api.claim.ClaimResultType;
import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.api.claim.ClaimTypes;
import com.griefdefender.api.permission.ResultTypes;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.api.permission.option.type.CreateModeTypes;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.internal.visual.GDClaimVisual;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.registry.ClaimTypeRegistryModule;
import com.griefdefender.util.EconomyUtil;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import net.kyori.text.Component;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.CLAIM_CREATE)
public class CommandClaimCreate extends BaseCommand {

    @CommandCompletion("@gddummy @gdclaimtypes @gddummy")
    @CommandAlias("claimcreate")
    @Description("%claim-create")
    @Syntax("<radius> [type]")
    @Subcommand("claim create")
    public void execute(Player player, int radius, @Optional String type) {
        final Location location = player.getLocation();
        final World world = location.getWorld();
        final int minClaimLevel = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), player, Options.MIN_LEVEL);
        final int maxClaimLevel = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), player, Options.MAX_LEVEL);
        if (location.getBlockY() < minClaimLevel || location.getBlockY() > maxClaimLevel) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_CHEST_OUTSIDE_LEVEL,
                    ImmutableMap.of(
                    "min-level", minClaimLevel,
                    "max-level", maxClaimLevel));
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }

        final int radiusLimit = GriefDefenderPlugin.getGlobalConfig().getConfig().claim.claimCreateRadiusLimit;
        if (radius > radiusLimit) {
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CREATE_FAILED_RESULT,
                    ImmutableMap.of("reason", "Radius exceeds limit of " + radiusLimit + ".")));
            return;
        }

        final Vector3i lesserBoundary = new Vector3i(
                location.getBlockX() - radius,
                minClaimLevel,
                location.getBlockZ() - radius);
        final Vector3i greaterBoundary = new Vector3i(
            location.getBlockX() + radius,
            maxClaimLevel,
            location.getBlockZ() + radius);

        final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(player);
        final GDPlayerData playerData = user.getInternalPlayerData();
        final ClaimType claimType = ClaimTypeRegistryModule.getInstance().getById(type).orElse(ClaimTypes.BASIC);
        if (claimType == ClaimTypes.WILDERNESS) {
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CREATE_FAILED_RESULT,
                    ImmutableMap.of("reason", ResultTypes.TARGET_NOT_VALID.getName())));
            return;
        }
        if (claimType == ClaimTypes.ADMIN && !playerData.ignoreAdminClaims && !playerData.canManageAdminClaims) {
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CREATE_FAILED_RESULT,
                    ImmutableMap.of("reason", ResultTypes.TARGET_NOT_VALID.getName())));
            return;
        }

        final boolean cuboid = playerData.getClaimCreateMode() == CreateModeTypes.VOLUME;
        if ((claimType == ClaimTypes.BASIC || claimType == ClaimTypes.TOWN) && GriefDefenderPlugin.getGlobalConfig().getConfig().economy.economyMode) {
            EconomyUtil.getInstance().economyCreateClaimConfirmation(player, playerData, location.getBlockY(), lesserBoundary, greaterBoundary, claimType,
                    cuboid, playerData.claimSubdividing);
            return;
        }

        final ClaimResult result = GriefDefender.getRegistry().createBuilder(Claim.Builder.class)
                .bounds(lesserBoundary, greaterBoundary)
                .cuboid(cuboid)
                .owner(user.getUniqueId())
                .type(claimType)
                .world(world.getUID())
                .build();
        GDClaim gdClaim = (GDClaim) result.getClaim().orElse(null);
        if (!result.successful()) {
            if (result.getResultType() == ClaimResultType.OVERLAPPING_CLAIM) {
                GDClaim overlapClaim = (GDClaim) result.getClaim().get();
                GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CREATE_OVERLAP_SHORT);
                Set<Claim> claims = new HashSet<>();
                claims.add(overlapClaim);
                CommandHelper.showOverlapClaims(player, claims, location.getBlockY());
            } else if (result.getResultType() == ClaimResultType.CLAIM_EVENT_CANCELLED) {
                GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CREATE_CANCEL);
            } else {
                GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CREATE_FAILED_RESULT,
                        ImmutableMap.of("reason", result.getResultType().name())));
            }
            return;
        } else {
            playerData.lastShovelLocation = null;
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CREATE_SUCCESS,
                    ImmutableMap.of(
                    "type", gdClaim.getFriendlyNameType(true)));
            GriefDefenderPlugin.sendMessage(player, message);
            final GDClaimVisual visual = gdClaim.getVisualizer();
            if (visual.getVisualTransactions().isEmpty()) {
                visual.createClaimBlockVisuals(location.getBlockY(), player.getLocation(), playerData);
            }
            visual.apply(player, false);
        }
    }
}
