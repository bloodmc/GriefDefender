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
package com.griefdefender.permission.option;

import com.griefdefender.api.permission.option.Option;
import com.griefdefender.api.permission.option.Option.Builder;
import com.griefdefender.registry.OptionRegistryModule;

public final class OptionBuilder<T> implements Option.Builder<T> {

    Class<T> typeClass;
    String id;
    String name;

    @Override
    public Builder<T> type(Class<T> tClass) {
        this.typeClass = tClass;
        return this;
    }

    @Override
    public Builder<T> id(String id) {
        this.id = id;
        return this;
    }

    @Override
    public Builder<T> name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public Option<T> build() {
        final GDOption<T> key = new GDOption<>(this);
        OptionRegistryModule.getInstance().registerCustomType(key);
        return key;
    }

    @Override
    public Builder<T> reset() {
        this.typeClass = null;
        this.id = null;
        this.name = null;
        return this;
    }
}