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
package com.griefdefender.configuration;

import com.griefdefender.configuration.category.ConfigCategory;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class PlayerDataConfig extends ConfigCategory {

    private boolean requiresSave = true;

    @Setting(value = "accrued-claim-blocks", comment = "How many claim blocks the player has earned in world via play time.")
    private int accruedClaimBlocks;
    @Setting(value = "bonus-claim-blocks",
            comment = "How many claim blocks the player has been gifted in world by admins, or purchased via economy integration.")
    private int bonusClaimBlocks = 0;
    @Setting(value = "migrated-blocks")
    private boolean migrated = false;

    public int getAccruedClaimBlocks() {
        return this.accruedClaimBlocks;
    }

    public int getBonusClaimBlocks() {
        return this.bonusClaimBlocks;
    }

    public void setAccruedClaimBlocks(int blocks) {
        this.requiresSave = true;
        this.accruedClaimBlocks = blocks;
    }

    public void setBonusClaimBlocks(int blocks) {
        this.requiresSave = true;
        this.bonusClaimBlocks = blocks;
    }

    public boolean requiresSave() {
        return this.requiresSave;
    }

    public void setRequiresSave(boolean flag) {
        this.requiresSave = flag;
    }

    // Remove after 4.0
    public boolean hasMigratedBlocks() {
        return this.migrated;
    }

    public void setMigratedBlocks(boolean flag) {
        this.migrated = flag;
    }
}
