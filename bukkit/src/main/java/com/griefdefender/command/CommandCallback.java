package com.griefdefender.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Description;
import com.griefdefender.text.action.GDCallbackHolder;
import org.bukkit.command.CommandSender;

import java.util.UUID;
import java.util.function.Consumer;

public class CommandCallback extends BaseCommand {

    @CommandAlias("gp:callback")
    @Description("Execute a callback registered as part of a Text object. Primarily for internal use")
    public void execute(CommandSender src, String[] args) {
        final UUID callbackId = UUID.fromString(args[0]);
        Consumer<CommandSender> callback = GDCallbackHolder.getInstance().getCallbackForUUID(callbackId).orElse(null);
        if (callback != null) {
            callback.accept(src);
        }
    }
}
