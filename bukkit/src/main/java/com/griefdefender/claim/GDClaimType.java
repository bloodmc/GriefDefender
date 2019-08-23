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

import com.griefdefender.api.claim.ClaimContexts;
import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.api.permission.Context;

public class GDClaimType implements ClaimType {

    private final String id;
    private final String name;
    private final Context context;
    private final Context defaultContext;
    private final Context overrideContext;

    public GDClaimType(String id, String name) {
        this.id = id;
        this.name = name;
        this.context = new Context("gd_claim_type", name.toLowerCase());
        if (name.equalsIgnoreCase("any")) {
            this.defaultContext = ClaimContexts.GLOBAL_DEFAULT_CONTEXT;
            this.overrideContext = ClaimContexts.GLOBAL_OVERRIDE_CONTEXT;
        } else if (name.equalsIgnoreCase("admin")) {
            this.defaultContext = ClaimContexts.ADMIN_DEFAULT_CONTEXT;
            this.overrideContext = ClaimContexts.ADMIN_OVERRIDE_CONTEXT;
        } else if (name.equalsIgnoreCase("basic")) {
            this.defaultContext = ClaimContexts.BASIC_DEFAULT_CONTEXT;
            this.overrideContext = ClaimContexts.BASIC_OVERRIDE_CONTEXT;
        } else if (name.equalsIgnoreCase("subdivision")) {
            this.defaultContext = ClaimContexts.SUBDIVISION_DEFAULT_CONTEXT;
            this.overrideContext = ClaimContexts.SUBDIVISION_OVERRIDE_CONTEXT;
        } else if (name.equalsIgnoreCase("town")) {
            this.defaultContext = ClaimContexts.TOWN_DEFAULT_CONTEXT;
            this.overrideContext = ClaimContexts.TOWN_OVERRIDE_CONTEXT;
        } else if (name.equalsIgnoreCase("wilderness")) {
            this.defaultContext = ClaimContexts.WILDERNESS_DEFAULT_CONTEXT;
            this.overrideContext = ClaimContexts.WILDERNESS_OVERRIDE_CONTEXT;
        } else {
            this.defaultContext = new Context("gd_claim_default", name.toLowerCase());
            this.overrideContext = new Context("gd_claim_override", name.toLowerCase());
        }
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public String toString() {
        return this.id;
    }

    @Override
    public Context getContext() {
        return this.context;
    }

    @Override
    public Context getDefaultContext() {
        return this.defaultContext;
    }

    @Override
    public Context getOverrideContext() {
        return this.overrideContext;
    }
}
