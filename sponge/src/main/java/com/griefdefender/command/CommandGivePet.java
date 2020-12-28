package com.griefdefender.command;

import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.permission.GDPermissions;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_GIVE_PET)
public class CommandGivePet extends BaseCommand {

    @CommandCompletion("@gdplayers @gddummy")
    @CommandAlias("givepet")
    @Description("%player-give-pet")
    @Syntax("<player>")
    @Subcommand("givepet")
    public void execute(Player player, User newOwner) {
        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        playerData.petRecipientUniqueId = newOwner.getUniqueId();
        GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().COMMAND_PET_TRANSFER_READY);
    }
}
