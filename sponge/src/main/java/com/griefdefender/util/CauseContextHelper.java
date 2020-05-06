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

import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimContexts;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.internal.util.NMSUtil;
import com.griefdefender.permission.ContextGroupKeys;
import com.griefdefender.permission.ContextGroups;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;
import net.kyori.text.TextComponent;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CauseContextHelper {

    public static User getEventUser(Event event) {
        final Cause cause = event.getCause();
        final Object source = event.getSource();
        final EventContext context = event.getContext();
        // Don't attempt to set user for leaf decay
        if (context.containsKey(EventContextKeys.LEAVES_DECAY)) {
            return null;
        }

        User user = null;
        User fakePlayer = null;
        if (cause != null) {
            user = cause.first(User.class).orElse(null);
            if (user == null) {
                // check for FakePlayer in context
                user = cause.getContext().get(EventContextKeys.FAKE_PLAYER).orElse(null);
            }
            if (user != null && user instanceof Entity && NMSUtil.getInstance().isFakePlayer((Entity) user)) {
                fakePlayer = user;
            }
        }

        // Only check notifier for fire spread
        if (context.containsKey(EventContextKeys.FIRE_SPREAD)) {
            return context.get(EventContextKeys.NOTIFIER).orElse(null);
        }

        if (user == null || fakePlayer != null) {
            // Always use owner for ticking TE's
            // See issue MinecraftPortCentral/GriefDefender#610 for more information
            if (cause.containsType(TileEntity.class)) {
                user = context.get(EventContextKeys.OWNER)
                        .orElse(context.get(EventContextKeys.NOTIFIER)
                                .orElse(context.get(EventContextKeys.CREATOR)
                                        .orElse(null)));
            } else {
                // Check entity
                final Entity entity = cause.last(Entity.class).orElse(null);
                if (entity != null && entity != fakePlayer) {
                    final UUID creator = entity.getCreator().orElse(null);
                    if (creator != null) {
                        final GDPermissionUser gdUser = PermissionHolderCache.getInstance().getOrCreateUser(creator);
                        return gdUser.getOfflinePlayer();
                    }
                }
                user = context.get(EventContextKeys.NOTIFIER)
                        .orElse(context.get(EventContextKeys.OWNER)
                                .orElse(context.get(EventContextKeys.CREATOR)
                                        .orElse(null)));
            }
        }

        if (user == null) {
            // fall back to fakeplayer if we still don't have a user
            user = fakePlayer;
            if (event instanceof ExplosionEvent) {
                // Check igniter
                final Living living = context.get(EventContextKeys.IGNITER).orElse(null);
                if (living != null && living instanceof User) {
                    user = (User) living;
                }
            }
        }

        return user;
    }

    public static Object getEventFakePlayerSource(Event event) {
        Object source = event.getSource();
        if (source instanceof net.minecraft.entity.Entity && NMSUtil.getInstance().isFakePlayer(source)) {
            final Object actualSource = event.getCause().last(Object.class).orElse(null);
            if (actualSource != source && (actualSource instanceof TileEntity || actualSource instanceof Entity)) {
                return actualSource;
            }
        }
        return source;
    }

    // Credit to digitok of freenode for the regex assistance
    //final String CONTEXT_PATTERN2 = "^contexts?\\[ *(?:[\\w.-]+:[\\w.-]+:[\\w\\/.-]+ *(?:, *(?!\\]$)|(?=\\]$)))+ *\\]$";
    //private static final Pattern CONTEXT_PATTERN = Pattern.compile("^context?\\[ *(?:[\\w.-]+:[\\w.-]+:[\\w\\/.-]+ *(?:, *(?!\\]$)|(?=\\]$)))+ *\\]$"); 

    // original = private static final Pattern CONTEXT_PATTERN = Pattern.compile("^context?\\[ *((?:[\\w.-]+:[\\w.-]+(?::[\\w\\/.-]+)? *(?:, *(?!\\]$)|(?=\\]$)))+) *\\]$");
    private static final Pattern CONTEXT_PATTERN = Pattern.compile("^context?\\[ *((?:[\\w.-]+=[#\\w.-]+(?::[#\\w\\/.-]+)? *(?:, *(?!\\]$)|(?=\\]$)))+) *\\]$");
    private static final Pattern CONTEXT_SPLIT = Pattern.compile("^context?\\[ *((?:[\\w.-]+:[\\w.-]+:[\\w\\/.-]+(?: *, *(?!\\]$)|(?= *\\]$)))+) *\\]$");
    private static final List<String> VALID_CONTEXTS = Arrays.asList("world", "server", "mode", "player", "group", "source", "used_item", "type");
    //  final String regex = "^context?\\[ *((?:[\\w.-]+:[\\w.-]+:[\\w\\/.-]+(?: *, *(?!\\]$)|(?= *\\]$)))+) *\\]$";
    public static Set<Context> generateContexts(String permission, CommandSource src, Claim claim, String context) {
        return generateContexts(permission, src, claim, context, false);
    }

    public static Set<Context> generateContexts(String permission, CommandSource src, Claim claim, String context, boolean isOption) {
        // verify context is valid
        if (context == null) {
            return new HashSet<>();
        }
        context = context.replace(" ", "");
        Matcher matcher = CONTEXT_PATTERN.matcher(context);
        if (!matcher.find()) {
            GriefDefenderPlugin.sendMessage(src, TextComponent.of("Invalid context entered."));
            return null;
        }
        /*if (context.contains("mode=") && !context.contains("type=")) {
            GriefDefenderPlugin.sendMessage(src, Text.of("Context 'mode' requires 'type'."));
            return null;
        }*/

        final boolean canManageDefaults = src.hasPermission(GDPermissions.MANAGE_FLAG_DEFAULTS);
        final boolean canManageOverrides = src.hasPermission(GDPermissions.MANAGE_FLAG_OVERRIDES);
        boolean hasSourceContext = false;
        boolean hasTargetContext = false;
        final Set<Context> contextSet = new HashSet<>();
        final String contexts = matcher.group(1);
        String[] split = contexts.split(",");
        String reason = null;
        for (int i = 0; i < split.length; i++) {
            String[] parts = split[i].split("=");
            //final String[] parts = split[i].split(":");
            if (parts.length < 2) {
                GriefDefenderPlugin.sendMessage(src, TextComponent.of("Invalid context entered."));
                return null;
            }
            final String contextName = parts[0].toLowerCase();
            parts = parts[1].split(":");
            if (parts.length < 1) {
                GriefDefenderPlugin.sendMessage(src, TextComponent.of("Invalid context entered."));
                return null;
            }
            final String arg1 = parts[0];
            final String arg2 = parts.length > 1 ? parts[1] : null;
            String id = "";
            if (arg2 == null) {
                id = "minecraft:" + arg1;
            } else {
                id = arg1 + ":" + arg2;
            }
            if (contextName.equals("world")) {
                boolean found = false;
                for (WorldProperties worldProperties : Sponge.getServer().getAllWorldProperties()) {
                    if (arg1.equalsIgnoreCase(worldProperties.getWorldName())) {
                        contextSet.add(new Context(contextName, arg1));
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    GriefDefenderPlugin.sendMessage(src, TextComponent.of("No world found with name '" + arg1 + "'."));
                    return null;
                }
            } else if (contextName.equals("server")) {
                contextSet.add(new Context(contextName, arg1));
            } else if (contextName.equals("default")) {
                if (!canManageDefaults) {
                    GriefDefenderPlugin.sendMessage(src, MessageCache.getInstance().PERMISSION_FLAG_DEFAULTS);
                    return new HashSet<>();
                }
                if (arg1.equals("any") || arg1.equals("global")) {
                    contextSet.add(ClaimContexts.GLOBAL_DEFAULT_CONTEXT);
                } else if (arg1.equals("admin")) {
                    contextSet.add(ClaimContexts.ADMIN_DEFAULT_CONTEXT);
                } else if (arg1.equals("basic")) {
                    contextSet.add(ClaimContexts.BASIC_DEFAULT_CONTEXT);
                } else if (arg1.equals("subdivision")) {
                    contextSet.add(ClaimContexts.SUBDIVISION_DEFAULT_CONTEXT);
                } else if (arg1.equals("town")) {
                    contextSet.add(ClaimContexts.TOWN_DEFAULT_CONTEXT);
                } else if (arg1.equals("wilderness")) {
                    contextSet.add(ClaimContexts.WILDERNESS_DEFAULT_CONTEXT);
                } else { 
                    GriefDefenderPlugin.sendMessage(src, TextComponent.of(contextName + " context requires format '" + contextName + ":type'. \nValid types are 'world', 'server', and 'global'."));
                    return null;
                }
            } else if (contextName.equals("override")) {
                if (isOption) {
                    GriefDefenderPlugin.sendMessage(src, TextComponent.of("Options do not support overrides."));
                    return null;
                }
                if (!canManageOverrides) {
                    GriefDefenderPlugin.sendMessage(src, MessageCache.getInstance().PERMISSION_FLAG_OVERRIDES);
                    return null;
                }
                if (arg1.equals("any") || arg1.equals("global")) {
                    contextSet.add(ClaimContexts.GLOBAL_OVERRIDE_CONTEXT);
                } else if (arg1.equals("admin")) {
                    contextSet.add(ClaimContexts.ADMIN_OVERRIDE_CONTEXT);
                } else if (arg1.equals("basic")) {
                    contextSet.add(ClaimContexts.BASIC_OVERRIDE_CONTEXT);
                } else if (arg1.equals("subdivision")) {
                    contextSet.add(ClaimContexts.SUBDIVISION_OVERRIDE_CONTEXT);
                } else if (arg1.equals("town")) {
                    contextSet.add(ClaimContexts.TOWN_OVERRIDE_CONTEXT);
                } else if (arg1.equals("wilderness")) {
                    contextSet.add(ClaimContexts.WILDERNESS_OVERRIDE_CONTEXT);
                } else if (arg1.equals("claim")) {
                    contextSet.add(((GDClaim) claim).getOverrideClaimContext());
                } else { 
                    GriefDefenderPlugin.sendMessage(src, TextComponent.of(contextName + " context requires format '" + contextName + ":type'. \nValid types are 'world', 'server', and 'global'."));
                    return new HashSet<>();
                }
            } else if (contextName.equals("player")) {
                contextSet.add(new Context(contextName, arg1));
            } else if (contextName.equals("group")) {
                contextSet.add(new Context(contextName, arg1));
            } else if (contextName.equals("meta")) {
                contextSet.add(new Context(contextName, arg1));
            } else if (contextName.equals("source")) {
                hasSourceContext = true;
                if (!arg1.contains("#") && !arg1.equalsIgnoreCase("any") && !arg1.equalsIgnoreCase("all")) {
                    contextSet.add(new Context(contextName, id));
                } else {
                    contextSet.add(new Context(contextName, arg1));
                }
            } else if (contextName.contentEquals("state")) {
                contextSet.add(new Context(contextName, id));
            } else if (contextName.equals("used_item")) {
                final ItemType type = Sponge.getRegistry().getType(ItemType.class, id).orElse(null); 
                if (type == null) {
                    GriefDefenderPlugin.sendMessage(src, TextComponent.of("Invalid context entered."));
                    return null;
                }
                contextSet.add(new Context(contextName, type.getId()));
            } else {
                if (contextName.equals("target")) {
                    hasTargetContext = true;
                    if (permission.equals(Options.SPAWN_LIMIT.getPermission())
                            && !arg1.contains("#") && !arg1.equalsIgnoreCase("any") && !arg1.equalsIgnoreCase("all")) {
                        contextSet.add(new Context(contextName, id));
                    }
                } else if (arg2 == null) {
                    contextSet.add(new Context(contextName, arg1));
                } else {
                    contextSet.add(new Context(contextName, id));
                }
            }
        }

        if (permission.equals(Options.SPAWN_LIMIT.getPermission())) {
            if (!hasSourceContext) {
                contextSet.add(ContextGroups.SOURCE_ALL);
            }
            if (!hasTargetContext) {
                contextSet.add(ContextGroups.TARGET_ALL);
            }
        }
        return contextSet;
    }
}
