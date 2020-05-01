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
package com.griefdefender.claim;

import static com.google.common.base.Preconditions.checkNotNull;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimBlockSystem;
import com.griefdefender.api.claim.ClaimContexts;
import com.griefdefender.api.claim.ClaimManager;
import com.griefdefender.api.claim.ClaimResult;
import com.griefdefender.api.claim.ClaimResultType;
import com.griefdefender.api.claim.ClaimSchematic;
import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.api.claim.ClaimTypes;
import com.griefdefender.api.claim.ShovelTypes;
import com.griefdefender.api.claim.TrustType;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.api.data.ClaimData;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.configuration.ClaimDataConfig;
import com.griefdefender.configuration.ClaimStorageData;
import com.griefdefender.configuration.IClaimData;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.configuration.TownDataConfig;
import com.griefdefender.configuration.TownStorageData;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.event.GDChangeClaimEvent;
import com.griefdefender.event.GDCreateClaimEvent;
import com.griefdefender.event.GDRemoveClaimEvent;
import com.griefdefender.event.GDGroupTrustClaimEvent;
import com.griefdefender.event.GDSaveClaimEvent;
import com.griefdefender.event.GDTransferClaimEvent;
import com.griefdefender.event.GDUserTrustClaimEvent;
import com.griefdefender.internal.provider.WorldGuardProvider;
import com.griefdefender.internal.util.BlockUtil;
import com.griefdefender.internal.util.VecHelper;
import com.griefdefender.internal.visual.ClaimVisual;
import com.griefdefender.permission.GDPermissionHolder;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.provider.VaultProvider;
import com.griefdefender.registry.TrustTypeRegistryModule;
import com.griefdefender.storage.BaseStorage;
import com.griefdefender.util.EconomyUtil;
import com.griefdefender.util.PermissionUtil;

