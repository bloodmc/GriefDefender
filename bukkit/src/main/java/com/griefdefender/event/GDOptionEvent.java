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

import com.griefdefender.api.event.OptionPermissionEvent;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.option.Option;
import com.griefdefender.permission.GDPermissionHolder;

public class GDOptionEvent extends GDPermissionEvent implements OptionPermissionEvent {

    public GDOptionEvent(GDPermissionHolder subject, java.util.Set<Context> contexts) {
        super(subject, contexts);
    }

    public static class ClearAll extends GDOptionEvent implements OptionPermissionEvent.ClearAll {

        public ClearAll(GDPermissionHolder subject, java.util.Set<Context> contexts) {
            super(subject, contexts);
        }
    }

    public static class Clear extends GDOptionEvent implements OptionPermissionEvent.Clear {

        public Clear(GDPermissionHolder subject, java.util.Set<Context> contexts) {
            super(subject, contexts);
        }
    }

    public static class Set extends GDOptionEvent implements OptionPermissionEvent.Set {

        private final Option option;
        private final String value;

        public Set(GDPermissionHolder subject, Option option, String value, java.util.Set<Context> contexts) {
            super(subject, contexts);
            this.option = option;
            this.value = value;
        }

        @Override
        public Option getOption() {
            return this.option;
        }

        @Override
        public String getValue() {
            return this.value;
        }
    }
}
