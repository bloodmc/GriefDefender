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
package com.griefdefender.configuration;

import com.flowpowered.math.vector.Vector3i;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.api.claim.ClaimTypes;
import com.griefdefender.api.data.EconomyData;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.category.ConfigCategory;
import com.griefdefender.internal.util.BlockUtil;

import net.kyori.text.Component;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ConfigSerializable
public class ClaimDataConfig extends ConfigCategory implements IClaimData {

    private boolean requiresSave = false;
    private Vector3i lesserPos;
    private Vector3i greaterPos;
    private Vector3i spawnPos;
    private ClaimStorageData claimStorage;

    @Setting
    private UUID parent;
    @Setting(value = ClaimStorageData.MAIN_INHERIT_PARENT)
    public boolean inheritParent = true;
    @Setting(value = ClaimStorageData.MAIN_WORLD_UUID)
    private UUID worldUniqueId;
    @Setting(value = ClaimStorageData.MAIN_OWNER_UUID)
    private UUID ownerUniqueId;
    @Setting(value = ClaimStorageData.MAIN_CLAIM_TYPE)
    private ClaimType claimType = ClaimTypes.BASIC;
    @Setting(value = ClaimStorageData.MAIN_CLAIM_CUBOID)
    private boolean isCuboid = false;
    @Setting(value = ClaimStorageData.MAIN_CLAIM_RESIZABLE)
    private boolean isResizable = true;
    @Setting
    private boolean isExpired = false;
    @Setting
    private boolean sizeRestrictions = true;
    @Setting(value = ClaimStorageData.MAIN_ALLOW_DENY_MESSAGES)
    private boolean allowDenyMessages = true;
    @Setting(value = ClaimStorageData.MAIN_ALLOW_CLAIM_EXPIRATION)
    private boolean allowClaimExpiration = true;
    @Setting(value = ClaimStorageData.MAIN_ALLOW_FLAG_OVERRIDES)
    private boolean allowFlagOverrides = true;
    @Setting(value = ClaimStorageData.MAIN_REQUIRES_CLAIM_BLOCKS)
    private boolean requiresClaimBlocks = true;
    @Setting(value = ClaimStorageData.MAIN_CLAIM_PVP)
    private Tristate pvpOverride = Tristate.UNDEFINED;
    @Setting(value = ClaimStorageData.MAIN_CLAIM_DATE_CREATED)
    private String dateCreated = Instant.now().toString();
    @Setting(value = ClaimStorageData.MAIN_CLAIM_DATE_LAST_ACTIVE)
    private String dateLastActive = Instant.now().toString();
    @Setting(value = ClaimStorageData.MAIN_CLAIM_NAME)
    private Component claimName;
    @Setting(value = ClaimStorageData.MAIN_CLAIM_GREETING)
    private Component claimGreetingMessage;
    @Setting(value = ClaimStorageData.MAIN_CLAIM_FAREWELL)
    private Component claimFarewellMessage;
    @Setting(value = ClaimStorageData.MAIN_CLAIM_SPAWN)
    private String claimSpawn;
    @Setting(value = ClaimStorageData.MAIN_LESSER_BOUNDARY_CORNER)
    private String lesserBoundaryCornerPos;
    @Setting(value = ClaimStorageData.MAIN_GREATER_BOUNDARY_CORNER)
    private String greaterBoundaryCornerPos;
    @Setting(value = ClaimStorageData.MAIN_ACCESSORS)
    private List<UUID> accessors = new ArrayList<>();
    @Setting(value = ClaimStorageData.MAIN_BUILDERS)
    private List<UUID> builders = new ArrayList<>();
    @Setting(value = ClaimStorageData.MAIN_CONTAINERS)
    private List<UUID> containers = new ArrayList<>();
    @Setting(value = ClaimStorageData.MAIN_MANAGERS)
    private List<UUID> managers = new ArrayList<>();
    @Setting(value = ClaimStorageData.MAIN_ACCESSOR_GROUPS)
    private List<String> accessorGroups = new ArrayList<>();
    @Setting(value = ClaimStorageData.MAIN_BUILDER_GROUPS)
    private List<String> builderGroups = new ArrayList<>();
    @Setting(value = ClaimStorageData.MAIN_CONTAINER_GROUPS)
    private List<String> containerGroups = new ArrayList<>();
    @Setting(value = ClaimStorageData.MAIN_MANAGER_GROUPS)
    private List<String> managerGroups = new ArrayList<>();
    @Setting
    private EconomyDataConfig economyData = new EconomyDataConfig();

