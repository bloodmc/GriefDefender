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

import com.flowpowered.math.vector.Vector3i;
import com.google.gson.Gson;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.data.EconomyData;
import com.griefdefender.api.economy.PaymentTransaction;
import com.griefdefender.api.economy.TransactionType;
import com.griefdefender.api.economy.PaymentType;
import com.griefdefender.configuration.category.ConfigCategory;
import com.griefdefender.economy.GDPaymentTransaction;
import com.griefdefender.util.BlockUtil;
import com.griefdefender.util.TaskUtil;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.checkerframework.checker.nullness.qual.Nullable;

@ConfigSerializable
public class EconomyDataConfig extends ConfigCategory implements EconomyData {

    public GriefDefenderConfig<?> activeConfig;
    private Vector3i rentSignVec;
    private Vector3i saleSignVec;
    private List<PaymentTransaction> bankPaymentTransactions;
    private List<PaymentTransaction> rentPaymentTransactions;
    private List<PaymentTransaction> taxPaymentTransactions;

    @Setting(value = ClaimStorageData.MAIN_CLAIM_FOR_SALE)
    private boolean forSale = false;
    @Setting
    private boolean forRent = false;
    @Setting(value = "rent-balances")
    private Map<UUID, Double> rentBalances = new HashMap<>();
    @Setting(value = "rent-payment-type")
    private PaymentType paymentType = PaymentType.UNDEFINED;
    @Setting
    private List<UUID> renters = new ArrayList<>();
    @Setting(value = "renters-delinquent")
    private List<UUID> delinquentRenters = new ArrayList<>();
    @Setting(value = "rent-min-time")
    private int rentMinTime = 0;
    @Setting(value = "rent-max-time")
    private int rentMaxTime = 30;
    @Setting(value = ClaimStorageData.MAIN_RENT_PRICE)
    private double rentRate = 0.0;
    @Setting(value = ClaimStorageData.MAIN_RENT_START_DATE)
    private String rentStartDate;
    @Setting(value = ClaimStorageData.MAIN_RENT_END_DATE)
    private String rentEndDate;
    @Setting(value = ClaimStorageData.MAIN_RENT_PAST_DUE_DATE)
    private String rentPastDueDate;
    @Setting(value = ClaimStorageData.MAIN_RENT_SIGN_POS)
    private String rentSignPos;
    @Setting(value = ClaimStorageData.MAIN_SALE_SIGN_POS)
    private String saleSignPos;
    @Setting(value = ClaimStorageData.MAIN_SALE_END_DATE)
    private String saleEndDate;
    @Setting(value = ClaimStorageData.MAIN_SALE_PRICE)
    private double salePrice = 0.0;
    @Setting(value = ClaimStorageData.MAIN_TAX_BALANCE)
    private double taxBalance = 0.0;
    @Setting(value = ClaimStorageData.MAIN_TAX_PAST_DUE_DATE)
    private String taxPastDueDate;
    @Setting
    private List<String> bankTransactionLog = new ArrayList<>();
    @Setting
    private List<String> rentTransactionLog = new ArrayList<>();
    @Setting
    private List<String> taxTransactionLog = new ArrayList<>();

    @Override
    public int getRentMaxTime() {
        return this.rentMaxTime;
    }

    @Override
    public double getRentRate() {
        return this.rentRate;
    }

