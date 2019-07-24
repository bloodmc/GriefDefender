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
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.event.GDUserTrustClaimEvent;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.format.TextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_TRUSTALL_PLAYER)
public class CommandTrustPlayerAll extends BaseCommand {

    @CommandAlias("trustall")
    @Description("Grants a player access to all your claims."
            + "\nAccessor: access to interact with all blocks except inventory."
            + "\nContainer: access to interact with all blocks including inventory."
            + "\nBuilder: access to everything above including ability to place and break blocks."
            + "\nManager: access to everything above including ability to manage claim settings.")
    @Syntax("<player> <accessor|builder|container|manager>")
    @Subcommand("trustall player")
    public void execute(Player player, String target, @Optional String type) {
        TrustType trustType = null;
        if (type == null) {
            trustType = TrustTypes.BUILDER;
        } else {
            trustType = CommandHelper.getTrustType(type);
            if (trustType == null) {
                GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.trustInvalid.toText());
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
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.commandPlayerInvalid
                    .apply(ImmutableMap.of(
                    "player", target)).build());
            return;
        }

        if (user.getUniqueId().equals(player.getUniqueId())) {
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.trustSelf.toText());
            return;
        }

        GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        Set<Claim> claimList = null;
        if (playerData != null) {
            claimList = playerData.getInternalClaims();
        }

        if (playerData == null || claimList == null || claimList.size() == 0) {
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.trustNoClaims.toText());
            return;
        }

        GDCauseStackManager.getInstance().pushCause(player);
        GDUserTrustClaimEvent.Add
            event = new GDUserTrustClaimEvent.Add(new ArrayList<>(claimList), ImmutableList.of(user.getUniqueId()), trustType);
        GriefDefender.getEventManager().post(event);
        GDCauseStackManager.getInstance().popCause();
        if (event.cancelled()) {
            TextAdapter.sendComponent(player, event.getMessage().orElse(TextComponent.of("Could not add trust for user '" + user.getName() + "'. A plugin has denied it.").color(TextColor.RED)));
            return;
        }

        for (Claim claim : claimList) {
            this.addAllUserTrust(claim, user, trustType);
        }

        final Component message = GriefDefenderPlugin.getInstance().messageData.trustIndividualAllClaims
                .apply(ImmutableMap.of(
                "player", user.getName())).build();
        GriefDefenderPlugin.sendMessage(player, message);
    }

    private void addAllUserTrust(Claim claim, GDPermissionUser user, TrustType trustType) {
        GDClaim gpClaim = (GDClaim) claim;
        List<UUID> trustList = gpClaim.getUserTrustList(trustType);
        if (!trustList.contains(user.getUniqueId())) {
            trustList.add(user.getUniqueId());
        }

        gpClaim.getInternalClaimData().setRequiresSave(true);
        for (Claim child : gpClaim.children) {
            this.addAllUserTrust(child, user, trustType);
        }
    }
}
