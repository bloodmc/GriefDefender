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
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import com.griefdefender.GDBootstrap;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimContexts;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.ContextKeys;
import com.griefdefender.api.permission.PermissionResult;
import com.griefdefender.api.permission.option.Option;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.api.permission.option.type.CreateModeType;
import com.griefdefender.api.permission.option.type.CreateModeTypes;
import com.griefdefender.api.permission.option.type.GameModeType;
import com.griefdefender.api.permission.option.type.GameModeTypes;
import com.griefdefender.api.permission.option.type.WeatherType;
import com.griefdefender.api.permission.option.type.WeatherTypes;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.internal.pagination.PaginationList;
import com.griefdefender.listener.CommonEntityEventHandler;
import com.griefdefender.permission.GDPermissionHolder;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.permission.option.GDOption;
import com.griefdefender.permission.option.GDOptions;
import com.griefdefender.permission.ui.ClaimClickData;
import com.griefdefender.permission.ui.MenuType;
import com.griefdefender.permission.ui.OptionData;
import com.griefdefender.permission.ui.OptionData.OptionContextHolder;
import com.griefdefender.permission.ui.UIHelper;
import com.griefdefender.registry.OptionRegistryModule;
import com.griefdefender.text.action.GDCallbackHolder;
import com.griefdefender.util.CauseContextHelper;
import com.griefdefender.util.ChatCaptureUtil;
import com.griefdefender.util.PaginationUtil;
import com.griefdefender.util.PermissionUtil;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings({ "unchecked", "rawtypes" })
public abstract class ClaimOptionBase extends BaseCommand {

