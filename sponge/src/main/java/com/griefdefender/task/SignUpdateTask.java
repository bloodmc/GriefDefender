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
package com.griefdefender.task;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.flowpowered.math.vector.Vector3i;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.claim.GDClaimManager;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.util.SignUtil;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.World;

public class SignUpdateTask implements Runnable {

    public SignUpdateTask() {

    }

    @Override
    public void run() {
        for (World world : Sponge.getServer().getWorlds()) {
            final GDClaimManager claimManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(world.getUniqueId());
            Set<Claim> claimList = claimManager.getWorldClaims();
            if (claimList.size() == 0) {
                continue;
            }

            final Iterator<Claim> iterator = new HashSet<>(claimList).iterator();
            while (iterator.hasNext()) {
                final GDClaim claim = (GDClaim) iterator.next();
                this.checkSign(claim, world);
                for (Claim child : claim.getChildren(true)) {
                    this.checkSign((GDClaim) child, world);
                }
            }
        }
    }

    private void checkSign(GDClaim claim, World world) {
        final Vector3i pos = claim.getEconomyData().getRentSignPosition();
        if (pos == null || claim.getEconomyData() == null || claim.getEconomyData().getRentEndDate() == null) {
            return;
        }

        final Sign sign = SignUtil.getSign(world, pos);
        if (SignUtil.isRentSign(claim, sign)) {
            final List<Text> lines = sign.getSignData().asList();
            final String header = lines.get(0).toPlain();
            if (header == null) {
                // Should not happen but just in case
                return;
            }

            final Duration duration = Duration.between(Instant.now(), claim.getEconomyData().getRentEndDate());
            final long seconds = duration.getSeconds();
            if (seconds <= 0) {
                if (claim.getEconomyData().isRented()) {
                    final UUID renterUniqueId = claim.getEconomyData().getRenters().get(0);
                    final GDPermissionUser renter = PermissionHolderCache.getInstance().getOrCreateUser(renterUniqueId);
                    if (renter != null && renter.getOnlinePlayer() != null) {
                        GriefDefenderPlugin.sendMessage(renter.getOnlinePlayer(), MessageCache.getInstance().ECONOMY_CLAIM_RENT_CANCELLED);
                    }
                }
                sign.getLocation().setBlockType(BlockTypes.AIR);
                SignUtil.resetRentData(claim);
                claim.getData().save();
                return;
            }

            final String remainingTime = String.format("%02d:%02d:%02d", duration.toDays(), (seconds % 86400 ) / 3600, (seconds % 3600) / 60);
            final SignData signData = sign.getOrCreate(SignData.class).orElse(null);
            if (signData != null) { 
                signData.addElement(3, Text.of(TextColors.DARK_AQUA, remainingTime));
                sign.offer(signData);
            }
        }
    }
}
