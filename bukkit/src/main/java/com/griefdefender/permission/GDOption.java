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
package com.griefdefender.permission;

import com.griefdefender.api.permission.option.Option;
import net.kyori.text.Component;

import java.util.Arrays;
import java.util.List;

public class GDOption implements Option {

    private static final List<String> GLOBAL_OPTIONS = Arrays.asList(
            "blocks-accrued-per-hour", "chest-expiration",
            "create-mode", "economy-block-cost",
            "economy-block-sell", "initial-blocks",
            "max-accrued-blocks", "max-level", "min-level",
            "radius-list", "radius-inspect");

    private final String id;
    private final String name;
    private Component description;
    private boolean isGlobal;

    public GDOption(String id, String name) {
        this.id = id;
        this.name = name;
        this.isGlobal = GLOBAL_OPTIONS.contains(name);
    }

    @Override
    public String getId() {
        return this.id;
    }

    public String getPermission() {
        return "griefdefender." + this.name.toLowerCase();
    }

    public String getName() {
        return this.name;
    }

    public boolean isGlobal() {
        return this.isGlobal;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public Component getDescription() {
        return this.description;
    }
}
