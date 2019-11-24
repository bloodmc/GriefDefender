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
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.IEntityOwnable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.Villager;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.World;
import org.spongepowered.common.SpongeImplHooks;
import org.spongepowered.common.entity.SpongeEntityType;

import java.util.UUID;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_CLAIM_CLEAR)
public class CommandClaimClear extends BaseCommand {

    @CommandCompletion("@gdentityids @gddummy")
    @CommandAlias("claimclear")
    @Description("Allows clearing of entities within one or more claims.")
    @Syntax("<entity_id> [claim_uuid]")
    @Subcommand("claim clear")
    public void execute(Player player, String target, @Optional String claimId) {
        World world = player.getWorld();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUniqueId())) {
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
        // Unfortunately this is required until Pixelmon registers their entities correctly in FML
        // If target was not found in registry, assume its a pixelmon animal
        EntityType entityType = Sponge.getRegistry().getType(EntityType.class, target).orElse(null);
        boolean isPixelmonAnimal = target.contains("pixelmon") && entityType == null;

        int count = 0;
        String[] parts = target.split(":");
        String modId = "minecraft";
        EnumCreatureType creatureType = null;
        if (parts.length > 1) {
            modId = parts[0];
            creatureType = NMSUtil.SPAWN_TYPES.get(parts[1]);
        } else {
            creatureType = NMSUtil.SPAWN_TYPES.get(parts[0]);
            if (creatureType == null) {
                target = "minecraft:" + target;
            }
        }

        for (Entity entity : world.getEntities()) {
            net.minecraft.entity.Entity mcEntity = (net.minecraft.entity.Entity) entity;
            if (entity instanceof Villager) {
                continue;
            }
            if (entity instanceof IEntityOwnable) {
                IEntityOwnable ownable = (IEntityOwnable) entity;
                if (ownable.getOwnerId() != null) {
                    continue;
                }
            }

            if (isPixelmonAnimal) {
                if (parts[1].equalsIgnoreCase(mcEntity.getName().toLowerCase())) {
                    GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAt(entity.getLocation());
                    if (claim != null && claim.getUniqueId().equals(claimUniqueId)) {
                        mcEntity.setDead();
                        count++;
                    }
                }
            } else if (creatureType != null && SpongeImplHooks.isCreatureOfType(mcEntity, creatureType)) {
                GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAt(entity.getLocation());
                if (claim != null && claim.getUniqueId().equals(claimUniqueId)) {
                    // check modId
                    String mod = ((SpongeEntityType) entity.getType()).getModId();
                    if (modId.equalsIgnoreCase(mod)) {
                        mcEntity.setDead();
                        count++;
                    }
                }
            } else {
                if (entityType == null || !entityType.equals(((SpongeEntityType) entity.getType()))) {
                    continue;
                }

                GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAt(entity.getLocation());
                if (claim != null && claim.getUniqueId().equals(claimUniqueId)) {
                    mcEntity.setDead();
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
