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
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.claim.ClaimContexts;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.option.Option;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.internal.pagination.PaginationList;
import com.griefdefender.permission.GDPermissionHolder;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.registry.OptionRegistryModule;
import com.griefdefender.util.CauseContextHelper;
import com.griefdefender.util.PermissionUtil;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.spongeapi.TextAdapter;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.entity.living.player.Player;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_OPTIONS_CLAIM)
public class CommandClaimOption extends ClaimOptionBase {

    public CommandClaimOption() {
        super(ClaimSubjectType.GLOBAL);
    }

    @CommandCompletion("@gdoptions @gddummy")
    @CommandAlias("co|claimoption")
    @Description("Gets/Sets claim options in the claim you are standing in.")
    @Syntax("[<option> <value> [context[key=value]]")
    @Subcommand("option claim")
    public void execute(Player player, @Optional String[] args) throws InvalidCommandArgument {
        this.subject = GriefDefenderPlugin.DEFAULT_HOLDER;
        this.friendlySubjectName = "ALL";
        super.execute(player, args);
    }
}
