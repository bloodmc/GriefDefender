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
import com.google.common.reflect.TypeToken;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.User;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.api.claim.ShovelType;
import com.griefdefender.api.claim.ShovelTypes;
import com.griefdefender.api.data.PlayerData;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.api.permission.option.type.CreateModeType;
import com.griefdefender.api.permission.option.type.CreateModeTypes;
import com.griefdefender.api.permission.option.type.GameModeType;
import com.griefdefender.api.permission.option.type.GameModeTypes;
import com.griefdefender.api.permission.option.type.WeatherType;
import com.griefdefender.cache.EventResultCache;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.GriefDefenderConfig;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.permission.option.GDOptions;
import com.griefdefender.storage.BaseStorage;
import com.griefdefender.task.ClaimVisualRevertTask;
import com.griefdefender.util.PermissionUtil;
import net.kyori.text.Component;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.common.event.tracking.context.BlockTransaction;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

public class GDPlayerData implements PlayerData {

    public UUID playerID;
    public UUID worldUniqueId;
    private String worldName;
    private WeakReference<GDPermissionUser> playerSubject;
    private Set<Claim> claimList;
    private Set<Context> optionContexts;
    public Location<World> lastAfkCheckLocation;
    public Location<World> lastShovelLocation;
    public Location<World> endShovelLocation;
    public Location<World> lastValidInspectLocation;
    public Location<World> lastNonAirInspectLocation;
    public boolean isInvestigating = false;
    public boolean claimMode = false;
    public boolean claimTool = true;
    public ShovelType shovelMode = ShovelTypes.BASIC;

    public GDClaim claimResizing;
    public GDClaim claimSubdividing;
    public Map<UUID, Runnable> createBlockVisualRevertRunnables = new HashMap<>();
    public Map<UUID, List<Transaction<BlockSnapshot>>> createBlockVisualTransactions = new HashMap<>();
    public Map<UUID, Task> claimVisualRevertTasks = new HashMap<>();
    public Map<UUID, List<Transaction<BlockSnapshot>>> visualClaimBlocks = new HashMap<>();
    public List<BlockSnapshot> queuedVisuals = new ArrayList<>();
    public UUID tempVisualUniqueId = null;
    public UUID petRecipientUniqueId;

    public boolean ignoreClaims = false;
    public boolean debugClaimPermissions = false;
    public boolean inTown = false;
    public boolean townChat = false;
    public boolean lockPlayerDeathDrops = false;
    public boolean trappedRequest = false;
    public boolean runningPlayerCommands = false;
    public List<Component> chatLines = new ArrayList<>();
    public Instant recordChatTimestamp;
    public Instant commandInputTimestamp;
    public String commandInput;
    public Consumer<CommandSource> commandConsumer;

    // Always ignore active contexts by default
    // This prevents protection issues when other plugins call getActiveContext
    public boolean ignoreActiveContexts = true;

    public EventResultCache eventResultCache;

    // collide event cache
    public int lastCollideEntityId = 0;
    public boolean lastCollideEntityResult = false;

    private String playerName;

    public boolean allowFlight = false;
    public boolean ignoreFallDamage = false;
    public boolean inLiquid = false;

    public GameModeType lastGameMode = GameModeTypes.UNDEFINED;

    // teleport data
    public int teleportDelay = 0;
    public Location<World> teleportSourceLocation;
    public Location<World> teleportLocation;

    public Instant lastPvpTimestamp;
    public Instant lastTrappedTimestamp;
    public WeatherType lastWeatherType;

    // cached global option values
    public int minClaimLevel;
    private CreateModeType optionClaimCreateMode;

