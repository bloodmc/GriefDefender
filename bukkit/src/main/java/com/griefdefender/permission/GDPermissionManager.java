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

import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimContexts;
import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.api.claim.TrustType;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.PermissionManager;
import com.griefdefender.api.permission.PermissionResult;
import com.griefdefender.api.permission.ResultTypes;
import com.griefdefender.api.permission.flag.Flag;
import com.griefdefender.api.permission.flag.Flags;
import com.griefdefender.api.permission.option.Option;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.command.CommandHelper;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.event.GDFlagClaimEvent;
import com.griefdefender.internal.registry.BlockTypeRegistryModule;
import com.griefdefender.internal.registry.EntityTypeRegistryModule;
import com.griefdefender.internal.registry.GDEntityType;
import com.griefdefender.internal.registry.ItemTypeRegistryModule;
import com.griefdefender.internal.util.NMSUtil;
import com.griefdefender.registry.FlagRegistryModule;
import com.griefdefender.registry.OptionRegistryModule;
import com.griefdefender.util.PermissionUtil;
import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.User;
import me.lucko.luckperms.api.caching.MetaData;
import me.lucko.luckperms.api.context.MutableContextSet;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GDPermissionManager implements PermissionManager {

    private static GDPermissionManager instance;
    public boolean blacklistCheck = false;
    private Event currentEvent;
    private Location eventLocation;
    private GDPermissionHolder eventSubject;
    private String eventSourceId = "none";
    private String eventTargetId = "none";
    private Set<Context> eventContexts = new HashSet<>();
    private Component eventMessage;
    private static final Pattern PATTERN_META = Pattern.compile("\\.[\\d+]*$");
    private static final List<Context> CONTEXT_LIST = Arrays.asList(
            ClaimContexts.ADMIN_DEFAULT_CONTEXT, ClaimContexts.ADMIN_OVERRIDE_CONTEXT,
            ClaimContexts.BASIC_DEFAULT_CONTEXT, ClaimContexts.BASIC_OVERRIDE_CONTEXT,
            ClaimContexts.TOWN_DEFAULT_CONTEXT, ClaimContexts.TOWN_OVERRIDE_CONTEXT,
            ClaimContexts.WILDERNESS_OVERRIDE_CONTEXT, ClaimContexts.WILDERNESS_DEFAULT_CONTEXT);

    public GDPermissionHolder getDefaultHolder() {
        return GriefDefenderPlugin.DEFAULT_HOLDER;
    }

    @Override
    public Tristate getFinalPermission(Claim claim, String identifier, Flag flag, Object source, Object target, Set<Context> contexts, TrustType type, boolean checkOverride) {
        final GDPermissionHolder permissionHolder = PermissionHolderCache.getInstance().getOrCreateHolder(identifier);
        if (permissionHolder.getLuckPermsHolder() == null) {
            return Tristate.UNDEFINED;
        }
        return getFinalPermission(null, null, contexts, claim, flag.getPermission(), source, target, permissionHolder, null, checkOverride);
    }

    public Tristate getFinalPermission(Event event, Location location, Claim claim, String flagPermission, Object source, Object target, GDPermissionHolder permissionHolder) {
        return getFinalPermission(event, location, claim, flagPermission, source, target, permissionHolder, null, false);
    }

    public Tristate getFinalPermission(Event event, Location location, Claim claim, String flagPermission, Object source, Object target, Player player) {
        final GDPermissionHolder permissionHolder = PermissionHolderCache.getInstance().getOrCreateUser(player);
        return getFinalPermission(event, location, claim, flagPermission, source, target, permissionHolder, null, false);
    }

    public Tristate getFinalPermission(Event event, Location location, Claim claim, String flagPermission, Object source, Object target, Player player, boolean checkOverride) {
        final GDPermissionHolder permissionHolder = PermissionHolderCache.getInstance().getOrCreateUser(player);
        return getFinalPermission(event, location, claim, flagPermission, source, target, permissionHolder, null, checkOverride);
    }

    public Tristate getFinalPermission(Event event, Location location, Claim claim, String flagPermission, Object source, Object target, GDPermissionHolder permissionHolder, boolean checkOverride) {
        return getFinalPermission(event, location, claim, flagPermission, source, target, permissionHolder, null, checkOverride);
    }

    public Tristate getFinalPermission(Event event, Location location, Claim claim, String flagPermission, Object source, Object target, Player player, TrustType type, boolean checkOverride) {
        final GDPermissionHolder permissionHolder = PermissionHolderCache.getInstance().getOrCreateUser(player);
        return getFinalPermission(event, location, claim, flagPermission, source, target, permissionHolder, type, checkOverride);
    }

    public Tristate getFinalPermission(Event event, Location location, Claim claim, String flagPermission, Object source, Object target, GDPermissionHolder permissionHolder, TrustType type, boolean checkOverride) {
        final Set<Context> contexts = new HashSet<>();
        if (permissionHolder instanceof GDPermissionUser) {
            GDPermissionUser user = (GDPermissionUser) permissionHolder;
            if (user.getOnlinePlayer() != null && NMSUtil.getInstance().getActiveItem(user.getOnlinePlayer(), event) != null) {
                contexts.add(new Context("used_item", getPermissionIdentifier(NMSUtil.getInstance().getActiveItem(user.getOnlinePlayer()))));
            }
        }

        return getFinalPermission(event, location, contexts, claim, flagPermission, source, target, permissionHolder, type, checkOverride);
    }

    public Tristate getFinalPermission(Event event, Location location, Set<Context> contexts, Claim claim, String flagPermission, Object source, Object target, GDPermissionHolder permissionHolder, TrustType type, boolean checkOverride) {
        if (claim == null) {
            return Tristate.TRUE;
        }

        GDPlayerData playerData = null;
        final GDPermissionUser user = permissionHolder instanceof GDPermissionUser ? (GDPermissionUser) permissionHolder : null;
        this.eventSubject = null;
        this.eventMessage = null;
        if (permissionHolder != null) {
            this.eventSubject = permissionHolder;
            if (user != null) {
                playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(claim.getWorldUniqueId(), user.getUniqueId());
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
        this.eventContexts = contexts;

        String sourceId = getPermissionIdentifier(source, true);
        String targetPermission = flagPermission;
        String targetId = getPermissionIdentifier(target);
        if (!targetId.isEmpty()) {
            String[] parts = targetId.split(":");
            String targetMod = parts[0];
            // move target meta to end of permission
            Matcher m = PATTERN_META.matcher(targetId);
            String targetMeta = "";
            if (!flagPermission.contains("command-execute")) {
                if (m.find()) {
                    targetMeta = m.group(0);
                    targetId = StringUtils.replace(targetId, targetMeta, "");
                }
            }
            targetPermission += "." + targetId + targetMeta;
        }
        if (!sourceId.isEmpty()) {
            this.eventContexts.add(new Context("source", sourceId));
        }

        targetPermission = StringUtils.replace(targetPermission, ":", ".");
        // If player can ignore admin claims and is currently ignoring , allow
        if (playerData != null && playerData.ignoreAdminClaims && playerData.canIgnoreClaim(claim)) {
            return processResult(claim, targetPermission, "ignore", Tristate.TRUE, user);
        }

        if (checkOverride) {
            Tristate override = Tristate.UNDEFINED;
            if (user != null) {
                // check global bans in wilderness
                override = getFlagOverride(claim.getWilderness(), user, playerData, targetPermission);
                if (override != Tristate.UNDEFINED) {
                    return override;
                }
            }
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
        final MutableContextSet contexts = PermissionUtil.getInstance().getActiveContexts(holder);
        contexts.addAll(this.eventContexts);

        for (Claim parentClaim : inheritParents) {
            GDClaim parent = (GDClaim) parentClaim;
            // check parent context
            contexts.add(parent.getContext());  

            Tristate value = PermissionUtil.getInstance().getPermissionValue((GDClaim) claim, holder, permission, contexts);
            if (value != Tristate.UNDEFINED) {
                return processResult(claim, permission, value, holder);
            }

            contexts.removeAll(parent.getContext().getKey());
        }

        contexts.add(claim.getContext());
        contexts.add(claim.getType().getContext());
        Tristate value = PermissionUtil.getInstance().getPermissionValue((GDClaim) claim, holder, permission, contexts);
        if (value != Tristate.UNDEFINED) {
            return processResult(claim, permission, value, holder);
        }

        return getClaimFlagPermission(claim, permission);
    }

    private Tristate getClaimFlagPermission(Claim claim, String permission) {
        Set<Context> contexts = new HashSet<>();
        contexts.add(claim.getContext());
        contexts.add(claim.getType().getContext());
        contexts.addAll(this.eventContexts);

        Tristate value = PermissionUtil.getInstance().getPermissionValue((GDClaim) claim, GriefDefenderPlugin.DEFAULT_HOLDER, permission, contexts);
        if (value != Tristate.UNDEFINED) {
            return processResult(claim, permission, value, GriefDefenderPlugin.DEFAULT_HOLDER);
        }

        return getFlagDefaultPermission(claim, permission);
    }

    // Only uses world and claim type contexts
    private Tristate getFlagDefaultPermission(Claim claim, String permission) {
        final GDClaim gpClaim = (GDClaim) claim;
        // Fallback to defaults
        Set<Context> contexts = new HashSet<>();
        if (gpClaim.parent != null && claim.getData().doesInheritParent()) {
            if (gpClaim.parent.parent != null && gpClaim.parent.getData().doesInheritParent()) {
                claim = gpClaim.parent.parent;
            } else {
                claim = gpClaim.parent;
            }
        }

        if (claim.isAdminClaim()) {
            contexts.add(ClaimContexts.ADMIN_DEFAULT_CONTEXT);
        } else if (claim.isBasicClaim() || claim.isSubdivision()) {
            contexts.add(ClaimContexts.BASIC_DEFAULT_CONTEXT);
        } else if (claim.isTown()) {
            contexts.add(ClaimContexts.TOWN_DEFAULT_CONTEXT);
        } else { // wilderness
            contexts.add(ClaimContexts.WILDERNESS_DEFAULT_CONTEXT);
        }

        contexts.add(ClaimContexts.GLOBAL_DEFAULT_CONTEXT);
        //contexts.add(claim.getWorld().getContext());
        contexts.addAll(this.eventContexts);
        Tristate value = PermissionUtil.getInstance().getPermissionValue((GDClaim) claim, GriefDefenderPlugin.DEFAULT_HOLDER, permission, contexts);
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
        MutableContextSet contexts = PermissionUtil.getInstance().getActiveContexts(permissionHolder, playerData, claim);
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
            if (player != null && claim.isWilderness() && value == Tristate.FALSE) {
                Component reason = GriefDefenderPlugin.getGlobalConfig().getConfig().bans.getReason(flagPermission);
                if (reason != null && reason != TextComponent.empty()) {
                    TextAdapter.sendComponent(player, reason);
                }
            }
            if (value == Tristate.FALSE) {
                this.eventMessage = MessageCache.getInstance().PERMISSION_OVERRIDE_DENY;
            }
            return processResult(claim, flagPermission, value, permissionHolder);
        }

        return Tristate.UNDEFINED;
    }

    public Tristate processResult(Claim claim, String permission, Tristate permissionValue, GDPermissionHolder permissionHolder) {
        return processResult(claim, permission, null, permissionValue, permissionHolder);
    }

    public Tristate processResult(Claim claim, String permission, String trust, Tristate permissionValue, GDPermissionHolder permissionHolder) {
        if (GriefDefenderPlugin.debugActive) {
            if (this.currentEvent != null && (this.currentEvent instanceof BlockPhysicsEvent)) {
                if (((GDClaim) claim).getWorld().getTime() % 100 != 0L) {
                    return permissionValue;
                }
            }

            GriefDefenderPlugin.addEventLogEntry(this.currentEvent, this.eventLocation, this.eventSourceId, this.eventTargetId, this.eventSubject == null ? permissionHolder : this.eventSubject, permission, trust, permissionValue);
        }

        return permissionValue;
    }

    public String getPermissionIdentifier(Object obj) {
        return getPermissionIdentifier(obj, false);
    }

    @SuppressWarnings("deprecation")
    public String getPermissionIdentifier(Object obj, boolean isSource) {
        if (obj != null) {
            if (obj instanceof Entity) {
                Entity targetEntity = (Entity) obj;
                final String name = targetEntity.getType().getName() == null ? targetEntity.getType().name().toLowerCase() : targetEntity.getType().getName();
                final GDEntityType type = EntityTypeRegistryModule.getInstance().getById(name).orElse(null);
                if (type == null) {
                    // Should never happen
                    return "unknown";
                }

                String id = type.getId();
                if (type.getEnumCreatureTypeId() != null) {
                    id = type.getEnumCreatureTypeId() + "." + type.getName();
                }

                if (targetEntity instanceof Item) {
                    id = ((Item) targetEntity).getItemStack().getType().name().toLowerCase();
                }

                return populateEventSourceTarget(id, isSource);
            } else if (obj instanceof Block) {
                final String id = BlockTypeRegistryModule.getInstance().getNMSKey((Block) obj);
                return populateEventSourceTarget(id, isSource);
            } else if (obj instanceof BlockState) {
                final BlockState blockstate = (BlockState) obj;
                final String id = BlockTypeRegistryModule.getInstance().getNMSKey(blockstate);
                return populateEventSourceTarget(id, isSource);
            } /*else if (obj instanceof TileEntity) {
                TileEntity tileEntity = (TileEntity) obj;
                final String id = tileEntity.getMinecraftKeyString();
                return populateEventSourceTarget(id, isSource);
            }*/ else if (obj instanceof Inventory) {
                final String id = ((Inventory) obj).getType().name().toLowerCase();
                return populateEventSourceTarget(id, isSource);
            } else if (obj instanceof Item) {
                
            } else if (obj instanceof ItemStack) {
                final ItemStack itemstack = (ItemStack) obj;
                String id = ItemTypeRegistryModule.getInstance().getNMSKey(itemstack);
                return populateEventSourceTarget(id, isSource);
            } else if (obj instanceof DamageCause) {
                final DamageCause damageCause = (DamageCause) obj;
                String id = damageCause.name().toLowerCase();
                return populateEventSourceTarget(id, isSource);
            } else if (obj instanceof SpawnReason) {
                return populateEventSourceTarget("spawnreason:" + ((SpawnReason) obj).name().toLowerCase(), isSource);
            } else if (obj instanceof CreatureSpawner) {
                final CreatureSpawner spawner = (CreatureSpawner) obj;
                return this.getPermissionIdentifier(spawner.getBlock());
            }  else if (obj instanceof String) {
                final String id = obj.toString().toLowerCase();
                return populateEventSourceTarget(id, isSource);
            } /* else if (obj instanceof Item) {
                final String id = ((ItemType) obj).getId().toLowerCase();
                populateEventSourceTarget(id, isSource);
                return id;
            } else if (obj instanceof EntityDamageSource) {
                final EntityDamageSource damageSource = (EntityDamageSource) obj;
                Entity sourceEntity = damageSource.getSource();

                if (this.eventSubject == null && sourceEntity instanceof User) {
                    this.eventSubject = (User) sourceEntity;
                }

                return getPermissionIdentifier(sourceEntity, isSource);
            } else if (obj instanceof DamageSource) {
                final DamageSource damageSource = (DamageSource) obj;
                String id = damageSource.getType().getId();
                if (!id.contains(":")) {
                    id = "minecraft:" + id;
                }

                populateEventSourceTarget(id, isSource);
                return id;
            }*/
        }

        populateEventSourceTarget("none", isSource);
        return "";
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

    private String populateEventSourceTarget(String id, boolean isSource) {
        if (this.blacklistCheck) {
            return id;
        }

        if (!id.contains("minecraft:")) {
            id = "minecraft:" + id;
        }
        if (isSource) {
            this.eventSourceId = id.toLowerCase();
        } else {
            this.eventTargetId = id.toLowerCase();
        }

        return id;
    }

    @Override
    public CompletableFuture<PermissionResult> clearAllPermissions(Claim claim, String identifier) {
        final GDPermissionHolder subject = PermissionHolderCache.getInstance().getOrCreateHolder(identifier);
        CompletableFuture<PermissionResult> result = new CompletableFuture<>();
        if (subject == null) {
            result.complete(new GDPermissionResult(ResultTypes.SUBJECT_DOES_NOT_EXIST));
            return result;
        }

        GDFlagClaimEvent.ClearAll event = new GDFlagClaimEvent.ClearAll(claim, subject);
        GriefDefender.getEventManager().post(event);
        if (event.cancelled()) {
            result.complete(new GDPermissionResult(ResultTypes.EVENT_CANCELLED, event.getMessage().orElse(null)));
            return result;
        }

        /*for (Map.Entry<Set<Context>, Map<String, Boolean>> mapEntry : subject.getSubjectData().getAllPermissions().entrySet()) {
            final Set<Context> contextSet = mapEntry.getKey();
            if (contextSet.contains(claim.getContext())) {
                subject.getSubjectData().clearPermissions(contextSet);
                continue;
            }
        }*/

        result.complete(new GDPermissionResult(ResultTypes.SUCCESS));
        return result;
    }

    @Override
    public CompletableFuture<PermissionResult> clearPermissions(Claim claim, Set<Context> contexts) {
        return clearPermissions(claim, GriefDefenderPlugin.DEFAULT_HOLDER, contexts);
    }

    @Override
    public CompletableFuture<PermissionResult> clearPermissions(Claim claim, String identifier, Set<com.griefdefender.api.permission.Context> contexts) {
        final GDPermissionHolder subject = PermissionHolderCache.getInstance().getOrCreateHolder(identifier);
        return this.clearPermissions(claim, subject, contexts);
    }

    public CompletableFuture<PermissionResult> clearPermissions(Claim claim, GDPermissionHolder subject, Set<Context> contexts) {
        CompletableFuture<PermissionResult> result = new CompletableFuture<>();
        if (subject == null) {
            result.complete(new GDPermissionResult(ResultTypes.SUBJECT_DOES_NOT_EXIST));
        }

        GDFlagClaimEvent.Clear event = new GDFlagClaimEvent.Clear(claim, subject, contexts);
        GriefDefender.getEventManager().post(event);
        if (event.cancelled()) {
            result.complete(new GDPermissionResult(ResultTypes.EVENT_CANCELLED, event.getMessage().orElse(null)));
            return result;
        }

        //contexts.add(claim.getWorld().getContext());
        //subject.getSubjectData().clearPermissions(contexts);
        result.complete(new GDPermissionResult(ResultTypes.SUCCESS));
        return result;
    }

    @Override
    public CompletableFuture<PermissionResult> setPermission(Claim claim, Flag flag, Tristate value, Set<Context> contexts) {
       return setPermission(claim, GriefDefenderPlugin.DEFAULT_HOLDER, flag, "any", value, contexts);
    }

    @Override
    public CompletableFuture<PermissionResult> setPermission(Claim claim, String identifier, Flag flag, Tristate value, Set<Context> contexts) {
        return setPermission(claim, identifier, flag, "any", value, contexts);
    }

    @Override
    public CompletableFuture<PermissionResult> setPermission(Claim claim, Flag flag, String target, Tristate value, Set<Context> contexts) {
        return setPermission(claim, GriefDefenderPlugin.DEFAULT_HOLDER, flag, target, value, contexts);
    }

    @Override
    public CompletableFuture<PermissionResult> setPermission(Claim claim, String identifier, Flag flag, String target, Tristate value, Set<com.griefdefender.api.permission.Context> contexts) {
        final GDPermissionHolder subject = PermissionHolderCache.getInstance().getOrCreateHolder(identifier);
        return this.setPermission(claim, subject, flag, target, value, contexts);
    }

    public CompletableFuture<PermissionResult> setPermission(Claim claim, GDPermissionHolder subject, Flag flag, String target, Tristate value, Set<Context> contexts) {
        if (target.equalsIgnoreCase("any:any")) {
            target = "any";
        }
        CompletableFuture<PermissionResult> result = new CompletableFuture<>();
        if (target != null && !GriefDefenderPlugin.ID_MAP.contains(target)) {
            result.complete(new GDPermissionResult(ResultTypes.TARGET_NOT_VALID));
            return result;
        }
        /*if (!CONTEXT_LIST.contains(context) && context != this.getContext()) {
            result.complete(new GPFlagResult(FlagResultType.CONTEXT_NOT_VALID));
            return result;
        }*/

        GDFlagClaimEvent.Set event = new GDFlagClaimEvent.Set(claim, subject, flag, target, value, contexts);
        GriefDefender.getEventManager().post(event);
        if (event.cancelled()) {
            result.complete(new GDPermissionResult(ResultTypes.EVENT_CANCELLED, event.getMessage().orElse(null)));
            return result;
        }

        CommandSender commandSource = Bukkit.getConsoleSender();
        String subjectName = subject.getFriendlyName();
        if (subject instanceof User) {
            subjectName = ((User) subject).getName();
        } else if (subject == GriefDefenderPlugin.DEFAULT_HOLDER) {
            subjectName = "ALL";
        }
        result.complete(CommandHelper.addFlagPermission(commandSource, subject, subjectName, claim, flag, target, value, contexts));
        return result;
    }

    // internal
    public CompletableFuture<PermissionResult> setPermission(Claim claim, GDPermissionHolder subject, String friendlyName, Flag flag, String target, Tristate value, Set<Context> contexts) {
        if (target.equalsIgnoreCase("any:any")) {
            target = "any";
        }

        CompletableFuture<PermissionResult> result = new CompletableFuture<>();
        if (flag != Flags.COMMAND_EXECUTE && flag != Flags.COMMAND_EXECUTE_PVP) {
            String[] parts = target.split(":");
            if (parts.length > 1 && parts[0].equalsIgnoreCase("minecraft")) {
                target = parts[1];
            }
            if (target != null && !GriefDefenderPlugin.ID_MAP.contains(target)) {
                result.complete(new GDPermissionResult(ResultTypes.TARGET_NOT_VALID));
                return result;
            }
        }

        GDFlagClaimEvent.Set event = new GDFlagClaimEvent.Set(claim, subject, flag, target, value, contexts);
        GriefDefender.getEventManager().post(event);
        if (event.cancelled()) {
            result.complete(new GDPermissionResult(ResultTypes.EVENT_CANCELLED, event.getMessage().orElse(null)));
            return result;
        }

        final Player player = GDCauseStackManager.getInstance().getCurrentCause().first(Player.class).orElse(null);
        CommandSender commandSource = player != null ? player : Bukkit.getConsoleSender();
        result.complete(CommandHelper.addFlagPermission(commandSource, subject, friendlyName, claim, flag, target, value, contexts));
        return result;
    }

    @Override
    public Tristate getPermissionValue(Claim claim, Flag flag, String target, Set<Context> contexts) {
        return getPermissionValue(claim, GriefDefenderPlugin.DEFAULT_HOLDER, flag, target, contexts);
    }

    @Override
    public Tristate getPermissionValue(Claim claim, String identifier, Flag flag, String target, Set<Context> contexts) {
        final GDPermissionHolder subject = PermissionHolderCache.getInstance().getOrCreateHolder(identifier);
        return this.getPermissionValue(claim, subject, flag, target, contexts);
    }

    public Tristate getPermissionValue(Claim claim, GDPermissionHolder subject, Flag flag, String target, Set<Context> contexts) {
        if (target.equalsIgnoreCase("any:any") || target.equalsIgnoreCase("any")) {
            target = null;
        }
        /*if (subject != GriefDefenderPlugin.DEFAULT_HOLDER && (context == this.getDefaultContext() || context == this.getOverrideContext())) {
            return Tristate.UNDEFINED;
        }*/

        final String flagBasePermission = GDPermissions.FLAG_BASE + "." + flag.toString();
        String targetPermission = flagBasePermission;
        String targetId = getPermissionIdentifier(target);
        if (!targetId.isEmpty()) {
            Matcher m = PATTERN_META.matcher(targetId);
            String targetMeta = "";
            if (!targetPermission.contains("command-execute")) {
                if (m.find()) {
                    targetMeta = m.group(0);
                    targetId = StringUtils.replace(targetId, targetMeta, "");
                }
            }
            targetPermission += "." + targetId + targetMeta;
        }
        targetPermission = StringUtils.replace(targetPermission, ":", ".");
        return PermissionUtil.getInstance().getPermissionValue((GDClaim) claim, subject, targetPermission, contexts);
    }

    @Override
    public Map<String, Boolean> getPermissions(Set<Context> contexts) {
        return getPermissions(GriefDefenderPlugin.DEFAULT_HOLDER, contexts);
    }

    @Override
    public Map<String, Boolean> getPermissions(String identifier, Set<Context> contexts) {
        final GDPermissionHolder subject = PermissionHolderCache.getInstance().getOrCreateHolder(identifier);
        return this.getPermissions(subject, contexts);
    }

    public Map<String, Boolean> getPermissions(GDPermissionHolder subject, Set<Context> contexts) {
        if (subject == null) {
            return new HashMap<>();
        }
        return PermissionUtil.getInstance().getPermissions(subject, contexts);
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
    public Optional<String> getOptionValue(String identifier, Option option, Set<Context> contexts) {
        return Optional.empty();
    }

    public Double getInternalOptionValue(OfflinePlayer player, Option option, GDPlayerData playerData) {
        return getInternalOptionValue(player, option, null, playerData);
    }

    public Double getInternalOptionValue(OfflinePlayer player, Option option, Claim claim, GDPlayerData playerData) {
        final GDPermissionHolder holder = PermissionHolderCache.getInstance().getOrCreateHolder(player.getUniqueId().toString());
        if (claim != null) {
            return this.getInternalOptionValue(holder, option, claim, claim.getType(), playerData);
        }
        return this.getInternalOptionValue(holder, option, (ClaimType) null, playerData);
    }

    public Double getInternalOptionValue(GDPermissionHolder holder, Option option, GDPlayerData playerData) {
        return this.getInternalOptionValue(holder, option, (ClaimType) null, playerData);
    }

    public Double getInternalOptionValue(GDPermissionHolder holder, Option option, Claim claim, GDPlayerData playerData) {
        if (claim != null) {
            return this.getInternalOptionValue(holder, option, claim, claim.getType(), playerData);
        }
        return this.getInternalOptionValue(holder, option, (ClaimType) null, playerData);
    }

    public Double getInternalOptionValue(GDPermissionHolder holder, Option option, ClaimType type, GDPlayerData playerData) {
        return this.getInternalOptionValue(holder, option, null, type, playerData);
    }

    public Double getInternalOptionValue(GDPermissionHolder holder, Option option, Claim claim, ClaimType type, GDPlayerData playerData) {
        final MutableContextSet contexts = MutableContextSet.create();
        if (holder != GriefDefenderPlugin.DEFAULT_HOLDER) {
            if (playerData != null) {
                playerData.ignoreActiveContexts = true;
            }
            //contexts.addAll(PermissionUtil.getInstance().getActiveContexts(holder));
            contexts.addAll(PermissionUtil.getInstance().getActiveContexts(holder, playerData, claim));
        }

        if (!option.isGlobal() && (claim != null || type != null)) {
            // check claim
            if (claim != null) {
                contexts.add(claim.getContext());
                Contexts context = Contexts.global().setContexts(contexts);
                MetaData metaData = holder.getLuckPermsHolder().getCachedData().getMetaData(context);
                String value = metaData.getMeta().get(option.getPermission());
                if (value != null) {
                    return this.getDoubleValue(value);
                }
                contexts.remove(claim.getContext().getType(), claim.getContext().getType());
            }

            // check claim type
            if (type != null) {
                contexts.add(type.getContext());
                Contexts context = Contexts.global().setContexts(contexts);
                MetaData metaData = holder.getLuckPermsHolder().getCachedData().getMetaData(context);
                String value = metaData.getMeta().get(option.getPermission());
                if (value != null) {
                    return this.getDoubleValue(value);
                }
                contexts.remove(type.getContext().getKey(), type.getContext().getValue());
            }
        }

        // Check only active contexts
        Contexts context = Contexts.global().setContexts(contexts);
        MetaData metaData = holder.getLuckPermsHolder().getCachedData().getMetaData(context);
        String value = metaData.getMeta().get(option.getPermission());
        if (value != null) {
            return this.getDoubleValue(value);
        }

        // Check type/global default context
        if (type != null) {
            contexts.add(type.getDefaultContext());
        }
        contexts.add(ClaimContexts.GLOBAL_DEFAULT_CONTEXT);
        context = Contexts.global().setContexts(contexts);
        metaData = holder.getLuckPermsHolder().getCachedData().getMetaData(context);
        value = metaData.getMeta().get(option.getPermission());
        if (value != null) {
            return this.getDoubleValue(value);
        }

        // Check global
        if (holder != GriefDefenderPlugin.DEFAULT_HOLDER) {
            return getInternalOptionValue(GriefDefenderPlugin.DEFAULT_HOLDER, option, claim, type, playerData);
        }

        // Should never happen but if it does just return 0
        return 0.0;
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

        return OptionRegistryModule.getInstance().getById(value);
    }

    public Component getEventMessage() {
        return this.eventMessage;
    }
}
