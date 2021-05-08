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

import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.storage.BaseStorage;
import com.griefdefender.util.SpongeUtil;

//import io.github.nucleuspowered.nucleus.api.module.home.event.NucleusHomeEvent;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class NucleusEventHandler {

    private static final BaseStorage DATASTORE = GriefDefenderPlugin.getInstance().dataStore;

    /*@Listener
    public void onSetHome(NucleusHomeEvent.Create event) {
        Location<World> location = event.getLocation().orElse(null);
        if (location == null) {
            return;
        }

        GDClaim claim = DATASTORE.getClaimAt(location);
        if (claim != null && !claim.isWilderness() && !claim.isAdminClaim()) {
            if (!claim.isUserTrusted(event.getUser(), TrustTypes.ACCESSOR)) {
                event.setCancelled(true);
                event.setCancelMessage(SpongeUtil.getSpongeText(TextComponent.of("You must be trusted in order to use /sethome here.").color(TextColor.RED)));
            }
        }
    }*/

}
