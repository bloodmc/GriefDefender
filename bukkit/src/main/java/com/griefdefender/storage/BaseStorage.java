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
package com.griefdefender.storage;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimContexts;
import com.griefdefender.api.claim.ClaimResult;
import com.griefdefender.api.claim.ClaimResultType;
import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.api.claim.ClaimTypes;
import com.griefdefender.api.permission.Context;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.claim.GDClaimManager;
import com.griefdefender.claim.GDClaimResult;
import com.griefdefender.configuration.ClaimTemplateStorage;
import com.griefdefender.configuration.GriefDefenderConfig;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.configuration.type.ConfigBase;
import com.griefdefender.configuration.type.GlobalConfig;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.event.GDDeleteClaimEvent;
import com.griefdefender.internal.util.VecHelper;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.util.PermissionUtil;

import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.context.MutableContextSet;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BaseStorage {

    protected final Map<UUID, GDClaimManager> claimWorldManagers = new ConcurrentHashMap<>();

    public static Map<UUID, GriefDefenderConfig<ConfigBase>> dimensionConfigMap = new HashMap<>();
    public static Map<UUID, GriefDefenderConfig<ConfigBase>> worldConfigMap = new HashMap<>();
    public static Map<String, ClaimTemplateStorage> globalTemplates = new HashMap<>();
    public static GriefDefenderConfig<GlobalConfig> globalConfig;
    public static Map<UUID, GDPlayerData> GLOBAL_PLAYER_DATA = new ConcurrentHashMap<>();
    public static boolean USE_GLOBAL_PLAYER_STORAGE = true;
    public static Map<ClaimType, Map<String, Boolean>> CLAIM_FLAG_DEFAULTS = new HashMap<>();
    public static Map<String, Map<String, String>> CLAIM_OPTION_DEFAULTS = new HashMap<>();
    public static Map<String, Double> GLOBAL_OPTION_DEFAULTS = new HashMap<>();

    public final static Path dataLayerFolderPath = GriefDefenderPlugin.getInstance().getConfigPath();
    public final static Path globalPlayerDataPath = dataLayerFolderPath.resolve("GlobalPlayerData");

    public void initialize() throws Exception {
        USE_GLOBAL_PLAYER_STORAGE = GriefDefenderPlugin.getGlobalConfig().getConfig().playerdata.useGlobalPlayerDataStorage;
        if (USE_GLOBAL_PLAYER_STORAGE) {
            File globalPlayerDataFolder = globalPlayerDataPath.toFile();
            if (!globalPlayerDataFolder.exists()) {
                globalPlayerDataFolder.mkdirs();
            }
        }

        // handle default flag/option permissions
        this.setDefaultGlobalPermissions();
    }

    public void clearCachedPlayerData(UUID worldUniqueId, UUID playerUniqueId) {
        this.getClaimWorldManager(worldUniqueId).removePlayer(playerUniqueId);
    }

    public abstract void writeClaimToStorage(GDClaim claim);

    public abstract void deleteClaimFromStorage(GDClaim claim);

    public Claim getClaim(UUID worldUniqueId, UUID id) {
        return this.getClaimWorldManager(worldUniqueId).getClaimByUUID(id).orElse(null);
    }

    public void asyncSaveGlobalPlayerData(UUID playerID, GDPlayerData playerData) {
        // save everything except the ignore list
        this.overrideSavePlayerData(playerID, playerData);
    }

    abstract void overrideSavePlayerData(UUID playerID, GDPlayerData playerData);

    public ClaimResult createClaim(World world, Vector3i point1, Vector3i point2, ClaimType claimType, UUID ownerUniqueId, boolean cuboid) {
        return createClaim(world, point1, point2, claimType, ownerUniqueId, cuboid, null);
    }

    public ClaimResult createClaim(World world, Vector3i point1, Vector3i point2, ClaimType claimType, UUID ownerUniqueId, boolean cuboid, Claim parent) {
        ClaimResult claimResult = Claim.builder()
                .bounds(point1, point2)
                .cuboid(cuboid)
                .world(world.getUID())
                .type(claimType)
                .owner(ownerUniqueId)
                .parent(parent)
                .build();

        if (claimResult.successful()) {
            final Claim claim = claimResult.getClaim().get();
            final GDClaimManager claimManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(world.getUID());
            claimManager.addClaim(claim, true);
    
            /*if (claimResult.getClaims().size() > 1) {
                claim.migrateClaims(new ArrayList<>(claimResult.getClaims()));
            }*/
        }

        return claimResult;
    }

    public ClaimResult deleteAllAdminClaims(CommandSender src, World world) {
        GDClaimManager claimWorldManager = this.claimWorldManagers.get(world.getUID());
        if (claimWorldManager == null) {
            return new GDClaimResult(ClaimResultType.CLAIMS_DISABLED);
        }

        List<Claim> claimsToDelete = new ArrayList<Claim>();
        boolean adminClaimFound = false;
        for (Claim claim : claimWorldManager.getWorldClaims()) {
            if (claim.isAdminClaim()) {
                claimsToDelete.add(claim);
                adminClaimFound = true;
            }
        }

        if (!adminClaimFound) {
            return new GDClaimResult(ClaimResultType.CLAIM_NOT_FOUND);
        }

        GDCauseStackManager.getInstance().pushCause(src);
        GDDeleteClaimEvent event = new GDDeleteClaimEvent(ImmutableList.copyOf(claimsToDelete));
        GriefDefender.getEventManager().post(event);
        GDCauseStackManager.getInstance().popCause();
        if (event.cancelled()) {
            return new GDClaimResult(ClaimResultType.CLAIM_EVENT_CANCELLED,
                event.getMessage().orElse(GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.DELETE_ALL_TYPE_DENY,
                        ImmutableMap.of("type", TextComponent.of("ADMIN").color(TextColor.RED)))));
        }

        for (Claim claim : claimsToDelete) {
            PermissionUtil.getInstance().clearPermissions(GriefDefenderPlugin.DEFAULT_HOLDER, ImmutableSet.of(claim.getContext()));
            claimWorldManager.deleteClaimInternal(claim, true);
        }

        return new GDClaimResult(claimsToDelete, ClaimResultType.SUCCESS);
    }

    public ClaimResult deleteClaim(Claim claim, boolean deleteChildren) {
        GDClaimManager claimManager = this.getClaimWorldManager(claim.getWorldUniqueId());
        return claimManager.deleteClaim(claim, deleteChildren);
    }

    public void deleteClaimsForPlayer(UUID playerID) {
        if (BaseStorage.USE_GLOBAL_PLAYER_STORAGE && playerID != null) {
            List<Claim> claimsToDelete = new ArrayList<>(BaseStorage.GLOBAL_PLAYER_DATA.get(playerID).getInternalClaims());
            for (Claim claim : claimsToDelete) {
                PermissionUtil.getInstance().clearPermissions(GriefDefenderPlugin.DEFAULT_HOLDER, ImmutableSet.of(claim.getContext()));
                GDClaimManager claimWorldManager = this.claimWorldManagers.get(claim.getWorldUniqueId());
                claimWorldManager.deleteClaimInternal(claim, true);
            }
            return;
        }

        for (GDClaimManager claimWorldManager : this.claimWorldManagers.values()) {
            Set<Claim> claims = claimWorldManager.getInternalPlayerClaims(playerID);
            if (playerID == null) {
                claims = claimWorldManager.getWorldClaims();
            }
            if (claims == null) {
                continue;
            }

            List<Claim> claimsToDelete = new ArrayList<Claim>();
            for (Claim claim : claims) {
                if (!claim.isAdminClaim()) {
                    claimsToDelete.add(claim);
                }
            }
 
            for (Claim claim : claimsToDelete) {
                PermissionUtil.getInstance().clearPermissions(GriefDefenderPlugin.DEFAULT_HOLDER, ImmutableSet.of(claim.getContext()));
                claimWorldManager.deleteClaimInternal(claim, true);
            }
        }
    }

    public GDClaim getClaimAtPlayer(GDPlayerData playerData, Location location) {
        GDClaimManager claimManager = this.getClaimWorldManager(location.getWorld().getUID());
        return (GDClaim) claimManager.getClaimAtPlayer(location, playerData);
    }

    public GDClaim getClaimAtPlayer(Location location, GDPlayerData playerData, boolean useBorderBlockRadius) {
        GDClaimManager claimManager = this.getClaimWorldManager(location.getWorld().getUID());
        return (GDClaim) claimManager.getClaimAt(VecHelper.toVector3i(location), null, playerData, useBorderBlockRadius);
    }

    public GDClaim getClaimAtPlayer(Location location, GDClaim cachedClaim, GDPlayerData playerData, boolean useBorderBlockRadius) {
        GDClaimManager claimManager = this.getClaimWorldManager(location.getWorld().getUID());
        return (GDClaim) claimManager.getClaimAt(VecHelper.toVector3i(location), cachedClaim, playerData, useBorderBlockRadius);
    }

    public GDClaim getClaimAt(Location location) {
        GDClaimManager claimManager = this.getClaimWorldManager(location.getWorld().getUID());
        return (GDClaim) claimManager.getClaimAt(VecHelper.toVector3i(location), null, null, false);
    }

    public GDClaim getClaimAt(Location location, GDClaim cachedClaim) {
        GDClaimManager claimManager = this.getClaimWorldManager(location.getWorld().getUID());
        return (GDClaim) claimManager.getClaimAt(VecHelper.toVector3i(location), cachedClaim, null, false);
    }

    public GDPlayerData getPlayerData(World world, UUID playerUniqueId) {
        return this.getPlayerData(world.getUID(), playerUniqueId);
    }

    public GDPlayerData getPlayerData(UUID worldUniqueId, UUID playerUniqueId) {
        GDPlayerData playerData = null;
        GDClaimManager claimWorldManager = this.getClaimWorldManager(worldUniqueId);
        playerData = claimWorldManager.getPlayerDataMap().get(playerUniqueId);
        return playerData;
    }

    public GDPlayerData getOrCreatePlayerData(World world, UUID playerUniqueId) {
        return getOrCreatePlayerData(world.getUID(), playerUniqueId);
    }

    public GDPlayerData getOrCreatePlayerData(UUID worldUniqueId, UUID playerUniqueId) {
        GDClaimManager claimWorldManager = this.getClaimWorldManager(worldUniqueId);
        return claimWorldManager.getOrCreatePlayerData(playerUniqueId);
    }

    public void removePlayerData(UUID worldUniqueId, UUID playerUniqueId) {
        GDClaimManager claimWorldManager = this.getClaimWorldManager(worldUniqueId);
        claimWorldManager.removePlayer(playerUniqueId);
    }

    public GDClaimManager getClaimWorldManager(UUID worldUniqueId) {
        GDClaimManager claimWorldManager = null;
        if (worldUniqueId == null) {
            worldUniqueId = Bukkit.getWorlds().get(0).getUID();
        }
        World world = Bukkit.getWorld(worldUniqueId);
        if (world == null) {
            world = Bukkit.getWorlds().get(0);
        }
        claimWorldManager = this.claimWorldManagers.get(world.getUID());

        if (claimWorldManager == null) {
            registerWorld(world);
            claimWorldManager = this.claimWorldManagers.get(world.getUID());
        }
        return claimWorldManager;
    }

    public void removeClaimWorldManager(UUID worldUniqueId) {
        if (BaseStorage.USE_GLOBAL_PLAYER_STORAGE) {
            return;
        }
        this.claimWorldManagers.remove(worldUniqueId);
    }

    public void setDefaultGlobalPermissions() {
        // Admin defaults
        Set<Context> contexts = new HashSet<>();
        contexts.add(ClaimContexts.ADMIN_DEFAULT_CONTEXT);
        //contexts.add(GPContexts.DEFAULT_CONTEXT_HOLDER);
        final GriefDefenderConfig<GlobalConfig> activeConfig = GriefDefenderPlugin.getGlobalConfig();
        final Map<String, Boolean> adminDefaults = activeConfig.getConfig().permissionCategory.getFlagDefaults(ClaimTypes.ADMIN);
        CLAIM_FLAG_DEFAULTS.put(ClaimTypes.ADMIN, adminDefaults);
        this.setDefaultFlags(contexts, adminDefaults);

        // Basic defaults
        contexts = new HashSet<>();
        contexts.add(ClaimContexts.BASIC_DEFAULT_CONTEXT);
        //contexts.add(GPContexts.DEFAULT_CONTEXT_HOLDER);
        final Map<String, Boolean> basicDefaultFlags = activeConfig.getConfig().permissionCategory.getFlagDefaults(ClaimTypes.BASIC);
        final Map<String, String> basicDefaultOptions = activeConfig.getConfig().permissionCategory.getBasicOptionDefaults();
        CLAIM_FLAG_DEFAULTS.put(ClaimTypes.BASIC, basicDefaultFlags);
        CLAIM_OPTION_DEFAULTS.put(ClaimTypes.BASIC.toString(), basicDefaultOptions);
        this.setDefaultFlags(contexts, basicDefaultFlags);
        contexts = new HashSet<>();
        contexts.add(ClaimTypes.BASIC.getDefaultContext());
        this.setDefaultOptions(ClaimTypes.BASIC.toString(), contexts, new HashMap<>(basicDefaultOptions));

        // Town defaults
        contexts = new HashSet<>();
        contexts.add(ClaimContexts.TOWN_DEFAULT_CONTEXT);
        //contexts.add(GPContexts.DEFAULT_CONTEXT_HOLDER);
        //contexts.add(world.getContext());
        final Map<String, Boolean> townDefaults = activeConfig.getConfig().permissionCategory.getFlagDefaults(ClaimTypes.TOWN);
        final Map<String, String> townDefaultOptions = activeConfig.getConfig().permissionCategory.getTownOptionDefaults();
        CLAIM_FLAG_DEFAULTS.put(ClaimTypes.TOWN, townDefaults);
        CLAIM_OPTION_DEFAULTS.put(ClaimTypes.TOWN.toString(), townDefaultOptions);
        this.setDefaultFlags(contexts, townDefaults);
        contexts = new HashSet<>();
        contexts.add(ClaimTypes.TOWN.getDefaultContext());
        this.setDefaultOptions(ClaimTypes.TOWN.toString(), contexts, new HashMap<>(townDefaultOptions));

        // Subdivision defaults
        contexts = new HashSet<>();
        contexts.add(ClaimTypes.SUBDIVISION.getDefaultContext());
        //contexts.add(GPContexts.DEFAULT_CONTEXT_HOLDER);
        final Map<String, String> subdivisionDefaultOptions = activeConfig.getConfig().permissionCategory.getSubdivisionOptionDefaults();
        CLAIM_OPTION_DEFAULTS.put(ClaimTypes.SUBDIVISION.toString(), subdivisionDefaultOptions);
        this.setDefaultOptions(ClaimTypes.SUBDIVISION.toString(), contexts, new HashMap<>(subdivisionDefaultOptions));

        // Wilderness defaults
        contexts = new HashSet<>();
        contexts.add(ClaimContexts.WILDERNESS_DEFAULT_CONTEXT);
        //contexts.add(GPContexts.DEFAULT_CONTEXT_HOLDER);
        final Map<String, Boolean> wildernessDefaults = activeConfig.getConfig().permissionCategory.getFlagDefaults(ClaimTypes.WILDERNESS);
        //final Map<String, String> wildernessDefaultOptions = activeConfig.getConfig().options.getWildernessDefaults();
        CLAIM_FLAG_DEFAULTS.put(ClaimTypes.WILDERNESS, wildernessDefaults);
        //CLAIM_OPTION_DEFAULTS.put(ClaimTypes.WILDERNESS, wildernessDefaultOptions);
        this.setDefaultFlags(contexts, wildernessDefaults);
        //this.setDefaultOptions(ClaimTypes.TOWN, contexts, new HashMap<>(wildernessDefaultOptions));

        // Global default options
        contexts = new HashSet<>();
        contexts.add(ClaimContexts.GLOBAL_DEFAULT_CONTEXT);
        //contexts.add(GPContexts.DEFAULT_CONTEXT_HOLDER);
        final Map<String, String> globalDefaultOptions = activeConfig.getConfig().permissionCategory.getUserOptionDefaults();
        CLAIM_OPTION_DEFAULTS.put(ClaimContexts.GLOBAL_DEFAULT_CONTEXT.getName(), globalDefaultOptions);
        this.setDefaultOptions(ClaimContexts.GLOBAL_DEFAULT_CONTEXT.getName(), contexts, new HashMap<>(globalDefaultOptions));
        activeConfig.save();
    }

    private void setDefaultFlags(Set<Context> contexts, Map<String, Boolean> defaultFlags) {
        GriefDefenderPlugin.getInstance().executor.execute(() -> {
            MutableContextSet contextSet = MutableContextSet.fromEntries(contexts);
            for (Map.Entry<String, Boolean> mapEntry : defaultFlags.entrySet()) {
                final Node node = GriefDefenderPlugin.getInstance().getLuckPermsProvider().getApi().getNodeFactory().newBuilder(GDPermissions.FLAG_BASE + "." + mapEntry.getKey() + ".*").setValue(mapEntry.getValue()).withExtraContext(contextSet).build();
                GriefDefenderPlugin.DEFAULT_HOLDER.getLuckPermsHolder().setTransientPermission(node);
            }
            GriefDefenderPlugin.DEFAULT_HOLDER.getLuckPermsHolder().refreshCachedData();
        });
    }

    private void setDefaultOptions(String type, Set<Context> contexts, Map<String, String> defaultOptions) {
        GriefDefenderPlugin.getInstance().executor.execute(() -> {
            MutableContextSet contextSet = MutableContextSet.fromEntries(contexts);
            for (Map.Entry<String, String> optionEntry : defaultOptions.entrySet()) {
                final Node node = GriefDefenderPlugin.getInstance().getLuckPermsProvider().getApi().getNodeFactory().makeMetaNode("griefdefender." + optionEntry.getKey(), optionEntry.getValue()).withExtraContext(contextSet).build();
                GriefDefenderPlugin.DEFAULT_HOLDER.getLuckPermsHolder().setTransientPermission(node);
            }
            GriefDefenderPlugin.DEFAULT_HOLDER.getLuckPermsHolder().refreshCachedData();
        });
    }

    abstract GDPlayerData getPlayerDataFromStorage(UUID playerID);

    public abstract void registerWorld(World world);

    public abstract void loadWorldData(World world);

    public abstract void unloadWorldData(World world);

    abstract void loadClaimTemplates();
}