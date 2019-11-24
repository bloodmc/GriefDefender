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

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class EconomyCategory extends ConfigCategory {

    @Setting(value = "economy-mode", comment = "Uses economy instead of player claim blocks for claim creation."
            + "\nIf true, disables the claim block system in favor of economy."
            + "\nNote: Using this mode disables the '/buyblocks' command as claim creation will pull funds directly from a player's economy balance."
            + "\nNote: If players have existing claimblocks from past configurations, they can use the '/sellblocks' command to convert remainder to currency.")
    public boolean economyMode = false;
    @Setting(value = "use-claim-block-task", comment = "Claim blocks earned will be converted to economy based on 'claim-block-cost'." 
            + "\n(Default: false)\nNote: This setting can only be used if 'economy-mode' is true.")
    public boolean useClaimBlockTask = false;
}