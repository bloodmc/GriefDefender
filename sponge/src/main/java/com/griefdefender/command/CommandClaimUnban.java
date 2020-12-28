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
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.spongeapi.TextAdapter;
import net.kyori.text.format.TextColor;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;
import com.google.common.collect.ImmutableMap;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.internal.registry.BlockTypeRegistryModule;
import com.griefdefender.internal.registry.EntityTypeRegistryModule;
import com.griefdefender.internal.registry.ItemTypeRegistryModule;
import com.griefdefender.internal.util.NMSUtil;
import com.griefdefender.permission.GDPermissions;

@CommandAlias("%griefdefender")
@CommandPermission(GDPermissions.COMMAND_CLAIM_BAN)
public class CommandClaimUnban extends BaseCommand {

    @CommandCompletion("@gdbantypes @gdmcids @gddummy")
    @CommandAlias("claimunban")
    @Description("%claim-unban")
    @Syntax("hand | <type> <target>")
    @Subcommand("unban")
    public void execute(Player player, String type, @Optional String id) {
        if (type.equalsIgnoreCase("block")) {
            if (!BlockTypeRegistryModule.getInstance().getById(id).isPresent()) {
                TextAdapter.sendComponent(player, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.REGISTRY_BLOCK_NOT_FOUND,
                        ImmutableMap.of("id", TextComponent.of(id, TextColor.LIGHT_PURPLE))));
                return;
            }
            GriefDefenderPlugin.getGlobalConfig().getConfig().bans.removeBlockBan(id);
            GriefDefenderPlugin.getGlobalConfig().save();
            TextAdapter.sendComponent(player, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.COMMAND_CLAIMUNBAN_SUCCESS_BLOCK,
                    ImmutableMap.of("id", id)));
        } else if (type.equalsIgnoreCase("entity")) {
            if (!EntityTypeRegistryModule.getInstance().getById(id).isPresent()) {
                TextAdapter.sendComponent(player, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.REGISTRY_ENTITY_NOT_FOUND,
                        ImmutableMap.of("id", TextComponent.of(id, TextColor.LIGHT_PURPLE))));
                return;
            }

            GriefDefenderPlugin.getGlobalConfig().getConfig().bans.removeEntityBan(id);
            GriefDefenderPlugin.getGlobalConfig().save();
            TextAdapter.sendComponent(player, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.COMMAND_CLAIMUNBAN_SUCCESS_ENTITY,
                    ImmutableMap.of("id", TextComponent.of(id, TextColor.LIGHT_PURPLE))));
        } else if (type.equalsIgnoreCase("item")) {
            if (!ItemTypeRegistryModule.getInstance().getById(id).isPresent()) {
                TextAdapter.sendComponent(player, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.REGISTRY_ITEM_NOT_FOUND,
                        ImmutableMap.of("id", TextComponent.of(id, TextColor.LIGHT_PURPLE))));
                return;
            }

            GriefDefenderPlugin.getGlobalConfig().getConfig().bans.removeItemBan(id);
            GriefDefenderPlugin.getGlobalConfig().save();
            TextAdapter.sendComponent(player, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.COMMAND_CLAIMUNBAN_SUCCESS_ITEM,
                    ImmutableMap.of("id", TextComponent.of(id, TextColor.LIGHT_PURPLE))));
        } else if (type.equalsIgnoreCase("hand")) {
            final ItemStack itemInHand = player.getItemInHand(HandTypes.MAIN_HAND).orElse(null);
            if (itemInHand == null) {
                return;
            }
            final String handItemId = itemInHand.getType().getId();
            GriefDefenderPlugin.getGlobalConfig().getConfig().bans.removeItemBan(handItemId);
            GriefDefenderPlugin.getGlobalConfig().save();
            TextAdapter.sendComponent(player, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.COMMAND_CLAIMUNBAN_SUCCESS_ITEM,
                    ImmutableMap.of("id", TextComponent.of(handItemId, TextColor.LIGHT_PURPLE))));
        } else {
            TextAdapter.sendComponent(player, MessageStorage.MESSAGE_DATA.getMessage(MessageStorage.COMMAND_INVALID_TYPE,
                    ImmutableMap.of("type", type)));
        }
    }
}
