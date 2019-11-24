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
import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.api.claim.ClaimTypes;
import com.griefdefender.api.claim.ShovelType;
import com.griefdefender.api.claim.ShovelTypes;
import com.griefdefender.api.permission.option.type.CreateModeTypes;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.internal.visual.ClaimVisual;
import com.griefdefender.internal.visual.GDClaimVisualType;
import com.griefdefender.permission.GDPermissionUser;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.property.entity.EyeLocationProperty;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.util.Direction;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

public class PlayerUtil {

    private static Direction[] faces = { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    private static PlayerUtil instance;

    public static PlayerUtil getInstance() {
        return instance;
    }

    static {
        instance = new PlayerUtil();
    }

    public Direction getBlockFace(Player player) {
        return faces[Math.round((int) player.getTransform().getYaw() / 90f) & 0x3].getOpposite();
    }

    public Direction getBlockFace(String param) {
        Direction face = null;
        try {
            face = Direction.valueOf(param.toUpperCase());
        } catch (IllegalArgumentException e) {
            // ignore
        }
        return face;
    }

    public boolean hasItemInOneHand(Player player, ItemType itemType) {
        ItemStack mainHand = player.getItemInHand(HandTypes.MAIN_HAND).orElse(null);
        ItemStack offHand = player.getItemInHand(HandTypes.OFF_HAND).orElse(null);
        if ((mainHand != null && mainHand.getType().equals(itemType)) || (offHand != null && offHand.getType().equals(itemType))) {
            return true;
        }

        return false;
    }

    public boolean hasItemInOneHand(Player player) {
        ItemStack mainHand = player.getItemInHand(HandTypes.MAIN_HAND).orElse(null);
        ItemStack offHand = player.getItemInHand(HandTypes.OFF_HAND).orElse(null);
        if (mainHand != null || offHand != null) {
            return true;
        }

        return false;
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

    public Component getClaimTypeComponentFromShovel(ShovelType shovelMode) {
        return getClaimTypeComponentFromShovel(shovelMode, true);
    }

    public Component getClaimTypeComponentFromShovel(ShovelType shovelMode, boolean upper) {
        if (shovelMode == ShovelTypes.ADMIN) {
            if (upper) {
                return TextComponent.of(ClaimTypes.ADMIN.getName().toUpperCase(), TextColor.RED);
            }
            return TextComponent.of("Admin", TextColor.RED);
        }

        if (shovelMode == ShovelTypes.TOWN) {
            if (upper) {
                return TextComponent.of(ClaimTypes.TOWN.getName().toUpperCase(), TextColor.GREEN);
            }
            return TextComponent.of("Town", TextColor.GREEN);
        }

        if (shovelMode == ShovelTypes.SUBDIVISION) {
            if (upper) {
                return TextComponent.of(ClaimTypes.SUBDIVISION.getName().toUpperCase(), TextColor.AQUA);
            }
            return TextComponent.of("Subdivision", TextColor.AQUA);
        }
        if (upper) {
            return TextComponent.of(ClaimTypes.BASIC.getName().toUpperCase(), TextColor.YELLOW);
        }
        return TextComponent.of("Basic", TextColor.YELLOW);
    }

    public GDClaimVisualType getVisualTypeFromShovel(ShovelType shovelMode) {
        if (shovelMode == ShovelTypes.ADMIN) {
            return ClaimVisual.ADMIN;
        }
        if (shovelMode == ShovelTypes.SUBDIVISION) {
            return ClaimVisual.SUBDIVISION;
        }
        if (shovelMode == ShovelTypes.TOWN) {
            return ClaimVisual.TOWN;
        }
        return ClaimVisual.BASIC;
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
            return "unknown";
        }

        return user.getName();
    }

    public String getPlayerName(String uuid) {
        if (uuid.equals(GriefDefenderPlugin.WORLD_USER_UUID.toString())) {
            return "administrator";
        }
        if (uuid.equals(GriefDefenderPlugin.ADMIN_USER_UUID.toString()) || uuid.equals(GriefDefenderPlugin.WORLD_USER_UUID.toString())) {
            return "administrator";
        }

        Optional<User> user = Sponge.getGame().getServiceManager().provide(UserStorageService.class).get().get(UUID.fromString(uuid));
        if (!user.isPresent()) {
            return "unknown";
        }

        return user.get().getName();
    }

    public int getEyeHeight(Player player) {
        return player.getProperty(EyeLocationProperty.class).get().getValue().getFloorY();
    }

    public int getVisualClaimHeight(GDPlayerData playerData, int height) {
        if (playerData.getClaimCreateMode() == CreateModeTypes.VOLUME) {
            return height;
        }
        if (playerData.getMaxClaimLevel() < 255) {
            return playerData.getMaxClaimLevel();
        }
        return 0;
    }
}
