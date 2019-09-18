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
package com.griefdefender.permission.flag;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.ClaimContexts;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.flag.Flag;
import com.griefdefender.api.permission.flag.Flags;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.event.GDCauseStackManager;

public class GDCustomFlagDefinitions {

    // ADMIN
    public static final GDCustomFlagDefinition BLOCK_BREAK;
    public static final GDCustomFlagDefinition BLOCK_GROW;
    public static final GDCustomFlagDefinition BLOCK_PLACE;
    public static final GDCustomFlagDefinition BLOCK_SPREAD;
    public static final GDCustomFlagDefinition ENDERPEARL;
    public static final GDCustomFlagDefinition EXIT_PLAYER;
    public static final GDCustomFlagDefinition EXPLOSION_BLOCK;
    public static final GDCustomFlagDefinition EXPLOSION_ENTITY;
    public static final GDCustomFlagDefinition EXP_DROP;
    public static final GDCustomFlagDefinition FALL_DAMAGE;
    public static final GDCustomFlagDefinition INTERACT_BLOCK;
    public static final GDCustomFlagDefinition INTERACT_ENTITY;
    public static final GDCustomFlagDefinition INTERACT_INVENTORY;
    public static final GDCustomFlagDefinition INVINCIBLE;
    public static final GDCustomFlagDefinition ITEM_DROP;
    public static final GDCustomFlagDefinition ITEM_PICKUP;
    public static final GDCustomFlagDefinition MONSTER_DAMAGE;
    public static final GDCustomFlagDefinition PISTONS;
    public static final GDCustomFlagDefinition PORTAL_USE;
    public static final GDCustomFlagDefinition SPAWN_MONSTER;
    public static final GDCustomFlagDefinition TELEPORT_FROM;
    public static final GDCustomFlagDefinition TELEPORT_TO;
    public static final GDCustomFlagDefinition USE;
    public static final GDCustomFlagDefinition VEHICLE_DESTROY;
    public static final GDCustomFlagDefinition WITHER_DAMAGE;

    // USER
    public static final GDCustomFlagDefinition BLOCK_TRAMPLING;
    public static final GDCustomFlagDefinition CHEST_ACCESS;
    public static final GDCustomFlagDefinition CHORUS_FRUIT_TELEPORT;
    public static final GDCustomFlagDefinition CROP_GROWTH;
    public static final GDCustomFlagDefinition DAMAGE_ANIMALS;
    public static final GDCustomFlagDefinition ENDERMAN_GRIEF;
    public static final GDCustomFlagDefinition ENTER_PLAYER;
    public static final GDCustomFlagDefinition EXPLOSION_CREEPER;
    public static final GDCustomFlagDefinition EXPLOSION_TNT;
    public static final GDCustomFlagDefinition FIRE_DAMAGE;
    public static final GDCustomFlagDefinition FIRE_SPREAD;
    public static final GDCustomFlagDefinition GRASS_GROWTH;
    public static final GDCustomFlagDefinition ICE_FORM;
    public static final GDCustomFlagDefinition ICE_MELT;
    public static final GDCustomFlagDefinition LAVA_FLOW;
    public static final GDCustomFlagDefinition LEAF_DECAY;
    public static final GDCustomFlagDefinition LIGHTNING;
    public static final GDCustomFlagDefinition LIGHTER;
    public static final GDCustomFlagDefinition MUSHROOM_GROWTH;
    public static final GDCustomFlagDefinition MYCELIUM_SPREAD;
    public static final GDCustomFlagDefinition PVP;
    public static final GDCustomFlagDefinition RIDE;
    public static final GDCustomFlagDefinition SLEEP;
    public static final GDCustomFlagDefinition SNOW_FALL;
    public static final GDCustomFlagDefinition SNOW_MELT;
    public static final GDCustomFlagDefinition SNOWMAN_TRAIL;
    public static final GDCustomFlagDefinition SOIL_DRY;
    public static final GDCustomFlagDefinition SPAWN_AMBIENT;
    public static final GDCustomFlagDefinition SPAWN_ANIMAL;
    public static final GDCustomFlagDefinition SPAWN_AQUATIC;
    public static final GDCustomFlagDefinition VEHICLE_DESTROY_CLAIM;
    public static final GDCustomFlagDefinition VEHICLE_PLACE;
    public static final GDCustomFlagDefinition VINE_GROWTH;
    public static final GDCustomFlagDefinition WATER_FLOW;

