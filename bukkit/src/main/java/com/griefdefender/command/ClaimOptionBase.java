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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.griefdefender.GDPlayerData;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.permission.option.Option;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.event.GDOptionEvent;
import com.griefdefender.permission.GDPermissionHolder;
import com.griefdefender.registry.OptionRegistryModule;

import org.bukkit.command.CommandSender;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ClaimOptionBase {

    public enum OptionType {
        ALL,
        DEFAULT,
        CLAIM,
        //OVERRIDE,
        INHERIT,
        GROUP,
        PLAYER
    }

    protected ClaimSubjectType subjectType;
    protected GDPermissionHolder subject;
    protected String friendlySubjectName;
    protected boolean isAdmin = false;
    protected GDPlayerData sourcePlayerData;
    private final Cache<UUID, OptionType> lastActiveFlagTypeMap = Caffeine.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES)
            .build();

    protected ClaimOptionBase(ClaimSubjectType type) {
        this.subjectType = type;
    }

    /*protected void showOptions(CommandSource src, GPClaim claim, OptionType optionType) {

        boolean isAdmin = false;
        if (src.hasPermission(GPPermissions.DELETE_CLAIM_ADMIN)) {
            isAdmin = true;
        }
        if (src instanceof Player) {
            final Player player = (Player) src;
            final OptionType lastFlagType = this.lastActiveFlagTypeMap.getIfPresent(player.getUniqueId());
            if (lastFlagType != null && lastFlagType != optionType) {
                PaginationUtils.resetActivePage(player.getUniqueId());
            }
        }
        final Text whiteOpenBracket = Text.of(TextColors.AQUA, "[");
        final Text whiteCloseBracket = Text.of(TextColors.AQUA, "]");
        final Text showAllText = Text.of("Click here to show all options for claim.");
        //final Text showOverrideText = Text.of("Click here to filter by ", TextColors.RED, "OVERRIDE ", TextColors.RESET, "permissions.");
        final Text showDefaultText = Text.of("Click here to filter by ", TextColors.LIGHT_PURPLE, "DEFAULT ", TextColors.RESET, "options.");
        final Text showClaimText = Text.of("Click here to filter by ", TextColors.GOLD, "CLAIM ", TextColors.RESET, "options.");
        final Text showInheritText = Text.of("Click here to filter by ", TextColors.AQUA, "INHERIT ", TextColors.RESET, "options.");
        Text allTypeText = Text.EMPTY;
        Text defaultFlagText = Text.EMPTY;
        if (isAdmin) {
            allTypeText = Text.builder()
                    .append(Text.of(optionType == OptionType.ALL ? Text.of(whiteOpenBracket, TextColors.GOLD, "ALL", whiteCloseBracket) : Text.of(TextColors.GRAY, "ALL")))
                    .onClick(TextActions.executeCallback(createClaimOptionConsumer(src, claim, OptionType.ALL)))
                    .onHover(TextActions.showText(showAllText)).build();
            defaultFlagText = Text.builder()
                    .append(Text.of(optionType == OptionType.DEFAULT ? Text.of(whiteOpenBracket, TextColors.LIGHT_PURPLE, "DEFAULT", whiteCloseBracket) : Text.of(TextColors.GRAY, "DEFAULT")))
                    .onClick(TextActions.executeCallback(createClaimOptionConsumer(src, claim, OptionType.DEFAULT)))
                    .onHover(TextActions.showText(showDefaultText)).build();
        }
       // final Text overrideFlagText = Text.builder()
       //         .append(Text.of(flagType == OptionType.OVERRIDE ? Text.of(whiteOpenBracket, TextColors.RED, "OVERRIDE", whiteCloseBracket) : Text.of(TextColors.GRAY, "OVERRIDE")))
       //         .onClick(TextActions.executeCallback(createClaimFlagConsumer(src, claim, OptionType.OVERRIDE)))
       //         .onHover(TextActions.showText(showOverrideText)).build();
        final Text claimFlagText = Text.builder()
                .append(Text.of(optionType == OptionType.CLAIM ? Text.of(whiteOpenBracket, TextColors.YELLOW, "CLAIM", whiteCloseBracket) : Text.of(TextColors.GRAY, "CLAIM")))
                .onClick(TextActions.executeCallback(createClaimOptionConsumer(src, claim, OptionType.CLAIM)))
                .onHover(TextActions.showText(showClaimText)).build();
        final Text inheritFlagText = Text.builder()
                .append(Text.of(optionType == OptionType.INHERIT ? Text.of(whiteOpenBracket, TextColors.AQUA, "INHERIT", whiteCloseBracket) : Text.of(TextColors.GRAY, "INHERIT")))
                .onClick(TextActions.executeCallback(createClaimOptionConsumer(src, claim, OptionType.INHERIT)))
                .onHover(TextActions.showText(showInheritText)).build();
        Text claimFlagHead = Text.of();
        if (this.subjectType == ClaimSubjectType.GLOBAL) {
            if (isAdmin) {
                claimFlagHead = Text.builder().append(Text.of(
                    TextColors.AQUA," Displaying : ", allTypeText, "  ", defaultFlagText, "  ", claimFlagText, "  ", inheritFlagText)).build();
            } else {
                claimFlagHead = Text.builder().append(Text.of(
                        TextColors.AQUA," Displaying : ", claimFlagText, "  ", inheritFlagText)).build();
            }
        } else {
            claimFlagHead = Text.builder().append(Text.of(
                    TextColors.AQUA," ", this.subjectType.getFriendlyName(), " ", TextColors.YELLOW, this.friendlySubjectName, TextColors.AQUA, " : ", allTypeText, "  ", claimFlagText, "  ", inheritFlagText)).build();
        }
        Map<Set<Context>, Map<String, Text>> contextMap = new HashMap<>();
        List<Text> textList = new ArrayList<>();
        Set<Context> contexts = new HashSet<>();
        Set<Context> overrideContexts = new HashSet<>();
        contexts.add(claim.world.getContext());
        if (claim.isAdminClaim()) {
            contexts.add(ClaimContexts.ADMIN_DEFAULT_CONTEXT);
            //overrideContexts.add(ClaimContexts.ADMIN_OVERRIDE_CONTEXT);
            //overrideContexts.add(claim.world.getContext());
        } else if (claim.isBasicClaim() || claim.isSubdivision()) {
            contexts.add(ClaimContexts.BASIC_DEFAULT_CONTEXT);
            //overrideContexts.add(ClaimContexts.BASIC_OVERRIDE_CONTEXT);
            //overrideContexts.add(claim.world.getContext());
        } else if (claim.isTown()) {
            contexts.add(ClaimContexts.TOWN_DEFAULT_CONTEXT);
            //overrideContexts.add(ClaimContexts.TOWN_OVERRIDE_CONTEXT);
            //overrideContexts.add(claim.world.getContext());
        } else {
            contexts.add(ClaimContexts.WILDERNESS_DEFAULT_CONTEXT);
           // overrideContexts.add(ClaimContexts.WILDERNESS_OVERRIDE_CONTEXT);
        }

        Map<Set<Context>, Map<String, String>> defaultOptionMap = new HashMap<>();
        Map<Set<Context>, Map<String, String>> defaultTransientOptionMap = new HashMap<>();
        if (isAdmin) {
           // defaultTransientPermissions = this.subject.getTransientSubjectData().getPermissions(contexts);
            for (Map.Entry<Set<Context>, Map<String, String>> mapEntry : this.subject.getTransientSubjectData().getAllOptions().entrySet()) {
                final Set<Context> contextSet = mapEntry.getKey();
                if (contextSet.contains(claim.getDefaultContext()) { // && contextSet.contains(claim.world.getContext())
                    defaultTransientOptionMap.put(mapEntry.getKey(), mapEntry.getValue());
                }
            }
            for (Map.Entry<Set<Context>, Map<String, String>> mapEntry : this.subject.getSubjectData().getAllOptions().entrySet()) {
                final Set<Context> contextSet = mapEntry.getKey();
                if (contextSet.contains(claim.getDefaultContext()) { // && contextSet.contains(claim.world.getContext())
                    defaultOptionMap.put(mapEntry.getKey(), mapEntry.getValue());
                }
            }
        } else {
            
        }

        Map<Set<Context>, Map<String, String>> overridePermissionMap = new HashMap<>();
       // Map<String, Boolean> claimPermissions = new HashMap<>();
        Map<Set<Context>, Map<String, String>> claimPermissionMap = new HashMap<>();
        for (Map.Entry<Set<Context>, Map<String, String>> mapEntry : this.subject.getSubjectData().getAllOptions().entrySet()) {
            final Set<Context> contextSet = mapEntry.getKey();
            if (contextSet.contains(claim.getContext())) {
                claimPermissionMap.put(mapEntry.getKey(), mapEntry.getValue());
            }
            if (contextSet.contains(claim.getOverrideContext()) { //&& contextSet.contains(claim.world.getContext())
                overridePermissionMap.put(mapEntry.getKey(), mapEntry.getValue());
            }
        }

        Map<Set<Context>, ClaimClickData> inheritPermissionMap = Maps.newHashMap();

        final List<Claim> inheritParents = claim.getInheritedParents();
        Collections.reverse(inheritParents);
        for (Claim current : inheritParents) {
            GPClaim currentClaim = (GPClaim) current;
            for (Map.Entry<Set<Context>, Map<String, String>> mapEntry : this.subject.getSubjectData().getAllOptions().entrySet()) {
                final Set<Context> contextSet = mapEntry.getKey();
                if (contextSet.contains(currentClaim.getContext())) {
                    if (this.subjectType == ClaimSubjectType.GLOBAL) {
                        //claimPermissions.put(mapEntry.getKey(), mapEntry.getValue());
                    } else {
                       // subjectPermissionMap.put(mapEntry.getKey(), mapEntry.getValue());
                    }
                    inheritPermissionMap.put(mapEntry.getKey(), new ClaimClickData(currentClaim, mapEntry.getValue()));
                }
            }
        }

        final Text denyText = claim.allowEdit((Player) src);
        final boolean hasPermission = denyText == null;

        if (optionType == OptionType.ALL) {
            for (Map.Entry<Set<Context>, Map<String, String>> mapEntry : defaultTransientOptionMap.entrySet()) {
                final Set<Context> contextSet = mapEntry.getKey();
                Map<String, Text> textMap = contextMap.get(contextSet);
                if (textMap == null) {
                    textMap = new HashMap<String, Text>();
                    contextMap.put(contextSet, new HashMap<String, Text>());
                }
                for (Map.Entry<String, String> permissionEntry : mapEntry.getValue().entrySet()) {
                    Text flagText = null;
                    String optionPermission = permissionEntry.getKey();
                    String baseFlagPerm = optionPermission.replace(GPPermissions.FLAG_BASE + ".",  "");
                    final ClaimOption baseOption = GPOptionHandler.getOptionFromPermission(optionPermission);
                    if (baseOption == null) {
                        // invalid flag
                        continue;
                    }
                    // check if transient default has been overridden and if so display that value instead
                    String flagValue = permissionEntry.getValue();
                    final Map<String, String> subjectPerms = this.subject.getSubjectData().getOptions(contextSet);
                    final String overriddenValue = subjectPerms.get(optionPermission);
                    if (overriddenValue != null) {
                        flagValue = overriddenValue;
                    }

                    Text baseFlagText = getFlagText(contextSet, optionPermission, baseFlagPerm.toString()); 
                    //Text baseFlagText = Text.builder().append(Text.of(TextColors.GREEN, baseFlagPerm))
                    //        .onHover(TextActions.showText(CommandHelper.getBaseFlagOverlayText(baseFlagPerm))).build();
                    flagText = Text.of(
                            baseFlagText, "  ",
                            TextColors.WHITE, "[",
                            TextColors.LIGHT_PURPLE, getClickableText(src, claim, contextSet, optionPermission, flagValue, OptionType.DEFAULT));

                    final Set<Context> claimContexts = createClaimContextSet(claim, contextSet);
                    final Map<String, String> claimPermissions = claimPermissionMap.get(claimContexts);
                    String claimValue = claimPermissions == null ? null : claimPermissions.get(permissionEntry.getKey());
                    final String claimFinalValue = claimValue == null ? "undefined" : claimValue;
                    //if (claimPermissions == null || claimPermissions.get(permissionEntry.getKey()) == null) {
                        flagText = Text.join(flagText, 
                                Text.of(
                                TextColors.WHITE, ", ",
                                TextColors.GOLD, getClickableText(src, claim, claimContexts, optionPermission, claimFinalValue, OptionType.CLAIM)));
                        if (overridePermissionMap.get(optionPermission) == null) {
                            flagText = Text.join(flagText, Text.of(TextColors.WHITE, "]"));
                        }
                    //}
                    textMap.put(optionPermission, flagText);
                    textList.add(flagText);
                }
            }



            for (Map.Entry<Set<Context>, Map<String, String>> mapEntry : claimPermissionMap.entrySet()) {
                final Set<Context> contextSet = mapEntry.getKey();
                Map<String, Text> textMap = contextMap.get(contextSet);
                if (textMap == null) {
                    textMap = new HashMap<String, Text>();
                    contextMap.put(contextSet, new HashMap<String, Text>());
                }
                for (Map.Entry<String, String> permissionEntry : mapEntry.getValue().entrySet()) {
                    String flagPermission = permissionEntry.getKey();
                    final ClaimOption claimFlag = GPOptionHandler.getOptionFromPermission(flagPermission);
                    if (claimFlag == null) {
                        // invalid flag
                        continue;
                    }
                    final String baseFlag = flagPermission.replace("griefdefender.",  "");
                    if (claimFlag.toString().equalsIgnoreCase(baseFlag)) {
                        // All base flag permissions are handled above in transient logic
                        continue;
                    }

                    String flagValue = permissionEntry.getValue();
                    Text flagText = null;
                    ClaimClickData claimClickData = inheritPermissionMap.get(flagPermission);
                    if (claimClickData != null) {
                        flagText = Text.of(TextColors.AQUA, getClickableText(src, claimClickData.claim, contextSet, flagPermission, Tristate.fromBoolean((boolean) claimClickData.value), OptionType.INHERIT));
                    } else {
                        flagText = Text.of(TextColors.GOLD, getClickableText(src, claim, contextSet, flagPermission, Tristate.fromBoolean(flagValue), OptionType.CLAIM));
                    }

                    Text currentText = textMap.get(flagPermission);
                    boolean customFlag = false;
                    if (currentText == null) {
                        customFlag = true;
                        // custom flag
                        Text baseFlagText = getFlagText(contextSet, flagPermission, baseFlag.toString()); 
                        currentText = Text.builder().append(Text.of(
                                TextColors.GREEN, baseFlagText, "  ",
                                TextColors.WHITE, "[")).build();
                                //.onHover(TextActions.showText(CommandHelper.getBaseFlagOverlayText(baseFlagPerm))).build();
 
                    }
    
                    if (overridePermissionMap.get(flagPermission) == null) {
                        final Text text = Text.join(currentText, Text.of(customFlag ? "" : ", ", flagText, TextColors.WHITE, "]"));
                        textMap.put(flagPermission, text);
                        textList.add(text);
                    } else {
                        final Text text = Text.join(currentText, Text.of(customFlag ? "" : ", ", flagText));
                        textMap.put(flagPermission, text);
                        textList.add(text);
                    }
                }
            }

            for (Map.Entry<Set<Context>, Map<String, Boolean>> mapEntry : overridePermissionMap.entrySet()) {
                final Set<Context> contextSet = mapEntry.getKey();
                Map<String, Text> textMap = contextMap.get(contextSet);
                if (textMap == null) {
                    textMap = new HashMap<String, Text>();
                    contextMap.put(contextSet, new HashMap<String, Text>());
                }
                for (Map.Entry<String, Boolean> permissionEntry : mapEntry.getValue().entrySet()) {
                    String flagPermission = permissionEntry.getKey();
                    Boolean flagValue = permissionEntry.getValue();
                    Text flagText = Text.of(TextColors.RED, getClickableText(src, claim, contextSet, flagPermission, Tristate.fromBoolean(flagValue), OptionType.OVERRIDE));
                    Text currentText = textMap.get(flagPermission);
                    boolean customFlag = false;
                    if (currentText == null) {
                        customFlag = true;
                        // custom flag
                        String baseFlagPerm = flagPermission.replace(GPPermissions.FLAG_BASE + ".",  "");
                        currentText = Text.builder().append(Text.of(
                                TextColors.GREEN, baseFlagPerm, "  ",
                                TextColors.WHITE, "["))
                                .onHover(TextActions.showText(CommandHelper.getBaseFlagOverlayText(baseFlagPerm))).build();
                    }
                    final Text text = Text.join(currentText, Text.of(customFlag ? "" : ", ", flagText, TextColors.WHITE, "]"));
                    textMap.put(flagPermission, text);
                    textList.add(text);
                }
            }
        } else if (optionType == OptionType.CLAIM) {
            for (Map.Entry<Set<Context>, Map<String, Boolean>> mapEntry : defaultTransientOptionMap.entrySet()) {
                final Set<Context> contextSet = mapEntry.getKey();
                Map<String, Text> textMap = contextMap.get(contextSet);
                if (textMap == null) {
                    textMap = new HashMap<String, Text>();
                    contextMap.put(contextSet, new HashMap<String, Text>());
                }
                for (Map.Entry<String, Boolean> permissionEntry : mapEntry.getValue().entrySet()) {
                    Text flagText = null;
                    String flagPermission = permissionEntry.getKey();
                    if (contextMap.containsKey(flagPermission)) {
                        // only display flags not overridden
                        continue;
                    }
    
                    Boolean flagValue = permissionEntry.getValue();
                    String baseFlagPerm = flagPermission.replace(GPPermissions.FLAG_BASE + ".",  "");
                    final ClaimFlag baseFlag = GPPermissionManager.getInstance().getFlagFromPermission(baseFlagPerm);
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
                                flagText = Text.builder().append(
                                        Text.of(TextColors.RED, mapEntry.getValue()))
                                        .onHover(TextActions.showText(Text.of(TextColors.GREEN, baseFlagPerm, TextColors.WHITE, " is currently being ", TextColors.RED, "overridden", TextColors.WHITE, " by an administrator and can ", TextColors.RED, TextStyles.UNDERLINE, "NOT", TextStyles.RESET, TextColors.WHITE, " be changed.")))
                                        .build();
                                break;
                            }
                        }
                    }
                    if (!hasOverride) {
                        // check if transient default has been overridden and if so display that value instead
                        final Set<Context> claimContexts = createClaimContextSet(claim, contextSet);
                        final Map<String, Boolean> subjectPerms = this.subject.getSubjectData().getPermissions(claimContexts);
                        Boolean overriddenValue = subjectPerms.get(flagPermission);
                        if (overriddenValue == null && this.subject != GriefDefenderPlugin.GLOBAL_SUBJECT) {
                            // Check claim
                            final Map<String, Boolean> claimPerms = claimPermissionMap.get(claimContexts);
                            if (claimPerms != null) {
                                overriddenValue = claimPerms.get(flagPermission);
                            }
                        }
    
                        final Tristate currentValue = overriddenValue == null ? Tristate.UNDEFINED : Tristate.fromBoolean(overriddenValue);
                        Text undefinedText = null;
                        if (hasPermission) {
                            undefinedText = Text.builder().append(
                                Text.of(currentValue == Tristate.UNDEFINED ? Text.of(whiteOpenBracket, TextColors.GOLD, "undefined") : Text.of(TextColors.GRAY, "undefined"), TextStyles.RESET, currentValue == Tristate.UNDEFINED ? whiteCloseBracket : ""))
                                .onHover(TextActions.showText(Text.of(TextColors.GREEN, baseFlagPerm, TextColors.WHITE, " is currently not set.\nThe default claim value of ", TextColors.LIGHT_PURPLE, flagValue, TextColors.WHITE, " will be active until set.")))
                                .onClick(TextActions.executeCallback(createFlagConsumer(src, claim, claimContexts, flagPermission, Tristate.UNDEFINED, optionType, OptionType.CLAIM, false))).build();
                        } else {
                            undefinedText = Text.builder().append(
                                    Text.of(currentValue == Tristate.UNDEFINED ? Text.of(whiteOpenBracket, TextColors.GOLD, "undefined") : Text.of(TextColors.GRAY, "undefined"), TextStyles.RESET, currentValue == Tristate.UNDEFINED ? whiteCloseBracket : ""))
                                    .onHover(TextActions.showText(denyText)).build();
                        }

                        final Text trueText = Text.of(TextColors.GRAY, getClickableText(src, claim, claimContexts, flagPermission, currentValue, Tristate.TRUE, optionType, OptionType.CLAIM, false));
                        final Text falseText = Text.of(TextColors.GRAY, getClickableText(src, claim, claimContexts, flagPermission, currentValue, Tristate.FALSE, optionType, OptionType.CLAIM, false));
                        flagText = Text.of(undefinedText, "  ", trueText, "  ", falseText);
                    }
                    Text baseFlagText = getFlagText(contexts, flagPermission, baseFlag.toString()); 
                    flagText = Text.of(
                            baseFlagText, "  ",
                            flagText);
                    textMap.put(flagPermission, flagText);
                    textList.add(flagText);
                }
            }
            for (Map.Entry<Set<Context>, Map<String, Boolean>> mapEntry : claimPermissionMap.entrySet()) {
                final Set<Context> contextSet = mapEntry.getKey();
                Map<String, Text> textMap = contextMap.get(contextSet);
                if (textMap == null) {
                    textMap = new HashMap<String, Text>();
                    contextMap.put(contextSet, new HashMap<String, Text>());
                }
                for (Map.Entry<String, Boolean> permissionEntry : mapEntry.getValue().entrySet()) {
                    String flagPermission = permissionEntry.getKey();
                    final ClaimFlag claimFlag = GPPermissionManager.getInstance().getFlagFromPermission(flagPermission);
                    if (claimFlag == null) {
                        // invalid flag
                        continue;
                    }
                    final String baseFlag = flagPermission.replace(GPPermissions.FLAG_BASE + ".",  "");
                    if (claimFlag.toString().equalsIgnoreCase(baseFlag)) {
                        // All base flag permissions are handled above in transient logic
                        continue;
                    }

                    Boolean flagValue = permissionEntry.getValue();
                    Text flagText = null;
    
                    boolean hasOverride = false;
                    for (Map.Entry<Set<Context>, Map<String, Boolean>> overrideEntry : overridePermissionMap.entrySet()) {
                        final Set<Context> overrideContextSet = overrideEntry.getKey();
                        for (Map.Entry<String, Boolean> overridePermissionEntry : overrideEntry.getValue().entrySet()) {
                            if (flagPermission.contains(overridePermissionEntry.getKey())) {
                                hasOverride = true;
                                Text undefinedText = null;
                                if (hasPermission) {
                                    undefinedText = Text.builder().append(
                                        Text.of(TextColors.GRAY, "undefined"))
                                        .onHover(TextActions.showText(Text.of(TextColors.GREEN, baseFlag, TextColors.WHITE, " is currently being ", TextColors.RED, "overridden", TextColors.WHITE, " by an administrator", TextColors.WHITE, ".\nClick here to remove this flag.")))
                                        .onClick(TextActions.executeCallback(createFlagConsumer(src, claim, overrideContextSet, flagPermission, Tristate.UNDEFINED, optionType, OptionType.CLAIM, false))).build();
                                } else {
                                    undefinedText = Text.builder().append(
                                            Text.of(TextColors.GRAY, "undefined"))
                                            .onHover(TextActions.showText(denyText)).build();
                                }
                                flagText = Text.builder().append(
                                        Text.of(undefinedText, "  ", TextColors.AQUA, "[", TextColors.RED, mapEntry.getValue(), TextStyles.RESET, TextColors.AQUA, "]"))
                                        .onHover(TextActions.showText(Text.of(TextColors.WHITE, "This flag has been overridden by an administrator and can ", TextColors.RED, TextStyles.UNDERLINE, "NOT", TextStyles.RESET, TextColors.WHITE, " be changed.")))
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
                            final Text undefinedText = getClickableText(src, claimClickData.claim, claimClickContexts, flagPermission, currentValue, Tristate.UNDEFINED, optionType, OptionType.INHERIT, false);
                            final Text trueText = getClickableText(src, claimClickData.claim, claimClickContexts, flagPermission, currentValue, Tristate.TRUE, optionType, OptionType.INHERIT, false);
                            final Text falseText = getClickableText(src, claimClickData.claim, claimClickContexts, flagPermission, currentValue, Tristate.FALSE, optionType, OptionType.INHERIT, false);
                            flagText = Text.of(undefinedText, "  ", trueText, "  ", falseText);
                        } else {
                            final Text undefinedText = getClickableText(src, claim, contextSet, flagPermission, currentValue, Tristate.UNDEFINED, optionType, OptionType.CLAIM, false);
                            final Text trueText = getClickableText(src, claim, contextSet, flagPermission, currentValue, Tristate.TRUE, optionType, OptionType.CLAIM, false);
                            final Text falseText = getClickableText(src, claim, contextSet, flagPermission, currentValue, Tristate.FALSE, optionType, OptionType.CLAIM, false);
                            flagText = Text.of(undefinedText, "  ", trueText, "  ", falseText);
                        }
                    }
    
                    Text currentText = textMap.get(flagPermission);
                    if (currentText == null) {
                        currentText = Text.builder().append(Text.of(
                                TextColors.GREEN, baseFlag, "  "))
                                //TextColors.WHITE, "["))
                                .onHover(TextActions.showText(CommandHelper.getBaseFlagOverlayText(baseFlag))).build();
                    }
                    Text baseFlagText = getFlagText(contexts, flagPermission, baseFlag.toString()); 
                    flagText = Text.of(
                            baseFlagText, "  ",
                            flagText);
                    textMap.put(flagPermission, flagText);//Text.join(currentText, Text.of(customFlag ? "" : ", ", flagText)));
                    textList.add(flagText);
                }
            }
        } else if (optionType == OptionType.OVERRIDE) {
            for (Map.Entry<Set<Context>, Map<String, Boolean>> mapEntry : overridePermissionMap.entrySet()) {
                final Set<Context> contextSet = mapEntry.getKey();
                Map<String, Text> textMap = contextMap.get(contextSet);
                if (textMap == null) {
                    textMap = new HashMap<String, Text>();
                    contextMap.put(contextSet, new HashMap<String, Text>());
                }
                for (Map.Entry<String, Boolean> permissionEntry : mapEntry.getValue().entrySet()) {
                    String flagPermission = permissionEntry.getKey();
                    Boolean flagValue = permissionEntry.getValue();
                    Text flagText = Text.of(TextColors.RED, getClickableText(src, claim, contextSet, flagPermission, Tristate.fromBoolean(flagValue), OptionType.OVERRIDE));
                    Text currentText = textMap.get(flagPermission);
                    String baseFlagPerm = flagPermission.replace(GPPermissions.FLAG_BASE + ".",  "");
                    boolean customFlag = false;
                    Text hover = CommandHelper.getBaseFlagOverlayText(baseFlagPerm);
                    if (claim.isWilderness()) {
                        Text reason = GriefDefenderPlugin.getGlobalConfig().getConfig().bans.getReason(baseFlagPerm);
                        if (reason != null && !reason.isEmpty()) {
                            hover = Text.of(TextColors.GREEN, "Ban Reason", TextColors.WHITE, " : ", reason);
                        }
                    }
                    if (currentText == null) {
                        customFlag = true;
                        // custom flag
                        currentText = Text.builder().append(Text.of(
                                TextColors.GREEN, baseFlagPerm, "  ",
                                TextColors.WHITE, "["))
                                .onHover(TextActions.showText(hover)).build();
                    }
                    final Text text = Text.join(currentText, Text.of(customFlag ? "" : ", ", flagText, TextColors.WHITE, "]"));
                    textMap.put(flagPermission, text);
                    textList.add(text);
                }
            }
        } else if (optionType == OptionType.INHERIT) {
            for (Map.Entry<Set<Context>, ClaimClickData> mapEntry : inheritPermissionMap.entrySet()) {
                final Set<Context> contextSet = mapEntry.getKey();
                Map<String, Text> textMap = contextMap.get(contextSet);
                if (textMap == null) {
                    textMap = new HashMap<String, Text>();
                    contextMap.put(contextSet, new HashMap<String, Text>());
                }
                Map<String, Boolean> permissionMap = (Map<String, Boolean>) mapEntry.getValue().value;
                for (Map.Entry<String, Boolean> permissionEntry : permissionMap.entrySet()) {
                    String flagPermission = permissionEntry.getKey();
                    final String baseFlagPerm = flagPermission.replace(GPPermissions.FLAG_BASE + ".",  "");
                    final ClaimClickData claimClickData = mapEntry.getValue();
                    //final boolean flagValue = (boolean) claimClickData.value;
                    Text flagText = null;
                    final ClaimFlag baseFlag = GPPermissionManager.getInstance().getFlagFromPermission(flagPermission);
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
                                final Text undefinedText = Text.builder().append(
                                        Text.of(TextColors.GRAY, "undefined"))
                                        .onHover(TextActions.showText(Text.of(TextColors.GREEN, baseFlagPerm, TextColors.WHITE, " is currently being ", TextColors.RED, "overridden", TextColors.WHITE, " by an administrator", TextColors.WHITE, ".\nClick here to remove this flag.")))
                                        .onClick(TextActions.executeCallback(createFlagConsumer(src, claim, overrideContextSet, flagPermission, Tristate.UNDEFINED, optionType, OptionType.CLAIM, false))).build();
                                flagText = Text.builder().append(
                                        Text.of(undefinedText, "  ", TextColors.AQUA, "[", TextColors.RED, mapEntry.getValue(), TextStyles.RESET, TextColors.AQUA, "]"))
                                        .onHover(TextActions.showText(Text.of(TextColors.WHITE, "This flag has been overridden by an administrator and can ", TextColors.RED, TextStyles.UNDERLINE, "NOT", TextStyles.RESET, TextColors.WHITE, " be changed.")))
                                        .build();
                                break;
                            }
                        }
                    }
    
                    if (!hasOverride) {
                        //flagText = Text.of(TextColors.AQUA, getClickableText(src, claimClickData.claim, flagPermission, Tristate.fromBoolean(flagValue), FlagType.INHERIT));
                    }
    
                    Text currentText = textMap.get(flagPermission);
                    if (currentText == null) {
                        currentText = Text.builder().append(Text.of(
                                TextColors.GREEN, baseFlagPerm, "  "))
                                .onHover(TextActions.showText(CommandHelper.getBaseFlagOverlayText(baseFlagPerm))).build();
                    }
                    Text baseFlagText = getFlagText(contexts, flagPermission, baseFlag.toString()); 
                    flagText = Text.of(
                            baseFlagText, "  ",
                            flagText);
                    textMap.put(flagPermission, flagText);
                    textList.add(flagText);
                }
            }
        } else if (optionType == OptionType.DEFAULT) {
            for (Map.Entry<Set<Context>, Map<String, Boolean>> mapEntry : defaultTransientOptionMap.entrySet()) {
                final Set<Context> contextSet = mapEntry.getKey();
                Map<String, Text> textMap = contextMap.get(contextSet);
                if (textMap == null) {
                    textMap = new HashMap<String, Text>();
                    contextMap.put(contextSet, new HashMap<String, Text>());
                }
                for (Map.Entry<String, Boolean> permissionEntry : mapEntry.getValue().entrySet()) {
                    Text flagText = null;
                    String flagPermission = permissionEntry.getKey();
                    Boolean flagValue = permissionEntry.getValue();
                    String baseFlagPerm = flagPermission.replace(GPPermissions.FLAG_BASE + ".",  "");
                    if (!ClaimFlag.contains(baseFlagPerm)) {
                        continue;
                    }
                    final ClaimFlag baseFlag = ClaimFlag.getEnum(baseFlagPerm);
    
                    // check if transient default has been overridden and if so display that value instead
                    final Map<String, Boolean> subjectPerms = this.subject.getSubjectData().getPermissions(contextSet);
                    Boolean defaultTransientOverrideValue = subjectPerms.get(flagPermission);
                    if (defaultTransientOverrideValue != null) {
                        flagValue = defaultTransientOverrideValue;
                    }
    
                    final Text trueText = Text.of(TextColors.GRAY, getClickableText(src, claim, contextSet, flagPermission, Tristate.fromBoolean(flagValue), Tristate.TRUE, optionType, OptionType.DEFAULT, false));
                    final Text falseText = Text.of(TextColors.GRAY, getClickableText(src, claim, contextSet, flagPermission, Tristate.fromBoolean(flagValue), Tristate.FALSE, optionType, OptionType.DEFAULT, false));
                    flagText = Text.of(trueText, "  ", falseText);
                    Text baseFlagText = getFlagText(contexts, flagPermission, baseFlag.toString()); 
                    flagText = Text.of(
                            baseFlagText, "  ",
                            flagText);
                    textMap.put(flagPermission, flagText);
                    textList.add(flagText);
                }
            }

            // Handle custom defaults
            for (Map.Entry<Set<Context>, Map<String, Boolean>> mapEntry : defaultOptionMap.entrySet()) {
                final Set<Context> contextSet = mapEntry.getKey();
                Map<String, Text> textMap = contextMap.get(contextSet);
                if (textMap == null) {
                    textMap = new HashMap<String, Text>();
                    contextMap.put(contextSet, new HashMap<String, Text>());
                }
                for (Map.Entry<String, Boolean> permissionEntry : mapEntry.getValue().entrySet()) {
                    Text flagText = null;
                    String flagPermission = permissionEntry.getKey();
                    final ClaimFlag claimFlag = GPPermissionManager.getInstance().getFlagFromPermission(flagPermission);
                    if (claimFlag == null) {
                        // invalid flag
                        continue;
                    }
                    final String baseFlag = flagPermission.replace(GPPermissions.FLAG_BASE + ".",  "");
                    if (claimFlag.toString().equalsIgnoreCase(baseFlag)) {
                        // All base flag permissions are handled above in transient logic
                        continue;
                    }

                    Boolean flagValue = permissionEntry.getValue();
    
                    // check if transient default has been overridden and if so display that value instead
                    final Map<String, Boolean> subjectPerms = this.subject.getSubjectData().getPermissions(contextSet);
                    Boolean defaultTransientOverrideValue = subjectPerms.get(flagPermission);
                    if (defaultTransientOverrideValue != null) {
                        flagValue = defaultTransientOverrideValue;
                    }
    
                    final Tristate currentValue = Tristate.fromBoolean(flagValue);
                    final Text trueText = Text.of(TextColors.GRAY, getClickableText(src, claim, contextSet, flagPermission, currentValue, Tristate.TRUE, optionType, OptionType.DEFAULT, false));
                    final Text falseText = Text.of(TextColors.GRAY, getClickableText(src, claim, contextSet, flagPermission, currentValue, Tristate.FALSE, optionType, OptionType.DEFAULT, false));
                    final Text undefinedText = Text.of(TextColors.GRAY, getClickableText(src, claim, contextSet, flagPermission, currentValue, Tristate.UNDEFINED, optionType, OptionType.DEFAULT, false));
                    flagText = Text.of(trueText, "  ", falseText, " ", undefinedText);
                    Text baseFlagText = getFlagText(contexts, flagPermission, baseFlag.toString()); 
                    flagText = Text.of(
                            baseFlagText, "  ",
                            flagText);
                    textMap.put(flagPermission, flagText);
                    textList.add(flagText);
                }
            }
        }

        //List<Text> textList = new ArrayList<>(contextMap.values());
        Collections.sort(textList);
        int fillSize = 20 - (textList.size() + 2);
        for (int i = 0; i < fillSize; i++) {
            textList.add(Text.of(" "));
        }
        PaginationService paginationService = Sponge.getServiceManager().provide(PaginationService.class).get();
        PaginationList.Builder paginationBuilder = paginationService.builder()
                .title(claimFlagHead).padding(Text.of(TextStyles.STRIKETHROUGH,"-")).contents(textList);
        final PaginationList paginationList = paginationBuilder.build();
        Integer activePage = 1;
        if (src instanceof Player) {
            final Player player = (Player) src;
            activePage = PaginationUtils.getActivePage(player.getUniqueId());
            if (activePage == null) {
                activePage = 1;
            }
            this.lastActiveFlagTypeMap.put(player.getUniqueId(), optionType);
        }
        paginationList.sendTo(src, activePage);
    }

    private static Set<Context> createClaimContextSet(GPClaim claim, Set<Context> contexts) {
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

    private static Text getFlagText(Set<Context> contexts, String flagPermission, String baseFlag) {
        //final String flagTarget = GPPermissionHandler.getTargetPermission(flagPermission);
        Text.Builder builder = Text.builder();
        boolean customContext = false;
        for (Context context : contexts) {
            if (context.getKey().contains("gd_claim") || context.getKey().contains("world")) {
                continue;
            }

            customContext = true;
            builder.append(Text.of(TextColors.WHITE, context.getKey() + "=", TextColors.GREEN, context.getValue(),"\n"));
        }
        //builder.append(Text.of("target=", TextColors.GREEN, flagTarget));
        final Text hoverText = builder.build();
        final Text baseFlagText = Text.builder().append(Text.of(customContext ? TextColors.YELLOW : TextColors.GREEN, baseFlag.toString(), " "))
                .onHover(TextActions.showText(customContext ? hoverText : CommandHelper.getBaseFlagOverlayText(baseFlag))).build();
        final Text baseText = Text.builder().append(Text.of(
                baseFlagText)).build();
                //sourceText,
                //targetText)).build();
        return baseText;
        /*String flagSource = null;
        String flagUsedItem = null;
        for (Context context : contexts) {
            if (context.getKey().equalsIgnoreCase("source")) {
                flagSource = context.getValue();
            }
            if (context.getKey().equalsIgnoreCase("used_item")) {
                flagUsedItem = context.getValue();
            }
        }
        Text sourceText = flagSource == null ? null : Text.of(TextColors.WHITE, "source=",TextColors.GREEN, flagSource);
        Text targetText = flagTarget == null ? null : Text.of(TextColors.WHITE, "target=",TextColors.GREEN, flagTarget);
        Text usedItemText = flagUsedItem == null ? null : Text.of(TextColors.WHITE, "used_item=", TextColors.GREEN, flagUsedItem); 
        if (sourceText != null) {
                sourceText = Text.of(sourceText, "\n");
            //}
        } else {
            sourceText = Text.of();
        }
        if (targetText != null) {
            targetText = Text.of(targetText);
        } else {
            targetText = Text.of();
        }
        Text baseFlagText = Text.of();
        if (flagSource == null && flagTarget == null && flagUsedItem == null) {
            baseFlagText = Text.builder().append(Text.of(TextColors.GREEN, baseFlag.toString(), " "))
                .onHover(TextActions.showText(Text.of(sourceText, targetText))).build();
        } else {
            baseFlagText = Text.builder().append(Text.of(TextStyles.ITALIC, TextColors.YELLOW, baseFlag.toString(), " ", TextStyles.RESET))
                    .onHover(TextActions.showText(Text.of(sourceText, usedItemText))).build();
        }
        final Text baseText = Text.builder().append(Text.of(
                baseFlagText)).build();
                //sourceText,
                //targetText)).build();
        return baseText;
        */
    //}

    private Consumer<CommandSender> createFlagConsumer(CommandSender src, GDClaim claim, Set<com.griefdefender.api.permission.Context> contexts, String flagPermission, String flagValue, OptionType displayType, OptionType flagType, boolean toggleType) {
        return consumer -> {
            // Toggle DEFAULT type
            //final String targetId = GPPermissionManager.getInstance().getTargetPermission(flagPermission);
            final Option claimOption = OptionRegistryModule.getInstance().getById(flagPermission).orElse(null);
            if (claimOption == null) {
                return;
            }
            //Context claimContext = claim.getContext();
            //final Set<Context> contexts = new HashSet<>();
            //contexts.add(claimContext);
            GDCauseStackManager.getInstance().pushCause(src);
            GDOptionEvent.Set event = new GDOptionEvent.Set(this.subject, claimOption, flagValue == null ? "undefined" : flagValue, contexts);
            GriefDefender.getEventManager().post(event);
            GDCauseStackManager.getInstance().popCause();
            if (event.cancelled()) {
                return;
            }
            // TODO
            //OptionResult result = CommandHelper.applyFlagPermission(src, this.subject, "ALL", claim, flagPermission, flagValue == null ? "undefined" : flagValue, contexts, flagType, true);
            //if (result.successful()) {
            //    showOptions(src, claim, displayType);
            //}
        };
    }
}