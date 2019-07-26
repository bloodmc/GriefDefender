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
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.permission.GDPermissions;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_TOWN_TAG)
public class CommandTownTag extends BaseCommand {

    @CommandAlias("towntag")
    @Description("Sets the tag of your town.")
    @Subcommand("town tag")
    public void execute(Player player, String tag) {
        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        final GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(playerData, player.getLocation());
        if (claim == null || !claim.isInTown()) {
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.TOWN_NOT_IN));
            return;
        }

        final GDClaim town = claim.getTownClaim();
        final Component result = town.allowEdit(player);
        if (result != null) {
            GriefDefenderPlugin.sendMessage(player, result);
            return;
        }

        TextComponent name = LegacyComponentSerializer.legacy().deserialize(tag, '&');
        if (name == TextComponent.empty() || name.content().equals("clear")) {
            town.getTownData().setTownTag(null);
        } else {
            town.getTownData().setTownTag(name);
        }

        town.getInternalClaimData().setRequiresSave(true);
        Component resultMessage = null;
        if (!claim.getTownData().getTownTag().isPresent()) {
            resultMessage = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.TOWN_TAG_CLEAR);
        } else {
            resultMessage = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.TOWN_TAG,
                    ImmutableMap.of(
                    "tag", name));
        }
        TextAdapter.sendComponent(player, resultMessage);
    }
}
