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
package com.griefdefender.permission.ui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.griefdefender.api.claim.ClaimContexts;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.flag.Flag;
import com.griefdefender.api.permission.flag.Flags;
import com.griefdefender.permission.GDPermissionHolder;

import net.kyori.text.format.TextColor;

public class UIFlagData {

    public GDPermissionHolder holder;
    public Flag flag;
    public Map<Integer, FlagContextHolder> flagContextMap = new HashMap<>();

    public UIFlagData(GDPermissionHolder holder, Flag flag, Boolean value, MenuType type, Set<Context> contexts) {
        this.holder = holder;
        this.flag = flag;
        this.addContexts(flag, value, type, contexts);
    }

    public boolean addContexts(Flag flag, Boolean value, MenuType type, Set<Context> contexts) {
        final Set<Context> filteredContexts = UIHelper.getFilteredContexts(contexts);
        final int hashCode = Objects.hash(filteredContexts);
        final FlagContextHolder flagHolder = this.flagContextMap.get(hashCode);
        if (flagHolder != null) {
            if (flagHolder.getType() == MenuType.CLAIM && type == MenuType.DEFAULT) {
                // ignore
                return false;
            }

            final boolean hasGlobalDefault = contexts.contains(ClaimContexts.GLOBAL_DEFAULT_CONTEXT);
            final boolean hasGlobalOverride = contexts.contains(ClaimContexts.GLOBAL_OVERRIDE_CONTEXT);
            final boolean hasUserDefault = contexts.contains(ClaimContexts.USER_DEFAULT_CONTEXT);
            final boolean hasUserOverride = contexts.contains(ClaimContexts.USER_OVERRIDE_CONTEXT);

            // Context Default Types have higher priority than global
            if (hasGlobalDefault && hasUserDefault) {
                for (Context context : flagHolder.getAllContexts()) {
                    if (context.getKey().equalsIgnoreCase("gd_claim_default")) {
                        if (!context.getValue().equalsIgnoreCase("global") && !context.getValue().equalsIgnoreCase("user")) {
                            return false;
                        }
                    }
                }
            }
            if (hasGlobalDefault && !hasUserDefault) {
                for (Context context : flagHolder.getAllContexts()) {
                    if (context.getKey().equalsIgnoreCase("gd_claim_default")) {
                        if (!context.getValue().equalsIgnoreCase("global")) {
                            return false;
                        }
                    }
                }
            }

            // Context Override Types have higher priority than global
            if (hasGlobalOverride && hasUserOverride) {
                for (Context context : flagHolder.getAllContexts()) {
                    if (context.getKey().equalsIgnoreCase("gd_claim_override")) {
                        if (!context.getValue().equalsIgnoreCase("global") && !context.getValue().equalsIgnoreCase("user")) {
                            return false;
                        }
                    }
                }
            }
            if (hasGlobalOverride && !hasUserOverride) {
                for (Context context : flagHolder.getAllContexts()) {
                    if (context.getKey().equalsIgnoreCase("gd_claim_override")) {
                        if (!context.getValue().equalsIgnoreCase("global")) {
                            return false;
                        }
                    }
                }
            }
        }
        this.flagContextMap.put(Objects.hash(filteredContexts), new FlagContextHolder(flag, value, type, contexts));
        return true;
    }

    public void setHolder(GDPermissionHolder holder) {
        this.holder = holder;
    }

    public class FlagContextHolder {
        private Set<Context> contexts;
        private Flag flag;
        private Boolean value;
        private MenuType type;
        private TextColor color;
        private Set<Context> removedContexts = new HashSet<>();
        
        public FlagContextHolder(Flag flag, Boolean value, MenuType type, Set<Context> contexts) {
            this.flag = flag;
            this.value = value;
            this.contexts = this.getFilteredContexts(contexts);
            this.type = type;
            this.color = UIHelper.getPermissionMenuTypeColor(type);
        }

        public Flag getFlag() {
            return this.flag;
        }

        public Boolean getValue() {
            return this.value;
        }

        public MenuType getType() {
            return this.type;
        }

        public TextColor getColor() {
            return this.color;
        }

        public Set<Context> getRemovedContexts() {
            return this.removedContexts;
        }

        public Set<Context> getContexts() {
            return this.contexts;
        }

        public Set<Context> getAllContexts() {
            Set<Context> allContexts = new HashSet<>();
            allContexts.addAll(this.removedContexts);
            allContexts.addAll(this.contexts);
            return allContexts;
        }

        private Set<Context> getFilteredContexts(Set<Context> contexts) {
            Set<Context> filteredContexts = new HashSet<>(contexts);
            for (Context context : contexts) {
                if (context.getKey().contains("gd_claim") || context.getKey().equals("server")) {
                    this.removedContexts.add(context);
                    filteredContexts.remove(context);
                }
            }

            return filteredContexts;
        }
    }
}
