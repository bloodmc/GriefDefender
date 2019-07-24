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

import com.griefdefender.util.PermissionUtil;
import me.lucko.luckperms.api.PermissionHolder;

import java.util.UUID;

public class GDPermissionHolder {

    private String identifier;
    protected PermissionHolder luckPermsHolder;
    private Integer hashCode;

    public GDPermissionHolder(String identifier) {
        this.identifier = identifier;
    }

    // used for default
    public GDPermissionHolder(PermissionHolder holder) {
        this.luckPermsHolder = holder;
        this.identifier = holder.getObjectName();
    }

    public PermissionHolder getLuckPermsHolder() {
        if (this.luckPermsHolder == null) {
            UUID uuid = null;
            try {
                uuid = UUID.fromString(this.identifier);
            } catch (IllegalArgumentException e) {
                
            }

            if (uuid != null) {
                this.luckPermsHolder = PermissionUtil.getInstance().getUserSubject(uuid);
            } else {
                this.luckPermsHolder = PermissionUtil.getInstance().getGroupSubject(this.identifier);
            }
        }
        return this.luckPermsHolder;
    }

    public String getFriendlyName() {
        return this.getLuckPermsHolder().getFriendlyName();
    }

    public String getIdentifier() {
        return this.identifier;
    }

    @Override
    public int hashCode() {
        if (this.hashCode == null) {
            this.hashCode = 31 * this.getLuckPermsHolder().hashCode();
        }
        return this.hashCode;
    }
}
