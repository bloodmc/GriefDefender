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
package com.griefdefender.command.gphelper;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import net.kyori.text.Component;

import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.api.permission.flag.Flags;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.permission.option.GDOptions;
import com.griefdefender.util.PlayerUtil;

import java.time.Duration;
import java.time.Instant;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_TRAPPED)
public class CommandTrapped extends BaseCommand {

    @CommandAlias("trapped")
    @Description("%trapped")
    @Subcommand("player trapped")
    public void execute(Player player) {
        final GameMode gameMode = player.getGameMode();
        if (gameMode == GameMode.SPECTATOR) {
            return;
        }

        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        final GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(playerData, player.getLocation());

        if (playerData.inPvpCombat()) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().COMMAND_TRAPPED_PVP_COMBAT);
            return;
        }
        if (player.getUniqueId().equals(claim.getOwnerUniqueId()) || claim.isUserTrusted(player, TrustTypes.BUILDER)) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().COMMAND_TRAPPED_BUILD_ACCESS);
            return;
        }

        final Instant now = Instant.now();
        final int cooldown = GriefDefenderPlugin.getActiveConfig(player.getWorld()).getConfig().claim.trappedCooldown;
        if (playerData.lastTrappedTimestamp != null && !playerData.lastTrappedTimestamp.plusSeconds(cooldown).isBefore(now)) {
            final int duration = (int) Duration.between(playerData.lastTrappedTimestamp, now).getSeconds();
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.COMMAND_TRAPPED_CANCEL_COOLDOWN, ImmutableMap.of(
                    "time-remaining", cooldown - duration));
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }

        playerData.lastTrappedTimestamp = null;
        // check place
        boolean canBuild = true;
        final Tristate placeResult = GDPermissionManager.getInstance().getFinalPermission(null, player.getLocation(), claim, Flags.BLOCK_PLACE, player, player.getLocation(), player, TrustTypes.BUILDER, true);
        if (placeResult == Tristate.FALSE) {
            canBuild = false;
        } else {
            // check break
            final Tristate breakResult = GDPermissionManager.getInstance().getFinalPermission(null, player.getLocation(), claim, Flags.BLOCK_BREAK, player, player.getLocation(), player, TrustTypes.BUILDER, true);
            if (breakResult == Tristate.FALSE) {
                canBuild = false;
            }
        }
        if (canBuild) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().COMMAND_TRAPPED_BUILD_ACCESS);
            return;
        }

        playerData.teleportLocation = PlayerUtil.getInstance().getSafeClaimLocation(claim);
        int teleportDelay = 0;
        if (GDOptions.PLAYER_TELEPORT_DELAY) {
            teleportDelay = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), player, Options.PLAYER_TELEPORT_DELAY, claim);
        }

        if (teleportDelay > 0) {
            playerData.trappedRequest = true;
            playerData.teleportDelay = teleportDelay + 1;
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.COMMAND_TRAPPED_REQUEST, ImmutableMap.of(
                    "time-remaining", teleportDelay));
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }

        playerData.lastTrappedTimestamp = now;
        player.teleport(playerData.teleportLocation);
        playerData.teleportLocation = null;
        GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().COMMAND_TRAPPED_SUCCESS);
    }
}
