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
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.event.GDGroupTrustClaimEvent;
import com.griefdefender.permission.GDPermissionGroup;
import com.griefdefender.permission.GDPermissions;
import net.kyori.text.Component;
import net.kyori.text.adapter.bukkit.TextAdapter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_UNTRUSTALL_GROUP)
public class CommandUntrustGroupAll extends BaseCommand {

    @CommandCompletion("@gdgroups @gdtrusttypes @gddummy")
    @CommandAlias("untrustallgroup")
    @Description("%untrust-group-all")
    @Syntax("<group> [<accessor|builder|container|manager>]")
    @Subcommand("untrustall group")
    public void execute(Player player, String target, @Optional String type) {
        TrustType trustType = null;
        if (type == null) {
            trustType = TrustTypes.NONE;
        } else {
            trustType = CommandHelper.getTrustType(type);
            if (trustType == null) {
                GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().TRUST_INVALID);
                return;
            }
        }

        final GDPermissionGroup group = PermissionHolderCache.getInstance().getOrCreateGroup(target);
        // validate player argument
        if (group == null) {
            GriefDefenderPlugin.sendMessage(player, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.COMMAND_INVALID_GROUP,
                    ImmutableMap.of(
                    "group", target)));
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
        GDGroupTrustClaimEvent.Remove
            event = new GDGroupTrustClaimEvent.Remove(new ArrayList<>(claimList), ImmutableList.of(group.getName()), trustType);
        GriefDefender.getEventManager().post(event);
        GDCauseStackManager.getInstance().popCause();
        if (event.cancelled()) {
            TextAdapter.sendComponent(player, event.getMessage().orElse(MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.TRUST_PLUGIN_CANCEL,
                    ImmutableMap.of("target", group.getName()))));
            return;
        }

        for (Claim claim : claimList) {
            final GDClaim gdClaim = (GDClaim) claim;
            if (trustType == TrustTypes.NONE) {
                this.removeAllGroupTrust(gdClaim, group);
            } else {
                this.removeGroupTrust(gdClaim, group, trustType);
            }
        }

        final Component message = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.UNTRUST_INDIVIDUAL_ALL_CLAIMS,
               ImmutableMap.of(
                "player", group.getName()));
        GriefDefenderPlugin.sendMessage(player, message);
    }

    private void removeGroupTrust(GDClaim claim, GDPermissionGroup group, TrustType type) {
        final List<String> trustList = claim.getGroupTrustList(type);
        if (trustList.remove(group.getName())) {
            claim.getInternalClaimData().setRequiresSave(true);
            claim.getInternalClaimData().save();
        }
        for (Claim child : claim.children) {
            this.removeGroupTrust((GDClaim) child, group, type);
        }
    }

    private void removeAllGroupTrust(Claim claim, GDPermissionGroup holder) {
        GDClaim gdClaim = (GDClaim) claim;
        gdClaim.removeAllTrustsFromGroup(holder.getName());
        gdClaim.getInternalClaimData().setRequiresSave(true);
        gdClaim.getInternalClaimData().save();
        for (Claim child : gdClaim.children) {
            this.removeAllGroupTrust(child, holder);
        }
    }
}
