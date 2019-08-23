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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.OfflinePlayer;

import com.griefdefender.GDPlayerData;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.PermissionResult;
import com.griefdefender.api.permission.flag.Flag;
import com.griefdefender.api.permission.option.Option;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.permission.GDPermissionHolder;

public interface PermissionProvider {

    boolean hasGroupSubject(String identifier);

    UUID lookupUserUniqueId(String name);

    List<String> getAllLoadedPlayerNames();

    List<String> getAllLoadedGroupNames();

    void addActiveContexts(Set<Context> contexts, GDPermissionHolder permissionHolder);

    void addActiveContexts(Set<Context> contexts, GDPermissionHolder permissionHolder, GDPlayerData playerData, Claim claim);

    boolean containsDefaultContext(Set<Context> contexts);

    boolean containsOverrideContext(Set<Context> contexts);

    void clearPermissions(GDClaim claim);

    void clearPermissions(OfflinePlayer player, Context context);

    void clearPermissions(GDPermissionHolder holder, Context context);

    void clearPermissions(GDPermissionHolder holder, Set<Context> contexts);

    boolean holderHasPermission(GDPermissionHolder holder, String permission);

    Map<String, Boolean> getPermissions(GDPermissionHolder holder, Set<Context> contexts);

    Map<String, String> getOptions(GDPermissionHolder holder, Set<Context> contexts);

    Map<Set<Context>, Map<String, Boolean>> getPermanentPermissions(GDClaim claim, GDPermissionHolder holder);

    Map<Set<Context>, Map<String, Boolean>> getTransientPermissions(GDClaim claim, GDPermissionHolder holder);

    Map<Set<Context>, Map<String, String>> getPermanentOptions(GDPermissionHolder holder);

    Map<Set<Context>, Map<String, String>> getTransientOptions(GDPermissionHolder holder);

    Map<String, String> getPermanentOptions(GDClaim claim, GDPermissionHolder holder, Set<Context> contexts);

    Map<String, String> getTransientOptions(GDClaim claim, GDPermissionHolder holder, Set<Context> contexts);

    Map<Set<Context>, Map<String, Boolean>> getAllPermissions(GDClaim claim, GDPermissionHolder holder);

    Tristate getPermissionValue(GDPermissionHolder holder, String permission);

    Tristate getPermissionValue(GDClaim claim, GDPermissionHolder holder, String permission, Set<Context> contexts);

    Tristate getPermissionValue(GDClaim claim, GDPermissionHolder holder, String permission, Set<Context> contexts, boolean checkTransient);

    Tristate getPermissionValue(GDPermissionHolder holder, String permission, Set<Context> contexts);

    String getOptionValue(GDPermissionHolder holder, Option option, Set<Context> contexts);

    PermissionResult setOptionValue(GDPermissionHolder holder, String permission, String value, Set<Context> contexts);

    PermissionResult setPermissionValue(GDPermissionHolder holder, Flag flag, Tristate value, Set<Context> contexts);

    boolean setPermissionValue(GDPermissionHolder holder, String permission, Tristate value, Set<Context> contexts);

    void setTransientOption(GDPermissionHolder holder, String permission, String value, Set<Context> contexts);

    void setTransientPermission(GDPermissionHolder holder, String permission, Boolean value, Set<Context> contexts);

    void refreshCachedData(GDPermissionHolder holder);
}
