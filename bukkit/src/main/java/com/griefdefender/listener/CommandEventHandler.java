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
package com.griefdefender.listener;

import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GDTimings;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.api.permission.flag.Flags;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.flag.GDFlags;
import com.griefdefender.storage.BaseStorage;
import com.griefdefender.util.PaginationUtil;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.RemoteServerCommandEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Iterator;

public class CommandEventHandler implements Listener {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault());
    private BaseStorage dataStore;

    public CommandEventHandler(BaseStorage dataStore) {
        this.dataStore = dataStore;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onServerCommand(ServerCommandEvent event) {
        //CauseTracker.getInstance().getCauseStack().add(event.getSender());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onRemoteServerCommand(RemoteServerCommandEvent event) {
       // CauseTracker.getInstance().getCauseStack().add(event.getSender());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChatPost(AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        final GDPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        final Iterator<Player> iterator = event.getRecipients().iterator();
        // check for command input
        if (playerData.isWaitingForInput()) {
            playerData.commandInput = event.getMessage();
            playerData.commandConsumer.accept(player);
            event.setCancelled(true);
            return;
        }

        while (iterator.hasNext()) {
            final Player receiver = iterator.next();
            if (receiver == player) {
                continue;
            }

            final GDPlayerData receiverData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(receiver.getWorld(), receiver.getUniqueId());
            if (receiverData.isRecordingChat()) {
                iterator.remove();
                final String s = String.format(event.getFormat(), event.getPlayer().getDisplayName(), event.getMessage());
                final Component message = LegacyComponentSerializer.legacy().deserialize(s, '&');
                final Component component = TextComponent.builder()
                        .append(TextComponent.builder()
                                .append(message)
                                .hoverEvent(HoverEvent.showText(TextComponent.of(formatter.format(Instant.now()))))
                                .build())
                        .build();
                receiverData.chatLines.add(component);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (!GDFlags.COMMAND_EXECUTE && !GDFlags.COMMAND_EXECUTE_PVP) {
            return;
        }

        final Player player = event.getPlayer();
        GDCauseStackManager.getInstance().pushCause(player);
        final boolean commandExecuteSourceBlacklisted = GriefDefenderPlugin.isSourceIdBlacklisted(Flags.COMMAND_EXECUTE.getName(), player, player.getWorld().getUID());
        final boolean commandExecutePvpSourceBlacklisted = GriefDefenderPlugin.isSourceIdBlacklisted(Flags.COMMAND_EXECUTE_PVP.getName(), player, player.getWorld().getUID());

        GDTimings.PLAYER_COMMAND_EVENT.startTiming();
        String message = event.getMessage();
        String arguments = "";
        String command = "";
        if (!message.contains(" ")) {
            command = message.replace("/", "");
        } else {
            command = message.substring(0, message.indexOf(" ")).replace("/", "");
            arguments = message.substring(message.indexOf(" ") + 1, message.length());
        }

        String[] args = arguments.split(" ");
        String[] parts = message.split(":");
        String pluginId = null;

        if (parts.length > 1) {
            pluginId = parts[0].replace("/", "");
            command = command.replace(pluginId + ":", "");
        }

        if (pluginId == null || !pluginId.equals("minecraft")) {
            PluginCommand pluginCommand = Bukkit.getPluginCommand(command);
            if (pluginCommand != null) {
                pluginId = pluginCommand.getPlugin().getName().toLowerCase();
            }
            if (pluginId == null) {
                pluginId = "minecraft";
            }
        }

        PaginationUtil.getInstance().updateActiveCommand(player.getUniqueId(), command, arguments);
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUID())) {
            GDTimings.PLAYER_COMMAND_EVENT.stopTiming();
            return;
        }

        GDPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        // if requires access trust, check for permission
        Location location = player.getLocation();
        GDClaim claim = this.dataStore.getClaimAtPlayer(playerData, location);
        if (playerData.canIgnoreClaim(claim)) {
            GDTimings.PLAYER_COMMAND_EVENT.stopTiming();
            return;
        }

        final int combatTimeRemaining = playerData.getPvpCombatTimeRemaining();
        final boolean inPvpCombat = combatTimeRemaining > 0;
        final boolean pvpCombatCommand = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Boolean.class), player, Options.PVP_COMBAT_COMMAND);
        if (!pvpCombatCommand && inPvpCombat) {
            final Component denyMessage = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PVP_IN_COMBAT_NOT_ALLOWED,
                    ImmutableMap.of(
                    "time-remaining", combatTimeRemaining));
            GriefDefenderPlugin.sendMessage(player, denyMessage);
            event.setCancelled(true);
            GDTimings.PLAYER_COMMAND_EVENT.stopTiming();
            return;
        }

        String commandBaseTarget = pluginId + ":" + command;
        String commandTargetWithArgs = commandBaseTarget;
        // first check the args
        for (String arg : args) {
            if (!arg.isEmpty()) {
                commandTargetWithArgs = commandTargetWithArgs + "." + arg;
            }
        }

        boolean commandExecuteTargetBlacklisted = false;
        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.COMMAND_EXECUTE.getName(), commandBaseTarget, player.getWorld().getUID())) {
            commandExecuteTargetBlacklisted = true;
        } else if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.COMMAND_EXECUTE.getName(), commandTargetWithArgs, player.getWorld().getUID())) {
            commandExecuteTargetBlacklisted = true;
        }

        if (GDFlags.COMMAND_EXECUTE && !inPvpCombat && !commandExecuteSourceBlacklisted && !commandExecuteTargetBlacklisted) {
            // First check base command
            Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, player.getLocation(), claim, Flags.COMMAND_EXECUTE, player, commandBaseTarget, player, TrustTypes.MANAGER, true);
            if (result != Tristate.FALSE && args.length > 0) {
                // Check with args
                // Test with each arg, break once result returns false
                String commandBaseTargetArgCheck = commandBaseTarget;
                for (String arg : args) {
                    if (!arg.isEmpty()) {
                        commandBaseTargetArgCheck = commandBaseTargetArgCheck + "." + arg;
                        result = GDPermissionManager.getInstance().getFinalPermission(event, player.getLocation(), claim, Flags.COMMAND_EXECUTE, player, commandBaseTargetArgCheck, player, TrustTypes.MANAGER, true);
                        if (result == Tristate.FALSE) {
                            break;
                        }

                    }
                }
            }
            if (result == Tristate.FALSE) {
                final Component denyMessage = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.COMMAND_BLOCKED,
                        ImmutableMap.of(
                        "command", command,
                        "player", claim.getOwnerName()));
                GriefDefenderPlugin.sendMessage(player, denyMessage);
                event.setCancelled(true);
                GDTimings.PLAYER_COMMAND_EVENT.stopTiming();
                return;
            }

            GDTimings.PLAYER_COMMAND_EVENT.stopTiming();
            return;
        }
        if (GDFlags.COMMAND_EXECUTE_PVP && inPvpCombat && !commandExecuteSourceBlacklisted && !commandExecuteTargetBlacklisted) {
            // First check base command
            Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, player.getLocation(), claim, Flags.COMMAND_EXECUTE_PVP, player, commandBaseTarget, player, true);
            if (result != Tristate.FALSE && args.length > 0) {
                // check with args
                // Test with each arg, break once result returns false
                String commandBaseTargetArgCheck = commandBaseTarget;
                for (String arg : args) {
                    if (!arg.isEmpty()) {
                        commandBaseTargetArgCheck = commandBaseTargetArgCheck + "." + arg;
                        result = GDPermissionManager.getInstance().getFinalPermission(event, player.getLocation(), claim, Flags.COMMAND_EXECUTE_PVP, player, commandBaseTargetArgCheck, player, true);
                        if (result == Tristate.FALSE) {
                            break;
                        }
                    }
                }
            }
            if (result == Tristate.FALSE) {
                final Component denyMessage = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.COMMAND_BLOCKED,
                        ImmutableMap.of(
                        "command", command,
                        "player", claim.getOwnerName()));
                GriefDefenderPlugin.sendMessage(player, denyMessage);
                event.setCancelled(true);
                GDTimings.PLAYER_COMMAND_EVENT.stopTiming();
                return;
            }
        }
        GDTimings.PLAYER_COMMAND_EVENT.stopTiming();
    }
}
