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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.PermissionResult;
import com.griefdefender.api.permission.flag.Flag;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.category.CustomFlagGroupCategory;

import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;

public class GDCustomFlagDefinition {

    private boolean enabled = true;
    private Set<Context> definitionContexts = new HashSet<>();
    private List<CustomFlagData> data = new ArrayList<>();
    private String displayName;
    private Tristate defaultValue = Tristate.UNDEFINED;
    private Component description;

    public GDCustomFlagDefinition(Flag flag, Set<Context> contexts, String displayName, Component description) {
        this.data.add(new CustomFlagData(flag, contexts));
        this.displayName = displayName;
        this.description = description;
    }

    public GDCustomFlagDefinition(List<CustomFlagData> flagData, String displayName, Component description) {
        this.data = flagData;
        this.displayName = displayName;
        this.description = description;
    }

    public void addFlagData(Flag flag, Set<Context> contexts) {
        this.data.add(new CustomFlagData(flag, contexts));
    }

    public List<Flag> getFlags() {
        List<Flag> flags = new ArrayList<>();
        for (CustomFlagData flagData : this.data) {
            flags.add(flagData.getFlag());
        }
        return flags;
    }

    public List<CustomFlagData> getFlagData() {
        return this.data;
    }

    public Component getDescription() {
        return this.description;
    }

    public Set<Context> getDefinitionContexts() {
        return this.definitionContexts;
    }

    public void setDefinitionContexts(Set<Context> contexts) {
        this.definitionContexts = contexts;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setDefaultValue(Tristate value) {
        this.defaultValue = value;
    }

    public void setIsEnabled(boolean val) {
        this.enabled = val;
    }
}
