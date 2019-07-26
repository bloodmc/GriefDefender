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
import com.griefdefender.GDPlayerData;
import com.griefdefender.GDTimings;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.ChatType;
import com.griefdefender.api.ChatTypes;
import com.griefdefender.api.GriefDefender;
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
import com.griefdefender.api.permission.flag.Flags;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.claim.GDClaimManager;
import com.griefdefender.command.CommandHelper;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.event.GDBorderClaimEvent;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.internal.provider.WorldEditProvider;
import com.griefdefender.internal.provider.WorldGuardProvider;
import com.griefdefender.internal.util.BlockUtil;
import com.griefdefender.internal.util.NMSUtil;
import com.griefdefender.internal.util.VecHelper;
import com.griefdefender.internal.visual.ClaimVisual;
import com.griefdefender.permission.GDFlags;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.storage.BaseStorage;
import com.griefdefender.util.BlockRay;
import com.griefdefender.util.BlockRayHit;
import com.griefdefender.util.PaginationUtil;
import com.griefdefender.util.PlayerUtil;
import com.griefdefender.visual.ClaimVisualType;
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
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.lang.ref.WeakReference;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerEventHandler implements Listener {

    private final BaseStorage dataStore;
    private final WorldEditProvider worldEditProvider;
    private int lastInteractItemPrimaryTick = -1;
    private int lastInteractItemSecondaryTick = -1;
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChangeHeldItem(PlayerItemHeldEvent event) {
        final Player player = event.getPlayer();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUID())) {
            return;
        }

        GDTimings.PLAYER_CHANGE_HELD_ITEM_EVENT.startTiming();
        GDPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());

        int newSlot = event.getNewSlot();
        ItemStack newItemStack = player.getInventory().getItem(newSlot);
        if(newItemStack != null && newItemStack.getType().equals(GriefDefenderPlugin.getInstance().modificationTool)) {
            playerData.lastShovelLocation = null;
            playerData.endShovelLocation = null;
            playerData.claimResizing = null;
            // always reset to basic claims mode
            if (playerData.shovelMode != ShovelTypes.BASIC) {
                playerData.shovelMode = ShovelTypes.BASIC;
                GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.MODE_BASIC));
            }

            // tell him how many claim blocks he has available
            if (GriefDefenderPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.VOLUME) {
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PLAYER_REMAINING_BLOCKS_3D,
                        ImmutableMap.of(
                        "amount", playerData.getRemainingClaimBlocks()));
                GriefDefenderPlugin.sendMessage(player, message);
            } else {
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PLAYER_REMAINING_BLOCKS_2D,
                       ImmutableMap.of(
                        "amount-blocks", playerData.getRemainingClaimBlocks(),
                        "amount", playerData.getRemainingChunks()));
                GriefDefenderPlugin.sendMessage(player, message);
            }
        } else {
            if (playerData.lastShovelLocation != null) {
                playerData.revertActiveVisual(player);
                // check for any active WECUI visuals
                if (this.worldEditProvider != null) {
                    this.worldEditProvider.revertVisuals(player, playerData, null);
                }
            }
            playerData.lastShovelLocation = null;
            playerData.endShovelLocation = null;
            playerData.claimResizing = null;
            playerData.shovelMode = ShovelTypes.BASIC;
        }
        GDTimings.PLAYER_CHANGE_HELD_ITEM_EVENT.stopTiming();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (!GDFlags.ITEM_DROP) {
            return;
        }

        final Player player = event.getPlayer();
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

        if (GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, GDPermissions.ITEM_DROP, player, event.getItemDrop(), player, TrustTypes.ACCESSOR, true) == Tristate.FALSE) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractInventoryOpen(InventoryOpenEvent event) {
        final HumanEntity player = event.getPlayer();
        if (!GDFlags.INTERACT_INVENTORY || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUID())) {
            return;
        }

        final InventoryHolder holder = event.getInventory().getHolder();
        if (holder == null) {
            return;
        }
        final BlockState state = holder instanceof BlockState ? (BlockState) holder : null;
        if (state == null) {
            return;
        }

        final Block block = state.getBlock();
        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.INTERACT_INVENTORY.getName(), block, player.getWorld().getUID())) {
            return;
        }

        GDTimings.PLAYER_INTERACT_INVENTORY_OPEN_EVENT.startTiming();
        final Location location = block.getLocation();
        final GDClaim claim = this.dataStore.getClaimAt(location);
        final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(player.getUniqueId());
        final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, claim, GDPermissions.INVENTORY_OPEN, player, block, user, TrustTypes.CONTAINER, true);
        if (result == Tristate.FALSE) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_INVENTORY_OPEN,
                    ImmutableMap.of(
                    "player", claim.getOwnerName(),
                    "block", NMSUtil.getInstance().getMaterialKey(block.getState().getType())));
            GriefDefenderPlugin.sendClaimDenyMessage(claim, player, message);
            event.setCancelled(true);
        }

        GDTimings.PLAYER_INTERACT_INVENTORY_OPEN_EVENT.stopTiming();
    }

    /*@EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractInventoryClose(InventoryCloseEvent event) {
        final HumanEntity player = event.getPlayer();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUID())) {
            return;
        }
        final Block block = event.getInventory().getLocation().getBlock();
        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.ITEM_DROP.getName(), block, player.getWorld().getUID())) {
            return;
        }

        GPTimings.PLAYER_INTERACT_INVENTORY_CLOSE_EVENT.startTiming();
        final Location location = player.getLocation();
        final GPClaim claim = this.dataStore.getClaimAt(location);
        final User user = PermissionUtils.getInstance().getUserSubject(player.getUniqueId());
        if (GPPermissionManager.getInstance().getFinalPermission(event, location, claim, GPPermissions.ITEM_DROP, player, block, user, TrustType.ACCESSOR, true) == Tristate.FALSE) {
            TextComponent message = GriefDefenderPlugin.getInstance().messageData.permissionItemDrop
                    .apply(ImmutableMap.of(
                    "owner", claim.getOwnerName(),
                    "item", block.getType().name().toLowerCase())).build();
            GriefDefenderPlugin.sendClaimDenyMessage(claim, player, message);
            event.setCancelled(true);
        }

        GPTimings.PLAYER_INTERACT_INVENTORY_CLOSE_EVENT.stopTiming();
    }*/

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractInventoryClick(InventoryClickEvent event) {
        final HumanEntity player = event.getWhoClicked();
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
            return;
        }

        final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(player.getUniqueId());
        final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, claim, GDPermissions.INVENTORY_CLICK, source, target, user, TrustTypes.CONTAINER, true);
        if (result == Tristate.FALSE) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_INTERACT_ITEM,
                    ImmutableMap.of(
                    "player", claim.getOwnerName(),
                    "item", NMSUtil.getInstance().getMaterialKey(target.getType())));
            GriefDefenderPlugin.sendClaimDenyMessage(claim, player, message);
            event.setCancelled(true);
        }
        GDTimings.PLAYER_INTERACT_INVENTORY_CLICK_EVENT.stopTiming();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractItem(PlayerInteractEvent event) {
        final World world = event.getPlayer().getWorld();
        final Block clickedBlock = event.getClickedBlock();
        final ItemStack itemInHand = event.getItem();
        final Player player = event.getPlayer();
        GDCauseStackManager.getInstance().pushCause(player);
        if (itemInHand == null || itemInHand.getType().isEdible()) {
            return;
        }

        if ((!GDFlags.INTERACT_ITEM_PRIMARY && !GDFlags.INTERACT_ITEM_SECONDARY) || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUID())) {
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
        final String ITEM_PERMISSION = primaryEvent ? GDPermissions.INTERACT_ITEM_PRIMARY : GDPermissions.INTERACT_ITEM_SECONDARY;
        if ((itemPrimaryBlacklisted && ITEM_PERMISSION.equals(GDPermissions.INTERACT_ITEM_PRIMARY)) || (itemSecondaryBlacklisted && ITEM_PERMISSION.equals(GDPermissions.INTERACT_ITEM_SECONDARY))) {
            return;
        }

        if (itemInHand.getType().equals(GriefDefenderPlugin.getInstance().modificationTool) ||
                itemInHand.getType().equals(GriefDefenderPlugin.getInstance().investigationTool)) {
            if (investigateClaim(event, player, clickedBlock, itemInHand)) {
                return;
            }

            //onPlayerHandleShovelAction(event, clickedBlock, player,  ((HandInteractEvent) event).getHandType(), playerData);
            return;
        }

        if (GDPermissionManager.getInstance().getFinalPermission(event, location, claim, ITEM_PERMISSION, player, itemInHand, player, TrustTypes.ACCESSOR, true) == Tristate.FALSE) {
            Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_INTERACT_ITEM,
                    ImmutableMap.of(
                    "player", claim.getOwnerName(),
                    "item", NMSUtil.getInstance().getMaterialKey(itemInHand.getType())));
            GriefDefenderPlugin.sendClaimDenyMessage(claim, player, message);
            event.setCancelled(true);
            lastInteractItemCancelled = true;
            return;
        }
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
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUID())) {
            return;
        }
        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.INTERACT_BLOCK_SECONDARY.getName(), clickedBlock, player.getWorld().getUID())) {
            return;
        }

        GDCauseStackManager.getInstance().pushCause(event.getPlayer());
        GDTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.startTiming();
        final Object source = player;
        final GDPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        final Location location = clickedBlock.getLocation();
        final GDClaim claim = this.dataStore.getClaimAt(location);
        final TrustType trustType = NMSUtil.getInstance().isBlockContainer(clickedBlock) ? TrustTypes.CONTAINER : TrustTypes.ACCESSOR;

        Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, claim, GDPermissions.INTERACT_BLOCK_SECONDARY, source, clickedBlock, player, trustType, true);
        if (result == Tristate.FALSE) {
            event.setCancelled(true);
            GDTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTiming();
            return;
        }

        playerData.setLastInteractData(claim);
        GDTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTiming();
    }

    public void onPlayerInteractBlockPrimary(PlayerInteractEvent event, Player player) {
        if (!GDFlags.INTERACT_BLOCK_PRIMARY || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUID())) {
            return;
        }
        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.INTERACT_BLOCK_PRIMARY.getName(), event.getClickedBlock(), player.getWorld().getUID())) {
            return;
        }

        GDTimings.PLAYER_INTERACT_BLOCK_PRIMARY_EVENT.startTiming();
        GDCauseStackManager.getInstance().pushCause(event.getPlayer());
        final Block clickedBlock = event.getClickedBlock();
        final ItemStack itemInHand = event.getItem();
        final Location location = clickedBlock == null ? null : clickedBlock.getLocation();
        final Object source = itemInHand != null ? itemInHand : player;
        if (location == null) {
            GDTimings.PLAYER_INTERACT_BLOCK_PRIMARY_EVENT.stopTiming();
            return;
        }

        final GDPlayerData playerData = this.dataStore.getOrCreatePlayerData(location.getWorld(), player.getUniqueId());
        final GDClaim claim = this.dataStore.getClaimAt(location);
        final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, claim, GDPermissions.INTERACT_BLOCK_PRIMARY, source, clickedBlock.getState(), player, TrustTypes.BUILDER, true);
        if (result == Tristate.FALSE) {
            if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.BLOCK_BREAK.toString(), clickedBlock.getState(), player.getWorld().getUID())) {
                GDTimings.PLAYER_INTERACT_BLOCK_PRIMARY_EVENT.stopTiming();
                return;
            }
            if (GDPermissionManager.getInstance().getFinalPermission(event, location, claim, GDPermissions.BLOCK_BREAK, player, clickedBlock.getState(), player, TrustTypes.BUILDER, true) == Tristate.TRUE) {
                GDTimings.PLAYER_INTERACT_BLOCK_PRIMARY_EVENT.stopTiming();
                playerData.setLastInteractData(claim);
                return;
            }

            // Don't send a deny message if the player is holding an investigation tool
            if (!NMSUtil.getInstance().hasItemInOneHand(player, GriefDefenderPlugin.getInstance().investigationTool)) {
                this.sendInteractBlockDenyMessage(itemInHand, clickedBlock, claim, player, playerData);
            }
            event.setCancelled(true);
            GDTimings.PLAYER_INTERACT_BLOCK_PRIMARY_EVENT.stopTiming();
            return;
        }
        playerData.setLastInteractData(claim);
        GDTimings.PLAYER_INTERACT_BLOCK_PRIMARY_EVENT.stopTiming();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractBlockSecondary(PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            onPlayerInteractBlockPrimary(event, player);
            return;
        }
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUID())) {
            return;
        }
        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.INTERACT_BLOCK_SECONDARY.getName(), event.getClickedBlock(), player.getWorld().getUID())) {
            return;
        }

        GDCauseStackManager.getInstance().pushCause(event.getPlayer());
        GDTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.startTiming();
        final Block clickedBlock = event.getClickedBlock();
        final ItemStack itemInHand = event.getItem();
        final Object source = player;
        final GDPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        final Location location = event.getClickedBlock() != null ? event.getClickedBlock().getLocation() : null;

        if (location == null && itemInHand != null) {
            onPlayerHandleShovelAction(event, clickedBlock, player, itemInHand, playerData);
            GDTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTiming();
            return;
        }

        if (itemInHand != null && (itemInHand.getType().equals(GriefDefenderPlugin.getInstance().modificationTool))) {
            onPlayerHandleShovelAction(event, clickedBlock, player, itemInHand, playerData);
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
        //GriefDefender.getPermissionManager().getFinalPermission(claim, Flags.ENTITY_SPAWN, source, target, user)
        final TrustType trustType = NMSUtil.getInstance().isBlockContainer(clickedBlock) ? TrustTypes.CONTAINER : TrustTypes.ACCESSOR;
        if (GDFlags.INTERACT_BLOCK_SECONDARY && playerData != null) {
            String permission = GDPermissions.INTERACT_BLOCK_SECONDARY;
            if (event.getAction() == Action.PHYSICAL) {
                permission = GDPermissions.COLLIDE_BLOCK;
            }
            Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, claim, permission, source, clickedBlock, player, trustType, true);
            if (result == Tristate.FALSE) {
                // if player is holding an item, check if it can be placed
                if (GDFlags.BLOCK_PLACE && itemInHand != null && itemInHand.getType().isBlock()) {
                    if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.BLOCK_PLACE.getName(), itemInHand, player.getWorld().getUID())) {
                        GDTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTiming();
                        return;
                    }
                    if (GDPermissionManager.getInstance().getFinalPermission(event, location, claim, GDPermissions.BLOCK_PLACE, source, itemInHand, player, TrustTypes.BUILDER, true) == Tristate.TRUE) {
                        GDTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTiming();
                        playerData.setLastInteractData(claim);
                        return;
                    }
                }
                // Don't send a deny message if the player is holding an investigation tool
                if (NMSUtil.getInstance().getRunningServerTicks() != lastInteractItemSecondaryTick || lastInteractItemCancelled != true) {
                    if (!NMSUtil.getInstance().hasItemInOneHand(player, GriefDefenderPlugin.getInstance().investigationTool)) {
                        this.sendInteractBlockDenyMessage(itemInHand, clickedBlock, claim, player, playerData);
                    }
                }

                event.setUseInteractedBlock(Result.DENY);
                GDTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTiming();
                return;
            }
        }

        if (itemInHand != null && (itemInHand.getType() == GriefDefenderPlugin.getInstance().modificationTool)) {
            onPlayerHandleShovelAction(event, clickedBlock, player, itemInHand, playerData);
            // avoid changing blocks after using a shovel
            event.setUseInteractedBlock(Result.DENY);
            this.sendInteractBlockDenyMessage(itemInHand, clickedBlock, claim, player, playerData);
        }
        playerData.setLastInteractData(claim);
        GDTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTiming();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!GDFlags.ENTITY_TELEPORT_FROM && !GDFlags.ENTITY_TELEPORT_TO) {
            return;
        }

        final Player player = event.getPlayer();
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
        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        final GDClaim sourceClaim =  this.dataStore.getClaimAtPlayer(playerData, player.getLocation());

        if (sourceClaim != null) {
            if (GDFlags.ENTITY_TELEPORT_FROM && !teleportFromBlacklisted && GDPermissionManager.getInstance().getFinalPermission(event, sourceLocation, sourceClaim, GDPermissions.ENTITY_TELEPORT_FROM, type, player, player, TrustTypes.ACCESSOR, true) == Tristate.FALSE) {
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_PORTAL_EXIT,
                        ImmutableMap.of(
                        "player", sourceClaim.getOwnerName()));
                if (player != null) {
                    GriefDefenderPlugin.sendMessage(player, message);
                }

                event.setCancelled(true);
                GDTimings.ENTITY_TELEPORT_EVENT.stopTiming();
                return;
            }
        }

        // check if destination world is enabled
        final Location destination = event.getTo();
        final World toWorld = destination.getWorld();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(toWorld.getUID())) {
            GDTimings.ENTITY_TELEPORT_EVENT.stopTiming();
            return;
        }

        final GDClaim toClaim = this.dataStore.getClaimAt(destination);
        if (toClaim != null) {
            if (GDFlags.ENTITY_TELEPORT_TO && !teleportToBlacklisted && GDPermissionManager.getInstance().getFinalPermission(event, destination, toClaim, GDPermissions.ENTITY_TELEPORT_TO, type, player, player, TrustTypes.ACCESSOR, true) == Tristate.FALSE) {
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_PORTAL_ENTER,
                        ImmutableMap.of(
                        "player", toClaim.getOwnerName()));
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
        final Vector3i fromPos = VecHelper.toVector3i(event.getFrom());
        final Vector3i toPos = VecHelper.toVector3i(event.getTo());
        if (fromPos.equals(toPos)) {
            return;
        }
        if ((!GDFlags.ENTER_CLAIM && !GDFlags.EXIT_CLAIM)) {
            return;
        }

        final Player player = event.getPlayer();
        final World world = event.getPlayer().getWorld();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUID())) {
            return;
        }
        final boolean enterBlacklisted = GriefDefenderPlugin.isSourceIdBlacklisted(Flags.ENTER_CLAIM.getName(), player, world.getUID());
        final boolean exitBlacklisted = GriefDefenderPlugin.isSourceIdBlacklisted(Flags.EXIT_CLAIM.getName(), player, world.getUID());
        if (enterBlacklisted && exitBlacklisted) {
            return;
        }

        GDTimings.ENTITY_MOVE_EVENT.startTiming();
        GDPlayerData playerData = this.dataStore.getOrCreatePlayerData(world, player.getUniqueId());

        final Location fromLocation = event.getFrom();
        final Location toLocation = event.getTo();

        GDClaim fromClaim = null;
        GDClaim toClaim = this.dataStore.getClaimAt(toLocation);
        if (playerData != null) {
            fromClaim = this.dataStore.getClaimAtPlayer(playerData, fromLocation);
        } else {
            fromClaim = this.dataStore.getClaimAt(fromLocation);
        }

        if (GDFlags.ENTER_CLAIM && !enterBlacklisted && playerData != null && playerData.lastClaim != null) {
            final GDClaim lastClaim = (GDClaim) playerData.lastClaim.get();
            if (lastClaim != null && lastClaim != fromClaim) {
                if (GDPermissionManager.getInstance().getFinalPermission(event, toLocation, toClaim, GDPermissions.ENTER_CLAIM, player, player, player, TrustTypes.ACCESSOR, false) == Tristate.FALSE) {
                    Location claimCorner = new Location(toClaim.getWorld(), toClaim.lesserBoundaryCorner.getX(), player.getLocation().getY(), toClaim.greaterBoundaryCorner.getZ());
                    player.teleport(claimCorner);
                }
            }
        }
        if (fromClaim == toClaim) {
            GDTimings.ENTITY_MOVE_EVENT.stopTiming();
            return;
        }

        GDBorderClaimEvent gpEvent = new GDBorderClaimEvent(player, fromClaim, toClaim);
        if (toClaim.isUserTrusted(player, TrustTypes.ACCESSOR)) {
            GriefDefender.getEventManager().post(gpEvent);
            if (gpEvent.cancelled()) {
                if (player.getVehicle() != null) {
                   /* final Vehicle vehicle = (Vehicle) player.getVehicle();
                    final EntityPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
                    final net.minecraft.server.Entity nmsVehicle = ((CraftEntity) vehicle).getHandle();
                    final BlockPosition fromBlockPos = VecHelper.toBlockPos(event.getFrom());
                    nmsVehicle.setPositionRotation(fromBlockPos, nmsVehicle.lastYaw, nmsVehicle.lastPitch);
                    nmsPlayer.playerConnection.networkManager.sendPacket(new PacketPlayOutVehicleMove(nmsVehicle));*/
                }
                event.setCancelled(true);
                final Component cancelMessage = gpEvent.getMessage().orElse(null);
                if (player != null && cancelMessage != null) {
                    TextAdapter.sendComponent(player, cancelMessage);
                }
            } else {
                if (playerData != null) {
                    final boolean showGpPrefix = GriefDefenderPlugin.getGlobalConfig().getConfig().message.showGdPrefixGreetingFarewell;
                    playerData.lastClaim = new WeakReference<>(toClaim);
                    TextComponent welcomeMessage = (TextComponent) gpEvent.getEnterMessage().orElse(null);
                    if (welcomeMessage != null && !welcomeMessage.equals(TextComponent.empty())) {
                        ChatType chatType = gpEvent.getEnterMessageChatType();
                        if (chatType == ChatTypes.ACTION_BAR) {
                            TextAdapter.sendActionBar(player, TextComponent.builder("")
                                    .append(showGpPrefix ? GriefDefenderPlugin.GD_TEXT : TextComponent.empty())
                                    .append(welcomeMessage)
                                    .build());
                        } else {
                            TextAdapter.sendComponent(player, TextComponent.builder("")
                                    .append(showGpPrefix ? GriefDefenderPlugin.GD_TEXT : TextComponent.empty())
                                    .append(welcomeMessage)
                                    .build());
                        }
                    }

                    Component farewellMessage = gpEvent.getExitMessage().orElse(null);
                    if (farewellMessage != null && !farewellMessage.equals(TextComponent.empty()) && !farewellMessage.equals("")) {
                        ChatType chatType = gpEvent.getExitMessageChatType();
                        if (chatType == ChatTypes.ACTION_BAR) {
                            TextAdapter.sendActionBar(player, TextComponent.builder("")
                                    .append(showGpPrefix ? GriefDefenderPlugin.GD_TEXT : TextComponent.empty())
                                    .append(farewellMessage)
                                    .build());
                        } else {
                            TextAdapter.sendComponent(player, TextComponent.builder("")
                                    .append(showGpPrefix ? GriefDefenderPlugin.GD_TEXT : TextComponent.empty())
                                    .append(farewellMessage)
                                    .build());
                        }
                    }

                    if (toClaim.isInTown()) {
                        playerData.inTown = true;
                    } else {
                        playerData.inTown = false;
                    }
                }
            }

            GDTimings.ENTITY_MOVE_EVENT.stopTiming();
            return;
        }

        if (fromClaim != toClaim) {
            boolean enterCancelled = false;
            boolean exitCancelled = false;
            // enter
            if (GDFlags.ENTER_CLAIM && !enterBlacklisted && GDPermissionManager.getInstance().getFinalPermission(event, toLocation, toClaim, GDPermissions.ENTER_CLAIM, player, player, player) == Tristate.FALSE) {
                enterCancelled = true;
                gpEvent.cancelled(true);
            }

            // exit
            if (GDFlags.EXIT_CLAIM && !exitBlacklisted && GDPermissionManager.getInstance().getFinalPermission(event, fromLocation, fromClaim, GDPermissions.EXIT_CLAIM, player, player, player) == Tristate.FALSE) {
                exitCancelled = true;
                gpEvent.cancelled(true);
            }

            GriefDefender.getEventManager().post(gpEvent);
            if (gpEvent.cancelled()) {
                final Component cancelMessage = gpEvent.getMessage().orElse(null);
                if (exitCancelled) {
                    if (cancelMessage != null) {
                        GriefDefenderPlugin.sendClaimDenyMessage(fromClaim, player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_CLAIM_EXIT));
                    }
                } else if (enterCancelled) {
                    if (cancelMessage != null) {
                        GriefDefenderPlugin.sendClaimDenyMessage(toClaim, player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_CLAIM_ENTER));
                    }
                }

                if (cancelMessage != null) {
                    TextAdapter.sendComponent(player, cancelMessage);
                }

                if (player.getVehicle() != null) {
                   /* final Vehicle vehicle = (Vehicle) player.getVehicle();
                    final EntityPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
                    final net.minecraft.server.Entity nmsVehicle = ((CraftEntity) vehicle).getHandle();
                    final BlockPosition fromBlockPos = VecHelper.toBlockPos(event.getFrom());
                    nmsVehicle.setPositionRotation(fromBlockPos, nmsVehicle.lastYaw, nmsVehicle.lastPitch);
                    nmsPlayer.playerConnection.networkManager.sendPacket(new PacketPlayOutVehicleMove(nmsVehicle));*/
                }
                event.setCancelled(true);
                GDTimings.ENTITY_MOVE_EVENT.stopTiming();
                return;
            }

            if (playerData != null) {
                final boolean showGpPrefix = GriefDefenderPlugin.getGlobalConfig().getConfig().message.showGdPrefixGreetingFarewell;
                playerData.lastClaim = new WeakReference<>(toClaim);
                Component welcomeMessage = gpEvent.getEnterMessage().orElse(null);
                if (welcomeMessage != null && !welcomeMessage.equals(TextComponent.empty()) && !welcomeMessage.equals("")) {
                    ChatType chatType = gpEvent.getEnterMessageChatType();
                    if (chatType == ChatTypes.ACTION_BAR) {
                        TextAdapter.sendActionBar(player, TextComponent.builder("")
                                .append(showGpPrefix ? GriefDefenderPlugin.GD_TEXT : TextComponent.empty())
                                .append(welcomeMessage)
                                .build());
                    } else {
                        TextAdapter.sendComponent(player, TextComponent.builder("")
                                .append(showGpPrefix ? GriefDefenderPlugin.GD_TEXT : TextComponent.empty())
                                .append(welcomeMessage)
                                .build());
                    }
                }

                Component farewellMessage = gpEvent.getExitMessage().orElse(null);
                if (farewellMessage != null && !farewellMessage.equals(TextComponent.empty()) && !farewellMessage.equals("")) {
                    ChatType chatType = gpEvent.getExitMessageChatType();
                    if (chatType == ChatTypes.ACTION_BAR) {
                        TextAdapter.sendActionBar(player, TextComponent.builder("")
                                .append(showGpPrefix ? GriefDefenderPlugin.GD_TEXT : TextComponent.empty())
                                .append(farewellMessage)
                                .build());
                    } else {
                        TextAdapter.sendComponent(player, TextComponent.builder("")
                                .append(showGpPrefix ? GriefDefenderPlugin.GD_TEXT : TextComponent.empty())
                                .append(farewellMessage)
                                .build());
                    }
                }

                if (toClaim.isInTown()) {
                    playerData.inTown = true;
                } else {
                    playerData.inTown = false;
                }
            }
        }

        GDTimings.ENTITY_MOVE_EVENT.stopTiming();
    }

    private void onPlayerHandleShovelAction(PlayerInteractEvent event, Block clickedBlock, Player player, ItemStack itemInHand, GDPlayerData playerData) {
        if (itemInHand == null || !itemInHand.getType().equals(GriefDefenderPlugin.getInstance().modificationTool)) {
            return;
        }

        GDTimings.PLAYER_HANDLE_SHOVEL_ACTION.startTiming();
        Location location = clickedBlock != null ? clickedBlock.getLocation() : null;

        if (location == null) {
            boolean ignoreAir = false;
            if (this.worldEditProvider != null) {
                // Ignore air so players can use client-side WECUI block target which uses max reach distance
                if (this.worldEditProvider.hasCUISupport(player) && playerData.getClaimCreateMode() == 1 && playerData.lastShovelLocation != null) {
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
                GriefDefenderPlugin.sendMessage(player, TextComponent.of("This mode is currently being worked on and will be available in a future build.", TextColor.RED));
                GDTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTiming();
                return;
            }

            final GDClaim claim = this.dataStore.getClaimAtPlayer(location, playerData, true);
            if (!claim.isUserTrusted(player, TrustTypes.MANAGER)) {
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.BLOCK_CLAIMED,
                        ImmutableMap.of(
                        "player", claim.getOwnerName()));
                GriefDefenderPlugin.sendMessage(player, message);
                ClaimVisual claimVisual = new ClaimVisual(claim, ClaimVisualType.ERROR);
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

        if (playerData.claimResizing != null) {
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
                ClaimVisual visualization = new ClaimVisual(claim, ClaimVisualType.ERROR);
                visualization.createClaimBlockVisuals(location.getBlockY(), player.getLocation(), playerData);
                visualization.apply(player);
                Set<Claim> claims = new HashSet<>();
                claims.add(claim);
                CommandHelper.showClaims(player, claims, location.getBlockY(), true);
            } else if (BlockUtil.getInstance().clickedClaimCorner(claim, VecHelper.toVector3i(location))) {
                handleResizeStart(event, player, location, playerData, claim);
            } else if ((playerData.shovelMode == ShovelTypes.SUBDIVISION 
                    || ((claim.isTown() || claim.isAdminClaim()) && (playerData.lastShovelLocation == null || playerData.claimSubdividing != null)) && playerData.shovelMode != ShovelTypes.TOWN)) {
                if (claim.getTownClaim() != null && playerData.shovelMode == ShovelTypes.TOWN) {
                    GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CREATE_OVERLAP_SHORT));
                    Set<Claim> claims = new HashSet<>();
                    claims.add(claim);
                    CommandHelper.showClaims(player, claims, location.getBlockY(), true);
                } else if (playerData.lastShovelLocation == null) {
                    createSubdivisionStart(event, player, location, playerData, claim);
                } else if (playerData.claimSubdividing != null) {
                    createSubdivisionFinish(event, player, location, playerData, claim);
                }
            } else {
                GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CREATE_OVERLAP));
                Set<Claim> claims = new HashSet<>();
                claims.add(claim);
                CommandHelper.showClaims(player, claims, location.getBlockY(), true);
            }
            GDTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTiming();
            return;
        } else if (playerData.shovelMode == ShovelTypes.SUBDIVISION && playerData.lastShovelLocation != null) {
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CREATE_SUBDIVISION_FAIL));
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

        if (!player.hasPermission(GDPermissions.OVERRIDE_CLAIM_LIMIT)) {
            int createClaimLimit = -1;
            if (playerData.shovelMode == ShovelTypes.BASIC && (claim.isAdminClaim() || claim.isTown() || claim.isWilderness())) {
                createClaimLimit = GDPermissionManager.getInstance().getInternalOptionValue(player, Options.CREATE_LIMIT, claim, playerData).intValue();
            } else if (playerData.shovelMode == ShovelTypes.TOWN && (claim.isAdminClaim() || claim.isWilderness())) {
                createClaimLimit = GDPermissionManager.getInstance().getInternalOptionValue(player, Options.CREATE_LIMIT, claim, playerData).intValue();
            } else if (playerData.shovelMode == ShovelTypes.SUBDIVISION && !claim.isWilderness()) {
                createClaimLimit = GDPermissionManager.getInstance().getInternalOptionValue(player, Options.CREATE_LIMIT, claim, playerData).intValue();
            }

            if (createClaimLimit > 0 && createClaimLimit < (playerData.getInternalClaims().size() + 1)) {
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
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CREATE_SUBDIVISION_FAIL));
            return;
        }

        playerData.revertActiveVisual(player);
        playerData.lastShovelLocation = location;
        final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_START,
                ImmutableMap.of(
                "type", playerData.shovelMode.getName()));
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
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CREATE_OVERLAP_SHORT));
            Set<Claim> claims = new HashSet<>();
            claims.add(overlapClaim);
            CommandHelper.showClaims(player, claims, location.getBlockY(), true);
            return;
        }

        final boolean cuboid = playerData.getClaimCreateMode() == 1;
        Vector3i lesserBoundaryCorner = new Vector3i(
                lastShovelLocation.getBlockX(),
                cuboid ? lastShovelLocation.getBlockY() : playerData.getMinClaimLevel(),
                lastShovelLocation.getBlockZ());
        Vector3i greaterBoundaryCorner = new Vector3i(
                location.getBlockX(),
                cuboid ? location.getBlockY() : playerData.getMaxClaimLevel(),
                location.getBlockZ());

        final ClaimType type = PlayerUtil.getInstance().getClaimTypeFromShovel(playerData.shovelMode);
        /*if ((type == ClaimTypes.BASIC || type == ClaimTypes.TOWN) && GriefDefenderPlugin.getGlobalConfig().getConfig().economy.economyMode) {
            EconomyUtils.economyCreateClaimConfirmation(player, playerData, location.getBlockY(), lesserBoundaryCorner, greaterBoundaryCorner, PlayerUtils.getClaimTypeFromShovel(playerData.shovelMode),
                    cuboid, playerData.claimSubdividing);
            GPTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTiming();
            return;
        }*/

        GDCauseStackManager.getInstance().pushCause(player);
        ClaimResult result = this.dataStore.createClaim(
                player.getWorld(),
                lesserBoundaryCorner,
                greaterBoundaryCorner,
                type, player.getUniqueId(), cuboid);
        GDCauseStackManager.getInstance().popCause();

        GDClaim gpClaim = (GDClaim) result.getClaim().orElse(null);
        if (!result.successful()) {
            if (result.getResultType() == ClaimResultType.OVERLAPPING_CLAIM) {
                GDClaim overlapClaim = (GDClaim) result.getClaim().get();
                GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CREATE_OVERLAP_SHORT));
                Set<Claim> claims = new HashSet<>();
                claims.add(overlapClaim);
                CommandHelper.showOverlapClaims(player, claims, location.getBlockY());
            } else if (result.getResultType() == ClaimResultType.CLAIM_EVENT_CANCELLED) {
                GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.claimCreateCancel.toText());
            }
            return;
        } else {
            playerData.lastShovelLocation = null;
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CREATE_SUCCESS,
                    ImmutableMap.of(
                    "type", gpClaim.getType().getName()));
            GriefDefenderPlugin.sendMessage(player, message);
            if (this.worldEditProvider != null) {
                this.worldEditProvider.stopVisualDrag(player);
                this.worldEditProvider.visualizeClaim(gpClaim, player, playerData, false);
            }
            gpClaim.getVisualizer().createClaimBlockVisuals(location.getBlockY(), player.getLocation(), playerData);
            gpClaim.getVisualizer().apply(player, false);
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
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.RESIZE_OVERLAP_SUBDIVISION));
        } else {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_START,
                    ImmutableMap.of(
                    "type", playerData.shovelMode.getName()));
            GriefDefenderPlugin.sendMessage(player, message);
            playerData.lastShovelLocation = location;
            playerData.claimSubdividing = claim;
            ClaimVisual visualization = ClaimVisual.fromClick(location, location.getBlockY(), PlayerUtil.getInstance().getVisualTypeFromShovel(playerData.shovelMode), player, playerData);
            visualization.apply(player, false);
            return;
        }
    }

    private void createSubdivisionFinish(PlayerInteractEvent event, Player player, Location location, GDPlayerData playerData, GDClaim claim) {
        final GDClaim clickedClaim = GriefDefenderPlugin.getInstance().dataStore.getClaimAt(location);
        if (clickedClaim == null || !playerData.claimSubdividing.getUniqueId().equals(clickedClaim.getUniqueId())) {
            if (clickedClaim != null) {
                GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CREATE_OVERLAP_SHORT));
                final GDClaim overlapClaim = playerData.claimSubdividing;
                Set<Claim> claims = new HashSet<>();
                claims.add(overlapClaim);
                CommandHelper.showClaims(player, claims, location.getBlockY(), true);
            }

            return;
        }

        Vector3i lesserBoundaryCorner = new Vector3i(playerData.lastShovelLocation.getBlockX(), 
                playerData.getClaimCreateMode() == 1 ? playerData.lastShovelLocation.getBlockY() : playerData.getMinClaimLevel(),
                playerData.lastShovelLocation.getBlockZ());
        Vector3i greaterBoundaryCorner = new Vector3i(location.getBlockX(), 
                playerData.getClaimCreateMode() == 1 ? location.getBlockY() : playerData.getMaxClaimLevel(),
                        location.getBlockZ());

        ClaimResult result = this.dataStore.createClaim(player.getWorld(),
                lesserBoundaryCorner, greaterBoundaryCorner, PlayerUtil.getInstance().getClaimTypeFromShovel(playerData.shovelMode),
                player.getUniqueId(), playerData.getClaimCreateMode() == 1, playerData.claimSubdividing);

        GDClaim gpClaim = (GDClaim) result.getClaim().orElse(null);
        if (!result.successful()) {
            if (result.getResultType() == ClaimResultType.OVERLAPPING_CLAIM) {
                GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CREATE_OVERLAP_SHORT));
                Set<Claim> claims = new HashSet<>();
                claims.add(gpClaim);
                CommandHelper.showOverlapClaims(player, claims, location.getBlockY());
            }
            event.setCancelled(true);
            return;
        } else {
            playerData.lastShovelLocation = null;
            playerData.claimSubdividing = null;
            final Component message = GriefDefenderPlugin.getInstance().messageData.claimCreateSuccess
                    .apply(ImmutableMap.of(
                    "type", playerData.shovelMode.getName())).build();
            GriefDefenderPlugin.sendMessage(player, message);
            gpClaim.getVisualizer().createClaimBlockVisuals(location.getBlockY(), player.getLocation(), playerData);
            gpClaim.getVisualizer().apply(player, false);
            if (this.worldEditProvider != null) {
                this.worldEditProvider.stopVisualDrag(player);
                this.worldEditProvider.visualizeClaim(gpClaim, player, playerData, false);
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
            } else if (!player.getUniqueId().equals(claim.getOwnerUniqueId())) {
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
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_CLAIM_RESIZE));
            return;
        }

        playerData.revertActiveVisual(player);
        playerData.claimResizing = claim;
        playerData.lastShovelLocation = location;
        if (this.worldEditProvider != null) {
            final int x = playerData.lastShovelLocation.getBlockX() == claim.lesserBoundaryCorner.getX() ? claim.greaterBoundaryCorner.getX() : claim.lesserBoundaryCorner.getX();
            final int y = playerData.lastShovelLocation.getBlockY() == claim.lesserBoundaryCorner.getY() ? claim.greaterBoundaryCorner.getY() : claim.lesserBoundaryCorner.getY();
            final int z = playerData.lastShovelLocation.getBlockZ() == claim.lesserBoundaryCorner.getZ() ? claim.greaterBoundaryCorner.getZ() : claim.lesserBoundaryCorner.getZ();
            this.worldEditProvider.visualizeClaim(claim, new Vector3i(x, y, z), VecHelper.toVector3i(playerData.lastShovelLocation), player, playerData, false);
        }
        GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.RESIZE_START));
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
        GDCauseStackManager.getInstance().pushCause(player);
        claimResult = playerData.claimResizing.resize(smallX, bigX, smallY, bigY, smallZ, bigZ);
        GDCauseStackManager.getInstance().popCause();
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
                    if (!owner.isOnline()) {
                        this.dataStore.clearCachedPlayerData(player.getWorld().getUID(), ownerID);
                    }
                }
            }

            playerData.claimResizing = null;
            playerData.lastShovelLocation = null;
            playerData.endShovelLocation = null;
            if (GriefDefenderPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.VOLUME) {
                final double claimableChunks = claimBlocksRemaining / 65536.0;
                final Map<String, ?> params = ImmutableMap.of(
                        "remaining-chunks", Math.round(claimableChunks * 100.0)/100.0, 
                        "remaining-blocks", claimBlocksRemaining);
                GriefDefenderPlugin.sendMessage(player, MessageStorage.CLAIM_RESIZE_SUCCESS_3D, GriefDefenderPlugin.getInstance().messageData.claimResizeSuccess3d, params);
            } else {
                final Map<String, ?> params = ImmutableMap.of(
                        "remaining-blocks", claimBlocksRemaining);
                GriefDefenderPlugin.sendMessage(player, MessageStorage.CLAIM_RESIZE_SUCCESS_2D, GriefDefenderPlugin.getInstance().messageData.claimResizeSuccess, params);
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
                GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.RESIZE_OVERLAP));
                Set<Claim> claims = new HashSet<>();
                claims.add(overlapClaim);
                CommandHelper.showOverlapClaims(player, claims, location.getBlockY());
            } else {
                if (!claimResult.getMessage().isPresent()) {
                    GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_NOT_YOURS));
                }
            }

            playerData.claimSubdividing = null;
            event.setCancelled(true);
        }
    }

    private boolean investigateClaim(PlayerInteractEvent event, Player player, Block clickedBlock, ItemStack itemInHand) {
        if (itemInHand == null || itemInHand.getType() != GriefDefenderPlugin.getInstance().investigationTool) {
            return false;
        }

        GDTimings.PLAYER_INVESTIGATE_CLAIM.startTiming();
        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            playerData.revertActiveVisual(player);
            if (this.worldEditProvider != null) {
                this.worldEditProvider.revertVisuals(player, playerData, null);
            }
            GDTimings.PLAYER_INVESTIGATE_CLAIM.stopTiming();
            return false;
        }

        GDClaim claim = null;
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_AIR) {
            claim = this.findNearbyClaim(player);
            if (player.isSneaking()) {
                if (!playerData.canIgnoreClaim(claim) && !player.hasPermission(GDPermissions.VISUALIZE_CLAIMS_NEARBY)) {
                    GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_VISUAL_CLAIMS_NEARBY));
                    GDTimings.PLAYER_INVESTIGATE_CLAIM.stopTiming();
                    return false;
                }

                Location nearbyLocation = playerData.lastValidInspectLocation != null ? playerData.lastValidInspectLocation : player.getLocation();
                Set<Claim> claims = BlockUtil.getInstance().getNearbyClaims(nearbyLocation);
                int height = (int) (playerData.lastValidInspectLocation != null ? playerData.lastValidInspectLocation.getBlockY() : PlayerUtil.getInstance().getEyeHeight(player));

                boolean hideBorders = this.worldEditProvider != null &&
                                      this.worldEditProvider.hasCUISupport(player) &&
                                      GriefDefenderPlugin.getActiveConfig(player.getWorld().getUID()).getConfig().claim.hideBorders;
                if (!hideBorders) {
                    ClaimVisual visualization = ClaimVisual.fromClaims(claims, playerData.getClaimCreateMode() == 1 ? height : PlayerUtil.getInstance().getEyeHeight(player), player.getLocation(), playerData, null);
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
            playerData.revertActiveVisual(player);
            claim.getVisualizer().createClaimBlockVisuals(playerData.getClaimCreateMode() == 1 ? height : PlayerUtil.getInstance().getEyeHeight(player), player.getLocation(), playerData);
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

    private GDClaim findNearbyClaim(Player player) {
        int maxDistance = GriefDefenderPlugin.getInstance().maxInspectionDistance;
        BlockRay blockRay = BlockRay.from(player).distanceLimit(maxDistance).build();
        GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        GDClaim claim = null;
        int count = 0;

        while (blockRay.hasNext()) {
            BlockRayHit blockRayHit = blockRay.next();
            Location location = blockRayHit.getLocation();
            claim = this.dataStore.getClaimAt(location);
            if (claim != null && !claim.isWilderness() && (playerData.visualBlocks == null || (claim.getUniqueId() != playerData.visualClaimId))) {
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
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_TOO_FAR));
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
                    "block",  NMSUtil.getInstance().getMaterialKey(block.getState().getType())));
            GriefDefenderPlugin.sendClaimDenyMessage(claim, player, message);
        } else {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_INTERACT_ITEM_BLOCK,
                    ImmutableMap.of(
                    "item", NMSUtil.getInstance().getMaterialKey(playerItem.getType()),
                    "block", NMSUtil.getInstance().getMaterialKey(block.getState().getType())));
            GriefDefenderPlugin.sendClaimDenyMessage(claim, player, message);
        }
    }
}