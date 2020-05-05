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

import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.api.claim.ClaimTypes;
import com.griefdefender.api.permission.flag.Flag;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;

public class GDFlag implements Flag {

    private final String id;
    private final String name;
    private Component description;

    public GDFlag(String id, String name) {
        this.id = id;
        this.name = name.toLowerCase();
    }

    @Override
    public String getId() {
        return this.id;
    }

    public String getPermission() {
        return "griefdefender.flag." + this.name.toLowerCase();
    }

    public String getName() {
        return this.name;
    }

    public Component getDescription() {
        if (this.description == null) {
            this.description = this.createDescription();
        }
        return this.description;
    }

    @Override
    public String toString() {
        return this.name;
    }

    private Component createDescription() {
        final Component description = GriefDefenderPlugin.getInstance().messageData.getMessage("flag-description-" + this.name.toLowerCase());
        if (description != null) {
            return description;
        }
        return TextComponent.of("Not defined.");
    }

    public void reloadDescription() {
        this.description = null;
    }

    @Override
    public boolean getDefaultClaimTypeValue(ClaimType type) {
        if (type == null || type != ClaimTypes.WILDERNESS) {
            switch (this.name) {
                case "block-break" :
                case "block-modify" :
                case "block-place" :
                case "collide-block" :
                case "collide-entity" :
                case "entity-damage" :
                case "explosion-block" :
                case "explosion-entity" :
                case "interact-block-primary" :
                case "interact-block-secondary" :
                case "interact-entity-primary" :
                case "interact-inventory" : 
                case "liquid-flow" : 
                case "projectile-impact-block" :
                case "projectile-impact-entity" : 
                    return false;
                default :
                    return true;
            }
        }

        return true;
    }
}
