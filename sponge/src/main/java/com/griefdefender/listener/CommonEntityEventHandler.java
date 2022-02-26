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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.Transform;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameMode;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
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
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.flag.Flags;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.api.permission.option.type.GameModeType;
import com.griefdefender.api.permission.option.type.GameModeTypes;
import com.griefdefender.api.permission.option.type.WeatherType;
import com.griefdefender.api.permission.option.type.WeatherTypes;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.command.CommandHelper;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.event.GDBorderClaimEvent;
import com.griefdefender.internal.util.BlockUtil;
import com.griefdefender.internal.util.NMSUtil;
import com.griefdefender.internal.util.VecHelper;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.flag.GDFlags;
import com.griefdefender.permission.option.GDOptions;
import com.griefdefender.permission.option.OptionContexts;
import com.griefdefender.provider.MCClansProvider;
import com.griefdefender.storage.BaseStorage;
import com.griefdefender.util.EntityUtils;
import com.griefdefender.util.PermissionUtil;
import com.griefdefender.util.PlayerUtil;
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

    public boolean onEntityMove(Event event, Location<World> fromLocation, Location<World> toLocation, Entity targetEntity){
        if (targetEntity instanceof Item || targetEntity instanceof Projectile) {
            return true;
        }

        if ((!GDFlags.ENTER_CLAIM && !GDFlags.EXIT_CLAIM) || fromLocation.getBlockPosition().equals(toLocation.getBlockPosition())) {
            return true;
        }

        Player player = null;
        GDPermissionUser user = null;
        boolean onMount = false;
        if (targetEntity instanceof Player) {
            player = (Player) targetEntity;
            user = PermissionHolderCache.getInstance().getOrCreateUser(player);
        } else {
            final Entity controller = EntityUtils.getControllingPassenger(targetEntity);
            if (controller != null && controller instanceof Player) {
                player = (Player) controller;
                user = PermissionHolderCache.getInstance().getOrCreateUser(player);
                onMount = true;
            } else {
                user = PermissionHolderCache.getInstance().getOrCreateUser(targetEntity.getCreator().orElse(null));
                if (user != null && user.getOnlinePlayer() != null) {
                    player = user.getOnlinePlayer();
                }
            }
        }
        if (user != null) {
            if (user.getInternalPlayerData().trappedRequest) {
                GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().COMMAND_TRAPPED_CANCEL_MOVE);
                user.getInternalPlayerData().trappedRequest = false;
                user.getInternalPlayerData().teleportDelay = 0;
            }
        }
        if (player != null && user != null) {
            if (event instanceof MoveEntityEvent.Teleport && user.getInternalPlayerData().runningPlayerCommands) {
                return true;
            }
        }

        World world = targetEntity.getWorld();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUniqueId())) {
            return true;
        }

        GDTimings.ENTITY_MOVE_EVENT.startTimingIfSync();

        if (user != null && user.getOnlinePlayer() != null) {
            final boolean preInLiquid = user.getInternalPlayerData().inLiquid;
            final boolean inLiquid = NMSUtil.getInstance().isBlockLiquid(player.getLocation().getBlock());
            if (preInLiquid != inLiquid) {
                user.getInternalPlayerData().inLiquid = inLiquid;
            }
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
                if (GDFlags.ENTER_CLAIM && GDPermissionManager.getInstance().getFinalPermission(event, toLocation, toClaim, Flags.ENTER_CLAIM, targetEntity, targetEntity, user) == Tristate.FALSE) {
                    gpEvent.cancelled(true);
                    if (event != null && event instanceof Cancellable) {
                        ((Cancellable) event).setCancelled(true);
                    }
                }

                // exit
                if (GDFlags.EXIT_CLAIM && GDPermissionManager.getInstance().getFinalPermission(event, fromLocation, fromClaim, Flags.EXIT_CLAIM, targetEntity, targetEntity, user) == Tristate.FALSE) {
                    gpEvent.cancelled(true);
                    if (event != null && event instanceof Cancellable) {
                        ((Cancellable) event).setCancelled(true);
                    }
                }

                GriefDefender.getEventManager().post(gpEvent);
                if (gpEvent.cancelled()) {
                    if (event != null && event instanceof Cancellable) {
                        ((Cancellable) event).setCancelled(true);
                    }
                    if (!(targetEntity instanceof Player) && EntityUtils.getOwnerUniqueId(targetEntity) == null) {
                        targetEntity.remove();
                    }
                    if (event instanceof RespawnPlayerEvent) {
                        // Respawn player in safe location in source claim
                        ((RespawnPlayerEvent) event).setToTransform(new Transform<World>(PlayerUtil.getInstance().getSafeClaimLocation(fromClaim)));
                    }
                    GDTimings.ENTITY_MOVE_EVENT.stopTimingIfSync();
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

        if (fromClaim == toClaim) {
            if (user != null) {
                this.checkPlayerFlight(user, fromClaim, toClaim);
                this.checkPlayerFlySpeed(user, fromClaim, toClaim);
                this.checkPlayerGameMode(user, fromClaim, toClaim);
                this.checkPlayerGodMode(user, fromClaim, toClaim);
                this.checkPlayerWalkSpeed(user, fromClaim, toClaim);
                this.checkPlayerWeather(user, fromClaim, toClaim, false);
            }
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
                if (event instanceof Cancellable) {
                    ((Cancellable) event).setCancelled(true);
                }
                if (!(targetEntity instanceof Player) && EntityUtils.getOwnerUniqueId(targetEntity) == null) {
                    targetEntity.remove();
                }
                final Component cancelMessage = gpEvent.getMessage().orElse(null);
                if (player != null && cancelMessage != null) {
                    TextAdapter.sendComponent(player, cancelMessage);
                }
                if (event instanceof RespawnPlayerEvent) {
                    // Respawn player in safe location in source claim
                    ((RespawnPlayerEvent) event).setToTransform(new Transform<World>(PlayerUtil.getInstance().getSafeClaimLocation(fromClaim)));
                }
                GDTimings.ENTITY_MOVE_EVENT.stopTimingIfSync();
                return false;
            } else {
                    final boolean showGpPrefix = GriefDefenderPlugin.getGlobalConfig().getConfig().message.enterExitShowGdPrefix;
                    if (player != null) {
                        TextComponent welcomeMessage = (TextComponent) gpEvent.getEnterMessage().orElse(null);
                        if (welcomeMessage != null && !welcomeMessage.equals(TextComponent.empty()) && !fromClaim.isParent(toClaim)) {
                            ChatType chatType = gpEvent.getEnterMessageChatType();
                            if (showGpPrefix) {
                                final Component enterPrefix = toClaim.isWilderness() || toClaim.isAdminClaim() ? GriefDefenderPlugin.GD_TEXT : MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.CLAIM_PREFIX_ENTER, ImmutableMap.of(
                                        "owner", toClaim.getOwnerDisplayName()));
                                TextAdapter.sendComponent(player, TextComponent.builder("")
                                        .append(enterClanTag != null ? enterClanTag : enterPrefix)
                                        .append(welcomeMessage).build(), SpongeUtil.getSpongeChatType(chatType));
                            } else {
                                TextAdapter.sendComponent(player, enterClanTag != null ? enterClanTag : welcomeMessage, SpongeUtil.getSpongeChatType(chatType));
                            }
                        }
    
                        Component farewellMessage = gpEvent.getExitMessage().orElse(null);
                        if (farewellMessage != null && farewellMessage != TextComponent.empty() && !toClaim.isParent(fromClaim)) {
                            ChatType chatType = gpEvent.getExitMessageChatType();
                            if (showGpPrefix) {
                                final Component exitPrefix = fromClaim.isWilderness() || fromClaim.isAdminClaim() ? GriefDefenderPlugin.GD_TEXT : MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.CLAIM_PREFIX_EXIT, ImmutableMap.of(
                                        "owner", fromClaim.getOwnerDisplayName()));
                                TextAdapter.sendComponent(player, TextComponent.builder("")
                                        .append(exitClanTag != null ? exitClanTag : exitPrefix)
                                        .append(farewellMessage)
                                        .build(), SpongeUtil.getSpongeChatType(chatType));
                            } else {
                                TextAdapter.sendComponent(player, exitClanTag != null ? exitClanTag : farewellMessage, SpongeUtil.getSpongeChatType(chatType));
                            }
                        }
                    }

                    if (toClaim.isInTown()) {
                        user.getInternalPlayerData().inTown = true;
                    } else {
                        user.getInternalPlayerData().inTown = false;
                    }
                    if (player != null) {
                        this.checkPlayerFlight(user, fromClaim, toClaim);
                        this.checkPlayerFlySpeed(user, fromClaim, toClaim);
                        this.checkPlayerGameMode(user, fromClaim, toClaim);
                        this.checkPlayerGodMode(user, fromClaim, toClaim);
                        this.checkPlayerWalkSpeed(user, fromClaim, toClaim);
                        this.checkPlayerWeather(user, fromClaim, toClaim, false);
                        // Exit command - Don't run if to claim is child of from claim
                        if (!toClaim.isParent(fromClaim)) {
                            this.runPlayerCommands(fromClaim, user, false);
                        }
                        // Enter command - Don't run if to claim is parent of from claim
                        if (!fromClaim.isParent(toClaim)) {
                            this.runPlayerCommands(toClaim, user, true);
                        }
                    }
            }

            GDTimings.ENTITY_MOVE_EVENT.stopTimingIfSync();
            return true;
        }

        if (fromClaim != toClaim) {
            boolean enterCancelled = false;
            boolean exitCancelled = false;
            // enter
            if (GDFlags.ENTER_CLAIM && GDPermissionManager.getInstance().getFinalPermission(event, toLocation, toClaim, Flags.ENTER_CLAIM, targetEntity, targetEntity, user, true) == Tristate.FALSE) {
                enterCancelled = true;
                gpEvent.cancelled(true);
            }

            // exit
            if (GDFlags.EXIT_CLAIM && GDPermissionManager.getInstance().getFinalPermission(event, fromLocation, fromClaim, Flags.EXIT_CLAIM, targetEntity, targetEntity, user, true) == Tristate.FALSE) {
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

                if (event instanceof Cancellable) {
                    ((Cancellable) event).setCancelled(true);
                }
                if (!(targetEntity instanceof Player) && EntityUtils.getOwnerUniqueId(targetEntity) == null) {
                    targetEntity.remove();
                }
                if (event instanceof RespawnPlayerEvent) {
                    // Respawn player in safe location in source claim
                    ((RespawnPlayerEvent) event).setToTransform(new Transform<World>(PlayerUtil.getInstance().getSafeClaimLocation(fromClaim)));
                }
                GDTimings.ENTITY_MOVE_EVENT.stopTimingIfSync();
                return false;
            }

            if (player != null) {
                if (GDFlags.ENTITY_RIDING && onMount) {
                    if (GDPermissionManager.getInstance().getFinalPermission(event, targetEntity.getLocation(), toClaim, Flags.ENTITY_RIDING, player, targetEntity, player, TrustTypes.ACCESSOR, true) == Tristate.FALSE) {
                        if (event instanceof Cancellable) {
                            ((Cancellable) event).setCancelled(true);
                            Location<World> safeLocation = Sponge.getGame().getTeleportHelper()
                                    .getSafeLocation(fromLocation, 80, 0)
                                    .orElseGet(() -> Sponge.getGame().getTeleportHelper()
                                            .getSafeLocation(fromLocation, 80, 6)
                                            .orElse(world.getSpawnLocation())
                                    );
                            targetEntity.getBaseVehicle().clearPassengers();
                            player.setTransform(player.getTransform().setLocation(safeLocation));
                            GDTimings.ENTITY_MOVE_EVENT.stopTimingIfSync();
                            return false;
                        }
                    }
                }

                final boolean showGpPrefix = GriefDefenderPlugin.getGlobalConfig().getConfig().message.enterExitShowGdPrefix;
                Component welcomeMessage = gpEvent.getEnterMessage().orElse(null);
                if (welcomeMessage != null && !welcomeMessage.equals(TextComponent.empty()) && !fromClaim.isParent(toClaim)) {
                    ChatType chatType = gpEvent.getEnterMessageChatType();
                    if (showGpPrefix) {
                        final Component enterPrefix = toClaim.isWilderness() || toClaim.isAdminClaim() ? GriefDefenderPlugin.GD_TEXT : MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.CLAIM_PREFIX_ENTER, ImmutableMap.of(
                                "owner", toClaim.getOwnerDisplayName()));
                        TextAdapter.sendComponent(player, TextComponent.builder("")
                                .append(enterClanTag != null ? enterClanTag : enterPrefix)
                                .append(welcomeMessage)
                                .build(), SpongeUtil.getSpongeChatType(chatType));
                    } else {
                        TextAdapter.sendComponent(player, enterClanTag != null ? enterClanTag : welcomeMessage, SpongeUtil.getSpongeChatType(chatType));
                    }
                }

                Component farewellMessage = gpEvent.getExitMessage().orElse(null);
                if (farewellMessage != null && !farewellMessage.equals(TextComponent.empty()) && !toClaim.isParent(fromClaim)) {
                    ChatType chatType = gpEvent.getExitMessageChatType();
                    if (showGpPrefix) {
                        final Component exitPrefix = fromClaim.isWilderness() || fromClaim.isAdminClaim() ? GriefDefenderPlugin.GD_TEXT : MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.CLAIM_PREFIX_EXIT, ImmutableMap.of(
                                "owner", fromClaim.getOwnerDisplayName()));
                        TextAdapter.sendComponent(player, TextComponent.builder("")
                                .append(exitClanTag != null ? exitClanTag : exitPrefix)
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
                    this.checkPlayerFlight(user, fromClaim, toClaim);
                    this.checkPlayerFlySpeed(user, fromClaim, toClaim);
                    this.checkPlayerGameMode(user, fromClaim, toClaim);
                    this.checkPlayerGodMode(user, fromClaim, toClaim);
                    this.checkPlayerWalkSpeed(user, fromClaim, toClaim);
                    this.checkPlayerWeather(user, fromClaim, toClaim, false);
                    // Exit command - Don't run if to claim is child of from claim
                    if (!toClaim.isParent(fromClaim)) {
                        this.runPlayerCommands(fromClaim, user, false);
                    }
                    // Enter command - Don't run if to claim is parent of from claim
                    if (!fromClaim.isParent(toClaim)) {
                        this.runPlayerCommands(toClaim, user, true);
                    }
                }
            }
        }

        GDTimings.ENTITY_MOVE_EVENT.stopTimingIfSync();
        return true;
    }

    final static Pattern pattern = Pattern.compile("([^\\s]+)", Pattern.MULTILINE);

    private void runPlayerCommands(GDClaim claim, GDPermissionUser user, boolean enter) {
        if (user == null) {
            return;
        }
        final Player player = user.getOnlinePlayer();
        if (player == null || NMSUtil.getInstance().isFakePlayer(player)) {
            // Most likely NPC
            return;
        }
        if (!GDOptions.PLAYER_COMMAND_ENTER && !GDOptions.PLAYER_COMMAND_EXIT) {
            return;
        }
        if (user.getInternalPlayerData().runningPlayerCommands) {
            return;
        }

        user.getInternalPlayerData().runningPlayerCommands = true;
        List<String> rawCommandList = new ArrayList<>();
        Set<Context> contexts = new HashSet<>();
        if (player.getUniqueId().equals(claim.getOwnerUniqueId())) {
            contexts.add(OptionContexts.COMMAND_RUNFOR_OWNER);
        } else {
            contexts.add(OptionContexts.COMMAND_RUNFOR_MEMBER);
        }
        contexts.add(OptionContexts.COMMAND_RUNFOR_PUBLIC);
        // Check console commands
        contexts.add(OptionContexts.COMMAND_RUNAS_CONSOLE);
        if (enter) {
            rawCommandList = GDPermissionManager.getInstance().getInternalOptionValue(new TypeToken<List<String>>() {}, user, Options.PLAYER_COMMAND_ENTER, claim, contexts);
        } else {
            rawCommandList = GDPermissionManager.getInstance().getInternalOptionValue(new TypeToken<List<String>>() {}, user, Options.PLAYER_COMMAND_EXIT, claim, contexts);
        }

        if (rawCommandList != null) {
            runCommand(claim, player, rawCommandList, true);
        }

        // Check player commands
        contexts.remove(OptionContexts.COMMAND_RUNAS_CONSOLE);
        contexts.add(OptionContexts.COMMAND_RUNAS_PLAYER);
        if (enter) {
            rawCommandList = GDPermissionManager.getInstance().getInternalOptionValue(new TypeToken<List<String>>() {}, user, Options.PLAYER_COMMAND_ENTER, claim, contexts);
        } else {
            rawCommandList = GDPermissionManager.getInstance().getInternalOptionValue(new TypeToken<List<String>>() {}, user, Options.PLAYER_COMMAND_EXIT, claim, contexts);
        }

        if (rawCommandList != null) {
            runCommand(claim, player, rawCommandList, false);
        }
        user.getInternalPlayerData().runningPlayerCommands = false;
    }

    private void runCommand(GDClaim claim, Player player, List<String> rawCommandList, boolean runAsConsole) {
        final List<String> commands = new ArrayList<>();
        for (String command : rawCommandList) {
            commands.add(this.replacePlaceHolders(claim, player, command));
        }
        for (String command : commands) {
            final Matcher matcher = pattern.matcher(command);
            if (matcher.find()) {
                String baseCommand = matcher.group(0);
                String args = command.replace(baseCommand + " ", "");
                baseCommand = baseCommand.replace("\\", "").replace("/", "");
                args = args.replace("%player%", player.getName());
                // Handle WorldEdit commands
                if (command.startsWith("//") && !baseCommand.startsWith("/")) {
                    baseCommand = "/" + baseCommand;
                }
                if (runAsConsole) {
                    CommandHelper.executeCommand(Sponge.getServer().getConsole(), baseCommand, args);
                } else {
                    CommandHelper.executeCommand(player, baseCommand, args);
                }
            }
        }
    }

    private String replacePlaceHolders(GDClaim claim, Player player, String command) {
        command = command
                .replace("%player%", player.getName())
                .replace("%owner%", claim.getOwnerName())
                .replace("%uuid%", player.getUniqueId().toString())
                .replace("%world%", claim.getWorld().getName())
                .replace("%server%", PermissionUtil.getInstance().getServerName())
                .replace("%location%", BlockUtil.getInstance().posToString(player.getLocation()));
        return command;
    }

    private void checkPlayerFlight(GDPermissionUser user, GDClaim fromClaim, GDClaim toClaim) {
        if (user == null) {
            return;
        }
        final Player player = user.getOnlinePlayer();
        if (player == null || NMSUtil.getInstance().isFakePlayer(player) || !player.get(Keys.IS_FLYING).get()) {
            // Most likely NPC
            return;
        }
        if (!GDOptions.PLAYER_DENY_FLIGHT) {
            return;
        }

        final GDPlayerData playerData = user.getInternalPlayerData();
        final GameMode gameMode = player.get(Keys.GAME_MODE).orElse(null);
        if (gameMode == null || gameMode == GameModes.SPECTATOR) {
            return;
        }
        if (playerData.inPvpCombat() && !GriefDefenderPlugin.getActiveConfig(player.getWorld().getUniqueId()).getConfig().pvp.allowFly) {
            player.offer(Keys.CAN_FLY, false);
            player.offer(Keys.IS_FLYING, false);
            playerData.ignoreFallDamage = true;
            Location<World> safeLocation = Sponge.getGame().getTeleportHelper()
                .getSafeLocation(player.getLocation(), 80, 0)
                .orElseGet(() -> Sponge.getGame().getTeleportHelper()
                        .getSafeLocation(player.getLocation(), 80, 6)
                        .orElse(player.getWorld().getSpawnLocation())
                );
            player.setTransform(player.getTransform().setLocation(safeLocation));
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().OPTION_APPLY_PLAYER_DENY_FLIGHT);
            return;
        }

        if (playerData.userOptionBypassPlayerDenyFlight) {
            return;
        }

        boolean trustFly = false;
        if (toClaim.isBasicClaim() || (toClaim.parent != null && toClaim.parent.isBasicClaim()) || toClaim.isInTown()) {
            // check owner
            if (playerData.userOptionPerkFlyOwner && toClaim.allowEdit(player) == null) {
                trustFly = true;
            } else {
                if (playerData.userOptionPerkFlyAccessor && toClaim.isUserTrusted(player, TrustTypes.ACCESSOR)) {
                    trustFly = true;
                } else if (playerData.userOptionPerkFlyBuilder && toClaim.isUserTrusted(player, TrustTypes.BUILDER)) {
                    trustFly = true;
                } else if (playerData.userOptionPerkFlyContainer && toClaim.isUserTrusted(player, TrustTypes.CONTAINER)) {
                    trustFly = true;
                } else if (playerData.userOptionPerkFlyManager && toClaim.isUserTrusted(player, TrustTypes.MANAGER)) {
                    trustFly = true;
                }
             }
        }

        if (trustFly) {
            return;
        }

        final Boolean noFly = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Boolean.class), playerData.getSubject(), Options.PLAYER_DENY_FLIGHT, toClaim);
        if (noFly != null && noFly) {
            player.offer(Keys.CAN_FLY, false);
            player.offer(Keys.IS_FLYING, false);
            playerData.ignoreFallDamage = true;
            Location<World> safeLocation = Sponge.getGame().getTeleportHelper()
                    .getSafeLocation(player.getLocation(), 80, 0)
                    .orElseGet(() -> Sponge.getGame().getTeleportHelper()
                            .getSafeLocation(player.getLocation(), 80, 6)
                            .orElse(player.getWorld().getSpawnLocation())
                    );
            player.setTransform(player.getTransform().setLocation(safeLocation));
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().OPTION_APPLY_PLAYER_DENY_FLIGHT);
        }
    }

    private void checkPlayerGodMode(GDPermissionUser user, GDClaim fromClaim, GDClaim toClaim) {
        if (user == null) {
            return;
        }
        final Player player = user.getOnlinePlayer();
        if (player == null || NMSUtil.getInstance().isFakePlayer(player) || !player.get(Keys.INVULNERABLE).get()) {
            // Most likely NPC
            return;
        }
        if (!GDOptions.PLAYER_DENY_GODMODE) {
            return;
        }

        final GDPlayerData playerData = user.getInternalPlayerData();
        final GameMode gameMode = player.get(Keys.GAME_MODE).get();
        if (gameMode == GameModes.CREATIVE || gameMode == GameModes.SPECTATOR || !player.get(Keys.INVULNERABLE).get()) {
            return;
        }

        Boolean noGodMode = playerData.optionNoGodMode;
        if (noGodMode == null || fromClaim != toClaim) {
            noGodMode = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Boolean.class), playerData.getSubject(), Options.PLAYER_DENY_GODMODE, toClaim);
            playerData.optionNoGodMode = noGodMode;
        }
        final boolean bypassOption = playerData.userOptionBypassPlayerDenyGodmode;
        if (!bypassOption && noGodMode) {
            player.offer(Keys.INVULNERABLE, false);
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().OPTION_APPLY_PLAYER_DENY_GODMODE);
        }
    }

    private void checkPlayerGameMode(GDPermissionUser user, GDClaim fromClaim, GDClaim toClaim) {
        if (user == null) {
            return;
        }
        final Player player = user.getOnlinePlayer();
        if (player == null || NMSUtil.getInstance().isFakePlayer(player)) {
            // Most likely Citizens NPC
            return;
        }
        if (!GDOptions.PLAYER_GAMEMODE) {
            return;
        }

        final GDPlayerData playerData = user.getInternalPlayerData();
        final GameMode currentGameMode = player.get(Keys.GAME_MODE).get();
        GameModeType gameModeType = playerData.optionGameModeType;
        if (gameModeType == null || fromClaim != toClaim) {
            gameModeType = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(GameModeType.class), playerData.getSubject(), Options.PLAYER_GAMEMODE, toClaim);
            playerData.optionGameModeType = gameModeType;
        }
        if (gameModeType == GameModeTypes.UNDEFINED && playerData.lastGameMode != GameModeTypes.UNDEFINED) {
            player.offer(Keys.GAME_MODE, PlayerUtil.GAMEMODE_MAP.get(playerData.lastGameMode));
            return;
        }

        final boolean bypassOption = playerData.userOptionBypassPlayerGamemode;
        if (!bypassOption && gameModeType != null && gameModeType != GameModeTypes.UNDEFINED) {
            final GameMode newGameMode = PlayerUtil.GAMEMODE_MAP.get(gameModeType);
            if (currentGameMode != newGameMode) {
                playerData.lastGameMode = PlayerUtil.GAMEMODE_MAP.inverse().get(gameModeType);
                player.offer(Keys.GAME_MODE, PlayerUtil.GAMEMODE_MAP.get(gameModeType));
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.OPTION_APPLY_PLAYER_GAMEMODE,
                        ImmutableMap.of(
                        "gamemode", gameModeType.getName()));
                GriefDefenderPlugin.sendMessage(player, message);
            }
        }
    }

    private void checkPlayerFlySpeed(GDPermissionUser user, GDClaim fromClaim, GDClaim toClaim) {
        if (user == null) {
            return;
        }
        final Player player = user.getOnlinePlayer();
        if (player == null || NMSUtil.getInstance().isFakePlayer(player) || !player.get(Keys.IS_FLYING).get()) {
            // Most likely Citizens NPC
            return;
        }
        if (!GDOptions.PLAYER_FLY_SPEED) {
            return;
        }

        final GDPlayerData playerData = user.getInternalPlayerData();
        final double currentFlySpeed = Math.round(player.get(Keys.FLYING_SPEED).get() * 100.0) / 100.0;
        Double flySpeed = playerData.optionFlySpeed;
        if (flySpeed == null || fromClaim != toClaim) {
            flySpeed = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Double.class), playerData.getSubject(), Options.PLAYER_FLY_SPEED, toClaim);
            playerData.optionFlySpeed = flySpeed;
        }
        if (flySpeed <= 0) {
            String configValue = GriefDefenderPlugin.getOptionConfig().getConfig().vanillaFallbackMap.get(Options.PLAYER_FLY_SPEED.getName().toLowerCase());
            Double defaultFlySpeed = null;
            try {
                defaultFlySpeed = Double.parseDouble(configValue);
            } catch (Throwable t) {
                defaultFlySpeed = 0.05;
            }
            if (currentFlySpeed != defaultFlySpeed) {
                // set back to default
                player.offer(Keys.FLYING_SPEED, defaultFlySpeed);
                if (fromClaim.getWorldUniqueId().equals(toClaim.getWorldUniqueId())) {
                    final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.OPTION_APPLY_PLAYER_FLY_SPEED,
                            ImmutableMap.of(
                            "speed", defaultFlySpeed.floatValue()));
                    GriefDefenderPlugin.sendMessage(player, message);
                }
            }
            return;
        }

        if (flySpeed > 0) {
            if (currentFlySpeed != flySpeed) {
                player.offer(Keys.FLYING_SPEED, flySpeed);
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.OPTION_APPLY_PLAYER_FLY_SPEED,
                        ImmutableMap.of(
                        "speed", flySpeed));
                GriefDefenderPlugin.sendMessage(player, message);
            }
        }
    }

    private void checkPlayerWalkSpeed(GDPermissionUser user, GDClaim fromClaim, GDClaim toClaim) {
        if (user == null) {
            return;
        }
        final Player player = user.getOnlinePlayer();
        if (player == null || NMSUtil.getInstance().isFakePlayer(player) || player.get(Keys.IS_FLYING).get()) {
            // Most likely Citizens NPC
            return;
        }
        if (!GDOptions.PLAYER_WALK_SPEED) {
            return;
        }

        final GDPlayerData playerData = user.getInternalPlayerData();
        final double currentWalkSpeed = Math.round(player.get(Keys.WALKING_SPEED).get() * 100.0) / 100.0;
        Double walkSpeed = user.getInternalPlayerData().optionWalkSpeed;
        if (walkSpeed == null || fromClaim != toClaim) {
            walkSpeed = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Double.class), playerData.getSubject(), Options.PLAYER_WALK_SPEED, toClaim);
            user.getInternalPlayerData().optionWalkSpeed = walkSpeed;
        }
        if (walkSpeed <= 0) {
            String configValue = GriefDefenderPlugin.getOptionConfig().getConfig().vanillaFallbackMap.get(Options.PLAYER_WALK_SPEED.getName().toLowerCase());
            Double defaultWalkSpeed = null;
            try {
                defaultWalkSpeed = Double.parseDouble(configValue);
            } catch (Throwable t) {
                defaultWalkSpeed = 0.1;
            }
            if (currentWalkSpeed != defaultWalkSpeed) {
                // set back to default
                player.offer(Keys.WALKING_SPEED, defaultWalkSpeed);
                if (fromClaim.getWorldUniqueId().equals(toClaim.getWorldUniqueId())) {
                    final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.OPTION_APPLY_PLAYER_WALK_SPEED,
                            ImmutableMap.of(
                            "speed", defaultWalkSpeed.floatValue()));
                    GriefDefenderPlugin.sendMessage(player, message);
                }
            }
            return;
        }

        if (walkSpeed > 0) {
            if (currentWalkSpeed != walkSpeed) {
                player.offer(Keys.WALKING_SPEED, walkSpeed);
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.OPTION_APPLY_PLAYER_WALK_SPEED,
                        ImmutableMap.of(
                        "speed", walkSpeed));
                GriefDefenderPlugin.sendMessage(player, message);
            }
        }
    }

    public void checkPlayerWeather(GDPermissionUser user, GDClaim fromClaim, GDClaim toClaim, boolean force) {
        if (user == null) {
            return;
        }
        final Player player = user.getOnlinePlayer();
        if (player == null || NMSUtil.getInstance().isFakePlayer(player)) {
            // Most likely Citizens NPC
            return;
        }
        if (!GDOptions.PLAYER_WEATHER) {
            return;
        }

        final GDPlayerData playerData = user.getInternalPlayerData();
        WeatherType weatherType = playerData.optionWeatherType;
        if (weatherType == null || fromClaim != toClaim) {
            weatherType = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(WeatherType.class), playerData.getSubject(), Options.PLAYER_WEATHER, toClaim);
            playerData.optionWeatherType = weatherType;
        }
        if (weatherType == null || weatherType == WeatherTypes.UNDEFINED) {
            NMSUtil.getInstance().resetPlayerWeather(user);
            return;
        }

        NMSUtil.getInstance().setPlayerWeather(user, weatherType);
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