    public ClaimDataConfig() {

    }

    public ClaimDataConfig(GDClaim claim) {
        this.lesserBoundaryCornerPos = BlockUtil.getInstance().posToString(claim.lesserBoundaryCorner);
        this.greaterBoundaryCornerPos = BlockUtil.getInstance().posToString(claim.greaterBoundaryCorner);
        this.isCuboid = claim.cuboid;
        this.claimType = claim.getType();
        this.ownerUniqueId = claim.getOwnerUniqueId();
    }

    @Override
    public UUID getWorldUniqueId() {
        return this.worldUniqueId;
    }

    @Override
    public UUID getOwnerUniqueId() {
        return this.ownerUniqueId;
    }

    @Override
    public boolean allowExpiration() {
        return this.allowClaimExpiration;
    }

    @Override
    public boolean allowFlagOverrides() {
        return this.allowFlagOverrides;
    }

    @Override
    public boolean isCuboid() {
        return this.isCuboid;
    }

    @Override
    public boolean allowDenyMessages() {
        return this.allowDenyMessages;
    }

    @Override
    public Tristate getPvpOverride() {
        return this.pvpOverride;
    }

    @Override
    public boolean isResizable() {
        return this.isResizable;
    }

    @Override
    public boolean hasSizeRestrictions() {
        if (this.claimType == ClaimTypes.ADMIN || this.claimType == ClaimTypes.WILDERNESS) {
            this.sizeRestrictions = false;
            return false;
        }
        return this.sizeRestrictions;
    }

    @Override
    public ClaimType getType() {
        return this.claimType;
    }

    @Override
    public Instant getDateCreated() {
        return Instant.parse(this.dateCreated);
    }

    @Override
    public Instant getDateLastActive() {
        return Instant.parse(this.dateLastActive);
    }

    @Override
    public Optional<Component> getName() {
        return Optional.ofNullable(this.claimName);
    }

    @Override
    public Optional<Component> getGreeting() {
        return Optional.ofNullable(this.claimGreetingMessage);
    }

    @Override
    public Optional<Component> getFarewell() {
        return Optional.ofNullable(this.claimFarewellMessage);
    }

