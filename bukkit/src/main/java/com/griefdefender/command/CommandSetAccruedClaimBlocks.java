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
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;

import com.google.common.collect.ImmutableMap;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.configuration.MessageDataConfig;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;
import net.kyori.text.adapter.bukkit.TextAdapter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_SET_ACCRUED_CLAIM_BLOCKS)
public class CommandSetAccruedClaimBlocks extends BaseCommand {

    private MessageDataConfig MESSAGE_DATA = GriefDefenderPlugin.getInstance().messageData;

    @CommandCompletion("@gdplayers @gddummy")
    @CommandAlias("setaccruedblocks")
    @Description("Updates a player's accrued claim block total.")
    @Syntax("<amount>")
    @Subcommand("player setaccruedblocks")
    public void execute(CommandSender src, String[] args) throws InvalidCommandArgument {
        if (GriefDefenderPlugin.getGlobalConfig().getConfig().economy.economyMode) {
            TextAdapter.sendComponent(src, MessageCache.getInstance().COMMAND_NOT_AVAILABLE_ECONOMY);
            return;
        }

        if (args.length < 2) {
            throw new InvalidCommandArgument();
        }

        GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(args[0]);
        int newAmount = Integer.parseInt(args[1]);
        World world = null;
        if (user == null) {
            TextAdapter.sendComponent(src, MESSAGE_DATA.getMessage(MessageStorage.COMMAND_PLAYER_NOT_FOUND,
                    ImmutableMap.of("player", args[0])));
            return;
        }
        if (args.length > 2) {
            world = Bukkit.getServer().getWorld(args[2]);
            if (world == null) {
                TextAdapter.sendComponent(src, MESSAGE_DATA.getMessage(MessageStorage.COMMAND_WORLD_NOT_FOUND,
                        ImmutableMap.of("world", args[2])));
                return;
            }
        }

        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUID())) {
            GriefDefenderPlugin.sendMessage(src, MessageCache.getInstance().CLAIM_DISABLED_WORLD);
            return;
        }

        // set player's blocks
        GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(world.getUID(), user.getUniqueId());
        if (!playerData.setAccruedClaimBlocks(newAmount)) {
            TextAdapter.sendComponent(src, MESSAGE_DATA.getMessage(MessageStorage.PLAYER_ACCRUED_BLOCKS_EXCEEDED,
                    ImmutableMap.of(
                        "player", user.getName(),
                        "total", playerData.getAccruedClaimBlocks(),
                        "amount", newAmount)));
            return;
        }

        playerData.getStorageData().save();
        GriefDefenderPlugin.sendMessage(src, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ADJUST_ACCRUED_BLOCKS_SUCCESS));
    }
}
