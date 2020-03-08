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
package com.griefdefender.internal.util;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.Maps;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimBlockSystem;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.claim.GDClaimManager;
import com.griefdefender.util.BlockPosCache;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketBlockChange;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.property.block.MatterProperty;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.LocatableBlock;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.common.bridge.world.chunk.ChunkBridge;
import org.spongepowered.common.bridge.world.chunk.ChunkProviderBridge;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    private static final int NUM_XZ_BITS = 4;
    private static final int NUM_SHORT_Y_BITS = 8;
    private static final short XZ_MASK = 0xF;
    private static final short Y_SHORT_MASK = 0xFF;

    public static final Map<Integer, BlockPosCache> ENTITY_BLOCK_CACHE = new Int2ObjectArrayMap<>();
    private static final Map<BlockState, Integer> BLOCKSTATE_META_CACHE = Maps.newHashMap();
    public static boolean POPULATING_CHUNK = false;

    public String posToString(Location<World> location) {
        return posToString(location.getBlockPosition());
    }

    public String posToString(Vector3i pos) {
        return posToString(pos.getX(), pos.getY(), pos.getZ());
    }

    public String posToString(int x, int y, int z) {
        return x + ";" + y + ";" + z;
    }

    public Vector3i posFromString(String pos) throws Exception {
        String[] elements = pos.split(";");

        if (elements.length < 3) {
            throw new Exception("Invalid position " + pos + "");
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

    public int getBlockStateMeta(BlockState state) {
        Integer meta = BLOCKSTATE_META_CACHE.get(state);
        if (meta == null) {
            Block mcBlock = (net.minecraft.block.Block) state.getType();
            meta = mcBlock.getMetaFromState((IBlockState) state);
            BLOCKSTATE_META_CACHE.put(state, meta);
        }
        return meta;
    }

    public int getClaimBlockCost(World world, Vector3i lesser, Vector3i greater, boolean cuboid) {
        final int claimWidth = greater.getX() - lesser.getX() + 1;
        final int claimHeight = greater.getY() - lesser.getY() + 1;
        final int claimLength = greater.getZ() - lesser.getZ() + 1;
        if (GriefDefenderPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.AREA) {
            return claimWidth * claimLength;
        }

        return claimLength * claimWidth * claimHeight;
    }

    public long asLong(int x, int z) {
        return (long)x & 4294967295L | ((long)z & 4294967295L) << 32;
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

    public void restoreClaim(GDClaim claim) {
        if (claim.isAdminClaim()) {
            return;
        }

        // it's too expensive to do this for huge claims
        if (claim.getClaimBlocks() > (GriefDefenderPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.VOLUME ? 2560000 : 10000)) {
            return;
        }

        ArrayList<org.spongepowered.api.world.Chunk> chunks = claim.getChunks();
        final List<net.minecraft.world.chunk.Chunk> regeneratedChunks = new ArrayList<>();
        final ChunkProviderServer chunkProviderServer = (ChunkProviderServer) ((WorldServer) claim.getWorld()).getChunkProvider();
        for (org.spongepowered.api.world.Chunk chunk : chunks) {
            final Vector3i min = chunk.getBlockMin();
            final Vector3i max = chunk.getBlockMax();
            if (claim.contains(min, true) && claim.contains(max, true)) {
                BlockUtil.getInstance().regenerateChunk((net.minecraft.world.chunk.Chunk) chunk);
            } else {
                regeneratedChunks.add((net.minecraft.world.chunk.Chunk) chunk);
            }
        }

        for (net.minecraft.world.chunk.Chunk chunk : regeneratedChunks) {
            final net.minecraft.world.chunk.Chunk newChunk = chunkProviderServer.chunkGenerator.generateChunk(chunk.x, chunk.z);
            final net.minecraft.world.WorldServer world = (WorldServer) chunk.getWorld();
            List<BlockPos> changedPositions = new ArrayList<>();
            if (newChunk != null) {
                final int cx = newChunk.x << 4;
                final int cz = newChunk.z << 4;
                for(int xx = cx; xx < cx + 16; xx++) {
                    for(int zz = cz; zz < cz + 16; zz++) {
                        for(int yy = 0; yy < 256; yy++) {
                            final BlockPos blockPos = new BlockPos(xx, yy, zz);
                            if (claim.contains(xx, yy, zz, true, null, false)) {
                                final IBlockState newState = newChunk.getBlockState(blockPos);
                                final IBlockState oldState = chunk.getBlockState(blockPos);
                                if (oldState == newState) {
                                    continue;
                                }
                                world.removeTileEntity(blockPos);
                                chunk.setBlockState(blockPos, newState);
                                changedPositions.add(blockPos);
                            }
                        }
                    }
                }

                final PlayerChunkMapEntry playerMapChunkEntry = ((WorldServer) chunk.getWorld()).getPlayerChunkMap().getEntry(chunk.x, chunk.z);
                if (playerMapChunkEntry != null) {
                    List<EntityPlayerMP> chunkPlayers = playerMapChunkEntry.players;
                    for (EntityPlayerMP playerMP: chunkPlayers) {
                        for (BlockPos pos : changedPositions) {
                            playerMP.connection.sendPacket(new SPacketBlockChange(world, pos));
                        }
                    }
                }
            }
        }
    }

    private void saveChunkData(ChunkProviderServer chunkProviderServer, Chunk chunkIn)
    {
        try
        {
            chunkIn.setLastSaveTime(chunkIn.getWorld().getTotalWorldTime());
            chunkProviderServer.chunkLoader.saveChunk(chunkIn.getWorld(), chunkIn);
        }
        catch (IOException ioexception)
        {
            //LOGGER.error((String)"Couldn\'t save chunk", (Throwable)ioexception);
        }
        catch (MinecraftException minecraftexception)
        {
            //LOGGER.error((String)"Couldn\'t save chunk; already in use by another instance of Minecraft?", (Throwable)minecraftexception);
        }
        try
        {
            chunkProviderServer.chunkLoader.saveExtraChunkData(chunkIn.getWorld(), chunkIn);
        }
        catch (Exception exception)
        {
            //LOGGER.error((String)"Couldn\'t save entities", (Throwable)exception);
        }
    }

    private void unloadChunk(net.minecraft.world.chunk.Chunk chunk) {
        ChunkProviderServer chunkProviderServer = (ChunkProviderServer) chunk.getWorld().getChunkProvider();

        boolean saveChunk = false;
        if (chunk.needsSaving(true)) {
            saveChunk = true;
        }

        chunk.onUnload();

        if (saveChunk) {
            saveChunkData(chunkProviderServer, chunk);
        }

        chunkProviderServer.loadedChunks.remove(ChunkPos.asLong(chunk.x, chunk.z));
        ((ChunkBridge) chunk).bridge$setScheduledForUnload(-1);
    }

    public boolean regenerateChunk(Chunk chunk) {
        // Before unloading the chunk, we copy its entities then clear.
        // This ensures that they don't get unloaded when we unload the chunk,
        // since we want to keep them.
        //
        // We explicitly do *not* clear any tileentity-related things - we
        // want those to be saved and unloaded, since the new chunk
        // will have completely different blocks
        List<Entity> entityList = new ArrayList<>();
        for (ClassInheritanceMultiMap<Entity> multiEntityList : chunk.getEntityLists()) {
            entityList.addAll(multiEntityList);
        }

        for (Entity entity : entityList) {
            chunk.removeEntity(entity);
        }

        unloadChunk(chunk);
        ChunkProviderServer chunkProviderServer = (ChunkProviderServer) chunk.getWorld().getChunkProvider();
        Chunk newChunk = chunkProviderServer.chunkGenerator.generateChunk(chunk.x, chunk.z);
        PlayerChunkMapEntry playerChunk = ((WorldServer) chunk.getWorld()).getPlayerChunkMap().getEntry(chunk.x, chunk.z);
        if (playerChunk != null) {
            playerChunk.chunk = newChunk;
        }

        if (newChunk != null) {
            WorldServer world = (WorldServer) newChunk.getWorld();
            world.getChunkProvider().loadedChunks.put(ChunkPos.asLong(newChunk.x, newChunk.z), newChunk);
            newChunk.onLoad();
            POPULATING_CHUNK = true;
            newChunk.populate(world.getChunkProvider(), world.getChunkProvider().chunkGenerator);
            POPULATING_CHUNK = false;
            for (Entity entity: entityList) {
                newChunk.addEntity(entity);
            }
            refreshChunk(newChunk);
        }

        return newChunk != null;
    }

    public boolean refreshChunk(Chunk chunk) {
        int x = chunk.x;
        int z = chunk.z;
        WorldServer world = (WorldServer) chunk.getWorld();
        ChunkProviderBridge chunkProviderServer = (ChunkProviderBridge) world.getChunkProvider();
        if (chunkProviderServer.bridge$getLoadedChunkWithoutMarkingActive(x, z) == null) {
            return false;
        }

        List<EntityPlayerMP> chunkPlayers = ((WorldServer) chunk.getWorld()).getPlayerChunkMap().getEntry(chunk.x, chunk.z).players;

        // We deliberately send two packets, to avoid sending a 'fullChunk' packet
        // (a changedSectionFilter of 65535). fullChunk packets cause the client to
        // completely overwrite its current chunk with a new chunk instance. This causes
        // weird issues, such as making any entities in that chunk invisible (until they leave it
        // for a new chunk)
        for (EntityPlayerMP playerMP: chunkPlayers) {
            playerMP.connection.sendPacket(new SPacketChunkData(chunk, 65534));
            playerMP.connection.sendPacket(new SPacketChunkData(chunk, 1));
            /*for (ClassInheritanceMultiMap<Entity> multiEntityList : chunk.getEntityLists()) {
                for (Entity entity : multiEntityList) {
                    if (entity != playerMP) {
                        final EntityTracker entityTracker = world.getEntityTracker();
                        final EntityTrackerEntry lookup = entityTracker.trackedEntityHashTable.lookup(entity.getEntityId());
                        Packet<?> newPacket = lookup.createSpawnPacket();
                        playerMP.connection.sendPacket(newPacket);
                    }
                }
            }*/
        }

        return true;
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

    public Optional<Location<World>> getTargetBlock(Player player, GDPlayerData playerData, int maxDistance, boolean ignoreAir) throws IllegalStateException {
        BlockRay<World> blockRay = BlockRay.from(player).distanceLimit(maxDistance).build();
        GDClaim claim = null;
        if (playerData.visualClaimId != null) {
            claim = (GDClaim) GriefDefenderPlugin.getInstance().dataStore.getClaim(player.getWorld().getUniqueId(), playerData.visualClaimId);
        }

        while (blockRay.hasNext()) {
            BlockRayHit<World> blockRayHit = blockRay.next();
            if (claim != null) {
                for (Vector3i corner : claim.getVisualizer().getVisualCorners()) {
                    if (corner.equals(blockRayHit.getBlockPosition())) {
                        return Optional.of(blockRayHit.getLocation());
                    }
                }
            }
            if (ignoreAir) {
                if (blockRayHit.getLocation().getBlockType() != BlockTypes.TALLGRASS) {
                    return Optional.of(blockRayHit.getLocation());
                }
            } else {
                if (blockRayHit.getLocation().getBlockType() != BlockTypes.AIR &&
                        blockRayHit.getLocation().getBlockType() != BlockTypes.TALLGRASS) {
                    return Optional.of(blockRayHit.getLocation());
                }
            }
        }

        return Optional.empty();
    }

    public boolean isLiquidSource(Object source) {
        if (source instanceof BlockSnapshot) {
            final BlockSnapshot blockSnapshot = (BlockSnapshot) source;
            MatterProperty matterProperty = blockSnapshot.getState().getProperty(MatterProperty.class).orElse(null);
            if (matterProperty != null && matterProperty.getValue() == MatterProperty.Matter.LIQUID) {
                return true;
            }
        }
        if (source instanceof LocatableBlock) {
            final LocatableBlock locatableBlock = (LocatableBlock) source;
            MatterProperty matterProperty = locatableBlock.getBlockState().getProperty(MatterProperty.class).orElse(null);
            if (matterProperty != null && matterProperty.getValue() == MatterProperty.Matter.LIQUID) {
                return true;
            }
        }
        return false;
    }

    public Set<Claim> getNearbyClaims(Location<World> location, int blockDistance) {
        Set<Claim> claims = new HashSet<>();
        GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(location.getExtent().getUniqueId());
        if (claimWorldManager == null) {
            return claims;
        }

        org.spongepowered.api.world.Chunk lesserChunk = location.getExtent().getChunkAtBlock(location.sub(blockDistance, 0, blockDistance).getBlockPosition()).orElse(null);
        org.spongepowered.api.world.Chunk greaterChunk = location.getExtent().getChunkAtBlock(location.add(blockDistance, 0, blockDistance).getBlockPosition()).orElse(null);

        if (lesserChunk != null && greaterChunk != null) {
            for (int chunkX = lesserChunk.getPosition().getX(); chunkX <= greaterChunk.getPosition().getX(); chunkX++) {
                for (int chunkZ = lesserChunk.getPosition().getZ(); chunkZ <= greaterChunk.getPosition().getZ(); chunkZ++) {
                    org.spongepowered.api.world.Chunk chunk = location.getExtent().getChunk(chunkX, 0, chunkZ).orElse(null);
                    if (chunk != null) {
                        Set<Claim> claimsInChunk = claimWorldManager.getInternalChunksToClaimsMap().get(ChunkPos.asLong(chunkX, chunkZ));
                        if (claimsInChunk != null) {
                            for (Claim claim : claimsInChunk) {
                                final GDClaim gpClaim = (GDClaim) claim;
                                if (gpClaim.parent == null) {
                                    claims.add(claim);
                                }
                            }
                        }
                    }
                }
            }
        }

        return claims;
    }

    public boolean isBlockWater(BlockState state) {
        Optional<MatterProperty> matterProperty = state.getProperty(MatterProperty.class);
        if (matterProperty.isPresent() && matterProperty.get().getValue() == MatterProperty.Matter.LIQUID) {
            return true;
        }
        return false;
    }
}
