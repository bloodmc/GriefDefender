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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.flag.Flag;
import com.griefdefender.api.permission.flag.FlagData;
import com.griefdefender.api.permission.flag.FlagDefinition;

import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.serializer.plain.PlainComponentSerializer;

public class GDFlagDefinition implements FlagDefinition {

    private boolean enabled = true;
    private boolean adminDefinition = false;
    private Set<Context> definitionContexts = new HashSet<>();
    private List<FlagData> data = new ArrayList<>();
    private String displayName;
    private String groupName;
    private Tristate defaultValue = Tristate.UNDEFINED;
    private Component description;

    public GDFlagDefinition(List<FlagData> flagData, String displayName, Component description, String groupName, boolean isAdmin, Set<Context> contexts) {
        this.data = flagData;
        this.displayName = displayName;
        this.description = description;
        this.groupName = groupName;
        this.definitionContexts = contexts;
        this.adminDefinition = isAdmin;
    }

    public void addFlagData(Flag flag, Set<Context> contexts) {
        this.data.add(new GDFlagData(flag, contexts));
    }

    @Override
    public void addFlagData(FlagData flagData) {
        this.data.add(new GDFlagData(flagData.getFlag(), flagData.getContexts()));
    }

    @Override
    public List<Flag> getFlags() {
        List<Flag> flags = new ArrayList<>();
        for (FlagData flagData : this.data) {
            flags.add(flagData.getFlag());
        }
        return flags;
    }

    @Override
    public List<FlagData> getFlagData() {
        return this.data;
    }

    @Override
    public Component getDescription() {
        return this.description;
    }

    @Override
    public String getFriendlyDescription() {
        if (this.description == null) {
            return "";
        }

        return PlainComponentSerializer.INSTANCE.serialize(this.description);
    }

    @Override
    public Set<Context> getContexts() {
        return this.definitionContexts;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public void setIsEnabled(boolean val) {
        this.enabled = val;
    }

    @Override
    public boolean isAdmin() {
        return this.adminDefinition;
    }

    @Override
    public String getId() {
        return GriefDefenderPlugin.MOD_ID + ":" + this.displayName;
    }

    @Override
    public String getName() {
        return this.displayName;
    }

    @Override
    public String getGroupName() {
        return this.groupName;
    }

    @Override
    public Tristate getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public void setDefaultValue(Tristate value) {
        this.defaultValue = value;
    }

    public static class FlagDefinitionBuilder implements Builder {

        private boolean enabled = true;
        private boolean isAdmin = false;
        private Set<Context> contexts = new HashSet<>();
        private List<FlagData> data = new ArrayList<>();
        private String displayName;
        private String groupName;
        private Tristate defaultValue = Tristate.UNDEFINED;
        private Component description = TextComponent.empty();

        @Override
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        @Override
        public Builder admin(boolean value) {
            this.isAdmin = value;
            return this;
        }

        @Override
        public Builder group(String group) {
            this.groupName = group;
            return this;
        }

        @Override
        public Builder defaultValue(Tristate value) {
            this.defaultValue = value;
            return this;
        }

        @Override
        public Builder flagData(FlagData data) {
            this.data.add(data);
            return this;
        }

        @Override
        public Builder flagData(List<FlagData> data) {
            this.data = data;
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
        public Builder name(String name) {
            this.displayName = name;
            return this;
        }

        @Override
        public Builder description(Component description) {
            this.description = description;
            return this;
        }

        @Override
        public Builder reset() {
            this.enabled = true;
            this.isAdmin = false;
            this.contexts = new HashSet<>();
            this.data = new ArrayList<>();
            this.displayName = "";
            this.groupName = "";
            this.defaultValue = Tristate.UNDEFINED;
            this.description = TextComponent.empty();
            return this;
        }

        @Override
        public FlagDefinition build() {
            checkNotNull(this.data);
            checkNotNull(this.displayName);
            checkNotNull(this.groupName);
            checkNotNull(this.description);
            final GDFlagDefinition definition = new GDFlagDefinition(this.data, this.displayName, this.description, this.groupName, this.isAdmin, this.contexts);
            definition.setIsEnabled(this.enabled);
            definition.setDefaultValue(this.defaultValue);
            return definition;
        }
        
    }
}
