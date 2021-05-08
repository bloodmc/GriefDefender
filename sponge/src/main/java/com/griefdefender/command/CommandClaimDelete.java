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
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimResult;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.text.action.GDCallbackHolder;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.spongeapi.TextAdapter;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;

import java.util.function.Consumer;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_DELETE_CLAIMS)
public class CommandClaimDelete extends BaseCommand {

    protected boolean deleteTopLevelClaim = false;

    @CommandAlias("deleteclaim")
    @Description("%delete-claim")
    @Subcommand("delete claim")
    public void execute(Player player) {
        final GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAt(player.getLocation());
        if (claim.isWilderness()) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CLAIM_NOT_FOUND);
            return;
        }

        final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_CLAIM_DELETE,
                ImmutableMap.of(
                "type", claim.getType().getName()));

        if (claim.isAdminClaim() && !player.hasPermission(GDPermissions.DELETE_CLAIM_ADMIN)) {
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }
        if (claim.isBasicClaim() && !player.hasPermission(GDPermissions.DELETE_CLAIM_BASIC)) {
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }

        final Component confirmationText = TextComponent.builder()
                .append(GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.DELETE_CLAIM_WARNING, 
                        ImmutableMap.of("player", claim.getOwnerDisplayName().color(TextColor.AQUA))))
                .append(TextComponent.builder()
                    .append("\n[")
                    .append(MessageCache.getInstance().LABEL_CONFIRM.color(TextColor.GREEN))
                    .append("]\n")
                    .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(player, createConfirmationConsumer(player, claim, deleteTopLevelClaim), true)))
                    .hoverEvent(HoverEvent.showText(MessageCache.getInstance().UI_CLICK_CONFIRM)).build())
                .build();
        TextAdapter.sendComponent(player, confirmationText);
    }

    private static Consumer<CommandSource> createConfirmationConsumer(Player player, Claim claim, boolean deleteTopLevelClaim) {
        return confirm -> {
            final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
            GDCauseStackManager.getInstance().pushCause(player);
            ClaimResult claimResult = GriefDefenderPlugin.getInstance().dataStore.deleteClaim(claim, !deleteTopLevelClaim);
            GDCauseStackManager.getInstance().popCause();
            if (!claimResult.successful()) {
                GriefDefenderPlugin.sendMessage(player, claimResult.getMessage().orElse(MessageCache.getInstance().PLUGIN_EVENT_CANCEL));
                return;
            }

            playerData.onClaimDelete();
            playerData.revertClaimVisual((GDClaim) claim);

            if (claim.isTown()) {
                playerData.inTown = false;
                playerData.townChat = false;
            }

            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.DELETE_CLAIM_SUCCESS, 
                ImmutableMap.of("player", claim.getOwnerDisplayName().color(TextColor.AQUA)));
            GriefDefenderPlugin.sendMessage(player, message);
        };
    }
}
