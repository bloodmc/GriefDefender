/*
 * This file is part of GriefDefender, licensed under the MIT License (MIT).
 *
 * Copyright (c) bloodmc
 * Copyright (c) zml
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
package com.griefdefender.provider.permissionsex;

import ca.stellardrift.permissionsex.context.ContextDefinition;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import com.google.common.collect.ImmutableSet;
import com.griefdefender.api.CatalogType;
import com.griefdefender.api.registry.CatalogRegistryModule;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class CatalogTypeContextDefinition<T extends CatalogType> extends ContextDefinition<T> {
    private final BiConsumer<CalculatedSubject, Consumer<? super T>> currentValueAccumulator;

    private final CatalogRegistryModule<T> registry;

    public CatalogTypeContextDefinition(String key, CatalogRegistryModule<T> registry) {
       this(key, registry, null);
    }

    public CatalogTypeContextDefinition(String key, CatalogRegistryModule<T> registry, BiConsumer<CalculatedSubject, Consumer<? super T>> currentValueAccumulator) {
        super(key);
        this.registry = registry;
        this.currentValueAccumulator = currentValueAccumulator == null ? (x, y) -> {} : currentValueAccumulator;
    }

    @Override
    public void accumulateCurrentValues(CalculatedSubject calculatedSubject, Consumer<T> consumer) {
        currentValueAccumulator.accept(calculatedSubject, consumer);
    }

    @Override
    public T deserialize(@NotNull String s) {
        return registry.getById(s).orElseThrow(() -> new IllegalArgumentException("Provided value '" + s + "' was not a valid value in catalog type " + registry.getClass().getSimpleName()));
    }

    @Override
    public boolean matches(T t, T t2) {
        return t.getId().equalsIgnoreCase(t2.getId());
    }

    @NotNull
    @Override
    public String serialize(T t) {
        return t.getId();
    }

    @NotNull
    @Override
    public Set<T> suggestValues(@NotNull CalculatedSubject subject) {
        return ImmutableSet.copyOf(registry.getAll());
    }
}
