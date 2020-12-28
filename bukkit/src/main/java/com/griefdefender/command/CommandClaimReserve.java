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
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;

import com.google.common.collect.ImmutableMap;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.configuration.GriefDefenderConfig;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.internal.pagination.PaginationList;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.text.action.GDCallbackHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.io.FilenameUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_CLAIM_RESERVE)
public class CommandClaimReserve extends BaseCommand {

    @CommandAlias("claimreserve")
    @Syntax("[<name>]")
    @Description("%claim-reserve")
    @Subcommand("claim reserve")
    public void execute(CommandSender src, @Optional String name) {
        GriefDefenderConfig<?> activeConfig = null;
        if (src instanceof Player) {
            final Player player = (Player) src;
            activeConfig = GriefDefenderPlugin.getActiveConfig(player.getWorld().getUID());
        } else {
            activeConfig = GriefDefenderPlugin.getGlobalConfig();
        }

        if (name == null) {
            List<Component> nameList = new ArrayList<>();
            for (String reserved : activeConfig.getConfig().claim.reservedClaimNames) {
                nameList.add(TextComponent.builder("")
                            .append(reserved, TextColor.GREEN)
                            .append(" ")
                            .append("[", TextColor.WHITE)
                            .append(TextComponent.builder()
                                .append("x", TextColor.RED)
                                .hoverEvent(HoverEvent.showText(MessageCache.getInstance().UI_CLICK_REMOVE))
                                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createRemoveConsumer(activeConfig, name))))
                                .build())
                            .append("]", TextColor.WHITE)
                            .build());
            }

            final int fillSize = 20 - (nameList.size() + 2);
            for (int i = 0; i < fillSize; i++) {
                nameList.add(TextComponent.of(" "));
            }

            PaginationList.Builder paginationBuilder = PaginationList.builder()
                    .title(TextComponent.of("Reserved Claim Names", TextColor.AQUA)).padding(TextComponent.of(" ").decoration(TextDecoration.STRIKETHROUGH, true)).contents(nameList);
            paginationBuilder.sendTo(src);
            return;
        }

        for (String str : activeConfig.getConfig().claim.reservedClaimNames) {
            if (FilenameUtils.wildcardMatch(name, str)) {
                GriefDefenderPlugin.sendMessage(src, MessageCache.getInstance().CLAIM_RESERVE_EXISTS);
                return;
            }
        }

        activeConfig.getConfig().claim.reservedClaimNames.add(name);
        activeConfig.save();
        final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_RESERVE_ADD,
                ImmutableMap.of(
                "name", name));
        GriefDefenderPlugin.sendMessage(src, message);
    }

    private Consumer<CommandSender> createRemoveConsumer(GriefDefenderConfig<?> activeConfig, String name) {
        return consumer -> {
            activeConfig.getConfig().claim.reservedClaimNames.remove(name);
            activeConfig.save();
        };
    }
}
