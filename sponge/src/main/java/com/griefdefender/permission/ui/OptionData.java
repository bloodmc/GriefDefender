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

import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.option.Option;

import net.kyori.text.format.TextColor;

@SuppressWarnings("rawtypes")
public class OptionData {

    public Option option;
    public Map<Integer, OptionContextHolder> optionContextMap = new HashMap<>();

    public OptionData(Option option, String value, MenuType type, Set<Context> contexts) {
        this.option = option;
        this.addContexts(option, value, type, contexts);
    }

    public boolean addContexts(Option option, String value, MenuType type, Set<Context> contexts) {
        final Set<Context> filteredContexts = UIHelper.getFilteredContexts(contexts);
        final int hashCode = Objects.hash(filteredContexts);
        final OptionContextHolder flagHolder = this.optionContextMap.get(hashCode);
        if (flagHolder != null) {
            if (flagHolder.getType() == MenuType.CLAIM && type == MenuType.DEFAULT) {
                // ignore
                return false;
            }
        }
        this.optionContextMap.put(Objects.hash(filteredContexts), new OptionContextHolder(option, value, type, contexts));
        return true;
    }

    public class OptionContextHolder {
        private Set<Context> contexts;
        private Option option;
        private String value;
        private MenuType type;
        private TextColor color;
        private Set<Context> removedContexts = new HashSet<>();
        
        public OptionContextHolder(Option option, String value, MenuType type, Set<Context> contexts) {
            this.option = option;
            this.value = value;
            this.contexts = this.getFilteredContexts(contexts);
            this.type = type;
            this.color = UIHelper.getPermissionMenuTypeColor(type);
        }

        public Option getOption() {
            return this.option;
        }

        public String getValue() {
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
