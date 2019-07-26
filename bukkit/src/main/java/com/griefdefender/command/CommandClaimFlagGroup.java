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
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import com.google.common.collect.ImmutableMap;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.util.PermissionUtil;
import net.kyori.text.Component;
import org.bukkit.entity.Player;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_FLAGS_GROUP)
public class CommandClaimFlagGroup extends ClaimFlagBase {

    public CommandClaimFlagGroup() {
        super(ClaimSubjectType.GROUP);
    }

    @CommandAlias("cfg")
    @Description("Gets/Sets flag permission for a group in claim you are standing in.")
    @Syntax("<group> <flag> <target> <value> [context[key=value]]")
    @Subcommand("flag group")
    public void execute(Player player, String group, @Optional String[] args) throws InvalidCommandArgument {
        if (args.length < 2 || args.length > 3) {
            throw new InvalidCommandArgument();
        }

        if (!PermissionUtil.getInstance().hasGroupSubject(group)) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.COMMAND_INVALID_GROUP)
                    .apply(ImmutableMap.of(
                    "group", group)).build();
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }

        /*String reason = ctx.<String>getOne("reason").orElse(null);
        Text reasonText = null;
        if (reason != null) {
            reasonText = TextSerializers.FORMATTING_CODE.deserialize(reason);
        }*/

        this.subject = PermissionHolderCache.getInstance().getOrCreateHolder(group);
        this.friendlySubjectName = group;

        super.execute(player, args);
    }
}