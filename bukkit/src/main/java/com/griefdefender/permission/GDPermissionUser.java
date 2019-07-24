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

import me.lucko.luckperms.api.User;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

import javax.annotation.Nullable;

public class GDPermissionUser extends GDPermissionHolder {

    private String userName;
    private UUID uniqueId;
    private OfflinePlayer offlinePlayer;

    public GDPermissionUser(Player player) {
        super(player.getUniqueId().toString());
        this.uniqueId = player.getUniqueId();
        this.offlinePlayer = player;
        this.userName = player.getName();
    }

    public GDPermissionUser(User user) {
        super(user.getObjectName());
        this.userName = user.getName();
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
            this.userName = this.getOfflinePlayer().getName();
            if (this.userName == null) {
                // fallback to LP
                this.userName = super.getFriendlyName();
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

    public User getLuckPermsUser() {
        return (User) this.getLuckPermsHolder();
    }

    public UUID getUniqueId() {
        return this.uniqueId;
    }
}
