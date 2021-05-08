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
import co.aikar.commands.annotation.Syntax;

import com.google.common.collect.ImmutableMap;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.text.action.GDCallbackHolder;

import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;

import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_SET_ACCRUED_CLAIM_BLOCKS_ALL)
public class CommandAdjustBonusClaimBlocksAll extends BaseCommand {

    @CommandAlias("acball|adjustclaimblocksall")
    @Description("%player-adjust-bonus-blocks-all")
    @Syntax("<amount>")
    @Subcommand("player adjustbonusblocksall")
    public void execute(CommandSender src, int amount) {
        if (!(src instanceof Player)) {
            updateOnlinePlayerBonusBlocks(src, amount);
            return;
        }

        final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ADJUST_BONUS_BLOCKS_ALL_WARNING, ImmutableMap.of(
                "amount", amount));
        final Component confirmationText = TextComponent.builder()
                .append(message)
                    .append(TextComponent.builder()
                    .append("\n[")
                    .append(MessageCache.getInstance().LABEL_CONFIRM.color(TextColor.GREEN))
                    .append("]\n")
                    .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(src, createConfirmationConsumer(src, amount), true)))
                    .hoverEvent(HoverEvent.showText(MessageCache.getInstance().UI_CLICK_CONFIRM)).build())
                .build();
        TextAdapter.sendComponent(src, confirmationText);
    }

    private static Consumer<CommandSender> createConfirmationConsumer(CommandSender src, int amount) {
        return confirm -> {
            updateOnlinePlayerBonusBlocks(src, amount);
        };
    }

    private static void updateOnlinePlayerBonusBlocks(CommandSender src, int amount) {
        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            // give blocks to player
            GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreateGlobalPlayerData(player.getUniqueId());
            playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks() + amount);
            count++;
        }

        final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ADJUST_BONUS_BLOCKS_ALL_SUCCESS, ImmutableMap.of(
                "count", count,
                "amount", amount));
        TextAdapter.sendComponent(src, message);
        GriefDefenderPlugin.getInstance().getLogger().info(
                src.getName() + " adjusted " + count + " online player's bonus claim blocks by " + amount + ".");
    }
}