    public static final List<GDCustomFlagDefinition> ADMIN_FLAGS = new ArrayList<>();
    public static final List<GDCustomFlagDefinition> USER_FLAGS = new ArrayList<>();

    static {
        Set<Context> contexts = new HashSet<>();

        contexts = new HashSet<>();
        BLOCK_BREAK = new GDCustomFlagDefinition(Flags.BLOCK_BREAK, contexts, "block-break", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_BLOCK_BREAK);

        contexts = new HashSet<>();
        BLOCK_PLACE = new GDCustomFlagDefinition(Flags.BLOCK_PLACE, contexts, "block-place", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_BLOCK_PLACE);

        contexts = new HashSet<>();
        BLOCK_GROW = new GDCustomFlagDefinition(Flags.BLOCK_GROW, contexts, "block-grow", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_BLOCK_GROW);

        contexts = new HashSet<>();
        BLOCK_SPREAD = new GDCustomFlagDefinition(Flags.BLOCK_SPREAD, contexts, "block-spread", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_BLOCK_SPREAD);

        // ADMIN
        contexts = new HashSet<>();
        contexts.add(FlagContexts.SOURCE_ENDERMAN);
        ENDERPEARL = new GDCustomFlagDefinition(Flags.INTERACT_ITEM_SECONDARY, contexts, "enderpearl", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_ENDERPEARL);
        ENDERPEARL.getDefinitionContexts().add(ClaimContexts.GLOBAL_DEFAULT_CONTEXT);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.SOURCE_PLAYER);
        ENTER_PLAYER = new GDCustomFlagDefinition(Flags.ENTER_CLAIM, contexts, "enter-player", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_ENTER_PLAYER);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.SOURCE_PLAYER);
        EXIT_PLAYER = new GDCustomFlagDefinition(Flags.ENTER_CLAIM, contexts, "exit-player", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_EXIT_PLAYER);

        contexts = new HashSet<>();
        EXPLOSION_BLOCK = new GDCustomFlagDefinition(Flags.EXPLOSION_BLOCK, contexts, "explosion-block", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_EXPLOSION_BLOCK);
        EXPLOSION_BLOCK.getDefinitionContexts().add(ClaimContexts.GLOBAL_DEFAULT_CONTEXT);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.SOURCE_CREEPER);
        EXPLOSION_CREEPER = new GDCustomFlagDefinition(Flags.EXPLOSION_BLOCK, contexts, "explosion-creeper", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_EXPLOSION_CREEPER);
        EXPLOSION_CREEPER.addFlagData(Flags.EXPLOSION_ENTITY, contexts);

        contexts = new HashSet<>();
        EXPLOSION_ENTITY = new GDCustomFlagDefinition(Flags.EXPLOSION_ENTITY, contexts, "explosion-entity", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_EXPLOSION_ENTITY);
        EXPLOSION_ENTITY.getDefinitionContexts().add(ClaimContexts.GLOBAL_DEFAULT_CONTEXT);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.SOURCE_TNT);
        EXPLOSION_TNT = new GDCustomFlagDefinition(Flags.EXPLOSION_BLOCK, contexts, "explosion-tnt", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_EXPLOSION_TNT);
        EXPLOSION_TNT.addFlagData(Flags.EXPLOSION_ENTITY, contexts);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.TARGET_XP_ORB);
        EXP_DROP = new GDCustomFlagDefinition(Flags.ENTITY_SPAWN, contexts, "exp-drop", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_EXP_DROP);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.SOURCE_FALL);
        contexts.add(FlagContexts.TARGET_PLAYER);
        FALL_DAMAGE = new GDCustomFlagDefinition(Flags.ENTITY_DAMAGE, contexts, "fall-damage", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_FALL_DAMAGE);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.SOURCE_PLAYER);
        INTERACT_BLOCK = new GDCustomFlagDefinition(Flags.INTERACT_BLOCK_SECONDARY, contexts, "interact-block", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_INTERACT_BLOCK);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.SOURCE_PLAYER);
        INTERACT_ENTITY = new GDCustomFlagDefinition(Flags.INTERACT_ENTITY_SECONDARY, contexts, "interact-entity", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_INTERACT_ENTITY);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.SOURCE_PLAYER);
        INTERACT_INVENTORY = new GDCustomFlagDefinition(Flags.INTERACT_INVENTORY, contexts, "interact-inventory", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_INTERACT_INVENTORY);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.TARGET_PLAYER);
        INVINCIBLE = new GDCustomFlagDefinition(Flags.ENTITY_DAMAGE, contexts, "invincible", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_INVINCIBLE);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.SOURCE_PLAYER);
        ITEM_DROP = new GDCustomFlagDefinition(Flags.ITEM_DROP, contexts, "item-drop", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_ITEM_DROP);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.SOURCE_PLAYER);
        ITEM_PICKUP = new GDCustomFlagDefinition(Flags.ITEM_PICKUP, contexts, "item-pickup", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_ITEM_PICKUP);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.SOURCE_TYPE_MONSTER);
        MONSTER_DAMAGE = new GDCustomFlagDefinition(Flags.ENTITY_DAMAGE, contexts, "monster-damage", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_MONSTER_DAMAGE);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.TARGET_PISTON);
        PISTONS = new GDCustomFlagDefinition(Flags.INTERACT_BLOCK_SECONDARY, contexts, "pistons", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_PISTONS);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.SOURCE_PLAYER);
        contexts.add(FlagContexts.TARGET_TYPE_PORTAL);
        PORTAL_USE = new GDCustomFlagDefinition(Flags.INTERACT_BLOCK_SECONDARY, contexts, "portal-use", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_PORTAL_USE);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.TARGET_TYPE_MONSTER);
        SPAWN_MONSTER = new GDCustomFlagDefinition(Flags.ENTITY_SPAWN, contexts, "spawn-monster", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_SPAWN_MONSTER);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.TARGET_PLAYER);
        TELEPORT_FROM = new GDCustomFlagDefinition(Flags.ENTITY_TELEPORT_FROM, contexts, "teleport-from", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_TELEPORT_FROM);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.TARGET_PLAYER);
        TELEPORT_TO = new GDCustomFlagDefinition(Flags.ENTITY_TELEPORT_TO, contexts, "teleport-to", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_TELEPORT_TO);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.TARGET_TYPE_VEHICLE);
        VEHICLE_DESTROY = new GDCustomFlagDefinition(Flags.ENTITY_DAMAGE, contexts, "vehicle-destroy", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_VEHICLE_DESTROY);
        VEHICLE_DESTROY.getDefinitionContexts().add(ClaimContexts.GLOBAL_DEFAULT_CONTEXT);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.SOURCE_WITHER);
        WITHER_DAMAGE = new GDCustomFlagDefinition(Flags.ENTITY_DAMAGE, contexts, "wither-damage", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_WITHER_DAMAGE);
        WITHER_DAMAGE.getDefinitionContexts().add(ClaimContexts.GLOBAL_DEFAULT_CONTEXT);

        ADMIN_FLAGS.add(BLOCK_BREAK);
        ADMIN_FLAGS.add(BLOCK_PLACE);
        ADMIN_FLAGS.add(BLOCK_GROW);
        ADMIN_FLAGS.add(BLOCK_SPREAD);
        ADMIN_FLAGS.add(ENDERPEARL);
        ADMIN_FLAGS.add(ENTER_PLAYER);
        ADMIN_FLAGS.add(EXIT_PLAYER);
        ADMIN_FLAGS.add(EXPLOSION_BLOCK);
        ADMIN_FLAGS.add(EXPLOSION_CREEPER);
        ADMIN_FLAGS.add(EXPLOSION_ENTITY);
        ADMIN_FLAGS.add(EXPLOSION_TNT);
        ADMIN_FLAGS.add(EXP_DROP);
        ADMIN_FLAGS.add(FALL_DAMAGE);
        ADMIN_FLAGS.add(INTERACT_BLOCK);
        ADMIN_FLAGS.add(INTERACT_ENTITY);
        ADMIN_FLAGS.add(INTERACT_INVENTORY);
        ADMIN_FLAGS.add(INVINCIBLE);
        ADMIN_FLAGS.add(ITEM_DROP);
        ADMIN_FLAGS.add(ITEM_PICKUP);
        ADMIN_FLAGS.add(MONSTER_DAMAGE);
        ADMIN_FLAGS.add(PISTONS);
        ADMIN_FLAGS.add(SPAWN_MONSTER);
        ADMIN_FLAGS.add(TELEPORT_FROM);
        ADMIN_FLAGS.add(TELEPORT_TO);
        ADMIN_FLAGS.add(VEHICLE_DESTROY);
        ADMIN_FLAGS.add(WITHER_DAMAGE);


        // USER
        contexts = new HashSet<>();
        contexts.add(FlagContexts.TARGET_FARMLAND);
        contexts.add(FlagContexts.TARGET_TURTLE_EGG);
        BLOCK_TRAMPLING = new GDCustomFlagDefinition(Flags.COLLIDE_BLOCK, contexts, "block-trampling", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_BLOCK_TRAMPLING);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.SOURCE_PLAYER);
        contexts.add(FlagContexts.TARGET_CHEST);
        CHEST_ACCESS = new GDCustomFlagDefinition(Flags.INTERACT_BLOCK_SECONDARY, contexts, "chest-access", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_CHEST_ACCESS);
        CHEST_ACCESS.addFlagData(Flags.INTERACT_INVENTORY, contexts);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.TARGET_CHORUS_FRUIT);
        CHORUS_FRUIT_TELEPORT = new GDCustomFlagDefinition(Flags.INTERACT_ITEM_SECONDARY, contexts, "chorus-fruit-teleport", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_CHORUS_FRUIT_TELEPORT);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.TARGET_TYPE_CROP);
        CROP_GROWTH = new GDCustomFlagDefinition(Flags.BLOCK_GROW, contexts, "crop-growth", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_CROP_GROWTH);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.TARGET_TYPE_ANIMAL);
        DAMAGE_ANIMALS = new GDCustomFlagDefinition(Flags.ENTITY_DAMAGE, contexts, "damage-animals", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_DAMAGE_ANIMALS);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.SOURCE_ENDERMAN);
        ENDERMAN_GRIEF = new GDCustomFlagDefinition(Flags.ENTITY_DAMAGE, contexts, "enderman-grief", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_ENDERMAN_GRIEF);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.SOURCE_FIRE);
        FIRE_DAMAGE = new GDCustomFlagDefinition(Flags.BLOCK_MODIFY, contexts, "fire-damage", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_FIRE_DAMAGE);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.SOURCE_FIRE);
        FIRE_SPREAD = new GDCustomFlagDefinition(Flags.BLOCK_SPREAD, contexts, "fire-spread", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_FIRE_SPREAD);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.TARGET_GRASS);
        GRASS_GROWTH = new GDCustomFlagDefinition(Flags.BLOCK_GROW, contexts, "grass-growth", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_GRASS_GROWTH);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.TARGET_ICE_FORM);
        ICE_FORM = new GDCustomFlagDefinition(Flags.BLOCK_MODIFY, contexts, "ice-form", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_ICE_FORM);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.TARGET_ICE_MELT);
        ICE_MELT = new GDCustomFlagDefinition(Flags.BLOCK_MODIFY, contexts, "ice-melt", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_ICE_MELT);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.SOURCE_LAVA);
        LAVA_FLOW = new GDCustomFlagDefinition(Flags.LIQUID_FLOW, contexts, "lava-flow", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_LAVA_FLOW);

        contexts = new HashSet<>();
        LEAF_DECAY = new GDCustomFlagDefinition(Flags.LEAF_DECAY, contexts, "leaf-decay", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_LEAF_DECAY);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.SOURCE_LIGHTNING_BOLT);
        LIGHTNING = new GDCustomFlagDefinition(Flags.ENTITY_DAMAGE, contexts, "lightning", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_LIGHTNING);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.TARGET_FLINTANDSTEEL);
        LIGHTER = new GDCustomFlagDefinition(Flags.INTERACT_ITEM_SECONDARY, contexts, "lighter", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_LIGHTER);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.TARGET_TYPE_MUSHROOM);
        MUSHROOM_GROWTH = new GDCustomFlagDefinition(Flags.BLOCK_GROW, contexts, "mushroom-growth", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_MUSHROOM_GROWTH);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.TARGET_MYCELIUM);
        MYCELIUM_SPREAD = new GDCustomFlagDefinition(Flags.BLOCK_SPREAD, contexts, "mycelium-spread", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_MYCELIUM_SPREAD);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.SOURCE_PLAYER);
        contexts.add(FlagContexts.TARGET_PLAYER);
        PVP = new GDCustomFlagDefinition(Flags.ENTITY_DAMAGE, contexts, "pvp", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_PVP);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.SOURCE_PLAYER);
        contexts.add(FlagContexts.TARGET_TYPE_VEHICLE);
        RIDE = new GDCustomFlagDefinition(Flags.ENTITY_RIDING, contexts, "ride", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_RIDE);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.SOURCE_PLAYER);
        contexts.add(FlagContexts.TARGET_BED);
        SLEEP = new GDCustomFlagDefinition(Flags.INTERACT_BLOCK_SECONDARY, contexts, "sleep", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_SLEEP);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.TARGET_SNOW_LAYER);
        SNOW_FALL = new GDCustomFlagDefinition(Flags.BLOCK_PLACE, contexts, "snow-fall", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_SNOW_FALL);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.TARGET_SNOW_LAYER);
        SNOW_MELT = new GDCustomFlagDefinition(Flags.BLOCK_BREAK, contexts, "snow-melt", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_SNOW_MELT);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.SOURCE_SNOWMAN);
        SNOWMAN_TRAIL = new GDCustomFlagDefinition(Flags.BLOCK_MODIFY, contexts, "snowman-trail", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_SNOWMAN_TRAIL);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.STATE_FARMLAND_DRY);
        SOIL_DRY = new GDCustomFlagDefinition(Flags.BLOCK_MODIFY, contexts, "soil-dry", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_SOIL_DRY);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.TARGET_TYPE_AMBIENT);
        SPAWN_AMBIENT = new GDCustomFlagDefinition(Flags.ENTITY_SPAWN, contexts, "spawn-ambient", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_SPAWN_AMBIENT);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.TARGET_TYPE_ANIMAL);
        SPAWN_ANIMAL = new GDCustomFlagDefinition(Flags.ENTITY_SPAWN, contexts, "spawn-animal", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_SPAWN_ANIMAL);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.TARGET_TYPE_AQUATIC);
        SPAWN_AQUATIC = new GDCustomFlagDefinition(Flags.ENTITY_SPAWN, contexts, "spawn-aquatic", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_SPAWN_AQUATIC);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.SOURCE_PLAYER);
        USE = new GDCustomFlagDefinition(Flags.INTERACT_BLOCK_SECONDARY, contexts, "use", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_USE);
        USE.addFlagData(Flags.INTERACT_ENTITY_SECONDARY, new HashSet<>(contexts));

        contexts = new HashSet<>();
        contexts.add(FlagContexts.TARGET_TYPE_VEHICLE);
        VEHICLE_DESTROY_CLAIM = new GDCustomFlagDefinition(Flags.ENTITY_DAMAGE, contexts, "vehicle-destroy", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_VEHICLE_DESTROY);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.TARGET_TYPE_VEHICLE);
        VEHICLE_PLACE = new GDCustomFlagDefinition(Flags.BLOCK_PLACE, contexts, "vehicle-place", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_VEHICLE_PLACE);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.TARGET_VINE);
        VINE_GROWTH = new GDCustomFlagDefinition(Flags.BLOCK_GROW, contexts, "vine-growth", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_VINE_GROWTH);

        contexts = new HashSet<>();
        contexts.add(FlagContexts.SOURCE_WATER);
        WATER_FLOW = new GDCustomFlagDefinition(Flags.LIQUID_FLOW, contexts, "water-flow", MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_WATER_FLOW);

        USER_FLAGS.add(CHEST_ACCESS);
        USER_FLAGS.add(CHORUS_FRUIT_TELEPORT);
        USER_FLAGS.add(CROP_GROWTH);
        USER_FLAGS.add(DAMAGE_ANIMALS);
        USER_FLAGS.add(ENDERMAN_GRIEF);
        USER_FLAGS.add(FIRE_DAMAGE);
        USER_FLAGS.add(FIRE_SPREAD);
        USER_FLAGS.add(GRASS_GROWTH);
        USER_FLAGS.add(ICE_FORM);
        USER_FLAGS.add(ICE_MELT);
        USER_FLAGS.add(LAVA_FLOW);
        USER_FLAGS.add(LEAF_DECAY);
        USER_FLAGS.add(LIGHTER);
        USER_FLAGS.add(LIGHTNING);
        USER_FLAGS.add(MYCELIUM_SPREAD);
        USER_FLAGS.add(PVP);
        USER_FLAGS.add(RIDE);
        USER_FLAGS.add(SLEEP);
        USER_FLAGS.add(SNOW_FALL);
        USER_FLAGS.add(SNOW_MELT);
        USER_FLAGS.add(SOIL_DRY);
        USER_FLAGS.add(SPAWN_AMBIENT);
        USER_FLAGS.add(SPAWN_ANIMAL);
        USER_FLAGS.add(SPAWN_AQUATIC);
        USER_FLAGS.add(USE);
        USER_FLAGS.add(VEHICLE_DESTROY_CLAIM);
        USER_FLAGS.add(VEHICLE_PLACE);
        USER_FLAGS.add(WATER_FLOW);
    }
}
