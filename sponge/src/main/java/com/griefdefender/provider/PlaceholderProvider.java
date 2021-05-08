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

import java.util.List;
import java.util.Set;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.World;

import com.griefdefender.GDBootstrap;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.api.claim.ClaimTypes;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.internal.util.VecHelper;
import com.griefdefender.permission.GDPermissionUser;

import me.rojo8399.placeholderapi.Placeholder;
import me.rojo8399.placeholderapi.PlaceholderService;
import me.rojo8399.placeholderapi.Source;
import me.rojo8399.placeholderapi.Token;
import net.kyori.text.Component;
import net.kyori.text.serializer.plain.PlainComponentSerializer;

public class PlaceholderProvider {

    private PlaceholderService service;
    private static String CLAIM_ADMIN = "claim_admin";
    private static String CLAIM_BASIC = "claim_basic";
    private static String CLAIM_SUBDIVISION = "claim_subdivision";
    private static String CLAIM_TOWN = "claim_town";
    private static String CLAIMS_ADMIN = "claims_admin";
    private static String CLAIMS_BASIC = "claims_basic";
    private static String CLAIMS_SUBDIVISION = "claims_subdivision";
    private static String CLAIMS_TOWN = "claims_town";
    private static String CLAIM_TOWN_TAG = "claim_town_tag";
    private static String CLAIMS_TOWN_BASIC = "claims_town_basic";
    private static String CLAIMS_TOWN_SUBDIVISION = "claims_town_subdivision";
    private static String CLAIM_FOR_RENT = "claim_for_rent";
    private static String CLAIM_FOR_SALE = "claim_for_sale";
    private static String CLAIM_NAME = "claim_name";
    private static String CLAIM_OWNER = "claim_owner";
    private static String CLAIM_TRUST = "claim_trust";
    private static String CLAIM_TYPE = "claim_type";
    private static String PVP = "pvp";
    private static String PVP_CLAIM = "pvp_claim";
    private static String PVP_COMBAT_ACTIVE = "pvp_combat_active";
    private static String BLOCKS_TOTAL = "blocks_total";
    private static String BLOCKS_LEFT = "blocks_left";
    private static String BLOCKS_ACCRUED_RATE = "blocks_accrued_rate";
    private static String BLOCKS_ACCRUED_MAX = "blocks_accrued_max";

