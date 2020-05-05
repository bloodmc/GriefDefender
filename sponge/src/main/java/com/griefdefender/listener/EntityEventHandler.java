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

import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GDTimings;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.TrustType;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.api.permission.flag.Flag;
import com.griefdefender.api.permission.flag.Flags;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.claim.GDClaimManager;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.internal.util.NMSUtil;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.flag.GDFlags;
import com.griefdefender.storage.BaseStorage;
import com.griefdefender.util.CauseContextHelper;
import net.kyori.text.Component;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.ExperienceOrb;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.hanging.ItemFrame;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.cause.entity.damage.DamageTypes;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.IndirectEntityDamageSource;
import org.spongepowered.api.event.cause.entity.teleport.TeleportType;
import org.spongepowered.api.event.cause.entity.teleport.TeleportTypes;
import org.spongepowered.api.event.entity.AttackEntityEvent;
import org.spongepowered.api.event.entity.CollideEntityEvent;
import org.spongepowered.api.event.entity.ConstructEntityEvent;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.entity.RideEntityEvent;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.explosion.Explosion;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.common.SpongeImpl;

import java.time.Instant;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

//handles events related to entities
public class EntityEventHandler {

    private int lastConstructEntityTick = -1;
    private boolean lastConstructEntityCancelled = false;

    // convenience reference for the singleton datastore
    private final BaseStorage dataStore;

    public EntityEventHandler(BaseStorage dataStore) {
        this.dataStore = dataStore;
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onEntityExplosionPre(ExplosionEvent.Pre event) {
        if (!GDFlags.EXPLOSION_ENTITY || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(event.getTargetWorld().getUniqueId())) {
            return;
        }
        if (GriefDefenderPlugin.isSourceIdBlacklisted(Flags.EXPLOSION_ENTITY.getName(), event.getSource(), event.getTargetWorld().getProperties())) {
            return;
        }

        GDTimings.ENTITY_EXPLOSION_PRE_EVENT.startTimingIfSync();
        Location<World> location = event.getExplosion().getLocation();
        GDClaim claim =  GriefDefenderPlugin.getInstance().dataStore.getClaimAt(location);
        User user = CauseContextHelper.getEventUser(event);
        Object source = event.getSource();
        if (source instanceof Explosion) {
            final Explosion explosion = (Explosion) source;
            if (explosion.getSourceExplosive().isPresent()) {
                source = explosion.getSourceExplosive().get();
            } else {
                Entity exploder = event.getCause().first(Entity.class).orElse(null);
                if (exploder != null) {
                    source = exploder;
                }
            }
        }

        /*if (location.getPosition().getY() > ((net.minecraft.world.World) location.getExtent()).getSeaLevel() && !GriefDefenderPlugin.getActiveConfig(location.getExtent().getUniqueId()).getConfig().claim.explosionSurface) {
            event.setCancelled(true);
            return;
        }*/

        Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.EXPLOSION_ENTITY, source, location.getBlock(), user, true);

        if(result == Tristate.FALSE) {
            event.setCancelled(true);
            GDTimings.ENTITY_EXPLOSION_PRE_EVENT.stopTimingIfSync();
            return;
        }

        GDTimings.ENTITY_EXPLOSION_PRE_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onEntityExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!GDFlags.EXPLOSION_ENTITY || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(event.getTargetWorld().getUniqueId())) {
            return;
        }
        if (GriefDefenderPlugin.isSourceIdBlacklisted(Flags.EXPLOSION_ENTITY.getName(), event.getSource(), event.getTargetWorld().getProperties())) {
            return;
        }

        GDTimings.ENTITY_EXPLOSION_DETONATE_EVENT.startTimingIfSync();
        final User user = CauseContextHelper.getEventUser(event);
        Iterator<Entity> iterator = event.getEntities().iterator();
        GDClaim targetClaim = null;
        Object source = event.getSource();
        if (source instanceof Explosion) {
            final Explosion explosion = (Explosion) source;
            if (explosion.getSourceExplosive().isPresent()) {
                source = explosion.getSourceExplosive().get();
            } else {
                Entity exploder = event.getCause().first(Entity.class).orElse(null);
                if (exploder != null) {
                    source = exploder;
                }
            }
        }

