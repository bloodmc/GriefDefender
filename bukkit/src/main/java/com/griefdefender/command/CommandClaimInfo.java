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
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;

import com.flowpowered.math.vector.Vector3i;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimResult;
import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.api.claim.ClaimTypes;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.claim.GDClaimManager;
import com.griefdefender.internal.pagination.PaginationList;
import com.griefdefender.internal.util.VecHelper;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.text.action.GDCallbackHolder;
import com.griefdefender.util.PlayerUtil;

import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_CLAIM_INFO_BASE)
public class CommandClaimInfo extends BaseCommand {

    private static final Component NONE = TextComponent.of("none", TextColor.GRAY);
    private static final String ADMIN_SETTINGS = "Admin Settings";
    private static final String CLAIM_EXPIRATION = "ClaimExpiration";
    private static final String DENY_MESSAGES = "DenyMessages";
    private static final String FLAG_OVERRIDES = "FlagOverrides";
    private static final String INHERIT_PARENT = "InheritParent";
    private static final String PVP_OVERRIDE = "PvPOverride";
    private static final String RESIZABLE = "Resizable";
    private static final String REQUIRES_CLAIM_BLOCKS = "RequiresClaimBlocks";
    private static final String SIZE_RESTRICTIONS = "SizeRestrictions";
    private static final String FOR_SALE = "ForSale";
    private boolean useTownInfo = false;

    public CommandClaimInfo() {
        
    }

    public CommandClaimInfo(boolean useTownInfo) {
        this.useTownInfo = useTownInfo;
    }

