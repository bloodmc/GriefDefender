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
package com.griefdefender.event;

import com.flowpowered.math.vector.Vector3i;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.api.event.ChangeClaimEvent;
import com.griefdefender.internal.util.VecHelper;
import org.bukkit.Location;

public class GDChangeClaimEvent extends GDClaimEvent implements ChangeClaimEvent {

    public GDChangeClaimEvent(Claim claim) {
        super(claim);
    }

    public static class Type extends GDChangeClaimEvent implements ChangeClaimEvent.Type {
        private final ClaimType originalType;
        private final ClaimType newType;
    
        public Type(Claim claim, ClaimType newType) {
            super(claim);
            this.originalType = claim.getType();
            this.newType = newType;
        }
    
        @Override
        public ClaimType getOriginalType() {
            return originalType;
        }
    
        @Override
        public ClaimType getType() {
            return newType;
        }
    }

    public static class Resize extends GDChangeClaimEvent implements ChangeClaimEvent.Resize {
        private Vector3i startCorner;
        private Vector3i endCorner;

        public Resize(Claim claim, Location startCorner, Location endCorner) {
            super(claim);
            this.startCorner = VecHelper.toVector3i(startCorner);
            this.endCorner = VecHelper.toVector3i(endCorner);
        }

        @Override
        public Vector3i getStartCorner() {
            return this.startCorner;
        }

        @Override
        public Vector3i getEndCorner() {
            return this.endCorner;
        }
    }
}
