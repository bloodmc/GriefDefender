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
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.permission.Context;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.internal.pagination.PaginationList;
import com.griefdefender.permission.GDPermissionHolder;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.permission.ui.UIHelper;
import com.griefdefender.util.PermissionUtil;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_CLAIM_PERMISSION_PLAYER)
public class CommandClaimPermissionPlayer extends BaseCommand {

    @CommandCompletion("@gdplayers @gddummy")
    @CommandAlias("cpp")
    @Description("%permission-player")
    @Syntax("<player> [<permission> <value>]")
    @Subcommand("permission player")
    public void execute(Player player, OfflinePlayer otherPlayer, @Optional String[] args) throws CommandException, InvalidCommandArgument {
        String permission = null;
        String value = null;
        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        if (args.length > 0) {
            if (args.length < 2) {
                throw new InvalidCommandArgument();
            }
            permission = args[0];
            if (!playerData.ignoreClaims && permission != null && !player.hasPermission(permission)) {
                GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().PERMISSION_ASSIGN_WITHOUT_HAVING);
                return;
            }
    
            value = args[1];
        }

        final GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(playerData, player.getLocation());
        final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_CLAIM_MANAGE,
                ImmutableMap.of(
                "type", claim.getType().getName()));
        if (claim.isWilderness() && !playerData.canManageWilderness) {
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        } else if (claim.isAdminClaim() && !playerData.canManageAdminClaims) {
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }

        final GDPermissionHolder holder = PermissionHolderCache.getInstance().getOrCreateUser(player);
        Set<Context> contexts = new HashSet<>();
        contexts.add(claim.getContext());
        if (permission == null || value == null) {
            // display current permissions for user
            List<Component> permList = Lists.newArrayList();
            Map<String, Boolean> permissions = PermissionUtil.getInstance().getPermissions(holder, contexts);
            for (Map.Entry<String, Boolean> permissionEntry : permissions.entrySet()) {
                Boolean permValue = permissionEntry.getValue();
                Component permText = TextComponent.builder("")
                        .append(permissionEntry.getKey(), TextColor.GREEN)
                        .append("  ")
                        .append(permValue.toString(), TextColor.GOLD).build();
                permList.add(permText);
            }

            List<Component> finalTexts = UIHelper.stripeText(permList);

            PaginationList.Builder paginationBuilder = PaginationList.builder()
                    .title(TextComponent.of(otherPlayer.getName() + " Permissions", TextColor.AQUA)).padding(TextComponent.of(" ").decoration(TextDecoration.STRIKETHROUGH, true)).contents(finalTexts);
            paginationBuilder.sendTo(player);
            return;
        }

        Tristate tristateValue = PermissionUtil.getInstance().getTristateFromString(value);
        if (tristateValue == null) {
            TextAdapter.sendComponent(player, TextComponent.of("Invalid value entered. '" + value + "' is not a valid value. Valid values are : true, false, undefined, 1, -1, or 0.", TextColor.RED));
            return;
        }

        final GDPermissionHolder targetHolder = PermissionHolderCache.getInstance().getOrCreateUser(otherPlayer);
        PermissionUtil.getInstance().setPermissionValue(targetHolder, permission, tristateValue, contexts);
        TextAdapter.sendComponent(player, TextComponent.builder("")
                .append("Set permission ")
                .append(permission, TextColor.AQUA)
                .append(" to ")
                .append(value, TextColor.GREEN)
                .append(" on user ")
                .append(otherPlayer.getName(), TextColor.GOLD)
                .append(".").build());
    }

}
