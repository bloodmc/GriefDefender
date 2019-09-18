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
import com.griefdefender.GDBootstrap;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.text.action.GDCallbackHolder;

import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;

import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerTickTask extends BukkitRunnable {

    public PlayerTickTask() {
        this.runTaskTimer(GDBootstrap.getInstance(), 1L, 1L);
    }

    @Override
    public void run() {
        for (World world : Bukkit.getServer().getWorlds()) {
            for (Player player : world.getPlayers()) {
                if (player.isDead()) {
                    continue;
                }
                final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
                final GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(playerData, player.getLocation());
                // health regen
                if (world.getFullTime() % 100 == 0L) {
                    final GameMode gameMode = player.getGameMode();
                    // Handle player health regen
                    if (gameMode != GameMode.CREATIVE && gameMode != GameMode.SPECTATOR) {
                        final double maxHealth = player.getMaxHealth();
                        if (player.getHealth() < maxHealth) {
                            final double regenAmount = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Double.class), playerData.getSubject(), Options.PLAYER_HEALTH_REGEN, claim);
                            if (regenAmount > 0) {
                                final double newHealth = player.getHealth() + regenAmount;
                                if (newHealth > maxHealth) {
                                    player.setHealth(maxHealth);
                                } else {
                                    player.setHealth(newHealth);
                                }
                            }
                        }
                    }
                }
                // teleport delay
                if (world.getFullTime() % 20 == 0L) {
                    if (playerData.teleportDelay > 0) {
                        final int delay = playerData.teleportDelay - 1;
                        if (delay == 0) {
                            player.teleport(playerData.teleportLocation);
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
