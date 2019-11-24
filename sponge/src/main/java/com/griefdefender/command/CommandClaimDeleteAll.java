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
import com.griefdefender.cache.MessageCache;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.event.GDRemoveClaimEvent;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.text.action.GDCallbackHolder;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.spongeapi.TextAdapter;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;

import java.util.function.Consumer;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_DELETE_CLAIMS)
public class CommandClaimDeleteAll extends BaseCommand {

    @CommandCompletion("@gdplayers @gddummy")
    @CommandAlias("deleteall")
    @Description("Delete all of another player's claims.")
    @Syntax("<player>")
    @Subcommand("delete all")
    public void execute(Player src, User otherPlayer) {
        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(src.getWorld(), otherPlayer.getUniqueId());
        int originalClaimCount = playerData.getInternalClaims().size();

        if (originalClaimCount == 0) {
            TextAdapter.sendComponent(src, TextComponent.of("Player " + otherPlayer.getName() + " has no claims to delete.", TextColor.RED));
            return;
        }

        final Component confirmationText = TextComponent.builder("")
                .append(GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.DELETE_ALL_PLAYER_WARNING, 
                        ImmutableMap.of("player", TextComponent.of(otherPlayer.getName()).color(TextColor.AQUA))))
                .append(TextComponent.builder()
                    .append("\n[")
                    .append("Confirm", TextColor.GREEN)
                    .append("]\n")
                    .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createConfirmationConsumer(src, otherPlayer, playerData))))
                    .hoverEvent(HoverEvent.showText(MessageCache.getInstance().UI_CLICK_CONFIRM)).build())
                .build();
        TextAdapter.sendComponent(src, confirmationText);
    }

    private static Consumer<CommandSource> createConfirmationConsumer(Player src, User otherPlayer, GDPlayerData playerData) {
        return confirm -> {
            GDCauseStackManager.getInstance().pushCause(src);
            GDRemoveClaimEvent event = new GDRemoveClaimEvent(ImmutableList.copyOf(playerData.getInternalClaims()));
            GriefDefender.getEventManager().post(event);
            GDCauseStackManager.getInstance().popCause();
            if (event.cancelled()) {
                GriefDefenderPlugin.sendMessage(src, event.getMessage().orElse(MessageCache.getInstance().PLUGIN_EVENT_CANCEL));
                return;
            }

            GriefDefenderPlugin.getInstance().dataStore.deleteClaimsForPlayer(otherPlayer.getUniqueId());
            if (src != null) {
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.DELETE_ALL_PLAYER_SUCCESS, ImmutableMap.of(
                        "player", TextComponent.of(otherPlayer.getName()).color(TextColor.AQUA)));
                GriefDefenderPlugin.sendMessage(src, message);
                // revert any current visualization
                playerData.revertActiveVisual(src);
            }
        };
    }
}
