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
import co.aikar.commands.annotation.Subcommand;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.event.GDDeleteClaimEvent;
import com.griefdefender.permission.GDPermissions;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.format.TextColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_DELETE_CLAIMS)
public class CommandClaimDeleteAll extends BaseCommand {

    @CommandCompletion("@gdplayers @gddummy")
    @CommandAlias("deleteall")
    @Description("Delete all of another player's claims.")
    @Subcommand("delete all")
    public void execute(Player player, OfflinePlayer otherPlayer) {
        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        int originalClaimCount = playerData.getInternalClaims().size();

        if (originalClaimCount == 0) {
            TextAdapter.sendComponent(player, TextComponent.of("Player " + otherPlayer.getName() + " has no claims to delete.", TextColor.RED));
            return;
        }

        GDCauseStackManager.getInstance().pushCause(player);
        GDDeleteClaimEvent event = new GDDeleteClaimEvent(ImmutableList.copyOf(playerData.getInternalClaims()));
        GriefDefender.getEventManager().post(event);
        GDCauseStackManager.getInstance().popCause();
        if (event.cancelled()) {
            GriefDefenderPlugin.sendMessage(player, event.getMessage().orElse(TextComponent.of("Could not delete all claims. A plugin has denied it.").color(TextColor.RED)));
            return;
        }

        GriefDefenderPlugin.getInstance().dataStore.deleteClaimsForPlayer(otherPlayer.getUniqueId());
        final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.DELETE_ALL_PLAYER_SUCCESS, ImmutableMap.of(
                "player", otherPlayer.getName()));
        GriefDefenderPlugin.sendMessage(player, message);
        if (player != null) {
            GriefDefenderPlugin.getInstance().getLogger().info(player.getName() + " deleted all claims belonging to " + otherPlayer.getName() + ".");

            // revert any current visualization
            playerData.revertActiveVisual(player);
        }
    }
}
