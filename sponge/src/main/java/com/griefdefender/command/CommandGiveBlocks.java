package com.griefdefender.command;

import java.util.function.Consumer;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import com.google.common.collect.ImmutableMap;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.text.action.GDCallbackHolder;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.spongeapi.TextAdapter;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_GIVE_BLOCKS)
public class CommandGiveBlocks extends BaseCommand {

    @CommandCompletion("@gdplayers @gddummy")
    @CommandAlias("giveblocks")
    @Description("%player-give-blocks")
    @Syntax("<player> <amount>")
    @Subcommand("giveblocks")
    public void execute(Player src, User targetPlayer, int amount) {
        if (amount <= 0) {
            TextAdapter.sendComponent(src, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.COMMAND_INVALID_AMOUNT, 
                    ImmutableMap.of("amount", TextComponent.of(amount, TextColor.GOLD))));
            return;
        }
        if (targetPlayer.getUniqueId().equals(src.getUniqueId())) {
            TextAdapter.sendComponent(src, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.COMMAND_INVALID_PLAYER, 
                    ImmutableMap.of("player", TextComponent.of(src.getName(), TextColor.GOLD))));
            return;
        }

        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(src.getWorld(), src.getUniqueId());
        int availableBlocks = playerData.getAccruedClaimBlocks() + playerData.getBonusClaimBlocks();
        if (amount > availableBlocks) {
            TextAdapter.sendComponent(src, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.COMMAND_GIVEBLOCKS_NOT_ENOUGH, 
                    ImmutableMap.of("amount", TextComponent.of(availableBlocks, TextColor.GOLD))));
            return;
        }

        final GDPermissionUser targetUser = PermissionHolderCache.getInstance().getOrCreateUser(targetPlayer);
        if ((targetUser.getInternalPlayerData().getAccruedClaimBlocks() + amount) >= targetUser.getInternalPlayerData().getMaxAccruedClaimBlocks()) {
            TextAdapter.sendComponent(src, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.PLAYER_ACCRUED_BLOCKS_EXCEEDED,
                    ImmutableMap.of(
                        "player", targetPlayer.getName(),
                        "total", targetUser.getInternalPlayerData().getAccruedClaimBlocks(),
                        "amount", amount)));
            return;
        }

        final Component confirmationText = TextComponent.builder()
                .append(MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.COMMAND_GIVEBLOCKS_CONFIRMATION, 
                        ImmutableMap.of("player", TextComponent.of(targetPlayer.getName(), TextColor.AQUA),
                                        "amount", TextComponent.of(amount, TextColor.GREEN))))
                    .append(TextComponent.builder()
                    .append("\n[")
                    .append(MessageCache.getInstance().LABEL_CONFIRM.color(TextColor.GREEN))
                    .append("]\n")
                    .clickEvent(ClickEvent.runCommand(GDCallbackHolder.getInstance().createCallbackRunCommand(src, createConfirmationConsumer(src, targetUser, amount), true)))
                    .hoverEvent(HoverEvent.showText(MessageCache.getInstance().UI_CLICK_CONFIRM)).build())
                .build();
        TextAdapter.sendComponent(src, confirmationText);
    }

    private static Consumer<CommandSource> createConfirmationConsumer(Player src, GDPermissionUser targetUser, int amount) {
        return confirm -> {
            final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(src.getWorld(), src.getUniqueId());
            final int accruedTotal = playerData.getAccruedClaimBlocks();
            final int bonusTotal = playerData.getBonusClaimBlocks();
            int availableBlocks = accruedTotal + bonusTotal;
            if (amount > availableBlocks) {
                TextAdapter.sendComponent(src, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.COMMAND_GIVEBLOCKS_NOT_ENOUGH, 
                        ImmutableMap.of("amount", TextComponent.of(availableBlocks, TextColor.GOLD))));
                return;
            }
            if ((targetUser.getInternalPlayerData().getAccruedClaimBlocks() + amount) >= targetUser.getInternalPlayerData().getMaxAccruedClaimBlocks()) {
                TextAdapter.sendComponent(src, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.PLAYER_ACCRUED_BLOCKS_EXCEEDED,
                        ImmutableMap.of(
                            "player", targetUser.getName(),
                            "total", targetUser.getInternalPlayerData().getAccruedClaimBlocks(),
                            "amount", amount)));
                return;
            }

            if (bonusTotal >= amount) {
                playerData.setBonusClaimBlocks(bonusTotal - amount);
            } else if (accruedTotal >= amount) {
                playerData.setAccruedClaimBlocks(accruedTotal - amount);
            } else {
                int remaining = amount - bonusTotal;
                playerData.setBonusClaimBlocks(0);
                int newAccrued = accruedTotal - remaining;
                playerData.setAccruedClaimBlocks(newAccrued);
            }

            targetUser.getInternalPlayerData().setAccruedClaimBlocks(targetUser.getInternalPlayerData().getAccruedClaimBlocks() + amount);
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.COMMAND_GIVEBLOCKS_CONFIRMED);
            TextAdapter.sendComponent(src, message);

            if (targetUser.getOnlinePlayer() != null) {
                final Component targetMessage = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.COMMAND_GIVEBLOCKS_RECEIVED, 
                        ImmutableMap.of("amount", TextComponent.of(amount, TextColor.GOLD),
                                        "player", TextComponent.of(src.getName(), TextColor.AQUA)));
                TextAdapter.sendComponent((Player) targetUser.getOnlinePlayer(), targetMessage);
            }
        };
    }
}
