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

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GDTimings;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimBlockSystem;
import com.griefdefender.api.claim.ClaimResult;
import com.griefdefender.api.claim.ClaimResultType;
import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.api.claim.ClaimTypes;
import com.griefdefender.api.claim.ShovelTypes;
import com.griefdefender.api.claim.TrustType;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.api.permission.flag.Flag;
import com.griefdefender.api.permission.flag.Flags;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.api.permission.option.type.CreateModeTypes;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.claim.GDClaimManager;
import com.griefdefender.command.CommandHelper;
import com.griefdefender.configuration.GriefDefenderConfig;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.internal.provider.WorldEditProvider;
import com.griefdefender.internal.util.BlockUtil;
import com.griefdefender.internal.util.NMSUtil;
import com.griefdefender.internal.visual.ClaimVisual;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.permission.flag.GDFlags;
import com.griefdefender.provider.NucleusProvider;
import com.griefdefender.storage.BaseStorage;
import com.griefdefender.util.CauseContextHelper;
import com.griefdefender.util.EconomyUtil;
import com.griefdefender.util.PaginationUtil;
import com.griefdefender.util.PlayerUtil;
import com.griefdefender.util.SpongeUtil;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;
import net.kyori.text.serializer.gson.GsonComponentSerializer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.command.CommandMapping;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.type.HandType;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.ArmorStand;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.action.InteractEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.entity.living.humanoid.HandInteractEvent;
import org.spongepowered.api.event.entity.living.humanoid.player.RespawnPlayerEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.item.inventory.ChangeInventoryEvent;
import org.spongepowered.api.event.item.inventory.ClickInventoryEvent;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.event.item.inventory.InteractInventoryEvent;
import org.spongepowered.api.event.item.inventory.InteractItemEvent;
import org.spongepowered.api.event.item.inventory.UseItemStackEvent;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.transaction.SlotTransaction;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.ban.BanService;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.channel.MessageChannel;
import org.spongepowered.api.text.channel.MessageReceiver;
import org.spongepowered.api.text.channel.MutableMessageChannel;
import org.spongepowered.api.text.channel.type.FixedMessageChannel;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.common.SpongeImpl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class PlayerEventHandler {

    private final BaseStorage dataStore;
    private final WorldEditProvider worldEditProvider;
    private final BanService banService;
    private int lastInteractItemPrimaryTick = -1;
    private int lastInteractItemSecondaryTick = -1;
    private boolean lastInteractItemCancelled = false;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault());

    public PlayerEventHandler(BaseStorage dataStore, GriefDefenderPlugin plugin) {
        this.dataStore = dataStore;
        this.worldEditProvider = GriefDefenderPlugin.getInstance().worldEditProvider;
        this.banService = Sponge.getServiceManager().getRegistration(BanService.class).get().getProvider();
    }

    @Listener(order = Order.POST)
    public void onPlayerChatPost(MessageChannelEvent.Chat event) {
        final CommandSource commandSource = (CommandSource) event.getSource();
        final MessageChannel channel = event.getChannel().orElse(null);
        if (channel != null) {
            final MutableMessageChannel mutableChannel = channel.asMutable();
            final Iterator<MessageReceiver> iterator = mutableChannel.getMembers().iterator();
            List<MessageReceiver> receiversToRemove = new ArrayList<>();
            while (iterator.hasNext()) {
                final MessageReceiver receiver = iterator.next();
                if (receiver == commandSource) {
                    continue;
                }
                if (receiver instanceof Player) {
                    final Player playerReceiver = (Player) receiver;
                    final GDPlayerData receiverData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(playerReceiver.getWorld(), playerReceiver.getUniqueId());
                    if (receiverData.isRecordingChat()) {
                        receiversToRemove.add(receiver);
                        final Component message = GsonComponentSerializer.INSTANCE.deserialize(TextSerializers.JSON.serialize(event.getMessage()));
                        final Component component = TextComponent.builder()
                                .append(TextComponent.builder()
                                        .append(message)
                                        .hoverEvent(HoverEvent.showText(TextComponent.of(formatter.format(Instant.now()))))
                                        .build())
                                .build();
                        receiverData.chatLines.add(component);
                    }
                }
            }
            for (MessageReceiver receiver : receiversToRemove) {
                mutableChannel.removeMember(receiver);
            }
            event.setChannel(mutableChannel);
        }
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onPlayerChat(MessageChannelEvent.Chat event, @First Player player) {
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUniqueId())) {
            return;
        }

        GDTimings.PLAYER_CHAT_EVENT.startTimingIfSync();
        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        // check for command input
        if (playerData.isWaitingForInput()) {
            playerData.commandInput = event.getRawMessage().toPlain();
            playerData.commandConsumer.accept(player);
            event.setCancelled(true);
            return;
        }

        if (playerData.inTown && playerData.townChat) {
            final MessageChannel channel = event.getChannel().orElse(null);
            if (GriefDefenderPlugin.getInstance().nucleusApiProvider != null && channel != null) {
                if (GriefDefenderPlugin.getInstance().nucleusApiProvider.isChatChannel(channel)) {
                    GDTimings.PLAYER_CHAT_EVENT.stopTimingIfSync();
                    return;
                }
            }
            final GDClaim sourceClaim = this.dataStore.getClaimAtPlayer(playerData, player.getLocation());
            if (sourceClaim.isInTown()) {
                playerData.inTown = true;
            } else {
                playerData.inTown = false;
            }
            final GDClaim sourceTown = sourceClaim.getTownClaim();
            final Component townTag = sourceTown.getTownData().getTownTag().orElse(null);

            Text header = event.getFormatter().getHeader().toText();
            Text body = event.getFormatter().getBody().toText();
            Text footer = event.getFormatter().getFooter().toText();
            Text townMessage = Text.of(TextColors.GREEN, body);
            if (townTag != null) {
                townMessage = Text.of(SpongeUtil.getSpongeText(townTag), townMessage);
            }
            event.setMessage(townMessage);
            Set<CommandSource> recipientsToRemove = new HashSet<>();
            Iterator<MessageReceiver> iterator = event.getChannel().get().getMembers().iterator();
            while (iterator.hasNext()) {
                MessageReceiver receiver = iterator.next();
                if (receiver instanceof Player) {
                    Player recipient = (Player) receiver;
                    if (GriefDefenderPlugin.getInstance().nucleusApiProvider != null) {
                        if (NucleusProvider.getPrivateMessagingService().isPresent() && NucleusProvider.getPrivateMessagingService().get().isSocialSpy(recipient)) {
                            // always allow social spy users
                            continue;
                        }
                    }

                    final GDPlayerData targetPlayerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(recipient.getWorld(), recipient.getUniqueId());
                    if (!targetPlayerData.inTown) {
                        recipientsToRemove.add(recipient);
                        continue;
                    }

                    final GDClaim targetClaim = this.dataStore.getClaimAtPlayer(targetPlayerData, recipient.getLocation());
                    final GDClaim targetTown = targetClaim.getTownClaim();
                    if (targetPlayerData.canIgnoreClaim(targetClaim)) {
                        continue;
                    }
                    if (sourceTown != null && (targetTown == null || !sourceTown.getUniqueId().equals(targetTown.getUniqueId()))) {
                        recipientsToRemove.add(recipient);
                    }
                }
            }

            if (!recipientsToRemove.isEmpty()) {
                Set<MessageReceiver> newRecipients = Sets.newHashSet(event.getChannel().get().getMembers().iterator());
                newRecipients.removeAll(recipientsToRemove);
                event.setChannel(new FixedMessageChannel(newRecipients));
            }
        }

        GDTimings.PLAYER_CHAT_EVENT.stopTimingIfSync();
    }

    // when a player uses a slash command...
    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onPlayerCommand(SendCommandEvent event, @First Player player) {
        if (!GDFlags.COMMAND_EXECUTE && !GDFlags.COMMAND_EXECUTE_PVP) {
            return;
        }
        final boolean commandExecuteSourceBlacklisted = GriefDefenderPlugin.isSourceIdBlacklisted(Flags.COMMAND_EXECUTE.getName(),event.getSource(), player.getWorld().getProperties());
        final boolean commandExecutePvpSourceBlacklisted = GriefDefenderPlugin.isSourceIdBlacklisted(Flags.COMMAND_EXECUTE_PVP.getName(),event.getSource(), player.getWorld().getProperties());

        GDTimings.PLAYER_COMMAND_EVENT.startTimingIfSync();
        String command = event.getCommand();
        String[] args = event.getArguments().split(" ");
        String[] parts = command.split(":");
        String pluginId = null;

        if (parts.length > 1) {
            pluginId = parts[0];
            command = parts[1];
        }

        String message = "/" + event.getCommand() + " " + event.getArguments();
        if (pluginId == null || !pluginId.equals("minecraft")) {
            CommandMapping commandMapping = Sponge.getCommandManager().get(command).orElse(null);
            PluginContainer pluginContainer = null;
            if (commandMapping != null) {
                pluginContainer = Sponge.getCommandManager().getOwner(commandMapping).orElse(null);
                if (pluginContainer != null) {
                    pluginId = pluginContainer.getId();
                }
            }
            if (pluginId == null) {
                pluginId = "minecraft";
            }
        }

        PaginationUtil.getInstance().updateActiveCommand(player.getUniqueId(), command, event.getArguments());
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUniqueId())) {
            GDTimings.PLAYER_COMMAND_EVENT.stopTimingIfSync();
            return;
        }

        GDPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        // if requires access trust, check for permission
        Location<World> location = player.getLocation();
        GDClaim claim = this.dataStore.getClaimAtPlayer(playerData, location);
        if (playerData.canIgnoreClaim(claim)) {
            GDTimings.PLAYER_COMMAND_EVENT.stopTimingIfSync();
            return;
        }

        final int combatTimeRemaining = playerData.getPvpCombatTimeRemaining();
        final boolean inPvpCombat = combatTimeRemaining > 0;
        final boolean pvpCombatCommand = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Boolean.class), player, Options.PVP_COMBAT_COMMAND);
        if (!pvpCombatCommand && inPvpCombat) {
            final Component denyMessage = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PVP_IN_COMBAT_NOT_ALLOWED,
                    ImmutableMap.of(
                    "time-remaining", combatTimeRemaining));
            GriefDefenderPlugin.sendMessage(player, denyMessage);
            event.setCancelled(true);
            GDTimings.PLAYER_COMMAND_EVENT.stopTimingIfSync();
            return;
        }

        String commandBaseTarget = pluginId + ":" + command;
        String commandTargetWithArgs = commandBaseTarget;
        // first check the args
        for (String arg : args) {
            if (!arg.isEmpty()) {
                commandTargetWithArgs = commandTargetWithArgs + "." + arg;
            }
        }

        boolean commandExecuteTargetBlacklisted = false;
        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.COMMAND_EXECUTE.getName(), commandBaseTarget, player.getWorld().getProperties())) {
            commandExecuteTargetBlacklisted = true;
        } else if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.COMMAND_EXECUTE.getName(), commandTargetWithArgs, player.getWorld().getProperties())) {
            commandExecuteTargetBlacklisted = true;
        }

        if (GDFlags.COMMAND_EXECUTE && !inPvpCombat && !commandExecuteSourceBlacklisted && !commandExecuteTargetBlacklisted) {
            // First check base command
            Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, player.getLocation(), claim, Flags.COMMAND_EXECUTE, event.getSource(), commandBaseTarget, player, TrustTypes.MANAGER, true);
            if (result != Tristate.FALSE && args.length > 0) {
                // Check with args
                // Test with each arg, break once result returns false
                String commandBaseTargetArgCheck = commandBaseTarget;
                for (String arg : args) {
                    if (!arg.isEmpty()) {
                        commandBaseTargetArgCheck = commandBaseTargetArgCheck + "." + arg;
                        result = GDPermissionManager.getInstance().getFinalPermission(event, player.getLocation(), claim, Flags.COMMAND_EXECUTE, event.getSource(), commandBaseTargetArgCheck, player, TrustTypes.MANAGER, true);
                        if (result == Tristate.FALSE) {
                            break;
                        }

                    }
                }
            }
            if (result == Tristate.FALSE) {
                final Component denyMessage = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.COMMAND_BLOCKED,
                        ImmutableMap.of(
                        "command", command,
                        "player", claim.getOwnerName()));
                GriefDefenderPlugin.sendMessage(player, denyMessage);
                event.setCancelled(true);
                GDTimings.PLAYER_COMMAND_EVENT.stopTimingIfSync();
                return;
            }
            GDTimings.PLAYER_COMMAND_EVENT.stopTimingIfSync();
            return;
        }
        if (GDFlags.COMMAND_EXECUTE_PVP && inPvpCombat && !commandExecuteSourceBlacklisted && !commandExecuteTargetBlacklisted) {
            // First check base command
            Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, player.getLocation(), claim, Flags.COMMAND_EXECUTE_PVP, event.getSource(), commandBaseTarget, player, true);
            if (result != Tristate.FALSE && args.length > 0) {
                // check with args
                // Test with each arg, break once result returns false
                String commandBaseTargetArgCheck = commandBaseTarget;
                for (String arg : args) {
                    if (!arg.isEmpty()) {
                        commandBaseTargetArgCheck = commandBaseTargetArgCheck + "." + arg;
                        result = GDPermissionManager.getInstance().getFinalPermission(event, player.getLocation(), claim, Flags.COMMAND_EXECUTE_PVP, event.getSource(), commandBaseTargetArgCheck, player, true);
                        if (result == Tristate.FALSE) {
                            break;
                        }
                    }
                }
            }
            if (result == Tristate.FALSE) {
                final Component denyMessage = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.COMMAND_BLOCKED,
                        ImmutableMap.of(
                        "command", command,
                        "player", claim.getOwnerName()));
                GriefDefenderPlugin.sendMessage(player, denyMessage);
                event.setCancelled(true);
                GDTimings.PLAYER_COMMAND_EVENT.stopTimingIfSync();
                return;
            }
        }

        GDTimings.PLAYER_COMMAND_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onPlayerLogin(ClientConnectionEvent.Login event) {
        GDTimings.PLAYER_LOGIN_EVENT.startTimingIfSync();
        User player = event.getTargetUser();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(event.getToTransform().getExtent().getUniqueId())) {
            GDTimings.PLAYER_LOGIN_EVENT.stopTimingIfSync();
            return;
        }

        final WorldProperties worldProperties = event.getToTransform().getExtent().getProperties();
        final UUID playerUniqueId = player.getUniqueId();
        final GDClaimManager claimWorldManager = this.dataStore.getClaimWorldManager(worldProperties.getUniqueId());
        final Instant dateNow = Instant.now();
        for (Claim claim : claimWorldManager.getWorldClaims()) {
            if (claim.getType() != ClaimTypes.ADMIN && claim.getOwnerUniqueId().equals(playerUniqueId)) {
                claim.getData().setDateLastActive(dateNow);
                for (Claim subdivision : ((GDClaim) claim).children) {
                    subdivision.getData().setDateLastActive(dateNow);
                }
                ((GDClaim) claim).getInternalClaimData().setRequiresSave(true);
            }
        }
        GDTimings.PLAYER_LOGIN_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST)
    public void onPlayerJoin(ClientConnectionEvent.Join event) {
        GDTimings.PLAYER_JOIN_EVENT.startTimingIfSync();
        Player player = event.getTargetEntity();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUniqueId())) {
            GDTimings.PLAYER_JOIN_EVENT.stopTimingIfSync();
            return;
        }

        UUID playerID = player.getUniqueId();

        final GDPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), playerID);
        final GDClaim claim = this.dataStore.getClaimAtPlayer(playerData, player.getLocation());
        if (claim.isInTown()) {
            playerData.inTown = true;
        }

        GDTimings.PLAYER_JOIN_EVENT.stopTimingIfSync();
    }

    @Listener(order= Order.LAST)
    public void onPlayerQuit(ClientConnectionEvent.Disconnect event) {
        final Player player = event.getTargetEntity();
        if (!SpongeImpl.getServer().isServerRunning() || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUniqueId())) {
            return;
        }

        GDTimings.PLAYER_QUIT_EVENT.startTimingIfSync();
        UUID playerID = player.getUniqueId();
        GDPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), playerID);

        if (this.worldEditProvider != null) {
            this.worldEditProvider.revertVisuals(player, playerData, null);
            this.worldEditProvider.removePlayer(player);
        }

        playerData.onDisconnect();
        PaginationUtil.getInstance().removeActivePageData(player.getUniqueId());
        if (playerData.getClaims().isEmpty()) {
            this.dataStore.clearCachedPlayerData(player.getWorld().getUniqueId(), playerID);
        }

        GDTimings.PLAYER_QUIT_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onPlayerDeath(DestructEntityEvent.Death event) {
        if (!(event.getTargetEntity() instanceof Player)) {
            return;
        }

        final Player player = (Player) event.getTargetEntity();
        GDCauseStackManager.getInstance().pushCause(player);
        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        final GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(playerData, player.getLocation());
        final Tristate keepInventory = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Tristate.class), playerData.getSubject(), Options.PLAYER_KEEP_INVENTORY, claim);
        //final Tristate keepLevel = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Tristate.class), playerData.getSubject(), Options.PLAYER_KEEP_LEVEL, claim);
        if (keepInventory != Tristate.UNDEFINED) {
            event.setKeepInventory(keepInventory.asBoolean());
        }
        //if (keepLevel != Tristate.UNDEFINED) {
        //    event.setKeepLevel(keepLevel.asBoolean());
       // }
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onPlayerRespawn(RespawnPlayerEvent event) {
        final World world = event.getToTransform().getExtent();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUniqueId())) {
            return;
        }

        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(event.getTargetEntity().getWorld(), event.getTargetEntity().getUniqueId());
        playerData.lastPvpTimestamp = null;
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onPlayerDispenseItem(DropItemEvent.Dispense event, @Root Entity spawncause) {
        if (!GDFlags.ITEM_DROP || !(spawncause instanceof User)) {
            return;
        }

        final User user = (User) spawncause;
        final World world = spawncause.getWorld();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUniqueId())) {
            return;
        }

        GDTimings.PLAYER_DISPENSE_ITEM_EVENT.startTimingIfSync();
        Player player = user instanceof Player ? (Player) user : null;
        GDPlayerData playerData = this.dataStore.getOrCreatePlayerData(world, user.getUniqueId());

        for (Entity entityItem : event.getEntities()) {
            if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.ITEM_DROP.toString(), entityItem, world.getProperties())) {
                continue;
            }

            Location<World> location = entityItem.getLocation();
            GDClaim claim = this.dataStore.getClaimAtPlayer(playerData, location);
            if (claim != null) {
                if (GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.ITEM_DROP, user, entityItem, user, TrustTypes.ACCESSOR, true) == Tristate.FALSE) {
                    event.setCancelled(true);
                    if (spawncause instanceof Player) {
                        final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_ITEM_DROP,
                                ImmutableMap.of(
                                "player", claim.getOwnerName(),
                                "item", entityItem.getType().getId()));
                        GriefDefenderPlugin.sendClaimDenyMessage(claim, player, message);
                    }
                    GDTimings.PLAYER_DISPENSE_ITEM_EVENT.stopTimingIfSync();
                    return;
                }
            }
        }
        GDTimings.PLAYER_DISPENSE_ITEM_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onPlayerInteractInventoryOpen(InteractInventoryEvent.Open event, @First Player player) {
        if (!GDFlags.INTERACT_INVENTORY || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUniqueId())) {
            return;
        }

        final Cause cause = event.getCause();
        final EventContext context = cause.getContext();
        final BlockSnapshot blockSnapshot = context.get(EventContextKeys.BLOCK_HIT).orElse(BlockSnapshot.NONE);
        if (blockSnapshot == BlockSnapshot.NONE) {
            return;
        }
        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.INTERACT_INVENTORY.getName(), blockSnapshot, player.getWorld().getProperties())) {
            return;
        }

        GDTimings.PLAYER_INTERACT_INVENTORY_OPEN_EVENT.startTimingIfSync();
        final Location<World> location = blockSnapshot.getLocation().get();
        final GDClaim claim = this.dataStore.getClaimAt(location);
        final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.INTERACT_INVENTORY, player, blockSnapshot, player, TrustTypes.CONTAINER, true);
        if (result == Tristate.FALSE) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_INVENTORY_OPEN,
                    ImmutableMap.of(
                    "player", claim.getOwnerName(),
                    "block", blockSnapshot.getState().getType().getId()));
            GriefDefenderPlugin.sendClaimDenyMessage(claim, player, message);
            NMSUtil.getInstance().closePlayerScreen(player);
            event.setCancelled(true);
        }

        GDTimings.PLAYER_INTERACT_INVENTORY_OPEN_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onPlayerInteractInventoryClose(InteractInventoryEvent.Close event, @Root Player player) {
        final ItemStackSnapshot cursor = event.getCursorTransaction().getOriginal();
        if (cursor == ItemStackSnapshot.NONE || !GDFlags.ITEM_DROP || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUniqueId())) {
            return;
        }
        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.ITEM_DROP.getName(), cursor, player.getWorld().getProperties())) {
            return;
        }

        GDTimings.PLAYER_INTERACT_INVENTORY_CLOSE_EVENT.startTimingIfSync();
        final Location<World> location = player.getLocation();
        final GDClaim claim = this.dataStore.getClaimAt(location);
        if (GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.ITEM_DROP, player, cursor, player, TrustTypes.ACCESSOR, true) == Tristate.FALSE) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_ITEM_DROP,
                    ImmutableMap.of(
                    "player", claim.getOwnerName(),
                    "item", cursor.getType().getId()));
            GriefDefenderPlugin.sendClaimDenyMessage(claim, player, message);
            event.setCancelled(true);
        }

        GDTimings.PLAYER_INTERACT_INVENTORY_CLOSE_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onPlayerInteractInventoryClick(ClickInventoryEvent event, @First Player player) {
        if (!GDFlags.INTERACT_INVENTORY_CLICK || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUniqueId())) {
            return;
        }

        if (NMSUtil.getInstance().isContainerCustomInventory(event.getTargetInventory())) {
        }

        GDTimings.PLAYER_INTERACT_INVENTORY_CLICK_EVENT.startTimingIfSync();
        final Location<World> location = player.getLocation();
        final GDClaim claim = this.dataStore.getClaimAt(location);
        final boolean isDrop = event instanceof ClickInventoryEvent.Drop;
        final ItemStackSnapshot cursorItem = event.getCursorTransaction().getOriginal();
        // check if original cursor item can be dropped
        if (isDrop && cursorItem != ItemStackSnapshot.NONE && !GriefDefenderPlugin.isTargetIdBlacklisted(Flags.ITEM_DROP.getName(), cursorItem, player.getWorld().getProperties())) {
            if (GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.ITEM_DROP, player, cursorItem, player, TrustTypes.ACCESSOR, true) == Tristate.FALSE) {
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_ITEM_DROP,
                        ImmutableMap.of(
                        "player", claim.getOwnerName(),
                        "item", cursorItem.getType().getId()));
                GriefDefenderPlugin.sendClaimDenyMessage(claim, player, message);
                event.setCancelled(true);
                GDTimings.PLAYER_INTERACT_INVENTORY_CLICK_EVENT.stopTimingIfSync();
                return;
            }
        }
        for (SlotTransaction transaction : event.getTransactions()) {
            if (transaction.getOriginal() == ItemStackSnapshot.NONE) {
                continue;
            }

            if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.INTERACT_INVENTORY_CLICK.getName(), transaction.getOriginal(), player.getWorld().getProperties())) {
                continue;
            }

            final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.INTERACT_INVENTORY_CLICK, player, transaction.getOriginal(), player, TrustTypes.CONTAINER, true);
            if (result == Tristate.FALSE) {
                Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_INTERACT_ITEM,
                        ImmutableMap.of(
                        "player", claim.getOwnerName(),
                        "item", transaction.getOriginal().getType().getId()));
                GriefDefenderPlugin.sendClaimDenyMessage(claim, player, message);
                event.setCancelled(true);
                GDTimings.PLAYER_INTERACT_INVENTORY_CLICK_EVENT.stopTimingIfSync();
                return;
            }

            if (isDrop && transaction.getFinal() != ItemStackSnapshot.NONE) {
                if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.ITEM_DROP.getName(), transaction.getFinal(), player.getWorld().getProperties())) {
                    continue;
                }

                if (GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.ITEM_DROP, player, transaction.getFinal(), player, TrustTypes.ACCESSOR, true) == Tristate.FALSE) {
                    final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_ITEM_DROP,
                            ImmutableMap.of(
                            "player", claim.getOwnerName(),
                            "item", transaction.getFinal().getType().getId()));
                    GriefDefenderPlugin.sendClaimDenyMessage(claim, player, message);
                    event.setCancelled(true);
                    GDTimings.PLAYER_INTERACT_INVENTORY_CLICK_EVENT.stopTimingIfSync();
                    return;
                }
            }
        }
        GDTimings.PLAYER_INTERACT_INVENTORY_CLICK_EVENT.stopTimingIfSync();
    }
 
    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onPlayerInteractEntity(InteractEntityEvent.Primary event, @First Player player) {
        if (!GDFlags.INTERACT_ENTITY_PRIMARY || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUniqueId())) {
            return;
        }

        final Entity targetEntity = event.getTargetEntity();
        if (targetEntity instanceof Player) {
            // PvP is handled during entity damage
            return;
        }
        final HandType handType = event.getHandType();
        final ItemStack itemInHand = player.getItemInHand(handType).orElse(ItemStack.empty());
        final Object source = player;
        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.INTERACT_ENTITY_PRIMARY.getName(), targetEntity, player.getWorld().getProperties())) {
            return;
        }

        GDTimings.PLAYER_INTERACT_ENTITY_PRIMARY_EVENT.startTimingIfSync();
        // check give pet
        final GDPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        if (playerData.petRecipientUniqueId != null) {
            playerData.petRecipientUniqueId = null;
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().COMMAND_PET_TRANSFER_CANCEL);
            event.setCancelled(true);
            return;
        }
        if (targetEntity instanceof Living && targetEntity.get(Keys.TAMED_OWNER).isPresent()) {
            final UUID ownerID = targetEntity.get(Keys.TAMED_OWNER).get().orElse(null);
            if (ownerID != null) {
                // always allow owner to interact with their pets
                if (player.getUniqueId().equals(ownerID)) {
                    GDTimings.PLAYER_INTERACT_ENTITY_PRIMARY_EVENT.stopTimingIfSync();
                    return;
                }
                // If pet protection is enabled, deny the interaction
                if (GriefDefenderPlugin.getActiveConfig(player.getWorld().getProperties()).getConfig().claim.protectTamedEntities) {
                    final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(ownerID);
                    final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_PROTECTED_ENTITY,
                            ImmutableMap.of(
                            "player", user.getName()));
                    GriefDefenderPlugin.sendMessage(player, message);
                    event.setCancelled(true);
                    GDTimings.PLAYER_INTERACT_ENTITY_PRIMARY_EVENT.stopTimingIfSync();
                    return;
                }
            }
        }

        Location<World> location = targetEntity.getLocation();
        GDClaim claim = this.dataStore.getClaimAt(location);
        if (event.isCancelled() && claim.getData().getPvpOverride() == Tristate.TRUE && targetEntity instanceof Player) {
            event.setCancelled(false);
        }

        Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.INTERACT_ENTITY_PRIMARY, source, targetEntity, player, TrustTypes.ACCESSOR, true);
        if (result == Tristate.FALSE) {
            if (GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.ENTITY_DAMAGE, source, targetEntity, player, TrustTypes.ACCESSOR, true) != Tristate.TRUE) {
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_PROTECTED_ENTITY,
                        ImmutableMap.of(
                        "player", claim.getOwnerName()));
                GriefDefenderPlugin.sendMessage(player, message);
                event.setCancelled(true);
                this.sendInteractEntityDenyMessage(itemInHand, targetEntity, claim, player, handType);
                GDTimings.PLAYER_INTERACT_ENTITY_PRIMARY_EVENT.stopTimingIfSync();
                return;
            }
        }
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onPlayerInteractEntity(InteractEntityEvent.Secondary event, @First Player player) {
        if (!GDFlags.INTERACT_ENTITY_SECONDARY || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUniqueId())) {
            return;
        }

        final Entity targetEntity = event.getTargetEntity();
        final HandType handType = event.getHandType();
        final ItemStack itemInHand = player.getItemInHand(handType).orElse(ItemStack.empty());
        final Object source = player;
        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.INTERACT_ENTITY_SECONDARY.getName(), targetEntity, player.getWorld().getProperties())) {
            return;
        }

        GDTimings.PLAYER_INTERACT_ENTITY_SECONDARY_EVENT.startTimingIfSync();
        final Location<World> location = targetEntity.getLocation();
        final GDClaim claim = this.dataStore.getClaimAt(location);
        final GDPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        if (targetEntity instanceof Living && targetEntity.get(Keys.TAMED_OWNER).isPresent()) {
            final UUID ownerID = targetEntity.get(Keys.TAMED_OWNER).get().orElse(null);
            if (ownerID != null) {
                // always allow owner to interact with their pets
                if (player.getUniqueId().equals(ownerID) || playerData.canIgnoreClaim(claim)) {
                    if (playerData.petRecipientUniqueId != null) {
                        targetEntity.offer(Keys.TAMED_OWNER, Optional.of(playerData.petRecipientUniqueId));
                        playerData.petRecipientUniqueId = null;
                        GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().COMMAND_PET_CONFIRMATION);
                        event.setCancelled(true);
                    }
                    GDTimings.PLAYER_INTERACT_ENTITY_SECONDARY_EVENT.stopTimingIfSync();
                    return;
                }
                // If pet protection is enabled, deny the interaction
                if (GriefDefenderPlugin.getActiveConfig(player.getWorld().getProperties()).getConfig().claim.protectTamedEntities) {
                    final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(ownerID);
                    final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_PROTECTED_ENTITY,
                            ImmutableMap.of(
                            "player", user.getName()));
                    GriefDefenderPlugin.sendMessage(player, message);
                    event.setCancelled(true);
                    GDTimings.PLAYER_INTERACT_ENTITY_SECONDARY_EVENT.stopTimingIfSync();
                    return;
                }
            }
        }

        if (playerData.canIgnoreClaim(claim)) {
            GDTimings.PLAYER_INTERACT_ENTITY_SECONDARY_EVENT.stopTimingIfSync();
            return;
        }

        Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.INTERACT_ENTITY_SECONDARY, source, targetEntity, player, TrustTypes.ACCESSOR, true);
        if (result == Tristate.TRUE && targetEntity instanceof ArmorStand) {
            result = GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.INTERACT_INVENTORY, source, targetEntity, player, TrustTypes.CONTAINER, false);
        }
        if (result == Tristate.FALSE) {
            event.setCancelled(true);
            this.sendInteractEntityDenyMessage(itemInHand, targetEntity, claim, player, handType);
            GDTimings.PLAYER_INTERACT_ENTITY_SECONDARY_EVENT.stopTimingIfSync();
        }
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onPlayerInteractItem(InteractItemEvent event, @Root Player player) {
        if (event instanceof InteractItemEvent.Primary) {
            lastInteractItemPrimaryTick = Sponge.getServer().getRunningTimeTicks();
        } else {
            lastInteractItemSecondaryTick = Sponge.getServer().getRunningTimeTicks();
        }

        final World world = player.getWorld();
        final HandInteractEvent handEvent = (HandInteractEvent) event;
        final ItemStack itemInHand = player.getItemInHand(handEvent.getHandType()).orElse(ItemStack.empty());

        handleItemInteract(event, player, world, itemInHand);
    }

    @Listener(order = Order.LAST, beforeModifications = true)
    public void onPlayerPickupItem(ChangeInventoryEvent.Pickup.Pre event, @Root Player player) {
        if (!GDFlags.ITEM_PICKUP || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUniqueId())) {
            return;
        }
        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.ITEM_PICKUP.getName(), event.getTargetEntity(), player.getWorld().getProperties())) {
            return;
        }

        GDTimings.PLAYER_PICKUP_ITEM_EVENT.startTimingIfSync();
        final World world = player.getWorld();
        GDPlayerData playerData = this.dataStore.getOrCreatePlayerData(world, player.getUniqueId());
        Location<World> location = player.getLocation();
        GDClaim claim = this.dataStore.getClaimAtPlayer(playerData, location);
        if (GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.ITEM_PICKUP, player, event.getTargetEntity(), player, true) == Tristate.FALSE) {
            event.setCancelled(true);
        }

        GDTimings.PLAYER_PICKUP_ITEM_EVENT.stopTimingIfSync();
    }

    @Listener
    public void onPlayerChangeHeldItem(ChangeInventoryEvent.Held event, @First Player player) {
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUniqueId())) {
            return;
        }

        GDTimings.PLAYER_CHANGE_HELD_ITEM_EVENT.startTimingIfSync();
        GDPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());

        int count = 0;
        // if he's switching to the golden shovel
        for (SlotTransaction transaction : event.getTransactions()) {
            ItemStackSnapshot newItemStack = transaction.getFinal();
            if (count == 1 && newItemStack != null && newItemStack.getType().equals(GriefDefenderPlugin.getInstance().modificationTool.getType())) {
                playerData.lastShovelLocation = null;
                playerData.endShovelLocation = null;
                playerData.claimResizing = null;
                // always reset to basic claims mode
                if (playerData.shovelMode != ShovelTypes.BASIC) {
                    playerData.shovelMode = ShovelTypes.BASIC;
                    GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().MODE_BASIC);
                }

                // tell him how many claim blocks he has available
                if (GriefDefenderPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.VOLUME) {
                    final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PLAYER_REMAINING_BLOCKS_3D,
                            ImmutableMap.of(
                            "block-amount", playerData.getRemainingClaimBlocks(),
                            "chunk-amount", playerData.getRemainingChunks()));
                    GriefDefenderPlugin.sendMessage(player, message);
                } else {
                    final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PLAYER_REMAINING_BLOCKS_2D,
                            ImmutableMap.of(
                             "block-amount", playerData.getRemainingClaimBlocks()));
                    GriefDefenderPlugin.sendMessage(player, message);
                }

            } else if (!playerData.claimMode) {
                if (playerData.lastShovelLocation != null) {
                    playerData.revertActiveVisual(player);
                    // check for any active WECUI visuals
                    if (this.worldEditProvider != null) {
                        this.worldEditProvider.revertVisuals(player, playerData, null);
                    }
                }
                playerData.lastShovelLocation = null;
                playerData.endShovelLocation = null;
                playerData.claimResizing = null;
                playerData.shovelMode = ShovelTypes.BASIC;
            }
            count++;
        }
        GDTimings.PLAYER_CHANGE_HELD_ITEM_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onPlayerUseItem(UseItemStackEvent.Start event, @First Player player) {
        if (!GDFlags.ITEM_USE || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUniqueId())) {
            return;
        }
        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.ITEM_USE.getName(), event.getItemStackInUse().getType(), player.getWorld().getProperties())) {
            return;
        }

        GDTimings.PLAYER_USE_ITEM_EVENT.startTimingIfSync();
        Location<World> location = player.getLocation();
        GDPlayerData playerData = this.dataStore.getOrCreatePlayerData(location.getExtent(), player.getUniqueId());
        GDClaim claim = this.dataStore.getClaimAtPlayer(playerData, location);

        final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.ITEM_USE, player, event.getItemStackInUse().getType(), player, TrustTypes.ACCESSOR, true);
        if (result == Tristate.FALSE) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_ITEM_USE,
                    ImmutableMap.of(
                    "item", event.getItemStackInUse().getType().getId()));
            GriefDefenderPlugin.sendClaimDenyMessage(claim, player,  message);
            event.setCancelled(true);
        }
        GDTimings.PLAYER_USE_ITEM_EVENT.stopTimingIfSync();
    }

    @Listener
    public void onInteractBlock(InteractBlockEvent event) {
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onPlayerInteractBlockPrimary(InteractBlockEvent.Primary.MainHand event) {
        User user = CauseContextHelper.getEventUser(event);
        final Object source = CauseContextHelper.getEventFakePlayerSource(event);
        final Player player = source instanceof Player ? (Player) source : null;
        if (player == null || NMSUtil.getInstance().isFakePlayer(player)) {
            if (user == null) {
                user = player;
            }

            this.handleFakePlayerInteractBlockPrimary(event, user, source);
            return;
        }

        final GDPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        final HandType handType = event.getHandType();
        final ItemStack itemInHand = player.getItemInHand(handType).orElse(ItemStack.empty());
        if (event.getTargetBlock() != BlockSnapshot.NONE) {
            // Run our item hook since Sponge no longer fires InteractItemEvent when targetting a non-air block
            if (handleItemInteract(event, player, player.getWorld(), itemInHand).isCancelled()) {
                return;
            }
        } else {
            if (investigateClaim(event, player, event.getTargetBlock(), itemInHand)) {
                event.setCancelled(true);
            }
        }

        if (playerData.claimMode) {
            return;
        }
        // check give pet
        if (playerData.petRecipientUniqueId != null) {
            playerData.petRecipientUniqueId = null;
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().COMMAND_PET_TRANSFER_CANCEL);
            event.setCancelled(true);
            return;
        }
        if (!GDFlags.INTERACT_BLOCK_PRIMARY || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUniqueId())) {
            return;
        }
        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.INTERACT_BLOCK_PRIMARY.getName(), event.getTargetBlock().getState(), player.getWorld().getProperties())) {
            return;
        }

        GDTimings.PLAYER_INTERACT_BLOCK_PRIMARY_EVENT.startTimingIfSync();
        final BlockSnapshot clickedBlock = event.getTargetBlock();
        final Location<World> location = clickedBlock.getLocation().orElse(null);
        if (location == null) {
            GDTimings.PLAYER_INTERACT_BLOCK_PRIMARY_EVENT.stopTimingIfSync();
            return;
        }

        final GDClaim claim = this.dataStore.getClaimAt(location);
        final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.INTERACT_BLOCK_PRIMARY, source, clickedBlock.getState(), player, TrustTypes.BUILDER, true);
        if (result == Tristate.FALSE) {
            if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.BLOCK_BREAK.getName(), clickedBlock.getState(), player.getWorld().getProperties())) {
                GDTimings.PLAYER_INTERACT_BLOCK_PRIMARY_EVENT.stopTimingIfSync();
                return;
            }
            if (GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.BLOCK_BREAK, source, clickedBlock.getState(), player, TrustTypes.BUILDER, true) == Tristate.TRUE) {
                GDTimings.PLAYER_INTERACT_BLOCK_PRIMARY_EVENT.stopTimingIfSync();
                return;
            }

            // Don't send a deny message if the player is holding an investigation tool
            if (Sponge.getServer().getRunningTimeTicks() != lastInteractItemPrimaryTick || lastInteractItemCancelled != true) {
                if (!PlayerUtil.getInstance().hasItemInOneHand(player, GriefDefenderPlugin.getInstance().investigationTool.getType())) {
                    this.sendInteractBlockDenyMessage(itemInHand, clickedBlock, claim, player, playerData, handType);
                }
            }
            event.setCancelled(true);
            GDTimings.PLAYER_INTERACT_BLOCK_PRIMARY_EVENT.stopTimingIfSync();
            return;
        }
        GDTimings.PLAYER_INTERACT_BLOCK_PRIMARY_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onPlayerInteractBlockSecondary(InteractBlockEvent.Secondary event) {
        User user = CauseContextHelper.getEventUser(event);
        final Object source = CauseContextHelper.getEventFakePlayerSource(event);
        final Player player = source instanceof Player ? (Player) source : null;
        if (player == null || NMSUtil.getInstance().isFakePlayer(player)) {
            if (user == null) {
                user = player;
            }
            this.handleFakePlayerInteractBlockSecondary(event, user, source);
            return;
        }

        final HandType handType = event.getHandType();
        final ItemStack itemInHand = player.getItemInHand(handType).orElse(ItemStack.empty());
        if (handleItemInteract(event, player, player.getWorld(), itemInHand).isCancelled()) {
            event.setCancelled(true);
            return;
        }

        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUniqueId())) {
            return;
        }
        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.INTERACT_BLOCK_SECONDARY.getName(), event.getTargetBlock().getState(), player.getWorld().getProperties())) {
            return;
        }

        GDTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.startTimingIfSync();
        final BlockSnapshot clickedBlock = event.getTargetBlock();
        // Check if item is banned
        final GDPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), user.getUniqueId());
        final Location<World> location = clickedBlock.getLocation().orElse(null);

        final GDClaim claim = this.dataStore.getClaimAt(location);
        //GriefDefender.getPermissionManager().getFinalPermission(claim, Flags.ENTITY_SPAWN, source, target, user)
        final TileEntity tileEntity = clickedBlock.getLocation().get().getTileEntity().orElse(null);
        final TrustType trustType = (tileEntity != null && NMSUtil.getInstance().containsInventory(tileEntity)) ? TrustTypes.CONTAINER : TrustTypes.ACCESSOR;
        if (GDFlags.INTERACT_BLOCK_SECONDARY && playerData != null) {
            Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.INTERACT_BLOCK_SECONDARY, source, event.getTargetBlock(), user, trustType, true);
            if (result == Tristate.FALSE) {
                // if player is holding an item, check if it can be placed
                if (GDFlags.BLOCK_PLACE && !itemInHand.isEmpty() && NMSUtil.getInstance().isItemBlock(itemInHand)) {
                    if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.BLOCK_PLACE.getName(), itemInHand, player.getWorld().getProperties())) {
                        GDTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTimingIfSync();
                        return;
                    }
                    if (GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.BLOCK_PLACE, source, itemInHand, user, TrustTypes.BUILDER, true) == Tristate.TRUE) {
                        GDTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTimingIfSync();
                        return;
                    }
                }
                // Don't send a deny message if the player is holding an investigation tool
                if (Sponge.getServer().getRunningTimeTicks() != lastInteractItemSecondaryTick || lastInteractItemCancelled != true) {
                    if (!PlayerUtil.getInstance().hasItemInOneHand(player, GriefDefenderPlugin.getInstance().investigationTool.getType())) {
                        this.sendInteractBlockDenyMessage(itemInHand, clickedBlock, claim, player, playerData, handType);
                    }
                }
                if (handType == HandTypes.MAIN_HAND) {
                    NMSUtil.getInstance().closePlayerScreen(player);
                }

                event.setCancelled(true);
                GDTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTimingIfSync();
                return;
            }
        }

        GDTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTimingIfSync();
    }

    private void handleFakePlayerInteractBlockPrimary(InteractBlockEvent event, User user, Object source) {
        final BlockSnapshot clickedBlock = event.getTargetBlock();
        final Location<World> location = clickedBlock.getLocation().orElse(null);
        final GDClaim claim = this.dataStore.getClaimAt(location);
        final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.INTERACT_BLOCK_PRIMARY, source, event.getTargetBlock(), user, TrustTypes.BUILDER, true);
        if (result == Tristate.FALSE) {
            event.setCancelled(true);
        }
    }

    private void handleFakePlayerInteractBlockSecondary(InteractBlockEvent event, User user, Object source) {
        final BlockSnapshot clickedBlock = event.getTargetBlock();
        final Location<World> location = clickedBlock.getLocation().orElse(null);
        final GDClaim claim = this.dataStore.getClaimAt(location);
        final TileEntity tileEntity = clickedBlock.getLocation().get().getTileEntity().orElse(null);
        final TrustType trustType = (tileEntity != null && NMSUtil.getInstance().containsInventory(tileEntity)) ? TrustTypes.CONTAINER : TrustTypes.ACCESSOR;
        final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.INTERACT_BLOCK_SECONDARY, source, event.getTargetBlock(), user, trustType, true);
        if (result == Tristate.FALSE) {
            event.setCancelled(true);
        }
    }

    public InteractEvent handleItemInteract(InteractEvent event, Player player, World world, ItemStack itemInHand) {
        final ItemType itemType = itemInHand.getType();
        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        if (!playerData.claimMode && (itemInHand.isEmpty() || NMSUtil.getInstance().isItemFood(itemType))) {
            return event;
        }

        final boolean primaryEvent = event instanceof InteractItemEvent.Primary || event instanceof InteractBlockEvent.Primary;
        if (!GDFlags.INTERACT_ITEM_PRIMARY && primaryEvent || !GDFlags.INTERACT_ITEM_SECONDARY && !primaryEvent || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUniqueId())) {
            return event;
        }

        if (primaryEvent && GriefDefenderPlugin.isTargetIdBlacklisted(Flags.INTERACT_ITEM_PRIMARY.toString(), itemInHand.getType(), world.getProperties())) {
            return event;
        }
        if (!primaryEvent && GriefDefenderPlugin.isTargetIdBlacklisted(Flags.INTERACT_ITEM_SECONDARY.toString(), itemInHand.getType(), world.getProperties())) {
            return event;
        }

        final Cause cause = event.getCause();
        final EventContext context = cause.getContext();
        final BlockSnapshot blockSnapshot = context.get(EventContextKeys.BLOCK_HIT).orElse(BlockSnapshot.NONE);
        final Vector3d interactPoint = event.getInteractionPoint().orElse(null);
        final Entity entity = context.get(EventContextKeys.ENTITY_HIT).orElse(null);
        final Location<World> location = entity != null ? entity.getLocation() 
                : blockSnapshot != BlockSnapshot.NONE ? blockSnapshot.getLocation().get() 
                        : interactPoint != null ? new Location<World>(world, interactPoint) 
                                : player.getLocation();
        final GDClaim claim = this.dataStore.getClaimAt(location);

        final Flag flag = primaryEvent ? Flags.INTERACT_ITEM_PRIMARY : Flags.INTERACT_ITEM_SECONDARY;

        if (playerData.claimMode || (!itemInHand.isEmpty() && (itemInHand.getType().equals(GriefDefenderPlugin.getInstance().modificationTool.getType()) ||
                itemInHand.getType().equals(GriefDefenderPlugin.getInstance().investigationTool.getType())))) {
            GDPermissionManager.getInstance().addEventLogEntry(event, claim, location, itemInHand, blockSnapshot == null ? entity : blockSnapshot, player, flag, null, Tristate.TRUE);
            event.setCancelled(true);
            if (investigateClaim(event, player, blockSnapshot, itemInHand)) {
                return event;
            }
            if (!primaryEvent) {
                if (playerData.claimMode && event instanceof HandInteractEvent) {
                    final HandInteractEvent handInteractEvent = (HandInteractEvent) event;
                    if (handInteractEvent.getHandType() == HandTypes.MAIN_HAND) {
                        onPlayerHandleClaimCreateAction(event, blockSnapshot, player, itemInHand, playerData);
                    }
                } else {
                    onPlayerHandleClaimCreateAction(event, blockSnapshot, player, itemInHand, playerData);
                }
            }
            return event;
        }

        if (GDPermissionManager.getInstance().getFinalPermission(event, location, claim, flag, player, itemInHand, player, TrustTypes.ACCESSOR, true) == Tristate.FALSE) {
            Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_INTERACT_ITEM,
                    ImmutableMap.of(
                    "player", claim.getOwnerName(),
                    "item", itemInHand.getType().getId()));
            GriefDefenderPlugin.sendClaimDenyMessage(claim, player, message);
            if (event instanceof InteractBlockEvent.Secondary) {
                ((InteractBlockEvent.Secondary) event).setUseItemResult(SpongeUtil.getSpongeTristate(Tristate.FALSE));
            } else {
                event.setCancelled(true);
            }
            lastInteractItemCancelled = true;
        }
        return event;
    }

    private void onPlayerHandleClaimCreateAction(InteractEvent event, BlockSnapshot targetBlock, Player player, ItemStack itemInHand, GDPlayerData playerData) {
        if (player.get(Keys.IS_SNEAKING).get() && (event instanceof InteractBlockEvent.Secondary || event instanceof InteractItemEvent.Secondary)) {
            playerData.revertActiveVisual(player);
            // check for any active WECUI visuals
            if (this.worldEditProvider != null) {
                this.worldEditProvider.revertVisuals(player, playerData, null);
            }
            playerData.lastShovelLocation = null;
            playerData.endShovelLocation = null;
            playerData.claimResizing = null;
            playerData.shovelMode = ShovelTypes.BASIC;
            return;
        }

        if (!playerData.claimMode) {
            GriefDefenderConfig<?> activeConfig = GriefDefenderPlugin.getActiveConfig(player.getWorld().getProperties());
            if (!itemInHand.getType().getId().equals(activeConfig.getConfig().claim.modificationTool)) {
                return;
            }
        }

        GDTimings.PLAYER_HANDLE_SHOVEL_ACTION.startTimingIfSync();
        BlockSnapshot clickedBlock = targetBlock;
        Location<World> location = clickedBlock.getLocation().orElse(null);

        if (clickedBlock.getState().getType() == BlockTypes.AIR) {
            boolean ignoreAir = false;
            if (this.worldEditProvider != null) {
                // Ignore air so players can use client-side WECUI block target which uses max reach distance
                if (this.worldEditProvider.hasCUISupport(player) && playerData.getClaimCreateMode() == CreateModeTypes.VOLUME && playerData.lastShovelLocation != null) {
                    ignoreAir = true;
                }
            }
            final int distance = !ignoreAir ? 100 : NMSUtil.getInstance().getPlayerBlockReachDistance(player);
            location = BlockUtil.getInstance().getTargetBlock(player, playerData, distance, ignoreAir).orElse(null);
            if (location == null) {
                GDTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTiming();
                return;
            }
        }

        if (location == null) {
            GDTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTimingIfSync();
            return;
        }

        event.setCancelled(true);
        if (playerData.shovelMode == ShovelTypes.RESTORE) {
            if (true) {
                GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().FEATURE_NOT_AVAILABLE);
                GDTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTiming();
                return;
            }
            final GDClaim claim = this.dataStore.getClaimAtPlayer(location, playerData, true);
            if (!claim.isUserTrusted(player, TrustTypes.MANAGER)) {
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.BLOCK_CLAIMED,
                        ImmutableMap.of(
                        "player", claim.getOwnerName()));
                GriefDefenderPlugin.sendMessage(player, message);
                ClaimVisual claimVisual = new ClaimVisual(claim, ClaimVisual.ERROR);
                claimVisual.createClaimBlockVisuals(location.getBlockY(), player.getLocation(), playerData);
                claimVisual.apply(player);
                GDTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTiming();
                return;
            }

            Chunk chunk = player.getWorld().getChunk(location.getBlockX() >> 4, 0, location.getBlockZ() >> 4).get();
            int miny = location.getBlockY();
            World world = chunk.getWorld();
            final Chunk newChunk = world.regenerateChunk(chunk.getPosition().getX(), 0, chunk.getPosition().getZ()).orElse(null);
            GDTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTiming();
            return;
        }

        if (!playerData.canCreateClaim(player, true)) {
            GDTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTiming();
            return;
        }

        if (playerData.claimResizing != null && playerData.lastShovelLocation != null) {
            handleResizeFinish(event, player, location, playerData);
            GDTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTiming();
            return;
        }

        GDClaim claim = this.dataStore.getClaimAtPlayer(location, playerData, true);
        if (!claim.isWilderness()) {
            Component noEditReason = claim.allowEdit(player);
            if (noEditReason != null) {
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CREATE_OVERLAP_PLAYER,
                        ImmutableMap.of(
                        "player", claim.getOwnerName()));
                GriefDefenderPlugin.sendMessage(player, message);
                ClaimVisual visualization = new ClaimVisual(claim, ClaimVisual.ERROR);
                visualization.createClaimBlockVisuals(location.getBlockY(), player.getLocation(), playerData);
                visualization.apply(player);
                Set<Claim> claims = new HashSet<>();
                claims.add(claim);
                CommandHelper.showClaims(player, claims, location.getBlockY(), true);
            } else if (playerData.lastShovelLocation == null && BlockUtil.getInstance().clickedClaimCorner(claim, location.getBlockPosition())) {
                handleResizeStart(event, player, location, playerData, claim);
            } else if ((playerData.shovelMode == ShovelTypes.SUBDIVISION 
                    || ((claim.isTown() || claim.isAdminClaim()) && (playerData.lastShovelLocation == null || playerData.claimSubdividing != null)) && playerData.shovelMode != ShovelTypes.TOWN)) {
                if (claim.getTownClaim() != null && playerData.shovelMode == ShovelTypes.TOWN) {
                    GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CREATE_OVERLAP_SHORT);
                    Set<Claim> claims = new HashSet<>();
                    claims.add(claim);
                    CommandHelper.showClaims(player, claims, location.getBlockY(), true);
                } else if (playerData.lastShovelLocation == null) {
                    createSubdivisionStart(event, player, itemInHand, location, playerData, claim);
                } else if (playerData.claimSubdividing != null) {
                    createSubdivisionFinish(event, player, location, playerData, claim);
                }
            } else {
                GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CREATE_OVERLAP);
                Set<Claim> claims = new HashSet<>();
                claims.add(claim);
                CommandHelper.showClaims(player, claims, location.getBlockY(), true);
            }
            GDTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTiming();
            return;
        } else if (playerData.shovelMode == ShovelTypes.SUBDIVISION && playerData.lastShovelLocation != null) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CREATE_SUBDIVISION_FAIL);
            playerData.lastShovelLocation = null;
            GDTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTiming();
            return;
        }

        Location<World> lastShovelLocation = playerData.lastShovelLocation;
        if (lastShovelLocation == null) {
            createClaimStart(event, player, itemInHand, location, playerData, claim);
            GDTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTiming();
            return;
        }

        createClaimFinish(event, player, location, playerData, claim);
        GDTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTiming();
    }

    private void createClaimStart(InteractEvent event, Player player, ItemStack itemInHand, Location<World> location, GDPlayerData playerData, GDClaim claim) {
        final ClaimType type = PlayerUtil.getInstance().getClaimTypeFromShovel(playerData.shovelMode);
        if (!player.hasPermission(GDPermissions.BYPASS_CLAIM_LIMIT)) {
            int createClaimLimit = -1;
            if (playerData.shovelMode == ShovelTypes.BASIC && (claim.isAdminClaim() || claim.isTown() || claim.isWilderness())) {
                createClaimLimit = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), player, Options.CREATE_LIMIT, claim).intValue();
            } else if (playerData.shovelMode == ShovelTypes.TOWN && (claim.isAdminClaim() || claim.isWilderness())) {
                createClaimLimit = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), player, Options.CREATE_LIMIT, claim).intValue();
            } else if (playerData.shovelMode == ShovelTypes.SUBDIVISION && !claim.isWilderness()) {
                createClaimLimit = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), player, Options.CREATE_LIMIT, claim).intValue();
            }

            if (createClaimLimit > 0 && createClaimLimit < (playerData.getClaimTypeCount(type) + 1)) {
                GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CREATE_FAILED_CLAIM_LIMIT));
                return;
            }
        }

        final int minClaimLevel = playerData.getMinClaimLevel();
        if (playerData.shovelMode != ShovelTypes.ADMIN && location.getBlockY() < minClaimLevel) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_BELOW_LEVEL,
                    ImmutableMap.of(
                    "limit", minClaimLevel));
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }
        final int maxClaimLevel = playerData.getMaxClaimLevel();
        if (playerData.shovelMode != ShovelTypes.ADMIN && location.getBlockY() > maxClaimLevel) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_ABOVE_LEVEL,
                    ImmutableMap.of(
                    "limit", maxClaimLevel));
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }

        if (playerData.shovelMode == ShovelTypes.SUBDIVISION && claim.isWilderness()) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CREATE_SUBDIVISION_FAIL);
            return;
        }

        if ((type == ClaimTypes.BASIC || type == ClaimTypes.TOWN) && GriefDefenderPlugin.getGlobalConfig().getConfig().economy.economyMode) {
            // Check current economy mode cost
            final Double economyBlockCost = playerData.getInternalEconomyBlockCost();
            if (economyBlockCost == null || economyBlockCost <= 0) {
                GriefDefenderPlugin.sendMessage(player, TextComponent.builder().color(TextColor.RED)
                        .append("Economy mode is enabled but the current cost for blocks is ")
                        .append("0", TextColor.GOLD)
                        .append("\nRaise the value for option 'economy-block-cost' in config or via '")
                        .append("/gd option claim", TextColor.WHITE)
                        .append("' command.", TextColor.RED)
                        .build());
                return;
            }
        }

        playerData.lastShovelLocation = location;
        Component message = null;
        if (playerData.claimMode) {
            message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_MODE_START,
                    ImmutableMap.of(
                    "type", PlayerUtil.getInstance().getClaimTypeComponentFromShovel(playerData.shovelMode)));
        } else {
            message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_START,
                    ImmutableMap.of(
                    "type", PlayerUtil.getInstance().getClaimTypeComponentFromShovel(playerData.shovelMode),
                    "item", itemInHand.getType().getId()));
        }
        GriefDefenderPlugin.sendMessage(player, message);
        ClaimVisual visual = ClaimVisual.fromClick(location, location.getBlockY(), PlayerUtil.getInstance().getVisualTypeFromShovel(playerData.shovelMode), player, playerData);
        visual.apply(player, false);
    }

    private void createClaimFinish(InteractEvent event, Player player, Location<World> location, GDPlayerData playerData, GDClaim claim) {
        Location<World> lastShovelLocation = playerData.lastShovelLocation;
        final GDClaim firstClaim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(playerData.lastShovelLocation, playerData, true);
        final GDClaim clickedClaim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(location, playerData, true);
        if (!firstClaim.equals(clickedClaim)) {
            final GDClaim overlapClaim = firstClaim.isWilderness() ? clickedClaim : firstClaim;
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CREATE_OVERLAP_SHORT);
            Set<Claim> claims = new HashSet<>();
            claims.add(overlapClaim);
            CommandHelper.showClaims(player, claims, location.getBlockY(), true);
            return;
        }

        final boolean cuboid = playerData.getClaimCreateMode() == CreateModeTypes.VOLUME;
        Vector3i lesserBoundaryCorner = new Vector3i(
                lastShovelLocation.getBlockX(),
                cuboid ? lastShovelLocation.getBlockY() : playerData.getMinClaimLevel(),
                lastShovelLocation.getBlockZ());
        Vector3i greaterBoundaryCorner = new Vector3i(
                location.getBlockX(),
                cuboid ? location.getBlockY() : playerData.getMaxClaimLevel(),
                location.getBlockZ());

        final ClaimType type = PlayerUtil.getInstance().getClaimTypeFromShovel(playerData.shovelMode);
        if ((type == ClaimTypes.BASIC || type == ClaimTypes.TOWN) && GriefDefenderPlugin.getGlobalConfig().getConfig().economy.economyMode) {
            EconomyUtil.getInstance().economyCreateClaimConfirmation(player, playerData, location.getBlockY(), lesserBoundaryCorner, greaterBoundaryCorner, PlayerUtil.getInstance().getClaimTypeFromShovel(playerData.shovelMode),
                    cuboid, playerData.claimSubdividing);
            return;
        }

        GDCauseStackManager.getInstance().pushCause(player);
        ClaimResult result = this.dataStore.createClaim(
                player.getWorld(),
                lesserBoundaryCorner,
                greaterBoundaryCorner,
                type, player.getUniqueId(), cuboid);
        GDCauseStackManager.getInstance().popCause();
        GDClaim gdClaim = (GDClaim) result.getClaim().orElse(null);
        if (!result.successful()) {
            if (result.getResultType() == ClaimResultType.OVERLAPPING_CLAIM) {
                GDClaim overlapClaim = (GDClaim) result.getClaim().get();
                GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CREATE_OVERLAP_SHORT);
                Set<Claim> claims = new HashSet<>();
                claims.add(overlapClaim);
                CommandHelper.showOverlapClaims(player, claims, location.getBlockY());
            } else if (result.getResultType() == ClaimResultType.CLAIM_EVENT_CANCELLED) {
                GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CREATE_CANCEL);
            } else {
                GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CREATE_FAILED_RESULT,
                        ImmutableMap.of("reason", result.getResultType())));
            }
            return;
        } else {
            playerData.lastShovelLocation = null;
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CREATE_SUCCESS,
                    ImmutableMap.of(
                    "type", gdClaim.getFriendlyNameType(true)));
            GriefDefenderPlugin.sendMessage(player, message);
            final WorldEditProvider worldEditProvider = GriefDefenderPlugin.getInstance().worldEditProvider;
            if (worldEditProvider != null) {
                worldEditProvider.stopVisualDrag(player);
                worldEditProvider.visualizeClaim(gdClaim, player, playerData, false);
            }
            gdClaim.getVisualizer().createClaimBlockVisuals(location.getBlockY(), player.getLocation(), playerData);
            gdClaim.getVisualizer().apply(player, false);
        }
    }

    private void createSubdivisionStart(InteractEvent event, Player player, ItemStack itemInHand, Location<World> location, GDPlayerData playerData, GDClaim claim) {
        final int minClaimLevel = playerData.getMinClaimLevel();
        if (playerData.shovelMode != ShovelTypes.ADMIN && location.getBlockY() < minClaimLevel) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_BELOW_LEVEL,
                    ImmutableMap.of(
                    "limit", minClaimLevel));
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }
        final int maxClaimLevel = playerData.getMaxClaimLevel();
        if (playerData.shovelMode != ShovelTypes.ADMIN && location.getBlockY() > maxClaimLevel) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_ABOVE_LEVEL,
                    ImmutableMap.of(
                    "limit", maxClaimLevel));
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }

        if (claim.isSubdivision()) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().RESIZE_OVERLAP_SUBDIVISION);
        } else {
            Component message = null;
            if (playerData.claimMode) {
                message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_START,
                        ImmutableMap.of(
                        "type", playerData.shovelMode.getName()));
            } else {
                message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_START,
                        ImmutableMap.of(
                        "type", playerData.shovelMode.getName(),
                        "item", itemInHand.getType().getId()));
            }
            GriefDefenderPlugin.sendMessage(player, message);
            playerData.lastShovelLocation = location;
            playerData.claimSubdividing = claim;
            ClaimVisual visualization = ClaimVisual.fromClick(location, location.getBlockY(), PlayerUtil.getInstance().getVisualTypeFromShovel(playerData.shovelMode), player, playerData);
            visualization.apply(player, false);
        }
    }

    private void createSubdivisionFinish(InteractEvent event, Player player, Location<World> location, GDPlayerData playerData, GDClaim claim) {
        final GDClaim clickedClaim = GriefDefenderPlugin.getInstance().dataStore.getClaimAt(location);
        if (clickedClaim == null || !playerData.claimSubdividing.getUniqueId().equals(clickedClaim.getUniqueId())) {
            if (clickedClaim != null) {
                GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CREATE_OVERLAP_SHORT);
                final GDClaim overlapClaim = playerData.claimSubdividing;
                Set<Claim> claims = new HashSet<>();
                claims.add(overlapClaim);
                CommandHelper.showClaims(player, claims, location.getBlockY(), true);
            }

            return;
        }

        Vector3i lesserBoundaryCorner = new Vector3i(playerData.lastShovelLocation.getBlockX(), 
                playerData.getClaimCreateMode() == CreateModeTypes.VOLUME ? playerData.lastShovelLocation.getBlockY() : playerData.getMinClaimLevel(),
                playerData.lastShovelLocation.getBlockZ());
        Vector3i greaterBoundaryCorner = new Vector3i(location.getBlockX(), 
                playerData.getClaimCreateMode() == CreateModeTypes.VOLUME ? location.getBlockY() : playerData.getMaxClaimLevel(),
                        location.getBlockZ());

        GDCauseStackManager.getInstance().pushCause(player);
        ClaimResult result = this.dataStore.createClaim(player.getWorld(),
                lesserBoundaryCorner, greaterBoundaryCorner, PlayerUtil.getInstance().getClaimTypeFromShovel(playerData.shovelMode),
                player.getUniqueId(), playerData.getClaimCreateMode() == CreateModeTypes.VOLUME, playerData.claimSubdividing);
        GDCauseStackManager.getInstance().popCause();
        GDClaim gdClaim = (GDClaim) result.getClaim().orElse(null);
        if (!result.successful()) {
            if (result.getResultType() == ClaimResultType.OVERLAPPING_CLAIM) {
                GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CREATE_OVERLAP_SHORT);
                Set<Claim> claims = new HashSet<>();
                claims.add(gdClaim);
                CommandHelper.showOverlapClaims(player, claims, location.getBlockY());
            }
            event.setCancelled(true);
            return;
        } else {
            playerData.lastShovelLocation = null;
            playerData.claimSubdividing = null;
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CREATE_SUCCESS, ImmutableMap.of(
                    "type", gdClaim.getFriendlyNameType(true)));
            GriefDefenderPlugin.sendMessage(player, message);
            gdClaim.getVisualizer().createClaimBlockVisuals(location.getBlockY(), player.getLocation(), playerData);
            gdClaim.getVisualizer().apply(player, false);
            final WorldEditProvider worldEditProvider = GriefDefenderPlugin.getInstance().worldEditProvider;
            if (worldEditProvider != null) {
                worldEditProvider.stopVisualDrag(player);
                worldEditProvider.visualizeClaim(gdClaim, player, playerData, false);
            }
        }
    }

    private void handleResizeStart(InteractEvent event, Player player, Location<World> location, GDPlayerData playerData, GDClaim claim) {
        boolean playerCanResize = true;
        if (!player.hasPermission(GDPermissions.CLAIM_RESIZE_ALL) 
                && !playerData.canIgnoreClaim(claim) 
                && !claim.isUserTrusted(player.getUniqueId(), TrustTypes.MANAGER)) {

            if (claim.isAdminClaim()) {
                if (!playerData.canManageAdminClaims) {
                    playerCanResize = false;
                }
            } else if (!player.getUniqueId().equals(claim.getOwnerUniqueId()) || !player.hasPermission(GDPermissions.CLAIM_RESIZE)) {
                playerCanResize = false;
            }
            if (!playerCanResize) {
                if (claim.parent != null) {
                    if (claim.parent.isAdminClaim() && claim.isSubdivision()) {
                        playerCanResize = player.hasPermission(GDPermissions.CLAIM_RESIZE_ADMIN_SUBDIVISION);
                    } else if (claim.parent.isBasicClaim() && claim.isSubdivision()) {
                        playerCanResize = player.hasPermission(GDPermissions.CLAIM_RESIZE_BASIC_SUBDIVISION);
                    } else if (claim.isTown()) {
                        playerCanResize = player.hasPermission(GDPermissions.CLAIM_RESIZE_TOWN);
                    } else if (claim.isAdminClaim()) {
                        playerCanResize = player.hasPermission(GDPermissions.CLAIM_RESIZE_ADMIN);
                    } else {
                        playerCanResize = player.hasPermission(GDPermissions.CLAIM_RESIZE_BASIC);
                    }
                } else if (claim.isTown()) {
                    playerCanResize = player.hasPermission(GDPermissions.CLAIM_RESIZE_TOWN);
                } else if (claim.isAdminClaim()) {
                    playerCanResize = player.hasPermission(GDPermissions.CLAIM_RESIZE_ADMIN);
                } else {
                    playerCanResize = player.hasPermission(GDPermissions.CLAIM_RESIZE_BASIC);
                }
            }
        }

        if (!claim.getInternalClaimData().isResizable() || !playerCanResize) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().PERMISSION_CLAIM_RESIZE);
            return;
        }

        playerData.claimResizing = claim;
        playerData.lastShovelLocation = location;
        if (GriefDefenderPlugin.getInstance().worldEditProvider != null) {
            // get opposite corner
            final int x = playerData.lastShovelLocation.getBlockX() == claim.lesserBoundaryCorner.getX() ? claim.greaterBoundaryCorner.getX() : claim.lesserBoundaryCorner.getX();
            final int y = playerData.lastShovelLocation.getBlockY() == claim.lesserBoundaryCorner.getY() ? claim.greaterBoundaryCorner.getY() : claim.lesserBoundaryCorner.getY();
            final int z = playerData.lastShovelLocation.getBlockZ() == claim.lesserBoundaryCorner.getZ() ? claim.greaterBoundaryCorner.getZ() : claim.lesserBoundaryCorner.getZ();
            GriefDefenderPlugin.getInstance().worldEditProvider.visualizeClaim(claim, new Vector3i(x, y, z), playerData.lastShovelLocation.getBlockPosition(), player, playerData, false);
        }
        // Show visual block for resize corner click
        ClaimVisual visual = ClaimVisual.fromClick(location, location.getBlockY(), PlayerUtil.getInstance().getVisualTypeFromShovel(playerData.shovelMode), player, playerData);
        visual.apply(player, false);
        GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().RESIZE_START);
    }

    private void handleResizeFinish(InteractEvent event, Player player, Location<World> location, GDPlayerData playerData) {
        if (location.equals(playerData.lastShovelLocation)) {
            return;
        }

        playerData.endShovelLocation = location;
        int newx1, newx2, newz1, newz2, newy1, newy2;
        int smallX = 0, smallY = 0, smallZ = 0, bigX = 0, bigY = 0, bigZ = 0;

        newx1 = playerData.lastShovelLocation.getBlockX();
        newx2 = location.getBlockX();
        newy1 = playerData.lastShovelLocation.getBlockY();
        newy2 = location.getBlockY();
        newz1 = playerData.lastShovelLocation.getBlockZ();
        newz2 = location.getBlockZ();
        Vector3i lesserBoundaryCorner = playerData.claimResizing.getLesserBoundaryCorner();
        Vector3i greaterBoundaryCorner = playerData.claimResizing.getGreaterBoundaryCorner();
        smallX = lesserBoundaryCorner.getX();
        smallY = lesserBoundaryCorner.getY();
        smallZ = lesserBoundaryCorner.getZ();
        bigX = greaterBoundaryCorner.getX();
        bigY = greaterBoundaryCorner.getY();
        bigZ = greaterBoundaryCorner.getZ();

        if (newx1 == smallX) {
            smallX = newx2;
        } else {
            bigX = newx2;
        }

        if (newy1 == smallY) {
            smallY = newy2;
        } else {
            bigY = newy2;
        }

        if (newz1 == smallZ) {
            smallZ = newz2;
        } else {
            bigZ = newz2;
        }

        ClaimResult claimResult = null;
        GDCauseStackManager.getInstance().pushCause(player);
        claimResult = playerData.claimResizing.resize(smallX, bigX, smallY, bigY, smallZ, bigZ);
        GDCauseStackManager.getInstance().popCause();
        if (claimResult.successful()) {
            Claim claim = (GDClaim) claimResult.getClaim().get();
            int claimBlocksRemaining = playerData.getRemainingClaimBlocks();;
            if (!playerData.claimResizing.isAdminClaim()) {
                UUID ownerID = playerData.claimResizing.getOwnerUniqueId();
                if (playerData.claimResizing.parent != null) {
                    ownerID = playerData.claimResizing.parent.getOwnerUniqueId();
                }

                if (ownerID.equals(player.getUniqueId())) {
                    claimBlocksRemaining = playerData.getRemainingClaimBlocks();
                } else {
                    GDPlayerData ownerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), ownerID);
                    claimBlocksRemaining = ownerData.getRemainingClaimBlocks();
                    Optional<User> owner = Sponge.getGame().getServiceManager().provide(UserStorageService.class).get().get(ownerID);
                    if (owner.isPresent() && !owner.get().isOnline()) {
                        this.dataStore.clearCachedPlayerData(player.getWorld().getUniqueId(), ownerID);
                    }
                }
            }

            playerData.claimResizing = null;
            playerData.lastShovelLocation = null;
            playerData.endShovelLocation = null;
            if (GriefDefenderPlugin.getInstance().isEconomyModeEnabled()) {
                final Account playerAccount = GriefDefenderPlugin.getInstance().economyService.get().getOrCreateAccount(player.getUniqueId()).orElse(null);
                if (playerAccount != null) {
                    final Currency defaultCurrency = GriefDefenderPlugin.getInstance().economyService.get().getDefaultCurrency();
                    final BigDecimal currentFunds = playerAccount.getBalance(defaultCurrency);
                    if (GriefDefenderPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.VOLUME) {
                        final double claimableChunks = claimBlocksRemaining / 65536.0;
                        final Map<String, Object> params = ImmutableMap.of(
                                "balance", String.valueOf("$" + currentFunds.intValue()),
                                "chunk-amount", Math.round(claimableChunks * 100.0)/100.0, 
                                "block-amount", claimBlocksRemaining);
                        GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_MODE_RESIZE_SUCCESS_3D, params));
                    } else {
                        final Map<String, Object> params = ImmutableMap.of(
                                "balance", String.valueOf("$" + currentFunds.intValue()),
                                "block-amount", claimBlocksRemaining);
                        GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_MODE_RESIZE_SUCCESS_2D, params));
                    }
                }
            } else {
                if (GriefDefenderPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.VOLUME) {
                    final double claimableChunks = claimBlocksRemaining / 65536.0;
                    final Map<String, Object> params = ImmutableMap.of(
                            "chunk-amount", Math.round(claimableChunks * 100.0)/100.0, 
                            "block-amount", claimBlocksRemaining);
                    GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.RESIZE_SUCCESS_3D, params));
                } else {
                    final Map<String, Object> params = ImmutableMap.of(
                            "block-amount", claimBlocksRemaining);
                    GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.RESIZE_SUCCESS_2D, params));
                }
            }
            playerData.revertActiveVisual(player);
            ((GDClaim) claim).getVisualizer().resetVisuals();
            ((GDClaim) claim).getVisualizer().createClaimBlockVisuals(location.getBlockY(), player.getLocation(), playerData);
            ((GDClaim) claim).getVisualizer().apply(player);
            if (this.worldEditProvider != null) {
                this.worldEditProvider.visualizeClaim(claim, player, playerData, false);
            }
        } else {
            if (claimResult.getResultType() == ClaimResultType.OVERLAPPING_CLAIM) {
                GDClaim overlapClaim = (GDClaim) claimResult.getClaim().get();
                GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().RESIZE_OVERLAP);
                Set<Claim> claims = new HashSet<>();
                claims.add(overlapClaim);
                CommandHelper.showOverlapClaims(player, claims, location.getBlockY());
            } else {
                if (!claimResult.getMessage().isPresent()) {
                    GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CLAIM_NOT_YOURS);
                }
            }

            playerData.claimSubdividing = null;
            event.setCancelled(true);
        }
    }

    private boolean investigateClaim(InteractEvent event, Player player, BlockSnapshot clickedBlock, ItemStack itemInHand) {
        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        if (playerData.claimMode && (event instanceof InteractItemEvent.Secondary || event instanceof InteractBlockEvent.Secondary)) {
            if (player.get(Keys.IS_SNEAKING).get()) {
                return true;
            }
            // claim mode inspects with left-click
            return false;
        }

        if (event instanceof InteractItemEvent.Primary || event instanceof InteractBlockEvent.Primary) {
            if (!playerData.claimMode || !playerData.visualBlocks.isEmpty()) {
                playerData.revertActiveVisual(player);
                if (this.worldEditProvider != null) {
                    this.worldEditProvider.revertVisuals(player, playerData, null);
                }
                GDTimings.PLAYER_INVESTIGATE_CLAIM.stopTiming();
                return false;
            }
        }

        if (!playerData.claimMode && (itemInHand.isEmpty() || itemInHand.getType() != GriefDefenderPlugin.getInstance().investigationTool.getType())) {
            return false;
        }

        GDTimings.PLAYER_INVESTIGATE_CLAIM.startTimingIfSync();
        // if holding shift (sneaking), show all claims in area
        GDClaim claim = null;
        if (clickedBlock.getState().getType().equals(BlockTypes.AIR)) {
            final int maxDistance = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), player, Options.RADIUS_INSPECT);
            claim = this.findNearbyClaim(player, maxDistance);
            // if holding shift (sneaking), show all claims in area
            if (player.get(Keys.IS_SNEAKING).get()) {
                if (!playerData.canIgnoreClaim(claim) && !player.hasPermission(GDPermissions.VISUALIZE_CLAIMS_NEARBY)) {
                    GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().PERMISSION_VISUAL_CLAIMS_NEARBY);
                    GDTimings.PLAYER_INVESTIGATE_CLAIM.stopTimingIfSync();
                    return false;
                }

                Location<World> nearbyLocation = playerData.lastValidInspectLocation != null ? playerData.lastValidInspectLocation : player.getLocation();
                Set<Claim> claims = BlockUtil.getInstance().getNearbyClaims(nearbyLocation, maxDistance);
                int height = (int) (playerData.lastValidInspectLocation != null ? playerData.lastValidInspectLocation.getBlockY() : PlayerUtil.getInstance().getEyeHeight(player));
                boolean hideBorders = this.worldEditProvider != null &&
                                      this.worldEditProvider.hasCUISupport(player) &&
                                      GriefDefenderPlugin.getActiveConfig(player.getWorld().getUniqueId()).getConfig().visual.hideBorders;
                if (!hideBorders) {
                    ClaimVisual visualization = ClaimVisual.fromClaims(claims, PlayerUtil.getInstance().getVisualClaimHeight(playerData, height), player.getLocation(), playerData, null);
                    visualization.apply(player);
                }

                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_SHOW_NEARBY,
                        ImmutableMap.of(
                        "amount", claims.size()));
                GriefDefenderPlugin.sendMessage(player, message);
                if (!claims.isEmpty()) {

                    if (this.worldEditProvider != null) {
                        worldEditProvider.revertVisuals(player, playerData, null);
                        worldEditProvider.visualizeClaims(claims, player, playerData, true);
                    }
                    CommandHelper.showClaims(player, claims);
                }
                GDTimings.PLAYER_INVESTIGATE_CLAIM.stopTimingIfSync();
                return true;
            }
            if (claim != null && claim.isWilderness()) {
                playerData.lastValidInspectLocation = null;
                GDTimings.PLAYER_INVESTIGATE_CLAIM.stopTimingIfSync();
                return false;
            }
        } else {
            claim = this.dataStore.getClaimAt(clickedBlock.getLocation().get());
            if (claim.isWilderness()) {
                GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.BLOCK_NOT_CLAIMED));
                GDTimings.PLAYER_INVESTIGATE_CLAIM.stopTimingIfSync();
                return false;
            }
        }

        // visualize boundary
        if (claim.getUniqueId() != playerData.visualClaimId) {
            int height = playerData.lastValidInspectLocation != null ? playerData.lastValidInspectLocation.getBlockY() : clickedBlock.getLocation().get().getBlockY();
            claim.getVisualizer().createClaimBlockVisuals(playerData.getClaimCreateMode() == CreateModeTypes.VOLUME ? height : PlayerUtil.getInstance().getEyeHeight(player), player.getLocation(), playerData);
            claim.getVisualizer().apply(player);
            if (this.worldEditProvider != null) {
                worldEditProvider.visualizeClaim(claim, player, playerData, true);
            }
            Set<Claim> claims = new HashSet<>();
            claims.add(claim);
            CommandHelper.showClaims(player, claims);
        }
        final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.BLOCK_CLAIMED,
                ImmutableMap.of(
                "player", claim.getOwnerName()));
        GriefDefenderPlugin.sendMessage(player, message);

        GDTimings.PLAYER_INVESTIGATE_CLAIM.stopTimingIfSync();
        return true;
    }

    private GDClaim findNearbyClaim(Player player, int maxDistance) {
        if (maxDistance <= 0) {
            maxDistance = 100;
        }
        BlockRay<World> blockRay = BlockRay.from(player).distanceLimit(maxDistance).build();
        GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        GDClaim claim = null;
        int count = 0;
        while (blockRay.hasNext()) {
            BlockRayHit<World> blockRayHit = blockRay.next();
            Location<World> location = blockRayHit.getLocation();
            claim = this.dataStore.getClaimAt(location);
            if (claim != null && !claim.isWilderness() && (playerData.visualBlocks == null || (claim.getUniqueId() != playerData.visualClaimId))) {
                playerData.lastValidInspectLocation = location;
                return claim;
            }

            BlockType blockType = location.getBlockType();
            if (blockType != BlockTypes.AIR && blockType != BlockTypes.TALLGRASS) {
                break;
            }
            count++;
        }

        if (count == maxDistance) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CLAIM_TOO_FAR);
        } else if (claim != null && claim.isWilderness()){
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.BLOCK_NOT_CLAIMED));
        }

        return claim;
    }

    private void sendInteractEntityDenyMessage(ItemStack playerItem, Entity entity, GDClaim claim, Player player, HandType handType) {
        if (entity instanceof Player || (claim.getData() != null && !claim.getData().allowDenyMessages())) {
            return;
        }

        final String entityId = entity.getType() != null ? entity.getType().getId() : NMSUtil.getInstance().getEntityName(entity);
        if (playerItem == null || playerItem == ItemTypes.NONE || playerItem.isEmpty()) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_INTERACT_ENTITY, ImmutableMap.of(
                    "player", claim.getOwnerName(),
                    "entity", entityId));
            GriefDefenderPlugin.sendClaimDenyMessage(claim, player, message);
        } else {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_INTERACT_ITEM_ENTITY, ImmutableMap.of(
                    "item", playerItem.getType().getId(),
                    "entity", entityId));
            GriefDefenderPlugin.sendClaimDenyMessage(claim, player, message);
        }
    }

    private void sendInteractBlockDenyMessage(ItemStack playerItem, BlockSnapshot blockSnapshot, GDClaim claim, Player player, GDPlayerData playerData, HandType handType) {
        if (claim.getData() != null && !claim.getData().allowDenyMessages()) {
            return;
        }

        if (playerData != null && claim.getData() != null && claim.getData().isExpired() /*&& GriefDefenderPlugin.getActiveConfig(player.getWorld().getProperties()).getConfig().claim.bankTaxSystem*/) {
            playerData.sendTaxExpireMessage(player, claim);
        } else if (playerItem == null || playerItem == ItemTypes.NONE || playerItem.isEmpty()) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_INTERACT_BLOCK,
                    ImmutableMap.of(
                    "player", claim.getOwnerName(),
                    "block", blockSnapshot.getState().getType().getId()));
            GriefDefenderPlugin.sendClaimDenyMessage(claim, player, message);
        } else {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_INTERACT_ITEM_BLOCK,
                    ImmutableMap.of(
                    "item", playerItem.getType().getId(),
                    "block", blockSnapshot.getState().getType().getId()));
            GriefDefenderPlugin.sendClaimDenyMessage(claim, player, message);
        }
        if (handType == HandTypes.MAIN_HAND) {
            NMSUtil.getInstance().closePlayerScreen(player);
        }
    }
}