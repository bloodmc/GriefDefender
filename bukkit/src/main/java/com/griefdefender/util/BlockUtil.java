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

import com.flowpowered.math.vector.Vector3i;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimBlockSystem;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.claim.GDClaimManager;
import com.griefdefender.internal.util.NMSUtil;
import com.griefdefender.internal.util.VecHelper;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class BlockUtil {

    private static BlockUtil instance;

    static {
        instance = new BlockUtil();
    }

    public static BlockUtil getInstance() {
        return instance;
    }

    public static final Direction[] CARDINAL_SET = {
            Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
        };
    public static final Direction[] ORDINAL_SET = {
            Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST,
            Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST,
        };
    public static final Map<BlockFace, Direction> blockFaceToDirectionMap = new HashMap<BlockFace, Direction>()
    {{
         put(BlockFace.DOWN, Direction.DOWN);
         put(BlockFace.EAST, Direction.EAST);
         put(BlockFace.EAST_NORTH_EAST, Direction.EAST_NORTHEAST);
         put(BlockFace.EAST_SOUTH_EAST, Direction.EAST_SOUTHEAST);
         put(BlockFace.NORTH, Direction.NORTH);
         put(BlockFace.NORTH_EAST, Direction.NORTHEAST);
         put(BlockFace.NORTH_NORTH_EAST, Direction.NORTH_NORTHEAST);
         put(BlockFace.NORTH_NORTH_WEST, Direction.NORTH_NORTHWEST);
         put(BlockFace.NORTH_WEST, Direction.NORTHWEST);
         put(BlockFace.SOUTH, Direction.SOUTH);
         put(BlockFace.SOUTH_EAST, Direction.SOUTHEAST);
         put(BlockFace.SOUTH_SOUTH_EAST, Direction.SOUTH_SOUTHEAST);
         put(BlockFace.SOUTH_SOUTH_WEST, Direction.SOUTH_SOUTHWEST);
         put(BlockFace.SOUTH_WEST, Direction.SOUTHWEST);
         put(BlockFace.UP, Direction.UP);
         put(BlockFace.WEST, Direction.WEST);
         put(BlockFace.WEST_NORTH_WEST, Direction.WEST_NORTHWEST);
         put(BlockFace.WEST_SOUTH_WEST, Direction.WEST_SOUTHWEST);
         put(BlockFace.SELF, Direction.NONE);
    }};
    private static final int NUM_XZ_BITS = 4;
    private static final int NUM_SHORT_Y_BITS = 8;
    private static final short XZ_MASK = 0xF;
    private static final short Y_SHORT_MASK = 0xFF;
    private static final Vector3i CHUNK_SIZE = new Vector3i(16, 256, 16);

    public String posToString(Location location) {
        return posToString(VecHelper.toVector3i(location));
    }

    public String posToString(Vector3i pos) {
        return posToString(pos.getX(), pos.getY(), pos.getZ());
    }

    public String posToString(int x, int y, int z) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(x);
        stringBuilder.append(";");
        stringBuilder.append(y);
        stringBuilder.append(";");
        stringBuilder.append(z);

        return stringBuilder.toString();
    }

    public Vector3i posFromString(String string) {
        String[] elements = string.split(";");

        if (elements.length < 3) {
            return null;
        }

        String xString = elements[0];
        String yString = elements[1];
        String zString = elements[2];

        int x = Integer.parseInt(xString);
        int y = Integer.parseInt(yString);
        int z = Integer.parseInt(zString);

        return new Vector3i(x, y, z);
    }

    public boolean clickedClaimCorner(GDClaim claim, Vector3i clickedPos) {
        int clickedX = clickedPos.getX();
        int clickedY = clickedPos.getY();
        int clickedZ = clickedPos.getZ();
        int lesserX = claim.getLesserBoundaryCorner().getX();
        int lesserY = claim.getLesserBoundaryCorner().getY();
        int lesserZ = claim.getLesserBoundaryCorner().getZ();
        int greaterX = claim.getGreaterBoundaryCorner().getX();
        int greaterY = claim.getGreaterBoundaryCorner().getY();
        int greaterZ = claim.getGreaterBoundaryCorner().getZ();
        if ((clickedX == lesserX || clickedX == greaterX) && (clickedZ == lesserZ || clickedZ == greaterZ)
                && (!claim.isCuboid() || (clickedY == lesserY || clickedY == greaterY))) {
            return true;
        }

        return false;
    }

    public int getClaimBlockCost(World world, Vector3i point1, Vector3i point2, boolean cuboid) {
        int minx = Math.min(point1.getX(), point2.getX());
        int miny = Math.min(point1.getY(), point2.getY());
        int minz = Math.min(point1.getZ(), point2.getZ());
        int maxx = Math.max(point1.getX(), point2.getX());
        int maxy = Math.max(point1.getY(), point2.getY());
        int maxz = Math.max(point1.getZ(), point2.getZ());

        final int claimWidth = Math.abs(maxx - minx) + 1;
        final int claimHeight = Math.abs(maxy - miny) + 1;
        final int claimLength = Math.abs(maxz - minz) + 1;
        if (GriefDefenderPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.AREA) {
            return claimWidth * claimLength;
        }

        return claimLength * claimWidth * claimHeight;
    }

    public long asLong(int x, int z) {
        return (long) x & 0xffffffffL | ((long) z & 0xffffffffL) << 32;
    }

    public short blockPosToShort(Location location) {
        short serialized = (short) setNibble(0, location.getBlockX() & XZ_MASK, 0, NUM_XZ_BITS);
        serialized = (short) setNibble(serialized, location.getBlockY() & Y_SHORT_MASK, 1, NUM_SHORT_Y_BITS);
        serialized = (short) setNibble(serialized, location.getBlockZ() & XZ_MASK, 3, NUM_XZ_BITS);
        return serialized;
    }

    /**
     * Serialize this BlockPos into a short value
     */
    public short blockPosToShort(Vector3i pos) {
        short serialized = (short) setNibble(0, pos.getX() & XZ_MASK, 0, NUM_XZ_BITS);
        serialized = (short) setNibble(serialized, pos.getY() & Y_SHORT_MASK, 1, NUM_SHORT_Y_BITS);
        serialized = (short) setNibble(serialized, pos.getZ() & XZ_MASK, 3, NUM_XZ_BITS);
        return serialized;
    }

    private int setNibble(int num, int data, int which, int bitsToReplace) {
        return (num & ~(bitsToReplace << (which * 4)) | (data << (which * 4)));
    }

    private int directionToIndex(Direction direction) {
        switch (direction) {
            case NORTH:
            case NORTHEAST:
            case NORTHWEST:
                return 0;
            case SOUTH:
            case SOUTHEAST:
            case SOUTHWEST:
                return 1;
            case EAST:
                return 2;
            case WEST:
                return 3;
            default:
                throw new IllegalArgumentException("Unexpected direction");
        }
    }

    public Optional<Location> getTargetBlock(Player player, GDPlayerData playerData, int maxDistance, boolean ignoreAir) throws IllegalStateException {
        BlockRay blockRay = BlockRay.from(player).distanceLimit(maxDistance).build();
        boolean waterIsTransparent = false;
        if (player != null && NMSUtil.getInstance().isBlockWater(player.getLocation().getBlock())) {
            waterIsTransparent = true;
        }

        while (blockRay.hasNext()) {
            BlockRayHit blockRayHit = blockRay.next();
            final Block block = blockRayHit.getLocation().getBlock();
            boolean blockTransparent = NMSUtil.getInstance().isBlockTransparent(block);
            final boolean isWaterBlock = NMSUtil.getInstance().isBlockWater(block);
            if (waterIsTransparent && isWaterBlock) {
                continue;
            } else if (isWaterBlock) {
                blockTransparent = false;
            }

            if (ignoreAir) {
                if (!blockTransparent) {
                    return Optional.of(blockRayHit.getLocation());
                }
            } else {
                if (!block.isEmpty() && !blockTransparent) {
                    return Optional.of(blockRayHit.getLocation());
                }
            }
        }

        return Optional.empty();
    }

    public Vector3i getBlockRelative(Vector3i pos, Direction direction) {
        final Vector3i offset = direction.asBlockOffset();
        return pos.add(offset);
    }

    public Location getBlockRelative(Location location, BlockFace face) {
        return getBlockRelative(location, blockFaceToDirectionMap.get(face));
    }

    public Location getBlockRelative(Location location, BlockFace face, int distance) {
        int count = 0;
        Location relativeLocation = null;
        Location sourceLocation = location.clone();
        while (count < distance) {
            relativeLocation = getBlockRelative(sourceLocation, blockFaceToDirectionMap.get(face));
            sourceLocation = relativeLocation;
            count++;
        }
        return relativeLocation;
    }

    public Location getBlockRelative(Location location, Direction direction) {
        final Vector3i offset = direction.asBlockOffset();
        // We must clone here as Bukkit's location is mutable
        location = location.clone();
        return location.add(offset.getX(), offset.getY(), offset.getZ());
    }

    public Set<Claim> getNearbyClaims(Location location, int blockDistance, boolean includeChildren) {
        Set<Claim> claims = new HashSet<>();
        GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(location.getWorld().getUID());
        if (claimWorldManager == null) {
            return claims;
        }

        final World world = location.getWorld();
        org.bukkit.Chunk lesserChunk = location.getWorld().getChunkAt((location.getBlockX() - blockDistance) >> 4, (location.getBlockZ() - blockDistance) >> 4);
        org.bukkit.Chunk greaterChunk = location.getWorld().getChunkAt((location.getBlockX() + blockDistance) >> 4, (location.getBlockZ() + blockDistance) >> 4);

        if (lesserChunk != null && greaterChunk != null) {
            for (int chunkX = lesserChunk.getX(); chunkX <= greaterChunk.getX(); chunkX++) {
                for (int chunkZ = lesserChunk.getZ(); chunkZ <= greaterChunk.getZ(); chunkZ++) {
                    if (!world.isChunkLoaded(chunkX, chunkZ)) {
                        continue;
                    }
                    org.bukkit.Chunk chunk = location.getWorld().getChunkAt(chunkX, chunkZ);
                    if (chunk != null) {
                        Set<Claim> claimsInChunk = claimWorldManager.getInternalChunksToClaimsMap().get(NMSUtil.getInstance().getChunkCoordIntPair(chunkX, chunkZ));
                        if (claimsInChunk != null) {
                            for (Claim claim : claimsInChunk) {
                                final GDClaim gdClaim = (GDClaim) claim;
                                if (!includeChildren) {
                                    if (gdClaim.parent == null && !claims.contains(claim)) {
                                        claims.add(claim);
                                    }
                                } else if (!claims.contains(claim)){
                                    claims.add(claim);
                                    for (Claim child : gdClaim.getChildren(true)) {
                                        claims.add(child);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return claims;
    }

    public Vector3i getChunkMin(org.bukkit.Chunk chunk) {
        return new Vector3i(chunk.getX() << 4, 0 << 8, chunk.getZ() << 4);
    }

    public Vector3i getChunkMax(Vector3i min, org.bukkit.Chunk chunk) {
        return min.add(CHUNK_SIZE).sub(1, 1, 1);
    }

    public Vector3i getChunkMax(org.bukkit.Chunk chunk) {
        return getChunkMin(chunk).add(CHUNK_SIZE).sub(1, 1, 1);
    }

    public boolean isChunkLoaded(World world, int cx, int cz) {
        return world.isChunkLoaded(cx, cz);
    }

    public boolean isChunkLoaded(Location location) {
        return this.isChunkLoadedAtBlock(location.getWorld(), location.getBlockX(), location.getBlockZ());
    }

    public boolean isChunkLoadedAtBlock(World world, int x, int z) {
        return world.isChunkLoaded(x >> 4, z >> 4);
    }
}