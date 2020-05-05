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
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimResult;
import com.griefdefender.api.claim.ClaimTypes;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.api.permission.flag.Flags;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.cache.EventResultCache;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.claim.GDClaimManager;
import com.griefdefender.configuration.GriefDefenderConfig;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.internal.util.BlockUtil;
import com.griefdefender.internal.util.NMSUtil;
import com.griefdefender.internal.visual.ClaimVisual;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.permission.flag.GDFlags;
import com.griefdefender.storage.BaseStorage;
import com.griefdefender.util.BlockPosCache;
import com.griefdefender.util.CauseContextHelper;
import net.kyori.text.Component;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.Piston;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.block.tileentity.carrier.Chest;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.FallingBlock;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.CollideBlockEvent;
import org.spongepowered.api.event.block.NotifyNeighborBlockEvent;
import org.spongepowered.api.event.block.tileentity.ChangeSignEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.LocatableBlock;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.explosion.Explosion;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

//event handlers related to blocks
public class BlockEventHandler {

    private int lastBlockPreTick = -1;
    private boolean lastBlockPreCancelled = false;

    // convenience reference to singleton datastore
    private final BaseStorage dataStore;

    // constructor
    public BlockEventHandler(BaseStorage dataStore) {
        this.dataStore = dataStore;
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onBlockPre(ChangeBlockEvent.Pre event) {
        lastBlockPreTick = Sponge.getServer().getRunningTimeTicks();
        if (GriefDefenderPlugin.isSourceIdBlacklisted("block-pre", event.getSource(), event.getLocations().get(0).getExtent().getProperties())) {
            return;
        }

        final World world = event.getLocations().get(0).getExtent();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUniqueId())) {
            GDTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
            return;
        }

        final Cause cause = event.getCause();
        final EventContext context = event.getContext();
        final User user = CauseContextHelper.getEventUser(event);
        final boolean hasFakePlayer = context.containsKey(EventContextKeys.FAKE_PLAYER);

        if (user != null) {
            if (context.containsKey(EventContextKeys.PISTON_RETRACT)) {
                return;
            }
        }

        final LocatableBlock locatableBlock = cause.first(LocatableBlock.class).orElse(null);
        final TileEntity tileEntity = cause.first(TileEntity.class).orElse(null);
        Entity sourceEntity = null;
        // Always use TE as source if available
        final Object source = tileEntity != null ? tileEntity : cause.root();
        Location<World> sourceLocation = locatableBlock != null ? locatableBlock.getLocation() : tileEntity != null ? tileEntity.getLocation() : null;
        if (sourceLocation == null && source instanceof Entity) {
            // check entity
            sourceEntity = ((Entity) source);
            sourceLocation = sourceEntity.getLocation();
        }
        final boolean pistonExtend = context.containsKey(EventContextKeys.PISTON_EXTEND);
        final boolean isLiquidSource = context.containsKey(EventContextKeys.LIQUID_FLOW);
        final boolean isFireSource = isLiquidSource ? false : context.containsKey(EventContextKeys.FIRE_SPREAD);
        final boolean isLeafDecay = context.containsKey(EventContextKeys.LEAVES_DECAY);
        if (!GDFlags.LEAF_DECAY && isLeafDecay) {
            return;
        }
        if (!GDFlags.LIQUID_FLOW && isLiquidSource) {
            return;
        }
        if (!GDFlags.BLOCK_SPREAD && isFireSource) {
            return;
        }

        lastBlockPreCancelled = false;
        final boolean isForgePlayerBreak = context.containsKey(EventContextKeys.PLAYER_BREAK);
        GDTimings.BLOCK_PRE_EVENT.startTimingIfSync();
        // Handle player block breaks separately
        if (isForgePlayerBreak && !hasFakePlayer && source instanceof Player) {
            final Player player = (Player) source;
            GDClaim targetClaim = null;
            final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getPlayerData(world, player.getUniqueId());
            for (Location<World> location : event.getLocations()) {
                if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.BLOCK_BREAK.getName(), location.getBlock(), world.getProperties())) {
                   GDTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
                   return;
                }

                targetClaim = this.dataStore.getClaimAt(location, targetClaim);
                if (location.getBlockType() == BlockTypes.AIR) {
                    continue;
                }
                if (!checkSurroundings(event, location, player, playerData, targetClaim)) {
                    event.setCancelled(true);
                    GDTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
                    return;
                }

