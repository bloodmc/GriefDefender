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
package com.griefdefender.provider;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.ContextKeys;
import com.griefdefender.api.permission.PermissionResult;
import com.griefdefender.api.permission.ResultTypes;
import com.griefdefender.api.permission.flag.Flag;
import com.griefdefender.api.permission.option.Option;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.listener.LuckPermsEventHandler;
import com.griefdefender.permission.GDPermissionHolder;
import com.griefdefender.permission.GDPermissionResult;
import com.griefdefender.permission.GDPermissionUser;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.DataMutateResult;
import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.PermissionHolder;
import me.lucko.luckperms.api.User;
import me.lucko.luckperms.api.caching.MetaData;
import me.lucko.luckperms.api.caching.PermissionData;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.api.context.MutableContextSet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FilenameUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class LuckPermsProvider implements PermissionProvider {

    private final Cache<String, Group> groupCache = Caffeine.newBuilder().expireAfterAccess(30, TimeUnit.MINUTES)
            .build();
    private final Cache<String, User> userCache = Caffeine.newBuilder().expireAfterAccess(30, TimeUnit.MINUTES)
            .build();

    public static Comparator<Set<Context>> CONTEXT_COMPARATOR = new Comparator<Set<Context>>() {
        @Override
        public int compare(Set<Context> s1, Set<Context> s2) {
            if (s1.size() > s2.size()) {
                return -1;
            }
            if (s1.size() < s2.size()) {
                return 1;
            }
            return s1.equals(s2) ? 0 : -1;
        }
    };

    private final LuckPermsApi luckPermsApi;

    public LuckPermsProvider() {
        this.luckPermsApi = Bukkit.getServicesManager().getRegistration(LuckPermsApi.class).getProvider();
        new LuckPermsEventHandler(this.luckPermsApi);
    }

    public LuckPermsApi getApi() {
        return this.luckPermsApi;
    }

    @Override
    public boolean hasGroupSubject(String identifier) {
        return this.getGroupSubject(identifier) != null;
    }

    public PermissionHolder getLuckPermsHolder(GDPermissionHolder holder) {
        if (holder.getIdentifier().equalsIgnoreCase("default")) {
            return this.luckPermsApi.getGroup("default");
        }
        if (holder instanceof GDPermissionUser) {
            return this.getLuckPermsUser(holder.getIdentifier());
        }

        return this.getLuckPermsGroup(holder.getIdentifier());
    }

    public User getLuckPermsUser(String identifier) {
        User user = this.userCache.getIfPresent(identifier);
        if (user != null) {
            return user;
        }

        UUID uuid = null;
        if (identifier.length() == 36) {
            try {
                uuid = UUID.fromString(identifier);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        if (uuid != null) {
            user = this.getUserSubject(uuid);
        }

        if (user == null) {
            user = this.luckPermsApi.getUser(identifier);
        }
        if (user != null) {
            this.userCache.put(identifier, user);
        }

        return user;
    }

    public Group getLuckPermsGroup(String identifier) {
        if (identifier.equalsIgnoreCase("default")) {
            return this.luckPermsApi.getGroup("default");
        }
        Group group = this.groupCache.getIfPresent(identifier);
        if (group != null) {
            return group;
        }

        group = this.luckPermsApi.getGroup(identifier);
        if (group != null) {
            this.groupCache.put(identifier, group);
        }
        return group;
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

   /* public GDPermissionUser getUserSubject(String name) {
        final User user = this.luckPermsApi.getUserManager().getUser(name);
        if (user != null) {
            return new GDPermissionUser(user);
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
    }*/

    public UUID lookupUserUniqueId(String name) {
        final User user = this.getLuckPermsUser(name);
        if (user != null) {
            return user.getUuid();
        }
        return null;
    }

    public User getUserSubject(UUID uuid) {
        User user = this.luckPermsApi.getUserManager().getUser(uuid);
        if (user != null) {
            return user;
        }

        try {
            user = this.luckPermsApi.getUserManager().loadUser(uuid).get();
            if (user != null) {
                return user;
            }
        } catch (InterruptedException e) {
            return null;
        } catch (ExecutionException e) {
            return null;
        }
        return null;
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

    public void addActiveContexts(Set<Context> contexts, GDPermissionHolder permissionHolder) {
        addActiveContexts(contexts, permissionHolder, null, null);
    }

    public void addActiveContexts(Set<Context> contexts, GDPermissionHolder permissionHolder, GDPlayerData playerData, Claim claim) {
        if (playerData != null) {
            playerData.ignoreActiveContexts = true;
        }
        final PermissionHolder luckPermsHolder = this.getLuckPermsHolder(permissionHolder);
        if (luckPermsHolder instanceof Group) {
            contexts.addAll(this.getGDContexts(this.luckPermsApi.getContextManager().getStaticContext().mutableCopy()));
            return;
        }

        ImmutableContextSet contextSet = this.luckPermsApi.getContextManager().lookupApplicableContext((User) luckPermsHolder).orElse(null);
        if (contextSet == null) {
            contextSet = this.luckPermsApi.getContextManager().getStaticContext();
        }
        if (contextSet == null) {
            return;
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
        contexts.addAll(this.getGDContexts(activeContexts));
    }

    public void clearPermissions(GDClaim claim) {
        Map<Set<Context>, Map<String, Boolean>> permissionMap = this.getAllPermissions(claim, GriefDefenderPlugin.DEFAULT_HOLDER);
        for (Entry<Set<Context>, Map<String, Boolean>> mapEntry : permissionMap.entrySet()) {
            for (Context context : mapEntry.getKey()) {
                if (context.getKey().equalsIgnoreCase("gd_claim")) {
                    this.clearPermissions(GriefDefenderPlugin.DEFAULT_HOLDER, mapEntry.getKey());
                    break;
                }
            }
        }
    }

    public void clearPermissions(OfflinePlayer player, Context context) {
        final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(player);
        clearPermissions(user, context);
    }

    public void clearPermissions(GDPermissionHolder holder, Context context) {
        clearPermissions(holder, ImmutableSet.of(context));
    }

    public void clearPermissions(GDPermissionHolder holder, Set<Context> contexts) {
        final PermissionHolder permissionHolder = this.getLuckPermsHolder(holder);
        if (permissionHolder == null) {
            return;
        }

        ImmutableContextSet set = ImmutableContextSet.fromEntries(contexts);
        permissionHolder.clearNodes(set);
        this.savePermissionHolder(permissionHolder);
    }

    public boolean holderHasPermission(GDPermissionHolder holder, String permission) {
        Node node = this.luckPermsApi.getNodeFactory().newBuilder(permission).build();
        final PermissionHolder permissionHolder = this.getLuckPermsHolder(holder);
        if (permissionHolder == null) {
            return false;
        }
        return permissionHolder.hasPermission(node) == me.lucko.luckperms.api.Tristate.TRUE;
    }

    public Map<String, Boolean> getPermissions(GDPermissionHolder holder, Set<Context> contexts) {
        ImmutableContextSet set = ImmutableContextSet.fromEntries(contexts);
        Contexts context = Contexts.global().setContexts(set);
        final PermissionHolder permissionHolder = this.getLuckPermsHolder(holder);
        if (permissionHolder == null) {
            return new HashMap<>();
        }
        PermissionData cachedData = permissionHolder.getCachedData().getPermissionData(context);
        return cachedData.getImmutableBacking();
    }

    public Map<String, String> getOptions(GDPermissionHolder holder, Set<Context> contexts) {
        ImmutableContextSet set = ImmutableContextSet.fromEntries(contexts);
        Contexts context = Contexts.global().setContexts(set);
        final PermissionHolder permissionHolder = this.getLuckPermsHolder(holder);
        if (permissionHolder == null) {
            return new HashMap<>();
        }
        MetaData cachedData = permissionHolder.getCachedData().getMetaData(context);
        return cachedData.getMeta();
    }

    public Map<Set<Context>, Map<String, Boolean>> getPermanentPermissions(GDClaim claim, GDPermissionHolder holder) {
        final PermissionHolder permissionHolder = this.getLuckPermsHolder(holder);
        if (permissionHolder == null) {
            return new HashMap<>();
        }

        final ImmutableCollection<Node> nodes = permissionHolder.getNodes().values();
        Map<Set<Context>, Map<String, Boolean>> permanentPermissionMap = new TreeMap<Set<Context>, Map<String, Boolean>>(CONTEXT_COMPARATOR);
        Map<ContextSet, Set<Context>> contextMap = new HashMap<>();
        for (Node node : nodes) {
            if (node.isMeta()) {
                continue;
            }

            String serverName = node.getServer().orElse(null);
            if (serverName != null && serverName.equalsIgnoreCase("global")) {
                serverName = null;
            }
            Set<Context> contexts = null;
            if (contextMap.get(node.getContexts()) == null) {
                contexts = getGPContexts(node.getContexts());
                if (serverName != null && !serverName.equalsIgnoreCase("undefined")) {
                    contexts.add(new Context("server", serverName));
                }
                contextMap.put(node.getContexts(), contexts);
            } else {
                contexts = contextMap.get(node.getContexts());
                if (serverName != null && !serverName.equalsIgnoreCase("undefined")) {
                    contexts.add(new Context("server", serverName));
                }
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
        final PermissionHolder permissionHolder = this.getLuckPermsHolder(holder);
        if (permissionHolder == null) {
            return new HashMap<>();
        }

        final Set<? extends Node> nodes = permissionHolder.getTransientPermissions();
        Map<Set<Context>, Map<String, Boolean>> transientPermissionMap = new TreeMap<Set<Context>, Map<String, Boolean>>(CONTEXT_COMPARATOR);
        Map<ContextSet, Set<Context>> contextMap = new HashMap<>();
        for (Node node : nodes) {
            if (node.isMeta()) {
                continue;
            }

            String serverName = node.getServer().orElse(null);
            if (serverName != null && serverName.equalsIgnoreCase("global")) {
                serverName = null;
            }
            Set<Context> contexts = null;
            if (contextMap.get(node.getContexts()) == null) {
                contexts = getGPContexts(node.getContexts());
                if (serverName != null && !serverName.equalsIgnoreCase("undefined")) {
                    contexts.add(new Context("server", serverName));
                }
                contextMap.put(node.getContexts(), contexts);
            } else {
                contexts = contextMap.get(node.getContexts());
                if (serverName != null && !serverName.equalsIgnoreCase("undefined")) {
                    contexts.add(new Context("server", serverName));
                }
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

    public Map<Set<Context>, Map<String, String>> getPermanentOptions(GDPermissionHolder holder) {
        final PermissionHolder permissionHolder = this.getLuckPermsHolder(holder);
        if (permissionHolder == null) {
            return new HashMap<>();
        }

        final ImmutableCollection<Node> nodes = permissionHolder.getNodes().values();
        Map<Set<Context>, Map<String, String>> permanentPermissionMap = new TreeMap<Set<Context>, Map<String, String>>(CONTEXT_COMPARATOR);
        Map<ContextSet, Set<Context>> contextMap = new HashMap<>();
        for (Node node : nodes) {
            if (!node.isMeta()) {
                continue;
            }
            String serverName = node.getServer().orElse(null);
            if (serverName != null && serverName.equalsIgnoreCase("global")) {
                serverName = null;
            }
            Set<Context> contexts = null;
            if (contextMap.get(node.getContexts()) == null) {
                contexts = getGPContexts(node.getContexts());
                if (serverName != null && !serverName.equalsIgnoreCase("undefined")) {
                    contexts.add(new Context("server", serverName));
                }
                contextMap.put(node.getContexts(), contexts);
            } else {
                contexts = contextMap.get(node.getContexts());
                if (serverName != null && !serverName.equalsIgnoreCase("undefined")) {
                    contexts.add(new Context("server", serverName));
                }
            }
            Map<String, String> metaEntry = permanentPermissionMap.get(contexts);
            if (metaEntry == null) {
                metaEntry = new HashMap<>();
                metaEntry.put(node.getMeta().getKey(), node.getMeta().getValue());
                permanentPermissionMap.put(contexts, metaEntry);
            } else {
                metaEntry.put(node.getMeta().getKey(), node.getMeta().getValue());
            }
        }
        return permanentPermissionMap;
    }

    public Map<Set<Context>, Map<String, String>> getTransientOptions(GDPermissionHolder holder) {
        final PermissionHolder permissionHolder = this.getLuckPermsHolder(holder);
        if (permissionHolder == null) {
            return new HashMap<>();
        }

        final Set<? extends Node> nodes = permissionHolder.getTransientPermissions();
        Map<Set<Context>, Map<String, String>> permanentPermissionMap = new TreeMap<Set<Context>, Map<String, String>>(CONTEXT_COMPARATOR);
        Map<ContextSet, Set<Context>> contextMap = new HashMap<>();
        for (Node node : nodes) {
            if (!node.isMeta()) {
                continue;
            }
            String serverName = node.getServer().orElse(null);
            if (serverName != null && serverName.equalsIgnoreCase("global")) {
                serverName = null;
            }
            Set<Context> contexts = null;
            if (contextMap.get(node.getContexts()) == null) {
                contexts = getGPContexts(node.getContexts());
                if (serverName != null && !serverName.equalsIgnoreCase("undefined")) {
                    contexts.add(new Context("server", serverName));
                }
                contextMap.put(node.getContexts(), contexts);
            } else {
                contexts = contextMap.get(node.getContexts());
                if (serverName != null && !serverName.equalsIgnoreCase("undefined")) {
                    contexts.add(new Context("server", serverName));
                }
            }
            Map<String, String> metaEntry = permanentPermissionMap.get(contexts);
            if (metaEntry == null) {
                metaEntry = new HashMap<>();
                metaEntry.put(node.getMeta().getKey(), node.getMeta().getValue());
                permanentPermissionMap.put(contexts, metaEntry);
            } else {
                metaEntry.put(node.getMeta().getKey(), node.getMeta().getValue());
            }
        }
        return permanentPermissionMap;
    }

    public Map<String, String> getPermanentOptions(GDClaim claim, GDPermissionHolder holder, Set<Context> contexts) {
        final PermissionHolder permissionHolder = this.getLuckPermsHolder(holder);
        if (permissionHolder == null) {
            return new HashMap<>();
        }

        final Set<? extends Node> nodes = permissionHolder.getPermissions();
        final Map<String, String> options = new HashMap<>();
        for (Node node : nodes) {
            if (!node.isMeta()) {
                continue;
            }

            if (contexts == null) {
                options.put(node.getMeta().getKey(), node.getMeta().getValue());
            } else if (getGPContexts(node.getContexts()).containsAll(contexts)) {
                options.put(node.getMeta().getKey(), node.getMeta().getValue());
            }
        }
        return options;
    }

    public Map<String, String> getTransientOptions(GDClaim claim, GDPermissionHolder holder, Set<Context> contexts) {
        final PermissionHolder permissionHolder = this.getLuckPermsHolder(holder);
        if (permissionHolder == null) {
            return new HashMap<>();
        }

        final Set<? extends Node> nodes = permissionHolder.getTransientPermissions();
        final Map<String, String> options = new HashMap<>();
        for (Node node : nodes) {
            if (!node.isMeta()) {
                continue;
            }

            if (contexts == null) {
                options.put(node.getMeta().getKey(), node.getMeta().getValue());
            } else if (getGPContexts(node.getContexts()).containsAll(contexts)) {
                options.put(node.getMeta().getKey(), node.getMeta().getValue());
            }
        }
        return options;
    }

    public Map<Set<Context>, Map<String, Boolean>> getAllPermissions(GDClaim claim, GDPermissionHolder holder) {
        final PermissionHolder permissionHolder = this.getLuckPermsHolder(holder);
        if (permissionHolder == null) {
            return new HashMap<>();
        }

        final Set<? extends Node> nodes = permissionHolder.getAllNodes();
        Map<Set<Context>, Map<String, Boolean>> permissionMap = new HashMap<>();
        Map<ContextSet, Set<Context>> contextMap = new HashMap<>();
        for (Node node : nodes) {
            Set<Context> contexts = null;
            if (contextMap.get(node.getContexts()) == null) {
                contexts = getGPContexts(node.getContexts());
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

    public Set<Context> getGPContexts(ContextSet contextSet) {
        final Set<Context> gpContexts = new HashSet<>();
        for (Map.Entry<String, String> mapEntry : contextSet.toSet()) {
            if (mapEntry.getKey().startsWith("gd_") || mapEntry.getKey().equals("used_item") 
                    || mapEntry.getKey().equals("source") || mapEntry.getKey().equals("target")
                    || mapEntry.getKey().equals("world") || mapEntry.getKey().equals("server")
                    || mapEntry.getKey().equals("state")) {
                if (contextSet.containsKey(ContextKeys.CLAIM) && mapEntry.getKey().equals("server")) {
                    continue;
                }

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
        return getPermissionValue(claim, holder, permission, this.getGDContexts(contexts));
    }

    public Tristate getPermissionValue(GDClaim claim, GDPermissionHolder holder, String permission, Set<Context> contexts) {
        return getPermissionValue(claim, holder, permission, contexts, true);
    }

    public Tristate getPermissionValue(GDClaim claim, GDPermissionHolder holder, String permission, Set<Context> contexts, boolean checkTransient) {
        final Set<Context> activeContexts = new HashSet<>();
        this.addActiveContexts(activeContexts, holder, null, claim);
        contexts.addAll(activeContexts);
        final int contextHash =  Objects.hash(claim, holder, permission, contexts);
        final Cache<Integer, Tristate> cache = PermissionHolderCache.getInstance().getOrCreatePermissionCache(holder);
        Tristate result = cache.getIfPresent(contextHash);
        if (result != null) {
            return result;
        }
        // check persistent permissions first
        Map<Set<Context>, Map<String, Boolean>> permanentPermissions = getPermanentPermissions(claim, holder);
        for (Entry<Set<Context>, Map<String, Boolean>> entry : permanentPermissions.entrySet()) {
            if (entry.getKey().isEmpty()) {
                continue;
            }
            boolean match = true;
            for (Context context : entry.getKey()) {
                if (!contexts.contains(context)) {
                    match = false;
                    break;
                }
            }
            if (match) {
                for (Map.Entry<String, Boolean> permEntry : entry.getValue().entrySet()) {
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

        if (!checkTransient) {
            return Tristate.UNDEFINED;
        }

        // check transient permissions last
        Map<Set<Context>, Map<String, Boolean>> transientPermissions = getTransientPermissions(claim, holder);
        for (Entry<Set<Context>, Map<String, Boolean>> entry : transientPermissions.entrySet()) {
            if (entry.getKey().isEmpty()) {
                continue;
            }
            boolean match = true;
            for (Context context : entry.getKey()) {
                if (!contexts.contains(context)) {
                    match = false;
                    break;
                }
            }
            if (match) {
                for (Map.Entry<String, Boolean> permEntry : entry.getValue().entrySet()) {
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

        cache.put(contextHash, Tristate.UNDEFINED);
        return Tristate.UNDEFINED;
    }

    public Tristate getPermissionValueWithRequiredContexts(GDClaim claim, GDPermissionHolder holder, String permission, Set<Context> contexts, String contextFilter) {
        Map<Set<Context>, Map<String, Boolean>> permanentPermissions = getPermanentPermissions(claim, holder);
        for (Entry<Set<Context>, Map<String, Boolean>> entry : permanentPermissions.entrySet()) {
            if (entry.getKey().isEmpty()) {
                continue;
            }
            boolean match = true;
            for (Context context : entry.getKey()) {
                if (!contexts.contains(context)) {
                    match = false;
                    break;
                }
            }

            // Check for required contexts
            for (Context context : contexts) {
                if (!context.getKey().contains(contextFilter)) {
                    if (!entry.getKey().contains(context)) {
                        match = false;
                        break;
                    }
                }
            }
            if (match) {
                for (Map.Entry<String, Boolean> permEntry : entry.getValue().entrySet()) {
                    if (FilenameUtils.wildcardMatch(permission, permEntry.getKey())) {
                        final Tristate value = Tristate.fromBoolean(permEntry.getValue());
                        return value;
                    }
                }
            }
        }
        return Tristate.UNDEFINED;
    }

    public Tristate getPermissionValue(GDPermissionHolder holder, String permission, Set<Context> contexts) {
        ImmutableContextSet contextSet = ImmutableContextSet.fromEntries(contexts);
        return this.getPermissionValue(holder, permission, contextSet);
    }

    public Tristate getPermissionValue(GDPermissionHolder holder, String permission, ContextSet contexts) {
        Contexts context = Contexts.global().setContexts(contexts);
        final PermissionHolder permissionHolder = this.getLuckPermsHolder(holder);
        if (permissionHolder == null) {
            return Tristate.UNDEFINED;
        }

        PermissionData cachedData = permissionHolder.getCachedData().getPermissionData(context);
        return getGDTristate(cachedData.getPermissionValue(permission));
    }

    // To set options, pass "meta.option".
    @Override
    public String getOptionValue(GDPermissionHolder holder, Option option, Set<Context> contexts) {
        ImmutableContextSet set = ImmutableContextSet.fromEntries(contexts);
        Contexts context = Contexts.global().setContexts(set);
        final PermissionHolder permissionHolder = this.getLuckPermsHolder(holder);
        if (permissionHolder == null) {
            return null;
        }

        MetaData metaData = permissionHolder.getCachedData().getMetaData(context);
        return metaData.getMeta().get(option.getPermission());
    }

    public PermissionResult setOptionValue(GDPermissionHolder holder, String permission, String value, Set<Context> contexts) {
        DataMutateResult result = null;
        ImmutableContextSet set = ImmutableContextSet.fromEntries(contexts);
        Contexts context = Contexts.global().setContexts(set);
        final PermissionHolder permissionHolder = this.getLuckPermsHolder(holder);
        if (permissionHolder == null) {
            return new GDPermissionResult(ResultTypes.FAILURE);
        }

        MetaData metaData = permissionHolder.getCachedData().getMetaData(context);
        for (Map.Entry<String, String> mapEntry : metaData.getMeta().entrySet()) {
            if (mapEntry.getKey().equalsIgnoreCase(permission)) {
                // Always unset existing meta first
                final Node node = this.luckPermsApi.getNodeFactory().makeMetaNode(permission, mapEntry.getValue()).withExtraContext(set).build();
                result = permissionHolder.unsetPermission(node);
            }
        }

        final Node node = this.luckPermsApi.getNodeFactory().makeMetaNode(permission, value).withExtraContext(set).build();
        if (!value.equalsIgnoreCase("undefined")) {
            result = permissionHolder.setPermission(node);
        }
        if (result != null && result.wasSuccess()) {
            this.savePermissionHolder(permissionHolder);
            return new GDPermissionResult(ResultTypes.SUCCESS);
        }
        return new GDPermissionResult(ResultTypes.FAILURE);
    }

    public PermissionResult setPermissionValue(GDPermissionHolder holder, Flag flag, Tristate value, Set<Context> contexts) {
        final boolean result = setPermissionValue(holder, flag.getPermission(), value, contexts);
        if (result) {
            return new GDPermissionResult(ResultTypes.SUCCESS);
        }
        return new GDPermissionResult(ResultTypes.FAILURE);
    }

    public boolean setPermissionValue(GDPermissionHolder holder, String permission, Tristate value, Set<Context> contexts) {
        DataMutateResult result = null;
        ImmutableContextSet set = ImmutableContextSet.fromEntries(contexts);
        final Node node = this.luckPermsApi.getNodeFactory().newBuilder(permission).setValue(value.asBoolean()).withExtraContext(set).build();
        final PermissionHolder permissionHolder = this.getLuckPermsHolder(holder);
        if (permissionHolder == null) {
            return false;
        }

        if (value == Tristate.UNDEFINED) {
            result = permissionHolder.unsetPermission(node);
        } else {
            result = permissionHolder.setPermission(node);
        }

        if (result.wasSuccess()) {
            if (permissionHolder instanceof Group) {
                final Group group = (Group) permissionHolder;
                group.refreshCachedData();
                for (User user :this.luckPermsApi.getUserManager().getLoadedUsers()) {
                    user.refreshCachedData();
                }
                // If a group is changed, we invalidate all cache
                PermissionHolderCache.getInstance().invalidateAllPermissionCache();
            } else {
                // We need to invalidate cache outside of LP listener so we can guarantee proper result returns
                PermissionHolderCache.getInstance().getOrCreatePermissionCache(holder).invalidateAll();
            }

            this.savePermissionHolder(permissionHolder);
        }
        return result.wasSuccess();
    }

    public void setTransientOption(GDPermissionHolder holder, String permission, String value, Set<Context> contexts) {
        MutableContextSet contextSet = MutableContextSet.fromEntries(contexts);
        final PermissionHolder permissionHolder = this.getLuckPermsHolder(holder);
        if (permissionHolder == null) {
            return;
        }

        final Node node = this.luckPermsApi.getNodeFactory().makeMetaNode(permission, value).withExtraContext(contextSet).build();
        permissionHolder.setTransientPermission(node);
    }

    public void setTransientPermission(GDPermissionHolder holder, String permission, Boolean value, Set<Context> contexts) {
        MutableContextSet contextSet = MutableContextSet.fromEntries(contexts);
        final PermissionHolder permissionHolder = this.getLuckPermsHolder(holder);
        if (permissionHolder == null) {
            return;
        }

        final Node node = this.luckPermsApi.getNodeFactory().newBuilder(permission).setValue(value).withExtraContext(contextSet).build();
        permissionHolder.setTransientPermission(node);
    }

    public void savePermissionHolder(PermissionHolder holder) {
        if (holder instanceof User) {
            this.luckPermsApi.getUserManager().saveUser((User) holder);
        } else {
            this.luckPermsApi.getGroupManager().saveGroup((Group) holder);
        }
    }

    public void refreshCachedData(GDPermissionHolder holder) {
        final PermissionHolder permissionHolder = this.getLuckPermsHolder(holder);
        if (permissionHolder == null) {
            return;
        }
        permissionHolder.refreshCachedData();
    }

    public Set<Context> getGDContexts(ContextSet contextSet) {
        final Set<Context> gdContexts = new HashSet<>();
        contextSet.forEach(entry -> {
            gdContexts.add(new Context(entry.getKey(), entry.getValue()));
        });

        return gdContexts;
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
}
