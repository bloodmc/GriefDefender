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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.TrustType;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.event.GDGroupTrustClaimEvent;
import com.griefdefender.permission.GDPermissionGroup;
import com.griefdefender.permission.GDPermissions;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.format.TextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_TRUSTALL_GROUP)
public class CommandTrustGroupAll extends BaseCommand {

    @CommandAlias("trustallgroup")
    @Description("Grants a group access to all your claims."
            + "\nAccessor: access to interact with all blocks except inventory."
            + "\nContainer: access to interact with all blocks including inventory."
            + "\nBuilder: access to everything above including ability to place and break blocks."
            + "\nManager: access to everything above including ability to manage claim settings.")
    @Syntax("<group> <accessor|builder|container|manager>")
    @Subcommand("trustall group")
    public void execute(Player player, String target, String type) {
        final TrustType trustType = CommandHelper.getTrustType(type);
        if (trustType == null) {
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.TRUST_INVALID));
            return;
        }

        final GDPermissionGroup group = PermissionHolderCache.getInstance().getOrCreateGroup(target);

        // validate player argument
        if (group == null) {
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.COMMAND_INVALID_GROUP,
                    ImmutableMap.of(
                    "group", target));
            return;
        }

        GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        Set<Claim> claimList = null;
        if (playerData != null) {
            claimList = playerData.getInternalClaims();
        }

        if (playerData == null || claimList == null || claimList.size() == 0) {
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.TRUST_NO_CLAIMS));
            return;
        }

        GDCauseStackManager.getInstance().pushCause(player);
        GDGroupTrustClaimEvent.Add
            event = new GDGroupTrustClaimEvent.Add(new ArrayList<>(claimList), ImmutableList.of(group.getName()), trustType);
        GriefDefender.getEventManager().post(event);
        GDCauseStackManager.getInstance().popCause();
        if (event.cancelled()) {
            TextAdapter.sendComponent(player, event.getMessage().orElse(TextComponent.of("Could not add trust for group '" + group + "'. A plugin has denied it.", TextColor.RED)));
            return;
        }

        for (Claim claim : claimList) {
            this.addAllGroupTrust(claim, group, trustType);
        }

        final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.TRUST_INDIVIDUAL_ALL_CLAIMS,
                ImmutableMap.of(
                "player", group.getName()));
        GriefDefenderPlugin.sendMessage(player, message);
    }

    private void addAllGroupTrust(Claim claim, GDPermissionGroup holder, TrustType trustType) {
        GDClaim gpClaim = (GDClaim) claim;
        List<String> trustList = gpClaim.getGroupTrustList(trustType);
        if (!trustList.contains(holder.getFriendlyName())) {
            trustList.add(holder.getFriendlyName());
        }

        gpClaim.getInternalClaimData().setRequiresSave(true);
        for (Claim child : gpClaim.children) {
            this.addAllGroupTrust(child, holder, trustType);
        }
    }
}
