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
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimContexts;
import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.api.claim.ClaimTypes;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.PermissionResult;
import com.griefdefender.api.permission.flag.Flag;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.MessageDataConfig;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.event.GDFlagClaimEvent;
import com.griefdefender.internal.pagination.PaginationList;
import com.griefdefender.internal.util.NMSUtil;
import com.griefdefender.permission.GDPermissionHolder;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.registry.FlagRegistryModule;
import com.griefdefender.storage.BaseStorage;
import com.griefdefender.text.action.GDCallbackHolder;
import com.griefdefender.util.CauseContextHelper;
import com.griefdefender.util.ClaimClickData;
import com.griefdefender.util.PaginationUtil;
import com.griefdefender.util.PermissionUtil;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public abstract class ClaimFlagBase extends BaseCommand {

    private MessageDataConfig MESSAGE_DATA = GriefDefenderPlugin.getInstance().messageData;

    public enum FlagType {
        DEFAULT,
        CLAIM,
        OVERRIDE,
        INHERIT,
        GROUP,
        PLAYER
    }

    protected ClaimSubjectType subjectType;
    protected GDPermissionHolder subject;
    protected String friendlySubjectName;
    protected boolean isAdmin = false;
    protected GDPlayerData sourcePlayerData;
    private final Cache<UUID, FlagType> lastActiveFlagTypeMap = Caffeine.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES)
            .build();

    protected ClaimFlagBase(ClaimSubjectType type) {
        this.subjectType = type;
    }

    public void execute(Player player, String[] args) throws InvalidCommandArgument {
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
            if (args.length < 3) {
                throw new InvalidCommandArgument();
            }
            commandFlag = args[0];
            target = args[1];
            value = args[2];
        }
        final Flag flag = FlagRegistryModule.getInstance().getById(commandFlag).orElse(null);
        if (commandFlag != null && flag == null) {
            TextAdapter.sendComponent(player, MESSAGE_DATA.getMessage(MessageStorage.FLAG_NOT_FOUND, ImmutableMap.of(
                    "flag", commandFlag)));
            return;
        }

        if (flag != null && !player.hasPermission(GDPermissions.USER_CLAIM_FLAGS + flag.getPermission().replace(GDPermissions.FLAG_BASE, ""))) {
            TextAdapter.sendComponent(player, MessageCache.getInstance().PERMISSION_FLAG_USE);
            return;
        }

        if (target != null && target.equalsIgnoreCase("hand")) {
            ItemStack stack = NMSUtil.getInstance().getActiveItem(player);
            if (stack != null) {
                target = GDPermissionManager.getInstance().getPermissionIdentifier(stack);
            }
        }

        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        final Claim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(playerData, player.getLocation());
        final Set<Context> contextSet = CauseContextHelper.generateContexts(player, claim, contexts);
        if (contextSet == null) {
            return;
        }

        if (claim != null) {
            if (flag == null && value == null && player.hasPermission(GDPermissions.COMMAND_LIST_CLAIM_FLAGS)) {
                showFlagPermissions(player, (GDClaim) claim, FlagType.CLAIM);
                return;
            } else if (flag != null && value != null) {
               // if (context != null) {
                    /*claimContext = CommandHelper.validateCustomContext(src, claim, context);
                    if (claimContext == null) {
                        final Text message = GriefDefenderPlugin.getInstance().messageData.flagInvalidContext
                                .apply(ImmutableMap.of(
                                "context", context,
                                "flag", flag)).build();
                        GriefDefenderPlugin.sendMessage(src, message);
                        return CommandResult.success();
                    }*/
                //}

                GDCauseStackManager.getInstance().pushCause(player);
                ((GDPermissionManager) GriefDefender.getPermissionManager()).setPermission(claim, this.subject, this.friendlySubjectName, flag, target, PermissionUtil.getInstance().getTristateFromString(value.toUpperCase()), contextSet);
                GDCauseStackManager.getInstance().popCause();
                return;
            }
        } else {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CLAIM_NOT_FOUND);
        }
    }

    protected void showFlagPermissions(CommandSender src, GDClaim claim, FlagType flagType) {
        boolean isAdmin = false;
        if (src.hasPermission(GDPermissions.DELETE_CLAIM_ADMIN)) {
            isAdmin = true;
        }
        if (src instanceof Player) {
            final Player player = (Player) src;
            final FlagType lastFlagType = this.lastActiveFlagTypeMap.getIfPresent(player.getUniqueId());
            if (lastFlagType != null && lastFlagType != flagType) {
                PaginationUtil.getInstance().resetActivePage(player.getUniqueId());
            }
        }
        final Component whiteOpenBracket = TextComponent.of("[", TextColor.AQUA);
        final Component whiteCloseBracket = TextComponent.of("]", TextColor.AQUA);
        final Component showOverrideText = MESSAGE_DATA.getMessage(MessageStorage.UI_CLICK_FILTER_TYPE, 
                ImmutableMap.of("type", TextComponent.of("OVERRIDE", TextColor.RED)));
        final Component showDefaultText = MESSAGE_DATA.getMessage(MessageStorage.UI_CLICK_FILTER_TYPE, 
                ImmutableMap.of("type", TextComponent.of("DEFAULT", TextColor.LIGHT_PURPLE)));
        final Component showClaimText = MESSAGE_DATA.getMessage(MessageStorage.UI_CLICK_FILTER_TYPE, 
                ImmutableMap.of("type", TextComponent.of("CLAIM", TextColor.GOLD)));
        final Component showInheritText = MESSAGE_DATA.getMessage(MessageStorage.UI_CLICK_FILTER_TYPE, 
                ImmutableMap.of("type", TextComponent.of("INHERIT", TextColor.AQUA)));
        Component defaultFlagText = TextComponent.empty();
        if (isAdmin) {
            defaultFlagText = TextComponent.builder("")
                    .append(flagType == FlagType.DEFAULT ? TextComponent.builder("")
                            .append(whiteOpenBracket)
                            .append("DEFAULT", TextColor.LIGHT_PURPLE)
                            .append(whiteCloseBracket).build() : TextComponent.of("DEFAULT", TextColor.GRAY))
                    .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createClaimFlagConsumer(src, claim, FlagType.DEFAULT))))
                    .hoverEvent(HoverEvent.showText(showDefaultText)).build();
        }
        final Component overrideFlagText = TextComponent.builder("")
                .append(flagType == FlagType.OVERRIDE ? TextComponent.builder("")
                        .append(whiteOpenBracket)
                        .append("OVERRIDE", TextColor.RED)
                        .append(whiteCloseBracket).build() : TextComponent.of("OVERRIDE", TextColor.GRAY))
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createClaimFlagConsumer(src, claim, FlagType.OVERRIDE))))
                .hoverEvent(HoverEvent.showText(showOverrideText)).build();
        final Component claimFlagText = TextComponent.builder("")
                .append(flagType == FlagType.CLAIM ? TextComponent.builder("")
                        .append(whiteOpenBracket)
                        .append("CLAIM", TextColor.YELLOW)
                        .append(whiteCloseBracket).build() : TextComponent.of("CLAIM", TextColor.GRAY))
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createClaimFlagConsumer(src, claim, FlagType.CLAIM))))
                .hoverEvent(HoverEvent.showText(showClaimText)).build();
        final Component inheritFlagText = TextComponent.builder("")
                .append(flagType == FlagType.INHERIT ? TextComponent.builder("")
                        .append(whiteOpenBracket)
                        .append("INHERIT", TextColor.AQUA)
                        .append(whiteCloseBracket).build() : TextComponent.of("INHERIT", TextColor.GRAY))
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createClaimFlagConsumer(src, claim, FlagType.INHERIT))))
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
        Map<Set<Context>, Map<String, Component>> contextMap = new HashMap<>();
        List<Component> textList = new ArrayList<>();
        Set<Context> defaultContexts = new HashSet<>();
        Set<Context> overrideContexts = new HashSet<>();
        //contexts.add(claim.world.getContext());
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

        Map<Set<Context>, Map<String, Boolean>> defaultPermissionMap = new HashMap<>();
        Map<Set<Context>, Map<String, Boolean>> defaultTransientPermissionMap = new HashMap<>();
        for (Map.Entry<Set<Context>, Map<String, Boolean>> mapEntry : PermissionUtil.getInstance().getTransientPermissions(claim, this.subject).entrySet()) {
            final Set<Context> contextSet = mapEntry.getKey();
            if (contextSet.contains(claim.getDefaultTypeContext()) || contextSet.contains(ClaimContexts.GLOBAL_DEFAULT_CONTEXT)) {
                defaultTransientPermissionMap.put(mapEntry.getKey(), mapEntry.getValue());
            }
        }

        Map<Set<Context>, Map<String, Boolean>> overridePermissionMap = new HashMap<>();
        Map<Set<Context>, Map<String, Boolean>> claimPermissionMap = new HashMap<>();
        for (Map.Entry<Set<Context>, Map<String, Boolean>> mapEntry : PermissionUtil.getInstance().getPermanentPermissions(claim, this.subject).entrySet()) {
            final Set<Context> contextSet = mapEntry.getKey();
            if (contextSet.contains(claim.getDefaultTypeContext())) {
                defaultPermissionMap.put(mapEntry.getKey(), mapEntry.getValue());
            }
            if (contextSet.contains(claim.getContext())) {
                claimPermissionMap.put(mapEntry.getKey(), mapEntry.getValue());
            }
            if (contextSet.contains(claim.getOverrideClaimContext())) {
                overridePermissionMap.put(mapEntry.getKey(), mapEntry.getValue());
            } else if (contextSet.contains(claim.getOverrideTypeContext())) {
                overridePermissionMap.put(mapEntry.getKey(), mapEntry.getValue());
            }
            if (contextSet.contains(ClaimContexts.GLOBAL_DEFAULT_CONTEXT)) {
                defaultPermissionMap.put(mapEntry.getKey(), mapEntry.getValue());
            }
            if (contextSet.contains(ClaimContexts.GLOBAL_OVERRIDE_CONTEXT)) {
                overridePermissionMap.put(mapEntry.getKey(), mapEntry.getValue());
            }
        }

        Map<Set<Context>, ClaimClickData> inheritPermissionMap = Maps.newHashMap();

        final List<Claim> inheritParents = claim.getInheritedParents();
        Collections.reverse(inheritParents);
        for (Claim current : inheritParents) {
            GDClaim currentClaim = (GDClaim) current;
            for (Map.Entry<Set<Context>, Map<String, Boolean>> mapEntry : PermissionUtil.getInstance().getPermanentPermissions(claim, this.subject).entrySet()) {
                final Set<Context> contextSet = mapEntry.getKey();
                if (contextSet.contains(currentClaim.getContext())) {
                    inheritPermissionMap.put(mapEntry.getKey(), new ClaimClickData(currentClaim, mapEntry.getValue()));
                }
            }
        }

        final Component denyText = claim.allowEdit((Player) src);
        final boolean hasPermission = denyText == null;
        if (flagType == FlagType.CLAIM) {
            final Map<String, Set<Context>> permissionMap = new HashMap<>();
            for (Map.Entry<Set<Context>, Map<String, Boolean>> mapEntry : defaultTransientPermissionMap.entrySet()) {
                final Set<Context> contextSet = mapEntry.getKey();
                Map<String, Component> textMap = contextMap.get(contextSet);
                if (textMap == null) {
                    textMap = new HashMap<String, Component>();
                    contextMap.put(contextSet, textMap);
                }
                for (Map.Entry<String, Boolean> permissionEntry : mapEntry.getValue().entrySet()) {
                    Component flagText = null;
                    String flagPermission = permissionEntry.getKey();
                    /*if (textMap.containsKey(flagPermission)) {
                        // only display flags not overridden
                        continue;
                    }*/
    
                    Boolean flagValue = permissionEntry.getValue();
                    String baseFlagPerm = flagPermission.replace(GDPermissions.FLAG_BASE + ".",  "").replace(".*", "");
                    final Flag baseFlag = FlagRegistryModule.getInstance().getById(flagPermission).orElse(null);
                    if (baseFlag == null) {
                        // invalid flag
                        continue;
                    }

                    boolean hasOverride = false;
                    for (Map.Entry<Set<Context>, Map<String, Boolean>> overrideEntry : overridePermissionMap.entrySet()) {
                        final Set<Context> overrideContextSet = overrideEntry.getKey();
                        for (Map.Entry<String, Boolean> overridePermissionEntry : overrideEntry.getValue().entrySet()) {
                            if (flagPermission.contains(overridePermissionEntry.getKey())) {
                                hasOverride = true;
                                Component undefinedText = null;
                                if (hasPermission) {
                                    undefinedText = TextComponent.builder("")
                                            .append("undefined", TextColor.GRAY)
                                            .hoverEvent(HoverEvent.showText(MESSAGE_DATA.getMessage(MessageStorage.FLAG_UI_OVERRIDE_PERMISSION,
                                                    ImmutableMap.of("flag", baseFlagPerm))))
                                            .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createFlagConsumer(src, claim, overrideContextSet, flagPermission, Tristate.UNDEFINED, flagType, FlagType.CLAIM, false)))).build();
                                } else {
                                    undefinedText = TextComponent.builder("")
                                            .append("undefined", TextColor.GRAY)
                                            .hoverEvent(HoverEvent.showText(denyText)).build();
                                }
                                flagText = TextComponent.builder("")
                                        .append(undefinedText)
                                        .append("  ")
                                        .append("[", TextColor.AQUA)
                                        .append(String.valueOf(overridePermissionEntry.getValue()), TextColor.RED)
                                        .append("]", TextColor.AQUA)
                                        .hoverEvent(HoverEvent.showText(MessageCache.getInstance().FLAG_UI_OVERRIDE_NO_PERMISSION))
                                        .build();
                                break;
                            }
                        }
                    }
                    if (!hasOverride) {
                        // check if transient default has been overridden and if so display that value instead
                        final Set<Context> claimContexts = createClaimContextSet(claim, contextSet);
                        final Map<String, Boolean> subjectPerms = PermissionUtil.getInstance().getPermissions(this.subject, claimContexts);
                        Boolean overriddenValue = subjectPerms.get(flagPermission);
                        if (overriddenValue == null && this.subject != GriefDefenderPlugin.DEFAULT_HOLDER) {
                            // Check claim
                            final Map<String, Boolean> claimPerms = claimPermissionMap.get(claimContexts);
                            if (claimPerms != null) {
                                overriddenValue = claimPerms.get(flagPermission);
                            }
                        }
    
                        final Tristate currentValue = overriddenValue == null ? Tristate.UNDEFINED : Tristate.fromBoolean(overriddenValue);
                        Component undefinedText = null;
                        if (hasPermission) {
                            undefinedText = TextComponent.builder("")
                                .append(currentValue == Tristate.UNDEFINED ? 
                                        TextComponent.builder("")
                                            .append(whiteOpenBracket)
                                            .append("undefined", TextColor.GOLD)
                                            .append(whiteCloseBracket)
                                            .build() : 
                                        TextComponent.builder("")
                                            .append("undefined", TextColor.GRAY)
                                            .append(TextComponent.empty())
                                            .build())
                                .hoverEvent(HoverEvent.showText(MESSAGE_DATA.getMessage(MessageStorage.FLAG_NOT_SET,
                                        ImmutableMap.of(
                                            "flag", baseFlagPerm,
                                            "value", TextComponent.of(flagValue, TextColor.LIGHT_PURPLE)))))
                                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createFlagConsumer(src, claim, claimContexts, flagPermission, Tristate.UNDEFINED, flagType, FlagType.CLAIM, false)))).build();
                        } else {
                            undefinedText = TextComponent.builder("").append(
                                    TextComponent.builder("")
                                    .append(currentValue == Tristate.UNDEFINED ? 
                                            TextComponent.builder("")
                                                .append(whiteOpenBracket)
                                                .append("undefined", TextColor.GOLD)
                                                .append(whiteCloseBracket)
                                                .build() : 
                                            TextComponent.builder("")
                                                .append("undefined", TextColor.GRAY)
                                                .append(currentValue == Tristate.UNDEFINED ? whiteCloseBracket : TextComponent.empty())
                                                .build())
                                    .build())
                                    .hoverEvent(HoverEvent.showText(denyText)).build();
                        }

                        final Component trueText = TextComponent.builder("")
                                .append(getClickableText(src, claim, claimContexts, flagPermission, currentValue, Tristate.TRUE, flagType, FlagType.CLAIM, false).color(TextColor.GRAY)).build();
                        final Component falseText = TextComponent.builder("")
                                .append(getClickableText(src, claim, claimContexts, flagPermission, currentValue, Tristate.FALSE, flagType, FlagType.CLAIM, false).color(TextColor.GRAY)).build();
                        flagText = TextComponent.builder("")
                                .append(undefinedText)
                                .append("  ")
                                .append(trueText)
                                .append("  ")
                                .append(falseText).build();
                    }
                    Component baseFlagText = getFlagText(defaultContexts, flagPermission, baseFlag.toString(), flagType); 
                    flagText = TextComponent.builder("")
                            .append(baseFlagText)
                            .append("  ")
                            .append(flagText).build();
                    textMap.put(flagPermission, flagText);
                    permissionMap.put(flagPermission, contextSet);
                    textList.add(flagText);
                }
            }
            for (Map.Entry<Set<Context>, Map<String, Boolean>> mapEntry : claimPermissionMap.entrySet()) {
                final Set<Context> contextSet = mapEntry.getKey();
                Map<String, Component> textMap = contextMap.get(contextSet);
                if (textMap == null) {
                    textMap = new HashMap<String, Component>();
                    contextMap.put(contextSet, textMap);
                }
                for (Map.Entry<String, Boolean> permissionEntry : mapEntry.getValue().entrySet()) {
                    String flagPermission = permissionEntry.getKey();
                    final Flag claimFlag = FlagRegistryModule.getInstance().getById(flagPermission).orElse(null);
                    if (claimFlag == null) {
                        // invalid flag
                        continue;
                    }
                    final String baseFlag = flagPermission.replace(GDPermissions.FLAG_BASE + ".",  "").replace(".*", "");
                    final Set<Context> contexts = permissionMap.get(flagPermission);
                    if (this.ignoreFlagEntry(contextSet)) {
                        continue;
                    }

                    Boolean flagValue = permissionEntry.getValue();
                    Component flagText = null;
    
                    boolean hasOverride = false;
                    for (Map.Entry<Set<Context>, Map<String, Boolean>> overrideEntry : overridePermissionMap.entrySet()) {
                        final Set<Context> overrideContextSet = overrideEntry.getKey();
                        for (Map.Entry<String, Boolean> overridePermissionEntry : overrideEntry.getValue().entrySet()) {
                            if (flagPermission.contains(overridePermissionEntry.getKey())) {
                                hasOverride = true;
                                Component undefinedText = null;
                                if (hasPermission) {
                                    undefinedText = TextComponent.builder("")
                                        .append("undefined", TextColor.GRAY)
                                        .hoverEvent(HoverEvent.showText(MESSAGE_DATA.getMessage(MessageStorage.FLAG_UI_OVERRIDE_PERMISSION,
                                                ImmutableMap.of("flag", TextComponent.of(baseFlag, TextColor.GREEN)))))
                                        .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createFlagConsumer(src, claim, overrideContextSet, flagPermission, Tristate.UNDEFINED, flagType, FlagType.CLAIM, false)))).build();
                                } else {
                                    undefinedText = TextComponent.builder("")
                                            .append("undefined", TextColor.GRAY)
                                            .hoverEvent(HoverEvent.showText(denyText)).build();
                                }
                                flagText = TextComponent.builder("")
                                        .append(undefinedText)
                                        .append("  ")
                                        .append("[", TextColor.AQUA)
                                        .append(String.valueOf(overridePermissionEntry.getValue()), TextColor.RED)
                                        .append("]", TextColor.AQUA)
                                        .hoverEvent(HoverEvent.showText(MessageCache.getInstance().FLAG_UI_OVERRIDE_NO_PERMISSION))
                                        .build();
                                break;
                            }
                        }
                    }
                    if (!hasOverride) {
                        final Tristate currentValue = Tristate.fromBoolean(flagValue);
                        ClaimClickData claimClickData = inheritPermissionMap.get(flagPermission);
                        if (claimClickData != null) {
                            Set<Context> claimClickContexts = new HashSet<>(contextSet);
                            claimClickContexts.remove(claim.getContext());
                            claimClickContexts.add(claimClickData.claim.getContext());
                            final Component undefinedText = getClickableText(src, claimClickData.claim, claimClickContexts, flagPermission, currentValue, Tristate.UNDEFINED, flagType, FlagType.INHERIT, false);
                            final Component trueText = getClickableText(src, claimClickData.claim, claimClickContexts, flagPermission, currentValue, Tristate.TRUE, flagType, FlagType.INHERIT, false);
                            final Component falseText = getClickableText(src, claimClickData.claim, claimClickContexts, flagPermission, currentValue, Tristate.FALSE, flagType, FlagType.INHERIT, false);
                            flagText = TextComponent.builder("")
                                    .append(undefinedText)
                                    .append("  ")
                                    .append(trueText)
                                    .append("  ")
                                    .append(falseText).build();
                        } else {
                            final Component undefinedText = getClickableText(src, claim, contextSet, flagPermission, currentValue, Tristate.UNDEFINED, flagType, FlagType.CLAIM, false);
                            final Component trueText = getClickableText(src, claim, contextSet, flagPermission, currentValue, Tristate.TRUE, flagType, FlagType.CLAIM, false);
                            final Component falseText = getClickableText(src, claim, contextSet, flagPermission, currentValue, Tristate.FALSE, flagType, FlagType.CLAIM, false);
                            flagText = TextComponent.builder("")
                                    .append(undefinedText)
                                    .append("  ")
                                    .append(trueText)
                                    .append("  ")
                                    .append(falseText).build();
                        }
                    }
    
                    Component currentText = textMap.get(flagPermission);
                    if (currentText == null) {
                        currentText = TextComponent.builder("")
                                .append(baseFlag, TextColor.GREEN)
                                .append(" ")
                                .hoverEvent(HoverEvent.showText(CommandHelper.getBaseFlagOverlayText(baseFlag))).build();
                    }
                    Component baseFlagText = getFlagText(defaultContexts, flagPermission, baseFlag.toString(), flagType); 
                    flagText = TextComponent.builder("")
                            .append(baseFlagText)
                            .append("  ")
                            .append(flagText).build();
                    textMap.put(flagPermission, flagText);//Text.join(currentText, Text.of(customFlag ? "" : ", ", flagText)));
                    textList.add(flagText);
                }
            }
        } else if (flagType == FlagType.OVERRIDE) {
            for (Map.Entry<Set<Context>, Map<String, Boolean>> mapEntry : overridePermissionMap.entrySet()) {
                final Set<Context> contextSet = mapEntry.getKey();
                Map<String, Component> textMap = contextMap.get(contextSet);
                if (textMap == null) {
                    textMap = new HashMap<String, Component>();
                    contextMap.put(contextSet, textMap);
                }
                for (Map.Entry<String, Boolean> permissionEntry : mapEntry.getValue().entrySet()) {
                    String flagPermission = permissionEntry.getKey();
                    Boolean flagValue = permissionEntry.getValue();
                    Component flagText = TextComponent.builder("")
                            .append(getClickableText(src, claim, contextSet, flagPermission, Tristate.fromBoolean(flagValue), FlagType.OVERRIDE).color(TextColor.RED)).build();
                  //  Text currentText = textMap.get(flagPermission);
                    String baseFlag = flagPermission.replace(GDPermissions.FLAG_BASE + ".",  "").replace(".*", "");
                    //boolean customFlag = false;
                    //Text hover = CommandHelper.getBaseFlagOverlayText(baseFlag);
                    Component baseFlagText = getFlagText(contextSet, flagPermission, baseFlag, flagType);
                    flagText = TextComponent.builder("")
                            .append(baseFlagText)
                            .append("  ")
                            .append(flagText).build();
                    /*if (claim.isWilderness()) {
                        Text reason = GriefDefenderPlugin.getGlobalConfig().getConfig().bans.getReason(baseFlag);
                        if (reason != null && !reason.isEmpty()) {
                            hover = Text.of(TextColors.GREEN, "Ban Reason", TextColors.WHITE, " : ", reason);
                        }
                    }
                    if (currentText == null) {
                        customFlag = true;
                        // custom flag
                        currentText = TextComponent.builder("").append(Text.of(
                                TextColors.GREEN, baseFlag, "  ",
                                TextColors.WHITE, "["))
                                .hoverEvent(HoverEvent.showText(hover)).build();
                    }
                    final Text text = Text.join(currentText, Text.of(customFlag ? "" : ", ", flagText, TextColors.WHITE, "]"));*/
                    textMap.put(flagPermission, flagText);
                    textList.add(flagText);
                }
            }
        } else if (flagType == FlagType.INHERIT) {
            for (Map.Entry<Set<Context>, ClaimClickData> mapEntry : inheritPermissionMap.entrySet()) {
                final Set<Context> contextSet = mapEntry.getKey();
                Map<String, Component> textMap = contextMap.get(contextSet);
                if (textMap == null) {
                    textMap = new HashMap<String, Component>();
                    contextMap.put(contextSet, textMap);
                }
                Map<String, Boolean> permissionMap = (Map<String, Boolean>) mapEntry.getValue().value;
                for (Map.Entry<String, Boolean> permissionEntry : permissionMap.entrySet()) {
                    String flagPermission = permissionEntry.getKey();
                    final String baseFlagPerm = flagPermission.replace(GDPermissions.FLAG_BASE + ".",  "").replace(".*", "");
                    final ClaimClickData claimClickData = mapEntry.getValue();
                    Component flagText = null;
                    final Flag baseFlag = FlagRegistryModule.getInstance().getById(flagPermission).orElse(null);
                    if (baseFlag == null) {
                        // invalid flag
                        continue;
                    }
    
                    boolean hasOverride = false;
                    for (Map.Entry<Set<Context>, Map<String, Boolean>> overrideEntry : overridePermissionMap.entrySet()) {
                        final Set<Context> overrideContextSet = overrideEntry.getKey();
                        for (Map.Entry<String, Boolean> overridePermissionEntry : overrideEntry.getValue().entrySet()) {
                            if (flagPermission.contains(overridePermissionEntry.getKey())) {
                                hasOverride = true;
                                final Component undefinedText = TextComponent.builder("")
                                        .append("undefined", TextColor.GRAY)
                                        .hoverEvent(HoverEvent.showText(MESSAGE_DATA.getMessage(MessageStorage.FLAG_UI_OVERRIDE_PERMISSION,
                                                ImmutableMap.of("flag", TextComponent.of(baseFlag.toString().toLowerCase(), TextColor.GREEN)))))
                                        .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createFlagConsumer(src, claim, overrideContextSet, flagPermission, Tristate.UNDEFINED, flagType, FlagType.CLAIM, false)))).build();
                                flagText = TextComponent.builder("")
                                        .append(undefinedText)
                                        .append("  ")
                                        .append("[", TextColor.AQUA)
                                        .append(String.valueOf(overridePermissionEntry.getValue()))
                                        .append("]", TextColor.AQUA)
                                        .hoverEvent(HoverEvent.showText(MessageCache.getInstance().FLAG_UI_OVERRIDE_NO_PERMISSION))
                                        .build();
                                break;
                            }
                        }
                    }
    
                    if (!hasOverride) {
                        flagText = TextComponent.builder("")
                                .append(getClickableText(src, claimClickData.claim, contextSet, flagPermission, Tristate.fromBoolean(permissionEntry.getValue()), FlagType.INHERIT).color(TextColor.AQUA))
                                .build();
                    }
    
                    Component currentText = textMap.get(flagPermission);
                    if (currentText == null) {
                        currentText = TextComponent.builder("")
                                .append(baseFlagPerm, TextColor.GREEN)
                                .append("  ")
                                .hoverEvent(HoverEvent.showText(CommandHelper.getBaseFlagOverlayText(baseFlagPerm))).build();
                    }
                    Component baseFlagText = getFlagText(defaultContexts, flagPermission, baseFlag.toString(), flagType); 
                    flagText = TextComponent.builder("")
                            .append(baseFlagText)
                            .append("  ")
                            .append(flagText).build();
                    textMap.put(flagPermission, flagText);
                    textList.add(flagText);
                }
            }
        } else if (flagType == FlagType.DEFAULT) {
            final Map<String, Set<Context>> permissionMap = new HashMap<>();
            for (Map.Entry<Set<Context>, Map<String, Boolean>> mapEntry : defaultTransientPermissionMap.entrySet()) {
                final Set<Context> contextSet = mapEntry.getKey();
                Map<String, Component> textMap = contextMap.get(contextSet);
                if (textMap == null) {
                    textMap = new HashMap<String, Component>();
                    contextMap.put(contextSet, textMap);
                }
                for (Map.Entry<String, Boolean> permissionEntry : mapEntry.getValue().entrySet()) {
                    Component flagText = null;
                    String flagPermission = permissionEntry.getKey();
                    Boolean flagValue = permissionEntry.getValue();
                    String baseFlagPerm = flagPermission.replace(GDPermissions.FLAG_BASE + ".",  "").replace(".*", "");
                    final Flag baseFlag = FlagRegistryModule.getInstance().getById(baseFlagPerm).orElse(null);
                    if (baseFlag == null) {
                        continue;
                    }
    
                    // check if transient default has been overridden and if so display that value instead
                    Tristate actualValue = PermissionUtil.getInstance().getPermissionValue(claim, this.subject, flagPermission, contextSet);

                    final Component trueText = getClickableText(src, claim, contextSet, flagPermission, actualValue, Tristate.TRUE, flagType, FlagType.DEFAULT, false).color(TextColor.GRAY);
                    final Component falseText = getClickableText(src, claim, contextSet, flagPermission, actualValue, Tristate.FALSE, flagType, FlagType.DEFAULT, false).color(TextColor.GRAY);

                    flagText = TextComponent.builder("")
                            .append(trueText)
                            .append("  ")
                            .append(falseText).build();
                    Component baseFlagText = getFlagText(defaultContexts, flagPermission, baseFlag.toString(), flagType); 
                    flagText = TextComponent.builder("")
                            .append(baseFlagText)
                            .append("  ")
                            .append(flagText).build();
                    textMap.put(flagPermission, flagText);
                    permissionMap.put(flagPermission, contextSet);
                    textList.add(flagText);
                }
            }

            // Handle custom defaults
            for (Map.Entry<Set<Context>, Map<String, Boolean>> mapEntry : defaultPermissionMap.entrySet()) {
                final Set<Context> contextSet = mapEntry.getKey();
                Map<String, Component> textMap = contextMap.get(contextSet);
                if (textMap == null) {
                    textMap = new HashMap<String, Component>();
                    contextMap.put(contextSet, textMap);
                }
                for (Map.Entry<String, Boolean> permissionEntry : mapEntry.getValue().entrySet()) {
                    Component flagText = null;
                    String flagPermission = permissionEntry.getKey();
                    final Flag claimFlag = FlagRegistryModule.getInstance().getById(flagPermission).orElse(null);
                    if (claimFlag == null) {
                        // invalid flag
                        continue;
                    }
                    final String baseFlag = flagPermission.replace(GDPermissions.FLAG_BASE + ".",  "").replace(".*", "");
                    final Set<Context> contexts = permissionMap.get(flagPermission);
                    if (this.ignoreFlagEntry(contexts)) {
                        continue;
                    }

                    Boolean flagValue = permissionEntry.getValue();
    
                    // check if transient default has been overridden and if so display that value instead
                    final Map<String, Boolean> subjectPerms = PermissionUtil.getInstance().getPermissions(this.subject, contextSet);
                    Boolean defaultTransientOverrideValue = subjectPerms.get(flagPermission);
                    if (defaultTransientOverrideValue != null) {
                        flagValue = defaultTransientOverrideValue;
                    }
    
                    final Tristate currentValue = Tristate.fromBoolean(flagValue);
                    final Component trueText = getClickableText(src, claim, contextSet, flagPermission, currentValue, Tristate.TRUE, flagType, FlagType.DEFAULT, false).color(TextColor.GRAY);
                    final Component falseText = getClickableText(src, claim, contextSet, flagPermission, currentValue, Tristate.FALSE, flagType, FlagType.DEFAULT, false).color(TextColor.GRAY);
                    final Component undefinedText = getClickableText(src, claim, contextSet, flagPermission, currentValue, Tristate.UNDEFINED, flagType, FlagType.DEFAULT, false).color(TextColor.GRAY);
                    flagText = TextComponent.builder("")
                            .append(trueText)
                            .append("  ")
                            .append(falseText)
                            .append(" ")
                            .append(undefinedText).build();
                    Component baseFlagText = getFlagText(defaultContexts, flagPermission, baseFlag.toString(), flagType); 
                    flagText = TextComponent.builder("")
                            .append(baseFlagText)
                            .append("  ")
                            .append(flagText).build();
                    textMap.put(flagPermission, flagText);
                    textList.add(flagText);
                }
            }
        }

        //List<Text> textList = new ArrayList<>(contextMap.values());
        Collections.sort(textList, CommandHelper.PLAIN_COMPARATOR);
        int fillSize = 20 - (textList.size() + 2);
        for (int i = 0; i < fillSize; i++) {
            textList.add(TextComponent.of(" "));
        }

        //PaginationList.Builder paginationBuilder = paginationService.builder()
        //       .title(claimFlagHead).padding(Text.of(TextStyles.STRIKETHROUGH,"-")).contents(textList);
        //final PaginationList paginationList = paginationBuilder.build();
        PaginationList.Builder paginationBuilder = PaginationList.builder()
                .title(claimFlagHead).padding(TextComponent.builder(" ").decoration(TextDecoration.STRIKETHROUGH, true).build()).contents(textList);
        final PaginationList paginationList = paginationBuilder.build();
        Integer activePage = 1;
        if (src instanceof Player) {
            final Player player = (Player) src;
            activePage = PaginationUtil.getInstance().getActivePage(player.getUniqueId());
            if (activePage == null) {
                activePage = 1;
            }
            this.lastActiveFlagTypeMap.put(player.getUniqueId(), flagType);
        }
        paginationList.sendTo(src, activePage);
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

    private static Component getFlagText(Set<Context> contexts, String flagPermission, String baseFlag, FlagType type) {
        //final String flagTarget = GPPermissionHandler.getTargetPermission(flagPermission);
        TextComponent.Builder builder = TextComponent.builder("");
        boolean customContext = false;
        for (Context context : contexts) {
            if (type != FlagType.INHERIT && type != FlagType.OVERRIDE && (context.getKey().contains("gd_") || context.getKey().contains("world"))) {
                continue;
            }

            customContext = true;
            builder.append(context.getKey() + "=", TextColor.GREEN).append(context.getValue() + "\n").build();
        }

        final Component hoverText = builder.build();
        final Component baseFlagText = TextComponent.builder("").color(customContext ? TextColor.YELLOW : TextColor.GREEN).append(baseFlag.toString() + " ")
                .hoverEvent(HoverEvent.showText(customContext ? hoverText : CommandHelper.getBaseFlagOverlayText(baseFlag))).build();
        return baseFlagText;
    }

    private Consumer<CommandSender> createFlagConsumer(CommandSender src, GDClaim claim, Set<Context> contexts, String flagPermission, Tristate flagValue, FlagType displayType, FlagType flagType, boolean toggleType) {
        return consumer -> {
            // Toggle DEFAULT type
            final String targetId = GDPermissionManager.getInstance().getTargetPermission(flagPermission);
            final Flag claimFlag = FlagRegistryModule.getInstance().getById(flagPermission).orElse(null);
            if (claimFlag == null) {
                return;
            }
            //Context claimContext = claim.getContext();
            Tristate newValue = Tristate.UNDEFINED;
            if (flagType == FlagType.DEFAULT) {
                if (toggleType) {
                    if (flagValue == Tristate.TRUE) {
                        newValue = Tristate.FALSE;
                    } else {
                        newValue = Tristate.TRUE;
                    }
                    ClaimType claimType = claim.getType();
                    if (claimType == ClaimTypes.SUBDIVISION) {
                        claimType = ClaimTypes.BASIC;
                    }
                    final Boolean defaultValue = BaseStorage.CLAIM_FLAG_DEFAULTS.get(claimType).get(claimFlag.getName());
                    if (defaultValue != null && defaultValue == newValue.asBoolean()) {
                        newValue = Tristate.UNDEFINED;
                    }
                }
                //claimContext = CommandHelper.validateCustomContext(src, claim, "default");
            // Toggle CLAIM type
            } else if (flagType == FlagType.CLAIM) {
                if (flagValue == Tristate.TRUE) {
                    newValue = Tristate.FALSE;
                } else if (flagValue == Tristate.UNDEFINED) {
                    newValue = Tristate.TRUE;
                }
            // Toggle OVERRIDE type
            } else if (flagType == FlagType.OVERRIDE) {
                if (flagValue == Tristate.TRUE) {
                    newValue = Tristate.FALSE;
                } else if (flagValue == Tristate.UNDEFINED) {
                    newValue = Tristate.TRUE;
                }
            }

            GDCauseStackManager.getInstance().pushCause(src);
            GDFlagClaimEvent.Set event = new GDFlagClaimEvent.Set(claim, this.subject, claimFlag, targetId, toggleType ? newValue :flagValue, contexts);
            GriefDefender.getEventManager().post(event);
            GDCauseStackManager.getInstance().popCause();
            if (event.cancelled()) {
                return;
            }
            PermissionResult result = CommandHelper.applyFlagPermission(src, this.subject, "ALL", claim, flagPermission, "any", toggleType ? newValue : flagValue, contexts, flagType, true);
            if (result.successful()) {
                showFlagPermissions(src, claim, displayType);
            }
        };
    }

    private Component getClickableText(CommandSender src, GDClaim claim, Set<Context> contexts, String flagPermission, Tristate flagValue, FlagType flagType) {
        return getClickableText(src, claim, contexts, flagPermission, Tristate.UNDEFINED, flagValue, FlagType.CLAIM, flagType, true);
    }

    private Component getClickableText(CommandSender src, GDClaim claim, Set<Context> contexts, String flagPermission, Tristate currentValue, Tristate flagValue, FlagType displayType, FlagType flagType, boolean toggleType) {
        Component hoverEventText = MESSAGE_DATA.getMessage(MessageStorage.FLAG_UI_CLICK_TOGGLE, 
                ImmutableMap.of( "flag", flagType.name().toLowerCase()));
        if (!toggleType) {
            if (flagValue == Tristate.TRUE) {
                hoverEventText = MessageCache.getInstance().FLAG_UI_CLICK_ALLOW;
            } else if (flagValue == Tristate.FALSE) {
                hoverEventText = MessageCache.getInstance().FLAG_UI_CLICK_DENY;
            } else {
                hoverEventText = MessageCache.getInstance().FLAG_UI_CLICK_REMOVE;
            }
        }
        TextColor flagColor = TextColor.GOLD;
        boolean hasPermission = true;
        if (flagType == FlagType.DEFAULT) {
            flagColor = TextColor.LIGHT_PURPLE;
            if (!src.hasPermission(GDPermissions.MANAGE_FLAG_DEFAULTS)) {
                hoverEventText = MessageCache.getInstance().PERMISSION_FLAG_DEFAULTS;
                hasPermission = false;
            }
        } else if (flagType == FlagType.OVERRIDE) {
            flagColor = TextColor.RED;
            if (!src.hasPermission(GDPermissions.MANAGE_FLAG_OVERRIDES)) {
                hoverEventText = MessageCache.getInstance().PERMISSION_FLAG_OVERRIDES;
                hasPermission = false;
            }
        } else if (flagType == FlagType.INHERIT) {
            flagColor = TextColor.AQUA;
            hoverEventText = MESSAGE_DATA.getMessage(MessageStorage.FLAG_UI_INHERIT_PARENT,
                    ImmutableMap.of("name", claim.getFriendlyNameType()));
            hasPermission = false;
        } else if (src instanceof Player) {
            Component denyReason = claim.allowEdit((Player) src);
            if (denyReason != null) {
                hoverEventText = denyReason;
                hasPermission = false;
            } else {
                // check flag perm
                if (!src.hasPermission(GDPermissions.USER_CLAIM_FLAGS + flagPermission.replace(GDPermissions.FLAG_BASE, ""))) {
                    hoverEventText = MessageCache.getInstance().PERMISSION_FLAG_USE;
                    hasPermission = false;
                }
            }
        }

        if (toggleType) {
            TextComponent.Builder textBuilder = TextComponent.builder("")
                    .append(flagValue.toString().toLowerCase())
                    .hoverEvent(HoverEvent.showText(TextComponent.builder("")
                            .append(hoverEventText)
                            .append("\n")
                            .append(CommandHelper.getFlagTypeHoverText(flagType)).build()));
            if (hasPermission) {
                textBuilder.clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createFlagConsumer(src, claim, contexts, flagPermission, flagValue, displayType, flagType, true))));
            }
            return textBuilder.build();
        }

        TextComponent.Builder textBuilder = TextComponent.builder("")
                .append(flagValue.toString().toLowerCase())
                .hoverEvent(HoverEvent.showText(TextComponent.builder("")
                        .append(hoverEventText)
                        .append("\n")
                        .append(CommandHelper.getFlagTypeHoverText(flagType)).build()));
        if (hasPermission) {
            textBuilder.clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createFlagConsumer(src, claim, contexts, flagPermission, flagValue, displayType, flagType, false))));
        }
        Component result = textBuilder.build();
        if (currentValue == flagValue) {
            final Component whiteOpenBracket = TextComponent.of("[", TextColor.AQUA);
            final Component whiteCloseBracket = TextComponent.of("]", TextColor.AQUA);
            result = TextComponent.builder("")
                    .append(whiteOpenBracket)
                    .append(result.color(flagColor))
                    .append(whiteCloseBracket).build();
        } else {
            result = result.color(TextColor.GRAY);
        }
        return result;
    }

    private Consumer<CommandSender> createClaimFlagConsumer(CommandSender src, GDClaim claim, FlagType flagType) {
        return consumer -> {
            showFlagPermissions(src, claim, flagType);
        };
    }

    private boolean ignoreFlagEntry(Set<Context> contexts) {
        if (contexts == null) {
            return false;
        }

        for (Context context : contexts) {
            if (!context.getKey().startsWith("gd_")) {
                return false;
            }
        }
        return true;
    }
}