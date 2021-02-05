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
package com.griefdefender.util;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimResult;
import com.griefdefender.api.claim.ClaimResultType;
import com.griefdefender.api.claim.ClaimSchematic;
import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.api.claim.ClaimTypes;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.api.economy.PaymentType;
import com.griefdefender.api.economy.TransactionResultType;
import com.griefdefender.api.economy.TransactionType;
import com.griefdefender.api.permission.flag.Flag;
import com.griefdefender.api.permission.flag.Flags;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.claim.GDClaimResult;
import com.griefdefender.command.CommandHelper;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.economy.GDPaymentTransaction;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.internal.provider.GDWorldEditProvider;
import com.griefdefender.internal.util.BlockUtil;
import com.griefdefender.internal.visual.GDClaimVisual;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.text.action.GDCallbackHolder;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.spongeapi.TextAdapter;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.format.TextColor;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.living.monster.Monster;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class EconomyUtil {

    private final EconomyService economyService = GriefDefenderPlugin.getInstance().economyService.orElse(null);
    private static EconomyUtil instance;

    public static EconomyUtil getInstance() {
        return instance;
    }

    static {
        instance = new EconomyUtil();
    }

    public TransactionResult withdrawTax(GDClaim claim, UUID playerUniqueId, double taxOwed) {
        if (GriefDefenderPlugin.getGlobalConfig().getConfig().economy.bankSystem) {
            final Account bankAccount = this.economyService.getOrCreateAccount(claim.getUniqueId().toString()).orElse(null);
            if (bankAccount != null) {
                final Currency defaultCurrency = GriefDefenderPlugin.getInstance().economyService.get().getDefaultCurrency();
                final double bankBalance = bankAccount.getBalance(defaultCurrency).doubleValue();
                if (bankBalance > 0) {
                    TransactionResult withdrawResponse = null;
                    if (taxOwed == bankBalance || bankBalance > taxOwed) {
                        withdrawResponse = bankAccount.withdraw(defaultCurrency, BigDecimal.valueOf(taxOwed), Sponge.getCauseStackManager().getCurrentCause());
                        if (withdrawResponse.getResult() == ResultType.SUCCESS) {
                            claim.getData().getEconomyData().addPaymentTransaction(
                                    new GDPaymentTransaction(TransactionType.BANK_WITHDRAW, TransactionResultType.SUCCESS, playerUniqueId, Instant.now(), taxOwed));
                            return withdrawResponse;
                        }
                    } else {
                        withdrawResponse = bankAccount.withdraw(defaultCurrency, BigDecimal.valueOf(bankBalance), Sponge.getCauseStackManager().getCurrentCause());
                        if (withdrawResponse.getResult() == ResultType.SUCCESS) {
                            taxOwed -= bankBalance;
                            claim.getData().getEconomyData().addPaymentTransaction(
                                    new GDPaymentTransaction(TransactionType.BANK_WITHDRAW, TransactionResultType.SUCCESS, playerUniqueId, Instant.now(), bankBalance));
                        }
                    }
                }
            }
        }
        return withdrawFunds(playerUniqueId, taxOwed);
    }

    public void economyCreateClaimConfirmation(Player player, GDPlayerData playerData, int height, Vector3i point1, Vector3i point2, ClaimType claimType, boolean cuboid, Claim parent) {
        GDClaim claim = new GDClaim(player.getWorld(), point1, point2, claimType, player.getUniqueId(), cuboid);
        claim.parent = (GDClaim) parent;
        final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(player);
        final int claimCost = BlockUtil.getInstance().getClaimBlockCost(player.getWorld(), claim.lesserBoundaryCorner, claim.greaterBoundaryCorner, claim.cuboid);
        final Double economyBlockCost = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Double.class), user, Options.ECONOMY_BLOCK_COST, claim);
        final double requiredFunds = claimCost * economyBlockCost;
        final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_BUY_CONFIRMATION,
                ImmutableMap.of("amount", "$" + String.format("%.2f", requiredFunds)));
        final Component buyConfirmationText = TextComponent.builder()
                .append(message)
                .append(TextComponent.builder()
                    .append("\n[")
                    .append(MessageCache.getInstance().LABEL_CONFIRM.color(TextColor.GREEN))
                    .append("]\n")
                    .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(player, economyClaimBuyConfirmed(player, playerData, height, requiredFunds, point1, point2, claimType, cuboid, parent), true))).build())
                .build();
        GriefDefenderPlugin.sendMessage(player, buyConfirmationText);
    }

    private static Consumer<CommandSource> economyClaimBuyConfirmed(Player player, GDPlayerData playerData, int height, double requiredFunds, Vector3i lesserBoundaryCorner, Vector3i greaterBoundaryCorner, ClaimType claimType, boolean cuboid, Claim parent) {
        return confirm -> {
            // try to create a new claim
            ClaimResult result = null;
            GDCauseStackManager.getInstance().pushCause(player);
            result = GriefDefenderPlugin.getInstance().dataStore.createClaim(
                    player.getWorld(),
                    lesserBoundaryCorner,
                    greaterBoundaryCorner,
                    claimType, player.getUniqueId(), cuboid);
            GDCauseStackManager.getInstance().popCause();

            GDClaim gdClaim = (GDClaim) result.getClaim().orElse(null);
            // if it didn't succeed, tell the player why
            if (!result.successful()) {
                if (result.getResultType() == ClaimResultType.OVERLAPPING_CLAIM) {
                    GDClaim overlapClaim = (GDClaim) result.getClaim().get();
                    GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CREATE_OVERLAP_SHORT);
                    Set<Claim> claims = new HashSet<>();
                    claims.add(overlapClaim);
                    CommandHelper.showClaims(player, claims, height, true);
                } else {
                    GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CREATE_FAILED_RESULT,
                            ImmutableMap.of("reason", result.getResultType())));
                }
                return;
            }

            // otherwise, advise him on the /trust command and show him his new claim
            else {
                Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_BUY_CONFIRMED, ImmutableMap.of(
                            "amount", "$" + String.format("%.2f", requiredFunds)));
                GriefDefenderPlugin.sendMessage(player, message);
                playerData.lastShovelLocation = null;
                message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CREATE_SUCCESS, ImmutableMap.of(
                        "type", gdClaim.getFriendlyNameType(true)));
                GriefDefenderPlugin.sendMessage(player, message);
                final GDWorldEditProvider worldEditProvider = GriefDefenderPlugin.getInstance().worldEditProvider;
                final GDClaimVisual visual = gdClaim.getVisualizer();
                if (visual.getVisualTransactions().isEmpty()) {
                    visual.createClaimBlockVisuals(height, player.getLocation(), playerData);
                }
                visual.apply(player, false);
            }
        };
    }

    public static TransactionResult depositFunds(UUID uuid, double amount) {
        final Account playerAccount = GriefDefenderPlugin.getInstance().economyService.get().getOrCreateAccount(uuid).orElse(null);
        final Currency defaultCurrency = GriefDefenderPlugin.getInstance().economyService.get().getDefaultCurrency();
        return playerAccount.deposit(defaultCurrency, BigDecimal.valueOf(amount), Sponge.getCauseStackManager().getCurrentCause());
    }

    public static TransactionResult withdrawFunds(UUID uuid, double amount) {
        final Account playerAccount = GriefDefenderPlugin.getInstance().economyService.get().getOrCreateAccount(uuid).orElse(null);
        final Currency defaultCurrency = GriefDefenderPlugin.getInstance().economyService.get().getDefaultCurrency();
        return playerAccount.withdraw(defaultCurrency, BigDecimal.valueOf(amount), Sponge.getCauseStackManager().getCurrentCause());
    }

    public GDClaimResult checkEconomyFunds(GDClaim claim, GDPlayerData newPlayerData, boolean withdrawFunds) {
        if (!GriefDefenderPlugin.getInstance().isEconomyModeEnabled()) { 
            return new GDClaimResult(claim, ClaimResultType.ECONOMY_ACCOUNT_NOT_FOUND);
        }

        final Object root = GDCauseStackManager.getInstance().getCurrentCause().root();
        final Player player = root instanceof Player ? (Player) root : null; 
        final World world = claim.getWorld();
        final int claimCost = BlockUtil.getInstance().getClaimBlockCost(world, claim.lesserBoundaryCorner, claim.greaterBoundaryCorner, claim.cuboid);
        final GDPermissionUser targetPlayer = newPlayerData.getSubject();
        final Account playerAccount = GriefDefenderPlugin.getInstance().economyService.get().getOrCreateAccount(targetPlayer.getUniqueId()).orElse(null);
        if (playerAccount == null) {
            return new GDClaimResult(claim, ClaimResultType.ECONOMY_ACCOUNT_NOT_FOUND);
        }

        final Currency defaultCurrency = GriefDefenderPlugin.getInstance().economyService.get().getDefaultCurrency();
        double requiredFunds = claimCost * claim.getOwnerEconomyBlockCost();
        final double currentFunds = playerAccount.getBalance(defaultCurrency).doubleValue();
        if (currentFunds < requiredFunds) {
            Component message = null;
            if (player != null) {
                message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_NOT_ENOUGH_FUNDS, ImmutableMap.of(
                        "balance", "$" + String.format("%.2f", currentFunds),
                        "amount", "$" + String.format("%.2f", requiredFunds)));
                GriefDefenderPlugin.sendMessage(player, message);
            }

            newPlayerData.lastShovelLocation = null;
            newPlayerData.claimResizing = null;
            return new GDClaimResult(claim, ClaimResultType.ECONOMY_NOT_ENOUGH_FUNDS, message);
        }

        if (withdrawFunds) {
            final TransactionResult result = playerAccount.withdraw(defaultCurrency, BigDecimal.valueOf(requiredFunds), Sponge.getCauseStackManager().getCurrentCause());
            if (result.getResult() != ResultType.SUCCESS) {
                Component message = null;
                if (player != null) {
                    message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_WITHDRAW_ERROR, ImmutableMap.of(
                            "reason", result.getResult().name()));
                    GriefDefenderPlugin.sendMessage(player, message);
                }
    
                newPlayerData.lastShovelLocation = null;
                newPlayerData.claimResizing = null;
                return new GDClaimResult(claim, ClaimResultType.ECONOMY_WITHDRAW_FAIL, message);
            }
        }

        return new GDClaimResult(claim, ClaimResultType.SUCCESS);
    }

    public Consumer<CommandSource> buyClaimConsumerConfirmation(CommandSource src, Claim claim) {
        return confirm -> {
            this.buyClaimConsumerConfirmation(src, claim, null);
        };
    }

    public void buyClaimConsumerConfirmation(CommandSource src, Claim claim, Sign sign) {
        final Player player = (Player) src;
        if (player.getUniqueId().equals(claim.getOwnerUniqueId())) {
            return;
        }

        final Account playerAccount = GriefDefenderPlugin.getInstance().economyService.get().getOrCreateAccount(player.getUniqueId()).orElse(null);
        if (playerAccount == null) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_PLAYER_NOT_FOUND, ImmutableMap.of(
                    "player", player.getName()));
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }

        final Currency defaultCurrency = GriefDefenderPlugin.getInstance().economyService.get().getDefaultCurrency();
        final double balance = playerAccount.getBalance(defaultCurrency).doubleValue();
        if (balance < claim.getEconomyData().getSalePrice()) {
            Map<String, Object> params = ImmutableMap.of(
                    "amount", "$" + String.format("%.2f",claim.getEconomyData().getSalePrice()),
                    "balance", "$" + String.format("%.2f", balance),
                    "amount_required", "$" + String.format("%.2f", claim.getEconomyData().getSalePrice() -  balance));
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_BUY_NOT_ENOUGH_FUNDS, params));
            return;
        }

        final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_BUY_CONFIRMATION,
                ImmutableMap.of("amount", "$" + String.format("%.2f", claim.getEconomyData().getSalePrice())));
        final Component buyConfirmationText = TextComponent.builder()
                .append(message)
                .append(TextComponent.builder()
                    .append("\n[")
                    .append(MessageCache.getInstance().LABEL_CONFIRM.color(TextColor.GREEN))
                    .append("]\n")
                    .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(src, this.createBuyConsumerConfirmed(src, claim, sign), true))).build())
                .build();
        GriefDefenderPlugin.sendMessage(player, buyConfirmationText);
    }

    private Consumer<CommandSource> createBuyConsumerConfirmed(CommandSource src, Claim claim, Sign sign) {
        return confirm -> {
            if (!claim.getEconomyData().isForSale()) {
                return;
            }
            final Player player = (Player) src;
            final GDPermissionUser owner = PermissionHolderCache.getInstance().getOrCreateUser(claim.getOwnerUniqueId());

            final Account ownerAccount = GriefDefenderPlugin.getInstance().economyService.get().getOrCreateAccount(owner.getUniqueId()).orElse(null);
            if (ownerAccount == null) {
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_PLAYER_NOT_FOUND, ImmutableMap.of(
                        "player", owner.getName()));
                GriefDefenderPlugin.sendMessage(player, message);
                return;
            }

            GDCauseStackManager.getInstance().pushCause(player);
            ClaimType originalType = claim.getType();
            if (claim.isAdminClaim()) {
                ((GDClaim) claim).setType(ClaimTypes.BASIC);
            }
            final ClaimResult result = ((GDClaim) claim).transferOwner(player.getUniqueId(), true, false);
            if (!result.successful()) {
                ((GDClaim) claim).setType(originalType);
                final Component defaultMessage = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.ECONOMY_CLAIM_BUY_TRANSFER_CANCELLED,
                        ImmutableMap.of(
                            "owner", owner.getName(),
                            "player", player.getName(),
                            "result", result.getMessage().orElse(TextComponent.of(result.getResultType().toString()))));
                TextAdapter.sendComponent(src, result.getMessage().orElse(defaultMessage));
                return;
            }

            final Account playerAccount = GriefDefenderPlugin.getInstance().economyService.get().getOrCreateAccount(player.getUniqueId()).orElse(null);
            final Currency defaultCurrency = GriefDefenderPlugin.getInstance().economyService.get().getDefaultCurrency();
            final double balance = playerAccount.getBalance(defaultCurrency).doubleValue();
            if (balance < claim.getEconomyData().getSalePrice()) {
                Map<String, Object> params = ImmutableMap.of(
                        "amount", "$" + String.format("%.2f",claim.getEconomyData().getSalePrice()),
                        "balance", "$" + String.format("%.2f", balance),
                        "amount_required", "$" + String.format("%.2f", claim.getEconomyData().getSalePrice() -  balance));
                GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_BUY_NOT_ENOUGH_FUNDS, params));
                return;
            }

            final double salePrice = claim.getEconomyData().getSalePrice();
            final ResultType transactionResult = owner.getOfflinePlayer() == null ? ResultType.SUCCESS :  depositFunds(owner.getOfflinePlayer().getUniqueId(), salePrice).getResult();
            if (transactionResult == ResultType.SUCCESS) {
                final TransactionResult withdrawResponse = withdrawFunds(player.getUniqueId(), salePrice);
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_BUY_CONFIRMED,
                    ImmutableMap.of(
                        "amount", "$" + String.format("%.2f",salePrice)));
                final Component saleMessage = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_SOLD,
                    ImmutableMap.of(
                        "amount", "$" + String.format("%.2f", salePrice),
                        "balance","$" + String.format("%.2f", ownerAccount.getBalance(defaultCurrency))));
                if (owner.getOnlinePlayer() != null) {
                    TextAdapter.sendComponent(owner.getOnlinePlayer(), saleMessage);
                }
                claim.getEconomyData().setForSale(false);
                claim.getEconomyData().setSalePrice(0);
                claim.getData().save();
                GriefDefenderPlugin.sendMessage(src, message);
                Sign buySign = sign;
                if (buySign == null) {
                    buySign = SignUtil.getSign(player.getWorld(), claim.getEconomyData().getRentSignPosition());
                }
                if (buySign != null) {
                    final SignData signData = buySign.getOrCreate(SignData.class).orElse(null);
                    if (signData != null) { 
                        signData.addElement(0, SpongeUtil.getSpongeText(MessageCache.getInstance().ECONOMY_SIGN_SOLD_LINE1));
                        final Component playerSignName = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_SIGN_SOLD_LINE2, ImmutableMap.of(
                                "player", player.getName()));
                        signData.addElement(1, SpongeUtil.getSpongeText(playerSignName));
                        signData.addElement(2, SpongeUtil.getSpongeText(MessageCache.getInstance().ECONOMY_SIGN_SOLD_LINE3));
                        signData.addElement(3, SpongeUtil.getSpongeText(MessageCache.getInstance().ECONOMY_SIGN_SOLD_LINE4));
                        buySign.offer(signData);
                    }
                }
            } else {
                ((GDClaim) claim).setType(originalType);
            }
        };
    }

    public Consumer<CommandSource> rentClaimConsumerConfirmation(CommandSource src, Claim claim) {
        return confirm -> {
            this.rentClaimConsumerConfirmation(src, claim, null);
        };
    }

    public void rentClaimConsumerConfirmation(CommandSource src, Claim claim, Sign sign) {
        final Player player = (Player) src;
        if (player.getUniqueId().equals(claim.getOwnerUniqueId())) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CLAIM_OWNER_ALREADY);
            return;
        }

        if (claim.getEconomyData().isRented()) {
            // already rented
            final UUID uuid = claim.getEconomyData().getRenters().get(0);
            final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(uuid);
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_RENTED_ALREADY, ImmutableMap.of(
                    "player", user.getName()));
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }

        final Account playerAccount = GriefDefenderPlugin.getInstance().economyService.get().getOrCreateAccount(player.getUniqueId()).orElse(null);
        if (playerAccount == null) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_PLAYER_NOT_FOUND, ImmutableMap.of(
                    "player", player.getName()));
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }

        final Currency defaultCurrency = GriefDefenderPlugin.getInstance().economyService.get().getDefaultCurrency();
        final double balance = playerAccount.getBalance(defaultCurrency).doubleValue();
        final double rate = claim.getEconomyData().getRentRate();
        if (balance < rate) {
            Map<String, Object> params = ImmutableMap.of(
                    "amount", "$" + String.format("%.2f", rate),
                    "balance", "$" + String.format("%.2f", balance),
                    "amount_required", "$" + String.format("%.2f", rate -  balance));
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_RENT_NOT_ENOUGH_FUNDS, params));
            return;
        }

        final int min = claim.getEconomyData().getRentMinTime();
        final int max = claim.getEconomyData().getRentMaxTime();
        final PaymentType paymentType = claim.getEconomyData().getPaymentType();
        Duration duration = null;
        Component message = null;
        Component maxTime = null;
        Component minTime = null;
        if (max > 0 && claim.getEconomyData().getRentEndDate() != null) {
            duration = Duration.between(Instant.now(), claim.getEconomyData().getRentEndDate());
            final long seconds = duration.getSeconds();
            final int day = (int)TimeUnit.SECONDS.toDays(seconds);        
            final long hours = TimeUnit.SECONDS.toHours(seconds) - (day *24);
            final long minutes = TimeUnit.SECONDS.toMinutes(seconds) - (TimeUnit.SECONDS.toHours(seconds)* 60);
            TextComponent.Builder maxBuilder = TextComponent.builder();
            if (day > 0) {
                maxBuilder.append(String.valueOf(day))
                    .append(" ")
                    .append((day > 1 ? MessageCache.getInstance().LABEL_DAYS : MessageCache.getInstance().LABEL_DAY))
                    .append(" ");
            }
            if (hours > 0) {
                maxBuilder.append(String.valueOf(hours))
                .append(" ")
                .append((hours > 1 ? MessageCache.getInstance().LABEL_HOURS : MessageCache.getInstance().LABEL_HOUR))
                .append(" ");
            }
            if (minutes > 0) {
                maxBuilder.append(String.valueOf(minutes))
                .append(" ")
                .append((minutes > 1 ? MessageCache.getInstance().LABEL_MINUTES : MessageCache.getInstance().LABEL_MINUTE))
                .append(" ");
            }
            maxTime = maxBuilder.build();
        }
        if (min > 0) {
            minTime = TextComponent.builder()
            .append(String.valueOf(min))
            .append(" ")
            .append(paymentType == PaymentType.DAILY ? 
                    (min > 1 ? MessageCache.getInstance().LABEL_DAYS : MessageCache.getInstance().LABEL_DAY) : 
                        (min > 1 ? MessageCache.getInstance().LABEL_HOURS : MessageCache.getInstance().LABEL_HOUR))
                .build();
        }

        if (min <= 0 && max <= 0) {
            message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_RENTER_CONFIRMATION,
                    ImmutableMap.of(
                        "amount", "$" + String.format("%.2f",rate),
                        "type", claim.getEconomyData().getPaymentType() == PaymentType.DAILY ? MessageCache.getInstance().LABEL_DAY : MessageCache.getInstance().LABEL_HOUR,
                        "fee", "$" + String.format("%.2f",rate)));
        } else if (min > 0 && max <= 0) {
            message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_RENTER_CONFIRMATION_MIN,
                    ImmutableMap.of(
                    "amount", "$" + String.format("%.2f",rate),
                    "min-time", minTime,
                    "type", paymentType == PaymentType.DAILY ? MessageCache.getInstance().LABEL_DAYS : MessageCache.getInstance().LABEL_HOUR,
                    "fee", "$" + String.format("%.2f",min * rate)));
        } else if (min <= 0 && max > 0) {
            message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_RENTER_CONFIRMATION_MAX,
                    ImmutableMap.of(
                    "amount", "$" + String.format("%.2f",rate),
                    "max-time", maxTime,
                    "type", paymentType == PaymentType.DAILY ? MessageCache.getInstance().LABEL_DAY : MessageCache.getInstance().LABEL_HOUR,
                    "fee", "$" + String.format("%.2f",rate)));
        } else if (min > 0 && max > 0) {
            message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_RENTER_CONFIRMATION_MIN_MAX,
                    ImmutableMap.of(
                    "amount", "$" + String.format("%.2f",rate),
                    "min-time", minTime,
                    "max-time", maxTime,
                    "type", paymentType == PaymentType.DAILY ? MessageCache.getInstance().LABEL_DAY : MessageCache.getInstance().LABEL_HOUR,
                    "fee", "$" + String.format("%.2f",min * rate)));
        }

        final Component rentConfirmationText = TextComponent.builder()
                .append(message)
                .append(TextComponent.builder()
                    .append("\n[")
                    .append(MessageCache.getInstance().LABEL_CONFIRM.color(TextColor.GREEN))
                    .append("]\n")
                    .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(src, this.createRentConsumerConfirmed(src, claim, sign), true))).build())
                .build();
        GriefDefenderPlugin.sendMessage(player, rentConfirmationText);
    }

    private Consumer<CommandSource> createRentConsumerConfirmed(CommandSource src, Claim claim, Sign sign) {
        return confirm -> {
            final GDPermissionUser owner = PermissionHolderCache.getInstance().getOrCreateUser(claim.getOwnerUniqueId());
            final Player player = (Player) src;
            if (claim.getEconomyData().isRented()) {
                // already rented
                final UUID uuid = claim.getEconomyData().getRenters().get(0);
                final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(uuid);
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_RENTED_ALREADY, ImmutableMap.of(
                        "player", user.getName()));
                GriefDefenderPlugin.sendMessage(player, message);
                return;
            }

            final Account playerAccount = GriefDefenderPlugin.getInstance().economyService.get().getOrCreateAccount(player.getUniqueId()).orElse(null);
            if (playerAccount == null) {
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_PLAYER_NOT_FOUND, ImmutableMap.of(
                        "player", player.getName()));
                GriefDefenderPlugin.sendMessage(player, message);
                return;
            }

            double rate = claim.getEconomyData().getRentRate();
            if (claim.getEconomyData().getRentMinTime() > 0) {
                rate = rate * claim.getEconomyData().getRentMinTime();
            }
            final TransactionResult withdrawResponse = withdrawFunds(player.getUniqueId(), rate);
            final Currency defaultCurrency = GriefDefenderPlugin.getInstance().economyService.get().getDefaultCurrency();
            if (withdrawResponse.getResult() != ResultType.SUCCESS) {
                Component message = null;
                if (player != null) {
                    message = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.ECONOMY_NOT_ENOUGH_FUNDS, ImmutableMap.of(
                            "balance", "$" + String.format("%.2f", playerAccount.getBalance(defaultCurrency),
                            "amount", "$" + String.format("%.2f", rate))));
                    GriefDefenderPlugin.sendMessage(player, message);
                }
                return;
            }

            claim.getEconomyData().addPaymentTransaction(new GDPaymentTransaction(TransactionType.RENT, TransactionResultType.SUCCESS, player.getUniqueId(), Instant.now(), rate));
            if (claim.getEconomyData().getRentMinTime() > 0) {
                final double minDeposit = rate * claim.getEconomyData().getRentMinTime();
                final double currentBalance = claim.getEconomyData().getRentBalance(player.getUniqueId());
                claim.getEconomyData().setRentBalance(player.getUniqueId(), currentBalance - minDeposit);
            }
            if (claim.isAdminClaim()) {
                final UUID bankAccount = claim.getEconomyAccountId().orElse(null);
                if (bankAccount != null) {
                    final Account account = GriefDefenderPlugin.getInstance().economyService.get().getOrCreateAccount(bankAccount).orElse(null);
                    account.deposit(defaultCurrency, BigDecimal.valueOf(rate), Sponge.getCauseStackManager().getCurrentCause());
                }
            } else {
                final Account ownerAccount = GriefDefenderPlugin.getInstance().economyService.get().getOrCreateAccount(owner.getUniqueId()).orElse(null);
                if (ownerAccount == null) {
                    final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_PLAYER_NOT_FOUND, ImmutableMap.of(
                            "player", owner.getName()));
                    GriefDefenderPlugin.sendMessage(player, message);
                    return;
                }

                GDCauseStackManager.getInstance().pushCause(player);
                final TransactionResult response = ownerAccount.deposit(defaultCurrency, BigDecimal.valueOf(rate), Sponge.getCauseStackManager().getCurrentCause());
                final Component saleMessage = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_RENTED,
                    ImmutableMap.of(
                        "amount", "$" + String.format("%.2f",rate),
                        "balance","$" + String.format("%.2f", ownerAccount.getBalance(defaultCurrency))));
                if (owner.getOnlinePlayer() != null) {
                    TextAdapter.sendComponent(owner.getOnlinePlayer(), saleMessage);
                }
            }

            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_RENTER_CONFIRMED,
                        ImmutableMap.of(
                            "amount", "$" + String.format("%.2f",rate),
                            "type", claim.getEconomyData().getPaymentType() == PaymentType.DAILY ? MessageCache.getInstance().LABEL_DAY : MessageCache.getInstance().LABEL_HOUR));

            // remove any existing trust of renter
            claim.removeUserTrust(player.getUniqueId(), TrustTypes.NONE);
            claim.getEconomyData().setForRent(false);
            claim.getEconomyData().getRenters().add(player.getUniqueId());
            claim.addUserTrust(player.getUniqueId(), TrustTypes.ACCESSOR);
            final Instant existingDate = claim.getEconomyData().getRentStartDate();
            claim.getEconomyData().setRentStartDate(Instant.now());
            if (claim.getEconomyData().getRentEndDate() == null && claim.getEconomyData().getRentMaxTime() > 0) {
                claim.getEconomyData().setRentEndDate(claim.getEconomyData().getRentStartDate().plus(claim.getEconomyData().getRentMaxTime(), ChronoUnit.DAYS));
            }

            boolean rentRestore = false;
            if (claim.isAdminClaim()) {
                rentRestore = GriefDefenderPlugin.getGlobalConfig().getConfig().economy.rentSchematicRestoreAdmin;
            } else {
                rentRestore = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Boolean.class), owner, Options.RENT_RESTORE, claim).booleanValue();
            }
            if (rentRestore) {
                // create schematic for restore when rent is done
                claim.deleteSchematic("__rent__");
                // remove monsters to avoid duping
                for (Entity entity : ((GDClaim) claim).getEntities()) {
                    if (entity instanceof Monster) {
                        entity.remove();
                    }
                }
                final ClaimSchematic schematic = ClaimSchematic.builder().claim(claim).name("__rent__").build().orElse(null);
                if (schematic != null) {
                    // remove all dropped items from claim
                    for (Entity entity : ((GDClaim) claim).getEntities()) {
                        if (entity instanceof Item || entity instanceof Monster) {
                            entity.remove();
                        }
                    }
                }
            }
            GriefDefenderPlugin.sendMessage(src, message);
            // check for existing sign pos
            Sign rentSign = sign;
            if (rentSign == null) {
                rentSign = SignUtil.getSign(player.getWorld(), claim.getEconomyData().getRentSignPosition());
            }
            if (rentSign != null) {
                claim.getEconomyData().setRentSignPosition(rentSign.getLocation().getBlockPosition());
                final SignData signData = rentSign.getOrCreate(SignData.class).orElse(null);
                if (signData != null) { 
                    signData.addElement(0, SpongeUtil.getSpongeText(MessageCache.getInstance().ECONOMY_SIGN_RENTED_LINE1));
                    signData.addElement(1, Text.of(player.getName()));
                    if (claim.getEconomyData().getRentEndDate() != null) {
                        signData.addElement(2, SpongeUtil.getSpongeText(MessageCache.getInstance().ECONOMY_SIGN_RENTED_LINE3));
                    } else {
                        signData.addElement(2, Text.EMPTY);
                        signData.addElement(3, Text.EMPTY);
                    }
                    rentSign.offer(signData);
                }
            }
            claim.getData().save();
        };
    }

    public void sellCancelConfirmation(CommandSource src, Claim claim, Sign sign) {
        final Player player = (Player) src;
        final GDClaim gdClaim = (GDClaim) claim;
        // check sell access
        if (gdClaim.allowEdit(player) != null) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CLAIM_NOT_YOURS);
            return;
        }

        final Component sellCancelConfirmationText = TextComponent.builder()
                .append("\n[")
                .append(MessageCache.getInstance().LABEL_CONFIRM.color(TextColor.GREEN))
                .append("]\n")
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(src, createSellCancelConfirmed(src, claim, sign), true)))
                .build();
        final Component message = TextComponent.builder()
                .append(MessageCache.getInstance().ECONOMY_CLAIM_SALE_CANCEL_CONFIRMATION)
                .append("\n")
                .append(sellCancelConfirmationText)
                .build();
        GriefDefenderPlugin.sendMessage(src, message);
    }

    private Consumer<CommandSource> createSellCancelConfirmed(CommandSource src, Claim claim, Sign sign) {
        return confirm -> {
            if (!claim.getEconomyData().isForSale()) {
                return;
            }
            Location<World> location = null;
            if (sign != null) {
                location = sign.getLocation();
            } else {
                final Sign saleSign = SignUtil.getSign(((GDClaim) claim).getWorld(), claim.getEconomyData().getSaleSignPosition());
                if (saleSign != null) {
                    location = saleSign.getLocation();
                }
            }
            if (location != null && location.getBlockType() != BlockTypes.AIR) {
                location.setBlockType(BlockTypes.AIR);
                SignUtil.resetSellData(claim);
                claim.getData().save();
                GriefDefenderPlugin.sendMessage(src, MessageCache.getInstance().ECONOMY_CLAIM_SALE_CANCELLED);
            }
        };
    }

    public void rentCancelConfirmation(CommandSource src, Claim claim, Sign sign) {
        final Player player = (Player) src;
        final GDClaim gdClaim = (GDClaim) claim;
        final List<UUID> renters = claim.getEconomyData().getRenters();
        GDPermissionUser renter = null;
        if (!renters.isEmpty()) {
            renter = PermissionHolderCache.getInstance().getOrCreateUser(claim.getEconomyData().getRenters().get(0));
        }

        // check rent access
        if (renter != null && !player.getUniqueId().equals(renter.getUniqueId())) {
            final GDPermissionUser srcPlayer = PermissionHolderCache.getInstance().getOrCreateUser(player);
            if (!srcPlayer.getInternalPlayerData().canIgnoreClaim(gdClaim)) {
                if (player.getUniqueId().equals(claim.getOwnerUniqueId()) || (claim.isAdminClaim() && gdClaim.allowEdit(srcPlayer) == null)) {
                    final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_RENTED_NO_CANCEL,
                            ImmutableMap.of(
                                "player", renter.getFriendlyName()));
                    GriefDefenderPlugin.sendMessage(player, message);
                    return;
                } else {
                    GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CLAIM_NOT_YOURS);
                    return;
                }
            }
        } else {
            if (!gdClaim.getEconomyData().isRented() && gdClaim.allowEdit(player) != null) {
                GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().ECONOMY_CLAIM_RENT_NOT_RENTING);
                return;
            }
        }

        final GDPermissionUser owner = PermissionHolderCache.getInstance().getOrCreateUser(claim.getOwnerUniqueId());
        boolean rentRestore = false;
        if (claim.isAdminClaim()) {
           rentRestore = GriefDefenderPlugin.getGlobalConfig().getConfig().economy.rentSchematicRestoreAdmin;
        } else {
           rentRestore = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Boolean.class), owner == null ? GriefDefenderPlugin.DEFAULT_HOLDER : owner, Options.RENT_RESTORE, claim).booleanValue();
        }
        Component rentCancelConfirmationMessage = rentRestore ? MessageCache.getInstance().ECONOMY_CLAIM_RENT_RESTORE_CANCEL_CONFIRMATION : MessageCache.getInstance().ECONOMY_CLAIM_RENT_CANCEL_CONFIRMATION;
        // check renter balance
        boolean addDelinquent = false;
        if (renter != null && player.getUniqueId().equals(renter.getUniqueId())) {
            final double rentBalance = claim.getEconomyData().getRentBalance(renter.getUniqueId());
            if (rentBalance > 0) {
                final Account playerAccount = GriefDefenderPlugin.getInstance().getEconomyService().getOrCreateAccount(renter.getUniqueId()).orElse(null);
                final Currency defaultCurrency = GriefDefenderPlugin.getInstance().economyService.get().getDefaultCurrency();
                if (rentRestore) {
                    rentCancelConfirmationMessage = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_RENT_RESTORE_CANCEL_CONFIRMATION_BALANCE,
                            ImmutableMap.of(
                                "balance", "$" + String.format("%.2f", playerAccount.getBalance(defaultCurrency).doubleValue())));
                } else {
                    rentCancelConfirmationMessage = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_RENT_CANCEL_CONFIRMATION_BALANCE,
                            ImmutableMap.of(
                                "balance", "$" + String.format("%.2f", playerAccount.getBalance(defaultCurrency).doubleValue())));
                }
                addDelinquent = true;
            }
        }
        final Component rentCancelConfirmationText = TextComponent.builder()
                .append("\n[")
                .append(MessageCache.getInstance().LABEL_CONFIRM.color(TextColor.GREEN))
                .append("]\n")
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(src, createRentCancelConfirmed(src, claim, sign, addDelinquent), true)))
                .build();
        final Component message = TextComponent.builder()
                .append(rentCancelConfirmationMessage)
                .append("\n")
                .append(rentCancelConfirmationText)
                .build();
        GriefDefenderPlugin.sendMessage(src, message);
    }

    private Consumer<CommandSource> createRentCancelConfirmed(CommandSource src, Claim claim, Sign sign, boolean addDelinquent) {
        return confirm -> {
            if (!claim.getEconomyData().isForRent() && !claim.getEconomyData().isRented()) {
                return;
            }
            final Player player = (Player) src;
            boolean isRenter = false;
            for (UUID uuid : claim.getEconomyData().getRenters()) {
                if (player.getUniqueId().equals(uuid)) {
                    isRenter = true;
                    break;
                }
            }
            if (player.getUniqueId().equals(claim.getOwnerUniqueId()) || (claim.isAdminClaim() && !isRenter)) {
                    Location<World> location = null;
                if (sign != null) {
                    location = sign.getLocation();
                } else {
                    final Sign rentSign = SignUtil.getSign(((GDClaim) claim).getWorld(), claim.getEconomyData().getRentSignPosition());
                    if (rentSign != null) {
                        location = rentSign.getLocation();
                    }
                }
                if (location != null) {
                    location.setBlockType(BlockTypes.AIR);
                }
                SignUtil.resetRentData(claim);
                claim.getData().save();
                GriefDefenderPlugin.sendMessage(src, MessageCache.getInstance().ECONOMY_CLAIM_RENT_CANCELLED);
            } else if (claim.getEconomyData().isRented()) {
                // reset sign
                claim.getEconomyData().setForRent(true);
                SignUtil.updateSignRentable(claim, sign);
                claim.getEconomyData().setRentSignPosition(null);
                claim.getEconomyData().getRenters().clear();
                claim.getEconomyData().setRentStartDate(null);
                if (addDelinquent) {
                    claim.getEconomyData().getDelinquentRenters().add(player.getUniqueId());
                }
                claim.removeUserTrust(player.getUniqueId(), TrustTypes.NONE);
                claim.getData().save();
                final GDPermissionUser owner = PermissionHolderCache.getInstance().getOrCreateUser(claim.getOwnerUniqueId());
                boolean rentRestore = false;
                if (claim.isAdminClaim()) {
                    rentRestore = GriefDefenderPlugin.getGlobalConfig().getConfig().economy.rentSchematicRestoreAdmin;
                } else {
                    rentRestore = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Boolean.class), owner == null ? GriefDefenderPlugin.DEFAULT_HOLDER : owner, Options.RENT_RESTORE, claim).booleanValue();
                }
                if (rentRestore) {
                    // expiration days keep is up
                    // restore schematic and remove renter rights
                    final ClaimSchematic schematic = claim.getSchematics().get("__rent__");
                    if (schematic != null) {
                        if (schematic.apply()) {
                            if (owner != null && owner.getOnlinePlayer() != null) {
                                owner.getOnlinePlayer().sendMessage(Text.of("Claim '" + ((GDClaim) claim).getFriendlyName() + "' has been restored."));
                            }
                        }
                    }
                }
                GriefDefenderPlugin.sendMessage(src, MessageCache.getInstance().ECONOMY_CLAIM_RENT_CANCELLED);
            }
        };
    }

    public boolean isRenter(Claim claim, GDPermissionUser user) {
        if (GriefDefenderPlugin.getInstance().getEconomyService() == null) {
            return false;
        }

        for (UUID uuid : claim.getEconomyData().getRenters()) {
            if (user.getUniqueId().equals(uuid)) {
                return true;
            }
        }

        return false;
    }

    public boolean isRentFlag(Flag flag) {
        if (flag == Flags.EXPLOSION_BLOCK || flag == Flags.EXPLOSION_ENTITY) {
            return false;
        }

        return true;
    }

    public Component getUserTimeRemaining(Instant to, Component label) {
        Duration duration = Duration.between(Instant.now(), to);
        final long seconds = duration.getSeconds();
        final int day = (int)TimeUnit.SECONDS.toDays(seconds);        
        final long hours = TimeUnit.SECONDS.toHours(seconds) - (day *24);
        final long minutes = TimeUnit.SECONDS.toMinutes(seconds) - (TimeUnit.SECONDS.toHours(seconds)* 60);
        TextComponent.Builder builder = TextComponent.builder()
                .append(label.color(TextColor.YELLOW))
                .append(" : ");
        if (day > 0) {
            builder.append(String.valueOf(day))
                .append(" ")
                .append((day > 1 ? MessageCache.getInstance().LABEL_DAYS : MessageCache.getInstance().LABEL_DAY))
                .append(" ");
        }
        if (hours > 0) {
            builder.append(String.valueOf(hours))
                .append(" ")
                .append((hours > 1 ? MessageCache.getInstance().LABEL_HOURS : MessageCache.getInstance().LABEL_HOUR))
                .append(" ");
        }
        if (minutes > 0) {
            builder.append(String.valueOf(minutes))
                .append(" ")
                .append((minutes > 1 ? MessageCache.getInstance().LABEL_MINUTES : MessageCache.getInstance().LABEL_MINUTE))
                .append(" ");
        }
        return builder.build();
    }
}
