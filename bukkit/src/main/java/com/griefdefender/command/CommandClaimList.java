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
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.ImmutableMap;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.api.claim.ClaimTypes;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.claim.GDClaimManager;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.internal.pagination.PaginationList;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.text.action.GDCallbackHolder;
import com.griefdefender.util.ChatCaptureUtil;
import com.griefdefender.util.PaginationUtil;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_CLAIM_LIST_BASE)
public class CommandClaimList extends BaseCommand {

    private final ClaimType forcedType;
    private final Cache<UUID, String> lastActiveClaimTypeMap = Caffeine.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES)
            .build();

    public CommandClaimList() {
        this.forcedType = null;
    }

    public CommandClaimList(ClaimType type) {
        this.forcedType = type;
    }

    @CommandCompletion("@gdplayers @gdworlds @gddummy")
    @CommandAlias("claimlist|claimslist")
    @Syntax("[<player>|<player> <world>]")
    @Description("%claim-list")
    @Subcommand("claim list")
    public void execute(Player src, @Optional String targetPlayer, @Optional World world) {
        GDPermissionUser user = null;
        // check target player
        if (targetPlayer != null) {
            user = PermissionHolderCache.getInstance().getOrCreateUser(targetPlayer);
            if (user != null && user.getOnlinePlayer() != src && !src.hasPermission(GDPermissions.COMMAND_CLAIM_LIST_OTHERS)) {
                TextAdapter.sendComponent(src, MessageCache.getInstance().PERMISSION_PLAYER_VIEW_OTHERS);
                return;
            }
        } else {
            user = PermissionHolderCache.getInstance().getOrCreateUser(src);
        }

        if (user == null) {
            GriefDefenderPlugin.sendMessage(src, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.COMMAND_INVALID_PLAYER,
                    ImmutableMap.of(
                    "player", targetPlayer)));
            return;
        }
        if (world == null) {
            world = src.getWorld();
        }

        showClaimList(src, user, this.forcedType, world.getUID());
    }

    private void showClaimList(Player src, GDPermissionUser user, ClaimType type, UUID worldUniqueId) {
        List<Component> claimsTextList = new ArrayList<>();
        Set<Claim> claims = new HashSet<>();
        final String worldName = worldUniqueId == null ? "" : Bukkit.getWorld(worldUniqueId).getName();
        final boolean otherUser = !src.getUniqueId().equals(user.getUniqueId());
        for (World world : Bukkit.getServer().getWorlds()) {
            if (type != null && !world.getUID().equals(worldUniqueId)) {
                continue;
            }
            final GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(world.getUID());
            // load the target player's data
            final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(world, user.getUniqueId());
            Set<Claim> claimList = null;
            if (type == null || otherUser) {
                claimList = playerData.getClaims();
            } else {
                claimList = claimWorldManager.getWorldClaims();
            }

            for (Claim claim : claimList) {
                if (claims.contains(claim)) {
                    continue;
                }

                if (((GDClaim) claim).allowEdit(src) != null && !claim.isUserTrusted(src.getUniqueId(), TrustTypes.ACCESSOR)) {
                    continue;
                }
                if (type == null) {
                    claims.add(claim);
                } else {
                    if (claim.getType() == type) {
                        claims.add(claim);
                    } else if (type == ClaimTypes.SUBDIVISION) {
                        for (Claim child : claim.getChildren(true)) {
                            if (child.getType() == type) {
                                claims.add(child);
                            }
                        }
                    }
                }
            }
        }
        if (src instanceof Player) {
            final Player player = (Player) src;
            final String lastClaimType = this.lastActiveClaimTypeMap.getIfPresent(player.getUniqueId());
            String currentType = type == null ? "OWN" : type.toString();
            if (lastClaimType != null && !lastClaimType.equals(currentType.toString())) {
                PaginationUtil.getInstance().resetActivePage(player.getUniqueId());
            }
        }
        claimsTextList = CommandHelper.generateClaimTextListCommand(claimsTextList, claims, worldName, user, src, createClaimListConsumer(src, user, type, worldUniqueId), false);

        final Component whiteOpenBracket = TextComponent.of("[");
        final Component whiteCloseBracket = TextComponent.of("]");
        Component ownedShowText = MessageCache.getInstance().CLAIMLIST_UI_CLICK_VIEW_CLAIMS;
        Component adminShowText = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.UI_CLICK_FILTER_TYPE,
                ImmutableMap.of("type", TextComponent.of("ADMIN", TextColor.RED)));
        Component basicShowText = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.UI_CLICK_FILTER_TYPE,
                ImmutableMap.of("type", TextComponent.of("BASIC", TextColor.YELLOW)));
        Component subdivisionShowText = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.UI_CLICK_FILTER_TYPE,
                ImmutableMap.of("type", TextComponent.of("SUBDIVISION", TextColor.AQUA)));
        Component townShowText = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.UI_CLICK_FILTER_TYPE,
                ImmutableMap.of("type", TextComponent.of("TOWN", TextColor.GREEN)));
        Component ownedTypeText = TextComponent.builder("")
                .append(type == null ? 
                        TextComponent.builder("")
                        .append(whiteOpenBracket)
                        .append(otherUser ? TextComponent.of(user.getFriendlyName()).color(TextColor.GOLD) : MessageCache.getInstance().TITLE_OWN.color(TextColor.GOLD))
                        .append(whiteCloseBracket).build() : otherUser ? TextComponent.of(user.getFriendlyName()).color(TextColor.GRAY) : MessageCache.getInstance().TITLE_OWN.color(TextColor.GRAY))
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createClaimListConsumer(src, user, null, worldUniqueId))))
                .hoverEvent(HoverEvent.showText(ownedShowText)).build();
        Component adminTypeText = TextComponent.builder("")
                .append(type == ClaimTypes.ADMIN ? TextComponent.builder("")
                        .append(whiteOpenBracket)
                        .append("ADMIN", TextColor.RED)
                        .append(whiteCloseBracket).build() : TextComponent.of("ADMIN", TextColor.GRAY))
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createClaimListConsumer(src, user, ClaimTypes.ADMIN, worldUniqueId))))
                .hoverEvent(HoverEvent.showText(adminShowText)).build();
        Component basicTypeText = TextComponent.builder("")
                .append(type == ClaimTypes.BASIC ? TextComponent.builder("")
                        .append(whiteOpenBracket)
                        .append("BASIC", TextColor.YELLOW)
                        .append(whiteCloseBracket).build() : TextComponent.of("BASIC", TextColor.GRAY))
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createClaimListConsumer(src, user, ClaimTypes.BASIC, worldUniqueId))))
                .hoverEvent(HoverEvent.showText(basicShowText)).build();
        Component subTypeText = TextComponent.builder("")
                .append(type == ClaimTypes.SUBDIVISION ? TextComponent.builder("")
                        .append(whiteOpenBracket)
                        .append("SUBDIVISION", TextColor.AQUA)
                        .append(whiteCloseBracket).build() : TextComponent.of("SUBDIVISION", TextColor.GRAY))
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createClaimListConsumer(src, user, ClaimTypes.SUBDIVISION, worldUniqueId))))
                .hoverEvent(HoverEvent.showText(subdivisionShowText)).build();
        Component townTypeText = TextComponent.builder("")
                .append(type == ClaimTypes.TOWN ? TextComponent.builder("")
                        .append(whiteOpenBracket)
                        .append("TOWN", TextColor.GREEN)
                        .append(whiteCloseBracket).build() : TextComponent.of("TOWN", TextColor.GRAY))
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createClaimListConsumer(src, user, ClaimTypes.TOWN, worldUniqueId))))
                .hoverEvent(HoverEvent.showText(townShowText)).build();
        Component claimListHead = TextComponent.builder("")
                .append(" ")
                .append(MessageCache.getInstance().LABEL_DISPLAYING.color(TextColor.AQUA))
                .append(" : ", TextColor.AQUA)
                .append(ownedTypeText)
                .append(" ")
                .append(otherUser ? TextComponent.of("") : adminTypeText)
                .append(otherUser ? "" : " ")
                .append(basicTypeText)
                .append(" ")
                .append(subTypeText)
                .append(" ")
                .append(townTypeText).build();

        int fillSize = 20 - (claimsTextList.size() + 2);
        Component footer = null;
        if (src != null && src.hasPermission(GDPermissions.CHAT_CAPTURE)) {
            footer = ChatCaptureUtil.getInstance().createRecordChatComponent(src, null, user.getInternalPlayerData(), "claimlist");
            fillSize = 20 - (claimsTextList.size() + 3);
        }

        for (int i = 0; i < fillSize; i++) {
            claimsTextList.add(TextComponent.of(" "));
        }

        PaginationList paginationList = PaginationList.builder()
                .title(claimListHead).padding(TextComponent.of(" ").decoration(TextDecoration.STRIKETHROUGH, true)).contents(claimsTextList).footer(footer).build();
        Integer activePage = 1;
        if (src instanceof Player) {
            final Player player = (Player) src;
            activePage = PaginationUtil.getInstance().getActivePage(player.getUniqueId());
            if (activePage == null) {
                activePage = 1;
            }
            this.lastActiveClaimTypeMap.put(player.getUniqueId(), type == null ? "OWN" : type.toString());
        }
        paginationList.sendTo(src, activePage);
    }

    private Consumer<CommandSender> createClaimListConsumer(Player src, GDPermissionUser user, ClaimType type, UUID worldUniqueId) {
        return consumer -> {
            showClaimList(src, user, type, worldUniqueId);
        };
    }
}