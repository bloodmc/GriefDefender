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
import java.util.concurrent.CompletableFuture;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.griefdefender.GDPlayerData;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.PermissionResult;
import com.griefdefender.api.permission.flag.Flag;
import com.griefdefender.api.permission.option.Option;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.permission.GDPermissionHolder;

/**
 * Represents a provider of permission data.
 * 
 * <p>This is the interface that a permissions plugin must implement and
 * to provide permissions lookups for GriefDefender.</p>
 */
public interface PermissionProvider {

    /**
     * Get server name.
     * 
     * @return The server name
     */
    String getServerName();

    /**
     * Checks if the group identifier exists.
     * 
     * @param identifier The group identifier
     * @return whether the group exists
     */
    boolean hasGroupSubject(String identifier);

    /**
     * Performs a lookup for a UUID with matching 
     * username.
     * 
     * @param name The username to search with
     * @return The user uuid if available
     */
    @Nullable UUID lookupUserUniqueId(String name);

    /**
     * Retrieves an immutable list of all loaded player
     * names that exist in permissions.
     * 
     * @return An immutable list of player names or empty if none.
     */
    List<String> getAllLoadedPlayerNames();

    /**
     * Retrieves an immutable list of all loaded group
     * names that exist in permissions.
     * 
     * @return An immutable list of group names or empty if none.
     */
    List<String> getAllLoadedGroupNames();

    /**
     * Appends all active contexts to passed context set currently
     * active on permission holder.
     * 
     * @param contexts The set to append to
     * @param permissionHolder The holder to check active contexts
     */
    void addActiveContexts(Set<Context> contexts, GDPermissionHolder permissionHolder);

    /**
     * Appends all active contexts to passed context set currently
     * active on permission holder.
     * 
     * @param contexts The set to append to
     * @param permissionHolder The holder to check active contexts
     * @param playerData The player data
     * @param claim The claim
     */
    void addActiveContexts(Set<Context> contexts, GDPermissionHolder permissionHolder, GDPlayerData playerData, Claim claim);

    /**
     * Clears all permissions that contain {@link Claim#getContext()}
     * from passed {@link Claim}.
     * 
     * @param claim The claim
     */
    void clearPermissions(GDClaim claim);

    /**
     * Clears all permissions that contain {@link Context}
     * from passed holder.
     * 
     * @param claim The claim
     */
    void clearPermissions(GDPermissionHolder holder, Context context);

    /**
     * Clears all permissions that contain {@link Context}'s
     * from passed player.
     * 
     * @param claim The claim
     */
    void clearPermissions(GDPermissionHolder holder, Set<Context> contexts);

    /**
     * Checks if holder has permission.
     * 
     * @param permission The permission
     * @return whether the holder has permission
     */
    boolean holderHasPermission(GDPermissionHolder holder, String permission);

    /**
     * Gets all cached permissions of holder that contain specific {@link Context}'s.
     * 
     * @param holder The holder
     * @param contexts The contexts required
     * @return An immutable map of cached permissions or empty if none.
     */
    Map<String, Boolean> getPermissions(GDPermissionHolder holder, Set<Context> contexts);

    /**
     * Gets all cached options of holder that contain specific {@link Context}'s.
     * 
     * @param holder The holder
     * @param contexts The contexts required
     * @return An immutable map of cached options or empty if none.
     */
    Map<String, String> getOptions(GDPermissionHolder holder, Set<Context> contexts);

    /**
     * Gets all persisted permissions with associated contexts of holder.
     * 
     * @param holder The holder
     * @return An immutable map of persisted permissions or empty if none.
     */
    Map<Set<Context>, Map<String, Boolean>> getPermanentPermissions(GDPermissionHolder holder);

    /**
     * Gets all transient permissions with associated contexts of holder.
     * 
     * @param holder The holder
     * @return An immutable map of transient permissions or empty if none.
     */
    Map<Set<Context>, Map<String, Boolean>> getTransientPermissions(GDPermissionHolder holder);

    /**
     * Gets all persisted options with associated contexts of holder.
     * 
     * @param holder The holder
     * @return An immutable map of persisted options or empty if none.
     */
    Map<Set<Context>, Map<String, String>> getPermanentOptions(GDPermissionHolder holder);

    /**
     * Gets all transient options with associated contexts of holder.
     * 
     * @param holder The holder
     * @return An immutable map of transient options or empty if none.
     */
    Map<Set<Context>, Map<String, String>> getTransientOptions(GDPermissionHolder holder);

    /**
     * Gets all persisted options and associated values of holder.
     * 
     * @param holder The holder
     * @return An immutable map of persisted options or empty if none.
     */
    Map<String, String> getPermanentOptions(GDPermissionHolder holder, Set<Context> contexts);

    /**
     * Gets all transient options and associated values of holder.
     * 
     * @param holder The holder
     * @return An immutable map of transient options or empty if none.
     */
    Map<String, String> getTransientOptions(GDPermissionHolder holder, Set<Context> contexts);

    /**
     * Gets all persisted permissions, including inherited nodes, with associated contexts of holder.
     * 
     * @param holder The holder
     * @return An immutable map of persisted permissions or empty if none.
     */
    Map<Set<Context>, Map<String, Boolean>> getAllPermissions(GDPermissionHolder holder);

    /**
     * Gets the current value of a permission assigned to a holder.
     * 
     * @param holder The holder
     * @param permission The permission to check
     * @return The permission value
     */
    Tristate getPermissionValue(GDPermissionHolder holder, String permission);

    /**
     * Gets the current value of a permission assigned to a holder.
     * 
     * @param claim The current claim
     * @param holder The holder
     * @param permission The permission to check
     * @param contexts The contexts
     * @return The permission value
     */
    Tristate getPermissionValue(GDClaim claim, GDPermissionHolder holder, String permission, Set<Context> contexts);

