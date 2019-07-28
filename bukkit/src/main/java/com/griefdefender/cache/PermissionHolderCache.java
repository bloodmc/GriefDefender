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
package com.griefdefender.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.griefdefender.api.Tristate;
import com.griefdefender.permission.GDPermissionGroup;
import com.griefdefender.permission.GDPermissionHolder;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.util.PermissionUtil;
import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.User;
import org.bukkit.OfflinePlayer;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class PermissionHolderCache {

    private static PermissionHolderCache instance;
    private final Cache<String, GDPermissionHolder> holderCache = Caffeine.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES)
            .build();
    private final ConcurrentHashMap<GDPermissionHolder, Cache<Integer, Tristate>> permissionCache = new ConcurrentHashMap<>();

    public GDPermissionUser getOrCreateUser(OfflinePlayer player) {
        if (player == null) {
            return null;
        }

        return getOrCreateUser(player.getUniqueId());
    }

    public GDPermissionUser getOrCreateUser(UUID uuid) {
        GDPermissionUser user = null;
        GDPermissionHolder holder = this.holderCache.getIfPresent(uuid.toString());
        if (holder != null) {
            return (GDPermissionUser) holder;
        }

        user = new GDPermissionUser(uuid);
        this.holderCache.put(user.getIdentifier(), user);
        return user;
    }

    public GDPermissionUser getOrCreateUser(String username) {
        final User luckPermsUser = PermissionUtil.getInstance().getUserSubject(username);
        if (luckPermsUser != null) {
            return this.getOrCreateUser(luckPermsUser.getUuid());
        }
        return null;
    }

    public GDPermissionGroup getOrCreateGroup(String groupName) {
        GDPermissionGroup group = null;
        GDPermissionHolder holder = this.holderCache.getIfPresent(groupName);
        if (holder != null) {
            return (GDPermissionGroup) holder;
        }

        final Group luckPermsGroup = PermissionUtil.getInstance().getGroupSubject(groupName);
        if (luckPermsGroup == null) {
            return null;
        }

        group = new GDPermissionGroup(luckPermsGroup);
        this.holderCache.put(groupName, group);
        return group;
    }

    public GDPermissionGroup getOrCreateGroup(Group group) {
        GDPermissionGroup permissionHolder = null;
        GDPermissionHolder holder = this.holderCache.getIfPresent(group.getName());
        if (holder != null) {
            return (GDPermissionGroup) holder;
        }

        permissionHolder = new GDPermissionGroup(group);
        this.holderCache.put(group.getName(), permissionHolder);
        return permissionHolder;
    }

    public GDPermissionHolder getOrCreateHolder(String identifier) {
        GDPermissionHolder holder = this.holderCache.getIfPresent(identifier);
        if (holder != null) {
            return holder;
        }

        holder = new GDPermissionHolder(identifier);
        this.holderCache.put(identifier, holder);
        return holder;
    }

    public Cache<Integer, Tristate> getOrCreatePermissionCache(GDPermissionHolder holder) {
        Cache<Integer, Tristate> cache = this.permissionCache.get(holder);
        if (cache == null) {
            cache = Caffeine.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES).build();
            this.permissionCache.put(holder, cache);
        }
        return cache;
    }

    public void invalidateAllPermissionCache() {
        for (Cache<Integer, Tristate> cache : this.permissionCache.values()) {
            cache.invalidateAll();
        }
    }

    static {
        instance = new PermissionHolderCache();
    }

    public static PermissionHolderCache getInstance() {
        return instance;
    }
}
