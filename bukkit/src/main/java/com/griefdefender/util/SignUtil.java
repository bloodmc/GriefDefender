package com.griefdefender.util;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableMap;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.economy.PaymentType;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.internal.util.VecHelper;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.text.action.GDCallbackHolder;

import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.format.TextColor;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;

public class SignUtil {

    final static String SELL_SIGN = "[GD-sell]";
    final static String RENT_SIGN = "[GD-rent]";

    public static boolean isSellSign(Block block) {
        if (!isSign(block)) {
            return false;
        }

        final Sign sign = (Sign) block.getState();
        final String header = ChatColor.stripColor(sign.getLine(0));
        if (header.equalsIgnoreCase(SELL_SIGN)) {
            return true;
        }

        return false;
    }

    public static boolean isRentSign(Block block) {
        if (!isSign(block)) {
            return false;
        }

        final Sign sign = (Sign) block.getState();
        final String header = ChatColor.stripColor(sign.getLine(0));
        if (header.equalsIgnoreCase(RENT_SIGN)) {
            return true;
        }

        return false;
    }

    public static boolean isSellSign(Sign sign) {
        if (sign == null) {
            return false;
        }

        final String header = ChatColor.stripColor(sign.getLine(0));
        if (header.equalsIgnoreCase(SELL_SIGN)) {
            return true;
        }

        return false;
    }

    public static boolean isRentSign(Claim claim, Sign sign) {
        if (sign == null) {
            return false;
        }

        if (claim.getEconomyData() == null) {
            return false;
        }

        if (claim.getEconomyData().getRentSignPosition() == null) {
            return isRentSign(sign);
        }

        return claim.getEconomyData().getRentSignPosition().equals(VecHelper.toVector3i(sign.getLocation()));
    }

    public static boolean isRentSign(Sign sign) {
        if (sign == null) {
            return false;
        }

        final String header = ChatColor.stripColor(sign.getLine(0));
        if (header.equalsIgnoreCase(RENT_SIGN)) {
            return true;
        }

        return false;
    }

    public static boolean isSign(Block block) {
        if (block == null) {
            return false;
        }

        final BlockState state = block.getState();
        if (!(state instanceof Sign)) {
            return false;
        }

        return true;
    }

    public static Sign getSign(World world, Vector3i pos) {
        if (pos == null) {
            return null;
        }

        // Don't load chunks to update signs
        if (!world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) {
            return null;
        }

        return getSign(VecHelper.toLocation(world, pos));
    }

    public static Sign getSign(Location location) {
        if (location == null) {
            return null;
        }

        final Block block = location.getBlock();
        final BlockState state = block.getState();
        if (!(state instanceof Sign)) {
            return null;
        }

        return (Sign) state;
    }

