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
package com.griefdefender.internal.visual;

import com.flowpowered.math.vector.Vector3i;
import com.griefdefender.GDBootstrap;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.api.claim.ClaimTypes;
import com.griefdefender.api.permission.option.type.CreateModeTypes;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.internal.util.BlockUtil;
import com.griefdefender.internal.util.NMSUtil;
import com.griefdefender.task.ClaimVisualApplyTask;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.property.block.MatterProperty;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ClaimVisual {

    public static GDClaimVisualType ADMIN;
    public static GDClaimVisualType BASIC;
    public static GDClaimVisualType DEFAULT;
    public static GDClaimVisualType ERROR;
    public static GDClaimVisualType SUBDIVISION;
    public static GDClaimVisualType RESTORENATURE;
    public static GDClaimVisualType TOWN;

    private List<Transaction<BlockSnapshot>> visualTransactions;
    private List<Transaction<BlockSnapshot>> newVisuals;
    private List<Vector3i> corners;
    private GDClaimVisualType type;
    private GDClaim claim;
    private Vector3i lesserBoundaryCorner;
    private Vector3i greaterBoundaryCorner;
    private boolean cuboidVisual = false;
    private boolean isPlayerInWater = false;
    private int sx, sy, sz;
    private int bx, by, bz;
    private int minx, minz;
    private int maxx, maxz;
    private BlockType cornerMaterial;
    private BlockType accentMaterial;
    private BlockType fillerMaterial; // used for 3d cuboids
    private BlockSnapshot.Builder snapshotBuilder;
    public boolean displaySubdivisions = false;
    private int STEP = 10;

    static {
        ADMIN = new GDClaimVisualType("griefdefender:admin", "admin");
        BASIC = new GDClaimVisualType("griefdefender:basic", "basic");
        DEFAULT = new GDClaimVisualType("griefdefender:default", "default");
        ERROR = new GDClaimVisualType("griefdefender:error", "error");
        RESTORENATURE = new GDClaimVisualType("griefdefender:restorenature", "restorenature");
        SUBDIVISION = new GDClaimVisualType("griefdefender:subdivision", "subdivision");
        TOWN = new GDClaimVisualType("griefdefender:town", "town");
    }

    public ClaimVisual(GDClaimVisualType type) {
        initBlockVisualTypes(type);
        this.type = type;
        this.snapshotBuilder = Sponge.getGame().getRegistry().createBuilder(BlockSnapshot.Builder.class);
        this.visualTransactions = new ArrayList<Transaction<BlockSnapshot>>();
        this.newVisuals = new ArrayList<>();
        this.corners = new ArrayList<>();
    }

    public ClaimVisual(GDClaim claim, GDClaimVisualType type) {
        this(claim.lesserBoundaryCorner, claim.greaterBoundaryCorner, type);
        this.claim = claim;
    }

    public ClaimVisual(Vector3i lesserBoundaryCorner, Vector3i greaterBoundaryCorner, GDClaimVisualType type) {
        initBlockVisualTypes(type);
        this.lesserBoundaryCorner = lesserBoundaryCorner;
        this.greaterBoundaryCorner = greaterBoundaryCorner;
        this.type = type;
        this.snapshotBuilder = Sponge.getGame().getRegistry().createBuilder(BlockSnapshot.Builder.class);
        this.visualTransactions = new ArrayList<Transaction<BlockSnapshot>>();
        this.newVisuals = new ArrayList<>();
        this.corners = new ArrayList<>();
    }

    public void initBlockVisualTypes(GDClaimVisualType type) {
        if (type == BASIC) {
            cornerMaterial = BASIC.getVisualCornerBlock();
            accentMaterial = BASIC.getVisualAccentBlock();
            fillerMaterial = BASIC.getVisualFillerBlock();
        } else if (type == ADMIN) {
            cornerMaterial = ADMIN.getVisualCornerBlock();
            accentMaterial = ADMIN.getVisualAccentBlock();
            fillerMaterial = ADMIN.getVisualFillerBlock();
        } else if (type == SUBDIVISION) {
            cornerMaterial = SUBDIVISION.getVisualCornerBlock();
            accentMaterial = SUBDIVISION.getVisualAccentBlock();
            fillerMaterial = SUBDIVISION.getVisualFillerBlock();
        } else if (type == RESTORENATURE) {
            cornerMaterial = RESTORENATURE.getVisualCornerBlock();
            accentMaterial = RESTORENATURE.getVisualAccentBlock();
        } else if (type == TOWN) {
            cornerMaterial = TOWN.getVisualCornerBlock();
            accentMaterial = TOWN.getVisualAccentBlock();
            fillerMaterial = TOWN.getVisualFillerBlock();
        } else {
            cornerMaterial = DEFAULT.getVisualCornerBlock();
            accentMaterial = DEFAULT.getVisualAccentBlock();
            fillerMaterial = DEFAULT.getVisualFillerBlock();
        }
    }

    public static GDClaimVisualType getClaimVisualType(GDClaim claim) {
        ClaimType type = claim.getType();
        if (type != null) {
            if (type == ClaimTypes.ADMIN) {
                return ADMIN;
            } else if (type == ClaimTypes.TOWN) {
                return TOWN;
            } else if (type == ClaimTypes.SUBDIVISION) {
                return SUBDIVISION;
            }
        }

        return BASIC;
    }

    public GDClaim getClaim() {
        return this.claim;
    }

    public void apply(Player player) {
        this.apply(player, true);
    }

    public void apply(Player player, boolean resetActive) {
        GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());

        // if he has any current visualization, clear it first
        //playerData.revertActiveVisual(player);

        if (player.isOnline() && this.visualTransactions.size() > 0
                && this.visualTransactions.get(0).getOriginal().getLocation().get().getExtent().equals(player.getWorld())) {
            Sponge.getGame().getScheduler().createTaskBuilder().delayTicks(1L)
                    .execute(new ClaimVisualApplyTask(player, playerData, this, resetActive)).submit(GDBootstrap.getInstance());
            //GriefDefenderPlugin.getInstance().executor.execute(new VisualizationApplicationTask(player, playerData, this, resetActive));
        }
    }

    public void revert(Player player) {
        if (!player.isOnline()) {
            return;
        }

        GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());

        int minx = player.getLocation().getBlockX() - 100;
        int minz = player.getLocation().getBlockZ() - 100;
        int maxx = player.getLocation().getBlockX() + 100;
        int maxz = player.getLocation().getBlockZ() + 100;

        if (!this.cuboidVisual) {
            this.removeElementsOutOfRange(this.visualTransactions, minx, minz, maxx, maxz);
        }

        for (int i = 0; i < this.visualTransactions.size(); i++) {
            BlockSnapshot snapshot = this.visualTransactions.get(i).getOriginal();

            if (i == 0) {
                if (!player.getWorld().equals(snapshot.getLocation().get().getExtent())) {
                    return;
                }
            }

            player.sendBlockChange(snapshot.getPosition(), snapshot.getState());
        }

        playerData.visualBlocks = null;
        if (playerData.visualRevertTask != null) {
            playerData.visualRevertTask.cancel();
        }
    }

    public static ClaimVisual fromClick(Location<World> location, int height, GDClaimVisualType visualizationType, Player player, GDPlayerData playerData) {
        ClaimVisual visualization = new ClaimVisual(visualizationType);
        visualization.cornerMaterial = GriefDefenderPlugin.getInstance().createVisualBlock.getType();
        BlockSnapshot blockClicked =
                visualization.snapshotBuilder.from(location).blockState(visualization.cornerMaterial.getDefaultState()).build();
        visualization.visualTransactions.add(new Transaction<BlockSnapshot>(blockClicked.getLocation().get().createSnapshot(), blockClicked));
        if (GriefDefenderPlugin.getInstance().worldEditProvider != null && playerData.getClaimCreateMode() == CreateModeTypes.VOLUME && GriefDefenderPlugin.getGlobalConfig().getConfig().visual.hideDrag2d) {
            GriefDefenderPlugin.getInstance().worldEditProvider.sendVisualDrag(player, playerData, location.getBlockPosition());
        }
        return visualization;
    }

    public void resetVisuals() {
        this.visualTransactions.clear();
        this.newVisuals.clear();
    }

    public void createClaimBlockVisualWithType(GDClaim claim, int height, Location<World> locality, GDPlayerData playerData, GDClaimVisualType visualType) {
        this.type = visualType;
        this.claim = claim;
        this.addClaimElements(height, locality, playerData);
    }

    public void createClaimBlockVisuals(int height, Location<World> locality, GDPlayerData playerData) {
        if (this.visualTransactions.size() != 0) {
            return;
        }

        this.addClaimElements(height, locality, playerData);
    }

    public GDClaimVisualType getType() {
        return this.type;
    }

    public void setType(GDClaimVisualType type) {
        this.type = type;
    }

    private void addClaimElements(int height, Location<World> locality, GDPlayerData playerData) {
        this.initBlockVisualTypes(type);
        Vector3i lesser = this.claim.getLesserBoundaryCorner();
        Vector3i greater = this.claim.getGreaterBoundaryCorner();
        World world = locality.getExtent();
        boolean liquidTransparent = locality.getBlock().getType().getProperty(MatterProperty.class).isPresent() ? false : true;

        this.sx = lesser.getX();
        this.sy = this.useCuboidVisual() ? lesser.getY() : 0;
        this.sz = lesser.getZ();
        this.bx = greater.getX();
        this.by = this.useCuboidVisual() ? greater.getY() : 0;
        this.bz = greater.getZ();
        this.minx = this.claim.cuboid ? this.sx : locality.getBlockX() - 75;
        this.minz = this.claim.cuboid ? this.sz : locality.getBlockZ() - 75;
        this.maxx = this.claim.cuboid ? this.bx : locality.getBlockX() + 75;
        this.maxz = this.claim.cuboid ? this.bz : locality.getBlockZ() + 75;

        if (this.sx == this.bx && this.sy == this.by && this.sz == this.bz) {
            BlockSnapshot blockClicked =
                    snapshotBuilder.from(new Location<World>(world, this.sx, this.sy, this.sz)).blockState(this.cornerMaterial.getDefaultState()).build();
            visualTransactions.add(new Transaction<BlockSnapshot>(blockClicked.getLocation().get().createSnapshot(), blockClicked));
            return;
        }

        // check CUI support
        if (GriefDefenderPlugin.getInstance().worldEditProvider != null && playerData != null
                && GriefDefenderPlugin.getInstance().worldEditProvider.hasCUISupport(playerData.getPlayerName())) {
            playerData.showVisualFillers = false;
            STEP = 0;
        }

        // Check if player is in water
        final Player player = Sponge.getCauseStackManager().getCurrentCause().first(Player.class).orElse(null);
        if (player != null) {
            if (BlockUtil.getInstance().isBlockWater(player.getLocation().getBlock())) {
                this.isPlayerInWater = true;
            } else {
                this.isPlayerInWater = false;
            }
        } else {
            this.isPlayerInWater = false;
        }

        if (this.useCuboidVisual()) {
            this.addVisuals3D(claim, playerData);
        } else {
            this.addVisuals2D(claim, height);
        }
    }

    public void addVisuals3D(GDClaim claim, GDPlayerData playerData) {
        final World world = claim.getWorld();

        this.addTopLine(world, this.sy, this.cornerMaterial, this.accentMaterial);
        this.addTopLine(world, this.by, this.cornerMaterial, this.accentMaterial);
        this.addBottomLine(world, this.sy, this.cornerMaterial, this.accentMaterial);
        this.addBottomLine(world, this.by, this.cornerMaterial, this.accentMaterial);
        this.addLeftLine(world, this.sy, this.cornerMaterial, this.accentMaterial);
        this.addLeftLine(world, this.by, this.cornerMaterial, this.accentMaterial);
        this.addRightLine(world, this.sy, this.cornerMaterial, this.accentMaterial);
        this.addRightLine(world, this.by, this.cornerMaterial, this.accentMaterial);
        // don't show corners while subdividing
        if (playerData == null || (playerData.claimSubdividing == null)) {
            // top corners
            this.addCorners(world, this.by - 1, this.accentMaterial);
            // bottom corners
            this.addCorners(world, this.sy + 1, this.accentMaterial);
        }

        if (STEP != 0 && (playerData == null || playerData.showVisualFillers)) {
            for (int y = this.sy + STEP; y < this.by - STEP / 2; y += STEP) {
                this.addTopLine(world, y, fillerMaterial, fillerMaterial);
            }
            for (int y = this.sy + STEP; y < this.by - STEP / 2; y += STEP) {
                this.addBottomLine(world, y, fillerMaterial, fillerMaterial);
            }
            for (int y = this.sy + STEP; y < this.by - STEP / 2; y += STEP) {
                this.addLeftLine(world, y, fillerMaterial, fillerMaterial);
            }
            for (int y = this.sy + STEP; y < this.by - STEP / 2; y += STEP) {
                this.addRightLine(world, y, fillerMaterial, fillerMaterial);
            }
        }
        this.visualTransactions.addAll(newVisuals);
    }

    public void addVisuals2D(GDClaim claim, int height) {
        final World world = claim.getWorld();
        this.addTopLine(world, height, this.cornerMaterial, this.accentMaterial);
        this.addBottomLine(world, height, this.cornerMaterial, this.accentMaterial);
        this.addLeftLine(world, height, this.cornerMaterial, this.accentMaterial);
        this.addRightLine(world, height, this.cornerMaterial, this.accentMaterial);

        this.removeElementsOutOfRange(this.newVisuals, this.minx, this.minz, this.maxx, this.maxz);

        for (int i = 0; i < this.newVisuals.size(); i++) {
            final BlockSnapshot element = this.newVisuals.get(i).getFinal();
            if (!claim.contains(element.getPosition())) {
                this.newVisuals.remove(i);
            }
        }

        ArrayList<Transaction<BlockSnapshot>> actualElements = new ArrayList<Transaction<BlockSnapshot>>();
        for (Transaction<BlockSnapshot> element : this.newVisuals) {
            Location<World> tempLocation = element.getFinal().getLocation().get();
            Location<World> visibleLocation =
                    getVisibleLocation(tempLocation.getExtent(), tempLocation.getBlockX(), height, tempLocation.getBlockZ());
            element = new Transaction<BlockSnapshot>(element.getOriginal().withLocation(visibleLocation).withState(visibleLocation.getBlock()),
                    element.getFinal().withLocation(visibleLocation));
            height = element.getFinal().getPosition().getY();
            actualElements.add(element);
        }

        this.visualTransactions.addAll(actualElements);
    }

    public void addCorners(World world, int y, BlockType accentMaterial) {
        BlockSnapshot corner1 =
                this.snapshotBuilder.from(new Location<World>(world, this.sx, y, this.bz)).blockState(accentMaterial.getDefaultState()).build();
        this.newVisuals.add(new Transaction<BlockSnapshot>(corner1.getLocation().get().createSnapshot(), corner1));
        BlockSnapshot corner2 =
                this.snapshotBuilder.from(new Location<World>(world, this.bx, y, this.bz)).blockState(accentMaterial.getDefaultState()).build();
        this.newVisuals.add(new Transaction<BlockSnapshot>(corner2.getLocation().get().createSnapshot(), corner2));
        BlockSnapshot corner3 =
                this.snapshotBuilder.from(new Location<World>(world, this.bx, y, this.sz)).blockState(accentMaterial.getDefaultState()).build();
        this.newVisuals.add(new Transaction<BlockSnapshot>(corner3.getLocation().get().createSnapshot(), corner3));
        BlockSnapshot corner4 =
                this.snapshotBuilder.from(new Location<World>(world, this.sx, y, this.sz)).blockState(accentMaterial.getDefaultState()).build();
        this.newVisuals.add(new Transaction<BlockSnapshot>(corner4.getLocation().get().createSnapshot(), corner4));
    }

    public void addTopLine(World world, int y, BlockType cornerMaterial, BlockType accentMaterial) {
        BlockSnapshot topVisualBlock1 =
                this.snapshotBuilder.from(new Location<World>(world, this.sx, y, this.bz)).blockState(cornerMaterial.getDefaultState()).build();
        this.newVisuals.add(new Transaction<BlockSnapshot>(topVisualBlock1.getLocation().get().createSnapshot(), topVisualBlock1));
        this.corners.add(topVisualBlock1.getPosition());
        BlockSnapshot topVisualBlock2 =
                this.snapshotBuilder.from(new Location<World>(world, this.sx + 1, y, this.bz)).blockState(accentMaterial.getDefaultState()).build();
        this.newVisuals.add(new Transaction<BlockSnapshot>(topVisualBlock2.getLocation().get().createSnapshot(), topVisualBlock2));
        BlockSnapshot topVisualBlock3 =
                this.snapshotBuilder.from(new Location<World>(world, this.bx - 1, y, this.bz)).blockState(accentMaterial.getDefaultState()).build();
        this.newVisuals.add(new Transaction<BlockSnapshot>(topVisualBlock3.getLocation().get().createSnapshot(), topVisualBlock3));

        if (STEP != 0) {
            for (int x = this.sx + STEP; x < this.bx - STEP / 2; x += STEP) {
                if ((y != 0 && x >= this.sx && x <= this.bx) || (x > this.minx && x < this.maxx)) {
                    BlockSnapshot visualBlock =
                            this.snapshotBuilder.from(new Location<World>(world, x, y, this.bz)).blockState(accentMaterial.getDefaultState()).build();
                    this.newVisuals.add(new Transaction<BlockSnapshot>(visualBlock.getLocation().get().createSnapshot(), visualBlock));
                }
            }
        }
    }

    public void addBottomLine(World world, int y, BlockType cornerMaterial, BlockType accentMaterial) {
        BlockSnapshot bottomVisualBlock1 =
                this.snapshotBuilder.from(new Location<World>(world, this.sx + 1, y, this.sz)).blockState(accentMaterial.getDefaultState()).build();
        this.newVisuals.add(new Transaction<BlockSnapshot>(bottomVisualBlock1.getLocation().get().createSnapshot(), bottomVisualBlock1));
        this.corners.add(bottomVisualBlock1.getPosition());
        BlockSnapshot bottomVisualBlock2 =
                this.snapshotBuilder.from(new Location<World>(world, this.bx - 1, y, this.sz)).blockState(accentMaterial.getDefaultState()).build();
        this.newVisuals.add(new Transaction<BlockSnapshot>(bottomVisualBlock2.getLocation().get().createSnapshot(), bottomVisualBlock2));

        if (STEP != 0) {
            for (int x = this.sx + STEP; x < this.bx - STEP / 2; x += STEP) {
                if ((y != 0 && x >= this.sx && x <= this.bx) || (x > this.minx && x < this.maxx)) {
                    BlockSnapshot visualBlock =
                            this.snapshotBuilder.from(new Location<World>(world, x, y, this.sz)).blockState(accentMaterial.getDefaultState()).build();
                    this.newVisuals.add(new Transaction<BlockSnapshot>(visualBlock.getLocation().get().createSnapshot(), visualBlock));
                }
            }
        }
    }

    public void addLeftLine(World world, int y, BlockType cornerMaterial, BlockType accentMaterial) {
        BlockSnapshot leftVisualBlock1 =
                this.snapshotBuilder.from(new Location<World>(world, this.sx, y, this.sz)).blockState(cornerMaterial.getDefaultState()).build();
        this.newVisuals.add(new Transaction<BlockSnapshot>(leftVisualBlock1.getLocation().get().createSnapshot(), leftVisualBlock1));
        this.corners.add(leftVisualBlock1.getPosition());
        BlockSnapshot leftVisualBlock2 =
                this.snapshotBuilder.from(new Location<World>(world, this.sx, y, this.sz + 1)).blockState(accentMaterial.getDefaultState()).build();
        this.newVisuals.add(new Transaction<BlockSnapshot>(leftVisualBlock2.getLocation().get().createSnapshot(), leftVisualBlock2));
        BlockSnapshot leftVisualBlock3 =
                this.snapshotBuilder.from(new Location<World>(world, this.sx, y, this.bz - 1)).blockState(accentMaterial.getDefaultState()).build();
        this.newVisuals.add(new Transaction<BlockSnapshot>(leftVisualBlock3.getLocation().get().createSnapshot(), leftVisualBlock3));

        if (STEP != 0) {
            for (int z = this.sz + STEP; z < this.bz - STEP / 2; z += STEP) {
                if ((y != 0 && z >= this.sz && z <= this.bz) || (z > this.minz && z < this.maxz)) {
                    BlockSnapshot visualBlock =
                            this.snapshotBuilder.from(new Location<World>(world, this.sx, y, z)).blockState(accentMaterial.getDefaultState()).build();
                    this.newVisuals.add(new Transaction<BlockSnapshot>(visualBlock.getLocation().get().createSnapshot(), visualBlock));
               }
            }
        }
    }

    public void addRightLine(World world, int y, BlockType cornerMaterial, BlockType accentMaterial) {
        BlockSnapshot rightVisualBlock1 =
                this.snapshotBuilder.from(new Location<World>(world, this.bx, y, this.sz)).blockState(cornerMaterial.getDefaultState()).build();
        this.newVisuals.add(new Transaction<BlockSnapshot>(rightVisualBlock1.getLocation().get().createSnapshot(), rightVisualBlock1));
        this.corners.add(rightVisualBlock1.getPosition());
        BlockSnapshot rightVisualBlock2 =
                this.snapshotBuilder.from(new Location<World>(world, this.bx, y, this.sz + 1)).blockState(accentMaterial.getDefaultState()).build();
        this.newVisuals.add(new Transaction<BlockSnapshot>(rightVisualBlock2.getLocation().get().createSnapshot(), rightVisualBlock2));
        if (STEP != 0) {
            for (int z = this.sz + STEP; z < this.bz - STEP / 2; z += STEP) {
                if ((y != 0 && z >= this.sz && z <= this.bz) || (z > this.minz && z < this.maxz)) {
                    BlockSnapshot visualBlock =
                            this.snapshotBuilder.from(new Location<World>(world, this.bx, y, z)).blockState(accentMaterial.getDefaultState()).build();
                    this.newVisuals.add(new Transaction<BlockSnapshot>(visualBlock.getLocation().get().createSnapshot(), visualBlock));
                }
            }
        }
        BlockSnapshot rightVisualBlock3 =
                this.snapshotBuilder.from(new Location<World>(world, this.bx, y, this.bz - 1)).blockState(accentMaterial.getDefaultState()).build();
        this.newVisuals.add(new Transaction<BlockSnapshot>(rightVisualBlock3.getLocation().get().createSnapshot(), rightVisualBlock3));
        BlockSnapshot rightVisualBlock4 =
                this.snapshotBuilder.from(new Location<World>(world, this.bx, y, this.bz)).blockState(cornerMaterial.getDefaultState()).build();
        this.newVisuals.add(new Transaction<BlockSnapshot>(rightVisualBlock4.getLocation().get().createSnapshot(), rightVisualBlock4));
        this.corners.add(rightVisualBlock4.getPosition());
    }

    public List<Transaction<BlockSnapshot>> getVisualTransactions() {
        return this.visualTransactions;
    }

    public List<Vector3i> getVisualCorners() {
        return this.corners;
    }

    private void removeElementsOutOfRange(List<Transaction<BlockSnapshot>> elements, int minx, int minz, int maxx, int maxz) {
        for (int i = 0; i < elements.size(); i++) {
            Location<World> location = elements.get(i).getFinal().getLocation().get();
            if (location.getX() < minx || location.getX() > maxx || location.getZ() < minz || location.getZ() > maxz) {
                elements.remove(i);
            }
        }
    }

    private Location<World> getVisibleLocation(World world, int x, int y, int z) {
        Location<World> location = world.getLocation(x, y, z);
        Direction direction = (isTransparent(location.getBlock())) ? Direction.DOWN : Direction.UP;

        while (location.getPosition().getY() >= 1 &&
                location.getPosition().getY() < world.getDimension().getBuildHeight() - 1 &&
                (!isTransparent(location.getRelative(Direction.UP).getBlock())
                        || isTransparent(location.getBlock()))) {
            location = location.getRelative(direction);
        }

        return location;
    }

    private boolean isTransparent(BlockState state) {
        if (state.getType() == BlockTypes.SNOW_LAYER) {
            return false;
        }

        if (!this.isPlayerInWater && BlockUtil.getInstance().isBlockWater(state)) {
            return false;
        }
        return NMSUtil.getInstance().isTransparent(state);
    }

    public static ClaimVisual fromClaims(Set<Claim> claims, int height, Location<World> locality, GDPlayerData playerData, ClaimVisual visualization) {
        if (visualization == null) {
            visualization = new ClaimVisual(BASIC);
        }

        for (Claim claim : claims) {
            GDClaim gpClaim = (GDClaim) claim;
            if (!gpClaim.children.isEmpty()) {
                fromClaims(gpClaim.children, height, locality, playerData, visualization);
            }
            if (gpClaim.claimVisual != null && !gpClaim.claimVisual.visualTransactions.isEmpty()) {
                visualization.visualTransactions.addAll(gpClaim.getVisualizer().visualTransactions);
            } else {
                visualization.createClaimBlockVisualWithType(gpClaim, height, locality, playerData, ClaimVisual.getClaimVisualType(gpClaim));
            }
        }

        return visualization;
    }

    private boolean useCuboidVisual() {
        if (this.claim.cuboid) {
            return true;
        }

        final GDPlayerData ownerData = this.claim.getOwnerPlayerData();
        if (ownerData != null && (this.claim.getOwnerMinClaimLevel() > 0 || this.claim.getOwnerMaxClaimLevel() < 255)) {
            return true;
        }
        // Claim could of been created with different min/max levels, so check Y values
        if (this.claim.getLesserBoundaryCorner().getY() > 0 || this.claim.getGreaterBoundaryCorner().getY() < 255) {
            return true;
        }

        return false;
    }
}