import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.serializer.plain.PlainComponentSerializer;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class GDClaim implements Claim {

    public static final BaseStorage DATASTORE = GriefDefenderPlugin.getInstance().dataStore;
    // Note: 2D cuboids will ignore the upper Y value while 3D cuboids do not
    public Vector3i lesserBoundaryCorner;
    public Vector3i greaterBoundaryCorner;
    private World world;
    private ClaimType type = ClaimTypes.BASIC;
    private Set<Long> chunkHashes;
    private final int hashCode;
    private final GDClaimManager worldClaimManager;
    private final Claim wildernessClaim;
    private final VaultProvider vaultProvider = GriefDefenderPlugin.getInstance().getVaultProvider();

    // Permission Context
    private final Context context;
    private final Context overrideClaimContext;
    private final Context worldContext;

    private UUID id = null;
    private UUID ownerUniqueId;

    public boolean cuboid = false;
    public boolean markVisualDirty = false;

    protected ClaimStorageData claimStorage;
    protected IClaimData claimData;

    public GDClaim parent = null;
    public Set<Claim> children = new HashSet<>();
    public ClaimVisual claimVisual;
    public List<UUID> playersWatching = new ArrayList<>();
    public Map<String, ClaimSchematic> schematics = new HashMap<>();

    private GDPlayerData ownerPlayerData;
    private static final int MAX_AREA = GriefDefenderPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.VOLUME ? 2560000 : 10000;

    public GDClaim(World world, Vector3i point1, Vector3i point2, ClaimType type, UUID ownerUniqueId, boolean cuboid) {
        this(world, point1, point2, type, ownerUniqueId, cuboid, null);
    }

    public GDClaim(World world, Vector3i point1, Vector3i point2, ClaimType type, UUID ownerUniqueId, boolean cuboid, GDClaim parent) {
        int minx = Math.min(point1.getX(), point2.getX());
        int miny = Math.min(point1.getY(), point2.getY());
        int minz = Math.min(point1.getZ(), point2.getZ());
        int maxx = Math.max(point1.getX(), point2.getX());
        int maxy = Math.max(point1.getY(), point2.getY());
        int maxz = Math.max(point1.getZ(), point2.getZ());

        this.world = world;
        this.lesserBoundaryCorner = new Vector3i(minx, miny, minz);
        this.greaterBoundaryCorner = new Vector3i(maxx, maxy, maxz);
        if (ownerUniqueId != null) {
            this.ownerUniqueId = ownerUniqueId;
            this.ownerPlayerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(this.world, this.ownerUniqueId);
        }
        this.type = type;
        this.id = UUID.randomUUID();
        this.context = new Context("gd_claim", this.id.toString());
        this.worldContext = new Context("world", world.getName().toLowerCase());
        this.overrideClaimContext = new Context("gd_claim_override", this.id.toString());
        this.cuboid = cuboid;
        this.parent = parent;
        this.hashCode = this.id.hashCode();
        this.worldClaimManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(this.world.getUID());
        if (this.type == ClaimTypes.WILDERNESS) {
            this.wildernessClaim = this;
        } else {
            this.wildernessClaim = this.worldClaimManager.getWildernessClaim();
        }
    }

    // Used for visualizations
    public GDClaim(World world, Vector3i lesserBoundaryCorner, Vector3i greaterBoundaryCorner, ClaimType type, boolean cuboid) {
        this(world, lesserBoundaryCorner, greaterBoundaryCorner, UUID.randomUUID(), type, null, cuboid);
    }

    // Used at server startup
    public GDClaim(World world, Vector3i lesserBoundaryCorner, Vector3i greaterBoundaryCorner, UUID claimId, ClaimType type, UUID ownerUniqueId, boolean cuboid) {
        this.id = claimId;
        this.overrideClaimContext = new Context("gd_claim_override", this.id.toString());
        this.lesserBoundaryCorner = lesserBoundaryCorner;
        this.greaterBoundaryCorner = greaterBoundaryCorner;
        this.world = world;
        if (ownerUniqueId != null) {
            this.ownerUniqueId = ownerUniqueId;
            this.ownerPlayerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(this.world, this.ownerUniqueId);
        }
        this.type = type;
        this.cuboid = cuboid;
        this.context = new Context("gd_claim", this.id.toString());
        this.worldContext = new Context("world", world.getName().toLowerCase());
        this.hashCode = this.id.hashCode();
        this.worldClaimManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(this.world.getUID());
        if (this.type == ClaimTypes.WILDERNESS) {
            this.wildernessClaim = this;
        } else {
            this.wildernessClaim = this.worldClaimManager.getWildernessClaim();
        }
    }

    public void initializeClaimData(GDClaim parent) {
        Path claimDataFolderPath = null;
        // check if main world
        if (parent != null) {
            claimDataFolderPath = parent.getClaimStorage().filePath.getParent().resolve(this.type.getName().toLowerCase());
        } else {
            claimDataFolderPath = BaseStorage.worldConfigMap.get(this.world.getUID()).getPath().getParent().resolve("ClaimData").resolve(this.type.getName().toLowerCase());
        }
        try {
            if (Files.notExists(claimDataFolderPath)) {
                Files.createDirectories(claimDataFolderPath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        File claimFile = new File(claimDataFolderPath + File.separator + this.id);
        if (this.isTown()) {
            this.claimStorage = new TownStorageData(claimFile.toPath(), this.world.getUID(), this.ownerUniqueId, this.cuboid);
        } else {
            this.claimStorage = new ClaimStorageData(claimFile.toPath(), this.world.getUID(), this.ownerUniqueId, this.type, this.cuboid);
        }
        this.claimData = this.claimStorage.getConfig();
        this.parent = parent;
        if (parent != null) {
            this.claimStorage.getConfig().setParent(parent.getUniqueId());
        }
        this.updateClaimStorageData();
    }

    public ClaimType getType() {
        return this.type;
    }

    public void setType(ClaimType type) {
        this.type = type;
        if (this.claimData != null) {
            this.claimData.setType(type);
        }
    }

    public ClaimVisual getVisualizer() {
        if (this.claimVisual == null || this.markVisualDirty) {
            this.claimVisual = new ClaimVisual(this, ClaimVisual.getClaimVisualType(this));
            this.markVisualDirty = false;
        }
        return this.claimVisual;
    }

    public void resetVisuals() {
        List<UUID> playersWatching = new ArrayList<>(this.playersWatching);
        for (UUID playerUniqueId : playersWatching) {
            final Player spongePlayer = Bukkit.getServer().getPlayer(playerUniqueId);
            final GDPlayerData data = this.worldClaimManager.getOrCreatePlayerData(playerUniqueId);
            if (spongePlayer != null) {
                data.revertActiveVisual(spongePlayer);
            }
        }
        this.claimVisual = null;
    }

    public GDPlayerData getOwnerPlayerData() {
        if (this.ownerPlayerData == null && this.ownerUniqueId != null) {
            this.ownerPlayerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(this.world.getUID(), this.ownerUniqueId);
        }

        return this.ownerPlayerData;
    }

    public UUID getOwnerUniqueId() {
        if (this.isAdminClaim()) {
            return GriefDefenderPlugin.ADMIN_USER_UUID;
        }
        if (this.ownerUniqueId == null) {
            if (this.parent != null) {
                return this.parent.getOwnerUniqueId();
            }

            return GriefDefenderPlugin.ADMIN_USER_UUID;
        }

        return this.ownerUniqueId;
    }

    public void setOwnerUniqueId(UUID uniqueId) {
        this.ownerUniqueId = uniqueId;
    }

    public boolean isAdminClaim() {
        return this.type == ClaimTypes.ADMIN;
    }

    @Override
    public boolean isCuboid() {
        if (this.claimData != null) {
            return this.claimData.isCuboid();
        }

        return this.cuboid;
    }

    @Override
    public boolean isInTown() {
        if (this.isTown()) {
            return true;
        }

        GDClaim parent = this.parent;
        while (parent != null) {
            if (parent.isTown()) {
                return true;
            }
            parent = parent.parent;
        }

        return false;
    }

    @Override
    public Optional<Claim> getTown() {
        return Optional.ofNullable(this.getTownClaim());
    }

    @Nullable
    public GDClaim getTownClaim() {
        if (this.isTown()) {
            return this;
        }

        if (this.parent == null) {
            return null;
        }

        GDClaim parent = this.parent;
        while (parent != null) {
            if (parent.isTown()) {
                return parent;
            }
            parent = parent.parent;
        }

        return null;
    }

    @Override
    public UUID getUniqueId() {
        return this.id;
    }

    public Optional<Component> getName() {
        if (this.claimData == null) {
            return Optional.empty();
        }
        return this.claimData.getName();
    }

    public String getFriendlyName() {
        final Component claimName = this.claimData.getName().orElse(null);
        if (claimName == null) {
            return "none";
        }
        return PlainComponentSerializer.INSTANCE.serialize(claimName);
    }

    public Component getFriendlyNameType() {
        return this.getFriendlyNameType(false);
    }

    public Component getFriendlyNameType(boolean upper) {
        if (this.type == ClaimTypes.ADMIN) {
            if (upper) {
                return TextComponent.of(this.type.getName().toUpperCase(), TextColor.RED);
            }
            return TextComponent.of("Admin", TextColor.RED);
        }

        if (this.type == ClaimTypes.BASIC) {
            if (upper) {
                return TextComponent.of(this.type.getName().toUpperCase(), TextColor.YELLOW);
            }
            return TextComponent.of("Basic", TextColor.YELLOW);
        }

        if (this.type == ClaimTypes.SUBDIVISION) {
            if (upper) {
                return TextComponent.of(this.type.getName().toUpperCase(), TextColor.AQUA);
            }
            return TextComponent.of("Subdivision", TextColor.AQUA);
        }

        if (upper) {
            return TextComponent.of(this.type.getName().toUpperCase(), TextColor.GREEN);
        }
        return TextComponent.of("Town", TextColor.GREEN);
    }

    @Override
    public int getClaimBlocks() {
        if (GriefDefenderPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.VOLUME) {
            return this.getVolume();
        }

        return this.getArea();
    }

    @Override
    public int getArea() {
        final int claimWidth = this.greaterBoundaryCorner.getX() - this.lesserBoundaryCorner.getX() + 1;
        final int claimLength = this.greaterBoundaryCorner.getZ() - this.lesserBoundaryCorner.getZ() + 1;

        return claimWidth * claimLength;
    }

    @Override
    public int getVolume() {
        final int claimWidth = this.greaterBoundaryCorner.getX() - this.lesserBoundaryCorner.getX() + 1;
        final int claimLength = this.greaterBoundaryCorner.getZ() - this.lesserBoundaryCorner.getZ() + 1;
        final int claimHeight = this.greaterBoundaryCorner.getY() - this.lesserBoundaryCorner.getY() + 1;

        return claimWidth * claimLength * claimHeight;
    }

    @Override
    public int getWidth() {
        return this.greaterBoundaryCorner.getX() - this.lesserBoundaryCorner.getX() + 1;
    }

    @Override
    public int getHeight() {
        return this.greaterBoundaryCorner.getY() - this.lesserBoundaryCorner.getY() + 1;
    }

    @Override
    public int getLength() {
        return this.greaterBoundaryCorner.getZ() - this.lesserBoundaryCorner.getZ() + 1;
    }

    public Component allowEdit(Player player) {
        final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(player);
        if (user != null) {
            return allowEdit(user);
        }

        return TextComponent.of("");
    }

    public Component allowEdit(GDPermissionUser holder) {
        return allowEdit(holder, false);
    }

    public Component allowEdit(GDPermissionUser holder, boolean forced) {
        if (this.isUserTrusted(holder, TrustTypes.MANAGER, null, forced)) {
            return null;
        }

        if (PermissionUtil.getInstance().holderHasPermission(holder, GDPermissions.COMMAND_DELETE_CLAIMS)) {
            return null;
        }

        if (this.parent != null && this.getData().doesInheritParent()) {
            return this.parent.allowEdit(holder);
        }

        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(this.getWorldUniqueId(), holder.getUniqueId());
        if (playerData.canIgnoreClaim(this)) {
            return null;
        }

        final Component message = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.CLAIM_OWNER_ONLY, ImmutableMap.of(
                "player", this.getOwnerName()));
        return message;
    }

    public Component allowGrantPermission(Player player) {
        if(this.allowEdit(player) == null) {
            return null;
        }

        for(int i = 0; i < this.claimData.getManagers().size(); i++) {
            UUID managerID = this.claimData.getManagers().get(i);
            if(player.getUniqueId().equals(managerID)) {
                return null;
            }
        }

        if(this.parent != null && this.getData().doesInheritParent()) {
            return this.parent.allowGrantPermission(player);
        }

        final Component reason = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.PERMISSION_TRUST, ImmutableMap.of(
                "player", this.getOwnerName()));
        return reason;
    }

    @Override
    public Vector3i getLesserBoundaryCorner() {
        return this.lesserBoundaryCorner.clone();
    }

    @Override
    public Vector3i getGreaterBoundaryCorner() {
        return this.greaterBoundaryCorner.clone();
    }

    @Override
    public Component getOwnerName() {
        if (this.isAdminClaim() || this.isWilderness()) {
            return MessageCache.getInstance().OWNER_ADMIN;
        }

        if (this.getOwnerPlayerData() == null) {
            return TextComponent.of("[unknown]");
        }

        return TextComponent.of(this.getOwnerPlayerData().getPlayerName());
    }

    public String getOwnerFriendlyName() {
        if (this.isAdminClaim()) {
            return "administrator";
        }
        if (this.isWilderness()) {
            return "wilderness";
        }
        final GDPlayerData playerData = this.ownerPlayerData;
        if (playerData == null) {
            return "[unknown]";
        }
        return playerData.getPlayerName();
    }

    @Override
    public boolean contains(Vector3i pos, boolean excludeChildren) {
        return this.contains(pos.getX(), pos.getY(), pos.getZ(), excludeChildren, null, false);
    }


    public boolean contains(Vector3i pos, GDPlayerData playerData, boolean useBorderBlockRadius) {
        return this.contains(pos.getX(), pos.getY(), pos.getZ(), false, playerData, useBorderBlockRadius);
    }

    public boolean contains(int x, int y, int z, boolean excludeChildren, GDPlayerData playerData, boolean useBorderBlockRadius) {
        int borderBlockRadius = 0;
        if (useBorderBlockRadius && (playerData != null && !playerData.bypassBorderCheck)) {
            final int borderRadiusConfig = GriefDefenderPlugin.getActiveConfig(this.world.getUID()).getConfig().claim.borderBlockRadius;
            if (borderRadiusConfig > 0 && !this.isUserTrusted(playerData.getSubject(), TrustTypes.BUILDER)) {
                borderBlockRadius = borderRadiusConfig;
            }
        }
 
        boolean inClaim = (
                y >= (this.lesserBoundaryCorner.getY() - borderBlockRadius)) &&
                y < (this.greaterBoundaryCorner.getY() + 1 + borderBlockRadius) &&
                x >= (this.lesserBoundaryCorner.getX() - borderBlockRadius) &&
                x < (this.greaterBoundaryCorner.getX() + 1 + borderBlockRadius) &&
                z >= (this.lesserBoundaryCorner.getZ() - borderBlockRadius) &&
                z < (this.greaterBoundaryCorner.getZ() + 1 + borderBlockRadius);

        if (!inClaim) {
            return false;
        }

        if (!excludeChildren && this.parent != null && (this.getData() == null || (this.getData() != null && this.getData().doesInheritParent()))) {
            return this.parent.contains(x, y, z, false, null, false);
        }

        return true;
    }

    public boolean isClaimOnBorder(GDClaim claim) {
        if (claim.cuboid) {
            return false;
        }

        boolean result = claim.lesserBoundaryCorner.getX() == this.lesserBoundaryCorner.getX() ||
               claim.greaterBoundaryCorner.getX() == this.greaterBoundaryCorner.getX() ||
               claim.lesserBoundaryCorner.getZ() == this.lesserBoundaryCorner.getZ() ||
               claim.greaterBoundaryCorner.getZ() == this.greaterBoundaryCorner.getZ();
        if (claim.cuboid) {
            result = claim.lesserBoundaryCorner.getY() == this.lesserBoundaryCorner.getY() ||
                    claim.greaterBoundaryCorner.getY() == this.greaterBoundaryCorner.getY();
        }
        return result;
    }

    @Override
    public boolean overlaps(Claim other) {
        GDClaim otherClaim = (GDClaim) other;
        if (this.id == otherClaim.id) {
            return false;
        }

        // Handle claims entirely within a town
        if (this.isTown() && !otherClaim.isTown() && otherClaim.isInside(this)) {
            return false;
        }

        //verify that no claim's lesser boundary point is inside this new claim, to cover the "existing claim is entirely inside new claim" case
        if(this.contains(otherClaim.getLesserBoundaryCorner(), false)) {
            return true;
        }

        return this.isBandingAcross(otherClaim);
    }

    //Checks if claim bands across another claim, either horizontally or vertically
    public boolean isBandingAcross(GDClaim otherClaim) {
        final boolean isClaimInside = otherClaim.isInside(this);
        if (isClaimInside) {
            return false;
        }

        final int smallX = otherClaim.getLesserBoundaryCorner().getX();
        final int smallY = otherClaim.getLesserBoundaryCorner().getY();
        final int smallZ = otherClaim.getLesserBoundaryCorner().getZ();
        final int bigX = otherClaim.getGreaterBoundaryCorner().getX();
        final int bigY = otherClaim.getGreaterBoundaryCorner().getY();
        final int bigZ = otherClaim.getGreaterBoundaryCorner().getZ();

        if(this.contains(otherClaim.lesserBoundaryCorner, false)) {
            return true;
        }
        if(this.contains(otherClaim.greaterBoundaryCorner, false)) {
            return true;
        }
        if(this.contains(new Vector3i(smallX, 0, bigZ), false)) {
            return true;
        }
        if(this.contains(new Vector3i(bigX, 0, smallZ), false)) {
            return true;
        }

        boolean inArea = false;
        if(this.getLesserBoundaryCorner().getZ() <= bigZ &&
           this.getLesserBoundaryCorner().getZ() >= smallZ &&
           this.getLesserBoundaryCorner().getX() < smallX &&
           this.getGreaterBoundaryCorner().getX() > bigX)
           inArea = true;

        if( this.getGreaterBoundaryCorner().getZ() <= bigZ && 
            this.getGreaterBoundaryCorner().getZ() >= smallZ && 
            this.getLesserBoundaryCorner().getX() < smallX &&
            this.getGreaterBoundaryCorner().getX() > bigX )
            inArea = true;
        
        if( this.getLesserBoundaryCorner().getX() <= bigX && 
            this.getLesserBoundaryCorner().getX() >= smallX && 
            this.getLesserBoundaryCorner().getZ() < smallZ &&
            this.getGreaterBoundaryCorner().getZ() > bigZ )
            inArea = true;
            
        if( this.getGreaterBoundaryCorner().getX() <= bigX && 
            this.getGreaterBoundaryCorner().getX() >= smallX && 
            this.getLesserBoundaryCorner().getZ() < smallZ &&
            this.getGreaterBoundaryCorner().getZ() > bigZ )
            inArea = true;

        if (inArea) {
            // check height
            if ((this.lesserBoundaryCorner.getY() >= smallY &&
                 this.lesserBoundaryCorner.getY() <= bigY) ||
                (this.greaterBoundaryCorner.getY() <= smallY &&
                 this.greaterBoundaryCorner.getY() >= smallY)) {
                return true;
            }

            return false;
        }

        return false;
    }

    @Override
    public boolean isInside(Claim claim) {
        final GDClaim otherClaim = (GDClaim) claim;
        if(!otherClaim.contains(this.lesserBoundaryCorner)) {
            return false;
        }
        if(!otherClaim.contains(this.greaterBoundaryCorner)) {
            return false;
        }

        if(!otherClaim.contains(new Vector3i(this.lesserBoundaryCorner.getX(), this.lesserBoundaryCorner.getY(), this.greaterBoundaryCorner.getZ()))) {
            return false;
        }
        if(!otherClaim.contains(new Vector3i(this.greaterBoundaryCorner.getX(), this.greaterBoundaryCorner.getY(), this.lesserBoundaryCorner.getZ()))) {
            return false;
        }

        return true;
    }

    @Override
    public ArrayList<Vector3i> getChunkPositions() {
        ArrayList<Vector3i> chunkPositions = new ArrayList<Vector3i>();
        final Set<Long> chunkHashes = this.getChunkHashes(true);
        for (Long hash : chunkHashes) {
            //chunkPositions.add(ChunkPos.)
        }
        return chunkPositions;
    }

    public ArrayList<Chunk> getChunks() {
        ArrayList<Chunk> chunks = new ArrayList<Chunk>();

        Chunk lesserChunk = this.world
                .getChunkAt(this.getLesserBoundaryCorner().getX() >> 4, this.getLesserBoundaryCorner().getZ() >> 4);
        Chunk greaterChunk = this.world
                .getChunkAt(this.getGreaterBoundaryCorner().getX() >> 4, this.getGreaterBoundaryCorner().getZ() >> 4);

        if (lesserChunk != null && greaterChunk != null) {
            for (int x = lesserChunk.getX(); x <= greaterChunk.getX(); x++) {
                for (int z = lesserChunk.getZ(); z <= greaterChunk.getZ(); z++) {
                    Chunk chunk = world.getChunkAt(x, z);
                    if (chunk != null) {
                        chunks.add(chunk);
                    }
                }
            }
        }

        return chunks;
    }

    public boolean canIgnoreHeight() {
        if (this.isCuboid()) {
            return false;
        }

        if (this.ownerPlayerData != null && (this.getOwnerMinClaimLevel() > 0 || this.getOwnerMaxClaimLevel() < 255)) {
            return false;
        }

        return true;
    }

    public double getOwnerEconomyBlockCost() {
        return this.getOwnerEconomyBlockCost(this.ownerPlayerData);
    }

    public double getOwnerEconomyBlockCost(GDPlayerData playerData) {
        final GDPermissionHolder subject = playerData == null ? GriefDefenderPlugin.DEFAULT_HOLDER : playerData.getSubject();
        return GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Double.class), subject, Options.ECONOMY_BLOCK_COST).doubleValue();
    }

    public int getOwnerMinClaimLevel() {
        return this.getOwnerMinClaimLevel(this.ownerPlayerData);
    }

    public int getOwnerMinClaimLevel(GDPlayerData playerData) {
        final GDPermissionHolder subject = playerData == null ? GriefDefenderPlugin.DEFAULT_HOLDER : playerData.getSubject();
        return GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), subject, Options.MIN_LEVEL).intValue();
    }

    public int getOwnerMaxClaimLevel() {
        return this.getOwnerMaxClaimLevel(this.ownerPlayerData);
    }

    public int getOwnerMaxClaimLevel(GDPlayerData playerData) {
        final GDPermissionHolder subject = playerData == null ? GriefDefenderPlugin.DEFAULT_HOLDER : playerData.getSubject();
        return GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), subject, Options.MAX_LEVEL).intValue();
    }

    @Override
    public Set<Long> getChunkHashes() {
        return this.getChunkHashes(true);
    }

    public Set<Long> getChunkHashes(boolean refresh) {
        if (this.chunkHashes == null || refresh) {
            this.chunkHashes = new HashSet<Long>();
            int smallX = this.lesserBoundaryCorner.getX() >> 4;
            int smallZ = this.lesserBoundaryCorner.getZ() >> 4;
            int largeX = this.greaterBoundaryCorner.getX() >> 4;
            int largeZ = this.greaterBoundaryCorner.getZ() >> 4;
    
            for (int x = smallX; x <= largeX; x++) {
                for (int z = smallZ; z <= largeZ; z++) {
                    this.chunkHashes.add(BlockUtil.getInstance().asLong(x, z));
                }
            }
        }

        return this.chunkHashes;
    }

    @Override
    public ClaimData getData() {
        return (ClaimData) this.claimData;
    }

    public IClaimData getInternalClaimData() {
        return this.claimData;
    }

    @Nullable
    public TownDataConfig getTownData() {
        if (!(this.claimData instanceof TownDataConfig)) {
            return null;
        }

        return (TownDataConfig) this.claimData;
    }

    public ClaimStorageData getClaimStorage() {
        return this.claimStorage;
    }

    public void setClaimData(IClaimData data) {
        this.claimData = data;
    }

    public void setClaimStorage(ClaimStorageData storage) {
        this.claimStorage = storage;
    }

    public void updateClaimStorageData() {
        if (!this.isAdminClaim()) {
            this.claimStorage.getConfig().setOwnerUniqueId(this.getOwnerUniqueId());
        }
        this.claimStorage.getConfig().setWorldUniqueId(this.world.getUID());
        this.claimData.setCuboid(this.cuboid);
        this.claimData.setType(this.type);
        this.claimData.setLesserBoundaryCorner(BlockUtil.getInstance().posToString(this.lesserBoundaryCorner));
        this.claimData.setGreaterBoundaryCorner(BlockUtil.getInstance().posToString(this.greaterBoundaryCorner));
        // Will save next world save
        this.claimData.setRequiresSave(true);
    }

    public void save() {
        for (Claim child : this.children) {
            GDClaim childClaim = (GDClaim) child;
            if (childClaim.getInternalClaimData().requiresSave()) {
                childClaim.save();
            }
        }
        GDSaveClaimEvent.Pre preEvent = new GDSaveClaimEvent.Pre(this);
        GriefDefender.getEventManager().post(preEvent);
        if (this.getInternalClaimData().requiresSave()) {
            this.updateClaimStorageData();
            this.getClaimStorage().save();
            this.getInternalClaimData().setRequiresSave(false);
        }
        GDSaveClaimEvent.Post postEvent = new GDSaveClaimEvent.Post(this);
        GriefDefender.getEventManager().post(postEvent);
    }

    public boolean isPvpEnabled() {
        final boolean isPvPAllowed = this.world.getPVP();
        if (isPvPAllowed) {
            Tristate value = this.claimData.getPvpOverride();
            if (value != Tristate.UNDEFINED) {
                return value.asBoolean();
            }
        }

        return isPvPAllowed;
    }

    public void setPvpOverride(Tristate value) {
        this.claimData.setPvpOverride(value);
        this.getClaimStorage().save();
    }

    @Override
    public ClaimResult transferOwner(UUID newOwnerID) {
        return this.transferOwner(newOwnerID, false, false);
    }

    public ClaimResult transferOwner(UUID newOwnerID, boolean checkEconomy, boolean withdrawFunds) {
        if (this.isWilderness()) {
            return new GDClaimResult(ClaimResultType.WRONG_CLAIM_TYPE, TextComponent.builder("").append("The wilderness cannot be transferred.", TextColor.RED).build());
        }

        if (this.isAdminClaim()) {
            return new GDClaimResult(ClaimResultType.WRONG_CLAIM_TYPE, TextComponent.builder("").append("Admin claims cannot be transferred.", TextColor.RED).build());
        }

        GDPlayerData ownerData = DATASTORE.getOrCreatePlayerData(this.world, this.getOwnerUniqueId());
        // determine new owner
        GDPlayerData newOwnerData = DATASTORE.getOrCreatePlayerData(this.world, newOwnerID);

        if (this.isBasicClaim() && this.claimData.requiresClaimBlocks()) {
            if (GriefDefenderPlugin.getInstance().isEconomyModeEnabled()) {
                if (checkEconomy) {
                    final GDClaimResult result = EconomyUtil.getInstance().checkEconomyFunds(this, newOwnerData, withdrawFunds);
                    if (!result.successful()) {
                        return result;
                    }
                }
            } else {
                int remainingClaimBlocks = newOwnerData.getRemainingClaimBlocks();
                if (remainingClaimBlocks < 0 || (this.getClaimBlocks() > remainingClaimBlocks)) {
                    return new GDClaimResult(ClaimResultType.INSUFFICIENT_CLAIM_BLOCKS);
                }
            }
        }

        // Check limits
        final Player currentOwner = ownerData.getSubject() instanceof Player ? (Player) ownerData.getSubject() : null;
        final int createClaimLimit = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), newOwnerData.getSubject(), Options.CREATE_LIMIT, this);
        if (createClaimLimit > -1 && (newOwnerData.getClaimTypeCount(this.getType()) + 1) > createClaimLimit) {
            if (currentOwner != null) {
                GriefDefenderPlugin.sendMessage(currentOwner, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.CLAIM_TRANSFER_EXCEEDS_LIMIT));
            }
            return new GDClaimResult(this, ClaimResultType.EXCEEDS_MAX_CLAIM_LIMIT, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.CLAIM_TRANSFER_EXCEEDS_LIMIT));
        }

        // transfer
        GDTransferClaimEvent event = new GDTransferClaimEvent(this, this.getOwnerUniqueId(), newOwnerID);
        GriefDefender.getEventManager().post(event);
        if (event.cancelled()) {
            return new GDClaimResult(this, ClaimResultType.CLAIM_EVENT_CANCELLED, event.getMessage().orElse(null));
        }

        if (this.isAdminClaim()) {
            // convert to basic
            this.type = ClaimTypes.BASIC;
            this.getVisualizer().setType(ClaimVisual.BASIC);
            this.claimData.setType(ClaimTypes.BASIC);
        }

        this.ownerUniqueId = event.getNewOwner();
        if (!this.getOwnerUniqueId().equals(newOwnerID)) {
            newOwnerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(this.world, this.getOwnerUniqueId());
        }

        this.claimData.setOwnerUniqueId(newOwnerID);
        if (this.isBasicClaim()) {
            ownerData.getInternalClaims().remove(this);
            newOwnerData.getInternalClaims().add(this);
        }

        this.ownerPlayerData = newOwnerData;
        this.getClaimStorage().save();
        return new GDClaimResult(this, ClaimResultType.SUCCESS);
    }

    public ClaimResult findParent(GDClaim claimToSearch) {
        if (!this.isInside(claimToSearch)) {
            return new GDClaimResult(ClaimResultType.CLAIM_NOT_FOUND);
        }
        Claim current = claimToSearch;
        for (Claim child : current.getChildren(true)) {
            if (this.isInside(child)) {
                current = child;
            }
        }
        return new GDClaimResult(current, ClaimResultType.SUCCESS);
    }

    public ClaimResult doesClaimOverlap() {
        if (this.parent != null) {
            final GDClaim parentClaim = (GDClaim) this.parent;
            // 1 - Make sure new claim is inside parent
            if (!this.isInside(parentClaim)) {
                return new GDClaimResult(parentClaim, ClaimResultType.OVERLAPPING_CLAIM);
            }

            // 2 - Check parent children
            for (Claim child : parentClaim.children) {
                final GDClaim childClaim = (GDClaim) child;
                if (this.isBandingAcross(childClaim) || childClaim.isBandingAcross(this)) {
                    return new GDClaimResult(childClaim, ClaimResultType.OVERLAPPING_CLAIM);
                }
            }
            return new GDClaimResult(this, ClaimResultType.SUCCESS);
        }

        final GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(this.world.getUID());
        final Set<Long> chunkHashes = this.getChunkHashes(true);

        // Since there is no parent we need to check all claims stored in chunk hashes
        for (Long chunkHash : chunkHashes) {
            Set<Claim> claimsInChunk = claimWorldManager.getInternalChunksToClaimsMap().get(chunkHash);
            if (claimsInChunk == null || claimsInChunk.size() == 0) {
                continue;
            }
            for (Claim child : claimsInChunk) {
                final GDClaim gpChild = (GDClaim) child;
                // First check if newly resized claim is crossing another
                if (this.isBandingAcross(gpChild) || gpChild.isBandingAcross(this)) {
                    return new GDClaimResult(child, ClaimResultType.OVERLAPPING_CLAIM);
                }
            }
        }

        return new GDClaimResult(this, ClaimResultType.SUCCESS);
    }

    // Scans area for any overlaps and migrates children to a newly created or resized claim
    public ClaimResult checkArea(boolean resize) {
        final List<Claim> claimsInArea = new ArrayList<>();
        claimsInArea.add(this);

        if (this.parent != null) {
            return checkAreaParent(claimsInArea, resize);
        }

        final List<Claim> claimsToMigrate = new ArrayList<>();
        // First check children
        for (Claim child : this.children) {
            final GDClaim childClaim = (GDClaim) child;
            if (this.isBandingAcross(childClaim) || childClaim.isBandingAcross(this)) {
                return new GDClaimResult(childClaim, ClaimResultType.OVERLAPPING_CLAIM);
            }
            if (childClaim.isInside(this)) {
                if (!this.isAdminClaim()) {
                    if (this.type.equals(childClaim.type) || childClaim.isAdminClaim()) {
                        return new GDClaimResult(childClaim, ClaimResultType.OVERLAPPING_CLAIM);
                    }
                }
            } else {
                // child is no longer within parent
                // if resizing, migrate the child claim out
                if (resize) {
                    claimsToMigrate.add(childClaim);
                } else {
                    return new GDClaimResult(childClaim, ClaimResultType.OVERLAPPING_CLAIM);
                }
            }
        }

        if (!claimsToMigrate.isEmpty()) {
            ((GDClaim) this.wildernessClaim).migrateClaims(claimsToMigrate);
        }

        final GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(this.world.getUID());
        final Set<Long> chunkHashes = this.getChunkHashes(true);

        // Since there is no parent we need to check all claims stored in chunk hashes
        for (Long chunkHash : chunkHashes) {
            Set<Claim> claimsInChunk = claimWorldManager.getInternalChunksToClaimsMap().get(chunkHash);
            if (claimsInChunk == null || claimsInChunk.size() == 0) {
                continue;
            }
            for (Claim chunkClaim : claimsInChunk) {
                final GDClaim gpChunkClaim = (GDClaim) chunkClaim;
                if (gpChunkClaim.equals(this) || claimsInArea.contains(gpChunkClaim)) {
                    continue;
                }
                if (this.isAdminClaim() && gpChunkClaim.isAdminClaim() && gpChunkClaim.parent != null && gpChunkClaim.parent.equals(this)) {
                    continue;
                }

                // First check if new claim is crossing another
                if (this.isBandingAcross(gpChunkClaim) || gpChunkClaim.isBandingAcross(this)) {
                    return new GDClaimResult(gpChunkClaim, ClaimResultType.OVERLAPPING_CLAIM);
                }
                if (gpChunkClaim.isInside(this)) {
                     if (!this.isAdminClaim()) {
                        if (this.type.equals(gpChunkClaim.type) || gpChunkClaim.isAdminClaim()) {
                            return new GDClaimResult(gpChunkClaim, ClaimResultType.OVERLAPPING_CLAIM);
                        }
                    }
                    if (!this.canEnclose(gpChunkClaim)) {
                        return new GDClaimResult(gpChunkClaim, ClaimResultType.OVERLAPPING_CLAIM);
                    }
                    if (!this.isSubdivision()) {
                        claimsInArea.add(gpChunkClaim);
                    }
                } else if (this.isInside(gpChunkClaim)) {
                    // Fix WorldEdit issue
                    // Make sure to check if chunk claim can enclose newly created claim
                    if (!gpChunkClaim.canEnclose(this)) {
                        return new GDClaimResult(gpChunkClaim, ClaimResultType.OVERLAPPING_CLAIM);
                    }
                }
            }
        }

        return new GDClaimResult(claimsInArea, ClaimResultType.SUCCESS);
    }

    public ClaimResult checkAreaParent(List<Claim> claimsInArea, boolean resize) {
        if (this.isClaimOnBorder(this.parent)) {
            return new GDClaimResult(this.parent, ClaimResultType.OVERLAPPING_CLAIM);
        }
        final GDClaim parentClaim = (GDClaim) this.parent;
        // 1 - Make sure new claim is inside parent
        if (!this.isInside(parentClaim)) {
            return new GDClaimResult(parentClaim, ClaimResultType.OVERLAPPING_CLAIM);
        }

        // 2 - Check parent children
        for (Claim child : parentClaim.children) {
            final GDClaim childClaim = (GDClaim) child;
            if (this.equals(child)) {
                continue;
            }
            if (this.isBandingAcross(childClaim) || childClaim.isBandingAcross(this)) {
                return new GDClaimResult(childClaim, ClaimResultType.OVERLAPPING_CLAIM);
            }

            if (childClaim.isInside(this)) {
                if (!this.isAdminClaim()) {
                    if (this.type.equals(childClaim.type) || childClaim.isAdminClaim()) {
                        return new GDClaimResult(childClaim, ClaimResultType.OVERLAPPING_CLAIM);
                    }
                }
                if (!this.isSubdivision()) {
                    claimsInArea.add(childClaim);
                }
            }
            // ignore claims not inside
        }

        if (resize) {
            // Make sure children are still within their parent
            final List<Claim> claimsToMigrate = new ArrayList<>();
            for (Claim child : this.children) {
                GDClaim childClaim = (GDClaim) child;
                if (this.isBandingAcross(childClaim) || childClaim.isBandingAcross(this)) {
                    return new GDClaimResult(childClaim, ClaimResultType.OVERLAPPING_CLAIM);
                }
                if (!childClaim.isInside(this)) {
                    if (this.parent != null) {
                        claimsToMigrate.add(childClaim);
                    } else {
                        childClaim.parent = null;
                        this.children.remove(childClaim);
                        final GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(this.world.getUID());
                        claimWorldManager.addClaim(childClaim, true);
                    }
                }
            }
            if (!claimsToMigrate.isEmpty()) {
                this.parent.migrateClaims(claimsToMigrate);
            }
        }
        return new GDClaimResult(claimsInArea, ClaimResultType.SUCCESS);
    }

    public boolean canEnclose(Claim claim) {
        if (claim.isWilderness()) {
            return false;
        }
        if (this.isAdminClaim()) {
            // admin claims can enclose any type
            return true;
        }
        if (this.isSubdivision()) {
            return false;
        }
        if (this.isBasicClaim()) {
            if (!claim.isSubdivision()) {
                return false;
            }
            return true;
        }
        if (this.isTown()) {
            if (claim.isAdminClaim()) {
                return false;
            }
            return true;
        }
        return true;
    }

    // Checks to see if the passed in claim is a parent of this claim
    @Override
    public boolean isParent(Claim claim) {
        if (this.parent == null) {
            return false;
        }

        GDClaim parent = this.parent;
        while (parent != null) {
            if (parent.getUniqueId().equals(claim.getUniqueId())) {
                return true;
            }
            parent = parent.parent;
        }

        return false;
    }

    @Override
    public ClaimResult resize(int x1, int x2, int y1, int y2, int z1, int z2) {
        int minx = Math.min(x1, x2);
        int miny = Math.min(y1, y2);
        int minz = Math.min(z1, z2);
        int maxx = Math.max(x1, x2);
        int maxy = Math.max(y1, y2);
        int maxz = Math.max(z1, z2);

        final Object root = GDCauseStackManager.getInstance().getCurrentCause().root();
        final GDPermissionUser user = root instanceof GDPermissionUser ? (GDPermissionUser) root : null;
        final Player player = user != null ? user.getOnlinePlayer() : null;
        if (this.cuboid) {
            return resizeCuboid(player, minx, miny, minz, maxx, maxy, maxz);
        }

        Location startCorner = null;
        Location endCorner = null;
        GDPlayerData playerData = null;
        if (player != null) {
            playerData = GriefDefenderPlugin.getInstance().dataStore.getPlayerData(this.world, player.getUniqueId());
        } else if (!this.isAdminClaim() && this.ownerUniqueId != null) {
            playerData = GriefDefenderPlugin.getInstance().dataStore.getPlayerData(this.world, this.ownerUniqueId);
        }

        if (playerData == null) {
            if (GriefDefenderPlugin.getActiveConfig(this.world.getUID()).getConfig().claim.claimAutoSchematicRestore) {
                return new GDClaimResult(this, ClaimResultType.FAILURE);
            }
            startCorner = new Location(this.world, minx, miny, minz);
            endCorner = new Location(this.world, maxx, maxy, maxz);
        } else {
            if (!playerData.canIgnoreClaim(this) && GriefDefenderPlugin.getActiveConfig(this.world.getUID()).getConfig().claim.claimAutoSchematicRestore) {
                return new GDClaimResult(this, ClaimResultType.FAILURE);
            }
            startCorner = playerData.lastShovelLocation;
            endCorner = playerData.endShovelLocation;
        }

        // Auto-adjust Y levels for 2D claims
        if (playerData != null) {
            miny = this.getOwnerMinClaimLevel();
        }
        if (playerData != null) {
            maxy = this.getOwnerMaxClaimLevel();
        }
        Vector3i currentLesserCorner = this.getLesserBoundaryCorner();
        Vector3i currentGreaterCorner = this.getGreaterBoundaryCorner();
        Vector3i newLesserCorner = new Vector3i(minx, miny, minz);
        Vector3i newGreaterCorner = new Vector3i(maxx, maxy, maxz);

        GDChangeClaimEvent.Resize event = new GDChangeClaimEvent.Resize(this, startCorner, endCorner);
        GriefDefender.getEventManager().post(event);
        if (event.cancelled()) {
            return new GDClaimResult(this, ClaimResultType.CLAIM_EVENT_CANCELLED);
        }

        // check player has enough claim blocks
        if ((this.isBasicClaim() || this.isTown()) && this.claimData.requiresClaimBlocks()) {
            final int newCost = BlockUtil.getInstance().getClaimBlockCost(this.world, newLesserCorner, newGreaterCorner, this.cuboid);
            final int currentCost = BlockUtil.getInstance().getClaimBlockCost(this.world, currentLesserCorner, currentGreaterCorner, this.cuboid);
            if (GriefDefenderPlugin.getInstance().isEconomyModeEnabled()) {
                if (!this.vaultProvider.getApi().hasAccount(player)) {
                    return new GDClaimResult(ClaimResultType.ECONOMY_ACCOUNT_NOT_FOUND);
                }

                final double requiredFunds = Math.abs((newCost - currentCost) * this.getOwnerEconomyBlockCost());
                if (newCost > currentCost) {
                    final double currentFunds = this.vaultProvider.getApi().getBalance(player);
                    final EconomyResponse result = this.vaultProvider.getApi().withdrawPlayer(player, requiredFunds);
                    if (!result.transactionSuccess()) {
                        Component message = null;
                        if (player != null) {
                            message = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.ECONOMY_NOT_ENOUGH_FUNDS, ImmutableMap.of(
                                    "balance", this.vaultProvider.getApi().getBalance(player),
                                    "amount", requiredFunds));
                            GriefDefenderPlugin.sendMessage(player, message);
                        }
    
                        playerData.lastShovelLocation = null;
                        playerData.claimResizing = null;
                        return new GDClaimResult(ClaimResultType.ECONOMY_NOT_ENOUGH_FUNDS, message);
                    }
                } else {
                    final EconomyResponse result = this.vaultProvider.getApi().depositPlayer(player, requiredFunds);
                }
            } else if (newCost > currentCost) {
                final int remainingClaimBlocks = this.ownerPlayerData.getRemainingClaimBlocks() - (newCost - currentCost);
                if (remainingClaimBlocks < 0) {
                    if (player != null) {
                        if (GriefDefenderPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.VOLUME) {
                            final double claimableChunks = Math.abs(remainingClaimBlocks / 65536.0);
                            GriefDefenderPlugin.sendMessage(player, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.CLAIM_SIZE_NEED_BLOCKS_3D, ImmutableMap.of(
                                    "chunk-amount", Math.round(claimableChunks * 100.0)/100.0,
                                    "block-amount", Math.abs(remainingClaimBlocks))));
                        } else {
                            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_SIZE_NEED_BLOCKS_2D, ImmutableMap.of(
                                    "block-amount", Math.abs(remainingClaimBlocks))));
                        }
                    }
                    playerData.lastShovelLocation = null;
                    playerData.claimResizing = null;
                    this.lesserBoundaryCorner = currentLesserCorner;
                    this.greaterBoundaryCorner = currentGreaterCorner;
                    return new GDClaimResult(ClaimResultType.INSUFFICIENT_CLAIM_BLOCKS);
                }
            }
        }

        this.lesserBoundaryCorner = newLesserCorner;
        this.greaterBoundaryCorner = newGreaterCorner;

        // checkArea refreshes the current chunk hashes so it is important
        // to make a copy before making the call
        final Set<Long> currentChunkHashes = new HashSet<>(this.chunkHashes);

        final ClaimResult result = this.checkArea(true);
        if (!result.successful()) {
            this.lesserBoundaryCorner = currentLesserCorner;
            this.greaterBoundaryCorner = currentGreaterCorner;
            return result;
        }

        if (this.type != ClaimTypes.ADMIN && this.type != ClaimTypes.WILDERNESS) {
            ClaimResult claimResult = checkSizeLimits(player, playerData, newLesserCorner, newGreaterCorner);
            if (!claimResult.successful()) {
                this.lesserBoundaryCorner = currentLesserCorner;
                this.greaterBoundaryCorner = currentGreaterCorner;
                return claimResult;
            }
        }

        // This needs to be adjusted before we check for overlaps
        this.lesserBoundaryCorner = newLesserCorner;
        this.greaterBoundaryCorner = newGreaterCorner;
        GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(this.world.getUID());

        // resize validated, remove invalid chunkHashes
        if (this.parent == null) {
            for (Long chunkHash : currentChunkHashes) {
                Set<Claim> claimsInChunk = claimWorldManager.getInternalChunksToClaimsMap().get(chunkHash);
                if (claimsInChunk != null && claimsInChunk.size() > 0) {
                    claimsInChunk.remove(this);
                }
            }

            final Set<Long> newChunkHashes = this.getChunkHashes(true);
            // add new chunk hashes
            for (Long chunkHash : newChunkHashes) {
                Set<Claim> claimsInChunk = claimWorldManager.getInternalChunksToClaimsMap().get(chunkHash);
                if (claimsInChunk == null) {
                    claimsInChunk = new HashSet<>();
                    claimWorldManager.getInternalChunksToClaimsMap().put(chunkHash, claimsInChunk);
                }

                claimsInChunk.add(this);
            }
        }

        this.claimData.setLesserBoundaryCorner(BlockUtil.getInstance().posToString(this.lesserBoundaryCorner));
        this.claimData.setGreaterBoundaryCorner(BlockUtil.getInstance().posToString(this.greaterBoundaryCorner));
        this.claimData.setRequiresSave(true);
        this.getClaimStorage().save();

        if (result.getClaims().size() > 1) {
            this.migrateClaims(new ArrayList<>(result.getClaims()));
        }
        this.resetVisuals();
        return new GDClaimResult(this, ClaimResultType.SUCCESS);
    }

    public ClaimResult resizeCuboid(Player player, int smallX, int smallY, int smallZ, int bigX, int bigY, int bigZ) {
        // make sure resize doesn't cross paths
        if (smallX >= bigX || smallY >= bigY || smallZ >= bigZ) {
            return new GDClaimResult(this, ClaimResultType.OVERLAPPING_CLAIM);
        }

        Location startCorner = null;
        Location endCorner = null;
        GDPlayerData playerData = null;

        if (player != null) {
            playerData = GriefDefenderPlugin.getInstance().dataStore.getPlayerData(this.world, player.getUniqueId());
        } else if (!this.isAdminClaim() && this.ownerUniqueId != null) {
            playerData = GriefDefenderPlugin.getInstance().dataStore.getPlayerData(this.world, this.ownerUniqueId);
        }

        if (playerData == null) {
            if (GriefDefenderPlugin.getActiveConfig(this.world.getUID()).getConfig().claim.claimAutoSchematicRestore) {
                return new GDClaimResult(this, ClaimResultType.FAILURE);
            }
            startCorner = new Location(this.world, smallX, smallY, smallZ);
            endCorner = new Location(this.world, bigX, bigY, bigZ);
        } else {
            if (!playerData.canIgnoreClaim(this) && GriefDefenderPlugin.getActiveConfig(this.world.getUID()).getConfig().claim.claimAutoSchematicRestore) {
                return new GDClaimResult(this, ClaimResultType.FAILURE);
            }
            startCorner = playerData.lastShovelLocation;
            endCorner = playerData.endShovelLocation;
        }

        GDChangeClaimEvent.Resize event = new GDChangeClaimEvent.Resize(this, startCorner, endCorner);
        GriefDefender.getEventManager().post(event);
        if (event.cancelled()) {
            return new GDClaimResult(this, ClaimResultType.CLAIM_EVENT_CANCELLED);
        }

        final int minClaimLevel = this.getOwnerMinClaimLevel();
        if (playerData != null && playerData.shovelMode != ShovelTypes.ADMIN && smallY < minClaimLevel) {
            final Component message = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.CLAIM_BELOW_LEVEL, ImmutableMap.of(
                    "limit", minClaimLevel));
            GriefDefenderPlugin.sendMessage(player, message);
            return new GDClaimResult(ClaimResultType.BELOW_MIN_LEVEL);
        }
        final int maxClaimLevel = this.getOwnerMaxClaimLevel();
        if (playerData != null && playerData.shovelMode != ShovelTypes.ADMIN && bigY > maxClaimLevel) {
            final Component message = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.CLAIM_ABOVE_LEVEL, ImmutableMap.of(
                    "limit", maxClaimLevel));
            GriefDefenderPlugin.sendMessage(player, message);
            return new GDClaimResult(ClaimResultType.ABOVE_MAX_LEVEL);
        }
        // check if child extends past parent limits
        if (this.parent != null) {
            if (smallX < this.parent.getLesserBoundaryCorner().getX() ||
                smallY < this.parent.getLesserBoundaryCorner().getY() ||
                smallZ < this.parent.getLesserBoundaryCorner().getZ()) {
                return new GDClaimResult(this.parent, ClaimResultType.OVERLAPPING_CLAIM);
            }
            if (bigX > this.parent.getGreaterBoundaryCorner().getX() ||
                (this.parent.isCuboid() && bigY > this.parent.getGreaterBoundaryCorner().getY()) ||
                bigZ > this.parent.getGreaterBoundaryCorner().getZ()) {
                return new GDClaimResult(this.parent, ClaimResultType.OVERLAPPING_CLAIM);
            }
        }

        Vector3i currentLesserCorner = this.lesserBoundaryCorner;
        Vector3i currentGreaterCorner = this.greaterBoundaryCorner;
        Vector3i newLesserCorner = new Vector3i(smallX, smallY, smallZ);
        Vector3i newGreaterCorner = new Vector3i(bigX, bigY, bigZ);
        this.lesserBoundaryCorner = newLesserCorner;
        this.greaterBoundaryCorner = newGreaterCorner;

        // checkArea refreshes the current chunk hashes so it is important
        // to make a copy before making the call
        final Set<Long> currentChunkHashes = new HashSet<>(this.chunkHashes);

        final ClaimResult result = this.checkArea(true);
        if (!result.successful()) {
            this.lesserBoundaryCorner = currentLesserCorner;
            this.greaterBoundaryCorner = currentGreaterCorner;
            return result;
        }

        if (this.type != ClaimTypes.ADMIN && this.type != ClaimTypes.WILDERNESS) {
            ClaimResult claimResult = checkSizeLimits(player, playerData, newLesserCorner, newGreaterCorner);
            if (!claimResult.successful()) {
                this.lesserBoundaryCorner = currentLesserCorner;
                this.greaterBoundaryCorner = currentGreaterCorner;
                return claimResult;
            }
        }

        this.lesserBoundaryCorner = newLesserCorner;
        this.greaterBoundaryCorner = newGreaterCorner;
        // resize validated, remove invalid chunkHashes
        final GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(this.world.getUID());
        if (this.parent == null) {
            for (Long chunkHash : currentChunkHashes) {
                Set<Claim> claimsInChunk = claimWorldManager.getInternalChunksToClaimsMap().get(chunkHash);
                if (claimsInChunk != null && claimsInChunk.size() > 0) {
                    claimsInChunk.remove(this);
                }
            }

            final Set<Long> newChunkHashes = this.getChunkHashes(true);
            // add new chunk hashes
            for (Long chunkHash : newChunkHashes) {
                Set<Claim> claimsInChunk = claimWorldManager.getInternalChunksToClaimsMap().get(chunkHash);
                if (claimsInChunk == null) {
                    claimsInChunk = new HashSet<>();
                    claimWorldManager.getInternalChunksToClaimsMap().put(chunkHash, claimsInChunk);
                }

                claimsInChunk.add(this);
            }
        }

        this.claimData.setLesserBoundaryCorner(BlockUtil.getInstance().posToString(this.lesserBoundaryCorner));
        this.claimData.setGreaterBoundaryCorner(BlockUtil.getInstance().posToString(this.greaterBoundaryCorner));
        this.claimData.setRequiresSave(true);
        this.getClaimStorage().save();
        if (result.getClaims().size() > 1) {
            this.migrateClaims(new ArrayList<>(result.getClaims()));
        }
        this.resetVisuals();
        return new GDClaimResult(this, ClaimResultType.SUCCESS);
    }

    private ClaimResult checkSizeLimits(Player player, GDPlayerData playerData, Vector3i lesserCorner, Vector3i greaterCorner) {
        if (playerData == null) {
            return new GDClaimResult(ClaimResultType.SUCCESS);
        }

        final GDPermissionHolder holder = PermissionHolderCache.getInstance().getOrCreateUser(playerData.playerID);
        final int minClaimX = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), holder, Options.MIN_SIZE_X, this);
        final int minClaimY = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), holder, Options.MIN_SIZE_Y, this);
        final int minClaimZ = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), holder, Options.MIN_SIZE_Z, this);
        final int maxClaimX = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), holder, Options.MAX_SIZE_X, this);
        final int maxClaimY = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), holder, Options.MAX_SIZE_Y, this);
        final int maxClaimZ = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), holder, Options.MAX_SIZE_Z, this);

        // Handle single block selection
        if ((this.isCuboid() && greaterCorner.equals(lesserCorner)) || (!this.isCuboid() && greaterCorner.getX() == lesserCorner.getX() && greaterCorner.getZ() == lesserCorner.getZ())) {
            if (playerData.claimResizing != null) {
                final Component message = MessageCache.getInstance().RESIZE_SAME_LOCATION;
                GriefDefenderPlugin.sendMessage(player, message);
                playerData.lastShovelLocation = null;
                playerData.claimResizing = null;
                // TODO: Add new result type for this
                return new GDClaimResult(ClaimResultType.BELOW_MIN_SIZE_X, message);
            }
            if (playerData.claimSubdividing == null) {
                final Component message = MessageCache.getInstance().CREATE_SUBDIVISION_ONLY;
                GriefDefenderPlugin.sendMessage(player, message);
                playerData.lastShovelLocation = null;
                playerData.claimResizing = null;
                // TODO: Add new result type for this
                return new GDClaimResult(ClaimResultType.BELOW_MIN_SIZE_X, message);
            }
        }
        Component message = null;
        String maxCuboidArea = maxClaimX + "x" + maxClaimY + "x" + maxClaimZ;
        if (maxClaimX == 0 && maxClaimY == 0 && maxClaimZ == 0) {
            maxCuboidArea = "∞";
        }
        String maxArea = maxClaimX + "x" + maxClaimZ;
        if (maxClaimX == 0 && maxClaimZ == 0) {
            maxArea = "∞";
        }

        if (maxClaimX > 0) {
            int size = Math.abs(greaterCorner.getX() - lesserCorner.getX()) + 1;
            if (size > maxClaimX) {
                if (player != null) {
                    if (this.isCuboid()) {
                        message = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.CLAIM_SIZE_MAX, ImmutableMap.of(
                                "axis", "x",
                                "size", size,
                                "max-size", maxClaimX == 0 ? "∞" : maxClaimX,
                                "min-area", minClaimX + "x" + minClaimY + "x" + minClaimZ,
                                "max-area", maxCuboidArea));
                    } else {
                        message = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.CLAIM_SIZE_MAX, ImmutableMap.of(
                                "axis", "x",
                                "size", size,
                                "max-size", maxClaimX == 0 ? "∞" : maxClaimX,
                                "min-area", minClaimX + "x" + minClaimZ,
                                "max-area", maxArea));
                    }
                    GriefDefenderPlugin.sendMessage(player, message);
                }
                return new GDClaimResult(ClaimResultType.EXCEEDS_MAX_SIZE_X, message);
            }
        }
        if (this.cuboid && maxClaimY > 0) {
            int size = Math.abs(greaterCorner.getY() - lesserCorner.getY()) + 1;
            if (size > maxClaimY) {
                if (player != null) {
                    if (this.isCuboid()) {
                        message = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.CLAIM_SIZE_MAX, ImmutableMap.of(
                                "axis", "y",
                                "size", size,
                                "max-size", maxClaimY == 0 ? "∞" : maxClaimY,
                                "min-area", minClaimX + "x" + minClaimY + "x" + minClaimZ,
                                "max-area", maxCuboidArea));
                    } else {
                        message = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.CLAIM_SIZE_MAX, ImmutableMap.of(
                                "axis", "y",
                                "size", size,
                                "max-size", maxClaimY == 0 ? "∞" : maxClaimY,
                                "min-area", minClaimX + "x" + minClaimZ,
                                "max-area", maxArea));
                    }
                    GriefDefenderPlugin.sendMessage(player, message);
                }
                return new GDClaimResult(ClaimResultType.EXCEEDS_MAX_SIZE_Y, message);
            }
        }
        if (maxClaimZ > 0) {
            int size = Math.abs(greaterCorner.getZ() - lesserCorner.getZ()) + 1;
            if (size > maxClaimZ) {
                if (player != null) {
                    if (this.isCuboid()) {
                        message = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.CLAIM_SIZE_MAX, ImmutableMap.of(
                                "axis", "z",
                                "size", size,
                                "max-size", maxClaimZ == 0 ? "∞" : maxClaimZ,
                                "min-area", minClaimX + "x" + minClaimY + "x" + minClaimZ,
                                "max-area", maxCuboidArea));
                    } else {
                        message = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.CLAIM_SIZE_MAX, ImmutableMap.of(
                                "axis", "z",
                                "size", size,
                                "max-size", maxClaimZ == 0 ? "∞" : maxClaimZ,
                                "min-area", minClaimX + "x" + minClaimZ,
                                "max-area", maxArea));
                    }
                    GriefDefenderPlugin.sendMessage(player, message);
                }
                return new GDClaimResult(ClaimResultType.EXCEEDS_MAX_SIZE_Z, message);
            }
        }
        if (minClaimX > 0) {
            int size = Math.abs(greaterCorner.getX() - lesserCorner.getX()) + 1;
            if (size < minClaimX) {
                if (player != null) {
                    if (this.isCuboid()) {
                        message = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.CLAIM_SIZE_MIN, ImmutableMap.of(
                                "axis", "x",
                                "size", size,
                                "min-size", minClaimX == 0 ? "∞" : minClaimX,
                                "min-area", minClaimX + "x" + minClaimY + "x" + minClaimZ,
                                "max-area", maxCuboidArea));
                    } else {
                        message = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.CLAIM_SIZE_MIN, ImmutableMap.of(
                                "axis", "x",
                                "size", size,
                                "min-size", minClaimX,
                                "min-area", minClaimX + "x" + minClaimZ,
                                "max-area", maxArea));
                    }
                    GriefDefenderPlugin.sendMessage(player, message);
                }
                return new GDClaimResult(ClaimResultType.BELOW_MIN_SIZE_X, message);
            }
        }
        if (this.cuboid && minClaimY > 0) {
            int size = Math.abs(greaterCorner.getY() - lesserCorner.getY()) + 1;
            if (size < minClaimY) {
                if (player != null) {
                    if (this.isCuboid()) {
                        message = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.CLAIM_SIZE_MIN, ImmutableMap.of(
                                "axis", "y",
                                "size", size,
                                "min-size", minClaimY == 0 ? "∞" : minClaimY,
                                "min-area", minClaimX + "x" + minClaimY + "x" + minClaimZ,
                                "max-area", maxCuboidArea));
                    } else {
                        message = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.CLAIM_SIZE_MIN, ImmutableMap.of(
                                "axis", "y",
                                "size", size,
                                "min-size", minClaimY == 0 ? "∞" : minClaimY,
                                "min-area", minClaimX + "x" + minClaimZ,
                                "max-area", maxArea));
                    }
                    GriefDefenderPlugin.sendMessage(player, message);
                }
                return new GDClaimResult(ClaimResultType.BELOW_MIN_SIZE_Y, message);
            }
        }
        if (minClaimZ > 0) {
            int size = Math.abs(greaterCorner.getZ() - lesserCorner.getZ()) + 1;
            if (size < minClaimZ) {
                if (player != null) {
                    if (this.isCuboid()) {
                        message = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.CLAIM_SIZE_MIN, ImmutableMap.of(
                                "axis", "z",
                                "size", size,
                                "min-size", minClaimZ == 0 ? "∞" : minClaimZ,
                                "min-area", minClaimX + "x" + minClaimY + "x" + minClaimZ,
                                "max-area", maxCuboidArea));
                    } else {
                        message = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.CLAIM_SIZE_MIN, ImmutableMap.of(
                                "axis", "z",
                                "size", size,
                                "min-size", minClaimZ == 0 ? "∞" : minClaimZ,
                                "min-area", minClaimX + "x" + minClaimZ,
                                "max-area", maxArea));
                    }
                    GriefDefenderPlugin.sendMessage(player, message);
                }
                return new GDClaimResult(ClaimResultType.BELOW_MIN_SIZE_Z, message);
            }
        }

        return new GDClaimResult(ClaimResultType.SUCCESS);
    }

    public void unload() {
        // clear any references
        this.world = null;
        if (this.ownerPlayerData != null) {
            this.ownerPlayerData.getInternalClaims().remove(this);
        }
    }

    @Override
    public Claim getWilderness() {
        return this.wildernessClaim;
    }

    @Override
    public ClaimManager getClaimManager() {
        return (ClaimManager) this.worldClaimManager;
    }

    @Override
    public Context getContext() {
        return this.context;
    }
 
    public Context getInheritContext() {
        if (this.parent == null || !this.getData().doesInheritParent()) {
            return this.context;
        }

        return this.parent.getInheritContext();
    }

    public boolean hasAdminParent() {
        if (this.parent == null || this.isAdminClaim()) {
            return false;
        }

        if (this.parent.isAdminClaim()) {
            return true;
        }

        return this.parent.hasAdminParent();
    }

    @Override
    public boolean extend(int newDepth) {
        return false;
    }

    @Override
    public Optional<Claim> getParent() {
        return Optional.ofNullable(this.parent);
    }

    @Override
    public UUID getWorldUniqueId() {
        return this.world.getUID();
    }

    public World getWorld() {
        return this.world;
    }

    public void setWorld(World world) {
        this.world = world;
    }
    /*@Override
    public List<Entity> getEntities() {
        Collection<Entity> worldEntityList = Bukkit.getServer().getWorld(this.world.getUID()).getEntities();
        List<Entity> entityList = new ArrayList<>();
        for (Entity entity : worldEntityList) {
            if (!(entity.isDead() && this.contains(VecHelper.toVector3i(entity.getLocation())))) {
                entityList.add(entity);
            }
        }

        return entityList;
    }*/

    @Override
    public List<UUID> getPlayers() {
        Collection<Player> worldPlayerList = Bukkit.getServer().getWorld(this.world.getUID()).getPlayers();
        List<UUID> playerList = new ArrayList<>();
        for (Player player : worldPlayerList) {
            if (!player.isDead() && this.contains(VecHelper.toVector3i(player.getLocation()))) {
                playerList.add(player.getUniqueId());
            }
        }

        return playerList;
    }

    @Override
    public Set<Claim> getChildren(boolean recursive) {
        if (recursive) {
            Set<Claim> claimList = new HashSet<>(this.children);
            List<Claim> subChildren = new ArrayList<>();
            for (Claim child : claimList) {
                GDClaim childClaim = (GDClaim) child;
                if (!childClaim.children.isEmpty()) {
                    subChildren.addAll(childClaim.getChildren(true));
                }
            }
            claimList.addAll(subChildren);
            return claimList;
        }
        return ImmutableSet.copyOf(this.children);
    }

    @Override
    public List<Claim> getParents(boolean recursive) {
        List<Claim> parents = new ArrayList<>();
        GDClaim currentClaim = this;
        while (currentClaim.parent != null) {
            parents.add(currentClaim.parent);
            currentClaim = currentClaim.parent;
        }

        // Index 0 is highest parent while last index represents direct
        Collections.reverse(parents);
        return ImmutableList.copyOf(parents);
    }

    public List<Claim> getInheritedParents() {
        List<Claim> parents = new ArrayList<>();
        GDClaim currentClaim = this;
        while (currentClaim.parent != null && (currentClaim.getData() == null || currentClaim.getData().doesInheritParent())) {
            if (currentClaim.isAdminClaim()) {
                if (currentClaim.parent.isAdminClaim()) {
                    parents.add(currentClaim.parent);
                }
            } else {
                parents.add(currentClaim.parent);
            }
            currentClaim = currentClaim.parent;
        }

        // Index 0 is highest parent while last index represents direct
        Collections.reverse(parents);
        return ImmutableList.copyOf(parents);
    }

    @Override
    public ClaimResult deleteChild(Claim child) {
        boolean found = false;
        for (Claim childClaim : this.children) {
            if (childClaim.getUniqueId().equals(child.getUniqueId())) {
                found = true;
            }
        }

        if (!found) {
            return new GDClaimResult(ClaimResultType.CLAIM_NOT_FOUND);
        }

        final GDClaimManager claimManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(this.world.getUID());
        return claimManager.deleteClaim(child, true);
    }

    @Override
    public ClaimResult deleteChildren() {
        return this.deleteChildren(null);
    }

    @Override
    public ClaimResult deleteChildren(ClaimType claimType) {
        List<Claim> claimList = new ArrayList<>();
        for (Claim child : this.children) {
            if (claimType == null || child.getType() == claimType) {
                claimList.add(child);
            }
        }

        if (claimList.isEmpty()) {
            return new GDClaimResult(ClaimResultType.CLAIM_NOT_FOUND);
        }

        GDRemoveClaimEvent event = new GDRemoveClaimEvent(claimList);
        GriefDefender.getEventManager().post(event);
        if (event.cancelled()) {
            return new GDClaimResult(claimList, ClaimResultType.CLAIM_EVENT_CANCELLED);
        }

        final GDClaimManager claimManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(this.world.getUID());
        for (Claim child : claimList) {
            claimManager.deleteClaimInternal(child, true);
        }

        return new GDClaimResult(event.getClaims(), ClaimResultType.SUCCESS);
    }

    @Override
    public ClaimResult changeType(ClaimType type, Optional<UUID> ownerUniqueId) {
        return changeType(type, ownerUniqueId, null);
    }

    public ClaimResult changeType(ClaimType type, Optional<UUID> ownerUniqueId, CommandSender src) {
        if (type == this.type) {
            return new GDClaimResult(ClaimResultType.SUCCESS);
        }

        GDChangeClaimEvent.Type event = new GDChangeClaimEvent.Type(this, type);
        GriefDefender.getEventManager().post(event);
        if (event.cancelled()) {
            return new GDClaimResult(ClaimResultType.CLAIM_EVENT_CANCELLED, event.getMessage().orElse(null));
        }

        final GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(this.world.getUID());
        final GDPlayerData sourcePlayerData = src != null && src instanceof Player ? claimWorldManager.getOrCreatePlayerData(((Player) src).getUniqueId()) : null;
        UUID newOwnerUUID = ownerUniqueId.orElse(this.ownerUniqueId);
        final ClaimResult result = this.validateClaimType(type, newOwnerUUID, sourcePlayerData);
        if (!result.successful()) {
            return result;
        }

        if (type == ClaimTypes.ADMIN) {
            newOwnerUUID = GriefDefenderPlugin.ADMIN_USER_UUID;
        }

        final String fileName = this.getClaimStorage().filePath.getFileName().toString();
        final Path newPath = this.getClaimStorage().folderPath.getParent().resolve(type.getName().toLowerCase()).resolve(fileName);
        try {
            if (Files.notExists(newPath.getParent())) {
                Files.createDirectories(newPath.getParent());
            }
            Files.move(this.getClaimStorage().filePath, newPath);
            if (type == ClaimTypes.TOWN) {
                this.setClaimStorage(new TownStorageData(newPath, this.getWorldUniqueId(), newOwnerUUID, this.cuboid));
            } else {
                this.setClaimStorage(new ClaimStorageData(newPath, this.getWorldUniqueId(), (ClaimDataConfig) this.getInternalClaimData()));
            }
            this.claimData = this.claimStorage.getConfig();
            this.getClaimStorage().save();
        } catch (IOException e) {
            e.printStackTrace();
            return new GDClaimResult(ClaimResultType.CLAIM_NOT_FOUND, TextComponent.of(e.getMessage()));
        }

        // If switched to admin or new owner, remove from player claim list
        if (type == ClaimTypes.ADMIN || !this.ownerUniqueId.equals(newOwnerUUID)) {
            final Set<Claim> currentPlayerClaims = claimWorldManager.getInternalPlayerClaims(this.ownerUniqueId);
            if (currentPlayerClaims != null) {
                currentPlayerClaims.remove(this);
            }
        }
        if (type != ClaimTypes.ADMIN) {
            final Set<Claim> newPlayerClaims = claimWorldManager.getInternalPlayerClaims(newOwnerUUID);
            if (newPlayerClaims != null && !newPlayerClaims.contains(this)) {
                newPlayerClaims.add(this);
            }
        }

        if (!this.isAdminClaim() && this.ownerPlayerData != null) {
            final Player player = Bukkit.getServer().getPlayer(this.ownerUniqueId);
            if (player != null) {
                this.ownerPlayerData.revertActiveVisual(player);
            }
        }

        // revert visuals for all players watching this claim
        List<UUID> playersWatching = new ArrayList<>(this.playersWatching);
        for (UUID playerUniqueId : playersWatching) {
            final Player spongePlayer = Bukkit.getServer().getPlayer(playerUniqueId);
            final GDPlayerData playerData = claimWorldManager.getOrCreatePlayerData(playerUniqueId);
            if (spongePlayer != null) {
                playerData.revertActiveVisual(spongePlayer);
            }
        }

        if (!newOwnerUUID.equals(GriefDefenderPlugin.ADMIN_USER_UUID)) {
            this.setOwnerUniqueId(newOwnerUUID);
        }
        this.setType(type);
        this.claimVisual = null;
        this.getInternalClaimData().setRequiresSave(true);
        this.getClaimStorage().save();
        return new GDClaimResult(ClaimResultType.SUCCESS);
    }

    public ClaimResult validateClaimType(ClaimType type, UUID newOwnerUUID, GDPlayerData playerData) {
        boolean isAdmin = false;
        if (playerData != null && (playerData.canManageAdminClaims || playerData.canIgnoreClaim(this))) {
            isAdmin = true;
        }

        GDPermissionUser user = null;
        if (newOwnerUUID != null) {
            user = PermissionHolderCache.getInstance().getOrCreateUser(newOwnerUUID);
        }

        if (type == ClaimTypes.ADMIN) {
            if (!isAdmin) {
                final Component message = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.RESULT_TYPE_CHANGE_NOT_ADMIN,
                        ImmutableMap.of("type", TextComponent.of("ADMIN").color(TextColor.RED)));
                return new GDClaimResult(ClaimResultType.WRONG_CLAIM_TYPE, message);
            }
        } else if (type == ClaimTypes.BASIC) {
            if (this.isAdminClaim() && newOwnerUUID == null) {
                return new GDClaimResult(ClaimResultType.REQUIRES_OWNER, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.RESULT_TYPE_REQUIRES_OWNER,
                        ImmutableMap.of(
                            "type", TextComponent.of("ADMIN", TextColor.RED),
                            "target_type", TextComponent.of("BASIC", TextColor.GREEN))));
            }
            if (this.parent != null && this.parent.isBasicClaim()) {
                final Component message = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.RESULT_TYPE_CHILD_SAME,
                        ImmutableMap.of("type", TextComponent.of("BASIC").color(TextColor.GREEN)));
                return new GDClaimResult(ClaimResultType.WRONG_CLAIM_TYPE, message);
            }
            for (Claim child : this.children) {
                if (!child.isSubdivision()) {
                    final Component message = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.RESULT_TYPE_ONLY_SUBDIVISION,
                            ImmutableMap.of("type", TextComponent.of("BASIC").color(TextColor.GREEN)));
                    return new GDClaimResult(ClaimResultType.WRONG_CLAIM_TYPE, message);
                }
            }
        } else if (type == ClaimTypes.SUBDIVISION) {
            if (!this.children.isEmpty()) {
                final Component message = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.RESULT_TYPE_NO_CHILDREN,
                        ImmutableMap.of("type", TextComponent.of("SUBDIVISION").color(TextColor.AQUA)));
                return new GDClaimResult(ClaimResultType.WRONG_CLAIM_TYPE, message);
            }
            if (this.parent == null) {
                final Component message = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.RESULT_TYPE_CREATE_DENY,
                        ImmutableMap.of(
                                "type", this.getFriendlyNameType(true),
                                "target_type", TextComponent.of("WILDERNESS", TextColor.GREEN)));
                return new GDClaimResult(ClaimResultType.WRONG_CLAIM_TYPE, message);
            }
            if (this.isAdminClaim() && newOwnerUUID == null) {
                return new GDClaimResult(ClaimResultType.REQUIRES_OWNER, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.RESULT_TYPE_REQUIRES_OWNER,
                        ImmutableMap.of(
                                "type", this.getFriendlyNameType(true),
                                "target_type", TextComponent.of("SUBDIVISION", TextColor.AQUA))));
            }
        } else if (type == ClaimTypes.TOWN) {
            if (this.parent != null && this.parent.isTown()) {
                final Component message = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.RESULT_TYPE_NO_CHILDREN,
                        ImmutableMap.of("type", TextComponent.of("TOWN").color(TextColor.GREEN)));
                return new GDClaimResult(ClaimResultType.WRONG_CLAIM_TYPE, message);
            }
            if (!isAdmin && user != null && !PermissionUtil.getInstance().holderHasPermission(user, GDPermissions.CLAIM_CREATE_TOWN)) {
                final Component message = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.RESULT_TYPE_CREATE_DENY,
                        ImmutableMap.of(
                                "type", this.getFriendlyNameType(true),
                                "target_type", TextComponent.of("TOWN", TextColor.GREEN)));
                return new GDClaimResult(ClaimResultType.WRONG_CLAIM_TYPE, message);
            }
        } else if (type == ClaimTypes.WILDERNESS) {
            final Component message = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.RESULT_TYPE_CHANGE_DENY,
                    ImmutableMap.of("type", TextComponent.of("WILDERNESS").color(TextColor.GREEN)));
            return new GDClaimResult(ClaimResultType.WRONG_CLAIM_TYPE, message);
        }

        return new GDClaimResult(ClaimResultType.SUCCESS);
    }

    public int countEntities(EntityType type) {
        int count = 0;
        for (Chunk chunk : this.getChunks()) {
            for (Entity entity : chunk.getEntities()) {
                if (entity.getType() == type) {
                    count++;
                }
            }
        }

        return count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof GDClaim)) {
            return false;
        }
        GDClaim that = (GDClaim) o;
        return this.type == that.type &&
               Objects.equal(this.id, that.id);
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    @Override
    public List<UUID> getUserTrusts() {
        List<UUID> trustList = new ArrayList<>();
        trustList.addAll(this.claimData.getAccessors());
        trustList.addAll(this.claimData.getContainers());
        trustList.addAll(this.claimData.getBuilders());
        trustList.addAll(this.claimData.getManagers());
        return ImmutableList.copyOf(trustList);
    }

    @Override
    public List<UUID> getUserTrusts(TrustType type) {
        return ImmutableList.copyOf(this.getUserTrustList(type));
    }

    public boolean isUserTrusted(Player player, TrustType type) {
        final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(player);
        return isUserTrusted(user, type, null);
    }

    public boolean isUserTrusted(GDPermissionUser user, TrustType type) {
        return isUserTrusted(user, type, null);
    }

    @Override
    public boolean isUserTrusted(UUID uuid, TrustType type) {
        final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(uuid);
        return isUserTrusted(user, type, null);
    }

    public boolean isUserTrusted(GDPermissionUser user, TrustType type, Set<Context> contexts) {
        return isUserTrusted(user, type, contexts, false);
    }

    public boolean isUserTrusted(GDPermissionUser user, TrustType type, Set<Context> contexts, boolean forced) {
        if (user == null) {
            return false;
        }

        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(world, user.getUniqueId());
        if (!playerData.canIgnoreClaim(this) && this.getInternalClaimData().isExpired()) {
            return false;
        }
        if (forced || !playerData.debugClaimPermissions) {
            if (user.getUniqueId().equals(this.getOwnerUniqueId())) {
                return true;
            }
            if (this.isAdminClaim() && playerData.canManageAdminClaims) {
                return true;
            }
            if (this.isWilderness() && playerData.canManageWilderness) {
                return true;
            }
            if (playerData.canIgnoreClaim(this)) {
                return true;
            }
        }

        if (type == null) {
            return true;
        }
        if (this.isPublicTrusted(type)) {
            return true;
        }

        if (type == TrustTypes.ACCESSOR) {
            if (this.claimData.getAccessors().contains(user.getUniqueId())) {
                return true;
            }
            if (this.claimData.getBuilders().contains(user.getUniqueId())) {
                return true;
            }
            if (this.claimData.getContainers().contains(user.getUniqueId())) {
                return true;
            }
            if (this.claimData.getManagers().contains(user.getUniqueId())) {
                return true;
            }
        } else if (type == TrustTypes.BUILDER) {
            if (this.claimData.getBuilders().contains(user.getUniqueId())) {
                return true;
            }
            if (this.claimData.getManagers().contains(user.getUniqueId())) {
                return true;
            }
        } else if (type == TrustTypes.CONTAINER) {
            if (this.claimData.getContainers().contains(user.getUniqueId())) {
                return true;
            }
            if (this.claimData.getBuilders().contains(user.getUniqueId())) {
                return true;
            }
            if (this.claimData.getManagers().contains(user.getUniqueId())) {
                return true;
            }
        } else if (type == TrustTypes.MANAGER) {
            if (this.claimData.getManagers().contains(user.getUniqueId())) {
                return true;
            }
        }

        if (contexts == null) {
            contexts = new HashSet<>();
            contexts.add(this.getContext());
        }

        if (PermissionUtil.getInstance().getPermissionValue(this, user, GDPermissions.getTrustPermission(type), contexts) == Tristate.TRUE) {
            return true;
        }

        // Only check parent if this claim inherits
        if (this.parent != null && this.getData().doesInheritParent()) {
            return this.parent.isUserTrusted(user, type, contexts);
        }

        return false;
    }

    private boolean isPublicTrusted(TrustType type) {
        if (type == TrustTypes.ACCESSOR) {
            if (this.claimData.getAccessors().contains(GriefDefenderPlugin.PUBLIC_UUID)) {
                return true;
            }
            if (this.claimData.getBuilders().contains(GriefDefenderPlugin.PUBLIC_UUID)) {
                return true;
            }
            if (this.claimData.getContainers().contains(GriefDefenderPlugin.PUBLIC_UUID)) {
                return true;
            }
            if (this.claimData.getManagers().contains(GriefDefenderPlugin.PUBLIC_UUID)) {
                return true;
            }
        } else if (type == TrustTypes.BUILDER) {
            if (this.claimData.getBuilders().contains(GriefDefenderPlugin.PUBLIC_UUID)) {
                return true;
            }
            if (this.claimData.getManagers().contains(GriefDefenderPlugin.PUBLIC_UUID)) {
                return true;
            }
        } else if (type == TrustTypes.CONTAINER) {
            if (this.claimData.getContainers().contains(GriefDefenderPlugin.PUBLIC_UUID)) {
                return true;
            }
            if (this.claimData.getBuilders().contains(GriefDefenderPlugin.PUBLIC_UUID)) {
                return true;
            }
            if (this.claimData.getManagers().contains(GriefDefenderPlugin.PUBLIC_UUID)) {
                return true;
            }
        } else if (type == TrustTypes.MANAGER) {
            if (this.claimData.getManagers().contains(GriefDefenderPlugin.PUBLIC_UUID)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isGroupTrusted(String name, TrustType type) {
        if (name == null) {
            return false;
        }

        if (!PermissionUtil.getInstance().hasGroupSubject(name)) {
            return false;
        }

        final GDPermissionHolder holder = PermissionHolderCache.getInstance().getOrCreateHolder(name);
        Set<Context> contexts = new HashSet<>();
        contexts.add(this.getContext());

        return PermissionUtil.getInstance().getPermissionValue(this, holder, GDPermissions.getTrustPermission(type), contexts) == Tristate.TRUE;
    }

    @Override
    public ClaimResult addUserTrust(UUID uuid, TrustType type) {
        GDUserTrustClaimEvent.Add event = new GDUserTrustClaimEvent.Add(this, ImmutableList.of(uuid), type);
        GriefDefender.getEventManager().post(event);
        if (event.cancelled()) {
            return new GDClaimResult(ClaimResultType.CLAIM_EVENT_CANCELLED, event.getMessage().orElse(null));
        }

        List<UUID> userList = this.getUserTrustList(type);
        if (!userList.contains(uuid)) {
            userList.add(uuid);
        }

        this.claimData.setRequiresSave(true);
        this.claimData.save();
        return new GDClaimResult(this, ClaimResultType.SUCCESS);
    }

    @Override
    public ClaimResult addUserTrusts(List<UUID> uuids, TrustType type) {
        GDUserTrustClaimEvent.Add event = new GDUserTrustClaimEvent.Add(this, uuids, type);
        GriefDefender.getEventManager().post(event);
        if (event.cancelled()) {
            return new GDClaimResult(ClaimResultType.CLAIM_EVENT_CANCELLED, event.getMessage().orElse(null));
        }

        for (UUID uuid : uuids) {
            List<UUID> userList = this.getUserTrustList(type);
            if (!userList.contains(uuid)) {
                userList.add(uuid);
            }
        }

        this.claimData.setRequiresSave(true);
        this.claimData.save();
        return new GDClaimResult(this, ClaimResultType.SUCCESS);
    }

    @Override
    public ClaimResult removeUserTrust(UUID uuid, TrustType type) {
        GDUserTrustClaimEvent.Remove event = new GDUserTrustClaimEvent.Remove(this, ImmutableList.of(uuid), type);
        GriefDefender.getEventManager().post(event);
        if (event.cancelled()) {
            return new GDClaimResult(ClaimResultType.CLAIM_EVENT_CANCELLED, event.getMessage().orElse(null));
        }

        if (type == TrustTypes.NONE) {
            final ClaimResult result = this.removeAllTrustsFromUser(uuid);
            this.claimData.setRequiresSave(true);
            this.claimData.save();
            return result;
        }

        this.getUserTrustList(type).remove(uuid);
        this.claimData.setRequiresSave(true);
        this.claimData.save();
        return new GDClaimResult(this, ClaimResultType.SUCCESS);
    }

    @Override
    public ClaimResult removeUserTrusts(List<UUID> uuids, TrustType type) {
        GDUserTrustClaimEvent.Remove event = new GDUserTrustClaimEvent.Remove(this, uuids, type);
        GriefDefender.getEventManager().post(event);
        if (event.cancelled()) {
            return new GDClaimResult(ClaimResultType.CLAIM_EVENT_CANCELLED, event.getMessage().orElse(null));
        }

        if (type == TrustTypes.NONE) {
            for (UUID uuid : uuids) {
                this.removeAllTrustsFromUser(uuid);
            }

            this.claimData.setRequiresSave(true);
            this.claimData.save();
            return new GDClaimResult(this, ClaimResultType.SUCCESS);
        }

        List<UUID> userList = this.getUserTrustList(type);
        for (UUID uuid : uuids) {
            if (userList.contains(uuid)) {
                userList.remove(uuid);
            }
        }

        this.claimData.setRequiresSave(true);
        this.claimData.save();
        return new GDClaimResult(this, ClaimResultType.SUCCESS);
    }

    @Override
    public ClaimResult addGroupTrust(String group, TrustType type) {
        GDGroupTrustClaimEvent.Add event = new GDGroupTrustClaimEvent.Add(this, ImmutableList.of(group), type);
        GriefDefender.getEventManager().post(event);
        if (event.cancelled()) {
            return new GDClaimResult(ClaimResultType.CLAIM_EVENT_CANCELLED, event.getMessage().orElse(null));
        }

        List<String> groupList = this.getGroupTrustList(type);
        if (!groupList.contains(group)) {
            groupList.add(group);
        }

        this.claimData.setRequiresSave(true);
        this.claimData.save();
        return new GDClaimResult(this, ClaimResultType.SUCCESS);
    }

    @Override
    public ClaimResult addGroupTrusts(List<String> groups, TrustType type) {
        GDGroupTrustClaimEvent.Add event = new GDGroupTrustClaimEvent.Add(this, groups, type);
        GriefDefender.getEventManager().post(event);
        if (event.cancelled()) {
            return new GDClaimResult(ClaimResultType.CLAIM_EVENT_CANCELLED, event.getMessage().orElse(null));
        }

        for (String group : groups) {
            List<String> groupList = this.getGroupTrustList(type);
            if (!groupList.contains(group)) {
                groupList.add(group);
            }
        }

        this.claimData.setRequiresSave(true);
        this.claimData.save();
        return new GDClaimResult(this, ClaimResultType.SUCCESS);
    }

    @Override
    public ClaimResult removeGroupTrust(String group, TrustType type) {
        GDGroupTrustClaimEvent.Remove event = new GDGroupTrustClaimEvent.Remove(this, ImmutableList.of(group), type);
        GriefDefender.getEventManager().post(event);
        if (event.cancelled()) {
            return new GDClaimResult(ClaimResultType.CLAIM_EVENT_CANCELLED, event.getMessage().orElse(null));
        }

        if (type == TrustTypes.NONE) {
            final ClaimResult result = this.removeAllTrustsFromGroup(group);
            this.claimData.setRequiresSave(true);
            this.claimData.save();
            return result;
        }

        this.getGroupTrustList(type).remove(group);
        this.claimData.setRequiresSave(true);
        this.claimData.save();
        return new GDClaimResult(this, ClaimResultType.SUCCESS);
    }

    @Override
    public ClaimResult removeGroupTrusts(List<String> groups, TrustType type) {
        GDGroupTrustClaimEvent.Remove event = new GDGroupTrustClaimEvent.Remove(this, groups, type);
        GriefDefender.getEventManager().post(event);
        if (event.cancelled()) {
            return new GDClaimResult(ClaimResultType.CLAIM_EVENT_CANCELLED, event.getMessage().orElse(null));
        }

        if (type == TrustTypes.NONE) {
            for (String group : groups) {
                this.removeAllTrustsFromGroup(group);
            }

            this.claimData.setRequiresSave(true);
            this.claimData.save();
            return new GDClaimResult(this, ClaimResultType.SUCCESS);
        }

        List<String> groupList = this.getGroupTrustList(type);
        for (String group : groups) {
            if (groupList.contains(group)) {
                groupList.remove(group);
            }
        }

        this.claimData.setRequiresSave(true);
        this.claimData.save();
        return new GDClaimResult(this, ClaimResultType.SUCCESS);
    }

    @Override
    public ClaimResult removeAllTrusts() {
        List<UUID> userTrustList = this.getUserTrusts();
        GDUserTrustClaimEvent.Remove userEvent = new GDUserTrustClaimEvent.Remove(this, userTrustList, TrustTypes.NONE);
        GriefDefender.getEventManager().post(userEvent);
        if (userEvent.cancelled()) {
            return new GDClaimResult(ClaimResultType.CLAIM_EVENT_CANCELLED, userEvent.getMessage().orElse(null));
        }

        List<String> groupTrustList = this.getGroupTrusts();
        GDGroupTrustClaimEvent.Remove event = new GDGroupTrustClaimEvent.Remove(this, groupTrustList, TrustTypes.NONE);
        GriefDefender.getEventManager().post(event);
        if (event.cancelled()) {
            return new GDClaimResult(ClaimResultType.CLAIM_EVENT_CANCELLED, event.getMessage().orElse(null));
        }

        for (TrustType type : TrustTypeRegistryModule.getInstance().getAll()) {
            this.getUserTrustList(type).clear();
        }

        for (TrustType type : TrustTypeRegistryModule.getInstance().getAll()) {
            this.getGroupTrustList(type).clear();
        }

        this.claimData.setRequiresSave(true);
        this.claimData.save();
        return new GDClaimResult(this, ClaimResultType.SUCCESS);
    }

    @Override
    public ClaimResult removeAllUserTrusts() {
        List<UUID> trustList = this.getUserTrusts();
        GDUserTrustClaimEvent.Remove event = new GDUserTrustClaimEvent.Remove(this, trustList, TrustTypes.NONE);
        GriefDefender.getEventManager().post(event);
        if (event.cancelled()) {
            return new GDClaimResult(ClaimResultType.CLAIM_EVENT_CANCELLED, event.getMessage().orElse(null));
        }

        for (TrustType type : TrustTypeRegistryModule.getInstance().getAll()) {
            this.getUserTrustList(type).clear();
        }

        this.claimData.setRequiresSave(true);
        this.claimData.save();
        return new GDClaimResult(this, ClaimResultType.SUCCESS);
    }

    @Override
    public ClaimResult removeAllGroupTrusts() {
        List<String> trustList = this.getGroupTrusts();
        GDGroupTrustClaimEvent.Remove event = new GDGroupTrustClaimEvent.Remove(this, trustList, TrustTypes.NONE);
        GriefDefender.getEventManager().post(event);
        if (event.cancelled()) {
            return new GDClaimResult(ClaimResultType.CLAIM_EVENT_CANCELLED, event.getMessage().orElse(null));
        }

        for (TrustType type : TrustTypeRegistryModule.getInstance().getAll()) {
            this.getGroupTrustList(type).clear();
        }

        this.claimData.setRequiresSave(true);
        this.claimData.save();
        return new GDClaimResult(this, ClaimResultType.SUCCESS);
    }

    public ClaimResult removeAllTrustsFromUser(UUID userUniqueId) {
        for (TrustType type : TrustTypeRegistryModule.getInstance().getAll()) {
            this.getUserTrustList(type).remove(userUniqueId);
        }

        return new GDClaimResult(this, ClaimResultType.SUCCESS);
    }

    public ClaimResult removeAllTrustsFromGroup(String group) {
        for (TrustType type : TrustTypeRegistryModule.getInstance().getAll()) {
            this.getGroupTrustList(type).remove(group);
        }

        return new GDClaimResult(this, ClaimResultType.SUCCESS);
    }

    public List<UUID> getUserTrustList(TrustType type) {
        if (type == TrustTypes.NONE) {
            return new ArrayList<>();
        }
        if (type == TrustTypes.ACCESSOR) {
            return this.claimData.getAccessors();
        }
        if (type == TrustTypes.CONTAINER) {
            return this.claimData.getContainers();
        }
        if (type == TrustTypes.BUILDER) {
            return this.claimData.getBuilders();
        }
        return this.claimData.getManagers();
    }

    public List<UUID> getParentUserTrustList(TrustType type) {
        List<UUID> userList = new ArrayList<>();
        for (Claim claim : this.getInheritedParents()) {
            GDClaim parentClaim = (GDClaim) claim;
            userList.addAll(parentClaim.getUserTrusts(type));
        }
        return userList;
    }

    public List<String> getParentGroupTrustList(TrustType type) {
        List<String> trustList = new ArrayList<>();
        for (Claim claim : this.getInheritedParents()) {
            GDClaim parentClaim = (GDClaim) claim;
            trustList.addAll(parentClaim.getGroupTrusts(type));
        }
        return trustList;
    }

    public List<UUID> getUserTrustList(TrustType type, boolean includeParents) {
        List<UUID> trustList = new ArrayList<>();
        if (type == TrustTypes.ACCESSOR) {
            trustList.addAll(this.claimData.getAccessors());
        } else if (type == TrustTypes.CONTAINER) {
            trustList.addAll(this.claimData.getContainers());
        } else if (type == TrustTypes.BUILDER) {
            trustList.addAll(this.claimData.getBuilders());
        } else {
            trustList.addAll(this.claimData.getManagers());
        }

        if (includeParents) {
            List<UUID> parentList = getParentUserTrustList(type);
            for (UUID uuid : parentList) {
                if (!trustList.contains(uuid)) {
                    trustList.add(uuid);
                }
            }
        }

        return trustList;
    }

    public List<String> getGroupTrustList(TrustType type) {
        if (type == TrustTypes.NONE) {
            return new ArrayList<>();
        }
        if (type == TrustTypes.ACCESSOR) {
            return this.claimData.getAccessorGroups();
        }
        if (type == TrustTypes.CONTAINER) {
            return this.claimData.getContainerGroups();
        }
        if (type == TrustTypes.BUILDER) {
            return this.claimData.getBuilderGroups();
        }
        return this.claimData.getManagerGroups();
    }

    public List<String> getGroupTrustList(TrustType type, boolean includeParents) {
        List<String> trustList = new ArrayList<>();
        if (type == TrustTypes.ACCESSOR) {
            trustList.addAll(this.claimData.getAccessorGroups());
        } else if (type == TrustTypes.CONTAINER) {
            trustList.addAll(this.claimData.getContainerGroups());
        } else if (type == TrustTypes.BUILDER) {
            trustList.addAll(this.claimData.getBuilderGroups());
        } else {
            trustList.addAll(this.claimData.getManagerGroups());
        }

        if (includeParents) {
            List<String> parentList = getParentGroupTrustList(type);
            for (String groupId : parentList) {
                if (!trustList.contains(groupId)) {
                    trustList.add(groupId);
                }
            }
        }

        return trustList;
    }

    @Override
    public List<String> getGroupTrusts() {
        List<String> groups = new ArrayList<>();
        groups.addAll(this.getInternalClaimData().getAccessorGroups());
        groups.addAll(this.getInternalClaimData().getBuilderGroups());
        groups.addAll(this.getInternalClaimData().getContainerGroups());
        groups.addAll(this.getInternalClaimData().getManagerGroups());
        return ImmutableList.copyOf(groups);
    }

    @Override
    public List<String> getGroupTrusts(TrustType type) {
        return ImmutableList.copyOf(this.getGroupTrustList(type));
    }

    public Optional<UUID> getEconomyAccountId() {
        if (this.vaultProvider == null || this.vaultProvider.getApi() == null || !this.vaultProvider.getApi().hasBankSupport() || this.isAdminClaim() || this.isSubdivision() || !GriefDefenderPlugin.getGlobalConfig().getConfig().claim.bankTaxSystem) {
            return Optional.empty();
        }

        if (this.vaultProvider.getApi().getBanks().contains(this.id.toString())) {
            return Optional.of(this.id);
        }

        if (this.vaultProvider != null) {
            this.vaultProvider.getApi().createBank(this.claimStorage.filePath.getFileName().toString(), this.ownerPlayerData.getSubject().getOfflinePlayer());
            return Optional.ofNullable(this.id);
        }
        return Optional.empty();
    }

    public static class ClaimBuilder implements Builder {

        private UUID ownerUniqueId;
        private ClaimType type = ClaimTypes.BASIC;
        private boolean cuboid = false;
        private boolean requiresClaimBlocks = true;
        private boolean denyMessages = true;
        private boolean expire = true;
        private boolean resizable = true;
        private boolean inherit = true;
        private boolean overrides = true;
        private boolean createLimitRestrictions = true;
        private boolean levelRestrictions = true;
        private boolean sizeRestrictions = true;
        private UUID worldUniqueId;
        private Vector3i point1;
        private Vector3i point2;
        private Vector3i spawnPos;
        private Component greeting;
        private Component farewell;
        private Claim parent;

        public ClaimBuilder() {
            
        }

        @Override
        public Builder bounds(Vector3i point1, Vector3i point2) {
            this.point1 = point1;
            this.point2 = point2;
            return this;
        }

        @Override
        public Builder cuboid(boolean cuboid) {
            this.cuboid = cuboid;
            return this;
        }

        @Override
        public Builder owner(UUID ownerUniqueId) {
            this.ownerUniqueId = ownerUniqueId;
            return this;
        }

        @Override
        public Builder parent(Claim parentClaim) {
            this.parent = parentClaim;
            return this;
        }

        @Override
        public Builder type(ClaimType type) {
            this.type = type;
            return this;
        }

        @Override
        public Builder world(UUID worldUniqueId) {
            this.worldUniqueId = worldUniqueId;
            return this;
        }

        @Override
        public Builder createLimitRestrictions(boolean checkCreateLimit) {
            this.createLimitRestrictions = checkCreateLimit;
            return this;
        }

        @Override
        public Builder levelRestrictions(boolean checkLevel) {
            this.levelRestrictions = checkLevel;
            return this;
        }

        @Override
        public Builder sizeRestrictions(boolean checkSize) {
            this.sizeRestrictions = checkSize;
            return this;
        }

        @Override
        public Builder requireClaimBlocks(boolean requiresClaimBlocks) {
            this.requiresClaimBlocks = requiresClaimBlocks;
            return this;
        }

        @Override
        public Builder denyMessages(boolean allowDenyMessages) {
            this.denyMessages = allowDenyMessages;
            return this;
        }

        @Override
        public Builder expire(boolean allowExpire) {
            this.expire = allowExpire;
            return this;
        }

        @Override
        public Builder inherit(boolean inherit) {
            this.inherit = inherit;
            return this;
        }

        @Override
        public Builder resizable(boolean allowResize) {
            this.resizable = allowResize;
            return this;
        }

        @Override
        public Builder overrides(boolean allowOverrides) {
            this.overrides = allowOverrides;
            return this;
        }

        @Override
        public Builder farewell(Component farewell) {
            this.farewell = farewell;
            return this;
        }

        @Override
        public Builder greeting(Component greeting) {
            this.greeting = greeting;
            return this;
        }

        @Override
        public Builder spawnPos(Vector3i spawnPos) {
            this.spawnPos = spawnPos;
            return this;
        }

        @Override
        public Builder reset() {
            this.ownerUniqueId = null;
            this.type = ClaimTypes.BASIC;
            this.cuboid = false;
            this.worldUniqueId = null;
            this.point1 = null;
            this.point2 = null;
            this.parent = null;
            return this;
        }

        @Override
        public ClaimResult build() {
            checkNotNull(this.type);
            checkNotNull(this.worldUniqueId);
            checkNotNull(this.point1);
            checkNotNull(this.point2);

            final World world = Bukkit.getServer().getWorld(this.worldUniqueId);
            if (world == null) {
                return new GDClaimResult(ClaimResultType.WORLD_NOT_FOUND);
            }

            if (this.type == ClaimTypes.SUBDIVISION) {
                checkNotNull(this.parent);
            }

            if (this.type == ClaimTypes.ADMIN || this.type == ClaimTypes.WILDERNESS) {
                this.sizeRestrictions = false;
                this.levelRestrictions = false;
            }

            GDClaim claim = null;
            if (this.type == ClaimTypes.TOWN) {
                claim = new GDTown(world, this.point1, this.point2, this.type, this.ownerUniqueId, this.cuboid);
            } else {
                claim = new GDClaim(world, this.point1, this.point2, this.type, this.ownerUniqueId, this.cuboid);
            }
            claim.parent = (GDClaim) this.parent;
            Player player = null;
            final Object source = GDCauseStackManager.getInstance().getCurrentCause().root();
            if (source instanceof GDPermissionUser) {
                final GDPermissionUser user = (GDPermissionUser) source;
                player = (Player) user.getOnlinePlayer();
            }
            GDPlayerData playerData = null;
            double requiredFunds = 0;

            if (this.ownerUniqueId != null) {
                if (playerData == null) {
                    playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(this.worldUniqueId, this.ownerUniqueId);
                }

                final WorldGuardProvider worldGuardProvider = GriefDefenderPlugin.getInstance().getWorldGuardProvider();
                if (worldGuardProvider != null) {
                    if (player != null) {
                        if (!worldGuardProvider.allowClaimCreate(claim, player)) {
                            return new GDClaimResult(claim, ClaimResultType.CLAIM_EVENT_CANCELLED);
                        }
                    } else {
                        final GDPermissionUser user = playerData.getSubject();
                        if (user != null && !worldGuardProvider.allowClaimCreate(claim, user.getOnlinePlayer())) {
                            return new GDClaimResult(claim, ClaimResultType.CLAIM_EVENT_CANCELLED);
                        }
                    }
                }

                if (this.levelRestrictions) {
                    final int minClaimLevel = claim.getOwnerMinClaimLevel();
                    if (claim.getLesserBoundaryCorner().getY() < minClaimLevel) {
                        Component message = null;
                        if (player != null) {
                            message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_BELOW_LEVEL, ImmutableMap.of(
                                    "limit", minClaimLevel));
                            GriefDefenderPlugin.sendMessage(player, message);
                        }
                        return new GDClaimResult(claim, ClaimResultType.BELOW_MIN_LEVEL, message);
                    }
                    final int maxClaimLevel = claim.getOwnerMaxClaimLevel();
                    if (claim.getGreaterBoundaryCorner().getY() > maxClaimLevel) {
                        Component message = null;
                        if (player != null) {
                            message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_ABOVE_LEVEL, ImmutableMap.of(
                                    "limit", maxClaimLevel));
                            GriefDefenderPlugin.sendMessage(player, message);
                        }
                        return new GDClaimResult(claim, ClaimResultType.ABOVE_MAX_LEVEL, message);
                    }
                }

                if (this.sizeRestrictions) {
                    ClaimResult claimResult = claim.checkSizeLimits(player, playerData, this.point1, this.point2);
                    if (!claimResult.successful()) {
                        return claimResult;
                    }
                }

                final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(this.ownerUniqueId);
                if (this.createLimitRestrictions && !PermissionUtil.getInstance().holderHasPermission(user, GDPermissions.BYPASS_CLAIM_LIMIT)) {
                    final int createClaimLimit = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), user, Options.CREATE_LIMIT, claim);
                    if (createClaimLimit > -1 && (playerData.getClaimTypeCount(claim.getType()) + 1) > createClaimLimit) {
                        final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CREATE_FAILED_CLAIM_LIMIT, ImmutableMap.of(
                                "limit", createClaimLimit,
                                "type", claim.getType().getName()));
                        if (player != null) {
                            GriefDefenderPlugin.sendMessage(player, message);
                        }
                        return new GDClaimResult(claim, ClaimResultType.EXCEEDS_MAX_CLAIM_LIMIT, message);
                    }
                }

                // check player has enough claim blocks
                if ((claim.isBasicClaim() || claim.isTown()) && this.requiresClaimBlocks) {
                    final int claimCost = BlockUtil.getInstance().getClaimBlockCost(world, claim.lesserBoundaryCorner, claim.greaterBoundaryCorner, claim.cuboid);
                    if (GriefDefenderPlugin.getInstance().isEconomyModeEnabled()) {
                        final GDClaimResult result = EconomyUtil.getInstance().checkEconomyFunds(claim, playerData, true);
                        if (!result.successful()) {
                            return result;
                        }
                        requiredFunds = claimCost * claim.getOwnerEconomyBlockCost();
                    } else {
                        final int remainingClaimBlocks = playerData.getRemainingClaimBlocks() - claimCost;
                        if (remainingClaimBlocks < 0) {
                            Component message = null;
                            if (player != null) {
                                if (GriefDefenderPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.VOLUME) {
                                    final double claimableChunks = Math.abs(remainingClaimBlocks / 65536.0);
                                    message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_SIZE_NEED_BLOCKS_3D, ImmutableMap.of(
                                            "chunk-amount", Math.round(claimableChunks * 100.0)/100.0,
                                            "block-amount", Math.abs(remainingClaimBlocks)));
                                } else {
                                    message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_SIZE_NEED_BLOCKS_2D, ImmutableMap.of(
                                            "block-amount", Math.abs(remainingClaimBlocks)));
                                }
                                GriefDefenderPlugin.sendMessage(player, message);
                            }
                            //playerData.lastShovelLocation = null;
                            playerData.claimResizing = null;
                            return new GDClaimResult(ClaimResultType.INSUFFICIENT_CLAIM_BLOCKS, message);
                        }
                    }
                }

                if (!GriefDefenderPlugin.getInstance().isEconomyModeEnabled() && claim.isTown() && player != null) {
                    final double townCost = GriefDefenderPlugin.getGlobalConfig().getConfig().town.cost;
                    if (townCost > 0) {
                        final Economy economy = GriefDefenderPlugin.getInstance().getVaultProvider().getApi();
                        if (!economy.hasAccount(player)) {
                            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_PLAYER_NOT_FOUND, ImmutableMap.of(
                                    "player", claim.getOwnerName()));
                            GriefDefenderPlugin.sendMessage(player, message);
                            return new GDClaimResult(claim, ClaimResultType.NOT_ENOUGH_FUNDS, message);
                        }
                        final double balance = economy.getBalance(player);
                        if (balance < townCost) {
                            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.TOWN_CREATE_NOT_ENOUGH_FUNDS, ImmutableMap.of(
                                    "amount", String.valueOf("$" +townCost),
                                    "balance", String.valueOf("$" + balance),
                                    "amount-needed", String.valueOf("$" + (townCost - balance))));
                            GriefDefenderPlugin.sendMessage(player, message);
                            return new GDClaimResult(claim, ClaimResultType.NOT_ENOUGH_FUNDS, message);
                        }
                        economy.withdrawPlayer(player, townCost);
                    }
                }
            }

            final ClaimResult claimResult = claim.checkArea(false);
            if (!claimResult.successful()) {
                if (player != null && (claim.isBasicClaim() || claim.isTown()) && this.requiresClaimBlocks && GriefDefenderPlugin.getInstance().isEconomyModeEnabled()) {
                    GriefDefenderPlugin.getInstance().getVaultProvider().getApi().depositPlayer(player, requiredFunds);
                }
                return claimResult;
            }

            GDCreateClaimEvent.Pre event = new GDCreateClaimEvent.Pre(claim);
            GriefDefender.getEventManager().post(event);
            if (event.cancelled()) {
                final Component message = event.getMessage().orElse(null);
                if (message != null && player != null) {
                    GriefDefenderPlugin.sendMessage(player, message);
                }
                if (player != null && (claim.isBasicClaim() || claim.isTown()) && this.requiresClaimBlocks && GriefDefenderPlugin.getGlobalConfig().getConfig().economy.economyMode) {
                    GriefDefenderPlugin.getInstance().getVaultProvider().getApi().depositPlayer(player, requiredFunds);
                }
                return new GDClaimResult(claim, ClaimResultType.CLAIM_EVENT_CANCELLED, message);
            }

            claim.initializeClaimData((GDClaim) this.parent);
            if (this.parent != null) {
                if (this.parent.isTown()) {
                    claim.getData().setInheritParent(true);
                }
                claim.getData().setParent(this.parent.getUniqueId());
            }

            claim.getData().setExpiration(this.expire);
            claim.getData().setDenyMessages(this.denyMessages);
            claim.getData().setFlagOverrides(this.overrides);
            claim.getData().setInheritParent(this.inherit);
            claim.getData().setResizable(this.resizable);
            claim.getData().setRequiresClaimBlocks(this.requiresClaimBlocks);
            claim.getData().setFarewell(this.farewell);
            claim.getData().setGreeting(this.greeting);
            claim.getData().setSpawnPos(this.spawnPos);
            claim.getData().setSizeRestrictions(this.sizeRestrictions);
            final GDClaimManager claimManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(this.worldUniqueId);
            claimManager.addClaim(claim, true);

            if (claimResult.getClaims().size() > 1) {
                claim.migrateClaims(new ArrayList<>(claimResult.getClaims()));
            }

            GDCreateClaimEvent.Post postEvent = new GDCreateClaimEvent.Post(claim);
            GriefDefender.getEventManager().post(postEvent);
            if (postEvent.cancelled()) {
                final Component message = postEvent.getMessage().orElse(null);
                if (message != null && player != null) {
                    GriefDefenderPlugin.sendMessage(player, message);
                }
                final GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(world.getUID());
                claimWorldManager.deleteClaimInternal(claim, true);
                return new GDClaimResult(claim, ClaimResultType.CLAIM_EVENT_CANCELLED, message);
            }

            if (GriefDefenderPlugin.getInstance().getWorldEditProvider() != null) {
                if (GriefDefenderPlugin.getActiveConfig(this.worldUniqueId).getConfig().claim.claimAutoSchematicRestore) {
                    final ClaimSchematic schematic = ClaimSchematic.builder().claim(claim).name("__restore__").build().orElse(null);
                }
            }
            return new GDClaimResult(claim, ClaimResultType.SUCCESS);
        }
    }

    public boolean migrateClaims(List<Claim> claims) {
        GDClaim parentClaim = this;
        for (Claim child : claims) {
            if (child.equals(this)) {
                continue;
            }

            moveChildToParent(parentClaim, (GDClaim) child);
        }

        return true;
    }

    public void moveChildToParent(GDClaim parentClaim, GDClaim childClaim) {
        // Remove child from current parent if available
        if (childClaim.parent != null && childClaim.parent != parentClaim) {
            childClaim.parent.children.remove(childClaim);
        }
        childClaim.parent = parentClaim;
        String fileName = childClaim.getClaimStorage().filePath.getFileName().toString();
        Path newPath = parentClaim.getClaimStorage().folderPath.resolve(childClaim.getType().getName().toLowerCase()).resolve(fileName);
        try {
            if (Files.notExists(newPath.getParent())) {
                Files.createDirectories(newPath.getParent());
            }
            Files.move(childClaim.getClaimStorage().filePath, newPath);
            if (childClaim.getClaimStorage().folderPath.toFile().listFiles().length == 0) {
                Files.delete(childClaim.getClaimStorage().folderPath);
            }
            childClaim.setClaimStorage(new ClaimStorageData(newPath, this.getWorldUniqueId(), (ClaimDataConfig) childClaim.getInternalClaimData()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Make sure to update new parent in storage
        childClaim.getInternalClaimData().setParent(parentClaim.getUniqueId());
        this.worldClaimManager.addClaim(childClaim, true);
        for (Claim child : childClaim.children) {
            moveChildToParent(childClaim, (GDClaim) child);
        }
    }

    @Override
    public Context getDefaultTypeContext() {
        if (this.isAdminClaim()) {
            return ClaimContexts.ADMIN_DEFAULT_CONTEXT;
        }
        if (this.isBasicClaim() || this.isSubdivision()) {
            return ClaimContexts.BASIC_DEFAULT_CONTEXT;
        }
        if (this.isTown()) {
            return ClaimContexts.TOWN_DEFAULT_CONTEXT;
        }

        return ClaimContexts.WILDERNESS_DEFAULT_CONTEXT;
    }

    @Override
    public Context getOverrideTypeContext() {
        if (this.isAdminClaim()) {
            return ClaimContexts.ADMIN_OVERRIDE_CONTEXT;
        }
        if (this.isBasicClaim() || this.isSubdivision()) {
            return ClaimContexts.BASIC_OVERRIDE_CONTEXT;
        }
        if (this.isTown()) {
            return ClaimContexts.TOWN_OVERRIDE_CONTEXT;
        }

        return ClaimContexts.WILDERNESS_OVERRIDE_CONTEXT;
    }

    @Override
    public Context getOverrideClaimContext() {
        return this.overrideClaimContext;
    }

    public Context getWorldContext() {
        return this.worldContext;
    }

    @Override
    public Map<String, ClaimSchematic> getSchematics() {
        return this.schematics;
    }

    @Override
    public boolean deleteSchematic(String name) {
        if (GriefDefenderPlugin.getInstance().getWorldEditProvider() == null) {
            return false;
        }

        final ClaimSchematic schematic = this.schematics.remove(name);
        if (schematic == null) {
            return true;
        }
        final Path schematicPath = GriefDefenderPlugin.getInstance().getWorldEditProvider().getSchematicWorldMap().get(schematic.getClaim().getWorldUniqueId()).resolve(schematic.getClaim().getUniqueId().toString());
        if (!Files.exists(schematicPath)) {
            return false;
        }

        File outputFile = schematicPath.resolve(schematic.getName() + ".schematic").toFile();
        if (outputFile.delete()) {
            return true;
        }
        return false;
    }
}