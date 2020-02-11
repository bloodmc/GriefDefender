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
import com.griefdefender.configuration.IClaimData;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.internal.pagination.PaginationList;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.text.action.GDCallbackHolder;
import com.griefdefender.util.ChatCaptureUtil;
import com.griefdefender.util.PermissionUtil;

import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_LIST_TRUST)
public class CommandTrustList extends BaseCommand {

    @CommandAlias("trustlist")
    @Description("Manages trust for the claim you're standing in.")
    @Subcommand("trust list")
    public void execute(Player player) {
        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        final GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(playerData, player.getLocation());
        final Component message = claim.allowGrantPermission(player);
        if (message != null) {
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }
        showTrustList(player, claim, playerData, TrustTypes.NONE, new ArrayList<>(), null);
    }

    public static void showTrustList(Player src, GDClaim claim, GDPlayerData playerData, TrustType type, List<Component> messages, Component returnCommand) {
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
                        .append(MessageCache.getInstance().TITLE_ALL)
                        .append(whiteCloseBracket)
                        .build() : TextComponent.builder("")
                        .append(MessageCache.getInstance().TITLE_ALL.color(TextColor.GRAY))
                        .build())
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createTrustConsumer(src, claim, playerData, TrustTypes.NONE, returnCommand))))
                .hoverEvent(HoverEvent.showText(showAllText)).build();
        final Component accessorTrustText = TextComponent.builder("")
                .append(type == TrustTypes.ACCESSOR ? TextComponent.builder("")
                        .append(whiteOpenBracket)
                        .append(MessageCache.getInstance().TITLE_ACCESSOR.color(TextColor.YELLOW))
                        .append(whiteCloseBracket)
                        .build() : MessageCache.getInstance().TITLE_ACCESSOR.color(TextColor.GRAY))
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createTrustConsumer(src, claim, playerData, TrustTypes.ACCESSOR, returnCommand))))
                .hoverEvent(HoverEvent.showText(showAccessorText)).build();
        final Component builderTrustText = TextComponent.builder("")
                .append(type == TrustTypes.BUILDER ? TextComponent.builder("")
                        .append(whiteOpenBracket)
                        .append(MessageCache.getInstance().TITLE_BUILDER.color(TextColor.GREEN))
                        .append(whiteCloseBracket)
                        .build() : MessageCache.getInstance().TITLE_BUILDER.color(TextColor.GRAY))
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createTrustConsumer(src, claim, playerData, TrustTypes.BUILDER, returnCommand))))
                .hoverEvent(HoverEvent.showText(showBuilderText)).build();
        final Component containerTrustText = TextComponent.builder("")
                .append(type == TrustTypes.CONTAINER ? TextComponent.builder("")
                        .append(whiteOpenBracket)
                        .append(MessageCache.getInstance().TITLE_CONTAINER.color(TextColor.LIGHT_PURPLE))
                        .append(whiteCloseBracket)
                        .build() : MessageCache.getInstance().TITLE_CONTAINER.color(TextColor.GRAY))
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createTrustConsumer(src, claim, playerData, TrustTypes.CONTAINER, returnCommand))))
                .hoverEvent(HoverEvent.showText(showContainerText)).build();
        final Component managerTrustText = TextComponent.builder("")
                .append(type == TrustTypes.MANAGER ? TextComponent.builder("")
                        .append(whiteOpenBracket)
                        .append(MessageCache.getInstance().TITLE_MANAGER.color(TextColor.GOLD))
                        .append(whiteCloseBracket)
                        .build() : MessageCache.getInstance().TITLE_MANAGER.color(TextColor.GRAY))
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createTrustConsumer(src, claim, playerData, TrustTypes.MANAGER, returnCommand))))
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
        if (returnCommand != null) {
            trustList.add(returnCommand);
        }
        if (type == TrustTypes.NONE) {
            // check highest trust first
            for (UUID uuid : claim.getInternalClaimData().getManagers()) {
                final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(uuid);
                trustList.add(TextComponent.builder("")
                        .append(user.getName(), TextColor.GOLD)
                        .append(" ")
                        .append("[", TextColor.WHITE)
                        .append(TextComponent.builder()
                            .append("x", TextColor.RED)
                            .hoverEvent(HoverEvent.showText(MessageCache.getInstance().UI_CLICK_REMOVE))
                            .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(
                                    createRemoveConsumer(src, claim, playerData, type, returnCommand, claim.getInternalClaimData(), claim.getInternalClaimData().getManagers(), uuid))))
                            .build())
                        .append("]", TextColor.WHITE)
                        .build());
                userIdList.remove(user.getUniqueId());
            }

            for (UUID uuid : claim.getInternalClaimData().getBuilders()) {
                if (!userIdList.contains(uuid)) {
                    continue;
                }

                final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(uuid);
                trustList.add(TextComponent.builder("")
                        .append(user.getName(), TextColor.GOLD)
                        .append(" ")
                        .append("[", TextColor.WHITE)
                        .append(TextComponent.builder()
                            .append("x", TextColor.RED)
                            .hoverEvent(HoverEvent.showText(MessageCache.getInstance().UI_CLICK_REMOVE))
                            .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(
                                    createRemoveConsumer(src, claim, playerData, type, returnCommand, claim.getInternalClaimData(), claim.getInternalClaimData().getBuilders(), uuid))))
                            .build())
                        .append("]", TextColor.WHITE)
                        .build());
                userIdList.remove(uuid);
            }
    
            for (UUID uuid : claim.getInternalClaimData().getContainers()) {
                if (!userIdList.contains(uuid)) {
                    continue;
                }

                final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(uuid);
                trustList.add(TextComponent.builder("")
                        .append(user.getName(), TextColor.GOLD)
                        .append(" ")
                        .append("[", TextColor.WHITE)
                        .append(TextComponent.builder()
                            .append("x", TextColor.RED)
                            .hoverEvent(HoverEvent.showText(MessageCache.getInstance().UI_CLICK_REMOVE))
                            .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(
                                    createRemoveConsumer(src, claim, playerData, type, returnCommand, claim.getInternalClaimData(), claim.getInternalClaimData().getContainers(), uuid))))
                            .build())
                        .append("]", TextColor.WHITE)
                        .build());
                userIdList.remove(uuid);
            }

            for (UUID uuid : claim.getInternalClaimData().getAccessors()) {
                if (!userIdList.contains(uuid)) {
                    continue;
                }

                final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(uuid);
                trustList.add(TextComponent.builder("")
                        .append(user.getName(), TextColor.GOLD)
                        .append(" ")
                        .append("[", TextColor.WHITE)
                        .append(TextComponent.builder()
                            .append("x", TextColor.RED)
                            .hoverEvent(HoverEvent.showText(MessageCache.getInstance().UI_CLICK_REMOVE))
                            .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(
                                    createRemoveConsumer(src, claim, playerData, type, returnCommand, claim.getInternalClaimData(), claim.getInternalClaimData().getAccessors(), uuid))))
                            .build())
                        .append("]", TextColor.WHITE)
                        .build());
                userIdList.remove(uuid);
            }
        } else {
            final List<UUID> trusts = claim.getUserTrustList(type);
            trustList.add(TextComponent.builder("")
                    .append("[", TextColor.WHITE)
                    .append(TextComponent.builder()
                        .append("+", TextColor.GREEN)
                        .hoverEvent(HoverEvent.showText(TextComponent.of("Click here to add")))
                        .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(
                                createInputConsumer(src, claim, playerData, type, messages, returnCommand))))
                        .build())
                    .append("]", TextColor.WHITE)
                    .build());

            for (UUID uuid : trusts) {
                if (!userIdList.contains(uuid)) {
                    continue;
                }

                final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(uuid);
                trustList.add(TextComponent.builder("")
                        .append(user.getName(), TextColor.GOLD)
                        .append(" ")
                        .append("[", TextColor.WHITE)
                        .append(TextComponent.builder()
                            .append("x", TextColor.RED)
                            .hoverEvent(HoverEvent.showText(MessageCache.getInstance().UI_CLICK_REMOVE))
                            .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(
                                    createRemoveConsumer(src, claim, playerData, type, returnCommand, claim.getInternalClaimData(), trusts, uuid))))
                            .build())
                        .append("]", TextColor.WHITE)
                        .build());
                userIdList.remove(uuid);
            }
        }

        Component footer = null;
        int fillSize = 20 - (trustList.size() + 2);
        if (src.hasPermission(GDPermissions.CHAT_CAPTURE)) {
            fillSize = 20 - (trustList.size() + 3);
            if (messages != null && !messages.isEmpty()) {
                footer = TextComponent.builder()
                            .append(ChatCaptureUtil.getInstance().createRecordChatComponent(src, claim, playerData, "trustlist", returnCommand))
                            .append(TextComponent.of("\n"))
                            .build();
                for (Component message : messages) {
                    footer = footer.append(message);
                    fillSize -= 1;
                }
                messages.clear();
            } else {
                footer = TextComponent.builder()
                        .append(ChatCaptureUtil.getInstance().createRecordChatComponent(src, claim, playerData, "trustlist", returnCommand))
                        .build();
            }
        }

        for (int i = 0; i < fillSize; i++) {
            trustList.add(TextComponent.of(" "));
        }

        PaginationList.Builder paginationBuilder = PaginationList.builder()
                .title(claimTrustHead).padding(TextComponent.of(" ").decoration(TextDecoration.STRIKETHROUGH, true)).contents(trustList).footer(footer);
        paginationBuilder.sendTo(src);
    }

    private static Consumer<CommandSender> createTrustConsumer(Player src, GDClaim claim, GDPlayerData playerData, TrustType type, Component returnCommand) {
        return consumer -> {
            showTrustList(src, claim, playerData, type, new ArrayList<>(), returnCommand);
        };
    }

    private static Consumer<CommandSender> createInputConsumer(Player src, GDClaim claim, GDPlayerData playerData, TrustType type, List<Component> messages, Component returnCommand) {
        return consumer -> {
            if (messages == null || messages.isEmpty()) {
                playerData.commandInputTimestamp = Instant.now();
                playerData.trustAddConsumer = createAddConsumer(src, claim, playerData, type, returnCommand);
            }
            messages.add(TextComponent.builder()
                    .append(TextComponent.of("Do you want to add a ")
                    .append(TextComponent.builder()
                            .append(MessageCache.getInstance().LABEL_PLAYER.color(TextColor.GOLD))
                            .clickEvent(ClickEvent.suggestCommand("player <name>"))
                            .hoverEvent(HoverEvent.showText(MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.UI_CLICK_ADD_TARGET,
                                    ImmutableMap.of("target", "player"))))
                            .build())
                            .append(TextComponent.of(" or "))
                            .append(TextComponent.builder()
                                    .append(MessageCache.getInstance().LABEL_GROUP.color(TextColor.AQUA))
                                    .clickEvent(ClickEvent.suggestCommand("group <name>"))
                                    .hoverEvent(HoverEvent.showText(MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.UI_CLICK_ADD_TARGET,
                                            ImmutableMap.of("target", "group"))))
                                    .build()))
                    .append(" ? ")
                    .append("[")
                    .append(TextComponent.builder()
                            .append(MessageCache.getInstance().LABEL_CANCEL.color(TextColor.RED))
                            .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(
                                    createCancelConsumer(src, claim, playerData, type, returnCommand))))
                            .build())
                    .append("]")
                    .build());
            showTrustList(src, claim, playerData, type, messages, returnCommand);
        };
    }

    private static Consumer<CommandSender> createAddConsumer(Player src, GDClaim claim, GDPlayerData playerData, TrustType type, Component returnCommand) {
        return consumer -> {
            String name = playerData.commandInput;
            List<Component> messages = new ArrayList<>();
            if (playerData.commandInput.contains("player ")) {
                name = name.replace("player ", "");
                if (!name.equalsIgnoreCase("public") && PermissionUtil.getInstance().lookupUserUniqueId(name) == null) {
                    messages.add(MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.COMMAND_INVALID_PLAYER,
                            ImmutableMap.of(
                            "player", name)));
                    messages.add(TextComponent.of("\n"));
                    createInputConsumer(src, claim, playerData, type, messages, returnCommand).accept(src);
                    return;
                }
            } else if (playerData.commandInput.contains("group ")) {
                name = name.replace("group ", "");
                if (!name.equalsIgnoreCase("public") && !PermissionUtil.getInstance().hasGroupSubject(name)) {
                    messages.add(MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.COMMAND_INVALID_PLAYER,
                            ImmutableMap.of(
                            "player", name)));
                    messages.add(TextComponent.of("\n"));
                    createInputConsumer(src, claim, playerData, type, messages, returnCommand).accept(src);
                    return;
                }
            } else {
                messages.add(MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.COMMAND_INVALID_INPUT,
                        ImmutableMap.of(
                        "input", playerData.commandInput)));
                messages.add(TextComponent.of("\n"));
                createInputConsumer(src, claim, playerData, type, messages, returnCommand).accept(src);
                return;
            }
            CommandHelper.executeCommand(src, "trust", name + " " + type.getName().toLowerCase());
            playerData.commandInputTimestamp = null;
            playerData.trustAddConsumer = null;
            showTrustList(src, claim, playerData, type, messages, returnCommand);
        };
    }

    private static Consumer<CommandSender> createCancelConsumer(Player src, GDClaim claim, GDPlayerData playerData, TrustType type, Component returnCommand) {
        return consumer -> {
            playerData.commandInputTimestamp = null;
            playerData.trustAddConsumer = null;
            showTrustList(src, claim, playerData, type, new ArrayList<>(), returnCommand);
        };
    }

    private static Consumer<CommandSender> createRemoveConsumer(Player src, GDClaim claim, GDPlayerData playerData, TrustType type, Component returnCommand, IClaimData data, List<UUID> trustList, UUID uuid) {
        return consumer -> {
            trustList.remove(uuid);
            data.setRequiresSave(true);
            data.save();
            showTrustList(src, claim, playerData, type, new ArrayList<>(), returnCommand);
        };
    }
}
