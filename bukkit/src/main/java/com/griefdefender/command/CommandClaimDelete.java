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
import com.griefdefender.api.claim.ClaimResult;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.util.PermissionUtil;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.bukkit.entity.Player;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_DELETE_CLAIMS)
public class CommandClaimDelete extends BaseCommand {

    protected boolean deleteTopLevelClaim = false;

    @CommandAlias("deleteclaim")
    @Description("Deletes the claim you're standing in, even if it's not your claim.")
    @Subcommand("delete claim")
    public void execute(Player player) {
        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        final GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAt(player.getLocation());
        final boolean isTown = claim.isTown();
        if (claim.isWilderness()) {
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.claimNotFound.toText());
            return;
        }

        final Component message = GriefDefenderPlugin.getInstance().messageData.permissionClaimDelete
                .apply(ImmutableMap.of(
                "type", claim.getType().getName())).build();

        if (claim.isAdminClaim() && !player.hasPermission(GDPermissions.DELETE_CLAIM_ADMIN)) {
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }
        if (claim.isBasicClaim() && !player.hasPermission(GDPermissions.DELETE_CLAIM_BASIC)) {
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }

        if (!this.deleteTopLevelClaim && !claim.isTown() && claim.children.size() > 0 /*&& !playerData.warnedAboutMajorDeletion*/) {
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.claimChildrenWarning.toText());
            return;
        }

        GDCauseStackManager.getInstance().pushCause(player);
        ClaimResult claimResult = GriefDefenderPlugin.getInstance().dataStore.deleteClaim(claim, !this.deleteTopLevelClaim);
        GDCauseStackManager.getInstance().popCause();
        if (!claimResult.successful()) {
            GriefDefenderPlugin.sendMessage(player, claimResult.getMessage().orElse(TextComponent.of("Could not delete claim. A plugin has denied it.").color(TextColor.RED)));
            return;
        }

        PermissionUtil.getInstance().clearPermissions(GriefDefenderPlugin.DEFAULT_HOLDER, claim.getContext());
        GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.claimDeleted.toText());

        playerData.revertActiveVisual(player);

        if (isTown) {
            playerData.inTown = false;
            playerData.townChat = false;
        }
    }
}
