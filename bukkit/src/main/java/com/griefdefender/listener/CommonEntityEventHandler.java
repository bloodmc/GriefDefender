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
package com.griefdefender.listener;

import java.lang.ref.WeakReference;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GDTimings;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.ChatType;
import com.griefdefender.api.ChatTypes;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.api.permission.flag.Flags;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.event.GDBorderClaimEvent;
import com.griefdefender.internal.registry.ItemTypeRegistryModule;
import com.griefdefender.internal.util.VecHelper;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.permission.flag.GDFlags;
import com.griefdefender.storage.BaseStorage;

import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;

public class CommonEntityEventHandler {

    private static CommonEntityEventHandler instance;

    public static CommonEntityEventHandler getInstance() {
        return instance;
    }

    static {
        instance = new CommonEntityEventHandler();
    }

    private final BaseStorage storage;

    public CommonEntityEventHandler() {
        this.storage = GriefDefenderPlugin.getInstance().dataStore;
    }

    public void onEntityMove(Event event, Location fromLocation, Location toLocation, Entity targetEntity){
        final Vector3i fromPos = VecHelper.toVector3i(fromLocation);
        final Vector3i toPos = VecHelper.toVector3i(toLocation);
        if (fromPos.equals(toPos)) {
            return;
        }
        if ((!GDFlags.ENTER_CLAIM && !GDFlags.EXIT_CLAIM)) {
            return;
        }

        final Player player = targetEntity instanceof Player ? (Player) targetEntity : null;
        final GDPermissionUser user = player != null ? PermissionHolderCache.getInstance().getOrCreateUser(player) : null;
        final World world = targetEntity.getWorld();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUID())) {
            return;
        }
        final boolean enterBlacklisted = GriefDefenderPlugin.isSourceIdBlacklisted(Flags.ENTER_CLAIM.getName(), targetEntity, world.getUID());
        final boolean exitBlacklisted = GriefDefenderPlugin.isSourceIdBlacklisted(Flags.EXIT_CLAIM.getName(), targetEntity, world.getUID());
        if (enterBlacklisted && exitBlacklisted) {
            return;
        }

        GDTimings.ENTITY_MOVE_EVENT.startTiming();

        GDClaim fromClaim = null;
        GDClaim toClaim = this.storage.getClaimAt(toLocation);
        if (user != null) {
            fromClaim = this.storage.getClaimAtPlayer(user.getInternalPlayerData(), fromLocation);
        } else {
            fromClaim = this.storage.getClaimAt(fromLocation);
        }

        if (GDFlags.ENTER_CLAIM && !enterBlacklisted && user != null && user.getInternalPlayerData().lastClaim != null) {
            final GDClaim lastClaim = (GDClaim) user.getInternalPlayerData().lastClaim.get();
            if (lastClaim != null && lastClaim != fromClaim) {
                if (GDPermissionManager.getInstance().getFinalPermission(event, toLocation, toClaim, GDPermissions.ENTER_CLAIM, targetEntity, targetEntity, player, TrustTypes.ACCESSOR, false) == Tristate.FALSE) {
                    Location claimCorner = new Location(toLocation.getWorld(), toClaim.lesserBoundaryCorner.getX(), targetEntity.getLocation().getBlockY(), toClaim.greaterBoundaryCorner.getZ());
                    targetEntity.teleport(claimCorner);
                }
            }
        }
        if (fromClaim == toClaim) {
            GDTimings.ENTITY_MOVE_EVENT.stopTiming();
            return;
        }

        GDBorderClaimEvent gpEvent = new GDBorderClaimEvent(targetEntity, fromClaim, toClaim);
        if (user != null && toClaim.isUserTrusted(user, TrustTypes.ACCESSOR)) {
            GriefDefender.getEventManager().post(gpEvent);
            if (gpEvent.cancelled()) {
                if (targetEntity instanceof Vehicle) {
                    final Vehicle vehicle = (Vehicle) targetEntity;
                    vehicle.teleport(fromLocation);
                    GDTimings.ENTITY_MOVE_EVENT.stopTiming();
                    return;
                }
                if (event instanceof Cancellable) {
                    ((Cancellable) event).setCancelled(true);
                }
                final Component cancelMessage = gpEvent.getMessage().orElse(null);
                if (player != null && cancelMessage != null) {
                    TextAdapter.sendComponent(player, cancelMessage);
                }
            } else {
                final boolean showGpPrefix = GriefDefenderPlugin.getGlobalConfig().getConfig().message.enterExitShowGdPrefix;
                user.getInternalPlayerData().lastClaim = new WeakReference<>(toClaim);
                TextComponent welcomeMessage = (TextComponent) gpEvent.getEnterMessage().orElse(null);
                if (welcomeMessage != null && !welcomeMessage.equals(TextComponent.empty())) {
                    ChatType chatType = gpEvent.getEnterMessageChatType();
                    if (chatType == ChatTypes.ACTION_BAR) {
                        TextAdapter.sendActionBar(player, TextComponent.builder("")
                                .append(showGpPrefix ? GriefDefenderPlugin.GD_TEXT : TextComponent.empty())
                                .append(welcomeMessage)
                                .build());
                    } else {
                        TextAdapter.sendComponent(player, TextComponent.builder("")
                                .append(showGpPrefix ? GriefDefenderPlugin.GD_TEXT : TextComponent.empty())
                                .append(welcomeMessage)
                                .build());
                    }
                }

                Component farewellMessage = gpEvent.getExitMessage().orElse(null);
                if (farewellMessage != null && !farewellMessage.equals(TextComponent.empty()) && !farewellMessage.equals("")) {
                    ChatType chatType = gpEvent.getExitMessageChatType();
                    if (chatType == ChatTypes.ACTION_BAR) {
                        TextAdapter.sendActionBar(player, TextComponent.builder("")
                                .append(showGpPrefix ? GriefDefenderPlugin.GD_TEXT : TextComponent.empty())
                                .append(farewellMessage)
                                .build());
                    } else {
                        TextAdapter.sendComponent(player, TextComponent.builder("")
                                .append(showGpPrefix ? GriefDefenderPlugin.GD_TEXT : TextComponent.empty())
                                .append(farewellMessage)
                                .build());
                    }
                }

                if (toClaim.isInTown()) {
                    user.getInternalPlayerData().inTown = true;
                } else {
                    user.getInternalPlayerData().inTown = false;
                }
            }

            GDTimings.ENTITY_MOVE_EVENT.stopTiming();
            return;
        }

        if (fromClaim != toClaim) {
            boolean enterCancelled = false;
            boolean exitCancelled = false;
            // enter
            if (GDFlags.ENTER_CLAIM && !enterBlacklisted && GDPermissionManager.getInstance().getFinalPermission(event, toLocation, toClaim, GDPermissions.ENTER_CLAIM, targetEntity, targetEntity, user) == Tristate.FALSE) {
                enterCancelled = true;
                gpEvent.cancelled(true);
            }

            // exit
            if (GDFlags.EXIT_CLAIM && !exitBlacklisted && GDPermissionManager.getInstance().getFinalPermission(event, fromLocation, fromClaim, GDPermissions.EXIT_CLAIM, targetEntity, targetEntity, user) == Tristate.FALSE) {
                exitCancelled = true;
                gpEvent.cancelled(true);
            }

            GriefDefender.getEventManager().post(gpEvent);
            if (gpEvent.cancelled()) {
                final Component cancelMessage = gpEvent.getMessage().orElse(null);
                if (exitCancelled) {
                    if (cancelMessage != null && player != null) {
                        GriefDefenderPlugin.sendClaimDenyMessage(fromClaim, player, MessageCache.getInstance().PERMISSION_CLAIM_EXIT);
                    }
                } else if (enterCancelled) {
                    if (cancelMessage != null && player != null) {
                        GriefDefenderPlugin.sendClaimDenyMessage(toClaim, player, MessageCache.getInstance().PERMISSION_CLAIM_ENTER);
                    }
                }

                if (cancelMessage != null && player != null) {
                    TextAdapter.sendComponent(player, cancelMessage);
                }

                if (targetEntity instanceof Vehicle) {
                    final Vehicle vehicle = (Vehicle) targetEntity;
                    vehicle.teleport(fromLocation);
                    GDTimings.ENTITY_MOVE_EVENT.stopTiming();
                    return;
                }
                if (event instanceof Cancellable) {
                    ((Cancellable) event).setCancelled(true);
                }
                GDTimings.ENTITY_MOVE_EVENT.stopTiming();
                return;
            }

            if (user != null) {
                final boolean showGpPrefix = GriefDefenderPlugin.getGlobalConfig().getConfig().message.enterExitShowGdPrefix;
                user.getInternalPlayerData().lastClaim = new WeakReference<>(toClaim);
                Component welcomeMessage = gpEvent.getEnterMessage().orElse(null);
                if (welcomeMessage != null && !welcomeMessage.equals(TextComponent.empty()) && !welcomeMessage.equals("")) {
                    ChatType chatType = gpEvent.getEnterMessageChatType();
                    if (chatType == ChatTypes.ACTION_BAR) {
                        TextAdapter.sendActionBar(player, TextComponent.builder("")
                                .append(showGpPrefix ? GriefDefenderPlugin.GD_TEXT : TextComponent.empty())
                                .append(welcomeMessage)
                                .build());
                    } else {
                        TextAdapter.sendComponent(player, TextComponent.builder("")
                                .append(showGpPrefix ? GriefDefenderPlugin.GD_TEXT : TextComponent.empty())
                                .append(welcomeMessage)
                                .build());
                    }
                }

                Component farewellMessage = gpEvent.getExitMessage().orElse(null);
                if (farewellMessage != null && !farewellMessage.equals(TextComponent.empty()) && !farewellMessage.equals("")) {
                    ChatType chatType = gpEvent.getExitMessageChatType();
                    if (chatType == ChatTypes.ACTION_BAR) {
                        TextAdapter.sendActionBar(player, TextComponent.builder("")
                                .append(showGpPrefix ? GriefDefenderPlugin.GD_TEXT : TextComponent.empty())
                                .append(farewellMessage)
                                .build());
                    } else {
                        TextAdapter.sendComponent(player, TextComponent.builder("")
                                .append(showGpPrefix ? GriefDefenderPlugin.GD_TEXT : TextComponent.empty())
                                .append(farewellMessage)
                                .build());
                    }
                }

                if (toClaim.isInTown()) {
                    user.getInternalPlayerData().inTown = true;
                } else {
                    user.getInternalPlayerData().inTown = false;
                }

                checkPlayerFlight(player, user.getInternalPlayerData(), fromClaim, toClaim);
            }
        }

        GDTimings.ENTITY_MOVE_EVENT.stopTiming();
    }

    private void checkPlayerFlight(Player player, GDPlayerData playerData, GDClaim fromClaim, GDClaim toClaim) {
        final GameMode gameMode = player.getGameMode();
        if (gameMode == GameMode.CREATIVE || gameMode == GameMode.SPECTATOR) {
            return;
        }

        if (fromClaim == toClaim || !player.isFlying()) {
            // only handle player-fly in enter/exit
            return;
        }

        final Boolean noFly = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Boolean.class), playerData.getSubject(), Options.PLAYER_DENY_FLIGHT, toClaim);
        final boolean adminFly = player.hasPermission(GDPermissions.BYPASS_OPTION);
        final boolean ownerFly = toClaim.isBasicClaim() ? player.hasPermission(GDPermissions.USER_OPTION_PERK_OWNER_FLY_BASIC) : toClaim.isTown() ? player.hasPermission(GDPermissions.USER_OPTION_PERK_OWNER_FLY_TOWN) : false;
        if (player.getUniqueId().equals(toClaim.getOwnerUniqueId()) && ownerFly) {
            return;
        }
        if (!adminFly && noFly) {
            player.setAllowFlight(false);
            player.setFlying(false);
            playerData.ignoreFallDamage = true;
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().OPTION_PLAYER_DENY_FLIGHT);
        }
    }

    public void sendInteractEntityDenyMessage(ItemStack playerItem, Entity entity, GDClaim claim, Player player) {
        if (entity instanceof Player || (claim.getData() != null && !claim.getData().allowDenyMessages())) {
            return;
        }

        final String entityId = entity.getType().getName() == null ? entity.getType().name().toLowerCase() : entity.getType().getName();
        if (playerItem == null || playerItem.getType() == Material.AIR) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_INTERACT_ENTITY, ImmutableMap.of(
                    "player", claim.getOwnerName(),
                    "entity", entityId));
            GriefDefenderPlugin.sendClaimDenyMessage(claim, player, message);
        } else {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_INTERACT_ITEM_ENTITY, ImmutableMap.of(
                    "item", ItemTypeRegistryModule.getInstance().getNMSKey(playerItem),
                    "entity", entityId));
            GriefDefenderPlugin.sendClaimDenyMessage(claim, player, message);
        }
    }
}
