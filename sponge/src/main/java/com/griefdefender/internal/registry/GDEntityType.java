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

import com.griefdefender.api.CatalogType;
import com.griefdefender.api.permission.Context;
import net.minecraft.entity.EnumCreatureType;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.common.entity.SpongeEntityType;

import javax.annotation.Nullable;

public class GDEntityType implements CatalogType {

    public final int entityTypeId;
    public final String modId;
    public final EntityType type;
    public EnumCreatureType creatureType;

    public GDEntityType(EntityType type) {
        this.type = type;
        this.modId = ((SpongeEntityType) this.type).modId;
        this.entityTypeId = ((SpongeEntityType) type).entityTypeId;
        this.creatureType = ((SpongeEntityType) type).getEnumCreatureType();
    }

    @Override
    public String getName() {
        return this.type.getName();
    }

    public String getId() {
        return this.type.getId();
    }

    public String getModId() {
        return this.modId;
    }

    @Nullable
    public String getEnumCreatureTypeId() {
        if (this.getEnumCreatureType() == null) {
            return null;
        }
        switch (this.creatureType) {
            case CREATURE:
                return this.modId + ":animal";
            case WATER_CREATURE:
                return this.modId + ":aquatic";
            default:
                break;
        }
        return this.modId + ":" + this.creatureType.name().toLowerCase();
    }

    @Nullable
    public Context getEnumCreatureTypeContext(boolean isSource) {
        if (this.creatureType == null) {
            return null;
        }

        final String contextKey = isSource ? "source" : "target";
        switch (this.creatureType) {
            case CREATURE:
                return new Context(contextKey, "#" + this.modId + ":animal");
            case WATER_CREATURE:
                return new Context(contextKey, "#" + this.modId + ":aquatic");
            default:
                break;
        }
        return new Context(contextKey, "#" + this.modId + ":" + this.creatureType.name().toLowerCase());
    }

    @Nullable
    public EnumCreatureType getEnumCreatureType() {
        if (this.creatureType == null) {
            final SpongeEntityType spongeEntityType = ((SpongeEntityType) Sponge.getRegistry().getType(EntityType.class, this.getId()).orElse(null));
            if (spongeEntityType != null) {
                this.creatureType = spongeEntityType.getEnumCreatureType();
            }
        }
        return this.creatureType;
    }

    public void setEnumCreatureType(EnumCreatureType type) {
        this.creatureType = type;
    }
}
