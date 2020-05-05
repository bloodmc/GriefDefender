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
package com.griefdefender.permission.flag;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashSet;
import java.util.Set;

import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.flag.Flag;
import com.griefdefender.api.permission.flag.FlagData;

public class GDFlagData implements FlagData {

    private Flag flag;
    private Set<Context> contexts;

    public GDFlagData(Flag flag, Set<Context> contexts) {
        this.flag = flag;
        this.contexts = contexts;
    }

    @Override
    public Set<Context> getContexts() {
        return this.contexts;
    }

    @Override
    public Flag getFlag() {
        return this.flag;
    }

    @Override
    public boolean matches(Flag otherFlag, Set<Context> otherContexts) {
        for (Context context : this.contexts) {
            boolean found = false;
            for (Context other : otherContexts) {
                if (other.getKey().equals(context.getKey())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    public static class FlagDataBuilder implements Builder {

        private Flag flag;
        private Set<Context> contexts = new HashSet<>();

        @Override
        public Builder flag(Flag flag) {
            this.flag = flag;
            return this;
        }

        @Override
        public Builder context(Context context) {
            this.contexts.add(context);
            return this;
        }

        @Override
        public Builder contexts(Set<Context> contexts) {
            this.contexts = contexts;
            return this;
        }

        @Override
        public Builder reset() {
            this.flag = null;
            this.contexts = new HashSet<>();
            return this;
        }

        @Override
        public FlagData build() {
            checkNotNull(this.flag);
            return new GDFlagData(this.flag, this.contexts);
        }
        
    }
}
