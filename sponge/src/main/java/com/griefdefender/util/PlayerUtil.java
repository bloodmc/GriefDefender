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
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.api.claim.ClaimTypes;
import com.griefdefender.api.claim.ClaimVisualType;
import com.griefdefender.api.claim.ClaimVisualTypes;
import com.griefdefender.api.claim.ShovelType;
import com.griefdefender.api.claim.ShovelTypes;
import com.griefdefender.api.permission.option.type.CreateModeTypes;
import com.griefdefender.api.permission.option.type.GameModeType;
import com.griefdefender.api.permission.option.type.GameModeTypes;
import com.griefdefender.api.permission.option.type.WeatherType;
import com.griefdefender.api.permission.option.type.WeatherTypes;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.internal.util.NMSUtil;
import com.griefdefender.permission.GDPermissionUser;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.property.entity.EyeLocationProperty;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nullable;

public class PlayerUtil {

    private static Direction[] faces = { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    public static BiMap<GameModeType, GameMode> GAMEMODE_MAP = ImmutableBiMap.of(
        GameModeTypes.ADVENTURE, GameModes.ADVENTURE,
        GameModeTypes.CREATIVE, GameModes.CREATIVE,
        GameModeTypes.SPECTATOR, GameModes.SPECTATOR,
        GameModeTypes.SURVIVAL, GameModes.SURVIVAL
    );
    public static BiMap<WeatherType, org.spongepowered.api.world.weather.Weather> WEATHERTYPE_MAP = ImmutableBiMap.of(
        WeatherTypes.CLEAR, org.spongepowered.api.world.weather.Weathers.CLEAR,
        WeatherTypes.DOWNFALL, org.spongepowered.api.world.weather.Weathers.RAIN
    );

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

    public boolean shouldRefreshVisual(GDPlayerData playerData, Location<World> locality, Set<Transaction<BlockSnapshot>> corners, Set<Transaction<BlockSnapshot>> accents) {
        if (corners.isEmpty()) {
            return true;
        }
        if (playerData.lastNonAirInspectLocation == null && !corners.isEmpty()) {
            return false;
        }
        Integer lowestY = null;
        Integer highestY = null;
        if (!corners.isEmpty() && playerData != null && playerData.lastNonAirInspectLocation != null) {
            for (Transaction<BlockSnapshot> transaction : corners) {
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
            for (Transaction<BlockSnapshot> transaction : accents) {
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
            if (!playerData.inLiquid && NMSUtil.getInstance().isBlockLiquid(playerData.lastNonAirInspectLocation.getBlock())) {
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

    public GDClaim findNearbyClaim(Player player, GDPlayerData playerData, int maxDistance, boolean hidingVisuals) {
        if (maxDistance <= 20) {
            maxDistance = 100;
        }

        BlockRay<World> blockRay = BlockRay.from(player).distanceLimit(maxDistance).build();
        GDClaim playerClaim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(playerData, player.getLocation());
        GDClaim firstClaim = null;
        GDClaim claim = null;
        playerData.lastNonAirInspectLocation = null;
        playerData.lastValidInspectLocation = null;
        while (blockRay.hasNext()) {
            BlockRayHit<World> blockRayHit = blockRay.next();
            Location<World> location = blockRayHit.getLocation();
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

            if (playerData.lastNonAirInspectLocation == null && !location.getBlockType().equals(BlockTypes.AIR)) {
                playerData.lastNonAirInspectLocation = location;
            }
            if (claim != null && !claim.isWilderness() && !playerClaim.getUniqueId().equals(claim.getUniqueId())) {
                playerData.lastValidInspectLocation = location;
            }

            if (!location.getBlockType().equals(BlockTypes.AIR) && !NMSUtil.getInstance().isBlockTransparent(location.getBlock())) {
                break;
            }
        }

        if (claim == null || claim.isWilderness()) {
            if (firstClaim == null) {
                return GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(player.getWorld().getUniqueId()).getWildernessClaim();
            }
            return firstClaim;
        }

        return claim;
    }

    public Location<World> getSafeClaimLocation(GDClaim claim) {
        final int minClaimLevel = claim.getOwnerMinClaimLevel();
        double claimY = claim.getOwnerPlayerData() == null ? 65.0D : (minClaimLevel > 65.0D ? minClaimLevel : 65);
        if (claim.isCuboid()) {
            claimY = claim.lesserBoundaryCorner.getY();
        }

        final int random = ThreadLocalRandom.current().nextInt(2, 20 + 1);
        final int randomCorner = ThreadLocalRandom.current().nextInt(1, 4 + 1);
        Location<World> claimCorner = null;
        switch (randomCorner) {
            case 1: // SW
                claimCorner = new Location<>(claim.getWorld(), claim.lesserBoundaryCorner.getX() - random, claimY, claim.greaterBoundaryCorner.getZ() + random);
            case 2: // NW
                claimCorner = new Location<>(claim.getWorld(), claim.lesserBoundaryCorner.getX() - random, claimY, claim.lesserBoundaryCorner.getZ() - random);
            case 3: // SE
                claimCorner = new Location<>(claim.getWorld(), claim.greaterBoundaryCorner.getX() + random, claimY, claim.greaterBoundaryCorner.getZ() + random);
            case 4: // NE
                claimCorner = new Location<>(claim.getWorld(), claim.greaterBoundaryCorner.getX() + random, claimY, claim.lesserBoundaryCorner.getZ() - random);
        }

        final Location<World> safeLocation = Sponge.getTeleportHelper().getSafeLocation(claimCorner, 64, 16, 2).orElse(null);
        if (safeLocation != null) {
            return safeLocation;
        }

        // If no safe location was found, fall back to corner
        return claimCorner;
    }

    public boolean forceItemInteract(ItemType itemType, Player player) {
        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        if (playerData.claimMode) {
            return true;
        }
        if (!playerData.claimTool) {
            return GriefDefenderPlugin.getGlobalConfig().getConfig().mod.forceItemInteract(itemType.getId());
        }
        if (GriefDefenderPlugin.getInstance().modificationTool != null && itemType.getId().equals(GriefDefenderPlugin.getInstance().modificationTool)) {
            return true;
        }
        if (GriefDefenderPlugin.getInstance().investigationTool != null && itemType.getId().equals(GriefDefenderPlugin.getInstance().investigationTool)) {
            return true;
        }
        return GriefDefenderPlugin.getGlobalConfig().getConfig().mod.forceItemInteract(itemType.getId());
    }
}
