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
package com.griefdefender.cache;

import java.util.UUID;

import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.TrustType;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.api.permission.flag.Flag;
import com.griefdefender.claim.GDClaim;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Sponge;

public class EventResultCache {

    public final String eventFlag;
    public final Tristate lastResult;
    public final int lastTickCounter;
    public final UUID lastClaim;
    public final String lastTrust;

    public EventResultCache(Claim claim, String flag, Tristate result) {
        this(claim, flag, result, null);
    }

    public EventResultCache(Claim claim, String flag, Tristate result, String trust) {
        this.eventFlag = flag;
        this.lastClaim = claim.getUniqueId();
        this.lastResult = result;
        this.lastTickCounter = Sponge.getServer().getRunningTimeTicks();
        this.lastTrust = trust == null ? "cache" : trust;
    }

    public Tristate checkEventResultCache(GDClaim claim) {
        return this.checkEventResultCache(claim, null);
    }

    public Tristate checkEventResultCache(GDClaim claim, String flag) {
        if (Sponge.getServer().getRunningTimeTicks() > this.lastTickCounter) {
            return Tristate.UNDEFINED;
        }

        if (claim.getUniqueId().equals(this.lastClaim)) {
            if (flag != null && this.eventFlag != null && !this.eventFlag.equalsIgnoreCase(flag)) {
                return Tristate.UNDEFINED;
            }
            return this.lastResult;
        }

        return Tristate.UNDEFINED;
    }
}
