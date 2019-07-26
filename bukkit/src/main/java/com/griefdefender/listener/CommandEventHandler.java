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
import com.griefdefender.GDPlayerData;
import com.griefdefender.GDTimings;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.api.permission.flag.Flags;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.permission.GDFlags;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.storage.BaseStorage;
import com.griefdefender.util.PaginationUtil;
import me.lucko.luckperms.api.Tristate;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.RemoteServerCommandEvent;
import org.bukkit.event.server.ServerCommandEvent;

public class CommandEventHandler implements Listener {

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
                pluginId = pluginCommand.getName().toLowerCase();
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
        String commandPermission = pluginId + "." + command;
        // first check the args
        String argument = "";
        for (String arg : args) {
            //argument = argument + "." + arg;
        }

        if (GDFlags.COMMAND_EXECUTE && !commandExecuteSourceBlacklisted && !GriefDefenderPlugin.isTargetIdBlacklisted(Flags.COMMAND_EXECUTE.getName(), commandPermission + argument, player.getWorld().getUID())) {
            final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, player.getLocation(), claim, GDPermissions.COMMAND_EXECUTE, player, commandPermission + argument, player, TrustTypes.ACCESSOR, true);
            if (result == Tristate.FALSE) {
                final Component denyMessage = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.COMMAND_BLOCKED,
                        ImmutableMap.of(
                        "command", command,
                        "player", claim.getOwnerName()));
                TextAdapter.sendComponent(player, denyMessage);
                event.setCancelled(true);
                GDTimings.PLAYER_COMMAND_EVENT.stopTiming();
                return;
            }
        }

        GDTimings.PLAYER_COMMAND_EVENT.stopTiming();
    }
}
