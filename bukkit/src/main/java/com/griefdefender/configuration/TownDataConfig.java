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

import com.griefdefender.api.data.TownData;
import net.kyori.text.Component;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ConfigSerializable
public class TownDataConfig extends ClaimDataConfig implements TownData {

    @Setting
    private Component townTag;
    @Setting
    private Map<UUID, Integer> accruedBlocks = new HashMap<>();
    @Setting
    private Map<UUID, Integer> bonusBlocks = new HashMap<>();
    @Setting
    private Map<UUID, Integer> createMode = new HashMap<>();
    @Setting
    private Map<UUID, String> residentPastDueTaxTimestamps = new HashMap<>();
    @Setting
    private Map<UUID, Double> residentTaxBalances = new HashMap<>();

    @Override
    public Optional<Component> getTownTag() {
        return Optional.ofNullable(this.townTag);
    }

    public void setTownTag(Component tag) {
        this.townTag = tag;
    }

    public Map<UUID, Integer> getAccruedClaimBlocks() {
        return this.accruedBlocks;
    }

    public Map<UUID, Integer> getBonusClaimBlocks() {
        return this.bonusBlocks;
    }

    public Map<UUID, Integer> getCreateModes() {
        return this.createMode;
    }

    public Map<UUID, String> getResidentPastDueTaxTimestamps() {
        return this.residentPastDueTaxTimestamps;
    }

    public Map<UUID, Double> getResidentTaxBalances() {
        return this.residentTaxBalances;
    }
}
