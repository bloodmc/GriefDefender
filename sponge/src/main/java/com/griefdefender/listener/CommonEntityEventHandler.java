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

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GDTimings;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.ChatType;
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
import com.griefdefender.internal.util.VecHelper;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.permission.flag.GDFlags;
import com.griefdefender.provider.MCClansProvider;
import com.griefdefender.storage.BaseStorage;
import com.griefdefender.util.EntityUtils;
import com.griefdefender.util.SpongeUtil;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.spongeapi.TextAdapter;
import nl.riebie.mcclans.api.ClanPlayer;

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

    public boolean onEntityMove(MoveEntityEvent event, Location<World> fromLocation, Location<World> toLocation, Entity targetEntity){
        if ((!GDFlags.ENTER_CLAIM && !GDFlags.EXIT_CLAIM) || fromLocation.getBlockPosition().equals(toLocation.getBlockPosition())) {
            return true;
        }

        World world = targetEntity.getWorld();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUniqueId())) {
            return true;
        }
        final boolean enterBlacklisted = GriefDefenderPlugin.isSourceIdBlacklisted(Flags.ENTER_CLAIM.getName(), targetEntity, world.getProperties());
        final boolean exitBlacklisted = GriefDefenderPlugin.isSourceIdBlacklisted(Flags.EXIT_CLAIM.getName(), targetEntity, world.getProperties());
        if (enterBlacklisted && exitBlacklisted) {
            return true;
        }

        GDTimings.ENTITY_MOVE_EVENT.startTimingIfSync();
        Player player = null;
        GDPermissionUser user = null;
        if (targetEntity instanceof Player) {
            player = (Player) targetEntity;
            user = PermissionHolderCache.getInstance().getOrCreateUser(player);
        } else {
            final Entity controller = EntityUtils.getControllingPassenger(targetEntity);
            if (controller != null && controller instanceof Player) {
                player = (Player) controller;
            }
            user = PermissionHolderCache.getInstance().getOrCreateUser(targetEntity.getCreator().orElse(null));
        }

        if (user != null) {
            if (user.getInternalPlayerData().teleportDelay > 0) {
                if (!toLocation.getBlockPosition().equals(VecHelper.toVector3i(user.getInternalPlayerData().teleportSourceLocation))) {
                    user.getInternalPlayerData().teleportDelay = 0;
                    TextAdapter.sendComponent(player, MessageCache.getInstance().TELEPORT_MOVE_CANCEL);
                }
            }
        }

        if (player == null && user == null) {
            // Handle border event without player
            GDClaim fromClaim = this.storage.getClaimAt(fromLocation);
            GDClaim toClaim = this.storage.getClaimAt(toLocation);
            if (fromClaim != toClaim) {
                GDBorderClaimEvent gpEvent = new GDBorderClaimEvent(targetEntity, fromClaim, toClaim);
                // enter
                if (GDFlags.ENTER_CLAIM && !enterBlacklisted && GDPermissionManager.getInstance().getFinalPermission(event, toLocation, toClaim, GDPermissions.ENTER_CLAIM, targetEntity, targetEntity, user) == Tristate.FALSE) {
                    gpEvent.cancelled(true);
                    if (event != null) {
                        event.setCancelled(true);
                    }
                }

                // exit
                if (GDFlags.EXIT_CLAIM && !exitBlacklisted && GDPermissionManager.getInstance().getFinalPermission(event, fromLocation, fromClaim, GDPermissions.EXIT_CLAIM, targetEntity, targetEntity, user) == Tristate.FALSE) {
                    gpEvent.cancelled(true);
                    if (event != null) {
                        event.setCancelled(true);
                    }
                }

                GriefDefender.getEventManager().post(gpEvent);
                if (gpEvent.cancelled()) {
                    if (event != null) {
                        event.setCancelled(true);
                    }
                    if (!(targetEntity instanceof Player) && EntityUtils.getOwnerUniqueId(targetEntity) == null) {
                        targetEntity.remove();
                    }
                    return false;
                }
            }
            GDTimings.ENTITY_MOVE_EVENT.stopTimingIfSync();
            return true;
        }

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
                    Location<World> claimCorner = new Location<>(toClaim.getWorld(), toClaim.lesserBoundaryCorner.getX(), player.getLocation().getY(), toClaim.greaterBoundaryCorner.getZ());
                    Location<World> safeLocation = Sponge.getGame().getTeleportHelper().getSafeLocation(claimCorner, 9, 9).orElse(player.getWorld().getSpawnLocation());
                    if (event != null) {
                        event.setToTransform(player.getTransform().setLocation(safeLocation));
                    }
                    return false;
                }
            }
        }
        if (fromClaim == toClaim) {
            GDTimings.ENTITY_MOVE_EVENT.stopTimingIfSync();
            return true;
        }
        // MCClans tag support
        Component enterClanTag = null;
        Component exitClanTag = null;
        MCClansProvider clanApiProvider = GriefDefenderPlugin.getInstance().clanApiProvider;
        if (clanApiProvider != null) {
            if ((fromClaim.isBasicClaim() || (fromClaim.isSubdivision() && !fromClaim.isAdminClaim()))) {
                ClanPlayer clanPlayer = clanApiProvider.getClanService().getClanPlayer(fromClaim.getOwnerUniqueId());
                if (clanPlayer != null && clanPlayer.getClan() != null) {
                    exitClanTag = SpongeUtil.fromSpongeText(Text.of(clanPlayer.getClan().getTagColored(), " "));
                }
            }
            if ((toClaim.isBasicClaim() || (toClaim.isSubdivision() && !toClaim.isAdminClaim()))) {
                ClanPlayer clanPlayer = clanApiProvider.getClanService().getClanPlayer(toClaim.getOwnerUniqueId());
                if (clanPlayer != null && clanPlayer.getClan() != null) {
                    enterClanTag = SpongeUtil.fromSpongeText(Text.of(clanPlayer.getClan().getTagColored(), " "));
                }
            }
        }

        GDBorderClaimEvent gpEvent = new GDBorderClaimEvent(targetEntity, fromClaim, toClaim);
        if (user != null && toClaim.isUserTrusted(user, TrustTypes.ACCESSOR)) {
            GriefDefender.getEventManager().post(gpEvent);
            if (gpEvent.cancelled()) {
                event.setCancelled(true);
                if (!(targetEntity instanceof Player) && EntityUtils.getOwnerUniqueId(targetEntity) == null) {
                    targetEntity.remove();
                }
                final Component cancelMessage = gpEvent.getMessage().orElse(null);
                if (player != null && cancelMessage != null) {
                    TextAdapter.sendComponent(player, cancelMessage);
                }
                return false;
            } else {
                    final boolean showGpPrefix = GriefDefenderPlugin.getGlobalConfig().getConfig().message.enterExitShowGdPrefix;
                    user.getInternalPlayerData().lastClaim = new WeakReference<>(toClaim);
                    TextComponent welcomeMessage = (TextComponent) gpEvent.getEnterMessage().orElse(null);
                    if (welcomeMessage != null && !welcomeMessage.equals(TextComponent.empty()) && !welcomeMessage.content().equals("")) {
                        ChatType chatType = gpEvent.getEnterMessageChatType();
                        if (showGpPrefix) {
                            TextAdapter.sendComponent(player, TextComponent.builder("")
                                    .append(enterClanTag != null ? enterClanTag : GriefDefenderPlugin.GD_TEXT)
                                    .append(welcomeMessage).build(), SpongeUtil.getSpongeChatType(chatType));
                        } else {
                            TextAdapter.sendComponent(player, enterClanTag != null ? enterClanTag : welcomeMessage, SpongeUtil.getSpongeChatType(chatType));
                        }
                    }

                    Component farewellMessage = gpEvent.getExitMessage().orElse(null);
                    if (farewellMessage != null && !farewellMessage.equals(Text.of())) {
                        ChatType chatType = gpEvent.getExitMessageChatType();
                        if (showGpPrefix) {
                            TextAdapter.sendComponent(player, TextComponent.builder("")
                                    .append(exitClanTag != null ? exitClanTag : GriefDefenderPlugin.GD_TEXT)
                                    .append(farewellMessage)
                                    .build(), SpongeUtil.getSpongeChatType(chatType));
                        } else {
                            TextAdapter.sendComponent(player, exitClanTag != null ? exitClanTag : farewellMessage, SpongeUtil.getSpongeChatType(chatType));
                        }
                    }

                    if (toClaim.isInTown()) {
                        user.getInternalPlayerData().inTown = true;
                    } else {
                        user.getInternalPlayerData().inTown = false;
                    }
            }

            GDTimings.ENTITY_MOVE_EVENT.stopTimingIfSync();
            return true;
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

                event.setCancelled(true);
                if (!(targetEntity instanceof Player) && EntityUtils.getOwnerUniqueId(targetEntity) == null) {
                    targetEntity.remove();
                }
                GDTimings.ENTITY_MOVE_EVENT.stopTimingIfSync();
                return false;
            }

            if (user != null) {
                final boolean showGpPrefix = GriefDefenderPlugin.getGlobalConfig().getConfig().message.enterExitShowGdPrefix;
                user.getInternalPlayerData().lastClaim = new WeakReference<>(toClaim);
                Component welcomeMessage = gpEvent.getEnterMessage().orElse(null);
                if (welcomeMessage != null && !welcomeMessage.equals(TextComponent.empty())) {
                    ChatType chatType = gpEvent.getEnterMessageChatType();
                    if (showGpPrefix) {
                        TextAdapter.sendComponent(player, TextComponent.builder("")
                                .append(enterClanTag != null ? enterClanTag : GriefDefenderPlugin.GD_TEXT)
                                .append(welcomeMessage)
                                .build(), SpongeUtil.getSpongeChatType(chatType));
                    } else {
                        TextAdapter.sendComponent(player, enterClanTag != null ? enterClanTag : welcomeMessage, SpongeUtil.getSpongeChatType(chatType));
                    }
                }

                Component farewellMessage = gpEvent.getExitMessage().orElse(null);
                if (farewellMessage != null && !farewellMessage.equals(Text.of())) {
                    ChatType chatType = gpEvent.getExitMessageChatType();
                    if (showGpPrefix) {
                        TextAdapter.sendComponent(player, TextComponent.builder("")
                                .append(exitClanTag != null ? exitClanTag : GriefDefenderPlugin.GD_TEXT)
                                .append(farewellMessage)
                                .build(), SpongeUtil.getSpongeChatType(chatType));
                    } else {
                        TextAdapter.sendComponent(player, exitClanTag != null ? exitClanTag : farewellMessage, SpongeUtil.getSpongeChatType(chatType));
                    }
                }

                if (toClaim.isInTown()) {
                    user.getInternalPlayerData().inTown = true;
                } else {
                    user.getInternalPlayerData().inTown = false;
                }

                if (player != null) {
                    checkPlayerFlight(player, user.getInternalPlayerData(), fromClaim, toClaim);
                }
            }
        }

        GDTimings.ENTITY_MOVE_EVENT.stopTimingIfSync();
        return true;
    }

    private void checkPlayerFlight(Player player, GDPlayerData playerData, GDClaim fromClaim, GDClaim toClaim) {
        final GameMode gameMode = player.get(Keys.GAME_MODE).orElse(null);
        if (gameMode == null || gameMode == GameModes.CREATIVE || gameMode == GameModes.SPECTATOR) {
            return;
        }

        if (fromClaim == toClaim || !player.get(Keys.IS_FLYING).get()) {
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
            player.offer(Keys.CAN_FLY, false);
            player.offer(Keys.IS_FLYING, false);
            playerData.ignoreFallDamage = true;
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().OPTION_PLAYER_DENY_FLIGHT);
        }
    }

    public void sendInteractEntityDenyMessage(ItemStack playerItem, Entity entity, GDClaim claim, Player player) {
        if (entity instanceof Player || (claim.getData() != null && !claim.getData().allowDenyMessages())) {
            return;
        }

        final String entityId = entity.getType().getId().toLowerCase();
        if (playerItem == null || playerItem.isEmpty()) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_INTERACT_ENTITY, ImmutableMap.of(
                    "player", claim.getOwnerName(),
                    "entity", entityId));
            GriefDefenderPlugin.sendClaimDenyMessage(claim, player, message);
        } else {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_INTERACT_ITEM_ENTITY, ImmutableMap.of(
                    "item", playerItem.getType().getId().toLowerCase(),
                    "entity", entityId));
            GriefDefenderPlugin.sendClaimDenyMessage(claim, player, message);
        }
    }
}