    @Override
    public Instant getRentPastDueDate() {
        if (this.rentPastDueDate == null) {
            return null;
        }
        try {
            return (Instant.parse(this.rentPastDueDate));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    @Override
    public void setRentPastDueDate(Instant date) {
        this.rentPastDueDate = date == null ? null : date.toString();
    }

    @Override
    public Instant getRentStartDate() {
        if (this.rentStartDate == null) {
            return null;
        }
        try {
            return Instant.parse(this.rentStartDate);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    @Override
    public Instant getRentEndDate() {
        if (this.rentEndDate == null) {
            return null;
        }
        try {
            return Instant.parse(this.rentEndDate);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    @Override
    public Instant getRentPaymentDueDate() {
        final List<PaymentTransaction> transactions = this.getPaymentTransactions(TransactionType.RENT);
        if (transactions.isEmpty()) {
            return null;
        }

        final PaymentTransaction lastTransaction = transactions.get(0);
        if (this.paymentType == PaymentType.HOURLY) {
            return lastTransaction.getTimestamp().plus(Duration.ofHours(1));
        }
        if (this.paymentType == PaymentType.DAILY) {
            return lastTransaction.getTimestamp().plus(Duration.ofDays(1));
        }
        final LocalDate localDate = TaskUtil.convertToLocalDate(Date.from(lastTransaction.getTimestamp()));
        if (this.paymentType == PaymentType.WEEKLY) {
            return TaskUtil.convertToInstant(localDate.plusWeeks(1));
        }
        return TaskUtil.convertToInstant(localDate.plusMonths(1));
    }

    @Override
    public List<UUID> getRenters() {
        return this.renters;
    }

    @Override
    public List<UUID> getDelinquentRenters() {
        return this.delinquentRenters;
    }

    @Override
    public int getRentMinTime() {
        return this.rentMinTime;
    }

    @Override
    public void setRentMinTime(int min) {
        this.rentMinTime = min;
    }

    @Override
    public boolean isForRent() {
        return this.forRent;
    }

    @Override
    public boolean isForSale() {
        return this.forSale;
    }

    @Override
    public double getSalePrice() {
        return this.salePrice;
    }

    @Override
    public void setForRent(boolean forRent) {
        this.forRent = forRent;
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
    public Instant getTaxPastDueDate() {
        if (this.taxPastDueDate == null) {
            return null;
        }
        try {
            return Instant.parse(this.taxPastDueDate);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    @Override
    public void setTaxPastDueDate(Instant date) {
        this.taxPastDueDate = date == null ? null : date.toString();
    }

    @Override
    public void setRentMaxTime(int max) {
        this.rentMaxTime = max;
    }

    @Override
    public void setRentRate(double price) {
        this.rentRate = price;
    }

    @Override
    public void setRentStartDate(Instant date) {
        this.rentStartDate = date == null ? null : date.toString();
    }

    @Override
    public void setRentEndDate(Instant date) {
        this.rentEndDate = date == null ? null : date.toString();
    }

    @Override
    public @Nullable Instant getSaleEndDate() {
        if (this.saleEndDate == null) {
            return null;
        }
        try {
            return Instant.parse(this.saleEndDate);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    @Override
    public void setSaleEndDate(Instant date) {
        this.saleEndDate = date == null ? null : date.toString();
    }

    @Override
    public void setSalePrice(double price) {
        this.salePrice = price;
    }

    @Override
    public @Nullable Vector3i getRentSignPosition() {
        if (this.rentSignPos == null) {
            return null;
        }

        if (this.rentSignVec == null) {
            try {
                this.rentSignVec = BlockUtil.getInstance().posFromString(this.rentSignPos);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return this.rentSignVec;
    }

    @Override
    public @Nullable Vector3i getSaleSignPosition() {
        if (this.saleSignPos == null) {
            return null;
        }

        if (this.saleSignVec == null) {
            try {
                this.saleSignVec = BlockUtil.getInstance().posFromString(this.saleSignPos);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return this.saleSignVec;
    }

    @Override
    public void setRentSignPosition(Vector3i pos) {
        if (pos == null) {
            this.rentSignPos = null;
            return;
        }

        // force reset of vec
        this.rentSignVec = null;
        this.rentSignPos = BlockUtil.getInstance().posToString(pos);
    }

    @Override
    public void setSaleSignPosition(Vector3i pos) {
        if (pos == null) {
            this.saleSignPos = null;
            return;
        }

        // force reset of vec
        this.saleSignVec = null;
        this.saleSignPos = BlockUtil.getInstance().posToString(pos);
    }

    @Override
    public List<PaymentTransaction> getPaymentTransactions(TransactionType type) {
        if (type == TransactionType.RENT) {
            if (this.rentPaymentTransactions == null) {
                this.rentPaymentTransactions = new ArrayList<>();
                for (String transaction : this.rentTransactionLog) {
                    this.rentPaymentTransactions.add(new Gson().fromJson(transaction, GDPaymentTransaction.class));
                }
            }
            return this.rentPaymentTransactions;
        } else if (type == TransactionType.BANK_DEPOSIT || type == TransactionType.BANK_WITHDRAW){
            if (this.bankPaymentTransactions == null) {
                this.bankPaymentTransactions = new ArrayList<>();
                for (String transaction : this.bankTransactionLog) {
                    this.bankPaymentTransactions.add(new Gson().fromJson(transaction, GDPaymentTransaction.class));
                }
            }
            return this.bankPaymentTransactions;
        } else {
            if (this.taxPaymentTransactions == null) {
                this.taxPaymentTransactions = new ArrayList<>();
                for (String transaction : this.taxTransactionLog) {
                    this.taxPaymentTransactions.add(new Gson().fromJson(transaction, GDPaymentTransaction.class));
                }
            }
            return this.taxPaymentTransactions;
        }
    }

    public List<String> getPaymentTransactionLog(TransactionType type) {
        if (type == TransactionType.BANK_DEPOSIT || type == TransactionType.BANK_WITHDRAW) {
            return this.bankTransactionLog;
        }
        if (type == TransactionType.RENT) {
            return this.rentTransactionLog;
        }
        return this.taxTransactionLog;
    }

    @Override
    public void addPaymentTransaction(PaymentTransaction transaction) {
        final List<String> transactionLog = this.getPaymentTransactionLog(transaction.getType());
        if (transactionLog.size() == GriefDefenderPlugin.getGlobalConfig().getConfig().economy.getTransactionLogLimit(transaction.getType())) {
            transactionLog.remove(0);
        }
        // add new payments to start of list
        // oldest payments should always be at end
        transactionLog.add(0, new Gson().toJson(transaction));
        this.getPaymentTransactions(transaction.getType()).add(0, transaction);
    }

    @Override
    public void clearPaymentTransactions(TransactionType type) {
        if (type == TransactionType.RENT) {
            this.rentTransactionLog.clear();
            this.rentPaymentTransactions.clear();
        } else {
            this.bankTransactionLog.clear();
            this.bankPaymentTransactions.clear();
        }
    }

    @Override
    public PaymentType getPaymentType() {
        return this.paymentType;
    }

    @Override
    public void setPaymentType(PaymentType type) {
        this.paymentType = type;
    }

    @Override
    public boolean isUserRenting(UUID uuid) {
        return this.renters.contains(uuid);
    }

    @Override
    public double getRentBalance(UUID uuid) {
        final Double balance = this.rentBalances.get(uuid);
        if (balance == null) {
            return 0;
        }
        return balance;
    }

    @Override
    public void setRentBalance(UUID uuid, double balance) {
        if (balance <= 0) {
            this.rentBalances.remove(uuid);
            return;
        }
        this.rentBalances.put(uuid, balance);
    }
}
