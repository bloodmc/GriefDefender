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
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.internal.util.NMSUtil;
import com.griefdefender.permission.GDPermissions;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Villager;

import java.util.UUID;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_CLAIM_CLEAR)
public class CommandClaimClear extends BaseCommand {

    @CommandCompletion("@gdentityids @gddummy")
    @CommandAlias("claimclear")
    @Description("%claim-clear")
    @Syntax("<entity_id> [claim_uuid]")
    @Subcommand("claim clear")
    public void execute(Player player, String target, @Optional String claimId) {
        World world = player.getWorld();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUID())) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CLAIM_DISABLED_WORLD);
            return;
        }

        UUID claimUniqueId = null;
        GDClaim targetClaim = null;
        if (claimId == null) {
            targetClaim = GriefDefenderPlugin.getInstance().dataStore.getClaimAt(player.getLocation());
            final Component result = targetClaim.allowEdit(player);
            if (result != null) {
                GriefDefenderPlugin.sendMessage(player, result);
                return;
            }
            claimUniqueId = targetClaim.getUniqueId();
        } else {
            if (!player.hasPermission(GDPermissions.COMMAND_DELETE_CLAIMS)) {
                GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().COMMAND_CLAIMCLEAR_UUID_DENY);
                return;
            }
            try {
                claimUniqueId = UUID.fromString(claimId);
            } catch (IllegalArgumentException e) {
                return;
            }
        }

        if (targetClaim.isWilderness()) {
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_ACTION_NOT_AVAILABLE, 
                    ImmutableMap.of("type", TextComponent.of("the wilderness"))));
            return;
        }

        int count = 0;
        String[] parts = target.split(":");
        if (parts.length > 1) {
            target = parts[1];
        }

        for (Chunk chunk : targetClaim.getChunks()) {
            for (Entity entity : chunk.getEntities()) {
                if (entity instanceof Player) {
                    continue;
                }
                if (entity instanceof Villager || !(entity instanceof LivingEntity)) {
                    continue;
                }
                if (entity instanceof Tameable) {
                    final UUID ownerUniqueId = NMSUtil.getInstance().getTameableOwnerUUID(entity);
                    if (ownerUniqueId != null && !ownerUniqueId.equals(player.getUniqueId())) {
                        continue;
                    }
                }
                LivingEntity livingEntity = (LivingEntity) entity;
    
                String entityName = entity.getType().getName().toLowerCase();
                if (target.equalsIgnoreCase("any") || target.equalsIgnoreCase("all") || target.equalsIgnoreCase("minecraft") || target.equalsIgnoreCase(entityName)) {
                    livingEntity.setHealth(0);
                    count++;
                }
            }
        }

        if (count == 0) {
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.COMMAND_CLAIMCLEAR_NO_ENTITIES,
                    ImmutableMap.of("type", TextComponent.of(target, TextColor.GREEN))));
        } else {
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.COMMAND_CLAIMCLEAR_NO_ENTITIES,
                    ImmutableMap.of(
                        "amount", count,
                        "type", TextComponent.of(target, TextColor.GREEN))));
        }
    }
}
