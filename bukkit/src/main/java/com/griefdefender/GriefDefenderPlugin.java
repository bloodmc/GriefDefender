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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import com.griefdefender.provider.permissionsex.PermissionsExProvider;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.plugin.Plugin;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;
import com.google.inject.Guice;
import com.google.inject.Stage;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimBlockSystem;
import com.griefdefender.api.claim.ClaimSchematic;
import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.api.claim.TrustType;
import com.griefdefender.api.economy.BankTransaction;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.flag.Flag;
import com.griefdefender.api.permission.flag.FlagData;
import com.griefdefender.api.permission.flag.FlagDefinition;
import com.griefdefender.api.permission.option.Option;
import com.griefdefender.api.permission.option.type.CreateModeType;
import com.griefdefender.api.permission.option.type.GameModeType;
import com.griefdefender.api.permission.option.type.WeatherType;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.claim.GDClaimManager;
import com.griefdefender.command.CommandAdjustBonusClaimBlocks;
import com.griefdefender.command.CommandCallback;
import com.griefdefender.command.CommandClaimAbandon;
import com.griefdefender.command.CommandClaimAbandonAll;
import com.griefdefender.command.CommandClaimAbandonTop;
import com.griefdefender.command.CommandClaimAdmin;
import com.griefdefender.command.CommandClaimBan;
import com.griefdefender.command.CommandClaimBank;
import com.griefdefender.command.CommandClaimBasic;
import com.griefdefender.command.CommandClaimBuy;
import com.griefdefender.command.CommandClaimBuyBlocks;
import com.griefdefender.command.CommandClaimClear;
import com.griefdefender.command.CommandClaimContract;
import com.griefdefender.command.CommandClaimCreate;
import com.griefdefender.command.CommandClaimCuboid;
import com.griefdefender.command.CommandClaimDelete;
import com.griefdefender.command.CommandClaimDeleteAll;
import com.griefdefender.command.CommandClaimDeleteAllAdmin;
import com.griefdefender.command.CommandClaimDeleteTop;
import com.griefdefender.command.CommandClaimExpand;
import com.griefdefender.command.CommandClaimFarewell;
import com.griefdefender.command.CommandClaimFlag;
import com.griefdefender.command.CommandClaimFlagDebug;
import com.griefdefender.command.CommandClaimFlagGroup;
import com.griefdefender.command.CommandClaimFlagPlayer;
import com.griefdefender.command.CommandClaimFlagReset;
import com.griefdefender.command.CommandClaimGreeting;
import com.griefdefender.command.CommandClaimIgnore;
import com.griefdefender.command.CommandClaimInfo;
import com.griefdefender.command.CommandClaimInherit;
import com.griefdefender.command.CommandClaimList;
import com.griefdefender.command.CommandClaimMode;
import com.griefdefender.command.CommandClaimName;
import com.griefdefender.command.CommandClaimOption;
import com.griefdefender.command.CommandClaimOptionGroup;
import com.griefdefender.command.CommandClaimOptionPlayer;
import com.griefdefender.command.CommandClaimPermissionGroup;
import com.griefdefender.command.CommandClaimPermissionPlayer;
import com.griefdefender.command.CommandClaimReserve;
import com.griefdefender.command.CommandClaimSchematic;
import com.griefdefender.command.CommandClaimSell;
import com.griefdefender.command.CommandClaimSellBlocks;
import com.griefdefender.command.CommandClaimSetSpawn;
import com.griefdefender.command.CommandClaimSpawn;
import com.griefdefender.command.CommandClaimSubdivision;
import com.griefdefender.command.CommandClaimTown;
import com.griefdefender.command.CommandClaimTransfer;
import com.griefdefender.command.CommandClaimUnban;
import com.griefdefender.command.CommandClaimWorldEdit;
import com.griefdefender.command.CommandDebug;
import com.griefdefender.command.CommandGDReload;
import com.griefdefender.command.CommandGDVersion;
import com.griefdefender.command.CommandGiveBlocks;
import com.griefdefender.command.CommandGivePet;
import com.griefdefender.command.CommandHelp;
import com.griefdefender.command.CommandPagination;
import com.griefdefender.command.CommandPlayerInfo;
import com.griefdefender.command.CommandRestoreClaim;
import com.griefdefender.command.CommandRestoreNature;
import com.griefdefender.command.CommandSetAccruedClaimBlocks;
import com.griefdefender.command.CommandTownChat;
import com.griefdefender.command.CommandTownTag;
import com.griefdefender.command.CommandTrustGroup;
import com.griefdefender.command.CommandTrustGroupAll;
import com.griefdefender.command.CommandTrustList;
import com.griefdefender.command.CommandTrustPlayer;
import com.griefdefender.command.CommandTrustPlayerAll;
import com.griefdefender.command.CommandUntrustGroup;
import com.griefdefender.command.CommandUntrustGroupAll;
import com.griefdefender.command.CommandUntrustPlayer;
import com.griefdefender.command.CommandUntrustPlayerAll;
import com.griefdefender.command.gphelper.CommandAccessTrust;
import com.griefdefender.command.gphelper.CommandContainerTrust;
import com.griefdefender.configuration.FlagConfig;
import com.griefdefender.configuration.GriefDefenderConfig;
import com.griefdefender.configuration.MessageDataConfig;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.configuration.OptionConfig;
import com.griefdefender.configuration.category.BlacklistCategory;
import com.griefdefender.configuration.serializer.ClaimTypeSerializer;
import com.griefdefender.configuration.serializer.ComponentConfigSerializer;
import com.griefdefender.configuration.serializer.CreateModeTypeSerializer;
import com.griefdefender.configuration.serializer.FlagDefinitionSerializer;
import com.griefdefender.configuration.serializer.GameModeTypeSerializer;
import com.griefdefender.configuration.serializer.WeatherTypeSerializer;
import com.griefdefender.configuration.type.ConfigBase;
import com.griefdefender.configuration.type.GlobalConfig;
import com.griefdefender.economy.GDBankTransaction;
import com.griefdefender.inject.GriefDefenderImplModule;
import com.griefdefender.internal.provider.WorldEditProvider;
import com.griefdefender.internal.provider.WorldGuardProvider;
import com.griefdefender.internal.registry.BlockTypeRegistryModule;
import com.griefdefender.internal.registry.EntityTypeRegistryModule;
import com.griefdefender.internal.registry.GDBlockType;
import com.griefdefender.internal.registry.GDEntityType;
import com.griefdefender.internal.registry.GDItemType;
import com.griefdefender.internal.registry.ItemTypeRegistryModule;
import com.griefdefender.internal.schematic.GDClaimSchematic;
import com.griefdefender.internal.util.NMSUtil;
import com.griefdefender.internal.util.VecHelper;
import com.griefdefender.listener.BlockEventHandler;
import com.griefdefender.listener.BlockEventTracker;
import com.griefdefender.listener.CommandEventHandler;
import com.griefdefender.listener.EntityEventHandler;
import com.griefdefender.listener.PlayerEventHandler;
import com.griefdefender.listener.WorldEventHandler;
import com.griefdefender.permission.ContextGroupKeys;
import com.griefdefender.permission.GDPermissionHolder;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.flag.GDFlagData;
import com.griefdefender.permission.flag.GDFlagDefinition;
import com.griefdefender.permission.flag.GDFlags;
import com.griefdefender.provider.DynmapProvider;
import com.griefdefender.provider.EssentialsProvider;
import com.griefdefender.provider.LuckPermsProvider;
import com.griefdefender.provider.PermissionProvider;
import com.griefdefender.provider.PlaceholderProvider;
import com.griefdefender.provider.VaultProvider;
import com.griefdefender.registry.ChatTypeRegistryModule;
import com.griefdefender.registry.ClaimTypeRegistryModule;
import com.griefdefender.registry.CreateModeTypeRegistryModule;
import com.griefdefender.registry.FlagDefinitionRegistryModule;
import com.griefdefender.registry.FlagRegistryModule;
import com.griefdefender.registry.GameModeTypeRegistryModule;
import com.griefdefender.registry.OptionRegistryModule;
import com.griefdefender.registry.ResultTypeRegistryModule;
import com.griefdefender.registry.ShovelTypeRegistryModule;
import com.griefdefender.registry.TrustTypeRegistryModule;
import com.griefdefender.registry.WeatherTypeRegistryModule;
import com.griefdefender.storage.BaseStorage;
import com.griefdefender.storage.FileStorage;
import com.griefdefender.task.ClaimBlockTask;
import com.griefdefender.task.ClaimCleanupTask;
import com.griefdefender.task.PlayerTickTask;
import com.griefdefender.util.PermissionUtil;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.PaperCommandManager;
import co.aikar.commands.RegisteredCommand;
import co.aikar.commands.RootCommand;
import co.aikar.timings.lib.MCTiming;
import co.aikar.timings.lib.TimingManager;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;
import net.kyori.text.serializer.plain.PlainComponentSerializer;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;

