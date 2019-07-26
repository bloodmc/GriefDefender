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
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.claim.ShovelTypes;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.internal.util.NMSUtil;
import com.griefdefender.permission.GDPermissions;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.format.TextColor;
import org.bukkit.entity.Player;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_RESTORE_NATURE)
public class CommandRestoreNature extends BaseCommand {

    @CommandAlias("modenature")
    @Description("Switches the shovel tool to restoration mode.")
    @Subcommand("mode nature")
    public void execute(Player player) {
        if (true) {
            GriefDefenderPlugin.sendMessage(player, TextComponent.of("This mode is currently being worked on and will be available in a future build.", TextColor.RED));
            return;
        }

        if (!NMSUtil.getInstance().hasItemInOneHand(player, GriefDefenderPlugin.getInstance().modificationTool)) {
            TextAdapter.sendComponent(player, TextComponent.builder("")
                    .append("You do not have ", TextColor.RED)
                    .append(GriefDefenderPlugin.getInstance().modificationTool.name().toLowerCase(), TextColor.GREEN)
                    .append(" equipped.", TextColor.RED).build());
            return;
        }

        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        playerData.shovelMode = ShovelTypes.RESTORE;
        GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_RESTORE_NATURE_ACTIVATE));
    }
}
