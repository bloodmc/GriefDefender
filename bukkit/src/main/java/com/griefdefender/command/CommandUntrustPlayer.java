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
import co.aikar.commands.annotation.Syntax;
import com.griefdefender.configuration.MessageStorage;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.format.TextColor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.event.GDUserTrustClaimEvent;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;

import org.bukkit.entity.Player;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_UNTRUST_PLAYER)
public class CommandUntrustPlayer extends BaseCommand {

    @CommandAlias("untrust")
    @Description("Revokes player access to your claim.")
    @Syntax("<player>")
    @Subcommand("untrust player")
    public void execute(Player player, String target) {
        GDPermissionUser user;
        if (target.equalsIgnoreCase("public")) {
            user = GriefDefenderPlugin.PUBLIC_USER;
        } else {
            user = PermissionHolderCache.getInstance().getOrCreateUser(target);
        }

        System.out.println("user = " + user);
        if (user == null) {
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.COMMAND_INVALID_PLAYER,
                    ImmutableMap.of(
                    "player", target)));
            return;
        }
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUID())) {
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_DISABLED_WORLD));
            return;
        }

        // determine which claim the player is standing in
        GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(playerData, player.getLocation());
        if (!claim.getOwnerUniqueId().equals(player.getUniqueId()) && !playerData.canIgnoreClaim(claim) && claim.allowEdit(player) != null) {
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_COMMAND_TRUST));
            return;
        }

        if (user.getUniqueId().equals(player.getUniqueId()) && !playerData.canIgnoreClaim(claim)) {
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.UNTRUST_SELF));
            return;
        }

        if (user != null && claim.getOwnerUniqueId().equals(user.getUniqueId())) {
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_OWNER_ALREADY));
            return;
        }

        GDCauseStackManager.getInstance().pushCause(player);
        GDUserTrustClaimEvent.Remove
            event =
            new GDUserTrustClaimEvent.Remove(claim, ImmutableList.of(user.getUniqueId()), TrustTypes.NONE);
        GriefDefender.getEventManager().post(event);
        GDCauseStackManager.getInstance().popCause();
        if (event.cancelled()) {
            TextAdapter.sendComponent(player, event.getMessage().orElse(TextComponent.of("Could not untrust user '" + user.getName() + "'. A plugin has denied it.").color(TextColor.RED)));
            return;
        }

        claim.removeAllTrustsFromUser(user.getUniqueId());
        claim.getInternalClaimData().setRequiresSave(true);
        claim.getInternalClaimData().save();

        final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.UNTRUST_INDIVIDUAL_SINGLE_CLAIM,
            ImmutableMap.of(
                "target", user.getName()));
        GriefDefenderPlugin.sendMessage(player, message);
    }
}