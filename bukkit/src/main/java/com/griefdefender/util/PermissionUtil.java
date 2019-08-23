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
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
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
import com.griefdefender.api.permission.option.type.CreateModeType;
import com.griefdefender.api.permission.option.type.CreateModeTypes;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.permission.GDPermissionGroup;
import com.griefdefender.permission.GDPermissionHolder;
import com.griefdefender.permission.GDPermissionResult;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.provider.PermissionProvider;

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
import org.apache.commons.io.FilenameUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class PermissionUtil {

    private final PermissionProvider PERMISSION_PROVIDER;

    private static PermissionUtil instance;

    private LuckPermsApi luckPermsApi;

    public static PermissionUtil getInstance() {
        return instance;
    }

    static {
        instance = new PermissionUtil();
    }

    public PermissionUtil() {
        this.PERMISSION_PROVIDER = GriefDefenderPlugin.getInstance().getPermissionProvider();
    }

    public boolean hasGroupSubject(String identifier) {
        return PERMISSION_PROVIDER.hasGroupSubject(identifier);
    }

    public UUID lookupUserUniqueId(String name) {
        return PERMISSION_PROVIDER.lookupUserUniqueId(name);
    }

    public List<String> getAllLoadedPlayerNames() {
        return PERMISSION_PROVIDER.getAllLoadedPlayerNames();
    } 

    public List<String> getAllLoadedGroupNames() {
        return PERMISSION_PROVIDER.getAllLoadedGroupNames();
    } 

    public void addActiveContexts(Set<Context> contexts, GDPermissionHolder permissionHolder) {
        PERMISSION_PROVIDER.addActiveContexts(contexts, permissionHolder);
    }

    public void addActiveContexts(Set<Context> contexts, GDPermissionHolder permissionHolder, GDPlayerData playerData, Claim claim) {
        PERMISSION_PROVIDER.addActiveContexts(contexts, permissionHolder, null, null);
    }

    public boolean containsDefaultContext(Set<Context> contexts) {
        return PERMISSION_PROVIDER.containsDefaultContext(contexts);
    }

    public boolean containsOverrideContext(Set<Context> contexts) {
        return PERMISSION_PROVIDER.containsOverrideContext(contexts);
    }

    public void clearPermissions(GDClaim claim) {
        PERMISSION_PROVIDER.clearPermissions(claim);
    }

    public void clearPermissions(OfflinePlayer player, Context context) {
        PERMISSION_PROVIDER.clearPermissions(player, context);
    }

    public void clearPermissions(GDPermissionHolder holder, Context context) {
        PERMISSION_PROVIDER.clearPermissions(holder, context);
    }

    public void clearPermissions(GDPermissionHolder holder, Set<Context> contexts) {
        PERMISSION_PROVIDER.clearPermissions(holder, contexts);
    }

    public boolean holderHasPermission(GDPermissionHolder holder, String permission) {
        return PERMISSION_PROVIDER.holderHasPermission(holder, permission);
    }

    public Map<String, Boolean> getPermissions(GDPermissionHolder holder, Set<Context> contexts) {
        return PERMISSION_PROVIDER.getPermissions(holder, contexts);
    }

    public Map<String, String> getOptions(GDPermissionHolder holder, Set<Context> contexts) {
        return PERMISSION_PROVIDER.getOptions(holder, contexts);
    }

    public Map<Set<Context>, Map<String, Boolean>> getPermanentPermissions(GDClaim claim, GDPermissionHolder holder) {
        return PERMISSION_PROVIDER.getPermanentPermissions(claim, holder);
    }

    public Map<Set<Context>, Map<String, Boolean>> getTransientPermissions(GDClaim claim, GDPermissionHolder holder) {
        return PERMISSION_PROVIDER.getTransientPermissions(claim, holder);
    }

    public Map<Set<Context>, Map<String, String>> getPermanentOptions(GDPermissionHolder holder) {
        return PERMISSION_PROVIDER.getPermanentOptions(holder);
    }

    public Map<Set<Context>, Map<String, String>> getTransientOptions(GDPermissionHolder holder) {
        return PERMISSION_PROVIDER.getTransientOptions(holder);
    }

    public Map<String, String> getPermanentOptions(GDClaim claim, GDPermissionHolder holder, Set<Context> contexts) {
        return PERMISSION_PROVIDER.getPermanentOptions(claim, holder, contexts);
    }

    public Map<String, String> getTransientOptions(GDClaim claim, GDPermissionHolder holder, Set<Context> contexts) {
        return PERMISSION_PROVIDER.getTransientOptions(claim, holder, contexts);
    }

    public Map<Set<Context>, Map<String, Boolean>> getAllPermissions(GDClaim claim, GDPermissionHolder holder) {
        return PERMISSION_PROVIDER.getAllPermissions(claim, holder);
    }

    public Tristate getPermissionValue(GDPermissionHolder holder, String permission) {
        return PERMISSION_PROVIDER.getPermissionValue(holder, permission);
    }

    public Tristate getPermissionValue(GDClaim claim, GDPermissionHolder holder, String permission, Set<Context> contexts) {
        return PERMISSION_PROVIDER.getPermissionValue(claim, holder, permission, contexts);
    }

    public Tristate getPermissionValue(GDClaim claim, GDPermissionHolder holder, String permission, Set<Context> contexts, boolean checkTransient) {
        return PERMISSION_PROVIDER.getPermissionValue(claim, holder, permission, contexts, checkTransient);
    }

    public Tristate getPermissionValue(GDPermissionHolder holder, String permission, Set<Context> contexts) {
        return PERMISSION_PROVIDER.getPermissionValue(holder, permission, contexts);
    }

    public String getOptionValue(GDPermissionHolder holder, Option option, Set<Context> contexts) {
        return PERMISSION_PROVIDER.getOptionValue(holder, option, contexts);
    }

    public PermissionResult setOptionValue(GDPermissionHolder holder, String permission, String value, Set<Context> contexts) {
        return PERMISSION_PROVIDER.setOptionValue(holder, permission, value, contexts);
    }

    public PermissionResult setPermissionValue(GDPermissionHolder holder, Flag flag, Tristate value, Set<Context> contexts) {
        return PERMISSION_PROVIDER.setPermissionValue(holder, flag, value, contexts);
    }

    public boolean setPermissionValue(GDPermissionHolder holder, String permission, Tristate value, Set<Context> contexts) {
        return PERMISSION_PROVIDER.setPermissionValue(holder, permission, value, contexts);
    }

    public void setTransientOption(GDPermissionHolder holder, String permission, String value, Set<Context> contexts) {
        PERMISSION_PROVIDER.setTransientOption(holder, permission, value, contexts);
    }

    public void setTransientPermission(GDPermissionHolder holder, String permission, Boolean value, Set<Context> contexts) {
        PERMISSION_PROVIDER.setTransientPermission(holder, permission, value, contexts);
    }

    public void refreshCachedData(GDPermissionHolder holder) {
        PERMISSION_PROVIDER.refreshCachedData(holder);
    }

    public boolean containsKey(Set<Context> contexts, String key) {
        for (Context context : contexts) {
            if (context.getKey().equalsIgnoreCase(key)) {
                return true;
            }
        }
        return false;
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
