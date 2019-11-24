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
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import com.google.common.collect.ImmutableMap;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.permission.GDPermissions;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.spongeapi.TextAdapter;
import net.kyori.text.format.TextColor;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.world.storage.WorldProperties;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_SET_ACCRUED_CLAIM_BLOCKS)
public class CommandAdjustBonusClaimBlocks extends BaseCommand {

    @CommandCompletion("@gdplayers @gddummy")
    @CommandAlias("acb|adjustclaimblocks")
    @Description("Updates a player's accrued claim block total")
    @Syntax("<player> <amount>")
    @Subcommand("player adjustbonusblocks")
    public void execute(CommandSource src, User user, int amount, @Optional String world) {
        WorldProperties worldProperties = world == null ? null : Sponge.getServer().getWorldProperties(world).orElse(null);
        if (worldProperties == null) {
            if (src instanceof Player) {
                worldProperties = ((Player) src).getWorld().getProperties();
            } else {
                worldProperties = Sponge.getServer().getDefaultWorld().get();
            }
        }
        if (worldProperties == null || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(worldProperties.getUniqueId())) {
            GriefDefenderPlugin.sendMessage(src, MessageCache.getInstance().CLAIM_DISABLED_WORLD);
            return;
        }

        // parse the adjustment amount
        int adjustment = amount;
        //User user = args.<User>getOne("user").get();

        // give blocks to player
        GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(worldProperties.getUniqueId(), user.getUniqueId());
        playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks() + adjustment);
        playerData.getStorageData().save();
        final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ADJUST_BONUS_BLOCKS_SUCCESS, ImmutableMap.of(
                "player", user.getName(),
                "amount", adjustment,
                "total", playerData.getBonusClaimBlocks()));
        TextAdapter.sendComponent(src, message);
        GriefDefenderPlugin.getInstance().getLogger().info(
                src.getName() + " adjusted " + user.getName() + "'s bonus claim blocks by " + adjustment + ".");
    }
}
