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
package com.griefdefender.provider;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultProvider {

    private Economy vaultApi;

    public VaultProvider() {
        this.initEconomy();
    }

    public Economy getApi() {
        if (this.vaultApi == null) {
            this.initEconomy();
        }
        return this.vaultApi;
    }

    private void initEconomy() {
        final RegisteredServiceProvider<Economy> economyProvider = Bukkit.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            this.vaultApi = economyProvider.getProvider();
        }
    }

    public boolean hasAccount(OfflinePlayer player) {
        if (this.getApi() == null) {
            return false;
        }
        return this.vaultApi.hasAccount(player);
    }

    public double getBalance(Player player) {
        if (this.getApi() == null) {
            return 0;
        }
        return this.vaultApi.getBalance(player);
    }

    public boolean depositPlayer(OfflinePlayer player, double amount) {
        if (this.getApi() == null) {
            return false;
        }

        final EconomyResponse result = this.vaultApi.depositPlayer(player, amount);
        return result.transactionSuccess();
    }
}
