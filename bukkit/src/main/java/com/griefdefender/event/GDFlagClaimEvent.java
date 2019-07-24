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
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.event.FlagClaimEvent;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.flag.Flag;
import com.griefdefender.permission.GDPermissionHolder;

public class GDFlagClaimEvent extends GDClaimEvent implements FlagClaimEvent {

    private final GDPermissionHolder subject;

    public GDFlagClaimEvent(Claim claim, GDPermissionHolder subject) {
        super(claim);
        this.subject = subject;
    }

    public static class ClearAll extends GDFlagClaimEvent implements FlagClaimEvent.ClearAll {

        public ClearAll(Claim claim, GDPermissionHolder subject) {
            super(claim, subject);
        }
    }

    public static class Clear extends GDFlagClaimEvent implements FlagClaimEvent.Clear {

        private final java.util.Set<Context> contexts;

        public Clear(Claim claim, GDPermissionHolder subject, java.util.Set<Context> contexts) {
            super(claim, subject);
            this.contexts = ImmutableSet.copyOf(contexts);
        }

        @Override
        public java.util.Set<Context> getContexts() {
            return this.contexts;
        }
    }

    public static class Set extends GDFlagClaimEvent implements FlagClaimEvent.Set {

        private final Flag flag;
        private final Tristate value;
        private final String target;
        private final java.util.Set<Context> contexts;

        public Set(Claim claim, GDPermissionHolder subject, Flag flag, String target, Tristate value, java.util.Set<Context> contexts) {
            super(claim, subject);
            this.flag = flag;
            this.target = target;
            this.value = value;
            this.contexts = contexts;
        }

        @Override
        public Flag getFlag() {
            return this.flag;
        }

        @Override
        public String getTarget() {
            return this.target;
        }

        @Override
        public Tristate getValue() {
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
}
