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
package com.griefdefender.listener;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GDTimings;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimBlockSystem;
import com.griefdefender.api.claim.ClaimResult;
import com.griefdefender.api.claim.ClaimResultType;
import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.api.claim.ClaimTypes;
import com.griefdefender.api.claim.ShovelTypes;
import com.griefdefender.api.claim.TrustType;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.api.permission.flag.Flag;
import com.griefdefender.api.permission.flag.Flags;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.api.permission.option.type.CreateModeTypes;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.claim.GDClaimManager;
import com.griefdefender.command.CommandHelper;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.internal.provider.WorldEditProvider;
import com.griefdefender.internal.provider.WorldGuardProvider;
import com.griefdefender.internal.registry.BlockTypeRegistryModule;
import com.griefdefender.internal.registry.ItemTypeRegistryModule;
import com.griefdefender.internal.util.BlockUtil;
import com.griefdefender.internal.util.NMSUtil;
import com.griefdefender.internal.util.VecHelper;
import com.griefdefender.internal.visual.ClaimVisual;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.permission.flag.GDFlags;
import com.griefdefender.provider.VaultProvider;
import com.griefdefender.storage.BaseStorage;
import com.griefdefender.util.BlockRay;
import com.griefdefender.util.BlockRayHit;
import com.griefdefender.util.EconomyUtil;
import com.griefdefender.util.PaginationUtil;
import com.griefdefender.util.PlayerUtil;

