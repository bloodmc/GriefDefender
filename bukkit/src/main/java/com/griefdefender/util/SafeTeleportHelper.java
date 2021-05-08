/*
 * This file is part of GriefDefender, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
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

import com.flowpowered.math.GenericMath;
import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableSet;
import com.griefdefender.internal.util.NMSUtil;
import com.griefdefender.internal.util.VecHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public final class SafeTeleportHelper {

    /** The default height radius to scan for safe locations. */
    int DEFAULT_HEIGHT = 9;

    /** The default width radius to scan for safe locations. */
    int DEFAULT_WIDTH = 9;

    /**
     * The default distance to check for a suitable floor below any candidate
     * location.
     */
    int DEFAULT_FLOOR_CHECK_DISTANCE = 2;

    private static SafeTeleportHelper instance;

    public static SafeTeleportHelper getInstance() {
        if (instance == null) {
            instance = new SafeTeleportHelper();
        }
        return instance;
    }

    public Optional<Location> getSafeLocation(Location location, int height, int width, int distanceToDrop) {
        final World world = location.getWorld();

        // Get the vectors to check, and get the block types with them.
        // The vectors should be sorted by distance from the centre of the checking region, so
        // this makes it easier to try to get close, because we can just iterate and get progressively further out.
        Optional<Vector3i> result = this.getSafeLocation(world, this.getBlockLocations(location, height, width), distanceToDrop);
        if (result.isPresent()) {
            final Vector3d pos = new Vector3d(result.get().toDouble().add(0.5, 0, 0.5));
            return Optional.of(new Location(world, pos.getX(), pos.getY(), pos.getZ()));
        }

        return Optional.empty();
    }

    private Stream<Vector3i> getBlockLocations(Location worldLocation, int height, int width) {
        // We don't want to warp outside of the world border, so we want to check that we're within it.
        int worldBorderMinX = GenericMath.floor(NMSUtil.getInstance().getWorldBorderMinX(worldLocation.getWorld()));
        int worldBorderMinZ = GenericMath.floor(NMSUtil.getInstance().getWorldBorderMinZ(worldLocation.getWorld()));
        int worldBorderMaxX = GenericMath.floor(NMSUtil.getInstance().getWorldBorderMaxX(worldLocation.getWorld()));
        int worldBorderMaxZ = GenericMath.floor(NMSUtil.getInstance().getWorldBorderMaxZ(worldLocation.getWorld()));

        // Get the World and get the maximum Y value.
        int worldMaxY = worldLocation.getWorld().getMaxHeight();

        Vector3i vectorLocation = new Vector3i(worldLocation.getBlockX(), worldLocation.getBlockY(), worldLocation.getBlockZ());

        // We use clamp to remain within the world confines, so we don't waste time checking blocks outside of the
        // world border and the world height.
        int minY = GenericMath.clamp(vectorLocation.getY() - height, 0, worldMaxY);
        int maxY = GenericMath.clamp(vectorLocation.getY() + height, 0, worldMaxY);

        int minX = GenericMath.clamp(vectorLocation.getX() - width, worldBorderMinX, worldBorderMaxX);
        int maxX = GenericMath.clamp(vectorLocation.getX() + width, worldBorderMinX, worldBorderMaxX);

        int minZ = GenericMath.clamp(vectorLocation.getZ() - width, worldBorderMinZ, worldBorderMaxZ);
        int maxZ = GenericMath.clamp(vectorLocation.getZ() + width, worldBorderMinZ, worldBorderMaxZ);

        // We now iterate over all possible x, y and z positions to get all possible vectors.
        List<Vector3i> vectors = new ArrayList<>();
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    vectors.add(new Vector3i(x, y, z));
                }
            }
        }

        Comparator<Vector3i> c = Comparator.comparingInt(vectorLocation::distanceSquared);

        // The compiler seems to need this to be a new line.
        // We check to see what the y location is, preferring changes in Y over X and Z, and higher over lower locations.
        c = c.thenComparing(x -> -Math.abs(vectorLocation.getY() - x.getY())).thenComparing(x -> -x.getY());

        // Sort them according to the distance to the provided worldLocation.
        return vectors.stream().sorted(c);
    }

    private Optional<Vector3i> getSafeLocation(World world, Stream<Vector3i> positionsToCheck, int floorDistanceCheck) {
        // We cache the various block lookup results so we don't check a block twice.
        final Map<Vector3i, BlockData> blockCache = new HashMap<>();

        return positionsToCheck.filter(currentTarget -> {
            // Get the block, add it to the cache.
            BlockData block = this.getBlockData(currentTarget, world, blockCache);

            // If the block isn't safe, no point in continuing on this run.
            if (block.isSafeBody) {

                // Check the block ABOVE is safe for the body, and the two BELOW are safe too.
                if (this.getBlockData(
                    currentTarget.add(0, 1, 0), world, blockCache).isSafeBody
                        && (floorDistanceCheck <= 0 || this.isFloorSafe(currentTarget, world, blockCache, floorDistanceCheck))) {

                    // This position should be safe. Get the center of the block to spawn into.
                    return true;
                }
            }

            return false;
        }).findFirst();
    }

    private boolean isFloorSafe(Vector3i currentTarget, World world, Map<Vector3i, BlockData> blockCache, int floorDistanceCheck) {
        for (int i = 1; i < floorDistanceCheck; ++i) {
            BlockData data = this.getBlockData(currentTarget.sub(0, i, 0), world, blockCache);

            // If it's a safe floor, we can just say yes now.
            if (data.isSafeFloor) {
                return true;
            }

            // If it's not safe for the body, then we don't want to go through it anyway.
            if (!data.isSafeBody) {
                return false;
            }
        }

        // Check the next block down, if it's a floor, then we're good to go, otherwise we'd fall too far for our liking.
        return this.getBlockData(currentTarget.sub(0, floorDistanceCheck, 0), world, blockCache).isSafeFloor;
    }

    private BlockData getBlockData(Vector3i vector3i, World world, Map<Vector3i, BlockData> cache) {
        if (vector3i.getY() < 0) {
            // Anything below this isn't safe, no point going further.
            return new BlockData();
        }

        if (cache.containsKey(vector3i)) {
            return cache.get(vector3i);
        }

        BlockData data = new BlockData(world.getBlockAt(VecHelper.toLocation(world, vector3i)));
        cache.put(vector3i, data);
        return data;
    }

    private class BlockData {

        private final Set<Material> NOT_SAFE_FLOOR = ImmutableSet.of(Material.AIR, Material.CACTUS, Material.FIRE, Material.LAVA);

        private final boolean isSafeFloor;
        private final boolean isSafeBody;

        private BlockData() {
            this.isSafeFloor = false;
            this.isSafeBody = false;
        }

        private BlockData(Block blockState) {
            this.isSafeFloor = isSafeFloorMaterial(blockState);
            this.isSafeBody = isSafeBodyMaterial(blockState);
        }

        private boolean isSafeFloorMaterial(Block block) {
            return !NOT_SAFE_FLOOR.contains(block.getType());
        }

        private boolean isSafeBodyMaterial(Block block) {
            Material material = block.getType();

            // Deny blocks that suffocate
            if (block.getType().isOccluding()) {
                return false;
            }
            // Deny dangerous lava
            if (material == Material.LAVA) {
                return false;
            }

            // Deny non-passable non "full" blocks
            return NMSUtil.getInstance().isBlockTransparent(block);
        }
    }
}