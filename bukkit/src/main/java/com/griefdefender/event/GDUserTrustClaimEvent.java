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
import com.griefdefender.api.event.UserTrustClaimEvent;

import java.util.List;
import java.util.UUID;

public class GDUserTrustClaimEvent extends GDTrustClaimEvent implements UserTrustClaimEvent {

    private List<UUID> users;

    public GDUserTrustClaimEvent(Claim claim, List<UUID> users, TrustType trustType) {
        super(claim, trustType);
        this.users = users;
    }

    public GDUserTrustClaimEvent(List<Claim> claims, List<UUID> users, TrustType trustType) {
        super(claims, trustType);
        this.users = users;
    }

    @Override
    public List<UUID> getUsers() {
        return this.users;
    }

    public static class Add extends GDUserTrustClaimEvent implements UserTrustClaimEvent.Add {
        public Add(List<Claim> claims, List<UUID> users, TrustType trustType) {
            super(claims, users, trustType);
        }

        public Add(Claim claim, List<UUID> users, TrustType trustType) {
            super(claim, users, trustType);
        }
    }

    public static class Remove extends GDUserTrustClaimEvent implements UserTrustClaimEvent.Remove {
        public Remove(List<Claim> claims,List<UUID> users, TrustType trustType) {
            super(claims, users, trustType);
        }

        public Remove(Claim claim, List<UUID> users, TrustType trustType) {
            super(claim, users, trustType);
        }
    }
}