                // check overrides
                final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, Flags.BLOCK_BREAK, source, location.getBlock(), player, TrustTypes.BUILDER, true);
                if (result != Tristate.TRUE) {
                    final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_BUILD,
                            ImmutableMap.of(
                            "player", targetClaim.getOwnerName()));
                    GriefDefenderPlugin.sendClaimDenyMessage(targetClaim, player, message);
                    event.setCancelled(true);
                    lastBlockPreCancelled = true;
                    GDTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
                    return;
                }
            }

            GDTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
            return;
        }

        if (sourceLocation != null) {
            GDPlayerData playerData = null;
            if (user != null) {
                playerData = GriefDefenderPlugin.getInstance().dataStore.getPlayerData(world, user.getUniqueId());
            }
            GDClaim sourceClaim = this.dataStore.getClaimAt(sourceLocation);
            GDClaim targetClaim = null;
            List<Location<World>> sourceLocations = event.getLocations();
            if (pistonExtend) {
                // check next block in extend direction
                sourceLocations = new ArrayList<>(event.getLocations());
                Location<World> location = sourceLocations.get(sourceLocations.size() - 1);
                final Direction direction = locatableBlock.getLocation().getBlock().get(Keys.DIRECTION).get();
                final Location<World> dirLoc = location.getBlockRelative(direction);
                sourceLocations.add(dirLoc);
            }
            for (Location<World> location : sourceLocations) {
                // Mods such as enderstorage will send chest updates to itself
                // We must ignore cases like these to avoid issues with mod
                if (tileEntity != null) {
                    if (location.getPosition().equals(tileEntity.getLocation().getPosition())) {
                        continue;
                    }
                }

                final BlockState blockState = location.getBlock();
                targetClaim = this.dataStore.getClaimAt(location, targetClaim);
                // If a player successfully interacted with a block recently such as a pressure plate, ignore check
                // This fixes issues such as pistons not being able to extend
                if (user != null && !isForgePlayerBreak && playerData != null && playerData.eventResultCache != null && playerData.eventResultCache.checkEventResultCache(targetClaim, "block-pre") == Tristate.TRUE) {
                    GDPermissionManager.getInstance().addEventLogEntry(event, targetClaim, location, source, blockState, user, Flags.BLOCK_BREAK, playerData.eventResultCache.lastTrust, Tristate.TRUE);
                    continue;
                }
                if (user != null && targetClaim.isUserTrusted(user, TrustTypes.BUILDER)) {
                    GDPermissionManager.getInstance().addEventLogEntry(event, targetClaim, location, source, blockState, user, Flags.BLOCK_BREAK, TrustTypes.BUILDER.getName().toLowerCase(), Tristate.TRUE);
                    continue;
                }
                if (sourceClaim.getOwnerUniqueId().equals(targetClaim.getOwnerUniqueId()) && user == null && sourceEntity == null && !isFireSource && !isLeafDecay) {
                    GDPermissionManager.getInstance().addEventLogEntry(event, targetClaim, location, source, blockState, user, Flags.BLOCK_BREAK, "owner", Tristate.TRUE);
                    continue;
                }
                if (user != null && pistonExtend) {
                    if (targetClaim.isUserTrusted(user, TrustTypes.ACCESSOR)) {
                        GDPermissionManager.getInstance().addEventLogEntry(event, targetClaim, location, source, blockState, user, Flags.BLOCK_BREAK, TrustTypes.ACCESSOR.getName().toLowerCase(), Tristate.TRUE);
                        continue;
                    }
                }
                if (isLeafDecay) {
                    if (GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, Flags.LEAF_DECAY, source, blockState, user) == Tristate.FALSE) {
                        event.setCancelled(true);
                        GDTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
                        return;
                    }
                } else if (isFireSource) {
                    if (GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, Flags.BLOCK_SPREAD, source, blockState, user) == Tristate.FALSE) {
                        event.setCancelled(true);
                        GDTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
                        return;
                    }
                } else if (isLiquidSource) {
                    if (GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, Flags.LIQUID_FLOW, source, blockState, user) == Tristate.FALSE) {
                        event.setCancelled(true);
                        lastBlockPreCancelled = true;
                        GDTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
                        return;
                    }
                    continue;
                } else if (GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, Flags.BLOCK_BREAK, source, blockState, user) == Tristate.FALSE) {
                    // PRE events can be spammy so we need to avoid sending player messages here.
                    event.setCancelled(true);
                    lastBlockPreCancelled = true;
                    GDTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
                    return;
                }
            }
        } else if (user != null) {
            final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getPlayerData(world, user.getUniqueId());
            GDClaim targetClaim = null;
            for (Location<World> location : event.getLocations()) {
                // Mods such as enderstorage will send chest updates to itself
                // We must ignore cases like these to avoid issues with mod
                if (tileEntity != null) {
                    if (location.getPosition().equals(tileEntity.getLocation().getPosition())) {
                        continue;
                    }
                }

                final BlockState blockState = location.getBlock();
                targetClaim = this.dataStore.getClaimAt(location, targetClaim);
                // If a player successfully interacted with a block recently such as a pressure plate, ignore check
                // This fixes issues such as pistons not being able to extend
                if (!isForgePlayerBreak && playerData != null && playerData.eventResultCache != null && playerData.eventResultCache.checkEventResultCache(targetClaim, "block-pre") == Tristate.TRUE) {
                    GDPermissionManager.getInstance().addEventLogEntry(event, targetClaim, location, source, blockState, user, Flags.BLOCK_BREAK, playerData.eventResultCache.lastTrust, Tristate.TRUE);
                    continue;
                }
                if (targetClaim.isUserTrusted(user, TrustTypes.BUILDER)) {
                    GDPermissionManager.getInstance().addEventLogEntry(event, targetClaim, location, source, blockState, user, Flags.BLOCK_BREAK, TrustTypes.BUILDER.getName().toLowerCase(), Tristate.TRUE);
                    continue;
                }

                if (isFireSource) {
                    if (GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, Flags.BLOCK_SPREAD, source, blockState, user) == Tristate.FALSE) {
                        event.setCancelled(true);
                        GDTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
                        return;
                    }
                } else if (isLiquidSource) {
                    if (GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, Flags.LIQUID_FLOW, source, blockState, user) == Tristate.FALSE) {
                        event.setCancelled(true);
                        lastBlockPreCancelled = true;
                        GDTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
                        return;
                    }
                    continue;
                } else if (GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, Flags.BLOCK_BREAK, source, blockState, user) == Tristate.FALSE) {
                    event.setCancelled(true);
                    lastBlockPreCancelled = true;
                    GDTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
                    return;
                }
            }
        }
        GDTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
    }

    // Handle fluids flowing into claims
    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onBlockNotify(NotifyNeighborBlockEvent event) {
        LocatableBlock locatableBlock = event.getCause().first(LocatableBlock.class).orElse(null);
        TileEntity tileEntity = event.getCause().first(TileEntity.class).orElse(null);
        Location<World> sourceLocation = locatableBlock != null ? locatableBlock.getLocation() : tileEntity != null ? tileEntity.getLocation() : null;
        GDClaim sourceClaim = null;
        GDPlayerData playerData = null;
        if (sourceLocation != null) {
            if (GriefDefenderPlugin.isSourceIdBlacklisted("block-notify", event.getSource(), sourceLocation.getExtent().getProperties())) {
                return;
            }
        }

        final User user = CauseContextHelper.getEventUser(event);
        if (user == null) {
            return;
        }
        if (sourceLocation == null) {
            Player player = event.getCause().first(Player.class).orElse(null);
            if (player == null) {
                return;
            }

            sourceLocation = player.getLocation();
            playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
            sourceClaim = this.dataStore.getClaimAtPlayer(playerData, player.getLocation());
        } else {
            playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(sourceLocation.getExtent(), user.getUniqueId());
            sourceClaim = this.dataStore.getClaimAt(sourceLocation, playerData.lastClaim.get());
        }

        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(sourceLocation.getExtent().getUniqueId())) {
            return;
        }

        GDTimings.BLOCK_NOTIFY_EVENT.startTimingIfSync();
        Iterator<Direction> iterator = event.getNeighbors().keySet().iterator();
        GDClaim targetClaim = null;
        while (iterator.hasNext()) {
            Direction direction = iterator.next();
            Location<World> location = sourceLocation.getBlockRelative(direction);
            Vector3i pos = location.getBlockPosition();
            targetClaim = this.dataStore.getClaimAt(location, targetClaim);
            if (sourceClaim.isWilderness() && targetClaim.isWilderness()) {
                if (playerData != null) {
                    playerData.eventResultCache = new EventResultCache(targetClaim, "block-notify", Tristate.TRUE);
                }
                continue;
            } else if (!sourceClaim.isWilderness() && targetClaim.isWilderness()) {
                if (playerData != null) {
                    playerData.eventResultCache = new EventResultCache(targetClaim, "block-notify", Tristate.TRUE);
                }
                continue;
            } else if (sourceClaim.getUniqueId().equals(targetClaim.getUniqueId())) {
                if (playerData != null) {
                    playerData.eventResultCache = new EventResultCache(targetClaim, "block-notify", Tristate.TRUE);
                }
                continue;
            } else {
                if (playerData.eventResultCache != null && playerData.eventResultCache.checkEventResultCache(targetClaim, "block-notify") == Tristate.TRUE) {
                    continue;
                }
                // Needed to handle levers notifying doors to open etc.
                if (targetClaim.isUserTrusted(user, TrustTypes.ACCESSOR)) {
                    if (playerData != null) {
                        playerData.eventResultCache = new EventResultCache(targetClaim, "block-notify", Tristate.TRUE, TrustTypes.ACCESSOR.getName().toLowerCase());
                    }
                    continue;
                }
            }

            // no claim crossing unless trusted
            iterator.remove();
        }
        GDTimings.BLOCK_NOTIFY_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onBlockCollide(CollideBlockEvent event, @Root Entity source) {
        if (event instanceof CollideBlockEvent.Impact) {
            return;
        }
        // ignore falling blocks
        if (!GDFlags.COLLIDE_BLOCK || source instanceof FallingBlock) {
            return;
        }
        if (GriefDefenderPlugin.isSourceIdBlacklisted(Flags.COLLIDE_BLOCK.getName(), source.getType().getId(), source.getWorld().getProperties())) {
            return;
        }
        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.COLLIDE_BLOCK.getName(), event.getTargetBlock(), source.getWorld().getProperties())) {
            return;
        }

        final User user = CauseContextHelper.getEventUser(event);
        if (user == null) {
            return;
        }

        GDTimings.BLOCK_COLLIDE_EVENT.startTimingIfSync();
        final BlockType blockType = event.getTargetBlock().getType();
        if (blockType.equals(BlockTypes.AIR) 
                || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(event.getTargetLocation().getExtent().getUniqueId())) {
            GDTimings.BLOCK_COLLIDE_EVENT.stopTimingIfSync();
            return;
        }

        if (source instanceof Item && (blockType != BlockTypes.PORTAL && !NMSUtil.getInstance().isBlockPressurePlate(blockType))) {
            GDTimings.BLOCK_COLLIDE_EVENT.stopTimingIfSync();
            return;
        }

        Vector3i collidePos = event.getTargetLocation().getBlockPosition();
        short shortPos = BlockUtil.getInstance().blockPosToShort(collidePos);
        int entityId = NMSUtil.getInstance().getEntityMinecraftId(source);
        BlockPosCache entityBlockCache = BlockUtil.ENTITY_BLOCK_CACHE.get(entityId);
        if (entityBlockCache == null) {
            entityBlockCache = new BlockPosCache(shortPos);
            BlockUtil.ENTITY_BLOCK_CACHE.put(entityId, entityBlockCache);
        } else {
            Tristate result = entityBlockCache.getCacheResult(shortPos);
            if (result != Tristate.UNDEFINED) {
                if (result == Tristate.FALSE) {
                    event.setCancelled(true);
                }

                GDTimings.BLOCK_COLLIDE_EVENT.stopTimingIfSync();
                return;
            }
        }

        GDPlayerData playerData = null;
        GDClaim targetClaim = null;
        if (user instanceof Player) {
            playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(event.getTargetLocation().getExtent(), user.getUniqueId());
            targetClaim = this.dataStore.getClaimAtPlayer(playerData, event.getTargetLocation());
        } else {
            targetClaim = this.dataStore.getClaimAt(event.getTargetLocation());
        }

        if (GDPermissionManager.getInstance().getFinalPermission(event, event.getTargetLocation(), targetClaim, Flags.COLLIDE_BLOCK, source, event.getTargetBlock(), user, TrustTypes.ACCESSOR, true) == Tristate.TRUE) {
            entityBlockCache.setLastResult(Tristate.TRUE);
            GDTimings.BLOCK_COLLIDE_EVENT.stopTimingIfSync();
            return;
        }
        if (GDFlags.PORTAL_USE && event.getTargetBlock().getType() == BlockTypes.PORTAL) {
            if (GDPermissionManager.getInstance().getFinalPermission(event, event.getTargetLocation(), targetClaim, Flags.PORTAL_USE, source, event.getTargetBlock(), user, TrustTypes.ACCESSOR, true) == Tristate.TRUE) {
                GDTimings.BLOCK_COLLIDE_EVENT.stopTimingIfSync();
                return;
            }
            if (event.getCause().root() instanceof Player){
                if (event.getTargetLocation().getExtent().getProperties().getTotalTime() % 20 == 0L) { // log once a second to avoid spam
                    // Disable message temporarily
                    //GriefDefender.sendMessage((Player) user, TextMode.Err, Messages.NoPortalFromProtectedClaim, claim.getOwnerName());
                    /*final Text message = GriefDefenderPlugin.getInstance().messageData.permissionProtectedPortal
                            .apply(ImmutableMap.of(
                            "owner", targetClaim.getOwnerName())).build();*/
                    event.setCancelled(true);
                    entityBlockCache.setLastResult(Tristate.FALSE);
                    GDTimings.BLOCK_COLLIDE_EVENT.stopTimingIfSync();
                    return;
                }
            }
        }

        GDTimings.BLOCK_COLLIDE_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onProjectileImpactBlock(CollideBlockEvent.Impact event) {
        if (!GDFlags.PROJECTILE_IMPACT_BLOCK || !(event.getSource() instanceof Entity)) {
            return;
        }

        final Entity source = (Entity) event.getSource();
        if (GriefDefenderPlugin.isSourceIdBlacklisted(Flags.PROJECTILE_IMPACT_BLOCK.getName(), source.getType().getId(), source.getWorld().getProperties())) {
            return;
        }

        final User user = CauseContextHelper.getEventUser(event);
        if (user == null) {
            return;
        }

        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(event.getImpactPoint().getExtent().getUniqueId())) {
            return;
        }

        GDTimings.PROJECTILE_IMPACT_BLOCK_EVENT.startTimingIfSync();
        Location<World> impactPoint = event.getImpactPoint();
        GDClaim targetClaim = null;
        GDPlayerData playerData = null;
        if (user instanceof Player) {
            playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(event.getTargetLocation().getExtent(), user.getUniqueId());
            targetClaim = this.dataStore.getClaimAtPlayer(playerData, impactPoint);
        } else {
            targetClaim = this.dataStore.getClaimAt(impactPoint);
        }

        Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, impactPoint, targetClaim, Flags.PROJECTILE_IMPACT_BLOCK, source, event.getTargetBlock(), user, TrustTypes.ACCESSOR, true);
        if (result == Tristate.FALSE) {
            event.setCancelled(true);
            GDTimings.PROJECTILE_IMPACT_BLOCK_EVENT.stopTimingIfSync();
            return;
        }

        GDTimings.PROJECTILE_IMPACT_BLOCK_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onExplosionPre(ExplosionEvent.Pre event) {
        final World world = event.getExplosion().getWorld();
        if (!GDFlags.EXPLOSION_BLOCK || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUniqueId())) {
            return;
        }

        Object source = event.getSource();
        final Explosion explosion = event.getExplosion();
        if (explosion.getSourceExplosive().isPresent()) {
            source = explosion.getSourceExplosive().get();
        } else {
            Entity exploder = event.getCause().first(Entity.class).orElse(null);
            if (exploder != null) {
                source = exploder;
            }
        }

        if (GriefDefenderPlugin.isSourceIdBlacklisted(Flags.EXPLOSION_BLOCK.getName(), source, event.getExplosion().getWorld().getProperties())) {
            return;
        }

        GDTimings.EXPLOSION_PRE_EVENT.startTimingIfSync();
        final User user = CauseContextHelper.getEventUser(event);
        final Location<World> location = event.getExplosion().getLocation();
        final GDClaim radiusClaim = NMSUtil.getInstance().createClaimFromCenter(location, event.getExplosion().getRadius());
        final GDClaimManager claimManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(location.getExtent().getUniqueId());
        final Set<Claim> surroundingClaims = claimManager.findOverlappingClaims(radiusClaim);
        if (surroundingClaims.size() == 0) {
            return;
        }
        for (Claim claim : surroundingClaims) {
            // Use any location for permission check
            Location<World> targetLocation = new Location<>(location.getExtent(), claim.getLesserBoundaryCorner());
            Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.EXPLOSION_BLOCK, source, targetLocation, user, true);
            if (result == Tristate.FALSE) {
                event.setCancelled(true);
                break;
            }
        }

        GDTimings.EXPLOSION_PRE_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onExplosionDetonate(ExplosionEvent.Detonate event) {
        final World world = event.getExplosion().getWorld();
        if (!GDFlags.EXPLOSION_BLOCK || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUniqueId())) {
            return;
        }

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
        if (GriefDefenderPlugin.isSourceIdBlacklisted(Flags.EXPLOSION_BLOCK.getName(), source, event.getExplosion().getWorld().getProperties())) {
            return;
        }

        GDTimings.EXPLOSION_EVENT.startTimingIfSync();
        final User user = CauseContextHelper.getEventUser(event);
        GDClaim targetClaim = null;
        final List<Location<World>> filteredLocations = new ArrayList<>();
        for (Location<World> location : event.getAffectedLocations()) {
            targetClaim =  GriefDefenderPlugin.getInstance().dataStore.getClaimAt(location, targetClaim);
            /*if (location.getPosition().getY() > ((net.minecraft.world.World) world).getSeaLevel() && !GriefDefenderPlugin.getActiveConfig(world.getUniqueId()).getConfig().claim.explosionSurface) {
                filteredLocations.add(location);
                continue;
            }*/

            Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, Flags.EXPLOSION_BLOCK, source, location.getBlock(), user, true);

            if (result == Tristate.FALSE) {
                // Avoid lagging server from large explosions.
                if (event.getAffectedLocations().size() > 100) {
                    event.setCancelled(true);
                    break;
                }
                filteredLocations.add(location);
            }
        }
        // Workaround for SpongeForge bug
        if (event.isCancelled()) {
            event.getAffectedLocations().clear();
        } else if (!filteredLocations.isEmpty()) {
            event.getAffectedLocations().removeAll(filteredLocations);
        }
        GDTimings.EXPLOSION_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onBlockBreak(ChangeBlockEvent.Break event) {
        if (!GDFlags.BLOCK_BREAK || event instanceof ExplosionEvent) {
            return;
        }

        if (lastBlockPreTick == Sponge.getServer().getRunningTimeTicks()) {
            event.setCancelled(lastBlockPreCancelled);
            return;
        }

        Object source = event.getSource();
        // Handled in Explosion listeners
        if (source instanceof Explosion) {
            return;
        }

        // Pistons are handled in onBlockPre
        if (source == BlockTypes.PISTON || source instanceof Piston) {
            return;
        }

        final World world = event.getTransactions().get(0).getFinal().getLocation().get().getExtent();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUniqueId())) {
            return;
        }

        if (GriefDefenderPlugin.isSourceIdBlacklisted(Flags.BLOCK_BREAK.getName(), source, world.getProperties())) {
            return;
        }

        final Player player = source instanceof Player ? (Player) source : null;
        final User user = player != null ? player : CauseContextHelper.getEventUser(event);

        // ignore falling blocks when there is no user
        // avoids dupes with falling blocks such as Dragon Egg
        if (user == null && source instanceof FallingBlock) {
            return;
        }
        GDClaim sourceClaim = null;
        LocatableBlock locatable = null;
        if (source instanceof LocatableBlock) {
            locatable = (LocatableBlock) source;
            sourceClaim = this.dataStore.getClaimAt(locatable.getLocation());
        } else {
            sourceClaim = this.getSourceClaim(event.getCause());
        }
        if (sourceClaim == null) {
            return;
        }

        GDTimings.BLOCK_BREAK_EVENT.startTimingIfSync();
        List<Transaction<BlockSnapshot>> transactions = event.getTransactions();
        GDClaim targetClaim = null;
        for (Transaction<BlockSnapshot> transaction : transactions) {
            if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.BLOCK_BREAK.getName(), transaction.getOriginal(), world.getProperties())) {
                continue;
            }

            Location<World> location = transaction.getOriginal().getLocation().orElse(null);
            targetClaim = this.dataStore.getClaimAt(location, targetClaim);
            if (location == null || transaction.getOriginal().getState().getType() == BlockTypes.AIR) {
                continue;
            }

            // check overrides
            final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, Flags.BLOCK_BREAK, source, transaction.getOriginal(), user, TrustTypes.BUILDER, true);
            if (result != Tristate.TRUE) {
                if (player != null) {
                    final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_BUILD,
                            ImmutableMap.of("player", targetClaim.getOwnerName()));
                    GriefDefenderPlugin.sendClaimDenyMessage(targetClaim, player, message);
                }

                event.setCancelled(true);
                GDTimings.BLOCK_BREAK_EVENT.stopTimingIfSync();
                return;
            }
        }
        GDTimings.BLOCK_BREAK_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onBlockPlace(ChangeBlockEvent.Place event) {
        final Object source = event.getSource();
        // Pistons are handled in onBlockPre
        if (source instanceof Piston) {
            return;
        }

        final World world = event.getTransactions().get(0).getFinal().getLocation().get().getExtent();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUniqueId())) {
            return;
        }
        if (GriefDefenderPlugin.isSourceIdBlacklisted(Flags.BLOCK_PLACE.getName(), event.getSource(), world.getProperties())) {
            return;
        }

        GDTimings.BLOCK_PLACE_EVENT.startTimingIfSync();
        GDClaim sourceClaim = null;
        LocatableBlock locatable = null;
        final User user = CauseContextHelper.getEventUser(event);
        if (source instanceof LocatableBlock) {
            locatable = (LocatableBlock) source;
            if (user != null && user instanceof Player) {
                final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(locatable.getWorld(), user.getUniqueId());
                sourceClaim = this.dataStore.getClaimAt(locatable.getLocation(), playerData.lastClaim.get());
            } else {
                sourceClaim = this.dataStore.getClaimAt(locatable.getLocation());
            }
        } else {
            sourceClaim = this.getSourceClaim(event.getCause());
        }
        if (sourceClaim == null) {
            GDTimings.BLOCK_PLACE_EVENT.stopTimingIfSync();
            return;
        }

        Player player = user != null && user instanceof Player ? (Player) user : null;
        GDPlayerData playerData = null;
        if (user != null) {
            playerData = this.dataStore.getOrCreatePlayerData(world, user.getUniqueId());
        }

        GriefDefenderConfig<?> activeConfig = GriefDefenderPlugin.getActiveConfig(world.getProperties());
        if (sourceClaim != null && !(source instanceof User) && playerData != null && playerData.eventResultCache != null && playerData.eventResultCache.checkEventResultCache(sourceClaim, Flags.BLOCK_PLACE.getName()) == Tristate.TRUE) {
            GDTimings.BLOCK_PLACE_EVENT.stopTimingIfSync();
            return;
        }

        GDClaim targetClaim = null;
        for (Transaction<BlockSnapshot> transaction : event.getTransactions()) {
            final BlockSnapshot block = transaction.getFinal();
            if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.BLOCK_PLACE.getName(), block, world.getProperties())) {
                continue;
            }

            Location<World> location = block.getLocation().orElse(null);
            if (location == null) {
                continue;
            }

            targetClaim = this.dataStore.getClaimAt(location, targetClaim);
            if (!checkSurroundings(event, location, player, playerData, targetClaim)) {
                event.setCancelled(true);
                GDTimings.BLOCK_PLACE_EVENT.stopTimingIfSync();
                return;
            }

            if (GDFlags.BLOCK_PLACE) {
                // Allow blocks to grow within claims
                if (user == null && sourceClaim != null && sourceClaim.getUniqueId().equals(targetClaim.getUniqueId())) {
                    GDTimings.BLOCK_PLACE_EVENT.stopTimingIfSync();
                    return;
                }

                // check overrides
                final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, Flags.BLOCK_PLACE, source, block, user, TrustTypes.BUILDER, true);
                if (result != Tristate.TRUE) {
                    // TODO - make sure this doesn't spam
                    /*if (source instanceof Player) {
                        final Text message = GriefDefenderPlugin.getInstance().messageData.permissionBuild
                                .apply(ImmutableMap.of(
                                "player", Text.of(targetClaim.getOwnerName())
                        )).build();
                        GriefDefenderPlugin.sendClaimDenyMessage(targetClaim, (Player) source, message);
                    }*/
                    event.setCancelled(true);
                    GDTimings.BLOCK_PLACE_EVENT.stopTimingIfSync();
                    return;
                }
            }

            if (!(source instanceof Player)) {
                continue;
            }

            if (targetClaim.isWilderness() && activeConfig.getConfig().claim.autoChestClaimBlockRadius > -1) {
                TileEntity tileEntity = block.getLocation().get().getTileEntity().orElse(null);
                if (tileEntity == null || !(tileEntity instanceof Chest)) {
                    GDTimings.BLOCK_PLACE_EVENT.stopTimingIfSync();
                    continue;
                }

                final int minClaimLevel = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), user, Options.MIN_LEVEL).intValue();
                final int maxClaimLevel = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), user, Options.MAX_LEVEL).intValue();
                if (block.getPosition().getY() < minClaimLevel || block.getPosition().getY() > maxClaimLevel) {
                    final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_CHEST_OUTSIDE_LEVEL,
                            ImmutableMap.of(
                            "min-claim-level", minClaimLevel,
                            "max-claim-level", maxClaimLevel));
                    GriefDefenderPlugin.sendMessage(player, message);
                    GDTimings.BLOCK_PLACE_EVENT.stopTimingIfSync();
                    continue;
                }

                int radius = activeConfig.getConfig().claim.autoChestClaimBlockRadius;

                if (playerData.getInternalClaims().size() == 0) {
                    if (activeConfig.getConfig().claim.autoChestClaimBlockRadius == 0) {
                        GDCauseStackManager.getInstance().pushCause(player);
                        final ClaimResult result = GriefDefender.getRegistry().createBuilder(Claim.Builder.class)
                                .bounds(block.getPosition(), block.getPosition())
                                .cuboid(false)
                                .owner(player.getUniqueId())
                                .sizeRestrictions(false)
                                .type(ClaimTypes.BASIC)
                                .world(block.getLocation().get().getExtent().getUniqueId())
                                .build();
                        GDCauseStackManager.getInstance().popCause();
                        if (result.successful()) {
                            final Claim claim = result.getClaim().get();
                            final GDClaimManager claimManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(world.getUniqueId());
                            claimManager.addClaim(claim, true);
                            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CLAIM_CHEST_CONFIRMATION);
                            GDTimings.BLOCK_PLACE_EVENT.stopTimingIfSync();
                            continue;
                        }
                    } else {
                        Vector3i lesserBoundary = new Vector3i(
                            block.getPosition().getX() - radius,
                            minClaimLevel,
                            block.getPosition().getZ() - radius);
                        Vector3i greaterBoundary = new Vector3i(
                            block.getPosition().getX() + radius,
                            maxClaimLevel,
                            block.getPosition().getZ() + radius);

                        while (radius >= 0) {
                            GDCauseStackManager.getInstance().pushCause(player);
                            ClaimResult result = GriefDefender.getRegistry().createBuilder(Claim.Builder.class)
                                    .bounds(lesserBoundary, greaterBoundary)
                                    .cuboid(false)
                                    .owner(player.getUniqueId())
                                    .sizeRestrictions(false)
                                    .type(ClaimTypes.BASIC)
                                    .world(block.getLocation().get().getExtent().getUniqueId())
                                    .build();
                            GDCauseStackManager.getInstance().popCause();
                            if (!result.successful()) {
                                radius--;
                            } else {
                                GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CLAIM_AUTOMATIC_NOTIFICATION);
                                GDClaim newClaim = this.dataStore.getClaimAt(block.getLocation().get());
                                ClaimVisual visualization = new ClaimVisual(newClaim, ClaimVisual.BASIC);
                                visualization.createClaimBlockVisuals(block.getPosition().getY(), player.getLocation(), playerData);
                                visualization.apply(player);

                                GDTimings.BLOCK_PLACE_EVENT.stopTimingIfSync();
                                continue;
                            }
                        }
                    }
                }

                if (targetClaim.isWilderness() && player.hasPermission(GDPermissions.CLAIM_SHOW_TUTORIAL)) {
                    GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.TUTORIAL_CLAIM_BASIC));
                }
            }
        }

        GDTimings.BLOCK_PLACE_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onSignChanged(ChangeSignEvent event) {
        final User user = CauseContextHelper.getEventUser(event);
        if (user == null) {
            return;
        }

        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(event.getTargetTile().getLocation().getExtent().getUniqueId())) {
            return;
        }

        GDTimings.SIGN_CHANGE_EVENT.startTimingIfSync();
        Location<World> location = event.getTargetTile().getLocation();
        // Prevent users exploiting signs
        GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAt(location);
        if (GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.INTERACT_BLOCK_SECONDARY, user, location.getBlock(), user, TrustTypes.ACCESSOR, true) == Tristate.FALSE) {
            if (user instanceof Player) {
                event.setCancelled(true);
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_ACCESS,
                        ImmutableMap.of("player", claim.getOwnerName()));
                GriefDefenderPlugin.sendClaimDenyMessage(claim, (Player) user, message);
                return;
            }
        }

        GDTimings.SIGN_CHANGE_EVENT.stopTimingIfSync();
    }

    public GDClaim getSourceClaim(Cause cause) {
        BlockSnapshot blockSource = cause.first(BlockSnapshot.class).orElse(null);
        LocatableBlock locatableBlock = null;
        TileEntity tileEntitySource = null;
        Entity entitySource = null;
        if (blockSource == null) {
            locatableBlock = cause.first(LocatableBlock.class).orElse(null);
            if (locatableBlock == null) {
                entitySource = cause.first(Entity.class).orElse(null);
            }
            if (locatableBlock == null && entitySource == null) {
                tileEntitySource = cause.first(TileEntity.class).orElse(null);
            }
        }

        GDClaim sourceClaim = null;
        if (blockSource != null) {
            sourceClaim = this.dataStore.getClaimAt(blockSource.getLocation().get());
        } else if (locatableBlock != null) {
            sourceClaim = this.dataStore.getClaimAt(locatableBlock.getLocation());
        } else if (tileEntitySource != null) {
            sourceClaim = this.dataStore.getClaimAt(tileEntitySource.getLocation());
        } else if (entitySource != null) {
            Entity entity = entitySource;
            if (entity instanceof Player) {
                Player player = (Player) entity;
                GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
                sourceClaim = this.dataStore.getClaimAtPlayer(playerData, player.getLocation());
            } else {
                sourceClaim = this.dataStore.getClaimAt(entity.getLocation());
            }
        }

        return sourceClaim;
    }

    // TODO: Add configuration for distance between claims
    private boolean checkSurroundings(org.spongepowered.api.event.Event event, Location<World> location, Player player, GDPlayerData playerData, GDClaim targetClaim) {
        if (playerData == null) {
            return true;
        }
        // Don't allow players to break blocks next to land they do not own
        if (!playerData.canIgnoreClaim(targetClaim)) {
            // check surrounding blocks for access
            for (Direction direction : BlockUtil.CARDINAL_SET) {
                Location<World> loc = location.getBlockRelative(direction);
                if (!(loc.getTileEntity().isPresent())) {
                    continue;
                }
                final GDClaim claim = this.dataStore.getClaimAt(loc, targetClaim);
                if (!claim.isWilderness() && !targetClaim.equals(claim)) {
                    Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, loc, claim, Flags.BLOCK_BREAK, player, loc.getBlock(), player, TrustTypes.BUILDER, true);
                    if (result != Tristate.TRUE) {
                        final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_BUILD_NEAR_CLAIM,
                                ImmutableMap.of(
                                "player", claim.getOwnerName()));
                        GriefDefenderPlugin.sendClaimDenyMessage(claim, player, message);
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
