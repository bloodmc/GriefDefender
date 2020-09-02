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
package com.griefdefender.permission;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.User;
import com.griefdefender.api.data.PlayerData;

import java.util.UUID;

import org.checkerframework.checker.nullness.qual.Nullable;

public class GDPermissionUser extends GDPermissionHolder implements User {

    private String userName;
    private UUID uniqueId;
    private UUID worldUniqueId;
    private OfflinePlayer offlinePlayer;
    private GDPlayerData playerData;

    public GDPermissionUser(Player player) {
        super(player.getUniqueId().toString());
        this.uniqueId = player.getUniqueId();
        this.worldUniqueId = player.getWorld().getUID();
        this.offlinePlayer = player;
        this.userName = player.getName();
    }

    public GDPermissionUser(OfflinePlayer player) {
        super(player.getUniqueId().toString());
        this.uniqueId = player.getUniqueId();
        if (player instanceof Player) {
            // OfflinePlayer getName returns null sometimes
            // Always use Player getName if available
            this.userName = ((Player) player).getName();
        } else {
            this.userName = player.getName();
        }
    }

    public GDPermissionUser(UUID uuid, String objectName, String friendlyName) {
        super(objectName);
        this.uniqueId = uuid;
        this.userName = objectName;
    }

    public GDPermissionUser(UUID uuid) {
        super(uuid.toString());
        this.uniqueId = uuid;
    }

    // Used for Public/World user
    public GDPermissionUser(UUID uuid, String name) {
        super(uuid.toString());
        this.uniqueId = uuid;
        this.userName = name;
    }

    public String getName() {
        if (this.userName == null) {
            if (this.uniqueId.equals(GriefDefenderPlugin.PUBLIC_UUID)) {
                this.userName = "public";
            } else if (this.uniqueId.equals(GriefDefenderPlugin.ADMIN_USER_UUID) || this.uniqueId.equals(GriefDefenderPlugin.WORLD_USER_UUID)) {
                this.userName = "administrator";
            } else if (this.getOfflinePlayer() != null) {
                this.userName = this.getOfflinePlayer().getName();
            } else {
                // fallback to LP
                this.userName = super.getFriendlyName();
            }
            if (this.userName == null) {
                this.userName = "unknown";
            }
        }

        return this.userName;
    }

    public String getFriendlyName() {
        return this.getName();
    }

    @Nullable
    public Player getOnlinePlayer() {
        return Bukkit.getPlayer(this.uniqueId);
    }

    public OfflinePlayer getOfflinePlayer() {
        final OfflinePlayer player = this.getOnlinePlayer();
        if (player != null) {
            return player;
        }

        if (this.offlinePlayer == null) {
            this.offlinePlayer = Bukkit.getOfflinePlayer(this.uniqueId);
        }
        return this.offlinePlayer;
    }

    @Override
    public UUID getUniqueId() {
        return this.uniqueId;
    }

    @Override
    public PlayerData getPlayerData() {
        if (this.playerData == null) {
            if (this.worldUniqueId != null) {
                this.playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(this.worldUniqueId, this.uniqueId);
            } else {
                this.playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreateGlobalPlayerData(this.uniqueId);
            }
        }
        return this.playerData;
    }

    public GDPlayerData getInternalPlayerData() {
        return (GDPlayerData) this.getPlayerData();
    }

    @Override
    public boolean isOnline() {
        return this.getOnlinePlayer() != null;
    }
}
