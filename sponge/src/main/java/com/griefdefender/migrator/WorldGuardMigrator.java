/*
 * This file is part of GriefPrevention, licensed under the MIT License (MIT).
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
package com.griefdefender.migrator;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.reflect.TypeToken;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimResult;
import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.api.claim.ClaimTypes;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.ContextKeys;
import com.griefdefender.api.permission.flag.Flags;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.ClaimDataConfig;
import com.griefdefender.internal.util.BlockUtil;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissionUser;

import net.kyori.text.TextComponent;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import org.spongepowered.api.world.DimensionType;
import org.spongepowered.api.world.World;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

public class WorldGuardMigrator {

    static final Context SOURCE_PLAYER = new Context("source", "player");
    static final Context SOURCE_TNT = new Context("source", "tnt");
    static final Context SOURCE_CREEPER = new Context("source", "creeper");
    static final Context SOURCE_ENDERDRAGON = new Context("source", "enderdragon");
    static final Context SOURCE_GHAST = new Context("source", "ghast");
    static final Context SOURCE_ENDERMAN = new Context("source", "enderman");
    static final Context SOURCE_SNOWMAN = new Context("source", "snowman");
    static final Context SOURCE_MONSTER = new Context("source", "monster");
    static final Context SOURCE_WITHER = new Context("source", "wither");
    static final Context SOURCE_LAVA = new Context("source", "flowing_lava");
    static final Context SOURCE_WATER = new Context("source", "flowing_water");
    static final Context SOURCE_LIGHTNING_BOLT = new Context("source", "lightning_bolt");
    static final Context SOURCE_FALL = new Context("source", "fall");
    static final Context SOURCE_FIREWORKS = new Context("source", "fireworks");

    // Block States
    static final Context STATE_FARMLAND_DRY = new Context("state", "moisture:0");

    // Targets
    static final Context TARGET_BED = new Context("target", "bed");
    static final Context TARGET_BOAT = new Context("target", "boat");
    static final Context TARGET_CHEST = new Context("target", "chest");
    static final Context TARGET_CHORUS_FRUIT = new Context("target", "chorus_fruit");
    static final Context TARGET_ENDERPEARL = new Context("target", "enderpearl");
    static final Context TARGET_FARMLAND = new Context("target", "farmland");
    static final Context TARGET_FLINTANDSTEEL = new Context("target", "flint_and_steel");
    static final Context TARGET_GRASS= new Context("target", "grass");
    static final Context TARGET_ITEM_FRAME = new Context("target", "item_frame");
    static final Context TARGET_MINECART = new Context("target", "minecart");
    static final Context TARGET_PAINTING = new Context("target", "painting");
    static final Context TARGET_PLAYER = new Context("target", "player");
    static final Context TARGET_ICE_FORM = new Context("target", "ice");
    static final Context TARGET_ICE_MELT = new Context("target", "water");
    static final Context TARGET_SNOW = new Context("target", "snow");
    static final Context TARGET_TURTLE_EGG = new Context("target", "turtle_egg");
    static final Context TARGET_VINE = new Context("target", "vine");
    static final Context TARGET_XP_ORB = new Context("target", "xp_orb");
    static final Context TARGET_TYPE_ANIMAL = new Context("target", "#animal");
    static final Context TARGET_TYPE_CROP = new Context("target", "#crop");
    static final Context TARGET_TYPE_MONSTER = new Context("target", "#monster");
    static final Context TARGET_TYPE_MUSHROOM = new Context("target", "#mushroom");
    static final Context TARGET_TYPE_VEHICLE = new Context("target", "#vehicle");
    static final GDPermissionManager PERMISSION_MANAGER = GDPermissionManager.getInstance();

    public static void migrate(World world) throws FileNotFoundException, ClassNotFoundException {
        if (!GriefDefenderPlugin.getGlobalConfig().getConfig().migrator.worldGuardMigrator) {
            return;
        }

        final DimensionType dimType = world.getDimension().getType();
        final String worldName = world.getName().toLowerCase();
        final String dimName = dimType.getName().toLowerCase();
        final Path path = Paths.get("plugins", "WorldGuard", "worlds", worldName, "regions.yml");
        List<GDClaim> tempClaimList = new ArrayList<>();
        int count = 0;
        try {
            GriefDefenderPlugin.getInstance().getLogger().info("Starting WorldGuard region data migration for world " + worldName + "...");
            ConfigurationLoader<ConfigurationNode> regionManager = YAMLConfigurationLoader.builder().setPath(path).build();
            ConfigurationNode region = regionManager.load().getNode("regions");
            GriefDefenderPlugin.getInstance().getLogger().info("Scanning WorldGuard regions in world data file '" + path + "'...");
            for (Object key:region.getChildrenMap().keySet()){
                String rname = key.toString();

                boolean isWildernessClaim = false;
                if (rname.equalsIgnoreCase("__global__")) {
                    isWildernessClaim = true;
                }
                if (!region.getNode(rname).hasMapChildren()){
                    continue;
                }
                if (!isWildernessClaim && !region.getNode(rname, "type").getString().equals("cuboid")) {
                    GriefDefenderPlugin.getInstance().getLogger().info("Unable to migrate region '" + rname + "' as it is not a cuboid. Skipping...");
                    continue;
                }

                final Map<Object, ? extends ConfigurationNode> minMap = region.getNode(rname, "min").getChildrenMap();
                final Map<Object, ? extends ConfigurationNode> maxMap = region.getNode(rname, "max").getChildrenMap();
                final Map<Object, ? extends ConfigurationNode> flagsMap = region.getNode(rname, "flags").getChildrenMap();
                final List<UUID> membersList = region.getNode(rname, "members").getNode("unique-ids").getList(TypeToken.of(UUID.class));
                final List<UUID> ownersList = region.getNode(rname, "owners").getNode("unique-ids").getList(TypeToken.of(UUID.class));

                List<UUID> managers = new ArrayList<UUID>();
                UUID creator = null;
                for (UUID uuid : ownersList) {
                    if (managers.isEmpty()) {
                        creator = uuid;
                    } else {
                        managers.add(uuid);
                    }
                }

                // create GP claim data file
                GriefDefenderPlugin.getInstance().getLogger().info("Migrating WorldGuard region data '" + rname + "'...");
                GDPermissionUser owner = null;
                if (!isWildernessClaim) {
                    try {
                        // check cache first
                        owner = PermissionHolderCache.getInstance().getOrCreateUser(creator);
                    } catch (Throwable e) {
                        // assume admin claim
                    }
                }

                UUID claimUniqueId = isWildernessClaim ? world.getUniqueId() : UUID.randomUUID();
                ClaimType type = isWildernessClaim ? ClaimTypes.WILDERNESS : owner == null ? ClaimTypes.ADMIN : ClaimTypes.BASIC;

                ClaimDataConfig claimDataConfig = null;
                GDClaim newClaim = null;
                if (isWildernessClaim) {
                    newClaim = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(world.getUniqueId()).getWildernessClaim();
                } else {
                    final Vector3i min = new Vector3i(minMap.get("x").getInt(), minMap.get("y").getInt(), minMap.get("z").getInt());
                    final Vector3i max = new Vector3i(maxMap.get("x").getInt(), maxMap.get("y").getInt(), maxMap.get("z").getInt());
                    final boolean cuboid = min.getY() == 0 && max.getY() == 256 ? true : false;
                    newClaim = new GDClaim(world, min, max, claimUniqueId, type, owner == null ? null : owner.getUniqueId(), cuboid);
                    final ClaimResult claimResult = newClaim.checkArea(false);
                    if (!claimResult.successful()) {
                        GriefDefenderPlugin.getInstance().getLogger().info("Could not migrate region '" + rname + "' due to reason: " + claimResult.getResultType());
                        continue;
                    }

                    boolean validClaim = true;
                    Claim parentClaim = null;
                    Claim childClaim = null;
                    if (type != ClaimTypes.WILDERNESS) {
                        for (GDClaim claim : tempClaimList) {
                            // Search for parent
                            final ClaimResult result = newClaim.findParent(claim);
                            if (result.successful()) {
                                final Claim parent = result.getClaim().get();
                                if (parent.isSubdivision()) {
                                    // avoid creating child claim under subdivision
                                    // instead create admin claim
                                    break;
                                }
                                if (parent.isBasicClaim() || parent.isAdminClaim()) {
                                    parentClaim = parent;
                                    if (type == ClaimTypes.BASIC) {
                                        if (parent.isBasicClaim()) {
                                            type = ClaimTypes.SUBDIVISION;
                                            newClaim.setType(type);
                                        } else {
                                            type = ClaimTypes.BASIC;
                                            newClaim.setType(type);
                                        }
                                    }
                                    ((GDClaim) parent).children.add(newClaim);
                                    newClaim.parent = (GDClaim) parent;
                                    GriefDefenderPlugin.getInstance().getLogger().info("Found parent region '" + parent.getName().orElse(TextComponent.of("unknown")) + "'. Set current region '" + rname + "' as it's child.");
                                } else {
                                    GriefDefenderPlugin.getInstance().getLogger().warn("Could not migrate region '" + rname + "' as it exceeds the maximum level supported by migrator. Skipping...");
                                    validClaim = false;
                                }
                                break;
                            }
                            // Search for child
                            if (claim.isInside(newClaim)) {
                                if (!claim.getParent().isPresent()) {
                                    childClaim = claim;
                                    claim.getClaimStorage().getConfig().setType(ClaimTypes.SUBDIVISION);
                                    claim.getClaimStorage().getConfig().setParent(newClaim.getUniqueId());
                                    GriefDefenderPlugin.getInstance().getLogger().info("Found child region '" + claim.getName().orElse(TextComponent.of("unknown")) + "'. Set current region '" + rname + "' as it's parent.");
                                }
                            }
                        }
                        if (!validClaim) {
                            continue;
                        }
                        tempClaimList.add(newClaim);
                    }

                    newClaim.initializeClaimData((GDClaim) parentClaim);
                    if (childClaim != null) {
                        // Migrate child to parent
                        ((GDClaim) newClaim).moveChildToParent(newClaim, (GDClaim) childClaim);
                    }
                    claimDataConfig = newClaim.getClaimStorage().getConfig();
                    claimDataConfig.setName(TextComponent.of(rname));
                    claimDataConfig.setWorldUniqueId(world.getUniqueId());
                    if (owner != null) {
                        claimDataConfig.setOwnerUniqueId(owner.getUniqueId());
                    }
                    claimDataConfig.setLesserBoundaryCorner(BlockUtil.getInstance().posToString(min));
                    claimDataConfig.setGreaterBoundaryCorner(BlockUtil.getInstance().posToString(max));
                    claimDataConfig.setCuboid(cuboid);
                    claimDataConfig.setDateLastActive(Instant.now());
                    claimDataConfig.setType(type);
                }
                claimDataConfig = newClaim.getClaimStorage().getConfig();

                for (UUID builder : membersList) {
                    GDPermissionUser builderUser = null;
                    try {
                        builderUser = PermissionHolderCache.getInstance().getOrCreateUser(builder);
                    } catch (Throwable e) {
                        GriefDefenderPlugin.getInstance().getLogger().warn("Could not locate a valid UUID for user '" + builder + "' in region '" + rname + 
                                "'. Skipping...");
                        continue;
                    }
                    if (!claimDataConfig.getBuilders().contains(builderUser.getUniqueId()) && owner != null && !builderUser.getUniqueId().equals(owner.getUniqueId())) {
                        claimDataConfig.getBuilders().add(builderUser.getUniqueId());
                    }
                }

                for (UUID manager : managers) {
                    GDPermissionUser managerUser = null;
                    try {
                        managerUser = PermissionHolderCache.getInstance().getOrCreateUser(manager);
                    } catch (Throwable e) {
                        GriefDefenderPlugin.getInstance().getLogger().warn("Could not locate a valid UUID for user '" + manager + "' in region '" + rname + 
                                "'. Skipping...");
                        continue;
                    }
                    if (!claimDataConfig.getManagers().contains(managerUser.getUniqueId()) && owner != null && !managerUser.getUniqueId().equals(owner.getUniqueId())) {
                        claimDataConfig.getManagers().add(managerUser.getUniqueId());
                    }
                }
                final Set<Context> claimContextSet = new HashSet<>();
                claimContextSet.add(newClaim.getContext());

                // migrate flags
                for (Entry<Object, ? extends ConfigurationNode> mapEntry : flagsMap.entrySet()) {
                    if (!(mapEntry.getKey() instanceof String)) {
                        continue;
                    }
                    final String flag = (String) mapEntry.getKey();
                    final Set<Context> contexts = new HashSet<>(claimContextSet);
                    ConfigurationNode valueNode = mapEntry.getValue();
                    switch (flag) {
                        case "build":
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_BREAK, Tristate.FALSE, contexts);
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_PLACE, Tristate.FALSE, contexts);
                                PERMISSION_MANAGER.setFlagPermission(Flags.INTERACT_BLOCK_PRIMARY, Tristate.FALSE, contexts);
                                PERMISSION_MANAGER.setFlagPermission(Flags.INTERACT_BLOCK_SECONDARY, Tristate.FALSE, contexts);
                                PERMISSION_MANAGER.setFlagPermission(Flags.INTERACT_ENTITY_PRIMARY, Tristate.FALSE, contexts);
                                PERMISSION_MANAGER.setFlagPermission(Flags.INTERACT_ENTITY_SECONDARY, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_PLACE, Tristate.TRUE, contexts);
                            }
                            break;
                        case "interact":
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.INTERACT_BLOCK_PRIMARY, Tristate.FALSE, contexts);
                                PERMISSION_MANAGER.setFlagPermission(Flags.INTERACT_BLOCK_SECONDARY, Tristate.FALSE, contexts);
                                PERMISSION_MANAGER.setFlagPermission(Flags.INTERACT_ENTITY_PRIMARY, Tristate.FALSE, contexts);
                                PERMISSION_MANAGER.setFlagPermission(Flags.INTERACT_ENTITY_SECONDARY, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_PLACE, Tristate.TRUE, contexts);
                            }
                            break;
                        case "block-break":
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_BREAK, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_BREAK, Tristate.TRUE, contexts);
                            }
                            break;
                        case "block-place":
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_PLACE, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_PLACE, Tristate.TRUE, contexts);
                            }
                            break;
                        case "use":
                            if (valueNode.getString().equals("allow")) {
                                newClaim.addUserTrust(GriefDefenderPlugin.PUBLIC_UUID, TrustTypes.ACCESSOR);
                            }
                            break;
                        case "damage-animals":
                            contexts.add(TARGET_TYPE_ANIMAL);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.ENTITY_DAMAGE, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.ENTITY_DAMAGE, Tristate.TRUE, contexts);
                            }
                            break;
                        case "chest-access":
                            contexts.add(TARGET_CHEST);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.INTERACT_BLOCK_SECONDARY, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.INTERACT_BLOCK_SECONDARY, Tristate.TRUE, contexts);
                            }
                            break;
                        case "ride":
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.ENTITY_RIDING, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.ENTITY_RIDING, Tristate.TRUE, contexts);
                            }
                            break;
                        case "pvp":
                            contexts.add(SOURCE_PLAYER);
                            contexts.add(TARGET_PLAYER);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.ENTITY_DAMAGE, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.ENTITY_DAMAGE, Tristate.TRUE, contexts);
                            }
                            break;
                        case "sleep":
                            contexts.add(TARGET_BED);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.INTERACT_BLOCK_SECONDARY, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.INTERACT_BLOCK_SECONDARY, Tristate.TRUE, contexts);
                            }
                            break;
                        case "tnt":
                            contexts.add(SOURCE_TNT);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.EXPLOSION_BLOCK, Tristate.FALSE, contexts);
                                PERMISSION_MANAGER.setFlagPermission(Flags.EXPLOSION_ENTITY, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.EXPLOSION_BLOCK, Tristate.TRUE, contexts);
                                PERMISSION_MANAGER.setFlagPermission(Flags.EXPLOSION_ENTITY, Tristate.TRUE, contexts);
                            }
                            break;
                        case "vehicle-place":
                            contexts.add(TARGET_TYPE_VEHICLE);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.INTERACT_BLOCK_SECONDARY, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.INTERACT_BLOCK_SECONDARY, Tristate.TRUE, contexts);
                            }
                            break;
                        case "lighter":
                            contexts.add(TARGET_FLINTANDSTEEL);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.INTERACT_ITEM_SECONDARY, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.INTERACT_ITEM_SECONDARY, Tristate.TRUE, contexts);
                            }
                            break;
                        case "block-trampling":
                            contexts.add(TARGET_FARMLAND);
                            contexts.add(TARGET_TURTLE_EGG);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.COLLIDE_BLOCK, Tristate.FALSE, contexts);
                                PERMISSION_MANAGER.setFlagPermission(Flags.COLLIDE_BLOCK, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.COLLIDE_BLOCK, Tristate.TRUE, contexts);
                                PERMISSION_MANAGER.setFlagPermission(Flags.COLLIDE_BLOCK, Tristate.TRUE, contexts);
                            }
                            break;
                        case "frosted-ice-form":
                            break;
                        case "creeper-explosion":
                            contexts.add(SOURCE_CREEPER);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.EXPLOSION_BLOCK, Tristate.FALSE, contexts);
                                PERMISSION_MANAGER.setFlagPermission(Flags.EXPLOSION_ENTITY, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.EXPLOSION_BLOCK, Tristate.TRUE, contexts);
                                PERMISSION_MANAGER.setFlagPermission(Flags.EXPLOSION_ENTITY, Tristate.TRUE, contexts);
                            }
                            break;
                        case "enderdragon-block-damage":
                            contexts.add(SOURCE_ENDERDRAGON);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_BREAK, Tristate.FALSE, contexts);
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_MODIFY, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_BREAK, Tristate.TRUE, contexts);
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_MODIFY, Tristate.TRUE, contexts);
                            }
                            break;
                        case "ghast-fireball":
                            contexts.add(SOURCE_GHAST);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_BREAK, Tristate.FALSE, contexts);
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_MODIFY, Tristate.FALSE, contexts);
                                PERMISSION_MANAGER.setFlagPermission(Flags.ENTITY_DAMAGE, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_BREAK, Tristate.TRUE, contexts);
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_MODIFY, Tristate.TRUE, contexts);
                                PERMISSION_MANAGER.setFlagPermission(Flags.ENTITY_DAMAGE, Tristate.TRUE, contexts);
                            }
                            break;
                        case "other-explosion":
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.EXPLOSION_BLOCK, Tristate.FALSE, contexts);
                                PERMISSION_MANAGER.setFlagPermission(Flags.EXPLOSION_ENTITY, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.EXPLOSION_BLOCK, Tristate.TRUE, contexts);
                                PERMISSION_MANAGER.setFlagPermission(Flags.EXPLOSION_ENTITY, Tristate.TRUE, contexts);
                            }
                            break;
                        case "fire-spread":
                            contexts.add(new Context(ContextKeys.SOURCE, "fire"));
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_SPREAD, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_SPREAD, Tristate.TRUE, contexts);
                            }
                            break;
                        case "enderman-grief":
                            contexts.add(SOURCE_ENDERMAN);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_BREAK, Tristate.FALSE, contexts);
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_MODIFY, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_BREAK, Tristate.TRUE, contexts);
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_MODIFY, Tristate.TRUE, contexts);
                            }
                            break;
                        case "snowman-trail":
                            contexts.add(SOURCE_SNOWMAN);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_MODIFY, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_MODIFY, Tristate.TRUE, contexts);
                            }
                            break;
                        case "mob-damage": 
                            contexts.add(TARGET_TYPE_MONSTER);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.ENTITY_DAMAGE, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.ENTITY_DAMAGE, Tristate.TRUE, contexts);
                            }
                            break;
                        case "mob-spawning": 
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.ENTITY_SPAWN, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.ENTITY_SPAWN, Tristate.TRUE, contexts);
                            }
                            break;
                        case "deny-spawn": 
                            PERMISSION_MANAGER.setFlagPermission(Flags.ENTITY_SPAWN, Tristate.FALSE, contexts);
                            break;
                        case "entity-painting-destroy": 
                            contexts.add(TARGET_PAINTING);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.ENTITY_DAMAGE, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.ENTITY_DAMAGE, Tristate.TRUE, contexts);
                            }
                            break;
                        case "entity-item-frame-destroy": 
                            contexts.add(TARGET_ITEM_FRAME);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.ENTITY_DAMAGE, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.ENTITY_DAMAGE, Tristate.TRUE, contexts);
                            }
                            break;
                        case "wither-damage":
                            contexts.add(SOURCE_WITHER);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_BREAK, Tristate.FALSE, contexts);
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_MODIFY, Tristate.FALSE, contexts);
                                PERMISSION_MANAGER.setFlagPermission(Flags.ENTITY_DAMAGE, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_BREAK, Tristate.TRUE, contexts);
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_MODIFY, Tristate.TRUE, contexts);
                                PERMISSION_MANAGER.setFlagPermission(Flags.ENTITY_DAMAGE, Tristate.TRUE, contexts);
                            }
                            break;
                        case "lava-fire": 
                            contexts.add(SOURCE_LAVA);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_SPREAD, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_SPREAD, Tristate.TRUE, contexts);
                            }
                            break;
                        case "lightning": 
                            contexts.add(SOURCE_LIGHTNING_BOLT);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.ENTITY_DAMAGE, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.ENTITY_DAMAGE, Tristate.TRUE, contexts);
                            }
                            break;
                        case "water-flow": 
                            contexts.add(SOURCE_WATER);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.LIQUID_FLOW, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.LIQUID_FLOW, Tristate.TRUE, contexts);
                            }
                            break;
                        case "lava-flow": 
                            contexts.add(SOURCE_LAVA);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.LIQUID_FLOW, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.LIQUID_FLOW, Tristate.TRUE, contexts);
                            }
                            break;
                        case "snow-fall": 
                            contexts.add(TARGET_SNOW);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_PLACE, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_PLACE, Tristate.TRUE, contexts);
                            }
                            break;
                        case "snow-melt": 
                            contexts.add(TARGET_SNOW);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_BREAK, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_BREAK, Tristate.TRUE, contexts);
                            }
                            break;
                        case "ice-form": 
                            contexts.add(TARGET_ICE_FORM);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_MODIFY, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_MODIFY, Tristate.TRUE, contexts);
                            }
                            break;
                        case "ice-melt": 
                            contexts.add(TARGET_ICE_MELT);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_MODIFY, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_MODIFY, Tristate.TRUE, contexts);
                            }
                            break;
                        case "frosted-ice-melt":
                            break;
                        case "mushroom-growth":
                            contexts.add(TARGET_TYPE_MUSHROOM);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_GROW, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_GROW, Tristate.TRUE, contexts);
                            }
                            break;
                        case "leaf-decay":
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.LEAF_DECAY, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.LEAF_DECAY, Tristate.TRUE, contexts);
                            }
                            break;
                        case "grass-growth":
                            contexts.add(TARGET_GRASS);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_GROW, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_GROW, Tristate.TRUE, contexts);
                            }
                            break;
                        case "mycelium-spread":
                            break;
                        case "vine-growth":
                            contexts.add(TARGET_VINE);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_GROW, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_GROW, Tristate.TRUE, contexts);
                            }
                            break;
                        case "crop-growth":
                            contexts.add(TARGET_TYPE_CROP);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_GROW, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_GROW, Tristate.TRUE, contexts);
                            }
                            break;
                        case "soil-dry":
                            contexts.add(STATE_FARMLAND_DRY);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_MODIFY, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.BLOCK_MODIFY, Tristate.TRUE, contexts);
                            }
                            break;
                        case "entry":
                            contexts.add(TARGET_PLAYER);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.ENTER_CLAIM, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.ENTER_CLAIM, Tristate.TRUE, contexts);
                            }
                            break;
                        case "exit":
                            contexts.add(TARGET_PLAYER);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.EXIT_CLAIM, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.EXIT_CLAIM, Tristate.TRUE, contexts);
                            }
                            break;
                        // These can be handled via GD API
                        case "exit-override":
                        case "entry-deny-message":
                        case "exit-deny-message":
                        case "notify-enter":
                        case "notify-exit":
                            break;
                        case "greeting":
                            final String greeting = valueNode.getString();
                            if (greeting != null && !greeting.equals("")) {
                                claimDataConfig.setGreeting(LegacyComponentSerializer.legacy().deserialize(greeting, '&'));
                            }
                            break;
                        case "farewell":
                            final String farewell = valueNode.getString();
                            if (farewell != null && !farewell.equals("")) {
                                claimDataConfig.setFarewell(LegacyComponentSerializer.legacy().deserialize(farewell, '&'));
                            }
                        case "enderpearl":
                            contexts.add(TARGET_ENDERPEARL);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.INTERACT_ITEM_SECONDARY, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.INTERACT_ITEM_SECONDARY, Tristate.TRUE, contexts);
                            }
                            break;
                        case "chorus-fruit-teleport":
                            contexts.add(TARGET_CHORUS_FRUIT);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.INTERACT_ITEM_SECONDARY, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.INTERACT_ITEM_SECONDARY, Tristate.TRUE, contexts);
                            }
                            break;
                        case "teleport":
                            final String location = valueNode.getString();
                            break;
                        case "spawn":
                            break;
                        case "item-pickup":
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.ITEM_PICKUP, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.ITEM_PICKUP, Tristate.TRUE, contexts);
                            }
                            break;
                        case "item-drop":
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.ITEM_DROP, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.ITEM_DROP, Tristate.TRUE, contexts);
                            }
                            break;
                        case "exp-drop":
                            contexts.add(TARGET_XP_ORB);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.ITEM_DROP, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.ITEM_DROP, Tristate.TRUE, contexts);
                            }
                            break;
                        case "deny-message":
                            break;
                        case "invincible":
                            contexts.add(TARGET_PLAYER);
                            if (valueNode.getString().equals("allow")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.ENTITY_DAMAGE, Tristate.FALSE, contexts);
                            }
                            break;
                        case "fall-damage":
                            contexts.add(SOURCE_FALL);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.ENTITY_DAMAGE, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.ENTITY_DAMAGE, Tristate.TRUE, contexts);
                            }
                            break;
                        case "firework-damage":
                            contexts.add(SOURCE_FIREWORKS);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setFlagPermission(Flags.ENTITY_DAMAGE, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setFlagPermission(Flags.ENTITY_DAMAGE, Tristate.TRUE, contexts);
                            }
                            break;
                        case "game-mode":
                        case "time-lock":
                        case "weather-lock":
                        case "heal-delay":
                        case "heal-amount":
                        case "heal-min-health":
                        case "heal-max-health":
                        case "feed-delay":
                        case "feed-amount":
                        case "feed-min-hunger":
                        case "feed-max-hunger":
                            break;
                        case "blocked-cmds":
                            List<String> blocked = valueNode.getList(TypeToken.of(String.class));
                            for (String cmd : blocked) {
                                final Context context = new Context(ContextKeys.TARGET, cmd);
                                contexts.add(context);
                                PERMISSION_MANAGER.setFlagPermission(Flags.COMMAND_EXECUTE, Tristate.FALSE, contexts);
                                contexts.remove(context);
                            }
                            break;
                        case "allowed-cmds":
                            List<String> allowed = valueNode.getList(TypeToken.of(String.class));
                            for (String cmd : allowed) {
                                final Context context = new Context(ContextKeys.TARGET, cmd);
                                contexts.add(context);
                                PERMISSION_MANAGER.setFlagPermission(Flags.COMMAND_EXECUTE, Tristate.TRUE, contexts);
                                contexts.remove(context);
                            }
                    }
                }

                claimDataConfig.setRequiresSave(true);
                newClaim.getClaimStorage().save();
                GriefDefenderPlugin.getInstance().getLogger().info("Successfully migrated WorldGuard region data '" + rname + "' to '" + newClaim.getClaimStorage().filePath + "'");
                count++;
            }
            GriefDefenderPlugin.getInstance().getLogger().info("Finished WorldGuard region data migration for world '" + world.getName() + "'."
                    + " Migrated a total of " + count + " regions.");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ObjectMappingException e) {
            e.printStackTrace();
        } 
    }
}
