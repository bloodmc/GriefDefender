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
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import com.google.common.collect.ImmutableMap;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.claim.GDClaimManager;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.internal.pagination.PaginationList;
import com.griefdefender.permission.GDPermissions;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_CLAIM_BUY)
public class CommandClaimBuy extends BaseCommand {

    @CommandAlias("claimbuy")
    @Description("List all claims available for purchase.\nNote: Requires economy plugin.")
    @Subcommand("buy claim")
    public void execute(Player player) {
        if (GriefDefenderPlugin.getInstance().getVaultProvider() == null) {
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_NOT_INSTALLED));
            return;
        }

        final Economy economy = GriefDefenderPlugin.getInstance().getVaultProvider().getApi();
        if (!economy.hasAccount(player)) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_PLAYER_NOT_FOUND, ImmutableMap.of(
                    "player", player.getName()));
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }

        Set<Claim> claimsForSale = new HashSet<>();
        GDClaimManager claimManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(player.getWorld().getUID());
        for (Claim worldClaim : claimManager.getWorldClaims()) {
            if (worldClaim.isWilderness()) {
                continue;
            }
            if (!worldClaim.isAdminClaim() && worldClaim.getEconomyData().isForSale() && worldClaim.getEconomyData().getSalePrice() > -1) {
                claimsForSale.add(worldClaim);
            }
            for (Claim child : worldClaim.getChildren(true)) {
                if (child.isAdminClaim()) {
                    continue;
                }
                if (child.getEconomyData().isForSale() && child.getEconomyData().getSalePrice() > -1) {
                    claimsForSale.add(child);
                }
            }
        }

        List<Component> claimsTextList = CommandHelper.generateClaimTextList(new ArrayList<Component>(), claimsForSale, player.getWorld().getName(), null, player, CommandHelper.createCommandConsumer(player, "claimbuy", ""), true, false);
        PaginationList.Builder paginationBuilder = PaginationList.builder()
                .title(TextComponent.of("Claims for sale", TextColor.AQUA)).padding(TextComponent.of(" ").decoration(TextDecoration.STRIKETHROUGH, true)).contents(claimsTextList);
        paginationBuilder.sendTo(player);
        return;
    }
}
