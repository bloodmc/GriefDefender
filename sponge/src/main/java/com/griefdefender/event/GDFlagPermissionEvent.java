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

import com.griefdefender.api.Subject;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.event.FlagPermissionEvent;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.flag.Flag;

public class GDFlagPermissionEvent extends GDPermissionEvent implements FlagPermissionEvent {

    public GDFlagPermissionEvent(Subject subject) {
        super(subject);
    }

    public GDFlagPermissionEvent(Subject subject, java.util.Set<Context> contexts) {
        super(subject, contexts);
    }

    public static class ClearAll extends GDFlagPermissionEvent implements FlagPermissionEvent.ClearAll {

        public ClearAll(Subject subject) {
            super(subject);
        }

        public ClearAll(Subject subject, java.util.Set<Context> contexts) {
            super(subject, contexts);
        }
    }

    public static class Clear extends GDFlagPermissionEvent implements FlagPermissionEvent.Clear {

        public Clear(Subject subject, java.util.Set<Context> contexts) {
            super(subject, contexts);
        }
    }

    public static class Set extends GDFlagPermissionEvent implements FlagPermissionEvent.Set {

        private final Flag flag;
        private final Tristate value;

        public Set(Subject subject, Flag flag, Tristate value, java.util.Set<Context> contexts) {
            super(subject, contexts);
            this.flag = flag;
            this.value = value;
        }

        @Override
        public Flag getFlag() {
            return this.flag;
        }

        @Override
        public Tristate getValue() {
            return this.value;
        }
    }
}
