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
import com.google.common.collect.Lists;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.claim.ClaimContexts;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.option.Option;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.internal.pagination.PaginationList;
import com.griefdefender.permission.GDPermissionHolder;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.registry.OptionRegistryModule;
import com.griefdefender.util.CauseContextHelper;
import com.griefdefender.util.PermissionUtil;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_OPTIONS_BASE)
public class CommandClaimOption extends BaseCommand {

    private GDPermissionHolder subject = GriefDefenderPlugin.DEFAULT_HOLDER;
    private ClaimSubjectType subjectType = ClaimSubjectType.GLOBAL;
    private String friendlySubjectName;

    @CommandAlias("cop|claimoption")
    @Description("Gets/Sets claim options in the claim you are standing in.")
    @Syntax("[<option> <value> [context[key=value]]")
    @Subcommand("option claim")
    public void execute(Player player, @Optional String[] args) throws CommandException, InvalidCommandArgument {
        this.subject = PermissionHolderCache.getInstance().getOrCreateUser(player);
        String commandOption = null;
        Double optionValue = null;
        String contexts = null;
        Option option = null;
        if (args.length > 0) {
            if (args.length < 2) {
                throw new InvalidCommandArgument();
            }

            commandOption = args[0];
            option = OptionRegistryModule.getInstance().getById(commandOption).orElse(null);
            // Check if global option
            if (option != null && option.isGlobal() && !player.hasPermission(GDPermissions.MANAGE_GLOBAL_OPTIONS)) {
                GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.permissionGlobalOption.toText());
                return;
            }
            try {
                optionValue = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                TextAdapter.sendComponent(player, TextComponent.of("Invalid number ' " + args[1] + "' entered.", TextColor.RED));
                throw new InvalidCommandArgument();
            }
            if (args.length == 3) {
                contexts = args[2];
            }
        }
 
        /*if (contexts != null && !contexts.equalsIgnoreCase("default")) {
            final Text message = GriefDefenderPlugin.getInstance().messageData.contextInvalid
                    .apply(ImmutableMap.of(
                    "context", context)).build();
            GriefDefenderPlugin.sendMessage(src, message);
            return CommandResult.success();
        }*/

        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        final GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(playerData, player.getLocation());
        final Set<Context> contextSet = CauseContextHelper.generateContexts(player, claim, contexts, true);
        if (contextSet == null) {
            return;
        }

        boolean isClaimContext = true;
        if (!contextSet.isEmpty()) {
            Iterator<Context> iter = contextSet.iterator();
            while (iter.hasNext()) {
                final Context context = iter.next();
                if (context.getKey().equals("player")) {
                    final String playerName = context.getValue();
                    final GDPermissionHolder user = PermissionHolderCache.getInstance().getOrCreateUser(playerName);
                    if (user == null) {
                        TextAdapter.sendComponent(player, TextComponent.of("Could not locate player with name '" + playerName + "'."));
                        return;
                    }
    
                    this.subject = user;
                    this.subjectType = ClaimSubjectType.PLAYER;
                    this.friendlySubjectName = playerName;
                    iter.remove();
                } else if (context.getKey().equals("group")) {
                    final String groupName = context.getValue();
                    final GDPermissionHolder group = PermissionHolderCache.getInstance().getOrCreateHolder(groupName);
                    if (group == null) {
                        TextAdapter.sendComponent(player, TextComponent.of("Could not locate group with name '" + groupName + "'."));
                        return;
                    }
                    this.subject = group;
                    this.subjectType = ClaimSubjectType.GROUP;
                    this.friendlySubjectName = groupName;
                    iter.remove();
                } else if (context.getKey().equals("default")) {
                    isClaimContext = false;
                }
            }
        }

        if (option != null && !option.isGlobal()) {
            if (claim.isSubdivision()) {
                GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.commandOptionInvalidClaim.toText());
                return;
            }

