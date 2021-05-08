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
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.permission.GDPermissions;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_VERSION)
public class CommandGDVersion extends BaseCommand {

    @CommandAlias("gdversion")
    @Description("%version")
    @Subcommand("version")
    public void execute(CommandSender src) {

        Component gpVersion = TextComponent.builder("")
                .append(GriefDefenderPlugin.GD_TEXT)
                .append("Running ")
                .append("GriefDefender " + GriefDefenderPlugin.IMPLEMENTATION_VERSION, TextColor.AQUA)
                .build();
        Component bukkitVersion = TextComponent.builder("")
                .append(GriefDefenderPlugin.GD_TEXT)
                .append("Running ")
                .append(Bukkit.getVersion(), TextColor.YELLOW)
                .build();

        String permissionPlugin = Bukkit.getPluginManager().getPlugin("LuckPerms").getDescription().getFullName();
        Component permVersion = TextComponent.builder("")
                .append(GriefDefenderPlugin.GD_TEXT)
                .append("Running ")
                .append(permissionPlugin, TextColor.GREEN)
                .build();
        TextAdapter.sendComponent(src, TextComponent.builder("")
                .append(gpVersion)
                .append("\n")
                .append(bukkitVersion)
                .append("\n")
                .append(permVersion)
                .build());
    }
}
