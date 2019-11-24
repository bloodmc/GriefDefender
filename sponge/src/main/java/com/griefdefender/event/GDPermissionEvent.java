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
package com.griefdefender.event;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.griefdefender.api.Subject;
import com.griefdefender.api.event.EventCause;
import com.griefdefender.api.event.PermissionEvent;
import com.griefdefender.api.permission.Context;

import net.kyori.text.Component;

public class GDPermissionEvent implements PermissionEvent {

    private final Subject subject;
    private Set<Context> contexts;
    private Component message;
    private boolean isCancelled = false;

    public GDPermissionEvent(Subject subject) {
        this.subject = subject;
    }

    public GDPermissionEvent(Subject subject, Set<Context> contexts) {
        this.subject = subject;
        this.contexts = contexts;
    }

    @Override
    public Subject getSubject() {
        return this.subject;
    }

    @Override
    public Set<Context> getContexts() {
        if (this.contexts == null) {
            this.contexts = new HashSet<>();
        }
        return this.contexts;
    }

    @Override
    public void setMessage(Component message) {
        this.message = message;
    }

    @Override
    public Optional<Component> getMessage() {
        return Optional.ofNullable(this.message);
    }

    @Override
    public boolean cancelled() {
        return this.isCancelled;
    }

    @Override
    public void cancelled(boolean cancelled) {
        this.isCancelled = cancelled;
    }

    @Override
    public EventCause getCause() {
        return GDCauseStackManager.getInstance().getCurrentCause();
    }
}
