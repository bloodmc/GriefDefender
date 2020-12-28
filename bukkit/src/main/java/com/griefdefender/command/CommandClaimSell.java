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
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;

import com.google.common.collect.ImmutableMap;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.text.action.GDCallbackHolder;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.format.TextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_CLAIM_SELL)
public class CommandClaimSell extends BaseCommand {

    @CommandAlias("claimsell")
    @Description("%sell-claim")
    @Syntax("<amount> | cancel")
    @Subcommand("sell claim")
    public void execute(Player player, String arg) throws InvalidCommandArgument {
        // if economy is disabled, don't do anything
        if (GriefDefenderPlugin.getInstance().getVaultProvider() == null) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().ECONOMY_NOT_INSTALLED);
            return;
        }

        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        final GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAt(player.getLocation());

        if (claim.isWilderness()) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().ECONOMY_CLAIM_NOT_FOR_SALE);
            return;
        }

        if ((claim.isAdminClaim() && !playerData.canManageAdminClaims) || !claim.isAdminClaim() && !playerData.canIgnoreClaim(claim) && !player.getUniqueId().equals(claim.getOwnerUniqueId())) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().PERMISSION_CLAIM_SALE);
            return;
        }

        Double salePrice = null;
        if (arg.equalsIgnoreCase("cancel")) {
            claim.getEconomyData().setForSale(false);
            claim.getEconomyData().setSalePrice(-1);
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().ECONOMY_CLAIM_SALE_CANCELLED);
            return;
        } else {
            try {
                salePrice = Double.parseDouble(arg);
            } catch (NumberFormatException e) {
                throw new InvalidCommandArgument();
            }
        }

        if (salePrice == null || salePrice < 0) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_SALE_INVALID_PRICE,
                    ImmutableMap.of(
                    "amount", salePrice));
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }

        final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_SALE_CONFIRMATION,
                ImmutableMap.of(
                "amount", salePrice));
        GriefDefenderPlugin.sendMessage(player, message);

        final Component saleConfirmationText = TextComponent.builder("")
                .append("\n[")
                .append(MessageCache.getInstance().LABEL_CONFIRM.color(TextColor.GREEN))
                .append("]\n")
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(player, createSaleConfirmationConsumer(player, claim, salePrice), true)))
                .build();
        GriefDefenderPlugin.sendMessage(player, saleConfirmationText);
    }

    private static Consumer<CommandSender> createSaleConfirmationConsumer(CommandSender src, Claim claim, double price) {
        return confirm -> {
            claim.getEconomyData().setSalePrice(price);
            claim.getEconomyData().setForSale(true);
            claim.getData().save();
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_SALE_CONFIRMED,
                    ImmutableMap.of("amount", price));
            GriefDefenderPlugin.sendMessage(src, message);
        };
    }
}
