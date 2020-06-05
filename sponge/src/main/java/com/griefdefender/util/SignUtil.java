package com.griefdefender.util;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableMap;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.economy.PaymentType;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.internal.util.BlockUtil;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.text.action.GDCallbackHolder;

import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.spongeapi.TextAdapter;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.format.TextColor;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class SignUtil {

    final static String SELL_SIGN = "[GD-sell]";
    final static String RENT_SIGN = "[GD-rent]";

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

        return claim.getEconomyData().getRentSignPosition().equals(sign.getLocation().getBlockPosition());
    }

    public static boolean isRentSign(Sign sign) {
        if (sign == null) {
            return false;
        }

        final String header = sign.lines().get(0).toPlain();
        if (header.equalsIgnoreCase(RENT_SIGN)) {
            return true;
        }

        return false;
    }

    public static boolean isSellSign(Sign sign) {
        if (sign == null) {
            return false;
        }

        final String header = sign.lines().get(0).toPlain();
        if (header.equalsIgnoreCase(SELL_SIGN)) {
            return true;
        }

        return false;
    }

    public static Sign getSellSign(Location<World> location) {
        if (location == null) {
            return null;
        }

        final Sign sign = getSign(location);
        if (sign == null) {
            return null;
        }

        final String header = sign.lines().get(0).toPlain();
        if (header.equalsIgnoreCase(SELL_SIGN)) {
            return sign;
        }

        return null;
    }

    public static Sign getRentSign(Location<World> location) {
        if (location == null) {
            return null;
        }

        final Sign sign = getSign(location);
        if (sign == null) {
            return null;
        }

        final String header = sign.lines().get(0).toPlain();
        if (header.equalsIgnoreCase(RENT_SIGN)) {
            return sign;
        }

        return null;
    }

    public static Sign getSign(World world, Vector3i pos) {
        if (pos == null) {
            return null;
        }

        // Don't load chunks to update signs
        if (BlockUtil.getInstance().getLoadedChunkWithoutMarkingActive(world, pos.getX() >> 4, pos.getZ() >> 4) == null) {
            return null;
        }

        return getSign(new Location<>(world, pos));
    }

    public static Sign getSign(Location<World> location) {
        if (location == null) {
            return null;
        }

        final TileEntity tileEntity = location.getTileEntity().orElse(null);
        if (tileEntity == null) {
            return null;
        }

        if (!(tileEntity instanceof Sign)) {
            return null;
        }

        return (Sign) tileEntity;
    }

    public static void setClaimForSale(Claim claim, Player player, Sign sign, double price) {
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

    private static Consumer<CommandSource> createSaleConfirmationConsumer(CommandSource src, Claim claim, Sign sign,  double price) {
        return confirm -> {
            claim.getEconomyData().setSalePrice(price);
            claim.getEconomyData().setForSale(true);
            claim.getEconomyData().setSaleSignPosition(sign.getLocation().getBlockPosition());
            claim.getData().save();
            final List<Text> lines = createSellSignLines(claim.getEconomyData().getSalePrice());
            final SignData signData = sign.getOrCreate(SignData.class).orElse(null);
            if (signData != null) { 
                for (int i = 0; i < lines.size(); i++) {
                    signData.addElement(i, lines.get(i));
                }
                sign.offer(signData);
            }
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

    private static Consumer<CommandSource> createRentConfirmationConsumer(CommandSource src, Claim claim, Sign sign, double rate, int min, int max, PaymentType paymentType) {
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
                final int rentMaxLimit = GriefDefenderPlugin.getActiveConfig(((GDClaim) claim).getWorld().getUniqueId()).getConfig().economy.rentMaxTimeLimit;
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
                claim.getEconomyData().setRentSignPosition(rentSign.getLocation().getBlockPosition());
                claim.getData().save();
                final List<Text> lines = createRentSignLines(rate, paymentType);
                final SignData signData = rentSign.getOrCreate(SignData.class).orElse(null);
                if (signData != null) { 
                    for (int i = 0; i < lines.size(); i++) {
                        signData.addElement(i, lines.get(i));
                    }
                    sign.offer(signData);
                }
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
        final List<Text> lines = createRentSignLines(claim.getEconomyData().getRentRate(), paymentType);
        final SignData signData = sign.getOrCreate(SignData.class).orElse(null);
        if (signData != null) { 
            for (int i = 0; i < lines.size(); i++) {
                signData.addElement(i, lines.get(i));
            }
            sign.offer(signData);
        }
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

    public static List<Text> createSellSignLines(double price) {
        List<Text> colorLines = new ArrayList<>(4);
        final DecimalFormat format = new DecimalFormat("0.#");
        final TextComponent headerComponent = TextComponent.builder()
                    .append("[", TextColor.GRAY)
                    .append("GD", TextColor.AQUA)
                    .append("-", TextColor.GRAY)
                    .append("sell", TextColor.DARK_BLUE)
                    .append("]", TextColor.GRAY)
                    .build();

        colorLines.add(SpongeUtil.getSpongeText(headerComponent));
        colorLines.add(SpongeUtil.getSpongeText(MessageCache.getInstance().ECONOMY_SIGN_SELL_DESCRIPTION));
        colorLines.add(SpongeUtil.getSpongeText(TextComponent.builder().append("$" + format.format(price), TextColor.RED).build()));
        colorLines.add(SpongeUtil.getSpongeText(MessageCache.getInstance().ECONOMY_SIGN_SELL_FOOTER));
        return colorLines;
    }

    public static List<Text> createRentSignLines(double rate, PaymentType paymentType) {
        List<Text> lines = new ArrayList<>(4);
        final DecimalFormat format = new DecimalFormat("0.#");
        final TextComponent headerComponent = TextComponent.builder()
                .append("[", TextColor.GRAY)
                .append("GD", TextColor.AQUA)
                .append("-", TextColor.GRAY)
                .append("rent", TextColor.DARK_BLUE)
                .append("]", TextColor.GRAY)
                .build();
        lines.add(SpongeUtil.getSpongeText(headerComponent));
        lines.add(SpongeUtil.getSpongeText(MessageCache.getInstance().ECONOMY_SIGN_RENT_DESCRIPTION));
        lines.add(SpongeUtil.getSpongeText(TextComponent.builder()
                .append("$", TextColor.RED)
                .append(format.format(rate), TextColor.RED)
                .append("/", TextColor.RED)
                .append(paymentType == PaymentType.DAILY ? MessageCache.getInstance().LABEL_DAY.color(TextColor.RED) : MessageCache.getInstance().LABEL_HOUR.color(TextColor.RED))
                .build()));
        lines.add(Text.EMPTY);
        return lines;
    }
}
