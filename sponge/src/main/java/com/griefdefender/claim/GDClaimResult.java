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

import com.google.common.collect.ImmutableList;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimResult;
import com.griefdefender.api.claim.ClaimResultType;
import net.kyori.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GDClaimResult implements ClaimResult {

    private final Component eventMessage;
    private final List<Claim> claims;
    private final ClaimResultType resultType;

    public GDClaimResult(ClaimResultType type) {
        this(type, null);
    }

    public GDClaimResult(ClaimResultType type, Component message) {
        this.claims = ImmutableList.of();
        this.resultType = type;
        this.eventMessage = message;
    }

    public GDClaimResult(Claim claim, ClaimResultType type) {
        this(claim, type, null);
    }

    public GDClaimResult(Claim claim, ClaimResultType type, Component message) {
        List<Claim> claimList = new ArrayList<>();
        claimList.add(claim);
        this.claims = ImmutableList.copyOf(claimList);
        this.resultType = type;
        this.eventMessage = message;
    }

    public GDClaimResult(List<Claim> claims, ClaimResultType type) {
        this(claims, type, null);
    }

    public GDClaimResult(List<Claim> claims, ClaimResultType type, Component message) {
        this.claims = ImmutableList.copyOf(claims);
        this.resultType = type;
        this.eventMessage = message;
    }

    @Override
    public ClaimResultType getResultType() {
        return this.resultType;
    }

    @Override
    public Optional<Component> getMessage() {
        return Optional.ofNullable(this.eventMessage);
    }

    @Override
    public List<Claim> getClaims() {
        return this.claims;
    }
}
