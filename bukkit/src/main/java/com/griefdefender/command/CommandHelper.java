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

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimContexts;
import com.griefdefender.api.claim.TrustType;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.api.economy.BankTransactionType;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.PermissionResult;
import com.griefdefender.api.permission.ResultTypes;
import com.griefdefender.api.permission.flag.Flag;
import com.griefdefender.api.permission.flag.Flags;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.command.ClaimFlagBase.FlagType;
import com.griefdefender.configuration.GriefDefenderConfig;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.economy.GDBankTransaction;
import com.griefdefender.internal.pagination.PaginationList;
import com.griefdefender.internal.registry.BlockTypeRegistryModule;
import com.griefdefender.internal.registry.EntityTypeRegistryModule;
import com.griefdefender.internal.registry.ItemTypeRegistryModule;
import com.griefdefender.internal.util.NMSUtil;
import com.griefdefender.internal.util.VecHelper;
import com.griefdefender.internal.visual.ClaimVisual;
import com.griefdefender.permission.GDFlag;
import com.griefdefender.permission.GDPermissionHolder;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissionResult;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.registry.FlagRegistryModule;
import com.griefdefender.text.action.GDCallbackHolder;
import com.griefdefender.util.PermissionUtil;
import com.griefdefender.util.TaskUtil;

