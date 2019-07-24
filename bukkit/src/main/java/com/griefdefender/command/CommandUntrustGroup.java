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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.event.GDGroupTrustClaimEvent;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.util.PermissionUtil;
import me.lucko.luckperms.api.Group;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.format.TextColor;

import org.bukkit.entity.Player;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_UNTRUST_GROUP)
public class CommandUntrustGroup extends BaseCommand {

    @CommandAlias("untrustgroup")
    @Description("Revokes group access to your claim.")
    @Subcommand("untrust group")
    public void execute(Player player, String target) {
        final Group group = PermissionUtil.getInstance().getGroupSubject(target);
        if (group == null) {
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.commandGroupInvalid
                    .apply(ImmutableMap.of(
                    "group", target)).build());
            return;
        }

        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUID())) {
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.claimDisabledWorld.toText());
            return;
        }

        // determine which claim the player is standing in
        GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(playerData, player.getLocation());
        if (!playerData.canIgnoreClaim(claim) && claim.allowEdit(player) != null) {
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.permissionCommandTrust.toText());
            return;
        }

        //check permission here
        if(claim.allowGrantPermission(player) != null) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.permissionTrust
                    .apply(ImmutableMap.of(
                    "owner", claim.getOwnerName())).build();
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }

        GDCauseStackManager.getInstance().pushCause(player);
        GDGroupTrustClaimEvent.Remove event =
            new GDGroupTrustClaimEvent.Remove(claim, ImmutableList.of(group.getName()), TrustTypes.NONE);
        GriefDefender.getEventManager().post(event);
        GDCauseStackManager.getInstance().popCause();
        if (event.cancelled()) {
            TextAdapter.sendComponent(player, event.getMessage().orElse(TextComponent.of("Could not untrust group '" + group + "'. A plugin has denied it.").color(TextColor.RED)));
            return;
        }

        claim.removeAllTrustsFromGroup(group.getName());
        claim.getInternalClaimData().setRequiresSave(true);

        final Component message = GriefDefenderPlugin.getInstance().messageData.untrustIndividualSingleClaim
                .apply(ImmutableMap.of(
                "target", group)).build();
        GriefDefenderPlugin.sendMessage(player, message);
    }
}