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

import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.permission.Context;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.permission.GDPermissionHolder;
import com.griefdefender.permission.GDPermissionUser;
import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.DataMutateResult;
import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.PermissionHolder;
import me.lucko.luckperms.api.User;
import me.lucko.luckperms.api.caching.PermissionData;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.api.context.MutableContextSet;
import org.apache.commons.io.FilenameUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class PermissionUtil {

    private static PermissionUtil instance;
    private LuckPermsApi luckPermsApi;

    public static PermissionUtil getInstance() {
        return instance;
    }

    static {
        instance = new PermissionUtil();
    }

    public PermissionUtil() {
        this.luckPermsApi = Bukkit.getServicesManager().getRegistration(LuckPermsApi.class).getProvider();
    }

    public boolean hasGroupSubject(String identifier) {
        return this.getGroupSubject(identifier) != null;
    }

    public boolean hasUserSubject(UUID uuid) {
        return this.getUserSubject(uuid) != null;
    }

    public PermissionHolder getPermissionHolder(String identifier) {
        UUID uuid = null;
        try {
            uuid = UUID.fromString(identifier);
        } catch (IllegalArgumentException e) {
            
        }
        return uuid == null ? PermissionUtil.getInstance().getGroupSubject(identifier) : PermissionUtil.getInstance().getUserSubject(identifier);
    }

    public Group getGroupSubject(String identifier) {
        final Group group = this.luckPermsApi.getGroupManager().getGroup(identifier);
        if (group != null) {
            return group;
        }

        try {
            return this.luckPermsApi.getGroupManager().loadGroup(identifier).get().orElse(null);
        } catch (InterruptedException e) {
            return null;
        } catch (ExecutionException e) {
            return null;
        }
    }

    public User getUserSubject(String name) {
        final User user = this.luckPermsApi.getUserManager().getUser(name);
        if (user != null) {
            return user;
        }

        try {
            final UUID uuid = this.luckPermsApi.getUserManager().lookupUuid(name).get();
            if (uuid != null) {
                return this.luckPermsApi.getUserManager().loadUser(uuid).get();
            }
            return null;
        } catch (InterruptedException e) {
            return null;
        } catch (ExecutionException e) {
            return null;
        }
    }

    public User getUserSubject(UUID uuid) {
        final User user = this.luckPermsApi.getUserManager().getUser(uuid);
        if (user != null) {
            return user;
        }

        try {
            return this.luckPermsApi.getUserManager().loadUser(uuid).get();
        } catch (InterruptedException e) {
            return null;
        } catch (ExecutionException e) {
            return null;
        }
    }

    public List<String> getAllLoadedPlayerNames() {
        List<String> subjectList = new ArrayList<>();
        for (User user : this.luckPermsApi.getUserManager().getLoadedUsers()) {
            final String name = user.getName();
            if (name != null) {
                subjectList.add(name);
            }
        }
        if (!subjectList.contains("public")) {
            subjectList.add("public");
        }
        return subjectList;
    } 

    public List<String> getAllLoadedGroupNames() {
        List<String> subjectList = new ArrayList<>();
        for (Group group : this.luckPermsApi.getGroupManager().getLoadedGroups()) {
            final String name = group.getName();
            if (name != null) {
                subjectList.add(name);
            }
        }
        if (!subjectList.contains("public")) {
            subjectList.add("public");
        }
        return subjectList;
    } 

    public MutableContextSet getActiveContexts(GDPermissionHolder permissionHolder) {
        return getActiveContexts(permissionHolder, null, null);
    }

    public MutableContextSet getActiveContexts(GDPermissionHolder permissionHolder, GDPlayerData playerData, Claim claim) {
        if (playerData != null) {
            playerData.ignoreActiveContexts = true;
        }
        final PermissionHolder luckPermsHolder = permissionHolder.getLuckPermsHolder();
        if (luckPermsHolder instanceof Group) {
            return this.luckPermsApi.getContextManager().getStaticContext().mutableCopy();
        }

        ImmutableContextSet contextSet = this.luckPermsApi.getContextManager().lookupApplicableContext((User) luckPermsHolder).orElse(null);
        if (contextSet == null) {
            contextSet = this.luckPermsApi.getContextManager().getStaticContext();
        }
        if (contextSet == null) {
            return MutableContextSet.create();
        }
        MutableContextSet activeContexts = contextSet.mutableCopy();
        if (playerData != null && claim != null) {
            final Claim parent = claim.getParent().orElse(null);
            if (parent != null && claim.getData() != null && claim.getData().doesInheritParent()) {
                activeContexts.remove(parent.getContext().getKey(), parent.getContext().getValue());
            } else {
                activeContexts.remove(claim.getContext().getKey(), claim.getContext().getValue());
            }
        }
        return activeContexts;
    }

    public boolean containsDefaultContext(Set<Context> contexts) {
        for (Context context : contexts) {
            if (context.getKey().equals("gd_claim_default")) {
                return true;
            }
        }

        return false;
    }

    public boolean containsOverrideContext(Set<Context> contexts) {
        for (Context context : contexts) {
            if (context.getKey().equals("gd_claim_override")) {
                return true;
            }
        }

        return false;
    }

    public void clearPermissions(OfflinePlayer player, Context context) {
        final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(player);
        clearPermissions(user, context);
    }

    public void clearPermissions(GDPermissionHolder holder, Context context) {
        clearPermissions(holder, ImmutableSet.of(context));
    }

    public void clearPermissions(GDPermissionHolder holder, Set<Context> contexts) {
        ImmutableContextSet set = ImmutableContextSet.fromEntries(contexts);
        holder.getLuckPermsHolder().clearNodes(set);
        this.savePermissionHolder(holder.getLuckPermsHolder());
    }

    public boolean holderHasPermission(GDPermissionHolder holder, String permission) {
        Node node = GriefDefenderPlugin.getInstance().getLuckPermsProvider().getApi().getNodeFactory().newBuilder(permission).build();
        return holder.getLuckPermsHolder().hasPermission(node) == me.lucko.luckperms.api.Tristate.TRUE;
    }

    public Map<String, Boolean> getPermissions(GDPermissionHolder holder, Set<Context> contexts) {
        ImmutableContextSet set = ImmutableContextSet.fromEntries(contexts);
        Contexts context = Contexts.global().setContexts(set);
        PermissionData cachedData = holder.getLuckPermsHolder().getCachedData().getPermissionData(context);
        return cachedData.getImmutableBacking();
    }

    public Map<Set<Context>, Map<String, Boolean>> getPermanentPermissions(GDClaim claim, GDPermissionHolder holder) {
        final ImmutableCollection<Node> nodes = holder.getLuckPermsHolder().getNodes().values();
        Map<Set<Context>, Map<String, Boolean>> permanentPermissionMap = new HashMap<>();
        Map<ContextSet, Set<Context>> contextMap = new HashMap<>();
        for (Node node : nodes) {
            if (node.isMeta()) {
                continue;
            }
            Set<Context> contexts = null;
            if (contextMap.get(node.getContexts()) == null) {
                contexts = getGPContexts(claim, node.getContexts());
                contextMap.put(node.getContexts(), contexts);
            } else {
                contexts = contextMap.get(node.getContexts());
            }
            Map<String, Boolean> permissionEntry = permanentPermissionMap.get(contexts);
            if (permissionEntry == null) {
                permissionEntry = new HashMap<>();
                permissionEntry.put(node.getPermission(), node.getValue());
                permanentPermissionMap.put(contexts, permissionEntry);
            } else {
                permissionEntry.put(node.getPermission(), node.getValue());
            }
        }
        return permanentPermissionMap;
    }

    public Map<Set<Context>, Map<String, Boolean>> getTransientPermissions(GDClaim claim, GDPermissionHolder holder) {
        final Set<? extends Node> nodes = holder.getLuckPermsHolder().getTransientPermissions();
        Map<Set<Context>, Map<String, Boolean>> transientPermissionMap = new HashMap<>();
        Map<ContextSet, Set<Context>> contextMap = new HashMap<>();
        for (Node node : nodes) {
            Set<Context> contexts = null;
            if (contextMap.get(node.getContexts()) == null) {
                contexts = getGPContexts(claim, node.getContexts());
                contextMap.put(node.getContexts(), contexts);
            } else {
                contexts = contextMap.get(node.getContexts());
            }
            Map<String, Boolean> permissionEntry = transientPermissionMap.get(contexts);
            if (permissionEntry == null) {
                permissionEntry = new HashMap<>();
                permissionEntry.put(node.getPermission(), node.getValue());
                transientPermissionMap.put(contexts, permissionEntry);
            } else {
                permissionEntry.put(node.getPermission(), node.getValue());
            }
        }
        return transientPermissionMap;
    }

    public Map<String, String> getTransientOptions(GDClaim claim, GDPermissionHolder holder, Set<Context> contexts) {
        final Set<? extends Node> nodes = holder.getLuckPermsHolder().getTransientPermissions();
        final Map<String, String> options = new HashMap<>();
        for (Node node : nodes) {
            if (!node.isMeta()) {
                continue;
            }

            if (getGPContexts(claim, node.getContexts()).containsAll(contexts)) {
                options.put(node.getMeta().getKey(), node.getMeta().getValue());
            }
        }
        return options;
    }

    public Map<Set<Context>, Map<String, Boolean>> getAllPermissions(GDClaim claim, GDPermissionHolder holder) {
        final Set<? extends Node> nodes = holder.getLuckPermsHolder().getAllNodes();
        Map<Set<Context>, Map<String, Boolean>> permissionMap = new HashMap<>();
        Map<ContextSet, Set<Context>> contextMap = new HashMap<>();
        for (Node node : nodes) {
            Set<Context> contexts = null;
            if (contextMap.get(node.getContexts()) == null) {
                contexts = getGPContexts(claim, node.getContexts());
                contextMap.put(node.getContexts(), contexts);
            } else {
                contexts = contextMap.get(node.getContexts());
            }
            Map<String, Boolean> permissionEntry = permissionMap.get(contexts);
            if (permissionEntry == null) {
                permissionEntry = new HashMap<>();
                permissionEntry.put(node.getPermission(), node.getValue());
                permissionMap.put(contexts, permissionEntry);
            } else {
                permissionEntry.put(node.getPermission(), node.getValue());
            }
        }
        return permissionMap;
    }

    public Set<Context> getGPContexts(GDClaim claim, ContextSet contextSet) {
        final Set<Context> gpContexts = new HashSet<>();
        for (Map.Entry<String, String> mapEntry : contextSet.toSet()) {
            if (mapEntry.getKey().startsWith("gd_")) {
                gpContexts.add(new Context(mapEntry.getKey(), mapEntry.getValue()));
            }
        }
        return gpContexts;
    }

    public Tristate getPermissionValue(GDPermissionHolder holder, String permission) {
        ImmutableContextSet set = ImmutableContextSet.empty();
        return this.getPermissionValue(holder, permission, set);
    }

    
    public Tristate getPermissionValue(GDClaim claim, GDPermissionHolder holder, String permission, MutableContextSet contexts) {
        final int contextHash =  Objects.hash(claim, holder, permission, contexts);
        final Cache<Integer, Tristate> cache = PermissionHolderCache.getInstance().getOrCreatePermissionCache(holder);
        Tristate result = cache.getIfPresent(contextHash);
        if (result != null) {
            return result;
        }
        // check persistent permissions first
        Map<Set<Context>, Map<String, Boolean>> permanentPermissions = getPermanentPermissions(claim, holder);
        for (Set<Context> entry : permanentPermissions.keySet()) {
            if (entry.isEmpty()) {
                continue;
            }
            boolean match = true;
            for (Context context : entry) {
                if (!contexts.containsKey(context.getKey())) {
                    match = false;
                    break;
                }
            }
            if (match) {
                final Map<String, Boolean> matchPermissions = permanentPermissions.get(entry);
                for (Map.Entry<String, Boolean> permEntry : matchPermissions.entrySet()) {
                    if (FilenameUtils.wildcardMatch(permission, permEntry.getKey())) {
                        final Tristate value = Tristate.fromBoolean(permEntry.getValue());
                        cache.put(contextHash, value);
                        return value;
                    }
                }
                // If we get here, continue on normally
                break;
            }
        }

        /*if (holder != GriefDefenderPlugin.DEFAULT_HOLDER) {
            return getPermissionValue(claim, GriefDefenderPlugin.DEFAULT_HOLDER, permission, contexts);
        }*/
        Tristate value = getPermissionValue(holder, permission, contexts);
        if (value != Tristate.UNDEFINED) {
            cache.put(contextHash, value);
        }
        return value;
    }

    public Tristate getPermissionValue(GDClaim claim, GDPermissionHolder holder, String permission, Set<Context> contexts) {
        final int contextHash =  Objects.hash(claim, holder, permission, contexts);
        final Cache<Integer, Tristate> cache = PermissionHolderCache.getInstance().getOrCreatePermissionCache(holder);
        Tristate result = cache.getIfPresent(contextHash);
        if (result != null) {
            return result;
        }
        // check persistent permissions first
        Map<Set<Context>, Map<String, Boolean>> permanentPermissions = getPermanentPermissions(claim, holder);
        for (Set<Context> entry : permanentPermissions.keySet()) {
            if (entry.isEmpty()) {
                continue;
            }
            boolean match = true;
            for (Context context : entry) {
                if (!contexts.contains(context)) {
                    match = false;
                    break;
                }
            }
            if (match) {
                final Map<String, Boolean> matchPermissions = permanentPermissions.get(entry);
                for (Map.Entry<String, Boolean> permEntry : matchPermissions.entrySet()) {
                    if (FilenameUtils.wildcardMatch(permission, permEntry.getKey())) {
                        final Tristate value = Tristate.fromBoolean(permEntry.getValue());
                        cache.put(contextHash, value);
                        return value;
                    }
                }
                // If we get here, continue on normally
                break;
            }
        }

        Tristate value = getPermissionValue(holder, permission, contexts);
        if (value != Tristate.UNDEFINED) {
            cache.put(contextHash, value);
        }
        return value;
    }

    private Tristate getPermissionValue(GDPermissionHolder holder, String permission, Set<Context> contexts) {
        ImmutableContextSet contextSet = ImmutableContextSet.fromEntries(contexts);
        return this.getPermissionValue(holder, permission, contextSet);
    }

    public Tristate getPermissionValue(GDPermissionHolder holder, String permission, ContextSet contexts) {
        Contexts context = Contexts.global().setContexts(contexts);
        final PermissionHolder luckPermsHolder = holder.getLuckPermsHolder();
        PermissionData cachedData = luckPermsHolder.getCachedData().getPermissionData(context);
        return getGDTristate(cachedData.getPermissionValue(permission));
    }

    // To set options, pass "meta.option".
    public boolean setOptionValue(GDPermissionHolder holder, String permission, String value, Set<Context> contexts) {
        DataMutateResult result = null;
        ImmutableContextSet set = ImmutableContextSet.fromEntries(contexts);
        final Node node = GriefDefenderPlugin.getInstance().getLuckPermsProvider().getApi().getNodeFactory().makeMetaNode(permission, value).withExtraContext(set).build();
        result = holder.getLuckPermsHolder().setPermission(node);
        if (result.wasSuccess()) {
            this.savePermissionHolder(holder.getLuckPermsHolder());
        }
        return result.wasSuccess();
    }

    public boolean setPermissionValue(GDClaim claim, GDPermissionHolder holder, String permission, Tristate value, Set<Context> contexts) {
        DataMutateResult result = null;
        ImmutableContextSet set = ImmutableContextSet.fromEntries(contexts);
        final Node node = GriefDefenderPlugin.getInstance().getLuckPermsProvider().getApi().getNodeFactory().newBuilder(permission).setValue(value.asBoolean()).withExtraContext(set).build();
        if (value == Tristate.UNDEFINED) {
            result = holder.getLuckPermsHolder().unsetPermission(node);
        } else {
            result = holder.getLuckPermsHolder().setPermission(node);
        }

        if (result.wasSuccess()) {
            if (holder.getLuckPermsHolder() instanceof Group) {
                final Group group = (Group) holder.getLuckPermsHolder();
                group.refreshCachedData();
                for (User user :GriefDefenderPlugin.getInstance().getLuckPermsProvider().getApi().getUserManager().getLoadedUsers()) {
                    user.refreshCachedData();
                }
                // If a group is changed, we invalidate all cache
                PermissionHolderCache.getInstance().invalidateAllPermissionCache();
            } else {
                // We need to invalidate cache outside of LP listener so we can guarantee proper result returns
                PermissionHolderCache.getInstance().getOrCreatePermissionCache(holder).invalidateAll();
            }

            this.savePermissionHolder(holder.getLuckPermsHolder());
        }
        return result.wasSuccess();
    }

    public void savePermissionHolder(PermissionHolder holder) {
        if (holder instanceof User) {
            this.luckPermsApi.getUserManager().saveUser((User) holder);
        } else {
            this.luckPermsApi.getGroupManager().saveGroup((Group) holder);
        }
    }

    public Tristate getGDTristate(me.lucko.luckperms.api.Tristate state) {
        if (state == me.lucko.luckperms.api.Tristate.TRUE) {
            return Tristate.TRUE;
        }
        if (state == me.lucko.luckperms.api.Tristate.FALSE) {
            return Tristate.FALSE;
        }
        return Tristate.UNDEFINED;
    }

    public me.lucko.luckperms.api.Tristate getLPTristate(Tristate state) {
        if (state == Tristate.TRUE) {
            return me.lucko.luckperms.api.Tristate.TRUE;
        }
        if (state == Tristate.FALSE) {
            return me.lucko.luckperms.api.Tristate.FALSE;
        }
        return me.lucko.luckperms.api.Tristate.UNDEFINED;
    }

    public Tristate getTristateFromString(String value) {
        Tristate tristate = null;
        int intValue = -999;
        try {
            intValue = Integer.parseInt(value);
            if (intValue <= -1) {
                tristate = Tristate.FALSE;
            } else if (intValue == 0) {
                tristate = Tristate.UNDEFINED;
            } else {
                tristate = Tristate.TRUE;
            }
            return tristate;

        } catch (NumberFormatException e) {
            // ignore
        }

        try {
            tristate = Tristate.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }

        return tristate;
    }
}
