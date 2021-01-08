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
package com.griefdefender.registry;

import static com.google.common.base.Preconditions.checkNotNull;

import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.ClaimContexts;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.ContextKeys;
import com.griefdefender.api.permission.flag.FlagData;
import com.griefdefender.api.permission.flag.FlagDefinition;
import com.griefdefender.api.permission.flag.Flags;
import com.griefdefender.api.registry.CatalogRegistryModule;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.permission.flag.FlagContexts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class FlagDefinitionRegistryModule implements CatalogRegistryModule<FlagDefinition> {

    private static FlagDefinitionRegistryModule instance;
    final static Context OWNER_OVERRIDE_CONTEXT = new Context(ContextKeys.CLAIM_OVERRIDE, "claim");

    public static FlagDefinitionRegistryModule getInstance() {
        return instance;
    }

    private final Map<String, FlagDefinition> registryMap = new HashMap<>();

    @Override
    public Optional<FlagDefinition> getById(String id) {
        if (id == null) {
            return Optional.empty();
        }

        if (!id.contains(":")) {
            id = "griefdefender:" + id;
        }

        return Optional.ofNullable(this.registryMap.get(checkNotNull(id.toLowerCase())));
    }

    @Override
    public Collection<FlagDefinition> getAll() {
        return this.registryMap.values();
    }

    @Override
    public void registerDefaults() {
        List<FlagData> flagData = new ArrayList<>();
        Set<Context> definitionContexts = new HashSet<>();
        Set<Context> flagContexts = new HashSet<>();
        FlagDefinition.Builder definitionBuilder = GriefDefender.getRegistry().createBuilder(FlagDefinition.Builder.class);
        FlagData.Builder flagDataBuilder = GriefDefender.getRegistry().createBuilder(FlagData.Builder.class);

        // ADMIN
        flagContexts.add(FlagContexts.TARGET_TYPE_AMBIENT);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("ambient-spawn")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_SPAWN_AMBIENT)
                    .group("admin")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.ENTITY_SPAWN)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_TYPE_ANIMAL);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("animal-block-modify")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_ANIMAL_BLOCK_MODIFY)
                    .group("admin")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.BLOCK_MODIFY)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.TARGET_TYPE_ANIMAL);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("animal-spawn")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_SPAWN_ANIMAL)
                    .group("admin")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.ENTITY_SPAWN)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.TARGET_TYPE_AQUATIC);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("aquatic-spawn")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_SPAWN_AQUATIC)
                    .group("admin")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.ENTITY_SPAWN)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_PLAYER);
        flagContexts.add(FlagContexts.TARGET_ARMOR_STAND);
        flagData = new ArrayList<>();
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.INTERACT_ITEM_SECONDARY)
                .contexts(flagContexts)
                .build());
        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_PLAYER);
        flagContexts.add(FlagContexts.TARGET_ARMOR_STAND);
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.ENTITY_DAMAGE)
                .contexts(flagContexts)
                .build());
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.INTERACT_ENTITY_SECONDARY)
                .contexts(flagContexts)
                .build());
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("armorstand-use")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.FALSE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_ARMOR_STAND_USE)
                    .group("admin")
                    .flagData(flagData)
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_PLAYER);
        flagContexts.add(FlagContexts.TARGET_CHORUS_FRUIT);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("chorus-fruit-teleport")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.FALSE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_CHORUS_FRUIT_TELEPORT)
                    .group("admin")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.INTERACT_ITEM_SECONDARY)
                        .contexts(flagContexts)
                        .build())
                    .build());
 
        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_CREEPER);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("creeper-block-explosion")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.FALSE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_CREEPER_BLOCK_EXPLOSION)
                    .group("admin")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.EXPLOSION_BLOCK)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_CREEPER);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("creeper-entity-explosion")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.FALSE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_CREEPER_ENTITY_EXPLOSION)
                    .group("admin")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.EXPLOSION_ENTITY)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_ARROW);
        flagContexts.add(FlagContexts.TARGET_END_CRYSTAL);
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.PROJECTILE_IMPACT_ENTITY)
                .contexts(flagContexts)
                .build());
        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_PLAYER);
        flagContexts.add(FlagContexts.TARGET_END_CRYSTAL);
        flagData = new ArrayList<>();
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.INTERACT_ITEM_SECONDARY)
                .contexts(flagContexts)
                .build());
        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_PLAYER);
        flagContexts.add(FlagContexts.TARGET_END_CRYSTAL);
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.ENTITY_DAMAGE)
                .contexts(flagContexts)
                .build());
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.INTERACT_ENTITY_SECONDARY)
                .contexts(flagContexts)
                .build());
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("endcrystal-use")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.FALSE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_END_CRYSTAL_USE)
                    .group("admin")
                    .flagData(flagData)
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.TARGET_ARMOR_STAND);
        flagData = new ArrayList<>();
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.ENTITY_DAMAGE)
                .contexts(flagContexts)
                .build());
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.PROJECTILE_IMPACT_ENTITY)
                .contexts(flagContexts)
                .build());
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("entity-armorstand-damage")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.FALSE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_ENTITY_ARMORSTAND_DAMAGE)
                    .group("admin")
                    .flagData(flagData)
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.TARGET_ITEM_FRAME);
        flagData = new ArrayList<>();
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.ENTITY_DAMAGE)
                .contexts(flagContexts)
                .build());
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.PROJECTILE_IMPACT_ENTITY)
                .contexts(flagContexts)
                .build());
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("entity-itemframe-damage")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.FALSE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_ENTITY_ITEMFRAME_DAMAGE)
                    .group("admin")
                    .flagData(flagData)
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.TARGET_XP_ORB);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("exp-drop")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_EXP_DROP)
                    .group("admin")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.ENTITY_SPAWN)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_FALL);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("fall-entity-damage")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_FALL_ENTITY_DAMAGE)
                    .group("admin")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.ENTITY_DAMAGE)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_FALL);
        flagContexts.add(FlagContexts.TARGET_PLAYER);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("fall-player-damage")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_FALL_PLAYER_DAMAGE)
                    .group("admin")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.ENTITY_DAMAGE)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_FALL);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("falling-block-break")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_FALLING_BLOCK_BREAK)
                    .group("admin")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.BLOCK_BREAK)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_FIRE);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("fire-block-damage")
                    .admin(true)
                    .context(ClaimContexts.GLOBAL_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_FIRE_BLOCK_DAMAGE)
                    .group("admin")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.BLOCK_BREAK)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagData = new ArrayList<>();
        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_FIRE);
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.ENTITY_DAMAGE)
                .contexts(flagContexts)
                .build());
        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_FIRE_TICK);
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.ENTITY_DAMAGE)
                .contexts(flagContexts)
                .build());
        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_MAGMA_BLOCK);
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.ENTITY_DAMAGE)
                .contexts(flagContexts)
                .build());
        if (GriefDefenderPlugin.getMajorMinecraftVersion() > 12) {
            flagContexts = new HashSet<>();
            flagContexts.add(FlagContexts.SOURCE_LAVA);
            flagData.add(flagDataBuilder
                    .reset()
                    .flag(Flags.ENTITY_DAMAGE)
                    .contexts(flagContexts)
                    .build());
        } else {
            flagContexts = new HashSet<>();
            flagContexts.add(FlagContexts.SOURCE_LAVA_1_12);
            flagData.add(flagDataBuilder
                    .reset()
                    .flag(Flags.ENTITY_DAMAGE)
                    .contexts(flagContexts)
                    .build());
        }
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("fire-entity-damage")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_FIRE_ENTITY_DAMAGE)
                    .group("admin")
                    .flagData(flagData)
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_LIGHTNING_BOLT);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("lightning-damage")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_LIGHTNING)
                    .group("admin")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.ENTITY_DAMAGE)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_TYPE_MONSTER);
        flagContexts.add(FlagContexts.TARGET_TYPE_ANIMAL);
        flagData = new ArrayList<>();
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.ENTITY_DAMAGE)
                .contexts(flagContexts)
                .build());
        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_TYPE_MONSTER);
        flagContexts.add(FlagContexts.TARGET_TYPE_AQUATIC);
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.ENTITY_DAMAGE)
                .contexts(flagContexts)
                .build());
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.PROJECTILE_IMPACT_ENTITY)
                .contexts(flagContexts)
                .build());
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("monster-animal-damage")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.FALSE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_MONSTER_ANIMAL_DAMAGE)
                    .group("admin")
                    .flagData(flagData)
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_TYPE_MONSTER);
        flagContexts.add(FlagContexts.TARGET_PLAYER);
        flagData = new ArrayList<>();
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.ENTITY_DAMAGE)
                .contexts(flagContexts)
                .build());
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.PROJECTILE_IMPACT_ENTITY)
                .contexts(flagContexts)
                .build());
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("monster-player-damage")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_MONSTER_PLAYER_DAMAGE)
                    .group("admin")
                    .flagData(flagData)
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.TARGET_TYPE_MONSTER);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("monster-spawn")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_MONSTER_SPAWN)
                    .group("admin")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.ENTITY_SPAWN)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagData = new ArrayList<>();
        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_PISTON);
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.ITEM_SPAWN)
                .contexts(flagContexts)
                .build());
        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_PISTON_STICKY);
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.ITEM_SPAWN)
                .contexts(flagContexts)
                .build());
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("piston-item-spawn")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_PISTON_ITEM_SPAWN)
                    .group("admin")
                    .flagData(flagData)
                    .build());

        flagData = new ArrayList<>();
        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_PISTON);
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.BLOCK_BREAK)
                .contexts(flagContexts)
                .build());
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.BLOCK_PLACE)
                .contexts(flagContexts)
                .build());
        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_PISTON_STICKY);
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.BLOCK_BREAK)
                .contexts(flagContexts)
                .build());
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.BLOCK_PLACE)
                .contexts(flagContexts)
                .build());
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("piston-use")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.FALSE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_PISTON_USE)
                    .group("admin")
                    .flagData(flagData)
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_PLAYER);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("player-block-break")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.FALSE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_PLAYER_BLOCK_BREAK)
                    .group("admin")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.BLOCK_BREAK)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_PLAYER);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("player-block-interact")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.FALSE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_PLAYER_BLOCK_INTERACT)
                    .group("admin")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.INTERACT_BLOCK_SECONDARY)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_PLAYER);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("player-block-place")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.FALSE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_PLAYER_BLOCK_PLACE)
                    .group("admin")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.BLOCK_PLACE)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.TARGET_PLAYER);
        flagData = new ArrayList<>();
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.ENTITY_DAMAGE)
                .contexts(flagContexts)
                .build());
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.PROJECTILE_IMPACT_ENTITY)
                .contexts(flagContexts)
                .build());
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("player-damage")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_PLAYER_DAMAGE)
                    .group("admin")
                    .flagData(flagData)
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_PLAYER);
        flagContexts.add(FlagContexts.TARGET_ENDERPEARL);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("player-enderpearl-interact")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_PLAYER_ENDERPEARL_INTERACT)
                    .group("admin")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.INTERACT_ITEM_SECONDARY)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_PLAYER);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("player-enter")
                    .admin(true)
                    .context(new Context(ContextKeys.CLAIM, "claim"))
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_PLAYER_ENTER)
                    .group("admin")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.ENTER_CLAIM)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_PLAYER);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("player-entity-interact")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_PLAYER_ENTITY_INTERACT)
                    .group("admin")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.INTERACT_ENTITY_SECONDARY)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_PLAYER);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("player-exit")
                    .admin(true)
                    .context(new Context(ContextKeys.CLAIM, "claim"))
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_PLAYER_EXIT)
                    .group("admin")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.EXIT_CLAIM)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_PLAYER);
        flagContexts.add(FlagContexts.TARGET_ITEM_FRAME);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("player-itemframe-interact")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.FALSE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_PLAYER_ITEMFRAME_INTERACT)
                    .group("admin")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.INTERACT_ENTITY_SECONDARY)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_PLAYER);
        flagContexts.add(FlagContexts.TARGET_HANGING);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("player-itemhanging-place")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.FALSE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_PLAYER_ITEMHANGING_PLACE)
                    .group("admin")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.INTERACT_ITEM_SECONDARY)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_PLAYER);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("player-item-drop")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_PLAYER_ITEM_DROP)
                    .group("admin")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.ITEM_DROP)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_PLAYER);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("player-item-pickup")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_PLAYER_ITEM_PICKUP)
                    .group("admin")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.ITEM_PICKUP)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_END_PORTAL);
        flagContexts.add(FlagContexts.TARGET_PLAYER);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("player-endportal-use")
                    .admin(true)
                    .context(ClaimContexts.GLOBAL_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_PLAYER_ENDPORTAL_USE)
                    .group("admin")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.ENTITY_TELEPORT_FROM)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_NETHER_PORTAL);
        flagContexts.add(FlagContexts.TARGET_PLAYER);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("player-netherportal-use")
                    .admin(true)
                    .context(ClaimContexts.GLOBAL_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_PLAYER_NETHERPORTAL_USE)
                    .group("admin")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.ENTITY_TELEPORT_FROM)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.TARGET_PLAYER);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("player-teleport-from")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_PLAYER_TELEPORT_FROM)
                    .group("admin")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.ENTITY_TELEPORT_FROM)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.TARGET_PLAYER);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("player-teleport-to")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_PLAYER_TELEPORT_TO)
                    .group("admin")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.ENTITY_TELEPORT_TO)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_PLAYER);
        flagContexts.add(FlagContexts.TARGET_VILLAGER);
        flagData = new ArrayList<>();
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.ENTITY_DAMAGE)
                .contexts(flagContexts)
                .build());
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.PROJECTILE_IMPACT_ENTITY)
                .contexts(flagContexts)
                .build());
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("player-villager-damage")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.FALSE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_PLAYER_VILLAGER_DAMAGE)
                    .group("admin")
                    .flagData(flagData)
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_PLAYER);
        flagContexts.add(FlagContexts.TARGET_PLAYER);
        flagData = new ArrayList<>();
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.ENTITY_DAMAGE)
                .contexts(flagContexts)
                .build());
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.PROJECTILE_IMPACT_ENTITY)
                .contexts(flagContexts)
                .build());
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("pvp")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_PVP)
                    .group("admin")
                    .flagData(flagData)
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_SILVERFISH);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("silverfish-block-infest")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.FALSE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_SILVERFISH_BLOCK_INFEST)
                    .group("admin")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.BLOCK_MODIFY)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_TNT);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("tnt-block-explosion")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.FALSE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_TNT_BLOCK_EXPLOSION)
                    .group("admin")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.EXPLOSION_BLOCK)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_TNT);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("tnt-entity-explosion")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.FALSE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_TNT_ENTITY_EXPLOSION)
                    .group("admin")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.EXPLOSION_ENTITY)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_TURTLE_EGG);
        flagContexts.add(FlagContexts.TARGET_TURTLE_EGG);
        flagData = new ArrayList<>();
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.BLOCK_BREAK)
                .contexts(flagContexts)
                .build());
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("turtle-egg-hatch")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_TURTLE_EGG_HATCH)
                    .group("admin")
                    .flagData(flagData)
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_WITHER);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("wither-block-break")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.FALSE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_WITHER_BLOCK_BREAK)
                    .group("admin")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.BLOCK_BREAK)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_WITHER);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("wither-entity-damage")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_WITHER_ENTITY_DAMAGE)
                    .group("admin")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.ENTITY_DAMAGE)
                        .contexts(flagContexts)
                        .build())
                    .build());

        definitionContexts = new HashSet<>();
        definitionContexts.add(ClaimContexts.USER_DEFAULT_CONTEXT);
        definitionContexts.add(OWNER_OVERRIDE_CONTEXT);
        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_VILLAGER);
        flagContexts.add(FlagContexts.TARGET_TYPE_CROP);
        flagData = new ArrayList<>();
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.BLOCK_BREAK)
                .contexts(flagContexts)
                .build());
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.BLOCK_PLACE)
                .contexts(flagContexts)
                .build());
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("villager-farm")
                    .admin(true)
                    .contexts(definitionContexts)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_VILLAGER_FARM)
                    .group("admin")
                    .flagData(flagData)
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_PLAYER);
        flagContexts.add(FlagContexts.TARGET_VILLAGER);
        flagData = new ArrayList<>();
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.INTERACT_ENTITY_SECONDARY)
                .contexts(flagContexts)
                .build());
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("villager-trade")
                    .admin(true)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_VILLAGER_TRADE)
                    .group("admin")
                    .flagData(flagData)
                    .build());

        // USER
        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.TARGET_FARMLAND);
        flagContexts.add(FlagContexts.TARGET_TURTLE_EGG);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("block-trampling")
                    .admin(false)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.FALSE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_BLOCK_TRAMPLING)
                    .group("user")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.COLLIDE_BLOCK)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_PLAYER);
        flagContexts.add(FlagContexts.TARGET_CHEST);
        flagData = new ArrayList<>();
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.INTERACT_BLOCK_SECONDARY)
                .contexts(flagContexts)
                .build());
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.INTERACT_INVENTORY)
                .contexts(flagContexts)
                .build());
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("chest-access")
                    .admin(false)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.FALSE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_CHEST_ACCESS)
                    .group("user")
                    .flagData(flagData)
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.TARGET_TYPE_CROP);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("crop-growth")
                    .admin(false)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_CROP_GROWTH)
                    .group("user")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.BLOCK_GROW)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.TARGET_TYPE_ANIMAL);
        flagData = new ArrayList<>();
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.ENTITY_DAMAGE)
                .contexts(flagContexts)
                .build());
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.PROJECTILE_IMPACT_ENTITY)
                .contexts(flagContexts)
                .build());
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("damage-animals")
                    .admin(false)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.FALSE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_DAMAGE_ANIMALS)
                    .group("user")
                    .flagData(flagData)
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_ENDERMAN);
        flagData = new ArrayList<>();
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.BLOCK_BREAK)
                .contexts(flagContexts)
                .build());
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.BLOCK_PLACE)
                .contexts(flagContexts)
                .build());
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("enderman-grief")
                    .admin(false)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.FALSE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_ENDERMAN_GRIEF)
                    .group("user")
                    .flagData(flagData)
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_FIRE);
        definitionContexts = new HashSet<>();
        definitionContexts.add(ClaimContexts.USER_DEFAULT_CONTEXT);
        definitionContexts.add(OWNER_OVERRIDE_CONTEXT);
        flagData = new ArrayList<>();
        flagData.add(flagDataBuilder
                        .reset()
                        .flag(Flags.BLOCK_SPREAD)
                        .contexts(flagContexts)
                        .build());
        flagContexts = new HashSet<>();
        if (GriefDefenderPlugin.getMajorMinecraftVersion() > 12) {
            flagContexts.add(FlagContexts.SOURCE_LAVA);
        } else {
            flagContexts.add(FlagContexts.SOURCE_LAVA_1_12);
        }
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.BLOCK_SPREAD)
                .contexts(flagContexts)
                .build());
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("fire-spread")
                    .admin(false)
                    .contexts(definitionContexts)
                    .defaultValue(Tristate.FALSE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_FIRE_SPREAD)
                    .group("user")
                    .flagData(flagData)
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.TARGET_GRASS);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("grass-growth")
                    .admin(false)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_GRASS_GROWTH)
                    .group("user")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.BLOCK_GROW)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.TARGET_ICE_FORM);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("ice-form")
                    .admin(false)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_ICE_FORM)
                    .group("user")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.BLOCK_MODIFY)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_ICE);
        flagContexts.add(FlagContexts.TARGET_ICE_MELT);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("ice-melt")
                    .admin(false)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_ICE_MELT)
                    .group("user")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.BLOCK_MODIFY)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        if (GriefDefenderPlugin.getMajorMinecraftVersion() > 12) {
            flagContexts.add(FlagContexts.SOURCE_LAVA);
        } else {
            flagContexts.add(FlagContexts.SOURCE_LAVA_1_12);
        }
        flagContexts.add(FlagContexts.TARGET_AIR);
        flagData = new ArrayList<>();
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.LIQUID_FLOW)
                .contexts(flagContexts)
                .build());
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("lava-flow")
                    .admin(false)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.FALSE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_LAVA_FLOW)
                    .group("user")
                    .flagData(flagData)
                    .build());

        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("leaf-decay")
                    .admin(false)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_LEAF_DECAY)
                    .group("user")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.LEAF_DECAY)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_PLAYER);
        flagContexts.add(FlagContexts.TARGET_FLINTANDSTEEL);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("lighter")
                    .admin(false)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.FALSE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_LIGHTER)
                    .group("user")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.INTERACT_ITEM_SECONDARY)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.TARGET_TYPE_MUSHROOM);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("mushroom-growth")
                    .admin(false)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_MUSHROOM_GROWTH)
                    .group("user")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.BLOCK_GROW)
                        .context(FlagContexts.TARGET_TYPE_MUSHROOM)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.TARGET_MYCELIUM);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("mycelium-spread")
                    .admin(false)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_MYCELIUM_SPREAD)
                    .group("user")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.BLOCK_SPREAD)
                        .context(FlagContexts.TARGET_MYCELIUM)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_PLAYER);
        flagContexts.add(FlagContexts.TARGET_TYPE_VEHICLE);
        flagData = new ArrayList<>();
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.ENTITY_RIDING)
                .contexts(flagContexts)
                .build());
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.INTERACT_ENTITY_SECONDARY)
                .contexts(flagContexts)
                .build());
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("ride")
                    .admin(false)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_RIDE)
                    .group("user")
                    .flagData(flagData)
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_PLAYER);
        flagContexts.add(FlagContexts.TARGET_BED);
        flagData = new ArrayList<>();
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.INTERACT_BLOCK_SECONDARY)
                .contexts(flagContexts)
                .build());
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.INTERACT_ITEM_SECONDARY)
                .contexts(flagContexts)
                .build());
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("sleep")
                    .admin(false)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_SLEEP)
                    .group("user")
                    .flagData(flagData)
                    .build());

        definitionContexts = new HashSet<>();
        definitionContexts.add(ClaimContexts.USER_DEFAULT_CONTEXT);
        definitionContexts.add(OWNER_OVERRIDE_CONTEXT);
        flagContexts = new HashSet<>();
        if (GriefDefenderPlugin.getMajorMinecraftVersion() > 12) {
            flagContexts.add(FlagContexts.SOURCE_AIR);
            flagContexts.add(FlagContexts.TARGET_SNOW);
        } else {
            flagContexts.add(FlagContexts.SOURCE_AIR);
            flagContexts.add(FlagContexts.TARGET_SNOW_1_12);
        }
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("snow-fall")
                    .admin(false)
                    .contexts(definitionContexts)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_SNOW_FALL)
                    .group("user")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.BLOCK_PLACE)
                        .contexts(flagContexts)
                        .build())
                    .build());

        definitionContexts = new HashSet<>();
        definitionContexts.add(ClaimContexts.USER_DEFAULT_CONTEXT);
        definitionContexts.add(OWNER_OVERRIDE_CONTEXT);
        flagContexts = new HashSet<>();
        if (GriefDefenderPlugin.getMajorMinecraftVersion() > 12) {
            flagContexts.add(FlagContexts.SOURCE_SNOW);
        } else {
            flagContexts.add(FlagContexts.SOURCE_SNOW_1_12);
        }
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("snow-melt")
                    .admin(false)
                    .contexts(definitionContexts)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_SNOW_MELT)
                    .group("user")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.BLOCK_BREAK)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        if (GriefDefenderPlugin.getMajorMinecraftVersion() > 12) {
            flagContexts.add(FlagContexts.SOURCE_SNOWMAN);
            flagContexts.add(FlagContexts.TARGET_SNOW);
        } else {
            flagContexts.add(FlagContexts.SOURCE_SNOWMAN_1_12);
            flagContexts.add(FlagContexts.TARGET_SNOW);
        }
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("snowman-trail")
                    .admin(false)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_SNOWMAN_TRAIL)
                    .group("user")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.BLOCK_PLACE)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_FARMLAND);
        flagContexts.add(FlagContexts.STATE_FARMLAND_DRY);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("soil-dry")
                    .admin(false)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_SOIL_DRY)
                    .group("user")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.BLOCK_MODIFY)
                        .contexts(flagContexts)
                        .build())
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_PLAYER);
        flagContexts.add(FlagContexts.TARGET_TYPE_VEHICLE);
        flagData = new ArrayList<>();
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.INTERACT_ITEM_SECONDARY)
                .contexts(flagContexts)
                .build());
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.ENTITY_DAMAGE)
                .contexts(flagContexts)
                .build());
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.ENTITY_RIDING)
                .contexts(flagContexts)
                .build());
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.INTERACT_ENTITY_SECONDARY)
                .contexts(flagContexts)
                .build());
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("vehicle-use")
                    .admin(false)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.FALSE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_VEHICLE_USE)
                    .group("user")
                    .flagData(flagData)
                    .build());

        definitionContexts = new HashSet<>();
        definitionContexts.add(ClaimContexts.USER_DEFAULT_CONTEXT);
        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_PLAYER);
        flagContexts.add(FlagContexts.TARGET_VILLAGER);
        flagData = new ArrayList<>();
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.INTERACT_ENTITY_SECONDARY)
                .contexts(flagContexts)
                .build());
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("villager-trade")
                    .admin(false)
                    .contexts(definitionContexts)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_VILLAGER_TRADE)
                    .group("user")
                    .flagData(flagData)
                    .build());

        flagContexts = new HashSet<>();
        flagContexts.add(FlagContexts.SOURCE_PLAYER);
        flagContexts.add(FlagContexts.TARGET_VILLAGER);
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("vine-growth")
                    .admin(false)
                    .context(ClaimContexts.USER_DEFAULT_CONTEXT)
                    .defaultValue(Tristate.TRUE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_VINE_GROWTH)
                    .group("user")
                    .flagData(flagDataBuilder
                        .reset()
                        .flag(Flags.BLOCK_GROW)
                        .context(FlagContexts.TARGET_VINE)
                        .build())
                    .build());

        definitionContexts = new HashSet<>();
        definitionContexts.add(ClaimContexts.USER_DEFAULT_CONTEXT);
        definitionContexts.add(OWNER_OVERRIDE_CONTEXT);
        flagContexts = new HashSet<>();
        if (GriefDefenderPlugin.getMajorMinecraftVersion() > 12) {
            flagContexts.add(FlagContexts.SOURCE_WATER);
        } else {
            flagContexts.add(FlagContexts.SOURCE_WATER_1_12);
        }
        flagContexts.add(FlagContexts.TARGET_AIR);
        flagData = new ArrayList<>();
        flagData.add(flagDataBuilder
                .reset()
                .flag(Flags.LIQUID_FLOW)
                .contexts(flagContexts)
                .build());
        this.registerCustomType(
                definitionBuilder
                    .reset()
                    .name("water-flow")
                    .admin(false)
                    .contexts(definitionContexts)
                    .defaultValue(Tristate.FALSE)
                    .description(MessageCache.getInstance().FLAG_DESCRIPTION_CUSTOM_WATER_FLOW)
                    .group("user")
                    .flagData(flagData)
                    .build());
    }

    @Override
    public void registerCustomType(FlagDefinition type) {
        this.registryMap.put(type.getId().toLowerCase(), type);
    }

    static {
        instance = new FlagDefinitionRegistryModule();
    }
}