    // cached permission values
    // admin cache
    public boolean canManageAdminClaims = false;
    public boolean canManageWilderness = false;
    public boolean canManageGlobalOptions = false;
    public boolean canManageAdminOptions = false;
    public boolean canManageOverrideOptions = false;
    public boolean canManageFlagDefaults = false;
    public boolean canManageFlagOverrides = false;
    public boolean bypassBorderCheck = false;
    public boolean ignoreAdminClaims = false;
    public boolean ignoreBasicClaims = false;
    public boolean ignoreTowns = false;
    public boolean ignoreWilderness = false;
    // user cache
    public boolean userOptionPerkFlyOwner = false;
    public boolean userOptionPerkFlyAccessor = false;
    public boolean userOptionPerkFlyBuilder = false;
    public boolean userOptionPerkFlyContainer = false;
    public boolean userOptionPerkFlyManager = false;
    public boolean userOptionBypassPlayerDenyFlight = false;
    public boolean userOptionBypassPlayerDenyGodmode = false;
    public boolean userOptionBypassPlayerGamemode = false;

    // option cache
    public Boolean optionNoGodMode = null;
    public Double optionFlySpeed = null;
    public Double optionWalkSpeed = null;
    public GameModeType optionGameModeType = null;
    public WeatherType optionWeatherType = null;

    public boolean dataInitialized = false;
    public boolean showNoClaimsFoundMessage = true;
    public boolean useRestoreSchematic = false;
    private final int worldMaxHeight;

    public GDPlayerData(UUID worldUniqueId, String worldName, UUID playerUniqueId, Set<Claim> claims) {
        this.worldUniqueId = worldUniqueId;
        this.worldName = worldName;
        this.playerID = playerUniqueId;
        this.claimList = claims;
        final Set<Context> contexts = new HashSet<>();
        if (!BaseStorage.USE_GLOBAL_PLAYER_STORAGE) {
            contexts.add(new Context("server", PermissionUtil.getInstance().getServerName()));
            contexts.add(new Context("world", this.worldName.toLowerCase()));
        } else {
            final String contextType = GriefDefenderPlugin.getGlobalConfig().getConfig().playerdata.contextType;
            if (contextType.equalsIgnoreCase("world")) {
                contexts.add(new Context("world", this.worldName.toLowerCase()));
            } else if (contextType.equalsIgnoreCase("global")) {
                contexts.add(new Context("server", "global"));
            } else {
                contexts.add(new Context("server", PermissionUtil.getInstance().getServerName()));
            }
        }
        this.worldMaxHeight = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(this.worldUniqueId).getWorldMaxHeight();
        this.optionContexts = contexts;
        this.refreshPlayerOptions();
    }