    public static void setClaimForSale(Claim claim, Player player, Sign sign, double price) {
        if (claim.isWilderness()) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().ECONOMY_CLAIM_NOT_FOR_SALE);
            return;
        }

        // if not owner of claim, validate perms
        if (((GDClaim) claim).allowEdit(player) != null || !player.hasPermission(GDPermissions.COMMAND_CLAIM_INFO_OTHERS)) {
            TextAdapter.sendComponent(player, MessageCache.getInstance().CLAIM_NOT_YOURS);
            return;
        }

        final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_SALE_CONFIRMATION,
                ImmutableMap.of(
                "amount", price));
        GriefDefenderPlugin.sendMessage(player, message);

        final Component saleConfirmationText = TextComponent.builder("")
                .append("\n[")
                .append("Confirm", TextColor.GREEN)
                .append("]\n")
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(createSaleConfirmationConsumer(player, claim, sign, price))))
                .build();
        GriefDefenderPlugin.sendMessage(player, saleConfirmationText);
    }

    private static Consumer<CommandSender> createSaleConfirmationConsumer(CommandSender src, Claim claim, Sign sign, double price) {
        return confirm -> {
            claim.getEconomyData().setSalePrice(price);
            claim.getEconomyData().setForSale(true);
            claim.getEconomyData().setSaleSignPosition(VecHelper.toVector3i(sign.getLocation()));
            claim.getData().save();
            List<String> lines = new ArrayList<>(4);
            lines.add(ChatColor.translateAlternateColorCodes('&', "&7[&bGD&7-&1sell&7]"));
            lines.add(ChatColor.translateAlternateColorCodes('&', LegacyComponentSerializer.legacy().serialize(MessageCache.getInstance().ECONOMY_SIGN_SELL_DESCRIPTION)));
            lines.add(ChatColor.translateAlternateColorCodes('&', "&4$" + String.format("%.2f", price)));
            lines.add(ChatColor.translateAlternateColorCodes('&', LegacyComponentSerializer.legacy().serialize(MessageCache.getInstance().ECONOMY_SIGN_SELL_FOOTER)));

            for (int i = 0; i < lines.size(); i++) {
                sign.setLine(i, lines.get(i));
            }
            sign.update();
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_SALE_CONFIRMED,
                    ImmutableMap.of("amount", price));
            GriefDefenderPlugin.sendMessage(src, message);
        };
    }

    public static void setClaimForRent(Claim claim, Player player, Sign sign, double rate, int min, int max, PaymentType paymentType) {
        // if not owner of claim, validate perms
        if (((GDClaim) claim).allowEdit(player) != null || !player.hasPermission(GDPermissions.COMMAND_CLAIM_INFO_OTHERS)) {
            TextAdapter.sendComponent(player, MessageCache.getInstance().CLAIM_NOT_YOURS);
            return;
        }
        if (claim.getEconomyData().isRented()) {
            // already rented
            final UUID uuid = claim.getEconomyData().getRenters().get(0);
            final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(uuid);
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_RENTED_ALREADY, ImmutableMap.of(
                    "player", user.getName()));
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }

        Duration duration = null;
        Component message = null;
        Component maxTime = null;
        Component minTime = null;
        Instant endDate = max > 0 ? Instant.now().plus(max, paymentType == PaymentType.DAILY ? ChronoUnit.DAYS : ChronoUnit.HOURS) : null;
        if (max > 0) {
            duration = Duration.between(Instant.now(), endDate);
            final long seconds = duration.getSeconds();
            final int day = (int)TimeUnit.SECONDS.toDays(seconds);        
            final long hours = TimeUnit.SECONDS.toHours(seconds) - (day *24);
            final long minutes = TimeUnit.SECONDS.toMinutes(seconds) - (TimeUnit.SECONDS.toHours(seconds)* 60);
            TextComponent.Builder maxBuilder = TextComponent.builder();
            if (day > 0) {
                maxBuilder.append(String.valueOf(day))
                    .append(" ")
                    .append((day > 1 ? MessageCache.getInstance().LABEL_DAYS : MessageCache.getInstance().LABEL_DAY))
                    .append(" ");
            }
            if (hours > 0) {
                maxBuilder.append(String.valueOf(hours))
                .append(" ")
                .append((hours > 1 ? MessageCache.getInstance().LABEL_HOURS : MessageCache.getInstance().LABEL_HOUR))
                .append(" ");
            }
            if (minutes > 0) {
                maxBuilder.append(String.valueOf(minutes))
                .append(" ")
                .append((minutes > 1 ? MessageCache.getInstance().LABEL_MINUTES : MessageCache.getInstance().LABEL_MINUTE))
                .append(" ");
            }
            maxTime = maxBuilder.build();
        }
        if (min > 0) {
            minTime = TextComponent.builder()
            .append(String.valueOf(min))
            .append(" ")
            .append(claim.getEconomyData().getPaymentType() == PaymentType.DAILY ? 
                    (min > 1 ? MessageCache.getInstance().LABEL_DAYS : MessageCache.getInstance().LABEL_DAY) : 
                        (min > 1 ? MessageCache.getInstance().LABEL_HOURS : MessageCache.getInstance().LABEL_HOUR))
                .build();
        }

        if (min <= 0 && max <= 0) {
            message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_RENT_CONFIRMATION,
                ImmutableMap.of(
                "amount", "$" + String.format("%.2f", rate),
                "type", paymentType == PaymentType.DAILY ? MessageCache.getInstance().LABEL_DAY : MessageCache.getInstance().LABEL_HOUR));
        } else if (min > 0 && max <= 0) {
            message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_RENT_CONFIRMATION_MIN,
                    ImmutableMap.of(
                    "amount", "$" + String.format("%.2f",rate),
                    "type", paymentType == PaymentType.DAILY ? MessageCache.getInstance().LABEL_DAY : MessageCache.getInstance().LABEL_HOUR,
                    "min-time", minTime));
        } else if (min <= 0 && max > 0) {
            message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_RENT_CONFIRMATION_MAX,
                    ImmutableMap.of(
                    "amount", "$" + String.format("%.2f",rate),
                    "type", paymentType == PaymentType.DAILY ? MessageCache.getInstance().LABEL_DAY : MessageCache.getInstance().LABEL_HOUR,
                    "max-time", maxTime));
        } else if (min > 0 && max > 0) {
            message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_RENT_CONFIRMATION_MIN_MAX,
                    ImmutableMap.of(
                    "amount", "$" + String.format("%.2f",rate),
                    "type", paymentType == PaymentType.DAILY ? MessageCache.getInstance().LABEL_DAY : MessageCache.getInstance().LABEL_HOUR,
                    "min-time", minTime,
                    "max-time", maxTime));
        }
        GriefDefenderPlugin.sendMessage(player, message);

        final Component rentConfirmationText = TextComponent.builder("")
                .append("\n[")
                .append(MessageCache.getInstance().LABEL_CONFIRM.color(TextColor.GREEN))
                .append("]\n")
                .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(player, createRentConfirmationConsumer(player, claim, sign, rate, min, max, paymentType), true)))
                .build();
        GriefDefenderPlugin.sendMessage(player, rentConfirmationText);
    }

    private static Consumer<CommandSender> createRentConfirmationConsumer(CommandSender src, Claim claim, Sign sign, double rate, int min, int max, PaymentType paymentType) {
        return confirm -> {
            resetRentData(claim);
            claim.getEconomyData().setRentRate(rate);
            claim.getEconomyData().setPaymentType(paymentType);
            if (min > 0) {
                if (min > max) {
                    claim.getEconomyData().setRentMinTime(max);
                } else {
                    claim.getEconomyData().setRentMinTime(min);
                }
            }
            if (max > 0) {
                final int rentMaxLimit = GriefDefenderPlugin.getActiveConfig(((GDClaim) claim).getWorld()).getConfig().economy.rentMaxTimeLimit;
                if (max > rentMaxLimit) {
                    claim.getEconomyData().setRentMaxTime(rentMaxLimit);
                } else {
                    claim.getEconomyData().setRentMaxTime(max);
                }

                claim.getEconomyData().setRentEndDate(Instant.now().plus(max, paymentType == PaymentType.DAILY ? ChronoUnit.DAYS : ChronoUnit.HOURS));
            }
            claim.getEconomyData().setForRent(true);
            Sign rentSign = sign;
            if (rentSign == null) {
                rentSign = SignUtil.getSign(((GDClaim) claim).getWorld(), claim.getEconomyData().getRentSignPosition());
            }
            if (rentSign != null) {
                claim.getEconomyData().setRentSignPosition(VecHelper.toVector3i(sign.getLocation()));
                claim.getData().save();
                List<String> lines = new ArrayList<>(4);
                lines.add(ChatColor.translateAlternateColorCodes('&', "&7[&bGD&7-&1rent&7]"));
                lines.add(ChatColor.translateAlternateColorCodes('&', LegacyComponentSerializer.legacy().serialize(MessageCache.getInstance().ECONOMY_SIGN_RENT_DESCRIPTION)));
                lines.add(ChatColor.translateAlternateColorCodes('&', "&4$" + String.format("%.2f", rate) + "/" + LegacyComponentSerializer.legacy().serialize((paymentType == PaymentType.DAILY ? MessageCache.getInstance().LABEL_DAY : MessageCache.getInstance().LABEL_HOUR))));
                for (int i = 0; i < lines.size(); i++) {
                    sign.setLine(i, lines.get(i));
                }
                sign.update();
            }

            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.ECONOMY_CLAIM_RENT_CONFIRMED,
                    ImmutableMap.of(
                    "amount", "$" + rate,
                    "type", paymentType == PaymentType.DAILY ? MessageCache.getInstance().LABEL_DAY : MessageCache.getInstance().LABEL_HOUR));
            GriefDefenderPlugin.sendMessage(src, message);
        };
    }

    public static void updateSignRentable(Claim claim, Sign sign) {
        if (!isRentSign(claim, sign)) {
            return;
        }


        final PaymentType paymentType = claim.getEconomyData().getPaymentType();
        List<String> colorLines = new ArrayList<>(4);
        colorLines.add(ChatColor.translateAlternateColorCodes('&', "&7[&bGD&7-&1rent&7]"));
        colorLines.add(ChatColor.translateAlternateColorCodes('&', LegacyComponentSerializer.legacy().serialize(MessageCache.getInstance().ECONOMY_SIGN_RENT_DESCRIPTION)));
        colorLines.add(ChatColor.translateAlternateColorCodes('&', "&4$" + claim.getEconomyData().getRentRate()));

        for (int i = 0; i < colorLines.size(); i++) {
            sign.setLine(i, colorLines.get(i));
        }
        sign.update();
    }

    public static int getRentMinTime(String line) {
        if (line == null || line.isEmpty() && !line.matches(".*\\d.*")) {
            return 0;
        }

        // check if spaces
        String[] split = line.split(" ");
        String min = null;
        if (split[0].contains("max")) {
            return 0;
        }
        min = split[0].replace("min", "");

        Integer rentMin = null;
        try {
            rentMin = Integer.valueOf(min);
        } catch (NumberFormatException e) {
            return 0;
        }

        return rentMin;
    }

    public static int getRentMaxTime(String line) {
        if (line == null || line.isEmpty() && !line.matches(".*\\d.*")) {
            return 0;
        }

        // check if spaces
        String[] split = line.split(" ");
        String max = null;
        if (split.length == 1) {
            if (split[0].contains("min") && !split[0].contains("max")) {
                return 0;
            }
            max = split[0].replace("max", "");
        } else {
            max = split[1].replace("max", "");
        }

        Integer rentMax = null;
        try {
            rentMax = Integer.valueOf(max);
        } catch (NumberFormatException e) {
            return 0;
        }

        return rentMax;
    }

    public static PaymentType getPaymentType(String line) {
        final String strType = line.substring(line.length() - 1);
        PaymentType paymentType = PaymentType.UNDEFINED;
        if (strType.equalsIgnoreCase("d")) {
            paymentType = PaymentType.DAILY;
        } else if(strType.equalsIgnoreCase("h")) {
            paymentType = PaymentType.HOURLY;
        }
        return paymentType;
    }

    public static void resetRentData(Claim claim) {
        claim.getEconomyData().setForRent(false);
        claim.getEconomyData().setRentStartDate(null);
        claim.getEconomyData().setRentEndDate(null);
        claim.getEconomyData().setRentSignPosition(null);
        claim.getEconomyData().setRentPastDueDate(null);
        claim.getEconomyData().setRentMaxTime(0);
        claim.getEconomyData().setRentMinTime(0);
        claim.getEconomyData().getRenters().clear();
    }

    public static void resetSellData(Claim claim) {
        claim.getEconomyData().setForSale(false);
        claim.getEconomyData().setSaleEndDate(null);
        claim.getEconomyData().setSalePrice(-1);
        claim.getEconomyData().setSaleSignPosition(null);
    }
}
