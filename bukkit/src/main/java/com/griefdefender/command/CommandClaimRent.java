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
package com.griefdefender.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableMap;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.economy.PaymentType;
import com.griefdefender.api.economy.TransactionType;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.claim.GDClaimManager;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.internal.pagination.PaginationList;
import com.griefdefender.internal.util.VecHelper;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.text.action.GDCallbackHolder;
import com.griefdefender.util.ChatCaptureUtil;
import com.griefdefender.util.EconomyUtil;
import com.griefdefender.util.PlayerUtil;
import com.griefdefender.util.SignUtil;

import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import net.kyori.text.serializer.plain.PlainComponentSerializer;
import net.milkbowl.vault.economy.Economy;

import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_CLAIM_RENT)
public class CommandClaimRent extends BaseCommand {

    @CommandCompletion("@gdrentcommands @gddummy")
    @CommandAlias("claimrent")
    @Description("%claim-rent")
    @Syntax("create <rate> [<max_days>]|info|list|cancel]")
    @Subcommand("claim rent")
    public void execute(Player player, @Optional String[] args) {
        if (GriefDefenderPlugin.getInstance().getVaultProvider() == null) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().ECONOMY_NOT_INSTALLED);
            return;
        }
        if (!GriefDefenderPlugin.getGlobalConfig().getConfig().economy.rentSystem) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().RENT_SYSTEM_DISABLED);
            return;
        }

        final Economy economy = GriefDefenderPlugin.getInstance().getVaultProvider().getApi();
        if (!economy.hasAccount(player)) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_PLAYER_NOT_FOUND, ImmutableMap.of(
                    "player", player.getName()));
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }

        if (args.length == 0) {
            throw new InvalidCommandArgument();
        }

        if (args != null && args.length > 0) {
            final String subCommand = args[0];
            // cancel
            if (subCommand.equalsIgnoreCase("cancel")) {
                final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
                final GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(playerData, player.getLocation());
                if (claim.isWilderness()) {
                    return;
                }
                boolean isRenter = false;
                for (UUID uuid : claim.getEconomyData().getRenters()) {
                    if (player.getUniqueId().equals(uuid)) {
                        isRenter = true;
                        break;
                    }
                }
                if (!playerData.canIgnoreClaim(claim) && !claim.getEconomyData().isForRent() && !isRenter) {
                    if (player.getUniqueId().equals(claim.getOwnerUniqueId())) {
                        GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().ECONOMY_CLAIM_RENT_OWNER_NOT_RENTING);
                    } else {
                        GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().ECONOMY_CLAIM_RENT_NOT_RENTING);
                    }
                    return;
                }

                Sign sign = null;
                final Vector3i signPos = claim.getEconomyData().getRentSignPosition();
                if (signPos != null) {
                    sign = SignUtil.getSign(VecHelper.toLocation(player.getWorld(), signPos));
                }
                EconomyUtil.getInstance().rentCancelConfirmation(player, claim, sign);
                return;
            } else if (subCommand.equalsIgnoreCase("clearbalance")) {
                if (args.length != 2) {
                    return;
                }

                final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
                final GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(playerData, player.getLocation());
                if (claim.isWilderness()) {
                    return;
                }
                if (playerData.canIgnoreClaim(claim) || player.hasPermission(GDPermissions.COMMAND_DELETE_ADMIN_CLAIMS)) {
                    final GDPermissionUser renter = PermissionHolderCache.getInstance().getOrCreateUser(args[1]);
                    if (renter != null) {
                        claim.getEconomyData().setRentBalance(renter.getUniqueId(), 0);
                    }
                }
            } else if (subCommand.equalsIgnoreCase("list")) {
                final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
                Set<Claim> claimsForRent = new HashSet<>();
                GDClaimManager claimManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(player.getWorld().getUID());
                for (Claim worldClaim : claimManager.getWorldClaims()) {
                    if (worldClaim.isWilderness()) {
                        continue;
                    }
                    if (!worldClaim.isAdminClaim() && worldClaim.getEconomyData().isForRent() && worldClaim.getEconomyData().getRentRate() > -1) {
                        claimsForRent.add(worldClaim);
                    }
                    for (Claim child : worldClaim.getChildren(true)) {
                        if (child.isAdminClaim()) {
                            continue;
                        }
                        if (child.getEconomyData().isForRent() && child.getEconomyData().getRentRate() > -1) {
                            claimsForRent.add(child);
                        }
                    }
                }

                List<Component> textList = CommandHelper.generateClaimTextListCommand(new ArrayList<Component>(), claimsForRent, player.getWorld().getName(), null, player, CommandHelper.createCommandConsumer(player, "claimrent", ""), false);
                Component footer = TextComponent.empty();
                int fillSize = 20 - (textList.size() + 2);
                if (player.hasPermission(GDPermissions.CHAT_CAPTURE)) {
                    footer = TextComponent.builder()
                                .append(ChatCaptureUtil.getInstance().createRecordChatComponent(player, null, playerData, "claimrent"))
                                .build();
                    fillSize = 20 - (textList.size() + 3);
                }

                for (int i = 0; i < fillSize; i++) {
                    textList.add(TextComponent.of(" "));
                }

                PaginationList.Builder paginationBuilder = PaginationList.builder()
                        .title(MessageCache.getInstance().TITLE_RENT).padding(TextComponent.of(" ").decoration(TextDecoration.STRIKETHROUGH, true)).contents(textList).footer(footer);
                paginationBuilder.sendTo(player);
                return;
            } else if (subCommand.equalsIgnoreCase("create") && args.length > 1) {
                // create
                final String strRate = args[1];
                if (strRate.length() < 2) {
                    // invalid rate
                    return;
                }
                if (args.length <= 3) {
                    // no max
                    final PaymentType paymentType = SignUtil.getPaymentType(strRate);
                    if (paymentType == PaymentType.UNDEFINED) {
                        // invalid
                        return;
                    }

                    Double rate = null;
                    try {
                        rate = Double.valueOf(strRate.substring(0, strRate.length() - 1));
                    } catch (NumberFormatException e) {
                        return;
                    }

                    int rentMin = 0;
                    int rentMax = 0;
                    if (args.length == 3) {
                        rentMin = SignUtil.getRentMinTime(args[2]);
                        rentMax = SignUtil.getRentMaxTime(args[2]);
                    }

                    final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
                    final GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(playerData, player.getLocation());
                    SignUtil.setClaimForRent(claim, player, null, rate, rentMin, rentMax, paymentType);
                    return;
                }
            } else if (subCommand.equalsIgnoreCase("info")) {
                final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
                final GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(playerData, player.getLocation());
                if (claim.isWilderness()) {
                    return;
                }
                if (!claim.getEconomyData().isForRent() && !claim.getEconomyData().isRented()) {
                    if (player.getUniqueId().equals(claim.getOwnerUniqueId())) {
                        GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().ECONOMY_CLAIM_RENT_OWNER_NOT_RENTING);
                    } else {
                        GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().ECONOMY_CLAIM_RENT_NOT_RENTING);
                    }
                    return;
                }

                final UUID ownerUniqueId = claim.getOwnerUniqueId();
                final boolean isAdmin = player.getUniqueId().equals(ownerUniqueId) || player.hasPermission(GDPermissions.COMMAND_DELETE_ADMIN_CLAIMS) || claim.allowEdit(player) == null;
                List<Component> textList = new ArrayList<>();
                PaymentType paymentType = claim.getEconomyData().getPaymentType();
                if (paymentType == PaymentType.UNDEFINED) {
                    paymentType = PaymentType.DAILY;
                }

                Component rentRate = TextComponent.builder()
                        .append(MessageCache.getInstance().LABEL_RATE.color(TextColor.YELLOW))
                        .append(" : ")
                        .append("$", TextColor.GOLD)
                        .append(String.format("%.2f", (claim.getEconomyData().getRentRate())), TextColor.GOLD)
                        .append("/", TextColor.GOLD)
                        .append(paymentType == PaymentType.DAILY ? MessageCache.getInstance().LABEL_DAY.color(TextColor.GOLD) : MessageCache.getInstance().LABEL_HOUR.color(TextColor.GOLD))
                        .build();

                Component rentBalance = null;
                Component rentStart = null;
                Component rentDue = null;
                Date rentStartDate = null;
                Date rentDueDate = null;
                Instant instant = claim.getEconomyData().getRentStartDate();
                if (instant != null) {
                    rentBalance = TextComponent.builder()
                            .append(MessageCache.getInstance().LABEL_BALANCE.color(TextColor.YELLOW))
                            .append(" : ")
                            .append("$" + String.format("%.2f", claim.getEconomyData().getRentBalance(player.getUniqueId())))
                            .build();
                    rentStartDate = Date.from(instant);
                    rentStart = TextComponent.builder()
                            .append(MessageCache.getInstance().RENT_UI_START_DATE.color(TextColor.YELLOW))
                            .append(" : ")
                            .append(rentStartDate == null ? "N/A" : rentStartDate.toString())
                            .build();
                }

                if (claim.getEconomyData().getRentPaymentDueDate() != null) {
                    rentDue= EconomyUtil.getInstance().getUserTimeRemaining(claim.getEconomyData().getRentPaymentDueDate(), MessageCache.getInstance().RENT_UI_NEXT_PAYMENT_DUE_DATE);
                }

                final String ownerName = PlayerUtil.getInstance().getUserName(ownerUniqueId);
                final Component nameComponent = claim.getName().orElse(null);
                Component ownerLine = TextComponent.builder()
                        .append(MessageCache.getInstance().LABEL_OWNER.color(TextColor.YELLOW))
                        .append(" : ")
                        .append(ownerName != null && !claim.isAdminClaim() && !claim.isWilderness() ? ownerName : "administrator", TextColor.GOLD)
                        .build();
                Component claimName = TextComponent.builder()
                        .append(MessageCache.getInstance().LABEL_NAME.color(TextColor.YELLOW))
                        .append(" : ", TextColor.YELLOW)
                        .append(claim.getName().orElse(TextComponent.of(claim.getFriendlyName())))
                        .build();

                final int min = claim.getEconomyData().getRentMinTime();
                final int max = claim.getEconomyData().getRentMaxTime();
                Component maxTime = null;
                Component minTime = null;
                if (max > 0 && claim.getEconomyData().getRentEndDate() != null) {
                    maxTime = EconomyUtil.getInstance().getUserTimeRemaining(claim.getEconomyData().getRentEndDate(), MessageCache.getInstance().RENT_UI_END_DATE);
                }
                if (min > 0) {
                    minTime = TextComponent.builder()
                        .append(MessageCache.getInstance().RENT_UI_MINIMUM.color(TextColor.YELLOW))
                        .append(" : ")
                        .append(String.valueOf(min))
                        .append(" ")
                        .append(claim.getEconomyData().getPaymentType() == PaymentType.DAILY ? 
                            (min > 1 ? MessageCache.getInstance().LABEL_DAYS : MessageCache.getInstance().LABEL_DAY) : 
                                (min > 1 ? MessageCache.getInstance().LABEL_HOURS : MessageCache.getInstance().LABEL_HOUR))
                        .build();
                }

                Component transactions = null;
                if (isAdmin) {
                    if (claim.getEconomyData().getPaymentTransactions(TransactionType.RENT).isEmpty()) {
                        transactions = TextComponent.builder()
                                .append("[")
                                .append(MessageCache.getInstance().RENT_UI_VIEW_TRANSACTIONS.color(TextColor.AQUA))
                                .append("]")
                                .hoverEvent(HoverEvent.showText(MessageCache.getInstance().RENT_UI_NO_TRANSACTIONS))
                                .build();
                    } else {
                        transactions = TextComponent.builder()
                            .append("[")
                            .append(MessageCache.getInstance().RENT_UI_VIEW_TRANSACTIONS.color(TextColor.AQUA))
                            .append("]")
                            .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(CommandHelper.createRentTransactionsConsumer(player, claim, false, false))))
                            .hoverEvent(HoverEvent.showText(MessageCache.getInstance().UI_CLICK_VIEW_TRANSACTIONS))
                            .build();
                    }
                }

                textList.add(claimName);
                textList.add(ownerLine);
                textList.add(rentRate);
                if (rentStart != null) {
                    textList.add(rentBalance);
                    textList.add(rentStart);
                    textList.add(rentDue);
                }
                if (minTime != null) {
                    textList.add(minTime);
                }
                if (maxTime != null) {
                    textList.add(maxTime);
                }
                if (transactions != null) {
                    textList.add(transactions);
                }
                Component rentClaim = null;
                if (player != null && claim.getEconomyData().isForRent() && !player.getUniqueId().equals(ownerUniqueId) && player.hasPermission(GDPermissions.USER_RENT_BASE)) {
                    Component rentInfo = TextComponent.builder()
                            .append(MessageCache.getInstance().LABEL_PRICE.color(TextColor.AQUA))
                            .append(" : ", TextColor.WHITE)
                            .append(String.valueOf(claim.getEconomyData().getRentRate() + " per " + (claim.getEconomyData().getPaymentType() == PaymentType.DAILY ? "day" : "hour")), TextColor.GOLD)
                            .build();
                    rentClaim = TextComponent.builder()
                        .append(TextComponent.builder("[").append(MessageCache.getInstance().RENT_UI_CLICK_RENT.color(TextColor.GREEN)).append("]", TextColor.WHITE).build())
                        .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(EconomyUtil.getInstance().rentClaimConsumerConfirmation(player, claim))))
                        .hoverEvent(HoverEvent.showText(player.getUniqueId().equals(claim.getOwnerUniqueId()) ? MessageCache.getInstance().CLAIM_OWNER_ALREADY : rentInfo)).build();
                    textList.add(rentClaim);
                }


                int fillSize = 20 - (textList.size() + 2);
                Component footer = null;
                if (player != null && player.hasPermission(GDPermissions.CHAT_CAPTURE)) {
                    footer = ChatCaptureUtil.getInstance().createRecordChatComponent(player, claim, playerData, "claimrent info");
                    fillSize = 20 - (textList.size() + 3);
                }

                for (int i = 0; i < fillSize; i++) {
                    textList.add(TextComponent.of(" "));
                }

                Component header = TextComponent.builder()
                        .append(PlainComponentSerializer.INSTANCE.serialize(MessageCache.getInstance().RENT_UI_INFO_HEADER), TextColor.AQUA)
                        .build();
                PaginationList.Builder paginationBuilder = PaginationList.builder()
                        .title(header).padding(TextComponent.of(" ").decoration(TextDecoration.STRIKETHROUGH, true)).contents(textList).footer(footer);
                paginationBuilder.sendTo(player);
            }
        }
    }
}
