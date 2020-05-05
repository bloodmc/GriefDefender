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

import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.PermissionResult;
import com.griefdefender.api.permission.flag.Flag;
import com.griefdefender.api.permission.option.Option;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.permission.GDPermissionHolder;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.provider.PermissionProvider;
import org.spongepowered.api.entity.living.player.Player;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PermissionUtil {

    private final PermissionProvider PERMISSION_PROVIDER;

    private static PermissionUtil instance;

    public static PermissionUtil getInstance() {
        return instance;
    }

    static {
        instance = new PermissionUtil();
    }

    public PermissionUtil() {
        this.PERMISSION_PROVIDER = GriefDefenderPlugin.getInstance().getPermissionProvider();
    }

    public String getServerName() {
        return PERMISSION_PROVIDER.getServerName();
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

    public void clearPermissions(GDClaim claim) {
        PERMISSION_PROVIDER.clearPermissions(claim);
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

    public Map<Set<Context>, Map<String, Boolean>> getPermanentPermissions(GDPermissionHolder holder) {
        return PERMISSION_PROVIDER.getPermanentPermissions(holder);
    }

    public Map<Set<Context>, Map<String, Boolean>> getTransientPermissions(GDPermissionHolder holder) {
        return PERMISSION_PROVIDER.getTransientPermissions(holder);
    }

    public Map<Set<Context>, Map<String, String>> getPermanentOptions(GDPermissionHolder holder) {
        return PERMISSION_PROVIDER.getPermanentOptions(holder);
    }

    public Map<Set<Context>, Map<String, String>> getTransientOptions(GDPermissionHolder holder) {
        return PERMISSION_PROVIDER.getTransientOptions(holder);
    }

    public Map<String, String> getPermanentOptions(GDPermissionHolder holder, Set<Context> contexts) {
        return PERMISSION_PROVIDER.getPermanentOptions(holder, contexts);
    }

    public Map<String, String> getTransientOptions(GDPermissionHolder holder, Set<Context> contexts) {
        return PERMISSION_PROVIDER.getTransientOptions(holder, contexts);
    }

    public Map<Set<Context>, Map<String, Boolean>> getAllPermissions(GDPermissionHolder holder) {
        return PERMISSION_PROVIDER.getAllPermissions(holder);
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

    public Tristate getPermissionValueWithRequiredContexts(GDClaim claim, GDPermissionHolder holder, String permission, Set<Context> contexts, String contextFilter) {
        return PERMISSION_PROVIDER.getPermissionValueWithRequiredContexts(claim, holder, permission, contexts, contextFilter);
    }

    public String getOptionValue(GDPermissionHolder holder, Option option, Set<Context> contexts) {
        return PERMISSION_PROVIDER.getOptionValue(holder, option, contexts);
    }

    public List<String> getOptionValueList(GDPermissionHolder holder, Option option, Set<Context> contexts) {
        return PERMISSION_PROVIDER.getOptionValueList(holder, option, contexts);
    }

    public PermissionResult setOptionValue(GDPermissionHolder holder, String permission, String value, Set<Context> contexts) {
        return PERMISSION_PROVIDER.setOptionValue(holder, permission, value, contexts, true);
    }

    public PermissionResult setOptionValue(GDPermissionHolder holder, String permission, String value, Set<Context> contexts, boolean check) {
        return PERMISSION_PROVIDER.setOptionValue(holder, permission, value, contexts, check);
    }

    public PermissionResult setPermissionValue(GDPermissionHolder holder, Flag flag, Tristate value, Set<Context> contexts) {
        return PERMISSION_PROVIDER.setPermissionValue(holder, flag, value, contexts, true, true);
    }

    public PermissionResult setPermissionValue(GDPermissionHolder holder, String permission, Tristate value, Set<Context> contexts) {
        return PERMISSION_PROVIDER.setPermissionValue(holder, permission, value, contexts, true, true);
    }

    public PermissionResult setPermissionValue(GDPermissionHolder holder, Flag flag, Tristate value, Set<Context> contexts, boolean check, boolean save) {
        return PERMISSION_PROVIDER.setPermissionValue(holder, flag, value, contexts, check, save);
    }

    public PermissionResult setPermissionValue(GDPermissionHolder holder, String permission, Tristate value, Set<Context> contexts, boolean check, boolean save) {
        return PERMISSION_PROVIDER.setPermissionValue(holder, permission, value, contexts, check, save);
    }

    public PermissionResult setTransientOption(GDPermissionHolder holder, String permission, String value, Set<Context> contexts) {
        return PERMISSION_PROVIDER.setTransientOption(holder, permission, value, contexts);
    }

    public PermissionResult setTransientPermission(GDPermissionHolder holder, String permission, Tristate value, Set<Context> contexts) {
        return PERMISSION_PROVIDER.setTransientPermission(holder, permission, value, contexts);
    }

    public void refreshCachedData(GDPermissionHolder holder) {
        PERMISSION_PROVIDER.refreshCachedData(holder);
    }

    public CompletableFuture<Void> save(GDPermissionHolder holder) {
        return PERMISSION_PROVIDER.save(holder);
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

    public boolean canPlayerTeleport(Player player, GDClaim claim) {
        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
        if (!playerData.canIgnoreClaim(claim) && !playerData.canManageAdminClaims) {
            // if not owner of claim, validate perms
            if (!player.getUniqueId().equals(claim.getOwnerUniqueId())) {
                if (!player.hasPermission(GDPermissions.COMMAND_CLAIM_INFO_TELEPORT_OTHERS)) {
                    return false;
                }
                if (!claim.isUserTrusted(player, TrustTypes.ACCESSOR)) {
                    if (GriefDefenderPlugin.getInstance().economyService.orElse(null) != null) {
                        // Allow non-trusted to TP to claims for sale
                        if (!claim.getEconomyData().isForSale()) {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
            } else if (!player.hasPermission(GDPermissions.COMMAND_CLAIM_INFO_TELEPORT_BASE)) {
                return false;
            }
        }

        return true;
    }
}
