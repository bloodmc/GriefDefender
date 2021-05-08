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

import com.google.common.collect.ImmutableSet;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.ContextKeys;
import com.griefdefender.api.permission.PermissionResult;
import com.griefdefender.api.permission.ResultTypes;
import com.griefdefender.api.permission.flag.FlagData;
import com.griefdefender.api.permission.flag.FlagDefinition;
import com.griefdefender.api.permission.option.Option;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.ClaimContextCalculator;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.listener.LuckPermsEventHandler;
import com.griefdefender.permission.GDPermissionGroup;
import com.griefdefender.permission.GDPermissionHolder;
import com.griefdefender.permission.GDPermissionResult;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.registry.OptionRegistryModule;
import com.griefdefender.util.PermissionUtil;

import net.kyori.text.TextComponent;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.context.MutableContextSet;
import net.luckperms.api.model.PermissionHolder;
import net.luckperms.api.model.PermissionHolder.Identifier;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.MetaNode;
import net.luckperms.api.node.types.PermissionNode;
import net.luckperms.api.query.QueryMode;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.query.dataorder.DataQueryOrder;
import net.luckperms.api.query.dataorder.DataQueryOrderFunction;
import net.luckperms.api.query.dataorder.DataTypeFilter;
import net.luckperms.api.query.dataorder.DataTypeFilterFunction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.service.ProviderRegistration;

public class LuckPermsProvider implements PermissionProvider {

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

    private final LuckPerms luckPermsApi;
    private final static DefaultDataQueryOrderFunction DEFAULT_DATA_QUERY_ORDER = new DefaultDataQueryOrderFunction();
    private final static DefaultPersistentOnlyDataFilter DEFAULT_PERSISTENT_ONLY = new DefaultPersistentOnlyDataFilter();
    private final static DefaultTransientOnlyDataFilter DEFAULT_TRANSIENT_ONLY = new DefaultTransientOnlyDataFilter();
    private final static UserPersistentOnlyDataFilter USER_PERSISTENT_ONLY = new UserPersistentOnlyDataFilter();

    public LuckPermsProvider() {
        final ProviderRegistration<LuckPerms> service = Sponge.getServiceManager().getRegistration(LuckPerms.class).orElse(null);
        this.luckPermsApi = service.getProvider();
        new LuckPermsEventHandler(this.luckPermsApi);
    }

    public LuckPerms getApi() {
        return this.luckPermsApi;
    }

    @Override
    public String getServerName() {
        return this.luckPermsApi.getServerName();
    }

    @Override
    public boolean hasGroupSubject(String identifier) {
        return this.getGroupSubject(identifier) != null;
    }

    public PermissionHolder getLuckPermsHolder(GDPermissionHolder holder) {
        if (holder instanceof GDPermissionUser) {
            return this.getLuckPermsUser(holder.getIdentifier());
        }

        return this.getLuckPermsGroup(holder.getIdentifier());
    }

