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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimBlockSystem;
import com.griefdefender.api.claim.ClaimTypes;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.internal.pagination.PaginationList;
import com.griefdefender.internal.util.NMSUtil;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.storage.BaseStorage;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_PLAYER_INFO_BASE)
public class CommandPlayerInfo extends BaseCommand {

    @CommandCompletion("@gdplayers @gddummy")
    @CommandAlias("gdplayerinfo|playerinfo")
    @Description("%player-info")
    @Syntax("[<player>|<player> <world>]")
    @Subcommand("player info")
    public void execute(CommandSender src, @Optional String[] args) throws InvalidCommandArgument {
        GDPermissionUser user = null;
        World world = null;
        if (args.length > 0) {
            user = PermissionHolderCache.getInstance().getOrCreateUser(args[0]);
            if (user == null) {
                TextAdapter.sendComponent(src, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.COMMAND_PLAYER_NOT_FOUND,
                        ImmutableMap.of("player", args[0])));
                return;
            }
            if (args.length > 1) {
                world = Bukkit.getServer().getWorld(args[1]);
                if (world == null) {
                    TextAdapter.sendComponent(src, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.COMMAND_WORLD_NOT_FOUND,
                            ImmutableMap.of("world", args[1])));
                    return;
                }
            }
        }

        if (user == null) {
            if (!(src instanceof Player)) {
                GriefDefenderPlugin.sendMessage(src, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.COMMAND_INVALID_PLAYER));
                return;
            }

            user = PermissionHolderCache.getInstance().getOrCreateUser((OfflinePlayer) src);
            if (world == null) {
                world = ((Player) src).getWorld();
            }
        }
        if (world == null) {
            world = Bukkit.getServer().getWorlds().get(0);
        }

        // otherwise if no permission to delve into another player's claims data or self
        if (user != null && user.getOnlinePlayer() != src && !src.hasPermission(GDPermissions.COMMAND_PLAYER_INFO_OTHERS)) {
           TextAdapter.sendComponent(src, MessageCache.getInstance().PERMISSION_PLAYER_VIEW_OTHERS);
           return;
        }


        GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(world.getUID(), user.getUniqueId());
        boolean useGlobalData = BaseStorage.USE_GLOBAL_PLAYER_STORAGE;
        List<Claim> claimList = new ArrayList<>();
        for (Claim claim : playerData.getInternalClaims()) {
            if (useGlobalData) {
                claimList.add(claim);
            } else {
                if (claim.getWorldUniqueId().equals(world.getUID())) {
                    claimList.add(claim);
                }
            }
        }
        Component claimSizeLimit = TextComponent.of("none", TextColor.GRAY);
        if (playerData.getMaxClaimX(ClaimTypes.BASIC) != 0 || playerData.getMaxClaimY(ClaimTypes.BASIC) != 0 || playerData.getMaxClaimZ(ClaimTypes.BASIC) != 0) {
            claimSizeLimit = TextComponent.of(playerData.getMaxClaimX(ClaimTypes.BASIC) + "," + playerData.getMaxClaimY(ClaimTypes.BASIC) + "," + playerData.getMaxClaimZ(ClaimTypes.BASIC), TextColor.GRAY);
        }

        final double claimableChunks = GriefDefenderPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.VOLUME ? (playerData.getRemainingClaimBlocks() / 65536.0) : (playerData.getRemainingClaimBlocks() / 256.0);
        final Component uuidText = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_UUID, 
                ImmutableMap.of("id", user.getUniqueId().toString()));
        final Component worldText = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_WORLD, 
                ImmutableMap.of("name", world.getName()));
        final Component sizeLimitText = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_CLAIM_SIZE_LIMIT, 
                ImmutableMap.of("limit", claimSizeLimit));
        final Component initialBlockText = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_BLOCK_INITIAL, 
                ImmutableMap.of("amount", String.valueOf(playerData.getInitialClaimBlocks())));
        final Component accruedBlockText = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_BLOCK_ACCRUED, 
                ImmutableMap.of(
                    "amount", String.valueOf(playerData.getAccruedClaimBlocks()),
                    "block_amount", String.valueOf(playerData.getBlocksAccruedPerHour())));
        final Component maxAccruedBlockText = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_BLOCK_MAX_ACCRUED, 
                ImmutableMap.of("amount", String.valueOf(playerData.getMaxAccruedClaimBlocks())));
        final Component bonusBlockText = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_BLOCK_BONUS, 
                ImmutableMap.of("amount", String.valueOf(playerData.getBonusClaimBlocks())));
        final Component remainingBlockText = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_BLOCK_REMAINING, 
                ImmutableMap.of("amount", String.valueOf(playerData.getRemainingClaimBlocks())));
        final Component economyBlockAvailablePurchaseText = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_ECONOMY_BLOCK_AVAILABLE_PURCHASE, 
                ImmutableMap.of("amount", String.valueOf(playerData.getRemainingClaimBlocks())));
        final Component economyBlockCostText = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_ECONOMY_BLOCK_COST, 
                ImmutableMap.of("amount", "$" + String.format("%.2f", playerData.getInternalEconomyBlockCost())));
        final Component economyBlockSellReturnText = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_ECONOMY_BLOCK_SELL_RETURN, 
                ImmutableMap.of("amount", "$" + String.format("%.2f", playerData.getEconomyClaimBlockReturn())));
        final Component minMaxLevelText = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_CLAIM_LEVEL, 
                ImmutableMap.of("level", String.valueOf(playerData.getMinClaimLevel() + "-" + playerData.getMaxClaimLevel())));
        final Component abandonRatioText = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_ABANDON_RETURN_RATIO, 
                ImmutableMap.of("ratio", String.valueOf(playerData.getAbandonedReturnRatio(ClaimTypes.BASIC))));
        final Component totalTaxText = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_TAX_TOTAL, 
                ImmutableMap.of("amount", "$" + String.format("%.2f", playerData.getTotalTax())));
        final Component totalBlockText = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_BLOCK_TOTAL, 
                ImmutableMap.of("amount", String.valueOf(playerData.getInitialClaimBlocks() + playerData.getAccruedClaimBlocks() + playerData.getBonusClaimBlocks())));
        final Component totalClaimableChunkText = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_CHUNK_TOTAL, 
                ImmutableMap.of("amount", String.valueOf(Math.round(claimableChunks * 100.0)/100.0)));
        final Component totalClaimText = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_CLAIM_TOTAL, 
                ImmutableMap.of("amount", claimList.size(),
                                "block_amount", playerData.getTotalClaimsCost()));

        List<Component> claimsTextList = Lists.newArrayList();
        claimsTextList.add(uuidText);
        claimsTextList.add(worldText);
        claimsTextList.add(sizeLimitText);
        final int townLimit = playerData.getCreateClaimLimit(ClaimTypes.TOWN);
        final int basicLimit = playerData.getCreateClaimLimit(ClaimTypes.BASIC);
        final int subLimit = playerData.getCreateClaimLimit(ClaimTypes.SUBDIVISION);
        String townLimitText = townLimit < 0 ? "∞" : String.valueOf(townLimit);
        String basicLimitText = basicLimit < 0 ? "∞" : String.valueOf(basicLimit);
        String subLimitText = subLimit < 0 ? "∞" : String.valueOf(subLimit);
        Component claimCreateLimits = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_CLAIM_CREATION_LIMIT, 
                ImmutableMap.of("basic_limit", basicLimitText,
                                "sub_limit", subLimitText,
                                "town_limit", townLimitText));
        claimsTextList.add(claimCreateLimits);
        claimsTextList.add(initialBlockText);
        claimsTextList.add(accruedBlockText);
        claimsTextList.add(bonusBlockText);
        claimsTextList.add(totalBlockText);
        if (GriefDefenderPlugin.getInstance().isEconomyModeEnabled()) {
            claimsTextList.add(economyBlockCostText);
            claimsTextList.add(economyBlockSellReturnText);
            claimsTextList.add(economyBlockAvailablePurchaseText);
        } else {
            claimsTextList.add(remainingBlockText);
        }
        claimsTextList.add(maxAccruedBlockText);
        claimsTextList.add(minMaxLevelText);
        claimsTextList.add(abandonRatioText);

        if (GriefDefenderPlugin.getActiveConfig(world.getUID()).getConfig().economy.taxSystem) {
            Component townTaxRate = TextComponent.builder("")
                    .append("TOWN", TextColor.GRAY)
                    .append(" : ")
                    .append(String.valueOf(playerData.getTaxRate(ClaimTypes.TOWN)), TextColor.GREEN)
                    .build();
                    // TODO
                    //TextColors.GRAY, " BASIC", TextColors.WHITE, " : ", TextColors.GREEN, playerData.optionTaxRateTownBasic, 
                    //TextColors.GRAY, " SUB", TextColors.WHITE, " : ", TextColors.GREEN, playerData.getTaxRate(type));
            Component claimTaxRate = TextComponent.builder("")
                    .append("BASIC", TextColor.GRAY)
                    .append(" : ")
                    .append(String.valueOf(playerData.getTaxRate(ClaimTypes.BASIC)), TextColor.GREEN)
                    .append(" SUB", TextColor.GRAY)
                    .append(" : ")
                    .append(String.valueOf(playerData.getTaxRate(ClaimTypes.SUBDIVISION)), TextColor.GREEN)
                    .build();
            String taxRate = "N/A";
            if (src instanceof Player) {
                Player player = (Player) src;
                if (player.getUniqueId().equals(user.getUniqueId())) {
                    final GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAt(player.getLocation());
                    if (claim != null && !claim.isWilderness()) {
                        final double playerTaxRate = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Double.class), user, Options.TAX_RATE, claim);
                        taxRate = String.valueOf(playerTaxRate);
                    }
                }
            }
            final Component currentTaxRateText = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_TAX_CURRENT_RATE, 
                    ImmutableMap.of("rate", taxRate));
            final Component globalTownTaxText = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_TAX_GLOBAL_TOWN_RATE, 
                    ImmutableMap.of("rate", townTaxRate));
            final Component globalClaimTaxText = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_TAX_GLOBAL_CLAIM_RATE, 
                    ImmutableMap.of("rate", claimTaxRate));
            claimsTextList.add(currentTaxRateText);
            claimsTextList.add(globalTownTaxText);
            claimsTextList.add(globalClaimTaxText);
            claimsTextList.add(totalTaxText);
        }
        claimsTextList.add(totalClaimableChunkText);
        claimsTextList.add(totalClaimText);

        if (NMSUtil.getInstance().getLastLogin(user.getOfflinePlayer()) != 0) {
            Date lastActive = null;
            try {
                lastActive = new Date(NMSUtil.getInstance().getLastLogin(user.getOfflinePlayer()));
            } catch(DateTimeParseException ex) {
                // ignore
            }
            if (lastActive != null) {
                claimsTextList.add(MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_LAST_ACTIVE, 
                        ImmutableMap.of("date", lastActive)));
            }
        }

        final Component title = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.PLAYERINFO_UI_TITLE, 
                ImmutableMap.of("name", playerData.getName()));
        PaginationList.Builder paginationBuilder = PaginationList.builder()
                .title(title).padding(TextComponent.of(" ").decoration(TextDecoration.STRIKETHROUGH, true)).contents(claimsTextList);
        paginationBuilder.sendTo(src);
    }
}
