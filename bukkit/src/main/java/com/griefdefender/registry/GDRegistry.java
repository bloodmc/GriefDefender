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

import com.google.inject.Singleton;
import com.griefdefender.api.CatalogType;
import com.griefdefender.api.Registry;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimSchematic;
import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.api.claim.ShovelType;
import com.griefdefender.api.claim.TrustType;
import com.griefdefender.api.economy.BankTransaction;
import com.griefdefender.api.permission.ResultType;
import com.griefdefender.api.permission.flag.Flag;
import com.griefdefender.api.permission.option.Option;
import com.griefdefender.api.registry.CatalogRegistryModule;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.economy.GDBankTransaction;
import com.griefdefender.internal.schematic.GDClaimSchematic;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

@Singleton
public class GDRegistry implements Registry {

    protected final static Map<Class<? extends CatalogType>, CatalogRegistryModule<?>> catalogRegistryMap = new IdentityHashMap<>();

    static {
        catalogRegistryMap.put(ClaimType.class, (CatalogRegistryModule<ClaimType>) ClaimTypeRegistryModule.getInstance());
        catalogRegistryMap.put(Flag.class, (CatalogRegistryModule<Flag>) FlagRegistryModule.getInstance());
        catalogRegistryMap.put(Option.class, (CatalogRegistryModule<Option>) OptionRegistryModule.getInstance());
        catalogRegistryMap.put(ResultType.class, (CatalogRegistryModule<ResultType>) ResultTypeRegistryModule.getInstance());
        // TODO
        //catalogRegistryMap.put(ShovelType.class, (CatalogRegistryModule<ShovelType>)ShovelTypeRegistryModule.getInstance());
        //catalogRegistryMap.put(TrustType.class, (CatalogRegistryModule<TrustType>)TrustTypeRegistryModule.getInstance());
    }

    @Override
    public Claim.Builder createClaimBuilder() {
        return new GDClaim.ClaimBuilder();
    }

    @Override
    public ClaimSchematic.Builder createClaimSchematicBuilder() {
        return new GDClaimSchematic.ClaimSchematicBuilder();
    }

    @Override
    public BankTransaction.Builder createBankTransactionBuilder() {
        return new GDBankTransaction.BankTransactionBuilder();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends CatalogType> Optional<CatalogRegistryModule<T>> getRegistryModuleFor(Class<T> catalogClass) {
        return Optional.ofNullable((CatalogRegistryModule<T>) catalogRegistryMap.get(catalogClass));
    }
}