    /**
     * Gets the current value of a permission assigned to a holder.
     * 
     * @param claim The current claim
     * @param holder The holder
     * @param permission The permission to check
     * @param contexts The contexts
     * @param checkTransient Whether to check transient permissions
     * @return The permission value
     */
    Tristate getPermissionValue(GDClaim claim, GDPermissionHolder holder, String permission, Set<Context> contexts, boolean checkTransient);

    /**
     * Gets the current value of a permission assigned to a holder.
     * 
     * @param holder The holder
     * @param permission The permission to check
     * @param contexts The contexts
     * @return The permission value
     */
    Tristate getPermissionValue(GDPermissionHolder holder, String permission, Set<Context> contexts);

    /**
     * Gets the current value of a permission that contains all passed contexts
     * assigned to a holder.
     * 
     * @param claim The current claim
     * @param holder The holder
     * @param permission The permission to check
     * @param contexts The contexts required
     * @param contextFilter The context key to ignore for required contexts
     * @return The permission value
     */
    Tristate getPermissionValueWithRequiredContexts(GDClaim claim, GDPermissionHolder holder, String permission, Set<Context> contexts, String contextFilter);

    /**
     * Gets the current value of an option assigned to a holder.
     * 
     * @param holder The holder
     * @param permission The permission to check
     * @param contexts The contexts
     * @return The option value
     */
    String getOptionValue(GDPermissionHolder holder, Option option, Set<Context> contexts);

    /**
     * Gets the current values of an option assigned to a holder.
     * 
     * Note: This is intended to be used for options that support multiple values.
     * 
     * @param holder The holder
     * @param permission The permission to check
     * @param contexts The contexts
     * @return The option value list
     */
    List<String> getOptionValueList(GDPermissionHolder holder, Option option, Set<Context> contexts);

    /**
     * Sets an option and value with contexts to a holder.
     * 
     * @param holder The holder
     * @param permission The permission
     * @param value The value
     * @param contexts The contexts
     * @param check Whether to check and apply a server context if none exists
     * @return The permission result
     */
    default PermissionResult setOptionValue(GDPermissionHolder holder, String permission, String value, Set<Context> contexts) {
        return this.setOptionValue(holder, permission, value, contexts, true);
    }

    /**
     * Sets an option and value with contexts to a holder.
     * 
     * @param holder The holder
     * @param permission The permission
     * @param value The value
     * @param contexts The contexts
     * @param check Whether to check and apply a server context if none exists
     * @return The permission result
     */
    PermissionResult setOptionValue(GDPermissionHolder holder, String permission, String value, Set<Context> contexts, boolean check);

    /**
     * Sets a permission and value with contexts to a holder.
     * 
     * @param holder The holder
     * @param flag The flag to use for permission
     * @param value The value
     * @param contexts The contexts
     * @return The permission result
     */
    default PermissionResult setPermissionValue(GDPermissionHolder holder, Flag flag, Tristate value, Set<Context> contexts) {
        return this.setPermissionValue(holder, flag.getPermission(), value, contexts, true, true);
    }

    /**
     * Sets a permission and value with contexts to a holder.
     * 
     * @param holder The holder
     * @param flag The flag to use for permission
     * @param value The value
     * @param contexts The contexts
     * @param check Whether to check and apply a server context if none exists
     * @param save Whether a save should occur
     * @return The permission result
     */
    default PermissionResult setPermissionValue(GDPermissionHolder holder, Flag flag, Tristate value, Set<Context> contexts, boolean check, boolean save) {
        return this.setPermissionValue(holder, flag.getPermission(), value, contexts, check, save);
    }

    /**
     * Sets a permission and value with contexts to a holder.
     * 
     * @param holder The holder
     * @param permission The permission
     * @param value The value
     * @param contexts The contexts
     * @return Whether the set permission operation was successful
     */
    default PermissionResult setPermissionValue(GDPermissionHolder holder, String permission, Tristate value, Set<Context> contexts) {
        return this.setPermissionValue(holder, permission, value, contexts, true, true);
    }

    /**
     * Sets a permission and value with contexts to a holder.
     * 
     * @param holder The holder
     * @param permission The permission
     * @param value The value
     * @param contexts The contexts
     * @param check Whether to check and apply a server context if none exists
     * @param save Whether a save should occur
     * @return Whether the set permission operation was successful
     */
    PermissionResult setPermissionValue(GDPermissionHolder holder, String permission, Tristate value, Set<Context> contexts, boolean check, boolean save);

    /**
     * Sets a transient option and value with contexts to a holder.
     * 
     * @param holder The holder
     * @param permission The permission
     * @param value The value
     * @param contexts The contexts
     * @return Whether the set permission operation was successful
     */
    PermissionResult setTransientOption(GDPermissionHolder holder, String permission, String value, Set<Context> contexts);

    /**
     * Sets a transient permission and value with contexts to a holder.
     * 
     * @param holder The holder
     * @param permission The permission
     * @param value The value
     * @param contexts The contexts
     * @return Whether the set permission operation was successful
     */
    PermissionResult setTransientPermission(GDPermissionHolder holder, String permission, Tristate value, Set<Context> contexts);

    /**
     * Refreshes all cached permission data of holder.
     * 
     * @param holder The holder
     */
    void refreshCachedData(GDPermissionHolder holder);

    /**
     * Saves any pending permission changes to holder.
     * 
     * @param holder The holder
     * @return a future which will complete when save is done
     */
    CompletableFuture<Void> save(GDPermissionHolder holder);
}
