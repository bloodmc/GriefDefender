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
import com.google.common.reflect.TypeToken;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.api.claim.ClaimTypes;
import com.griefdefender.api.claim.ShovelType;
import com.griefdefender.api.claim.ShovelTypes;
import com.griefdefender.api.permission.flag.Flags;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.api.permission.option.type.CreateModeTypes;
import com.griefdefender.api.permission.option.type.GameModeType;
import com.griefdefender.api.permission.option.type.GameModeTypes;
import com.griefdefender.api.permission.option.type.WeatherType;
import com.griefdefender.api.permission.option.type.WeatherTypes;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.internal.util.NMSUtil;
import com.griefdefender.internal.visual.ClaimVisual;
import com.griefdefender.internal.visual.GDClaimVisualType;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissionUser;

import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import org.checkerframework.checker.nullness.qual.Nullable;

public class PlayerUtil {

    private static BlockFace[] faces = { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
    public static Map<GameModeType, GameMode> GAMEMODE_MAP = ImmutableMap.of(
        GameModeTypes.ADVENTURE, GameMode.ADVENTURE,
        GameModeTypes.CREATIVE, GameMode.CREATIVE,
        GameModeTypes.SPECTATOR, GameMode.SPECTATOR,
        GameModeTypes.SURVIVAL, GameMode.SURVIVAL
    );
    public static Map<WeatherType, org.bukkit.WeatherType> WEATHERTYPE_MAP = ImmutableMap.of(
        WeatherTypes.CLEAR, org.bukkit.WeatherType.CLEAR,
        WeatherTypes.DOWNFALL, org.bukkit.WeatherType.DOWNFALL
    );

    private static PlayerUtil instance;

    public static PlayerUtil getInstance() {
        return instance;
    }

    static {
        instance = new PlayerUtil();
    }

    public BlockFace getBlockFace(Player player) {
        return faces[Math.round(player.getLocation().getYaw() / 90f) & 0x3].getOppositeFace();
    }

    public BlockFace getBlockFace(String param) {
        BlockFace face = null;
        try {
            face = BlockFace.valueOf(param.toUpperCase());
        } catch (IllegalArgumentException e) {
            // ignore
        }
        return face;
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
        if (user.getName() != null) {
            return user.getName();
        }
        // check offline player
        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        if (offlinePlayer == null) {
            return "unknown";
        }
        return offlinePlayer.getName();
    }

    public int getEyeHeight(Player player) {
        if (player == null) {
            return 0;
        }
        return (int) (player.getLocation().getBlockY() + player.getEyeHeight());
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

    public boolean isSafeLocation(Location location) {
        // check if world is loaded
        if (Bukkit.getWorld(location.getWorld().getUID()) == null) {
            return false;
        }
        final Block currentBlock = location.getBlock();
        final Block aboveBlock = currentBlock.getRelative(BlockFace.UP);
        final Block belowBlock =currentBlock.getRelative(BlockFace.DOWN);
        if (!NMSUtil.getInstance().isBlockTransparent(currentBlock)) {
            return false;
        }
        if (!NMSUtil.getInstance().isBlockTransparent(aboveBlock)) {
            return false;
        }
        if (!belowBlock.getType().isSolid()) {
            return false;
        }
        return true;
    }

    public boolean canPlayerPvP(GDClaim claim, GDPermissionUser source) {
        final Player sourcePlayer = source.getOnlinePlayer();

        Tristate sourceResult = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Tristate.class), source, Options.PVP, claim);
        if (sourceResult == Tristate.UNDEFINED) {
            sourceResult = Tristate.fromBoolean(claim.getWorld().getPVP());
        }

        if (sourceResult == Tristate.FALSE) {
            return false;
        }

        final GDClaim sourceClaim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(source.getInternalPlayerData(), sourcePlayer.getLocation());
        if (!sourceClaim.isPvpEnabled()) {
            return false;
        }
        if (!claim.isPvpEnabled()) {
            return false;
        }

        final Tristate flagResult = GDPermissionManager.getInstance().getActiveFlagPermissionValue(claim, source, Flags.ENTITY_DAMAGE, source, "minecraft:player", new HashSet<>());
        return flagResult.asBoolean();
    }
}
