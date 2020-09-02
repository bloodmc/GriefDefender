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

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;

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
import com.griefdefender.util.PlayerUtil;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.text.Component;
import net.kyori.text.serializer.plain.PlainComponentSerializer;

public class PlaceholderProvider {

    public PlaceholderProvider() {
        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null){
            new GDPlaceholderExpansion().register();
        }
    }

    private class GDPlaceholderExpansion extends PlaceholderExpansion {

        public GDPlaceholderExpansion() {
            
        }

        @Override
        public String onPlaceholderRequest(Player player, String identifier){
            return onRequest(player, identifier);
        }

        @Override
        public String onRequest(OfflinePlayer offlinePlayer, String identifier) {
            final Player player = offlinePlayer instanceof Player ? (Player) offlinePlayer : null;
            final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(offlinePlayer);
            GDPlayerData playerData = null;
            Claim claim = null;
            if (player != null) {
                final World world = player.getWorld();
                claim = GriefDefender.getCore().getClaimManager(world.getUID()).getClaimAt(VecHelper.toVector3i(player.getLocation()));
                playerData = user.getInternalPlayerData();
            } else {
                playerData = user.getInternalPlayerData();
            }
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
                    if (claim == null || GriefDefenderPlugin.getInstance().getVaultProvider() == null || claim.getEconomyData() == null) {
                        return "false";
                    }
                    return String.valueOf(claim.getEconomyData().isForRent());
                case "claim_for_sale" :
                    if (claim == null || GriefDefenderPlugin.getInstance().getVaultProvider() == null || claim.getEconomyData() == null) {
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
                    return String.valueOf(PlayerUtil.getInstance().canPlayerPvP((GDClaim) claim, user));
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

        @Override
        public boolean canRegister(){
            return true;
        }

        @Override
        public String getIdentifier() {
            return GriefDefenderPlugin.MOD_ID.toLowerCase();
        }

        @Override
        public String getVersion() {
            return "0.1";
        }

        @Override
        public String getAuthor(){
            return GDBootstrap.getInstance().getDescription().getAuthors().toString();
        }

        /**
         * Because this is an internal class,
         * you must override this method to let PlaceholderAPI know to not unregister your expansion class when
         * PlaceholderAPI is reloaded
         *
         * @return true to persist through reloads
         */
        @Override
        public boolean persist(){
            return true;
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
            final World world = player == null ? Bukkit.getWorlds().get(0) : player.getWorld();
            int count = 0;
            final Set<Claim> claimList = GriefDefender.getCore().getClaimManager(world.getUID()).getWorldClaims();
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
}
