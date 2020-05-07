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
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.ImmutableMap;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimContexts;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.ContextKeys;
import com.griefdefender.api.permission.PermissionResult;
import com.griefdefender.api.permission.ResultTypes;
import com.griefdefender.api.permission.flag.Flag;
import com.griefdefender.api.permission.flag.FlagData;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.configuration.category.CustomFlagGroupCategory;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.event.GDFlagPermissionEvent;
import com.griefdefender.internal.pagination.PaginationList;
import com.griefdefender.internal.util.NMSUtil;
import com.griefdefender.permission.GDPermissionHolder;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.permission.flag.GDActiveFlagData;
import com.griefdefender.permission.flag.GDFlagDefinition;
import com.griefdefender.permission.ui.UIFlagData;
import com.griefdefender.permission.ui.MenuType;
import com.griefdefender.permission.ui.UIHelper;
import com.griefdefender.permission.ui.UIFlagData.FlagContextHolder;
import com.griefdefender.registry.FlagRegistryModule;
import com.griefdefender.text.action.GDCallbackHolder;
import com.griefdefender.util.CauseContextHelper;
import com.griefdefender.util.ChatCaptureUtil;
import com.griefdefender.util.PaginationUtil;
import com.griefdefender.util.PermissionUtil;

import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.spongeapi.TextAdapter;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public abstract class ClaimFlagBase extends BaseCommand {

    private static final Component whiteOpenBracket = TextComponent.of("[", TextColor.AQUA);
    private static final Component whiteCloseBracket = TextComponent.of("]", TextColor.AQUA);
    protected GDPermissionHolder subject;
    protected ClaimSubjectType subjectType;
    protected String friendlySubjectName;
    private final Cache<UUID, String> lastActivePresetMenuMap = Caffeine.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES)
            .build();
    private final Cache<UUID, String> lastActiveMenuTypeMap = Caffeine.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES)
            .build();

    protected ClaimFlagBase(ClaimSubjectType type) {
        this.subjectType = type;
    }

    public void execute(Player player, String[] args) throws InvalidCommandArgument {
        final GDPermissionUser src = PermissionHolderCache.getInstance().getOrCreateUser(player);
        final GDPermissionHolder commandSubject = subject;
        final GDPlayerData playerData = src.getInternalPlayerData();
        final GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(playerData, player.getLocation());
        String commandFlag = null;
        String target = null;
        String value = null;
        String contexts = null;
        final String arguments = String.join(" ", args);
        int index = arguments.indexOf("context[");
        if (index != -1) {
            contexts = arguments.substring(index, arguments.length());
        }
        if (args.length > 0) {
            if (!src.getInternalPlayerData().canIgnoreClaim(claim) && !player.hasPermission(GDPermissions.COMMAND_FLAGS_CLAIM_ARG)) {
                TextAdapter.sendComponent(player, MessageCache.getInstance().PERMISSION_FLAG_ARG);
                return;
            }
            if (args.length < 3) {
                throw new InvalidCommandArgument();
            }
            commandFlag = args[0];
            target = args[1];
            value = args[2];
        } else {
            if (!src.getInternalPlayerData().canIgnoreClaim(claim) && !player.hasPermission(GDPermissions.COMMAND_FLAGS_CLAIM_GUI)) {
                TextAdapter.sendComponent(player, MessageCache.getInstance().PERMISSION_FLAG_GUI);
                return;
            }
        }
        final Flag flag = FlagRegistryModule.getInstance().getById(commandFlag).orElse(null);
        if (commandFlag != null && flag == null) {
            TextAdapter.sendComponent(player, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.FLAG_NOT_FOUND, ImmutableMap.of(
                    "flag", commandFlag)));
            return;
        }

        if (flag != null && !player.hasPermission(GDPermissions.USER_CLAIM_FLAGS + "." + flag.getName().toLowerCase())) {
            TextAdapter.sendComponent(player, MessageCache.getInstance().PERMISSION_FLAG_USE);
            return;
        }

        if (target != null && target.equalsIgnoreCase("hand")) {
            ItemStack stack = NMSUtil.getInstance().getActiveItem(player);
            if (stack != null) {
                target = GDPermissionManager.getInstance().getPermissionIdentifier(stack);
            }
        }

        String flagPermission = flag != null ? flag.getPermission() : "";
        final Set<Context> contextSet = CauseContextHelper.generateContexts(flagPermission, player, claim, contexts);
        if (contextSet == null) {
            return;
        }

        if (claim != null) {
            if (flag == null && value == null && player.hasPermission(GDPermissions.COMMAND_LIST_CLAIM_FLAGS)) {
                String defaultGroup = "";
                for (Entry<String, CustomFlagGroupCategory> groupEntry : GriefDefenderPlugin.getInstance().flagConfig.getConfig().customFlags.getGroups().entrySet()) {
                    final String permission = groupEntry.getValue().isAdminGroup() ? GDPermissions.FLAG_CUSTOM_ADMIN_BASE : GDPermissions.FLAG_CUSTOM_USER_BASE;
                    if (!player.hasPermission(permission + "." + groupEntry.getKey()) && !src.getInternalPlayerData().canIgnoreClaim(claim)) {
                        continue;
                    }
                    defaultGroup = groupEntry.getKey();
                    break;
                }
                if (!defaultGroup.isEmpty()) {
                    showCustomFlags(src, claim, defaultGroup);
                } else {
                    TextAdapter.sendComponent(player, MessageCache.getInstance().PERMISSION_FLAG_USE);
                }
                return;
            } else if (flag != null && value != null) {
                GDCauseStackManager.getInstance().pushCause(player);
                PermissionResult result = CommandHelper.addFlagPermission(player, this.subject, claim, flag, target, PermissionUtil.getInstance().getTristateFromString(value.toUpperCase()), contextSet);
                final String flagTarget = target;
                if (result.getResultType() == ResultTypes.TARGET_NOT_VALID) {
                    GriefDefenderPlugin.sendMessage(player, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.FLAG_INVALID_TARGET,
                            ImmutableMap.of("target", flagTarget,
                                    "flag", flag)));
                } else if (result.getResultType() == ResultTypes.NO_PERMISSION) {
                    GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().PERMISSION_FLAG_USE);
                }
                GDCauseStackManager.getInstance().popCause();
                return;
            }
        } else {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CLAIM_NOT_FOUND);
        }
    }

    protected void showCustomFlags(GDPermissionUser src, GDClaim claim, String flagGroup) {
        final Player player = src.getOnlinePlayer();
        final String lastPermissionMenuType = this.lastActivePresetMenuMap.getIfPresent(player.getUniqueId());
        if (lastPermissionMenuType != null && !lastPermissionMenuType.equalsIgnoreCase(flagGroup.toLowerCase())) {
            PaginationUtil.getInstance().resetActivePage(player.getUniqueId());
        }

        TextComponent.Builder flagHeadBuilder = TextComponent.builder()
                .append(" Displaying :", TextColor.AQUA);
        final Map<String, CustomFlagGroupCategory> flagGroups = GriefDefenderPlugin.getInstance().flagConfig.getConfig().customFlags.getGroups();
        List<String> groups = new ArrayList<>();
        for (Map.Entry<String, CustomFlagGroupCategory> flagGroupEntry : flagGroups.entrySet()) {
            final CustomFlagGroupCategory flagGroupCat = flagGroupEntry.getValue();
            if (!flagGroupCat.isEnabled()) {
                continue;
            }
            final String groupName = flagGroupEntry.getKey();
            final boolean isAdminGroup = GriefDefenderPlugin.getInstance().flagConfig.getConfig().customFlags.getGroups().get(groupName).isAdminGroup();
            final String permission = isAdminGroup ? GDPermissions.FLAG_CUSTOM_ADMIN_BASE : GDPermissions.FLAG_CUSTOM_USER_BASE;
            if (!player.hasPermission(permission + "." + groupName) && !src.getInternalPlayerData().canIgnoreClaim(claim)) {
                continue;
            }

            groups.add(groupName);
        }

        final CustomFlagGroupCategory flagGroupCat = flagGroups.get(flagGroup);
        if (flagGroupCat == null || flagGroupCat.getFlagDefinitions().isEmpty()) {
            TextAdapter.sendComponent(player, TextComponent.of("No custom flag definitions were found for group '" + flagGroup + "'."));
            return;
        }

        Collections.sort(groups);
        for (String group : groups) {
            flagHeadBuilder.append(" ").append(flagGroup.equalsIgnoreCase(group) ? TextComponent.builder()
                    .append(whiteOpenBracket)
                    .append(group.toUpperCase(), flagGroups.get(group).isAdminGroup() ? TextColor.RED : TextColor.GOLD)
                    .append(whiteCloseBracket).build() : 
                        TextComponent.builder().append(group.toUpperCase(), TextColor.GRAY)
                            .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createCustomFlagConsumer(src, claim, group))))
                            .build());
        }

        List<Component> textComponents = new ArrayList<>();
        for (GDFlagDefinition customFlag : flagGroupCat.getFlagDefinitions().values()) {
            if (customFlag.isEnabled()) {
                Component flagText = TextComponent.builder()
                    .append(getCustomFlagText(customFlag))
                    .append(" ")
                    .append(this.getCustomClickableText(src, claim, customFlag, flagGroup))
                    .build();
                textComponents.add(flagText);
            }
        }


        Collections.sort(textComponents, UIHelper.PLAIN_COMPARATOR);
        String lastMenu = this.lastActiveMenuTypeMap.getIfPresent(src.getUniqueId());
        MenuType lastActiveMenu = MenuType.CLAIM;
        if (lastMenu != null) {
            lastActiveMenu = MenuType.valueOf(lastMenu.toUpperCase());
        }

        int fillSize = 20 - (textComponents.size() + 2);
        Component footer = null;
        if (player.hasPermission(GDPermissions.ADVANCED_FLAGS)) {
            footer = TextComponent.builder().append(whiteOpenBracket)
                .append(TextComponent.of("PRESET").color(TextColor.GOLD)).append(whiteCloseBracket)
                .append(" ")
                .append(TextComponent.builder()
                        .append(TextComponent.of("ADVANCED").color(TextColor.GRAY)
                        .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createClaimFlagConsumer(src, claim, lastActiveMenu)))))
                        .build())
                .build();
            if (player.hasPermission(GDPermissions.CHAT_CAPTURE)) {
                footer = footer.append(TextComponent.builder()
                    .append("\n")
                    .append(ChatCaptureUtil.getInstance().createRecordChatComponent(player, claim, src.getInternalPlayerData(), "claimflag"))
                    .build());
                fillSize = 20 - (textComponents.size() + 3);
            }
        } else if (player.hasPermission(GDPermissions.CHAT_CAPTURE)) {
            footer = TextComponent.builder().append(ChatCaptureUtil.getInstance().createRecordChatComponent(player, claim, src.getInternalPlayerData(), "claimflag")).build();
            fillSize = 20 - (textComponents.size() + 3);
        }

        for (int i = 0; i < fillSize; i++) {
            textComponents.add(TextComponent.of(" "));
        }
        PaginationList.Builder paginationBuilder = PaginationList.builder()
                .title(flagHeadBuilder.build()).padding(TextComponent.builder(" ").decoration(TextDecoration.STRIKETHROUGH, true).build()).contents(textComponents).footer(footer);
        final PaginationList paginationList = paginationBuilder.build();
        Integer activePage = 1;
        activePage = PaginationUtil.getInstance().getActivePage(player.getUniqueId());
        if (activePage == null) {
            activePage = 1;
        }
        this.lastActivePresetMenuMap.put(player.getUniqueId(), flagGroup.toLowerCase());
        paginationList.sendTo(player, activePage);
    }

    protected void showFlagPermissions(GDPermissionUser src, GDClaim claim, MenuType displayType) {
        final Player player = src.getOnlinePlayer();
        boolean isAdmin = false;
        if (player.hasPermission(GDPermissions.DELETE_CLAIM_ADMIN)) {
            isAdmin = true;
        }

        final String lastPermissionMenuType = this.lastActiveMenuTypeMap.getIfPresent(player.getUniqueId());
        if (lastPermissionMenuType != null && !lastPermissionMenuType.equalsIgnoreCase(displayType.name())) {
            PaginationUtil.getInstance().resetActivePage(player.getUniqueId());
        }

        final Component showOverrideText = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.UI_CLICK_FILTER_TYPE, 
                ImmutableMap.of("type", TextComponent.of("OVERRIDE", TextColor.RED)));
        final Component showDefaultText = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.UI_CLICK_FILTER_TYPE, 
                ImmutableMap.of("type", TextComponent.of("DEFAULT", TextColor.LIGHT_PURPLE)));
        final Component showClaimText = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.UI_CLICK_FILTER_TYPE, 
                ImmutableMap.of("type", TextComponent.of("CLAIM", TextColor.GOLD)));
        final Component showInheritText = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.UI_CLICK_FILTER_TYPE, 
                ImmutableMap.of("type", TextComponent.of("INHERIT", TextColor.AQUA)));
        Component defaultFlagText = TextComponent.empty();
        if (isAdmin) {
            defaultFlagText = TextComponent.builder("")
                    .append(displayType == MenuType.DEFAULT ? TextComponent.builder("")
                            .append(whiteOpenBracket)
                            .append("DEFAULT", TextColor.LIGHT_PURPLE)
                            .append(whiteCloseBracket).build() : TextComponent.of("DEFAULT", TextColor.GRAY))
                    .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createClaimFlagConsumer(src, claim, MenuType.DEFAULT))))
                    .hoverEvent(HoverEvent.showText(showDefaultText)).build();
        }
        final Component overrideFlagText = TextComponent.builder("")
                .append(displayType == MenuType.OVERRIDE ? TextComponent.builder("")
                        .append(whiteOpenBracket)
                        .append("OVERRIDE", TextColor.RED)
                        .append(whiteCloseBracket).build() : TextComponent.of("OVERRIDE", TextColor.GRAY))
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createClaimFlagConsumer(src, claim, MenuType.OVERRIDE))))
                .hoverEvent(HoverEvent.showText(showOverrideText)).build();
        final Component claimFlagText = TextComponent.builder("")
                .append(displayType == MenuType.CLAIM ? TextComponent.builder("")
                        .append(whiteOpenBracket)
                        .append("CLAIM", TextColor.YELLOW)
                        .append(whiteCloseBracket).build() : TextComponent.of("CLAIM", TextColor.GRAY))
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createClaimFlagConsumer(src, claim, MenuType.CLAIM))))
                .hoverEvent(HoverEvent.showText(showClaimText)).build();
        final Component inheritFlagText = TextComponent.builder("")
                .append(displayType == MenuType.INHERIT ? TextComponent.builder("")
                        .append(whiteOpenBracket)
                        .append("INHERIT", TextColor.AQUA)
                        .append(whiteCloseBracket).build() : TextComponent.of("INHERIT", TextColor.GRAY))
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createClaimFlagConsumer(src, claim, MenuType.INHERIT))))
                .hoverEvent(HoverEvent.showText(showInheritText)).build();
        Component claimFlagHead = TextComponent.empty();
        if (this.subjectType == ClaimSubjectType.GLOBAL) {
            if (isAdmin) {
                claimFlagHead = TextComponent.builder("")
                        .append(" Displaying : ", TextColor.AQUA)
                        .append(defaultFlagText)
                        .append("  ")
                        .append(claimFlagText)
                        .append("  ")
                        .append(inheritFlagText)
                        .append("  ")
                        .append(overrideFlagText).build();
            } else {
                claimFlagHead = TextComponent.builder("")
                        .append(" Displaying : ", TextColor.AQUA)
                        .append(claimFlagText)
                        .append("  ")
                        .append(inheritFlagText)
                        .append("  ")
                        .append(overrideFlagText).build();
            }
        } else {
            claimFlagHead = TextComponent.builder("")
                    .append(" " + this.subjectType.getFriendlyName() + " ", TextColor.AQUA)
                    .append(this.friendlySubjectName, TextColor.YELLOW)
                    .append(" : ", TextColor.AQUA)
                    .append(claimFlagText)
                    .append("  ")
                    .append(inheritFlagText)
                    .append("  ")
                    .append(overrideFlagText).build();
        }

        Set<Context> defaultContexts = new HashSet<>();
        Set<Context> overrideContexts = new HashSet<>();
        if (claim.isAdminClaim()) {
            defaultContexts.add(ClaimContexts.ADMIN_DEFAULT_CONTEXT);
            overrideContexts.add(ClaimContexts.ADMIN_OVERRIDE_CONTEXT);
        } else if (claim.isBasicClaim() || claim.isSubdivision()) {
            defaultContexts.add(ClaimContexts.BASIC_DEFAULT_CONTEXT);
            overrideContexts.add(ClaimContexts.BASIC_OVERRIDE_CONTEXT);
        } else if (claim.isTown()) {
            defaultContexts.add(ClaimContexts.TOWN_DEFAULT_CONTEXT);
            overrideContexts.add(ClaimContexts.TOWN_OVERRIDE_CONTEXT);
        } else {
            defaultContexts.add(ClaimContexts.WILDERNESS_DEFAULT_CONTEXT);
            overrideContexts.add(ClaimContexts.WILDERNESS_OVERRIDE_CONTEXT);
        }
        defaultContexts.add(claim.getWorldContext());
        defaultContexts.add(ClaimContexts.GLOBAL_DEFAULT_CONTEXT);
        overrideContexts.add(claim.getWorldContext());
        overrideContexts.add(ClaimContexts.GLOBAL_OVERRIDE_CONTEXT);
        overrideContexts.add(claim.getOverrideClaimContext());

        Map<String, UIFlagData> filteredContextMap = new HashMap<>();
        for (Map.Entry<Set<Context>, Map<String, Boolean>> mapEntry : PermissionUtil.getInstance().getTransientPermissions(this.subject).entrySet()) {
            final Set<Context> contextSet = mapEntry.getKey();
            if (contextSet.contains(claim.getDefaultTypeContext())) {
                this.addFilteredContexts(filteredContextMap, contextSet, MenuType.DEFAULT, mapEntry.getValue());
            } else if (contextSet.contains(ClaimContexts.GLOBAL_DEFAULT_CONTEXT)) {
                this.addFilteredContexts(filteredContextMap, contextSet, MenuType.DEFAULT, mapEntry.getValue());
            }
        }

        final List<Claim> inheritParents = claim.getInheritedParents();
        for (Map.Entry<Set<Context>, Map<String, Boolean>> mapEntry : PermissionUtil.getInstance().getPermanentPermissions(this.subject).entrySet()) {
            final Set<Context> contextSet = mapEntry.getKey();
            if (contextSet.contains(claim.getDefaultTypeContext())) {
                this.addFilteredContexts(filteredContextMap, contextSet, MenuType.DEFAULT, mapEntry.getValue());
            } else if (contextSet.contains(ClaimContexts.GLOBAL_DEFAULT_CONTEXT)) {
                this.addFilteredContexts(filteredContextMap, contextSet, MenuType.DEFAULT, mapEntry.getValue());
            }
            if (displayType != MenuType.DEFAULT) {
                if (contextSet.contains(claim.getContext())) {
                    this.addFilteredContexts(filteredContextMap, contextSet, MenuType.CLAIM, mapEntry.getValue());
                }
                for (Claim parentClaim : inheritParents) {
                    GDClaim parent = (GDClaim) parentClaim;
                    // check parent context
                    if (contextSet.contains(parent.getContext())) {
                        this.addFilteredContexts(filteredContextMap, contextSet, MenuType.INHERIT, mapEntry.getValue());
                    }
                }
                if (contextSet.contains(ClaimContexts.GLOBAL_OVERRIDE_CONTEXT)) {
                    this.addFilteredContexts(filteredContextMap, contextSet, MenuType.OVERRIDE, mapEntry.getValue());
                }
                if (contextSet.contains(claim.getOverrideClaimContext())) {
                    this.addFilteredContexts(filteredContextMap, contextSet, MenuType.OVERRIDE, mapEntry.getValue());
                } else if (contextSet.contains(claim.getOverrideTypeContext())) {
                    this.addFilteredContexts(filteredContextMap, contextSet, MenuType.OVERRIDE, mapEntry.getValue());
                }
            }
        }

        final Map<String, Map<Integer, Component>> textMap = new TreeMap<>();
        for (Entry<String, UIFlagData> mapEntry : filteredContextMap.entrySet()) {
            final UIFlagData flagData = mapEntry.getValue();
            final Flag flag = flagData.flag;
            for (FlagContextHolder flagHolder : flagData.flagContextMap.values()) {
                if (displayType != MenuType.CLAIM && flagHolder.getType() != displayType) {
                    continue;
                }

                final Set<Context> contexts = flagHolder.getAllContexts();
                Component flagText = TextComponent.builder()
                    .append(getFlagText(flag, contexts))
                    .append(" ")
                    .append(this.getClickableText(src, claim, flag, flagHolder, contexts, displayType))
                    .build();
                final int hashCode = Objects.hash(flag.getPermission(), contexts);
                Map<Integer, Component> componentMap = textMap.get(flag.getPermission());
                if (componentMap == null) {
                    componentMap = new HashMap<>();
                    componentMap.put(hashCode, flagText);
                    textMap.put(flag.getPermission(), componentMap);
                } else {
                    componentMap.put(hashCode, flagText);
                }
            }
        }

        List<Component> textList = new ArrayList<>();
        for (Entry<String, Map<Integer, Component>> mapEntry : textMap.entrySet()) {
            textList.addAll(mapEntry.getValue().values());
        }

        Collections.sort(textList, UIHelper.PLAIN_COMPARATOR);
        String lastActivePresetMenu = this.lastActivePresetMenuMap.getIfPresent(src.getUniqueId());
        if (lastActivePresetMenu == null) {
            lastActivePresetMenu = "user";
        }

        int fillSize = 20 - (textList.size() + 2);
        Component footer = null;
        if (player.hasPermission(GDPermissions.ADVANCED_FLAGS)) {
            footer = TextComponent.builder().append(TextComponent.builder()
                    .append(TextComponent.of("PRESET").color(TextColor.GRAY)
                    .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createCustomFlagConsumer(src, claim, lastActivePresetMenu)))))
                    .build())
                .append(" ")
                .append(whiteOpenBracket)
                .append(TextComponent.of("ADVANCED").color(TextColor.RED))
                .append(whiteCloseBracket)
                .build();
            if (player.hasPermission(GDPermissions.CHAT_CAPTURE)) {
                footer = footer.append(TextComponent.builder()
                    .append("\n")
                    .append(ChatCaptureUtil.getInstance().createRecordChatComponent(player, claim, src.getInternalPlayerData(), "claimflag"))
                    .build());
                fillSize = 20 - (textList.size() + 3);
            }
        } else if (player.hasPermission(GDPermissions.CHAT_CAPTURE)) {
            footer = TextComponent.builder().append(ChatCaptureUtil.getInstance().createRecordChatComponent(player, claim, src.getInternalPlayerData(), "claimflag")).build();
            fillSize = 20 - (textList.size() + 3);
        }

        for (int i = 0; i < fillSize; i++) {
            textList.add(TextComponent.of(" "));
        }

        PaginationList.Builder paginationBuilder = PaginationList.builder()
                .title(claimFlagHead).padding(TextComponent.builder(" ").decoration(TextDecoration.STRIKETHROUGH, true).build()).contents(textList).footer(footer);
        final PaginationList paginationList = paginationBuilder.build();
        Integer activePage = 1;
        activePage = PaginationUtil.getInstance().getActivePage(player.getUniqueId());
        if (activePage == null) {
            activePage = 1;
        }
        this.lastActiveMenuTypeMap.put(player.getUniqueId(), displayType.name().toLowerCase());
        paginationList.sendTo(player, activePage);
    }

    private void addFilteredContexts(Map<String, UIFlagData> filteredContextMap, Set<Context> contexts, MenuType type, Map<String, Boolean> permissions) {
        for (Map.Entry<String, Boolean> permissionEntry : permissions.entrySet()) {
            final Flag flag = FlagRegistryModule.getInstance().getById(permissionEntry.getKey()).orElse(null);
            if (flag == null) {
                continue;
            }
            final UIFlagData flagData = filteredContextMap.get(permissionEntry.getKey());
            if (flagData != null) {
                flagData.addContexts(flag, permissionEntry.getValue(), type, contexts);
            } else {
                filteredContextMap.put(permissionEntry.getKey(), new UIFlagData(flag, permissionEntry.getValue(), type, contexts));
            }
        }
    }

    private Component getCustomFlagText(GDFlagDefinition customFlag) {
        TextComponent definitionType = TextComponent.empty();
        TextColor flagColor = TextColor.YELLOW;
        if (customFlag.isAdmin()) {
            for (Context context : customFlag.getContexts()) {
                if (context.getKey().contains("default")) {
                    definitionType = TextComponent.builder()
                            .append("\n")
                            .append(MessageCache.getInstance().LABEL_TYPE.color(TextColor.AQUA))
                            .append(" : ", TextColor.WHITE)
                            .append("DEFAULT", TextColor.LIGHT_PURPLE)
                            .append(" ")
                            .append(context.getValue().toUpperCase(), TextColor.GRAY)
                            .build();
                    flagColor = TextColor.LIGHT_PURPLE;
                } else if (context.getKey().contains("override")) {
                    definitionType = TextComponent.builder()
                            .append("\n")
                            .append(MessageCache.getInstance().LABEL_TYPE.color(TextColor.AQUA))
                            .append(" : ", TextColor.WHITE)
                            .append("OVERRIDE", TextColor.RED)
                            .append(" ")
                            .append(context.getValue().toUpperCase(), TextColor.GRAY)
                            .build();
                    flagColor = TextColor.RED;
                }
            }
        }
        if (definitionType == TextComponent.empty()) {
            definitionType = TextComponent.builder()
                    .append("\n")
                    .append(MessageCache.getInstance().LABEL_TYPE.color(TextColor.AQUA))
                    .append(" : ", TextColor.WHITE)
                    .append("CLAIM", TextColor.YELLOW)
                    .build();
        }
        final Component baseFlagText = TextComponent.builder()
                .append(customFlag.getName(), flagColor)
                .append(" ")
                .hoverEvent(HoverEvent.showText(TextComponent.builder()
                        .append(customFlag.getDescription())
                        .append(definitionType)
                        .build())).build();
        return baseFlagText;
    }

    private Component getFlagText(Flag flag, Set<Context> contexts) {
        boolean customContext = UIHelper.containsCustomContext(contexts);

        final Component baseFlagText = TextComponent.builder().color(customContext ? TextColor.YELLOW : TextColor.GREEN).append(flag.getName() + " ")
                .hoverEvent(HoverEvent.showText(TextComponent.builder()
                        .append(flag.getDescription())
                        .build())).build();
        return baseFlagText;
    }

    private Component getCustomClickableText(GDPermissionUser src, GDClaim claim, GDFlagDefinition customFlag, String flagGroup) {
        boolean hasHover = false;
        TextComponent.Builder hoverBuilder = TextComponent.builder();
        final Player player = src.getOnlinePlayer();
        boolean hasEditPermission = true;
        Component denyReason = claim.allowEdit(player);
        if (denyReason != null) {
            hoverBuilder.append(denyReason).append("\n");
            hasEditPermission = false;
            hasHover = true;
        }

        final boolean isAdminGroup = GriefDefenderPlugin.getInstance().flagConfig.getConfig().customFlags.getGroups().get(flagGroup).isAdminGroup();
        final String permission = isAdminGroup ? GDPermissions.FLAG_CUSTOM_ADMIN_BASE : GDPermissions.FLAG_CUSTOM_USER_BASE;
         // check flag perm
        if (!player.hasPermission(permission + "." + flagGroup + "." + customFlag.getName())) {
            hoverBuilder.append(MessageCache.getInstance().PERMISSION_FLAG_USE).append("\n");
            hasEditPermission = false;
            hasHover = true;
        }

        final List<GDActiveFlagData> definitionResults = new ArrayList<>();
        boolean hasClaimContext = false;
        Set<Context> definitionContexts = new HashSet<>(customFlag.getContexts());
        for (Context context : definitionContexts) {
            if (context.getKey().contains("gd_claim")) {
                // Admins can set 'gd_claim' context value to 'claim' to represent it should be replaced with each claim UUID
                if (context.getValue().equalsIgnoreCase("claim")) {
                    definitionContexts.remove(context);
                    hasClaimContext = true;
                    break;
                }
            }
        }
        if (hasClaimContext) {
            definitionContexts.add(claim.getContext());
        }
        boolean hasOverride = false;
        for (FlagData flagData : customFlag.getFlagData()) {
            if (customFlag.isAdmin()) {
                definitionResults.add(this.getSpecificDefinitionResult(claim, customFlag, flagData));
            } else {
                final GDActiveFlagData activeData = this.getActiveDefinitionResult(claim, customFlag, flagData);
                definitionResults.add(activeData);
                if (activeData.getType() == GDActiveFlagData.Type.OVERRIDE) {
                    hasOverride = true;
                    hasEditPermission = false;
                }
            }
        }
        boolean properResult = true;
        Tristate defaultResult = null;
        for (GDActiveFlagData definitionFlagData : definitionResults) {
            final Tristate result = definitionFlagData.getValue();
            if (defaultResult == null) {
                defaultResult = result;
            } else if (defaultResult != result) {
                properResult = false;
                break;
            }
        }

        TextComponent.Builder valueBuilder = TextComponent.builder();
        if (!properResult) {
            if (hasEditPermission) {
                hoverBuilder.append("Conflict Data : \n");
                for (GDActiveFlagData definitionFlagData : definitionResults) {
                    hoverBuilder.append(definitionFlagData.getComponent())
                                .append("\n");
                }
                hasHover = true;
            }
            valueBuilder.append("partial");
            defaultResult = null;
        } else {
            TextColor valueColor = TextColor.GRAY;
            if (defaultResult == Tristate.TRUE) {
                valueColor = TextColor.GREEN;
            } else if (defaultResult == Tristate.FALSE) {
                valueColor = TextColor.RED;
            }
            valueBuilder.append(String.valueOf(defaultResult).toLowerCase(), valueColor);
        }

        if (hasEditPermission) {
            if (defaultResult == Tristate.TRUE) {
                hoverBuilder.append(MessageCache.getInstance().FLAG_UI_CLICK_DENY);
            } else {
                hoverBuilder.append(MessageCache.getInstance().FLAG_UI_CLICK_ALLOW);
            }

            if (properResult) {
                hoverBuilder.append("\nDefault Value: ", TextColor.AQUA);
                final Tristate defaultValue = customFlag.getDefaultValue();
                if (defaultValue == Tristate.UNDEFINED) {
                    hoverBuilder.append(customFlag.getDefaultValue().toString().toLowerCase(), TextColor.GRAY);
                } else if (defaultValue == Tristate.TRUE) {
                    hoverBuilder.append(customFlag.getDefaultValue().toString().toLowerCase(), TextColor.GREEN);
                } else {
                    hoverBuilder.append(customFlag.getDefaultValue().toString().toLowerCase(), TextColor.RED);
                }
    
                if (!customFlag.getContexts().isEmpty()) {
                    hoverBuilder.append("\nDefinition Contexts: ");
                    if (!customFlag.isAdmin()) {
                        hoverBuilder.append("gd_claim", TextColor.AQUA)
                        .append("=", TextColor.WHITE)
                        .append(claim.getUniqueId().toString(), TextColor.GRAY);
                    }
                    for (Context context : customFlag.getContexts()) {
                        if (!customFlag.isAdmin() && context.getKey().contains("gd_claim")) {
                            continue;
                        }
                        hoverBuilder.append("\n");
                        final String key = context.getKey();
                        final String value = context.getValue();
                        TextColor keyColor = TextColor.AQUA;
                        if (key.contains("default")) {
                            keyColor = TextColor.LIGHT_PURPLE;
                        } else if (key.contains("override")) {
                            keyColor = TextColor.RED;
                        } else if (key.contains("server")) {
                            keyColor = TextColor.GRAY;
                        }
                        hoverBuilder.append(key, keyColor)
                                .append("=", TextColor.WHITE)
                                .append(value.replace("minecraft:", ""), TextColor.GRAY);
                    }
                }

                for (FlagData flagData : customFlag.getFlagData()) {
                    hoverBuilder.append("\nFlag: ")
                                .append(flagData.getFlag().getName(), TextColor.GREEN);

                    if (!flagData.getContexts().isEmpty()) {
                        hoverBuilder.append("\nContexts: ");
                    }
                    for (Context context : flagData.getContexts()) {
                        if (!customFlag.isAdmin() && context.getKey().contains("gd_claim")) {
                            continue;
                        }
                        hoverBuilder.append("\n");
                        final String key = context.getKey();
                        final String value = context.getValue();
                        TextColor keyColor = TextColor.AQUA;
                        hoverBuilder.append(key, keyColor)
                                .append("=", TextColor.WHITE)
                                .append(value.replace("minecraft:", ""), TextColor.GRAY);
                    }

                    // show active value
                    final GDActiveFlagData activeData = this.getActiveDefinitionResult(claim, customFlag, flagData);
                    hoverBuilder.append("\nActive Result: ")
                                .append("\nvalue", TextColor.DARK_AQUA)
                                .append("=", TextColor.WHITE)
                                .append(activeData.getValue().name().toLowerCase(), TextColor.GOLD)
                                .append("\ntype", TextColor.DARK_AQUA)
                                .append("=", TextColor.WHITE)
                                .append(activeData.getType().name() + "\n", activeData.getColor());
                }

                hasHover = true;
            }
        } else {
            if (hasOverride) {
                hoverBuilder.append(MessageCache.getInstance().FLAG_UI_OVERRIDE_NO_PERMISSION);
                hasHover = true;
            }
        }

        if (hasHover) {
            valueBuilder.hoverEvent(HoverEvent.showText(hoverBuilder.build()));
        }
        TextComponent.Builder textBuilder = null;
        if (hasEditPermission) {
            textBuilder = TextComponent.builder()
            .append(valueBuilder
                    .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createCustomFlagConsumer(src, claim, customFlag, defaultResult, flagGroup))))
                    .build());
        } else {
            textBuilder = TextComponent.builder()
                    .append(valueBuilder
                            .build());
        }

        return textBuilder.build();
    }

    public GDActiveFlagData getSpecificDefinitionResult(GDClaim claim, GDFlagDefinition flagDefinition, FlagData flagData) {
        Set<Context> contexts = new HashSet<>(flagData.getContexts());
        contexts.addAll(flagDefinition.getContexts());

        boolean hasClaimContext = false;
        boolean hasOverrideClaimContext = false;
        boolean hasDefaultClaimContext = false;
        boolean replaceClaimContext = false;
        final Iterator<Context> iterator = contexts.iterator();
        while (iterator.hasNext()) {
            final Context context = iterator.next();
            if (context.getKey().equalsIgnoreCase("gd_claim")) {
                hasClaimContext = true;
                if (context.getValue().equalsIgnoreCase("claim")) {
                    iterator.remove();
                    replaceClaimContext = true;
                }
            } else if (context.getKey().equalsIgnoreCase("gd_claim_default")) {
                hasDefaultClaimContext = true;
            } else if (context.getKey().equalsIgnoreCase("gd_claim_override")) {
                hasOverrideClaimContext = true;
            }
        }

        GDActiveFlagData.Type type = GDActiveFlagData.Type.DEFAULT;
        if (hasClaimContext) {
            type = GDActiveFlagData.Type.CLAIM;
        } else if (hasOverrideClaimContext) {
            type = GDActiveFlagData.Type.OVERRIDE;
        }

        if (replaceClaimContext || !flagDefinition.isAdmin()) {
            contexts.add(claim.getContext());
        }

        Tristate result = PermissionUtil.getInstance().getPermissionValue(claim, GriefDefenderPlugin.DEFAULT_HOLDER, flagData.getFlag().getPermission(), contexts);
        if (result != Tristate.UNDEFINED) {
            return new GDActiveFlagData(flagDefinition, flagData, result, contexts, type);
        }

        return new GDActiveFlagData(flagDefinition, flagData, result, contexts, GDActiveFlagData.Type.UNDEFINED);
    }

    public GDActiveFlagData getActiveDefinitionResult(GDClaim claim, GDFlagDefinition flagDefinition, FlagData flagData) {
        Set<Context> contexts = new HashSet<>(flagData.getContexts());
        contexts.addAll(flagDefinition.getContexts());

        // check if admin definition has gd_claim contex
        boolean hasClaimContext = false;
        boolean hasOverrideClaimContext = false;
        boolean hasDefaultClaimContext = false;
        boolean replaceClaimContext = false;
        final Iterator<Context> iterator = contexts.iterator();
        Context claimContext = claim.getContext();
        while (iterator.hasNext()) {
            final Context context = iterator.next();
            if (!flagDefinition.isAdmin()) {
                hasClaimContext = true;
                if (context.getKey().contains("gd_claim")) {
                    iterator.remove();
                }
            } else {
                if (context.getKey().equalsIgnoreCase("gd_claim")) {
                    claimContext = context;
                    hasClaimContext = true;
                    if (context.getValue().equalsIgnoreCase("claim")) {
                        iterator.remove();
                        replaceClaimContext = true;
                    }
                } else if (context.getKey().equalsIgnoreCase("gd_claim_default")) {
                    hasDefaultClaimContext = true;
                } else if (context.getKey().equalsIgnoreCase("gd_claim_override")) {
                    hasOverrideClaimContext = true;
                }
            }
        }

        if (!flagDefinition.isAdmin() || hasClaimContext) {
            // First check if this permission has been overridden by admin
            // Check override
            contexts.remove(claimContext);
            contexts.add(ClaimContexts.GLOBAL_OVERRIDE_CONTEXT);
            contexts.add(claim.getWorldContext());
            contexts.add(claim.getOverrideTypeContext());
            contexts.add(claim.getOverrideClaimContext());
            Tristate result = PermissionUtil.getInstance().getPermissionValue(claim, GriefDefenderPlugin.DEFAULT_HOLDER, flagData.getFlag().getPermission(), contexts);
            if (result != Tristate.UNDEFINED) {
                return new GDActiveFlagData(flagDefinition, flagData, result, contexts, GDActiveFlagData.Type.OVERRIDE);
            }

            // Check claim
            contexts = new HashSet<>(flagData.getContexts());
            contexts.addAll(flagDefinition.getContexts());
            if (flagDefinition.isAdmin()) {
                if (replaceClaimContext) {
                    contexts.remove(claimContext);
                    contexts.add(claim.getContext());
                } else {
                    contexts.add(claimContext);
                }
            } else {
                contexts.add(claimContext);
            }

            result = PermissionUtil.getInstance().getPermissionValue(claim, GriefDefenderPlugin.DEFAULT_HOLDER, flagData.getFlag().getPermission(), contexts);
            if (result != Tristate.UNDEFINED) {
                return new GDActiveFlagData(flagDefinition, flagData, result, contexts, GDActiveFlagData.Type.CLAIM);
            }

        }

        if (!hasClaimContext && !hasOverrideClaimContext) {
            // Direct lookup

            contexts = new HashSet<>(flagData.getContexts());
            contexts.addAll(flagDefinition.getContexts());
            Tristate result = PermissionUtil.getInstance().getPermissionValue(claim, GriefDefenderPlugin.DEFAULT_HOLDER, flagData.getFlag().getPermission(), contexts);
            if (result != Tristate.UNDEFINED) {
                return new GDActiveFlagData(flagDefinition, flagData, result, contexts, GDActiveFlagData.Type.DEFAULT);
            }
        }

        if (!hasDefaultClaimContext) {
            contexts = new HashSet<>(flagData.getContexts());
            contexts.addAll(flagDefinition.getContexts());
            contexts.remove(claimContext);
            contexts.add(ClaimContexts.GLOBAL_DEFAULT_CONTEXT);
            contexts.add(claim.getWorldContext());
            contexts.add(claim.getDefaultTypeContext());
            Tristate result = PermissionUtil.getInstance().getPermissionValue(claim, GriefDefenderPlugin.DEFAULT_HOLDER, flagData.getFlag().getPermission(), contexts);
            if (result != Tristate.UNDEFINED) {
                return new GDActiveFlagData(flagDefinition, flagData, result, contexts, GDActiveFlagData.Type.DEFAULT);
            }
        }

        return new GDActiveFlagData(flagDefinition, flagData,Tristate.UNDEFINED, contexts, GDActiveFlagData.Type.UNDEFINED);
    }

    private Component getClickableText(GDPermissionUser src, GDClaim claim, Flag flag, FlagContextHolder flagHolder, Set<Context> contexts, MenuType displayType) {
        Component hoverEventText = TextComponent.empty();
        final MenuType flagType = flagHolder.getType();
        final Player player = src.getOnlinePlayer();
        boolean hasEditPermission = true;
        if (displayType == MenuType.DEFAULT) {
            if (!src.getInternalPlayerData().canManageFlagDefaults) {
                hoverEventText = MessageCache.getInstance().PERMISSION_FLAG_DEFAULTS;
                hasEditPermission = false;
            }
        } else if (flagType == MenuType.OVERRIDE) {
            if (!src.getInternalPlayerData().canManageFlagOverrides) {
                hoverEventText = MessageCache.getInstance().PERMISSION_FLAG_OVERRIDES;
                hasEditPermission = false;
            }
        } else if (flagType == MenuType.INHERIT) {
            UUID parentUniqueId = null;
            for (Context context : contexts) {
                if (context.getKey().equals("gd_claim")) {
                    try {
                        parentUniqueId = UUID.fromString(context.getValue());
                    } catch (IllegalArgumentException e) {
                        // ignore
                    }
                }
            }
            // should never happen but just in case
            if (parentUniqueId == null) {
                hoverEventText = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.FLAG_UI_INHERIT_PARENT,
                        ImmutableMap.of("name", "unknown"));
                hasEditPermission = false;
            } else {
                final GDClaim parentClaim = (GDClaim) GriefDefenderPlugin.getInstance().dataStore.getClaim(claim.getWorldUniqueId(), parentUniqueId);
                hoverEventText = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.FLAG_UI_INHERIT_PARENT,
                        ImmutableMap.of("name", parentClaim.getFriendlyNameType()));
                hasEditPermission = false;
            }
        } else {
            Component denyReason = claim.allowEdit(player);
            if (denyReason != null) {
                hoverEventText = denyReason;
                hasEditPermission = false;
            } else {
                // check flag perm
                if (!player.hasPermission(GDPermissions.USER_CLAIM_FLAGS + "." + flag.getName().toLowerCase())) {
                    hoverEventText = MessageCache.getInstance().PERMISSION_FLAG_USE;
                    hasEditPermission = false;
                }
            }
        }

        Set<Context> sortedContexts = new TreeSet<>(new Comparator<Context>() {
            @Override
            public int compare(Context o1, Context o2) {
                 return o1.getKey().compareTo(o2.getKey());
            }
        });
        sortedContexts.addAll(contexts);

        final boolean customContexts = UIHelper.containsCustomContext(contexts);
        Component flagContexts = UIHelper.getFriendlyContextString(claim, contexts);

        Component hoverText = TextComponent.builder()
                .append(hoverEventText)
                .append(hoverEventText == TextComponent.empty() ? "" : "\n")
                .append(UIHelper.getPermissionMenuTypeHoverText(flagHolder, displayType))
                .append("\n")
                .append(flagContexts)
                .build();

        Tristate newValue = Tristate.UNDEFINED;
        Boolean value = flagHolder.getValue();

        if (customContexts) {
            if (value) {
                newValue = Tristate.FALSE;
            } else {
                newValue = Tristate.TRUE;
            }
        } else {
            if (displayType == MenuType.DEFAULT || (displayType == MenuType.CLAIM && flagHolder.getType() == MenuType.DEFAULT)) {
                newValue = Tristate.fromBoolean(!value);
            } else {
                // Always fall back to transient default
                newValue = Tristate.UNDEFINED;
            }
        }

        TextComponent.Builder valueBuilder = TextComponent.builder()
                .append(String.valueOf(value), flagHolder.getColor())
                .hoverEvent(HoverEvent.showText(hoverText));
        TextComponent.Builder textBuilder = null;
        if (hasEditPermission) {
            textBuilder = TextComponent.builder()
            .append(valueBuilder
                    .hoverEvent(HoverEvent.showText(hoverText))
                    .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createFlagConsumer(src, claim, flag, flagHolder, newValue, contexts, displayType))))
                    .build());
        } else {
            textBuilder = TextComponent.builder()
                    .append(valueBuilder
                            .hoverEvent(HoverEvent.showText(hoverText))
                            .build());
        }

        // check source/target
        Component source = null;
        Component target = null;
        final Component whiteOpenBracket = TextComponent.of("[", TextColor.WHITE);
        final Component whiteCloseBracket = TextComponent.of("]", TextColor.WHITE);
        for (Context context : contexts) {
            if (context.getKey().equals(ContextKeys.SOURCE)) {
                source = TextComponent.builder()
                            .append(whiteOpenBracket)
                            .append("s", TextColor.GREEN)
                            .append("=", TextColor.WHITE)
                            .append(context.getValue().replace("minecraft:", ""), TextColor.GOLD)
                            .append(whiteCloseBracket)
                            .hoverEvent(HoverEvent.showText(MessageCache.getInstance().LABEL_SOURCE))
                            .build();
                textBuilder.append(" ").append(source);
            } else if (context.getKey().equals(ContextKeys.TARGET)) {
                target = TextComponent.builder()
                        .append(whiteOpenBracket)
                        .append("t", TextColor.GREEN)
                        .append("=", TextColor.WHITE)
                        .append(context.getValue().replace("minecraft:", ""), TextColor.GOLD)
                        .append(whiteCloseBracket)
                        .hoverEvent(HoverEvent.showText(MessageCache.getInstance().LABEL_TARGET))
                        .build();
                textBuilder.append(" ").append(target);
            }
        }

        if (customContexts) {
            textBuilder.append(" ")
                .append("[", TextColor.WHITE)
                .append(TextComponent.builder()
                        .append("x", TextColor.RED)
                        .hoverEvent(HoverEvent.showText(MessageCache.getInstance().FLAG_UI_CLICK_REMOVE))
                        .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createFlagConsumer(src, claim, flag, flagHolder, Tristate.UNDEFINED, contexts, displayType))))
                        .build())
                .append("]", TextColor.WHITE);
        }

        return textBuilder.build();
    }

    private Consumer<CommandSource> createCustomFlagConsumer(GDPermissionUser src, GDClaim claim, GDFlagDefinition customFlag, Tristate currentValue, String flagGroup) {
        final Player player = src.getOnlinePlayer();
        return consumer -> {
            GDCauseStackManager.getInstance().pushCause(player);
            Set<Context> definitionContexts = new HashSet<>(customFlag.getContexts());
            boolean addClaimContext = false;
            final Iterator<Context> iterator = definitionContexts.iterator();
            while (iterator.hasNext()) {
                final Context context = iterator.next();
                if (!customFlag.isAdmin()) {
                    if (context.getKey().contains("gd_claim")) {
                        iterator.remove();
                    }
                    addClaimContext = true;
                    break;
                } else if (context.getKey().equalsIgnoreCase("gd_claim") && context.getValue().equalsIgnoreCase("claim")) {
                    iterator.remove();
                    addClaimContext = true;
                    break;
                }
            }
            if (addClaimContext) {
                definitionContexts.add(claim.getContext());
            }
            for (FlagData flagData : customFlag.getFlagData()) {
                final Set<Context> newContexts = new HashSet<>(definitionContexts);
                newContexts.addAll(flagData.getContexts());
                Tristate newValue = Tristate.UNDEFINED;
                if (currentValue == Tristate.TRUE) {
                    newValue = Tristate.FALSE;
                } else {
                    newValue = Tristate.TRUE;
                }

                final Flag flag = flagData.getFlag();
                GDFlagPermissionEvent.Set event = new GDFlagPermissionEvent.Set(this.subject, flagData.getFlag(), newValue, newContexts);
                GriefDefender.getEventManager().post(event);
                if (event.cancelled()) {
                    return;
                }

                PermissionResult result = GriefDefenderPlugin.getInstance().getPermissionProvider().setPermissionValue(GriefDefenderPlugin.DEFAULT_HOLDER, flag, newValue, newContexts);
            }

            // Save after all permission changes have been made
            GriefDefenderPlugin.getInstance().getPermissionProvider().save(GriefDefenderPlugin.DEFAULT_HOLDER);
            GDCauseStackManager.getInstance().popCause();
            showCustomFlags(src, claim, flagGroup);
        };
    }

    private Consumer<CommandSource> createFlagConsumer(GDPermissionUser src, GDClaim claim, Flag flag, FlagContextHolder flagHolder, Tristate newValue, Set<Context> contexts, MenuType displayType) {
        final Player player = src.getOnlinePlayer();
        return consumer -> {
            GDCauseStackManager.getInstance().pushCause(player);
            Set<Context> newContexts = new HashSet<>(contexts);
            if (displayType == MenuType.CLAIM && newValue != Tristate.UNDEFINED) {
                final Iterator<Context> iterator = newContexts.iterator();
                while (iterator.hasNext()) {
                    final Context context = iterator.next();
                    if (context.getKey().equals("gd_claim_default")) {
                        iterator.remove();
                    }
                }
                newContexts.add(claim.getContext());
            }

            Context serverContext = null;
            final String serverName = PermissionUtil.getInstance().getServerName();
            if (serverName != null) {
                serverContext = new Context("server", serverName);
            }
            // Check server context
            final Iterator<Context> iterator = newContexts.iterator();
            boolean hasServerContext = false;
            boolean hasDefaultContext = false;
            while (iterator.hasNext()) {
                final Context context = iterator.next();
                if (context.getKey().equals("server")) {
                    hasServerContext = true;
                } else if (context.getKey().equalsIgnoreCase("gd_claim_default")) {
                    hasDefaultContext = true;
                }
            }

            if (!hasServerContext && serverContext != null) {
                newContexts.add(serverContext);
            }

            GDFlagPermissionEvent.Set event = new GDFlagPermissionEvent.Set(this.subject, flag, newValue, newContexts);
            GriefDefender.getEventManager().post(event);
            GDCauseStackManager.getInstance().popCause();
            if (event.cancelled()) {
                return;
            }

            if (displayType == MenuType.DEFAULT || (hasDefaultContext && src.getInternalPlayerData().canManageFlagDefaults)) {
                PermissionResult result = PermissionUtil.getInstance().setTransientPermission(this.subject, flag.getPermission(), newValue, newContexts);
                if (result.successful()) {
                    showFlagPermissions(src, claim, displayType);
                    return;
                }
            }
            PermissionResult result = PermissionUtil.getInstance().setPermissionValue(this.subject, flag, newValue, newContexts);
            if (!result.successful()) {
                // Try again without server context
                newContexts.remove(serverContext);
                result = PermissionUtil.getInstance().setPermissionValue(this.subject, flag, newValue, newContexts, false, true);
            }
            if (result.successful()) {
                showFlagPermissions(src, claim, displayType);
            }
        };
    }

    private Consumer<CommandSource> createCustomFlagConsumer(GDPermissionUser src, GDClaim claim, String flagGroup) {
        return consumer -> {
            PaginationUtil.getInstance().resetActivePage(src.getUniqueId());
            showCustomFlags(src, claim, flagGroup);
        };
    }

    private Consumer<CommandSource> createClaimFlagConsumer(GDPermissionUser src, GDClaim claim, MenuType flagType) {
        return consumer -> {
            PaginationUtil.getInstance().resetActivePage(src.getUniqueId());
            showFlagPermissions(src, claim, flagType);
        };
    }
}