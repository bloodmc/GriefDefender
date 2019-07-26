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
package com.griefdefender.configuration;

import com.griefdefender.GriefDefenderPlugin;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.commented.SimpleCommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMapper;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Level;

public class MessageStorage {

    private HoconConfigurationLoader loader;
    private CommentedConfigurationNode root = SimpleCommentedConfigurationNode.root(ConfigurationOptions.defaults()
            .setHeader(GriefDefenderPlugin.CONFIG_HEADER));
    private ObjectMapper<MessageDataConfig>.BoundInstance configMapper;
    private MessageDataConfig configBase;

    public static final String ABANDON_CLAIM_ADVERTISEMENT = "abandon-claim-advertisement";
    public static final String ABANDON_CLAIM_MISSING = "abandon-claim-missing";
    public static final String ABANDON_OTHER_SUCCESS = "abandon-other-success";
    public static final String ABANDON_SUCCESS = "abandon-success";
    public static final String ABANDON_TOP_LEVEL = "abandon-top-level";
    public static final String ABANDON_TOWN_CHILDREN = "abandon-town-children";
    public static final String ADJUST_ACCRUED_BLOCKS_SUCCESS = "adjust-accrued-blocks-success";
    public static final String ADJUST_BONUS_BLOCKS_SUCCESS = "adjust-bonus-blocks-success";
    public static final String BANK_TAX_SYSTEM_DISABLED = "bank-tax-system-disabled";
    public static final String BLOCK_CLAIMED = "block-claimed";
    public static final String BLOCK_SALE_VALUE = "block-sale-value";
    public static final String BOOK_TOOLS = "book-tools";
    public static final String CREATE_OVERLAP = "create-overlap";
    public static final String CREATE_OVERLAP_PLAYER = "create-overlap-player";
    public static final String CREATE_OVERLAP_SHORT = "create-overlap-short";
    public static final String CREATE_SUBDIVISION_FAIL = "create-subdivision-fail";
    public static final String CLAIM_ABANDON_SUCCESS = "claim-abandon-success";
    public static final String CLAIM_ABOVE_LEVEL = "claim-above-level";
    public static final String CLAIM_AUTOMATIC_NOTIFICATION = "claim-automatic-notification";
    public static final String CLAIM_BANK_INFO = "claim-bank-info";
    public static final String CLAIM_BELOW_LEVEL = "claim-below-level";
    public static final String CLAIM_CHEST_CONFIRMATION = "claim-chest-confirmation";
    public static final String CLAIM_DISABLED_WORLD = "claim-disabled-world";
    public static final String CLAIM_IGNORE = "claim-ignore";
    public static final String CLAIM_OWNER_ONLY = "claim-owner-only";
    public static final String CLAIM_NO_CLAIMS = "claim-no-claims";
    public static final String CLAIM_NOT_FOUND = "claim-not-found";
    public static final String CLAIM_NOT_YOURS = "claim-not-yours";
    public static final String CLAIM_SIZE_NEED_BLOCKS_2D = "size-need-blocks-2d";
    public static final String CLAIM_SIZE_NEED_BLOCKS_3D = "size-need-blocks-3d";
    public static final String CLAIM_RESIZE_SUCCESS_2D = "resize-success-2d";
    public static final String CLAIM_RESIZE_SUCCESS_3D = "resize-success-3d";
    public static final String CLAIM_RESPECTING = "claim-respecting";
    public static final String CLAIM_SIZE_MIN = "claim-size-min";
    public static final String CLAIM_SIZE_MAX = "claim-size-max";
    public static final String CLAIM_SIZE_TOO_SMALL = "claim-size-too-small";
    public static final String CLAIM_TRANSFER_EXCEEDS_LIMIT = "claim-transfer-exceeds-limit";
    public static final String CLAIM_TRANSFER_SUCCESS = "claim-transfer-success";
    public static final String CLAIM_CREATE_INSUFFICIENT_BLOCKS_2D = "claim-create-insufficient-blocks-2d";
    public static final String CLAIM_CREATE_INSUFFICIENT_BLOCKS_3D = "claim-create-insufficient-blocks-3d";
    public static final String COMMAND_CUBOID_DISABLED = "command-cuboid-disabled";
    public static final String COMMAND_CUBOID_ENABLED = "command-cuboid-enabled";
    public static final String CREATE_SUBDIVISION_ONLY = "create-subdivision-only";
    public static final String CREATE_FAILED_CLAIM_LIMIT = "create-failed-claim-limit";
    public static final String ECONOMY_BLOCK_BUY_INVALID = "economy-block-buy-invalid";
    public static final String ECONOMY_BLOCK_NOT_AVAILABLE = "economy-block-not-available";
    public static final String ECONOMY_BLOCK_ONLY_BUY = "economy-block-only-buy";
    public static final String ECONOMY_BLOCK_ONLY_SELL = "economy-block-only-sell";
    public static final String ECONOMY_BLOCK_PURCHASE_CONFIRMATION = "economy-block-purchase-confirmation";
    public static final String ECONOMY_BLOCK_PURCHASE_COST = "economy-block-purchase-cost";
    public static final String ECONOMY_BLOCK_PURCHASE_LIMIT = "economy-block-purchase-limit";
    public static final String ECONOMY_BUY_SELL_DISABLED = "economy-buy-sell-disabled";
    public static final String ECONOMY_CLAIM_ABANDON_SUCCESS = "economy-claim-abandon-success";
    public static final String ECONOMY_CLAIM_SALE_CONFIRMED = "economy-claim-sale-confirmed";
    public static final String ECONOMY_NOT_ENOUGH_FUNDS = "economy-not-enough-funds";
    public static final String ECONOMY_NOT_INSTALLED = "economy-not-installed";
    public static final String ECONOMY_PLAYER_NOT_FOUND = "economy-player-not-found";
    public static final String ECONOMY_WITHDRAW_ERROR = "economy-withdraw-error";
    public static final String MODE_ADMIN = "mode-admin";
    public static final String MODE_BASIC = "mode-basic";
    public static final String MODE_SUBDIVISION = "mode-subdivision";
    public static final String MODE_TOWN = "mode-town";
    public static final String OWNER_ADMIN = "owner-admin";
    public static final String PERMISSION_CLAIM_CREATE = "permission-claim-create";
    public static final String PERMISSION_CLAIM_DELETE = "permission-claim-delete";
    public static final String PERMISSION_CLAIM_ENTER = "permission-claim-enter";
    public static final String PERMISSION_CLAIM_EXIT = "permission-claim-exit";
    public static final String PERMISSION_CLAIM_IGNORE = "permission-claim-ignore";
    public static final String PERMISSION_CLAIM_LIST = "permission-claim-list";
    public static final String PERMISSION_CLAIM_MANAGE = "permission-claim-manage";
    public static final String PERMISSION_CLAIM_TRANSFER_ADMIN = "permission-claim-transfer-admin";
    public static final String PERMISSION_CREATE_RESET_FLAGS = "permission-claim-reset-flags";
    public static final String PERMISSION_COMMAND_TRUST = "permission-command-trust";
    public static final String PERMISSION_CUBOID = "permission-cuboid";
    public static final String PERMISSION_OVERRIDE_DENY = "permission-override-deny";
    public static final String PERMISSION_TRUST = "permission-trust";
    public static final String PLUGIN_EVENT_CANCEL = "plugin-event-cancel";
    public static final String RESIZE_SAME_LOCATION = "resize-same-location";
    public static final String SCHEMATIC_RESTORE_CONFIRMED = "schematic-restore-confirmed";
    public static final String TAX_CLAIM_EXPIRED = "tax-claim-expired";
    public static final String TOWN_CREATE_NOT_ENOUGH_FUNDS = "town-create-not-enough-funds";
    public static final String TOWN_NOT_IN = "town-not-in";
    public static final String TRUST_INVALID = "trust-invalid";
    public static final String TRUST_LIST_HEADER = "trust-list-header";
    public static final String TRUST_NO_CLAIMS = "trust-no-claims";
    public static final String TRUST_SELF = "trust-self";
    public static final String TUTORIAL_CLAIM_BASIC = "tutorial-claim-basic";
    public static final String UNTRUST_SELF = "untrust-self";

