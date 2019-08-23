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
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.claim.TrustType;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.MessageDataConfig;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.internal.pagination.PaginationList;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.text.action.GDCallbackHolder;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_LIST_TRUST)
public class CommandTrustList extends BaseCommand {

    @CommandAlias("trustlist")
    @Description("Lists permissions for the claim you're standing in.")
    @Subcommand("trust list")
    public void execute(Player player) {
        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        final GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(playerData, player.getLocation());
        showTrustList(player, claim, TrustTypes.NONE);
    }

    public static void showTrustList(CommandSender src, GDClaim claim, TrustType type) {
        final Component whiteOpenBracket = TextComponent.of("[", TextColor.AQUA);
        final Component whiteCloseBracket = TextComponent.of("]", TextColor.AQUA);
        final Component showAllText = MessageCache.getInstance().TRUST_CLICK_SHOW_LIST;
        final Component showAccessorText = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.UI_CLICK_FILTER_TYPE,
                ImmutableMap.of("type", MessageCache.getInstance().TITLE_ACCESSOR.color(TextColor.YELLOW)));
        final Component showContainerText = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.UI_CLICK_FILTER_TYPE,
                ImmutableMap.of("type", MessageCache.getInstance().TITLE_CONTAINER.color(TextColor.LIGHT_PURPLE)));
        final Component showBuilderText = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.UI_CLICK_FILTER_TYPE,
                ImmutableMap.of("type", MessageCache.getInstance().TITLE_BUILDER.color(TextColor.GREEN)));
        final Component showManagerText = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.UI_CLICK_FILTER_TYPE,
                ImmutableMap.of("type", MessageCache.getInstance().TITLE_MANAGER.color(TextColor.GOLD)));
        final Component allTypeText = TextComponent.builder("")
                .append(type == TrustTypes.NONE ? TextComponent.builder("")
                        .append(whiteOpenBracket)
                        .append("ALL")
                        .append(whiteCloseBracket)
                        .build() : TextComponent.builder("")
                        .append("ALL",TextColor.GRAY)
                        .build())
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createTrustConsumer(src, claim, TrustTypes.NONE))))
                .hoverEvent(HoverEvent.showText(showAllText)).build();
        final Component accessorTrustText = TextComponent.builder("")
                .append(type == TrustTypes.ACCESSOR ? TextComponent.builder("")
                        .append(whiteOpenBracket)
                        .append(MessageCache.getInstance().TITLE_ACCESSOR.color(TextColor.YELLOW))
                        .append(whiteCloseBracket)
                        .build() : MessageCache.getInstance().TITLE_ACCESSOR.color(TextColor.GRAY))
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createTrustConsumer(src, claim, TrustTypes.ACCESSOR))))
                .hoverEvent(HoverEvent.showText(showAccessorText)).build();
        final Component builderTrustText = TextComponent.builder("")
                .append(type == TrustTypes.BUILDER ? TextComponent.builder("")
                        .append(whiteOpenBracket)
                        .append(MessageCache.getInstance().TITLE_BUILDER.color(TextColor.GREEN))
                        .append(whiteCloseBracket)
                        .build() : MessageCache.getInstance().TITLE_BUILDER.color(TextColor.GRAY))
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createTrustConsumer(src, claim, TrustTypes.BUILDER))))
                .hoverEvent(HoverEvent.showText(showBuilderText)).build();
        final Component containerTrustText = TextComponent.builder("")
                .append(type == TrustTypes.CONTAINER ? TextComponent.builder("")
                        .append(whiteOpenBracket)
                        .append(MessageCache.getInstance().TITLE_CONTAINER.color(TextColor.LIGHT_PURPLE))
                        .append(whiteCloseBracket)
                        .build() : MessageCache.getInstance().TITLE_CONTAINER.color(TextColor.GRAY))
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createTrustConsumer(src, claim, TrustTypes.CONTAINER))))
                .hoverEvent(HoverEvent.showText(showContainerText)).build();
        final Component managerTrustText = TextComponent.builder("")
                .append(type == TrustTypes.MANAGER ? TextComponent.builder("")
                        .append(whiteOpenBracket)
                        .append("MANAGER", TextColor.GOLD)
                        .append(whiteCloseBracket)
                        .build() : MessageCache.getInstance().TITLE_MANAGER.color(TextColor.GRAY))
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createTrustConsumer(src, claim, TrustTypes.MANAGER))))
                .hoverEvent(HoverEvent.showText(showManagerText)).build();
        final Component claimTrustHead = TextComponent.builder()
                .append(" ")
                .append(MessageCache.getInstance().LABEL_DISPLAYING.color(TextColor.AQUA))
                .append(" ")
                .append(allTypeText)
                .append("  ")
                .append(accessorTrustText)
                .append("  ")
                .append(builderTrustText)
                .append("  ")
                .append(containerTrustText)
                .append("  ")
                .append(managerTrustText)
                .build();

        List<UUID> userIdList = new ArrayList<>(claim.getUserTrusts());
        List<Component> trustList = new ArrayList<>();
        trustList.add(TextComponent.empty());

        if (type == TrustTypes.NONE) {
            // check highest trust first
            for (UUID uuid : claim.getInternalClaimData().getManagers()) {
                final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(uuid);
                trustList.add(TextComponent.of(user.getName(), TextColor.GOLD));
                userIdList.remove(user.getUniqueId());
            }

            for (UUID uuid : claim.getInternalClaimData().getBuilders()) {
                if (!userIdList.contains(uuid)) {
                    continue;
                }

                final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(uuid);
                trustList.add(TextComponent.of(user.getName(), TextColor.GREEN));
                userIdList.remove(uuid);
            }
    
            /*for (String group : claim.getInternalClaimData().getManagerGroups()) {
                permissions.append(SPACE_TEXT, Text.of(group));
            }*/
    
            for (UUID uuid : claim.getInternalClaimData().getContainers()) {
                if (!userIdList.contains(uuid)) {
                    continue;
                }

                final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(uuid);
                trustList.add(TextComponent.of(user.getName(), TextColor.LIGHT_PURPLE));
                userIdList.remove(uuid);
            }
    
           /* for (String group : claim.getInternalClaimData().getBuilderGroups()) {
                permissions.append(SPACE_TEXT, Text.of(group));
            }*/
    
            for (UUID uuid : claim.getInternalClaimData().getAccessors()) {
                if (!userIdList.contains(uuid)) {
                    continue;
                }

                final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(uuid);
                trustList.add(TextComponent.of(user.getName(), TextColor.YELLOW));
                userIdList.remove(uuid);
            }
    
            /*for (String group : claim.getInternalClaimData().getContainerGroups()) {
                permissions.append(SPACE_TEXT, Text.of(group));
            }
    
            player.sendMessage(permissions.build());
            permissions = Text.builder(">").color(TextColors.BLUE);
    
            for (UUID uuid : claim.getInternalClaimData().getAccessors()) {
                User user = GriefDefenderPlugin.getOrCreateUser(uuid);
                permissions.append(SPACE_TEXT, Text.of(user.getName()));
            }
    
            for (String group : claim.getInternalClaimData().getAccessorGroups()) {
                permissions.append(SPACE_TEXT, Text.of(group));
            }*/
    
        } else {
            for (UUID uuid : claim.getUserTrusts(type)) {
                if (!userIdList.contains(uuid)) {
                    continue;
                }

                final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(uuid);
                trustList.add(TextComponent.of(user.getName(), getTrustColor(type)));
                userIdList.remove(uuid);
            }
        }

        int fillSize = 20 - (trustList.size() + 2);
        for (int i = 0; i < fillSize; i++) {
            trustList.add(TextComponent.of(" "));
        }

        PaginationList.Builder paginationBuilder = PaginationList.builder()
                .title(claimTrustHead).padding(TextComponent.of(" ").decoration(TextDecoration.STRIKETHROUGH, true)).contents(trustList);
        paginationBuilder.sendTo(src);
        paginationBuilder.sendTo(src);

    }

    private static TextColor getTrustColor(TrustType type) {
        if (type == TrustTypes.NONE) {
            return TextColor.WHITE;
        }
        if (type == TrustTypes.ACCESSOR) {
            return TextColor.YELLOW;
        }
        if (type == TrustTypes.BUILDER) {
            return TextColor.GREEN;
        }
        if (type == TrustTypes.CONTAINER) {
            return TextColor.LIGHT_PURPLE;
        }
        return TextColor.GOLD;
    }

    private static Consumer<CommandSender> createTrustConsumer(CommandSender src, GDClaim claim, TrustType type) {
        return consumer -> {
            showTrustList(src, claim, type);
        };
    }
}
