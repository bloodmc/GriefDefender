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

import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.ContextKeys;

public class ContextGroups {

    // Entity groups
    public static final Context SOURCE_AMBIENT = new Context(ContextKeys.SOURCE, ContextGroupKeys.AMBIENT);
    public static final Context SOURCE_ANIMAL = new Context(ContextKeys.SOURCE, ContextGroupKeys.ANIMAL);
    public static final Context SOURCE_AQUATIC = new Context(ContextKeys.SOURCE, ContextGroupKeys.AQUATIC);
    public static final Context SOURCE_MISC = new Context(ContextKeys.SOURCE, ContextGroupKeys.MISC);
    public static final Context SOURCE_MONSTER = new Context(ContextKeys.SOURCE, ContextGroupKeys.MONSTER);
    public static final Context SOURCE_VEHICLE = new Context(ContextKeys.SOURCE, ContextGroupKeys.VEHICLE);
    public static final Context TARGET_AMBIENT = new Context(ContextKeys.TARGET, ContextGroupKeys.AMBIENT);
    public static final Context TARGET_ANIMAL = new Context(ContextKeys.TARGET, ContextGroupKeys.ANIMAL);
    public static final Context TARGET_AQUATIC = new Context(ContextKeys.TARGET, ContextGroupKeys.AQUATIC);
    public static final Context TARGET_MISC = new Context(ContextKeys.TARGET, ContextGroupKeys.MISC);
    public static final Context TARGET_MONSTER = new Context(ContextKeys.TARGET, ContextGroupKeys.MONSTER);
    public static final Context TARGET_VEHICLE = new Context(ContextKeys.TARGET, ContextGroupKeys.VEHICLE);

    // Item groups
    public static final Context SOURCE_FOOD = new Context(ContextKeys.SOURCE, ContextGroupKeys.FOOD);
    public static final Context TARGET_FOOD = new Context(ContextKeys.TARGET, ContextGroupKeys.FOOD);
}
