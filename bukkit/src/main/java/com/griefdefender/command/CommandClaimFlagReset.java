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
import com.google.common.collect.ImmutableMap;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.permission.Context;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.util.PermissionUtil;
import net.kyori.text.Component;
import org.bukkit.entity.Player;

import java.util.Set;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_FLAGS_RESET)
public class CommandClaimFlagReset extends BaseCommand {

    @CommandAlias("cfr")
    @Description("Resets a claim to flag defaults.")
    @Subcommand("flag reset")
    public void execute(Player player) {
        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        final GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(playerData, player.getLocation());
        final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_CLAIM_RESET_FLAGS,
                ImmutableMap.of(
                "type", claim.getType().getName()));
        if (claim.isWilderness()) {
            if (!player.hasPermission(GDPermissions.MANAGE_WILDERNESS)) {
                GriefDefenderPlugin.sendMessage(player, message);
                return;
            }
        } else if (claim.isAdminClaim()) {
            if (!player.getUniqueId().equals(claim.getOwnerUniqueId()) && !player.hasPermission(GDPermissions.COMMAND_ADMIN_CLAIMS)) {
                GriefDefenderPlugin.sendMessage(player, message);
                return;
            }
        } else if (!player.hasPermission(GDPermissions.COMMAND_ADMIN_CLAIMS) && (claim.isBasicClaim() || claim.isSubdivision()) && !player.getUniqueId().equals(claim.getOwnerUniqueId())) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().PERMISSION_CLAIM_RESET_FLAGS_SELF);
            return;
        }

        // Remove persisted data
        for (Set<Context> contextSet : PermissionUtil.getInstance().getAllPermissions(claim, GriefDefenderPlugin.DEFAULT_HOLDER).keySet()) {
            if (contextSet.contains(claim.getContext())) {
                PermissionUtil.getInstance().clearPermissions(GriefDefenderPlugin.DEFAULT_HOLDER, contextSet);
            }
        }

        GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().FLAG_RESET_SUCCESS);
    }
}
