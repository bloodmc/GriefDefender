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

import com.griefdefender.api.permission.ResultType;
import com.griefdefender.api.permission.ResultTypes;
import com.griefdefender.api.registry.CatalogRegistryModule;
import com.griefdefender.permission.GDResultType;
import com.griefdefender.util.RegistryHelper;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class ResultTypeRegistryModule implements CatalogRegistryModule<ResultType> {

    private static ResultTypeRegistryModule instance;

    public static ResultTypeRegistryModule getInstance() {
        return instance;
    }

    private final Map<String, ResultType> resultTypeMap = new HashMap<>();

    @Override
    public Optional<ResultType> getById(String id) {
        return Optional.ofNullable(this.resultTypeMap.get(checkNotNull(id)));
    }

    @Override
    public Collection<ResultType> getAll() {
        return this.resultTypeMap.values();
    }

    @Override
    public void registerDefaults() {
        RegistryHelper.mapFields(ResultTypes.class, input -> {
            final ResultType type = new GDResultType("griefdefender:" + input.toLowerCase(), input.toLowerCase());
            this.resultTypeMap.put(input.toLowerCase(Locale.ENGLISH), type);
            return type;
        });
    }

    @Override
    public void registerCustomType(ResultType type) {
        this.resultTypeMap.put(type.getName().toLowerCase(Locale.ENGLISH), type);
    }

    static {
        instance = new ResultTypeRegistryModule();
    }
}
