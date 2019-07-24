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

import com.google.common.collect.ImmutableSet;
import com.griefdefender.api.event.EventCause;
import com.griefdefender.api.event.OptionEvent;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.option.Option;
import com.griefdefender.permission.GDPermissionHolder;

public class GDOptionEvent implements OptionEvent {

    private final GDPermissionHolder subject;
    private boolean isCancelled = false;

    public GDOptionEvent(GDPermissionHolder subject) {
        this.subject = subject;
    }

    public static class ClearAll extends GDOptionEvent implements OptionEvent.ClearAll {

        public ClearAll(GDPermissionHolder subject) {
            super(subject);
        }
    }

    public static class Clear extends GDOptionEvent implements OptionEvent.Clear {

        private final java.util.Set<Context> contexts;

        public Clear(GDPermissionHolder subject, java.util.Set<Context> contexts) {
            super(subject);
            this.contexts = ImmutableSet.copyOf(contexts);
        }

        @Override
        public java.util.Set<Context> getContexts() {
            return this.contexts;
        }
    }

    public static class Set extends GDOptionEvent implements OptionEvent.Set {

        private final Option option;
        private final String value;
        private final java.util.Set<Context> contexts;

        public Set(GDPermissionHolder subject, Option option, String value, java.util.Set<Context> contexts) {
            super(subject);
            this.option = option;
            this.value = value;
            this.contexts = contexts;
        }

        @Override
        public Option getOption() {
            return this.option;
        }

        @Override
        public String getValue() {
            return this.value;
        }

        @Override
        public java.util.Set<Context> getContexts() {
            return this.contexts;
        }
    }

    @Override
    public String getSubjectId() {
        return this.subject.getIdentifier();
    }

    @Override
    public boolean cancelled() {
        return this.isCancelled;
    }

    @Override
    public void cancelled(boolean cancel) {
        this.isCancelled = cancel;
    }

    @Override
    public EventCause getCause() {
        return GDCauseStackManager.getInstance().getCurrentCause();
    }
}