public class GriefDefenderPlugin {

    private static GriefDefenderPlugin instance;
    public static final String MOD_ID = "GriefDefender";
    public static final String API_VERSION = GriefDefenderPlugin.class.getPackage().getSpecificationVersion();
    public static final String IMPLEMENTATION_NAME = GriefDefenderPlugin.class.getPackage().getImplementationTitle();
    public static final String IMPLEMENTATION_VERSION =  GriefDefenderPlugin.class.getPackage().getImplementationVersion() == null ? "unknown" : GriefDefenderPlugin.class.getPackage().getImplementationVersion();
    private Path configPath = Paths.get(".", "plugins", "GriefDefender");
    public FlagConfig flagConfig;
    public OptionConfig optionConfig;
    public MessageStorage messageStorage;
    public MessageDataConfig messageData;
    public Map<UUID, Random> worldGeneratorRandoms = new HashMap<>();
    public static ClaimBlockSystem CLAIM_BLOCK_SYSTEM;

    public static final String CONFIG_HEADER = IMPLEMENTATION_VERSION + "\n"
            + "# If you need help with the configuration or have any issues related to GriefDefender,\n"
            + "# create a ticket on https://github.com/bloodmc/GriefDefender/issues.\n"
            + "# Note: If you have not purchased GriefDefender, please consider doing so to get \n"
            + "# exclusive access to Discord for prompt support.\n";

    // GP Public user info
    public static final UUID PUBLIC_UUID = UUID.fromString("41C82C87-7AfB-4024-BA57-13D2C99CAE77");
    public static final UUID WORLD_USER_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    public static final UUID ADMIN_USER_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static GDPermissionUser PUBLIC_USER;
    public static GDPermissionUser WORLD_USER;
    public static final String PUBLIC_NAME = "[GDPublic]";
    public static final String WORLD_USER_NAME = "[GDWorld]";

    public static GDPermissionHolder DEFAULT_HOLDER;
    private PaperCommandManager commandManager;
    private static TimingManager timingManager;

    public BaseStorage dataStore;

    private DynmapProvider dynmapProvider;
    private EssentialsProvider essentialsProvider;
    private WorldEditProvider worldEditProvider;
    private WorldGuardProvider worldGuardProvider;
    private VaultProvider vaultProvider;
    private PermissionProvider permissionProvider;

    public Executor executor;

    public GDBlockType createVisualBlock;
    public GDItemType modificationTool;
    public GDItemType investigationTool;

    public static boolean debugLogging = false;
    public static boolean debugActive = false;
    private Map<String, GDDebugData> debugUserMap = new HashMap<>();
    public static final Component GD_TEXT = TextComponent.builder("").append("[").append("GD", TextColor.AQUA).append("] ").build();
    public static final List<String> ID_MAP = new ArrayList<>();
    public static List<Component> helpComponents = new ArrayList<>();

    public static GriefDefenderPlugin getInstance() {
        if (instance == null) {
            instance = new GriefDefenderPlugin();
        }
        return instance;
    }

    public Path getConfigPath() {
        return this.configPath;
    }