import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.format.TextColor;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerEventHandler implements Listener {

    private final BaseStorage dataStore;
    private final WorldEditProvider worldEditProvider;
    private boolean lastInteractItemCancelled = false;

    public PlayerEventHandler(BaseStorage dataStore) {
        this.dataStore = dataStore;
        this.worldEditProvider = GriefDefenderPlugin.getInstance().getWorldEditProvider();
       // this.banService = Sponge.getServiceManager().getRegistration(BanService.class).get().getProvider();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        GDTimings.PLAYER_LOGIN_EVENT.startTiming();
        final Player player = event.getPlayer();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUID())) {
            GDTimings.PLAYER_LOGIN_EVENT.stopTiming();
            return;
        }

        final UUID worldUniqueId = event.getPlayer().getWorld().getUID();
        final UUID playerUniqueId = player.getUniqueId();
        final GDClaimManager claimWorldManager = this.dataStore.getClaimWorldManager(worldUniqueId);
        final Instant dateNow = Instant.now();
        for (Claim claim : claimWorldManager.getWorldClaims()) {
            if (claim.getType() != ClaimTypes.ADMIN && claim.getOwnerUniqueId().equals(playerUniqueId)) {
                claim.getData().setDateLastActive(dateNow);
                for (Claim subdivision : ((GDClaim) claim).children) {
                    subdivision.getData().setDateLastActive(dateNow);
                }
                ((GDClaim) claim).getInternalClaimData().setRequiresSave(true);
            }
        }
        GDTimings.PLAYER_LOGIN_EVENT.stopTiming();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        GDTimings.PLAYER_JOIN_EVENT.startTiming();
        Player player = event.getPlayer();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUID())) {
            GDTimings.PLAYER_JOIN_EVENT.stopTiming();
            return;
        }

        UUID playerID = player.getUniqueId();
        final GDPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), playerID);

        final GDClaim claim = this.dataStore.getClaimAtPlayer(playerData, player.getLocation());
        if (claim.isInTown()) {
            playerData.inTown = true;
        }

        GDTimings.PLAYER_JOIN_EVENT.stopTiming();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUID())) {
            return;
        }

        GDTimings.PLAYER_QUIT_EVENT.startTiming();
        UUID playerID = player.getUniqueId();
        GDPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), playerID);

        if (this.worldEditProvider != null) {
            this.worldEditProvider.revertVisuals(player, playerData, null);
            //this.worldEditProvider.removePlayer(player);
        }

        playerData.onDisconnect();
        PaginationUtil.getInstance().removeActivePageData(player.getUniqueId());
        if (playerData.getClaims().isEmpty()) {
            this.dataStore.clearCachedPlayerData(player.getWorld().getUID(), playerID);
        }

        GDTimings.PLAYER_QUIT_EVENT.stopTiming();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUID())) {
            return;
        }

        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        playerData.lastPvpTimestamp = null;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        final Player player = event.getEntity();
        GDCauseStackManager.getInstance().pushCause(player);
        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        final GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(playerData, player.getLocation());
        final Tristate keepInventory = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Tristate.class), playerData.getSubject(), Options.PLAYER_KEEP_INVENTORY, claim);
        final Tristate keepLevel = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Tristate.class), playerData.getSubject(), Options.PLAYER_KEEP_LEVEL, claim);
        if (keepInventory != Tristate.UNDEFINED) {
            event.setKeepInventory(keepInventory.asBoolean());
            if (keepInventory == Tristate.TRUE) {
                event.getDrops().clear();
            }
        }
        if (keepLevel != Tristate.UNDEFINED) {
            event.setKeepLevel(keepLevel.asBoolean());
            if (keepLevel == Tristate.TRUE) {
                event.setDroppedExp(0);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerFoodLevelChange(FoodLevelChangeEvent event) {
        final Player player = event.getEntity() instanceof Player ? (Player) event.getEntity() : null;
        if (player == null) {
            return;
        }

        GDCauseStackManager.getInstance().pushCause(player);
        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        final GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(playerData, player.getLocation());
        final Boolean denyHunger = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Boolean.class), playerData.getSubject(), Options.PLAYER_DENY_HUNGER, claim);
        if (denyHunger != null && denyHunger) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChangeHeldItem(PlayerItemHeldEvent event) {
        final Player player = event.getPlayer();
        GDCauseStackManager.getInstance().pushCause(player);
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUID())) {
            return;
        }

        GDTimings.PLAYER_CHANGE_HELD_ITEM_EVENT.startTiming();
        GDPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());

        int newSlot = event.getNewSlot();
        ItemStack newItemStack = player.getInventory().getItem(newSlot);
        if(newItemStack != null && GriefDefenderPlugin.getInstance().modificationTool != null && NMSUtil.getInstance().itemsEqual(newItemStack, GriefDefenderPlugin.getInstance().modificationTool)) {
            playerData.lastShovelLocation = null;
            playerData.endShovelLocation = null;
            playerData.claimResizing = null;
            // always reset to basic claims mode
            if (playerData.shovelMode != ShovelTypes.BASIC) {
                playerData.shovelMode = ShovelTypes.BASIC;
                GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().MODE_BASIC);
            }

            if (GriefDefenderPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.VOLUME) {
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PLAYER_REMAINING_BLOCKS_3D,
                        ImmutableMap.of(
                        "block-amount", playerData.getRemainingClaimBlocks(),
                        "chunk-amount", playerData.getRemainingChunks()));
                GriefDefenderPlugin.sendMessage(player, message);
            } else {
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PLAYER_REMAINING_BLOCKS_2D,
                       ImmutableMap.of(
                        "block-amount", playerData.getRemainingClaimBlocks()));
                GriefDefenderPlugin.sendMessage(player, message);
            }
        }
        GDTimings.PLAYER_CHANGE_HELD_ITEM_EVENT.stopTiming();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        final Player player = event.getPlayer();
        GDCauseStackManager.getInstance().pushCause(player);
        if (!GDFlags.ITEM_DROP) {
            return;
        }

        final World world = event.getPlayer().getWorld();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUID())) {
            return;
        }
        if (GriefDefenderPlugin.isSourceIdBlacklisted(Flags.ITEM_DROP.getName(), player, world.getUID())) {
            return;
        }

        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.ITEM_DROP.getName(), event.getItemDrop(), world.getUID())) {
            return;
        }

        final Location location = event.getItemDrop().getLocation();
        final GDClaim targetClaim = GriefDefenderPlugin.getInstance().dataStore.getClaimAt(location);

        if (GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, Flags.ITEM_DROP, player, event.getItemDrop(), player, TrustTypes.ACCESSOR, true) == Tristate.FALSE) {
            event.setCancelled(true);
        }
    }

    // Older MC versions do not have EntityPickupItemEvent so keep this in common
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        if (!GDFlags.ITEM_PICKUP) {
            return;
        }

        final Player player = event.getPlayer();
        GDCauseStackManager.getInstance().pushCause(player);
        final World world = player.getWorld();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUID())) {
            return;
        }
        if (GriefDefenderPlugin.isSourceIdBlacklisted(Flags.ITEM_PICKUP.getName(), player, world.getUID())) {
            return;
        }

        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.ITEM_PICKUP.getName(), event.getItem(), world.getUID())) {
            return;
        }

        final Location location = event.getItem().getLocation();
        final GDClaim targetClaim = GriefDefenderPlugin.getInstance().dataStore.getClaimAt(location);
        if (GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, Flags.ITEM_PICKUP, player, event.getItem(), player, TrustTypes.ACCESSOR, true) == Tristate.FALSE) {
            event.setCancelled(true);
        }
    }

    private void onInventoryOpen(Event event, Location location, Object target, HumanEntity player) {
        GDCauseStackManager.getInstance().pushCause(player);
        if (event instanceof InventoryOpenEvent) {
            final InventoryOpenEvent inventoryEvent = (InventoryOpenEvent) event;
            target = inventoryEvent.getView().getType();
        }
        if (!GDFlags.INTERACT_INVENTORY || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUID())) {
            return;
        }

        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.INTERACT_INVENTORY.getName(), target, player.getWorld().getUID())) {
            return;
        }

        String targetId = GDPermissionManager.getInstance().getPermissionIdentifier(target);
        GDTimings.PLAYER_INTERACT_INVENTORY_OPEN_EVENT.startTiming();
        final GDClaim claim = this.dataStore.getClaimAt(location);
        final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(player.getUniqueId());
        if (user.getInternalPlayerData() != null && user.getInternalPlayerData().eventResultCache != null && user.getInternalPlayerData().eventResultCache.checkEventResultCache(claim, Flags.INTERACT_BLOCK_SECONDARY.getName()) == Tristate.TRUE) {
            GDPermissionManager.getInstance().processResult(claim, Flags.INTERACT_INVENTORY.getPermission(), "cache", Tristate.TRUE, user);
            GDTimings.PLAYER_INTERACT_INVENTORY_OPEN_EVENT.stopTiming();
            return;
        }
        final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.INTERACT_INVENTORY, player, target, user, TrustTypes.CONTAINER, true);
        if (result == Tristate.FALSE) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_INVENTORY_OPEN,
                    ImmutableMap.of(
                    "player", claim.getOwnerName(),
                    "block", targetId));
            GriefDefenderPlugin.sendClaimDenyMessage(claim, player, message);
            ((Cancellable) event).setCancelled(true);
        }

        GDTimings.PLAYER_INTERACT_INVENTORY_OPEN_EVENT.stopTiming();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractInventoryClick(InventoryClickEvent event) {
        final HumanEntity player = event.getWhoClicked();
        GDCauseStackManager.getInstance().pushCause(player);
        if (!GDFlags.INTERACT_INVENTORY_CLICK || event.getClickedInventory() == null || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUID())) {
            return;
        }

        GDTimings.PLAYER_INTERACT_INVENTORY_CLICK_EVENT.startTiming();
        final Location location = player.getLocation();
        final GDClaim claim = this.dataStore.getClaimAt(location);
        final ItemStack cursorItem = event.getCursor();
        final Inventory source = event.getInventory();
        final ItemStack target = event.getCurrentItem();

        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.INTERACT_INVENTORY_CLICK.getName(), target, player.getWorld().getUID())) {
            GDTimings.PLAYER_INTERACT_INVENTORY_CLICK_EVENT.stopTiming();
            return;
        }

        final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(player.getUniqueId());
        final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.INTERACT_INVENTORY_CLICK, source, target, user, TrustTypes.CONTAINER, true);
        if (result == Tristate.FALSE) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_INTERACT_ITEM,
                    ImmutableMap.of(
                    "player", claim.getOwnerName(),
                    "item", ItemTypeRegistryModule.getInstance().getNMSKey(target)));
            GriefDefenderPlugin.sendClaimDenyMessage(claim, player, message);
            event.setCancelled(true);
        }
        GDTimings.PLAYER_INTERACT_INVENTORY_CLICK_EVENT.stopTiming();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerConsumeItem(PlayerItemConsumeEvent event) {
        final Player player = event.getPlayer();
        final ItemStack itemInUse = event.getItem();
        GDCauseStackManager.getInstance().pushCause(player);
        if (!GDFlags.ITEM_USE || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUID())) {
            return;
        }
        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.ITEM_USE.toString(), itemInUse, player.getWorld().getUID())) {
            return;
        }

        GDTimings.PLAYER_USE_ITEM_EVENT.startTiming();
        Location location = player.getLocation();
        GDPlayerData playerData = this.dataStore.getOrCreatePlayerData(location.getWorld(), player.getUniqueId());
        GDClaim claim = this.dataStore.getClaimAtPlayer(playerData, location);

        final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.ITEM_USE, player, itemInUse, player, TrustTypes.ACCESSOR, true);
        if (result == Tristate.FALSE) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_ITEM_USE,
                    ImmutableMap.of("item", ItemTypeRegistryModule.getInstance().getNMSKey(itemInUse)));
            GriefDefenderPlugin.sendClaimDenyMessage(claim, player,  message);
            event.setCancelled(true);
        }
        GDTimings.PLAYER_USE_ITEM_EVENT.stopTiming();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractItem(PlayerInteractEvent event) {
        final World world = event.getPlayer().getWorld();
        final Block clickedBlock = event.getClickedBlock();
        final ItemStack itemInHand = event.getItem();
        final Player player = event.getPlayer();
        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        GDCauseStackManager.getInstance().pushCause(player);
        if (!playerData.claimMode && (itemInHand == null || itemInHand.getType().isEdible())) {
            return;
        }

        if ((!GDFlags.INTERACT_ITEM_PRIMARY && !GDFlags.INTERACT_ITEM_SECONDARY) || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUID())) {
            return;
        }

        if (playerData.claimMode || (GriefDefenderPlugin.getInstance().modificationTool != null && NMSUtil.getInstance().itemsEqual(itemInHand, GriefDefenderPlugin.getInstance().modificationTool) ||
                GriefDefenderPlugin.getInstance().investigationTool != null && NMSUtil.getInstance().itemsEqual(itemInHand, GriefDefenderPlugin.getInstance().investigationTool))) {
            investigateClaim(event, player, clickedBlock, itemInHand);
            event.setCancelled(true);
            return;
        }

        final boolean itemPrimaryBlacklisted = GriefDefenderPlugin.isTargetIdBlacklisted(Flags.INTERACT_ITEM_PRIMARY.getName(), itemInHand, world.getUID());
        final boolean itemSecondaryBlacklisted = GriefDefenderPlugin.isTargetIdBlacklisted(Flags.INTERACT_ITEM_SECONDARY.getName(), itemInHand, world.getUID());
        if (itemPrimaryBlacklisted && itemSecondaryBlacklisted) {
            return;
        }

        final boolean primaryEvent = event.getAction() == Action.LEFT_CLICK_AIR ? true : false;
        final Location location = clickedBlock == null ? event.getPlayer().getLocation() : clickedBlock.getLocation();

        final GDClaim claim = this.dataStore.getClaimAt(location);
        final Flag flag = primaryEvent ? Flags.INTERACT_ITEM_PRIMARY : Flags.INTERACT_ITEM_SECONDARY;
        if ((itemPrimaryBlacklisted && flag == Flags.INTERACT_ITEM_PRIMARY) || (itemSecondaryBlacklisted && flag == Flags.INTERACT_ITEM_SECONDARY)) {
            return;
        }

        if (GDPermissionManager.getInstance().getFinalPermission(event, location, claim, flag, player, itemInHand, player, TrustTypes.ACCESSOR, true) == Tristate.FALSE) {
            Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_INTERACT_ITEM,
                    ImmutableMap.of(
                    "player", claim.getOwnerName(),
                    "item", ItemTypeRegistryModule.getInstance().getNMSKey(itemInHand)));
            GriefDefenderPlugin.sendClaimDenyMessage(claim, player, message);
            event.setCancelled(true);
            lastInteractItemCancelled = true;
            return;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerArmorStandManipulateEvent(PlayerArmorStandManipulateEvent event) {
        onPlayerInteractEntity(event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        final Player player = event.getPlayer();
        final World world = player.getWorld();
        GDCauseStackManager.getInstance().pushCause(player);
        if (!GDFlags.INTERACT_ENTITY_SECONDARY || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUID())) {
            return;
        }

        final Entity targetEntity = event.getRightClicked();
        final Location location = targetEntity.getLocation();
        final ItemStack activeItem = NMSUtil.getInstance().getActiveItem(player, event);
        final GDPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());

        if (targetEntity instanceof Tameable) {
            if (playerData.petRecipientUniqueId != null) {
                final Tameable tameableEntity = (Tameable) targetEntity;
                final GDPermissionUser recipientUser = PermissionHolderCache.getInstance().getOrCreateUser(playerData.petRecipientUniqueId);
                tameableEntity.setOwner(recipientUser.getOfflinePlayer());
                playerData.petRecipientUniqueId = null;
                TextAdapter.sendComponent(player, MessageCache.getInstance().COMMAND_PET_CONFIRMATION);
                event.setCancelled(true);
                return;
            }

            final UUID uuid = NMSUtil.getInstance().getTameableOwnerUUID(targetEntity);
            if (uuid != null) {
                // always allow owner to interact with their pets
                if (player.getUniqueId().equals(uuid)) {
                    return;
                }
                // If pet protection is enabled, deny the interaction
                if (GriefDefenderPlugin.getActiveConfig(player.getWorld().getUID()).getConfig().claim.protectTamedEntities) {
                    final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(uuid);
                    final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_PROTECTED_ENTITY,
                            ImmutableMap.of(
                            "player", user.getName()));
                    GriefDefenderPlugin.sendMessage(player, message);
                    event.setCancelled(true);
                    return;
                }
            }
        }

        if (activeItem != null && activeItem.getType() != Material.AIR) {
            // handle item usage
            if (!GDFlags.INTERACT_ITEM_SECONDARY || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUID())) {
                return;
            }

            if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.INTERACT_ITEM_SECONDARY.getName(), activeItem, world.getUID())) {
                return;
            }

            final GDClaim claim = this.dataStore.getClaimAt(location);
            if (GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.INTERACT_ITEM_SECONDARY, player, activeItem, player, TrustTypes.ACCESSOR, true) == Tristate.FALSE) {
                Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_INTERACT_ITEM,
                        ImmutableMap.of(
                        "player", claim.getOwnerName(),
                        "item", ItemTypeRegistryModule.getInstance().getNMSKey(activeItem)));
                GriefDefenderPlugin.sendClaimDenyMessage(claim, player, message);
                event.setCancelled(true);
                lastInteractItemCancelled = true;
                return;
            }
        }

        // Item permission checks passed, check entity
        final Object source = activeItem != null && activeItem.getType() != Material.AIR ? activeItem : player;
        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.INTERACT_ENTITY_SECONDARY.getName(), targetEntity, player.getWorld().getUID())) {
            return;
        }

        GDTimings.PLAYER_INTERACT_ENTITY_SECONDARY_EVENT.startTiming();
        final GDClaim claim = this.dataStore.getClaimAt(location);

        Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.INTERACT_ENTITY_SECONDARY, source, targetEntity, player, TrustTypes.ACCESSOR, true);
        if (result == Tristate.TRUE && targetEntity instanceof ArmorStand) {
            result = GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.INTERACT_INVENTORY, source, targetEntity, player, TrustTypes.CONTAINER, false);
        }
        if (result == Tristate.FALSE) {
            event.setCancelled(true);
            CommonEntityEventHandler.getInstance().sendInteractEntityDenyMessage(activeItem, targetEntity, claim, player);
        }
        GDTimings.PLAYER_INTERACT_ENTITY_SECONDARY_EVENT.stopTiming();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerBucketEmptyEvent(PlayerBucketEmptyEvent event) {
        onPlayerBucketEvent(event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerBucketFillEvent(PlayerBucketFillEvent event) {
        onPlayerBucketEvent(event);
    }

    public void onPlayerBucketEvent(PlayerBucketEvent event) {
        final Player player = event.getPlayer();
        final Block clickedBlock = event.getBlockClicked();
        GDCauseStackManager.getInstance().pushCause(player);
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUID())) {
            return;
        }
        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.INTERACT_BLOCK_SECONDARY.getName(), clickedBlock, player.getWorld().getUID())) {
            return;
        }

        GDTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.startTiming();
        final Object source = player;
        final Location location = clickedBlock.getLocation();
        final GDClaim claim = this.dataStore.getClaimAt(location);
        final TrustType trustType = NMSUtil.getInstance().hasBlockTileEntity(location) ? TrustTypes.CONTAINER : TrustTypes.ACCESSOR;

        Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.INTERACT_BLOCK_SECONDARY, source, clickedBlock, player, trustType, true);
        if (result == Tristate.FALSE) {
            event.setCancelled(true);
            GDTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTiming();
            return;
        }

        GDTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTiming();

        if (event instanceof PlayerBucketEmptyEvent) {
            // check block place
            result = GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.BLOCK_PLACE, source, event.getBucket().name().toLowerCase().replace("_bucket", ""), player, TrustTypes.BUILDER, true);
            if (result == Tristate.FALSE) {
                event.setCancelled(true);
                return;
            }
        } else if (event instanceof PlayerBucketFillEvent) {
            // check block break
            result = GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.BLOCK_BREAK, source, event.getBlockClicked(), player, TrustTypes.BUILDER, true);
            if (result == Tristate.FALSE) {
                event.setCancelled(true);
                return;
            }
        }
    }

    public void onPlayerInteractBlockPrimary(PlayerInteractEvent event, Player player) {
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        if (!GDFlags.INTERACT_BLOCK_PRIMARY || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUID())) {
            return;
        }
        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.INTERACT_BLOCK_PRIMARY.getName(), event.getClickedBlock(), player.getWorld().getUID())) {
            return;
        }

        final Block clickedBlock = event.getClickedBlock();
        final ItemStack itemInHand = event.getItem();
        final Location location = clickedBlock == null ? null : clickedBlock.getLocation();
        final GDPlayerData playerData = this.dataStore.getOrCreateGlobalPlayerData(player.getUniqueId());
        final Object source = itemInHand != null && !event.isBlockInHand() ? itemInHand : player;
        if (playerData.claimMode) {
            return;
        }
        // check give pet
        if (playerData.petRecipientUniqueId != null) {
            playerData.petRecipientUniqueId = null;
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().COMMAND_PET_TRANSFER_CANCEL);
            event.setCancelled(true);
            return;
        }

        if (location == null) {
            return;
        }

        GDTimings.PLAYER_INTERACT_BLOCK_PRIMARY_EVENT.startTiming();
        final GDClaim claim = this.dataStore.getClaimAt(location);
        final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.INTERACT_BLOCK_PRIMARY, source, clickedBlock.getState(), player, TrustTypes.BUILDER, true);
        if (result == Tristate.FALSE) {
            if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.BLOCK_BREAK.toString(), clickedBlock.getState(), player.getWorld().getUID())) {
                GDTimings.PLAYER_INTERACT_BLOCK_PRIMARY_EVENT.stopTiming();
                return;
            }
            if (GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.BLOCK_BREAK, player, clickedBlock.getState(), player, TrustTypes.BUILDER, true) == Tristate.TRUE) {
                GDTimings.PLAYER_INTERACT_BLOCK_PRIMARY_EVENT.stopTiming();
                return;
            }

            // Don't send a deny message if the player is in claim mode or is holding an investigation tool
            if (!playerData.claimMode && (GriefDefenderPlugin.getInstance().investigationTool == null || !NMSUtil.getInstance().hasItemInOneHand(player, GriefDefenderPlugin.getInstance().investigationTool))) {
                this.sendInteractBlockDenyMessage(itemInHand, clickedBlock, claim, player, playerData);
            }
            event.setCancelled(true);
            GDTimings.PLAYER_INTERACT_BLOCK_PRIMARY_EVENT.stopTiming();
            return;
        }
        GDTimings.PLAYER_INTERACT_BLOCK_PRIMARY_EVENT.stopTiming();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractBlockSecondary(PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        final GDPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.PHYSICAL) {
            onPlayerInteractBlockPrimary(event, player);
            return;
        }

        final Block clickedBlock = event.getClickedBlock();
        BlockState state = null;
        if (clickedBlock != null) {
            state = clickedBlock.getState();
        }
        final ItemStack itemInHand = event.getItem();
        final boolean hasTileEntity = clickedBlock != null ? NMSUtil.getInstance().hasBlockTileEntity(clickedBlock.getLocation()) : false;
        if (hasTileEntity && !(state instanceof Sign)) {
            onInventoryOpen(event, state.getLocation(), state, player);
            return;
        }
        GDCauseStackManager.getInstance().pushCause(player);
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUID())) {
            return;
        }
        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.INTERACT_BLOCK_SECONDARY.getName(), event.getClickedBlock(), player.getWorld().getUID())) {
            return;
        }

        GDTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.startTiming();
        final Object source = player;
        final Location location = event.getClickedBlock() != null ? event.getClickedBlock().getLocation() : null;

        if (NMSUtil.getInstance().isMainHandSlot(event.getHand()) && (playerData.claimMode || (itemInHand != null && GriefDefenderPlugin.getInstance().modificationTool != null && NMSUtil.getInstance().itemsEqual(itemInHand, GriefDefenderPlugin.getInstance().modificationTool)))) {
            onPlayerHandleClaimCreateAction(event, clickedBlock, player, itemInHand, playerData);
            // avoid changing blocks after using a shovel
            event.setUseInteractedBlock(Result.DENY);
            GDTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTiming();
            return;
        }

        if (location == null) {
            GDTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTiming();
            return;
        }
        final GDClaim claim = this.dataStore.getClaimAt(location);
        TrustType trustType = event.isBlockInHand() && event.getAction() != Action.PHYSICAL ? TrustTypes.BUILDER : TrustTypes.ACCESSOR;
        if (clickedBlock != null && clickedBlock.getType().toString().contains("DOOR")) {
            trustType = TrustTypes.ACCESSOR;
        }
        if (GDFlags.INTERACT_BLOCK_SECONDARY && playerData != null) {
            Flag flag = Flags.INTERACT_BLOCK_SECONDARY;
            if (event.getAction() == Action.PHYSICAL) {
                flag = Flags.COLLIDE_BLOCK;
            }
            Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, claim, flag, source, clickedBlock, player, trustType, true);
            if (result == Tristate.FALSE) {
                // if player is holding an item, check if it can be placed
                if (GDFlags.BLOCK_PLACE && itemInHand != null && itemInHand.getType().isBlock()) {
                    if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.BLOCK_PLACE.getName(), itemInHand, player.getWorld().getUID())) {
                        GDTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTiming();
                        return;
                    }
                    if (GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.BLOCK_PLACE, source, itemInHand, player, TrustTypes.BUILDER, true) == Tristate.TRUE) {
                        GDTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTiming();
                        return;
                    }
                }
                // Don't send a deny message if the player is in claim mode or is holding an investigation tool
                if (lastInteractItemCancelled != true) {
                    if (!playerData.claimMode && (GriefDefenderPlugin.getInstance().investigationTool == null || !NMSUtil.getInstance().hasItemInOneHand(player, GriefDefenderPlugin.getInstance().investigationTool))) {
                        if (event.getAction() == Action.PHYSICAL) {
                            if (player.getWorld().getTime() % 100 == 0L) {
                                this.sendInteractBlockDenyMessage(itemInHand, clickedBlock, claim, player, playerData);
                            }
                        } else {
                            this.sendInteractBlockDenyMessage(itemInHand, clickedBlock, claim, player, playerData);
                        }
                    }
                }

                event.setCancelled(true);
                GDTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTiming();
                return;
            }
        }

        GDTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTiming();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        final Player player = event.getPlayer();
        if (VecHelper.toVector3i(event.getFrom()).equals(VecHelper.toVector3i(event.getTo()))) {
            // Ignore teleports that have the same block position
            // This prevents players from getting through doors without permission
            return;
        }
        GDCauseStackManager.getInstance().pushCause(player);
        if (!GDFlags.ENTITY_TELEPORT_FROM && !GDFlags.ENTITY_TELEPORT_TO) {
            return;
        }

        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUID())) {
            return;
        }
        final boolean teleportFromBlacklisted = GriefDefenderPlugin.isSourceIdBlacklisted(Flags.ENTITY_TELEPORT_FROM.getName(), player, player.getWorld().getUID());
        final boolean teleportToBlacklisted = GriefDefenderPlugin.isSourceIdBlacklisted(Flags.ENTITY_TELEPORT_TO.getName(), player, player.getWorld().getUID());
        if (teleportFromBlacklisted && teleportToBlacklisted) {
            return;
        }

        GDTimings.ENTITY_TELEPORT_EVENT.startTiming();

        final TeleportCause type = event.getCause();
        final Location sourceLocation = event.getFrom();
        final Location destination = event.getTo();
        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        final GDClaim sourceClaim =  this.dataStore.getClaimAtPlayer(playerData, player.getLocation());
        // Cancel event if player is unable to teleport during PvP combat
        final boolean pvpCombatTeleport = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Boolean.class), player, Options.PVP_COMBAT_TELEPORT, sourceClaim);
        if (!pvpCombatTeleport) {
            final int combatTimeRemaining = playerData.getPvpCombatTimeRemaining();
            if (combatTimeRemaining > 0) {
                final Component denyMessage = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PVP_IN_COMBAT_NOT_ALLOWED,
                        ImmutableMap.of(
                        "time-remaining", combatTimeRemaining));
                GriefDefenderPlugin.sendMessage(player, denyMessage);
                event.setCancelled(true);
                GDTimings.ENTITY_TELEPORT_EVENT.stopTiming();
                return;
            }
        }

        // Handle BorderClaimEvent
        if (!CommonEntityEventHandler.getInstance().onEntityMove(event, sourceLocation, destination, player)) {
            event.setCancelled(true);
            GDTimings.ENTITY_TELEPORT_EVENT.stopTiming();
            return;
        }

        if (sourceClaim != null) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_PORTAL_EXIT,
                    ImmutableMap.of(
                    "player", sourceClaim.getOwnerName()));
            if (GDFlags.ENTITY_TELEPORT_FROM && !teleportFromBlacklisted && GDPermissionManager.getInstance().getFinalPermission(event, sourceLocation, sourceClaim, Flags.ENTITY_TELEPORT_FROM, type, player, player, TrustTypes.ACCESSOR, true) == Tristate.FALSE) {
                if (player != null) {
                    GriefDefenderPlugin.sendMessage(player, message);
                }

                event.setCancelled(true);
                GDTimings.ENTITY_TELEPORT_EVENT.stopTiming();
                return;
            } else if (GDFlags.EXIT_CLAIM && !teleportFromBlacklisted && GDPermissionManager.getInstance().getFinalPermission(event, sourceLocation, sourceClaim, Flags.EXIT_CLAIM, type, player, player, TrustTypes.ACCESSOR, true) == Tristate.FALSE) {
                if (player != null) {
                    GriefDefenderPlugin.sendMessage(player, message);
                }

                event.setCancelled(true);
                GDTimings.ENTITY_TELEPORT_EVENT.stopTiming();
                return;
            }
        }

        // check if destination world is enabled
        final World toWorld = destination.getWorld();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(toWorld.getUID())) {
            GDTimings.ENTITY_TELEPORT_EVENT.stopTiming();
            return;
        }

        final GDClaim toClaim = this.dataStore.getClaimAt(destination);
        if (toClaim != null) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_PORTAL_ENTER,
                    ImmutableMap.of(
                    "player", toClaim.getOwnerName()));
            if (GDFlags.ENTITY_TELEPORT_TO && !teleportToBlacklisted && GDPermissionManager.getInstance().getFinalPermission(event, destination, toClaim, Flags.ENTITY_TELEPORT_TO, type, player, player, TrustTypes.ACCESSOR, true) == Tristate.FALSE) {
                if (player != null) {
                    GriefDefenderPlugin.sendMessage(player, message);
                }

                event.setCancelled(true);
                GDTimings.ENTITY_TELEPORT_EVENT.stopTiming();
                return;
            } else if (GDFlags.ENTER_CLAIM && !teleportToBlacklisted && GDPermissionManager.getInstance().getFinalPermission(event, destination, toClaim, Flags.ENTER_CLAIM, type, player, player, TrustTypes.ACCESSOR, true) == Tristate.FALSE) {
                if (player != null) {
                    GriefDefenderPlugin.sendMessage(player, message);
                }

                event.setCancelled(true);
                GDTimings.ENTITY_TELEPORT_EVENT.stopTiming();
                return;
            }
        }

        if (player != null && !sourceLocation.getWorld().getUID().equals(toWorld.getUID())) {
            // new world, check if player has world storage for it
            GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(toWorld.getUID());

            // update lastActive timestamps for claims this player owns
            UUID playerUniqueId = player.getUniqueId();
            for (Claim claim : this.dataStore.getClaimWorldManager(toWorld.getUID()).getWorldClaims()) {
                if (claim.getOwnerUniqueId().equals(playerUniqueId)) {
                    // update lastActive timestamp for claim
                    claim.getData().setDateLastActive(Instant.now());
                    claimWorldManager.addClaim(claim);
                } else if (claim.getParent().isPresent() && claim.getParent().get().getOwnerUniqueId().equals(playerUniqueId)) {
                    // update lastActive timestamp for subdivisions if parent owner logs on
                    claim.getData().setDateLastActive(Instant.now());
                    claimWorldManager.addClaim(claim);
                }
            }
        }

        if (playerData != null) {
            if (toClaim.isTown()) {
                playerData.inTown = true;
            } else {
                playerData.inTown = false;
            }
        }
        // TODO
        /*if (event.getCause().first(PortalTeleportCause.class).isPresent()) {
            // FEATURE: when players get trapped in a nether portal, send them back through to the other side
            CheckForPortalTrapTask task = new CheckForPortalTrapTask(player, event.getFromTransform().getLocation());
            Sponge.getGame().getScheduler().createTaskBuilder().delayTicks(200).execute(task).submit(GriefDefender.instance);
        }*/
        GDTimings.ENTITY_TELEPORT_EVENT.stopTiming();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerMove(PlayerMoveEvent event){
        CommonEntityEventHandler.getInstance().onEntityMove(event, event.getFrom(), event.getTo(), event.getPlayer());
    }

    private void onPlayerHandleClaimCreateAction(PlayerInteractEvent event, Block clickedBlock, Player player, ItemStack itemInHand, GDPlayerData playerData) {
        if (player.isSneaking() && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            playerData.revertActiveVisual(player);
            // check for any active WECUI visuals
            if (this.worldEditProvider != null) {
                this.worldEditProvider.revertVisuals(player, playerData, null);
            }
            playerData.lastShovelLocation = null;
            playerData.endShovelLocation = null;
            playerData.claimResizing = null;
            playerData.shovelMode = ShovelTypes.BASIC;
            return;
        }

        GDTimings.PLAYER_HANDLE_SHOVEL_ACTION.startTiming();
        Location location = clickedBlock != null ? clickedBlock.getLocation() : null;

        if (location == null) {
            boolean ignoreAir = false;
            if (this.worldEditProvider != null) {
                // Ignore air so players can use client-side WECUI block target which uses max reach distance
                if (this.worldEditProvider.hasCUISupport(player) && playerData.getClaimCreateMode() == CreateModeTypes.VOLUME && playerData.lastShovelLocation != null) {
                    ignoreAir = true;
                }
            }
            final int distance = !ignoreAir ? 100 : 5;
            location = BlockUtil.getInstance().getTargetBlock(player, playerData, distance, ignoreAir).orElse(null);
            if (location == null) {
                GDTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTiming();
                return;
            }
        }

        // Always cancel to avoid breaking blocks such as grass
        event.setCancelled(true);
        playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        if (playerData.shovelMode == ShovelTypes.RESTORE) {
            if (true) {
                GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().FEATURE_NOT_AVAILABLE);
                GDTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTiming();
                return;
            }

            final GDClaim claim = this.dataStore.getClaimAtPlayer(location, playerData, true);
            if (!claim.isUserTrusted(player, TrustTypes.MANAGER)) {
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.BLOCK_CLAIMED,
                        ImmutableMap.of(
                        "player", claim.getOwnerName()));
                GriefDefenderPlugin.sendMessage(player, message);
                ClaimVisual claimVisual = new ClaimVisual(claim, ClaimVisual.ERROR);
                claimVisual.createClaimBlockVisuals(location.getBlockY(), player.getLocation(), playerData);
                claimVisual.apply(player);
                GDTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTiming();
                return;
            }

            Chunk chunk = player.getWorld().getChunkAt(location);
            World world = chunk.getWorld();
            world.regenerateChunk(chunk.getX(), chunk.getZ());
            GDTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTiming();
            return;
        }

        if (!playerData.canCreateClaim(player, true)) {
            GDTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTiming();
            return;
        }

        if (playerData.claimResizing != null && playerData.lastShovelLocation != null) {
            handleResizeFinish(event, player, location, playerData);
            GDTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTiming();
            return;
        }

        GDClaim claim = this.dataStore.getClaimAtPlayer(location, playerData, true);
        if (!claim.isWilderness()) {
            Component noEditReason = claim.allowEdit(player);
            if (noEditReason != null) {
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CREATE_OVERLAP_PLAYER,
                        ImmutableMap.of(
                        "player", claim.getOwnerName()));
                GriefDefenderPlugin.sendMessage(player, message);
                ClaimVisual visualization = new ClaimVisual(claim, ClaimVisual.ERROR);
                visualization.createClaimBlockVisuals(location.getBlockY(), player.getLocation(), playerData);
                visualization.apply(player);
                Set<Claim> claims = new HashSet<>();
                claims.add(claim);
                CommandHelper.showClaims(player, claims, location.getBlockY(), true);
            } else if (playerData.lastShovelLocation == null && BlockUtil.getInstance().clickedClaimCorner(claim, VecHelper.toVector3i(location))) {
                handleResizeStart(event, player, location, playerData, claim);
            } else if ((playerData.shovelMode == ShovelTypes.SUBDIVISION 
                    || ((claim.isTown() || claim.isAdminClaim()) && (playerData.lastShovelLocation == null || playerData.claimSubdividing != null)) && playerData.shovelMode != ShovelTypes.TOWN)) {
                if (claim.getTownClaim() != null && playerData.shovelMode == ShovelTypes.TOWN) {
                    GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CREATE_OVERLAP_SHORT);
                    Set<Claim> claims = new HashSet<>();
                    claims.add(claim);
                    CommandHelper.showClaims(player, claims, location.getBlockY(), true);
                } else if (playerData.lastShovelLocation == null) {
                    createSubdivisionStart(event, player, location, playerData, claim);
                } else if (playerData.claimSubdividing != null) {
                    createSubdivisionFinish(event, player, location, playerData, claim);
                }
            } else {
                GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CREATE_OVERLAP);
                Set<Claim> claims = new HashSet<>();
                claims.add(claim);
                CommandHelper.showClaims(player, claims, location.getBlockY(), true);
            }
            GDTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTiming();
            return;
        } else if (playerData.shovelMode == ShovelTypes.SUBDIVISION && playerData.lastShovelLocation != null) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CREATE_SUBDIVISION_FAIL);
            playerData.lastShovelLocation = null;
            GDTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTiming();
            return;
        }

        Location lastShovelLocation = playerData.lastShovelLocation;
        if (lastShovelLocation == null) {
            createClaimStart(event, player, location, playerData, claim);
            GDTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTiming();
            return;
        }

        createClaimFinish(event, player, location, playerData, claim);
        GDTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTiming();
    }

    private void createClaimStart(PlayerInteractEvent event, Player player, Location location, GDPlayerData playerData, GDClaim claim) {
        final WorldGuardProvider worldGuardProvider = GriefDefenderPlugin.getInstance().getWorldGuardProvider();
        if (worldGuardProvider != null && !worldGuardProvider.allowBuild(location, player)) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_BUILD,
                    ImmutableMap.of(
                    "player", "WorldGuard"
            ));
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }

        final ClaimType type = PlayerUtil.getInstance().getClaimTypeFromShovel(playerData.shovelMode);
        if (!player.hasPermission(GDPermissions.BYPASS_CLAIM_LIMIT)) {
            int createClaimLimit = -1;
            if (playerData.shovelMode == ShovelTypes.BASIC && (claim.isAdminClaim() || claim.isTown() || claim.isWilderness())) {
                createClaimLimit = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), player, Options.CREATE_LIMIT, claim).intValue();
            } else if (playerData.shovelMode == ShovelTypes.TOWN && (claim.isAdminClaim() || claim.isWilderness())) {
                createClaimLimit = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), player, Options.CREATE_LIMIT, claim).intValue();
            } else if (playerData.shovelMode == ShovelTypes.SUBDIVISION && !claim.isWilderness()) {
                createClaimLimit = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), player, Options.CREATE_LIMIT, claim).intValue();
            }

            if (createClaimLimit > 0 && createClaimLimit < (playerData.getClaimTypeCount(type) + 1)) {
                GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CREATE_FAILED_CLAIM_LIMIT));
                return;
            }
        }

        final int minClaimLevel = playerData.getMinClaimLevel();
        if (playerData.shovelMode != ShovelTypes.ADMIN && location.getBlockY() < minClaimLevel) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_BELOW_LEVEL,
                    ImmutableMap.of(
                    "limit", minClaimLevel));
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }
        final int maxClaimLevel = playerData.getMaxClaimLevel();
        if (playerData.shovelMode != ShovelTypes.ADMIN && location.getBlockY() > maxClaimLevel) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_ABOVE_LEVEL,
                    ImmutableMap.of(
                    "limit", maxClaimLevel));
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }

        if (playerData.shovelMode == ShovelTypes.SUBDIVISION && claim.isWilderness()) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CREATE_SUBDIVISION_FAIL);
            return;
        }

        if ((type == ClaimTypes.BASIC || type == ClaimTypes.TOWN) && GriefDefenderPlugin.getGlobalConfig().getConfig().economy.economyMode) {
            // Check current economy mode cost
            final Double economyBlockCost = playerData.getInternalEconomyBlockCost();
            if (economyBlockCost == null || economyBlockCost <= 0) {
                GriefDefenderPlugin.sendMessage(player, TextComponent.builder().color(TextColor.RED)
                        .append("Economy mode is enabled but the current cost for blocks is ")
                        .append("0", TextColor.GOLD)
                        .append("\nRaise the value for option 'economy-block-cost' in config or via '")
                        .append("/gd option claim", TextColor.WHITE)
                        .append("' command.", TextColor.RED)
                        .build());
                return;
            }
        }

        playerData.lastShovelLocation = location;
        Component message = null;
        if (playerData.claimMode) {
            message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_MODE_START,
                    ImmutableMap.of(
                    "type", PlayerUtil.getInstance().getClaimTypeComponentFromShovel(playerData.shovelMode)));
        } else {
            message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_START,
                    ImmutableMap.of(
                    "type", PlayerUtil.getInstance().getClaimTypeComponentFromShovel(playerData.shovelMode),
                    "item", ItemTypeRegistryModule.getInstance().getNMSKey(event.getItem())));
        }
        GriefDefenderPlugin.sendMessage(player, message);
        ClaimVisual visual = ClaimVisual.fromClick(location, location.getBlockY(), PlayerUtil.getInstance().getVisualTypeFromShovel(playerData.shovelMode), player, playerData);
        visual.apply(player, false);
    }

    private void createClaimFinish(PlayerInteractEvent event, Player player, Location location, GDPlayerData playerData, GDClaim claim) {
        final WorldGuardProvider worldGuardProvider = GriefDefenderPlugin.getInstance().getWorldGuardProvider();
        if (worldGuardProvider != null && !worldGuardProvider.allowBuild(location, player)) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_BUILD,
                    ImmutableMap.of(
                    "player", "WorldGuard"
            ));
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }

        Location lastShovelLocation = playerData.lastShovelLocation;
        final GDClaim firstClaim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(playerData.lastShovelLocation, playerData, true);
        final GDClaim clickedClaim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(location, playerData, true);
        if (!firstClaim.equals(clickedClaim)) {
            final GDClaim overlapClaim = firstClaim.isWilderness() ? clickedClaim : firstClaim;
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CREATE_OVERLAP_SHORT);
            Set<Claim> claims = new HashSet<>();
            claims.add(overlapClaim);
            CommandHelper.showClaims(player, claims, location.getBlockY(), true);
            return;
        }

        final boolean cuboid = playerData.getClaimCreateMode() == CreateModeTypes.VOLUME;
        Vector3i lesserBoundaryCorner = new Vector3i(
                lastShovelLocation.getBlockX(),
                cuboid ? lastShovelLocation.getBlockY() : playerData.getMinClaimLevel(),
                lastShovelLocation.getBlockZ());
        Vector3i greaterBoundaryCorner = new Vector3i(
                location.getBlockX(),
                cuboid ? location.getBlockY() : playerData.getMaxClaimLevel(),
                location.getBlockZ());

        final ClaimType type = PlayerUtil.getInstance().getClaimTypeFromShovel(playerData.shovelMode);
        if ((type == ClaimTypes.BASIC || type == ClaimTypes.TOWN) && GriefDefenderPlugin.getGlobalConfig().getConfig().economy.economyMode) {
            EconomyUtil.getInstance().economyCreateClaimConfirmation(player, playerData, location.getBlockY(), lesserBoundaryCorner, greaterBoundaryCorner, PlayerUtil.getInstance().getClaimTypeFromShovel(playerData.shovelMode),
                    cuboid, playerData.claimSubdividing);
            return;
        }

        ClaimResult result = this.dataStore.createClaim(
                player.getWorld(),
                lesserBoundaryCorner,
                greaterBoundaryCorner,
                type, player.getUniqueId(), cuboid);

        GDClaim gdClaim = (GDClaim) result.getClaim().orElse(null);
        if (!result.successful()) {
            if (result.getResultType() == ClaimResultType.OVERLAPPING_CLAIM) {
                GDClaim overlapClaim = (GDClaim) result.getClaim().get();
                GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CREATE_OVERLAP_SHORT);
                Set<Claim> claims = new HashSet<>();
                claims.add(overlapClaim);
                CommandHelper.showOverlapClaims(player, claims, location.getBlockY());
            } else if (result.getResultType() == ClaimResultType.CLAIM_EVENT_CANCELLED) {
                GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CREATE_CANCEL);
            } else {
                GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CREATE_FAILED_RESULT,
                        ImmutableMap.of("reason", result.getResultType())));
            }
            return;
        } else {
            playerData.lastShovelLocation = null;
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CREATE_SUCCESS,
                    ImmutableMap.of(
                    "type", gdClaim.getFriendlyNameType(true)));
            GriefDefenderPlugin.sendMessage(player, message);
            if (this.worldEditProvider != null) {
                this.worldEditProvider.stopVisualDrag(player);
                this.worldEditProvider.visualizeClaim(gdClaim, player, playerData, false);
            }
            gdClaim.getVisualizer().createClaimBlockVisuals(location.getBlockY(), player.getLocation(), playerData);
            gdClaim.getVisualizer().apply(player, false);
        }
    }

    private void createSubdivisionStart(PlayerInteractEvent event, Player player, Location location, GDPlayerData playerData, GDClaim claim) {
        final int minClaimLevel = playerData.getMinClaimLevel();
        if (playerData.shovelMode != ShovelTypes.ADMIN && location.getBlockY() < minClaimLevel) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_BELOW_LEVEL,
                    ImmutableMap.of(
                    "limit", minClaimLevel));
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }
        final int maxClaimLevel = playerData.getMaxClaimLevel();
        if (playerData.shovelMode != ShovelTypes.ADMIN && location.getBlockY() > maxClaimLevel) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_ABOVE_LEVEL,
                    ImmutableMap.of(
                    "limit", maxClaimLevel));
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }

        if (claim.isSubdivision()) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().RESIZE_OVERLAP_SUBDIVISION);
        } else {
            Component message = null;
            if (playerData.claimMode) {
                message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_START,
                        ImmutableMap.of(
                        "type", playerData.shovelMode.getName()));
            } else {
                message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_START,
                        ImmutableMap.of(
                        "type", playerData.shovelMode.getName(),
                        "item", ItemTypeRegistryModule.getInstance().getNMSKey(event.getItem())));
            }
            GriefDefenderPlugin.sendMessage(player, message);
            playerData.lastShovelLocation = location;
            playerData.claimSubdividing = claim;
            ClaimVisual visualization = ClaimVisual.fromClick(location, location.getBlockY(), PlayerUtil.getInstance().getVisualTypeFromShovel(playerData.shovelMode), player, playerData);
            visualization.apply(player, false);
        }
    }

    private void createSubdivisionFinish(PlayerInteractEvent event, Player player, Location location, GDPlayerData playerData, GDClaim claim) {
        final GDClaim clickedClaim = GriefDefenderPlugin.getInstance().dataStore.getClaimAt(location);
        if (clickedClaim == null || !playerData.claimSubdividing.getUniqueId().equals(clickedClaim.getUniqueId())) {
            if (clickedClaim != null) {
                GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CREATE_OVERLAP_SHORT);
                final GDClaim overlapClaim = playerData.claimSubdividing;
                Set<Claim> claims = new HashSet<>();
                claims.add(overlapClaim);
                CommandHelper.showClaims(player, claims, location.getBlockY(), true);
            }

            return;
        }

        Vector3i lesserBoundaryCorner = new Vector3i(playerData.lastShovelLocation.getBlockX(), 
                playerData.getClaimCreateMode() == CreateModeTypes.VOLUME ? playerData.lastShovelLocation.getBlockY() : playerData.getMinClaimLevel(),
                playerData.lastShovelLocation.getBlockZ());
        Vector3i greaterBoundaryCorner = new Vector3i(location.getBlockX(), 
                playerData.getClaimCreateMode() == CreateModeTypes.VOLUME ? location.getBlockY() : playerData.getMaxClaimLevel(),
                        location.getBlockZ());

        ClaimResult result = this.dataStore.createClaim(player.getWorld(),
                lesserBoundaryCorner, greaterBoundaryCorner, PlayerUtil.getInstance().getClaimTypeFromShovel(playerData.shovelMode),
                player.getUniqueId(), playerData.getClaimCreateMode() == CreateModeTypes.VOLUME, playerData.claimSubdividing);

        GDClaim gdClaim = (GDClaim) result.getClaim().orElse(null);
        if (!result.successful()) {
            if (result.getResultType() == ClaimResultType.OVERLAPPING_CLAIM) {
                GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CREATE_OVERLAP_SHORT);
                Set<Claim> claims = new HashSet<>();
                claims.add(gdClaim);
                CommandHelper.showOverlapClaims(player, claims, location.getBlockY());
            }
            event.setCancelled(true);
            return;
        } else {
            playerData.lastShovelLocation = null;
            playerData.claimSubdividing = null;
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CREATE_SUCCESS, ImmutableMap.of(
                    "type", gdClaim.getFriendlyNameType(true)));
            GriefDefenderPlugin.sendMessage(player, message);
            gdClaim.getVisualizer().createClaimBlockVisuals(location.getBlockY(), player.getLocation(), playerData);
            gdClaim.getVisualizer().apply(player, false);
            if (this.worldEditProvider != null) {
                this.worldEditProvider.stopVisualDrag(player);
                this.worldEditProvider.visualizeClaim(gdClaim, player, playerData, false);
            }
        }
    }

    private void handleResizeStart(PlayerInteractEvent event, Player player, Location location, GDPlayerData playerData, GDClaim claim) {
        boolean playerCanResize = true;
        if (!player.hasPermission(GDPermissions.CLAIM_RESIZE_ALL) 
                && !playerData.canIgnoreClaim(claim) 
                && !claim.isUserTrusted(player.getUniqueId(), TrustTypes.MANAGER)) {

            if (claim.isAdminClaim()) {
                if (!playerData.canManageAdminClaims) {
                    playerCanResize = false;
                }
            } else if (!player.getUniqueId().equals(claim.getOwnerUniqueId()) || !player.hasPermission(GDPermissions.CLAIM_RESIZE)) {
                playerCanResize = false;
            }
            if (!playerCanResize) {
                if (claim.parent != null) {
                    if (claim.parent.isAdminClaim() && claim.isSubdivision()) {
                        playerCanResize = player.hasPermission(GDPermissions.CLAIM_RESIZE_ADMIN_SUBDIVISION);
                    } else if (claim.parent.isBasicClaim() && claim.isSubdivision()) {
                        playerCanResize = player.hasPermission(GDPermissions.CLAIM_RESIZE_BASIC_SUBDIVISION);
                    } else if (claim.isTown()) {
                        playerCanResize = player.hasPermission(GDPermissions.CLAIM_RESIZE_TOWN);
                    } else if (claim.isAdminClaim()) {
                        playerCanResize = player.hasPermission(GDPermissions.CLAIM_RESIZE_ADMIN);
                    } else {
                        playerCanResize = player.hasPermission(GDPermissions.CLAIM_RESIZE_BASIC);
                    }
                } else if (claim.isTown()) {
                    playerCanResize = player.hasPermission(GDPermissions.CLAIM_RESIZE_TOWN);
                } else if (claim.isAdminClaim()) {
                    playerCanResize = player.hasPermission(GDPermissions.CLAIM_RESIZE_ADMIN);
                } else {
                    playerCanResize = player.hasPermission(GDPermissions.CLAIM_RESIZE_BASIC);
                }
            }
        }

        if (!claim.getInternalClaimData().isResizable() || !playerCanResize) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().PERMISSION_CLAIM_RESIZE);
            return;
        }

        playerData.claimResizing = claim;
        playerData.lastShovelLocation = location;
        if (this.worldEditProvider != null) {
            final int x = playerData.lastShovelLocation.getBlockX() == claim.lesserBoundaryCorner.getX() ? claim.greaterBoundaryCorner.getX() : claim.lesserBoundaryCorner.getX();
            final int y = playerData.lastShovelLocation.getBlockY() == claim.lesserBoundaryCorner.getY() ? claim.greaterBoundaryCorner.getY() : claim.lesserBoundaryCorner.getY();
            final int z = playerData.lastShovelLocation.getBlockZ() == claim.lesserBoundaryCorner.getZ() ? claim.greaterBoundaryCorner.getZ() : claim.lesserBoundaryCorner.getZ();
            this.worldEditProvider.visualizeClaim(claim, new Vector3i(x, y, z), VecHelper.toVector3i(playerData.lastShovelLocation), player, playerData, false);
        }
        // Show visual block for resize corner click
        ClaimVisual visual = ClaimVisual.fromClick(location, location.getBlockY(), PlayerUtil.getInstance().getVisualTypeFromShovel(playerData.shovelMode), player, playerData);
        visual.apply(player, false);
        GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().RESIZE_START);
    }

    private void handleResizeFinish(PlayerInteractEvent event, Player player, Location location, GDPlayerData playerData) {
        if (location.equals(playerData.lastShovelLocation)) {
            return;
        }

        playerData.endShovelLocation = location;
        int newx1, newx2, newz1, newz2, newy1, newy2;
        int smallX = 0, smallY = 0, smallZ = 0, bigX = 0, bigY = 0, bigZ = 0;

        newx1 = playerData.lastShovelLocation.getBlockX();
        newx2 = location.getBlockX();
        newy1 = playerData.lastShovelLocation.getBlockY();
        newy2 = location.getBlockY();
        newz1 = playerData.lastShovelLocation.getBlockZ();
        newz2 = location.getBlockZ();
        Vector3i lesserBoundaryCorner = playerData.claimResizing.getLesserBoundaryCorner();
        Vector3i greaterBoundaryCorner = playerData.claimResizing.getGreaterBoundaryCorner();
        smallX = lesserBoundaryCorner.getX();
        smallY = lesserBoundaryCorner.getY();
        smallZ = lesserBoundaryCorner.getZ();
        bigX = greaterBoundaryCorner.getX();
        bigY = greaterBoundaryCorner.getY();
        bigZ = greaterBoundaryCorner.getZ();

        if (newx1 == smallX) {
            smallX = newx2;
        } else {
            bigX = newx2;
        }

        if (newy1 == smallY) {
            smallY = newy2;
        } else {
            bigY = newy2;
        }

        if (newz1 == smallZ) {
            smallZ = newz2;
        } else {
            bigZ = newz2;
        }

        ClaimResult claimResult = null;
        claimResult = playerData.claimResizing.resize(smallX, bigX, smallY, bigY, smallZ, bigZ);
        if (claimResult.successful()) {
            Claim claim = (GDClaim) claimResult.getClaim().get();
            int claimBlocksRemaining = playerData.getRemainingClaimBlocks();;
            if (!playerData.claimResizing.isAdminClaim()) {
                UUID ownerID = playerData.claimResizing.getOwnerUniqueId();
                if (playerData.claimResizing.parent != null) {
                    ownerID = playerData.claimResizing.parent.getOwnerUniqueId();
                }

                if (ownerID.equals(player.getUniqueId())) {
                    claimBlocksRemaining = playerData.getRemainingClaimBlocks();
                } else {
                    GDPlayerData ownerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), ownerID);
                    claimBlocksRemaining = ownerData.getRemainingClaimBlocks();
                    final Player owner = Bukkit.getPlayer(ownerID);
                    if (owner == null || !owner.isOnline()) {
                        this.dataStore.clearCachedPlayerData(player.getWorld().getUID(), ownerID);
                    }
                }
            }

            playerData.claimResizing = null;
            playerData.lastShovelLocation = null;
            playerData.endShovelLocation = null;
            if (GriefDefenderPlugin.getInstance().isEconomyModeEnabled()) {
                final VaultProvider vaultProvider = GriefDefenderPlugin.getInstance().getVaultProvider();
                if (vaultProvider.hasAccount(player)) {
                    if (GriefDefenderPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.VOLUME) {
                        final double claimableChunks = claimBlocksRemaining / 65536.0;
                        final Map<String, Object> params = ImmutableMap.of(
                                "balance", String.valueOf("$" + vaultProvider.getBalance(player)),
                                "chunk-amount", Math.round(claimableChunks * 100.0)/100.0, 
                                "block-amount", claimBlocksRemaining);
                        GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_MODE_RESIZE_SUCCESS_3D, params));
                    } else {
                        final Map<String, Object> params = ImmutableMap.of(
                                "balance", String.valueOf("$" + vaultProvider.getBalance(player)),
                                "block-amount", claimBlocksRemaining);
                        GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_MODE_RESIZE_SUCCESS_2D, params));
                    }
                }
            } else {
                if (GriefDefenderPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.VOLUME) {
                    final double claimableChunks = claimBlocksRemaining / 65536.0;
                    final Map<String, Object> params = ImmutableMap.of(
                            "chunk-amount", Math.round(claimableChunks * 100.0)/100.0, 
                            "block-amount", claimBlocksRemaining);
                    GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.RESIZE_SUCCESS_3D, params));
                } else {
                    final Map<String, Object> params = ImmutableMap.of(
                            "block-amount", claimBlocksRemaining);
                    GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.RESIZE_SUCCESS_2D, params));
                }
            }
            playerData.revertActiveVisual(player);
            ((GDClaim) claim).getVisualizer().resetVisuals();
            ((GDClaim) claim).getVisualizer().createClaimBlockVisuals(location.getBlockY(), player.getLocation(), playerData);
            ((GDClaim) claim).getVisualizer().apply(player);
            if (this.worldEditProvider != null) {
                this.worldEditProvider.visualizeClaim(claim, player, playerData, false);
            }
        } else {
            if (claimResult.getResultType() == ClaimResultType.OVERLAPPING_CLAIM) {
                GDClaim overlapClaim = (GDClaim) claimResult.getClaim().get();
                GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().RESIZE_OVERLAP);
                Set<Claim> claims = new HashSet<>();
                claims.add(overlapClaim);
                CommandHelper.showOverlapClaims(player, claims, location.getBlockY());
            } else {
                if (!claimResult.getMessage().isPresent()) {
                    GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CLAIM_NOT_YOURS);
                }
            }

            playerData.claimSubdividing = null;
            event.setCancelled(true);
        }
    }

    private boolean investigateClaim(PlayerInteractEvent event, Player player, Block clickedBlock, ItemStack itemInHand) {
        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        if (playerData.claimMode && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            if (player.isSneaking()) {
                return true;
            }
            // claim mode inspects with left-click
            return false;
        }
        if (!playerData.claimMode && (itemInHand == null || GriefDefenderPlugin.getInstance().investigationTool == null || !NMSUtil.getInstance().itemsEqual(itemInHand, GriefDefenderPlugin.getInstance().investigationTool))){
            return false;
        }

        GDTimings.PLAYER_INVESTIGATE_CLAIM.startTiming();
        GDClaim claim = null;
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_AIR) {
            final int maxDistance = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), player, Options.RADIUS_INSPECT);
            claim = this.findNearbyClaim(player, maxDistance);
            if (player.isSneaking()) {
                if (!playerData.canIgnoreClaim(claim) && !player.hasPermission(GDPermissions.VISUALIZE_CLAIMS_NEARBY)) {
                    GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().PERMISSION_VISUAL_CLAIMS_NEARBY);
                    GDTimings.PLAYER_INVESTIGATE_CLAIM.stopTiming();
                    return false;
                }

                Location nearbyLocation = playerData.lastValidInspectLocation != null ? playerData.lastValidInspectLocation : player.getLocation();
                Set<Claim> claims = BlockUtil.getInstance().getNearbyClaims(nearbyLocation, maxDistance);
                int height = (int) (playerData.lastValidInspectLocation != null ? playerData.lastValidInspectLocation.getBlockY() : PlayerUtil.getInstance().getEyeHeight(player));

                boolean hideBorders = this.worldEditProvider != null &&
                                      this.worldEditProvider.hasCUISupport(player) &&
                                      GriefDefenderPlugin.getActiveConfig(player.getWorld().getUID()).getConfig().visual.hideBorders;
                if (!hideBorders) {
                    ClaimVisual visualization = ClaimVisual.fromClaims(claims, PlayerUtil.getInstance().getVisualClaimHeight(playerData, height), player.getLocation(), playerData, null);
                    visualization.apply(player);
                }

                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_SHOW_NEARBY,
                        ImmutableMap.of(
                        "amount", claims.size()));
                GriefDefenderPlugin.sendMessage(player, message);
                if (!claims.isEmpty()) {

                    if (this.worldEditProvider != null) {
                        worldEditProvider.revertVisuals(player, playerData, null);
                        worldEditProvider.visualizeClaims(claims, player, playerData, true);
                    }
                    CommandHelper.showClaims(player, claims);
                }
                GDTimings.PLAYER_INVESTIGATE_CLAIM.stopTiming();
                return true;
            }
            if (claim != null && claim.isWilderness()) {
                playerData.lastValidInspectLocation = null;
                GDTimings.PLAYER_INVESTIGATE_CLAIM.stopTiming();
                return false;
            }
        } else {
            claim = this.dataStore.getClaimAtPlayer(clickedBlock.getLocation(), playerData, true);
            if (claim.isWilderness()) {
                GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.BLOCK_NOT_CLAIMED));
                GDTimings.PLAYER_INVESTIGATE_CLAIM.stopTiming();
                return false;
            }
        }

        if (claim.getUniqueId() != playerData.visualClaimId) {
            int height = playerData.lastValidInspectLocation != null ? playerData.lastValidInspectLocation.getBlockY() : clickedBlock.getLocation().getBlockY();
            claim.getVisualizer().createClaimBlockVisuals(playerData.getClaimCreateMode() == CreateModeTypes.VOLUME ? height : PlayerUtil.getInstance().getEyeHeight(player), player.getLocation(), playerData);
            claim.getVisualizer().apply(player);
            if (this.worldEditProvider != null) {
                worldEditProvider.visualizeClaim(claim, player, playerData, true);
            }
            Set<Claim> claims = new HashSet<>();
            claims.add(claim);
            CommandHelper.showClaims(player, claims);
        }
        Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.BLOCK_CLAIMED,
                ImmutableMap.of(
                "player", claim.getOwnerName()));
        GriefDefenderPlugin.sendMessage(player, message);

        GDTimings.PLAYER_INVESTIGATE_CLAIM.stopTiming();
        return true;
    }

    private GDClaim findNearbyClaim(Player player, int maxDistance) {
        if (maxDistance <= 0) {
            maxDistance = 100;
        }
        BlockRay blockRay = BlockRay.from(player).distanceLimit(maxDistance).build();
        GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        GDClaim claim = null;
        int count = 0;

        while (blockRay.hasNext()) {
            BlockRayHit blockRayHit = blockRay.next();
            Location location = blockRayHit.getLocation();
            claim = this.dataStore.getClaimAt(location);
            if (claim != null && !claim.isWilderness() && (playerData.visualBlocks.isEmpty() || (claim.getUniqueId() != playerData.visualClaimId))) {
                playerData.lastValidInspectLocation = location;
                return claim;
            }

            final Block block = location.getBlock();
            if (!block.isEmpty() && !NMSUtil.getInstance().isBlockTransparent(block)) {
                break;
            }
            count++;
        }

        if (count == maxDistance) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CLAIM_TOO_FAR);
        } else if (claim != null && claim.isWilderness()){
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.BLOCK_NOT_CLAIMED));
        }

        return claim;
    }

    private void sendInteractBlockDenyMessage(ItemStack playerItem, Block block, GDClaim claim, Player player, GDPlayerData playerData) {
        if (claim.getData() != null && !claim.getData().allowDenyMessages()) {
            return;
        }

        if (claim.getData() != null && claim.getData().isExpired() && GriefDefenderPlugin.getActiveConfig(player.getWorld().getUID()).getConfig().claim.bankTaxSystem) {
            playerData.sendTaxExpireMessage(player, claim);
        } else if (playerItem == null || playerItem.getType() == Material.AIR) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_INTERACT_BLOCK,
                    ImmutableMap.of(
                    "player", claim.getOwnerName(),
                    "block",  BlockTypeRegistryModule.getInstance().getNMSKey(block)));
            GriefDefenderPlugin.sendClaimDenyMessage(claim, player, message);
        } else {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_INTERACT_ITEM_BLOCK,
                    ImmutableMap.of(
                    "item", ItemTypeRegistryModule.getInstance().getNMSKey(playerItem),
                    "block", BlockTypeRegistryModule.getInstance().getNMSKey(block)));
            GriefDefenderPlugin.sendClaimDenyMessage(claim, player, message);
        }
    }
}