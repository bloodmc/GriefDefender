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

import org.bukkit.World;
import org.bukkit.World.Environment;

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

    static final GDPermissionManager PERMISSION_MANAGER = GDPermissionManager.getInstance();

    public static void migrate(World world) throws FileNotFoundException, ClassNotFoundException {
        if (!GriefDefenderPlugin.getGlobalConfig().getConfig().migrator.worldGuardMigrator) {
            return;
        }

        final Environment dimType = world.getEnvironment();
        final String worldName = world.getName().toLowerCase();
        final String dimName = dimType.name().toLowerCase();
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

                UUID claimUniqueId = isWildernessClaim ? world.getUID() : UUID.randomUUID();
                ClaimType type = isWildernessClaim ? ClaimTypes.WILDERNESS : owner == null ? ClaimTypes.ADMIN : ClaimTypes.BASIC;

                ClaimDataConfig claimDataConfig = null;
                GDClaim newClaim = null;
                if (isWildernessClaim) {
                    newClaim = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(world.getUID()).getWildernessClaim();
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
                                System.out.println("FOUND PARENT " + parent.getUniqueId() + ", type = " + parent.getType() + ", name = " + parent.getName().orElse(TextComponent.of("unknown")));
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
                                    GriefDefenderPlugin.getInstance().getLogger().warning("Could not migrate region '" + rname + "' as it exceeds the maximum level supported by migrator. Skipping...");
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
                    claimDataConfig.setWorldUniqueId(world.getUID());
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
                        GriefDefenderPlugin.getInstance().getLogger().warning("Could not locate a valid UUID for user '" + builder + "' in region '" + rname + 
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
                        GriefDefenderPlugin.getInstance().getLogger().warning("Could not locate a valid UUID for user '" + manager + "' in region '" + rname + 
                                "'. Skipping...");
                        continue;
                    }
                    if (!claimDataConfig.getManagers().contains(managerUser.getUniqueId()) && owner != null && !managerUser.getUniqueId().equals(owner.getUniqueId())) {
                        claimDataConfig.getManagers().add(managerUser.getUniqueId());
                    }
                }
                final Set<Context> contexts = new HashSet<>();
                contexts.add(newClaim.getContext());
                final Context sourcePlayer = new Context("source", "player");
                final Context sourceTnt = new Context("source", "tnt");
                final Context sourceCreeper = new Context("source", "creeper");
                final Context sourceEnderDragon = new Context("source", "enderdragon");
                final Context sourceGhast = new Context("source", "ghast");
                final Context sourceEnderman = new Context("source", "enderman");
                final Context sourceSnowman = new Context("source", "snowman");
                final Context sourceMonster = new Context("source", "monster");
                final Context sourceWither = new Context("source", "wither");
                final Context sourceLava = new Context("source", "flowing_lava");
                final Context sourceWater = new Context("source", "flowing_water");
                final Context sourceLightningBolt = new Context("source", "lightning_bolt");
                final Context sourceFall = new Context("source", "fall");
                final Context sourceFireworks = new Context("source", "fireworks");
                // migrate flags
                for (Entry<Object, ? extends ConfigurationNode> mapEntry : flagsMap.entrySet()) {
                    if (!(mapEntry.getKey() instanceof String)) {
                        continue;
                    }
                    final String flag = (String) mapEntry.getKey();
                    ConfigurationNode valueNode = mapEntry.getValue();
                    System.out.println("Found flag " + flag + " with val " + valueNode.getString());
                    switch (flag) {
                        case "build":
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.BLOCK_BREAK, Tristate.FALSE, contexts);
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.BLOCK_PLACE, Tristate.FALSE, contexts);
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.INTERACT_BLOCK_PRIMARY, Tristate.FALSE, contexts);
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.INTERACT_BLOCK_SECONDARY, Tristate.FALSE, contexts);
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.INTERACT_ENTITY_PRIMARY, Tristate.FALSE, contexts);
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.INTERACT_ENTITY_SECONDARY, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.BLOCK_PLACE, Tristate.TRUE, contexts);
                            }
                            break;
                        case "interact":
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.INTERACT_BLOCK_PRIMARY, Tristate.FALSE, contexts);
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.INTERACT_BLOCK_SECONDARY, Tristate.FALSE, contexts);
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.INTERACT_ENTITY_PRIMARY, Tristate.FALSE, contexts);
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.INTERACT_ENTITY_SECONDARY, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.BLOCK_PLACE, Tristate.TRUE, contexts);
                            }
                            break;
                        case "block-break":
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.BLOCK_BREAK, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.BLOCK_BREAK, Tristate.TRUE, contexts);
                            }
                            break;
                        case "block-place":
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.BLOCK_PLACE, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.BLOCK_PLACE, Tristate.TRUE, contexts);
                            }
                            break;
                        case "use":
                            if (valueNode.getString().equals("allow")) {
                                newClaim.addUserTrust(GriefDefenderPlugin.PUBLIC_UUID, TrustTypes.ACCESSOR);
                            }
                            break;
                        case "damage-animals":
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.ENTITY_DAMAGE, "animal", Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.ENTITY_DAMAGE, "animal", Tristate.TRUE, contexts);
                            }
                            break;
                        case "chest-access":
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.INTERACT_BLOCK_SECONDARY, "chest", Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.INTERACT_BLOCK_SECONDARY, "chest", Tristate.TRUE, contexts);
                            }
                            break;
                        case "ride":
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.ENTITY_RIDING, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.ENTITY_RIDING, Tristate.TRUE, contexts);
                            }
                            break;
                        case "pvp":
                            contexts.add(sourcePlayer);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.ENTITY_DAMAGE, "player", Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.ENTITY_DAMAGE, "player", Tristate.TRUE, contexts);
                            }
                            contexts.remove(sourcePlayer);
                            break;
                        case "sleep":
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.INTERACT_BLOCK_SECONDARY, "bed", Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.INTERACT_BLOCK_SECONDARY, "bed", Tristate.TRUE, contexts);
                            }
                            break;
                        case "tnt":
                            contexts.add(sourceTnt);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.EXPLOSION_BLOCK, Tristate.FALSE, contexts);
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.EXPLOSION_ENTITY, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.EXPLOSION_BLOCK, Tristate.TRUE, contexts);
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.EXPLOSION_ENTITY, Tristate.TRUE, contexts);
                            }
                            contexts.remove(sourceTnt);
                            break;
                        case "vehicle-place":
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.INTERACT_BLOCK_SECONDARY, "boat", Tristate.FALSE, contexts);
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.INTERACT_BLOCK_SECONDARY, "minecart", Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.INTERACT_BLOCK_SECONDARY, "boat", Tristate.TRUE, contexts);
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.INTERACT_BLOCK_SECONDARY, "minecart", Tristate.TRUE, contexts);
                            }
                            break;
                        case "lighter":
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.INTERACT_ITEM_SECONDARY, "flint_and_steel", Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.INTERACT_ITEM_SECONDARY, "flint_and_steel", Tristate.TRUE, contexts);
                            }
                            break;
                        case "block-trampling":
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.COLLIDE_BLOCK, "farmland", Tristate.FALSE, contexts);
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.COLLIDE_BLOCK, "turtle_egg", Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.COLLIDE_BLOCK, "farmland", Tristate.TRUE, contexts);
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.COLLIDE_BLOCK, "turtle_egg", Tristate.TRUE, contexts);
                            }
                            break;
                        case "frosted-ice-form":
                            break;
                        case "creeper-explosion":
                            contexts.add(sourceCreeper);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.EXPLOSION_BLOCK, Tristate.FALSE, contexts);
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.EXPLOSION_ENTITY, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.EXPLOSION_BLOCK, Tristate.TRUE, contexts);
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.EXPLOSION_ENTITY, Tristate.TRUE, contexts);
                            }
                            contexts.remove(sourceCreeper);
                            break;
                        case "enderdragon-block-damage":
                            contexts.add(sourceEnderDragon);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.BLOCK_BREAK, Tristate.FALSE, contexts);
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.BLOCK_MODIFY, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.BLOCK_BREAK, Tristate.TRUE, contexts);
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.BLOCK_MODIFY, Tristate.TRUE, contexts);
                            }
                            contexts.remove(sourceEnderDragon);
                            break;
                        case "ghast-fireball":
                            contexts.add(sourceGhast);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.BLOCK_BREAK, Tristate.FALSE, contexts);
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.BLOCK_MODIFY, Tristate.FALSE, contexts);
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.ENTITY_DAMAGE, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.BLOCK_BREAK, Tristate.TRUE, contexts);
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.BLOCK_MODIFY, Tristate.TRUE, contexts);
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.ENTITY_DAMAGE, Tristate.TRUE, contexts);
                            }
                            contexts.remove(sourceGhast);
                            break;
                        case "other-explosion":
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.EXPLOSION_BLOCK, Tristate.FALSE, contexts);
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.EXPLOSION_ENTITY, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.EXPLOSION_BLOCK, Tristate.TRUE, contexts);
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.EXPLOSION_ENTITY, Tristate.TRUE, contexts);
                            }
                            break;
                        case "fire-spread":
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.FIRE_SPREAD, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.FIRE_SPREAD, Tristate.TRUE, contexts);
                            }
                            break;
                        case "enderman-grief":
                            contexts.add(sourceEnderman);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.BLOCK_BREAK, Tristate.FALSE, contexts);
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.BLOCK_MODIFY, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.BLOCK_BREAK, Tristate.TRUE, contexts);
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.BLOCK_MODIFY, Tristate.TRUE, contexts);
                            }
                            contexts.remove(sourceEnderman);
                            break;
                        case "snowman-trail":
                            contexts.add(sourceSnowman);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.BLOCK_MODIFY, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.BLOCK_MODIFY, Tristate.TRUE, contexts);
                            }
                            contexts.remove(sourceSnowman);
                            break;
                        case "mob-damage": 
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.ENTITY_DAMAGE, "monster", Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.ENTITY_DAMAGE, "monster", Tristate.TRUE, contexts);
                            }
                            break;
                        case "mob-spawning": 
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.ENTITY_SPAWN, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.ENTITY_SPAWN, Tristate.TRUE, contexts);
                            }
                            break;
                        case "deny-spawn": 
                            List<String> entityTypes = valueNode.getList(TypeToken.of(String.class));
                            for (String entityType : entityTypes) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.ENTITY_SPAWN, entityType, Tristate.FALSE, contexts);
                            }
                            break;
                        case "entity-painting-destroy": 
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.ENTITY_DAMAGE, "painting", Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.ENTITY_DAMAGE, "painting", Tristate.TRUE, contexts);
                            }
                            break;
                        case "entity-item-frame-destroy": 
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.ENTITY_DAMAGE, "item_frame", Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.ENTITY_DAMAGE, "item_frame", Tristate.TRUE, contexts);
                            }
                            break;
                        case "wither-damage":
                            contexts.add(sourceWither);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.BLOCK_BREAK, Tristate.FALSE, contexts);
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.BLOCK_MODIFY, Tristate.FALSE, contexts);
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.ENTITY_DAMAGE, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.BLOCK_BREAK, Tristate.TRUE, contexts);
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.BLOCK_MODIFY, Tristate.TRUE, contexts);
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.ENTITY_DAMAGE, Tristate.TRUE, contexts);
                            }
                            contexts.remove(sourceWither);
                            break;
                        case "lava-fire": 
                            contexts.add(sourceLava);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.FIRE_SPREAD, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.FIRE_SPREAD, Tristate.TRUE, contexts);
                            }
                            contexts.remove(sourceLava);
                            break;
                        case "lightning": 
                            contexts.add(sourceLightningBolt);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.ENTITY_DAMAGE, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.ENTITY_DAMAGE, Tristate.TRUE, contexts);
                            }
                            contexts.remove(sourceLightningBolt);
                            break;
                        case "water-flow": 
                            contexts.add(sourceWater);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.LIQUID_FLOW, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.LIQUID_FLOW, Tristate.TRUE, contexts);
                            }
                            contexts.remove(sourceWater);
                            break;
                        case "lava-flow": 
                            contexts.add(sourceLava);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.LIQUID_FLOW, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.LIQUID_FLOW, Tristate.TRUE, contexts);
                            }
                            contexts.remove(sourceLava);
                            break;
                        case "snow-fall": 
                        case "snow-melt": 
                        case "ice-form": 
                        case "ice-melt": 
                        case "frosted-ice-melt":
                            break;
                        case "mushroom-growth":
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.BLOCK_GROW, "mushroom", Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.BLOCK_GROW, "mushroom", Tristate.TRUE, contexts);
                            }
                            break;
                        case "leaf-decay":
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.LEAF_DECAY, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.LEAF_DECAY, Tristate.TRUE, contexts);
                            }
                            break;
                        case "grass-growth":
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.BLOCK_GROW, "grass", Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.BLOCK_GROW, "grass", Tristate.TRUE, contexts);
                            }
                            break;
                        case "mycelium-spread":
                            break;
                        case "vine-growth":
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.BLOCK_GROW, "vine", Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.BLOCK_GROW, "vine", Tristate.TRUE, contexts);
                            }
                            break;
                        case "crop-growth":
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.BLOCK_GROW, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.BLOCK_GROW, Tristate.TRUE, contexts);
                            }
                            break;
                        case "soil-dry":
                            break;
                        case "entry":
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.ENTER_CLAIM, "player", Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.ENTER_CLAIM, "player", Tristate.TRUE, contexts);
                            }
                            break;
                        case "exit":
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.EXIT_CLAIM, "player", Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.EXIT_CLAIM, "player", Tristate.TRUE, contexts);
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
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.INTERACT_ITEM_SECONDARY, "enderpearl", Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.INTERACT_ITEM_SECONDARY, "enderpearl", Tristate.TRUE, contexts);
                            }
                            break;
                        case "chorus-fruit-teleport":
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.INTERACT_ITEM_SECONDARY, "chorus_fruit", Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.INTERACT_ITEM_SECONDARY, "chorus_fruit", Tristate.TRUE, contexts);
                            }
                            break;
                        case "teleport":
                            final String location = valueNode.getString();
                            break;
                        case "spawn":
                            break;
                        case "item-pickup":
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.ITEM_PICKUP, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.ITEM_PICKUP, Tristate.TRUE, contexts);
                            }
                            break;
                        case "item-drop":
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.ITEM_DROP, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.ITEM_DROP, Tristate.TRUE, contexts);
                            }
                            break;
                        case "exp-drop":
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.ITEM_DROP, "xp_orb", Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.ITEM_DROP, "xp_orb", Tristate.TRUE, contexts);
                            }
                            break;
                        case "deny-message":
                            break;
                        case "invincible":
                            if (valueNode.getString().equals("allow")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.ENTITY_DAMAGE, "player", Tristate.FALSE, contexts);
                            }
                            break;
                        case "fall-damage":
                            contexts.add(sourceFall);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.ENTITY_DAMAGE, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.ENTITY_DAMAGE, Tristate.TRUE, contexts);
                            }
                            contexts.remove(sourceFall);
                            break;
                        case "firework-damage":
                            contexts.add(sourceFireworks);
                            if (valueNode.getString().equals("deny")) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.ENTITY_DAMAGE, Tristate.FALSE, contexts);
                            } else {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.ENTITY_DAMAGE, Tristate.TRUE, contexts);
                            }
                            contexts.remove(sourceFireworks);
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
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.COMMAND_EXECUTE, cmd, Tristate.FALSE, contexts);
                            }
                            break;
                        case "allowed-cmds":
                            List<String> allowed = valueNode.getList(TypeToken.of(String.class));
                            for (String cmd : allowed) {
                                PERMISSION_MANAGER.setPermission(newClaim, Flags.COMMAND_EXECUTE, cmd, Tristate.TRUE, contexts);
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
