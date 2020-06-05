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
import co.aikar.commands.annotation.Syntax;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.event.GDRemoveClaimEvent;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.text.action.GDCallbackHolder;

import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;

import java.util.Set;
import java.util.function.Consumer;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_DELETE_CLAIMS)
public class CommandClaimDeleteAll extends BaseCommand {

    @CommandCompletion("@gdplayers @gddummy")
    @CommandAlias("deleteall")
    @Description("Delete all of another player's claims.")
    @Syntax("<player>")
    @Subcommand("delete all")
    public void execute(Player src, OfflinePlayer otherPlayer) {
        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(src.getWorld(), otherPlayer.getUniqueId());
        int originalClaimCount = playerData.getInternalClaims().size();

        if (originalClaimCount == 0) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PLAYER_NO_CLAIMS_TO_DELETE, ImmutableMap.of(
                    "player", otherPlayer.getName()));
            TextAdapter.sendComponent(src, message);
            return;
        }

        final Component confirmationText = TextComponent.builder("")
                .append(GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.DELETE_ALL_PLAYER_WARNING, 
                        ImmutableMap.of("player", TextComponent.of(otherPlayer.getName()).color(TextColor.AQUA))))
                .append(TextComponent.builder()
                    .append("\n[")
                    .append(MessageCache.getInstance().LABEL_CONFIRM.color(TextColor.GREEN))
                    .append("]\n")
                    .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(src, createConfirmationConsumer(src, otherPlayer, playerData), true)))
                    .hoverEvent(HoverEvent.showText(MessageCache.getInstance().UI_CLICK_CONFIRM)).build())
                .build();
        TextAdapter.sendComponent(src, confirmationText);
    }

    private static Consumer<CommandSender> createConfirmationConsumer(Player src, OfflinePlayer otherPlayer, GDPlayerData playerData) {
        return confirm -> {
            GDCauseStackManager.getInstance().pushCause(src);
            GDRemoveClaimEvent.Delete event = new GDRemoveClaimEvent.Delete(ImmutableList.copyOf(playerData.getInternalClaims()));
            GriefDefender.getEventManager().post(event);
            GDCauseStackManager.getInstance().popCause();
            if (event.cancelled()) {
                GriefDefenderPlugin.sendMessage(src, event.getMessage().orElse(MessageCache.getInstance().PLUGIN_EVENT_CANCEL));
                return;
            }

            GriefDefenderPlugin.getInstance().dataStore.deleteClaimsForPlayer(otherPlayer.getUniqueId());
            playerData.onClaimDelete();
            if (src != null) {
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.DELETE_ALL_PLAYER_SUCCESS, ImmutableMap.of(
                        "player", TextComponent.of(otherPlayer.getName()).color(TextColor.AQUA)));
                GriefDefenderPlugin.sendMessage(src, message);
            }
        };
    }
}
