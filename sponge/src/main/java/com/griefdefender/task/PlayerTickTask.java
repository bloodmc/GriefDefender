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
package com.griefdefender.task;

import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.permission.GDPermissionManager;

import net.kyori.text.TextComponent;
import net.kyori.text.adapter.spongeapi.TextAdapter;
import net.kyori.text.format.TextColor;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.world.World;

public class PlayerTickTask implements Runnable {

    public PlayerTickTask() {

    }

    @Override
    public void run() {
        for (World world : Sponge.getServer().getWorlds()) {
            for (Player player : world.getPlayers()) {
                if (player.isRemoved()) {
                    continue;
                }
                final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
                final GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(playerData, player.getLocation());
                // health regen
                if (world.getProperties().getTotalTime() % 100 == 0L) {
                    final GameMode gameMode = player.get(Keys.GAME_MODE).get();
                    // Handle player health regen
                    if (gameMode != GameModes.CREATIVE && gameMode != GameModes.SPECTATOR) {
                        final double maxHealth = player.get(Keys.MAX_HEALTH).get();
                        final double currentHealth = player.get(Keys.HEALTH).get();
                        if (currentHealth < maxHealth) {
                            final double regenAmount = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Double.class), playerData.getSubject(), Options.PLAYER_HEALTH_REGEN, claim);
                            if (regenAmount > 0) {
                                final double newHealth = currentHealth + regenAmount;
                                if (newHealth > maxHealth) {
                                    player.offer(Keys.MAX_HEALTH, maxHealth);
                                } else {
                                    player.offer(Keys.HEALTH, newHealth);
                                }
                            }
                        }
                    }
                }
                // teleport delay
                if (world.getProperties().getTotalTime() % 20 == 0L) {
                    if (playerData.teleportDelay > 0) {
                        final int delay = playerData.teleportDelay - 1;
                        if (delay == 0) {
                            player.setLocation(playerData.teleportLocation);
                            playerData.teleportDelay = 0;
                            playerData.teleportLocation = null;
                            playerData.teleportSourceLocation = null;
                            continue;
                        }
                        TextAdapter.sendComponent(player, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.TELEPORT_DELAY_NOTICE, 
                                ImmutableMap.of("delay", TextComponent.of(delay, TextColor.GOLD))));
                        playerData.teleportDelay = delay;
                    }
                }
            }
        }
    }
}
