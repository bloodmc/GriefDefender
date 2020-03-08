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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.spongepowered.api.world.weather.Weather;
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
import com.griefdefender.internal.util.VecHelper;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.permission.flag.GDFlags;
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
            }
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
                if (GDFlags.ENTER_CLAIM && !enterBlacklisted && GDPermissionManager.getInstance().getFinalPermission(event, toLocation, toClaim, Flags.ENTER_CLAIM, targetEntity, targetEntity, user) == Tristate.FALSE) {
                    gpEvent.cancelled(true);
                    if (event != null) {
                        event.setCancelled(true);
                    }
                }

                // exit
                if (GDFlags.EXIT_CLAIM && !exitBlacklisted && GDPermissionManager.getInstance().getFinalPermission(event, fromLocation, fromClaim, Flags.EXIT_CLAIM, targetEntity, targetEntity, user) == Tristate.FALSE) {
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

        if (player != null && GDFlags.ENTER_CLAIM && !enterBlacklisted && user != null && user.getInternalPlayerData().lastClaim != null) {
            final GDClaim lastClaim = (GDClaim) user.getInternalPlayerData().lastClaim.get();
            if (lastClaim != null && lastClaim != fromClaim) {
                if (GDPermissionManager.getInstance().getFinalPermission(event, toLocation, toClaim, Flags.ENTER_CLAIM, targetEntity, targetEntity, player, TrustTypes.ACCESSOR, true) == Tristate.FALSE) {
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
            final GDPlayerData playerData = user.getInternalPlayerData();
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
                    if (player != null) {
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
                    }

                    if (toClaim.isInTown()) {
                        user.getInternalPlayerData().inTown = true;
                    } else {
                        user.getInternalPlayerData().inTown = false;
                    }
                    if (player != null) {
                        this.checkPlayerFlight(user, fromClaim, toClaim);
                        this.checkPlayerGameMode(user, fromClaim, toClaim);
                        this.checkPlayerGodMode(user, fromClaim, toClaim);
                        this.checkPlayerWalkSpeed(user, fromClaim, toClaim);
                        this.checkPlayerWeather(user, fromClaim, toClaim, false);
                        this.runPlayerCommands(fromClaim, user, false);
                        this.runPlayerCommands(toClaim, user, true);
                    }
            }

            GDTimings.ENTITY_MOVE_EVENT.stopTimingIfSync();
            return true;
        }

        if (fromClaim != toClaim) {
            boolean enterCancelled = false;
            boolean exitCancelled = false;
            // enter
            if (GDFlags.ENTER_CLAIM && !enterBlacklisted && GDPermissionManager.getInstance().getFinalPermission(event, toLocation, toClaim, Flags.ENTER_CLAIM, targetEntity, targetEntity, user, true) == Tristate.FALSE) {
                enterCancelled = true;
                gpEvent.cancelled(true);
            }

            // exit
            if (GDFlags.EXIT_CLAIM && !exitBlacklisted && GDPermissionManager.getInstance().getFinalPermission(event, fromLocation, fromClaim, Flags.EXIT_CLAIM, targetEntity, targetEntity, user, true) == Tristate.FALSE) {
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

            if (player != null) {
			    if (GDFlags.ENTITY_RIDING && onMount) {
                    if (GDPermissionManager.getInstance().getFinalPermission(event, targetEntity.getLocation(), toClaim, Flags.ENTITY_RIDING, player, targetEntity, player, TrustTypes.ACCESSOR, true) == Tristate.FALSE) {
                        event.setCancelled(true);
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
                final GDPlayerData playerData = user.getInternalPlayerData();
                final boolean showGpPrefix = GriefDefenderPlugin.getGlobalConfig().getConfig().message.enterExitShowGdPrefix;
                playerData.lastClaim = new WeakReference<>(toClaim);
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
                    this.checkPlayerFlight(user, fromClaim, toClaim);
                    this.checkPlayerGameMode(user, fromClaim, toClaim);
                    this.checkPlayerGodMode(user, fromClaim, toClaim);
                    this.checkPlayerWalkSpeed(user, fromClaim, toClaim);
                    this.checkPlayerWeather(user, fromClaim, toClaim, false);
                    this.runPlayerCommands(fromClaim, user, false);
                    this.runPlayerCommands(toClaim, user, true);
                }
            }
        }

        GDTimings.ENTITY_MOVE_EVENT.stopTimingIfSync();
        return true;
    }

    final static Pattern pattern = Pattern.compile("([^\\s]+)", Pattern.MULTILINE);

    private void runPlayerCommands(GDClaim claim, GDPermissionUser user, boolean enter) {
        final Player player = user.getOnlinePlayer();
        if (player == null) {
            // Most likely NPC
            return;
        }

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
                .replace("%owner%", claim.getOwnerFriendlyName())
                .replace("%uuid%", player.getUniqueId().toString())
                .replace("%world%", claim.getWorld().getName())
                .replace("%server%", PermissionUtil.getInstance().getServerName())
                .replace("%location%", BlockUtil.getInstance().posToString(player.getLocation()));
        return command;
    }

    private void checkPlayerFlight(GDPermissionUser user, GDClaim fromClaim, GDClaim toClaim) {
        final Player player = user.getOnlinePlayer();
        if (player == null) {
            // Most likely NPC
            return;
        }

        final GDPlayerData playerData = user.getInternalPlayerData();
        final GameMode gameMode = player.get(Keys.GAME_MODE).orElse(null);
        if (gameMode == null || gameMode == GameModes.CREATIVE || gameMode == GameModes.SPECTATOR) {
            return;
        }

        if (fromClaim == toClaim || !player.get(Keys.IS_FLYING).get()) {
            // only handle player-fly in enter/exit
            return;
        }

        final Boolean noFly = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Boolean.class), playerData.getSubject(), Options.PLAYER_DENY_FLIGHT, toClaim);
        final boolean adminFly = player.hasPermission(GDPermissions.BYPASS_OPTION + "." + Options.PLAYER_DENY_FLIGHT.getName().toLowerCase());
        final boolean ownerFly = toClaim.isBasicClaim() ? player.hasPermission(GDPermissions.USER_OPTION_PERK_OWNER_FLY_BASIC) : toClaim.isTown() ? player.hasPermission(GDPermissions.USER_OPTION_PERK_OWNER_FLY_TOWN) : false;
        if (player.getUniqueId().equals(toClaim.getOwnerUniqueId()) && ownerFly) {
            return;
        }
        if (!adminFly && noFly) {
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
        final Player player = user.getOnlinePlayer();
        if (player == null) {
            // Most likely NPC
            return;
        }

        final GDPlayerData playerData = user.getInternalPlayerData();
        final GameMode gameMode = player.get(Keys.GAME_MODE).get();
        if (gameMode == GameModes.CREATIVE || gameMode == GameModes.SPECTATOR || !player.get(Keys.INVULNERABLE).get()) {
            return;
        }

        if (fromClaim == toClaim) {
            return;
        }

        final Boolean noGodMode = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Boolean.class), playerData.getSubject(), Options.PLAYER_DENY_GODMODE, toClaim);
        final boolean bypassOption = player.hasPermission(GDPermissions.BYPASS_OPTION + "." + Options.PLAYER_DENY_GODMODE.getName().toLowerCase());
        if (!bypassOption && noGodMode) {
            player.offer(Keys.INVULNERABLE, false);
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().OPTION_APPLY_PLAYER_DENY_GODMODE);
        }
    }

    private void checkPlayerGameMode(GDPermissionUser user, GDClaim fromClaim, GDClaim toClaim) {
        if (fromClaim == toClaim) {
            return;
        }

        final Player player = user.getOnlinePlayer();
        if (player == null) {
            // Most likely NPC
            return;
        }

        final GDPlayerData playerData = user.getInternalPlayerData();
        final GameMode currentGameMode = player.get(Keys.GAME_MODE).get();
        final GameModeType gameModeType = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(GameModeType.class), playerData.getSubject(), Options.PLAYER_GAMEMODE, toClaim);
        final boolean bypassOption = player.hasPermission(GDPermissions.BYPASS_OPTION + "." + Options.PLAYER_GAMEMODE.getName().toLowerCase());
        if (!bypassOption && gameModeType != null && gameModeType != GameModeTypes.UNDEFINED) {
            final GameMode newGameMode = PlayerUtil.GAMEMODE_MAP.get(gameModeType);
            if (newGameMode != currentGameMode) {
                player.offer(Keys.GAME_MODE, PlayerUtil.GAMEMODE_MAP.get(gameModeType));
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.OPTION_APPLY_PLAYER_GAMEMODE,
                        ImmutableMap.of(
                        "gamemode", gameModeType.getName()));
                GriefDefenderPlugin.sendMessage(player, message);
            }
        }
    }

    private void checkPlayerWalkSpeed(GDPermissionUser user, GDClaim fromClaim, GDClaim toClaim) {
        if (fromClaim == toClaim) {
            return;
        }

        final Player player = user.getOnlinePlayer();
        if (player == null) {
            // Most likely NPC
            return;
        }

        final GDPlayerData playerData = user.getInternalPlayerData();
        final double currentWalkSpeed = player.get(Keys.WALKING_SPEED).get();
        final double walkSpeed = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Double.class), playerData.getSubject(), Options.PLAYER_WALK_SPEED, toClaim);
        final boolean bypassOption = player.hasPermission(GDPermissions.BYPASS_OPTION + "." + Options.PLAYER_WALK_SPEED.getName().toLowerCase());
        if (!bypassOption && walkSpeed > 0) {
            if (currentWalkSpeed != walkSpeed) {
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.OPTION_APPLY_PLAYER_WALK_SPEED,
                        ImmutableMap.of(
                        "speed", walkSpeed));
                GriefDefenderPlugin.sendMessage(player, message);
            }
        }
    }

    public void checkPlayerWeather(GDPermissionUser user, GDClaim fromClaim, GDClaim toClaim, boolean force) {
        if (!force && fromClaim == toClaim) {
            return;
        }

        final Player player = user.getOnlinePlayer();
        if (player == null) {
            // Most likely NPC
            return;
        }

        final GDPlayerData playerData = user.getInternalPlayerData();
        final WeatherType weatherType = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(WeatherType.class), playerData.getSubject(), Options.PLAYER_WEATHER, toClaim);
        if (weatherType != null) {
            final Weather currentWeather = player.getWorld().getWeather();
            PlayerUtil.getInstance().setPlayerWeather(user, weatherType);
            // TODO - improve so it doesn't spam
            /*final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.OPTION_APPLY_PLAYER_WEATHER,
                    ImmutableMap.of(
                    "weather", weatherType == WeatherTypes.UNDEFINED ? currentWeather.getName().toUpperCase() : weatherType.getName().toUpperCase()));
            GriefDefenderPlugin.sendMessage(player, message);*/
        } else {
            final WeatherType currentWeather = playerData.lastWeatherType;
            PlayerUtil.getInstance().resetPlayerWeather(user);
            final WeatherType newWeather = playerData.lastWeatherType;
            /*if (currentWeather != newWeather) {
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.OPTION_APPLY_PLAYER_WEATHER,
                        ImmutableMap.of(
                        "weather", newWeather.getName().toUpperCase()));
                GriefDefenderPlugin.sendMessage(player, message);
            }*/
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
