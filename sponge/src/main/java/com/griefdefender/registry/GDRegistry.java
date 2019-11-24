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

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.inject.Singleton;
import com.griefdefender.api.CatalogType;
import com.griefdefender.api.Registry;
import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.api.claim.ShovelType;
import com.griefdefender.api.claim.TrustType;
import com.griefdefender.api.permission.ResultType;
import com.griefdefender.api.permission.flag.Flag;
import com.griefdefender.api.permission.option.Option;
import com.griefdefender.api.registry.CatalogRegistryModule;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Singleton
public class GDRegistry implements Registry {

    protected final static Map<Class<? extends CatalogType>, CatalogRegistryModule<?>> catalogRegistryMap = new IdentityHashMap<>();
    private static final Map<Class<?>, Supplier<?>> BUILDER_SUPPLIERS = new IdentityHashMap<>();

    static {
        catalogRegistryMap.put(ClaimType.class, (CatalogRegistryModule<ClaimType>) ClaimTypeRegistryModule.getInstance());
        catalogRegistryMap.put(Flag.class, (CatalogRegistryModule<Flag>) FlagRegistryModule.getInstance());
        catalogRegistryMap.put(ResultType.class, (CatalogRegistryModule<ResultType>) ResultTypeRegistryModule.getInstance());
        catalogRegistryMap.put(Option.class, (CatalogRegistryModule<Option>) OptionRegistryModule.getInstance());
        catalogRegistryMap.put(ShovelType.class, (CatalogRegistryModule<ShovelType>)ShovelTypeRegistryModule.getInstance());
        catalogRegistryMap.put(TrustType.class, (CatalogRegistryModule<TrustType>)TrustTypeRegistryModule.getInstance());
    }

    @Override
    public <T> Registry registerBuilderSupplier(Class<T> builderClass, Supplier<? extends T> supplier) {
        checkArgument(!BUILDER_SUPPLIERS.containsKey(builderClass), "Already registered a builder supplier!");
        BUILDER_SUPPLIERS.put(builderClass, supplier);
        return this;
    }

    @Override
    public <T> T createBuilder(Class<T> builderClass) throws IllegalArgumentException {
        final Supplier<?> supplier = BUILDER_SUPPLIERS.get(builderClass);
        checkArgument(supplier != null, "Could not find a Supplier for the provided builder class: " + builderClass.getCanonicalName());
        return (T) supplier.get();
    }

    @Override
    public <T extends CatalogType> Optional<T> getType(Class<T> typeClass, String id) {
        CatalogRegistryModule<T> registryModule = getRegistryModuleFor(typeClass).orElse(null);
        if (registryModule == null) {
            return Optional.empty();
        }
        return registryModule.getById(id.toLowerCase(Locale.ENGLISH));
    }

    @Override
    public <T extends CatalogType> Collection<T> getAllOf(Class<T> typeClass) {
        CatalogRegistryModule<T> registryModule = getRegistryModuleFor(typeClass).orElse(null);
        if (registryModule == null) {
            return Collections.emptyList();
        }
        return registryModule.getAll();
    }

    @Override
    public <T extends CatalogType> Collection<T> getAllFor(String pluginId, Class<T> typeClass) {
        final CatalogRegistryModule<T> registryModule = getRegistryModuleFor(typeClass).orElse(null);
        if (registryModule == null) {
            return Collections.emptyList();
        }
        ImmutableList.Builder<T> builder = ImmutableList.builder();
        registryModule.getAll()
                .stream()
                .filter(type -> pluginId.equals(type.getId().split(":")[0]))
                .forEach(builder::add);

        return builder.build();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends CatalogType> Optional<CatalogRegistryModule<T>> getRegistryModuleFor(Class<T> catalogClass) {
        return Optional.ofNullable((CatalogRegistryModule<T>) catalogRegistryMap.get(catalogClass));
    }
}
