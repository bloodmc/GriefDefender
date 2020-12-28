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
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.permission.GDPermissions;
import org.bukkit.entity.Player;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_CLAIM_BANK)
public class CommandClaimBank extends BaseCommand {

    protected boolean townOnly = false;

    @CommandAlias("claimbank")
    @Description("%claim-bank")
    @Syntax("<withdraw|deposit> <amount>")
    @Subcommand("claim bank")
    public void execute(Player player, @Optional String[] args) throws CommandException {
        if (!GriefDefenderPlugin.getActiveConfig(player.getWorld().getUID()).getConfig().economy.bankSystem) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().BANK_TAX_SYSTEM_DISABLED);
            return;
        }
        GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAt(player.getLocation());
        if (this.townOnly) {
            if (!claim.isInTown()) {
                GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().TOWN_NOT_IN);
                return;
            }
            claim = claim.getTownClaim();
        } else {
            if (claim.isSubdivision() || claim.isAdminClaim()) {
                return;
            }
        }

        if (args.length == 0 || args.length < 2) {
            CommandHelper.displayClaimBankInfo(player, claim);
            return;
        }

        CommandHelper.handleBankTransaction(player, args, claim);
    }
}
