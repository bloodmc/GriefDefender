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
package com.griefdefender.permission;

import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.google.inject.Singleton;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.Subject;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimContexts;
import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.api.claim.TrustType;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.ContextKeys;
import com.griefdefender.api.permission.PermissionManager;
import com.griefdefender.api.permission.PermissionResult;
import com.griefdefender.api.permission.ResultTypes;
import com.griefdefender.api.permission.flag.Flag;
import com.griefdefender.api.permission.flag.FlagData;
import com.griefdefender.api.permission.flag.FlagDefinition;
import com.griefdefender.api.permission.flag.Flags;
import com.griefdefender.api.permission.option.Option;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.api.permission.option.type.CreateModeType;
import com.griefdefender.api.permission.option.type.CreateModeTypes;
import com.griefdefender.api.permission.option.type.GameModeType;
import com.griefdefender.api.permission.option.type.GameModeTypes;
import com.griefdefender.api.permission.option.type.WeatherType;
import com.griefdefender.api.permission.option.type.WeatherTypes;
import com.griefdefender.cache.EventResultCache;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.command.CommandHelper;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.configuration.category.BanCategory;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.event.GDFlagPermissionEvent;
import com.griefdefender.internal.registry.EntityTypeRegistryModule;
import com.griefdefender.internal.registry.GDEntityType;
import com.griefdefender.internal.util.BlockUtil;
import com.griefdefender.internal.util.NMSUtil;
import com.griefdefender.registry.FlagRegistryModule;
import com.griefdefender.util.EntityUtils;
import com.griefdefender.util.PermissionUtil;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.spongeapi.TextAdapter;
import net.kyori.text.format.TextColor;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayer;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.CatalogType;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.vehicle.Boat;
import org.spongepowered.api.entity.vehicle.minecart.Minecart;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.block.NotifyNeighborBlockEvent;
import org.spongepowered.api.event.cause.EventContextKey;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.equipment.EquipmentTypes;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.LocatableBlock;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.common.SpongeImplHooks;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class GDPermissionManager implements PermissionManager {

    private static final Pattern BLOCKSTATE_PATTERN = Pattern.compile("(?:\\w+=\\w+,)*\\w+=\\w+", Pattern.MULTILINE);

    private static GDPermissionManager instance;
    public boolean blacklistCheck = false;
    private Event currentEvent;
    private Location<World> eventLocation;
    private GDPermissionHolder eventSubject;
    private GDPlayerData eventPlayerData;
    private String eventSourceId = "none";
    private String eventTargetId = "none";
    private Set<Context> eventContexts = new HashSet<>();
    private Component eventMessage;
    private static final List<EventContextKey> IGNORED_EVENT_CONTEXTS = Arrays.asList(EventContextKeys.CREATOR, 
            EventContextKeys.FAKE_PLAYER, EventContextKeys.NOTIFIER, EventContextKeys.OWNER, EventContextKeys.PISTON_EXTEND, 
            EventContextKeys.PISTON_RETRACT, EventContextKeys.SERVICE_MANAGER, EventContextKeys.PLAYER_BREAK, EventContextKeys.SPAWN_TYPE);
    private static final Pattern PATTERN_META = Pattern.compile("\\.[\\d+]*$");

    private enum BanType {
        BLOCK,
        ENTITY,
        ITEM
    }

    public GDPermissionHolder getDefaultHolder() {
        return GriefDefenderPlugin.DEFAULT_HOLDER;
    }

    @Override
    public Tristate getActiveFlagPermissionValue(Claim claim, Subject subject, Flag flag, Object source, Object target, Set<Context> contexts, TrustType type, boolean checkOverride) {
        return getFinalPermission(null, null, contexts, claim, flag, source, target, (GDPermissionHolder) subject, null, checkOverride);
    }

    public Tristate getFinalPermission(Event event, Location<World> location, Claim claim, Flag flag, Object source, Object target, GDPermissionHolder permissionHolder) {
        return getFinalPermission(event, location, claim, flag, source, target, permissionHolder, null, false);
    }

    public Tristate getFinalPermission(Event event, Location<World> location, Claim claim, Flag flag, Object source, Object target, Player player) {
        final GDPermissionHolder permissionHolder = PermissionHolderCache.getInstance().getOrCreateUser(player);
        return getFinalPermission(event, location, claim, flag, source, target, permissionHolder, null, false);
    }

    public Tristate getFinalPermission(Event event, Location<World> location, Claim claim, Flag flag, Object source, Object target, Player player, boolean checkOverride) {
        final GDPermissionHolder permissionHolder = PermissionHolderCache.getInstance().getOrCreateUser(player);
        return getFinalPermission(event, location, claim, flag, source, target, permissionHolder, null, checkOverride);
    }

    public Tristate getFinalPermission(Event event, Location<World> location, Claim claim, Flag flag, Object source, Object target, GDPermissionHolder permissionHolder, boolean checkOverride) {
        return getFinalPermission(event, location, claim, flag, source, target, permissionHolder, null, checkOverride);
    }

    public Tristate getFinalPermission(Event event, Location<World> location, Claim claim, Flag flag, Object source, Object target, User user) {
        final GDPermissionHolder permissionHolder = PermissionHolderCache.getInstance().getOrCreateUser(user);
        return getFinalPermission(event, location, claim, flag, source, target, permissionHolder, null, false);
    }

    public Tristate getFinalPermission(Event event, Location<World> location, Claim claim, Flag flag, Object source, Object target, User user, boolean checkOverride) {
        final GDPermissionHolder permissionHolder = PermissionHolderCache.getInstance().getOrCreateUser(user);
        return getFinalPermission(event, location, claim, flag, source, target, permissionHolder, null, checkOverride);
    }

    public Tristate getFinalPermission(Event event, Location<World> location, Claim claim, Flag flag, Object source, Object target, Player player, TrustType type, boolean checkOverride) {
        final GDPermissionHolder permissionHolder = PermissionHolderCache.getInstance().getOrCreateUser(player);
        return getFinalPermission(event, location, claim, flag, source, target, permissionHolder, type, checkOverride);
    }

    public Tristate getFinalPermission(Event event, Location<World> location, Claim claim, Flag flag, Object source, Object target, User user, TrustType type, boolean checkOverride) {
        final GDPermissionHolder permissionHolder = PermissionHolderCache.getInstance().getOrCreateUser(user);
        return getFinalPermission(event, location, claim, flag, source, target, permissionHolder, type, checkOverride);
    }

    public Tristate getFinalPermission(Event event, Location<World> location, Claim claim, Flag flag, Object source, Object target, GDPermissionHolder subject, TrustType type, boolean checkOverride) {
        final Set<Context> contexts = new HashSet<>();
        for (Map.Entry<EventContextKey<?>, Object> mapEntry : event.getContext().asMap().entrySet()) {
            if (IGNORED_EVENT_CONTEXTS.contains(mapEntry.getKey())) {
                continue;
            }
            final String[] parts = mapEntry.getKey().getId().split(":");
            String contextId = getPermissionIdentifier(mapEntry.getValue(), false);
            final int index = contextId.indexOf(".");
            if (index > -1) {
                contextId = contextId.substring(0, index);
            }
            if (contextId.isEmpty()) {
                continue;
            }
            final Context context = new Context(parts[1], contextId);
            contexts.add(context);
        }
        return getFinalPermission(event, location, contexts, claim, flag, source, target, subject, type, checkOverride);
    }

    public Tristate getFinalPermission(Event event, Location<World> location, Set<Context> contexts, Claim claim, Flag flag, Object source, Object target, GDPermissionHolder permissionHolder, TrustType type, boolean checkOverride) {
        if (claim == null) {
            return Tristate.TRUE;
        }

        GDPlayerData playerData = null;
        final GDPermissionUser user = permissionHolder instanceof GDPermissionUser ? (GDPermissionUser) permissionHolder : null;
        this.eventSubject = null;
        boolean isFakePlayer = false;
        if (user != null) {
            this.eventSubject = user;
            if (user.getOnlinePlayer() != null) {
                playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(((GDClaim) claim).getWorldUniqueId(), user.getUniqueId());
                if (NMSUtil.getInstance().isFakePlayer((Entity) user.getOnlinePlayer())) {
                    isFakePlayer = true;
                }
            }
        }

        this.currentEvent = event;
        this.eventLocation = location;
        // refresh contexts
        this.eventContexts = new HashSet<>();
        /*final ItemStackSnapshot usedItem = event.getContext().get(EventContextKeys.USED_ITEM).orElse(null);
        final DamageType damageType = event.getContext().get(EventContextKeys.DAMAGE_TYPE).orElse(null);
        if (usedItem != null) {
            //final String id = getPermissionIdentifier(usedItem);
            this.eventContexts.add(new Context("used_item", usedItem.getType().getId()));
        }
        if (damageType != null) {
            //final String id = getPermissionIdentifier(damageType);
            this.eventContexts.add(new Context("damage_type", damageType.getId()));
        }*/
        if (isFakePlayer && source instanceof Game) {
            source = user.getOnlinePlayer();
        }
        if (user != null) {
            if (user.getOnlinePlayer() != null) {
                this.addPlayerContexts(user.getOnlinePlayer(), contexts);
            }
        }

        final Set<Context> sourceContexts = this.getPermissionContexts((GDClaim) claim, source, true);
        if (sourceContexts == null) {
            return Tristate.FALSE;
        }

        final Set<Context> targetContexts = this.getPermissionContexts((GDClaim) claim, target, false);
        if (targetContexts == null) {
            return Tristate.FALSE;
        }
        contexts.addAll(sourceContexts);
        contexts.addAll(targetContexts);
        contexts.add(((GDClaim) claim).getWorldContext());
        this.eventContexts = contexts;
        this.eventPlayerData = playerData;
        final String targetPermission = flag.getPermission();

        if (flag == Flags.ENTITY_SPAWN) {
            // Check spawn limit
            final int spawnLimit = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), GriefDefenderPlugin.DEFAULT_HOLDER, Options.SPAWN_LIMIT, claim, new HashSet<>(contexts));
            if (spawnLimit > -1) {
                if (target instanceof Entity) {
                    final Entity entity = (Entity) target;
                    final int currentEntityCount = ((GDClaim) claim).countEntities(entity .getType());
                    if (currentEntityCount >= spawnLimit) {
                        if (source instanceof Player) {
                            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.OPTION_APPLY_SPAWN_LIMIT,
                                    ImmutableMap.of(
                                    "type", entity.getType().getId(),
                                    "limit", spawnLimit));
                            GriefDefenderPlugin.sendMessage((Player) source, message);
                        }
                        return this.processResult(claim, flag.getPermission(), "spawn-limit", Tristate.FALSE, this.eventSubject);
                    }
                }
            }
        }

        // If player can ignore admin claims and is currently ignoring , allow
        if (user != null && playerData != null && !playerData.debugClaimPermissions && playerData.canIgnoreClaim(claim)) {
            return processResult(claim, targetPermission, "ignore", Tristate.TRUE, user);
        }
        if (checkOverride) {
            Tristate override = Tristate.UNDEFINED;
            // First check for claim flag overrides
            override = getFlagOverride(claim, permissionHolder == null ? GriefDefenderPlugin.DEFAULT_HOLDER : permissionHolder, playerData, targetPermission);
            if (override != Tristate.UNDEFINED) {
                return override;
            }
        }

        if (playerData != null && user != null) {
            if (playerData.debugClaimPermissions) {
                if (type != null && claim.isUserTrusted(user.getUniqueId(), type)) {
                    return processResult(claim, targetPermission, type.getName().toLowerCase(), Tristate.TRUE, user);
                }
                return getClaimFlagPermission(claim, targetPermission);
            }
             // Check for ignoreclaims after override and debug checks
            if (playerData.canIgnoreClaim(claim)) {
                return processResult(claim, targetPermission, "ignore", Tristate.TRUE, user);
            }
        }
        if (user != null) {
            if (type != null) {
                if (((GDClaim) claim).isUserTrusted(user, type)) {
                    return processResult(claim, targetPermission, type.getName().toLowerCase(), Tristate.TRUE, permissionHolder);
                }
            }
            return getUserPermission(user, claim, targetPermission);
        }

        return getClaimFlagPermission(claim, targetPermission);
    }

    private Tristate getUserPermission(GDPermissionHolder holder, Claim claim, String permission) {
        final List<Claim> inheritParents = claim.getInheritedParents();
        final Set<Context> contexts = new HashSet<>();
        contexts.addAll(this.eventContexts);

        for (Claim parentClaim : inheritParents) {
            GDClaim parent = (GDClaim) parentClaim;
            // check parent context
            contexts.add(parent.getContext());
            Tristate value = PermissionUtil.getInstance().getPermissionValue((GDClaim) claim, holder, permission, contexts);
            if (value != Tristate.UNDEFINED) {
                return processResult(claim, permission, value, holder);
            }

            contexts.remove(parent.getContext());
        }

        // Check claim context
        contexts.add(claim.getContext());
        Tristate value = PermissionUtil.getInstance().getPermissionValue((GDClaim) claim, holder, permission, contexts);
        if (value != Tristate.UNDEFINED) {
            return processResult(claim, permission, value, holder);
        }
        // Check default type context
        contexts.add(claim.getType().getContext());
        value = PermissionUtil.getInstance().getPermissionValue((GDClaim) claim, holder, permission, contexts);
        if (value != Tristate.UNDEFINED) {
            return processResult(claim, permission, value, holder);
        }

        if (holder == GriefDefenderPlugin.DEFAULT_HOLDER) {
            return getFlagDefaultPermission(claim, permission, contexts);
        }

        return getClaimFlagPermission(claim, permission, contexts);
    }

    private Tristate getClaimFlagPermission(Claim claim, String permission) {
        return this.getClaimFlagPermission(claim, permission, new HashSet<>());
    }

    private Tristate getClaimFlagPermission(Claim claim, String permission, Set<Context> contexts) {
        if (contexts.isEmpty()) {
            final List<Claim> inheritParents = claim.getInheritedParents();
            contexts.addAll(this.eventContexts);
            for (Claim parentClaim : inheritParents) {
                GDClaim parent = (GDClaim) parentClaim;
                // check parent context
                contexts.add(parent.getContext());
                Tristate value = PermissionUtil.getInstance().getPermissionValue((GDClaim) claim, GriefDefenderPlugin.DEFAULT_HOLDER, permission, contexts);
                if (value != Tristate.UNDEFINED) {
                    return processResult(claim, permission, value, GriefDefenderPlugin.DEFAULT_HOLDER);
                }

                contexts.remove(parent.getContext());
            }
            contexts.add(claim.getContext());
        }

        Tristate value = PermissionUtil.getInstance().getPermissionValue((GDClaim) claim, GriefDefenderPlugin.DEFAULT_HOLDER, permission, contexts);
        if (value != Tristate.UNDEFINED) {
            return processResult(claim, permission, value, GriefDefenderPlugin.DEFAULT_HOLDER);
        }

        return getFlagDefaultPermission(claim, permission, contexts);
    }

    // Only uses world and claim type contexts
    private Tristate getFlagDefaultPermission(Claim claim, String permission, Set<Context> contexts) {
        contexts.add(claim.getDefaultTypeContext());
        Tristate value = PermissionUtil.getInstance().getPermissionValue((GDClaim) claim, GriefDefenderPlugin.DEFAULT_HOLDER, permission, contexts);
        if (value != Tristate.UNDEFINED) {
            return processResult(claim, permission, value, GriefDefenderPlugin.DEFAULT_HOLDER);
        }
        contexts.remove(claim.getDefaultTypeContext());
        contexts.add(ClaimContexts.GLOBAL_DEFAULT_CONTEXT);
        value = PermissionUtil.getInstance().getPermissionValue((GDClaim) claim, GriefDefenderPlugin.DEFAULT_HOLDER, permission, contexts);
        if (value != Tristate.UNDEFINED) {
            return processResult(claim, permission, value, GriefDefenderPlugin.DEFAULT_HOLDER);
        }

        return processResult(claim, permission, Tristate.UNDEFINED, GriefDefenderPlugin.DEFAULT_HOLDER);
    }

    private Tristate getFlagOverride(Claim claim, GDPermissionHolder permissionHolder, GDPlayerData playerData, String flagPermission) {
        if (!((GDClaim) claim).getInternalClaimData().allowFlagOverrides()) {
            return Tristate.UNDEFINED;
        }

        Player player = null;
        Set<Context> contexts = new HashSet<>();
        if (claim.isAdminClaim()) {
            contexts.add(ClaimContexts.ADMIN_OVERRIDE_CONTEXT);
            //contexts.add(claim.world.getContext());
        } else if (claim.isTown()) {
            contexts.add(ClaimContexts.TOWN_OVERRIDE_CONTEXT);
            //contexts.add(claim.world.getContext());
        } else if (claim.isBasicClaim()) {
            contexts.add(ClaimContexts.BASIC_OVERRIDE_CONTEXT);
            //contexts.add(claim.world.getContext());
        } else if (claim.isWilderness()) {
            contexts.add(ClaimContexts.WILDERNESS_OVERRIDE_CONTEXT);
            player = permissionHolder instanceof GDPermissionUser ? ((GDPermissionUser) permissionHolder).getOnlinePlayer() : null;
        }

        contexts.add(((GDClaim) claim).getWorldContext());
        contexts.add(claim.getOverrideClaimContext());
        contexts.add(ClaimContexts.GLOBAL_OVERRIDE_CONTEXT);
        contexts.addAll(this.eventContexts);

        Tristate value = PermissionUtil.getInstance().getPermissionValue((GDClaim) claim, permissionHolder, flagPermission, contexts);
       /* if (value == Tristate.UNDEFINED) {
            // Check claim specific override
            contexts = PermissionUtils.getActiveContexts(subject, playerData, claim);
            contexts.add(claim.getContext());
            contexts.add(ClaimContexts.CLAIM_OVERRIDE_CONTEXT);
            value = subject.getPermissionValue(contexts, flagPermission);
        }*/
        if (value != Tristate.UNDEFINED) {
            if (value == Tristate.FALSE) {
                this.eventMessage = MessageCache.getInstance().PERMISSION_OVERRIDE_DENY;
            }
            return processResult(claim, flagPermission, value, permissionHolder);
        }
        if (permissionHolder != GriefDefenderPlugin.DEFAULT_HOLDER) {
            return getFlagOverride(claim, GriefDefenderPlugin.DEFAULT_HOLDER, playerData, flagPermission);
        }

        return Tristate.UNDEFINED;
    }

    public Tristate processResult(Claim claim, String permission, Tristate permissionValue, GDPermissionHolder permissionHolder) {
        return processResult(claim, permission, null, permissionValue, permissionHolder);
    }

    public Tristate processResult(Claim claim, String permission, String trust, Tristate permissionValue, GDPermissionHolder permissionHolder) {
        if (GriefDefenderPlugin.debugActive) {
            // Use the event subject always if available
            // This prevents debug showing 'default' for users
            if (eventSubject != null) {
                permissionHolder = eventSubject;
            } else if (permissionHolder == null) {
                final Object source = GDCauseStackManager.getInstance().getCurrentCause().root();
                if (source instanceof GDPermissionUser) {
                    permissionHolder = (GDPermissionUser) source;
                } else {
                    permissionHolder = GriefDefenderPlugin.DEFAULT_HOLDER;
                }
            }

            if (this.currentEvent != null && (this.currentEvent instanceof NotifyNeighborBlockEvent)) {
                if (((GDClaim) claim).getWorld().getProperties().getTotalTime() % 100 != 0L) {
                    return permissionValue;
                }
            }

            GriefDefenderPlugin.addEventLogEntry(this.currentEvent, claim, this.eventLocation, this.eventSourceId, this.eventTargetId, this.eventSubject == null ? permissionHolder : this.eventSubject, permission, trust, permissionValue, this.eventContexts);
        }

        if (eventPlayerData != null && eventPlayerData.eventResultCache != null) {
            final Flag flag = FlagRegistryModule.getInstance().getById(permission).orElse(null);
            if (flag != null) {
                eventPlayerData.eventResultCache = new EventResultCache((GDClaim) claim, flag.getName().toLowerCase(), permissionValue, trust);
            }
        }
        return permissionValue;
    }

    public void addEventLogEntry(Event event, Claim claim, Location<World> location, Object source, Object target, User user, Flag flag, String trust, Tristate result) {
        final GDPermissionHolder holder = PermissionHolderCache.getInstance().getOrCreateUser(user);
        addEventLogEntry(event, claim, location, source, target, holder, flag, trust, result);
    }

    // Used for situations where events are skipped for perf reasons
    public void addEventLogEntry(Event event, Claim claim, Location<World> location, Object source, Object target, GDPermissionHolder permissionSubject, Flag flag, String trust, Tristate result) {
        this.addEventLogEntry(event, claim, location, source, target, permissionSubject, flag.getPermission(), trust, result);
    }
    public void addEventLogEntry(Event event, Claim claim, Location<World> location, Object source, Object target, GDPermissionHolder permissionSubject, String permission, String trust, Tristate result) {
        if (GriefDefenderPlugin.debugActive) {
            String sourceId = getPermissionIdentifier(source, true);
            String targetId = getPermissionIdentifier(target);
            if (permissionSubject == null) {
                permissionSubject = GriefDefenderPlugin.DEFAULT_HOLDER;
            }
            GriefDefenderPlugin.addEventLogEntry(event, claim, location, sourceId, targetId, permissionSubject, permission, trust, result, this.eventContexts);
        }
    }

    public String getPermissionIdentifier(Object obj) {
        return getPermissionIdentifier(obj, false);
    }

    public String getPermissionIdentifier(Object obj, boolean isSource) {
        if (obj != null) {
            if (obj instanceof Entity) {
                return populateEventSourceTarget(NMSUtil.getInstance().getEntityId((Entity) obj, isSource), isSource);
            } else if (obj instanceof EntityType) {
                final String id = ((EntityType) obj).getId();
                return populateEventSourceTarget(id, isSource);
            } else if (obj instanceof BlockType) {
                final String id = ((BlockType) obj).getId();
                return populateEventSourceTarget(id, isSource);
            } else if (obj instanceof BlockSnapshot) {
                final BlockSnapshot blockSnapshot = (BlockSnapshot) obj;
                final BlockState blockstate = blockSnapshot.getState();
                String id = blockstate.getType().getId() + "." + BlockUtil.getInstance().getBlockStateMeta(blockstate);
                return populateEventSourceTarget(id, isSource);
            } else if (obj instanceof BlockState) {
                final BlockState blockstate = (BlockState) obj;
                final String id = blockstate.getType().getId() + "." + BlockUtil.getInstance().getBlockStateMeta(blockstate);
                return populateEventSourceTarget(id, isSource);
            } else if (obj instanceof LocatableBlock) {
                final LocatableBlock locatableBlock = (LocatableBlock) obj;
                final BlockState blockstate = locatableBlock.getBlockState();
                final String id = blockstate.getType().getId() + "." + BlockUtil.getInstance().getBlockStateMeta(blockstate);
                return populateEventSourceTarget(id, isSource);
            } else if (obj instanceof TileEntity) {
                TileEntity tileEntity = (TileEntity) obj;
                final String id = tileEntity.getType().getId().toLowerCase();
                return populateEventSourceTarget(id, isSource);
            } else if (obj instanceof ItemStack) {
                return populateEventSourceTarget(NMSUtil.getInstance().getItemStackId((ItemStack) obj), isSource);
            } else if (obj instanceof ItemType) {
                final String id = ((ItemType) obj).getId().toLowerCase();
                populateEventSourceTarget(id, isSource);
                return id;
            } else if (obj instanceof EntityDamageSource) {
                final EntityDamageSource damageSource = (EntityDamageSource) obj;
                Entity sourceEntity = damageSource.getSource();

                if (this.eventSubject == null && sourceEntity instanceof User) {
                    this.eventSubject = PermissionHolderCache.getInstance().getOrCreateUser((User) sourceEntity);
                }

                return getPermissionIdentifier(sourceEntity, isSource);
            } else if (obj instanceof DamageSource) {
                final DamageSource damageSource = (DamageSource) obj;
                String id = damageSource.getType().getId();
                if (!id.contains(":")) {
                    id = "minecraft:" + id;
                }

                return populateEventSourceTarget(id, isSource);
            } else if (obj instanceof ItemStackSnapshot) {
                final ItemStackSnapshot itemSnapshot = ((ItemStackSnapshot) obj);
                return getPermissionIdentifier(itemSnapshot.createStack(), isSource);
            } else if (obj instanceof CatalogType) {
                final String id = ((CatalogType) obj).getId();
                return populateEventSourceTarget(id, isSource);
            } else if (obj instanceof String) {
                final String id = obj.toString().toLowerCase();
                return populateEventSourceTarget(id, isSource);
            } else if (obj instanceof PluginContainer) {
                final String id = ((PluginContainer) obj).getId();
                return populateEventSourceTarget(id, isSource);
            } else if (obj instanceof Inventory) {
                return ((Inventory) obj).getArchetype().getId();
            } else if (obj instanceof Location) {
                return getPermissionIdentifier(((Location) obj).getBlock(), isSource);
            }
        }

        populateEventSourceTarget("none", isSource);
        return "";
    }

    public Set<Context> getPermissionContexts(GDClaim claim, Object obj, boolean isSource) {
        final Set<Context> contexts = new HashSet<>();
        if (obj != null) {
            if (obj instanceof Entity) {

                Entity targetEntity = (Entity) obj;
                net.minecraft.entity.Entity mcEntity = null;
                if (targetEntity instanceof net.minecraft.entity.Entity) {
                    mcEntity = (net.minecraft.entity.Entity) targetEntity;
                }
                String id = "";
                if (targetEntity.getType() != null) {
                    id = targetEntity.getType().getId();
                }

                if (mcEntity != null && id.contains("unknown") && SpongeImplHooks.isFakePlayer(mcEntity)) {
                    final String modId = SpongeImplHooks.getModIdFromClass(mcEntity.getClass());
                    id = modId + ":fakeplayer_" + EntityUtils.getFriendlyName(mcEntity).toLowerCase();
                } else if (id.equals("unknown:unknown") && obj instanceof EntityPlayer) {
                    id = "minecraft:player";
                }

                String modId = "";
                if (mcEntity != null && targetEntity instanceof Living) {
                    String[] parts = id.split(":");
                    if (parts.length > 1) {
                        modId = parts[0];
                        String name = parts[1];
                        if (modId.equalsIgnoreCase("pixelmon") && modId.equalsIgnoreCase(name)) {
                            name = EntityUtils.getFriendlyName(mcEntity).toLowerCase();
                            id = modId + ":" + name;
                        }
                    }
                }

                GDEntityType type = EntityTypeRegistryModule.getInstance().getById(targetEntity.getType().getId()).orElse(null);
                if (type == null) {
                    EntityTypeRegistryModule.getInstance().registerAdditionalCatalog(targetEntity.getType());
                    type = EntityTypeRegistryModule.getInstance().getById(targetEntity.getType().getId()).orElse(null);
                }

                if (type != null && !(targetEntity instanceof Player)) {
                    addCustomEntityTypeContexts(targetEntity, contexts, type, modId, id, isSource);
                }

                if (this.isObjectIdBanned(claim, id, BanType.ENTITY)) {
                    return null;
                }
                return populateEventSourceTargetContext(contexts, id, isSource);
            } else if (obj instanceof EntityType) {
                final String id = ((EntityType) obj).getId();
                return populateEventSourceTargetContext(contexts, id, isSource);
            } else if (obj instanceof BlockType) {
                final String id = ((BlockType) obj).getId();
                if (this.isObjectIdBanned(claim, id, BanType.BLOCK)) {
                    return null;
                }
                populateEventSourceTargetContext(contexts, id, isSource);
            } else if (obj instanceof BlockSnapshot) {
                final BlockSnapshot blockSnapshot = (BlockSnapshot) obj;
                final BlockState blockstate = blockSnapshot.getState();
                return this.getPermissionContexts(claim, blockstate, isSource);
            } else if (obj instanceof BlockState) {
                final BlockState blockstate = (BlockState) obj;
                if (this.isObjectIdBanned(claim, blockstate.getType().getId(), BanType.BLOCK)) {
                    return null;
                }
                final String id = blockstate.getType().getId();
                this.addBlockPropertyContexts(contexts, blockstate);
                if (this.isObjectIdBanned(claim, id, BanType.BLOCK)) {
                    return null;
                }
                return populateEventSourceTargetContext(contexts, id, isSource);
            } else if (obj instanceof LocatableBlock) {
                final LocatableBlock locatableBlock = (LocatableBlock) obj;
                final BlockState blockstate = locatableBlock.getBlockState();
                return this.getPermissionContexts(claim, blockstate, isSource);
            } else if (obj instanceof TileEntity) {
                TileEntity tileEntity = (TileEntity) obj;
                final String id = tileEntity.getType().getId().toLowerCase();
                return populateEventSourceTargetContext(contexts, id, isSource);
            } else if (obj instanceof ItemStack) {
                final ItemStack itemstack = (ItemStack) obj;
                if (NMSUtil.getInstance().isItemFood(itemstack.getType())) {
                    if (isSource) {
                        contexts.add(ContextGroups.SOURCE_FOOD);
                    } else {
                        contexts.add(ContextGroups.TARGET_FOOD);
                    }
                }
                if (this.isObjectIdBanned(claim, itemstack.getType().getId(), BanType.ITEM)) {
                    return null;
                }
                final String id = NMSUtil.getInstance().getItemStackId(itemstack);
                contexts.add(new Context("meta", NMSUtil.getInstance().getItemStackMeta(itemstack)));
                if (this.isObjectIdBanned(claim, id, BanType.ITEM)) {
                    return null;
                }
                return populateEventSourceTargetContext(contexts, id, isSource);
            } else if (obj instanceof ItemType) {
                final String id = ((ItemType) obj).getId().toLowerCase();
                if (NMSUtil.getInstance().isItemFood(((ItemType) obj))) {
                    if (isSource) {
                        contexts.add(ContextGroups.SOURCE_FOOD);
                    } else {
                        contexts.add(ContextGroups.TARGET_FOOD);
                    }
                }
                if (this.isObjectIdBanned(claim, id, BanType.ITEM)) {
                    return null;
                }
                return populateEventSourceTargetContext(contexts, id, isSource);
            } else if (obj instanceof EntityDamageSource) {
                final EntityDamageSource damageSource = (EntityDamageSource) obj;
                Entity sourceEntity = damageSource.getSource();

                if (this.eventSubject == null && sourceEntity instanceof User) {
                    this.eventSubject = PermissionHolderCache.getInstance().getOrCreateUser((User) sourceEntity);
                }

                return this.getPermissionContexts(claim, sourceEntity, isSource);
            } else if (obj instanceof DamageSource) {
                final DamageSource damageSource = (DamageSource) obj;
                String id = damageSource.getType().getId();
                if (!id.contains(":")) {
                    id = "minecraft:" + id;
                }

                return populateEventSourceTargetContext(contexts, id, isSource);
            } else if (obj instanceof ItemStackSnapshot) {
                final ItemStackSnapshot itemSnapshot = ((ItemStackSnapshot) obj);
                return this.getPermissionContexts(claim, itemSnapshot.createStack(), isSource);
            } else if (obj instanceof CatalogType) {
                final String id = ((CatalogType) obj).getId();
                return populateEventSourceTargetContext(contexts, id, isSource);
            } else if (obj instanceof String) {
                final String id = obj.toString().toLowerCase();
                return populateEventSourceTargetContext(contexts, id, isSource);
            } else if (obj instanceof PluginContainer) {
                final String id = ((PluginContainer) obj).getId();
                return populateEventSourceTargetContext(contexts, id, isSource);
            } else if (obj instanceof Inventory) {
                return populateEventSourceTargetContext(contexts, ((Inventory) obj).getArchetype().getId(), isSource);
            } else if (obj instanceof Location) {
                return this.getPermissionContexts(claim, ((Location) obj).getBlock(), isSource);
            }
        }

        return contexts;
    }

    public boolean isObjectIdBanned(GDClaim claim, String id, BanType type) {
        if (id.equalsIgnoreCase("player")) {
            return false;
        }

        GDPermissionUser user = null;
        if (this.eventSubject != null && this.eventSubject instanceof GDPermissionUser) {
            user = (GDPermissionUser) this.eventSubject;
            if (user.getInternalPlayerData() != null && user.getInternalPlayerData().canIgnoreClaim(claim)) {
                return false;
            }
        }

        final String permission = StringUtils.replace(id, ":", ".");
        Component banReason = null;
        final BanCategory banCategory = GriefDefenderPlugin.getGlobalConfig().getConfig().bans;
        if (type == BanType.BLOCK) {
            for (Entry<String, Component> banId : banCategory.getBlockMap().entrySet()) {
                if (FilenameUtils.wildcardMatch(id, banId.getKey())) {
                    banReason = GriefDefenderPlugin.getGlobalConfig().getConfig().bans.getBlockBanReason(banId.getKey());
                    if (banReason != null && banReason.equals(TextComponent.empty())) {
                        banReason = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.PERMISSION_BAN_BLOCK, 
                                ImmutableMap.of("id", TextComponent.of(id, TextColor.GOLD)));
                    }
                    break;
                }
            }
        } else if (type == BanType.ITEM) {
            for (Entry<String, Component> banId : banCategory.getItemMap().entrySet()) {
                if (FilenameUtils.wildcardMatch(id, banId.getKey())) {
                    banReason = GriefDefenderPlugin.getGlobalConfig().getConfig().bans.getItemBanReason(banId.getKey());
                    if (banReason != null && banReason.equals(TextComponent.empty())) {
                        banReason = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.PERMISSION_BAN_ITEM, 
                                ImmutableMap.of("id", TextComponent.of(id, TextColor.GOLD)));
                    }
                }
            }
        } else if (type == BanType.ENTITY) {
            for (Entry<String, Component> banId : banCategory.getEntityMap().entrySet()) {
                if (FilenameUtils.wildcardMatch(id, banId.getKey())) {
                    banReason = GriefDefenderPlugin.getGlobalConfig().getConfig().bans.getEntityBanReason(banId.getKey());
                    if (banReason != null && banReason.equals(TextComponent.empty())) {
                        banReason = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.PERMISSION_BAN_ENTITY, 
                                ImmutableMap.of("id", TextComponent.of(id, TextColor.GOLD)));
                    }
                }
            }
        }

        if (banReason != null && user != null) {
            final Player player = user.getOnlinePlayer();
            if (player != null) {
                if (banReason.equals(TextComponent.empty())) {
                    banReason = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.PERMISSION_BAN_BLOCK, 
                            ImmutableMap.of("id", id));
                }
                TextAdapter.sendComponent(player, banReason);
                this.processResult(claim, permission, "banned", Tristate.FALSE, user);
                return true;
            }
        }
        if (banReason != null) {
            // Detected ban
            this.processResult(claim, permission, "banned", Tristate.FALSE, this.eventSubject);
            return true;
        }
        return false;
    }

    public void addCustomEntityTypeContexts(Entity targetEntity, Set<Context> contexts, GDEntityType type, String modId, String id, boolean isSource) {
        if (isSource) {
            contexts.add(ContextGroups.SOURCE_ALL);
            if (modId != null && !modId.isEmpty()) {
                contexts.add(new Context(ContextKeys.SOURCE, modId + ":" + ContextGroupKeys.ALL));
            }
        } else {
            contexts.add(ContextGroups.TARGET_ALL);
            if (modId != null && !modId.isEmpty()) {
                contexts.add(new Context(ContextKeys.TARGET, modId + ":" + ContextGroupKeys.ALL));
            }
        }
        // check vehicle
        if (targetEntity instanceof Boat || targetEntity instanceof Minecart) {
            if (isSource) {
                contexts.add(ContextGroups.SOURCE_VEHICLE);
            } else {
                contexts.add(ContextGroups.TARGET_VEHICLE);
            }
        }

        String creatureType = type.getEnumCreatureTypeId();
        if (creatureType == null) {
            // fallback check for mods
            if (targetEntity instanceof net.minecraft.entity.Entity) {
                final net.minecraft.entity.Entity mcEntity = (net.minecraft.entity.Entity) targetEntity;
                if (SpongeImplHooks.isCreatureOfType(mcEntity, EnumCreatureType.CREATURE)) {
                    type.setEnumCreatureType(EnumCreatureType.CREATURE);
                    creatureType = type.getEnumCreatureTypeId();
                } else if (SpongeImplHooks.isCreatureOfType(mcEntity, EnumCreatureType.MONSTER)) {
                    type.setEnumCreatureType(EnumCreatureType.MONSTER);
                    creatureType = type.getEnumCreatureTypeId();
                } else if (SpongeImplHooks.isCreatureOfType(mcEntity, EnumCreatureType.WATER_CREATURE)) {
                    type.setEnumCreatureType(EnumCreatureType.WATER_CREATURE);
                    creatureType = type.getEnumCreatureTypeId();
                } else if (SpongeImplHooks.isCreatureOfType(mcEntity, EnumCreatureType.AMBIENT)) {
                    type.setEnumCreatureType(EnumCreatureType.AMBIENT);
                    creatureType = type.getEnumCreatureTypeId();
                }
                if (type.getEnumCreatureType() == null) {
                    return;
                }

                creatureType = type.getEnumCreatureTypeId();
            } else {
                return;
            }
        }

        //contexts.add(new Context(contextKey, "#" + creatureType));
        if (creatureType.contains("animal")) {
            if (isSource) {
                contexts.add(ContextGroups.SOURCE_ANIMAL);
                if (modId != null && !modId.isEmpty()) {
                    contexts.add(new Context(ContextKeys.SOURCE, modId + ":" + ContextGroupKeys.ANIMAL));
                }
            } else {
                contexts.add(ContextGroups.TARGET_ANIMAL);
                if (modId != null && !modId.isEmpty()) {
                    contexts.add(new Context(ContextKeys.TARGET, modId + ":" + ContextGroupKeys.ANIMAL));
                }
            }
            this.checkPetContext(targetEntity, id, contexts);
        } else if (creatureType.contains("aquatic")) {
            if (isSource) {
                contexts.add(ContextGroups.SOURCE_AQUATIC);
                if (modId != null && !modId.isEmpty()) {
                    contexts.add(new Context(ContextKeys.SOURCE, modId + ":" + ContextGroupKeys.AQUATIC));
                }
            } else {
                contexts.add(ContextGroups.TARGET_AQUATIC);
                if (modId != null && !modId.isEmpty()) {
                    contexts.add(new Context(ContextKeys.TARGET, modId + ":" + ContextGroupKeys.AQUATIC));
                }
            }
            this.checkPetContext(targetEntity, id, contexts);
        } else if (creatureType.contains("monster")) {
            if (isSource) {
                contexts.add(ContextGroups.SOURCE_MONSTER);
                if (modId != null && !modId.isEmpty()) {
                    contexts.add(new Context(ContextKeys.SOURCE, modId + ":" + ContextGroupKeys.MONSTER));
                }
            } else {
                contexts.add(ContextGroups.TARGET_MONSTER);
                if (modId != null && !modId.isEmpty()) {
                    contexts.add(new Context(ContextKeys.TARGET, modId + ":" + ContextGroupKeys.MONSTER));
                }
            }
        }  else if (creatureType.contains("ambient")) {
            if (isSource) {
                contexts.add(ContextGroups.SOURCE_AMBIENT);
                if (modId != null && !modId.isEmpty()) {
                    contexts.add(new Context(ContextKeys.SOURCE, modId + ":" + ContextGroupKeys.AMBIENT));
                }
            } else {
                contexts.add(ContextGroups.TARGET_AMBIENT);
                if (modId != null && !modId.isEmpty()) {
                    contexts.add(new Context(ContextKeys.TARGET, modId + ":" + ContextGroupKeys.AMBIENT));
                }
            }
            this.checkPetContext(targetEntity, id, contexts);
        } else {
            if (isSource) {
                contexts.add(ContextGroups.SOURCE_MISC);
                if (modId != null && !modId.isEmpty()) {
                    contexts.add(new Context(ContextKeys.SOURCE, modId + ":" + ContextGroupKeys.MISC));
                }
            } else {
                contexts.add(ContextGroups.TARGET_MISC);
                if (modId != null && !modId.isEmpty()) {
                    contexts.add(new Context(ContextKeys.TARGET, modId + ":" + ContextGroupKeys.MISC));
                }
            }
        }
    }

    private void checkPetContext(Entity targetEntity, String id, Set<Context> contexts) {
        if (this.eventSubject != null && this.eventSubject instanceof GDPermissionUser) {
            final GDPermissionUser user = (GDPermissionUser) this.eventSubject;
            final UUID uuid = targetEntity.getCreator().orElse(null);
            if (uuid != null && uuid.equals(user.getUniqueId())) {
                contexts.add(new Context(ContextGroupKeys.PET, id));
            }
        }
    }

    private void addPlayerContexts(Player player, Set<Context> contexts) {
        if(!PermissionUtil.getInstance().containsKey(contexts, "used_item") && NMSUtil.getInstance().getActiveItem(player, this.currentEvent) != null) {
            final ItemStack stack = NMSUtil.getInstance().getActiveItem(player, this.currentEvent);
            if (!stack.isEmpty()) {
                contexts.add(new Context("used_item", getPermissionIdentifier(stack)));
                final Text displayName = stack.get(Keys.DISPLAY_NAME).orElse(null);
                if (displayName != null) {
                    String itemName = displayName.toPlain().replaceAll("[^A-Za-z0-9]", "").toLowerCase();
                    if (itemName != null && !itemName.isEmpty()) {
                        if (!itemName.contains(":")) {
                            itemName = "minecraft:" + itemName;
                        }
                        contexts.add(new Context("item_name", itemName));
                    }
                }
            }
        }
        final ItemStack helmet = player.getEquipped(EquipmentTypes.HEADWEAR).orElse(null);
        final ItemStack chestplate = player.getEquipped(EquipmentTypes.CHESTPLATE).orElse(null);
        final ItemStack leggings = player.getEquipped(EquipmentTypes.LEGGINGS).orElse(null);
        final ItemStack boots = player.getEquipped(EquipmentTypes.BOOTS).orElse(null);
        if (helmet != null && !helmet.isEmpty()) {
            contexts.add(new Context("helmet", getPermissionIdentifier(helmet)));
        }
        if (chestplate != null && !chestplate.isEmpty()) {
            contexts.add(new Context("chestplate", getPermissionIdentifier(chestplate)));
        }
        if (leggings != null && !leggings.isEmpty()) {
            contexts.add(new Context("leggings", getPermissionIdentifier(leggings)));
        }
        if (boots != null && !boots.isEmpty()) {
            contexts.add(new Context("boots", getPermissionIdentifier(boots)));
        }
    }

    private Set<Context> addBlockPropertyContexts(Set<Context> contexts, BlockState block) {
        Matcher matcher = BLOCKSTATE_PATTERN.matcher(block.toString());
        if (matcher.find()) {
            final String properties[] = matcher.group(0).split(",");
            for (String property : properties) {
                contexts.add(new Context("state", property.replace("=", ":")));
            }
        }
        return contexts;
    }

    public String getSourcePermission(String flagPermission) {
        final int index = flagPermission.indexOf(".source.");
        if (index != -1) {
            return flagPermission.substring(index + 8);
        }

        return null;
    }

    public String getTargetPermission(String flagPermission) {
        flagPermission = StringUtils.replace(flagPermission, "griefdefender.flag.", "");
        boolean found = false;
        for (Flag flag : FlagRegistryModule.getInstance().getAll()) {
            if (flagPermission.contains(flag.toString() + ".")) {
                found = true;
            }
            flagPermission = StringUtils.replace(flagPermission, flag.toString() + ".", "");
        }
        if (!found) {
            return null;
        }
        final int sourceIndex = flagPermission.indexOf(".source.");
        if (sourceIndex != -1) {
            flagPermission = StringUtils.replace(flagPermission, flagPermission.substring(sourceIndex, flagPermission.length()), "");
        }

        return flagPermission;
    }

    // Used for debugging
    public String getPermission(Object source, Object target, String flagPermission) {
        String sourceId = getPermissionIdentifier(source, true);
        String targetPermission = flagPermission;
        String targetId = getPermissionIdentifier(target);
        if (!targetId.isEmpty()) {
            if (!sourceId.isEmpty()) {
                // move target meta to end of permission
                Matcher m = PATTERN_META.matcher(targetId);
                String targetMeta = "";
                if (m.find()) {
                    targetMeta = m.group(0);
                    targetId = StringUtils.replace(targetId, targetMeta, "");
                }
                targetPermission += "." + targetId + ".source." + sourceId + targetMeta;
            } else {
                targetPermission += "." + targetId;
            }
        }
        targetPermission = StringUtils.replace(targetPermission, ":", ".");
        return targetPermission;
    }

    public String getIdentifierWithoutMeta(String targetId) {
        Matcher m = PATTERN_META.matcher(targetId);
        String targetMeta = "";
        if (m.find()) {
            targetMeta = m.group(0);
            targetId = StringUtils.replace(targetId, targetMeta, "");
        }
        return targetId;
    }

    private Set<Context> populateEventSourceTargetContext(Set<Context> contexts, String id, boolean isSource) {
        if (!id.contains(":")) {
            id = "minecraft:" + id;
        }
        final String[] parts = id.split(":");
        final String modId = parts[0];
        if (isSource) {
            this.eventSourceId = id.toLowerCase();
            contexts.add(new Context("source", this.eventSourceId));
            contexts.add(new Context("source", modId + ":any"));
        } else {
            this.eventTargetId = id.toLowerCase();
            contexts.add(new Context("target", this.eventTargetId));
            contexts.add(new Context("target", modId + ":any"));
        }
        return contexts;
    }

    public String populateEventSourceTarget(String id, boolean isSource) {
        if (this.blacklistCheck) {
            return id;
        }

        if (!id.contains(":")) {
            id = "minecraft:" + id;
        }
        String[] parts = id.split(":");
        if (parts != null && parts.length == 3) {
            if (parts[0].equals(parts[1])) {
                id = parts[1] + ":" + parts[2];
            }
        }
        if (isSource) {
            this.eventSourceId = id.toLowerCase();
        } else {
            this.eventTargetId = id.toLowerCase();
        }

        return id;
    }

    @Override
    public CompletableFuture<PermissionResult> clearAllFlagPermissions(Subject subject) {
        CompletableFuture<PermissionResult> result = new CompletableFuture<>();
        if (subject == null) {
            result.complete(new GDPermissionResult(ResultTypes.SUBJECT_DOES_NOT_EXIST));
            return result;
        }

        GDFlagPermissionEvent.ClearAll event = new GDFlagPermissionEvent.ClearAll(subject);
        GriefDefender.getEventManager().post(event);
        if (event.cancelled()) {
            result.complete(new GDPermissionResult(ResultTypes.EVENT_CANCELLED, event.getMessage().orElse(null)));
            return result;
        }

        for (Map.Entry<Set<Context>, Map<String, Boolean>> mapEntry : PermissionUtil.getInstance().getPermanentPermissions((GDPermissionHolder) subject).entrySet()) {
            final Set<Context> contextSet = mapEntry.getKey();
            for (Context context : contextSet) {
                if (context.getValue().equals(subject.getIdentifier())) {
                    PermissionUtil.getInstance().clearPermissions((GDPermissionHolder) subject, context);
                }
            }
        }

        result.complete(new GDPermissionResult(ResultTypes.SUCCESS));
        return result;
    }

    @Override
    public CompletableFuture<PermissionResult> clearFlagPermissions(Set<Context> contexts) {
        return clearFlagPermissions(GriefDefenderPlugin.DEFAULT_HOLDER, contexts);
    }

    @Override
    public CompletableFuture<PermissionResult> clearFlagPermissions(Subject subject, Set<Context> contexts) {
        CompletableFuture<PermissionResult> result = new CompletableFuture<>();
        if (subject == null) {
            result.complete(new GDPermissionResult(ResultTypes.SUBJECT_DOES_NOT_EXIST));
        }

        GDFlagPermissionEvent.Clear event = new GDFlagPermissionEvent.Clear(subject, contexts);
        GriefDefender.getEventManager().post(event);
        if (event.cancelled()) {
            result.complete(new GDPermissionResult(ResultTypes.EVENT_CANCELLED, event.getMessage().orElse(null)));
            return result;
        }

        //contexts.add(((GDClaim) claim).getWorld().getContext());
        PermissionUtil.getInstance().clearPermissions((GDPermissionHolder) subject, contexts);
        result.complete(new GDPermissionResult(ResultTypes.SUCCESS));
        return result;
    }

    @Override
    public CompletableFuture<PermissionResult> setFlagPermission(Flag flag, Tristate value, Set<Context> contexts) {
        return setPermission(GriefDefenderPlugin.DEFAULT_HOLDER, flag, value, contexts);
    }

    public CompletableFuture<PermissionResult> setPermission(Subject subject, Flag flag, Tristate value, Set<Context> contexts) {
        CompletableFuture<PermissionResult> result = new CompletableFuture<>();

        GDFlagPermissionEvent.Set event = new GDFlagPermissionEvent.Set(subject, flag, value, contexts);
        GriefDefender.getEventManager().post(event);
        if (event.cancelled()) {
            result.complete(new GDPermissionResult(ResultTypes.EVENT_CANCELLED, event.getMessage().orElse(null)));
            return result;
        }

        result.complete(PermissionUtil.getInstance().setPermissionValue((GDPermissionHolder) subject, flag, value, contexts));
        return result;
    }

    // internal
    public CompletableFuture<PermissionResult> setPermission(Claim claim, GDPermissionHolder subject, Flag flag, String target, Tristate value, Set<Context> contexts) {
        if (target.equalsIgnoreCase("any:any")) {
            target = "any";
        }

        CompletableFuture<PermissionResult> result = new CompletableFuture<>();
        if (flag != Flags.COMMAND_EXECUTE && flag != Flags.COMMAND_EXECUTE_PVP && !target.contains("pixelmon")) {
            String[] parts = target.split(":");
            if (!target.startsWith("#") && parts.length > 1 && parts[0].equalsIgnoreCase("minecraft")) {
                target = parts[1];
            }

            if (target != null && !GriefDefenderPlugin.ID_MAP.contains(target)) {
                result.complete(new GDPermissionResult(ResultTypes.TARGET_NOT_VALID));
                return result;
            }
        }

        contexts.add(new Context(ContextKeys.TARGET, target));
        GDFlagPermissionEvent.Set event = new GDFlagPermissionEvent.Set(subject, flag, value, contexts);
        GriefDefender.getEventManager().post(event);
        if (event.cancelled()) {
            result.complete(new GDPermissionResult(ResultTypes.EVENT_CANCELLED, event.getMessage().orElse(null)));
            return result;
        }

        CommandSource commandSource = Sponge.getServer().getConsole();
        final Object root = GDCauseStackManager.getInstance().getCurrentCause().root();
        if (root instanceof CommandSource) {
            commandSource = (CommandSource) root;
        }
        result.complete(CommandHelper.addFlagPermission(commandSource, subject, claim, flag, target, value, contexts));
        return result;
    }

    @Override
    public Tristate getFlagPermissionValue(Flag flag, Set<Context> contexts) {
        return getPermissionValue(GriefDefenderPlugin.DEFAULT_HOLDER, flag, contexts);
    }

    public Tristate getPermissionValue(GDPermissionHolder subject, Flag flag, Set<Context> contexts) {
        return PermissionUtil.getInstance().getPermissionValue(subject, flag.getPermission(), contexts);
    }

    @Override
    public Map<String, Boolean> getFlagPermissions(Set<Context> contexts) {
        return getFlagPermissions(GriefDefenderPlugin.DEFAULT_HOLDER, contexts);
    }

    @Override
    public Map<String, Boolean> getFlagPermissions(Subject subject, Set<Context> contexts) {
        if (subject == null) {
            return new HashMap<>();
        }
        return PermissionUtil.getInstance().getPermissions((GDPermissionHolder) subject, contexts);
    }

    public static GDPermissionManager getInstance() {
        return instance;
    }

    static {
        instance = new GDPermissionManager();
    }

    @Override
    public Optional<String> getOptionValue(Option option, Set<Context> contexts) {
        return Optional.empty();
    }

    @Override
    public Optional<String> getOptionValue(Subject subject, Option option, Set<Context> contexts) {
        return Optional.empty();
    }

    public <T> T getInternalOptionValue(TypeToken<T> type, User player, Option<T> option) {
        return getInternalOptionValue(type, player, option, null);
    }

    public <T> T getInternalOptionValue(TypeToken<T> type, User player, Option<T> option, Claim claim) {
        final GDPermissionHolder holder = PermissionHolderCache.getInstance().getOrCreateHolder(player.getUniqueId().toString());
        if (claim != null) {
            return this.getInternalOptionValue(type, holder, option, claim, claim.getType(), new HashSet<>());
        }
        return this.getInternalOptionValue(type, holder, option, (ClaimType) null);
    }

    public <T> T getInternalOptionValue(TypeToken<T> type, GDPermissionHolder holder, Option<T> option) {
        return this.getInternalOptionValue(type, holder, option, (ClaimType) null);
    }

    public <T> T getInternalOptionValue(TypeToken<T> type, GDPermissionHolder holder, Option<T> option, Claim claim) {
        if (claim != null) {
            return this.getInternalOptionValue(type, holder, option, claim, claim.getType(), new HashSet<>());
        }
        return this.getInternalOptionValue(type, holder, option, (ClaimType) null);
    }

    public <T> T getInternalOptionValue(TypeToken<T> type, GDPermissionHolder holder, Option<T> option, Set<Context> contexts) {
        return getInternalOptionValue(type, holder, option, null, null, contexts);
    }

    public <T> T getInternalOptionValue(TypeToken<T> type, GDPermissionHolder holder, Option<T> option, Claim claim, Set<Context> contexts) {
        return getInternalOptionValue(type, holder, option, claim, null, contexts);
    }

    public <T> T getInternalOptionValue(TypeToken<T> type, GDPermissionHolder holder, Option<T> option, ClaimType claimType) {
        return this.getInternalOptionValue(type, holder, option, null, claimType, new HashSet<>());
    }

    public <T> T getInternalOptionValue(TypeToken<T> type, GDPermissionHolder holder, Option<T> option, Claim claim, ClaimType claimType, Set<Context> contexts) {
        if (holder != GriefDefenderPlugin.DEFAULT_HOLDER && holder instanceof GDPermissionUser) {
            final GDPermissionUser user = (GDPermissionUser) holder;
            final GDPlayerData playerData = (GDPlayerData) user.getPlayerData();
            if (playerData != null) {
                playerData.ignoreActiveContexts = true;
            }
            //contexts.addAll(PermissionUtil.getInstance().getActiveContexts(holder));
            PermissionUtil.getInstance().addActiveContexts(contexts, holder, playerData, claim);
        }

        if (!option.isGlobal() && (claim != null || claimType != null)) {
            // check claim
            if (claim != null) {
                contexts.add(claim.getContext());
                final T value = this.getOptionActualValue(type, holder, option, contexts);
                if (value != null) {
                    return value;
                }
                contexts.remove(claim.getContext());
            }

            // check claim type
            if (claimType != null) {
                contexts.add(claimType.getContext());
                final T value = this.getOptionActualValue(type, holder, option, contexts);
                if (value != null) {
                    return value;
                }
                contexts.remove(claimType.getContext());
            }
        }

        // Check only active contexts
        T value = this.getOptionActualValue(type, holder, option, contexts);
        if (value != null) {
            return value;
        }

        // Check type/global default context
        if (claimType != null) {
            contexts.add(claimType.getDefaultContext());
        }
        contexts.add(ClaimContexts.GLOBAL_DEFAULT_CONTEXT);
        value = this.getOptionActualValue(type, holder, option, contexts);
        if (value != null) {
            return value;
        }
        contexts.remove(ClaimContexts.GLOBAL_DEFAULT_CONTEXT);
        if (claimType != null) {
            contexts.remove(claimType.getDefaultContext());
        }

        // Check global
        if (holder != GriefDefenderPlugin.DEFAULT_HOLDER) {
            return getInternalOptionValue(type, GriefDefenderPlugin.DEFAULT_HOLDER, option, claim, claimType, contexts);
        }

        return option.getDefaultValue();
    }

    private <T> T getOptionActualValue(TypeToken<T> type, GDPermissionHolder holder, Option option, Set<Context> contexts) {
        if (option.multiValued()) {
            List<String> values = PermissionUtil.getInstance().getOptionValueList(holder, option, contexts);
            if (values != null && !values.isEmpty()) {
                return (T) values;
            }
        }
        String value = PermissionUtil.getInstance().getOptionValue(holder, option, contexts);
        if (value != null) {
            return this.getOptionTypeValue(type, value);
        }

        return null;
    }

    private <T> T getOptionTypeValue(TypeToken<T> type, String value) {
        if (type.getRawType().isAssignableFrom(Double.class)) {
            return (T) Double.valueOf(value);
        }
        if (type.getRawType().isAssignableFrom(Integer.class)) {
            if (value.equalsIgnoreCase("undefined")) {
                return (T) Integer.valueOf(-1);
            }
            Integer val  = null;
            try {
                val = Integer.valueOf(value);
            } catch (NumberFormatException e) {
                return (T) Integer.valueOf(-1);
            }
            return (T) Integer.valueOf(value);
        }
        if (type.getRawType().isAssignableFrom(String.class)) {
            return (T) value;
        }
        if (type.getRawType().isAssignableFrom(Tristate.class)) {
            if (value.equalsIgnoreCase("true")) {
                return (T) Tristate.TRUE;
            }
            if (value.equalsIgnoreCase("false")) {
                return (T) Tristate.FALSE;
            }
            int permValue = 0;
            try {
                permValue = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                
            }
            if (permValue == 0) {
                return (T) Tristate.UNDEFINED;
            }
            return (T) (permValue == 1 ? Tristate.TRUE : Tristate.FALSE);
        }
        if (type.getRawType().isAssignableFrom(CreateModeType.class)) {
            if (value.equalsIgnoreCase("undefined")) {
                return (T) CreateModeTypes.AREA;
            }
            if (value.equalsIgnoreCase("volume")) {
                return (T) CreateModeTypes.VOLUME;
            }
            if (value.equalsIgnoreCase("area")) {
                return (T) CreateModeTypes.AREA;
            }

            int permValue = 0;
            try {
                permValue = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                
            }
            if (permValue == 0) {
                return (T) CreateModeTypes.AREA;
            }
            return (T) (permValue == 1 ? CreateModeTypes.VOLUME : CreateModeTypes.AREA);
        }
        if (type.getRawType().isAssignableFrom(WeatherType.class)) {
            if (value.equalsIgnoreCase("downfall")) {
                return (T) WeatherTypes.DOWNFALL;
            }
            if (value.equalsIgnoreCase("clear")) {
                return (T) WeatherTypes.CLEAR;
            }

            return (T) WeatherTypes.UNDEFINED;
        }
        if (type.getRawType().isAssignableFrom(GameModeType.class)) {
            if (value.equalsIgnoreCase("adventure")) {
                return (T) GameModeTypes.ADVENTURE;
            }
            if (value.equalsIgnoreCase("creative")) {
                return (T) GameModeTypes.CREATIVE;
            }
            if (value.equalsIgnoreCase("spectator")) {
                return (T) GameModeTypes.SPECTATOR;
            }
            if (value.equalsIgnoreCase("survival")) {
                return (T) GameModeTypes.SURVIVAL;
            }

            return (T) GameModeTypes.UNDEFINED;
        }
        if (type.getRawType().isAssignableFrom(Boolean.class)) {
            return (T) Boolean.valueOf(Boolean.parseBoolean(value));
        }
        return (T) value;
    }

    // Uses passed contexts and only adds active contexts
    public Double getActualOptionValue(GDPermissionHolder holder, Option option, Claim claim, GDPlayerData playerData, Set<Context> contexts) {
        if (holder != GriefDefenderPlugin.DEFAULT_HOLDER) {
            if (playerData != null) {
                playerData.ignoreActiveContexts = true;
            }
            PermissionUtil.getInstance().addActiveContexts(contexts, holder, playerData, claim);
        }

        final String value = PermissionUtil.getInstance().getOptionValue(holder, option, contexts);
        if (value != null) {
            return this.getDoubleValue(value);
        }

        return Double.valueOf(option.getDefaultValue().toString());
    }

    private Double getDoubleValue(String option) {
        if (option == null) {
            return null;
        }

        double optionValue = 0.0;
        try {
            optionValue = Double.parseDouble(option);
        } catch (NumberFormatException e) {

        }
        return optionValue;
    }

    public Optional<Flag> getFlag(String value) {
        if (value == null) {
            return Optional.empty();
        }

        value = value.replace("griefdefender.flag.", "");
        String[] parts = value.split("\\.");
        if (parts.length > 0) {
            value = parts[0];
        }

        return FlagRegistryModule.getInstance().getById(value);
    }

    public Optional<Option> getOption(String value) {
        if (value == null) {
            return Optional.empty();
        }

        value = value.replace("griefdefender.", "");
        String[] parts = value.split("\\.");
        if (parts.length > 0) {
            value = parts[0];
        }

        return GriefDefender.getRegistry().getType(Option.class, value);
    }

    public Component getEventMessage() {
        return this.eventMessage;
    }

    @Override
    public CompletableFuture<PermissionResult> clearOptions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CompletableFuture<PermissionResult> clearOptions(Set<Context> contexts) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Tristate getFlagPermissionValue(Flag flag, Subject subject, Set<Context> contexts) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CompletableFuture<PermissionResult> setFlagPermission(Flag flag, Subject subject, Tristate value,
            Set<Context> contexts) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CompletableFuture<PermissionResult> setOption(Option option, String value, Set<Context> contexts) {
        final PermissionResult result = PermissionUtil.getInstance().setOptionValue(GriefDefenderPlugin.DEFAULT_HOLDER, option.getPermission(), value, contexts);
        return CompletableFuture.completedFuture(result);
    }

    @Override
    public CompletableFuture<PermissionResult> setOption(Option option, Subject subject, String value, Set<Context> contexts) {
        final PermissionResult result = PermissionUtil.getInstance().setOptionValue((GDPermissionHolder) subject, option.getPermission(), value, contexts);
        return CompletableFuture.completedFuture(result);
    }

    @Override
    public <T> Optional<T> getOptionValue(TypeToken<T> type, Option<T> option, Set<Context> contexts) {
        String value = PermissionUtil.getInstance().getOptionValue(GriefDefenderPlugin.DEFAULT_HOLDER, option, contexts);
        if (value != null) {
            return Optional.of(this.getOptionTypeValue(type, value));
        }

        return Optional.empty();
    }

    @Override
    public <T> Optional<T> getOptionValue(TypeToken<T> type, Subject subject, Option<T> option, Set<Context> contexts) {
        String value = PermissionUtil.getInstance().getOptionValue((GDPermissionHolder) subject, option, contexts);
        if (value != null) {
            return Optional.of(this.getOptionTypeValue(type, value));
        }

        return Optional.empty();
    }

    @Override
    public <T> T getActiveOptionValue(TypeToken<T> type, Option<T> option, Subject subject, Claim claim,
            Set<Context> contexts) {
        return this.getInternalOptionValue(type, (GDPermissionHolder) subject, option, claim, claim.getType(), contexts);
    }

    @Override
    public CompletableFuture<PermissionResult> setFlagDefinition(Subject subject, FlagDefinition flagDefinition, Tristate value) {
        final Set<Context> contexts = new HashSet<>();
        contexts.addAll(flagDefinition.getContexts());
        PermissionResult result = null;
        CompletableFuture<PermissionResult> future = new CompletableFuture<>();
        for (FlagData flagData : flagDefinition.getFlagData()) {
            final Set<Context> flagContexts = new HashSet<>(contexts);
            flagContexts.addAll(flagData.getContexts());
            result = PermissionUtil.getInstance().setPermissionValue((GDPermissionHolder) subject, flagData.getFlag(), value, flagContexts);
            if (!result.successful()) {
                future.complete(result);
                return future;
            }
        }

        future.complete(result);
        return future;
    }
}
