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
package com.griefdefender;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.api.claim.ShovelType;
import com.griefdefender.api.claim.ShovelTypes;
import com.griefdefender.api.data.PlayerData;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.GriefDefenderConfig;
import com.griefdefender.configuration.PlayerStorageData;
import com.griefdefender.internal.block.BlockSnapshot;
import com.griefdefender.internal.block.BlockTransaction;
import com.griefdefender.internal.util.NMSUtil;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.provider.VaultProvider;
import com.griefdefender.util.PermissionUtil;
import me.lucko.luckperms.api.context.MutableContextSet;
import net.kyori.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class GDPlayerData implements PlayerData {

    public UUID playerID;
    public UUID worldUniqueId;
    private WeakReference<GDPermissionUser> playerSubject;
    private Set<Claim> claimList;
    private PlayerStorageData playerStorage;
    public Location lastAfkCheckLocation;
    public Location lastShovelLocation;
    public Location endShovelLocation;
    public Location lastValidInspectLocation;
    public ShovelType shovelMode = ShovelTypes.BASIC;

    public GDClaim claimResizing;
    public GDClaim claimSubdividing;

    public List<BlockTransaction> visualBlocks;
    public UUID visualClaimId;
    public BukkitTask visualRevertTask;
    private final VaultProvider vaultProvider = GriefDefenderPlugin.getInstance().getVaultProvider();

    public boolean ignoreClaims = false;

    public boolean debugClaimPermissions = false;
    public WeakReference<GDClaim> lastClaim = new WeakReference<>(null);

    public boolean inTown = false;
    public boolean townChat = false;

    // Always ignore active contexts by default
    // This prevents protection issues when other plugins call getActiveContext
    public boolean ignoreActiveContexts = true;

    public boolean lastInteractResult = false;
    public int lastTickCounter = 0;
    public UUID lastInteractClaim = GriefDefenderPlugin.PUBLIC_UUID;

    // collide event cache
    public int lastCollideEntityId = 0;
    public boolean lastCollideEntityResult = false;

    private String playerName;

    // cached global option values
    public int minClaimLevel;
    private Integer optionClaimCreateMode;
    private Integer optionMaxAccruedBlocks;

    // cached permission values
    public boolean canManageAdminClaims = false;
    public boolean canManageWilderness = false;
    public boolean ignoreBorderCheck = false;
    public boolean ignoreAdminClaims = false;
    public boolean ignoreBasicClaims = false;
    public boolean ignoreTowns = false;
    public boolean ignoreWilderness = false;

    public boolean dataInitialized = false;
    public boolean showVisualFillers = true;
    private boolean checkedDimensionHeight = false;

    public GDPlayerData(UUID worldUniqueId, UUID playerUniqueId, PlayerStorageData playerStorage, GriefDefenderConfig<?> activeConfig, Set<Claim> claims) {
        this.worldUniqueId = worldUniqueId;
        this.playerID = playerUniqueId;
        this.playerStorage = playerStorage;
        this.claimList = claims;
        this.refreshPlayerOptions();
    }

    // Run async
    public void refreshPlayerOptions() {
        //final GriefDefenderConfig<?> activeConfig = GriefDefenderPlugin.getActiveConfig(this.worldUniqueId);
        GriefDefenderPlugin.getInstance().executor.execute(() -> {
            if (this.playerSubject == null || this.playerSubject.get() == null) {
                GDPermissionUser subject = PermissionHolderCache.getInstance().getOrCreateUser(this.playerID);
                this.playerSubject = new WeakReference<>(subject);
            }
            final GDPermissionUser subject = this.playerSubject.get();
            final MutableContextSet activeContexts = PermissionUtil.getInstance().getActiveContexts(subject);
            // permissions
            this.ignoreBorderCheck = PermissionUtil.getInstance().getPermissionValue(subject, GDPermissions.IGNORE_BORDER_CHECK, activeContexts).asBoolean();
            this.ignoreAdminClaims = PermissionUtil.getInstance().getPermissionValue(subject, GDPermissions.IGNORE_CLAIMS_ADMIN, activeContexts).asBoolean();
            this.ignoreTowns = PermissionUtil.getInstance().getPermissionValue(subject, GDPermissions.IGNORE_CLAIMS_TOWN, activeContexts).asBoolean();
            this.ignoreWilderness = PermissionUtil.getInstance().getPermissionValue(subject, GDPermissions.IGNORE_CLAIMS_WILDERNESS, activeContexts).asBoolean();
            this.ignoreBasicClaims = PermissionUtil.getInstance().getPermissionValue(subject, GDPermissions.IGNORE_CLAIMS_BASIC, activeContexts).asBoolean();
            this.canManageAdminClaims = PermissionUtil.getInstance().getPermissionValue(subject, GDPermissions.COMMAND_ADMIN_CLAIMS, activeContexts).asBoolean();
            this.canManageWilderness = PermissionUtil.getInstance().getPermissionValue(subject, GDPermissions.MANAGE_WILDERNESS, activeContexts).asBoolean();
            this.playerID = subject.getUniqueId();
            /*if (this.optionMaxClaimLevel > 255 || this.optionMaxClaimLevel <= 0 || this.optionMaxClaimLevel < this.optionMinClaimLevel) {
                this.optionMaxClaimLevel = 255;
            }
            if (this.optionMinClaimLevel < 0 || this.optionMinClaimLevel >= 255 || this.optionMinClaimLevel > this.optionMaxClaimLevel) {
                this.optionMinClaimLevel = 0;
            }*/
            this.dataInitialized = true;
            this.checkedDimensionHeight = false;
        });
    }

    public String getPlayerName() {
        if (this.playerName == null) {
            GDPermissionUser user = this.playerSubject.get();
            if (user == null) {
                user = PermissionHolderCache.getInstance().getOrCreateUser(this.playerID);
            }
            if (user != null) {
                this.playerName = user.getFriendlyName();
            }
            if (this.playerName == null) {
                this.playerName = "[unknown]";
            }
        }

        return this.playerName;
    }

    public void revertActiveVisual(Player player) {
        if (this.visualRevertTask != null) {
            this.visualRevertTask.cancel();
            this.visualRevertTask = null;
        }

        if (this.visualClaimId != null) {
            GDClaim claim = (GDClaim) GriefDefenderPlugin.getInstance().dataStore.getClaim(this.worldUniqueId, this.visualClaimId);
            if (claim != null) {
                claim.playersWatching.remove(this.playerID);
            }
        }
        this.visualClaimId = null;
        if (this.visualBlocks == null || !player.getWorld().equals(this.visualBlocks.get(0).getFinal().getLocation().getWorld())) {
            return;
        }

        for (int i = 0; i < this.visualBlocks.size(); i++) {
            BlockSnapshot snapshot = this.visualBlocks.get(i).getOriginal();
            NMSUtil.getInstance().sendBlockChange(player, snapshot);
        }
    }

    @Override
    public int getBlocksAccruedPerHour() {
        return GDPermissionManager.getInstance().getGlobalInternalOptionValue(this.getSubject(), Options.BLOCKS_ACCRUED_PER_HOUR, this).intValue();
    }

    @Override
    public int getChestClaimExpiration() {
        return GDPermissionManager.getInstance().getGlobalInternalOptionValue(this.getSubject(), Options.CHEST_EXPIRATION, this).intValue();
    }

    @Override
    public int getCreateClaimLimit(ClaimType type) {
        return GDPermissionManager.getInstance().getInternalOptionValue(this.getSubject(), Options.CREATE_LIMIT, type, this).intValue();
    }

    @Override
    public int getInitialClaimBlocks() {
        return GDPermissionManager.getInstance().getGlobalInternalOptionValue(this.getSubject(), Options.INITIAL_BLOCKS, this).intValue();
    }

    @Override
    public int getRemainingClaimBlocks() {
        final int initialClaimBlocks = GDPermissionManager.getInstance().getGlobalInternalOptionValue(this.getSubject(), Options.INITIAL_BLOCKS, this).intValue();
        int remainingBlocks = initialClaimBlocks + this.getAccruedClaimBlocks() + this.getBonusClaimBlocks();
        if (GriefDefenderPlugin.getGlobalConfig().getConfig().economy.economyMode) {
            if (!this.vaultProvider.getApi().hasAccount(this.getSubject().getOfflinePlayer())) {
                return 0;
            }

            final double currentFunds = this.vaultProvider.getApi().getBalance(this.getSubject().getOfflinePlayer());
            final Double economyBlockCost = GDPermissionManager.getInstance().getGlobalInternalOptionValue(this.getSubject(), Options.ECONOMY_BLOCK_COST, this);
            remainingBlocks =  (int) Math.round((currentFunds / economyBlockCost));
        } else {
            for (Claim claim : this.claimList) {
                if (claim.isSubdivision()) {
                    continue;
                }
    
                GDClaim gpClaim = (GDClaim) claim;
                if ((gpClaim.parent == null || gpClaim.parent.isAdminClaim()) && claim.getData().requiresClaimBlocks()) {
                    remainingBlocks -= claim.getClaimBlocks();
                }
            }
        }

        return remainingBlocks;
    }

    public int getTotalClaimsCost() {
        int totalCost = 0;
        for (Claim claim : this.claimList) {
            if (claim.isSubdivision()) {
                continue;
            }

            final GDClaim gpClaim = (GDClaim) claim;
            if ((gpClaim.parent == null || gpClaim.parent.isAdminClaim()) && claim.getData().requiresClaimBlocks()) {
                totalCost += claim.getClaimBlocks();
            }
        }

        return totalCost;
    }

    public double getRemainingChunks() {
        final double remainingChunks = this.getRemainingClaimBlocks() / 65536.0;
        return Math.round(remainingChunks * 100.0)/100.0;
    }

    @Override
    public int getAccruedClaimBlocks() {
        return this.playerStorage.getConfig().getAccruedClaimBlocks();
    }

    public boolean addAccruedClaimBlocks(int newAccruedClaimBlocks) {
        int currentTotal = this.getAccruedClaimBlocks();
        if ((currentTotal + newAccruedClaimBlocks) > this.getMaxAccruedClaimBlocks()) {
            return false;
        }

        this.playerStorage.getConfig().setAccruedClaimBlocks(currentTotal + newAccruedClaimBlocks);
        return true;
    }

    public boolean setAccruedClaimBlocks(int newAccruedClaimBlocks) {
        if (newAccruedClaimBlocks > this.getMaxAccruedClaimBlocks()) {
            return false;
        }

        this.playerStorage.getConfig().setAccruedClaimBlocks(newAccruedClaimBlocks);
        return true;
    }

    public int getBonusClaimBlocks() {
        return this.playerStorage.getConfig().getBonusClaimBlocks();
    }

    public void setBonusClaimBlocks(int bonusClaimBlocks) {
        this.playerStorage.getConfig().setBonusClaimBlocks(bonusClaimBlocks);
    }

    public int getClaimCreateMode() {
        if (this.optionClaimCreateMode == null) {
            int mode = GDPermissionManager.getInstance().getGlobalInternalOptionValue(this.getSubject(), Options.CREATE_MODE, this).intValue();
            // default to 0 if invalid
            if (mode != 0 && mode != 1) {
                mode = 0;
            }
            this.optionClaimCreateMode = mode;
        }

        return this.optionClaimCreateMode;
    }

    public void setClaimCreateMode(int mode) {
        // default to 0 if invalid
        if (mode != 0 && mode != 1) {
            mode = 0;
        }
        this.optionClaimCreateMode = mode;
    }

    public boolean canCreateClaim(Player player) {
        return canCreateClaim(player, false);
    }

    public boolean canCreateClaim(Player player, boolean sendMessage) {
        final int createMode = this.getClaimCreateMode();
        if (this.shovelMode == ShovelTypes.BASIC) {
            if (createMode == 0 && !player.hasPermission(GDPermissions.CLAIM_CREATE_BASIC)) {
                if (sendMessage) {
                    GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.permissionClaimCreate.toText());
                }
                return false;
            }
            if (createMode == 1 && !player.hasPermission(GDPermissions.CLAIM_CUBOID_BASIC)) {
                if (sendMessage) {
                    GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.permissionCuboid.toText());
                    GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.claimCuboidDisabled.toText());
                }
                return false;
            }
        } else if (this.shovelMode == ShovelTypes.SUBDIVISION) {
            if (createMode == 0 && !player.hasPermission(GDPermissions.CLAIM_CREATE_SUBDIVISION)) {
                if (sendMessage) {
                    GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.permissionClaimCreate.toText());
                }
                return false;
            } else if (!player.hasPermission(GDPermissions.CLAIM_CUBOID_SUBDIVISION)) {
                if (sendMessage) {
                    GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.permissionCuboid.toText());
                    GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.claimCuboidDisabled.toText());
                }
                return false;
            }
        } else if (this.shovelMode == ShovelTypes.ADMIN) {
            if (createMode == 0 && !player.hasPermission(GDPermissions.COMMAND_ADMIN_CLAIMS)) {
                return false;
            } else if (!player.hasPermission(GDPermissions.CLAIM_CUBOID_ADMIN)) {
                return false;
            }
        } else if (this.shovelMode == ShovelTypes.TOWN) {
            if (createMode == 0 && !player.hasPermission(GDPermissions.CLAIM_CREATE_TOWN)) {
                return false;
            } else if (!player.hasPermission(GDPermissions.CLAIM_CUBOID_TOWN)) {
                return false;
            }
        }

        return true;
    }

    public void saveAllData() {
        this.playerStorage.save();
    }

    public PlayerStorageData getStorageData() {
        return this.playerStorage;
    }

    public Set<Claim> getClaims() {
        return ImmutableSet.copyOf(this.claimList);
    }

    public Set<Claim> getInternalClaims() {
        return this.claimList;
    }

    public int getClaimTypeCount(ClaimType type) {
        int count = 0;
        for (Claim claim : this.claimList) {
            if (claim.getType() == type) {
                count++;
            }
        }
        return count;
    }

    public void setLastCollideEntityData(int entityId, boolean result) {
        this.lastCollideEntityId = entityId;
        this.lastCollideEntityResult = result;
    }

    public void setLastInteractData(GDClaim claim) {
        this.lastInteractResult = true;
        this.lastInteractClaim = claim.getUniqueId();
        this.lastTickCounter = NMSUtil.getInstance().getRunningServerTicks();
    }

    public boolean checkLastInteraction(GDClaim claim, GDPermissionUser user) {
        if (this.lastInteractResult && user != null && (NMSUtil.getInstance().getRunningServerTicks() - this.lastTickCounter) <= 2) {
            if (claim.getUniqueId().equals(this.lastInteractClaim) || claim.isWilderness()) {
                return true;
            }
        }

        return false;
    }

    public void setIgnoreClaims(boolean flag) {
        this.ignoreClaims = flag;
    }

    @Override
    public boolean canIgnoreClaim(Claim claim) {
        if (claim == null || this.ignoreClaims == false) {
            return false;
        }

        if (claim.isAdminClaim()) {
            return this.ignoreAdminClaims;
        } else if (claim.isWilderness()) {
            return this.ignoreWilderness;
        } else if (claim.isTown()) {
            return this.ignoreTowns;
        }
        return this.ignoreBasicClaims;
    }

    public boolean canManageOption(Player player, GDClaim claim, boolean isGroup) {
        if (claim.allowEdit(player) != null) {
            return false;
        }

        if (claim.isWilderness()) {
            return player.hasPermission(GDPermissions.MANAGE_WILDERNESS);
        }
        if (isGroup) {
            if (claim.isTown() && player.hasPermission(GDPermissions.COMMAND_OPTIONS_GROUP_TOWN)) {
                return true;
            }
            if (claim.isAdminClaim() && player.hasPermission(GDPermissions.COMMAND_OPTIONS_GROUP_ADMIN)) {
                return true;
            }
            if (claim.isBasicClaim() && player.hasPermission(GDPermissions.COMMAND_OPTIONS_GROUP_BASIC)) {
                return true;
            }
            if (claim.isSubdivision() && player.hasPermission(GDPermissions.COMMAND_OPTIONS_GROUP_SUBDIVISION)) {
                return true;
            }
        } else {
            if (claim.isTown() && player.hasPermission(GDPermissions.COMMAND_OPTIONS_PLAYER_TOWN)) {
                return true;
            }
            if (claim.isAdminClaim() && player.hasPermission(GDPermissions.COMMAND_OPTIONS_PLAYER_ADMIN)) {
                return true;
            }
            if (claim.isBasicClaim() && player.hasPermission(GDPermissions.COMMAND_OPTIONS_PLAYER_BASIC)) {
                return true;
            }
            if (claim.isSubdivision() && player.hasPermission(GDPermissions.COMMAND_OPTIONS_PLAYER_SUBDIVISION)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int getMaxAccruedClaimBlocks() {
        return GDPermissionManager.getInstance().getGlobalInternalOptionValue(this.getSubject(), Options.MAX_ACCRUED_BLOCKS, this).intValue();
    }

    @Override
    public double getAbandonedReturnRatio(ClaimType type) {
        return GDPermissionManager.getInstance().getGlobalInternalOptionValue(this.getSubject(), Options.ABANDON_RETURN_RATIO, this);
    }

    @Override
    public int getMaxClaimX(ClaimType type) {
        return GDPermissionManager.getInstance().getInternalOptionValue(this.getSubject(), Options.MAX_SIZE_X, type, this).intValue();
    }

    @Override
    public int getMaxClaimY(ClaimType type) {
        return GDPermissionManager.getInstance().getInternalOptionValue(this.getSubject(), Options.MAX_SIZE_Y, type, this).intValue();
    }

    @Override
    public int getMaxClaimZ(ClaimType type) {
        return GDPermissionManager.getInstance().getInternalOptionValue(this.getSubject(), Options.MAX_SIZE_Z, type, this).intValue();
    }

    @Override
    public int getMinClaimX(ClaimType type) {
        return GDPermissionManager.getInstance().getInternalOptionValue(this.getSubject(), Options.MIN_SIZE_X, type, this).intValue();
    }

    @Override
    public int getMinClaimY(ClaimType type) {
        return GDPermissionManager.getInstance().getInternalOptionValue(this.getSubject(), Options.MIN_SIZE_Y, type, this).intValue();
    }

    @Override
    public int getMinClaimZ(ClaimType type) {
        return GDPermissionManager.getInstance().getInternalOptionValue(this.getSubject(), Options.MIN_SIZE_Z, type, this).intValue();
    }

    @Override
    public int getMaxClaimLevel() {
        int maxClaimLevel = GDPermissionManager.getInstance().getGlobalInternalOptionValue(this.getSubject(), Options.MAX_LEVEL, this).intValue();
        if (!this.checkedDimensionHeight) {
            final World world = Bukkit.getServer().getWorld(this.worldUniqueId);
            if (world != null) {
                final int buildHeight = world.getMaxHeight() - 1;
                if (buildHeight < maxClaimLevel) {
                    maxClaimLevel = buildHeight;
                }
            }
            this.checkedDimensionHeight = true;
        }
        return maxClaimLevel;
    }

    @Override
    public int getMinClaimLevel() {
        return GDPermissionManager.getInstance().getGlobalInternalOptionValue(this.getSubject(), Options.MIN_LEVEL, this).intValue();
    }

    @Override
    public double getEconomyClaimBlockCost() {
        return GDPermissionManager.getInstance().getGlobalInternalOptionValue(this.getSubject(), Options.ECONOMY_BLOCK_COST, this);
    }

    @Override
    public double getEconomyClaimBlockReturn() {
        return GDPermissionManager.getInstance().getGlobalInternalOptionValue(this.getSubject(), Options.ECONOMY_BLOCK_SELL_RETURN, this);
    }

    @Override
    public double getTaxRate(ClaimType type) {
        return GDPermissionManager.getInstance().getInternalOptionValue(this.getSubject(), Options.TAX_RATE, type, this);
    }

    @Override
    public String getSubjectId() {
        return this.getSubject().getIdentifier();
    }

    public GDPermissionUser getSubject() {
        this.playerSubject = null;
        if (this.playerSubject == null || this.playerSubject.get() == null) {
            GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(this.playerID);
            this.playerSubject = new WeakReference<>(user);
        }

        return this.playerSubject.get();
    }

    public void sendTaxExpireMessage(Player player, GDClaim claim) {
        final double taxRate = GDPermissionManager.getInstance().getInternalOptionValue(player, Options.TAX_RATE, claim, this);
        final double taxOwed = claim.getClaimBlocks() * taxRate;
        final double remainingDays = GDPermissionManager.getInstance().getInternalOptionValue(player, Options.TAX_EXPIRATION_DAYS_KEEP, claim, this).intValue();
        final Component message = GriefDefenderPlugin.getInstance().messageData.taxClaimExpired
                .apply(ImmutableMap.of(
                "remaining_days", remainingDays,
                "tax_owed", taxOwed)).build();
        GriefDefenderPlugin.sendClaimDenyMessage(claim, player, message);
    }

    public double getTotalTax() {
        double totalTax = 0;
        final GDPermissionUser subject = this.getSubject();
        for (Claim claim : this.getInternalClaims()) {
            double playerTaxRate = GDPermissionManager.getInstance().getInternalOptionValue(subject, Options.TAX_RATE, claim, this);
            totalTax += (claim.getClaimBlocks() / 256) * playerTaxRate;
        }

        return totalTax;
    }

    public void onDisconnect() {
        this.visualBlocks = null;
        this.lastInteractClaim = null;
        this.claimResizing = null;
        this.claimSubdividing = null;
        if (this.visualRevertTask != null) {
            this.visualRevertTask.cancel();
            this.visualRevertTask = null;
        }
    }
}