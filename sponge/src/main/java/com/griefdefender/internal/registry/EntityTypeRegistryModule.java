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
package com.griefdefender.internal.registry;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import net.minecraft.entity.EnumCreatureType;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.common.entity.SpongeEntityType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class EntityTypeRegistryModule {

    protected final Map<String, GDEntityType> entityTypeMappings = new HashMap<>();
    public final BiMap<String, EnumCreatureType> SPAWN_TYPES = HashBiMap.create();
    private final org.spongepowered.common.registry.type.entity.EntityTypeRegistryModule SPONGE_REGISTRY = org.spongepowered.common.registry.type.entity.EntityTypeRegistryModule.getInstance();

    public static EntityTypeRegistryModule getInstance() {
        return Holder.INSTANCE;
    }

    public Optional<GDEntityType> getById(String id) {
        if (!checkNotNull(id).contains(":")) {
            id = "minecraft:" + id;
        }
        return Optional.ofNullable(this.entityTypeMappings.get(id.toLowerCase(Locale.ENGLISH)));
    }

    public Collection<GDEntityType> getAll() {
        return ImmutableList.copyOf(this.entityTypeMappings.values());
    }

    public String getFriendlyCreatureTypeName(Living entity) {
        return SPAWN_TYPES.inverse().get(((SpongeEntityType) entity.getType()).getEnumCreatureType());
    }

    public String getFriendlyCreatureTypeName(EnumCreatureType type) {
        return SPAWN_TYPES.inverse().get(type);
    }

    public EnumCreatureType getCreatureTypeByName(String name) {
        return SPAWN_TYPES.get(name);
    }

    public void registerDefaults() {
        for (EntityType type : SPONGE_REGISTRY.getAll()) {
            this.entityTypeMappings.put(type.getId(), new GDEntityType(type));
        }
        SPAWN_TYPES.put("animal", EnumCreatureType.CREATURE);
        SPAWN_TYPES.put("ambient", EnumCreatureType.AMBIENT);
        SPAWN_TYPES.put("aquatic", EnumCreatureType.WATER_CREATURE);
        SPAWN_TYPES.put("monster", EnumCreatureType.MONSTER);
    }

    public void registerAdditionalCatalog(EntityType type) {
        this.entityTypeMappings.put(type.getId(), new GDEntityType(type));
    }

    private static final class Holder {

        static final EntityTypeRegistryModule INSTANCE = new EntityTypeRegistryModule();
    }
}
