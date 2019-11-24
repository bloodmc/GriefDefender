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

import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.TrustType;
import com.griefdefender.api.event.GroupTrustClaimEvent;

import java.util.List;

public class GDGroupTrustClaimEvent extends GDTrustClaimEvent implements GroupTrustClaimEvent {

    private List<String> groups;

    public GDGroupTrustClaimEvent(Claim claim, List<String> groups, TrustType trustType) {
        super(claim, trustType);
        this.groups = groups;
    }

    public GDGroupTrustClaimEvent(List<Claim> claims, List<String> groups, TrustType trustType) {
        super(claims, trustType);
        this.groups = groups;
    }

    @Override
    public List<String> getGroups() {
        return this.groups;
    }

    public static class Add extends GDGroupTrustClaimEvent implements GroupTrustClaimEvent.Add {
        public Add(List<Claim> claims, List<String> groups, TrustType trustType) {
            super(claims, groups, trustType);
        }

        public Add(Claim claim, List<String> groups, TrustType trustType) {
            super(claim, groups, trustType);
        }
    }

    public static class Remove extends GDGroupTrustClaimEvent implements GroupTrustClaimEvent.Remove {
        public Remove(List<Claim> claims, List<String> groups, TrustType trustType) {
            super(claims, groups, trustType);
        }

        public Remove(Claim claim, List<String> groups, TrustType trustType) {
            super(claim, groups, trustType);
        }
    }
}
