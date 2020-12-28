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

import org.bukkit.entity.Player;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.event.GDUserTrustClaimEvent;
import com.griefdefender.permission.GDPermissionUser;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import net.kyori.text.Component;
import net.kyori.text.adapter.bukkit.TextAdapter;

public class CommandAccessTrust extends BaseCommand {

    @CommandCompletion("@gdplayers")
    @CommandAlias("at|accesstrust")
    @Description("%trust-access")
    @Syntax("<player>")
    public void execute(Player src, String target) {

        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(src.getWorld().getUID())) {
            GriefDefenderPlugin.sendMessage(src, MessageCache.getInstance().CLAIM_DISABLED_WORLD);
            return;
        }

        GDPermissionUser user = null;
        if (target.equalsIgnoreCase("public")) {
            user = GriefDefenderPlugin.PUBLIC_USER;
        } else {
            user = PermissionHolderCache.getInstance().getOrCreateUser(target);
        }

        if (user == null) {
            GriefDefenderPlugin.sendMessage(src, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.COMMAND_INVALID_PLAYER,
                    ImmutableMap.of(
                    "player", target)));
            return;
        }

        // determine which claim the player is standing in
        GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(src.getWorld(), src.getUniqueId());
        GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(playerData, src.getLocation());
        if (!claim.getOwnerUniqueId().equals(src.getUniqueId()) && !playerData.canIgnoreClaim(claim) && claim.allowEdit(src) != null) {
            GriefDefenderPlugin.sendMessage(src, MessageCache.getInstance().PERMISSION_COMMAND_TRUST);
            return;
        }

        if (user.getUniqueId().equals(src.getUniqueId()) && !playerData.canIgnoreClaim(claim)) {
            GriefDefenderPlugin.sendMessage(src, MessageCache.getInstance().TRUST_SELF);
            return;
        }

        if (user != null && claim.getOwnerUniqueId().equals(user.getUniqueId())) {
            GriefDefenderPlugin.sendMessage(src, MessageCache.getInstance().CLAIM_OWNER_ALREADY);
            return;
        } else {
            //check permission here
            if(claim.allowGrantPermission(src) != null) {
                final Component message = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.PERMISSION_TRUST,
                        ImmutableMap.of(
                        "player", claim.getOwnerDisplayName()));
                GriefDefenderPlugin.sendMessage(src, message);
                return;
            }
        }

        GDCauseStackManager.getInstance().pushCause(src);
        GDUserTrustClaimEvent.Add
            event =
            new GDUserTrustClaimEvent.Add(claim, ImmutableList.of(user.getUniqueId()), TrustTypes.ACCESSOR);
        GriefDefender.getEventManager().post(event);
        GDCauseStackManager.getInstance().popCause();
        if (event.cancelled()) {
            TextAdapter.sendComponent(src, event.getMessage().orElse(event.getMessage().orElse(MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.TRUST_PLUGIN_CANCEL,
                    ImmutableMap.of("target", user.getName())))));
            return;
        }

        claim.addUserTrust(user.getUniqueId(), TrustTypes.ACCESSOR);
        claim.getInternalClaimData().setRequiresSave(true);
        claim.getInternalClaimData().save();

        final Component message = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.TRUST_GRANT, ImmutableMap.of(
                "target", user.getName(),
                "type", TrustTypes.ACCESSOR.getName()));
        GriefDefenderPlugin.sendMessage(src, message);
    }
}
