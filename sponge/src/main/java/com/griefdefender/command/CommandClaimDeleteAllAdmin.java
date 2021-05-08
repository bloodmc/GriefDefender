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

import com.google.common.collect.ImmutableMap;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.claim.ClaimResult;
import com.griefdefender.api.claim.ClaimTypes;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.text.action.GDCallbackHolder;

import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.spongeapi.TextAdapter;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.api.world.World;

import java.util.UUID;
import java.util.function.Consumer;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_DELETE_ADMIN_CLAIMS)
public class CommandClaimDeleteAllAdmin extends BaseCommand {

    @CommandAlias("deletealladmin")
    @Description("%delete-all-admin")
    @Subcommand("delete alladmin")
    public void execute(Player player, @Optional String worldName) {
        WorldProperties worldProperties = null;
        if (worldName != null) {
            worldProperties = Sponge.getServer().getWorldProperties(worldName).orElse(null);
            if (worldProperties == null) {
                TextAdapter.sendComponent(player, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.COMMAND_WORLD_NOT_FOUND,
                        ImmutableMap.of("world", worldName)));
                return;
            }
        }

        Component message = null;
        if (worldProperties != null) {
            message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.DELETE_ALL_TYPE_WARNING_WORLD, ImmutableMap.of(
                    "type", TextComponent.of("ADMIN").color(TextColor.RED),
                    "world", worldProperties.getWorldName()));
        } else {
            message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.DELETE_ALL_TYPE_WARNING, ImmutableMap.of(
                    "type", TextComponent.of("ADMIN").color(TextColor.RED)));
        }
        final Component confirmationText = TextComponent.builder("")
                .append(message)
                .append(TextComponent.builder()
                    .append("\n[")
                    .append(MessageCache.getInstance().LABEL_CONFIRM.color(TextColor.GREEN))
                    .append("]\n")
                    .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(player, createConfirmationConsumer(player, worldProperties), true)))
                    .hoverEvent(HoverEvent.showText(MessageCache.getInstance().UI_CLICK_CONFIRM)).build())
                .build();
        TextAdapter.sendComponent(player, confirmationText);
    }

    private static Consumer<CommandSource> createConfirmationConsumer(Player player, WorldProperties worldProperties) {
        return confirm -> {
            final UUID worldUniqueId = worldProperties != null ? worldProperties.getUniqueId() : null;
            if (worldUniqueId == null) {
                for (World world : Sponge.getServer().getWorlds()) {
                    GriefDefenderPlugin.getInstance().dataStore.deleteAllAdminClaims(player, world.getUniqueId());
                }
            } else {
                ClaimResult claimResult = GriefDefenderPlugin.getInstance().dataStore.deleteAllAdminClaims(player, worldUniqueId);
                if (!claimResult.successful()) {
                    final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_TYPE_NOT_FOUND,
                            ImmutableMap.of(
                            "type", ClaimTypes.ADMIN.getName().toLowerCase()));
                    GriefDefenderPlugin.sendMessage(player, claimResult.getMessage().orElse(message));
                    return;
                }
            }

            Component message = null;
            if (worldProperties != null) {
                message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.DELETE_ALL_TYPE_SUCCESS_WORLD, ImmutableMap.of(
                        "type", TextComponent.of("ADMIN").color(TextColor.RED),
                        "world", worldProperties.getWorldName()));
            } else {
                message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.DELETE_ALL_TYPE_SUCCESS, ImmutableMap.of(
                        "type", TextComponent.of("ADMIN").color(TextColor.RED)));
            }

            TextAdapter.sendComponent(player, message);
            final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
            playerData.onClaimDelete();
        };
    }
}
