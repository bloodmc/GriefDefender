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
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.TrustType;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.MessageDataConfig;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.event.GDUserTrustClaimEvent;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;
import net.kyori.text.Component;
import net.kyori.text.adapter.spongeapi.TextAdapter;
import org.spongepowered.api.entity.living.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_TRUSTALL_PLAYER)
public class CommandTrustPlayerAll extends BaseCommand {

    @CommandCompletion("@gdplayers @gdtrusttypes @gddummy")
    @CommandAlias("trustall")
    @Description("%trust-player-all")
    @Syntax("<player> <accessor|builder|container|manager>")
    @Subcommand("trustall player")
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

        GDPermissionUser user;
        if (target.equalsIgnoreCase("public")) {
            user = GriefDefenderPlugin.PUBLIC_USER;
        } else {
            user = PermissionHolderCache.getInstance().getOrCreateUser(target);
        }

        // validate player argument
        if (user == null) {
            GriefDefenderPlugin.sendMessage(player, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.COMMAND_INVALID_PLAYER,
                    ImmutableMap.of(
                    "player", target)));
            return;
        }

        if (user.getUniqueId().equals(player.getUniqueId())) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().TRUST_SELF);
            return;
        }

        GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        Set<Claim> claimList = null;
        if (playerData != null) {
            claimList = playerData.getInternalClaims();
        }

        if (playerData == null || claimList == null || claimList.size() == 0) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().TRUST_NO_CLAIMS);
            return;
        }

        GDCauseStackManager.getInstance().pushCause(player);
        GDUserTrustClaimEvent.Add
            event = new GDUserTrustClaimEvent.Add(new ArrayList<>(claimList), ImmutableList.of(user.getUniqueId()), trustType);
        GriefDefender.getEventManager().post(event);
        GDCauseStackManager.getInstance().popCause();
        if (event.cancelled()) {
            TextAdapter.sendComponent(player, event.getMessage().orElse(MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.TRUST_PLUGIN_CANCEL,
                    ImmutableMap.of("target", user.getName()))));
            return;
        }

        for (Claim claim : claimList) {
            this.addAllUserTrust(claim, user, trustType);
        }

        final Component message = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.TRUST_INDIVIDUAL_ALL_CLAIMS,
                ImmutableMap.of(
                "player", user.getName()));
        GriefDefenderPlugin.sendMessage(player, message);
    }

    private void addAllUserTrust(Claim claim, GDPermissionUser user, TrustType trustType) {
        GDClaim gdClaim = (GDClaim) claim;
        List<UUID> trustList = gdClaim.getUserTrustList(trustType);
        if (!trustList.contains(user.getUniqueId())) {
            trustList.add(user.getUniqueId());
        }

        gdClaim.getInternalClaimData().setRequiresSave(true);
        gdClaim.getInternalClaimData().save();
        for (Claim child : gdClaim.children) {
            this.addAllUserTrust(child, user, trustType);
        }
    }
}
