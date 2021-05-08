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
import net.kyori.text.adapter.spongeapi.TextAdapter;
import net.kyori.text.format.TextColor;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.permission.PermissionService;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_VERSION)
public class CommandGDVersion extends BaseCommand {

    @CommandAlias("gdversion")
    @Description("%version")
    @Subcommand("version")
    public void execute(CommandSource src) {

        final String spongePlatform = Sponge.getPlatform().getContainer(org.spongepowered.api.Platform.Component.IMPLEMENTATION).getName();
        Component gpVersion = TextComponent.builder("")
                .append(GriefDefenderPlugin.GD_TEXT)
                .append("Running ")
                .append("GriefDefender " + GriefDefenderPlugin.IMPLEMENTATION_VERSION, TextColor.AQUA)
                .build();
        Component spongeVersion = TextComponent.builder("")
                .append(GriefDefenderPlugin.GD_TEXT)
                .append("Running ")
                .append(spongePlatform + " " + GriefDefenderPlugin.SPONGE_VERSION, TextColor.YELLOW)
                .build();
        String permissionPlugin = Sponge.getServiceManager().getRegistration(PermissionService.class).get().getPlugin().getId();
        String permissionVersion = Sponge.getServiceManager().getRegistration(PermissionService.class).get().getPlugin().getVersion().orElse("unknown");
        Component permVersion = TextComponent.builder("")
                .append(GriefDefenderPlugin.GD_TEXT)
                .append("Running ")
                .append(permissionPlugin + " " + permissionVersion, TextColor.GREEN)
                .build();
        TextAdapter.sendComponent(src, TextComponent.builder("")
                .append(gpVersion)
                .append("\n")
                .append(spongeVersion)
                .append("\n")
                .append(permVersion)
                .build());
    }
}
