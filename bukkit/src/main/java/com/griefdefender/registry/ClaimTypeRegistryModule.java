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

import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.api.claim.ClaimTypes;
import com.griefdefender.api.registry.CatalogRegistryModule;
import com.griefdefender.claim.GDClaimType;
import com.griefdefender.util.RegistryHelper;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class ClaimTypeRegistryModule implements CatalogRegistryModule<ClaimType> {

    private static ClaimTypeRegistryModule instance;

    public static ClaimTypeRegistryModule getInstance() {
        return instance;
    }

    private final Map<String, ClaimType> claimTypeMap = new HashMap<>();

    @Override
    public Optional<ClaimType> getById(String id) {
        return Optional.ofNullable(this.claimTypeMap.get(checkNotNull(id)));
    }

    @Override
    public Collection<ClaimType> getAll() {
        return this.claimTypeMap.values();
    }

    @Override
    public void registerDefaults() {
        RegistryHelper.mapFields(ClaimTypes.class, input -> {
            final ClaimType type = new GDClaimType("griefdefender:" + input.toLowerCase(), input.toLowerCase());
            this.claimTypeMap.put(input.toLowerCase(Locale.ENGLISH), type);
            return type;
        });
    }

    @Override
    public void registerCustomType(ClaimType type) {
        this.claimTypeMap.put(type.getName().toLowerCase(Locale.ENGLISH), type);
    }

    static {
        instance = new ClaimTypeRegistryModule();
    }
}
