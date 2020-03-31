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
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.Tristate;
import com.griefdefender.internal.util.NMSUtil;
import com.griefdefender.permission.GDPermissionGroup;
import com.griefdefender.permission.GDPermissionHolder;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.util.PermissionUtil;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.user.UserStorageService;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class PermissionHolderCache {

    private static PermissionHolderCache instance;
    private final Cache<UUID, GDPermissionUser> userCache = Caffeine.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES)
            .build();
    private final Cache<String, GDPermissionGroup> groupCache = Caffeine.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES)
            .build();
    private final ConcurrentHashMap<GDPermissionHolder, Cache<Integer, Tristate>> permissionCache = new ConcurrentHashMap<>();

    public GDPermissionUser getOrCreateUser(User user) {
        if (user == null) {
            return null;
        }

        return this.getOrCreateUser(user.getUniqueId());
    }

    public GDPermissionUser getOrCreateUser(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        if (uuid.equals(GriefDefenderPlugin.PUBLIC_UUID)) {
            return GriefDefenderPlugin.PUBLIC_USER;
        }
        if (uuid.equals(GriefDefenderPlugin.WORLD_USER_UUID)) {
            return GriefDefenderPlugin.WORLD_USER;
        }

        GDPermissionUser holder = this.userCache.getIfPresent(uuid);
        if (holder != null) {
            return holder;
        }

        holder = new GDPermissionUser(uuid);
        this.userCache.put(uuid, holder);
        return holder;
    }

    public GDPermissionUser getOrCreateUser(String username) {
        if (username == null || username.length() > 16) {
            return null;
        }

        final UUID uuid = PermissionUtil.getInstance().lookupUserUniqueId(username);
        if (uuid != null) {
            return this.getOrCreateUser(uuid);
        }
        User user = Sponge.getGame().getServiceManager().provide(UserStorageService.class).get().get(username).orElse(null);
        if (user == null) {
            user = NMSUtil.getInstance().createUserFromCache(username);
        }
        if (user != null) {
            return this.getOrCreateUser(user);
        }

        return null;
    }

    public GDPermissionGroup getOrCreateGroup(String groupName) {
        if (groupName == null) {
            return null;
        }
        GDPermissionGroup holder = this.groupCache.getIfPresent(groupName);
        if (holder != null) {
            return holder;
        }

        holder = new GDPermissionGroup(groupName);
        this.groupCache.put(groupName, holder);
        return holder;
    }

    public GDPermissionHolder getOrCreateHolder(String identifier) {
        if (identifier == null) {
            return null;
        }
        UUID uuid = null;
        try {
            uuid = UUID.fromString(identifier);
        } catch (IllegalArgumentException e) {
            return this.getOrCreateGroup(identifier);
        }

        return this.getOrCreateUser(uuid);
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
