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
package com.griefdefender.configuration.category;

import com.griefdefender.api.permission.option.Options;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.util.ArrayList;
import java.util.List;

@ConfigSerializable
public class OptionCategory extends ConfigCategory {

    @Setting(value = "user-town-options", comment = "A list of options standard users can manage in their towns with the /co commands.")
    private List<String> userTownOptions = new ArrayList<>();

    public OptionCategory() {
        this.userTownOptions.add(Options.ABANDON_RETURN_RATIO.toString());
        this.userTownOptions.add(Options.BLOCKS_ACCRUED_PER_HOUR.toString());
        this.userTownOptions.add(Options.EXPIRATION.toString());
        this.userTownOptions.add(Options.CREATE_LIMIT.toString());
        this.userTownOptions.add(Options.EXPIRATION.toString());
        this.userTownOptions.add(Options.INITIAL_BLOCKS.toString());
        this.userTownOptions.add(Options.MAX_ACCRUED_BLOCKS.toString());
        this.userTownOptions.add(Options.MAX_LEVEL.toString());
        this.userTownOptions.add(Options.MAX_SIZE_X.toString());
        this.userTownOptions.add(Options.MAX_SIZE_Y.toString());
        this.userTownOptions.add(Options.MAX_SIZE_Z.toString());
        this.userTownOptions.add(Options.MIN_LEVEL.toString());
        this.userTownOptions.add(Options.MIN_SIZE_X.toString());
        this.userTownOptions.add(Options.MIN_SIZE_Y.toString());
        this.userTownOptions.add(Options.MIN_SIZE_Z.toString());
        this.userTownOptions.add(Options.TAX_EXPIRATION.toString());
        this.userTownOptions.add(Options.TAX_EXPIRATION_DAYS_KEEP.toString());
        this.userTownOptions.add(Options.TAX_RATE.toString());
    }

    public List<String> getUserTownOptions() {
        return this.userTownOptions;
    }
}
