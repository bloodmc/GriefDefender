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

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.api.claim.ClaimTypes;
import com.griefdefender.api.claim.ClaimVisualType;
import com.griefdefender.api.claim.ClaimVisualTypes;
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
import com.griefdefender.internal.block.BlockTransaction;
import com.griefdefender.internal.util.NMSUtil;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.option.GDOptions;

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
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.checkerframework.checker.nullness.qual.Nullable;

public class PlayerUtil {

    private static BlockFace[] faces = { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
    public static BiMap<GameModeType, GameMode> GAMEMODE_MAP = ImmutableBiMap.of(
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

    public ClaimVisualType getVisualTypeFromShovel(ShovelType shovelMode) {
        if (shovelMode == ShovelTypes.ADMIN) {
            return ClaimVisualTypes.ADMIN;
        }
        if (shovelMode == ShovelTypes.SUBDIVISION) {
            return ClaimVisualTypes.SUBDIVISION;
        }
        if (shovelMode == ShovelTypes.TOWN) {
            return ClaimVisualTypes.TOWN;
        }
        return ClaimVisualTypes.BASIC;
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
                    "player", claim.getOwnerDisplayName(),
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
        if (!claim.getWorld().getPVP()) {
            return false;
        }

        final Tristate flagResult = GDPermissionManager.getInstance().getActiveFlagPermissionValue(claim, source, Flags.ENTITY_DAMAGE, source, "minecraft:player", new HashSet<>());
        if (flagResult == Tristate.FALSE) {
            return false;
        }

        Tristate sourceResult = Tristate.UNDEFINED;
        if (GDOptions.PVP) {
            sourceResult = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Tristate.class), source, Options.PVP, claim);
        }
        if (sourceResult == Tristate.FALSE) {
            return false;
        }

        return true;
    }

    public boolean shouldRefreshVisual(GDPlayerData playerData, Location locality, Set<BlockTransaction> corners, Set<BlockTransaction> accents) {
        if (corners.isEmpty()) {
            return true;
        }
        if (playerData.lastNonAirInspectLocation == null && !corners.isEmpty()) {
            return false;
        }
        Integer lowestY = null;
        Integer highestY = null;
        if (!corners.isEmpty() && playerData != null && playerData.lastNonAirInspectLocation != null) {
            for (BlockTransaction transaction : corners) {
                final int cornerHeight = transaction.getFinal().getPosition().getY();
                if (lowestY == null || (lowestY != null && cornerHeight < lowestY)) {
                    lowestY = cornerHeight;
                }
                if (highestY == null || (highestY != null && cornerHeight > highestY)) {
                    highestY = cornerHeight;
                }
            }
        }
        if (!accents.isEmpty() && playerData != null && playerData.lastNonAirInspectLocation != null) {
            for (BlockTransaction transaction : accents) {
                final int cornerHeight = transaction.getFinal().getPosition().getY();
                if (lowestY == null || (lowestY != null && cornerHeight < lowestY)) {
                    lowestY = cornerHeight;
                }
                if (highestY == null || (highestY != null && cornerHeight > highestY)) {
                    highestY = cornerHeight;
                }
            }
        }
        if (lowestY != null) {
            // check if water
            if (!playerData.inLiquid && playerData.lastNonAirInspectLocation.getBlock().isLiquid()) {
                if (highestY < playerData.lastNonAirInspectLocation.getBlockY()) {
                    return true;
                }
                return false;
            }
            if (locality.getBlockY() < lowestY) {
                return true;
            }
        }
        return false;
    }

    public boolean isFakePlayer(Player player) {
        if (player == null) {
            return false;
        }

        return GriefDefenderPlugin.getGlobalConfig().getConfig().mod.isFakePlayer(player);
    }

    public GDClaim findNearbyClaim(Player player, GDPlayerData playerData, int maxDistance, boolean hidingVisuals) {
        if (maxDistance <= 20) {
            maxDistance = 100;
        }
        BlockRay blockRay = BlockRay.from(player).distanceLimit(maxDistance).build();
        GDClaim playerClaim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(playerData, player.getLocation());
        GDClaim firstClaim = null;
        GDClaim claim = null;
        playerData.lastNonAirInspectLocation = null;
        playerData.lastValidInspectLocation = null;
        while (blockRay.hasNext()) {
            BlockRayHit blockRayHit = blockRay.next();
            Location location = blockRayHit.getLocation();
            claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAt(location);
            if (firstClaim == null && !claim.isWilderness()) {
                if (hidingVisuals) {
                    if (claim.hasActiveVisual(player)) {
                        firstClaim = claim;
                    }
                } else {
                    firstClaim = claim;
                }
            }

            if (playerData.lastNonAirInspectLocation == null && !location.getBlock().isEmpty()) {
                playerData.lastNonAirInspectLocation = location;
            }
            if (claim != null && !claim.isWilderness() && !playerClaim.getUniqueId().equals(claim.getUniqueId())) {
                playerData.lastValidInspectLocation = location;
            }

            final Block block = location.getBlock();
            if (!block.isEmpty() && !NMSUtil.getInstance().isBlockTransparent(block)) {
                break;
            }
        }

        if (claim == null || claim.isWilderness()) {
            if (firstClaim == null) {
                return GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(player.getWorld().getUID()).getWildernessClaim();
            }
            return firstClaim;
        }

        return claim;
    }

    public Location getSafeClaimLocation(GDClaim claim) {
        final int minClaimLevel = claim.getOwnerMinClaimLevel();
        double claimY = claim.getOwnerPlayerData() == null ? 65.0D : (minClaimLevel > 65.0D ? minClaimLevel : 65);
        if (claim.isCuboid()) {
            claimY = claim.lesserBoundaryCorner.getY();
        }

        final int random = ThreadLocalRandom.current().nextInt(2, 20 + 1);
        final int randomCorner = ThreadLocalRandom.current().nextInt(1, 4 + 1);
        Location claimCorner = null;
        switch (randomCorner) {
            case 1: // SW
                claimCorner = new Location(claim.getWorld(), claim.lesserBoundaryCorner.getX() - random, claimY, claim.greaterBoundaryCorner.getZ() + random);
            case 2: // NW
                claimCorner = new Location(claim.getWorld(), claim.lesserBoundaryCorner.getX() - random, claimY, claim.lesserBoundaryCorner.getZ() - random);
            case 3: // SE
                claimCorner = new Location(claim.getWorld(), claim.greaterBoundaryCorner.getX() + random, claimY, claim.greaterBoundaryCorner.getZ() + random);
            case 4: // NE
                claimCorner = new Location(claim.getWorld(), claim.greaterBoundaryCorner.getX() + random, claimY, claim.lesserBoundaryCorner.getZ() - random);
        }

        final Location safeLocation = SafeTeleportHelper.getInstance().getSafeLocation(claimCorner, 64, 16, 2).orElse(null);
        if (safeLocation != null) {
            return safeLocation;
        }

        // If no safe location was found, fall back to corner
        return claimCorner;
    }
}
