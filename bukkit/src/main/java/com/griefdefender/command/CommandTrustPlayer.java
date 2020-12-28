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

import com.griefdefender.configuration.MessageStorage;
import net.kyori.text.Component;
import net.kyori.text.adapter.bukkit.TextAdapter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.TrustType;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.event.GDUserTrustClaimEvent;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.util.PermissionUtil;

import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_TRUST_PLAYER)
public class CommandTrustPlayer extends BaseCommand {

    @CommandCompletion("@gdplayers @gdtrusttypes @gddummy")
    @CommandAlias("trust")
    @Description("%trust-player")
    @Syntax("<player> [<accessor|builder|container|manager>]")
    @Subcommand("trust player")
    public void execute(Player player, String target, @Optional String type) {
        TrustType trustType = null;
        if (type == null) {
            trustType = TrustTypes.BUILDER;
        } else {
            trustType = CommandHelper.getTrustType(type);
            if (trustType == null) {
                GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().TRUST_INVALID);
                return;
            }
        }

        GDPermissionUser user = null;
        if (target.equalsIgnoreCase("public")) {
            user = GriefDefenderPlugin.PUBLIC_USER;
        } else {
            if (PermissionUtil.getInstance().lookupUserUniqueId(target) == null) {
                GriefDefenderPlugin.sendMessage(player, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.COMMAND_INVALID_PLAYER,
                        ImmutableMap.of(
                        "player", target)));
                return;
            }
            user = PermissionHolderCache.getInstance().getOrCreateUser(target);
        }

        if (user == null) {
            GriefDefenderPlugin.sendMessage(player, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.COMMAND_INVALID_PLAYER,
                    ImmutableMap.of(
                    "player", target)));
            return;
        }
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUID())) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CLAIM_DISABLED_WORLD);
            return;
        }

        // determine which claim the player is standing in
        GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(playerData, player.getLocation());
        if (!claim.getOwnerUniqueId().equals(player.getUniqueId()) && !playerData.canIgnoreClaim(claim) && claim.allowEdit(player) != null) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().PERMISSION_COMMAND_TRUST);
            return;
        }

        if (user.getUniqueId().equals(player.getUniqueId()) && !playerData.canIgnoreClaim(claim) && claim.allowEdit(player) != null) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().TRUST_SELF);
            return;
        }

        if (user != null && claim.getOwnerUniqueId().equals(user.getUniqueId())) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CLAIM_OWNER_ALREADY);
            return;
        } else {
            //check permission here
            if(claim.allowGrantPermission(player) != null) {
                final Component message = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.PERMISSION_TRUST,
                        ImmutableMap.of(
                        "player", claim.getOwnerDisplayName()));
                GriefDefenderPlugin.sendMessage(player, message);
                return;
            }

            if(trustType == TrustTypes.MANAGER) {
                Component denyReason = claim.allowEdit(player);
                if(denyReason != null) {
                    GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().PERMISSION_GRANT);
                    return;
                }
            }
        }

        GDCauseStackManager.getInstance().pushCause(player);
        GDUserTrustClaimEvent.Add
            event =
            new GDUserTrustClaimEvent.Add(claim, ImmutableList.of(user.getUniqueId()), trustType);
        GriefDefender.getEventManager().post(event);
        GDCauseStackManager.getInstance().popCause();
        if (event.cancelled()) {
            TextAdapter.sendComponent(player, event.getMessage().orElse(event.getMessage().orElse(MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.TRUST_PLUGIN_CANCEL,
                    ImmutableMap.of("target", user.getName())))));
            return;
        }

        final List<UUID> trustList = claim.getUserTrustList(trustType);
        if (trustList.contains(user.getUniqueId())) {
            final Component message = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.TRUST_ALREADY_HAS,
                ImmutableMap.of(
                    "target", user.getName(),
                    "type", trustType.getName()));
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }

        trustList.add(user.getUniqueId());
        claim.getInternalClaimData().setRequiresSave(true);
        claim.getInternalClaimData().save();

        final Component message = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.TRUST_GRANT, ImmutableMap.of(
                "target", user.getName(),
                "type", trustType.getName()));
        GriefDefenderPlugin.sendMessage(player, message);
    }
}