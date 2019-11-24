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
package com.griefdefender.permission;

import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.Subject;


public class GDPermissionHolder implements Subject {

    private String identifier;
    private String friendlyName;
    private Integer hashCode;

    public GDPermissionHolder(String identifier) {
        this.identifier = identifier;
        this.friendlyName = identifier;
    }

    // used for default
    public GDPermissionHolder(String objectName, String friendlyName) {
        this.identifier = objectName;
        this.friendlyName = friendlyName;
    }

    @Override
    public String getFriendlyName() {
        if (this.friendlyName == null) {
            return this.identifier;
        }
        return this.friendlyName;
    }

    @Override
    public String getIdentifier() {
        return this.identifier;
    }

    public org.spongepowered.api.service.permission.Subject getDefaultUser() {
        return GriefDefenderPlugin.getInstance().permissionService.getDefaults();
    }

    @Override
    public int hashCode() {
        if (this.hashCode == null) {
            this.hashCode = 31 * this.identifier.hashCode();
        }
        return this.hashCode;
    }
}
