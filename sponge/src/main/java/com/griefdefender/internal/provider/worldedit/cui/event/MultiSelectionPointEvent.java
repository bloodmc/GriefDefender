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
package com.griefdefender.internal.provider.worldedit.cui.event;

import com.griefdefender.internal.provider.worldedit.cui.MultiSelectionType;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.internal.cui.CUIEvent;

public class MultiSelectionPointEvent implements CUIEvent {

    private final String TILDE = "~";
    private final String[] parameters;

    // Used to force WECUI to follow player EYE
    public MultiSelectionPointEvent(int id) {
        this.parameters = new String[] { 
                String.valueOf(id),
                TILDE,
                TILDE,
                TILDE,
                String.valueOf(0)};
    }

    public MultiSelectionPointEvent(int id, Vector pos, int area) {
        this.parameters = new String[] { 
                String.valueOf(id),
                String.valueOf(pos.getX()),
                String.valueOf(pos.getY()),
                String.valueOf(pos.getZ()),
                String.valueOf(area)};
    }

    @Override
    public String getTypeId() {
        return MultiSelectionType.POINT;
    }

    @Override
    public String[] getParameters() {
        return this.parameters;
    }

}