    public void refreshPlayerOptions() {
        final GriefDefenderConfig<?> activeConfig = GriefDefenderPlugin.getActiveConfig(this.worldUniqueId);
        GriefDefenderPlugin.getInstance().executor.execute(() -> {
            if (this.playerSubject == null || this.playerSubject.get() == null) {
                GDPermissionUser subject = PermissionHolderCache.getInstance().getOrCreateUser(this.playerID);
                this.playerSubject = new WeakReference<>(subject);
            }
            final GDPermissionUser subject = this.playerSubject.get();
            final Set<Context> activeContexts = new HashSet<>();
            PermissionUtil.getInstance().addActiveContexts(activeContexts, subject);
            // admin permissions
            this.bypassBorderCheck = PermissionUtil.getInstance().getPermissionValue(subject, GDPermissions.BYPASS_BORDER_CHECK, activeContexts).asBoolean();
            this.ignoreAdminClaims = PermissionUtil.getInstance().getPermissionValue(subject, GDPermissions.IGNORE_CLAIMS_ADMIN, activeContexts).asBoolean();
            this.ignoreTowns = PermissionUtil.getInstance().getPermissionValue(subject, GDPermissions.IGNORE_CLAIMS_TOWN, activeContexts).asBoolean();
            this.ignoreWilderness = PermissionUtil.getInstance().getPermissionValue(subject, GDPermissions.IGNORE_CLAIMS_WILDERNESS, activeContexts).asBoolean();
            this.ignoreBasicClaims = PermissionUtil.getInstance().getPermissionValue(subject, GDPermissions.IGNORE_CLAIMS_BASIC, activeContexts).asBoolean();
            this.canManageAdminClaims = PermissionUtil.getInstance().getPermissionValue(subject, GDPermissions.COMMAND_ADMIN_CLAIMS, activeContexts).asBoolean();
            this.canManageWilderness = PermissionUtil.getInstance().getPermissionValue(subject, GDPermissions.MANAGE_WILDERNESS, activeContexts).asBoolean();
            this.canManageOverrideOptions = PermissionUtil.getInstance().getPermissionValue(subject, GDPermissions.MANAGE_OVERRIDE_OPTIONS, activeContexts).asBoolean();
            this.canManageGlobalOptions = PermissionUtil.getInstance().getPermissionValue(subject, GDPermissions.MANAGE_GLOBAL_OPTIONS, activeContexts).asBoolean();
            this.canManageAdminOptions = PermissionUtil.getInstance().getPermissionValue(subject, GDPermissions.MANAGE_ADMIN_OPTIONS, activeContexts).asBoolean();
            this.canManageFlagDefaults = PermissionUtil.getInstance().getPermissionValue(subject, GDPermissions.MANAGE_FLAG_DEFAULTS, activeContexts).asBoolean();
            this.canManageFlagOverrides = PermissionUtil.getInstance().getPermissionValue(subject, GDPermissions.MANAGE_FLAG_OVERRIDES, activeContexts).asBoolean();
            // user permissions
            this.userOptionPerkFlyOwner = PermissionUtil.getInstance().getPermissionValue(subject, GDPermissions.USER_OPTION_PERK_FLY_OWNER, activeContexts).asBoolean();
            this.userOptionPerkFlyManager = PermissionUtil.getInstance().getPermissionValue(subject, GDPermissions.USER_OPTION_PERK_FLY_MANAGER, activeContexts).asBoolean();
            this.userOptionPerkFlyBuilder = PermissionUtil.getInstance().getPermissionValue(subject, GDPermissions.USER_OPTION_PERK_FLY_BUILDER, activeContexts).asBoolean();
            this.userOptionPerkFlyContainer = PermissionUtil.getInstance().getPermissionValue(subject, GDPermissions.USER_OPTION_PERK_FLY_CONTAINER, activeContexts).asBoolean();
            this.userOptionPerkFlyAccessor = PermissionUtil.getInstance().getPermissionValue(subject, GDPermissions.USER_OPTION_PERK_FLY_ACCESSOR, activeContexts).asBoolean();
            this.userOptionBypassPlayerDenyFlight = PermissionUtil.getInstance().getPermissionValue(subject, GDPermissions.BYPASS_OPTION + "." + Options.PLAYER_DENY_FLIGHT.getName().toLowerCase(), activeContexts).asBoolean();
            this.userOptionBypassPlayerDenyGodmode = PermissionUtil.getInstance().getPermissionValue(subject, GDPermissions.BYPASS_OPTION + "." + Options.PLAYER_DENY_GODMODE.getName().toLowerCase(), activeContexts).asBoolean();
            this.userOptionBypassPlayerGamemode = PermissionUtil.getInstance().getPermissionValue(subject, GDPermissions.BYPASS_OPTION + "." + Options.PLAYER_GAMEMODE.getName().toLowerCase(), activeContexts).asBoolean();
            this.playerID = subject.getUniqueId();
            this.dataInitialized = true;
        });
    }

    @Override
    public User getUser() {
        return this.getSubject();
    }