    public static void addEventLogEntry(Event event, Claim claim, Location location, String sourceId, String targetId, GDPermissionHolder permissionSubject, String permission, String trust, Tristate result, Set<Context> contexts) {
        final String eventName = event.getClass().getSimpleName().replace('$', '.').replace(".Impl", "");
        final String eventLocation = location == null ? "none" : VecHelper.toVector3i(location).toString();
        for (GDDebugData debugEntry : GriefDefenderPlugin.getInstance().getDebugUserMap().values()) {
            final CommandSender debugSource = debugEntry.getSource();
            final OfflinePlayer debugUser = debugEntry.getTarget();
            if (debugUser != null) {
                if (permissionSubject == null) {
                    continue;
                }
                // Check event source user
                if (!permissionSubject.getIdentifier().equals(debugUser.getUniqueId().toString())) {
                    continue;
                }
            }

            String messageUser = permissionSubject.getFriendlyName();
            if (permissionSubject instanceof GDPermissionUser) {
                messageUser = ((GDPermissionUser) permissionSubject).getName();
            }

            // record
            if (debugEntry.isRecording()) {
                permission = permission.replace("griefdefender.flag.", "");
                String messageFlag = permission;
                final Flag flag = FlagRegistryModule.getInstance().getById(permission).orElse(null);
                if (flag != null) {
                    messageFlag = flag.toString();
                }
                String messageSource = sourceId == null ? "none" : sourceId;
                String messageTarget = targetId == null ? "none" : targetId;
                if (messageTarget.endsWith(".0")) {
                    messageTarget = messageTarget.substring(0, messageTarget.length() - 2);
                }
                if (trust == null) {
                    trust = "none";
                }
                // Strip minecraft id on bukkit
                String[] parts = messageSource.split(":");
                if (parts.length > 1 && parts[0].equalsIgnoreCase("minecraft")) {
                    messageSource = parts[1];
                }
                parts = messageTarget.split(":");
                if (parts.length > 1 && parts[0].equalsIgnoreCase("minecraft")) {
                    messageTarget = parts[1];
                }
                String contextStr = "";
                final List<String> contextList = new ArrayList<>();
                for (Context context : contexts) {
                    contextList.add("<b>" + context.getKey() + "</b>=" + context.getValue());
                }
                final String serverName = PermissionUtil.getInstance().getServerName();
                if (serverName != null) {
                    contextList.add("<b>server</b>=" + serverName);
                } else {
                    contextList.add("<b>server</b>=global");
                }
                Collections.sort(contextList);
                for (String context : contextList) {
                    contextStr += context + "<br />";
                }

                String locationStr = "";
                locationStr += "<b>claim_uuid</b>=" + claim.getUniqueId() + "<br />";
                locationStr += "<b>claim_type</b>=" + claim.getType().getName().toLowerCase() + "<br />";
                locationStr += "<b>location</b>=" + eventLocation + "<br />";
                locationStr += "<b>world</b>=" + location.getWorld().getName().toLowerCase() + "<br />";

                String messageContexts = "<details>" + 
                        "  <summary><i>" + PlainComponentSerializer.INSTANCE.serialize(MessageCache.getInstance().DEBUG_CLICK_TO_EXPAND) + "</i></summary>" + 
                        contextStr +
                        "</details>";
                String messageLocation = "<details>" + 
                        "  <summary><i>" + PlainComponentSerializer.INSTANCE.serialize(MessageCache.getInstance().DEBUG_CLICK_TO_EXPAND) + "</i></summary>" + 
                        locationStr +
                        "</details>";
                debugEntry.addRecord(messageFlag, trust, messageSource, messageTarget, messageLocation, messageUser, messageContexts, result);
                continue;
            }

            final Component textEvent = TextComponent.builder("")
                    .append(GD_TEXT)
                    .append("Event: ", TextColor.GRAY)
                    .append(eventName == null ? TextComponent.of("Plugin").color(TextColor.GRAY) : TextComponent.of(eventName).color(TextColor.GRAY))
                    .append("\n").build();
            final Component textCause = TextComponent.builder("")
                    .append(GD_TEXT)
                    .append("Cause: ", TextColor.GRAY)
                    .append(sourceId, TextColor.LIGHT_PURPLE)
                    .append("\n").build();
            final Component textLocation = TextComponent.builder("")
                    .append(GD_TEXT)
                    .append("Location: ", TextColor.GRAY)
                    .append(eventLocation == null ? "NONE" : eventLocation).build();
            final Component textUser = TextComponent.builder("")
                    .append("User: ", TextColor.GRAY)
                    .append(messageUser, TextColor.GOLD)
                    .append("\n").build();
            final Component textLocationAndUser = TextComponent.builder("")
                    .append(textLocation)
                    .append(" ")
                    .append(textUser).build();
            Component textContext = null;
            Component textPermission = null;
            if (targetId != null) {
                textContext = TextComponent.builder("")
                        .append(GD_TEXT)
                        .append("Target: ", TextColor.GRAY)
                        .append(GDPermissionManager.getInstance().getPermissionIdentifier(targetId), TextColor.YELLOW)
                        .append("\n").build();
            }
            if (permission != null) {
                textPermission = TextComponent.builder("")
                        .append(GD_TEXT)
                        .append("Permission: ", TextColor.GRAY)
                        .append(permission, TextColor.RED)
                        .append("\n").build();
            }
            TextComponent.Builder textBuilder = TextComponent.builder("").append(textEvent);
            if (textContext != null) {
                textBuilder.append(textContext);
            } else {
                textBuilder.append(textCause);
            }
            if (textPermission != null) {
                textBuilder.append(textPermission);
            }
            textBuilder.append(textLocationAndUser);
            TextAdapter.sendComponent(debugSource, textBuilder.build());
        }
    }

