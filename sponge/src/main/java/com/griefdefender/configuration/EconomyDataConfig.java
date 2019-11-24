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

import com.google.gson.Gson;
import com.griefdefender.api.data.EconomyData;
import com.griefdefender.api.economy.BankTransaction;
import com.griefdefender.configuration.category.ConfigCategory;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ConfigSerializable
public class EconomyDataConfig extends ConfigCategory implements EconomyData {

    public GriefDefenderConfig<?> activeConfig;

    @Setting(value = ClaimStorageData.MAIN_CLAIM_FOR_SALE)
    private boolean forSale = false;
    @Setting(value = ClaimStorageData.MAIN_CLAIM_SALE_PRICE)
    private double salePrice = 0.0;
    @Setting(value = ClaimStorageData.MAIN_TAX_BALANCE)
    private double taxBalance = 0.0;
    @Setting(value = ClaimStorageData.MAIN_TAX_PAST_DUE_DATE)
    private String taxPastDueDate;
    @Setting
    private List<String> bankTransactionLog = new ArrayList<>();

    @Override
    public boolean isForSale() {
        return this.forSale;
    }

    @Override
    public double getSalePrice() {
        return this.salePrice;
    }

    @Override
    public void setForSale(boolean forSale) {
        this.forSale = forSale;
    }

    @Override
    public double getTaxBalance() {
        return this.taxBalance;
    }

    @Override
    public void setTaxBalance(double balance) {
        this.taxBalance = balance;
    }

    @Override
    public Optional<Instant> getTaxPastDueDate() {
        if (this.taxPastDueDate == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Instant.parse(this.taxPastDueDate));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    @Override
    public void setTaxPastDueDate(Instant date) {
        this.taxPastDueDate = date == null ? null : date.toString();
    }

    @Override
    public void setSalePrice(double price) {
        this.salePrice = price;
    }

    @Override
    public List<String> getBankTransactionLog() {
        return this.bankTransactionLog;
    }

    @Override
    public void addBankTransaction(BankTransaction transaction) {
        if (this.getBankTransactionLog().size() == this.activeConfig.getConfig().claim.bankTransactionLogLimit) {
            this.getBankTransactionLog().remove(0);
        }
        this.getBankTransactionLog().add(new Gson().toJson(transaction));
    }

    @Override
    public void clearBankTransactionLog() {
        this.bankTransactionLog.clear();
    }
}
