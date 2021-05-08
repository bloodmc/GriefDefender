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

import com.google.common.collect.ImmutableMap;
import com.griefdefender.GDDebugData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.format.TextColor;
import net.kyori.text.serializer.plain.PlainComponentSerializer;

import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_ADMIN_DEBUG)
public class CommandDebug extends BaseCommand {

    @CommandAlias("gddebug")
    @Description("%debug")
    @Syntax("<record|paste|on|off> [filter]")
    @Subcommand("debug")
    public void execute(CommandSender src, String command, @Optional String filter) {
        GDDebugData debugData = null;
        boolean paste = false;
        boolean verbose = false;
        if (command.equalsIgnoreCase("on")) {
            verbose = true;
            debugData = getOrCreateDebugUser(src, filter, true);
        } else if (command.equalsIgnoreCase("record")) {
            debugData = getOrCreateDebugUser(src, filter, false);
        } else if (command.equalsIgnoreCase("claim")) {
            
            debugData = getOrCreateDebugUser(src, filter, false);
        } else if (command.equalsIgnoreCase("paste")) {
            paste = true;
        } else if (command.equalsIgnoreCase("off")) {
            GriefDefenderPlugin.getInstance().getDebugUserMap().remove(src.getName());
            if (GriefDefenderPlugin.getInstance().getDebugUserMap().isEmpty()) {
                GriefDefenderPlugin.debugActive = false;
            }
        }

        if (debugData == null) {
            if (paste) {
                debugData = GriefDefenderPlugin.getInstance().getDebugUserMap().get(src.getName());
                if (debugData == null) {
                    TextAdapter.sendComponent(src, TextComponent.of("Nothing to paste!", TextColor.RED));
                } else {
                    debugData.pasteRecords();
                }
            }
            TextAdapter.sendComponent(src, TextComponent.builder("")
                    .append(GriefDefenderPlugin.GD_TEXT)
                    .append("Debug ", TextColor.GRAY)
                    .append("OFF", TextColor.RED)
                    .build());
            GriefDefenderPlugin.getInstance().getDebugUserMap().remove(src.getName());
            if (GriefDefenderPlugin.getInstance().getDebugUserMap().isEmpty()) {
                GriefDefenderPlugin.debugActive = false;
            }
        } else {
            GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(filter);
            TextComponent.Builder builder = TextComponent.builder("")
                .append(GriefDefenderPlugin.GD_TEXT)
                .append("Debug: ", TextColor.GRAY)
                .append("ON", TextColor.GREEN)
                .append(" | ")
                .append("Record: ", TextColor.GRAY)
                .append(debugData.isRecording() ? TextComponent.of("ON", TextColor.GREEN) : TextComponent.of("OFF", TextColor.RED))
                .append(" | ")
                .append("User: ", TextColor.GRAY)
                .append(user == null ? PlainComponentSerializer.INSTANCE.serialize(MessageCache.getInstance().TITLE_ALL) : user.getName(), TextColor.GOLD);
            if (filter != null && user == null) {
                builder.append(" | ")
                    .append("Filter: ", TextColor.GRAY)
                    .append(filter, TextColor.AQUA);
            }
            if (verbose) {
                builder.append(" | ")
                    .append("Verbose: ", TextColor.GRAY)
                    .append(TextComponent.of("ON", TextColor.GREEN));
            }
            TextAdapter.sendComponent(src, builder.build());
            GriefDefenderPlugin.getInstance().getDebugUserMap().put(src.getName(), debugData);
        }
    }


    private GDDebugData getOrCreateDebugUser(CommandSender src, String filter, boolean verbose) {
        GDDebugData debugData = GriefDefenderPlugin.getInstance().getDebugUserMap().get(src.getName());
        if (debugData != null) {
            debugData.stop();
        }

        debugData = new GDDebugData(src, filter, verbose);
        GriefDefenderPlugin.getInstance().getDebugUserMap().put(src.getName(), debugData);
        GriefDefenderPlugin.debugActive = true;
        return debugData;
    }
}
