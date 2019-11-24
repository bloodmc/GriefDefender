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

import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.event.TaxClaimEvent;

public class GDTaxClaimEvent extends GDClaimEvent implements TaxClaimEvent {

    private final double originalTaxRate;
    private final double originalTaxAmount;
    private double taxRate;

    public GDTaxClaimEvent(Claim claim, double rate, double amount) {
        super(claim);
        this.originalTaxRate = rate;
        this.originalTaxAmount = amount;
        this.taxRate = rate;
    }

    @Override
    public double getOriginalTaxRate() {
        return this.originalTaxRate;
    }

    @Override
    public double getOriginalTaxAmount() {
        return this.originalTaxAmount;
    }

    @Override
    public double getTaxRate() {
        return this.taxRate;
    }

    @Override
    public double getTaxAmount() {
        return (this.getClaim().getClaimBlocks() / 256) * this.taxRate;
    }

    @Override
    public void setTaxRate(double newTaxRate) {
        this.taxRate = newTaxRate;
    }
}