    @SuppressWarnings({"unchecked", "rawtypes"})
    public MessageStorage(Path path) {

        try {
            if (Files.notExists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            if (Files.notExists(path)) {
                Files.createFile(path);
            }

            this.loader = HoconConfigurationLoader.builder().setPath(path).build();
            this.configMapper = (ObjectMapper.BoundInstance) ObjectMapper.forClass(MessageDataConfig.class).bindToNew();

            reload();
            save();
        } catch (Exception e) {
            GriefDefenderPlugin.getInstance().getLogger().log(Level.SEVERE, "Failed to initialize configuration", e);
        }
    }

    public MessageDataConfig getConfig() {
        return this.configBase;
    }

    public void save() {
        try {
            this.configMapper.serialize(this.root.getNode(GriefDefenderPlugin.MOD_ID));
            this.loader.save(this.root);
        } catch (IOException | ObjectMappingException e) {
            GriefDefenderPlugin.getInstance().getLogger().log(Level.SEVERE, "Failed to save configuration", e);
        }
    }

    public void reload() {
        try {
            this.root = this.loader.load(ConfigurationOptions.defaults());
            this.configBase = this.configMapper.populate(this.root.getNode(GriefDefenderPlugin.MOD_ID));
        } catch (Exception e) {
            GriefDefenderPlugin.getInstance().getLogger().log(Level.SEVERE, "Failed to load configuration", e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public void resetMessageData(String message) {
        for (Map.Entry<Object, ? extends CommentedConfigurationNode> mapEntry : this.root.getNode(GriefDefenderPlugin.MOD_ID).getChildrenMap().entrySet()) {
            CommentedConfigurationNode node = (CommentedConfigurationNode) mapEntry.getValue();
            String key = "";
            String comment = node.getComment().orElse(null);
            if (comment == null && node.getKey() instanceof String) {
                key = (String) node.getKey();
                if (key.equalsIgnoreCase(message)) {
                    this.root.getNode(GriefDefenderPlugin.MOD_ID).removeChild(mapEntry.getKey());
                }
            }
        }
 
        try {
            this.loader.save(this.root);
            this.configMapper = (ObjectMapper.BoundInstance) ObjectMapper.forClass(MessageDataConfig.class).bindToNew();
            this.configBase = this.configMapper.populate(this.root.getNode(GriefDefenderPlugin.MOD_ID));
        } catch (IOException | ObjectMappingException e) {
            e.printStackTrace();
        }

       GriefDefenderPlugin.getInstance().messageData = this.configBase;
    }

    public CommentedConfigurationNode getRootNode() {
        return this.root.getNode(GriefDefenderPlugin.MOD_ID);
    }
}