import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.text.serializer.plain.PlainComponentSerializer;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World.Environment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandHelper {

    public static Comparator<Component> PLAIN_COMPARATOR = (text1, text2) -> PlainComponentSerializer.INSTANCE.serialize(text1).compareTo(PlainComponentSerializer.INSTANCE.serialize(text2));

    public static Player checkPlayer(CommandSender source) {
        if (source instanceof Player) {
            return ((Player) source);
        } else {
            //throw new CommandException(Text.of("You must be a player to run this command!"));
            return null;
        }
    }

    public static boolean validateFlagTarget(Flag flag, String target) {
        if (!(flag instanceof GDFlag)) {
            return true;
        }

        if (flag.getName().equals("block-break") || flag.getName().equals("block-place") || flag.getName().equals("collide-block")) {
            if (validateBlockTarget(target) ||
                validateItemTarget(target)) {
                return true;
            }
            return false;
        }
        if (flag.getName().equals("enter-claim") || flag.getName().equals("exit-claim") || flag.getName().equals("entity-riding") ||
                flag.getName().equals("entity-damage") || flag.getName().equals("portal-use")) {
            if (validateEntityTarget(target) ||
                validateBlockTarget(target) ||
                validateItemTarget(target)) {
                return true;
            }

            return false;
        }
        if (flag.getName().equals("interact-inventory") || flag.getName().equals("liquid-flow") ||
                flag.getName().equals("interact-block-primary") || flag.getName().equals("interact-block-secondary")) {
            return validateBlockTarget(target);
        }
        if (flag.getName().equals("entity-chunk-spawn") || flag.getName().equals("entity-spawn") ||
                flag.getName().equals("interact-entity-primary") || flag.getName().equals("interact-entity-secondary")) {
            return validateEntityTarget(target);
        }
        if (flag.getName().equals("item-drop") || flag.getName().equals("item-pickup") ||
                flag.getName().equals("item-spawn") || flag.getName().equals("item-use")) {
            return validateItemTarget(target);
        }

        return true;
    }

    private static boolean validateEntityTarget(String target) {
        if (EntityTypeRegistryModule.getInstance().getById(target).isPresent()) {
            return true;
        }

        return false;
    }

    private static boolean validateItemTarget(String target) {
        if (ItemTypeRegistryModule.getInstance().getById(target).isPresent()) {
            return true;
        }
        // target could be an item block, so validate blockstate
        /*Optional<BlockState> blockState = Sponge.getRegistry().getType(BlockState.class, target);
        if (blockState.isPresent()) {
            return true;
        }*/

        return false;
    }

    private static boolean validateBlockTarget(String target) {
        if (BlockTypeRegistryModule.getInstance().getById(target).isPresent()) {
            return true;
        }

        /*Optional<BlockState> blockState = Sponge.getRegistry().getType(BlockState.class, target);
        if (blockState.isPresent()) {
            return true;
        }*/
        return false;
    }

    public static PermissionResult addFlagPermission(CommandSender src, GDPermissionHolder holder, String subjectName, Claim claim, Flag claimFlag, String target, Tristate value, Set<Context> contexts) {
        if (src instanceof Player) {
            Component denyReason = ((GDClaim) claim).allowEdit((Player) src);
            if (denyReason != null) {
                GriefDefenderPlugin.sendMessage(src, denyReason);
                return new GDPermissionResult(ResultTypes.NO_PERMISSION);
            }
        }

        final String baseFlag = claimFlag.toString().toLowerCase();
        String flagPermission = GDPermissions.FLAG_BASE + "." + baseFlag;
        // special handling for commands
        if (baseFlag.equals(Flags.COMMAND_EXECUTE.getName()) || baseFlag.equals(Flags.COMMAND_EXECUTE_PVP.getName())) {
            target = handleCommandFlag(src, target);
            if (target == null) {
                // failed
                return new GDPermissionResult(ResultTypes.TARGET_NOT_VALID);
            }
            flagPermission = GDPermissions.FLAG_BASE + "." + baseFlag + "." + target;
        } else {
            if (!target.equalsIgnoreCase("any")) {
                if (!target.contains(":")) {
                    // assume vanilla
                    target = "minecraft:" + target;
                }
    
                String[] parts = target.split(":");
                if (parts[1].equalsIgnoreCase("any")) {
                    target = baseFlag + "." + parts[0] + ".*";
                } else {
                    // check for meta
                    parts = target.split("\\.");
                    String targetFlag = parts[0];
                    if (parts.length > 1) {
                        try {
                            Integer.parseInt(parts[1]);
                        } catch (NumberFormatException e) {
                            final Component message = GriefDefenderPlugin.getInstance().messageData.permissionClaimManage
                                    .apply(ImmutableMap.of(
                                    "meta", parts[1],
                                    "flag", baseFlag)).build();
                            GriefDefenderPlugin.sendMessage(src, message);
                            return new GDPermissionResult(ResultTypes.TARGET_NOT_VALID);
                        }
                    }
                    String entitySpawnFlag = NMSUtil.getInstance().getEntitySpawnFlag(claimFlag, targetFlag);
                    if (entitySpawnFlag == null && !CommandHelper.validateFlagTarget(claimFlag, targetFlag)) {
                        //TODO
                        /*final Text message = GriefDefenderPlugin.getInstance().messageData.permissionClaimManage
                                .apply(ImmutableMap.of(
                                "target", targetFlag,
                                "flag", baseFlag)).build();*/
                        GriefDefenderPlugin.sendMessage(src,TextComponent.of("Invalid flag " + targetFlag, TextColor.RED));
                        return new GDPermissionResult(ResultTypes.TARGET_NOT_VALID);
                    }
        
                    if (entitySpawnFlag != null) {
                        target = entitySpawnFlag;
                    } else {
                        target = baseFlag + "." + target.replace(":", ".");//.replace("[", ".[");
                    }
                }

                flagPermission = GDPermissions.FLAG_BASE + "." + target;
            } else {
                // Bukkit requires * for any
                target = "";
                flagPermission += ".*";
            }
        }

        return applyFlagPermission(src, holder, subjectName, claim, flagPermission, target, value, contexts, null, false);
    }

    public static PermissionResult applyFlagPermission(CommandSender src, GDPermissionHolder holder, String subjectName, Claim claim, String flagPermission, String target, Tristate value, Set<Context> contexts, FlagType flagType) {
        return applyFlagPermission(src, holder, subjectName, claim, flagPermission, target, value, contexts, flagType, false);
    }

    public static PermissionResult applyFlagPermission(CommandSender src, GDPermissionHolder holder, String subjectName, Claim claim, String flagPermission, String target, Tristate value, Set<Context> contexts, FlagType flagType, boolean clicked) {
        // Check if player can manage flag
        if (src instanceof Player) {
            final String basePermission = flagPermission.replace(GDPermissions.FLAG_BASE + ".", "").replace(".*", "");
            Player player = (Player) src;
            GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
            Tristate result = Tristate.UNDEFINED;
            if (playerData.canManageAdminClaims) {
                result = Tristate.fromBoolean(src.hasPermission(GDPermissions.ADMIN_CLAIM_FLAGS + "." + basePermission));
            } else {
                result = Tristate.fromBoolean(src.hasPermission(GDPermissions.USER_CLAIM_FLAGS + "." + basePermission));
            }

            if (result != Tristate.TRUE) {
                GriefDefenderPlugin.sendMessage(src, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_FLAG_USE));
                return new GDPermissionResult(ResultTypes.NO_PERMISSION);
            }
        }

        boolean hasDefaultContext = false;
        boolean hasOverrideContext = false;
        Component reason = null;
        Iterator<Context> iterator = contexts.iterator();
        while (iterator.hasNext()) {
            final Context context = iterator.next();
                // validate perms
            if (context.getKey().equalsIgnoreCase("reason")) {
                reason = LegacyComponentSerializer.legacy().deserialize(context.getValue(), '&');
                iterator.remove();
                continue;
            }
            if (hasDefaultContext || hasOverrideContext) {
                continue;
            }
            if (context.getKey().contains("gd_claim_default")) {
                hasDefaultContext = true;
            } else if (context.getKey().contains("gd_claim_override")) {
                hasOverrideContext = true;
            }
        }

        if (flagType == null) {
            if (hasDefaultContext) {
                flagType = FlagType.DEFAULT;
            } else if (hasOverrideContext) {
                flagType = FlagType.OVERRIDE;
            } else {
                flagType = FlagType.CLAIM;
            }
        }
        if (hasOverrideContext && contexts.contains(ClaimContexts.WILDERNESS_OVERRIDE_CONTEXT)) {
            if (reason != null) {
                if (reason != TextComponent.empty()) {
                    GriefDefenderPlugin.getGlobalConfig().getConfig().bans.addBanReason(flagPermission, reason);
                } else {
                    GriefDefenderPlugin.getGlobalConfig().getConfig().bans.removeBanReason(flagPermission);
                }
                GriefDefenderPlugin.getGlobalConfig().save();
            }
        }
        TextComponent.Builder builder = null;
        if (flagType == FlagType.OVERRIDE) {
            builder = TextComponent.builder("OVERRIDE").color(TextColor.RED);
        } else if (flagType == FlagType.DEFAULT) {
            builder = TextComponent.builder("DEFAULT").color(TextColor.LIGHT_PURPLE);
        } else if (flagType == FlagType.CLAIM) {
            builder = TextComponent.builder("CLAIM").color(TextColor.GOLD);
            if (contexts instanceof HashSet) {
                contexts.add(claim.getContext());
            }
        }
        Component flagTypeText = builder.build();

        if (contexts.isEmpty()) {
            // default to claim
            contexts.add(claim.getContext());
        }
        // wilderness overrides affect all worlds
        if (!contexts.contains(ClaimContexts.WILDERNESS_OVERRIDE_CONTEXT) && !contexts.contains(claim.getContext())) {
            if (contexts instanceof HashSet) {
                //contexts.add(claim.getWorld().getContext());
            }
        }

        if (holder == GriefDefenderPlugin.DEFAULT_HOLDER) {
            PermissionUtil.getInstance().setPermissionValue((GDClaim) claim, GriefDefenderPlugin.DEFAULT_HOLDER, flagPermission, value, contexts);
            if (!clicked) {
                TextAdapter.sendComponent(src, TextComponent.builder("")
                    .append(TextComponent.builder("\n[").append("Return to flags", TextColor.AQUA).append("]\n")
                    .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createCommandConsumer(src, "claimflag", "")))).build())
                        .append("Set ")
                        .append(flagTypeText)
                        .append(" permission ", TextColor.GREEN)
                        .append(flagPermission.replace(GDPermissions.FLAG_BASE + ".", "") + "\n", TextColor.AQUA)
                        .append("with contexts")
                        .append(getFriendlyContextString(claim, contexts), TextColor.GRAY)
                        .append("\n to ", TextColor.GREEN)
                        .append(getClickableText(src, (GDClaim) claim, GriefDefenderPlugin.DEFAULT_HOLDER, subjectName, contexts, flagPermission, value, flagType).color(TextColor.LIGHT_PURPLE))
                        .append(" on ", TextColor.GREEN)
                        .append("ALL", TextColor.GOLD).build());
            }
        } else {
            PermissionUtil.getInstance().setPermissionValue((GDClaim) claim, holder, flagPermission, value, contexts);
            if (!clicked) {
                TextAdapter.sendComponent(src, TextComponent.builder("")
                        .append(TextComponent.builder("")
                                .append("\n[")
                                .append("Return to flags", TextColor.AQUA)
                                .append("]\n", TextColor.WHITE)
                                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createCommandConsumer(src, holder instanceof GDPermissionUser ? "claimflagplayer" : "claimflaggroup", subjectName)))).build())
                        .append("Set ", TextColor.GREEN)
                        .append(flagTypeText)
                        .append(" permission ")
                        .append(flagPermission.replace(GDPermissions.FLAG_BASE + ".", ""), TextColor.AQUA)
                        .append("\n to ", TextColor.GREEN)
                        .append(getClickableText(src, (GDClaim) claim, holder, subjectName, contexts, flagPermission, value, flagType).color(TextColor.LIGHT_PURPLE))
                        .append(" on ", TextColor.GREEN)
                        .append(subjectName, TextColor.GOLD).build());
            }
        }

        return new GDPermissionResult(ResultTypes.SUCCESS);
    }

    private static String getFriendlyContextString(Claim claim, Set<Context> contexts) {
        contexts.remove(claim.getContext());
        String contextString = "";
        final Iterator<Context> iterator = contexts.iterator();
        while (iterator.hasNext()) {
            final Context context = iterator.next();
            contextString += "[" + context.getKey() + ":" + context.getValue();
            if (!iterator.hasNext()) {
                contextString += "]";
            }
        }
        return contextString;
    }

    public static TextColor getFlagTypeColor(FlagType type) {
        TextColor color = TextColor.LIGHT_PURPLE;
        if (type == FlagType.CLAIM) {
            color = TextColor.GOLD;
        } else if (type == FlagType.OVERRIDE) {
            color = TextColor.RED;
        }

        return color;
    }

   public static Consumer<CommandSender> createFlagConsumer(CommandSender src, GDClaim claim, GDPermissionHolder holder, String subjectName, Set<Context> contexts, String flagPermission, Tristate flagValue, FlagType flagType) {
        return consumer -> {
            Tristate newValue = Tristate.UNDEFINED;
            if (flagValue == Tristate.TRUE) {
                newValue = Tristate.FALSE;
            } else if (flagValue == Tristate.UNDEFINED) {
                newValue = Tristate.TRUE;
            }

            Component flagTypeText = TextComponent.empty();
            if (flagType == FlagType.OVERRIDE) {
                flagTypeText = TextComponent.of("OVERRIDE", TextColor.RED);
            } else if (flagType == FlagType.DEFAULT) {
                flagTypeText = TextComponent.of("DEFAULT", TextColor.LIGHT_PURPLE);
            } else if (flagType == FlagType.CLAIM) {
                flagTypeText = TextComponent.of("CLAIM", TextColor.GOLD);
            }
            String target = flagPermission.replace(GDPermissions.FLAG_BASE + ".",  "");
            Set<Context> newContexts = new HashSet<>(contexts);
            PermissionUtil.getInstance().setPermissionValue((GDClaim) claim, GriefDefenderPlugin.DEFAULT_HOLDER, flagPermission, newValue, newContexts);
            TextAdapter.sendComponent(src, TextComponent.builder("")
                    .append("Set ", TextColor.GREEN)
                    .append(flagTypeText)
                    .append(" permission ")
                    .append(target, TextColor.AQUA)
                    .append("\n to ", TextColor.GREEN)
                    .append(getClickableText(src, (GDClaim) claim, holder, subjectName, newContexts, flagPermission, newValue, flagType).color(TextColor.LIGHT_PURPLE))
                    .append(" for ", TextColor.GREEN)
                    .append(subjectName, TextColor.GOLD).build());
        };
    }

    public static Consumer<CommandSender> createCommandConsumer(CommandSender src, String command, String arguments) {
        return createCommandConsumer(src, command, arguments, null);
    }

    public static Consumer<CommandSender> createCommandConsumer(CommandSender src, String command, String arguments, Consumer<CommandSender> postConsumerTask) {
        return consumer -> {
            if (!NMSUtil.getInstance().getBukkitCommandMap().dispatch(src, command + " " + arguments)) {
                TextAdapter.sendComponent(src, TextComponent.of("Failed to execute command " + command + " " + arguments, TextColor.RED));
            }
            if (postConsumerTask != null) {
                postConsumerTask.accept(src);
            }
        };
    }

    public static void executeCommand(CommandSender src, String command, String arguments) {
        if (!NMSUtil.getInstance().getBukkitCommandMap().dispatch(src, command + " " + arguments)) {
            TextAdapter.sendComponent(src, TextComponent.of("Failed to execute command " + command + " " + arguments, TextColor.RED));
        }
    }

    public static void showClaims(CommandSender src, Set<Claim> claims) {
        if (claims.isEmpty()) {
            // do nothing
            return;
        }
        showClaims(src, claims, 0, false);
    }

    public static void showOverlapClaims(CommandSender src, Set<Claim> claims, int height) {
        showClaims(src, claims, height, true, true);
    }

    public static void showClaims(CommandSender src, Set<Claim> claims, int height, boolean visualizeClaims) {
        showClaims(src, claims, height, visualizeClaims, false);
    }

    public static void showClaims(CommandSender src, Set<Claim> claims, int height, boolean visualizeClaims, boolean overlap) {
        final String worldName = src instanceof Player ? ((Player) src).getWorld().getName() : Bukkit.getWorlds().get(0).getName();
        final boolean canListOthers = src.hasPermission(GDPermissions.LIST_OTHER_CLAIMS);
        List<Component> claimsTextList = generateClaimTextList(new ArrayList<Component>(), claims, worldName, null, src, createShowClaimsConsumer(src, claims, height, visualizeClaims), canListOthers, false, overlap);

        if (visualizeClaims && src instanceof Player) {
            Player player = (Player) src;
            final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
            if (claims.size() > 1) {
                if (height != 0) {
                    height = playerData.lastValidInspectLocation != null ? playerData.lastValidInspectLocation.getBlockY() : player.getEyeLocation().getBlockY();
                }
                ClaimVisual visualization = ClaimVisual.fromClaims(claims, playerData.getClaimCreateMode() == 1 ? height : player.getEyeLocation().getBlockY(), player.getLocation(), playerData, null);
                visualization.apply(player);
            } else {
                for (Claim claim : claims) {
                    GDClaim gpClaim = (GDClaim) claim;
                    gpClaim.getVisualizer().createClaimBlockVisuals(height, player.getLocation(), playerData);
                    gpClaim.getVisualizer().apply(player);
                }
            }
        }

        //PaginationService paginationService = Sponge.getServiceManager().provide(PaginationService.class).get();
        //PaginationList.Builder paginationBuilder = paginationService.builder()
        //        .title(Text.of(TextColors.RED,"Claim list")).padding(Text.of(TextStyles.STRIKETHROUGH, "-")).contents(claimsTextList);
        //paginationBuilder.sendTo(src);
        PaginationList.Builder builder = PaginationList.builder().title(TextComponent.of("Claim list", TextColor.RED)).padding(TextComponent.builder(" ").decoration(TextDecoration.STRIKETHROUGH, true).build()).contents(claimsTextList);
        builder.sendTo(src);
    }

    private static Consumer<CommandSender> createShowClaimsConsumer(CommandSender src, Set<Claim> claims, int height, boolean visualizeClaims) {
        return consumer -> {
            showClaims(src, claims, height, visualizeClaims);
        };
    }

    public static List<Component> generateClaimTextList(List<Component> claimsTextList, Set<Claim> claimList, String worldName, GDPermissionUser user, CommandSender src, Consumer<CommandSender> returnCommand, boolean canListOthers, boolean listChildren) {
        return generateClaimTextList(claimsTextList, claimList, worldName, user, src, returnCommand, canListOthers, listChildren, false);
    }

    public static List<Component> generateClaimTextList(List<Component> claimsTextList, Set<Claim> claimList, String worldName, GDPermissionUser user, CommandSender src, Consumer<CommandSender> returnCommand, boolean canListOthers, boolean listChildren, boolean overlap) {
        final OfflinePlayer offlinePlayer = src instanceof OfflinePlayer ? (OfflinePlayer) src : null;
        GDPermissionUser holder = null;
        if (offlinePlayer != null) {
            holder = PermissionHolderCache.getInstance().getOrCreateUser(offlinePlayer);
        }
        if (claimList.size() > 0) {
            for (Claim playerClaim : claimList) {
                GDClaim claim = (GDClaim) playerClaim;
                if (!overlap && !listChildren && claim.isSubdivision() && !claim.getData().getEconomyData().isForSale()) {
                    continue;
                }
                // Only list claims trusted if not an overlap claim
                if (!overlap && holder != null && !claim.isUserTrusted(holder, TrustTypes.ACCESSOR) && !canListOthers) {
                    continue;
                }

                double teleportHeight = claim.getOwnerPlayerData() == null ? 65.0D : (claim.getOwnerMinClaimLevel() > 65.0D ? claim.getOwnerMinClaimLevel() : 65);

                Vector3i lesserPos = claim.lesserBoundaryCorner;
                Vector3i greaterPos = claim.greaterBoundaryCorner;
                Vector3i center = claim.lesserBoundaryCorner.add(lesserPos.getX(), lesserPos.getY(), lesserPos.getZ()).div(2);
                if (teleportHeight == 65 && claim.getWorld().getEnvironment() == Environment.NORMAL) {
                    teleportHeight = claim.getWorld().getHighestBlockYAt((int)center.getX(), (int)center.getZ());
                }

                Vector3i newCenter = new Vector3i(center.getX(), teleportHeight, center.getZ());
                Vector3i southWest = new Vector3i(newCenter.getX(), newCenter.getY(), newCenter.getZ());
                //final double teleportHeight = claim.getOwnerPlayerData() == null ? 65.0D : (claim.getOwnerPlayerData().getMinClaimLevel() > 65.0D ? claim.getOwnerPlayerData().getMinClaimLevel() : 65);
                //Location<World> southWest = claim.lesserBoundaryCorner.setPosition(new Vector3d(claim.lesserBoundaryCorner.getPosition().getX(), teleportHeight, claim.greaterBoundaryCorner.getPosition().getZ()));
                Component claimName = claim.getData().getName().orElse(TextComponent.empty());
                Component teleportName = claim.getData().getName().orElse(claim.getFriendlyNameType());
                Component ownerLine = TextComponent.builder("Owner").color(TextColor.YELLOW)
                        .append(" : ", TextColor.WHITE)
                        .append(claim.getOwnerName().color(TextColor.GOLD))
                        .append("\n").build();
                Component claimTypeInfo = TextComponent.builder("Type").color(TextColor.YELLOW)
                        .append(" : ", TextColor.WHITE)
                        .append(claim.getFriendlyNameType())
                        .append(" ")
                        .append(claim.isCuboid() ? "3D " : "2D ", TextColor.GRAY)
                        .append(" (Area: ", TextColor.WHITE)
                        .append(String.valueOf(claim.getClaimBlocks()), TextColor.GRAY)
                        .append(" blocks)\n", TextColor.WHITE).build();
                Component clickInfo = TextComponent.of("Click to check more info.");
                Component basicInfo = TextComponent.builder("")
                        .append(ownerLine)
                        .append(claimTypeInfo)
                        .append(clickInfo).build();

                Component claimInfoCommandClick = TextComponent.builder("")
                        .append(claim.getFriendlyNameType())
                        .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(CommandHelper.createCommandConsumer(src, "claiminfo", claim.getUniqueId().toString(), createReturnClaimListConsumer(src, returnCommand)))))
                        .hoverEvent(HoverEvent.showText(basicInfo)).build();

                Component claimCoordsTPClick = TextComponent.builder("")
                        .append("[")
                        .append("TP", TextColor.LIGHT_PURPLE)
                        .append("]")
                        .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(CommandHelper.createTeleportConsumer(src, VecHelper.toLocation(claim.getWorld(), southWest), claim))))
                        .hoverEvent(HoverEvent.showText(TextComponent.builder("")
                                .append("Click here to teleport to ")
                                .append(teleportName)
                                .append(" ")
                                .append(southWest.toString())
                                .append(" in ")
                                .append(claim.getWorld().getName(), TextColor.LIGHT_PURPLE)
                                .append(".").build())).build();

                Component claimSpawn = null;
                if (claim.getData().getSpawnPos().isPresent()) {
                    Vector3i spawnPos = claim.getData().getSpawnPos().get();
                    Location spawnLoc = new Location(claim.getWorld(), spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
                    claimSpawn = TextComponent.builder("")
                            .append("[")
                            .append("TP", TextColor.LIGHT_PURPLE)
                            .append("]")
                            .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(CommandHelper.createTeleportConsumer(src, spawnLoc, claim))))
                            .hoverEvent(HoverEvent.showText(TextComponent.builder("")
                                    .append("Click here to teleport to ")
                                    .append(teleportName)
                                    .append("'s spawn @ ")
                                    .append(spawnPos.toString())
                                    .append(" in ")
                                    .append(claim.getWorld().getName(), TextColor.LIGHT_PURPLE)
                                    .append(".").build()))
                            .build();
                } else {
                    claimSpawn = claimCoordsTPClick;
                }

                List<Component> childrenTextList = new ArrayList<>();
                if (!listChildren) {
                    childrenTextList = generateClaimTextList(new ArrayList<Component>(), claim.getChildren(true), worldName, user, src, returnCommand, canListOthers, true);
                }
                final Player player = src instanceof Player ? (Player) src : null;
                Component buyClaim = TextComponent.empty();
                if (player != null && claim.getEconomyData().isForSale() && claim.getEconomyData().getSalePrice() > -1) {
                    Component buyInfo = TextComponent.builder("Price ").color(TextColor.AQUA)
                            .append(": ", TextColor.WHITE)
                            .append(String.valueOf(claim.getEconomyData().getSalePrice()), TextColor.GOLD)
                            .append("\nClick here to purchase claim.").build();
                    buyClaim = TextComponent.builder()
                        .append(claim.getEconomyData().isForSale() ? TextComponent.builder(" [").append("Buy", TextColor.GREEN).append("]", TextColor.WHITE).build() : TextComponent.empty())
                        .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(buyClaimConsumerConfirmation(src, claim))))
                        .hoverEvent(HoverEvent.showText(player.getUniqueId().equals(claim.getOwnerUniqueId()) ? TextComponent.of("You already own this claim.") : buyInfo)).build();
                }
                if (!childrenTextList.isEmpty()) {
                    Component children = TextComponent.builder("[")
                            .append("children", TextColor.AQUA)
                            .append("]", TextColor.WHITE)
                            .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(showChildrenList(childrenTextList, src, returnCommand, claim))))
                            .hoverEvent(HoverEvent.showText(TextComponent.of("Click here to view child claim list."))).build();
                    claimsTextList.add(TextComponent.builder("")
                            .append(claimSpawn)
                            .append(" ")
                            .append(claimInfoCommandClick)
                            .append(" : ", TextColor.WHITE)
                            .append(claim.getOwnerName().color(TextColor.GOLD))
                            .append(" ")
                            .append(children)
                            .append(" ")
                            .append(claimName == TextComponent.empty() ? TextComponent.of("") : claimName)
                            .append(" ")
                            .append(buyClaim)
                            .build());
                } else {
                   claimsTextList.add(TextComponent.builder("")
                           .append(claimSpawn)
                           .append(" ")
                           .append(claimInfoCommandClick)
                           .append(" : ", TextColor.WHITE)
                           .append(claim.getOwnerName().color(TextColor.GOLD))
                           .append(" ")
                           .append(claimName == TextComponent.empty() ? TextComponent.of("") : claimName)
                           .append(buyClaim)
                           .build());
                }
            }
            if (claimsTextList.size() == 0) {
                claimsTextList.add(TextComponent.of("No claims found in world.", TextColor.RED));
            }
        }
        return claimsTextList;
    }

    public static Consumer<CommandSender> buyClaimConsumerConfirmation(CommandSender src, Claim claim) {
        return confirm -> {
            final Player player = (Player) src;
            if (player.getUniqueId().equals(claim.getOwnerUniqueId())) {
                return;
            }
            /*Account playerAccount = GriefDefenderPlugin.getInstance().economyService.get().getOrCreateAccount(player.getUniqueId()).orElse(null);
            if (playerAccount == null) {
                Map<String, ?> params = ImmutableMap.of(
                        "user", player.getName());
                GriefDefenderPlugin.sendMessage(player, MessageStorage.ECONOMY_USER_NOT_FOUND, GriefDefenderPlugin.getInstance().messageData.economyUserNotFound, params);
                return;
            }

            final double balance = playerAccount.getBalance(GriefDefenderPlugin.getInstance().economyService.get().getDefaultCurrency()).doubleValue();
            if (balance < claim.getEconomyData().getSalePrice()) {
                Map<String, ?> params = ImmutableMap.of(
                        "sale_price", claim.getEconomyData().getSalePrice(),
                        "balance", balance,
                        "amount_needed", claim.getEconomyData().getSalePrice() -  balance);
                GriefDefenderPlugin.sendMessage(player, "economy-claim-buy-not-enough-funds", GriefDefenderPlugin.getInstance().messageData.economyClaimBuyNotEnoughFunds, params);
                return;
            }
            final Component message = GriefDefenderPlugin.getInstance().messageData.economyClaimBuyConfirmation
                    .apply(ImmutableMap.of(
                    "sale_price", claim.getEconomyData().getSalePrice())).build();
            GriefDefenderPlugin.sendMessage(src, message);
            final Component buyConfirmationText = TextComponent.builder("")
                    .append("\n[")
                    .append("Confirm", TextColor.GREEN)
                    .append("]\n")
                    .clickEvent(ClickEvent.runCommand(GPCallbackHolder.getInstance().createCallbackRunCommand(createBuyConsumerConfirmed(src, claim)))).build();
            GriefDefenderPlugin.sendMessage(player, buyConfirmationText);*/
        };
    }

    private static Consumer<CommandSender> createBuyConsumerConfirmed(CommandSender src, Claim claim) {
        return confirm -> {
            final Player player = (Player) src;
            final Player ownerPlayer = Bukkit.getPlayer(claim.getOwnerUniqueId());
            /*final Account ownerAccount = GriefDefenderPlugin.getInstance().economyService.get().getOrCreateAccount(claim.getOwnerUniqueId()).orElse(null);
            if (ownerAccount == null) {
                TextAdapter.sendComponent(src, TextComponent.of("Buy cancelled! Could not locate an economy account for owner.", TextColor.RED));
                return;
            }

            final ClaimResult result = claim.transferOwner(player.getUniqueId());
            if (!result.successful()) {
                final Component defaultMessage = TextComponent.builder("")
                        .append("Buy cancelled! Could not transfer owner. Result was ", TextColor.RED)
                        .append(result.getResultType().name(), TextColor.GREEN).build();
                TextAdapter.sendComponent(src, result.getMessage().orElse(defaultMessage));
                return;
            }

            final Currency defaultCurrency = GriefDefenderPlugin.getInstance().economyService.get().getDefaultCurrency();
            final double salePrice = claim.getEconomyData().getSalePrice();
            Sponge.getCauseStackManager().pushCause(src);
            final TransactionResult ownerResult = ownerAccount.deposit(defaultCurrency, BigDecimal.valueOf(salePrice), Sponge.getCauseStackManager().getCurrentCause());
            Account playerAccount = GriefDefenderPlugin.getInstance().economyService.get().getOrCreateAccount(player.getUniqueId()).orElse(null);
            final TransactionResult
                transactionResult =
                playerAccount.withdraw(defaultCurrency, BigDecimal.valueOf(salePrice), Sponge.getCauseStackManager().getCurrentCause());
            final Component message = GriefDefenderPlugin.getInstance().messageData.economyClaimBuyConfirmed
                .apply(ImmutableMap.of(
                    "sale_price", salePrice)).build();
            final Component saleMessage = GriefDefenderPlugin.getInstance().messageData.economyClaimSold
                .apply(ImmutableMap.of(
                    "amount", salePrice,
                    "balance", ownerAccount.getBalance(defaultCurrency))).build();
            if (ownerPlayer != null) {
                TextAdapter.sendComponent(ownerPlayer, saleMessage);
            }
            claim.getEconomyData().setForSale(false);
            claim.getEconomyData().setSalePrice(0);
            claim.getData().save();
            GriefDefenderPlugin.sendMessage(src, message);*/
        };
    }

    public static Consumer<CommandSender> showChildrenList(List<Component> childrenTextList, CommandSender src, Consumer<CommandSender> returnCommand, GDClaim parent) {
        return consumer -> {
            Component claimListReturnCommand = TextComponent.builder("")
                    .append("\n[")
                    .append("Return to claimslist", TextColor.AQUA)
                    .append("]\n", TextColor.WHITE)
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(returnCommand))).build();
    
            List<Component> textList = new ArrayList<>();
            textList.add(claimListReturnCommand);
            textList.addAll(childrenTextList);
            PaginationList.Builder builder = PaginationList.builder()
                    .title(parent.getName().orElse(parent.getFriendlyNameType())
                            .append(TextComponent.of(" Child Claims"))).padding(TextComponent.builder(" ").decoration(TextDecoration.STRIKETHROUGH, true).build()).contents(textList);
            builder.sendTo(src);
        };
    }

    public static Consumer<CommandSender> createReturnClaimListConsumer(CommandSender src, Consumer<CommandSender> returnCommand) {
        return consumer -> {
            Component claimListReturnCommand = TextComponent.builder("")
                    .append("\n[")
                    .append("Return to claimslist", TextColor.AQUA)
                    .append("]\n", TextColor.WHITE)
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(returnCommand))).build();
            TextAdapter.sendComponent(src, claimListReturnCommand);
        };
    }

    public static Consumer<CommandSender> createReturnClaimListConsumer(CommandSender src, String arguments) {
        return consumer -> {
            Component claimListReturnCommand = TextComponent.builder("")
                    .append("\n[")
                    .append("Return to claimslist", TextColor.AQUA)
                    .append("]\n", TextColor.WHITE)
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(CommandHelper.createCommandConsumer(src, "/claimslist", arguments)))).build();
            TextAdapter.sendComponent(src, claimListReturnCommand);
        };
    }

    public static Consumer<CommandSender> createFlagConsumer(CommandSender src, GDPermissionHolder subject, String subjectName, Set<Context> contexts, GDClaim claim, String flagPermission, Tristate flagValue, String source) {
        return consumer -> {
            String target = flagPermission.replace(GDPermissions.FLAG_BASE + ".", "");
            if (target.isEmpty()) {
                target = "any";
            }
            Tristate newValue = Tristate.UNDEFINED;
            if (flagValue == Tristate.TRUE) {
                newValue = Tristate.FALSE;
            } else if (flagValue == Tristate.UNDEFINED) {
                newValue = Tristate.TRUE;
            }

            CommandHelper.applyFlagPermission(src, subject, subjectName, claim, flagPermission, target, newValue, null, FlagType.GROUP);
        };
    }

    public static Component getClickableText(CommandSender src, GDClaim claim, GDPermissionHolder holder, String subjectName, Set<Context> contexts, String flagPermission, Tristate flagValue, FlagType type) {
        String onClickText = "Click here to toggle " + type.name().toLowerCase() + " value.";
        TextComponent.Builder textBuilder = TextComponent.builder(flagValue.toString().toLowerCase())
                .hoverEvent(HoverEvent.showText(TextComponent.builder("")
                        .append(onClickText)
                        .append("\n")
                        .append(getFlagTypeHoverText(type)).build()))
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createFlagConsumer(src, claim, holder, subjectName, contexts, flagPermission, flagValue, type))));
        return textBuilder.build();
    }

    public static Component getClickableText(CommandSender src, GDPermissionHolder subject, String subjectName, Set<Context> contexts, GDClaim claim, String flagPermission, Tristate flagValue, String source, FlagType type) {
        Component onClickText = TextComponent.of("Click here to toggle flag value.");
        boolean hasPermission = true;
        if (type == FlagType.INHERIT) {
            onClickText = TextComponent.builder("")
                    .append("This flag is inherited from parent claim ")
                    .append(claim.getName().orElse(claim.getFriendlyNameType()))
                    .append(" and ")
                    .append(TextComponent.of("cannot").decoration(TextDecoration.UNDERLINED, true))
                    .append(TextComponent.of(" be changed.")).build();
            hasPermission = false;
        } else if (src instanceof Player) {
            Component denyReason = claim.allowEdit((Player) src);
            if (denyReason != null) {
                onClickText = denyReason;
                hasPermission = false;
            }
        }

        TextComponent.Builder textBuilder = TextComponent.builder(flagValue.toString().toLowerCase())
                .hoverEvent(HoverEvent.showText(TextComponent.builder("")
                        .append(onClickText)
                        .append("\n")
                        .append(getFlagTypeHoverText(type)).build()));
        if (hasPermission) {
            textBuilder.clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createFlagConsumer(src, subject, subjectName, contexts, claim, flagPermission, flagValue, source))));
        }
        return textBuilder.build();
    }

    public static String handleCommandFlag(CommandSender src, String target) {
        String pluginId = "minecraft";
        String args = "";
        String command = "";
        int argsIndex = target.indexOf("[");
        if (argsIndex != -1) {
            if (argsIndex == 0) {
                // invalid
                TextAdapter.sendComponent(src, TextComponent.of(
                        "No valid command entered.", TextColor.RED));
                return null;
            }
            command = target.substring(0, argsIndex);
            String[] parts = command.split(":");
            if (parts.length > 1) {
                pluginId = parts[0];
                command = parts[1];
            }
            if (!validateCommandMapping(src, command, pluginId)) {
                return null;
            }
            /*if (!pluginId.equals("minecraft")) {
                Plugin pluginContainer = Sponge.getPluginManager().getPlugin(pluginId).orElse(null);
                if (pluginContainer == null) {
                    TextAdapter.sendComponent(src, TextComponent.builder("")
                            .append("Could not locate a plugin with id '", TextColor.RED)
                            .append(pluginId, TextColor.AQUA)
                            .append("'.", TextColor.RED).build());
                    return null;
                }
            }*/
            args = target.substring(argsIndex, target.length());
            Pattern p = Pattern.compile("\\[([^\\]]+)\\]");
            Matcher m = p.matcher(args);
            if (!m.find()) {
                // invalid
                TextAdapter.sendComponent(src, TextComponent.builder("Invalid arguments '").color(TextColor.RED)
                        .append(args, TextColor.AQUA)
                        .append("' entered. Check syntax matches  'command[arg1:arg2:etc]'", TextColor.RED).build());
                return null;
            }
            args = m.group(1);
            target = pluginId + "." + command + "." + args.replace(":", ".");
        } else {
            String[] parts = target.split(":");
            if (parts.length > 1) {
                pluginId = parts[0];
                command = parts[1];
            } else {
                command = target;
            }
            target = pluginId + "." + command;
        }

        // validate command
        if (!validateCommandMapping(src, command, pluginId)) {
            return null;
        }

        return target;
    }

    private static boolean validateCommandMapping(CommandSender src, String command, String pluginId) {
        Command commandMapping = NMSUtil.getInstance().getBukkitCommandMap().getCommand(command);
        if (commandMapping == null) {
            TextAdapter.sendComponent(src, TextComponent.builder("Could not locate the command '").color(TextColor.RED)
                    .append(command, TextColor.GREEN)
                    .append("' for mod id '", TextColor.RED)
                    .append(pluginId, TextColor.AQUA)
                    .append("'.", TextColor.RED).build());
            return false;
        }
        return true;
    }

    public static String getTrustPermission(TrustType trustType) {
        if (trustType == TrustTypes.ACCESSOR) {
            return GDPermissions.TRUST_ACCESSOR;
        } else if (trustType == TrustTypes.CONTAINER) {
            return GDPermissions.TRUST_CONTAINER;
        } else if (trustType == TrustTypes.BUILDER) {
            return GDPermissions.TRUST_BUILDER;
        } else {
            return GDPermissions.TRUST_MANAGER;
        }
    }

    public static Consumer<CommandSender> createTeleportConsumer(CommandSender src, Location location, Claim claim) {
        return teleport -> {
            if (!(src instanceof Player)) {
                // ignore
                return;
            }
            Player player = (Player) src;
            GDClaim gpClaim = (GDClaim) claim;
            GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
            if (!playerData.canIgnoreClaim(gpClaim) && !playerData.canManageAdminClaims) {
                // if not owner of claim, validate perms
                if (!player.getUniqueId().equals(claim.getOwnerUniqueId())) {
                    if (!player.hasPermission(GDPermissions.COMMAND_CLAIM_INFO_TELEPORT_OTHERS) && !gpClaim.isUserTrusted(player, TrustTypes.ACCESSOR)) {
                        TextAdapter.sendComponent(player, TextComponent.of("You do not have permission to use the teleport feature in this claim.", TextColor.RED)); 
                        return;
                    }
                } else if (!player.hasPermission(GDPermissions.COMMAND_CLAIM_INFO_TELEPORT_BASE)) {
                    TextAdapter.sendComponent(player, TextComponent.of("You do not have permission to use the teleport feature in your claim.", TextColor.RED)); 
                    return;
                }
            }

            player.teleport(location, TeleportCause.PLUGIN);
        };
    }

    public static Consumer<CommandSender> createForceTeleportConsumer(Player player, Location location) {
        return teleport -> {
            player.teleport(location, TeleportCause.PLUGIN);
        };
    }

    public static void handleBankTransaction(CommandSender src, String[] args, GDClaim claim) {
        if (GriefDefenderPlugin.getInstance().getVaultProvider() == null) {
            GriefDefenderPlugin.sendMessage(src, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_NOT_INSTALLED));
            return;
        }

        if (claim.isSubdivision() || claim.isAdminClaim()) {
            return;
        }

        if (!claim.getEconomyAccountId().isPresent()) {
            GriefDefenderPlugin.sendMessage(src, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_VIRTUAL_NOT_SUPPORTED));
            return;
        }

        final Economy economy = GriefDefenderPlugin.getInstance().getVaultProvider().getApi();
        final String command = args[0];
        final double amount = args.length > 1 ? Double.parseDouble(args[1]) : 0;

        final UUID playerSource = ((Player) src).getUniqueId();
        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getPlayerData(claim.getWorld(), claim.getOwnerUniqueId());
        if (playerData.canIgnoreClaim(claim) || claim.getOwnerUniqueId().equals(playerSource) || claim.getUserTrusts(TrustTypes.MANAGER).contains(playerData.playerID)) {
            final UUID bankAccount = claim.getEconomyAccountId().orElse(null);
            if (bankAccount == null) {
                GriefDefenderPlugin.sendMessage(src, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_VIRTUAL_NOT_SUPPORTED));
                return;
            }
            if (command.equalsIgnoreCase("withdraw")) {
                EconomyResponse result = economy.bankWithdraw(bankAccount.toString(), amount);
                if (result.transactionSuccess()) {
                    final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.BANK_WITHDRAW,
                        ImmutableMap.of(
                            "amount", amount));
                    economy.depositPlayer(((Player) src), amount);
                    claim.getData().getEconomyData().addBankTransaction(
                        new GDBankTransaction(BankTransactionType.WITHDRAW_SUCCESS, playerData.playerID, Instant.now(), amount));
                } else {
                    final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.BANK_WITHDRAW_NO_FUNDS,
                        ImmutableMap.of(
                            "balance", economy.bankBalance(bankAccount.toString()),
                            "amount", amount));
                    GriefDefenderPlugin.sendMessage(src, message);
                    claim.getData().getEconomyData()
                        .addBankTransaction(new GDBankTransaction(BankTransactionType.WITHDRAW_FAIL, playerData.playerID, Instant.now(), amount));
                    return;
                }
            } else if (command.equalsIgnoreCase("deposit")) {
                EconomyResponse result = economy.withdrawPlayer(((Player) src), amount);
                if (result.transactionSuccess()) {
                    double depositAmount = amount;
                    if (claim.getData().isExpired()) {
                        final double taxBalance = claim.getEconomyData().getTaxBalance();
                        depositAmount -= claim.getEconomyData().getTaxBalance();
                        if (depositAmount >= 0) {
                            claim.getEconomyData().addBankTransaction(new GDBankTransaction(BankTransactionType.TAX_SUCCESS, Instant.now(), taxBalance));
                            claim.getEconomyData().setTaxPastDueDate(null);
                            claim.getEconomyData().setTaxBalance(0);
                            claim.getInternalClaimData().setExpired(false);
                            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.TAX_CLAIM_PAID_BALANCE,
                                    ImmutableMap.of(
                                        "amount", taxBalance));
                            GriefDefenderPlugin.sendMessage(src, message);
                            if (depositAmount == 0) {
                                return;
                            }
                        } else {
                            final double newTaxBalance = Math.abs(depositAmount);
                            claim.getEconomyData().setTaxBalance(newTaxBalance);
                            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.TAX_CLAIM_PAID_PARTIAL,
                                    ImmutableMap.of(
                                        "amount", depositAmount,
                                        "balance", newTaxBalance));
                            GriefDefenderPlugin.sendMessage(src, message);
                            return;
                        }
                    }
                    final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.BANK_DEPOSIT, ImmutableMap.of(
                            "amount", depositAmount));
                    GriefDefenderPlugin.sendMessage(src, message);
                    economy.bankDeposit(bankAccount.toString(), depositAmount);
                    claim.getData().getEconomyData().addBankTransaction(
                        new GDBankTransaction(BankTransactionType.DEPOSIT_SUCCESS, playerData.playerID, Instant.now(), depositAmount));
                } else {
                    GriefDefenderPlugin.sendMessage(src, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.BANK_WITHDRAW_NO_FUNDS));
                    claim.getData().getEconomyData()
                        .addBankTransaction(new GDBankTransaction(BankTransactionType.DEPOSIT_FAIL, playerData.playerID, Instant.now(), amount));
                    return;
                }
            }
        } else {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.BANK_NO_PERMISSION,
                    ImmutableMap.of(
                            "player", claim.getOwnerName()));
            GriefDefenderPlugin.sendMessage(src, message);
        }
    }

    public static void displayClaimBankInfo(Player player, GDClaim claim) {
        displayClaimBankInfo(player, claim, false, false);
    }

    public static void displayClaimBankInfo(CommandSender player, GDClaim claim, boolean checkTown, boolean returnToClaimInfo) {
        if (GriefDefenderPlugin.getInstance().getVaultProvider() == null) {
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_NOT_INSTALLED));
            return;
        }

        if (checkTown && !claim.isInTown()) {
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.TOWN_NOT_IN));
            return;
        }

        if (!checkTown && (claim.isSubdivision() || claim.isAdminClaim())) {
            return;
        }

        final GDClaim town = claim.getTownClaim();
        final UUID bankAccount = checkTown ? town.getEconomyAccountId().orElse(null) : claim.getEconomyAccountId().orElse(null);
        if (bankAccount == null) {
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_VIRTUAL_NOT_SUPPORTED));
            return;
        }

        final Economy economy = GriefDefenderPlugin.getInstance().getVaultProvider().getApi();
        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getPlayerData(claim.getWorld(), claim.getOwnerUniqueId());
        final double claimBalance = economy.bankBalance(bankAccount.toString()).balance;
        double taxOwed = -1;
        final double playerTaxRate = GDPermissionManager.getInstance().getInternalOptionValue((Player) player, Options.TAX_RATE, claim, playerData);
        if (checkTown) {
            if (!town.getOwnerUniqueId().equals(playerData.playerID)) {
                for (Claim playerClaim : playerData.getInternalClaims()) {
                    GDClaim playerTown = (GDClaim) playerClaim.getTown().orElse(null);
                    if (!playerClaim.isTown() && playerTown != null && playerTown.getUniqueId().equals(claim.getUniqueId())) {
                        taxOwed += playerTown.getClaimBlocks() * playerTaxRate;
                    }
                }
            } else {
                taxOwed = town.getClaimBlocks() * playerTaxRate;
            }
        } else {
            taxOwed = claim.getClaimBlocks() * playerTaxRate;
        }

        final GriefDefenderConfig<?> activeConfig = GriefDefenderPlugin.getActiveConfig(claim.getWorld().getUID());
        final ZonedDateTime withdrawDate = TaskUtil.getNextTargetZoneDate(activeConfig.getConfig().claim.taxApplyHour, 0, 0);
        Duration duration = Duration.between(Instant.now().truncatedTo(ChronoUnit.SECONDS), withdrawDate.toInstant()) ;
        final long s = duration.getSeconds();
        final String timeLeft = String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, (s % 60));
        final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.BANK_INFO,
                ImmutableMap.of(
                "balance", claimBalance,
                "tax-amount", taxOwed,
                "time-remaining", timeLeft,
                "tax-balance", claim.getData().getEconomyData().getTaxBalance()));
        Component transactions = TextComponent.builder("")
                .append("Bank Transactions", TextColor.AQUA, TextDecoration.ITALIC)
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createBankTransactionsConsumer(player, claim, checkTown, returnToClaimInfo))))
                .hoverEvent(HoverEvent.showText(TextComponent.of("Click here to view bank transactions")))
                .build();
        List<Component> textList = new ArrayList<>();
        if (returnToClaimInfo) {
            textList.add(TextComponent.builder("")
                    .append("\n[")
                    .append("Return to claim info", TextColor.AQUA)
                    .append("]\n")
                    .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(CommandHelper.createCommandConsumer(player, "claiminfo", "")))).build());
        }
        textList.add(message);
        textList.add(transactions);
        PaginationList.Builder builder = PaginationList.builder()
                .title(TextComponent.of("Bank Info", TextColor.AQUA)).padding(TextComponent.builder(" ").decoration(TextDecoration.STRIKETHROUGH, true).build()).contents(textList);
        builder.sendTo(player);
    }

    public static Consumer<CommandSender> createBankTransactionsConsumer(CommandSender src, GDClaim claim, boolean checkTown, boolean returnToClaimInfo) {
        return settings -> {
            final String name = "Bank Transactions";
            List<String> bankTransactions = new ArrayList<>(claim.getData().getEconomyData().getBankTransactionLog());
            Collections.reverse(bankTransactions);
            List<Component> textList = new ArrayList<>();
            textList.add(TextComponent.builder("")
                    .append("\n[")
                    .append("Return to bank info", TextColor.AQUA)
                    .append("]\n")
                    .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(consumer -> { displayClaimBankInfo(src, claim, checkTown, returnToClaimInfo); }))).build());
            Gson gson = new Gson();
            for (String transaction : bankTransactions) {
                GDBankTransaction bankTransaction = gson.fromJson(transaction, GDBankTransaction.class);
                final Duration duration = Duration.between(bankTransaction.timestamp, Instant.now().truncatedTo(ChronoUnit.SECONDS)) ;
                final long s = duration.getSeconds();
                final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(bankTransaction.source);
                final String timeLeft = String.format("%dh %02dm %02ds", s / 3600, (s % 3600) / 60, (s % 60)) + " ago";
                textList.add(TextComponent.builder("")
                        .append(bankTransaction.type.name(), getTransactionColor(bankTransaction.type))
                        .append(" | ", TextColor.BLUE)
                        .append(TextComponent.of(String.valueOf(bankTransaction.amount)))
                        .append(" | ", TextColor.BLUE)
                        .append(timeLeft, TextColor.GRAY)
                        .append(user == null ? TextComponent.empty() : TextComponent.builder("")
                                .append(" | ", TextColor.BLUE)
                                .append(user.getName(), TextColor.LIGHT_PURPLE)
                                .build())
                        .build());
            }
            textList.add(TextComponent.builder("")
                    .append("\n[")
                    .append("Return to bank info", TextColor.AQUA)
                    .append("]\n")
                    .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(CommandHelper.createCommandConsumer(src, "claimbank", "")))).build());
            PaginationList.Builder builder = PaginationList.builder()
                    .title(TextComponent.of(name, TextColor.AQUA)).padding(TextComponent.builder(" ").decoration(TextDecoration.STRIKETHROUGH, true).build()).contents(textList);
            builder.sendTo(src);
        };
    }

    public static TextColor getTransactionColor(BankTransactionType type) {
        switch (type) {
            case DEPOSIT_SUCCESS :
            case TAX_SUCCESS :
            case WITHDRAW_SUCCESS :
                return TextColor.GREEN;
            case DEPOSIT_FAIL :
            case TAX_FAIL :
            case WITHDRAW_FAIL :
                return TextColor.RED;
            default :
                return TextColor.GREEN;
        }
    }

    public static List<Component> stripeText(List<Component> texts) {
        Collections.sort(texts, PLAIN_COMPARATOR);

        ImmutableList.Builder<Component> finalTexts = ImmutableList.builder();
        for (int i = 0; i < texts.size(); i++) {
            Component text = texts.get(i);
            if (i % 2 == 0) { 
                text = text.color(TextColor.GREEN);
            } else {
                text = text.color(TextColor.AQUA);
            }

            finalTexts.add(text);
        }
        return finalTexts.build();
    }

    public static Component getFlagTypeHoverText(FlagType type) {
        Component hoverText = TextComponent.empty();
        if (type == FlagType.DEFAULT) {
            hoverText = TextComponent.builder("")
                    .append("DEFAULT ", TextColor.LIGHT_PURPLE)
                    .append(" : Default is last to be checked. Both claim and override take priority over this.")
                    .build();
        } else if (type == FlagType.CLAIM) {
            hoverText = TextComponent.builder("")
                    .append("CLAIM", TextColor.GOLD)
                    .append(" : Claim is checked before default values. Allows claim owners to specify flag settings in claim only.")
                    .build();
        } else if (type == FlagType.OVERRIDE) {
            hoverText = TextComponent.builder("")
                    .append("OVERRIDE", TextColor.RED)
                    .append(" : Override has highest priority and is checked above both default and claim values. Allows admins to override all basic and admin claims.")
                    .build();
        } else if (type == FlagType.INHERIT) {
            hoverText = TextComponent.builder("")
                    .append("INHERIT", TextColor.AQUA)
                    .append(" : Inherit is an enforced flag set by a parent claim that cannot changed.")
                    .build();
        }
        return hoverText;
    }

    public static Component getBaseFlagOverlayText(String flagPermission) {
        String baseFlag = flagPermission.replace(GDPermissions.FLAG_BASE + ".", "").replace(".*", "");
        int endIndex = baseFlag.indexOf(".");
        if (endIndex != -1) {
            baseFlag = baseFlag.substring(0, endIndex);
        }

        final Flag flag = FlagRegistryModule.getInstance().getById(baseFlag).orElse(null);
        if (flag == null) {
            return TextComponent.of("Not defined.");
        }

        return flag.getDescription();
    }

    public static TrustType getTrustType(String type) {
        switch (type.toLowerCase()) {
            case "accessor" :
                return TrustTypes.ACCESSOR;
            case "builder" :
                return TrustTypes.BUILDER;
            case "container" :
                return TrustTypes.CONTAINER;
            case "manager" :
                return TrustTypes.MANAGER;
            case "none" :
                return TrustTypes.NONE;
            default :
                return null;
        }
    }

    public static boolean checkTrustPermission(Player player, TrustType type) {
        if (type == TrustTypes.ACCESSOR) {
            return player.hasPermission(GDPermissions.GIVE_ACCESS_TRUST);
        }
        if (type == TrustTypes.BUILDER) {
            return player.hasPermission(GDPermissions.GIVE_BUILDER_TRUST);
        }
        if (type == TrustTypes.CONTAINER) {
            return player.hasPermission(GDPermissions.GIVE_CONTAINER_TRUST);
        }
        if (type == TrustTypes.MANAGER) {
            return player.hasPermission(GDPermissions.GIVE_MANAGER_TRUST);
        }
        if (type == TrustTypes.NONE) {
            return player.hasPermission(GDPermissions.REMOVE_TRUST);
        }

        return true;
    }
}
