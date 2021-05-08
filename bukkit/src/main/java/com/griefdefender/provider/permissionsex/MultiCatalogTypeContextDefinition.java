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
import com.griefdefender.api.CatalogType;
import com.griefdefender.api.registry.CatalogRegistryModule;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MultiCatalogTypeContextDefinition extends ContextDefinition<CatalogType> {
    private final CatalogRegistryModule<?>[] registries;

    public MultiCatalogTypeContextDefinition(String key, CatalogRegistryModule<?>... registries) {
       super(key);
       this.registries = registries;
    }


    @Override
    public void accumulateCurrentValues(CalculatedSubject calculatedSubject, Consumer<CatalogType> consumer) {
    }

    @Override
    public CatalogType deserialize(@NotNull String s) {
        for (CatalogRegistryModule<?> reg : registries) {
            Optional<? extends CatalogType> possibility = reg.getById(s);
            if (possibility.isPresent()) {
                return possibility.get();
            }
        }
        throw new IllegalArgumentException("Provided value '" + s + "' was not a valid value in any of catalog types");
    }

    @Override
    public boolean matches(CatalogType t, CatalogType t2) {
        return t.getId().equalsIgnoreCase(t2.getId());
    }

    @NotNull
    @Override
    public String serialize(CatalogType t) {
        return t.getId();
    }

    @NotNull
    @Override
    public Set<CatalogType> suggestValues(@NotNull CalculatedSubject subject) {
        return Arrays.stream(registries)
                .flatMap(reg -> reg.getAll().stream())
                .collect(Collectors.toSet());
    }
}
