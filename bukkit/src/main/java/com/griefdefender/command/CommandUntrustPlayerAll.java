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
import java.util.Set;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_UNTRUSTALL_PLAYER)
public class CommandUntrustPlayerAll extends BaseCommand {

    @CommandAlias("untrustall")
    @Description("Revokes player access to all your claims.")
    @Syntax("<player>")
    @Subcommand("untrustall player")
    public void execute(Player player, String target) {
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
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.untrustSelf.toText());
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
        GDUserTrustClaimEvent.Remove
            event = new GDUserTrustClaimEvent.Remove(new ArrayList<>(claimList), ImmutableList.of(user.getUniqueId()), TrustTypes.NONE);
        GriefDefender.getEventManager().post(event);
        GDCauseStackManager.getInstance().popCause();
        if (event.cancelled()) {
            TextAdapter.sendComponent(player, event.getMessage().orElse(TextComponent.of("Could not remove trust for user '" + user.getName() + "'. A plugin has denied it.").color(TextColor.RED)));
            return;
        }

        for (Claim claim : claimList) {
            this.removeAllUserTrust(claim, user);
        }

        final Component message = GriefDefenderPlugin.getInstance().messageData.untrustIndividualAllClaims
                .apply(ImmutableMap.of(
                "player", user.getName())).build();
        GriefDefenderPlugin.sendMessage(player, message);
    }

    private void removeAllUserTrust(Claim claim, GDPermissionUser user) {
        final GDClaim gdClaim = ((GDClaim) claim);
        gdClaim.removeAllTrustsFromUser(user.getUniqueId());
        gdClaim.getInternalClaimData().setRequiresSave(true);
        for (Claim child : gdClaim.children) {
            this.removeAllUserTrust(child, user);
        }
    }
}
