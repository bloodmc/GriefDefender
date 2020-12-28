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
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.TrustType;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.api.permission.Context;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.event.GDGroupTrustClaimEvent;
import com.griefdefender.permission.GDPermissionGroup;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.util.PermissionUtil;
import net.kyori.text.Component;
import net.kyori.text.adapter.bukkit.TextAdapter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.entity.Player;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_TRUST_GROUP)
public class CommandTrustGroup extends BaseCommand {

    @CommandCompletion("@gdgroups @gdtrusttypes @gddummy")
    @CommandAlias("trustgroup")
    @Description("%trust-group")
    @Syntax("<group> [<accessor|builder|container|manager>]")
    @Subcommand("trust group")
    public void execute(Player player, String groupName, @Optional String type) {
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

        final GDPermissionGroup group = PermissionHolderCache.getInstance().getOrCreateGroup(groupName);
        if (group == null) {
            GriefDefenderPlugin.sendMessage(player, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.COMMAND_INVALID_GROUP, ImmutableMap.of(
                    "group", groupName)));
            return;
        }

        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUID())) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CLAIM_DISABLED_WORLD);
            return;
        }

        // determine which claim the player is standing in
        GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(playerData, player.getLocation());
        if (!playerData.canIgnoreClaim(claim) && claim.allowEdit(player) != null) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().PERMISSION_COMMAND_TRUST);
            return;
        }

        //check permission here
        if(claim.allowGrantPermission(player) != null) {
            final Component message = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.PERMISSION_TRUST,
                    ImmutableMap.of(
                    "owner", claim.getOwnerDisplayName()));
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }

        GDCauseStackManager.getInstance().pushCause(player);
        GDGroupTrustClaimEvent.Remove event =
            new GDGroupTrustClaimEvent.Remove(claim, ImmutableList.of(group.getName()), TrustTypes.NONE);
        GriefDefender.getEventManager().post(event);
        GDCauseStackManager.getInstance().popCause();
        if (event.cancelled()) {
            TextAdapter.sendComponent(player, event.getMessage().orElse(MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.TRUST_PLUGIN_CANCEL,
                    ImmutableMap.of("target", group))));
            return;
        }

        final String permission = CommandHelper.getTrustPermission(trustType);
        Set<Context> contexts = new HashSet<>(); 
        contexts.add(claim.getContext());
        final List<String> groupTrustList = claim.getGroupTrustList(trustType);
        if (!groupTrustList.contains(group.getName())) {
            groupTrustList.add(group.getName());
        } else {
            final Component message = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.TRUST_ALREADY_HAS,
                ImmutableMap.of(
                    "target", group.getName(),
                    "type", trustType.getName()));
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }

        PermissionUtil.getInstance().setPermissionValue(group, permission, Tristate.TRUE, contexts);
        claim.getInternalClaimData().setRequiresSave(true);
        claim.getInternalClaimData().save();

        final Component message = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.TRUST_GRANT, ImmutableMap.of(
                "target", group.getName(),
                "type", trustType.getName()));
        GriefDefenderPlugin.sendMessage(player, message);
    }
}