    @Override
    public String getName() {
        if (this.playerName == null) {
            GDPermissionUser user = null;
            if (this.playerSubject != null) {
                user = this.playerSubject.get();
            }
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

    @Override
    public void revertAllVisuals() {
        this.lastShovelLocation = null;
        this.claimResizing = null;
        this.claimSubdividing = null;
        final Player player = this.getSubject().getOnlinePlayer();
        if (player == null) {
            return;
        }
        if (this.visualClaimBlocks.isEmpty()) {
            return;
        }

        for (Map.Entry<UUID, Task> mapEntry : this.claimVisualRevertTasks.entrySet()) {
            mapEntry.getValue().cancel();
        }

        final List<UUID> visualIds = new ArrayList<>(this.visualClaimBlocks.keySet());
        for (UUID visualUniqueId : visualIds) {
            final Claim claim = GriefDefenderPlugin.getInstance().dataStore.getClaim(player.getWorld().getUniqueId(), visualUniqueId);
            this.revertVisualBlocks(player, (GDClaim) claim, visualUniqueId);
        }
        if (GriefDefenderPlugin.getInstance().getWorldEditProvider() != null) {
            GriefDefenderPlugin.getInstance().getWorldEditProvider().revertAllVisuals(this.playerID);
        }
        // Revert any temp visuals
        this.revertTempVisuals();
    }

    public void revertTempVisuals() {
        if (this.tempVisualUniqueId != null) {
            this.revertClaimVisual(null, this.tempVisualUniqueId);
            this.tempVisualUniqueId = null;
        }
    }

    @Override
    public void revertVisual(UUID uuid) {
        final Claim claim = GriefDefenderPlugin.getInstance().dataStore.getClaim(this.worldUniqueId, uuid);
        this.revertClaimVisual((GDClaim) claim, uuid);
    }

    @Override
    public void revertVisual(Claim claim) {
        this.revertClaimVisual((GDClaim) claim);
    }

    public void revertClaimVisual(GDClaim claim) {
        this.revertClaimVisual(claim, claim.getUniqueId());
    }

    public void revertClaimVisual(GDClaim claim, UUID visualUniqueId) {
        final Player player = this.getSubject().getOnlinePlayer();
        if (player == null) {
            return;
        }

        for (Map.Entry<UUID, Task> mapEntry : this.claimVisualRevertTasks.entrySet()) {
            if (visualUniqueId.equals(mapEntry.getKey())) {
                mapEntry.getValue().cancel();
                break;
            }
        }
        if (GriefDefenderPlugin.getInstance().getWorldEditProvider() != null) {
            GriefDefenderPlugin.getInstance().getWorldEditProvider().revertClaimCUIVisual(visualUniqueId, this.playerID);
        }

        this.revertVisualBlocks(player, claim, visualUniqueId);
    }

    private void revertVisualBlocks(Player player, GDClaim claim, UUID visualUniqueId) {
        final List<Transaction<BlockSnapshot>> visualTransactions = this.visualClaimBlocks.get(visualUniqueId);
        if (visualTransactions == null || visualTransactions.isEmpty()) {
            return;
        }

        // Gather create block visuals
        final List<Transaction<BlockSnapshot>> createBlockVisualTransactions = new ArrayList<>();
        for (Runnable runnable : this.createBlockVisualRevertRunnables.values()) {
            final ClaimVisualRevertTask revertTask = (ClaimVisualRevertTask) runnable;
            if (revertTask.getVisualUniqueId().equals(visualUniqueId)) {
                continue;
            }
            final List<Transaction<BlockSnapshot>> blockTransactions = this.createBlockVisualTransactions.get(revertTask.getVisualUniqueId());
            if (blockTransactions != null) {
                createBlockVisualTransactions.addAll(blockTransactions);
            }
        }

        for (int i = 0; i < visualTransactions.size(); i++) {
            BlockSnapshot snapshot = visualTransactions.get(i).getOriginal();
            // If original block does not exist, do not send to player
            final Location<World> location = snapshot.getLocation().orElse(null);
            if (location != null && (snapshot.getState().getType() != location.getBlockType())) {
                if (claim != null) {
                    claim.markVisualDirty = true;
                }
                continue;
            }
            boolean ignoreVisual = false;
            for (Transaction<BlockSnapshot> createVisualTransaction : createBlockVisualTransactions) {
                if (createVisualTransaction.getOriginal().getLocation().get().equals(snapshot.getLocation().get())) {
                    ignoreVisual = true;
                    break;
                }
            }
            if (ignoreVisual) {
                continue;
            }
            player.sendBlockChange(snapshot.getPosition(), snapshot.getState());
        }
        if (claim != null) {
            claim.playersWatching.remove(this.playerID);
        }

        this.claimVisualRevertTasks.remove(visualUniqueId);
        this.visualClaimBlocks.remove(visualUniqueId);
        this.createBlockVisualRevertRunnables.remove(visualUniqueId);
        this.createBlockVisualTransactions.remove(visualUniqueId);
    }

    @Override
    public int getBlocksAccruedPerHour() {
        final Integer value = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), this.getSubject(), Options.BLOCKS_ACCRUED_PER_HOUR);
        if (value == null) {
            return Options.BLOCKS_ACCRUED_PER_HOUR.getDefaultValue();
        }
        return value;
    }

