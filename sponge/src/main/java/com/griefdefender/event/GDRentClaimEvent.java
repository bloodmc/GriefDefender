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
package com.griefdefender.event;

import com.griefdefender.api.User;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.economy.PaymentType;
import com.griefdefender.api.event.RentClaimEvent;

public class GDRentClaimEvent extends GDClaimEvent implements RentClaimEvent {

    private final double originalRentRate;
    private final double originalRentBalance;
    private double rentRate;
    private PaymentType paymentType;
    private User user;

    public GDRentClaimEvent(Claim claim, User user, double rate, double amount) {
        super(claim);
        this.originalRentRate = rate;
        this.originalRentBalance = amount;
        this.rentRate = rate;
        this.user = user;
        this.paymentType = claim.getEconomyData().getPaymentType();
    }

    @Override
    public double getOriginalRentRate() {
        return this.originalRentRate;
    }

    @Override
    public double getOriginalRentBalance() {
        return this.originalRentBalance;
    }

    @Override
    public double getRentRate() {
        return this.rentRate;
    }

    @Override
    public double getRentBalance() {
        return this.originalRentBalance + this.rentRate;
    }

    @Override
    public void setRentRate(double newRentRate) {
        this.rentRate = newRentRate;
    }

    @Override
    public User getRenter() {
        return this.user;
    }

    @Override
    public PaymentType getPaymentType() {
        return this.paymentType;
    }
}