    public User getLuckPermsUser(String identifier) {
        User user = null;
        UUID uuid = null;
        if (identifier.length() == 36) {
            try {
                uuid = UUID.fromString(identifier);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        } else {
            user = this.luckPermsApi.getUserManager().getUser(identifier);
            if (user == null) {
                try {
                    uuid = this.luckPermsApi.getUserManager().lookupUniqueId(identifier).get();
                } catch (Throwable t) {
                    // ignore
                }
            }
        }
        if (uuid != null) {
            user = this.getUserSubject(uuid);
        }

        return user;
    }

    public boolean createDefaultGroup(String identifier) {
        Group  group = this.luckPermsApi.getGroupManager().getGroup(identifier);
        if (group == null) {
            try {
                GriefDefenderPlugin.getInstance().getLogger().info("Creating group '" + identifier + "' in LuckPerms...");
                final String dataType = identifier.replaceAll("griefdefender_", "");
                group = this.luckPermsApi.getGroupManager().createAndLoadGroup(identifier).get();
                GriefDefenderPlugin.getInstance().getLogger().info("Group created successfully.");
                if (group != null) {
                    final Group defaultGroup = this.luckPermsApi.getGroupManager().getGroup("default");
                    if (defaultGroup != null) {
                        GriefDefenderPlugin.getInstance().getLogger().info("Migrating legacy " + dataType + " permissions from 'default' to '" + identifier + "'...");
                        final Node node = this.luckPermsApi.getNodeBuilderRegistry().forInheritance().group(group).build();
                        // move all existing GD permissions to new group
                        List<Node> nodesToRemove = new ArrayList<>();
                        for (Node permNode : defaultGroup.data().toCollection()) {
                            if (!permNode.getKey().contains("griefdefender")) {
                                continue;
                            }
                            if (permNode.getType() == NodeType.META) {
                                if (permNode.getKey().contains("griefdefender") && identifier.equals(GriefDefenderPlugin.GD_OPTION_GROUP_NAME)) {
                                    GriefDefenderPlugin.getInstance().getLogger().info("Found legacy option node [Key: " + permNode.getKey() + ", Value: " + permNode.getValue() + ", Contexts: " + permNode.getContexts() + "].\nAdding option node to group '" + identifier + "'...");
                                    group.data().add(permNode);
                                    nodesToRemove.add(permNode);
                                }
                                continue;
                            }
                            if (permNode.getType() != NodeType.PERMISSION) {
                                continue;
                            }
                            if (identifier.equals(GriefDefenderPlugin.GD_OVERRIDE_GROUP_NAME)) {
                                if (permNode.getContexts().containsKey(ContextKeys.CLAIM_OVERRIDE)) {
                                    GriefDefenderPlugin.getInstance().getLogger().info("Found legacy override permission node [Key: " + permNode.getKey() + ", Value: " + permNode.getValue() + ", Contexts: " + permNode.getContexts() + "].\nAdding permission node to group '" + identifier + "'...");
                                    group.data().add(permNode);
                                    nodesToRemove.add(permNode);
                                }
                                continue;
                            } else if (identifier.equals(GriefDefenderPlugin.GD_CLAIM_GROUP_NAME)) {
                                if (permNode.getContexts().containsKey(ContextKeys.CLAIM)) {
                                    GriefDefenderPlugin.getInstance().getLogger().info("Found legacy claim permission node [Key: " + permNode.getKey() + ", Value: " + permNode.getValue() + ", Contexts: " + permNode.getContexts() + "].\nAdding permission node to group '" + identifier + "'...");
                                    group.data().add(permNode);
                                    nodesToRemove.add(permNode);
                                }
                                continue;
                            } else if (identifier.equals(GriefDefenderPlugin.GD_DEFAULT_GROUP_NAME)) {
                                if (!permNode.getContexts().isEmpty()) {
                                    GriefDefenderPlugin.getInstance().getLogger().info("Found legacy default permission node [Key: " + permNode.getKey() + ", Value: " + permNode.getValue() + ", Contexts: " + permNode.getContexts() + "].\nAdding permission node to group '" + identifier + "'...");
                                    group.data().add(permNode);
                                    nodesToRemove.add(permNode);
                                }
                            }
                        }
                        if (!nodesToRemove.isEmpty()) {
                            GriefDefenderPlugin.getInstance().getLogger().info("Removing legacy permission nodes from 'default' group...");
                        }
                        for (Node rem : nodesToRemove) {
                            defaultGroup.data().remove(rem);
                        }
                        if (!nodesToRemove.isEmpty()) {
                            GriefDefenderPlugin.getInstance().getLogger().info("Cleanup complete.");
                        }
                        final DataMutateResult result = defaultGroup.data().add(node);
                        GriefDefenderPlugin.getInstance().getLogger().info("Saving permission changes to 'default'...");
                        this.luckPermsApi.getGroupManager().saveGroup(defaultGroup).get();
                        GriefDefenderPlugin.getInstance().getLogger().info("Saving permission changes to '" + identifier + "'...");
                        this.luckPermsApi.getGroupManager().saveGroup(group).get();
                        GriefDefenderPlugin.getInstance().getLogger().info("Migration to group '" + identifier + "' complete.");
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return group != null;
    }

    public Group getLuckPermsGroup(String identifier) {
        return this.luckPermsApi.getGroupManager().getGroup(identifier);
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

    public UUID lookupUserUniqueId(String name) {
        final User user = this.getLuckPermsUser(name);
        if (user != null) {
            return user.getUniqueId();
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
            final String name = user.getUsername();
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

        ImmutableContextSet contextSet = this.luckPermsApi.getContextManager().getContext((User) luckPermsHolder).orElse(null);
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
        // check default holder
        this.clearPermission(claim.getUniqueId(), GriefDefenderPlugin.GD_CLAIM_HOLDER);
        // check loaded groups
        for (Group group : this.luckPermsApi.getGroupManager().getLoadedGroups()) {
            if (group.getName().equalsIgnoreCase(GriefDefenderPlugin.DEFAULT_GROUP_NAME) || group.getName().startsWith("griefdefender_")) {
                continue;
            }
            final GDPermissionHolder holder = PermissionHolderCache.getInstance().getOrCreateGroup(group.getName());
            if (holder == null) {
                continue;
            }

            this.clearPermission(claim.getUniqueId(), holder);
        }
        // check user trusts
        for (UUID uuid : claim.getUserTrusts()) {
            if (uuid.equals(GriefDefenderPlugin.PUBLIC_UUID) || uuid.equals(GriefDefenderPlugin.WORLD_USER_UUID) || uuid.equals(GriefDefenderPlugin.ADMIN_USER_UUID)) {
                continue;
            }
            final GDPermissionHolder holder = PermissionHolderCache.getInstance().getOrCreateUser(uuid);
            if (holder == null) {
                continue;
            }
            this.clearPermission(claim.getUniqueId(), holder);
        }
    }

    private void clearPermission(UUID claimUniqueId, GDPermissionHolder holder) {
        Map<Set<Context>, Map<String, Boolean>> permissionMap = this.getPermanentPermissions(holder);
        for (Entry<Set<Context>, Map<String, Boolean>> mapEntry : permissionMap.entrySet()) {
            for (Context context : mapEntry.getKey()) {
                if (context.getKey().equalsIgnoreCase("gd_claim") && context.getValue().equalsIgnoreCase(claimUniqueId.toString())) {
                    this.clearPermissions(holder, mapEntry.getKey());
                    break;
                }
            }
        }
        Map<Set<Context>, Map<String, List<String>>> optionMap = this.getPermanentOptions(holder);
        for (Entry<Set<Context>, Map<String, List<String>>> mapEntry : optionMap.entrySet()) {
            for (Context context : mapEntry.getKey()) {
                if (context.getKey().equalsIgnoreCase("gd_claim") && context.getValue().equalsIgnoreCase(claimUniqueId.toString())) {
                    this.clearPermissions(holder, mapEntry.getKey());
                    break;
                }
            }
        }
    }

    public void clearPermissions(GDPermissionHolder holder, Context context) {
        clearPermissions(holder, ImmutableSet.of(context));
    }

    public void clearPermissions(GDPermissionHolder holder, Set<Context> contexts) {
        final PermissionHolder permissionHolder = this.getLuckPermsHolder(holder);
        if (permissionHolder == null) {
            return;
        }

        ImmutableContextSet set = this.getLPContexts(contexts).immutableCopy();
        permissionHolder.data().clear(set);
        this.savePermissionHolder(permissionHolder);
    }

    public boolean holderHasPermission(GDPermissionHolder holder, String permission) {
        final PermissionHolder permissionHolder = this.getLuckPermsHolder(holder);
        if (permissionHolder == null) {
            return false;
        }

        final QueryOptions query = QueryOptions.builder(QueryMode.CONTEXTUAL).option(DataQueryOrderFunction.KEY, DEFAULT_DATA_QUERY_ORDER).build();
        return permissionHolder.getCachedData().getPermissionData(query).checkPermission(permission).asBoolean();
    }

    public Map<String, Boolean> getPermissions(GDPermissionHolder holder, Set<Context> contexts) {
        ImmutableContextSet set = this.getLPContexts(contexts).immutableCopy();
        final PermissionHolder permissionHolder = this.getLuckPermsHolder(holder);
        if (permissionHolder == null) {
            return new HashMap<>();
        }

        final QueryOptions query = QueryOptions.builder(QueryMode.CONTEXTUAL).option(DataQueryOrderFunction.KEY, DEFAULT_DATA_QUERY_ORDER).context(set).build();
        CachedPermissionData cachedData = permissionHolder.getCachedData().getPermissionData(query);
        return cachedData.getPermissionMap();
    }

    public Map<String, List<String>> getOptions(GDPermissionHolder holder, Set<Context> contexts) {
        ImmutableContextSet set = this.getLPContexts(contexts).immutableCopy();
        final PermissionHolder permissionHolder = this.getLuckPermsHolder(holder);
        if (permissionHolder == null) {
            return new HashMap<>();
        }

        final QueryOptions query = QueryOptions.builder(QueryMode.CONTEXTUAL).option(DataQueryOrderFunction.KEY, DEFAULT_DATA_QUERY_ORDER).context(set).build();
        CachedMetaData cachedData = permissionHolder.getCachedData().getMetaData(query);
        // TODO
        Map<String, List<String>> metaMap = new HashMap<>();
        for (Map.Entry<String, List<String>> mapEntry : cachedData.getMeta().entrySet()) {
            metaMap.put(mapEntry.getKey(), mapEntry.getValue());
        }
        return metaMap;
    }

    public Map<Set<Context>, Map<String, Boolean>> getPermanentPermissions(GDPermissionHolder holder) {
        final PermissionHolder permissionHolder = this.getLuckPermsHolder(holder);
        if (permissionHolder == null) {
            return new HashMap<>();
        }

        final Collection<Node> nodes = permissionHolder.data().toCollection();
        Map<Set<Context>, Map<String, Boolean>> permanentPermissionMap = new TreeMap<Set<Context>, Map<String, Boolean>>(CONTEXT_COMPARATOR);
        for (Node node : nodes) {
            if (node.getType() != NodeType.PERMISSION) {
                continue;
            }

            final PermissionNode permissionNode = (PermissionNode) node;
            final Set<Context> contexts = getGPContexts(node.getContexts());
            Map<String, Boolean> permissionEntry = permanentPermissionMap.get(contexts);
            if (permissionEntry == null) {
                permissionEntry = new HashMap<>();
                permissionEntry.put(permissionNode.getPermission(), node.getValue());
                permanentPermissionMap.put(contexts, permissionEntry);
            } else {
                permissionEntry.put(permissionNode.getPermission(), node.getValue());
            }
        }

        return permanentPermissionMap;
    }

    public Map<Set<Context>, Map<String, Boolean>> getAllPermanentPermissions() {
        Map<Set<Context>, Map<String, Boolean>> permanentPermissionMap = new TreeMap<Set<Context>, Map<String, Boolean>>(CONTEXT_COMPARATOR);
        this.addAllPermanentPermissions(this.getLuckPermsHolder(GriefDefenderPlugin.GD_CLAIM_HOLDER), permanentPermissionMap);
        this.addAllPermanentPermissions(this.getLuckPermsHolder(GriefDefenderPlugin.GD_DEFAULT_HOLDER), permanentPermissionMap);
        this.addAllPermanentPermissions(this.getLuckPermsHolder(GriefDefenderPlugin.GD_DEFINITION_HOLDER), permanentPermissionMap);
        this.addAllPermanentPermissions(this.getLuckPermsHolder(GriefDefenderPlugin.GD_OPTION_HOLDER), permanentPermissionMap);
        this.addAllPermanentPermissions(this.getLuckPermsHolder(GriefDefenderPlugin.GD_OVERRIDE_HOLDER), permanentPermissionMap);
        return permanentPermissionMap;
    }

    private Map<Set<Context>, Map<String, Boolean>> addAllPermanentPermissions(PermissionHolder holder, Map<Set<Context>, Map<String, Boolean>> permanentPermissionMap) {
        final Collection<Node> nodes = holder.data().toCollection();
        for (Node node : nodes) {
            if (node.getType() != NodeType.PERMISSION) {
                continue;
            }

            final PermissionNode permissionNode = (PermissionNode) node;
            final Set<Context> contexts = getGPContexts(node.getContexts());
            Map<String, Boolean> permissionEntry = permanentPermissionMap.get(contexts);
            if (permissionEntry == null) {
                permissionEntry = new HashMap<>();
                permissionEntry.put(permissionNode.getPermission(), node.getValue());
                permanentPermissionMap.put(contexts, permissionEntry);
            } else {
                permissionEntry.put(permissionNode.getPermission(), node.getValue());
            }
        }
        return permanentPermissionMap;
    }

    public Map<Set<Context>, Map<String, Boolean>> getTransientPermissions(GDPermissionHolder holder) {
        final PermissionHolder permissionHolder = this.getLuckPermsHolder(holder);
        if (permissionHolder == null) {
            return new HashMap<>();
        }

        final Collection<Node> nodes = permissionHolder.transientData().toCollection();
        Map<Set<Context>, Map<String, Boolean>> transientPermissionMap = new TreeMap<Set<Context>, Map<String, Boolean>>(CONTEXT_COMPARATOR);
        for (Node node : nodes) {
            if (node.getType() != NodeType.PERMISSION) {
                continue;
            }

            final PermissionNode permissionNode = (PermissionNode) node;
            final Set<Context> contexts = getGPContexts(node.getContexts());
            Map<String, Boolean> permissionEntry = transientPermissionMap.get(contexts);
            if (permissionEntry == null) {
                permissionEntry = new HashMap<>();
                permissionEntry.put(permissionNode.getPermission(), node.getValue());
                transientPermissionMap.put(contexts, permissionEntry);
            } else {
                permissionEntry.put(permissionNode.getPermission(), node.getValue());
            }
        }
        return transientPermissionMap;
    }

    public Map<Set<Context>, Map<String, List<String>>> getPermanentOptions(GDPermissionHolder holder) {
        final PermissionHolder permissionHolder = this.getLuckPermsHolder(holder);
        if (permissionHolder == null) {
            return new HashMap<>();
        }

        final Collection<Node> nodes = permissionHolder.data().toCollection();
        Map<Set<Context>, Map<String, List<String>>> permanentPermissionMap = new TreeMap<Set<Context>, Map<String, List<String>>>(CONTEXT_COMPARATOR);
        for (Node node : nodes) {
            if (node.getType() != NodeType.META) {
                continue;
            }

            final MetaNode metaNode = (MetaNode) node;
            final Set<Context> contexts = getGPContexts(node.getContexts());
            Map<String, List<String>> metaEntry = permanentPermissionMap.get(contexts);
            if (metaEntry == null) {
                metaEntry = new HashMap<>();
                final List<String> values = new ArrayList<>();
                values.add(metaNode.getMetaValue());
                metaEntry.put(metaNode.getMetaKey(), values);
                permanentPermissionMap.put(contexts, metaEntry);
            } else {
                List<String> values = metaEntry.get(metaNode.getMetaKey());
                if (values == null) {
                    values = new ArrayList<>();
                    values.add(metaNode.getMetaValue());
                    metaEntry.put(metaNode.getMetaKey(), values);
                } else {
                    values.add(metaNode.getMetaValue());
                }
            }
        }
        return permanentPermissionMap;
    }

    public Map<Set<Context>, Map<String, List<String>>> getTransientOptions(GDPermissionHolder holder) {
        final PermissionHolder permissionHolder = this.getLuckPermsHolder(holder);
        if (permissionHolder == null) {
            return new HashMap<>();
        }

        final Collection<Node> nodes = permissionHolder.transientData().toCollection();
        Map<Set<Context>, Map<String, List<String>>> permanentPermissionMap = new TreeMap<Set<Context>, Map<String, List<String>>>(CONTEXT_COMPARATOR);
        for (Node node : nodes) {
            if (node.getType() != NodeType.META) {
                continue;
            }

            final MetaNode metaNode = (MetaNode) node;
            final Set<Context> contexts = getGPContexts(node.getContexts());
            Map<String, List<String>> metaEntry = permanentPermissionMap.get(contexts);
            if (metaEntry == null) {
                metaEntry = new HashMap<>();
                final List<String> values = new ArrayList<>();
                values.add(metaNode.getMetaValue());
                metaEntry.put(metaNode.getMetaKey(), values);
                permanentPermissionMap.put(contexts, metaEntry);
            } else {
                List<String> values = metaEntry.get(metaNode.getMetaKey());
                if (values == null) {
                    values = new ArrayList<>();
                    values.add(metaNode.getMetaValue());
                    metaEntry.put(metaNode.getMetaKey(), values);
                } else {
                    values.add(metaNode.getMetaValue());
                }
            }
        }
        return permanentPermissionMap;
    }

    public Map<Set<Context>, Map<String, Boolean>> getAllPermissions(GDPermissionHolder holder) {
        final PermissionHolder permissionHolder = this.getLuckPermsHolder(holder);
        if (permissionHolder == null) {
            return new HashMap<>();
        }

        final Collection<Node> nodes = permissionHolder.getNodes();
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
                permissionEntry.put(node.getKey(), node.getValue());
                permissionMap.put(contexts, permissionEntry);
            } else {
                permissionEntry.put(node.getKey(), node.getValue());
            }
        }
        return permissionMap;
    }

    public Set<Context> getGPContexts(ContextSet contextSet) {
        final Set<Context> gpContexts = new HashSet<>();
        for (net.luckperms.api.context.Context context : contextSet.toSet()) {
            gpContexts.add(new Context(context.getKey(), context.getValue()));
        }
        return gpContexts;
    }

    @Override
    public Tristate getPermissionValue(GDPermissionHolder holder, String permission, Set<Context> contexts) {
        return this.getPermissionValue(holder, permission, contexts, PermissionDataType.PERSISTENT);
    }

    @Override
    public Tristate getPermissionValue(GDClaim claim, GDPermissionHolder holder, String permission, Set<Context> contexts) {
        return this.getPermissionValue(holder, permission, contexts);
    }

    @Override
    public Tristate getPermissionValue(GDClaim claim, GDPermissionHolder holder, String permission, Set<Context> contexts, PermissionDataType type) {
        return this.getPermissionValue(holder, permission, contexts, type);
    }

    public Tristate getPermissionValue(GDPermissionHolder holder, String permission) {
        final Set<Context> contexts = new HashSet<>();
        this.checkServerContext(contexts);
        ImmutableContextSet set = this.getLPContexts(contexts).immutableCopy();
        return this.getPermissionValue(holder, permission, set);
    }

    public Tristate getPermissionValue(GDPermissionHolder holder, String permission, MutableContextSet contexts) {
        return this.getPermissionValue(holder, permission, this.getGDContexts(contexts));
    }

    public Tristate getPermissionValue(GDPermissionHolder holder, String permission, Set<Context> contexts, PermissionDataType type) {
        this.checkServerContext(contexts);
        ImmutableContextSet contextSet = this.getLPContexts(contexts).immutableCopy();
        return this.getPermissionValue(holder, permission, contextSet, type);
    }

    public Tristate getPermissionValue(GDPermissionHolder holder, String permission, ContextSet contexts) {
        return this.getPermissionValue(holder, permission, contexts, PermissionDataType.PERSISTENT);
    }

    public Tristate getPermissionValue(GDPermissionHolder holder, String permission, ContextSet contexts, PermissionDataType type) {
        final PermissionHolder permissionHolder = type == PermissionDataType.TRANSIENT ? this.getLuckPermsHolder(GriefDefenderPlugin.GD_DEFAULT_HOLDER) : this.getLuckPermsHolder(holder);
        if (permissionHolder == null) {
            return Tristate.UNDEFINED;
        }

        QueryOptions query = null;
        if (type == PermissionDataType.TRANSIENT) {
            query = QueryOptions.builder(QueryMode.CONTEXTUAL).option(DataQueryOrderFunction.KEY, DEFAULT_DATA_QUERY_ORDER).option(DataTypeFilterFunction.KEY, DEFAULT_TRANSIENT_ONLY).context(contexts).build();
        } else if (type == PermissionDataType.PERSISTENT) {
            query = QueryOptions.builder(QueryMode.CONTEXTUAL).option(DataQueryOrderFunction.KEY, DEFAULT_DATA_QUERY_ORDER).option(DataTypeFilterFunction.KEY, DEFAULT_PERSISTENT_ONLY).context(contexts).build();
        } else if (type == PermissionDataType.USER_PERSISTENT) {
            query = QueryOptions.builder(QueryMode.CONTEXTUAL).option(DataQueryOrderFunction.KEY, DEFAULT_DATA_QUERY_ORDER).option(DataTypeFilterFunction.KEY, USER_PERSISTENT_ONLY).context(contexts).build();
        } else {
            query = QueryOptions.builder(QueryMode.CONTEXTUAL).option(DataQueryOrderFunction.KEY, DEFAULT_DATA_QUERY_ORDER).context(contexts).build();
        }
        CachedPermissionData cachedData = permissionHolder.getCachedData().getPermissionData(query);
        return getGDTristate(cachedData.checkPermission(permission));
    }

    // To set options, pass "meta.option".
    @Override
    public String getOptionValue(GDPermissionHolder holder, Option option, Set<Context> contexts) {
        // If no server context exists, add global
        this.checkServerContext(contexts);
        ImmutableContextSet set = this.getLPContexts(contexts).immutableCopy();
        final PermissionHolder permissionHolder = this.getLuckPermsHolder(holder);
        if (permissionHolder == null) {
            return null;
        }

        final QueryOptions query = QueryOptions.builder(QueryMode.CONTEXTUAL).option(DataQueryOrderFunction.KEY, DEFAULT_DATA_QUERY_ORDER).context(set).build();
        CachedMetaData metaData = permissionHolder.getCachedData().getMetaData(query);
        return metaData.getMetaValue(option.getPermission());
    }

    @Override
    public List<String> getOptionValueList(GDPermissionHolder holder, Option option, Set<Context> contexts) {
        // If no server context exists, add global
        this.checkServerContext(contexts);
        ImmutableContextSet set = this.getLPContexts(contexts).immutableCopy();
        final PermissionHolder permissionHolder = this.getLuckPermsHolder(holder);
        if (permissionHolder == null) {
            return null;
        }

        // First check user persistent data
        QueryOptions query = QueryOptions.builder(QueryMode.CONTEXTUAL).option(DataQueryOrderFunction.KEY, DEFAULT_DATA_QUERY_ORDER).option(DataTypeFilterFunction.KEY, USER_PERSISTENT_ONLY).context(set).build();
        CachedMetaData metaData = permissionHolder.getCachedData().getMetaData(query);
        List<String> list = metaData.getMeta().get(option.getPermission());
        if (list != null) {
            return list;
        }

        // Now check default persistent data
        query = QueryOptions.builder(QueryMode.CONTEXTUAL).option(DataQueryOrderFunction.KEY, DEFAULT_DATA_QUERY_ORDER).option(DataTypeFilterFunction.KEY, DEFAULT_PERSISTENT_ONLY).context(set).build();
        metaData = permissionHolder.getCachedData().getMetaData(query);
        list = metaData.getMeta().get(option.getPermission());
        if (list == null) {
            return new ArrayList<>();
        }
        return list;
    }

    public CompletableFuture<PermissionResult> setOptionValue(GDPermissionHolder holder, String key, String value, Set<Context> contexts, boolean check) {
        if (check) {
            // If no server context exists, add global
            this.checkServerContext(contexts);
        }
        if (holder == GriefDefenderPlugin.DEFAULT_HOLDER) {
            holder = GriefDefenderPlugin.GD_OPTION_HOLDER;
        }
        ImmutableContextSet set = this.getLPContexts(contexts).immutableCopy();
        final Node node = MetaNode.builder().key(key).value(value).context(set).build();
        return this.createOptionFuture(holder, node, key, value, false);
    }

    public CompletableFuture<PermissionResult> setPermissionValue(GDPermissionHolder holder, String permission, Tristate value, Set<Context> contexts, boolean check, boolean save) {
        if (check) {
            // If no server context exists, add global
            this.checkServerContext(contexts);
        }
        ImmutableContextSet set = this.getLPContexts(contexts).immutableCopy();
        final Node node = this.luckPermsApi.getNodeBuilderRegistry().forPermission().permission(permission).value(value.asBoolean()).context(set).build();
        return this.createPermissionFuture(PermissionUtil.getInstance().getGDPermissionHolder(holder, contexts), Arrays.asList(node), value, false);
    }

    @Override
    public CompletableFuture<PermissionResult> setFlagDefinition(GDPermissionHolder holder, FlagDefinition definition, Tristate value, Set<Context> contexts, boolean isTransient) {
        final List<Node> nodes = new ArrayList<>();
        for (FlagData flagData : definition.getFlagData()) {
            for (Context context : contexts) {
                Set<Context> permissionContexts = new HashSet<>(flagData.getContexts());
                permissionContexts.add(context);
                // If no server context exists, add global
                this.checkServerContext(permissionContexts);
                ImmutableContextSet set = this.getLPContexts(permissionContexts).immutableCopy();
                final Node node = this.luckPermsApi.getNodeBuilderRegistry().forPermission().permission(flagData.getFlag().getPermission()).value(value.asBoolean()).context(set).build();
                nodes.add(node);
            }
        }

        return this.createPermissionFuture(GriefDefenderPlugin.GD_DEFINITION_HOLDER, nodes, value, isTransient);
    }

    public CompletableFuture<PermissionResult> setTransientOption(GDPermissionHolder holder, String key, String value, Set<Context> contexts) {
        // If no server context exists, add global
        this.checkServerContext(contexts);
        MutableContextSet contextSet = this.getLPContexts(contexts);
        if (holder == GriefDefenderPlugin.DEFAULT_HOLDER) {
            holder = GriefDefenderPlugin.GD_OPTION_HOLDER;
        }
        Node node = null;
        if (value == null) {
            node = MetaNode.builder().key(key).context(contextSet).build();
        } else {
            node = MetaNode.builder().key(key).value(value).context(contextSet).build();
        }
        return this.createOptionFuture(holder, node, key, value, true);
    }

    public CompletableFuture<PermissionResult> setTransientPermission(GDPermissionHolder holder, String permission, Tristate value, Set<Context> contexts) {
        // If no server context exists, add global
        this.checkServerContext(contexts);
        MutableContextSet contextSet = this.getLPContexts(contexts);
        if (holder == GriefDefenderPlugin.DEFAULT_HOLDER) {
            holder = GriefDefenderPlugin.GD_OPTION_HOLDER;
        }
        final Node node = this.luckPermsApi.getNodeBuilderRegistry().forPermission().permission(permission).value(value.asBoolean()).context(contextSet).build();
        return this.createPermissionFuture(PermissionUtil.getInstance().getGDPermissionHolder(holder, contexts), Arrays.asList(node), value, true);
    }

    private CompletableFuture<PermissionResult> createOptionFuture(GDPermissionHolder holder, Node node, String key, String value, boolean isTransient) {
        if (holder instanceof GDPermissionGroup) {
            return CompletableFuture.supplyAsync(() -> {
                Group group = null;
                try {
                    group = this.luckPermsApi.getGroupManager().loadGroup(holder.getFriendlyName()).get().orElse(null);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                if (group != null) {
                    if (isTransient) {
                        return this.applyTransientOptionNode(holder, group, node, value);
                    }
                    return this.applyOptionNode(holder, group, node, key, value);
                }
                return new GDPermissionResult(ResultTypes.FAILURE, TextComponent.builder().append("COULD NOT LOAD GROUP " + holder.getFriendlyName()).build());
            }, GriefDefenderPlugin.getInstance().executor);
         } else {
             return CompletableFuture.supplyAsync(() -> {
                 User user = null;
                 try {
                     user = this.luckPermsApi.getUserManager().loadUser(((GDPermissionUser) holder).getUniqueId()).get();
                 } catch (Throwable t) {
                     t.printStackTrace();
                 }
                 if (user != null) {
                     if (isTransient) {
                         return this.applyTransientOptionNode(holder, user, node, value);
                     }
                     return this.applyOptionNode(holder, user, node, key, value);
                 }
                 return new GDPermissionResult(ResultTypes.FAILURE, TextComponent.builder().append("COULD NOT LOAD USER " + holder.getFriendlyName()).build());
             }, GriefDefenderPlugin.getInstance().executor);
        }
    }

    private CompletableFuture<PermissionResult> createPermissionFuture(GDPermissionHolder holder, List<Node> nodes, Tristate value, boolean isTransient) {
        if (holder instanceof GDPermissionGroup) {
            return CompletableFuture.supplyAsync(() -> {
                Group group = null;
                try {
                    group = this.luckPermsApi.getGroupManager().loadGroup(holder.getFriendlyName()).get().orElse(null);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                if (group != null) {
                    if (isTransient) {
                        return this.applyTransientPermissionNodes(holder, group, nodes, value);
                    }
                    return this.applyPermissionNodes(holder, group, nodes, value, true);
                }
                return new GDPermissionResult(ResultTypes.FAILURE, TextComponent.builder().append("COULD NOT LOAD GROUP " + holder.getFriendlyName()).build());
            }, GriefDefenderPlugin.getInstance().executor);
         } else {
             return CompletableFuture.supplyAsync(() -> {
                 User user = null;
                 try {
                     user = this.luckPermsApi.getUserManager().loadUser(((GDPermissionUser) holder).getUniqueId()).get();
                 } catch (Throwable t) {
                     t.printStackTrace();
                 }
                 if (user != null) {
                     if (isTransient) {
                         return this.applyTransientPermissionNodes(holder, user, nodes, value);
                     }
                     return this.applyPermissionNodes(holder, user, nodes, value, true);
                 }
                 return new GDPermissionResult(ResultTypes.FAILURE, TextComponent.builder().append("COULD NOT LOAD USER " + holder.getFriendlyName()).build());
             }, GriefDefenderPlugin.getInstance().executor);
        }
    }

    public PermissionResult applyOptionNode(GDPermissionHolder holder, PermissionHolder lpHolder, Node node, String key, String value) {
        final Option option = OptionRegistryModule.getInstance().getById(key).orElse(null);
        if (option == null) {
            new GDPermissionResult(ResultTypes.FAILURE);
        }

        DataMutateResult result = null;
        if (!value.equalsIgnoreCase("undefined")) {
            if (!option.multiValued()) {
                this.clearMeta(lpHolder, option.getPermission(), node.getContexts());
            }
            result = lpHolder.data().add(node);
        } else {
            this.clearMeta(lpHolder, option.getPermission(), node.getContexts());
            this.savePermissionHolder(lpHolder);
            return new GDPermissionResult(ResultTypes.SUCCESS);
        }
        if (result != null) {
            if (result.wasSuccessful()) {
                this.savePermissionHolder(lpHolder);
                return new GDPermissionResult(ResultTypes.SUCCESS, TextComponent.builder().append(result.name()).build());
            }

            return new GDPermissionResult(ResultTypes.FAILURE, TextComponent.builder().append(result.name()).build());
        }

        return new GDPermissionResult(ResultTypes.FAILURE);
    }

    public PermissionResult applyTransientOptionNode(GDPermissionHolder holder, PermissionHolder lpHolder, Node node, String value) {
        DataMutateResult result = null;
        if (value == null) {
            result = lpHolder.transientData().remove(node);
        } else {
            result = lpHolder.transientData().add(node);
        }

        if (result != null) {
            if (result.wasSuccessful()) {
                return new GDPermissionResult(ResultTypes.SUCCESS, TextComponent.builder().append(result.name()).build());
            }
            return new GDPermissionResult(ResultTypes.FAILURE, TextComponent.builder().append(result.name()).build());
        }

        return new GDPermissionResult(ResultTypes.FAILURE);
    }

    public PermissionResult applyPermissionNodes(GDPermissionHolder holder, PermissionHolder lpHolder, List<Node> nodes, Tristate value, boolean save) {
        DataMutateResult result = null;
        for (Node node : nodes) {
            if (value == Tristate.UNDEFINED) {
                result = lpHolder.data().remove(node);
            } else {
                result = lpHolder.data().add(node);
            }
        }

        if (result.wasSuccessful()) {
            if (save) {
                this.savePermissionHolder(lpHolder);
            }

            return new GDPermissionResult(ResultTypes.SUCCESS, TextComponent.builder().append(result.name()).build());
        }

        return new GDPermissionResult(ResultTypes.FAILURE, TextComponent.builder().append(result.name()).build());
    }

    public PermissionResult applyTransientPermissionNodes(GDPermissionHolder holder, PermissionHolder lpHolder, List<Node> nodes, Tristate value) {
        DataMutateResult result = null;
        for (Node node : nodes) {
            if (value == null || value == Tristate.UNDEFINED) {
                result = lpHolder.transientData().remove(node);
            } else {
                result = lpHolder.transientData().add(node);
            }
        }

        if (result.wasSuccessful()) {
            return new GDPermissionResult(ResultTypes.SUCCESS, TextComponent.builder().append(result.name()).build());
        }
        return new GDPermissionResult(ResultTypes.FAILURE, TextComponent.builder().append(result.name()).build());
    }

    public void savePermissionHolder(PermissionHolder holder) {
        // Always wait for completion to avoid save race conditions
        if (holder instanceof User) {
            try {
                this.luckPermsApi.getUserManager().saveUser((User) holder).get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        } else {
            try {
                this.luckPermsApi.getGroupManager().saveGroup((Group) holder).get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    public void refreshCachedData(GDPermissionHolder holder) {
        final PermissionHolder permissionHolder = this.getLuckPermsHolder(holder);
        if (permissionHolder == null) {
            return;
        }
        permissionHolder.getCachedData().invalidate();
    }

    @Override
    public CompletableFuture<Void> save(GDPermissionHolder holder) {
        final PermissionHolder permissionHolder = this.getLuckPermsHolder(holder);
        if (permissionHolder == null) {
            return new CompletableFuture<>();
        }

        if (permissionHolder instanceof User) {
            return this.luckPermsApi.getUserManager().saveUser((User) permissionHolder);
        } else {
            return this.luckPermsApi.getGroupManager().saveGroup((Group) permissionHolder);
        }
    }

    public Set<Context> getGDContexts(ContextSet contexts) {
        final Set<Context> gdContexts = new HashSet<>();
        contexts.forEach(entry -> {
            gdContexts.add(new Context(entry.getKey(), entry.getValue()));
        });

        return gdContexts;
    }

    public MutableContextSet getLPContexts(Set<Context> contexts) {
        MutableContextSet lpContexts = MutableContextSet.create();
        contexts.forEach(entry -> {
            lpContexts.add(entry.getKey(), entry.getValue());
        });

        return lpContexts;
    }

    public Tristate getGDTristate(net.luckperms.api.util.Tristate state) {
        if (state == net.luckperms.api.util.Tristate.TRUE) {
            return Tristate.TRUE;
        }
        if (state == net.luckperms.api.util.Tristate.FALSE) {
            return Tristate.FALSE;
        }
        return Tristate.UNDEFINED;
    }

    private void clearMeta(PermissionHolder holder, String metaKey, ContextSet set) {
        if (set.size() == 1 && set.containsKey("server")) {
            if (set.getAnyValue("server").get().equalsIgnoreCase("global")) {
                // LP does not remove meta if passing only global context so we need to make sure to pass none
                holder.data().clear(NodeType.META.predicate(node -> node.getMetaKey().equals(metaKey)));
                return;
            }
        }
        holder.data().clear(set, NodeType.META.predicate(node -> node.getMetaKey().equals(metaKey)));
    }

    private void checkServerContext(Set<Context> contexts) {
        for (Context context : contexts) {
            if (context.getKey().equalsIgnoreCase("server")) {
                return;
            }
        }
        final String serverName = this.luckPermsApi.getServerName();
        if (serverName != null) {
            contexts.add(new Context("server", serverName));
        } else {
            contexts.add(new Context("server", "global"));
        }
    }

    private static class DefaultDataQueryOrderFunction implements DataQueryOrderFunction {

        @Override
        public Comparator<DataType> getOrderComparator(Identifier identifier) {
            if (identifier.getType() == Identifier.GROUP_TYPE && (identifier.getName().equalsIgnoreCase(GriefDefenderPlugin.DEFAULT_GROUP_NAME) || identifier.getName().startsWith("griefdefender_"))) {
                return DataQueryOrder.TRANSIENT_LAST;
            }

            return DataQueryOrder.TRANSIENT_FIRST;
        }
    }

    private static class DefaultTransientOnlyDataFilter implements DataTypeFilterFunction {

        @Override
        public @NonNull Predicate<DataType> getTypeFilter(@NonNull Identifier identifier) {
            if (identifier.getType() == Identifier.GROUP_TYPE && (identifier.getName().equalsIgnoreCase(GriefDefenderPlugin.DEFAULT_GROUP_NAME) || identifier.getName().startsWith("griefdefender_"))) {
                return DataTypeFilter.TRANSIENT_ONLY;
            }
            return DataTypeFilter.ALL;
        }
        
    }

    private static class DefaultPersistentOnlyDataFilter implements DataTypeFilterFunction {

        @Override
        public @NonNull Predicate<DataType> getTypeFilter(@NonNull Identifier identifier) {
            if (identifier.getType() == Identifier.GROUP_TYPE && (identifier.getName().equalsIgnoreCase(GriefDefenderPlugin.DEFAULT_GROUP_NAME) || identifier.getName().startsWith("griefdefender_"))) {
                return DataTypeFilter.NORMAL_ONLY;
            }
            return DataTypeFilter.ALL;
        }
    }

    private static class UserPersistentOnlyDataFilter implements DataTypeFilterFunction {

        @Override
        public @NonNull Predicate<DataType> getTypeFilter(@NonNull Identifier identifier) {
            if (identifier.getType() == Identifier.GROUP_TYPE && (identifier.getName().equalsIgnoreCase(GriefDefenderPlugin.DEFAULT_GROUP_NAME) || identifier.getName().startsWith("griefdefender_"))) {
                return DataTypeFilter.NONE;
            }
            return DataTypeFilter.NORMAL_ONLY;
        }
    }
}
