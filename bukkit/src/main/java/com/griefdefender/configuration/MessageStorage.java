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

    public static final String ABANDON_CLAIM_MISSING = "abandon-claim-missing";
    public static final String ABANDON_OTHER_SUCCESS = "abandon-other-success";
    public static final String ABANDON_SUCCESS = "abandon-success";
    public static final String ABANDON_TOP_LEVEL = "abandon-top-level";
    public static final String ABANDON_TOWN_CHILDREN = "abandon-town-children";
    public static final String ABANDON_WARNING = "abandon-warning";
    public static final String ADJUST_ACCRUED_BLOCKS_SUCCESS = "adjust-accrued-blocks-success";
    public static final String ADJUST_BONUS_BLOCKS_SUCCESS = "adjust-bonus-blocks-success";
    public static final String BANK_DEPOSIT = "bank-deposit";
    public static final String BANK_DEPOSIT_NO_FUNDS = "bank-deposit-no-funds";
    public static final String BANK_INFO = "bank-info";
    public static final String BANK_NO_PERMISSION = "bank-no-permission";
    public static final String BANK_TAX_SYSTEM_DISABLED = "bank-tax-system-disabled";
    public static final String BANK_WITHDRAW = "bank-withdraw";
    public static final String BANK_WITHDRAW_NO_FUNDS = "bank-withdraw-no-funds";
    public static final String BLOCK_CLAIMED = "block-claimed";
    public static final String BLOCK_NOT_CLAIMED = "block-not-claimed";
    public static final String BLOCK_SALE_VALUE = "block-sale-value";
    public static final String COMMAND_BLOCKED = "command-blocked";
    public static final String COMMAND_CUBOID_DISABLED = "command-cuboid-disabled";
    public static final String COMMAND_CUBOID_ENABLED = "command-cuboid-enabled";
    public static final String COMMAND_INHERIT = "command-inherit";
    public static final String COMMAND_INVALID_CLAIM = "command-invalid-claim";
    public static final String COMMAND_INVALID_PLAYER_GROUP = "command-invalid-player-group";
    public static final String COMMAND_INVALID_GROUP = "command-invalid-group";
    public static final String COMMAND_INVALID_PLAYER = "command-invalid-player";
    public static final String COMMAND_OPTION_EXCEEDS_ADMIN = "command-option-exceeds-admin";
    public static final String CREATE_CUBOID_DISABLED = "create-cuboid-disabled";
    public static final String CREATE_CANCEL = "create-cancel";
    public static final String CREATE_FAILED_CLAIM_LIMIT = "create-failed-claim-limit";
    public static final String CREATE_INSUFFICIENT_BLOCKS_2D = "create-insufficient-blocks-2d";
    public static final String CREATE_INSUFFICIENT_BLOCKS_3D = "create-insufficient-blocks-3d";
    public static final String CREATE_OVERLAP = "create-overlap";
    public static final String CREATE_OVERLAP_PLAYER = "create-overlap-player";
    public static final String CREATE_OVERLAP_SHORT = "create-overlap-short";
    public static final String CREATE_SUBDIVISION_FAIL = "create-subdivision-fail";
    public static final String CREATE_SUBDIVISION_ONLY = "create-subdivision-only";
    public static final String CREATE_SUCCESS = "create-success";
    public static final String CREATE_WORLDEDIT_MISSING = "create-worldedit-missing";
    public static final String CLAIM_ABOVE_LEVEL = "claim-above-level";
    public static final String CLAIM_AUTOMATIC_NOTIFICATION = "claim-automatic-notification";
    public static final String CLAIM_BELOW_LEVEL = "claim-below-level";
    public static final String CLAIM_CHEST_CONFIRMATION = "claim-chest-confirmation";
    public static final String CLAIM_CHEST_OUTSIDE_LEVEL = "claim-chest-outside-level";
    public static final String CLAIM_CHILDREN_WARNING = "claim-children-warning";
    public static final String CLAIM_CONTEXT_NOT_FOUND = "claim-context-not-found";
    public static final String CLAIM_DISABLED_WORLD = "claim-disabled-world";
    public static final String CLAIM_FAREWELL = "claim-farewell";
    public static final String CLAIM_FAREWELL_CLEAR = "claim-farewell-clear";
    public static final String CLAIM_FAREWELL_INVALID = "claim-farewell-invalid";
    public static final String CLAIM_GREETING = "claim-greeting";
    public static final String CLAIM_GREETING_CLEAR = "claim-greeting-clear";
    public static final String CLAIM_IGNORE = "claim-ignore";
    public static final String CLAIM_LAST_ACTIVE = "claim-last-active";
    public static final String CLAIM_NAME = "claim-name";
    public static final String CLAIM_NO_CLAIMS = "claim-no-claims";
    public static final String CLAIM_NOT_FOUND = "claim-not-found";
    public static final String CLAIM_NOT_YOURS = "claim-not-yours";
    public static final String CLAIM_OWNER_ALREADY = "claim-owner-already";
    public static final String CLAIM_OWNER_ONLY = "claim-owner-only";
    public static final String CLAIM_PROTECTED_ENTITY = "claim-protected-entity";
    public static final String CLAIM_RESPECTING = "claim-respecting";
    public static final String CLAIM_RESTORE_SUCCESS = "claim-restore-success";
    public static final String CLAIM_RESTORE_NATURE_ACTIVATE = "claim-restore-nature-activate";
    public static final String CLAIM_SHOW_NEARBY = "claim-show-nearby";
    public static final String CLAIM_SIZE_MIN = "claim-size-min";
    public static final String CLAIM_SIZE_MAX = "claim-size-max";
    public static final String CLAIM_SIZE_NEED_BLOCKS_2D = "claim-size-need-blocks-2d";
    public static final String CLAIM_SIZE_NEED_BLOCKS_3D = "claim-size-need-blocks-3d";
    public static final String CLAIM_SIZE_TOO_SMALL = "size-too-small";
    public static final String CLAIM_NO_SET_HOME = "claim-no-set-home";
    public static final String CLAIM_START = "claim-start";
    public static final String CLAIM_TOO_FAR = "claim-too-far";
    public static final String CLAIM_TRANSFER_EXCEEDS_LIMIT = "claim-transfer-exceeds-limit";
    public static final String CLAIM_TRANSFER_SUCCESS = "claim-transfer-success";
    public static final String CLAIM_TYPE_NOT_FOUND = "claim-type-not-found";
    public static final String DELETE_ALL_ADMIN_SUCCESS = "delete-all-admin-success";
    public static final String DELETE_ALL_ADMIN_WARNING = "delete-all-admin-warning";
    public static final String DELETE_ALL_SUCCESS = "delete-all-success";
    public static final String DELETE_ALL_WARNING = "delete-all-warning";
    public static final String DELETE_CLAIM = "delete-claim";
    public static final String ECONOMY_BLOCK_NOT_AVAILABLE = "economy-block-not-available";
    public static final String ECONOMY_BLOCK_ONLY_BUY = "economy-block-only-buy";
    public static final String ECONOMY_BLOCK_ONLY_SELL = "economy-block-only-sell";
    public static final String ECONOMY_BLOCK_PURCHASE_CONFIRMATION = "economy-block-purchase-confirmation";
    public static final String ECONOMY_BLOCK_PURCHASE_COST = "economy-block-purchase-cost";
    public static final String ECONOMY_BLOCK_PURCHASE_LIMIT = "economy-block-purchase-limit";
    public static final String ECONOMY_BLOCK_SALE_CONFIRMATION = "economy-block-sale-confirmation";
    public static final String ECONOMY_BLOCK_SELL_ERROR = "economy-block-sell-error";
    public static final String ECONOMY_BLOCK_BUY_INVALID = "economy-block-buy-invalid";
    public static final String ECONOMY_BUY_SELL_DISABLED = "economy-buy-sell-disabled";
    public static final String ECONOMY_CLAIM_ABANDON_SUCCESS = "economy-claim-abandon-success";
    public static final String ECONOMY_CLAIM_BUY_CONFIRMATION = "economy-claim-buy-confirmation";
    public static final String ECONOMY_CLAIM_BUY_CONFIRMED = "economy-claim-buy-confirmed";
    public static final String ECONOMY_CLAIM_BUY_NOT_ENOUGH_FUNDS = "economy-claim-buy-not-enough-funds";
    public static final String ECONOMY_CLAIM_NOT_FOR_SALE = "economy-claim-not-for-sale";
    public static final String ECONOMY_CLAIM_SALE_CANCELLED = "economy-claim-sale-cancelled";
    public static final String ECONOMY_CLAIM_SALE_CONFIRMATION = "economy-claim-sale-confirmation";
    public static final String ECONOMY_CLAIM_SALE_CONFIRMED = "economy-claim-sale-confirmed";
    public static final String ECONOMY_CLAIM_SALE_INVALID_PRICE = "economy-claim-sale-invalid-price";
    public static final String ECONOMY_CLAIM_SOLD = "economy-claim-sold";
    public static final String ECONOMY_NOT_ENOUGH_FUNDS = "economy-not-enough-funds";
    public static final String ECONOMY_NOT_INSTALLED = "economy-not-installed";
    public static final String ECONOMY_PLAYER_NOT_FOUND = "economy-player-not-found";
    public static final String ECONOMY_VIRTUAL_NOT_SUPPORTED = "economy-virtual-not-supported";
    public static final String ECONOMY_WITHDRAW_ERROR = "economy-withdraw-error";
    public static final String FLAG_INVALID_CONTEXT = "flag-invalid-context";
    public static final String FLAG_INVALID_META = "flag-invalid-meta";
    public static final String FLAG_INVALID_TARGET = "flag-invalid-target";
    public static final String FLAG_OVERRIDDEN = "flag-overridden";
    public static final String FLAG_OVERRIDE_NOT_SUPPORTED = "flag-override-not-supported";
    public static final String FLAG_RESET_SUCCESS = "flag-reset-success";
    public static final String MODE_ADMIN = "mode-admin";
    public static final String MODE_BASIC = "mode-basic";
    public static final String MODE_SUBDIVISION = "mode-subdivision";
    public static final String MODE_TOWN = "mode-town";
    public static final String OWNER_ADMIN = "owner-admin";
    public static final String PERMISSION_ACCESS = "permission-access";
    public static final String PERMISSION_ASSIGN_WITHOUT_HAVING = "permission-assign-without-having";
    public static final String PERMISSION_BUILD = "permission-build";
    public static final String PERMISSION_BUILD_NEAR_CLAIM = "permission-build-near-claim";
    public static final String PERMISSION_CLAIM_CREATE = "permission-claim-create";
    public static final String PERMISSION_CLAIM_DELETE = "permission-claim-delete";
    public static final String PERMISSION_CLAIM_ENTER = "permission-claim-enter";
    public static final String PERMISSION_CLAIM_EXIT = "permission-claim-exit";
    public static final String PERMISSION_CLAIM_IGNORE = "permission-claim-ignore";
    public static final String PERMISSION_CLAIM_LIST = "permission-claim-list";
    public static final String PERMISSION_CLAIM_MANAGE = "permission-claim-manage";
    public static final String PERMISSION_CLAIM_RESET_FLAGS = "permission-claim-reset-flags";
    public static final String PERMISSION_CLAIM_RESET_FLAGS_SELF = "permission-claim-reset-flags-self";
    public static final String PERMISSION_CLAIM_RESIZE = "permission-claim-resize";
    public static final String PERMISSION_CLAIM_SALE = "permission-claim-sale";
    public static final String PERMISSION_CLAIM_TRANSFER_ADMIN = "permission-claim-transfer-admin";
    public static final String PERMISSION_CLEAR = "permission-clear";
    public static final String PERMISSION_CLEAR_ALL = "permission-clear-all";
    public static final String PERMISSION_COMMAND_TRUST = "permission-command-trust";
    public static final String PERMISSION_CUBOID = "permission-cuboid";
    public static final String PERMISSION_EDIT_CLAIM = "permission-edit-claim";
    public static final String PERMISSION_FIRE_SPREAD = "permission-fire-spread";
    public static final String PERMISSION_FLAG_DEFAULTS = "permission-flag-defaults";
    public static final String PERMISSION_FLAG_OVERRIDES = "permission-flag-overrides";
    public static final String PERMISSION_FLAG_USE = "permission-flag-use";
    public static final String PERMISSION_FLOW_LIQUID = "permission-flow-liquid";
    public static final String PERMISSION_GLOBAL_OPTION = "permission-global-option";
    public static final String PERMISSION_GRANT = "permission-grant";
    public static final String PERMISSION_GROUP_OPTION = "permission-group-option";
    public static final String PERMISSION_INTERACT_BLOCK = "permission-interact-block";
    public static final String PERMISSION_INTERACT_ENTITY = "permission-interact-entity";
    public static final String PERMISSION_INTERACT_ITEM = "permission-interact-item";
    public static final String PERMISSION_INTERACT_ITEM_BLOCK = "permission-interact-item-block";
    public static final String PERMISSION_INTERACT_ITEM_ENTITY = "permission-interact-item-entity";
    public static final String PERMISSION_INVENTORY_OPEN = "permission-inventory-open";
    public static final String PERMISSION_ITEM_DROP = "permission-item-drop";
    public static final String PERMISSION_ITEM_USE = "permission-item-use";
    public static final String PERMISSION_OVERRIDE_DENY = "permission-override-deny";
    public static final String PERMISSION_PLAYER_ADMIN_FLAGS = "permission-player-admin-flags";
    public static final String PERMISSION_PLAYER_OPTION = "permission-player-option";
    public static final String PERMISSION_PORTAL_ENTER = "permission-portal-enter";
    public static final String PERMISSION_PORTAL_EXIT = "permission-portal-exit";
    public static final String PERMISSION_PROTECTED_PORTAL = "permission-protected-portal";
    public static final String PERMISSION_TRUST = "permission-trust";
    public static final String PERMISSION_VISUAL_CLAIMS_NEARBY = "permission-visual-claims-nearby";
    public static final String PLAYER_REMAINING_BLOCKS_2D = "player-remaining-blocks-2d";
    public static final String PLAYER_REMAINING_BLOCKS_3D = "player-remaining-blocks-3d";
    public static final String PLUGIN_RELOAD = "plugin-reload";
    public static final String PLUGIN_EVENT_CANCEL = "plugin-event-cancel";
    public static final String RESIZE_OVERLAP = "resize-overlap";
    public static final String RESIZE_OVERLAP_SUBDIVISION = "resize-overlap-subdivision";
    public static final String RESIZE_SAME_LOCATION = "resize-same-location";
    public static final String RESIZE_START = "resize-start";
    public static final String RESIZE_SUCCESS_2D = "resize-success-2d";
    public static final String RESIZE_SUCCESS_3D = "resize-success-3d";
    public static final String SCHEMATIC_RESTORE_CONFIRMED = "schematic-restore-confirmed";
    public static final String SPAWN_NOT_SET = "spawn-not-set";
    public static final String SPAWN_SET_SUCCESS = "spawn-set-success";
    public static final String SPAWN_TELEPORT = "spawn-teleport";
    public static final String TAX_CLAIM_EXPIRED = "tax-claim-expired";
    public static final String TAX_CLAIM_PAID_BALANCE = "tax-claim-paid-balance";
    public static final String TAX_CLAIM_PAID_PARTIAL = "tax-claim-paid-partial";
    public static final String TAX_INFO = "tax-info";
    public static final String TAX_PAST_DUE = "tax-past-due";
    public static final String TOWN_CHAT_DISABLED = "town-chat-disabled";
    public static final String TOWN_CHAT_ENABLED = "town-chat-enabled";
    public static final String TOWN_CREATE_NOT_ENOUGH_FUNDS = "town-create-not-enough-funds";
    public static final String TOWN_NAME = "town-name";
    public static final String TOWN_NOT_FOUND = "town-not-found";
    public static final String TOWN_NOT_IN = "town-not-in";
    public static final String TOWN_OWNER = "town-owner";
    public static final String TOWN_TAG = "town-tag";
    public static final String TOWN_TAG_CLEAR = "town-tag-clear";
    public static final String TOWN_TAX_NO_CLAIMS = "town-tax-no-claims";
    public static final String TRUST_ALREADY_HAS = "trust-already-has";
    public static final String TRUST_CURRENT_CLAIM = "trust-current-claim";
    public static final String TRUST_INDIVIDUAL_ALL_CLAIMS = "trust-individual-all-claims";
    public static final String TRUST_INVALID = "trust-invalid";
    public static final String TRUST_LIST_HEADER = "trust-list-header";
    public static final String TRUST_NO_CLAIMS = "trust-no-claims";
    public static final String TRUST_SELF = "trust-self";
    public static final String TUTORIAL_CLAIM_BASIC = "tutorial-claim-basic";
    public static final String UNTRUST_INDIVIDUAL_ALL_CLAIMS = "untrust-individual-all-claims";
    public static final String UNTRUST_INDIVIDUAL_SINGLE_CLAIM = "untrust-individual-single-claim";
    public static final String UNTRUST_NO_CLAIMS = "untrust-no-claims";
    public static final String UNTRUST_OWNER = "untrust-owner";
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
