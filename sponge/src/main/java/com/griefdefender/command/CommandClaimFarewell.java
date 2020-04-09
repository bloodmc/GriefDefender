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
import co.aikar.commands.annotation.Syntax;
import com.google.common.collect.ImmutableMap;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.permission.GDPermissions;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.spongeapi.TextAdapter;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import org.spongepowered.api.entity.living.player.Player;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_SET_CLAIM_FAREWELL)
public class CommandClaimFarewell extends BaseCommand {

    @CommandAlias("claimfarewell")
    @Description("Sets the farewell message of your claim.")
    @Syntax("<message>|clear")
    @Subcommand("claim farewell")
    public void execute(Player player, String message) {
        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        final GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(playerData, player.getLocation());
        if (claim.allowEdit(player) != null) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().PERMISSION_EDIT_CLAIM);
            return;
        }

        TextComponent farewell = LegacyComponentSerializer.legacy().deserialize(message, '&');
        if (farewell == TextComponent.empty() || farewell.content().equals("clear")) {
            claim.getInternalClaimData().setFarewell(null);
        } else {
            claim.getInternalClaimData().setFarewell(farewell);
        }
        claim.getInternalClaimData().setRequiresSave(true);
        claim.getInternalClaimData().save();
        Component resultMessage = null;
        if (!claim.getInternalClaimData().getFarewell().isPresent()) {
            resultMessage = MessageCache.getInstance().CLAIM_FAREWELL_CLEAR;
        } else {
            resultMessage = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_FAREWELL,
                    ImmutableMap.of(
                    "farewell", farewell));
        }
        TextAdapter.sendComponent(player, resultMessage);
    }
}
