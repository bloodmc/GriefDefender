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
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.api.claim.ClaimTypes;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaimManager;
import com.griefdefender.internal.pagination.PaginationList;
import com.griefdefender.internal.util.BlockUtil;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.text.action.GDCallbackHolder;
import com.griefdefender.util.PaginationUtil;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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
@CommandPermission(GDPermissions.COMMAND_CLAIM_LIST)
public class CommandClaimList extends BaseCommand {

    private final ClaimType forcedType;
    private boolean canListOthers;
    private boolean canListAdmin;
    private boolean displayOwned = true;
    private final Cache<UUID, String> lastActiveClaimTypeMap = Caffeine.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES)
            .build();

    public CommandClaimList() {
        this.forcedType = null;
    }

    public CommandClaimList(ClaimType type) {
        this.forcedType = type;
    }

    @CommandAlias("claimlist")
    @Syntax("[<player>|<player> <world>]")
    @Subcommand("claim list")
    public void execute(Player src, @Optional String[] args) {
        OfflinePlayer user = null;
        World world = null;
        if (args.length > 0) {
            user = Bukkit.getServer().getOfflinePlayer(args[0]);
            if (user == null) {
                TextAdapter.sendComponent(src, TextComponent.of("User ' " + args[0] + "' could not be found.", TextColor.RED));
                return;
            }
            if (args.length > 1) {
                world = Bukkit.getServer().getWorld(args[1]);
                if (world == null) {
                    TextAdapter.sendComponent(src, TextComponent.of("World ' " + args[1] + "' could not be found.", TextColor.RED));
                    return;
                }
            }
        }

        if (user == null) {
            user = (OfflinePlayer) src;
            if (world == null) {
                world = ((Player) user).getWorld();
            }
        }
        if (world == null) {
            world = Bukkit.getServer().getWorlds().get(0);
        }

        this.canListOthers = src.hasPermission(GDPermissions.LIST_OTHER_CLAIMS);
        this.canListAdmin = src.hasPermission(GDPermissions.LIST_OTHER_CLAIMS);
        showClaimList(src, PermissionHolderCache.getInstance().getOrCreateUser(user), this.forcedType, world.getUID());
    }

    private void showClaimList(Player src, GDPermissionUser user, ClaimType type, UUID worldUniqueId) {
        List<Component> claimsTextList = new ArrayList<>();
        Set<Claim> claims = new HashSet<>();
        final String worldName = worldUniqueId == null ? "" : Bukkit.getWorld(worldUniqueId).getName();
        for (World world : Bukkit.getServer().getWorlds()) {
            if (!this.displayOwned && !world.getUID().equals(worldUniqueId)) {
                continue;
            }
            final GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(world.getUID());
            // load the target player's data
            final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(world, user.getUniqueId());
            Set<Claim> claimList = null;
            if (this.displayOwned) {
                claimList = playerData.getClaims();
            } else {
                claimList = BlockUtil.getInstance().getNearbyClaims(src.getLocation(), 100);
            }

            for (Claim claim : claimList) {
                if (claims.contains(claim)) {
                    continue;
                }

                if (user != null && this.displayOwned) {
                    if (user.getUniqueId().equals(claim.getOwnerUniqueId())) {
                        claims.add(claim);
                    }
                } else if (type != null) {
                    if (claim.getType() == type) {
                        claims.add(claim);
                    }
                } else {
                    claims.add(claim);
                }
            }
        }
        if (src instanceof Player) {
            final Player player = (Player) src;
            final String lastClaimType = this.lastActiveClaimTypeMap.getIfPresent(player.getUniqueId());
            String currentType = type == null ? "ALL" : type.toString();
            if (lastClaimType != null && !lastClaimType.equals(currentType.toString())) {
                PaginationUtil.getInstance().resetActivePage(player.getUniqueId());
            }
        }
        claimsTextList = CommandHelper.generateClaimTextList(claimsTextList, claims, worldName, user, src, createClaimListConsumer(src, user, type, worldUniqueId), this.canListOthers, false);

        final Component whiteOpenBracket = TextComponent.of("[");
        final Component whiteCloseBracket = TextComponent.of("]");
        Component ownedShowText = TextComponent.of("Click here to view the claims you own.");
        Component adminShowText = TextComponent.builder("")
                .append("Click here to filter by ")
                .append("ADMIN ", TextColor.RED)
                .append("type.").build();
        Component basicShowText = TextComponent.builder("")
                .append("Click here to filter by ")
                .append("BASIC ", TextColor.YELLOW)
                .append("type.").build();
        Component subdivisionShowText = TextComponent.builder("")
                .append("Click here to filter by ")
                .append("SUBDIVISION ", TextColor.AQUA)
                .append("type.").build();
        Component townShowText = TextComponent.builder("")
                .append("Click here to filter by ")
                .append("TOWN ", TextColor.GREEN)
                .append("type.").build();
        Component ownedTypeText = TextComponent.builder("")
                .append(this.displayOwned && type == null ? 
                        TextComponent.builder("")
                        .append(whiteOpenBracket)
                        .append("OWN", TextColor.GOLD)
                        .append(whiteCloseBracket).build() : TextComponent.of("OWN", TextColor.GRAY))
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createClaimListConsumer(src, user, "OWN", worldUniqueId))))
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
                .append(" Displaying : ", TextColor.AQUA)
                .append(ownedTypeText)
                .append("  ")
                .append(adminTypeText)
                .append("  ")
                .append(basicTypeText)
                .append("  ")
                .append(subTypeText)
                .append("  ")
                .append(townTypeText).build();
        final int fillSize = 20 - (claimsTextList.size() + 2);
        for (int i = 0; i < fillSize; i++) {
            claimsTextList.add(TextComponent.of(" "));
        }

        PaginationList paginationList = PaginationList.builder()
                .title(claimListHead).padding(TextComponent.of(" ").decoration(TextDecoration.STRIKETHROUGH, true)).contents(claimsTextList).build();
        Integer activePage = 1;
        if (src instanceof Player) {
            final Player player = (Player) src;
            activePage = PaginationUtil.getInstance().getActivePage(player.getUniqueId());
            if (activePage == null) {
                activePage = 1;
            }
            this.lastActiveClaimTypeMap.put(player.getUniqueId(), type == null ? "ALL" : type.toString());
        }
        paginationList.sendTo(src, activePage);
    }

    private Consumer<CommandSender> createClaimListConsumer(Player src, GDPermissionUser user, String type, UUID worldUniqueId) {
        return consumer -> {
            if (type.equalsIgnoreCase("ALL")) {
                this.displayOwned = false;
            } else {
                this.displayOwned = true;
            }
            showClaimList(src, user, null, worldUniqueId);
        };
    }

    private Consumer<CommandSender> createClaimListConsumer(Player src, GDPermissionUser user, ClaimType type, UUID worldUniqueId) {
        return consumer -> {
            this.displayOwned = false;
            showClaimList(src, user, type, worldUniqueId);
        };
    }
}