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
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;

import com.google.common.collect.ImmutableMap;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimSchematic;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.internal.pagination.PaginationList;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.text.action.GDCallbackHolder;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_CLAIM_SCHEMATIC)
public class CommandClaimSchematic extends BaseCommand {

    @CommandAlias("claimschematic")
    @Description("Manages claim schematics. Use '/claimschematic create <name>' to create a live backup of claim.")
    @Syntax("<create|delete> <name>")
    @Subcommand("schematic")
    public void execute(Player player, @Optional String[] args) throws CommandException, InvalidCommandArgument {
        if (GriefDefenderPlugin.getInstance().getWorldEditProvider() == null) {
            TextAdapter.sendComponent(player, TextComponent.of("This command requires WorldEdit to be running on server.", TextColor.RED));
            return;
        }

        String action = null;
        String name = null;
        if (args.length > 0) {
            action = args[0];
            if (args.length < 2) {
                throw new InvalidCommandArgument();
            }
            name = args[1];
        }

        GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(playerData, player.getLocation());
        final Component denyMessage = claim.allowEdit(player);
        if (denyMessage != null) {
            GriefDefenderPlugin.sendMessage(player, denyMessage);
            return;
        }

        if (action == null) {
            if (claim.schematics.isEmpty()) {
                TextAdapter.sendComponent(player, TextComponent.of("There are no schematic backups for this claim.", TextColor.RED));
                return;
            }

            List<Component> schematicTextList = new ArrayList<>();
            for (ClaimSchematic schematic : claim.schematics.values()) {
                final String schematicName = schematic.getName();
                final Instant schematicDate = schematic.getDateCreated();
                schematicTextList.add(
                        TextComponent.builder("")
                        .append(schematicName, TextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(displayConfirmationConsumer(player, claim, schematic))))
                        .hoverEvent(HoverEvent.showText(
                                TextComponent.builder("")
                                .append("Click here to restore claim schematic.\n")
                                .append("Name", TextColor.YELLOW)
                                .append(": ")
                                .append(schematicName, TextColor.GREEN)
                                .append("\n")
                                .append("Created", TextColor.YELLOW)
                                .append(": ")
                                .append(Date.from(schematicDate).toString(), TextColor.AQUA).build()))
                        .build());
            }

            PaginationList.Builder paginationBuilder = PaginationList.builder()
                    .title(TextComponent.of("Schematics", TextColor.AQUA)).padding(TextComponent.of(" ").decoration(TextDecoration.STRIKETHROUGH, true)).contents(schematicTextList);
            paginationBuilder.sendTo(player);
        } else if (action.equalsIgnoreCase("create")) {
            TextAdapter.sendComponent(player, TextComponent.of("Creating schematic backup...", TextColor.GREEN));
            final ClaimSchematic schematic = ClaimSchematic.builder().claim(claim).name(name).build().orElse(null);
            if (schematic != null) {
                TextAdapter.sendComponent(player, TextComponent.of("Schematic backup complete.", TextColor.GREEN));
            } else {
                TextAdapter.sendComponent(player, TextComponent.of("Schematic could not be created.", TextColor.GREEN));
            }
        } else if (action.equalsIgnoreCase("delete")) {
            claim.deleteSchematic(name);
            TextAdapter.sendComponent(player, TextComponent.of("Schematic '" + name + "' has been deleted.", TextColor.GREEN));
        }
    }

    private static Consumer<CommandSender> displayConfirmationConsumer(CommandSender src, Claim claim, ClaimSchematic schematic) {
        return confirm -> {
            final Component schematicConfirmationText = TextComponent.builder("")
                    .append("\n[")
                    .append("Confirm", TextColor.GREEN)
                    .append("]\n")
                    .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createConfirmationConsumer(src, claim, schematic))))
                    .hoverEvent(HoverEvent.showText(TextComponent.of("Clicking confirm will restore ALL claim data with schematic. Use cautiously!"))).build();
            TextAdapter.sendComponent(src, schematicConfirmationText);
        };
    }

    private static Consumer<CommandSender> createConfirmationConsumer(CommandSender src, Claim claim, ClaimSchematic schematic) {
        return confirm -> {
            schematic.apply();
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.SCHEMATIC_RESTORE_CONFIRMED,
                    ImmutableMap.of(
                    "name", schematic.getName())).build();
            GriefDefenderPlugin.sendMessage(src, message);
        };
    }
}
