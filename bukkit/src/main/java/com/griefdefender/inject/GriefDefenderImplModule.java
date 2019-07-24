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
package com.griefdefender.inject;

import com.google.inject.PrivateModule;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.griefdefender.GDCore;
import com.griefdefender.GDEventManager;
import com.griefdefender.GDVersion;
import com.griefdefender.api.Core;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.Registry;
import com.griefdefender.api.Version;
import com.griefdefender.api.event.EventManager;
import com.griefdefender.api.permission.PermissionManager;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.registry.GDRegistry;

import javax.annotation.OverridingMethodsMustInvokeSuper;

public class GriefDefenderImplModule extends PrivateModule {

    @Override
    @OverridingMethodsMustInvokeSuper
    protected void configure() {
        this.bindAndExpose(Core.class).to(GDCore.class);
        this.bindAndExpose(EventManager.class).to(GDEventManager.class);
        this.bindAndExpose(PermissionManager.class).to(GDPermissionManager.class);
        this.bindAndExpose(Registry.class).to(GDRegistry.class);
        this.bindAndExpose(Version.class).to(GDVersion.class);

        this.requestStaticInjection(GriefDefender.class);
    }

    protected <T> AnnotatedBindingBuilder<T> bindAndExpose(final Class<T> type) {
        this.expose(type);
        return this.bind(type);
    }

}
