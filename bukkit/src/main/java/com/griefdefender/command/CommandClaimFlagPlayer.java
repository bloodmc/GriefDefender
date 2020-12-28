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

import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.Tristate;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.util.PermissionUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_FLAGS_PLAYER)
public class CommandClaimFlagPlayer extends ClaimFlagBase  {

    public CommandClaimFlagPlayer() {
        super(ClaimSubjectType.PLAYER);
    }

    @CommandCompletion("@gdplayers @gdflags @gdmcids @gdtristates @gdcontexts @gddummy")
    @CommandAlias("cfp")
    @Description("%flag-player")
    @Syntax("<player> <flag> <target> <value> [context[key=value]]")
    @Subcommand("flag player")
    public void execute(Player src, OfflinePlayer player, @Optional String[] args) throws InvalidCommandArgument {
        this.subject = PermissionHolderCache.getInstance().getOrCreateUser(player);
        this.friendlySubjectName = player.getName();

        if (src.hasPermission(GDPermissions.COMMAND_ADMIN_CLAIMS) && !src.hasPermission(GDPermissions.SET_ADMIN_FLAGS)) {
            GriefDefenderPlugin.sendMessage(src, MessageCache.getInstance().PERMISSION_PLAYER_ADMIN_FLAGS);
            return;
        }

        super.execute(src, args);
    }
}