    protected GDPermissionHolder subject;
    protected ClaimSubjectType subjectType;
    protected String friendlySubjectName;
    private final Cache<UUID, MenuType> lastActiveMenuTypeMap = Caffeine.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES)
            .build();

    protected ClaimOptionBase(ClaimSubjectType type) {
        this.subjectType = type;
    }

    public void execute(Player player, String[] args) throws InvalidCommandArgument {
        final GDPermissionUser src = PermissionHolderCache.getInstance().getOrCreateUser(player);
        final GDPermissionHolder commandSubject = subject;
        String commandOption = null;
        String value = null;
        String contexts = null;
        final String arguments = String.join(" ", args);
        int index = arguments.indexOf("context[");
        if (index != -1) {
            contexts = arguments.substring(index, arguments.length());
        }
        if (args.length > 0) {
            if (args.length < 2) {
                throw new InvalidCommandArgument();
            }
            commandOption = args[0];
            // Check for quoted string
            Pattern pattern = Pattern.compile("\"(.*)\"");
            Matcher matcher = pattern.matcher(arguments);
            if (matcher.find()) {
                value = matcher.group(1);
            } else {
                value = args[1];
            }
        }

        Option<?> option = null;
        if (commandOption != null) {
            option = GriefDefender.getRegistry().getType(Option.class, commandOption).orElse(null);
            if (option == null) {
                TextAdapter.sendComponent(player, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.OPTION_NOT_FOUND, ImmutableMap.of(
                        "option", commandOption)));
                return;
            }
            if (option != null && !GDOptions.isOptionEnabled(option)) {
                TextAdapter.sendComponent(player, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.OPTION_NOT_ENABLED, ImmutableMap.of(
                        "option", commandOption)));
                return;
            }
            if (option == Options.PLAYER_COMMAND_ENTER || option == Options.PLAYER_COMMAND_EXIT) {
                if (contexts == null) {
                    TextAdapter.sendComponent(player, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.OPTION_REQUIRES_CONTEXTS, ImmutableMap.of(
                            "contexts", "run-as=[player|console] run-for=[public|owner|member]")));
                    return;
                }
            }
        }

        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        final GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(playerData, player.getLocation());
        if (claim.isWilderness()) {
            if(!playerData.canManageWilderness && !playerData.canIgnoreClaim(claim)) {
                GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().PERMISSION_GLOBAL_OPTION);
                return;
            }
        } else if (!claim.isTown() && !playerData.canManageAdminClaims && !playerData.canIgnoreClaim(claim)) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().PERMISSION_PLAYER_OPTION);
            return;
        }
        if (option != null) {
            if (option.isGlobal()) {
                if (!player.hasPermission(GDPermissions.MANAGE_GLOBAL_OPTIONS +"." + option.getPermission().toLowerCase())) {
                    GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().PERMISSION_GLOBAL_OPTION);
                    return;
                }
            } else if (!player.hasPermission(GDPermissions.USER_CLAIM_OPTIONS +"." + option.getPermission().toLowerCase())) {
                GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().PERMISSION_PLAYER_OPTION);
                return;
            }
        }

        if (!playerData.canManageAdminClaims && !playerData.canIgnoreClaim(claim)) {
            final Component denyMessage = claim.allowEdit(player);
            if (denyMessage != null) {
                GriefDefenderPlugin.sendMessage(player, denyMessage);
                return;
            }
        }

        String optionPermission = option != null ? option.getPermission() : "";
        final Set<Context> contextSet = CauseContextHelper.generateContexts(optionPermission, player, claim, contexts);
        if (contextSet == null) {
            return;
        }
        if (option == Options.PLAYER_COMMAND_ENTER || option == Options.PLAYER_COMMAND_EXIT) {
            final Set<String> requiredKeys = (Set<String>) option.getRequiredContextKeys();
            for (String key : requiredKeys) {
                boolean found = false;
                for (Context context : contextSet) {
                    if (context.getKey().equalsIgnoreCase(key)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    TextAdapter.sendComponent(player, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.OPTION_REQUIRES_CONTEXTS, ImmutableMap.of(
                            "contexts", key)));
                    return;
                }
            }
            if (contexts == null) {
                TextAdapter.sendComponent(player, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.OPTION_REQUIRES_CONTEXTS, ImmutableMap.of(
                        "contexts", "run-as=[player|console] run-for=[public|owner|member]")));
                return;
            }
        }

        if (claim != null) {
            if (commandOption == null && value == null && player.hasPermission(GDPermissions.COMMAND_LIST_CLAIM_OPTIONS)) {
                showOptionPermissions(src, (GDClaim) claim, MenuType.CLAIM);
                return;
            } else if (option != null && value != null) {
                if (!value.equalsIgnoreCase("undefined") && !((GDOption) option).validateStringValue(value, false)) {
                    GriefDefenderPlugin.sendMessage(player, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.OPTION_INVALID_VALUE, 
                            ImmutableMap.of(
                                    "value", value,
                                    "option", option.getName(),
                                    "type", option.getAllowedType().getSimpleName())));
                    return;
                }

                MenuType type = MenuType.DEFAULT;
                boolean useClaimContext = true;
                for (Context context : contextSet) {
                    if (context.getKey().equals(ContextKeys.CLAIM_DEFAULT)) {
                        useClaimContext = false;
                        break;
                    }
                    if (context.getKey().equals(ContextKeys.CLAIM)) {
                        type = MenuType.CLAIM;
                        break;
                    }
                    if (context.getKey().equals(ContextKeys.CLAIM_OVERRIDE)) {
                        type = MenuType.OVERRIDE;
                        useClaimContext = false;
                        break;
                    }
                }
                if (!option.isGlobal() && useClaimContext) {
                    contextSet.add(claim.getContext());
                    if (contextSet.isEmpty() ) {
                        type = MenuType.CLAIM;
                    }
                }
                GDCauseStackManager.getInstance().pushCause(player);
                PermissionUtil.getInstance().setOptionValue(this.subject, option.getPermission(), value, contextSet);
                TextAdapter.sendComponent(player, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.OPTION_SET_TARGET,
                                ImmutableMap.of(
                                        "type", type.name().toUpperCase(),
                                        "option", option.getName(),
                                        "contexts", UIHelper.getFriendlyContextString(claim, contextSet),
                                        "value", TextComponent.of(value).color(TextColor.LIGHT_PURPLE),
                                        "target", subject.getFriendlyName())));
                GDCauseStackManager.getInstance().popCause();
                return;
            }
        }
    }

    protected void showOptionPermissions(GDPermissionUser src, GDClaim claim, MenuType displayType) {
        boolean isAdmin = false;
        final Player player = src.getOnlinePlayer();
        final GDPlayerData playerData = src.getInternalPlayerData();
        final boolean isTaxEnabled = GriefDefenderPlugin.getGlobalConfig().getConfig().economy.taxSystem;
        if (player.hasPermission(GDPermissions.DELETE_CLAIM_ADMIN)) {
            isAdmin = true;
        }

        final MenuType lastFlagType = this.lastActiveMenuTypeMap.getIfPresent(player.getUniqueId());
        if (lastFlagType != null && lastFlagType != displayType) {
            PaginationUtil.getInstance().resetActivePage(player.getUniqueId());
        }
        final Component whiteOpenBracket = TextComponent.of("[", TextColor.AQUA);
        final Component whiteCloseBracket = TextComponent.of("]", TextColor.AQUA);
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
                    .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createClaimOptionConsumer(src, claim, MenuType.DEFAULT))))
                    .hoverEvent(HoverEvent.showText(showDefaultText)).build();
        }
        final Component overrideFlagText = TextComponent.builder("")
                .append(displayType == MenuType.OVERRIDE ? TextComponent.builder("")
                        .append(whiteOpenBracket)
                        .append("OVERRIDE", TextColor.RED)
                        .append(whiteCloseBracket).build() : TextComponent.of("OVERRIDE", TextColor.GRAY))
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createClaimOptionConsumer(src, claim, MenuType.OVERRIDE))))
                .hoverEvent(HoverEvent.showText(showOverrideText)).build();
        final Component claimFlagText = TextComponent.builder("")
                .append(displayType == MenuType.CLAIM ? TextComponent.builder("")
                        .append(whiteOpenBracket)
                        .append("CLAIM", TextColor.YELLOW)
                        .append(whiteCloseBracket).build() : TextComponent.of("CLAIM", TextColor.GRAY))
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createClaimOptionConsumer(src, claim, MenuType.CLAIM))))
                .hoverEvent(HoverEvent.showText(showClaimText)).build();
        final Component inheritFlagText = TextComponent.builder("")
                .append(displayType == MenuType.INHERIT ? TextComponent.builder("")
                        .append(whiteOpenBracket)
                        .append("INHERIT", TextColor.AQUA)
                        .append(whiteCloseBracket).build() : TextComponent.of("INHERIT", TextColor.GRAY))
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createClaimOptionConsumer(src, claim, MenuType.INHERIT))))
                .hoverEvent(HoverEvent.showText(showInheritText)).build();
        Component claimOptionHead = TextComponent.empty();
        if (this.subjectType == ClaimSubjectType.GLOBAL) {
            if (isAdmin) {
                claimOptionHead = TextComponent.builder("")
                        .append(" Displaying : ", TextColor.AQUA)
                        .append(defaultFlagText)
                        .append("  ")
                        .append(claimFlagText)
                        .append("  ")
                        .append(inheritFlagText)
                        .append("  ")
                        .append(overrideFlagText).build();
            } else {
                claimOptionHead = TextComponent.builder("")
                        .append(" Displaying : ", TextColor.AQUA)
                        .append(claimFlagText)
                        .append("  ")
                        .append(inheritFlagText)
                        .append("  ")
                        .append(overrideFlagText).build();
            }
        } else {
            claimOptionHead = TextComponent.builder("")
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
        if (!claim.isWilderness()) {
            defaultContexts.add(ClaimContexts.USER_DEFAULT_CONTEXT);
            overrideContexts.add(ClaimContexts.USER_OVERRIDE_CONTEXT);
        }
        defaultContexts.add(ClaimContexts.GLOBAL_DEFAULT_CONTEXT);
        overrideContexts.add(ClaimContexts.GLOBAL_OVERRIDE_CONTEXT);
        overrideContexts.add(claim.getOverrideClaimContext());

        Map<String, OptionData> filteredContextMap = new HashMap<>();
        for (Map.Entry<Set<Context>, Map<String, List<String>>> mapEntry : PermissionUtil.getInstance().getTransientOptions(GriefDefenderPlugin.GD_OPTION_HOLDER).entrySet()) {
            final Set<Context> contextSet = mapEntry.getKey();
            if (contextSet.contains(claim.getDefaultTypeContext()) || (contextSet.contains(ClaimContexts.GLOBAL_DEFAULT_CONTEXT) || (!claim.isWilderness() && contextSet.contains(ClaimContexts.USER_DEFAULT_CONTEXT)))) {
                this.addFilteredContexts(src, filteredContextMap, contextSet, MenuType.DEFAULT, mapEntry.getValue());
            }
        }

        if (displayType == MenuType.DEFAULT || displayType == MenuType.CLAIM) {
            final Set<Context> contexts = new HashSet<>();
            contexts.add(ClaimContexts.GLOBAL_DEFAULT_CONTEXT);
            if (!claim.isWilderness()) {
                contexts.add(ClaimContexts.USER_DEFAULT_CONTEXT);
            }
            for (Option option : OptionRegistryModule.getInstance().getAll()) {
                if (option.isGlobal() && displayType == MenuType.CLAIM) {
                    continue;
                }
                if (!GDOptions.isOptionEnabled(option)) {
                    continue;
                }
                // commands are special-cased as they use a List and cannot show up with no data
                if (option == Options.PLAYER_COMMAND_ENTER || option == Options.PLAYER_COMMAND_EXIT) {
                    continue;
                }
                boolean found = false;
                for (Entry<String, OptionData> optionEntry : filteredContextMap.entrySet()) {
                    if (optionEntry.getValue().option == option) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    filteredContextMap.put(option.getPermission(), new OptionData(option, option.getDefaultValue().toString(), MenuType.DEFAULT, contexts));
                }
            }
        }

        for (Map.Entry<Set<Context>, Map<String, List<String>>> mapEntry : PermissionUtil.getInstance().getPermanentOptions(this.subject).entrySet()) {
            final Set<Context> contextSet = mapEntry.getKey();
            if (contextSet.contains(ClaimContexts.GLOBAL_DEFAULT_CONTEXT) || (!claim.isWilderness() && contextSet.contains(ClaimContexts.USER_DEFAULT_CONTEXT))) {
                this.addFilteredContexts(src, filteredContextMap, contextSet, MenuType.DEFAULT, mapEntry.getValue());
            }
            if (contextSet.contains(claim.getDefaultTypeContext())) {
                this.addFilteredContexts(src, filteredContextMap, contextSet, MenuType.DEFAULT, mapEntry.getValue());
            }
            if (displayType != MenuType.DEFAULT) {
                //if (claim.isTown() || isAdmin) {
                    if (contextSet.contains(claim.getContext())) {
                        this.addFilteredContexts(src, filteredContextMap, contextSet, MenuType.CLAIM, mapEntry.getValue());
                    }
                //}
                if (contextSet.contains(ClaimContexts.GLOBAL_OVERRIDE_CONTEXT) || (!claim.isWilderness() && contextSet.contains(ClaimContexts.USER_OVERRIDE_CONTEXT))) {
                    this.addFilteredContexts(src, filteredContextMap, contextSet, MenuType.OVERRIDE, mapEntry.getValue());
                }
                if (contextSet.contains(claim.getOverrideClaimContext())) {
                    this.addFilteredContexts(src, filteredContextMap, contextSet, MenuType.OVERRIDE, mapEntry.getValue());
                } else if (contextSet.contains(claim.getOverrideTypeContext())) {
                    this.addFilteredContexts(src, filteredContextMap, contextSet, MenuType.OVERRIDE, mapEntry.getValue());
                }
            }
        }

        Map<Set<Context>, ClaimClickData> inheritPermissionMap = Maps.newHashMap();

        final List<Claim> inheritParents = claim.getInheritedParents();
        Collections.reverse(inheritParents);
        for (Claim current : inheritParents) {
            GDClaim currentClaim = (GDClaim) current;
            for (Map.Entry<Set<Context>, Map<String, List<String>>> mapEntry : PermissionUtil.getInstance().getPermanentOptions(this.subject).entrySet()) {
                final Set<Context> contextSet = mapEntry.getKey();
                if (contextSet.contains(currentClaim.getContext())) {
                    inheritPermissionMap.put(mapEntry.getKey(), new ClaimClickData(currentClaim, mapEntry.getValue()));
                }
            }
        }

        final Map<String, Map<Integer, Component>> textMap = new TreeMap<>();
        for (Entry<String, OptionData> mapEntry : filteredContextMap.entrySet()) {
            final OptionData optionData = mapEntry.getValue();
            final Option option = optionData.option;
            if (option.getName().contains("tax") && !GriefDefenderPlugin.getGlobalConfig().getConfig().economy.taxSystem) {
                continue;
            }
            if (option.isGlobal() && displayType == MenuType.CLAIM) {
                continue;
            }

            for (OptionContextHolder optionHolder : optionData.optionContextMap.values()) {
                if (displayType != MenuType.CLAIM && optionHolder.getType() != displayType) {
                    continue;
                }

                final Set<Context> contexts = optionHolder.getAllContexts();
                Component optionText = getClickableOptionComponent(src, claim, option, optionHolder, contexts, displayType);
                final int hashCode = Objects.hash(option.getPermission(), contexts);
                Map<Integer, Component> componentMap = textMap.get(option.getPermission());
                if (componentMap == null) {
                    componentMap = new HashMap<>();
                    componentMap.put(hashCode, optionText);
                    textMap.put(option.getPermission(), componentMap);
                } else {
                    componentMap.put(hashCode, optionText);
                }
            }
        }

        List<Component> textList = new ArrayList<>();
        for (Entry<String, Map<Integer, Component>> mapEntry : textMap.entrySet()) {
            textList.addAll(mapEntry.getValue().values());
        }

        Collections.sort(textList, UIHelper.PLAIN_COMPARATOR);
        int fillSize = 20 - (textList.size() + 2);
        Component footer = null;
        if (player != null && player.hasPermission(GDPermissions.CHAT_CAPTURE)) {
            footer = ChatCaptureUtil.getInstance().createRecordChatComponent(player, claim, playerData, "claimoption");
            fillSize = 20 - (textList.size() + 3);
        }

        for (int i = 0; i < fillSize; i++) {
            textList.add(TextComponent.of(" "));
        }
        PaginationList.Builder paginationBuilder = PaginationList.builder()
                .title(claimOptionHead).padding(TextComponent.builder(" ").decoration(TextDecoration.STRIKETHROUGH, true).build()).contents(textList).footer(footer);
        final PaginationList paginationList = paginationBuilder.build();
        Integer activePage = 1;
        activePage = PaginationUtil.getInstance().getActivePage(player.getUniqueId());
        if (activePage == null) {
            activePage = 1;
        }
        this.lastActiveMenuTypeMap.put(player.getUniqueId(), displayType);
        paginationList.sendTo(player, activePage);
    }

    private void addFilteredContexts(GDPermissionUser src, Map<String, OptionData> filteredContextMap, Set<Context> contexts, MenuType type, Map<String, List<String>> permissions) {
        final Player player = src.getOnlinePlayer();
        final GDPlayerData playerData = src.getInternalPlayerData();
        for (Map.Entry<String, List<String>> permissionEntry : permissions.entrySet()) {
            final Option option = OptionRegistryModule.getInstance().getById(permissionEntry.getKey()).orElse(null);
            if (option == null) {
                continue;
            }
            if (option.getName().contains("tax") && !GriefDefenderPlugin.getGlobalConfig().getConfig().economy.taxSystem) {
                continue;
            }

            if (option.isGlobal()) {
                if (!player.hasPermission(GDPermissions.MANAGE_GLOBAL_OPTIONS +"." + option.getName().toLowerCase())) {
                    continue;
                }
            } else if (((GDOption) option).isAdmin()) {
                if (!player.hasPermission(GDPermissions.MANAGE_ADMIN_OPTIONS +"." + option.getName().toLowerCase())) {
                    continue;
                }
            } else {
                if (!player.hasPermission(GDPermissions.USER_CLAIM_OPTIONS +"." + option.getName().toLowerCase())) {
                    continue;
                }
            }
            final OptionData optionData = filteredContextMap.get(permissionEntry.getKey());
            String optionValue = permissionEntry.getValue().get(0);
            if (option.multiValued()) {
                optionValue = "";
                for (String entry : permissionEntry.getValue()) {
                    if (optionValue.isEmpty()) {
                        optionValue += entry;
                    } else {
                        optionValue = optionValue + "\\|" + entry;
                    }
                }
            }
            if (optionData != null) {
                optionData.addContexts(option, optionValue, type, contexts);
            } else {
                filteredContextMap.put(permissionEntry.getKey(), new OptionData(option, optionValue, type, contexts));
            }
        }
    }

    private Component getClickableOptionComponent(GDPermissionUser src, GDClaim claim, Option option, OptionContextHolder optionHolder, Set<Context> contexts, MenuType displayType) {
        final Player player = src.getOnlinePlayer();
        final GDPlayerData playerData = src.getInternalPlayerData();
        boolean hasEditPermission = true;
        Component hoverEventText = TextComponent.empty();
        final MenuType flagType = optionHolder.getType();
        if (flagType == MenuType.DEFAULT) {
            if (!playerData.canManageGlobalOptions) {
                hoverEventText = MessageCache.getInstance().PERMISSION_OPTION_DEFAULTS;
                hasEditPermission = false;
            }
        } else if (flagType == MenuType.OVERRIDE) {
            if (!playerData.canManageOverrideOptions) {
                hoverEventText = MessageCache.getInstance().PERMISSION_OPTION_OVERRIDES;
                hasEditPermission = false;
            }
        } else if (flagType == MenuType.INHERIT) {
            hoverEventText = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.OPTION_UI_INHERIT_PARENT,
                    ImmutableMap.of("name", claim.getFriendlyNameType()));
            hasEditPermission = false;
        }
        if (displayType == MenuType.CLAIM) {
            Component denyReason = claim.allowEdit(player);
            if (denyReason != null) {
                hoverEventText = denyReason;
                hasEditPermission = false;
            } else {
                if (option.isGlobal()) {
                    if (!player.hasPermission(GDPermissions.MANAGE_GLOBAL_OPTIONS +"." + option.getName().toLowerCase())) {
                        hoverEventText = MessageCache.getInstance().PERMISSION_OPTION_USE;
                        hasEditPermission = false;
                    }
                } else if (((GDOption) option).isAdmin()) {
                    if (!player.hasPermission(GDPermissions.MANAGE_ADMIN_OPTIONS +"." + option.getName().toLowerCase())) {
                        hoverEventText = MessageCache.getInstance().PERMISSION_OPTION_USE;
                        hasEditPermission = false;
                    }
                } else {
                    if (!player.hasPermission(GDPermissions.USER_CLAIM_OPTIONS +"." + option.getName().toLowerCase())) {
                        hoverEventText = MessageCache.getInstance().PERMISSION_OPTION_USE;
                        hasEditPermission = false;
                    }
                }
            }
        }

        boolean customContexts = this.containsCustomContext(option, contexts);
        Component optionContexts = UIHelper.getFriendlyContextString(claim, contexts);
        String currentValue = optionHolder.getValue();
        TextColor color = optionHolder.getColor();
        boolean isNumber = false;
        if (option.getAllowedType().isAssignableFrom(Integer.class) || option.getAllowedType().isAssignableFrom(Double.class)) {
            isNumber = true;
        }

        TextComponent.Builder builder = null;
        if (isNumber && hasEditPermission) {
            builder = TextComponent.builder()
                    .append(getOptionText(option, contexts))
                    .append(" ")
                    .append(TextComponent.builder()
                            .append(TextComponent.of("< ").decoration(TextDecoration.BOLD, true))
                            .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(newOptionValueConsumer(src, claim, option, optionHolder, contexts, displayType, true)))))
                    .append(currentValue.toLowerCase(), color);
        } else {
            if (hoverEventText == TextComponent.empty() && hasEditPermission) {
                hoverEventText = MessageCache.getInstance().CLAIMINFO_UI_CLICK_TOGGLE;
            }
            final TextComponent valueNoHover = 
                    TextComponent.builder()
                        .append(currentValue.toLowerCase(), color).build();
            final TextComponent valueHover = 
                    TextComponent.builder()
                        .append(currentValue.toLowerCase(), color)
                        .hoverEvent(HoverEvent.showText(
                                hoverEventText
                                    .append(this.getHoverContextComponent(contexts))))
                        .build();
            builder = TextComponent.builder()
                    .append(getOptionText(option, contexts))
                    .append(" ")
                    .append(hoverEventText != TextComponent.empty() ? valueHover : valueNoHover);
        }
        if (hasEditPermission) {
            if (!option.getAllowedType().isAssignableFrom(Integer.class) && !option.getAllowedType().isAssignableFrom(Double.class)) {
                this.appendContexts(builder, contexts);
                builder.clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(newOptionValueConsumer(src, claim, option, optionHolder, contexts, displayType, false))));
            } else {
                builder.append(TextComponent.builder().append(TextComponent.of(" >").decoration(TextDecoration.BOLD, true)));
                this.appendContexts(builder, contexts);
                builder.clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(newOptionValueConsumer(src, claim, option, optionHolder, contexts, displayType, false))));
            }

            if (option.getAllowedType().isAssignableFrom(String.class)) {
                builder.clickEvent(createClickEvent(player, option));
            }
        }

        if (displayType == MenuType.DEFAULT) {
            builder.hoverEvent(HoverEvent.showText(TextComponent.builder().append(MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.OPTION_NOT_SET, 
                    ImmutableMap.of(
                            "option", TextComponent.of(option.getName().toLowerCase()).color(TextColor.GREEN),
                            "value", TextComponent.of(currentValue).color(TextColor.GOLD)))).build()));
        }

        if (customContexts) {
            builder.append(" ")
                .append("[", TextColor.WHITE)
                .append(TextComponent.builder()
                        .append("x", TextColor.RED)
                        .hoverEvent(HoverEvent.showText(MessageCache.getInstance().FLAG_UI_CLICK_REMOVE))
                        .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(removeOptionValueConsumer(src, claim, option, optionHolder, contexts, displayType))))
                        .build())
                .append("]", TextColor.WHITE);
        }

        return builder.build();
    }

    private Component getHoverContextComponent(Set<Context> contexts) {
        if (contexts.isEmpty()) {
            return TextComponent.empty();
        }

        TextComponent.Builder builder = TextComponent.builder()
                    .append("\n\n").append(MessageCache.getInstance().LABEL_CONTEXT).append(": \n");

        for (Context context : contexts) {
            final String key = context.getKey();
            final String value = context.getValue();
            TextColor keyColor = TextColor.AQUA;
            builder.append(key, keyColor)
                    .append("=", TextColor.WHITE)
                    .append(value.replace("minecraft:", ""), TextColor.GRAY)
                    .append("\n");
        }

        return builder.build();
    }

    private void appendContexts(TextComponent.Builder builder, Set<Context> contexts) {
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
                builder.append(" ").append(source);
            } else if (context.getKey().equals(ContextKeys.TARGET)) {
                target = TextComponent.builder()
                        .append(whiteOpenBracket)
                        .append("t", TextColor.GREEN)
                        .append("=", TextColor.WHITE)
                        .append(context.getValue().replace("minecraft:", ""), TextColor.GOLD)
                        .append(whiteCloseBracket)
                        .hoverEvent(HoverEvent.showText(MessageCache.getInstance().LABEL_TARGET))
                        .build();
                builder.append(" ").append(target);
            }
        }
    }

    private ClickEvent createClickEvent(Player src, Option option) {
        return ClickEvent.suggestCommand("/gd option " + option.getName() + " ");
    }
 
    private Consumer<CommandSender> newOptionValueConsumer(GDPermissionUser src, GDClaim claim, Option option, OptionContextHolder optionHolder, Set<Context> contexts, MenuType displayType, boolean leftArrow) {
        final String currentValue = optionHolder.getValue();
        return consumer -> {
            String newValue = "";
            if (option.getAllowedType().isAssignableFrom(Tristate.class)) {
                Tristate value = getMenuTypeValue(TypeToken.of(Tristate.class), currentValue);
                if (value == Tristate.TRUE) {
                    newValue = "false";
                } else if (value == Tristate.FALSE && optionHolder.getType() != MenuType.DEFAULT) {
                    newValue = "undefined";
                } else {
                    newValue = "true";
                }
                if (displayType == MenuType.CLAIM && optionHolder.getType() == MenuType.DEFAULT && newValue.equalsIgnoreCase(currentValue)) {
                    newValue = "undefined";
                }
            }
            if (option.getAllowedType().isAssignableFrom(Boolean.class)) {
                Boolean value = getMenuTypeValue(TypeToken.of(Boolean.class), currentValue);
                Tristate result = Tristate.UNDEFINED;
                if (displayType == MenuType.DEFAULT || (displayType == MenuType.CLAIM && optionHolder.getType() == MenuType.DEFAULT)) {
                    result = Tristate.fromBoolean(!value);
                } else {
                    // Always fall back to transient default
                    result = Tristate.UNDEFINED;
                }
                newValue = result.toString().toLowerCase();
            }
            if (option.getAllowedType().isAssignableFrom(GameModeType.class)) {
                GameModeType value = getMenuTypeValue(TypeToken.of(GameModeType.class), currentValue);
                if (value == null || value == GameModeTypes.UNDEFINED) {
                    newValue = "adventure";
                } else if (value == GameModeTypes.ADVENTURE) {
                    newValue = "creative";
                } else if (value == GameModeTypes.CREATIVE) {
                    newValue = "survival";
                } else if (value == GameModeTypes.SURVIVAL) {
                    newValue = "spectator";
                } else {
                    newValue = "undefined";
                }
            }
            if (option.getAllowedType().isAssignableFrom(CreateModeType.class)) {
                CreateModeType value = getMenuTypeValue(TypeToken.of(CreateModeType.class), currentValue);
                if (value == null || value == CreateModeTypes.UNDEFINED) {
                    newValue = "area";
                } else if (value == CreateModeTypes.AREA) {
                    newValue = "volume";
                } else {
                    newValue = "undefined";
                }
            }
            if (option.getAllowedType().isAssignableFrom(WeatherType.class)) {
                WeatherType value = getMenuTypeValue(TypeToken.of(WeatherType.class), currentValue);
                if (value == null || value == WeatherTypes.UNDEFINED) {
                    newValue = "clear";
                } else if (value == WeatherTypes.CLEAR) {
                    newValue = "downfall";
                } else {
                    newValue = "undefined";
                }
            }
            if (option.getAllowedType().isAssignableFrom(Integer.class)) {
                Integer value = getMenuTypeValue(TypeToken.of(Integer.class), currentValue);
                if (leftArrow) {
                    if (value == null || value < 1) {
                        TextAdapter.sendComponent(src.getOnlinePlayer(), MessageCache.getInstance().OPTION_UI_NOT_DEFINED.color(TextColor.RED));
                    } else {
                        if ((option == Options.MIN_LEVEL || option == Options.MAX_LEVEL || option == Options.MIN_SIZE_Y || option == Options.MAX_SIZE_Y) && value == 1) {
                            value = null;
                        } else {
                            value -= 1;
                            if (value <= 0) {
                                if (option == Options.MAX_LEVEL) {
                                    value = 255;
                                }
                            }
                        }
                    }
                } else {
                    if (value == null) {
                        value = 1;
                    } else {
                        if ((option == Options.MIN_SIZE_Y || option == Options.MAX_SIZE_Y) && value == 256) {
                            value = null;
                        } else if ((option == Options.MIN_LEVEL || option == Options.MAX_LEVEL) && value == 255) {
                            value = null;
                        } else {
                            value += 1;
                        }
                    }
                }
                newValue = value == null ? "undefined" :String.valueOf(value);
            }
            if (option.getAllowedType().isAssignableFrom(Double.class)) {
                Double value = getMenuTypeValue(TypeToken.of(Double.class), currentValue);
                if (leftArrow) {
                    if (value == null || value < 0) {
                        TextAdapter.sendComponent(src.getOnlinePlayer(), MessageCache.getInstance().OPTION_UI_NOT_DEFINED.color(TextColor.RED));
                    } else {
                        value -= 0.1;
                        if (option == Options.ABANDON_RETURN_RATIO && value <= 0) {
                            value = null;
                        } else {
                            if (value < 0) {
                                value = 0.0;
                            }
                        }
                    }
                } else {
                    if (value == null) {
                        value = 1.0;
                    } else {
                        value += 0.1;
                    }
                }
                newValue = value == null ? "undefined" : String.format("%.1f", value);
            }

            Set<Context> newContexts = new HashSet<>(contexts);
            if (displayType == MenuType.CLAIM) {
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
            while (iterator.hasNext()) {
                final Context context = iterator.next();
                if (context.getKey().equals("server")) {
                    hasServerContext = true;
                    break;
                }
            }

            if (!hasServerContext && serverContext != null) {
                newContexts.add(serverContext);
            }
            final Context permServerContext = serverContext;
            final String permValue = newValue;
            final CompletableFuture<PermissionResult> future = PermissionUtil.getInstance().setOptionValue(this.subject, option.getPermission(), newValue, newContexts);
            future.thenAcceptAsync(r -> {
                if (!r.successful()) {
                    // Try again without server context
                    newContexts.remove(permServerContext);
                    CompletableFuture<PermissionResult> newFuture = PermissionUtil.getInstance().setOptionValue(this.subject, option.getPermission(), permValue, newContexts, false);
                    newFuture.thenAccept(r2 -> {
                        if (r2.successful()) {
                            Bukkit.getScheduler().runTask(GDBootstrap.getInstance(), () -> {
                                if (option == Options.PLAYER_WEATHER) {
                                    CommonEntityEventHandler.getInstance().checkPlayerWeather(src, claim, claim, true);
                                }
                                showOptionPermissions(src, claim, displayType);
                            });
                        }
                    });
                } else {
                    Bukkit.getScheduler().runTask(GDBootstrap.getInstance(), () -> {
                        if (option == Options.PLAYER_WEATHER) {
                            CommonEntityEventHandler.getInstance().checkPlayerWeather(src, claim, claim, true);
                        }
                        showOptionPermissions(src, claim, displayType);
                    });
                }
            });
        };
    }

    private Consumer<CommandSender> removeOptionValueConsumer(GDPermissionUser src, GDClaim claim, Option option, OptionContextHolder optionHolder, Set<Context> contexts, MenuType displayType) {
        return consumer -> {
            final CompletableFuture<PermissionResult> future = PermissionUtil.getInstance().setOptionValue(this.subject, option.getPermission(), "undefined", contexts);
            future.thenAccept(r -> {
                if (r.successful()) {
                    Bukkit.getScheduler().runTask(GDBootstrap.getInstance(), () -> {
                        showOptionPermissions(src, claim, displayType);
                    });
                }
            });
        };
    }

    private Component getOptionText(Option option, Set<Context> contexts) {
        boolean customContext = this.containsCustomContext(option, contexts);

        final Component optionText = TextComponent.builder().color(customContext ? TextColor.YELLOW : TextColor.GREEN).append(option.getName() + " ")
                .hoverEvent(HoverEvent.showText(TextComponent.builder()
                        .append(option.getDescription())
                        .build())).build();
        return optionText;
    }

    private static <T> T getMenuTypeValue(TypeToken<T> type, String value) {
        if (type.getRawType().isAssignableFrom(Double.class)) {
            Double newValue = null;
            try {
                newValue = Double.valueOf(value);
            } catch (NumberFormatException e) {
                return null;
            }
            return (T) newValue;
        }
        if (type.getRawType().isAssignableFrom(Integer.class)) {
            Integer newValue = null;
            try {
                newValue = Integer.valueOf(value);
            } catch (NumberFormatException e) {
                return null;
            }
            return (T) newValue;
        }
        if (type.getRawType().isAssignableFrom(String.class)) {
            return (T) value;
        }
        if (type.getRawType().isAssignableFrom(Boolean.class)) {
            if (value.equalsIgnoreCase("false")) {
                return (T) Boolean.valueOf(value);
            } else if (value.equalsIgnoreCase("true")) {
                return (T) Boolean.valueOf(value);
            }
        }
        if (type.getRawType().isAssignableFrom(Tristate.class)) {
            if (value.equalsIgnoreCase("undefined")) {
                return (T) Tristate.UNDEFINED;
            }
            if (value.equalsIgnoreCase("true")) {
                return (T) Tristate.TRUE;
            }
            if (value.equalsIgnoreCase("false")) {
                return (T) Tristate.FALSE;
            }
            int permValue = 0;
            try {
                permValue = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return (T) Tristate.UNDEFINED;
            }
            if (permValue == 0) {
                return (T) Tristate.UNDEFINED;
            }
            return (T) (permValue == 1 ? Tristate.TRUE : Tristate.FALSE);
        }
        if (type.getRawType().isAssignableFrom(GameModeType.class)) {
            if (value.equalsIgnoreCase("undefined")) {
                return (T) GameModeTypes.UNDEFINED;
            }
            if (value.equalsIgnoreCase("adventure")) {
                return (T) GameModeTypes.ADVENTURE;
            }
            if (value.equalsIgnoreCase("creative")) {
                return (T) GameModeTypes.CREATIVE;
            }
            if (value.equalsIgnoreCase("survival")) {
                return (T) GameModeTypes.SURVIVAL;
            }
            if (value.equalsIgnoreCase("spectator")) {
                return (T) GameModeTypes.SPECTATOR;
            }
        }
        if (type.getRawType().isAssignableFrom(WeatherType.class)) {
            if (value.equalsIgnoreCase("undefined")) {
                return (T) WeatherTypes.UNDEFINED;
            }
            if (value.equalsIgnoreCase("clear")) {
                return (T) WeatherTypes.CLEAR;
            }
            if (value.equalsIgnoreCase("downfall")) {
                return (T) WeatherTypes.DOWNFALL;
            }
        }
        if (type.getRawType().isAssignableFrom(CreateModeType.class)) {
            if (value.equalsIgnoreCase("undefined")) {
                return (T) CreateModeTypes.UNDEFINED;
            }
            if (value.equalsIgnoreCase("area")) {
                return (T) CreateModeTypes.AREA;
            }
            if (value.equalsIgnoreCase("volume")) {
                return (T) CreateModeTypes.VOLUME;
            }
        }
        return null;
    }

    private Consumer<CommandSender> createClaimOptionConsumer(GDPermissionUser src, GDClaim claim, MenuType optionType) {
        return consumer -> {
            showOptionPermissions(src, claim, optionType);
        };
    }

    private boolean containsCustomContext(Option option, Set<Context> contexts) {
        boolean hasClaimContext = false;
        for (Context context : contexts) {
            if (context.getKey().equals("gd_claim")) {
                hasClaimContext = true;
                continue;
            }
            // Options with a claim context is considered custom
            if (context.getKey().equals("gd_claim_default") || context.getKey().equals("server")) {
                continue;
            }

            return true;
        }

        // Always treat double and integer options as custom if not default
        if (hasClaimContext) {
            if (option.getAllowedType().isAssignableFrom(Double.class) || option.getAllowedType().isAssignableFrom(Integer.class)) {
                return true;
            }
        }

        return false;
    }
}