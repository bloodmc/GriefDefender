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
package com.griefdefender;

import com.google.inject.Singleton;
import com.griefdefender.api.event.Event;
import com.griefdefender.api.event.EventManager;
import net.kyori.event.EventBus;
import net.kyori.event.SimpleEventBus;
import net.kyori.event.method.MethodSubscriptionAdapter;
import net.kyori.event.method.SimpleMethodSubscriptionAdapter;
import net.kyori.event.method.asm.ASMEventExecutorFactory;

@Singleton
public class GDEventManager implements EventManager {

    private final EventBus<Event> bus;
    private final MethodSubscriptionAdapter<Object> adapter;

    public GDEventManager() {
        this.bus = new SimpleEventBus<Event>(Event.class);
        this.adapter = new SimpleMethodSubscriptionAdapter<>(bus, new ASMEventExecutorFactory<>());
    }

    @Override
    public EventBus<Event> getBus() {
        return this.bus;
    }

    @Override
    public void post(Event event) {
        this.bus.post(event);
    }

    @Override
    public void register(Object listener) {
        this.adapter.register(listener);
    }

    @Override
    public void unregister(Object listener) {
        this.adapter.unregister(listener);
    }

}