    public void onEnable() {
        this.getLogger().info("GriefDefender boot start.");
        Plugin permissionPlugin = Bukkit.getPluginManager().getPlugin("LuckPerms");
        if (permissionPlugin != null) {
            this.permissionProvider = new LuckPermsProvider();
        } else {
            permissionPlugin = Bukkit.getPluginManager().getPlugin("PermissionsEx");
            if (permissionPlugin != null) {
                this.permissionProvider = PermissionsExProvider.initBukkit(permissionPlugin);
            }
        }
        instance = this;
        timingManager = TimingManager.of(GDBootstrap.getInstance());
        DEFAULT_HOLDER = new GDPermissionHolder("default");
        PUBLIC_USER = new GDPermissionUser(PUBLIC_UUID, PUBLIC_NAME);
        WORLD_USER = new GDPermissionUser(WORLD_USER_UUID, WORLD_USER_NAME);
        Guice.createInjector(Stage.PRODUCTION, new GriefDefenderImplModule());
        ChatTypeRegistryModule.getInstance().registerDefaults();
        ClaimTypeRegistryModule.getInstance().registerDefaults();
        ShovelTypeRegistryModule.getInstance().registerDefaults();
        TrustTypeRegistryModule.getInstance().registerDefaults();
        FlagRegistryModule.getInstance().registerDefaults();
        ResultTypeRegistryModule.getInstance().registerDefaults();
        EntityTypeRegistryModule.getInstance().registerDefaults();
        BlockTypeRegistryModule.getInstance().registerDefaults();
        ItemTypeRegistryModule.getInstance().registerDefaults();
        CreateModeTypeRegistryModule.getInstance().registerDefaults();
        GameModeTypeRegistryModule.getInstance().registerDefaults();
        WeatherTypeRegistryModule.getInstance().registerDefaults();
        OptionRegistryModule.getInstance().registerDefaults();
        GriefDefender.getRegistry().registerBuilderSupplier(BankTransaction.Builder.class, GDBankTransaction.BankTransactionBuilder::new);
        GriefDefender.getRegistry().registerBuilderSupplier(Claim.Builder.class, GDClaim.ClaimBuilder::new);
        GriefDefender.getRegistry().registerBuilderSupplier(FlagData.Builder.class, GDFlagData.FlagDataBuilder::new);
        GriefDefender.getRegistry().registerBuilderSupplier(FlagDefinition.Builder.class, GDFlagDefinition.FlagDefinitionBuilder::new);

        this.loadConfig();

        this.executor = Executors.newFixedThreadPool(GriefDefenderPlugin.getGlobalConfig().getConfig().thread.numExecutorThreads);

        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            this.vaultProvider = new VaultProvider();
            this.getLogger().info("Detected Vault. Checking for compatible economy plugin...");
            if (this.vaultProvider.getApi() != null) {
                this.getLogger().info("Found economy plugin '" + this.vaultProvider.getApi().getName() + "'. GriefDefender economy integration is now enabled.");
            } else {
                this.getLogger().info("Could not locate a compatible economy plugin for Vault. Please check with your server administrator.");
            }
            if (GriefDefenderPlugin.getGlobalConfig().getConfig().economy.economyMode) {
                this.getLogger().info("Economy mode enabled!. Claimblocks will be disabled...");
            }
        } else if (GriefDefenderPlugin.getGlobalConfig().getConfig().economy.economyMode) {
            this.getLogger().severe("No economy plugin found! Unable to initialize economy plugin.");
            return;
        }

