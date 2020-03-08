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
package com.griefdefender.listener;

import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.claim.GDClaim;
import nl.riebie.mcclans.api.events.ClanCreateEvent;
import nl.riebie.mcclans.api.events.ClanSetHomeEvent;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.World;

public class MCClansEventHandler {

    public void onClanSetHome(ClanSetHomeEvent event) {
        final World world = event.getLocation().getExtent();
        if (!GriefDefenderPlugin.getGlobalConfig().getConfig().town.clanRequireTown || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUniqueId())) {
            return;
        }

        final GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAt(event.getLocation());
        if (!claim.isInTown()) {
            event.setCancelledWithMessage("You must be in a town in order to set your clan home.");
        }
        else if (!claim.getOwnerUniqueId().equals(event.getClan().getOwner().getUUID())) {
            event.setCancelledWithMessage("You do not own this town.");
        }
    }

    public void onClanCreate(ClanCreateEvent event) {
        if (!GriefDefenderPlugin.getGlobalConfig().getConfig().town.clanRequireTown) {
            return;
        }

        final Player player = Sponge.getServer().getPlayer(event.getOwner().getUUID()).orElse(null);
        if (player == null) {
            return;
        }

        final World world = player.getWorld();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUniqueId())) {
            return;
        }

        final GDPlayerData playerData  = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(world, player.getUniqueId());
        for (Claim claim : playerData.getInternalClaims()) {
            if (claim.isTown()) {
                return;
            }
        }
        event.setCancelledWithMessage("You must own a town in order to create a clan.");
    }
}
