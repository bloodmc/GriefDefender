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
package com.griefdefender.claim;

import com.flowpowered.math.vector.Vector3i;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimSchematic;
import com.griefdefender.internal.util.VecHelper;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.persistence.DataFormats;
import org.spongepowered.api.data.persistence.DataTranslators;
import org.spongepowered.api.world.BlockChangeFlags;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.extent.ArchetypeVolume;
import org.spongepowered.api.world.schematic.Schematic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.zip.GZIPOutputStream;

public class GDClaimSchematic implements ClaimSchematic {

    private Claim claim;
    private Schematic schematic;
    private String name;
    private Vector3i origin;
    private final Instant dateCreated;

    // Used during server startup to load schematic
    public GDClaimSchematic(Claim claim, Schematic schematic, String fileName) {
        this.claim = claim;
        this.schematic = schematic;
        this.name = schematic.getMetadata().getString(DataQuery.of(".", Schematic.METADATA_NAME)).orElse(fileName);
        this.origin = claim.getLesserBoundaryCorner();
        this.dateCreated = Instant.now();
    }

    private GDClaimSchematic(Claim claim, Schematic schematic, String name, Vector3i origin) {
        this.claim = claim;
        this.schematic = schematic;
        this.name = name;
        this.origin = origin;
        this.dateCreated = Instant.now();
    }

    @Override
    public Claim getClaim() {
        return this.claim;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Instant getDateCreated() {
        return this.dateCreated;
    }

    @Override
    public Vector3i getOrigin() {
        return this.origin;
    }

    @Override
    public void setOrigin(Vector3i pos) {
        this.origin = pos;
    }

    public Schematic getSchematic() {
        return this.schematic;
    }

    /**
     * Applies schematic to claim.
     * 
     * @return If schematic apply was successful, false if not
     */
    public boolean apply() {
        if (!this.schematic.containsBlock(this.claim.getLesserBoundaryCorner()) && !this.schematic.containsBlock(this.claim.getGreaterBoundaryCorner())) {
            return false;
        }

        this.schematic.apply(VecHelper.toLocation(((GDClaim)(this.claim)).getWorld(), this.origin), BlockChangeFlags.ALL);
        return true;
    }

    public static class ClaimSchematicBuilder implements Builder {

        private Claim claim;
        private Schematic schematic;
        private String name;
        private Vector3i origin;

        @Override
        public Builder claim(Claim claim) {
            this.claim = claim;
            return this;
        }

        @Override
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public Builder origin(Vector3i origin) {
            this.origin = origin;
            return this;
        }

        @Override
        public Builder reset() {
            this.name = null;
            this.origin = null;
            this.schematic = null;
            return this;
        }

        @Override
        public Optional<ClaimSchematic> build() {
            if (this.origin == null) {
                this.origin = new Vector3i(0, 0, 0);
            }

            final World world = ((GDClaim) this.claim).getWorld();
            final ArchetypeVolume volume = world.createArchetypeVolume(this.claim.getLesserBoundaryCorner(), this.claim.getGreaterBoundaryCorner(), this.origin);
            final Schematic schematic = Schematic.builder()
                    .metaValue(Schematic.METADATA_NAME, this.name)
                    .metaValue(Schematic.METADATA_DATE, Instant.now().toString())
                    .metaValue("UUID", this.claim.getUniqueId().toString())
                    .volume(volume)
                    .build();
            this.schematic = schematic;
            DataContainer schematicData = DataTranslators.SCHEMATIC.translate(schematic);
            final Path schematicPath = GriefDefenderPlugin.getInstance().getWorldEditProvider().getSchematicWorldMap().get(world.getUniqueId()).resolve(this.claim.getUniqueId().toString());
            try {
                Files.createDirectories(schematicPath);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            File outputFile = schematicPath.resolve(this.name + ".schematic").toFile();
            try {
                DataFormats.NBT.writeTo(new GZIPOutputStream(new FileOutputStream(outputFile)), schematicData);
            } catch (Exception e) {
                e.printStackTrace();
                return Optional.empty();
            }

            final GDClaimSchematic claimSchematic = new GDClaimSchematic(this.claim, this.schematic, this.name, this.origin);
            ((GDClaim) this.claim).schematics.put(this.name, claimSchematic);
            return Optional.of(claimSchematic);
        }
    }
}