        if (Bukkit.getPluginManager().getPlugin("WorldEdit") != null || Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit") != null) {
            this.worldEditProvider = new WorldEditProvider();
            GriefDefender.getRegistry().registerBuilderSupplier(ClaimSchematic.Builder.class, GDClaimSchematic.ClaimSchematicBuilder::new);
        }

        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            this.worldGuardProvider = new WorldGuardProvider();
        }

        if (Bukkit.getPluginManager().getPlugin("Essentials") != null) {
            this.essentialsProvider = new EssentialsProvider();
        }

        if (Bukkit.getPluginManager().getPlugin("dynmap") != null
                && GriefDefenderPlugin.getGlobalConfig().getConfig().dynmap.enabled) {
            this.dynmapProvider = new DynmapProvider();
        }

        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null){
            this.getLogger().info("Detected PlaceholderAPI. Enabling GD PlaceholderAPI expansion...");
            new PlaceholderProvider();
            this.getLogger().info("GriefDefender PlaceholderAPI expansion enabled!");
        }

        if (this.dataStore == null) {
            try {
                this.dataStore = new FileStorage();
                this.dataStore.initialize();
            } catch (Exception e) {
                this.getLogger().info("Unable to initialize file storage.");
                this.getLogger().info(e.getMessage());
                e.printStackTrace();
                return;
            }
        }

        Bukkit.getPluginManager().registerEvents(new BlockEventHandler(dataStore), GDBootstrap.getInstance());
        Bukkit.getPluginManager().registerEvents(new BlockEventTracker(), GDBootstrap.getInstance());
        Bukkit.getPluginManager().registerEvents(new CommandEventHandler(dataStore), GDBootstrap.getInstance());
        Bukkit.getPluginManager().registerEvents(new PlayerEventHandler(dataStore), GDBootstrap.getInstance());
        Bukkit.getPluginManager().registerEvents(new EntityEventHandler(dataStore), GDBootstrap.getInstance());
        Bukkit.getPluginManager().registerEvents(new WorldEventHandler(), GDBootstrap.getInstance());
        Bukkit.getPluginManager().registerEvents(new NMSUtil(), GDBootstrap.getInstance());

        // run cleanup task
        int cleanupTaskInterval = GriefDefenderPlugin.getGlobalConfig().getConfig().claim.expirationCleanupInterval;
        if (cleanupTaskInterval > 0) {
            new ClaimCleanupTask(cleanupTaskInterval);
        }


        final boolean resetMigration = GriefDefenderPlugin.getGlobalConfig().getConfig().playerdata.resetMigrations;
        final boolean resetClaimData = GriefDefenderPlugin.getGlobalConfig().getConfig().playerdata.resetAccruedClaimBlocks;
        final int migration2dRate = GriefDefenderPlugin.getGlobalConfig().getConfig().playerdata.migrateAreaRate;
        final int migration3dRate = GriefDefenderPlugin.getGlobalConfig().getConfig().playerdata.migrateVolumeRate;
        boolean migrate = false;
        if (resetMigration || resetClaimData || (migration2dRate > -1 && GriefDefenderPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.AREA)
                || (migration3dRate > -1 && GriefDefenderPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.VOLUME)) {
            migrate = true;
        }

        if (migrate) {
            List<GDPlayerData> playerDataList = new ArrayList<>();
            if (BaseStorage.USE_GLOBAL_PLAYER_STORAGE) {
                final GDClaimManager claimWorldManager = this.dataStore.getClaimWorldManager(Bukkit.getServer().getWorlds().get(0).getUID());
                claimWorldManager.resetPlayerData();
                playerDataList = new ArrayList<>(claimWorldManager.getPlayerDataMap().values());
                for (GDPlayerData playerData : playerDataList) {
                    if (Bukkit.getServer().getPlayer(playerData.playerID) != null && playerData.getClaims().isEmpty()) {
                        playerData.onDisconnect();
                        claimWorldManager.removePlayer(playerData.playerID);
                    }
                }
            }
            if (!BaseStorage.USE_GLOBAL_PLAYER_STORAGE) {
                for (World world : Bukkit.getServer().getWorlds()) {
                    final GDClaimManager claimWorldManager = this.dataStore.getClaimWorldManager(world.getUID());
                    playerDataList = new ArrayList<>(claimWorldManager.getPlayerDataMap().values());
                    for (GDPlayerData playerData : playerDataList) {
                        if (Bukkit.getServer().getPlayer(playerData.playerID) != null && playerData.getClaims().isEmpty()) {
                            playerData.onDisconnect();
                            claimWorldManager.removePlayer(playerData.playerID);
                        }
                    }
                }
            }
            GriefDefenderPlugin.getGlobalConfig().getConfig().playerdata.resetMigrations = false;
            GriefDefenderPlugin.getGlobalConfig().getConfig().playerdata.resetAccruedClaimBlocks = false;
            GriefDefenderPlugin.getGlobalConfig().getConfig().playerdata.migrateAreaRate = -1;
            GriefDefenderPlugin.getGlobalConfig().getConfig().playerdata.migrateVolumeRate = -1;
            GriefDefenderPlugin.getGlobalConfig().save();
        }

        new ClaimBlockTask();
        new PlayerTickTask();
        registerBaseCommands();
        this.getLogger().info("Loaded successfully.");
    }

    public void onDisable() {
        // Spigot disables plugins before calling world save on shutdown so we need to manually save here
        for (World world : Bukkit.getServer().getWorlds()) {
            if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUID())) {
                continue;
            }
    
            GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(world.getUID());
            if (claimWorldManager == null) {
                continue;
            }
    
            claimWorldManager.save();
            claimWorldManager.playerIndexStorage.savePlayerDatData();
        }
    }

    public void registerBaseCommands() {
        PaperCommandManager manager = new PaperCommandManager(GDBootstrap.getInstance());
        this.commandManager = manager;
        manager.getCommandReplacements().addReplacement("griefdefender", "gd|griefdefender");
        manager.registerCommand(new CommandAccessTrust());
        manager.registerCommand(new CommandAdjustBonusClaimBlocks());
        manager.registerCommand(new CommandCallback());
        manager.registerCommand(new CommandClaimAbandon());
        manager.registerCommand(new CommandClaimAbandonAll());
        manager.registerCommand(new CommandClaimAbandonTop());
        manager.registerCommand(new CommandClaimAdmin());
        manager.registerCommand(new CommandClaimBan());
        manager.registerCommand(new CommandClaimBank());
        manager.registerCommand(new CommandClaimBasic());
        manager.registerCommand(new CommandClaimBuy());
        manager.registerCommand(new CommandClaimBuyBlocks());
        manager.registerCommand(new CommandClaimClear());
        manager.registerCommand(new CommandClaimContract());
        manager.registerCommand(new CommandClaimCreate());
        manager.registerCommand(new CommandClaimCuboid());
        manager.registerCommand(new CommandClaimDelete());
        manager.registerCommand(new CommandClaimDeleteAll());
        manager.registerCommand(new CommandClaimDeleteAllAdmin());
        manager.registerCommand(new CommandClaimDeleteTop());
        manager.registerCommand(new CommandClaimExpand());
        manager.registerCommand(new CommandClaimFarewell());
        manager.registerCommand(new CommandClaimFlag());
        manager.registerCommand(new CommandClaimFlagDebug());
        manager.registerCommand(new CommandClaimFlagGroup());
        manager.registerCommand(new CommandClaimFlagPlayer());
        manager.registerCommand(new CommandClaimFlagReset());
        manager.registerCommand(new CommandClaimGreeting());
        manager.registerCommand(new CommandClaimIgnore());
        manager.registerCommand(new CommandClaimInfo());
        manager.registerCommand(new CommandClaimInherit());
        manager.registerCommand(new CommandClaimList());
        manager.registerCommand(new CommandClaimMode());
        manager.registerCommand(new CommandClaimName());
        manager.registerCommand(new CommandClaimOption());
        manager.registerCommand(new CommandClaimOptionGroup());
        manager.registerCommand(new CommandClaimOptionPlayer());
        manager.registerCommand(new CommandClaimPermissionGroup());
        manager.registerCommand(new CommandClaimPermissionPlayer());
        manager.registerCommand(new CommandClaimReserve());
        manager.registerCommand(new CommandClaimSchematic());
        manager.registerCommand(new CommandClaimSell());
        manager.registerCommand(new CommandClaimSellBlocks());
        manager.registerCommand(new CommandClaimSetSpawn());
        manager.registerCommand(new CommandClaimSpawn());
        manager.registerCommand(new CommandClaimSubdivision());
        manager.registerCommand(new CommandClaimTown());
        manager.registerCommand(new CommandClaimTransfer());
        manager.registerCommand(new CommandClaimUnban());
        manager.registerCommand(new CommandClaimWorldEdit());
        manager.registerCommand(new CommandContainerTrust());
        manager.registerCommand(new CommandDebug());
        manager.registerCommand(new CommandGDReload());
        manager.registerCommand(new CommandGDVersion());
        manager.registerCommand(new CommandGiveBlocks());
        manager.registerCommand(new CommandGivePet());
        manager.registerCommand(new CommandHelp());
        manager.registerCommand(new CommandPagination());
        manager.registerCommand(new CommandPlayerInfo());
        manager.registerCommand(new CommandRestoreClaim());
        manager.registerCommand(new CommandRestoreNature());
        manager.registerCommand(new CommandSetAccruedClaimBlocks());
        manager.registerCommand(new CommandTownChat());
        manager.registerCommand(new CommandTownTag());
        manager.registerCommand(new CommandTrustGroup());
        manager.registerCommand(new CommandTrustPlayer());
        manager.registerCommand(new CommandTrustGroupAll());
        manager.registerCommand(new CommandTrustPlayerAll());
        manager.registerCommand(new CommandUntrustGroup());
        manager.registerCommand(new CommandUntrustPlayer());
        manager.registerCommand(new CommandUntrustGroupAll());
        manager.registerCommand(new CommandUntrustPlayerAll());
        manager.registerCommand(new CommandTrustList());
        manager.enableUnstableAPI("help");

        final Map<String, Component> helpMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        // Generate help text
        RootCommand rootCommand = getCommandManager().getRootCommand("gd");
        for (BaseCommand child : rootCommand.getChildren()) {
            for (RegisteredCommand registeredCommand : child.getRegisteredCommands()) {
                if (helpMap.get(registeredCommand.getPrefSubCommand()) != null) {
                    continue;
                }
                TextComponent permissionText = TextComponent.builder("")
                        .append("Permission: ", TextColor.GOLD)
                        .append(registeredCommand.getRequiredPermissions() == null ? "None" : String.join(",", registeredCommand.getRequiredPermissions()), TextColor.GRAY)
                        .build();
    
                TextComponent argumentsText = TextComponent.builder("")
                        //.append("Arguments: ", TextColor.AQUA)
                        .append(registeredCommand.getSyntaxText() == null ? "Arguments: None" : registeredCommand.getSyntaxText(), TextColor.GREEN)
                        .build();
    
                final TextComponent hoverText = TextComponent.builder("")
                    .append("Command: ", TextColor.AQUA)
                    .append(registeredCommand.getPrefSubCommand() + "\n", TextColor.GREEN)
                    .append("Description: ", TextColor.AQUA)
                    .append(registeredCommand.getHelpText() + "\n", TextColor.GREEN)
                    .append("Arguments: ", TextColor.AQUA)
                    .append(argumentsText)
                    .append("\n")
                    .append(permissionText)
                    .build();
    
                final TextComponent commandText = TextComponent.builder("")
                        .append("/gd " + registeredCommand.getPrefSubCommand(), TextColor.GREEN)
                        .hoverEvent(HoverEvent.showText(hoverText))
                        .clickEvent(ClickEvent.suggestCommand("/gd " + registeredCommand.getPrefSubCommand()))
                        .build();
                helpMap.put(registeredCommand.getPrefSubCommand(), commandText);
            }
        }
        helpComponents = new ArrayList<>(helpMap.values());

        NMSUtil.getInstance().populateTabComplete();
        ID_MAP.add("any");
        ID_MAP.add(ContextGroupKeys.AMBIENT);
        ID_MAP.add(ContextGroupKeys.ANIMAL);
        ID_MAP.add(ContextGroupKeys.AQUATIC);
        ID_MAP.add(ContextGroupKeys.FOOD);
        ID_MAP.add(ContextGroupKeys.MONSTER);
        ID_MAP.add("#minecraft:ambient");
        ID_MAP.add("#minecraft:animal");
        ID_MAP.add("#minecraft:aquatic");
        ID_MAP.add("#minecraft:monster");
        ID_MAP.add("#minecraft:food");

        // commands
        ID_MAP.add("griefdefender:cf");
        ID_MAP.add("griefdefender:cfg");
        ID_MAP.add("griefdefender:cfp");
        ID_MAP.add("griefdefender:cog");
        ID_MAP.add("griefdefender:cop");
        ID_MAP.add("griefdefender:cpg");
        ID_MAP.add("griefdefender:cpp");
        ID_MAP.add("griefdefender:claimflag");
        ID_MAP.add("griefdefender:claimflaggroup");
        ID_MAP.add("griefdefender:claimflagplayer");

        // Add our callback command to spam exclusion list
        // This prevents players from being kicked if clicking callbacks too fast
        if (!org.spigotmc.SpigotConfig.spamExclusions.contains("/gd:callback")) {
            org.spigotmc.SpigotConfig.spamExclusions.add("/gd:callback");
        }
        manager.getCommandCompletions().registerCompletion("gdplayers", c -> {
            return ImmutableList.copyOf(PermissionUtil.getInstance().getAllLoadedPlayerNames());
        });
        manager.getCommandCompletions().registerCompletion("gdgroups", c -> {
            return ImmutableList.copyOf(PermissionUtil.getInstance().getAllLoadedGroupNames());
        });
        manager.getCommandCompletions().registerCompletion("gdbantypes", c -> {
            List<String> tabList = new ArrayList<>();
            tabList.add("block");
            tabList.add("entity");
            tabList.add("item");
            tabList.add("hand");
            return ImmutableList.copyOf(tabList);
        });
        manager.getCommandCompletions().registerCompletion("gdblockfaces", c -> {
            List<String> tabList = new ArrayList<>();
            tabList.add("north");
            tabList.add("east");
            tabList.add("south");
            tabList.add("west");
            tabList.add("up");
            tabList.add("down");
            tabList.add("all");
            return ImmutableList.copyOf(tabList);
        });
        manager.getCommandCompletions().registerCompletion("gdclaimtypes", c -> {
            List<String> tabList = new ArrayList<>();
            for (ClaimType type : ClaimTypeRegistryModule.getInstance().getAll()) {
                tabList.add(type.getName());
            }
            return ImmutableList.copyOf(tabList);
        });
        manager.getCommandCompletions().registerCompletion("gdtrusttypes", c -> {
            List<String> tabList = new ArrayList<>();
            for (TrustType type : TrustTypeRegistryModule.getInstance().getAll()) {
                tabList.add(type.getName());
            }
            return ImmutableList.copyOf(tabList);
        });
        manager.getCommandCompletions().registerCompletion("gdflags", c -> {
            List<String> tabList = new ArrayList<>();
            for (Flag type : FlagRegistryModule.getInstance().getAll()) {
                tabList.add(type.getName());
            }
            return ImmutableList.copyOf(tabList);
        });
        manager.getCommandCompletions().registerCompletion("gdoptions", c -> {
            List<String> tabList = new ArrayList<>();
            for (Option type : GriefDefender.getRegistry().getAllOf(Option.class)) {
                tabList.add(type.getName());
            }
            return ImmutableList.copyOf(tabList);
        });
        manager.getCommandCompletions().registerCompletion("gdentityids", c -> {
            List<String> tabList = new ArrayList<>();
            for (GDEntityType type : EntityTypeRegistryModule.getInstance().getAll()) {
                tabList.add(type.getName());
            }
            return ImmutableList.copyOf(tabList);
        });
        manager.getCommandCompletions().registerCompletion("gdmcids", c -> {
            List<String> tabList = new ArrayList<>();
            for (GDBlockType type : BlockTypeRegistryModule.getInstance().getAll()) {
                tabList.add(type.getName());
            }
            for (GDItemType type : ItemTypeRegistryModule.getInstance().getAll()) {
                tabList.add(type.getName());
            }
            for (GDEntityType type : EntityTypeRegistryModule.getInstance().getAll()) {
                tabList.add(type.getName());
            }
            for (InventoryType type : InventoryType.values()) {
                tabList.add(type.name().toLowerCase());
            }
            return ImmutableList.copyOf(tabList);
        });
        manager.getCommandCompletions().registerCompletion("gdtristates", c -> {
            return ImmutableList.of("true", "false", "undefined");
        });
        manager.getCommandCompletions().registerCompletion("gdcontexts", c -> {
            return ImmutableList.of("context[<override|default|used_item|source|world|server|player|group>]");
        });
        manager.getCommandCompletions().registerCompletion("gdworlds", c -> {
            List<String> tabList = new ArrayList<>();
            for (World world : Bukkit.getServer().getWorlds()) {
                tabList.add(world.getName().toLowerCase());
            }
            return ImmutableList.copyOf(tabList);
        });
        manager.getCommandCompletions().registerCompletion("gddummy", c -> {
            return ImmutableList.of();
        });
    }

    public PaperCommandManager getCommandManager() {
        return this.commandManager;
    }

    public void loadConfig() {
        this.getLogger().info("Loading configuration...");
        try {
            TypeSerializers.getDefaultSerializers().registerType(TypeToken.of(Component.class), new ComponentConfigSerializer());
            TypeSerializers.getDefaultSerializers().registerType(TypeToken.of(ClaimType.class), new ClaimTypeSerializer());
            TypeSerializers.getDefaultSerializers().registerType(TypeToken.of(CreateModeType.class), new CreateModeTypeSerializer());
            TypeSerializers.getDefaultSerializers().registerType(TypeToken.of(FlagDefinition.class), new FlagDefinitionSerializer());
            TypeSerializers.getDefaultSerializers().registerType(TypeToken.of(GameModeType.class), new GameModeTypeSerializer());
            TypeSerializers.getDefaultSerializers().registerType(TypeToken.of(WeatherType.class), new WeatherTypeSerializer());

            if (Files.notExists(BaseStorage.dataLayerFolderPath)) {
                Files.createDirectories(BaseStorage.dataLayerFolderPath);
            }

            Path rootConfigPath = this.getConfigPath().resolve("worlds");
            BaseStorage.globalConfig = new GriefDefenderConfig<>(GlobalConfig.class, this.getConfigPath().resolve("global.conf"), null);
            String localeString = BaseStorage.globalConfig.getConfig().message.locale;
            try {
                LocaleUtils.toLocale(localeString);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                this.getLogger().severe("Could not validate the locale '" + localeString + "'. Defaulting to 'en_US'...");
                localeString = "en_US";
            }
            final Path localePath = this.getConfigPath().resolve("lang").resolve(localeString + ".conf");
            if (!localePath.toFile().exists()) {
                // Check for a default locale asset and copy to lang folder
                try {
                    final InputStream in = getClass().getResourceAsStream("/assets/lang/" + localeString + ".conf");
                    FileUtils.copyInputStreamToFile(in, localePath.toFile());
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
            messageStorage = new MessageStorage(localePath);
            messageData = messageStorage.getConfig();
            MessageCache.getInstance().loadCache();
            flagConfig = new FlagConfig(this.getConfigPath().resolve("flags.conf"));
            // FlagDefinition registry needs to init after config load
            FlagDefinitionRegistryModule.getInstance().registerDefaults();
            flagConfig.getConfig().customFlags.initDefaults();
            flagConfig.save();
            flagConfig.getConfig().defaultFlagCategory.refreshFlags();
            flagConfig.save();
            optionConfig = new OptionConfig(this.getConfigPath().resolve("options.conf"));
            optionConfig.getConfig().defaultOptionCategory.checkOptions();
            optionConfig.save();
            BaseStorage.globalConfig.save();
            BaseStorage.USE_GLOBAL_PLAYER_STORAGE = !BaseStorage.globalConfig.getConfig().playerdata.useWorldPlayerData();
            GDFlags.populateFlagStatus();
            PermissionHolderCache.getInstance().getOrCreatePermissionCache(GriefDefenderPlugin.DEFAULT_HOLDER).invalidateAll();
            CLAIM_BLOCK_SYSTEM = BaseStorage.globalConfig.getConfig().playerdata.claimBlockSystem;
            final GDBlockType defaultCreateVisualBlock = BlockTypeRegistryModule.getInstance().getById("minecraft:diamond_block").orElse(null);
            this.createVisualBlock = BlockTypeRegistryModule.getInstance().getById(BaseStorage.globalConfig.getConfig().visual.claimCreateStartBlock).orElse(defaultCreateVisualBlock);
            this.modificationTool  = ItemTypeRegistryModule.getInstance().getById(BaseStorage.globalConfig.getConfig().claim.modificationTool).orElse(null);
            this.investigationTool = ItemTypeRegistryModule.getInstance().getById(BaseStorage.globalConfig.getConfig().claim.investigationTool).orElse(null);
            if (this.dataStore != null) {
                for (World world : Bukkit.getServer().getWorlds()) {
                    final String dimType = world.getEnvironment().name().toLowerCase();
                    final Path dimPath = rootConfigPath.resolve(dimType);
                    if (Files.notExists(dimPath.resolve(world.getName()))) {
                        try {
                            Files.createDirectories(rootConfigPath.resolve(dimType).resolve(world.getName()));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
    
                    GriefDefenderConfig<ConfigBase> dimConfig = new GriefDefenderConfig<>(ConfigBase.class, dimPath.resolve("dimension.conf"), BaseStorage.globalConfig);
                    GriefDefenderConfig<ConfigBase> worldConfig = new GriefDefenderConfig<>(ConfigBase.class, dimPath.resolve(world.getName()).resolve("world.conf"), dimConfig);
    
                    BaseStorage.dimensionConfigMap.put(world.getUID(), dimConfig);
                    BaseStorage.worldConfigMap.put(world.getUID(), worldConfig);
    
                    // refresh player data
                    final GDClaimManager claimManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(world.getUID());
                    for (GDPlayerData playerData : claimManager.getPlayerDataMap().values()) {
                        if (playerData.playerID.equals(WORLD_USER_UUID) || playerData.playerID.equals(ADMIN_USER_UUID) || playerData.playerID.equals(PUBLIC_UUID)) {
                            continue;
                        }
                        playerData.refreshPlayerOptions();
                    }

                    if (GriefDefenderPlugin.getGlobalConfig().getConfig().migrator.classicMigrator) {
                        GriefDefenderPlugin.getGlobalConfig().getConfig().migrator.classicMigrator = false;
                        GriefDefenderPlugin.getGlobalConfig().save();
                    }
                    if (this.worldEditProvider != null) {
                        this.getLogger().info("Loading schematics for world " + world.getName() + "...");
                        this.worldEditProvider.loadSchematics(world);
                    }
                }
                // refresh default permissions
                this.dataStore.setDefaultGlobalPermissions();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendClaimDenyMessage(GDClaim claim, CommandSender source, Component message) {
        if (claim.getData() != null && !claim.getData().allowDenyMessages()) {
            return;
        }

        sendMessage(source, message);
    }

    public static void sendMessage(CommandSender source, Component message) {
        if (message == TextComponent.empty() || message == null) {
            return;
        }

        if (source == null) {
            GriefDefenderPlugin.getInstance().getLogger().warning(PlainComponentSerializer.INSTANCE.serialize(message));
        } else {
            TextAdapter.sendComponent(source, message);
        }
    }

    public static GriefDefenderConfig<?> getActiveConfig(World world) {
        return getActiveConfig(world.getUID());
    }

    public static GriefDefenderConfig<? extends ConfigBase> getActiveConfig(UUID worldUniqueId) {
        GriefDefenderConfig<ConfigBase> config = BaseStorage.worldConfigMap.get(worldUniqueId);
        if (config != null) {
            return config;
        }

        config = BaseStorage.dimensionConfigMap.get(worldUniqueId);
        if (config != null) {
            return config;
        }

        return BaseStorage.globalConfig;
    }

    public static GriefDefenderConfig<GlobalConfig> getGlobalConfig() {
        return BaseStorage.globalConfig;
    }

    public boolean claimsEnabledForWorld(UUID worldUniqueId) {
        return GriefDefenderPlugin.getActiveConfig(worldUniqueId).getConfig().claim.claimsEnabled != 0;
    }

    public int getSeaLevel(World world) {
        return world.getSeaLevel();
    }

    public Map<String, GDDebugData> getDebugUserMap() {
        return this.debugUserMap;
    }

    public static boolean isEntityProtected(Entity entity) {
        // ignore monsters
        if (entity instanceof Monster) {
            return false;
        }

        return true;
    }

    public static GDPermissionUser getOrCreateUser(UUID uuid) {
        if (uuid == null) {
            return null;
        }

        if (uuid == PUBLIC_UUID) {
            return PUBLIC_USER;
        }
        if (uuid == WORLD_USER_UUID) {
            return WORLD_USER;
        }

        // check the cache
        return PermissionHolderCache.getInstance().getOrCreateUser(uuid);
    }

    public static boolean isSourceIdBlacklisted(String flag, Object source, UUID worldUniqueId) {
        final List<String> flagList = GriefDefenderPlugin.getGlobalConfig().getConfig().blacklist.flagIdBlacklist.get(flag);
        final boolean checkFlag = flagList != null && !flagList.isEmpty();
        final boolean checkGlobal = !GriefDefenderPlugin.getGlobalConfig().getConfig().blacklist.globalSourceBlacklist.isEmpty();
        if (!checkFlag && !checkGlobal) {
            return false;
        }

        final GriefDefenderConfig<?> activeConfig = GriefDefenderPlugin.getActiveConfig(worldUniqueId);
        final String id = GDPermissionManager.getInstance().getPermissionIdentifier(source);
        final String idNoMeta = GDPermissionManager.getInstance().getIdentifierWithoutMeta(id);

        // Check global
        if (checkGlobal) {
            final BlacklistCategory blacklistCategory = activeConfig.getConfig().blacklist;
            final List<String> globalSourceBlacklist = blacklistCategory.getGlobalSourceBlacklist();
            if (globalSourceBlacklist == null) {
                return false;
            }
            for (String str : globalSourceBlacklist) {
                if (FilenameUtils.wildcardMatch(id, str)) {
                    return true;
                }
                if (FilenameUtils.wildcardMatch(idNoMeta, str)) {
                    return true;
                }
            }
        }
        // Check flag
        if (checkFlag) {
            for (String str : flagList) {
                if (FilenameUtils.wildcardMatch(id, str)) {
                    return true;
                }
                if (FilenameUtils.wildcardMatch(idNoMeta, str)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean isTargetIdBlacklisted(String flag, Object target, UUID worldUniqueId) {
        final List<String> flagList = GriefDefenderPlugin.getGlobalConfig().getConfig().blacklist.flagIdBlacklist.get(flag);
        final boolean checkFlag = flagList != null && !flagList.isEmpty();
        final boolean checkGlobal = !GriefDefenderPlugin.getGlobalConfig().getConfig().blacklist.globalTargetBlacklist.isEmpty();
        if (!checkFlag && !checkGlobal) {
            return false;
        }

        final GriefDefenderConfig<?> activeConfig = GriefDefenderPlugin.getActiveConfig(worldUniqueId);
        final String id = GDPermissionManager.getInstance().getPermissionIdentifier(target);
        final String idNoMeta = GDPermissionManager.getInstance().getIdentifierWithoutMeta(id);

        // Check global
        if (checkGlobal) {
            final BlacklistCategory blacklistCategory = activeConfig.getConfig().blacklist;
            final List<String> globalTargetBlacklist = blacklistCategory.getGlobalTargetBlacklist();
            if (globalTargetBlacklist == null) {
                return false;
            }
            for (String str : globalTargetBlacklist) {
                if (FilenameUtils.wildcardMatch(id, str)) {
                    return true;
                }
                if (FilenameUtils.wildcardMatch(idNoMeta, str)) {
                    return true;
                }
            }
        }
        // Check flag
        if (checkFlag) {
            for (String str : flagList) {
                if (FilenameUtils.wildcardMatch(id, str)) {
                    return true;
                }
                if (FilenameUtils.wildcardMatch(idNoMeta, str)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean isEconomyModeEnabled() {
        boolean vaultApi = this.getVaultProvider() != null && this.getVaultProvider().getApi() != null;
        if (vaultApi && GriefDefenderPlugin.getGlobalConfig().getConfig().economy.economyMode) {
            return true;
        }

        return false;
    }

    public DynmapProvider getDynmapProvider() {
        return this.dynmapProvider;
    }

    public EssentialsProvider getEssentialsProvider() {
        return this.essentialsProvider;
    }

    public WorldEditProvider getWorldEditProvider() {
        return this.worldEditProvider;
    }

    public WorldGuardProvider getWorldGuardProvider() {
        return this.worldGuardProvider;
    }

    public VaultProvider getVaultProvider() {
        return this.vaultProvider;
    }

    public Logger getLogger() {
        return GDBootstrap.getInstance().getLogger();
    }

    public PermissionProvider getPermissionProvider() {
        return this.permissionProvider;
    }

    public static int getMajorMinecraftVersion() {
        final String version = Bukkit.getVersion();
        if (version.contains("1.8.8")) {
            return 8;
        } else if (version.contains("1.12")) {
            return 12;
        } else if (version.contains("1.13")) {
            return 13;
        } else if (version.contains("1.14")) {
            return 14;
        } else if (version.contains("1.15")) {
            return 15;
        }

        return -1;
    }

    public static MCTiming timing(String name) {
        return timingManager.of(name);
    }
}