    @CommandAlias("claiminfo")
    @Syntax("[claim_uuid]")
    @Subcommand("claim info")
    public void execute(CommandSender src, String[] args) {
        String claimIdentifier = null;
        if (args.length > 0) {
            claimIdentifier = args[0];
        }

        Player player = null;
        if (src instanceof Player) {
            player = (Player) src;
            if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUID())) {
                GriefDefenderPlugin.sendMessage(src, GriefDefenderPlugin.getInstance().messageData.claimDisabledWorld.toText());
                return;
            }
        }

        if (player == null && claimIdentifier == null) {
            TextAdapter.sendComponent(src, TextComponent.of("No valid player or claim UUID found.", TextColor.RED));
            return;
        }

        boolean isAdmin = src.hasPermission(GDPermissions.COMMAND_ADMIN_CLAIMS);
        final GDPlayerData playerData = player != null ? GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId()) : null;
        Claim claim = null;
        if (claimIdentifier == null) {
            if (player != null) {
                claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAt(player.getLocation());
            } else {
                TextAdapter.sendComponent(src, TextComponent.of("Claim UUID is required if executing from non-player source.", TextColor.RED));
                return;
            }
        } else {
            for (World world : Bukkit.getServer().getWorlds()) {
                if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(world.getUID())) {
                    continue;
                }

                final GDClaimManager claimManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(world.getUID());
                UUID uuid = null;
                try {
                    uuid = UUID.fromString(claimIdentifier);
                    claim = claimManager.getClaimByUUID(uuid).orElse(null);
                    if (claim != null) {
                        break;
                    }
                } catch (IllegalArgumentException e) {
                    
                }
                if (uuid == null) {
                    final List<Claim> claimList = claimManager.getClaimsByName(claimIdentifier);
                    if (!claimList.isEmpty()) {
                        claim = claimList.get(0);
                    }
                }
            }
        }

        if (claim == null) {
            GriefDefenderPlugin.sendMessage(src, GriefDefenderPlugin.getInstance().messageData.claimNotFound.toText());
            return;
        }

        if (this.useTownInfo) {
            if (!claim.isInTown()) {
                GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.townNotIn.toText());
                return;
            }
            claim = claim.getTown().get();
        }

        final GDClaim gpClaim = (GDClaim) claim;
        UUID ownerUniqueId = claim.getOwnerUniqueId();

        if (!isAdmin) {
            isAdmin = playerData.canIgnoreClaim(gpClaim);
        }
        // if not owner of claim, validate perms
        if (!isAdmin && !player.getUniqueId().equals(claim.getOwnerUniqueId())) {
            if (!gpClaim.getInternalClaimData().getContainers().contains(player.getUniqueId()) 
                    && !gpClaim.getInternalClaimData().getBuilders().contains(player.getUniqueId())
                    && !gpClaim.getInternalClaimData().getManagers().contains(player.getUniqueId())
                    && !player.hasPermission(GDPermissions.COMMAND_CLAIM_INFO_OTHERS)) {
                TextAdapter.sendComponent(player, GriefDefenderPlugin.getInstance().messageData.claimNotYours.toText()); 
                return;
            }
        }

        final Component allowEdit = gpClaim.allowEdit(player);

        List<Component> textList = new ArrayList<>();
        Component name = claim.getName().orElse(null);
        Component greeting = claim.getData().getGreeting().orElse(null);
        Component farewell = claim.getData().getFarewell().orElse(null);
        String accessors = "";
        String builders = "";
        String containers = "";
        String managers = "";
        String accessorGroups = "";
        String builderGroups = "";
        String containerGroups = "";
        String managerGroups = "";

        final int minClaimLevel = gpClaim.getOwnerMinClaimLevel();
        double claimY = gpClaim.getOwnerPlayerData() == null ? 65.0D : (minClaimLevel > 65.0D ? minClaimLevel : 65);
        if (gpClaim.isCuboid()) {
            claimY = gpClaim.lesserBoundaryCorner.getY();
        }
        Location southWest = new Location(gpClaim.getWorld(), gpClaim.lesserBoundaryCorner.getX(), claimY, gpClaim.greaterBoundaryCorner.getZ());
        Location northWest = new Location(gpClaim.getWorld(), gpClaim.lesserBoundaryCorner.getX(), claimY, gpClaim.lesserBoundaryCorner.getZ());
        Location southEast = new Location(gpClaim.getWorld(), gpClaim.greaterBoundaryCorner.getX(), claimY, gpClaim.greaterBoundaryCorner.getZ());
        Location northEast = new Location(gpClaim.getWorld(), gpClaim.greaterBoundaryCorner.getX(), claimY, gpClaim.lesserBoundaryCorner.getZ());
        // String southWestCorner = 
        Date created = null;
        Date lastActive = null;
        try {
            Instant instant = claim.getData().getDateCreated();
            created = Date.from(instant);
        } catch(DateTimeParseException ex) {
            // ignore
        }

        try {
            Instant instant = claim.getData().getDateLastActive();
            lastActive = Date.from(instant);
        } catch(DateTimeParseException ex) {
            // ignore
        }

        final int sizeX = Math.abs(claim.getGreaterBoundaryCorner().getX() - claim.getLesserBoundaryCorner().getX()) + 1;
        final int sizeY = Math.abs(claim.getGreaterBoundaryCorner().getY() - claim.getLesserBoundaryCorner().getY()) + 1;
        final int sizeZ = Math.abs(claim.getGreaterBoundaryCorner().getZ() - claim.getLesserBoundaryCorner().getZ()) + 1;
        Component claimSize = TextComponent.empty();
        if (claim.isCuboid()) {
            claimSize = TextComponent.builder(" ")
                    .append("Area: ", TextColor.YELLOW)
                    .append(sizeX + "x" + sizeY + "x" + sizeZ, TextColor.GRAY).build();
        } else {
            claimSize = TextComponent.builder(" ")
                    .append("Area: ", TextColor.YELLOW)
                    .append(sizeX + "x" + sizeZ, TextColor.GRAY).build();
        }
        final Component claimCost = TextComponent.builder("  ")
                .append("Blocks: ", TextColor.YELLOW)
                .append(String.valueOf(claim.getClaimBlocks()), TextColor.GRAY).build();
        if (claim.isWilderness() && name == null) {
            name = TextComponent.of("Wilderness", TextColor.GREEN);
        }
        Component claimName = TextComponent.builder("")
                .append("Name", TextColor.YELLOW)
                .append(" : ")
                .append(name == null ? NONE : name).build();
        if (!claim.isWilderness() && !claim.isAdminClaim()) {
            claimName = TextComponent.builder("")
                    .append(claimName)
                    .append(claimSize)
                    .append(claimCost).build();
        }
        // users
        final List<UUID> accessorList = gpClaim.getUserTrustList(TrustTypes.ACCESSOR, true);
        final List<UUID> builderList = gpClaim.getUserTrustList(TrustTypes.BUILDER, true);
        final List<UUID> containerList = gpClaim.getUserTrustList(TrustTypes.CONTAINER, true);
        final List<UUID> managerList = gpClaim.getUserTrustList(TrustTypes.MANAGER, true);
        for (UUID uuid : accessorList) {
            final String userName = PlayerUtil.getInstance().getUserName(uuid);
            if (userName != null) {
                accessors += PlayerUtil.getInstance().getUserName(uuid) + " ";
            }
        }
        for (UUID uuid : builderList) {
            final String userName = PlayerUtil.getInstance().getUserName(uuid);
            if (userName != null) {
                builders += PlayerUtil.getInstance().getUserName(uuid) + " ";
            }
        }
        for (UUID uuid : containerList) {
            final String userName = PlayerUtil.getInstance().getUserName(uuid);
            if (userName != null) {
                containers += PlayerUtil.getInstance().getUserName(uuid) + " ";
            }
        }
        for (UUID uuid : managerList) {
            final String userName = PlayerUtil.getInstance().getUserName(uuid);
            if (userName != null) {
                managers += PlayerUtil.getInstance().getUserName(uuid) + " ";
            }
        }

        // groups
        for (String group : gpClaim.getInternalClaimData().getAccessorGroups()) {
            accessorGroups += group + " ";
        }
        for (String group : gpClaim.getInternalClaimData().getBuilderGroups()) {
            builderGroups += group + " ";
        }
        for (String group : gpClaim.getInternalClaimData().getContainerGroups()) {
            containerGroups += group + " ";
        }
        for (String group : gpClaim.getInternalClaimData().getManagerGroups()) {
            managerGroups += group + " ";
        }

        /*if (gpClaim.isInTown()) {
            Text returnToClaimInfo = Text.builder().append(Text.of(
                    TextColors.WHITE, "\n[", TextColors.AQUA, "Return to standard settings", TextColors.WHITE, "]\n"))
                .onClick(TextActions.executeCallback(CommandHelper.createCommandConsumer(src, "claiminfo", ""))).build();
            Text townName = Text.of(TextColors.YELLOW, "Name", TextColors.WHITE, " : ", TextColors.RESET,
                    gpClaim.getTownClaim().getTownData().getName().orElse(NONE));
            Text townTag = Text.of(TextColors.YELLOW, "Tag", TextColors.WHITE, " : ", TextColors.RESET,
                    gpClaim.getTownClaim().getTownData().getTownTag().orElse(NONE));
            townTextList.add(returnToClaimInfo);
            townTextList.add(townName);
            townTextList.add(townTag);
            Text townSettings = Text.builder()
                    .append(Text.of(TextStyles.ITALIC, TextColors.GREEN, TOWN_SETTINGS))
                    .onClick(TextActions.executeCallback(createSettingsConsumer(src, claim, townTextList, ClaimTypes.TOWN)))
                    .onHover(TextActions.showText(Text.of("Click here to view town settings")))
                    .build();
            textList.add(townSettings);
        }*/

        if (isAdmin) {
            Component adminSettings = TextComponent.builder("")
                    .append(TextComponent.of(ADMIN_SETTINGS, TextColor.RED).decoration(TextDecoration.ITALIC, true))
                    .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createSettingsConsumer(src, claim, generateAdminSettings(src, gpClaim), ClaimTypes.ADMIN))))
                    .hoverEvent(HoverEvent.showText(TextComponent.of("Click here to view admin settings")))
                    .build();
            textList.add(adminSettings);
        }

        Component bankInfo = null;
        Component forSaleText = null;
        if (GriefDefenderPlugin.getInstance().getVaultProvider() != null) {
             if (GriefDefenderPlugin.getActiveConfig(gpClaim.getWorld().getUID()).getConfig().claim.bankTaxSystem) {
                 bankInfo = TextComponent.builder("")
                         .append("Bank Info", TextColor.GOLD, TextDecoration.ITALIC)
                         .hoverEvent(HoverEvent.showText(TextComponent.of("Click to check bank information")))
                         .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(Consumer -> { CommandHelper.displayClaimBankInfo(src, gpClaim, gpClaim.isTown() ? true : false, true); })))
                         .build();
             }
             forSaleText = TextComponent.builder("")
                     .append("ForSale", TextColor.YELLOW)
                     .append(" : ")
                     .append(getClickableInfoText(src, claim, FOR_SALE, claim.getEconomyData().isForSale() ? TextComponent.of("YES", TextColor.GREEN) : TextComponent.of("NO", TextColor.GRAY))).build();
             if (claim.getEconomyData().isForSale()) {
                 forSaleText = TextComponent.builder("")
                         .append(forSaleText)
                         .append("  ")
                         .append("Price", TextColor.YELLOW)
                         .append(" : ")
                         .append(String.valueOf(claim.getEconomyData().getSalePrice()), TextColor.GOLD)
                         .build();
             }
        }

        Component claimId = TextComponent.builder("")
                .append("UUID", TextColor.YELLOW)
                .append(" : ")
                .append(TextComponent.builder("")
                        .append(claim.getUniqueId().toString(), TextColor.GRAY)
                        .insertion(claim.getUniqueId().toString()).build()).build();
        final String ownerName = PlayerUtil.getInstance().getUserName(ownerUniqueId);
        Component ownerLine = TextComponent.builder("")
                .append("Owner", TextColor.YELLOW)
                .append(" : ")
                .append(ownerName != null && !claim.isAdminClaim() ? ownerName : "administrator", TextColor.GOLD).build();
        Component adminShowText = TextComponent.empty();
        Component basicShowText = TextComponent.empty();
        Component subdivisionShowText = TextComponent.empty();
        Component townShowText = TextComponent.empty();
        Component claimType = TextComponent.empty();
        final Component whiteOpenBracket = TextComponent.of("[");
        final Component whiteCloseBracket = TextComponent.of("]");
        Component defaultTypeText = TextComponent.builder("")
                .append(whiteOpenBracket)
                .append(gpClaim.getFriendlyNameType(true))
                .append(whiteCloseBracket).build();
        if (allowEdit != null && !isAdmin) {
            adminShowText = allowEdit;
            basicShowText = allowEdit;
            subdivisionShowText = allowEdit;
            townShowText = allowEdit;
            Component adminTypeText = TextComponent.builder("")
                    .append(claim.getType() == ClaimTypes.ADMIN ? 
                            defaultTypeText : TextComponent.of("ADMIN", TextColor.GRAY))
                    .hoverEvent(HoverEvent.showText(adminShowText)).build();
            Component basicTypeText = TextComponent.builder("")
                    .append(claim.getType() == ClaimTypes.BASIC ? 
                            defaultTypeText : TextComponent.of("BASIC", TextColor.GRAY))
                    .hoverEvent(HoverEvent.showText(basicShowText)).build();
            Component subTypeText = TextComponent.builder("")
                    .append(claim.getType() == ClaimTypes.SUBDIVISION ? 
                            defaultTypeText : TextComponent.of("SUBDIVISION", TextColor.GRAY))
                    .hoverEvent(HoverEvent.showText(subdivisionShowText)).build();
            Component townTypeText = TextComponent.builder("")
                    .append(claim.getType() == ClaimTypes.TOWN ? 
                            defaultTypeText : TextComponent.of("TOWN", TextColor.GRAY))
                    .hoverEvent(HoverEvent.showText(townShowText)).build();
            claimType = TextComponent.builder("")
                    .append(claim.isCuboid() ? "3D " : "2D ", TextColor.GREEN)
                    .append(adminTypeText)
                    .append(" ")
                    .append(basicTypeText)
                    .append(" ")
                    .append(subTypeText)
                    .append(" ")
                    .append(townTypeText)
                    .build();
        } else {
            Component adminTypeText = defaultTypeText;
            Component basicTypeText = defaultTypeText;
            Component subTypeText = defaultTypeText;
            Component townTypeText = defaultTypeText;
            if (!claim.isAdminClaim()) {
                final Component message = ((GDClaim) claim).validateClaimType(ClaimTypes.ADMIN, ownerUniqueId, playerData).getMessage().orElse(null);
                adminShowText = message != null ? message : TextComponent.builder("")
                        .append("Click here to change claim to ")
                        .append("ADMIN ", TextColor.RED)
                        .append("type.").build();

                if (message == null) {
                    adminTypeText = TextComponent.builder("")
                        .append(claim.getType() == ClaimTypes.ADMIN ? 
                                defaultTypeText : TextComponent.of("ADMIN", TextColor.GRAY))
                        .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createClaimTypeConsumer(src, claim, ClaimTypes.ADMIN, isAdmin))))
                        .hoverEvent(HoverEvent.showText(adminShowText)).build();
                } else {
                    adminTypeText = TextComponent.builder("")
                        .append(claim.getType() == ClaimTypes.ADMIN ? 
                                defaultTypeText : TextComponent.of("ADMIN", TextColor.GRAY))
                        .hoverEvent(HoverEvent.showText(adminShowText)).build();
                }
            }
            if (!claim.isBasicClaim()) {
                final Component message = ((GDClaim) claim).validateClaimType(ClaimTypes.BASIC, ownerUniqueId, playerData).getMessage().orElse(null);
                basicShowText = message != null ? message : TextComponent.builder("")
                        .append("Click here to change claim to ")
                        .append("BASIC ", TextColor.YELLOW)
                        .append("type.").build();

                if (message == null) {
                    basicTypeText = TextComponent.builder("")
                            .append(claim.getType() == ClaimTypes.BASIC ? defaultTypeText : TextComponent.of("BASIC", TextColor.GRAY))
                            .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createClaimTypeConsumer(src, claim, ClaimTypes.BASIC, isAdmin))))
                            .hoverEvent(HoverEvent.showText(basicShowText)).build();
                } else {
                    basicTypeText = TextComponent.builder("")
                            .append(claim.getType() == ClaimTypes.BASIC ? defaultTypeText : TextComponent.of("BASIC", TextColor.GRAY))
                            .hoverEvent(HoverEvent.showText(basicShowText)).build();
                }
            }
            if (!claim.isSubdivision()) {
                final Component message = ((GDClaim) claim).validateClaimType(ClaimTypes.SUBDIVISION, ownerUniqueId, playerData).getMessage().orElse(null);
                subdivisionShowText = message != null ? message : TextComponent.builder("")
                        .append("Click here to change claim to ")
                        .append("SUBDIVISION ", TextColor.AQUA)
                        .append("type.").build();

                if (message == null) {
                    subTypeText = TextComponent.builder("")
                            .append(claim.getType() == ClaimTypes.SUBDIVISION ? defaultTypeText : TextComponent.of("SUBDIVISION", TextColor.GRAY))
                            .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createClaimTypeConsumer(src, claim, ClaimTypes.SUBDIVISION, isAdmin))))
                            .hoverEvent(HoverEvent.showText(subdivisionShowText)).build();
                } else {
                    subTypeText = TextComponent.builder("")
                            .append(claim.getType() == ClaimTypes.SUBDIVISION ? defaultTypeText : TextComponent.of("SUBDIVISION", TextColor.GRAY))
                            .hoverEvent(HoverEvent.showText(subdivisionShowText)).build();
                }
            }
            if (!claim.isTown()) {
                final Component message = ((GDClaim) claim).validateClaimType(ClaimTypes.TOWN, ownerUniqueId, playerData).getMessage().orElse(null);
                townShowText = message != null ? message : TextComponent.builder("")
                        .append("Click here to change claim to ")
                        .append("TOWN ", TextColor.GREEN)
                        .append("type.").build();

                if (message == null) {
                    townTypeText = TextComponent.builder("")
                            .append(claim.getType() == ClaimTypes.TOWN ? defaultTypeText : TextComponent.of("TOWN", TextColor.GRAY))
                            .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createClaimTypeConsumer(src, claim, ClaimTypes.TOWN, isAdmin))))
                            .hoverEvent(HoverEvent.showText(townShowText)).build();
                } else {
                    townTypeText = TextComponent.builder("")
                            .append(claim.getType() == ClaimTypes.TOWN ? defaultTypeText : TextComponent.of("TOWN", TextColor.GRAY))
                            .hoverEvent(HoverEvent.showText(townShowText)).build();
                }
            }

            claimType = TextComponent.builder("")
                    .append(claim.isCuboid() ? "3D " : "2D ", TextColor.GREEN)
                    .append(adminTypeText)
                    .append(" ")
                    .append(basicTypeText)
                    .append(" ")
                    .append(subTypeText)
                    .append(" ")
                    .append(townTypeText)
                    .build();
        }

        Component claimTypeInfo = TextComponent.builder("")
                .append("Type", TextColor.YELLOW)
                .append(" : ")
                .append(claimType).build();
        Component claimInherit = TextComponent.builder("")
                .append(INHERIT_PARENT, TextColor.YELLOW)
                .append(" : ")
                .append(getClickableInfoText(src, claim, INHERIT_PARENT, claim.getData().doesInheritParent() ? TextComponent.of("ON", TextColor.GREEN) : TextComponent.of("OFF", TextColor.RED))).build();
        Component claimExpired = TextComponent.builder("")
                .append("Expired", TextColor.YELLOW)
                .append(" : ")
                .append(claim.getData().isExpired() ? TextComponent.of("YES", TextColor.RED) : TextComponent.of("NO", TextColor.GRAY)).build();
        Component claimFarewell = TextComponent.builder("")
                .append("Farewell", TextColor.YELLOW)
                .append(" : ")
                .append(farewell == null ? NONE : farewell).build();
        Component claimGreeting = TextComponent.builder("")
                .append("Greeting", TextColor.YELLOW)
                .append(" : ")
                .append(greeting == null ? NONE : greeting).build();
        Component claimSpawn = null;
        if (claim.getData().getSpawnPos().isPresent()) {
            Vector3i spawnPos = claim.getData().getSpawnPos().get();
            Location spawnLoc = new Location(gpClaim.getWorld(), spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
            claimSpawn = TextComponent.builder("")
                    .append("Spawn", TextColor.GREEN)
                    .append(" : ")
                    .append(spawnPos.toString(), TextColor.GRAY)
                    .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(CommandHelper.createTeleportConsumer(player, spawnLoc, claim))))
                    .hoverEvent(HoverEvent.showText(TextComponent.of("Click here to teleport to claim spawn.")))
                    .build();
        }
        Component southWestCorner = TextComponent.builder("")
                .append("SW", TextColor.LIGHT_PURPLE)
                .append(" : ")
                .append(VecHelper.toVector3i(southWest).toString(), TextColor.GRAY)
                .append(" ")
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(CommandHelper.createTeleportConsumer(player, southWest, claim))))
                .hoverEvent(HoverEvent.showText(TextComponent.of("Click here to teleport to SW corner of claim.")))
                .build();
        Component southEastCorner = TextComponent.builder("")
                .append("SE", TextColor.LIGHT_PURPLE)
                .append(" : ")
                .append(VecHelper.toVector3i(southEast).toString(), TextColor.GRAY)
                .append(" ")
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(CommandHelper.createTeleportConsumer(player, southEast, claim))))
                .hoverEvent(HoverEvent.showText(TextComponent.of("Click here to teleport to SE corner of claim.")))
                .build();
        Component southCorners = TextComponent.builder("")
                .append("SouthCorners", TextColor.YELLOW)
                .append(" : ")
                .append(southWestCorner)
                .append(southEastCorner).build();
        Component northWestCorner = TextComponent.builder("")
                .append("NW", TextColor.LIGHT_PURPLE)
                .append(" : ")
                .append(VecHelper.toVector3i(northWest).toString(), TextColor.GRAY)
                .append(" ")
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(CommandHelper.createTeleportConsumer(player, northWest, claim))))
                .hoverEvent(HoverEvent.showText(TextComponent.of("Click here to teleport to NW corner of claim.")))
                .build();
        Component northEastCorner = TextComponent.builder("")
                .append("NE", TextColor.LIGHT_PURPLE)
                .append(" : ")
                .append(VecHelper.toVector3i(northEast).toString(), TextColor.GRAY)
                .append(" ")
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(CommandHelper.createTeleportConsumer(player, northEast, claim))))
                .hoverEvent(HoverEvent.showText(TextComponent.of("Click here to teleport to NE corner of claim.")))
                .build();
        Component northCorners = TextComponent.builder("")
                .append("NorthCorners", TextColor.YELLOW)
                .append(" : ")
                .append(northWestCorner)
                .append(northEastCorner).build();
        Component claimAccessors = TextComponent.builder("")
                .append("Accessors", TextColor.YELLOW)
                .append(" : ")
                .append(accessors.equals("") ? NONE : TextComponent.of(accessors, TextColor.BLUE))
                .append(" ")
                .append(accessorGroups, TextColor.LIGHT_PURPLE).build();
        Component claimBuilders = TextComponent.builder("")
                .append("Builders", TextColor.YELLOW)
                .append(" : ")
                .append(builders.equals("") ? NONE : TextComponent.of(builders, TextColor.BLUE))
                .append(" ")
                .append(builderGroups, TextColor.LIGHT_PURPLE).build();
        Component claimContainers = TextComponent.builder("")
                .append("Containers", TextColor.YELLOW)
                .append(" : ")
                .append(containers.equals("") ? NONE : TextComponent.of(containers, TextColor.BLUE))
                .append(" ")
                .append(containerGroups, TextColor.LIGHT_PURPLE).build();
        Component claimCoowners = TextComponent.builder("")
                .append("Managers", TextColor.YELLOW)
                .append(" : ")
                .append(managers.equals("") ? NONE : TextComponent.of(managers, TextColor.BLUE))
                .append(" ")
                .append(managerGroups, TextColor.LIGHT_PURPLE).build();
        Component dateCreated = TextComponent.builder("")
                .append("Created", TextColor.YELLOW)
                .append(" : ")
                .append(created != null ? created.toString() : "Unknown", TextColor.GRAY).build();
        Component dateLastActive = TextComponent.builder("")
                .append("LastActive", TextColor.YELLOW)
                .append(" : ")
                .append(lastActive != null ? lastActive.toString() : "Unknown", TextColor.GRAY).build();
        Component worldName = TextComponent.builder("")
                .append("World", TextColor.YELLOW)
                .append(" : ")
                .append(gpClaim.getWorld().getName(), TextColor.GRAY).build();

        if (claimSpawn != null) {
            textList.add(claimSpawn);
        }
        if (bankInfo != null) {
            textList.add(bankInfo);
        }
        textList.add(claimName);
        textList.add(ownerLine);
        textList.add(claimTypeInfo);
        if (!claim.isAdminClaim() && !claim.isWilderness()) {
            textList.add(TextComponent.builder("")
                    .append(claimInherit)
                    .append("   ")
                    .append(claimExpired).build());
            if (forSaleText != null) {
                textList.add(forSaleText);
            }
        }
        textList.add(claimAccessors);
        textList.add(claimBuilders);
        textList.add(claimContainers);
        textList.add(claimCoowners);
        textList.add(claimGreeting);
        textList.add(claimFarewell);
        textList.add(worldName);
        textList.add(dateCreated);
        textList.add(dateLastActive);
        textList.add(claimId);
        textList.add(northCorners);
        textList.add(southCorners);
        if (!claim.getParent().isPresent()) {
            textList.remove(claimInherit);
        }
        if (claim.isAdminClaim()) {
            textList.remove(bankInfo);
            textList.remove(dateLastActive);
        }
        if (claim.isWilderness()) {
            textList.remove(bankInfo);
            textList.remove(claimInherit);
            textList.remove(claimTypeInfo);
            textList.remove(dateLastActive);
            textList.remove(northCorners);
            textList.remove(southCorners);
        }

        PaginationList.Builder paginationBuilder = PaginationList.builder()
                .title(TextComponent.of("Claim Info", TextColor.AQUA)).padding(TextComponent.of(" ").decoration(TextDecoration.STRIKETHROUGH, true)).contents(textList);
        paginationBuilder.sendTo(src);
    }

    public static Consumer<CommandSender> createSettingsConsumer(CommandSender src, Claim claim, List<Component> textList, ClaimType type) {
        return settings -> {
            String name = type == ClaimTypes.TOWN ? "Town Settings" : "Admin Settings";
            PaginationList.Builder paginationBuilder = PaginationList.builder()
                    .title(TextComponent.of(name, TextColor.AQUA)).padding(TextComponent.of(" ").decoration(TextDecoration.STRIKETHROUGH, true)).contents(textList);
            paginationBuilder.sendTo(src);
        };
    }

    private static List<Component> generateAdminSettings(CommandSender src, GDClaim claim) {
        List<Component> textList = new ArrayList<>();
        Component returnToClaimInfo = TextComponent.builder("")
                .append("\n[")
                .append("Return to standard settings", TextColor.AQUA)
                .append("]\n")
            .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(CommandHelper.createCommandConsumer(src, "claiminfo", claim.getUniqueId().toString())))).build();
        Component claimDenyMessages = TextComponent.builder("")
                .append(DENY_MESSAGES, TextColor.YELLOW)
                .append(" : ")
                .append(getClickableInfoText(src, claim, DENY_MESSAGES, claim.getInternalClaimData().allowDenyMessages() ? TextComponent.of("ON", TextColor.GREEN) : TextComponent.of("OFF", TextColor.RED))).build();
        Component claimResizable = TextComponent.builder("")
                .append(RESIZABLE, TextColor.YELLOW)
                .append(" : ")
                .append(getClickableInfoText(src, claim, RESIZABLE, claim.getInternalClaimData().isResizable() ? TextComponent.of("ON", TextColor.GREEN) : TextComponent.of("OFF", TextColor.RED))).build();
        Component claimRequiresClaimBlocks = TextComponent.builder("")
                .append(REQUIRES_CLAIM_BLOCKS, TextColor.YELLOW)
                .append(" : ")
                .append(getClickableInfoText(src, claim, REQUIRES_CLAIM_BLOCKS, claim.getInternalClaimData().requiresClaimBlocks() ? TextComponent.of("ON", TextColor.GREEN) : TextComponent.of("OFF", TextColor.RED))).build();
        Component claimSizeRestrictions = TextComponent.builder("")
                .append(SIZE_RESTRICTIONS, TextColor.YELLOW)
                .append(" : ")
                .append(getClickableInfoText(src, claim, SIZE_RESTRICTIONS, claim.getInternalClaimData().hasSizeRestrictions() ? TextComponent.of("ON", TextColor.GREEN) : TextComponent.of("OFF", TextColor.RED))).build();
        Component claimExpiration = TextComponent.builder("")
                .append(CLAIM_EXPIRATION, TextColor.YELLOW)
                .append(" : ")
                .append(getClickableInfoText(src, claim, CLAIM_EXPIRATION, claim.getInternalClaimData().allowExpiration() ? TextComponent.of("ON", TextColor.GREEN) : TextComponent.of("OFF", TextColor.RED))).build();
        Component claimFlagOverrides = TextComponent.builder("")
                .append(FLAG_OVERRIDES, TextColor.YELLOW)
                .append(" : ")
                .append(getClickableInfoText(src, claim, FLAG_OVERRIDES, claim.getInternalClaimData().allowFlagOverrides() ? TextComponent.of("ON", TextColor.GREEN) : TextComponent.of("OFF", TextColor.RED))).build();
        Component pvp = TextComponent.builder("")
                .append("PvP", TextColor.YELLOW)
                .append(" : ")
                .append(getClickableInfoText(src, claim, PVP_OVERRIDE, claim.getInternalClaimData().getPvpOverride() == Tristate.TRUE ? TextComponent.of("ON", TextColor.GREEN) : TextComponent.of(claim.getInternalClaimData().getPvpOverride().name(), TextColor.RED))).build();
        textList.add(returnToClaimInfo);
        textList.add(claimDenyMessages);
        if (!claim.isAdminClaim() && !claim.isWilderness()) {
            textList.add(claimRequiresClaimBlocks);
            textList.add(claimExpiration);
            textList.add(claimResizable);
            textList.add(claimSizeRestrictions);
        }
        textList.add(claimFlagOverrides);
        textList.add(pvp);
        int fillSize = 20 - (textList.size() + 4);
        for (int i = 0; i < fillSize; i++) {
            textList.add(TextComponent.of(" "));
        }
        return textList;
    }

    private static void executeAdminSettings(CommandSender src, GDClaim claim) {
        PaginationList.Builder paginationBuilder = PaginationList.builder()
                .title(TextComponent.of("Admin Settings", TextColor.AQUA)).padding(TextComponent.of(" ").decoration(TextDecoration.STRIKETHROUGH, true)).contents(generateAdminSettings(src, claim));
        paginationBuilder.sendTo(src);
    }

    public static Component getClickableInfoText(CommandSender src, Claim claim, String title, Component infoText) {
        Component onClickText = TextComponent.of("Click here to toggle value.");
        boolean hasPermission = true;
        if (src instanceof Player) {
            Component denyReason = ((GDClaim) claim).allowEdit((Player) src);
            if (denyReason != null) {
                onClickText = denyReason;
                hasPermission = false;
            }
        }

        TextComponent.Builder textBuilder = TextComponent.builder("")
                .append(infoText)
                .hoverEvent(HoverEvent.showText(onClickText));
        if (hasPermission) {
            textBuilder.clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createClaimInfoConsumer(src, claim, title))));
        }
        return textBuilder.build();
    }

    private static Consumer<CommandSender> createClaimInfoConsumer(CommandSender src, Claim claim, String title) {
        GDClaim gpClaim = (GDClaim) claim;
        return info -> {
            switch (title) {
                case INHERIT_PARENT : 
                    if (!src.hasPermission(GDPermissions.COMMAND_CLAIM_INHERIT)) {
                        return;
                    }

                    gpClaim.getInternalClaimData().setInheritParent(!gpClaim.getInternalClaimData().doesInheritParent());
                    gpClaim.getInternalClaimData().setRequiresSave(true);
                    claim.getData().save();
                    CommandHelper.executeCommand(src, "claiminfo", gpClaim.getUniqueId().toString());
                    return;
                case CLAIM_EXPIRATION :
                    gpClaim.getInternalClaimData().setExpiration(!gpClaim.getInternalClaimData().allowExpiration());
                    gpClaim.getInternalClaimData().setRequiresSave(true);
                    gpClaim.getClaimStorage().save();
                    break;
                case DENY_MESSAGES :
                    gpClaim.getInternalClaimData().setDenyMessages(!gpClaim.getInternalClaimData().allowDenyMessages());
                    gpClaim.getInternalClaimData().setRequiresSave(true);
                    gpClaim.getClaimStorage().save();
                    break;
                case FLAG_OVERRIDES :
                    gpClaim.getInternalClaimData().setFlagOverrides(!gpClaim.getInternalClaimData().allowFlagOverrides());
                    gpClaim.getInternalClaimData().setRequiresSave(true);
                    gpClaim.getClaimStorage().save();
                    break;
                case PVP_OVERRIDE :
                    Tristate value = gpClaim.getInternalClaimData().getPvpOverride();
                    if (value == Tristate.UNDEFINED) {
                        gpClaim.getInternalClaimData().setPvpOverride(Tristate.TRUE);
                    } else if (value == Tristate.TRUE) {
                        gpClaim.getInternalClaimData().setPvpOverride(Tristate.FALSE);
                    } else {
                        gpClaim.getInternalClaimData().setPvpOverride(Tristate.UNDEFINED);
                    }
                    gpClaim.getInternalClaimData().setRequiresSave(true);
                    gpClaim.getClaimStorage().save();
                    break;
                case RESIZABLE :
                    boolean resizable = gpClaim.getInternalClaimData().isResizable();
                    gpClaim.getInternalClaimData().setResizable(!resizable);
                    gpClaim.getInternalClaimData().setRequiresSave(true);
                    gpClaim.getClaimStorage().save();
                    break;
                case REQUIRES_CLAIM_BLOCKS :
                    boolean requiresClaimBlocks = gpClaim.getInternalClaimData().requiresClaimBlocks();
                    gpClaim.getInternalClaimData().setRequiresClaimBlocks(!requiresClaimBlocks);
                    gpClaim.getInternalClaimData().setRequiresSave(true);
                    gpClaim.getClaimStorage().save();
                    break;
                case SIZE_RESTRICTIONS :
                    boolean sizeRestrictions = gpClaim.getInternalClaimData().hasSizeRestrictions();
                    gpClaim.getInternalClaimData().setSizeRestrictions(!sizeRestrictions);
                    gpClaim.getInternalClaimData().setRequiresSave(true);
                    gpClaim.getClaimStorage().save();
                    break;
                case FOR_SALE :
                    boolean forSale = gpClaim.getEconomyData().isForSale();
                    gpClaim.getEconomyData().setForSale(!forSale);
                    gpClaim.getInternalClaimData().setRequiresSave(true);
                    gpClaim.getClaimStorage().save();
                    CommandHelper.executeCommand(src, "claiminfo", gpClaim.getUniqueId().toString());
                    return;
                default:
            }
            executeAdminSettings(src, gpClaim);
        };
    }

    private static Consumer<CommandSender> createClaimTypeConsumer(CommandSender src, Claim gpClaim, ClaimType clicked, boolean isAdmin) {
        GDClaim claim = (GDClaim) gpClaim;
        return type -> {
            if (!(src instanceof Player)) {
                // ignore
                return;
            }

            final Player player = (Player) src;
            if (!isAdmin && ((GDClaim) gpClaim).allowEdit(player) != null) {
                TextAdapter.sendComponent(src, GriefDefenderPlugin.getInstance().messageData.claimNotYours.toText());
                return;
            }
            final ClaimResult result = claim.changeType(clicked, Optional.of(player.getUniqueId()), src);
            if (result.successful()) {
                CommandHelper.executeCommand(src, "claiminfo", gpClaim.getUniqueId().toString());
            } else {
                TextAdapter.sendComponent(src, result.getMessage().get());
            }
        };
    }
}
