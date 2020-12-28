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

import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.economy.TransactionType;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class EconomyCategory extends ConfigCategory {

    @Setting(value = "economy-mode", comment = "Uses economy instead of player claim blocks for claim creation."
            + "\nIf true, disables the claim block system in favor of economy."
            + "\nNote: Using this mode disables the '/buyblocks' command as claim creation will pull funds directly from a player's economy balance."
            + "\nNote: If players have existing claimblocks from past configurations, an admin must use the '/ecomigrateblocks' command to convert remainder to currency.")
    public boolean economyMode = false;
    @Setting(value = "use-claim-block-task", comment = "Claim blocks earned will be converted to economy based on 'economy-block-cost'." 
            + "\n(Default: false)\nNote: This setting can only be used if 'economy-mode' is true.")
    public boolean useClaimBlockTask = false;
    @Setting(value = "bank-system", comment = "Whether to enable the bank system for claims. Set to true to enable.")
    public boolean bankSystem = false;
    @Setting(value = "tax-system", comment = "Whether to enable the tax system for claims. Set to true to enable.")
    public boolean taxSystem = false;
    @Setting(value = "bank-transaction-log-limit", comment = "The amount of transactions to keep for history. Default: 60")
    public int bankTransactionLogLimit = 60;
    @Setting(value = "sell-sign", comment = "Whether sell signs are enabled.")
    private boolean sellSignEnabled = false;
    @Setting(value = "rent-max-time-limit", comment = "Controls the maximum time limit(hours or days) that a claim owner can have their rental max set to."
            + "\nNote: This only affects claim rentals that have a max specified. If no max is specified by the claim owner, a renter may rent as long as they want.")
    public int rentMaxTimeLimit = 100;
    @Setting(value = "rent-restore-day-warning", comment = "Controls which day a player should start to receive warnings about their rented claim nearing expiration. "
            + "\nEx. If set to '5', this will begin to send players messaging on login and at the rent apply hour when 5 days are remaining before expiration."
            + "\nNote: This only applies if the owner has 'rent-restore' option enabled and the rent owner sets a max.")
    public int rentRestoreDayWarning = 5;
    @Setting(value = "rent-schematic-restore-admin", comment = "Controls whether rented admin claims will use a schematic for restoration."
            + "\nNote: If set, the claim will create a schematic on rental start and restore it back when finished."
            + "\nNote: This ONLY applies to rentals with a max date set.")
    public boolean rentSchematicRestoreAdmin = false;
    @Setting(value = "rent-system", comment = "Controls whether the rent system is enabled. Note: This is currently experimental, use with caution.")
    public boolean rentSystem = false;
    @Setting(value = "rent-sign", comment = "Whether rent signs are enabled.")
    private boolean rentSignEnabled = false;
    @Setting(value = "rent-task-interval", comment = "The interval in minutes for checking claim rent payments that are due. Default: 1. Set to 0 to disable.")
    public int rentTaskInterval = 1;
    @Setting(value = "rent-delinquent-task-apply-hour", comment = "The specific hour in day to attempt to get owed claim rent balances from delinquent renters. Note: This uses military time and accepts values between 0-23. Default: 0")
    public int rentDelinquentApplyHour = 0;
    @Setting(value = "rent-transaction-log-limit", comment = "The amount of transactions to keep for history. Default: 60")
    public int rentTransactionLogLimit = 60;
    @Setting(value = "sign-update-interval", comment = "The interval in minutes for updating sign data. Default: 1. Set to 0 to disable.")
    public int signUpdateInterval = 1;
    @Setting(value = "tax-transaction-log-limit", comment = "The amount of transactions to keep for history. Default: 60")
    public int taxTransactionLogLimit = 60;
    @Setting(value = "tax-apply-hour", comment = "The specific hour in day to apply tax to all claims. Note: This uses military time and accepts values between 0-23. Default: 0")
    public int taxApplyHour = 0;

    public int getTransactionLogLimit(TransactionType type) {
        if (type == TransactionType.BANK_DEPOSIT || type == TransactionType.BANK_WITHDRAW) {
            return this.bankTransactionLogLimit;
        }
        if (type == TransactionType.RENT) {
            return this.rentTransactionLogLimit;
        }
        return this.taxTransactionLogLimit;
    }

    public boolean isSellSignEnabled() {
        if (GriefDefenderPlugin.getInstance().getVaultProvider() == null) {
            return false;
        }

        return this.sellSignEnabled;
    }

    public boolean isRentSignEnabled() {
        if (GriefDefenderPlugin.getInstance().getVaultProvider() == null) {
            return false;
        }

        return this.rentSignEnabled;
    }
}