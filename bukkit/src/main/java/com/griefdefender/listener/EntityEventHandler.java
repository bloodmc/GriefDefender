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

import com.google.common.reflect.TypeToken;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GDTimings;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.TrustType;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.flag.Flags;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.internal.tracking.EntityTracker;
import com.griefdefender.internal.tracking.entity.GDEntity;
import com.griefdefender.internal.util.NMSUtil;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.permission.flag.GDFlags;
import com.griefdefender.storage.BaseStorage;
import com.griefdefender.util.CauseContextHelper;
import com.griefdefender.util.PermissionUtil;
import com.griefdefender.util.PlayerUtil;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Tameable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class EntityEventHandler implements Listener {

    // convenience reference for the singleton datastore
    private final BaseStorage baseStorage;

    public EntityEventHandler(BaseStorage dataStore) {
        this.baseStorage = dataStore;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityBlockFormEvent(EntityBlockFormEvent event) {
        CommonBlockEventHandler.getInstance().handleBlockPlace(event, event.getEntity(), event.getBlock());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityBreakDoorEvent(EntityBreakDoorEvent event) {
        CommonBlockEventHandler.getInstance().handleBlockBreak(event, event.getEntity(), event.getBlock());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityChangeBlockEvent(EntityChangeBlockEvent event) {
        if (!GDFlags.BLOCK_BREAK) {
            return;
        }

        final Block block = event.getBlock();
        if (block.isEmpty()) {
            return;
        }
        final World world = event.getBlock().getWorld();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUID())) {
            return;
        }

        final Location location = block.getLocation();
        final GDClaim targetClaim = this.baseStorage.getClaimAt(location);
        if (targetClaim.isWilderness()) {
            return;
        }

        final Entity source = event.getEntity();
        GDPermissionUser user = null;
        if (source instanceof Tameable) {
            final UUID uuid = NMSUtil.getInstance().getTameableOwnerUUID(source);
            if (uuid != null) {
                user = PermissionHolderCache.getInstance().getOrCreateUser(uuid);
            }
        }
        if (user == null) {
            user = CauseContextHelper.getEventUser(event.getEntity().getLocation());
            if (user == null && source instanceof FallingBlock) {
                // always allow blocks to fall if no user found
                return;
            }
        }
        final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, GDPermissions.BLOCK_BREAK, event.getEntity(), event.getBlock(), user, TrustTypes.BUILDER, true);
        if (result == Tristate.FALSE) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityExplodeEvent(EntityExplodeEvent event) {
        final World world = event.getEntity().getLocation().getWorld();
        if (!GDFlags.EXPLOSION_BLOCK || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUID())) {
            return;
        }

        // check entity tracker
        final GDEntity gdEntity = EntityTracker.getCachedEntity(event.getEntity().getEntityId());
        GDPermissionUser user = null;
        if (gdEntity != null) {
            user = PermissionHolderCache.getInstance().getOrCreateUser(gdEntity.getOwnerUUID());
        } else {
           user = CauseContextHelper.getEventUser(event.getEntity().getLocation());
        }

        Entity source = event.getEntity();
        if (GriefDefenderPlugin.isSourceIdBlacklisted(Flags.EXPLOSION_BLOCK.toString(), source, world.getUID())) {
            return;
        }

        if (user == null && source instanceof TNTPrimed) {
            event.setCancelled(true);
            return;
        }

        GDTimings.EXPLOSION_EVENT.startTiming();
        GDClaim targetClaim = null;
        final List<Block> filteredLocations = new ArrayList<>();
        for (Block block : event.blockList()) {
            final Location location = block.getLocation();
            targetClaim =  GriefDefenderPlugin.getInstance().dataStore.getClaimAt(location, targetClaim);
            final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, GDPermissions.EXPLOSION_BLOCK, source, location.getBlock(), user, true);
            if (result == Tristate.FALSE) {
                // Avoid lagging server from large explosions.
                if (event.blockList().size() > 100) {
                    event.setCancelled(true);
                    break;
                }
                filteredLocations.add(block);
            }
        }

        if (event.isCancelled()) {
            event.blockList().clear();
        } else if (!filteredLocations.isEmpty()) {
            event.blockList().removeAll(filteredLocations);
        }
        GDTimings.EXPLOSION_EVENT.stopTiming();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onVehicleDamage(VehicleDamageEvent event) {
        GDTimings.ENTITY_DAMAGE_EVENT.startTiming();
        if (protectEntity(event, event.getAttacker(), (Entity) event.getVehicle())) {
            event.setCancelled(true);
        }
        GDTimings.ENTITY_DAMAGE_EVENT.stopTiming();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onVehicleDestroy(VehicleDestroyEvent event) {
        GDTimings.ENTITY_DAMAGE_EVENT.startTiming();
        if (protectEntity(event, event.getAttacker(), (Entity) event.getVehicle())) {
            event.setCancelled(true);
        }
        GDTimings.ENTITY_DAMAGE_EVENT.stopTiming();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityCombustByBlockEvent event) {
        GDTimings.ENTITY_DAMAGE_EVENT.startTiming();
        if (protectEntity(event, event.getCombuster(), event.getEntity())) {
            event.setCancelled(true);
        }
        GDTimings.ENTITY_DAMAGE_EVENT.stopTiming();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityCombustByEntityEvent event) {
        GDTimings.ENTITY_DAMAGE_EVENT.startTiming();
        if (protectEntity(event, event.getCombuster(), event.getEntity())) {
            event.setCancelled(true);
        }
        GDTimings.ENTITY_DAMAGE_EVENT.stopTiming();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageByBlockEvent event) {
        GDTimings.ENTITY_DAMAGE_EVENT.startTiming();
        if (protectEntity(event, event.getDamager(), event.getEntity())) {
            event.setCancelled(true);
        }
        GDTimings.ENTITY_DAMAGE_EVENT.stopTiming();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        /*if (event.getDamager() instanceof Projectile) {
            return;
        }*/
        GDTimings.ENTITY_DAMAGE_EVENT.startTiming();
        if (event.getDamager() instanceof Player) {
            final Player player = (Player) event.getDamager();
            GDCauseStackManager.getInstance().pushCause(player);
            final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
            // check give pet
            if (playerData.petRecipientUniqueId != null) {
                // cancel
                playerData.petRecipientUniqueId = null;
                GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().COMMAND_PET_TRANSFER_CANCEL);
                event.setCancelled(true);
                return;
            }
        }

        if (protectEntity(event, event.getDamager(), event.getEntity())) {
            event.setCancelled(true);
        }
        GDTimings.ENTITY_DAMAGE_EVENT.stopTiming();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageEvent event) {
        GDTimings.ENTITY_DAMAGE_EVENT.startTiming();
        if (protectEntity(event, event.getCause(), event.getEntity())) {
            event.setCancelled(true);
        }
        GDTimings.ENTITY_DAMAGE_EVENT.stopTiming();
    }

    public boolean protectEntity(Event event, Object source, Entity targetEntity) {
        if (!GDFlags.ENTITY_DAMAGE || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(targetEntity.getWorld().getUID())) {
            return false;
        }
        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.ENTITY_DAMAGE.getName(), targetEntity, targetEntity.getWorld().getUID())) {
            return false;
        }

        final GDClaim claim = this.baseStorage.getClaimAt(targetEntity.getLocation());
        if (source instanceof Player && targetEntity instanceof Player) {
            if (!claim.isPvpEnabled()) {
                return true;
            }
        }
        String permission = GDPermissions.ENTITY_DAMAGE;
        ProjectileSource projectileSource = null;
        UUID owner = source instanceof Player ? ((Player) source).getUniqueId() : null;
        if (owner == null && source instanceof Tameable) {
            owner = NMSUtil.getInstance().getTameableOwnerUUID((Entity) source);
        } else if (source instanceof Projectile) {
            projectileSource = ((Projectile) source).getShooter();
            if (projectileSource != null && projectileSource instanceof OfflinePlayer) {
                owner = ((OfflinePlayer) projectileSource).getUniqueId();
            }
            permission = GDPermissions.PROJECTILE_IMPACT_ENTITY;
        } else if (source instanceof DamageCause) {
            if (targetEntity instanceof Player) {
                owner = ((Player) targetEntity).getUniqueId();
            }
        }

        if (source != null && source instanceof Player) {
            GDCauseStackManager.getInstance().pushCause(source);
        }
        final GDPermissionUser user = owner != null ? PermissionHolderCache.getInstance().getOrCreateUser(owner) : CauseContextHelper.getEventUser(null);
        if (GriefDefenderPlugin.isSourceIdBlacklisted(Flags.ENTITY_DAMAGE.getName(), source, targetEntity.getWorld().getUID())) {
            return false;
        }

        GDPlayerData playerData = null;
        if (user != null) {
            playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(targetEntity.getWorld(), user.getUniqueId());
            if (source instanceof DamageCause && ((DamageCause) source) == DamageCause.FALL) {
                if (playerData.ignoreFallDamage) {
                    playerData.ignoreFallDamage = false;
                    return true;
                }
            }
            if (NMSUtil.getInstance().isRaidActive(targetEntity)) {
                final Boolean result = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Boolean.class), user, Options.RAID, claim);
                final Set<Context> contexts = new HashSet<>();
                contexts.add(claim.getContext());
                if (result != null && !result) {
                    NMSUtil.getInstance().stopRaid(targetEntity);
                    return true;
                }
            }
        }
        if (!GriefDefenderPlugin.isEntityProtected(targetEntity)) {
            return false;
        }

        final TrustType trustType = TrustTypes.BUILDER;
        if (GDPermissionManager.getInstance().getFinalPermission(event, targetEntity.getLocation(), claim, permission, source, targetEntity, user, trustType, true) == Tristate.FALSE) {
            return true;
        }

        // allow trusted users to attack entities within claim
        if (!(targetEntity instanceof Player) && user != null && claim.isUserTrusted(user, TrustTypes.ACCESSOR)) {
            return false;
        }

        // Protect owned entities anywhere in world
        if (targetEntity instanceof Tameable) {
            final UUID targetUniqueId = NMSUtil.getInstance().getTameableOwnerUUID(targetEntity);
            if (user != null && user.getUniqueId().equals(targetUniqueId)) {
                // allow owners to attack entities they own
                return false;
            }

            if (GDPermissionManager.getInstance().getFinalPermission(event, targetEntity.getLocation(), claim, permission, source, targetEntity, user, trustType, true) == Tristate.FALSE) {
                return true;
            }

            return false;
        }

        if (GDPermissionManager.getInstance().getFinalPermission(event, targetEntity.getLocation(), claim, permission, source, targetEntity, user, trustType, true) == Tristate.FALSE) {
            return true;
        }

        return false;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        handleEntitySpawn(event, event.getSpawnReason());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSpawnerSpawn(SpawnerSpawnEvent event) {
        handleEntitySpawn(event, event.getSpawner());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntitySpawn(EntitySpawnEvent event) {
        final Object source = GDCauseStackManager.getInstance().getCurrentCause().root();
        if (source != null && source instanceof GDPermissionUser) {
            final GDPermissionUser user = (GDPermissionUser) source;
            final GDEntity gdEntity = new GDEntity(event.getEntity().getEntityId());
            gdEntity.setOwnerUUID(user.getUniqueId());
            gdEntity.setNotifierUUID(user.getUniqueId());
            EntityTracker.addTempEntity(gdEntity);
        }
        handleEntitySpawn(event, null);
    }

    //@EventHandler(priority = EventPriority.LOWEST)
    //public void onEntitySpawn(EntitySpawnEvent event) {
    //}

    public void handleEntitySpawn(EntitySpawnEvent event, Object source) {
        if (!GDFlags.ENTITY_SPAWN) {
            return;
        }

        final World world = event.getEntity().getWorld();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUID())) {
            return;
        }
        if (GriefDefenderPlugin.isSourceIdBlacklisted(Flags.ENTITY_SPAWN.getName(), source, world.getUID())) {
            return;
        }
        if (event.getEntity() instanceof FallingBlock) {
            return;
        }

        GDTimings.ENTITY_SPAWN_EVENT.startTiming();
        final Object root = GDCauseStackManager.getInstance().getCurrentCause().root();
        GDPermissionUser user = root instanceof GDPermissionUser ? (GDPermissionUser) root : null;
        if (user == null && source instanceof Player) {
            GDCauseStackManager.getInstance().pushCause(source);
        }
        Location sourceLocation = null;
        if (source == null) {
            source = GDCauseStackManager.getInstance().getCurrentCause().root();
        }
        if (source != null) {
            if (source instanceof CreatureSpawner) {
                sourceLocation = ((CreatureSpawner) source).getLocation();
                user = CauseContextHelper.getEventUser(sourceLocation);
            } else if (source instanceof Player) {
                user = PermissionHolderCache.getInstance().getOrCreateUser((Player) source);
            }
        }

        final Entity entity = event.getEntity();
        // Player drops are handled in PlayerDropItemEvent
        if (source instanceof GDPermissionUser && entity instanceof Item) {
            GDTimings.ENTITY_SPAWN_EVENT.stopTiming();
            return;
        }
        if (entity instanceof ExperienceOrb) {
            GDTimings.ENTITY_SPAWN_EVENT.stopTiming();
            return;
        }

        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.ENTITY_SPAWN.getName(), entity, world.getUID())) {
            GDTimings.ENTITY_SPAWN_EVENT.stopTiming();
            return;
        }

        final GDClaim targetClaim = GriefDefenderPlugin.getInstance().dataStore.getClaimAt(entity.getLocation());
        String permission = GDPermissions.ENTITY_SPAWN;

        if (entity instanceof Item) {
            if (user == null) {
                GDTimings.ENTITY_SPAWN_EVENT.stopTiming();
                return;
            }
            if (!GDFlags.ITEM_SPAWN) {
                GDTimings.ENTITY_SPAWN_EVENT.stopTiming();
                return;
            }
            if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.ITEM_SPAWN.getName(), entity, world.getUID())) {
                GDTimings.ENTITY_SPAWN_EVENT.stopTiming();
                return;
            }
            permission = GDPermissions.ITEM_SPAWN;
        }

        if (GDPermissionManager.getInstance().getFinalPermission(event, entity.getLocation(), targetClaim, permission, source, entity, user, TrustTypes.ACCESSOR, true) == Tristate.FALSE) {
            event.setCancelled(true);
        }

        GDTimings.ENTITY_SPAWN_EVENT.stopTiming();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityMount(VehicleEnterEvent event) {
        if (!GDFlags.ENTITY_RIDING) {
            return;
        }

        final Entity source = event.getEntered();
        final World world = source.getWorld();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUID())) {
            return;
        }
        if (GriefDefenderPlugin.isSourceIdBlacklisted(Flags.ENTITY_RIDING.getName(), source, world.getUID())) {
            return;
        }
        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.ENTITY_RIDING.getName(), event.getVehicle(), world.getUID())) {
            return;
        }

        GDTimings.ENTITY_MOUNT_EVENT.startTiming();
        Player player = source instanceof Player ? (Player) source : null;
        if (player != null) {
            GDCauseStackManager.getInstance().pushCause(player);
        }
        final Location location = event.getVehicle().getLocation();
        final GDClaim targetClaim = GriefDefenderPlugin.getInstance().dataStore.getClaimAt(location);

        if (GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, GDPermissions.ENTITY_RIDING, source, event.getVehicle(), player, TrustTypes.ACCESSOR, true) == Tristate.FALSE) {
            if (player != null) {
                PlayerUtil.getInstance().sendInteractEntityDenyMessage(targetClaim, player, null, event.getVehicle());
            }
            event.setCancelled(true);
        }

        GDTimings.ENTITY_MOUNT_EVENT.stopTiming();
    }
}
