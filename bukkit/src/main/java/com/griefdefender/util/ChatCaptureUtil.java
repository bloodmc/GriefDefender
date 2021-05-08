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
package com.griefdefender.util;

import com.google.common.collect.ImmutableMap;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.claim.TrustType;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.command.CommandHelper;
import com.griefdefender.command.CommandTrustList;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.internal.pagination.PaginationList;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.text.action.GDCallbackHolder;
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
import java.util.function.Consumer;

public class ChatCaptureUtil {

    private static ChatCaptureUtil instance;
    private static final Component whiteOpenBracket = TextComponent.of("[", TextColor.AQUA);
    private static final Component whiteCloseBracket = TextComponent.of("]", TextColor.AQUA);

    public static ChatCaptureUtil getInstance() {
        return instance;
    }

    static {
        instance = new ChatCaptureUtil();
    }

    public Component createRecordChatComponent(Player player, GDClaim claim, GDPlayerData playerData, String command) {
        return this.createRecordChatComponent(player, claim, playerData, command, null);
    }

    public Component createRecordChatComponent(Player player, GDClaim claim, GDPlayerData playerData, String command, Component returnComponent) {
        final Component chatSettings = TextComponent.builder()
                    .append(TextComponent.builder()
                            .append(whiteOpenBracket)
                            .append(MessageCache.getInstance().UI_RECORD_CHAT).color(TextColor.GOLD).append(whiteCloseBracket)
                            .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createChatSettingsConsumer(player, claim, command, returnComponent))))
                            .hoverEvent(HoverEvent.showText(MessageCache.getInstance().UI_CLICK_RECORD_CHAT))
                            .build())
                    .append(TextComponent.builder()
                            .append(" ")
                            .append(getRecordChatClickableInfoText(player, claim, MessageCache.getInstance().CLAIMINFO_UI_CLICK_TOGGLE, playerData.isRecordingChat() ? TextComponent.of("ON", TextColor.GREEN) : TextComponent.of("OFF", TextColor.RED), command))
                            .build())
                    .build();
        return chatSettings;
    }

    public Component getRecordChatClickableInfoText(CommandSender src, GDClaim claim, Component clickText, Component infoText, String command) {
        boolean hasPermission = true;
        if (claim != null && src instanceof Player) {
            Component denyReason = claim.allowEdit((Player) src);
            if (denyReason != null) {
                clickText = denyReason;
                hasPermission = false;
            }
        }

        TextComponent.Builder textBuilder = TextComponent.builder()
                .append(infoText)
                .hoverEvent(HoverEvent.showText(clickText));
        if (hasPermission) {
            textBuilder.clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createChatInfoConsumer(src, claim, command))));
        }
        return textBuilder.build();
    }

    private Consumer<CommandSender> createChatInfoConsumer(CommandSender src, GDClaim claim, String command) {
        return info -> {
            if (!(src instanceof Player)) {
                return;
            }
            final Player player = (Player) src;
            final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(player);
            final GDPlayerData playerData = user.getInternalPlayerData();
            final boolean isRecordingChat = playerData.isRecordingChat();
            if (isRecordingChat) {
                playerData.recordChatTimestamp = null;
            } else {
                playerData.recordChatTimestamp = Instant.now();
                playerData.chatLines.clear();
            }
            if (command.equals("claiminfo")) {
                CommandHelper.executeCommand(src, command, claim.getUniqueId().toString());
            } else {
                CommandHelper.executeCommand(src, command, "");
            }
        };
    }

    public Consumer<CommandSender> createChatSettingsConsumer(Player player, GDClaim claim, String command, Component returnComponent) {
        return settings -> {
            PaginationList.Builder paginationBuilder = PaginationList.builder()
                    .title(TextComponent.of("RECORD-CHAT").color(TextColor.AQUA)).padding(TextComponent.of(" ").decoration(TextDecoration.STRIKETHROUGH, true)).contents(generateChatSettings(player, claim, command, returnComponent));
            paginationBuilder.sendTo(player);
        };
    }

    public List<Component> generateChatSettings(Player player, GDClaim claim, String command, Component returnComponent) {
        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        List<Component> textList = new ArrayList<>();
        Component returnToClaimInfo = null;
        if (command.equals("claiminfo")) {
            returnToClaimInfo = TextComponent.builder()
                    .append("[")
                    .append(MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.UI_CLICK_RETURN_COMMAND, 
                            ImmutableMap.of("command", command)))
                    .append("]")
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(CommandHelper.createCommandConsumer(player, command, claim.getUniqueId().toString())))).build();
        } else if (command.equals("trustlist")) {
            returnToClaimInfo = TextComponent.builder()
                    .append("[")
                    .append(MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.UI_CLICK_RETURN_COMMAND, 
                            ImmutableMap.of("command", command)))
                    .append("]")
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createTrustListConsumer(player, claim, playerData, TrustTypes.NONE, returnComponent)))).build();
        } else {
            returnToClaimInfo = TextComponent.builder()
                    .append("[")
                    .append(MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.UI_CLICK_RETURN_COMMAND, 
                            ImmutableMap.of("command", command)))
                    .append("]")
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(CommandHelper.createCommandConsumer(player, command, "")))).build();
        }
        textList.add(returnToClaimInfo);
        for (Component chatLine : playerData.chatLines) {
            textList.add(chatLine);
        }

        int fillSize = 20 - (textList.size() + 2);
        for (int i = 0; i < fillSize; i++) {
            textList.add(TextComponent.of(" "));
        }
        return textList;
    }

    private Consumer<CommandSender> createTrustListConsumer(Player src, GDClaim claim, GDPlayerData playerData, TrustType type, Component returnComponent) {
        return consumer -> {
            CommandTrustList.showTrustList(src, claim, playerData, type, new ArrayList<>(), returnComponent);
        };
    }
}
