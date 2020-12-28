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
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.permission.GDPermissions;
import net.kyori.text.Component;
import org.spongepowered.api.entity.living.player.Player;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_IGNORE_CLAIMS)
public class CommandClaimIgnore extends BaseCommand {

    @CommandAlias("claimignore|ignoreclaims|ic")
    @Description("%claim-ignore")
    @Subcommand("claim ignore")
    public void execute(Player player) {
        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        final GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAt(player.getLocation());
        if (claim.isBasicClaim() && !playerData.ignoreBasicClaims || claim.isWilderness() && !playerData.ignoreWilderness || claim.isAdminClaim() && !playerData.ignoreAdminClaims) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_CLAIM_IGNORE, ImmutableMap.of(
                    "type", claim.getType().getName()));
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }

        playerData.ignoreClaims = !playerData.ignoreClaims;

        if (!playerData.ignoreClaims) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CLAIM_RESPECTING);
        } else {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CLAIM_IGNORE);
        }
    }
}
