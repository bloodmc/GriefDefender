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

import com.google.common.collect.ImmutableMap;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.api.claim.ClaimTypes;
import com.griefdefender.api.claim.ShovelType;
import com.griefdefender.api.claim.ShovelTypes;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.visual.ClaimVisualType;
import net.kyori.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

import javax.annotation.Nullable;

public class PlayerUtil {

    private static PlayerUtil instance;

    public static PlayerUtil getInstance() {
        return instance;
    }

    static {
        instance = new PlayerUtil();
    }

    public ClaimType getClaimTypeFromShovel(ShovelType shovelMode) {
        if (shovelMode == ShovelTypes.ADMIN) {
            return ClaimTypes.ADMIN;
        }
        if (shovelMode == ShovelTypes.SUBDIVISION) {
            return ClaimTypes.SUBDIVISION;
        }
        if (shovelMode == ShovelTypes.TOWN) {
            return ClaimTypes.TOWN;
        }
        return ClaimTypes.BASIC;
    }

    public ClaimVisualType getVisualTypeFromShovel(ShovelType shovelMode) {
        if (shovelMode == ShovelTypes.ADMIN) {
            return ClaimVisualType.ADMINCLAIM;
        }
        if (shovelMode == ShovelTypes.SUBDIVISION) {
            return ClaimVisualType.SUBDIVISION;
        }
        if (shovelMode == ShovelTypes.TOWN) {
            return ClaimVisualType.TOWN;
        }
        return ClaimVisualType.CLAIM;
    }

    public String lookupPlayerName(String uuid) {
        if (uuid.equals(GriefDefenderPlugin.WORLD_USER_UUID.toString())) {
            return "administrator";
        }
        final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(uuid);
        if (user == null) {
            return "unknown";
        }

        return user.getName();
    }

    @Nullable
    public String getUserName(UUID uuid) {
        if (uuid.equals(GriefDefenderPlugin.PUBLIC_UUID)) {
            return "public";
        }
        if (uuid.equals(GriefDefenderPlugin.ADMIN_USER_UUID) || uuid.equals(GriefDefenderPlugin.WORLD_USER_UUID)) {
            return "administrator";
        }

        final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(uuid);
        if (user == null) {
            return null;
        }
        if (user.getName() != null) {
            return user.getName();
        }
        // check offline player
        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        if (offlinePlayer == null) {
            return null;
        }
        return offlinePlayer.getName();
    }

    public int getEyeHeight(Player player) {
        return (int) (player.getLocation().getBlockY() + player.getEyeHeight());
    }

    public void sendInteractEntityDenyMessage(GDClaim claim, Player player, ItemStack playerItem, Entity entity) {
        if (claim.getData() != null && !claim.getData().allowDenyMessages()) {
            return;
        }

        if (playerItem == null || playerItem.getType() == Material.AIR) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_INTERACT_ENTITY, ImmutableMap.of(
                    "player", claim.getOwnerName(),
                    "entity", entity.getName()));
            GriefDefenderPlugin.sendClaimDenyMessage(claim, player, message);
        } else {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_INTERACT_ITEM_ENTITY, ImmutableMap.of(
                    "item", playerItem.getType().name(),
                    "entity", entity.getName()));
            GriefDefenderPlugin.sendClaimDenyMessage(claim, player, message);
        }
    }
}
