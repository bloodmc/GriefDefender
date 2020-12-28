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
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaimManager;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.event.GDRemoveClaimEvent;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.text.action.GDCallbackHolder;
import com.griefdefender.util.PermissionUtil;

import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_DELETE_CLAIMS)
public class CommandClaimDeleteAll extends BaseCommand {

    @CommandCompletion("@gdplayers @gdworlds @gddummy")
    @CommandAlias("deleteall")
    @Description("%delete-all")
    @Syntax("<player> [<world>]")
    @Subcommand("delete all")
    public void execute(Player src, String otherPlayer, @Optional String worldName) {
        final UUID playerUniqueId = PermissionUtil.getInstance().lookupUserUniqueId(otherPlayer);
        if (playerUniqueId == null) {
            GriefDefenderPlugin.sendMessage(src, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.COMMAND_INVALID_PLAYER,
                    ImmutableMap.of(
                    "player", otherPlayer)));
            return;
        }

        final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(playerUniqueId);
        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(src.getWorld(), user.getUniqueId());
        int originalClaimCount = playerData.getInternalClaims().size();
        World world = null;
        if (worldName != null) {
            world = Bukkit.getWorld(worldName);
            if (world == null) {
                TextAdapter.sendComponent(src, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.COMMAND_WORLD_NOT_FOUND,
                        ImmutableMap.of("world", worldName)));
                return;
            }
            final GDClaimManager claimManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(world.getUID());
            final Set<Claim> claims = claimManager.getPlayerClaims(user.getUniqueId());
            if (claims == null || claims.isEmpty()) {
                originalClaimCount = 0;
            }
        }

        if (originalClaimCount == 0) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PLAYER_NO_CLAIMS_TO_DELETE, ImmutableMap.of(
                    "player", user.getFriendlyName()));
            TextAdapter.sendComponent(src, message);
            return;
        }

        Component message = null;
        if (world != null) {
            message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.DELETE_ALL_PLAYER_WARNING_WORLD, ImmutableMap.of(
                    "player", TextComponent.of(user.getFriendlyName()).color(TextColor.AQUA),
                    "world", world.getName()));
        } else {
            message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.DELETE_ALL_PLAYER_WARNING, ImmutableMap.of(
                    "player", TextComponent.of(user.getFriendlyName()).color(TextColor.AQUA)));
        }
        final Component confirmationText = TextComponent.builder("")
                .append(message)
                .append(TextComponent.builder()
                    .append("\n[")
                    .append(MessageCache.getInstance().LABEL_CONFIRM.color(TextColor.GREEN))
                    .append("]\n")
                    .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(src, createConfirmationConsumer(src, user, world), true)))
                    .hoverEvent(HoverEvent.showText(MessageCache.getInstance().UI_CLICK_CONFIRM)).build())
                .build();
        TextAdapter.sendComponent(src, confirmationText);
    }

    private static Consumer<CommandSender> createConfirmationConsumer(Player src, GDPermissionUser otherPlayer, World world) {
        return confirm -> {
            GDCauseStackManager.getInstance().pushCause(src);
            Set<Claim> claims;
            if (world != null) {
                final GDClaimManager claimManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(world.getUID());
                claims = new HashSet<>(claimManager.getInternalPlayerClaims(otherPlayer.getUniqueId()));
                final Iterator<Claim> iterator = claims.iterator();
                while (iterator.hasNext()) {
                    final Claim claim = iterator.next();
                    if (!claim.getWorldUniqueId().equals(world.getUID())) {
                        iterator.remove();
                    }
                }
            } else {
                claims = otherPlayer.getInternalPlayerData().getInternalClaims();
            }
            if (claims.isEmpty()) {
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PLAYER_NO_CLAIMS_TO_DELETE, ImmutableMap.of(
                        "player", otherPlayer.getFriendlyName()));
                TextAdapter.sendComponent(src, message);
                return;
            }
            GDRemoveClaimEvent.Delete event = new GDRemoveClaimEvent.Delete(ImmutableList.copyOf(claims));
            GriefDefender.getEventManager().post(event);
            GDCauseStackManager.getInstance().popCause();
            if (event.cancelled()) {
                GriefDefenderPlugin.sendMessage(src, event.getMessage().orElse(MessageCache.getInstance().PLUGIN_EVENT_CANCEL));
                return;
            }
            final UUID worldUniqueId = world != null ? world.getUID() : null;
            GriefDefenderPlugin.getInstance().dataStore.deleteClaimsForPlayer(otherPlayer.getUniqueId(), worldUniqueId);
            otherPlayer.getInternalPlayerData().onClaimDelete();
            if (src != null) {
                Component message = null;
                if (world != null) {
                    message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.DELETE_ALL_PLAYER_SUCCESS_WORLD, ImmutableMap.of(
                            "player", TextComponent.of(otherPlayer.getName()).color(TextColor.AQUA),
                            "world", world.getName()));
                } else {
                    message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.DELETE_ALL_PLAYER_SUCCESS, ImmutableMap.of(
                            "player", TextComponent.of(otherPlayer.getName()).color(TextColor.AQUA)));
                }
                GriefDefenderPlugin.sendMessage(src, message);
            }
        };
    }
}
