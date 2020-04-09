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
import com.griefdefender.api.claim.TrustType;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.flag.Flag;
import com.griefdefender.api.permission.flag.Flags;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.claim.GDClaimManager;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.internal.tracking.EntityTracker;
import com.griefdefender.internal.tracking.entity.GDEntity;
import com.griefdefender.internal.util.NMSUtil;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.flag.GDFlags;
import com.griefdefender.storage.BaseStorage;
import com.griefdefender.util.CauseContextHelper;
import com.griefdefender.util.PlayerUtil;
import net.kyori.text.Component;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Tameable;
import org.bukkit.event.Cancellable;
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
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.SlimeSplitEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.time.Instant;
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
            final GDEntity gdEntity = EntityTracker.getCachedEntity(event.getEntity().getEntityId());
            if (gdEntity != null) {
                user = PermissionHolderCache.getInstance().getOrCreateUser(gdEntity.getOwnerUUID());
            }
            if (user == null && source instanceof FallingBlock) {
                // always allow blocks to fall if no user found
                return;
            }
        }
        final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, Flags.BLOCK_BREAK, event.getEntity(), event.getBlock(), user, TrustTypes.BUILDER, true);
        if (result == Tristate.FALSE) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onExplosionPrimeEvent(ExplosionPrimeEvent event) {
        final World world = event.getEntity().getLocation().getWorld();
        if (!GDFlags.EXPLOSION_BLOCK && !GDFlags.EXPLOSION_ENTITY) {
            return;
        }
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUID())) {
            return;
        }

        GDCauseStackManager.getInstance().pushCause(event.getEntity());
        GDTimings.ENTITY_EXPLOSION_PRE_EVENT.startTiming();
        final GDEntity gdEntity = EntityTracker.getCachedEntity(event.getEntity().getEntityId());
        GDPermissionUser user = null;
        if (gdEntity != null) {
            user = PermissionHolderCache.getInstance().getOrCreateUser(gdEntity.getOwnerUUID());
        } else {
           user = CauseContextHelper.getEventUser(event.getEntity().getLocation());
        }

        final Location location = event.getEntity().getLocation();
        final GDClaim radiusClaim = NMSUtil.getInstance().createClaimFromCenter(location, event.getRadius());
        final GDClaimManager claimManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(location.getWorld().getUID());
        final Set<Claim> surroundingClaims = claimManager.findOverlappingClaims(radiusClaim);
        if (surroundingClaims.size() == 0) {
            GDTimings.ENTITY_EXPLOSION_PRE_EVENT.stopTiming();
            return;
        }
        for (Claim claim : surroundingClaims) {
            // Use any location for permission check
            final Vector3i pos = claim.getLesserBoundaryCorner();
            Location targetLocation = new Location(location.getWorld(), pos.getX(), pos.getY(), pos.getZ());
            Tristate blockResult = GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.EXPLOSION_BLOCK, event.getEntity(), targetLocation, user, true);
            if (blockResult == Tristate.FALSE) {
                // Check explosion entity
                if (GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.EXPLOSION_ENTITY, event.getEntity(), targetLocation, user, true) == Tristate.FALSE) {
                    event.setCancelled(true);
                    break;
                }
            }
        }
        GDTimings.ENTITY_EXPLOSION_PRE_EVENT.stopTiming();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityExplodeEvent(EntityExplodeEvent event) {
        final World world = event.getEntity().getLocation().getWorld();
        if (!GDFlags.EXPLOSION_BLOCK || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUID())) {
            return;
        }

        GDCauseStackManager.getInstance().pushCause(event.getEntity());
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
            final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, Flags.EXPLOSION_BLOCK, source, location.getBlock(), user, true);
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
    public void onVehicleMove(VehicleMoveEvent event) {
        CommonEntityEventHandler.getInstance().onEntityMove(event, event.getFrom(), event.getTo(), event.getVehicle());
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
    public void onHangingBreakEvent(HangingBreakByEntityEvent event) {
        GDTimings.ENTITY_DAMAGE_EVENT.startTiming();
        if (protectEntity(event, event.getRemover(), event.getEntity())) {
            event.setCancelled(true);
        }
        GDTimings.ENTITY_DAMAGE_EVENT.stopTiming();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
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
            if (event.getEntity() instanceof Tameable) {
                final UUID uuid = NMSUtil.getInstance().getTameableOwnerUUID(event.getEntity());
                if (uuid != null) {
                    // always allow owner to damage their pets
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
        }

        GDTimings.ENTITY_DAMAGE_EVENT.startTiming();
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
        // Ignore entity items
        if (targetEntity instanceof Item) {
            return false;
        }

        final DamageCause damageCause = source instanceof DamageCause ? (DamageCause) source : null;
        if (damageCause != null) {
            final Object cause = GDCauseStackManager.getInstance().getCurrentCause().root();
            if (cause != GriefDefenderPlugin.getInstance()) {
                source = cause;

            }
        }

        final GDClaim claim = this.baseStorage.getClaimAt(targetEntity.getLocation());
        final GDPermissionUser targetUser = targetEntity instanceof Player ? PermissionHolderCache.getInstance().getOrCreateUser((Player) targetEntity) : null;
        GDPermissionUser user = null;
        if (source instanceof Player && targetUser != null) {
            user = PermissionHolderCache.getInstance().getOrCreateUser(((Player) source).getUniqueId());
            if (user.getOnlinePlayer() != null && targetUser.getOnlinePlayer() != null) {
                return this.getPvpProtectResult(event, claim, user, targetUser);
            }
        }

        Flag flag = Flags.ENTITY_DAMAGE;
        ProjectileSource projectileSource = null;
        UUID owner = source instanceof Player ? ((Player) source).getUniqueId() : null;
        if (owner == null && source instanceof Tameable) {
            owner = NMSUtil.getInstance().getTameableOwnerUUID((Entity) source);
        } else if (source instanceof Projectile) {
            projectileSource = ((Projectile) source).getShooter();
            if (projectileSource != null && projectileSource instanceof OfflinePlayer) {
                owner = ((OfflinePlayer) projectileSource).getUniqueId();
            }
            flag = Flags.PROJECTILE_IMPACT_ENTITY;
        } else if (source instanceof DamageCause) {
            if (targetEntity instanceof Player) {
                owner = ((Player) targetEntity).getUniqueId();
            }
        }
        if (owner != null && targetUser != null && !owner.equals(targetUser.getUniqueId())) {
            final GDPermissionUser sourceUser = PermissionHolderCache.getInstance().getOrCreateUser(owner);
            if (sourceUser.getOnlinePlayer() != null && targetUser.getOnlinePlayer() != null) {
                return this.getPvpProtectResult(event, claim, sourceUser, targetUser);
            }
        }

        if (user == null) {
            user = owner != null ? PermissionHolderCache.getInstance().getOrCreateUser(owner) : CauseContextHelper.getEventUser(null);
            if (user != null) {
                GDCauseStackManager.getInstance().pushCause(user);
            }
        }
        if (GriefDefenderPlugin.isSourceIdBlacklisted(Flags.ENTITY_DAMAGE.getName(), source, targetEntity.getWorld().getUID())) {
            return false;
        }

        if (source instanceof Creeper || source instanceof TNTPrimed || (damageCause != null && damageCause == DamageCause.ENTITY_EXPLOSION)) {
            final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, targetEntity.getLocation(), claim, Flags.EXPLOSION_ENTITY, source, targetEntity, user, true);
            if (result == Tristate.FALSE) {
                return true;
            }

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
        if (GDPermissionManager.getInstance().getFinalPermission(event, targetEntity.getLocation(), claim, flag, source, targetEntity, user, trustType, true) == Tristate.FALSE) {
            if (source != null && source instanceof Player) {
                final Player player = (Player) source;
                CommonEntityEventHandler.getInstance().sendInteractEntityDenyMessage(NMSUtil.getInstance().getActiveItem(player), targetEntity, claim, player);
            }
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

            if (GDPermissionManager.getInstance().getFinalPermission(event, targetEntity.getLocation(), claim, flag, source, targetEntity, user, trustType, true) == Tristate.FALSE) {
                return true;
            }

            return false;
        }

        if (GDPermissionManager.getInstance().getFinalPermission(event, targetEntity.getLocation(), claim, flag, source, targetEntity, user, trustType, true) == Tristate.FALSE) {
            return true;
        }

        return false;
    }

    private boolean getPvpProtectResult(Event event, GDClaim claim, GDPermissionUser source, GDPermissionUser target) {
        final Player sourcePlayer = source.getOnlinePlayer();
        final Player targetPlayer = target.getOnlinePlayer();
        final boolean sourceInCombat = source.getInternalPlayerData().inPvpCombat();
        final boolean targetInCombat = target.getInternalPlayerData().inPvpCombat();
        // Always check if source or target is in combat and if so allow PvP
        // This prevents a player from moving to another claim where PvP is disabled
        if (sourceInCombat && targetInCombat && (source.getInternalPlayerData().lastPvpTimestamp == target.getInternalPlayerData().lastPvpTimestamp)) {
            source.getInternalPlayerData().lastPvpTimestamp = Instant.now();
            target.getInternalPlayerData().lastPvpTimestamp = Instant.now();
            return false;
        }

        // Check target claim
        if (!claim.isPvpEnabled()) {
            GriefDefenderPlugin.sendMessage(sourcePlayer, MessageCache.getInstance().PVP_CLAIM_NOT_ALLOWED);
            return true;
        }
        // Check source claim
        final GDClaim sourceClaim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(source.getInternalPlayerData(), sourcePlayer.getLocation());
        if (!sourceClaim.isPvpEnabled()) {
            GriefDefenderPlugin.sendMessage(sourcePlayer, MessageCache.getInstance().PVP_CLAIM_NOT_ALLOWED);
            return true;
        }

        // Check flags
        Tristate sourceResult = GDPermissionManager.getInstance().getFinalPermission(event, targetPlayer.getLocation(), claim, Flags.ENTITY_DAMAGE, sourcePlayer, targetPlayer, sourcePlayer, true);
        Tristate targetResult = GDPermissionManager.getInstance().getFinalPermission(event, sourcePlayer.getLocation(), claim, Flags.ENTITY_DAMAGE, targetPlayer, sourcePlayer, targetPlayer, true);
        if (sourceResult == Tristate.FALSE) {
            GriefDefenderPlugin.sendMessage(sourcePlayer, MessageCache.getInstance().PVP_SOURCE_NOT_ALLOWED);
            return true;
        }
        if (targetResult == Tristate.FALSE) {
            GriefDefenderPlugin.sendMessage(sourcePlayer, MessageCache.getInstance().PVP_TARGET_NOT_ALLOWED);
            return true;
        }

        // Check options
        sourceResult = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Tristate.class), source, Options.PVP, claim);
        targetResult = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Tristate.class), target, Options.PVP, claim);
        if (sourceResult == Tristate.UNDEFINED) {
            sourceResult = Tristate.fromBoolean(claim.getWorld().getPVP());
        }
        if (targetResult == Tristate.UNDEFINED) {
            targetResult = Tristate.fromBoolean(claim.getWorld().getPVP());
        }
        if (sourceResult == Tristate.FALSE) {
            GriefDefenderPlugin.sendMessage(sourcePlayer, MessageCache.getInstance().PVP_SOURCE_NOT_ALLOWED);
            return true;
        }
        if (targetResult == Tristate.FALSE) {
            GriefDefenderPlugin.sendMessage(sourcePlayer, MessageCache.getInstance().PVP_TARGET_NOT_ALLOWED);
            return true;
        }

        final Instant now = Instant.now();
        source.getInternalPlayerData().lastPvpTimestamp = now;
        target.getInternalPlayerData().lastPvpTimestamp = now;
        return false;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        handleEntitySpawn(event, event.getSpawnReason(), event.getEntity());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSpawnerSpawn(SpawnerSpawnEvent event) {
        handleEntitySpawn(event, event.getSpawner(), event.getEntity());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSlimeSplitEvent(SlimeSplitEvent event) {
        handleEntitySpawn(event, event.getEntity(), event.getEntity());
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
        handleEntitySpawn(event, null, event.getEntity());
    }

    //@EventHandler(priority = EventPriority.LOWEST)
    //public void onEntitySpawn(EntitySpawnEvent event) {
    //}

    public void handleEntitySpawn(Event event, Object source, Entity entity) {
        if (!GDFlags.ENTITY_SPAWN) {
            return;
        }

        final World world = entity.getWorld();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUID())) {
            return;
        }
        if (GriefDefenderPlugin.isSourceIdBlacklisted(Flags.ENTITY_SPAWN.getName(), source, world.getUID())) {
            return;
        }
        if (entity instanceof FallingBlock) {
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
        Flag flag = Flags.ENTITY_SPAWN;

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
            flag = Flags.ITEM_SPAWN;
        }

        if (GDPermissionManager.getInstance().getFinalPermission(event, entity.getLocation(), targetClaim, flag, source, entity, user, TrustTypes.ACCESSOR, true) == Tristate.FALSE) {
            ((Cancellable) event).setCancelled(true);
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

        if (GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, Flags.ENTITY_RIDING, source, event.getVehicle(), player, TrustTypes.ACCESSOR, true) == Tristate.FALSE) {
            if (player != null) {
                PlayerUtil.getInstance().sendInteractEntityDenyMessage(targetClaim, player, null, event.getVehicle());
            }
            event.setCancelled(true);
        }

        GDTimings.ENTITY_MOUNT_EVENT.stopTiming();
    }
}
