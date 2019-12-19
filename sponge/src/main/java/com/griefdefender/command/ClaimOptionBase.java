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
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimContexts;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.ContextKeys;
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
import com.griefdefender.permission.GDPermissionHolder;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.permission.option.GDOption;
import com.griefdefender.permission.ui.ClaimClickData;
import com.griefdefender.permission.ui.MenuType;
import com.griefdefender.permission.ui.OptionData;
import com.griefdefender.permission.ui.OptionData.OptionContextHolder;
import com.griefdefender.permission.ui.UIHelper;
import com.griefdefender.registry.OptionRegistryModule;
import com.griefdefender.text.action.GDCallbackHolder;
import com.griefdefender.util.CauseContextHelper;
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
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

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
            value = args[1];
        }

        Option option = null;
        if (commandOption != null) {
            option = GriefDefender.getRegistry().getType(Option.class, commandOption).orElse(null);
            if (commandOption != null && option == null) {
                TextAdapter.sendComponent(player, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.OPTION_NOT_FOUND, ImmutableMap.of(
                        "option", commandOption)));
                return;
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
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().PERMISSION_GLOBAL_OPTION);
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

        final Set<Context> contextSet = CauseContextHelper.generateContexts(player, claim, contexts);
        if (contextSet == null) {
            return;
        }

        if (claim != null) {
            if (commandOption == null && value == null && player.hasPermission(GDPermissions.COMMAND_LIST_CLAIM_OPTIONS)) {
                showOptionPermissions(src, (GDClaim) claim, MenuType.CLAIM);
                return;
            } else if (option != null && value != null) {
                if (!((GDOption) option).validateStringValue(value, false)) {
                    GriefDefenderPlugin.sendMessage(player, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.OPTION_INVALID_VALUE, 
                            ImmutableMap.of(
                                    "value", value,
                                    "option", option.getName(),
                                    "type", option.getAllowedType().getSimpleName())));
                    return;
                }

                MenuType type = MenuType.DEFAULT;
                for (Context context : contextSet) {
                    if (context.getKey().equals(ContextKeys.CLAIM)) {
                        type = MenuType.CLAIM;
                        break;
                    }
                    if (context.getKey().equals(ContextKeys.CLAIM_OVERRIDE)) {
                        type = MenuType.OVERRIDE;
                        break;
                    }
                }
                if (!option.isGlobal()) {
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
        final boolean isTaxEnabled = GriefDefenderPlugin.getActiveConfig(player.getWorld().getProperties()).getConfig().claim.bankTaxSystem;
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
        defaultContexts.add(ClaimContexts.GLOBAL_DEFAULT_CONTEXT);
        overrideContexts.add(ClaimContexts.GLOBAL_OVERRIDE_CONTEXT);
        overrideContexts.add(claim.getOverrideClaimContext());

        Map<String, OptionData> filteredContextMap = new HashMap<>();
        for (Map.Entry<Set<Context>, Map<String, String>> mapEntry : PermissionUtil.getInstance().getTransientOptions(this.subject).entrySet()) {
            final Set<Context> contextSet = mapEntry.getKey();
            if (contextSet.contains(claim.getDefaultTypeContext()) || contextSet.contains(ClaimContexts.GLOBAL_DEFAULT_CONTEXT)) {
                this.addFilteredContexts(src, filteredContextMap, contextSet, MenuType.DEFAULT, mapEntry.getValue());
            }
        }

        if (displayType == MenuType.DEFAULT) {
            final Set<Context> contexts = new HashSet<>();
            contexts.add(ClaimContexts.GLOBAL_DEFAULT_CONTEXT);
            for (Option option : OptionRegistryModule.getInstance().getAll()) {
                boolean found = false;
                for (Entry<String, OptionData> optionEntry : filteredContextMap.entrySet()) {
                    if (optionEntry.getValue().option == option) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    filteredContextMap.put(option.getPermission(), new OptionData(option, "undefined", displayType, contexts));
                }
            }
        }

        if (displayType == MenuType.CLAIM) {
            final Set<Context> contexts = new HashSet<>();
            contexts.add(ClaimContexts.GLOBAL_DEFAULT_CONTEXT);
            filteredContextMap.put(Options.PVP.getPermission(), new OptionData(Options.PVP, "undefined", MenuType.DEFAULT, contexts));
        }
        for (Map.Entry<Set<Context>, Map<String, String>> mapEntry : PermissionUtil.getInstance().getPermanentOptions(this.subject).entrySet()) {
            final Set<Context> contextSet = mapEntry.getKey();
            if (contextSet.contains(ClaimContexts.GLOBAL_DEFAULT_CONTEXT)) {
                this.addFilteredContexts(src, filteredContextMap, contextSet, MenuType.DEFAULT, mapEntry.getValue());
            }
            if (contextSet.contains(claim.getDefaultTypeContext())) {
                this.addFilteredContexts(src, filteredContextMap, contextSet, MenuType.DEFAULT, mapEntry.getValue());
            }
            if (displayType != MenuType.DEFAULT) {
                if (claim.isTown() || isAdmin) {
                    if (contextSet.contains(claim.getContext())) {
                        this.addFilteredContexts(src, filteredContextMap, contextSet, MenuType.CLAIM, mapEntry.getValue());
                    }
                }
                if (contextSet.contains(ClaimContexts.GLOBAL_OVERRIDE_CONTEXT)) {
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
            for (Map.Entry<Set<Context>, Map<String, String>> mapEntry : PermissionUtil.getInstance().getPermanentOptions(this.subject).entrySet()) {
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
            if (option.getName().contains("tax") && !GriefDefenderPlugin.getGlobalConfig().getConfig().claim.bankTaxSystem) {
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
        for (int i = 0; i < fillSize; i++) {
            textList.add(TextComponent.of(" "));
        }

        PaginationList.Builder paginationBuilder = PaginationList.builder()
                .title(claimOptionHead).padding(TextComponent.builder(" ").decoration(TextDecoration.STRIKETHROUGH, true).build()).contents(textList);
        final PaginationList paginationList = paginationBuilder.build();
        Integer activePage = 1;
        activePage = PaginationUtil.getInstance().getActivePage(player.getUniqueId());
        if (activePage == null) {
            activePage = 1;
        }
        this.lastActiveMenuTypeMap.put(player.getUniqueId(), displayType);
        paginationList.sendTo(player, activePage);
    }

    private void addFilteredContexts(GDPermissionUser src, Map<String, OptionData> filteredContextMap, Set<Context> contexts, MenuType type, Map<String, String> permissions) {
        final Player player = src.getOnlinePlayer();
        final GDPlayerData playerData = src.getInternalPlayerData();
        for (Map.Entry<String, String> permissionEntry : permissions.entrySet()) {
            final Option option = OptionRegistryModule.getInstance().getById(permissionEntry.getKey()).orElse(null);
            if (option == null) {
                continue;
            }
            if (option.getName().contains("tax") && !GriefDefenderPlugin.getGlobalConfig().getConfig().claim.bankTaxSystem) {
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
            if (optionData != null) {
                optionData.addContexts(option, permissionEntry.getValue(), type, contexts);
            } else {
                filteredContextMap.put(permissionEntry.getKey(), new OptionData(option, permissionEntry.getValue(), type, contexts));
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
                }
                else {
                    if (!player.hasPermission(GDPermissions.USER_CLAIM_OPTIONS +"." + option.getName().toLowerCase())) {
                        hoverEventText = MessageCache.getInstance().PERMISSION_OPTION_USE;
                        hasEditPermission = false;
                    }
                }
            }
        }

        final boolean customContexts = UIHelper.containsCustomContext(contexts);
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
            builder = TextComponent.builder()
                    .append(getOptionText(option, contexts))
                    .append(" ")
                    .append(TextComponent.builder()
                            .append(currentValue.toLowerCase(), color)
                            .hoverEvent(HoverEvent.showText(hoverEventText)));
        }
        if (hasEditPermission) {
            if (!option.getAllowedType().isAssignableFrom(Integer.class) && !option.getAllowedType().isAssignableFrom(Double.class)) {
                builder.clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(newOptionValueConsumer(src, claim, option, optionHolder, contexts, displayType, false))));
            } else {
                builder.append(TextComponent.builder()
                        .append(TextComponent.of(" >").decoration(TextDecoration.BOLD, true))
                        .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(newOptionValueConsumer(src, claim, option, optionHolder, contexts, displayType, false)))));
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

        return builder.build();
    }

    private boolean containsDefaultContext(Set<Context> contexts) {
        for (Context context : contexts) {
            if (context.getKey().contains("gd_claim_default")) {
                return true;
            }
        }
        return false;
    }

    private ClickEvent createClickEvent(Player src, Option option) {
        return ClickEvent.suggestCommand("/gd option " + option.getName() + " ");
    }
 
    private Consumer<CommandSource> newOptionValueConsumer(GDPermissionUser src, GDClaim claim, Option option, OptionContextHolder optionHolder, Set<Context> contexts, MenuType displayType, boolean leftArrow) {
        final String currentValue = optionHolder.getValue();
        return consumer -> {
            String newValue = "";
            if (option.getAllowedType().isAssignableFrom(Tristate.class)) {
                Tristate value = getMenuTypeValue(TypeToken.of(Tristate.class), currentValue);
                if (value == Tristate.TRUE) {
                    newValue = "false";
                } else if (value == Tristate.FALSE) {
                    newValue = "undefined";
                } else {
                    newValue = "true";
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
                    newValue = "rain";
                } else {
                    newValue = "undefined";
                }
            }
            if (option.getAllowedType().isAssignableFrom(Integer.class)) {
                Integer value = getMenuTypeValue(TypeToken.of(Integer.class), currentValue);
                if (leftArrow) {
                    if (value == null || value < 1) {
                        TextAdapter.sendComponent(src.getOnlinePlayer(), TextComponent.of("This value is NOT defined and cannot go any lower."));
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
                    if (value == null || value < 1) {
                        TextAdapter.sendComponent(src.getOnlinePlayer(), TextComponent.of("This value is NOT defined and cannot go any lower."));
                    } else {
                        value -= 1;
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
                        value += 1;
                    }
                }
                newValue = value == null ? "undefined" :String.valueOf(value);
            }

            Set<Context> newContexts = new HashSet<>();
            final boolean isCustom = UIHelper.containsCustomContext(contexts);
            if (!isCustom && displayType == MenuType.CLAIM) {
                newContexts.add(claim.getContext());
            } else {
                newContexts.addAll(contexts);
            }

            // Remove server context
            final Iterator<Context> iterator = newContexts.iterator();
            while (iterator.hasNext()) {
                final Context context = iterator.next();
                if (context.getKey().equals("server")) {
                    iterator.remove();
                }
            }

            PermissionUtil.getInstance().setOptionValue(this.subject, option.getPermission(), newValue, newContexts);
            showOptionPermissions(src, claim, displayType);
        };
    }

    private Consumer<CommandSource> adjustNumberConsumer(GDPermissionUser src, GDClaim claim, Option option, String currentValue, MenuType menuType, Set<Context> contexts) {
        return consumer -> {
            String newValue = "";
            final Set<Context> filteredContexts = applySelectedTypeContext(claim, menuType, contexts);
            if (option.getAllowedType().isAssignableFrom(Boolean.class)) {
                Boolean value = getMenuTypeValue(TypeToken.of(Boolean.class), currentValue);
                if (value == null) {
                    newValue = "true";
                } else if (value) {
                    newValue = "false";
                } else {
                    newValue = "undefined";
                }
                PermissionUtil.getInstance().setOptionValue(this.subject, option.getPermission(), newValue, filteredContexts);
            }
            if (option.getAllowedType().isAssignableFrom(Integer.class)) {
                newValue = getMenuTypeValue(TypeToken.of(String.class), currentValue);
                if (newValue == null) {
                    newValue = "undefined";
                }
                PermissionUtil.getInstance().setOptionValue(this.subject, option.getPermission(), newValue, filteredContexts);
            }
            showOptionPermissions(src, claim, menuType);
        };
    }

    private Set<Context> applySelectedTypeContext(Claim claim, MenuType menuType, Set<Context> contexts) {
        Set<Context> filteredContexts = new HashSet<>(contexts);
        for (Context context : contexts) {
            if (context.getKey().contains("gd_claim")) {
                filteredContexts.remove(context);
            }
        }
        if (menuType == MenuType.DEFAULT) {
            filteredContexts.add(ClaimContexts.GLOBAL_DEFAULT_CONTEXT);
        }
        if (menuType == MenuType.CLAIM) {
            filteredContexts.add(claim.getContext());
        }
        if (menuType == MenuType.OVERRIDE) {
            filteredContexts.add(ClaimContexts.GLOBAL_OVERRIDE_CONTEXT);
        }
        return filteredContexts;
    }

    private boolean contextsMatch(Set<Context> contexts, Set<Context> newContexts) {
        // first filter out gd claim contexts
        final Set<Context> filteredContexts = new HashSet<>();
        final Set<Context> filteredNewContexts = new HashSet<>();
        for (Context context : contexts) {
            if (context.getKey().contains("gd_claim")) {
                continue;
            }
            filteredContexts.add(context);
        }
        for (Context context : newContexts) {
            if (context.getKey().contains("gd_claim")) {
                continue;
            }
            filteredNewContexts.add(context);
        }
        return Objects.hash(filteredContexts) == Objects.hash(filteredNewContexts);
    }

    private static Set<Context> createClaimContextSet(GDClaim claim, Set<Context> contexts) {
        Set<Context> claimContexts = new HashSet<>();
        claimContexts.add(claim.getContext());
        for (Context context : contexts) {
            if (context.getKey().contains("world") || context.getKey().contains("gd_claim")) {
                continue;
            }
            claimContexts.add(context);
        }
        return claimContexts;
    }

    private static Component getOptionText(Option option, Set<Context> contexts) {
        boolean customContext = UIHelper.containsCustomContext(contexts);

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
            if (value.equalsIgnoreCase("rain")) {
                return (T) WeatherTypes.RAIN;
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

    private Consumer<CommandSource> createClaimOptionConsumer(GDPermissionUser src, GDClaim claim, MenuType optionType) {
        return consumer -> {
            showOptionPermissions(src, claim, optionType);
        };
    }
}