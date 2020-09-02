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
package com.griefdefender.economy;

import static com.google.common.base.Preconditions.checkNotNull;

import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.economy.PaymentTransaction;
import com.griefdefender.api.economy.TransactionResultType;
import com.griefdefender.api.economy.TransactionType;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class GDPaymentTransaction implements PaymentTransaction {

    public final TransactionType type;
    public final TransactionResultType result;
    public final UUID source;
    public final Instant timestamp;
    public final double amount;

    public GDPaymentTransaction(Claim claim, TransactionType type, TransactionResultType result, Instant timestamp, double amount) {
        this(type, result, timestamp, amount);
    }

    public GDPaymentTransaction(TransactionType type, TransactionResultType result, UUID source, Instant timestamp, double amount) {
        this.type = type;
        this.result = result;
        this.timestamp = timestamp;
        this.amount = amount;
        this.source = source;
    }

    public GDPaymentTransaction(TransactionType type, TransactionResultType result, Instant timestamp, double amount) {
        this.type = type;
        this.result = result;
        this.timestamp = timestamp;
        this.amount = amount;
        this.source = null;
    }

    @Override
    public TransactionType getType() {
        return this.type;
    }

    @Override
    public TransactionResultType getResultType() {
        return this.result;
    }

    @Override
    public Optional<UUID> getSource() {
        return Optional.ofNullable(this.source);
    }

    @Override
    public Instant getTimestamp() {
        return this.timestamp;
    }

    @Override
    public Double getAmount() {
        return this.amount;
    }

    public static class PaymentTransactionBuilder implements Builder {

        private UUID source;
        private double amount;
        private TransactionType type;
        private TransactionResultType result;

        @Override
        public Builder type(TransactionType type) {
            this.type = type;
            return this;
        }

        @Override
        public Builder result(TransactionResultType result) {
            this.result = result;
            return this;
        }

        @Override
        public Builder source(UUID source) {
            this.source = source;
            return this;
        }

        @Override
        public Builder amount(double amount) {
            this.amount = amount;
            return this;
        }

        @Override
        public Builder reset() {
            this.source = null;
            this.amount = 0;
            this.result = null;
            return this;
        }

        @Override
        public PaymentTransaction build() {
            checkNotNull(this.type);
            checkNotNull(this.result);
            checkNotNull(this.amount);
            if (this.source != null) {
                return new GDPaymentTransaction(this.type, this.result, this.source, Instant.now(), this.amount); 
            }
            return new GDPaymentTransaction(this.type, this.result, Instant.now(), this.amount); 
        }
        
    }
}
