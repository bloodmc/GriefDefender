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
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.format.TextColor;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;

import com.google.common.collect.ImmutableMap;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.internal.registry.BlockTypeRegistryModule;
import com.griefdefender.internal.registry.EntityTypeRegistryModule;
import com.griefdefender.internal.registry.ItemTypeRegistryModule;
import com.griefdefender.permission.GDPermissions;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_CLAIM_BAN)
public class CommandClaimBan extends BaseCommand {

    @CommandCompletion("@gdbantypes @gdmcids @gddummy")
    @CommandAlias("claimban")
    @Description("%claim-ban")
    @Syntax("hand | <type> <target> [<message>]")
    @Subcommand("ban")
    public void execute(Player player, String type, @Optional String id, @Optional String message) {
        Component component = null;
        if (type.equalsIgnoreCase("block")) {
            if (!BlockTypeRegistryModule.getInstance().getById(id).isPresent()) {
                TextAdapter.sendComponent(player, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.REGISTRY_BLOCK_NOT_FOUND,
                        ImmutableMap.of("id", TextComponent.of(id, TextColor.LIGHT_PURPLE))));
                return;
            }
            if (message == null) {
                component = TextComponent.empty();
            } else {
                component = LegacyComponentSerializer.legacy().deserialize(message, '&');
            }
            GriefDefenderPlugin.getGlobalConfig().getConfig().bans.addBlockBan(id, component);
            GriefDefenderPlugin.getGlobalConfig().save();
            TextAdapter.sendComponent(player, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.COMMAND_CLAIMBAN_SUCCESS_BLOCK,
                    ImmutableMap.of("id", TextComponent.of(id, TextColor.LIGHT_PURPLE))));
        } else if (type.equalsIgnoreCase("entity")) {
            if (!EntityTypeRegistryModule.getInstance().getById(id).isPresent()) {
                TextAdapter.sendComponent(player, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.REGISTRY_ENTITY_NOT_FOUND,
                        ImmutableMap.of("id", TextComponent.of(id, TextColor.LIGHT_PURPLE))));
                return;
            }
            if (message == null) {
                component = TextComponent.empty();
            } else {
                component = LegacyComponentSerializer.legacy().deserialize(message, '&');
            }
            GriefDefenderPlugin.getGlobalConfig().getConfig().bans.addEntityBan(id, component);
            GriefDefenderPlugin.getGlobalConfig().save();
            TextAdapter.sendComponent(player, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.COMMAND_CLAIMBAN_SUCCESS_ENTITY,
                    ImmutableMap.of("id", TextComponent.of(id, TextColor.LIGHT_PURPLE))));
        } else if (type.equalsIgnoreCase("item")) {
            if (!ItemTypeRegistryModule.getInstance().getById(id).isPresent()) {
                TextAdapter.sendComponent(player, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.REGISTRY_ITEM_NOT_FOUND,
                        ImmutableMap.of("id", TextComponent.of(id, TextColor.LIGHT_PURPLE))));
                return;
            }
            if (message == null) {
                component = TextComponent.empty();
            } else {
                component = LegacyComponentSerializer.legacy().deserialize(message, '&');
            }
            GriefDefenderPlugin.getGlobalConfig().getConfig().bans.addItemBan(id, component);
            GriefDefenderPlugin.getGlobalConfig().save();
            TextAdapter.sendComponent(player, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.COMMAND_CLAIMBAN_SUCCESS_ITEM,
                    ImmutableMap.of("id", TextComponent.of(id, TextColor.LIGHT_PURPLE))));
        } else if (type.equalsIgnoreCase("hand")) {
            final ItemStack itemInHand = player.getItemInHand();
            final String handItemId = ItemTypeRegistryModule.getInstance().getNMSKey(itemInHand);
            if (message == null) {
                component = TextComponent.empty();
            } else {
                component = LegacyComponentSerializer.legacy().deserialize(message, '&');
            }
            GriefDefenderPlugin.getGlobalConfig().getConfig().bans.addItemBan(handItemId, component);
            GriefDefenderPlugin.getGlobalConfig().save();
            TextAdapter.sendComponent(player, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.COMMAND_CLAIMBAN_SUCCESS_ITEM,
                    ImmutableMap.of("id", TextComponent.of(handItemId, TextColor.LIGHT_PURPLE))));
        }
        if (component == null) {
            TextAdapter.sendComponent(player, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.COMMAND_INVALID_TYPE,
                    ImmutableMap.of("type", type)));
        }
    }
}
