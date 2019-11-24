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
package com.griefdefender.util;

import com.griefdefender.api.Tristate;
import org.spongepowered.common.SpongeImpl;

public class BlockPosCache {

    private int lastTickCounter;
    private short lastBlockPos;
    private Tristate lastResult = Tristate.UNDEFINED;

    public BlockPosCache(short pos) {
        this.lastBlockPos = pos;
        this.lastTickCounter = SpongeImpl.getServer().getTickCounter();
    }

    public void setLastResult(Tristate result) {
        this.lastResult = result;
    }

    public Tristate getCacheResult(short pos) {
        int currentTick = SpongeImpl.getServer().getTickCounter();
        if (this.lastBlockPos != pos) {
            this.lastBlockPos = pos;
            this.lastTickCounter = currentTick;
            return Tristate.UNDEFINED;
        }

        if ((currentTick - this.lastTickCounter) <= 2) {
            this.lastTickCounter = currentTick;
            return this.lastResult;
        }

        this.lastTickCounter = currentTick;
        return Tristate.UNDEFINED;
    }
}