    public PlaceholderProvider() {
        this.service = Sponge.getServiceManager().provideUnchecked(PlaceholderService.class);
        this.service.loadAll(this, GDBootstrap.getInstance()).stream().map(builder -> {
            switch (builder.getId()) {
                case "griefdefender":
                    return builder.tokens(CLAIM_ADMIN, CLAIM_BASIC, CLAIM_SUBDIVISION, CLAIM_TOWN,
                                        CLAIMS_ADMIN, CLAIMS_BASIC, CLAIMS_SUBDIVISION, CLAIMS_TOWN,
                                        CLAIM_TOWN_TAG, CLAIMS_TOWN_BASIC, CLAIMS_TOWN_SUBDIVISION,
                                        CLAIM_FOR_RENT, CLAIM_FOR_SALE, CLAIM_NAME, CLAIM_OWNER,
                                        CLAIM_TRUST, CLAIM_TYPE, PVP, PVP_CLAIM, PVP_COMBAT_ACTIVE,
                                        BLOCKS_TOTAL, BLOCKS_LEFT, BLOCKS_ACCRUED_RATE,
                                        BLOCKS_ACCRUED_MAX).description("GriefDefender Placeholders");
            }
            return builder;
        }).map(builder -> builder.plugin(GDBootstrap.getInstance()).author("bloodmc").version("1.0")).forEach(builder -> {
        try {
            builder.buildAndRegister();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    @Placeholder(id = "griefdefender")
    public Object griefdefender(@Source Player player, @Token(fix = true) String identifier) {
        final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(player);
        final GDPlayerData playerData = user.getInternalPlayerData();
        final World world = player.getWorld();
        final Claim claim = GriefDefender.getCore().getClaimManager(world.getUniqueId()).getClaimAt(VecHelper.toVector3i(player.getLocation()));

        switch (identifier) {
            case "claim_admin" :
                return this.getAdminClaimsInside(player, claim);
            case "claim_basic" :
                return this.getPlayerClaimsInside(player, claim, ClaimTypes.BASIC);
            case "claim_subdivision" :
                return this.getPlayerClaimsInside(player, claim, ClaimTypes.SUBDIVISION);
            case "claim_town" :
                return this.getPlayerClaimsInside(player, claim, ClaimTypes.TOWN);
            case "claims_admin" :
                return this.getAllAdminClaims(player);
            case "claims_basic" :
                return this.getAllPlayerClaims(playerData, ClaimTypes.BASIC);
            case "claims_subdivision" :
                return this.getAllPlayerClaims(playerData, ClaimTypes.SUBDIVISION);
            case "claims_town" :
                return this.getAllPlayerClaims(playerData, ClaimTypes.TOWN);
            case "claim_town_tag" :
                if (claim == null || !playerData.inTown) {
                    return "";
                }
                final GDClaim town = (GDClaim) claim.getTown().orElse(null);
                if (town == null) {
                    return "";
                }
                final Component tag = town.getTownData().getTownTag().orElse(null);
                if (tag == null) {
                    return "";
                }
                return PlainComponentSerializer.INSTANCE.serialize(tag);
            case "claims_town_basic" :
                return this.getAllTownChildrenClaims(playerData, ClaimTypes.BASIC);
            case "claims_town_subdivision" :
                return this.getAllTownChildrenClaims(playerData, ClaimTypes.SUBDIVISION);
            case "claim_for_rent" :
                if (claim == null || GriefDefenderPlugin.getInstance().getEconomyService() == null || claim.getEconomyData() == null) {
                    return "false";
                }
                return String.valueOf(claim.getEconomyData().isForRent());
            case "claim_for_sale" :
                if (claim == null || GriefDefenderPlugin.getInstance().getEconomyService() == null || claim.getEconomyData() == null) {
                    return "false";
                }
                return String.valueOf(claim.getEconomyData().isForSale());
            case "claim_name" :
                if (claim == null) {
                    return "";
                }
                return ((GDClaim) claim).getFriendlyName();
            case "claim_owner" :
                if (claim == null) {
                    return "";
                }
                if (claim.isWilderness()) {
                    return "wilderness";
                }
                return ((GDClaim) claim).getOwnerName();
            case "claim_trust" : 
                if (claim == null) {
                    return "";
                }
                return String.valueOf(claim.isUserTrusted(player.getUniqueId(), TrustTypes.ACCESSOR));
            case "claim_type" :
                if (claim == null) {
                    return "";
                }
                return claim.getType().getName();
            case "pvp" : 
            if (claim == null) {
                return "";
            }
            return String.valueOf(playerData.canPvp(claim));
            case "pvp_claim" :
                if (claim == null) {
                    return "";
                }
                return String.valueOf(claim.isPvpAllowed());
            case "pvp_combat_active" :
                if (claim == null) {
                    return "";
                }
                return String.valueOf(playerData.getPvpCombatTimeRemaining((GDClaim) claim) > 0);
            case "blocks_total" :
                final int initial = playerData.getInitialClaimBlocks();
                final int accrued = playerData.getAccruedClaimBlocks();
                final int bonus = playerData.getBonusClaimBlocks();
                return String.valueOf(initial + accrued + bonus);
            case "blocks_left" :
                return String.valueOf(playerData.getRemainingClaimBlocks());
            case "blocks_accrued_rate" :
                return String.valueOf(playerData.getBlocksAccruedPerHour());
            case "blocks_accrued_max" :
                return String.valueOf(playerData.getMaxAccruedClaimBlocks());
            default :
                return null;
        }
    }

    private String getAdminClaimsInside(Player player, Claim currentClaim) {
        if (player == null) {
            return "0";
        }
        int count = 0;
        if (currentClaim.isWilderness()) {
            return this.getAllAdminClaims(player);
        }
        for (Claim claim : currentClaim.getChildren(true)) {
            if (claim.isAdminClaim()) {
                count++;
            }
        }
        return String.valueOf(count);
    }

    private String getAllAdminClaims(Player player) {
        if (player == null) {
            return "0";
        }

        int count = 0;
        final Set<Claim> claimList = GriefDefender.getCore().getClaimManager(player.getWorld().getUniqueId()).getWorldClaims();
        for (Claim claim : claimList) {
            if (claim.isAdminClaim()) {
                count++;
            }
        }
        return String.valueOf(count);
    }

    private String getPlayerClaimsInside(Player player, Claim currentClaim, ClaimType type) {
        if (player == null || currentClaim == null) {
            return "0";
        }
        int count = 0;
        if (currentClaim.isWilderness()) {
            return String.valueOf(GriefDefender.getCore().getAllPlayerClaims(player.getUniqueId()).size());
        }
        for (Claim claim : currentClaim.getChildren(true)) {
            if (claim.getType() == type) {
                count++;
            }
        }
        return String.valueOf(count);
    }

    private String getAllPlayerClaims(GDPlayerData playerData, ClaimType type) {
        if (playerData == null) {
            return "0";
        }
        int count = 0;
        final List<Claim> claimList = GriefDefender.getCore().getAllPlayerClaims(playerData.playerID);
        for (Claim claim : claimList) {
            if (claim.getType() == type) {
                count++;
            }
        }
        return String.valueOf(count);
    }

    private String getAllTownChildrenClaims(GDPlayerData playerData, ClaimType subType) {
        if (playerData == null) {
            return "0";
        }
        int count = 0;
        final List<Claim> claimList = GriefDefender.getCore().getAllPlayerClaims(playerData.playerID);
        for (Claim claim : claimList) {
            if (!claim.isTown() && claim.isInTown() && claim.getType() == subType) {
                count++;
            }
        }
        return String.valueOf(count);
    }
}