        while (iterator.hasNext()) {
            Entity entity = iterator.next();
            targetClaim =  GriefDefenderPlugin.getInstance().dataStore.getClaimAt(entity.getLocation(), targetClaim);
            if (GDPermissionManager.getInstance().getFinalPermission(event, entity.getLocation(), targetClaim, Flags.ENTITY_DAMAGE, source, entity, user) == Tristate.FALSE) {
                iterator.remove();
            }
        }
        GDTimings.ENTITY_EXPLOSION_DETONATE_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onEntityConstruct(ConstructEntityEvent.Pre event, @Root Object source) {
        lastConstructEntityTick = Sponge.getServer().getRunningTimeTicks();
        if (true || source instanceof ConsoleSource || !GDFlags.ENTITY_SPAWN) {
            return;
        }

        final World world = event.getTransform().getExtent();
        final String entityTypeId = event.getTargetType().getId();
        if (entityTypeId.equals(EntityTypes.EXPERIENCE_ORB.getId())) {
            return;
        }

        final Location<World> location = event.getTransform().getLocation();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUniqueId())) {
            return;
        }
        if (GriefDefenderPlugin.isSourceIdBlacklisted(Flags.ENTITY_SPAWN.getName(), source, world.getProperties())) {
            return;
        }
        if (GriefDefenderPlugin.isSourceIdBlacklisted(Flags.ENTITY_CHUNK_SPAWN.getName(), source, world.getProperties())) {
            return;
        }

        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.ENTITY_SPAWN.getName(), entityTypeId, world.getProperties())) {
            return;
        }

        GDTimings.ENTITY_SPAWN_PRE_EVENT.startTimingIfSync();
        final User user = CauseContextHelper.getEventUser(event);
        final GDClaim targetClaim = GriefDefenderPlugin.getInstance().dataStore.getClaimAt(location);
        if (targetClaim.isUserTrusted(user, TrustTypes.BUILDER)) {
            GDTimings.ENTITY_SPAWN_PRE_EVENT.stopTimingIfSync();
            return;
        }

        Flag flag = Flags.ENTITY_SPAWN;
        if (event.getTargetType() == EntityTypes.ITEM) {
            if (user == null) {
                GDTimings.ENTITY_SPAWN_PRE_EVENT.stopTimingIfSync();
                return;
            }
            if (!GDFlags.ITEM_SPAWN) {
                GDTimings.ENTITY_SPAWN_PRE_EVENT.stopTimingIfSync();
                return;
            }
            if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.ITEM_SPAWN.getName(), entityTypeId, world.getProperties())) {
                GDTimings.ENTITY_SPAWN_PRE_EVENT.stopTimingIfSync();
                return;
            }

            flag = Flags.ITEM_SPAWN;
            if (source instanceof BlockSnapshot) {
                final BlockSnapshot block = (BlockSnapshot) source;
                final Location<World> blockLocation = block.getLocation().orElse(null);
                if (blockLocation != null) {
                    if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.BLOCK_BREAK.getName(), block, world.getProperties())) {
                        GDTimings.ENTITY_SPAWN_PRE_EVENT.stopTimingIfSync();
                        return;
                    }
                    final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, Flags.BLOCK_BREAK, source, block, user, true);
                    if (result != Tristate.UNDEFINED) {
                        if (result == Tristate.TRUE) {
                            // Check if item drop is allowed
                            if (GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, flag, source, entityTypeId, user, true) == Tristate.FALSE) {
                                event.setCancelled(true);
                            }
                            GDTimings.ENTITY_SPAWN_PRE_EVENT.stopTimingIfSync();
                            return;
                        }
                        event.setCancelled(true);
                        GDTimings.ENTITY_SPAWN_PRE_EVENT.stopTimingIfSync();
                        return;
                    }
                }
            }
        }
        if (GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, flag, source, entityTypeId, user, true) == Tristate.FALSE) {
            event.setCancelled(true);
        }
        GDTimings.ENTITY_SPAWN_PRE_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onEntitySpawn(SpawnEntityEvent event) {
        // For whatever reason, some custom data seems to be triggering spawn events during shutdown
        if (!SpongeImpl.getServer().isServerRunning()) {
            return;
        }

        Object source = event.getSource();
        if (source instanceof ConsoleSource || !GDFlags.ENTITY_SPAWN || event.getEntities().isEmpty()) {
            return;
        }

        // If root cause is damage source, look for target as that should be passed instead
        // Ex. Entity dies and drops an item would be after EntityDamageSource
        if (source instanceof DamageSource) {
            final Object target = event.getCause().after(DamageSource.class).orElse(null);
            if (target != null) {
                source = target;
            }
        }

        final boolean isChunkSpawn = event instanceof SpawnEntityEvent.ChunkLoad;
        if (isChunkSpawn && !GDFlags.ENTITY_CHUNK_SPAWN) {
            return;
        }
        if (event instanceof DropItemEvent) {
            if (!GDFlags.ITEM_DROP) {
                return;
            }
            // only handle item spawns from non-living
            if (source instanceof Living || NMSUtil.getInstance().containsContainerPlayer(event.getCause())) {
                return;
            }
        }

        final World world = event.getEntities().get(0).getWorld();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUniqueId())) {
            return;
        }
        if (GriefDefenderPlugin.isSourceIdBlacklisted(Flags.ENTITY_SPAWN.getName(), source, world.getProperties())) {
            return;
        }
        if (isChunkSpawn && GriefDefenderPlugin.isSourceIdBlacklisted(Flags.ENTITY_CHUNK_SPAWN.getName(), source, world.getProperties())) {
            return;
        }

        GDTimings.ENTITY_SPAWN_EVENT.startTimingIfSync();
        final User user = CauseContextHelper.getEventUser(event);
        final Object actualSource = source;
        event.filterEntities(new Predicate<Entity>() {
            GDClaim targetClaim = null;

            @Override
            public boolean test(Entity entity) {
                if (entity instanceof ExperienceOrb) {
                    return true;
                }

                if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.ENTITY_SPAWN.getName(), entity, world.getProperties())) {
                    return true;
                }

                targetClaim = GriefDefenderPlugin.getInstance().dataStore.getClaimAt(entity.getLocation(), targetClaim);
                if (targetClaim == null) {
                    return true;
                }

                Flag flag = Flags.ENTITY_SPAWN;
                if (isChunkSpawn) {
                    if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.ENTITY_CHUNK_SPAWN.getName(), entity, world.getProperties())) {
                        return true;
                    }
                    // Always allow item frames in chunks to spawn
                    if (entity instanceof ItemFrame) {
                        return true;
                    }
                    flag = Flags.ENTITY_CHUNK_SPAWN;
                }

                if (!isChunkSpawn && entity instanceof Item) {
                    if (user == null) {
                        return true;
                    }
                    if (!GDFlags.ITEM_SPAWN) {
                        return true;
                    }
                    if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.ITEM_SPAWN.getName(), entity, world.getProperties())) {
                        return true;
                    }
                    flag = Flags.ITEM_SPAWN;
                    if (actualSource instanceof BlockSnapshot) {
                        final BlockSnapshot block = (BlockSnapshot) actualSource;
                        final Location<World> location = block.getLocation().orElse(null);
                        if (location != null) {
                            if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.BLOCK_BREAK.getName(), block, world.getProperties())) {
                                return true;
                            }
                            final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, Flags.BLOCK_BREAK, actualSource, block, user, TrustTypes.ACCESSOR, true);
                            if (result != Tristate.UNDEFINED) {
                                if (result == Tristate.TRUE) {
                                    // Check if item drop is allowed
                                    if (GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, flag, actualSource, entity, user, TrustTypes.ACCESSOR, true) == Tristate.FALSE) {
                                        return false;
                                    }
                                    return true;
                                }
                                return false;
                            }
                        }
                    }
                }

                if (GDPermissionManager.getInstance().getFinalPermission(event, entity.getLocation(), targetClaim, flag, actualSource, entity, user, TrustTypes.ACCESSOR, true) == Tristate.FALSE) {
                    return false;
                }
                
                return true;
            }
        });

        GDTimings.ENTITY_SPAWN_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onEntityAttack(AttackEntityEvent event, @First DamageSource damageSource) {
        GDTimings.ENTITY_ATTACK_EVENT.startTimingIfSync();
        if (protectEntity(event, event.getTargetEntity(), event.getCause(), damageSource)) {
            event.setCancelled(true);
        }
        GDTimings.ENTITY_ATTACK_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onEntityDamage(DamageEntityEvent event, @First DamageSource damageSource) {
        GDTimings.ENTITY_DAMAGE_EVENT.startTimingIfSync();
        if (protectEntity(event, event.getTargetEntity(), event.getCause(), damageSource)) {
            event.setCancelled(true);
        }

        GDTimings.ENTITY_DAMAGE_EVENT.stopTimingIfSync();
    }

    public boolean protectEntity(Event event, Entity targetEntity, Cause cause, DamageSource damageSource) {
        if (damageSource.getType() == DamageTypes.SUFFOCATE) {
            // Ignore as this can only happen if an entity is stuck in wall
            return false;
        }
        if (!GDFlags.ENTITY_DAMAGE || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(targetEntity.getWorld().getUniqueId())) {
            return false;
        }
        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.ENTITY_DAMAGE.getName(), targetEntity, targetEntity.getWorld().getProperties())) {
            return false;
        }

        User user = CauseContextHelper.getEventUser(event);
        Player player = cause.first(Player.class).orElse(null);
        Object source = damageSource;
        EntityDamageSource entityDamageSource = null;
        final TileEntity tileEntity = cause.first(TileEntity.class).orElse(null);
        // TE takes priority over entity damage sources
        if (tileEntity != null) {
            source = tileEntity;
        } else if (damageSource instanceof EntityDamageSource) {
            entityDamageSource = (EntityDamageSource) damageSource;
            source = entityDamageSource.getSource();
            if (entityDamageSource instanceof IndirectEntityDamageSource) {
                final Entity indirectSource = ((IndirectEntityDamageSource) entityDamageSource).getIndirectSource();
                if (indirectSource != null) {
                    source = indirectSource;
                }
            }
            if (source instanceof Player) {
                if (user == null) {
                    user = (User) source;
                }
                if (player == null) {
                    player = (Player) source;
                }

                final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
                // check give pet
                if (playerData.petRecipientUniqueId != null) {
                    // cancel
                    playerData.petRecipientUniqueId = null;
                    GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().COMMAND_PET_TRANSFER_CANCEL);
                    return true;
                }
                if (targetEntity instanceof Living && targetEntity.get(Keys.TAMED_OWNER).isPresent()) {
                    final UUID ownerID = targetEntity.get(Keys.TAMED_OWNER).get().orElse(null);
                    if (ownerID != null) {
                        // always allow owner to interact with their pets
                        if (player.getUniqueId().equals(ownerID)) {
                            return false;
                        }
                        // If pet protection is enabled, deny the interaction
                        if (GriefDefenderPlugin.getActiveConfig(player.getWorld().getProperties()).getConfig().claim.protectTamedEntities) {
                            final GDPermissionUser owner = PermissionHolderCache.getInstance().getOrCreateUser(ownerID);
                            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_PROTECTED_ENTITY,
                                    ImmutableMap.of(
                                    "player", owner.getName()));
                            GriefDefenderPlugin.sendMessage(player, message);
                            return true;
                        }
                    }
                }
            }
        }

        if (GriefDefenderPlugin.isSourceIdBlacklisted(Flags.ENTITY_DAMAGE.getName(), source, targetEntity.getWorld().getProperties())) {
            return false;
        }

        GDPlayerData playerData = null;
        if (player != null) {
            playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(targetEntity.getWorld(), player.getUniqueId());
        }

        if (NMSUtil.getInstance().isEntityMonster(targetEntity)) {
            return false;
        }

        final GDClaim claim = this.dataStore.getClaimAt(targetEntity.getLocation(), playerData != null ? playerData.lastClaim.get() : null);
        final GDPermissionUser targetUser = targetEntity instanceof Player ? PermissionHolderCache.getInstance().getOrCreateUser((Player) targetEntity) : null;
        if (source instanceof Player && targetUser != null) {
            final GDPermissionUser sourceUser = PermissionHolderCache.getInstance().getOrCreateUser(((Player) source).getUniqueId());
            if (sourceUser.getOnlinePlayer() != null && targetUser.getOnlinePlayer() != null) {
                return this.getPvpProtectResult(event, claim, sourceUser, targetUser);
            }
        }

        final TrustType trustType = TrustTypes.BUILDER;
        if (GDPermissionManager.getInstance().getFinalPermission(event, targetEntity.getLocation(), claim, Flags.ENTITY_DAMAGE, source, targetEntity, user, trustType, true) == Tristate.FALSE) {
            return true;
        }

        // allow trusted users to attack entities within claim
        if (!(targetEntity instanceof Player) && claim.isUserTrusted(user, TrustTypes.ACCESSOR)) {
            return false;
        }

        // Protect owned entities anywhere in world
        if (entityDamageSource != null && !NMSUtil.getInstance().isEntityMonster(targetEntity)) {
            Tristate perm = Tristate.UNDEFINED;
            // Ignore PvP checks for owned entities
            if (!(source instanceof Player) && !(targetEntity instanceof Player)) {
                if (source instanceof User) {
                    User sourceUser = (User) source;
                    perm = GDPermissionManager.getInstance().getFinalPermission(event, targetEntity.getLocation(), claim, Flags.ENTITY_DAMAGE, source, targetEntity, sourceUser, trustType, true);
                    if (targetEntity instanceof Living && perm == Tristate.TRUE) {
                        return false;
                    }
                    Optional<UUID> creatorUuid = targetEntity.getCreator();
                    if (creatorUuid.isPresent()) {
                        Optional<User> creator = Sponge.getGame().getServiceManager().provide(UserStorageService.class).get().get(creatorUuid.get());
                        if (creator.isPresent() && !creator.get().getUniqueId().equals(sourceUser.getUniqueId())) {
                            return true;
                        }
                    } else if (sourceUser.getUniqueId().equals(claim.getOwnerUniqueId())) {
                        return true;
                    }
    
                    return false;
                } else {
                    if (targetEntity instanceof Player) {
                        if (NMSUtil.getInstance().isEntityMonster((Entity) source)) {
                            if (GDPermissionManager.getInstance().getFinalPermission(event, targetEntity.getLocation(), claim, Flags.ENTITY_DAMAGE, source, targetEntity, user, trustType, true) != Tristate.TRUE) {
                                return true;
                            }
                        }
                    } else if (targetEntity instanceof Living && !NMSUtil.getInstance().isEntityMonster(targetEntity)) {
                        if (user != null && !user.getUniqueId().equals(claim.getOwnerUniqueId()) && perm != Tristate.TRUE) {
                            return true;
                        }
                    }
                }
            }
        }

        if (entityDamageSource == null || tileEntity != null) {
            return false;
        }

        Player attacker = null;
        Projectile projectile = null;

        if (source != null) {
            if (source instanceof Player) {
                attacker = (Player) source;
            } else if (source instanceof Projectile) {
                projectile = (Projectile) source;
                if (projectile.getShooter() instanceof Player) {
                    attacker = (Player) projectile.getShooter();
                }
            }
        }

        if (GDPermissionManager.getInstance().getFinalPermission(event, targetEntity.getLocation(), claim, Flags.ENTITY_DAMAGE, attacker, targetEntity, user, trustType, true) == Tristate.FALSE) {
            return true;
        }

        return false;
    }

    @Listener(order = Order.POST)
    public void onEntityDamageMonitor(DamageEntityEvent event) {
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(event.getTargetEntity().getWorld().getUniqueId())) {
            return;
        }

        GDTimings.ENTITY_DAMAGE_MONITOR_EVENT.startTimingIfSync();
        //FEATURE: prevent players who very recently participated in pvp combat from hiding inventory to protect it from looting
        //FEATURE: prevent players who are in pvp combat from logging out to avoid being defeated

        if (event.getTargetEntity().getType() != EntityTypes.PLAYER || NMSUtil.getInstance().isEntityMonster(event.getTargetEntity())) {
            GDTimings.ENTITY_DAMAGE_MONITOR_EVENT.stopTimingIfSync();
            return;
        }

        Player defender = (Player) event.getTargetEntity();

        //only interested in entities damaging entities (ignoring environmental damage)
        // the rest is only interested in entities damaging entities (ignoring environmental damage)
        if (!(event.getCause().root() instanceof EntityDamageSource)) {
            GDTimings.ENTITY_DAMAGE_MONITOR_EVENT.stopTimingIfSync();
            return;
        }

        GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(defender.getWorld(), defender.getUniqueId());
        GDClaim claim = this.dataStore.getClaimAtPlayer(playerData, defender.getLocation());
        EntityDamageSource entityDamageSource = (EntityDamageSource) event.getCause().root();

        //if not in a pvp rules world, do nothing
        if (!claim.isPvpEnabled()) {
            GDTimings.ENTITY_DAMAGE_MONITOR_EVENT.stopTimingIfSync();
            return;
        }

        GDTimings.ENTITY_DAMAGE_MONITOR_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onDestructEntity(DestructEntityEvent event) {
      //  Thread.dumpStack();
    }

    // when an entity drops items on death
    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onEntityDropItemDeath(DropItemEvent.Destruct event) {
        if (!GDFlags.ITEM_DROP || event.getEntities().isEmpty()) {
            return;
        }

        final World world = event.getEntities().get(0).getWorld();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUniqueId())) {
            return;
        }

        Object source = event.getSource();
        // If root cause is damage source, look for target as that should be passed instead
        // Ex. Entity dies and drops an item would be after EntityDamageSource
        if (source instanceof DamageSource) {
            final Object target = event.getCause().after(DamageSource.class).orElse(null);
            if (target != null) {
                source = target;
            }
        }
        if (!(source instanceof Entity)) {
            return;
        }

        final Entity entity = (Entity) source;
        if (GriefDefenderPlugin.isSourceIdBlacklisted(Flags.ITEM_DROP.getName(), entity, world.getProperties())) {
            return;
        }

        GDTimings.ENTITY_DROP_ITEM_DEATH_EVENT.startTimingIfSync();

        final User user = CauseContextHelper.getEventUser(event);
        event.filterEntities(new Predicate<Entity>() {
            GDClaim targetClaim = null;

            @Override
            public boolean test(Entity item) {
                if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.ITEM_DROP.getName(), item, world.getProperties())) {
                    return true;
                }

                targetClaim = GriefDefenderPlugin.getInstance().dataStore.getClaimAt(item.getLocation(), targetClaim);
                if (targetClaim == null) {
                    return true;
                }

                if (user == null) {
                    return true;
                }
                if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.ITEM_DROP.getName(), item, world.getProperties())) {
                    return true;
                }

                if (GDPermissionManager.getInstance().getFinalPermission(event, item.getLocation(), targetClaim, Flags.ITEM_DROP, entity, item, user, TrustTypes.ACCESSOR, true) == Tristate.FALSE) {
                    return false;
                }
                return true;
            }
        });

        GDTimings.ENTITY_DROP_ITEM_DEATH_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onEntityMove(MoveEntityEvent event){
        CommonEntityEventHandler.getInstance().onEntityMove(event, event.getFromTransform().getLocation(), event.getToTransform().getLocation(), event.getTargetEntity());
    }

    // when a player teleports
    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onEntityTeleport(MoveEntityEvent.Teleport event) {
        if (!GDFlags.ENTITY_TELEPORT_FROM && !GDFlags.ENTITY_TELEPORT_TO) {
            return;
        }

        final Entity entity = event.getTargetEntity();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(entity.getWorld().getUniqueId())) {
            return;
        }
        final boolean teleportFromBlacklisted = GriefDefenderPlugin.isSourceIdBlacklisted(Flags.ENTITY_TELEPORT_FROM.getName(), entity, entity.getWorld().getProperties());
        final boolean teleportToBlacklisted = GriefDefenderPlugin.isSourceIdBlacklisted(Flags.ENTITY_TELEPORT_TO.getName(), entity, entity.getWorld().getProperties());
        final boolean portalUseBlacklisted = GriefDefenderPlugin.isSourceIdBlacklisted(Flags.PORTAL_USE.getName(), entity, entity.getWorld().getProperties());
        if (teleportFromBlacklisted && teleportToBlacklisted && portalUseBlacklisted) {
            return;
        }

        GDTimings.ENTITY_TELEPORT_EVENT.startTimingIfSync();
        Player player = null;
        GDPermissionUser user = null;
        if (entity instanceof Player) {
            player = (Player) entity;
            user = PermissionHolderCache.getInstance().getOrCreateUser(player);
            final GDPlayerData playerData = user.getInternalPlayerData();
            // Cancel event if player is unable to teleport during PvP combat
            final boolean pvpCombatTeleport = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Boolean.class), player, Options.PVP_COMBAT_TELEPORT);
            if (!pvpCombatTeleport) {
                final int combatTimeRemaining = playerData.getPvpCombatTimeRemaining();
                if (combatTimeRemaining > 0) {
                    final Component denyMessage = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PVP_IN_COMBAT_NOT_ALLOWED,
                            ImmutableMap.of(
                            "time-remaining", combatTimeRemaining));
                    GriefDefenderPlugin.sendMessage(player, denyMessage);
                    event.setCancelled(true);
                    GDTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                    return;
                }
            }
        } else {
            user = PermissionHolderCache.getInstance().getOrCreateUser(entity.getCreator().orElse(null));
        }

        if (user == null) {
            GDTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
            return;
        }

        final Cause cause = event.getCause();
        final EventContext context = cause.getContext();

        final TeleportType type = context.get(EventContextKeys.TELEPORT_TYPE).orElse(TeleportTypes.ENTITY_TELEPORT);
        final Location<World> sourceLocation = event.getFromTransform().getLocation();
        final Location<World> destination = event.getToTransform().getLocation();
        // Handle BorderClaimEvent
        if (!CommonEntityEventHandler.getInstance().onEntityMove(event, sourceLocation, destination, entity)) {
            event.setCancelled(true);
            GDTimings.ENTITY_TELEPORT_EVENT.stopTiming();
            return;
        }

        GDClaim sourceClaim = null;
        GDPlayerData playerData = null;
        if (player != null) {
            playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
            sourceClaim = this.dataStore.getClaimAtPlayer(playerData, player.getLocation());
        } else {
            sourceClaim = this.dataStore.getClaimAt(sourceLocation);
        }

        if (sourceClaim != null) {
            if (GDFlags.ENTITY_TELEPORT_FROM && !teleportFromBlacklisted && GDPermissionManager.getInstance().getFinalPermission(event, sourceLocation, sourceClaim, Flags.ENTITY_TELEPORT_FROM, type, entity, user, TrustTypes.ACCESSOR, true) == Tristate.FALSE) {
                boolean cancelled = true;
                if (GDFlags.PORTAL_USE && type.equals(TeleportTypes.PORTAL)) {
                    if (portalUseBlacklisted || GDPermissionManager.getInstance().getFinalPermission(event, sourceLocation, sourceClaim, Flags.PORTAL_USE, type, entity, user) == Tristate.TRUE) {
                        cancelled = false;
                    }
                }
                if (cancelled) {
                    final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_PORTAL_EXIT,
                            ImmutableMap.of(
                            "player", sourceClaim.getOwnerName()));
                    if (player != null) {
                        GriefDefenderPlugin.sendMessage(player, message);
                    }

                    event.setCancelled(true);
                    GDTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                    return;
                }
            }
        }

        // check if destination world is enabled
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(event.getToTransform().getExtent().getUniqueId())) {
            GDTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
            return;
        }

        final GDClaim toClaim = this.dataStore.getClaimAt(destination);
        if (toClaim != null) {
            if (GDFlags.ENTITY_TELEPORT_TO && !teleportToBlacklisted && GDPermissionManager.getInstance().getFinalPermission(event, destination, toClaim, Flags.ENTITY_TELEPORT_TO, type, entity, user, TrustTypes.ACCESSOR, true) == Tristate.FALSE) {
                boolean cancelled = true;
                if (GDFlags.PORTAL_USE && type.equals(TeleportTypes.PORTAL)) {
                    if (portalUseBlacklisted || GDPermissionManager.getInstance().getFinalPermission(event, destination, toClaim, Flags.PORTAL_USE, type, entity, user, TrustTypes.ACCESSOR, true) == Tristate.TRUE) {
                        cancelled = false;
                    }
                }
                if (cancelled) {
                    final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_PORTAL_ENTER,
                            ImmutableMap.of(
                            "player", toClaim.getOwnerName()));
                    if (player != null) {
                        GriefDefenderPlugin.sendMessage(player, message);
                    }
    
                    if (type.equals(EntityTypes.ENDER_PEARL)) {
                        player.getInventory().offer(ItemStack.of(ItemTypes.ENDER_PEARL, 1));
                    }
                    event.setCancelled(true);
                    GDTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                    return;
                }
            }
        }

        if (player != null && !sourceLocation.getExtent().getUniqueId().equals(destination.getExtent().getUniqueId())) {
            // new world, check if player has world storage for it
            GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(destination.getExtent().getUniqueId());

            // update lastActive timestamps for claims this player owns
            WorldProperties worldProperties = destination.getExtent().getProperties();
            UUID playerUniqueId = player.getUniqueId();
            for (Claim claim : this.dataStore.getClaimWorldManager(worldProperties.getUniqueId()).getWorldClaims()) {
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
        GDTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
    }

    // Protects Item Frames
    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onEntityCollideEntity(CollideEntityEvent event) {
        if (!GDFlags.COLLIDE_ENTITY || event instanceof CollideEntityEvent.Impact) {
            return;
        }
        //if (GriefDefenderPlugin.isSourceIdBlacklisted(ClaimFlag.ENTITY_COLLIDE_ENTITY.toString(), event.getSource(), event.getEntities().get(0).getWorld().getProperties())) {
        //    return;
        //}

        Object rootCause = event.getCause().root();
        final boolean isRootEntityItemFrame = rootCause instanceof ItemFrame;
        if (!isRootEntityItemFrame) {
            return;
        }

        GDTimings.ENTITY_COLLIDE_EVENT.startTimingIfSync();
        event.filterEntities(new Predicate<Entity>() {
            @Override
            public boolean test(Entity entity) {
                // Avoid living entities breaking itemframes
                if (isRootEntityItemFrame && entity instanceof Living) {
                    return false;
                }

                return true;
            }
        });
        GDTimings.ENTITY_COLLIDE_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onProjectileImpactEntity(CollideEntityEvent.Impact event) {
        if (!GDFlags.PROJECTILE_IMPACT_ENTITY) {
            return;
        }
        if (GriefDefenderPlugin.isSourceIdBlacklisted(Flags.PROJECTILE_IMPACT_ENTITY.getName(), event.getSource(), event.getImpactPoint().getExtent().getProperties())) {
            return;
        }

        final User user = CauseContextHelper.getEventUser(event);
        if (user == null || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(event.getImpactPoint().getExtent().getUniqueId())) {
            return;
        }

        GDTimings.PROJECTILE_IMPACT_ENTITY_EVENT.startTimingIfSync();
        Object source = event.getCause().root();
        Location<World> impactPoint = event.getImpactPoint();
        GDClaim targetClaim = null;
        for (Entity entity : event.getEntities()) {
            if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.PROJECTILE_IMPACT_ENTITY.getName(), entity, event.getImpactPoint().getExtent().getProperties())) {
                return;
            }
            targetClaim = this.dataStore.getClaimAt(impactPoint, targetClaim);
            final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, impactPoint, targetClaim, Flags.PROJECTILE_IMPACT_ENTITY, source, entity, user, TrustTypes.ACCESSOR, true);
            if (result == Tristate.FALSE) {
                if (GDPermissionManager.getInstance().getFinalPermission(event, impactPoint, targetClaim, Flags.PROJECTILE_IMPACT_ENTITY, source, entity, user) == Tristate.TRUE) {
                    GDTimings.PROJECTILE_IMPACT_ENTITY_EVENT.stopTimingIfSync();
                    return;
                }

                event.setCancelled(true);
            }
        }
        GDTimings.PROJECTILE_IMPACT_ENTITY_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onEntityMount(RideEntityEvent.Mount event) {
        if (!GDFlags.ENTITY_RIDING) {
            return;
        }

        final Entity entity = event.getTargetEntity();
        final World world = entity.getWorld();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUniqueId())) {
            return;
        }
        if (GriefDefenderPlugin.isSourceIdBlacklisted(Flags.ENTITY_RIDING.getName(), entity, world.getUniqueId())) {
            return;
        }
        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.ENTITY_RIDING.getName(), entity, world.getUniqueId())) {
            return;
        }

        GDTimings.ENTITY_MOUNT_EVENT.startTiming();
        final Object source = event.getSource();
        Player player = source instanceof Player ? (Player) source : null;
        final Location<World> location = entity.getLocation();
        final GDClaim targetClaim = GriefDefenderPlugin.getInstance().dataStore.getClaimAt(location);

        if (GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, Flags.ENTITY_RIDING, source, entity, player, TrustTypes.ACCESSOR, true) == Tristate.FALSE) {
            if (player != null) {
                //sendInteractEntityDenyMessage(targetClaim, player, null, entity);
            }
            event.setCancelled(true);
        }

        GDTimings.ENTITY_MOUNT_EVENT.stopTiming();
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
            GDPermissionManager.getInstance().addEventLogEntry(event, claim, targetPlayer.getLocation(), source, targetPlayer, source, Flags.ENTITY_DAMAGE, "pvp-combat", Tristate.TRUE);
            return false;
        }

        // Check target claim
        if (!claim.isPvpEnabled()) {
            GriefDefenderPlugin.sendMessage(sourcePlayer, MessageCache.getInstance().PVP_CLAIM_NOT_ALLOWED);
            GDPermissionManager.getInstance().addEventLogEntry(event, claim, targetPlayer.getLocation(), source, targetPlayer, source, Flags.ENTITY_DAMAGE, "pvp", Tristate.FALSE);
            return true;
        }
        // Check source claim
        final GDClaim sourceClaim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(source.getInternalPlayerData(), sourcePlayer.getLocation());
        if (!sourceClaim.isPvpEnabled()) {
            GriefDefenderPlugin.sendMessage(sourcePlayer, MessageCache.getInstance().PVP_CLAIM_NOT_ALLOWED);
            GDPermissionManager.getInstance().addEventLogEntry(event, sourceClaim, targetPlayer.getLocation(), source, targetPlayer, source, Flags.ENTITY_DAMAGE, "pvp", Tristate.FALSE);
            return true;
        }

        // Check flags
        Tristate sourceResult = GDPermissionManager.getInstance().getFinalPermission(event, targetPlayer.getLocation(), claim, Flags.ENTITY_DAMAGE, sourcePlayer, targetPlayer, sourcePlayer, true);
        Tristate targetResult = GDPermissionManager.getInstance().getFinalPermission(event, sourcePlayer.getLocation(), claim, Flags.ENTITY_DAMAGE, targetPlayer, sourcePlayer, targetPlayer, true);
        if (sourceResult == Tristate.FALSE) {
            GriefDefenderPlugin.sendMessage(sourcePlayer, MessageCache.getInstance().PVP_SOURCE_NOT_ALLOWED);
            GDPermissionManager.getInstance().addEventLogEntry(event, claim, targetPlayer.getLocation(), source, targetPlayer, source, Flags.ENTITY_DAMAGE, "pvp", Tristate.FALSE);
            return true;
        }
        if (targetResult == Tristate.FALSE) {
            GriefDefenderPlugin.sendMessage(sourcePlayer, MessageCache.getInstance().PVP_TARGET_NOT_ALLOWED);
            GDPermissionManager.getInstance().addEventLogEntry(event, claim, targetPlayer.getLocation(), source, targetPlayer, source, Flags.ENTITY_DAMAGE, "pvp", Tristate.FALSE);
            return true;
        }

        // Check options
        sourceResult = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Tristate.class), source, Options.PVP, claim);
        targetResult = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Tristate.class), target, Options.PVP, claim);
        if (sourceResult == Tristate.UNDEFINED) {
            sourceResult = Tristate.fromBoolean(claim.getWorld().getProperties().isPVPEnabled());
        }
        if (targetResult == Tristate.UNDEFINED) {
            targetResult = Tristate.fromBoolean(claim.getWorld().getProperties().isPVPEnabled());
        }
        if (sourceResult == Tristate.FALSE) {
            GriefDefenderPlugin.sendMessage(sourcePlayer, MessageCache.getInstance().PVP_SOURCE_NOT_ALLOWED);
            GDPermissionManager.getInstance().addEventLogEntry(event, claim, targetPlayer.getLocation(), source, targetPlayer, source, Options.PVP.getPermission(), "pvp", Tristate.FALSE);
            return true;
        }
        if (targetResult == Tristate.FALSE) {
            GriefDefenderPlugin.sendMessage(sourcePlayer, MessageCache.getInstance().PVP_TARGET_NOT_ALLOWED);
            GDPermissionManager.getInstance().addEventLogEntry(event, claim, targetPlayer.getLocation(), source, targetPlayer, source, Options.PVP.getPermission(), "pvp", Tristate.FALSE);
            return true;
        }

        final Instant now = Instant.now();
        source.getInternalPlayerData().lastPvpTimestamp = now;
        target.getInternalPlayerData().lastPvpTimestamp = now;
        GDPermissionManager.getInstance().addEventLogEntry(event, claim, targetPlayer.getLocation(), source, targetPlayer, source, Flags.ENTITY_DAMAGE, "pvp", Tristate.TRUE);
        return false;
    }
}