            if (!playerData.canManageOption(player, claim, true)) {
                GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.permissionGroupOption.toText());
                return;
            }

            final Component message = GriefDefenderPlugin.getInstance().messageData.permissionClaimManage
                    .apply(ImmutableMap.of(
                    "type", claim.getType().getName())).build();
            if (claim.isWilderness() && !player.hasPermission(GDPermissions.MANAGE_WILDERNESS)) {
                GriefDefenderPlugin.sendMessage(player, message);
                return;
            } else if (claim.isAdminClaim() && !player.hasPermission(GDPermissions.COMMAND_ADMIN_CLAIMS)) {
                GriefDefenderPlugin.sendMessage(player, message);
                return;
            }

            // Validate new value against admin set value
            if (option != null && optionValue != null && isClaimContext) {
                Double tempValue = GDPermissionManager.getInstance().getInternalOptionValue(player, option, claim, playerData);
                if (tempValue > optionValue || tempValue < optionValue) {
                    final Component message2 = GriefDefenderPlugin.getInstance().messageData.commandOptionExceedsAdmin
                            .apply(ImmutableMap.of(
                            "original_value", optionValue,
                            "admin_value", tempValue)).build();
                    GriefDefenderPlugin.sendMessage(player, message2);
                    return;
                }
                contextSet.add(claim.getContext());
            }
        }

        if (option == null || optionValue == null) {
            List<Component> optionList = Lists.newArrayList();
            contextSet.add(ClaimContexts.GLOBAL_DEFAULT_CONTEXT);
            Map<String, String> options = PermissionUtil.getInstance().getTransientOptions(claim, GriefDefenderPlugin.DEFAULT_HOLDER, contextSet);
            for (Map.Entry<String, String> optionEntry : options.entrySet()) {
                String value = optionEntry.getValue();
                Component optionText = TextComponent.builder("")
                        .append(optionEntry.getKey(), TextColor.GREEN)
                        .append("  ")
                        .append(value, TextColor.GOLD).build();
                optionList.add(optionText);
            }

            List<Component> finalTexts = CommandHelper.stripeText(optionList);

            PaginationList.Builder paginationBuilder = PaginationList.builder()
                    .title(TextComponent.of("Claim Options")).padding(TextComponent.of(" ").decoration(TextDecoration.STRIKETHROUGH, true)).contents(finalTexts);
            paginationBuilder.sendTo(player);
            return;
       }

       final Option subjectOption = option;
       final Double subjectOptionValue = optionValue;

       boolean result = PermissionUtil.getInstance().setOptionValue(this.subject, option.getPermission(), optionValue.toString(), contextSet);
       if (!result) {
           TextAdapter.sendComponent(player, TextComponent.of("The permission plugin failed to set the option.", TextColor.RED));
           return;
       }

       if (contextSet.isEmpty()) {
           TextAdapter.sendComponent(player, TextComponent.builder("")
                   .append("Set ")
                   .append("default", TextColor.LIGHT_PURPLE)
                   .append(" option ")
                   .append(subjectOption.getName().toLowerCase(), TextColor.AQUA)
                   .append(" to ")
                   .append(String.valueOf(subjectOptionValue), TextColor.GREEN).build());
       } else {
           if (this.subjectType == ClaimSubjectType.GROUP) {
               TextAdapter.sendComponent(player, TextComponent.builder("")
                       .append("Set option ")
                       .append(subjectOption.getName().toLowerCase(), TextColor.AQUA)
                       .append(" to ")
                       .append(String.valueOf(subjectOptionValue), TextColor.GREEN)
                       .append(" on group ")
                       .append(this.subject.getFriendlyName(), TextColor.GOLD)
                       .append(".").build());
           } else {
               TextAdapter.sendComponent(player, TextComponent.builder("")
                       .append("Set option ")
                       .append(subjectOption.getName().toLowerCase(), TextColor.AQUA)
                       .append(" to ")
                       .append(String.valueOf(subjectOptionValue), TextColor.GREEN)
                       .append(" on user ")
                       .append(this.subject.getFriendlyName(), TextColor.GOLD)
                       .append(".").build());
           }
       }
    }
}