    @Override
    public int getChestClaimExpiration() {
        final Integer value = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), this.getSubject(), Options.CHEST_EXPIRATION);
        if (value == null) {
            return Options.CHEST_EXPIRATION.getDefaultValue();
        }
        return value;
    }

    @Override
    public int getCreateClaimLimit(ClaimType type) {
        final Integer value = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), this.getSubject(), Options.CREATE_LIMIT, type);
        if (value == null) {
            return Options.CREATE_LIMIT.getDefaultValue();
        }
        return value;
    }

    public CreateModeType getCreateMode() {
        final CreateModeType value = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(CreateModeType.class), this.getSubject(), Options.CREATE_MODE);
        if (value == null || value == CreateModeTypes.UNDEFINED) {
            return CreateModeTypes.AREA;
        }
        return value;
    }

    @Override
    public int getInitialClaimBlocks() {
        final Integer value = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), this.getSubject(), Options.INITIAL_BLOCKS);
        if (value == null) {
            return Options.INITIAL_BLOCKS.getDefaultValue();
        }
        return value;
    }

    public double getInternalEconomyBlockCost() {
        final Double value = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Double.class), this.getSubject(), Options.ECONOMY_BLOCK_COST);
        if (value == null) {
            return Options.ECONOMY_BLOCK_COST.getDefaultValue();
        }
        return value;
    }

    public int getInternalRemainingClaimBlocks() {
        final int initialClaimBlocks = this.getInitialClaimBlocks();
        int remainingBlocks = initialClaimBlocks + this.getAccruedClaimBlocks() + this.getBonusClaimBlocks();

        for (Claim claim : this.claimList) {
            if (claim.isSubdivision()) {
                continue;
            }

            GDClaim gpClaim = (GDClaim) claim;
            if ((gpClaim.parent == null || gpClaim.parent.isAdminClaim()) && claim.getData().requiresClaimBlocks()) {
                remainingBlocks -= claim.getClaimBlocks();
            }
        }

        return remainingBlocks;
    }

    @Override
    public int getRemainingClaimBlocks() {
        final int initialClaimBlocks = this.getInitialClaimBlocks();
        int remainingBlocks = initialClaimBlocks + this.getAccruedClaimBlocks() + this.getBonusClaimBlocks();

        if (GriefDefenderPlugin.getInstance().isEconomyModeEnabled()) {
            return this.getInternalEconomyAvailablePurchaseCost();
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

    public int getInternalEconomyAvailablePurchaseCost() {
        if (GriefDefenderPlugin.getInstance().isEconomyModeEnabled()) {
            final Account playerAccount = GriefDefenderPlugin.getInstance().economyService.get().getOrCreateAccount(this.playerID).orElse(null);
            if (playerAccount == null) {
                return 0;
            }

            final Currency defaultCurrency = GriefDefenderPlugin.getInstance().economyService.get().getDefaultCurrency();
            final BigDecimal currentFunds = playerAccount.getBalance(defaultCurrency);
            final Double economyBlockCost = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Double.class), this.getSubject(), Options.ECONOMY_BLOCK_COST);
            return (int) Math.round((currentFunds.doubleValue() / economyBlockCost));
        }
        return 0;
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
        return GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), this.getSubject(), Options.ACCRUED_BLOCKS, new HashSet<>(this.optionContexts));
    }

    public boolean addAccruedClaimBlocks(int newAccruedClaimBlocks) {
        int currentTotal = this.getAccruedClaimBlocks();
        if ((currentTotal + newAccruedClaimBlocks) > this.getMaxAccruedClaimBlocks()) {
            return false;
        }

        this.setAccruedClaimBlocks(currentTotal + newAccruedClaimBlocks);
        return true;
    }

    public boolean setAccruedClaimBlocks(int newAccruedClaimBlocks) {
        return this.setAccruedClaimBlocks(newAccruedClaimBlocks, true);
    }

    public boolean setAccruedClaimBlocks(int newAccruedClaimBlocks, boolean checkMax) {
        if (checkMax && newAccruedClaimBlocks > this.getMaxAccruedClaimBlocks()) {
            return false;
        }

        GDPermissionManager.getInstance().setOption(Options.ACCRUED_BLOCKS, this.getSubject(), String.valueOf(newAccruedClaimBlocks), new HashSet<>(this.optionContexts));
        return true;
    }

    public int getBonusClaimBlocks() {
        return GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), this.getSubject(), Options.BONUS_BLOCKS, new HashSet<>(this.optionContexts));
    }

    public void setBonusClaimBlocks(int bonusClaimBlocks) {
        GDPermissionManager.getInstance().setOption(Options.BONUS_BLOCKS, this.getSubject(), String.valueOf(bonusClaimBlocks), new HashSet<>(this.optionContexts));
    }

    public CreateModeType getClaimCreateMode() {
        if (this.optionClaimCreateMode == null) {
            CreateModeType mode = this.getCreateMode();
            // default to 0 if invalid
            if (mode == null) {
                mode = CreateModeTypes.AREA;
            }
            this.optionClaimCreateMode = mode;
        }

        return this.optionClaimCreateMode;
    }

    public void setClaimCreateMode(CreateModeType mode) {
        this.optionClaimCreateMode = mode;
    }

    public boolean canCreateClaim(Player player) {
        return canCreateClaim(player, false);
    }

    public boolean canCreateClaim(Player player, boolean sendMessage) {
        final CreateModeType createMode = this.getClaimCreateMode();
        if (this.shovelMode == ShovelTypes.BASIC) {
            if (createMode == CreateModeTypes.AREA && !player.hasPermission(GDPermissions.CLAIM_CREATE_BASIC)) {
                if (sendMessage) {
                    GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().PERMISSION_CLAIM_CREATE);
                }
                return false;
            }
            if (createMode == CreateModeTypes.VOLUME && !player.hasPermission(GDPermissions.CLAIM_CUBOID_BASIC)) {
                if (sendMessage) {
                    GriefDefenderPlugin.sendMessage(player,MessageCache.getInstance().PERMISSION_CUBOID);
                    GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().COMMAND_CUBOID_DISABLED);
                }
                return false;
            }
        } else if (this.shovelMode == ShovelTypes.SUBDIVISION) {
            if (createMode == CreateModeTypes.AREA && !player.hasPermission(GDPermissions.CLAIM_CREATE_SUBDIVISION)) {
                if (sendMessage) {
                    GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().PERMISSION_CLAIM_CREATE);
                }
                return false;
            } else if (createMode == CreateModeTypes.VOLUME && !player.hasPermission(GDPermissions.CLAIM_CUBOID_SUBDIVISION)) {
                if (sendMessage) {
                    GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().PERMISSION_CUBOID);
                    GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().COMMAND_CUBOID_DISABLED);
                }
                return false;
            }
        } else if (this.shovelMode == ShovelTypes.ADMIN) {
            if (createMode == CreateModeTypes.AREA && !player.hasPermission(GDPermissions.COMMAND_ADMIN_CLAIMS)) {
                return false;
            } else if (!player.hasPermission(GDPermissions.CLAIM_CUBOID_ADMIN)) {
                return false;
            }
        } else if (this.shovelMode == ShovelTypes.TOWN) {
            if (createMode == CreateModeTypes.AREA && !player.hasPermission(GDPermissions.CLAIM_CREATE_TOWN)) {
                return false;
            } else if (!player.hasPermission(GDPermissions.CLAIM_CUBOID_TOWN)) {
                return false;
            }
        }

        return true;
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

    @Override
    public int getMaxAccruedClaimBlocks() {
        return GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), this.getSubject(), Options.MAX_ACCRUED_BLOCKS);
    }

    @Override
    public double getAbandonedReturnRatio(ClaimType type) {
        return GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Double.class), this.getSubject(), Options.ABANDON_RETURN_RATIO);
    }

    @Override
    public int getMaxClaimX(ClaimType type) {
        return GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), this.getSubject(), Options.MAX_SIZE_X, type);
    }

    @Override
    public int getMaxClaimY(ClaimType type) {
        return GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), this.getSubject(), Options.MAX_SIZE_Y, type);
    }

    @Override
    public int getMaxClaimZ(ClaimType type) {
        return GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), this.getSubject(), Options.MAX_SIZE_Z, type);
    }

    @Override
    public int getMinClaimX(ClaimType type) {
        return GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), this.getSubject(), Options.MIN_SIZE_X, type);
    }

    @Override
    public int getMinClaimY(ClaimType type) {
        return GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), this.getSubject(), Options.MIN_SIZE_Y, type);
    }

    @Override
    public int getMinClaimZ(ClaimType type) {
        return GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), this.getSubject(), Options.MIN_SIZE_Z, type);
    }

    @Override
    public int getMaxClaimLevel() {
        int maxClaimLevel = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), this.getSubject(), Options.MAX_LEVEL);
        if (this.worldMaxHeight > -1 && this.worldMaxHeight < maxClaimLevel) {
            maxClaimLevel = this.worldMaxHeight;
        }
        return maxClaimLevel;
    }

    @Override
    public int getMinClaimLevel() {
        return GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), this.getSubject(), Options.MIN_LEVEL);
    }

    @Override
    public double getEconomyClaimBlockCost() {
        return GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Double.class), this.getSubject(), Options.ECONOMY_BLOCK_COST);
    }

    @Override
    public double getEconomyClaimBlockReturn() {
        return GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Double.class), this.getSubject(), Options.ECONOMY_BLOCK_SELL_RETURN);
    }

    @Override
    public double getTaxRate(ClaimType type) {
        return GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Double.class), this.getSubject(), Options.TAX_RATE, type);
    }

    @Override
    public UUID getUniqueId() {
        return this.getSubject().getUniqueId();
    }

    public GDPermissionUser getSubject() {
        if (this.playerSubject == null || this.playerSubject.get() == null) {
            GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(this.playerID);
            this.playerSubject = new WeakReference<>(user);
        }

        return this.playerSubject.get();
    }

    public void sendTaxExpireMessage(Player player, GDClaim claim) {
        final double taxRate = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Double.class), player, Options.TAX_RATE, claim);
        final double taxOwed = claim.getClaimBlocks() * taxRate;
        final double remainingDays = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), player, Options.TAX_EXPIRATION_DAYS_KEEP, claim);
        final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.TAX_EXPIRED, ImmutableMap.of(
                "days", remainingDays,
                "amount", taxOwed));
        GriefDefenderPlugin.sendClaimDenyMessage(claim, player, message);
    }

    public double getTotalTax() {
        double totalTax = 0;
        final GDPermissionUser subject = this.getSubject();
        for (Claim claim : this.getInternalClaims()) {
            double playerTaxRate = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Double.class), subject, Options.TAX_RATE, claim);
            totalTax += (claim.getClaimBlocks() / 256) * playerTaxRate;
        }

        return totalTax;
    }

    @Override
    public boolean canPvp(Claim claim) {
        if (!((GDClaim) claim).getWorld().getProperties().isPVPEnabled()) {
            return false;
        }
        if (!claim.isPvpAllowed()) {
            return false;
        }

        if (GDOptions.PVP) {
            final Tristate result = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Tristate.class), this.getSubject(), Options.PVP, claim);
            if (result != Tristate.UNDEFINED) {
                return result.asBoolean();
            }
        }
        return true;
    }

    @Override
    public boolean inPvpCombat() {
        final Player player = this.getSubject().getOnlinePlayer();
        if (this.lastPvpTimestamp == null || player == null) {
            return false;
        }

        final Instant now = Instant.now();
        final int combatTimeout = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), player, Options.PVP_COMBAT_TIMEOUT);
        if (combatTimeout <= 0) {
            return false;
        }

        if (this.lastPvpTimestamp.plusSeconds(combatTimeout).isBefore(now)) {
            this.lastPvpTimestamp = null;
            return false;
        }

        return true;
    }

    @Override
    public int getRemainingPvpCombatTime(Claim claim) {
        return this.getPvpCombatTimeRemaining((GDClaim) claim);
    }

    public int getPvpCombatTimeRemaining(GDClaim claim) {
        final Player player = this.getSubject().getOnlinePlayer();
        if (this.lastPvpTimestamp == null || player == null) {
            return 0;
        }

        final Instant now = Instant.now();
        int combatTimeout = 0;
        if (GDOptions.PVP_COMBAT_TIMEOUT) {
            combatTimeout = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), player, Options.PVP_COMBAT_TIMEOUT, claim);
        }
        if (combatTimeout <= 0) {
            return 0;
        }

        if (this.lastPvpTimestamp.plusSeconds(combatTimeout).isBefore(now)) {
            this.lastPvpTimestamp = null;
            return 0;
        }

        final int duration = (int) Duration.between(this.lastPvpTimestamp, now).getSeconds();
        return combatTimeout - duration;
    }

    public void updateRecordChat() {
        final Player player = this.getSubject().getOnlinePlayer();
        if (this.recordChatTimestamp == null || player == null) {
            return;
        }

        final Instant now = Instant.now();
        final int timeout = GriefDefenderPlugin.getGlobalConfig().getConfig().gui.chatCaptureIdleTimeout;
        if (timeout <= 0) {
            return;
        }

        if (this.recordChatTimestamp.plusSeconds(timeout).isBefore(now)) {
            this.recordChatTimestamp = null;
        }
    }

    public boolean isRecordingChat() {
        return this.recordChatTimestamp != null;
    }

    public void updateCommandInput() {
        final Player player = this.getSubject().getOnlinePlayer();
        if (this.commandInputTimestamp == null || player == null) {
            return;
        }

        final Instant now = Instant.now();
        final int timeout = GriefDefenderPlugin.getGlobalConfig().getConfig().gui.commandInputIdleTimeout;
        if (timeout <= 0) {
            return;
        }

        if (this.commandInputTimestamp.plusSeconds(timeout).isBefore(now)) {
            this.commandInputTimestamp = null;
        }
    }

    public boolean isWaitingForInput() {
        return this.commandInputTimestamp != null;
    }

    public void onClaimDelete() {
        this.lastShovelLocation = null;
        this.eventResultCache = null;
        this.claimResizing = null;
        this.claimSubdividing = null;
    }

    public void resetOptionCache() {
        this.optionNoGodMode = null;
        this.optionFlySpeed = null;
        this.optionWalkSpeed = null;
        this.optionGameModeType = null;
        this.optionWeatherType = null;
    }

    public void onDisconnect() {
        this.claimVisualRevertTasks.clear();
        this.visualClaimBlocks.clear();
        this.createBlockVisualTransactions.clear();
        this.createBlockVisualRevertRunnables.clear();
        this.queuedVisuals.clear();
        this.claimMode = false;
        this.claimTool = true;
        this.debugClaimPermissions = false;
        this.ignoreClaims = false;
        this.lastShovelLocation = null;
        this.eventResultCache = null;
        this.claimResizing = null;
        this.claimSubdividing = null;
        this.commandInputTimestamp = null;
        this.recordChatTimestamp = null;
        this.tempVisualUniqueId = null;
        if (GriefDefenderPlugin.getInstance().getWorldEditProvider() != null) {
            GriefDefenderPlugin.getInstance().getWorldEditProvider().revertAllVisuals(this.playerID);
        }
    }
}
