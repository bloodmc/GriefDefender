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
import com.google.common.collect.ImmutableMap;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.internal.util.BlockUtil;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.text.action.GDCallbackHolder;

import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;

import java.util.function.Consumer;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_RESTORE_CLAIM)
public class CommandRestoreClaim extends BaseCommand {

    @CommandAlias("restoreclaim|claimrestore")
    @Description("Restores claim to its natural state. Use with caution.")
    @Subcommand("claim restore")
    public void execute(Player player) {

        final GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAt(player.getLocation());

        if (claim.isWilderness()) {
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_NOT_FOUND));
            return;
        }

        final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_CLAIM_DELETE,
                ImmutableMap.of(
                "type", claim.getType().getName()));

        if (!player.hasPermission(GDPermissions.DELETE_CLAIM_ADMIN)) {
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }

        displayConfirmationConsumer(player, claim);
    }

    private static void displayConfirmationConsumer(CommandSender src, GDClaim claim) {
        final Component schematicConfirmationText = TextComponent.builder("")
                .append("Are you sure you want to restore this claim?")
                .append("\n[")
                .append("Confirm", TextColor.GREEN)
                .append("]\n")
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createConfirmationConsumer(src, claim))))
                .hoverEvent(HoverEvent.showText(TextComponent.of("Clicking confirm will restore ENTIRE claim back to default world gen state. Use cautiously!"))).build();
        TextAdapter.sendComponent(src, schematicConfirmationText);
    }

    private static Consumer<CommandSender> createConfirmationConsumer(CommandSender src, GDClaim claim) {
        return confirm -> {
            BlockUtil.getInstance().restoreClaim(claim);
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_RESTORE_SUCCESS);
            GriefDefenderPlugin.sendMessage(src, message);
        };
    }
}