    @Override
    public Optional<Vector3i> getSpawnPos() {
        if (this.spawnPos == null && this.claimSpawn != null) {
            try {
                this.spawnPos = BlockUtil.getInstance().posFromString(this.claimSpawn);
                this.requiresSave = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return Optional.ofNullable(this.spawnPos);
    }

    @Override
    public Vector3i getLesserBoundaryCornerPos() {
        if (this.lesserPos == null) {
            try {
                this.lesserPos = BlockUtil.getInstance().posFromString(this.lesserBoundaryCornerPos);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return this.lesserPos;
    }

    @Override
    public Vector3i getGreaterBoundaryCornerPos() {
        if (this.greaterPos == null) {
            try {
                this.greaterPos = BlockUtil.getInstance().posFromString(this.greaterBoundaryCornerPos);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return this.greaterPos;
    }

    public List<UUID> getAccessors() {
        return this.accessors;
    }

    public List<UUID> getBuilders() {
        return this.builders;
    }

    public List<UUID> getContainers() {
        return this.containers;
    }

    public List<UUID> getManagers() {
        return this.managers;
    }

    public List<String> getAccessorGroups() {
        return this.accessorGroups;
    }

    public List<String> getBuilderGroups() {
        return this.builderGroups;
    }

    public List<String> getContainerGroups() {
        return this.containerGroups;
    }

    public List<String> getManagerGroups() {
        return this.managerGroups;
    }

    @Override
    public void setDenyMessages(boolean flag) {
        this.requiresSave = true;
        this.allowDenyMessages = flag;
    }

    @Override
    public void setExpiration(boolean flag) {
        this.requiresSave = true;
        this.allowClaimExpiration = flag;
    }

    @Override
    public void setFlagOverrides(boolean flag) {
        this.allowFlagOverrides = flag;
    }

    @Override
    public void setCuboid(boolean cuboid) {
        this.isCuboid = cuboid;
    }

    @Override
    public void setPvpOverride(Tristate pvp) {
        this.requiresSave = true;
        this.pvpOverride = pvp;
    }

    @Override
    public void setResizable(boolean resizable) {
        this.requiresSave = true;
        this.isResizable = resizable;
    }

    @Override
    public void setType(ClaimType type) {
        this.requiresSave = true;
        this.claimType = type;
    }

    @Override
    public void setDateLastActive(Instant date) {
        this.requiresSave = true;
        this.dateLastActive = date.toString();
    }

    @Override
    public void setName(Component name) {
        this.requiresSave = true;
        this.claimName = name;
    }

    @Override
    public void setGreeting(Component message) {
        this.requiresSave = true;
        this.claimGreetingMessage = message;
    }

    @Override
    public void setFarewell(Component message) {
        this.requiresSave = true;
        this.claimFarewellMessage = message;
    }

    @Override
    public void setLesserBoundaryCorner(String location) {
        this.requiresSave = true;
        this.lesserBoundaryCornerPos = location;
        this.lesserPos = null;
    }

    @Override
    public void setGreaterBoundaryCorner(String location) {
        this.requiresSave = true;
        this.greaterBoundaryCornerPos = location;
        this.greaterPos = null;
    }

    @Override
    public void setAccessors(List<UUID> accessors) {
        this.requiresSave = true;
        this.accessors = accessors;
    }

    @Override
    public void setBuilders(List<UUID> builders) {
        this.requiresSave = true;
        this.builders = builders;
    }

    @Override
    public void setContainers(List<UUID> containers) {
        this.requiresSave = true;
        this.containers = containers;
    }

    @Override
    public void setManagers(List<UUID> coowners) {
        this.requiresSave = true;
        this.managers = coowners;
    }

    public boolean requiresSave() {
        return this.requiresSave;
    }

    @Override
    public void setRequiresSave(boolean flag) {
        this.requiresSave = flag;
    }

    @Override
    public void setSizeRestrictions(boolean sizeRestrictions) {
        this.sizeRestrictions = sizeRestrictions;
    }

    @Override
    public boolean doesInheritParent() {
        // NOTE: admin claims ONLY inherit from other parent admin claims
        return this.inheritParent;
    }

    @Override
    public void setInheritParent(boolean flag) {
        this.requiresSave = true;
        this.inheritParent = flag;
    }

    @Override
    public void setOwnerUniqueId(UUID newClaimOwner) {
        this.requiresSave = true;
        this.ownerUniqueId = newClaimOwner;
    }

    @Override
    public void setWorldUniqueId(UUID uuid) {
        this.requiresSave = true;
        this.worldUniqueId = uuid;
    }

    public void setClaimStorageData(ClaimStorageData claimStorage) {
        this.claimStorage = claimStorage;
    }

    @Override
    public void save() {
        this.claimStorage.save();
    }

    @Override
    public void setSpawnPos(Vector3i spawnPos) {
        if (spawnPos == null) {
            return;
        }

        this.requiresSave = true;
        this.spawnPos = spawnPos;
        this.claimSpawn = BlockUtil.getInstance().posToString(spawnPos);
    }

    @Override
    public boolean requiresClaimBlocks() {
        return this.requiresClaimBlocks;
    }

    @Override
    public void setRequiresClaimBlocks(boolean requiresClaimBlocks) {
        this.requiresSave = true;
        this.requiresClaimBlocks = requiresClaimBlocks;
    }

    @Override
    public void setParent(UUID uuid) {
        this.requiresSave = true;
        this.parent = uuid;
    }

    @Override
    public Optional<UUID> getParent() {
        return Optional.ofNullable(this.parent);
    }

    public boolean isExpired() {
        return this.isExpired;
    }

    public void setExpired(boolean expire) {
        this.isExpired = expire;
    }

    @Override
    public EconomyData getEconomyData() {
        return this.economyData;
    }
}