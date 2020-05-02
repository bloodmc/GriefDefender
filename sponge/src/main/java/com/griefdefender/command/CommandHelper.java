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
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.Subject;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimContexts;
import com.griefdefender.api.claim.ClaimResult;
import com.griefdefender.api.claim.TrustType;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.api.economy.BankTransactionType;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.PermissionResult;
import com.griefdefender.api.permission.ResultTypes;
import com.griefdefender.api.permission.flag.Flag;
import com.griefdefender.api.permission.flag.Flags;
import com.griefdefender.api.permission.option.Option;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.api.permission.option.type.CreateModeTypes;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.GriefDefenderConfig;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.economy.GDBankTransaction;
import com.griefdefender.internal.pagination.PaginationList;
import com.griefdefender.internal.util.VecHelper;
import com.griefdefender.internal.visual.ClaimVisual;
import com.griefdefender.permission.GDPermissionHolder;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissionResult;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.permission.flag.GDFlag;
import com.griefdefender.permission.ui.MenuType;
import com.griefdefender.permission.ui.UIHelper;
import com.griefdefender.text.action.GDCallbackHolder;
import com.griefdefender.util.PermissionUtil;
import com.griefdefender.util.TaskUtil;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.spongeapi.TextAdapter;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.text.serializer.plain.PlainComponentSerializer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandMapping;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.property.entity.EyeLocationProperty;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.math.BigDecimal;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandHelper {

    public static Comparator<Component> PLAIN_COMPARATOR = (text1, text2) -> PlainComponentSerializer.INSTANCE.serialize(text1).compareTo(PlainComponentSerializer.INSTANCE.serialize(text2));

    public static Player checkPlayer(CommandSource source) {
        if (source instanceof Player) {
            return ((Player) source);
        } else {
            return null;
        }
    }

    public static boolean validateFlagTarget(Flag flag, String target) {
        if (!(flag instanceof GDFlag)) {
            return true;
        }

        if (flag == Flags.BLOCK_BREAK || flag == Flags.BLOCK_PLACE || flag == Flags.COLLIDE_BLOCK) {
            if (validateBlockTarget(target) ||
                validateItemTarget(target)) {
                return true;
            }
            return false;
        }
        if (flag == Flags.ENTER_CLAIM || flag == Flags.EXIT_CLAIM || flag == Flags.ENTITY_RIDING ||
                flag == Flags.ENTITY_DAMAGE || flag == Flags.PORTAL_USE) {
            if (validateEntityTarget(target) ||
                validateBlockTarget(target) ||
                validateItemTarget(target)) {
                return true;
            }

            return false;
        }
        if (flag == Flags.INTERACT_INVENTORY) {
            if (validateEntityTarget(target) || validateBlockTarget(target)) {
                return true;
            }

            return false;
        }
        if (flag == Flags.LIQUID_FLOW || flag == Flags.INTERACT_BLOCK_PRIMARY 
                || flag == Flags.INTERACT_BLOCK_SECONDARY) {
            return validateBlockTarget(target);
        }
        if (flag == Flags.ENTITY_CHUNK_SPAWN || flag == Flags.ENTITY_SPAWN ||
                flag == Flags.INTERACT_ENTITY_PRIMARY || flag == Flags.INTERACT_ENTITY_SECONDARY) {
            return validateEntityTarget(target);
        }
        if (flag == Flags.ITEM_DROP|| flag == Flags.ITEM_PICKUP ||
                flag == Flags.ITEM_SPAWN || flag == Flags.ITEM_USE) {
            return validateItemTarget(target);
        }

        return true;
    }

    private static boolean validateEntityTarget(String target) {
        Optional<EntityType> entityType = Sponge.getRegistry().getType(EntityType.class, target);
        if (entityType.isPresent()) {
            return true;
        }

        return false;
    }

    private static boolean validateItemTarget(String target) {
        Optional<ItemType> itemType = Sponge.getRegistry().getType(ItemType.class, target);
        if (itemType.isPresent()) {
            return true;
        }
        // target could be an item block, so validate blockstate
        Optional<BlockState> blockState = Sponge.getRegistry().getType(BlockState.class, target);
        if (blockState.isPresent()) {
            return true;
        }

        return false;
    }

    private static boolean validateBlockTarget(String target) {
        Optional<BlockType> blockType = Sponge.getRegistry().getType(BlockType.class, target);
        if (blockType.isPresent()) {
            return true;
        }

        Optional<BlockState> blockState = Sponge.getRegistry().getType(BlockState.class, target);
        if (blockState.isPresent()) {
            return true;
        }
        return false;
    }

    public static PermissionResult addFlagPermission(CommandSource src, GDPermissionHolder subject, Claim claim, Flag flag, String target, Tristate value, Set<Context> contexts) {
        if (src instanceof Player) {
            Component denyReason = ((GDClaim) claim).allowEdit((Player) src);
            if (denyReason != null) {
                GriefDefenderPlugin.sendMessage(src, denyReason);
                return new GDPermissionResult(ResultTypes.NO_PERMISSION);
            }
            if (value == null) {
                GriefDefenderPlugin.sendMessage(src, MessageCache.getInstance().COMMAND_INVALID);
                return new GDPermissionResult(ResultTypes.FAILURE);
            }
        }

        // special handling for commands
        target = adjustTargetForTypes(target, flag);
        if (flag == Flags.COMMAND_EXECUTE || flag == Flags.COMMAND_EXECUTE_PVP) {
            target = handleCommandFlag(src, target);
            if (target == null) {
                // failed
                return new GDPermissionResult(ResultTypes.TARGET_NOT_VALID);
            }
        } else {
            if (!target.equalsIgnoreCase("any")) {
                if (!target.startsWith("#") && !target.contains(":")) {
                    // assume vanilla
                    target = "minecraft:" + target;
                }
    
                String[] parts = target.split(":");
                if (parts.length == 1) {
                    addFlagContexts(contexts, flag, target);
                } else if (parts.length > 1 && !parts[1].equalsIgnoreCase("any")) {
                    addFlagContexts(contexts, flag, target);
                    if (!target.contains("#") && !CommandHelper.validateFlagTarget(flag, target)) {
                        return new GDPermissionResult(ResultTypes.TARGET_NOT_VALID);
                    }
                }
            } else {
                target = "";
            }
        }

        return applyFlagPermission(src, subject, claim, flag, target, value, contexts, null, false);
    }

    public static PermissionResult applyFlagPermission(CommandSource src, GDPermissionHolder subject, Claim claim, Flag flag, String target, Tristate value, Set<Context> contexts, MenuType flagType) {
        return applyFlagPermission(src, subject, claim, flag, target, value, contexts, flagType, false);
    }

    public static PermissionResult applyFlagPermission(CommandSource src, GDPermissionHolder subject, Claim claim, Flag flag, String target, Tristate value, Set<Context> contexts, MenuType flagType, boolean clicked) {
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
        // Add target context
        if (target != null && !target.isEmpty() && !target.equalsIgnoreCase("any")) {
            contexts.add(new Context("target", target));
        }

        if (flagType == null) {
            if (hasDefaultContext) {
                flagType = MenuType.DEFAULT;
            } else if (hasOverrideContext) {
                flagType = MenuType.OVERRIDE;
            } else {
                flagType = MenuType.CLAIM;
            }
        }

        TextComponent.Builder builder = null;
        if (flagType == MenuType.OVERRIDE) {
            builder = TextComponent.builder("OVERRIDE").color(TextColor.RED);
        } else if (flagType == MenuType.DEFAULT) {
            builder = TextComponent.builder("DEFAULT").color(TextColor.LIGHT_PURPLE);
        } else if (flagType == MenuType.CLAIM) {
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

        // Check if player can manage flag with contexts
        if (src instanceof Player) {
            final GDPermissionUser sourceUser = PermissionHolderCache.getInstance().getOrCreateUser(((Player) src));
            final Tristate result = PermissionUtil.getInstance().getPermissionValue(sourceUser, GDPermissions.USER_CLAIM_FLAGS + "." + flag.getName().toLowerCase(), contexts);
            if (result != Tristate.TRUE) {
                GriefDefenderPlugin.sendMessage(src, MessageCache.getInstance().PERMISSION_FLAG_USE);
                return new GDPermissionResult(ResultTypes.NO_PERMISSION);
            }
        }

        if (subject == GriefDefenderPlugin.DEFAULT_HOLDER) {
            PermissionUtil.getInstance().setPermissionValue(GriefDefenderPlugin.DEFAULT_HOLDER, flag, value, contexts);
            if (!clicked && src instanceof Player) {
                TextAdapter.sendComponent(src, TextComponent.builder("")
                    .append(TextComponent.builder("\n[").append(MessageCache.getInstance().FLAG_UI_RETURN_FLAGS.color(TextColor.AQUA)).append("]\n")
                    .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createCommandConsumer(src, "claimflag", "")))).build())
                        .append(MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.FLAG_SET_PERMISSION_TARGET,
                                ImmutableMap.of(
                                    "type", flagTypeText,
                                    "permission", flag.getPermission(),
                                    "contexts", getFriendlyContextString(claim, contexts),
                                    "value", getClickableText(src, (GDClaim) claim, subject, contexts, flag, value, flagType).color(TextColor.LIGHT_PURPLE),
                                    "target", "ALL")))
                        .build());
            }
        } else {
            PermissionUtil.getInstance().setPermissionValue(subject, flag, value, contexts);
            if (!clicked && src instanceof Player) {
                TextAdapter.sendComponent(src, TextComponent.builder("")
                        .append(TextComponent.builder("")
                                .append("\n[")
                                .append(MessageCache.getInstance().FLAG_UI_RETURN_FLAGS.color(TextColor.AQUA))
                                .append("]\n", TextColor.WHITE)
                                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createCommandConsumer(src, subject instanceof GDPermissionUser ? "claimflagplayer" : "claimflaggroup", subject.getFriendlyName())))).build())
                        .append(MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.FLAG_SET_PERMISSION_TARGET,
                                ImmutableMap.of(
                                        "type", flagTypeText,
                                        "permission", flag.getPermission(),
                                        "contexts", getFriendlyContextString(claim, contexts),
                                        "value", getClickableText(src, (GDClaim) claim, subject, contexts, flag, value, flagType).color(TextColor.LIGHT_PURPLE),
                                        "target", subject.getFriendlyName())))
                        .build());
            }
        }

        return new GDPermissionResult(ResultTypes.SUCCESS);
    }

    public static String adjustTargetForTypes(String target, Flag flag) {
        if (target.equals("player") || target.equals("minecraft:player") || target.equalsIgnoreCase("any")) {
            return target;
        }

        if (flag.getName().contains("entity") || flag == Flags.ITEM_SPAWN) {
            final String contextKey = "target";
            String[] parts = target.split(":");
            String targetId = "";
            if (parts.length == 1) {
                targetId = parts[0];
            } else {
                targetId = parts[1];
            }

            if (targetId.equalsIgnoreCase("animal")) {
                return "#animal";
            } else if (targetId.equalsIgnoreCase("aquatic")) {
                return "#aquatic";
            } else if (targetId.equalsIgnoreCase("monster")) {
                return "#monster";
            } else if (targetId.equalsIgnoreCase("ambient")) {
                return "#ambient";
            }
            return target;
        } else {
            if ((target.equals("food") || target.endsWith(":food")) && !target.startsWith("#")) {
                target = "#" + target;
            }
        }
        return target;
    }

    public static void addFlagContexts(Set<Context> contexts, Flag flag, String target) {
        if (target.equals("player") || target.equals("minecraft:player") || target.equalsIgnoreCase("any")) {
            return;
        }

        if (flag.getName().contains("entity") || flag == Flags.ITEM_SPAWN) {
            final String contextKey = "target";
            String[] parts = target.split(":");
            if (parts.length == 1) {
                contexts.add(new Context(contextKey, target));
                return;
            }

            if (parts[1].equalsIgnoreCase("animal")) {
                contexts.add(new Context(contextKey, "#animal"));
            } else if (parts[1].equalsIgnoreCase("aquatic")) {
                contexts.add(new Context(contextKey, "#aquatic"));
            } else if (parts[1].equalsIgnoreCase("monster")) {
                contexts.add(new Context(contextKey, "#monster"));
            } else if (parts[1].equalsIgnoreCase("ambient")) {
                contexts.add(new Context(contextKey, "#ambient"));
            } else {
                contexts.add(new Context(contextKey, target));
            }
        }
    }

    public static Component getFriendlyContextString(Claim claim, Set<Context> contexts) {
        if (contexts.isEmpty()) {
            return TextComponent.of("[]", TextColor.WHITE);
        }

        TextComponent.Builder builder = TextComponent.builder();
        final Iterator<Context> iterator = contexts.iterator();
        while (iterator.hasNext()) {
            final Context context = iterator.next();
            builder.append("\n[", TextColor.WHITE)
                .append(context.getKey(), TextColor.GREEN)
                .append("=", TextColor.GRAY)
                .append(context.getValue(), TextColor.WHITE);

            if (iterator.hasNext()) {
                builder.append("], ");
            } else {
                builder.append("]");
            }
        }
        return builder.build();
    }

    public static TextColor getPermissionMenuTypeColor(MenuType type) {
        TextColor color = TextColor.LIGHT_PURPLE;
        if (type == MenuType.CLAIM) {
            color = TextColor.GOLD;
        } else if (type == MenuType.OVERRIDE) {
            color = TextColor.RED;
        }

        return color;
    }

   public static Consumer<CommandSource> createFlagConsumer(CommandSource src, GDClaim claim, Subject subject, Set<Context> contexts, Flag flag, Tristate flagValue, MenuType flagType) {
        return consumer -> {
            Tristate newValue = Tristate.UNDEFINED;
            if (flagValue == Tristate.TRUE) {
                newValue = Tristate.FALSE;
            } else if (flagValue == Tristate.UNDEFINED) {
                newValue = Tristate.TRUE;
            }

            Component flagTypeText = TextComponent.empty();
            if (flagType == MenuType.OVERRIDE) {
                flagTypeText = TextComponent.of("OVERRIDE", TextColor.RED);
            } else if (flagType == MenuType.DEFAULT) {
                flagTypeText = TextComponent.of("DEFAULT", TextColor.LIGHT_PURPLE);
            } else if (flagType == MenuType.CLAIM) {
                flagTypeText = TextComponent.of("CLAIM", TextColor.GOLD);
            }

            Set<Context> newContexts = new HashSet<>(contexts);
            PermissionUtil.getInstance().setPermissionValue(GriefDefenderPlugin.DEFAULT_HOLDER, flag, newValue, newContexts);
            TextAdapter.sendComponent(src, TextComponent.builder("")
                    .append("Set ", TextColor.GREEN)
                    .append(flagTypeText)
                    .append(" permission ")
                    .append(flag.getName().toLowerCase(), TextColor.AQUA)
                    .append("\n to ", TextColor.GREEN)
                    .append(getClickableText(src, (GDClaim) claim, subject, newContexts, flag, newValue, flagType).color(TextColor.LIGHT_PURPLE))
                    .append(" for ", TextColor.GREEN)
                    .append(subject.getFriendlyName(), TextColor.GOLD).build());
        };
    }

    public static Consumer<CommandSource> createCommandConsumer(CommandSource src, String command, String arguments) {
        return createCommandConsumer(src, command, arguments, null);
    }

    public static Consumer<CommandSource> createCommandConsumer(CommandSource src, String command, String arguments, Consumer<CommandSource> postConsumerTask) {
        return consumer -> {
            try {
                Sponge.getCommandManager().get(command).get().getCallable().process(src, arguments);
            } catch (CommandException e) {
                src.sendMessage(e.getText());
            }
            if (postConsumerTask != null) {
                postConsumerTask.accept(src);
            }
        };
    }

    public static void executeCommand(CommandSource src, String command, String arguments) {
        try {
            Sponge.getCommandManager().get(command).get().getCallable().process(src, arguments);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
        }
    }

    public static void showClaims(CommandSource src, Set<Claim> claims) {
        if (claims.isEmpty()) {
            // do nothing
            return;
        }
        showClaims(src, claims, 0, false);
    }

    public static void showOverlapClaims(CommandSource src, Set<Claim> claims, int height) {
        showClaims(src, claims, height, true, true);
    }

    public static void showClaims(CommandSource src, Set<Claim> claims, int height, boolean visualizeClaims) {
        showClaims(src, claims, height, visualizeClaims, false);
    }

    public static void showClaims(CommandSource src, Set<Claim> claims, int height, boolean visualizeClaims, boolean overlap) {
        final String worldName = src instanceof Player ? ((Player) src).getWorld().getName() : Sponge.getServer().getDefaultWorldName();
        List<Component> claimsTextList = generateClaimTextList(new ArrayList<Component>(), claims, worldName, null, src, createShowClaimsConsumer(src, claims, height, visualizeClaims), true, false, overlap);

        if (visualizeClaims && src instanceof Player) {
            Player player = (Player) src;
            final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
            if (claims.size() > 1) {
                if (height != 0) {
                    height = playerData.lastValidInspectLocation != null ? playerData.lastValidInspectLocation.getBlockY() : player.getProperty(EyeLocationProperty.class).get().getValue().getFloorY();
                }
                ClaimVisual visualization = ClaimVisual.fromClaims(claims, playerData.getClaimCreateMode() == CreateModeTypes.VOLUME ? height : player.getProperty(EyeLocationProperty.class).get().getValue().getFloorY(), player.getLocation(), playerData, null);
                visualization.apply(player);
            } else {
                for (Claim claim : claims) {
                    GDClaim gpClaim = (GDClaim) claim;
                    gpClaim.getVisualizer().createClaimBlockVisuals(height, player.getLocation(), playerData);
                    gpClaim.getVisualizer().apply(player);
                }
            }
        }

        PaginationList.Builder builder = PaginationList.builder().title(MessageCache.getInstance().CLAIMLIST_UI_TITLE.color(TextColor.RED)).padding(TextComponent.builder(" ").decoration(TextDecoration.STRIKETHROUGH, true).build()).contents(claimsTextList);
        builder.sendTo(src);
    }

    private static Consumer<CommandSource> createShowClaimsConsumer(CommandSource src, Set<Claim> claims, int height, boolean visualizeClaims) {
        return consumer -> {
            showClaims(src, claims, height, visualizeClaims);
        };
    }

    public static List<Component> generateClaimTextListCommand(List<Component> claimsTextList, Set<Claim> claimList, String worldName, GDPermissionUser user, CommandSource src, Consumer<CommandSource> returnCommand, boolean listChildren) {
        return generateClaimTextList(claimsTextList, claimList, worldName, user, src, returnCommand, listChildren, false, true);
    }

    public static List<Component> generateClaimTextList(List<Component> claimsTextList, Set<Claim> claimList, String worldName, GDPermissionUser user, CommandSource src, Consumer<CommandSource> returnCommand, boolean listChildren) {
        return generateClaimTextList(claimsTextList, claimList, worldName, user, src, returnCommand, listChildren, false, false);
    }

    public static List<Component> generateClaimTextList(List<Component> claimsTextList, Set<Claim> claimList, String worldName, GDPermissionUser user, CommandSource src, Consumer<CommandSource> returnCommand, boolean listChildren, boolean overlap, boolean listCommand) {
        if (claimList.size() > 0) {
            final Player player = src instanceof Player ? (Player) src : null;
            for (Claim playerClaim : claimList) {
                GDClaim claim = (GDClaim) playerClaim;
                if (player != null && !claim.getData().getEconomyData().isForSale() && !claim.isUserTrusted(player, TrustTypes.ACCESSOR)) {
                    continue;
                }
                if (!listCommand && !overlap && !listChildren && claim.isSubdivision() && !claim.getData().getEconomyData().isForSale()) {
                    continue;
                }

                double teleportHeight = claim.getOwnerPlayerData() == null ? 65.0D : (claim.getOwnerMinClaimLevel() > 65.0D ? claim.getOwnerMinClaimLevel() : 65);
                Vector3i lesserPos = claim.lesserBoundaryCorner;
                Vector3i greaterPos = claim.greaterBoundaryCorner;
                Vector3i center = claim.lesserBoundaryCorner.add(lesserPos.getX(), lesserPos.getY(), lesserPos.getZ()).div(2);
                Vector3i newCenter = new Vector3i(center.getX(), teleportHeight, center.getZ());
                Vector3i southWest = new Vector3i(newCenter.getX(), newCenter.getY(), newCenter.getZ());
                //final double teleportHeight = claim.getOwnerPlayerData() == null ? 65.0D : (claim.getOwnerPlayerData().getMinClaimLevel() > 65.0D ? claim.getOwnerPlayerData().getMinClaimLevel() : 65);
                //Location<World> southWest = claim.lesserBoundaryCorner.setPosition(new Vector3d(claim.lesserBoundaryCorner.getPosition().getX(), teleportHeight, claim.greaterBoundaryCorner.getPosition().getZ()));
                Component claimName = claim.getData().getName().orElse(TextComponent.empty());
                Component teleportName = claim.getData().getName().orElse(claim.getFriendlyNameType());
                Component ownerLine = TextComponent.builder()
                        .append(MessageCache.getInstance().LABEL_OWNER.color(TextColor.YELLOW))
                        .append(" : ", TextColor.WHITE)
                        .append(claim.getOwnerName().color(TextColor.GOLD))
                        .append("\n").build();
                Component claimTypeInfo = TextComponent.builder("Type").color(TextColor.YELLOW)
                        .append(" : ", TextColor.WHITE)
                        .append(claim.getFriendlyNameType())
                        .append(" ")
                        .append(claim.isCuboid() ? "3D " : "2D ", TextColor.GRAY)
                        .append(" (")
                        .append(MessageCache.getInstance().LABEL_AREA)
                        .append(": ", TextColor.WHITE)
                        .append(String.valueOf(claim.getClaimBlocks()), TextColor.GRAY)
                        .append(" blocks)\n", TextColor.WHITE).build();
                Component clickInfo = MessageCache.getInstance().CLAIMLIST_UI_CLICK_INFO;
                Component basicInfo = TextComponent.builder("")
                        .append(ownerLine)
                        .append(claimTypeInfo)
                        .append(clickInfo).build();

                Component claimInfoCommandClick = TextComponent.builder("")
                        .append(claim.getFriendlyNameType())
                        .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(CommandHelper.createCommandConsumer(src, "claiminfo", claim.getUniqueId().toString(), createReturnClaimListConsumer(src, returnCommand)))))
                        .hoverEvent(HoverEvent.showText(basicInfo)).build();

                Component claimSpawn = null;
                if (player !=null && PermissionUtil.getInstance().canPlayerTeleport(player, claim)) {
                    final Vector3i spawnPos = claim.getData().getSpawnPos().orElse(null);
                    if (spawnPos != null) {
                        Location spawnLoc = new Location(claim.getWorld(), spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
                        claimSpawn = TextComponent.builder("")
                                .append("[")
                                .append("TP", TextColor.LIGHT_PURPLE)
                                .append("]")
                                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(CommandHelper.createTeleportConsumer(src, spawnLoc, claim, true))))
                                .hoverEvent(HoverEvent.showText(MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.CLAIMLIST_UI_CLICK_TELEPORT_TARGET,
                                        ImmutableMap.of(
                                                "name", teleportName,
                                                "target", "'s spawn @ " + spawnPos.toString(),
                                                "world", claim.getWorld().getName()))))
                                .build();
                    } else {
                        claimSpawn = TextComponent.builder("")
                                .append("[")
                                .append("TP", TextColor.LIGHT_PURPLE)
                                .append("]")
                                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(CommandHelper.createTeleportConsumer(src, VecHelper.toLocation(claim.getWorld(), southWest), claim))))
                                .hoverEvent(HoverEvent.showText(MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.CLAIMLIST_UI_CLICK_TELEPORT_TARGET,
                                        ImmutableMap.of(
                                            "name", teleportName,
                                            "target", southWest.toString(),
                                            "world", claim.getWorld().getName()))))
                                .build();
                    }
                }

                List<Component> childrenTextList = new ArrayList<>();
                if (!listChildren) {
                    childrenTextList = generateClaimTextList(new ArrayList<Component>(), claim.getChildren(true), worldName, user, src, returnCommand, true);
                }
                Component buyClaim = TextComponent.empty();
                if (player != null && claim.getEconomyData().isForSale() && claim.getEconomyData().getSalePrice() > -1) {
                    Component buyInfo = TextComponent.builder()
                            .append(MessageCache.getInstance().LABEL_PRICE.color(TextColor.AQUA))
                            .append(" : ", TextColor.WHITE)
                            .append(String.valueOf(claim.getEconomyData().getSalePrice()), TextColor.GOLD)
                            .append("\n")
                            .append(MessageCache.getInstance().CLAIMLIST_UI_CLICK_PURCHASE).build();
                    buyClaim = TextComponent.builder()
                        .append(claim.getEconomyData().isForSale() ? TextComponent.builder(" [").append(MessageCache.getInstance().LABEL_BUY.color(TextColor.GREEN)).append("]", TextColor.WHITE).build() : TextComponent.empty())
                        .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(buyClaimConsumerConfirmation(src, claim))))
                        .hoverEvent(HoverEvent.showText(player.getUniqueId().equals(claim.getOwnerUniqueId()) ? MessageCache.getInstance().CLAIM_OWNER_ALREADY : buyInfo)).build();
                }
                if (!childrenTextList.isEmpty()) {
                    Component children = TextComponent.builder("[")
                            .append(MessageCache.getInstance().LABEL_CHILDREN.color(TextColor.AQUA))
                            .append("]", TextColor.WHITE)
                            .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(showChildrenList(childrenTextList, src, returnCommand, claim))))
                            .hoverEvent(HoverEvent.showText(MessageCache.getInstance().CLAIMLIST_UI_CLICK_VIEW_CHILDREN)).build();
                    claimsTextList.add(TextComponent.builder("")
                            .append(claimSpawn != null ? claimSpawn.append(TextComponent.of(" ")) : TextComponent.of(""))
                            .append(claimInfoCommandClick)
                            .append(" : ", TextColor.WHITE)
                            .append(claim.getOwnerName().color(TextColor.GOLD))
                            .append(" ")
                            .append(claimName == TextComponent.empty() ? TextComponent.of("") : claimName)
                            .append(" ")
                            .append(children)
                            .append(" ")
                            .append(buyClaim)
                            .build());
                } else {
                   claimsTextList.add(TextComponent.builder("")
                           .append(claimSpawn != null ? claimSpawn.append(TextComponent.of(" ")) : TextComponent.of(""))
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
                claimsTextList.add(MessageCache.getInstance().CLAIMLIST_UI_NO_CLAIMS_FOUND.color(TextColor.RED));
            }
        }
        return claimsTextList;
    }

    public static Consumer<CommandSource> buyClaimConsumerConfirmation(CommandSource src, Claim claim) {
        return confirm -> {
            final Player player = (Player) src;
            if (player.getUniqueId().equals(claim.getOwnerUniqueId())) {
                return;
            }
            Account playerAccount = GriefDefenderPlugin.getInstance().economyService.get().getOrCreateAccount(player.getUniqueId()).orElse(null);
            if (playerAccount == null) {
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_PLAYER_NOT_FOUND, ImmutableMap.of(
                        "player", player.getName()));
                GriefDefenderPlugin.sendMessage(player, message);
                return;
            }

            final double balance = playerAccount.getBalance(GriefDefenderPlugin.getInstance().economyService.get().getDefaultCurrency()).doubleValue();
            if (balance < claim.getEconomyData().getSalePrice()) {
                Map<String, Object> params = ImmutableMap.of(
                        "amount", claim.getEconomyData().getSalePrice(),
                        "balance", balance,
                        "amount_required", claim.getEconomyData().getSalePrice() -  balance);
                GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_BUY_NOT_ENOUGH_FUNDS, params));
                return;
            }
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_BUY_CONFIRMATION,
                    ImmutableMap.of("amount", "$" + claim.getEconomyData().getSalePrice()));
            final Component buyConfirmationText = TextComponent.builder()
                    .append(message)
                    .append(TextComponent.builder()
                        .append("\n[")
                        .append(MessageCache.getInstance().LABEL_CONFIRM.color(TextColor.GREEN))
                        .append("]\n")
                        .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createBuyConsumerConfirmed(src, claim)))).build())
                    .build();
            GriefDefenderPlugin.sendMessage(player, buyConfirmationText);
        };
    }

    private static Consumer<CommandSource> createBuyConsumerConfirmed(CommandSource src, Claim claim) {
        return confirm -> {
            final Player player = (Player) src;
            final GDPermissionUser owner = PermissionHolderCache.getInstance().getOrCreateUser(claim.getOwnerUniqueId());
            final Account ownerAccount = GriefDefenderPlugin.getInstance().economyService.get().getOrCreateAccount(claim.getOwnerUniqueId()).orElse(null);
            if (ownerAccount == null) {
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_PLAYER_NOT_FOUND, ImmutableMap.of(
                        "player", player.getName()));
                GriefDefenderPlugin.sendMessage(player, message);
                return;
            }

            final ClaimResult result = claim.transferOwner(player.getUniqueId());
            if (!result.successful()) {
                final Component defaultMessage = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.ECONOMY_CLAIM_BUY_TRANSFER_CANCELLED,
                        ImmutableMap.of(
                            "owner", owner.getName(),
                            "player", player.getName(),
                            "result", result.getMessage().orElse(TextComponent.of(result.getResultType().toString()))));
                TextAdapter.sendComponent(src, result.getMessage().orElse(defaultMessage));
                return;
            }

            try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
                final Currency defaultCurrency = GriefDefenderPlugin.getInstance().economyService.get().getDefaultCurrency();
                final double salePrice = claim.getEconomyData().getSalePrice();
                Sponge.getCauseStackManager().pushCause(src);
                final TransactionResult ownerResult = ownerAccount.deposit(defaultCurrency, BigDecimal.valueOf(salePrice), Sponge.getCauseStackManager().getCurrentCause());
                Account playerAccount = GriefDefenderPlugin.getInstance().economyService.get().getOrCreateAccount(player.getUniqueId()).orElse(null);
                final TransactionResult
                    transactionResult =
                    playerAccount.withdraw(defaultCurrency, BigDecimal.valueOf(salePrice), Sponge.getCauseStackManager().getCurrentCause());
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_BUY_CONFIRMED,
                    ImmutableMap.of(
                        "amount", String.valueOf("$" +salePrice)));
                final Component saleMessage = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_SOLD,
                    ImmutableMap.of(
                        "amount", String.valueOf("$" +salePrice),
                        "balance",String.valueOf("$" + playerAccount.getBalance(defaultCurrency))));
                if (owner.getOnlinePlayer() != null) {
                    TextAdapter.sendComponent(owner.getOnlinePlayer(), saleMessage);
                }
                claim.getEconomyData().setForSale(false);
                claim.getEconomyData().setSalePrice(0);
                claim.getData().save();
                GriefDefenderPlugin.sendMessage(src, message);
            }
        };
    }

    public static Consumer<CommandSource> showChildrenList(List<Component> childrenTextList, CommandSource src, Consumer<CommandSource> returnCommand, GDClaim parent) {
        return consumer -> {
            Component claimListReturnCommand = TextComponent.builder("")
                    .append("\n[")
                    .append(MessageCache.getInstance().CLAIMLIST_UI_RETURN_CLAIMSLIST.color(TextColor.AQUA))
                    .append("]\n", TextColor.WHITE)
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(returnCommand))).build();
    
            List<Component> textList = new ArrayList<>();
            textList.add(claimListReturnCommand);
            textList.addAll(childrenTextList);
            PaginationList.Builder builder = PaginationList.builder()
                    .title(parent.getName().orElse(parent.getFriendlyNameType())
                            .append(TextComponent.of(" ").append(MessageCache.getInstance().CLAIMLIST_UI_TITLE_CHILD_CLAIMS))).padding(TextComponent.builder(" ").decoration(TextDecoration.STRIKETHROUGH, true).build()).contents(textList);
            builder.sendTo(src);
        };
    }

    public static Consumer<CommandSource> createReturnClaimListConsumer(CommandSource src, Consumer<CommandSource> returnCommand) {
        return consumer -> {
            Component claimListReturnCommand = TextComponent.builder("")
                    .append("\n[")
                    .append(MessageCache.getInstance().CLAIMLIST_UI_RETURN_CLAIMSLIST.color(TextColor.AQUA))
                    .append("]\n", TextColor.WHITE)
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(returnCommand))).build();
            TextAdapter.sendComponent(src, claimListReturnCommand);
        };
    }

    public static Consumer<CommandSource> createReturnClaimListConsumer(CommandSource src, String arguments) {
        return consumer -> {
            Component claimListReturnCommand = TextComponent.builder("")
                    .append("\n[")
                    .append(MessageCache.getInstance().CLAIMLIST_UI_RETURN_CLAIMSLIST.color(TextColor.AQUA))
                    .append("]\n", TextColor.WHITE)
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(CommandHelper.createCommandConsumer(src, "/claimslist", arguments)))).build();
            TextAdapter.sendComponent(src, claimListReturnCommand);
        };
    }

    public static Consumer<CommandSource> createFlagConsumer(CommandSource src, GDPermissionHolder subject, String subjectName, Set<Context> contexts, GDClaim claim, Flag flag, Tristate flagValue, String source) {
        return consumer -> {
            String target = flag.getName().toLowerCase();
            if (target.isEmpty()) {
                target = "any";
            }
            Tristate newValue = Tristate.UNDEFINED;
            if (flagValue == Tristate.TRUE) {
                newValue = Tristate.FALSE;
            } else if (flagValue == Tristate.UNDEFINED) {
                newValue = Tristate.TRUE;
            }

            CommandHelper.applyFlagPermission(src, subject, claim, flag, target, newValue, null, MenuType.GROUP);
        };
    }

    public static Component getClickableText(CommandSource src, GDClaim claim, Subject subject, Set<Context> contexts, Flag flag, Tristate flagValue, MenuType type) {
        TextComponent.Builder textBuilder = TextComponent.builder(flagValue.toString().toLowerCase())
                .hoverEvent(HoverEvent.showText(TextComponent.builder("")
                        .append(MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.CLAIMLIST_UI_CLICK_TOGGLE_VALUE,
                                ImmutableMap.of("type", type.name().toLowerCase())))
                        .append("\n")
                        .append(UIHelper.getPermissionMenuTypeHoverText(type)).build()))
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createFlagConsumer(src, claim, subject, contexts, flag, flagValue, type))));
        return textBuilder.build();
    }

    public static Component getClickableText(CommandSource src, GDPermissionHolder subject, String subjectName, Set<Context> contexts, GDClaim claim, Flag flag, Tristate flagValue, String source, MenuType type) {
        Component onClickText = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.CLAIMLIST_UI_CLICK_TOGGLE_VALUE,
                ImmutableMap.of("type", "flag"));
        boolean hasPermission = true;
        if (type == MenuType.INHERIT) {
            onClickText = MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.FLAG_UI_INHERIT_PARENT,
                    ImmutableMap.of("name", claim.getFriendlyNameType()));
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
                        .append(UIHelper.getPermissionMenuTypeHoverText(type)).build()));
        if (hasPermission) {
            textBuilder.clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createFlagConsumer(src, subject, subjectName, contexts, claim, flag, flagValue, source))));
        }
        return textBuilder.build();
    }

    public static String handleCommandFlag(CommandSource src, String target) {
        String pluginId = "minecraft";
        String args = "";
        String command = "";
        int argsIndex = target.indexOf("[");
        if (argsIndex != -1) {
            if (argsIndex == 0) {
                // invalid
                TextAdapter.sendComponent(src, MessageCache.getInstance().COMMAND_INVALID);
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
            if (!pluginId.equals("minecraft")) {
                PluginContainer pluginContainer = Sponge.getPluginManager().getPlugin(pluginId).orElse(null);
                if (pluginContainer == null) {
                    TextAdapter.sendComponent(src, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.PLUGIN_NOT_FOUND, 
                            ImmutableMap.of("id", pluginId)));
                    return null;
                }
            }
            args = target.substring(argsIndex, target.length());
            Pattern p = Pattern.compile("\\[([^\\]]+)\\]");
            Matcher m = p.matcher(args);
            if (!m.find()) {
                // invalid
                TextAdapter.sendComponent(src, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.COMMAND_EXECUTE_FAILED,
                        ImmutableMap.of(
                                "command", command,
                                "args", args)));
                return null;
            }
            args = m.group(1);
            target = pluginId + ":" + command + "." + args.replace(":", ".");
        } else {
            String[] parts = target.split(":");
            if (parts.length > 1) {
                pluginId = parts[0];
                command = parts[1];
            } else {
                command = target;
            }
            target = pluginId + ":" + command;
        }

        // validate command
        if (!validateCommandMapping(src, command, pluginId)) {
            return null;
        }

        return target;
    }

    private static boolean validateCommandMapping(CommandSource src, String command, String pluginId) {
        if (command.equals("any")) {
            return true;
        }

        CommandMapping commandMapping = Sponge.getCommandManager().get(command).orElse(null);
        if (commandMapping == null) {
            TextAdapter.sendComponent(src, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.PLUGIN_COMMAND_NOT_FOUND,
                    ImmutableMap.of(
                        "command", command,
                        "id", pluginId)));
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

    public static Consumer<CommandSource> createTeleportConsumer(CommandSource src, Location<World> location, Claim claim) {
        return createTeleportConsumer(src, location, claim, false);
    }

    public static Consumer<CommandSource> createTeleportConsumer(CommandSource src, Location<World> location, Claim claim, boolean isClaimSpawn) {
        return teleport -> {
            if (!(src instanceof Player)) {
                // ignore
                return;
            }

            final Player player = (Player) src;
            final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
            final int teleportDelay = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), player, Options.PLAYER_TELEPORT_DELAY, claim);
            if (isClaimSpawn) {
                if (teleportDelay > 0) {
                    playerData.teleportDelay = teleportDelay + 1;
                    playerData.teleportSourceLocation = player.getLocation();
                    playerData.teleportLocation = location;
                    return;
                }

                player.setLocation(location);
                return;
            }

            Location<World> safeLocation = Sponge.getGame().getTeleportHelper().getSafeLocation(location, 64, 16).orElse(null);
            if (safeLocation == null) {
                if (teleportDelay > 0) {
                    playerData.teleportDelay = teleportDelay + 1;
                    playerData.teleportLocation = location;
                    return;
                }
                TextAdapter.sendComponent(player, TextComponent.builder("")
                        .append("Location is not safe. ", TextColor.RED)
                        .append(TextComponent.builder("")
                                .append("Are you sure you want to teleport here?", TextColor.GREEN)
                                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createForceTeleportConsumer(player, location)))).decoration(TextDecoration.UNDERLINED, true).build()).build());
            } else {
                if (teleportDelay > 0) {
                    playerData.teleportDelay = teleportDelay + 1;
                    playerData.teleportLocation = safeLocation;
                    return;
                }
                player.setLocation(safeLocation);
            }
        };
    }

    public static Consumer<CommandSource> createForceTeleportConsumer(Player player, Location<World> location) {
        return teleport -> {
            player.setLocation(location);
        };
    }

    public static void handleBankTransaction(CommandSource src, String[] args, GDClaim claim) {
        final EconomyService economyService = GriefDefenderPlugin.getInstance().economyService.orElse(null);
        if (economyService == null) {
            GriefDefenderPlugin.sendMessage(src, MessageCache.getInstance().ECONOMY_NOT_INSTALLED);
            return;
        }

        if (claim.isSubdivision() || claim.isAdminClaim()) {
            return;
        }

        Account bankAccount = null;//claim.getEconomyAccount().orElse(null);
        if (bankAccount == null) {
            GriefDefenderPlugin.sendMessage(src, MessageCache.getInstance().ECONOMY_VIRTUAL_NOT_SUPPORTED);
            return;
        }

        final String command = args[0];
        double amount = 0;
        try {
            amount = Double.valueOf(args[1]);
        } catch (NumberFormatException e) {
            
        }

        final UUID playerSource = ((Player) src).getUniqueId();
        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getPlayerData(claim.getWorld(), claim.getOwnerUniqueId());
        if (playerData.canIgnoreClaim(claim) || claim.getOwnerUniqueId().equals(playerSource) || claim.getUserTrusts(TrustTypes.MANAGER).contains(playerData.playerID)) {
            final UniqueAccount playerAccount = economyService.getOrCreateAccount(playerData.playerID).get();
            try (final CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
                Sponge.getCauseStackManager().pushCause(src);
                Sponge.getCauseStackManager().addContext(GriefDefenderPlugin.PLUGIN_CONTEXT, GriefDefenderPlugin.getInstance());
                if (command.equalsIgnoreCase("withdraw")) {
                    TransactionResult
                        result =
                        bankAccount.withdraw(economyService.getDefaultCurrency(), BigDecimal.valueOf(amount), Sponge.getCauseStackManager().getCurrentCause());
                    if (result.getResult() == ResultType.SUCCESS) {
                        final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.BANK_WITHDRAW,
                            ImmutableMap.of(
                                "amount", amount));
                        GriefDefenderPlugin.sendMessage(src, message);
                        playerAccount.deposit(economyService.getDefaultCurrency(), BigDecimal.valueOf(amount), Sponge.getCauseStackManager().getCurrentCause());
                        claim.getData().getEconomyData().addBankTransaction(
                            new GDBankTransaction(BankTransactionType.WITHDRAW_SUCCESS, playerData.playerID, Instant.now(), amount));
                    } else {
                        final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.BANK_WITHDRAW_NO_FUNDS,
                            ImmutableMap.of(
                                "balance", bankAccount.getBalance(economyService.getDefaultCurrency()),
                                "amount", amount));
                        GriefDefenderPlugin.sendMessage(src, message);
                        claim.getData().getEconomyData()
                            .addBankTransaction(new GDBankTransaction(BankTransactionType.WITHDRAW_FAIL, playerData.playerID, Instant.now(), amount));
                        return;
                    }
                } else if (command.equalsIgnoreCase("deposit")) {
                    TransactionResult
                        result =
                        playerAccount.withdraw(economyService.getDefaultCurrency(), BigDecimal.valueOf(amount), Sponge.getCauseStackManager().getCurrentCause());
                    if (result.getResult() == ResultType.SUCCESS) {
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
                        bankAccount.deposit(economyService.getDefaultCurrency(), BigDecimal.valueOf(depositAmount), Sponge.getCauseStackManager().getCurrentCause());
                        claim.getData().getEconomyData().addBankTransaction(
                            new GDBankTransaction(BankTransactionType.DEPOSIT_SUCCESS, playerData.playerID, Instant.now(), depositAmount));
                    } else {
                        GriefDefenderPlugin.sendMessage(src, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.BANK_WITHDRAW_NO_FUNDS));
                        claim.getData().getEconomyData()
                            .addBankTransaction(new GDBankTransaction(BankTransactionType.DEPOSIT_FAIL, playerData.playerID, Instant.now(), amount));
                        return;
                    }
                }
            }
        } else {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.BANK_NO_PERMISSION,
                    ImmutableMap.of(
                            "player", claim.getOwnerName()));
            GriefDefenderPlugin.sendMessage(src, message);
        }
    }

    public static void displayClaimBankInfo(CommandSource src, GDClaim claim) {
        displayClaimBankInfo(src, claim, false, false);
    }

    public static void displayClaimBankInfo(CommandSource src, GDClaim claim, boolean checkTown, boolean returnToClaimInfo) {
        final EconomyService economyService = GriefDefenderPlugin.getInstance().economyService.orElse(null);
        if (economyService == null) {
            GriefDefenderPlugin.sendMessage(src, MessageCache.getInstance().ECONOMY_NOT_INSTALLED);
            return;
        }

        if (checkTown && !claim.isInTown()) {
            GriefDefenderPlugin.sendMessage(src, MessageCache.getInstance().TOWN_NOT_IN);
            return;
        }

        if (!checkTown && (claim.isSubdivision() || claim.isAdminClaim())) {
            return;
        }

        final GDClaim town = claim.getTownClaim();
        Account bankAccount = checkTown ? town.getEconomyAccount() : claim.getEconomyAccount();
        if (bankAccount == null) {
            GriefDefenderPlugin.sendMessage(src, MessageCache.getInstance().ECONOMY_VIRTUAL_NOT_SUPPORTED);
            return;
        }

        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getPlayerData(claim.getWorld(), claim.getOwnerUniqueId());
        final double claimBalance = bankAccount.getBalance(economyService.getDefaultCurrency()).doubleValue();
        double taxOwed = -1;
        final double playerTaxRate = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Double.class), (Player) src, Options.TAX_RATE, claim);
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

        final GriefDefenderConfig<?> activeConfig = GriefDefenderPlugin.getActiveConfig(claim.getWorld().getProperties());
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
                .append(MessageCache.getInstance().BANK_TITLE_TRANSACTIONS.color(TextColor.AQUA).decoration(TextDecoration.ITALIC, true))
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createBankTransactionsConsumer(src, claim, checkTown, returnToClaimInfo))))
                .hoverEvent(HoverEvent.showText(MessageCache.getInstance().BANK_CLICK_VIEW_TRANSACTIONS))
                .build();
        List<Component> textList = new ArrayList<>();
        if (returnToClaimInfo) {
            textList.add(TextComponent.builder("")
                    .append("\n[")
                    .append(MessageCache.getInstance().CLAIMINFO_UI_RETURN_CLAIMINFO.color(TextColor.AQUA))
                    .append("]\n")
                    .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(CommandHelper.createCommandConsumer(src, "claiminfo", "")))).build());
        }
        textList.add(message);
        textList.add(transactions);
        PaginationList.Builder builder = PaginationList.builder()
                .title(TextComponent.of("Bank Info", TextColor.AQUA)).padding(TextComponent.builder(" ").decoration(TextDecoration.STRIKETHROUGH, true).build()).contents(textList);
        builder.sendTo(src);
    }

    public static Consumer<CommandSource> createBankTransactionsConsumer(CommandSource src, GDClaim claim, boolean checkTown, boolean returnToClaimInfo) {
        return settings -> {
            List<String> bankTransactions = new ArrayList<>(claim.getData().getEconomyData().getBankTransactionLog());
            Collections.reverse(bankTransactions);
            List<Component> textList = new ArrayList<>();
            textList.add(TextComponent.builder("")
                    .append("\n[")
                    .append(MessageCache.getInstance().CLAIMINFO_UI_RETURN_BANKINFO.color(TextColor.AQUA))
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
                    .append(MessageCache.getInstance().CLAIMINFO_UI_RETURN_BANKINFO.color(TextColor.AQUA))
                    .append("]\n")
                    .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(CommandHelper.createCommandConsumer(src, "claimbank", "")))).build());
            PaginationList.Builder builder = PaginationList.builder()
                    .title(MessageCache.getInstance().BANK_TITLE_TRANSACTIONS.color(TextColor.AQUA)).padding(TextComponent.builder(" ").decoration(TextDecoration.STRIKETHROUGH, true).build()).contents(textList);
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

    public static Component getBaseOptionOverlayText(String option) {
        String baseFlag = option.replace(GDPermissions.OPTION_BASE + ".", "");
        int endIndex = baseFlag.indexOf(".");
        if (endIndex != -1) {
            baseFlag = baseFlag.substring(0, endIndex);
        }

        final Option flag = GriefDefender.getRegistry().getType(Option.class, baseFlag).orElse(null);
        if (flag == null) {
            return MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.OPTION_NOT_FOUND, ImmutableMap.of(
                    "option", baseFlag));
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