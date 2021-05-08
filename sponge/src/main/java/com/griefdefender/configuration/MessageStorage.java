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

public class MessageStorage {

    private HoconConfigurationLoader loader;
    private CommentedConfigurationNode root = SimpleCommentedConfigurationNode.root(ConfigurationOptions.defaults());
    private ObjectMapper<MessageDataConfig>.BoundInstance configMapper;
    private MessageDataConfig configBase;
    public static MessageDataConfig MESSAGE_DATA;

    // descriptions
    public static final String DESCRIPTION_ABANDON_ALL = "abandon-all";
    public static final String DESCRIPTION_ABANDON_CLAIM = "abandon-claim";
    public static final String DESCRIPTION_ABANDON_TOP = "abandon-top";
    public static final String DESCRIPTION_ABANDON_WORLD = "abandon-world";
    public static final String DESCRIPTION_BUY_BLOCKS = "buy-blocks";
    public static final String DESCRIPTION_BUY_CLAIM = "buy-claim";
    public static final String DESCRIPTION_CALLBACK = "callback";
    public static final String DESCRIPTION_CLAIM_BAN = "claim-ban";
    public static final String DESCRIPTION_CLAIM_BANK = "claim-bank";
    public static final String DESCRIPTION_CLAIM_CLEAR = "claim-clear";
    public static final String DESCRIPTION_CLAIM_CONTRACT = "claim-contract";
    public static final String DESCRIPTION_CLAIM_CREATE = "claim-create";
    public static final String DESCRIPTION_CLAIM_DEBUG = "claim-debug";
    public static final String DESCRIPTION_CLAIM_EXPAND = "claim-expand";
    public static final String DESCRIPTION_CLAIM_FAREWELL = "claim-farewell";
    public static final String DESCRIPTION_CLAIM_GREETING = "claim-greeting";
    public static final String DESCRIPTION_CLAIM_IGNORE = "claim-ignore";
    public static final String DESCRIPTION_CLAIM_INFO = "claim-info";
    public static final String DESCRIPTION_CLAIM_INHERIT = "claim-inherit";
    public static final String DESCRIPTION_CLAIM_INVESTIGATE = "claim-investigate";
    public static final String DESCRIPTION_CLAIM_LIST = "claim-list";
    public static final String DESCRIPTION_CLAIM_NAME = "claim-name";
    public static final String DESCRIPTION_CLAIM_RENT = "claim-rent";
    public static final String DESCRIPTION_CLAIM_RESERVE = "claim-reserve";
    public static final String DESCRIPTION_CLAIM_RESTORE = "claim-restore";
    public static final String DESCRIPTION_CLAIM_SETSPAWN = "claim-setspawn";
    public static final String DESCRIPTION_CLAIM_SPAWN = "claim-spawn";
    public static final String DESCRIPTION_CLAIM_TAX = "claim-tax";
    public static final String DESCRIPTION_CLAIM_TOOL = "claim-tool";
    public static final String DESCRIPTION_CLAIM_TRANSFER = "claim-transfer";
    public static final String DESCRIPTION_CLAIM_UNBAN = "claim-unban";
    public static final String DESCRIPTION_CLAIM_WORLDEDIT = "claim-worldedit";
    public static final String DESCRIPTION_CONFIRM = "confirm";
    public static final String DESCRIPTION_CUBOID = "cuboid";
    public static final String DESCRIPTION_DEBUG = "debug";
    public static final String DESCRIPTION_DELETE_ALL = "delete-all";
    public static final String DESCRIPTION_DELETE_ALL_ADMIN = "delete-all-admin";
    public static final String DESCRIPTION_DELETE_CLAIM = "delete-claim";
    public static final String DESCRIPTION_DELETE_TOP = "delete-top";
    public static final String DESCRIPTION_ECONOMY_BLOCK_TRANSFER = "economy-block-transfer";
    public static final String DESCRIPTION_FLAG_CLAIM = "flag-claim";
    public static final String DESCRIPTION_FLAG_GROUP = "flag-group";
    public static final String DESCRIPTION_FLAG_PLAYER = "flag-player";
    public static final String DESCRIPTION_FLAG_RESET = "flag-reset";
    public static final String DESCRIPTION_HELP = "help";
    public static final String DESCRIPTION_MODE_ADMIN = "mode-admin";
    public static final String DESCRIPTION_MODE_BASIC = "mode-basic";
    public static final String DESCRIPTION_MODE_CLAIM = "mode-claim";
    public static final String DESCRIPTION_MODE_NATURE = "mode-nature";
    public static final String DESCRIPTION_MODE_SUBDIVISION = "mode-subdivision";
    public static final String DESCRIPTION_MODE_TOWN = "mode-town";
    public static final String DESCRIPTION_OPTION_CLAIM = "option-claim";
    public static final String DESCRIPTION_OPTION_GROUP = "option-group";
    public static final String DESCRIPTION_OPTION_PLAYER = "option-player";
    public static final String DESCRIPTION_PERMISSION_GROUP = "permission-group";
    public static final String DESCRIPTION_PERMISSION_PLAYER = "permission-player";
    public static final String DESCRIPTION_PLAYER_ADJUST_BONUS_BLOCKS = "player-adjust-bonus-blocks";
    public static final String DESCRIPTION_PLAYER_ADJUST_BONUS_BLOCKS_ALL = "player-adjust-bonus-blocks-all";
    public static final String DESCRIPTION_PLAYER_GIVE_BLOCKS = "player-give-blocks";
    public static final String DESCRIPTION_PLAYER_GIVE_PET = "player-give-pet";
    public static final String DESCRIPTION_PLAYER_INFO = "player-info";
    public static final String DESCRIPTION_PLAYER_SET_ACCRUED_BLOCKS = "player-set-accrued-blocks";
    public static final String DESCRIPTION_PLAYER_UNLOCK_DROPS = "player-unlock-drops";
    public static final String DESCRIPTION_RELOAD = "reload";
    public static final String DESCRIPTION_SCHEMATIC = "schematic";
    public static final String DESCRIPTION_SELL_BLOCKS = "sell-blocks";
    public static final String DESCRIPTION_SELL_CLAIM = "sell-claim";
    public static final String DESCRIPTION_TOWN_CHAT = "town-chat";
    public static final String DESCRIPTION_TOWN_TAG = "town-tag";
    public static final String DESCRIPTION_TRAPPED = "trapped";
    public static final String DESCRIPTION_TRUST_ACCESS = "trust-access";
    public static final String DESCRIPTION_TRUST_CONTAINER = "trust-container";
    public static final String DESCRIPTION_TRUST_GROUP = "trust-group";
    public static final String DESCRIPTION_TRUST_GROUP_ALL = "trust-group-all";
    public static final String DESCRIPTION_TRUST_LIST = "trust-list";
    public static final String DESCRIPTION_TRUST_PLAYER = "trust-player";
    public static final String DESCRIPTION_TRUST_PLAYER_ALL = "trust-player-all";
    public static final String DESCRIPTION_UNTRUST_GROUP = "untrust-group";
    public static final String DESCRIPTION_UNTRUST_GROUP_ALL = "untrust-group-all";
    public static final String DESCRIPTION_UNTRUST_PLAYER = "untrust-player";
    public static final String DESCRIPTION_UNTRUST_PLAYER_ALL = "untrust-player-all";
    public static final String DESCRIPTION_VERSION = "version";

    // messages with parameters
    public static final String ABANDON_ALL_WARNING_WORLD = "abandon-all-warning-world";
    public static final String ABANDON_CLAIM_DELAY_WARNING = "abandon-claim-delay-warning";
    public static final String ABANDON_FAILED = "abandon-failed";
    public static final String ABANDON_OTHER_SUCCESS = "abandon-other-success";
    public static final String ABANDON_SUCCESS = "abandon-success";
    public static final String ABANDON_SUCCESS_WORLD = "abandon-success";
    public static final String ABANDON_WORLD_SUCCESS = "abandon-world-success";
    public static final String ABANDON_WORLD_WARNING = "abandon-world-warning";
    public static final String ADJUST_ACCRUED_BLOCKS_SUCCESS = "adjust-accrued-blocks-success";
    public static final String ADJUST_BONUS_BLOCKS_SUCCESS = "adjust-bonus-blocks-success";
    public static final String ADJUST_BONUS_BLOCKS_ALL_SUCCESS = "adjust-bonus-blocks-all-success";
    public static final String ADJUST_BONUS_BLOCKS_ALL_WARNING = "adjust-bonus-blocks-all-warning";
    public static final String BANK_DEPOSIT = "bank-deposit";
    public static final String BANK_INFO = "bank-info";
    public static final String BANK_NO_PERMISSION = "bank-no-permission";
    public static final String BANK_WITHDRAW = "bank-withdraw";
    public static final String BANK_WITHDRAW_NO_FUNDS = "bank-withdraw-no-funds";
    public static final String BLOCK_CLAIMED = "block-claimed";
    public static final String BLOCK_NOT_CLAIMED = "block-not-claimed";
    public static final String BLOCK_SALE_VALUE = "block-sale-value";
    public static final String CLAIM_ABOVE_LEVEL = "claim-above-level";
    public static final String CLAIM_ACTION_NOT_AVAILABLE = "claim-action-not-available";
    public static final String CLAIM_BELOW_LEVEL = "claim-below-level";
    public static final String CLAIM_CHEST_OUTSIDE_LEVEL = "claim-chest-outside-level";
    public static final String CLAIM_CONTEXT_NOT_FOUND = "claim-context-not-found";
    public static final String CLAIM_EXPIRED_INACTIVITY = "claim-expired-inactivity";
    public static final String CLAIM_FAREWELL = "claim-farewell";
    public static final String CLAIM_FAREWELL_INVALID = "claim-farewell-invalid";
    public static final String CLAIM_GREETING = "claim-greeting";
    public static final String CLAIM_LAST_ACTIVE = "claim-last-active";
    public static final String CLAIM_MODE_START = "claim-mode-start";
    public static final String CLAIM_NAME = "claim-name";
    public static final String CLAIM_OWNER_ONLY = "claim-owner-only";
    public static final String CLAIM_PREFIX_ENTER = "claim-prefix-enter";
    public static final String CLAIM_PREFIX_EXIT = "claim-prefix-enter";
    public static final String CLAIM_PROTECTED_ENTITY = "claim-protected-entity";
    public static final String CLAIM_RESERVE_ADD = "claim-reserve-add";
    public static final String CLAIM_SHOW_NEARBY = "claim-show-nearby";
    public static final String CLAIM_SIZE_MIN = "claim-size-min";
    public static final String CLAIM_SIZE_MAX = "claim-size-max";
    public static final String CLAIM_SIZE_NEED_BLOCKS_2D = "claim-size-need-blocks-2d";
    public static final String CLAIM_SIZE_NEED_BLOCKS_3D = "claim-size-need-blocks-3d";
    public static final String CLAIM_SIZE_TOO_SMALL = "size-too-small";
    public static final String CLAIM_NO_SET_HOME = "claim-no-set-home";
    public static final String CLAIM_START = "claim-start";
    public static final String CLAIM_TRANSFER_EXCEEDS_LIMIT = "claim-transfer-exceeds-limit";
    public static final String CLAIM_TRANSFER_SUCCESS = "claim-transfer-success";
    public static final String CLAIM_TYPE_NOT_FOUND = "claim-type-not-found";
    public static final String CLAIMINFO_UI_CLICK_CHANGE_CLAIM = "claiminfo-ui-click-change-claim";
    public static final String CLAIMINFO_UI_TELEPORT_DIRECTION = "claiminfo-ui-teleport-direction";
    public static final String CLAIMLIST_UI_CLICK_TELEPORT_TARGET = "claimlist-ui-click-teleport-target";
    public static final String CLAIMLIST_UI_CLICK_TOGGLE_VALUE = "claimlist-ui-click-toggle-value";
    public static final String COMMAND_BLOCKED = "command-blocked";
    public static final String COMMAND_CLAIMBAN_SUCCESS_BLOCK = "command-claimban-success-block";
    public static final String COMMAND_CLAIMBAN_SUCCESS_ENTITY = "command-claimban-success-entity";
    public static final String COMMAND_CLAIMBAN_SUCCESS_ITEM = "command-claimban-success-item";
    public static final String COMMAND_CLAIMCLEAR_NO_ENTITIES = "command-claimclear-no-entities";
    public static final String COMMAND_CLAIMNAME_NOT_FOUND = "command-claimname-not-found";
    public static final String COMMAND_CLAIMUNBAN_SUCCESS_BLOCK = "command-claimunban-success-block";
    public static final String COMMAND_CLAIMUNBAN_SUCCESS_ENTITY = "command-claimunban-success-entity";
    public static final String COMMAND_CLAIMUNBAN_SUCCESS_ITEM = "command-claimunban-success-item";
    public static final String COMMAND_EXECUTE_FAILED = "command-execute-failed";
    public static final String COMMAND_GIVEBLOCKS_CONFIRMATION = "command-giveblocks-confirmation";
    public static final String COMMAND_GIVEBLOCKS_CONFIRMED = "command-giveblocks-confirmed";
    public static final String COMMAND_GIVEBLOCKS_NOT_ENOUGH = "command-giveblocks-not-enough";
    public static final String COMMAND_GIVEBLOCKS_RECEIVED = "command-giveblocks-received";
    public static final String COMMAND_INVALID_AMOUNT = "command-invalid-amount";
    public static final String COMMAND_INVALID_CLAIM = "command-invalid-claim";
    public static final String COMMAND_INVALID_GROUP = "command-invalid-group";
    public static final String COMMAND_INVALID_INPUT = "command-invalid-input";
    public static final String COMMAND_INVALID_PLAYER = "command-invalid-player";
    public static final String COMMAND_INVALID_TYPE = "command-invalid-type";
    public static final String COMMAND_OPTION_EXCEEDS_ADMIN = "command-option-exceeds-admin";
    public static final String COMMAND_PET_INVALID = "command-pet-invalid";
    public static final String COMMAND_PLAYER_NOT_FOUND = "command-player-not-found";
    public static final String COMMAND_TRAPPED_CANCEL_COOLDOWN = "command-trapped-cancel-cooldown";
    public static final String COMMAND_TRAPPED_REQUEST = "command-trapped-request";
    public static final String COMMAND_WORLD_NOT_FOUND = "command-world-not-found";
    public static final String CREATE_FAILED_CLAIM_LIMIT = "create-failed-claim-limit";
    public static final String CREATE_FAILED_RESULT = "create-failed-result";
    public static final String CREATE_INSUFFICIENT_BLOCKS_2D = "create-insufficient-blocks-2d";
    public static final String CREATE_INSUFFICIENT_BLOCKS_3D = "create-insufficient-blocks-3d";
    public static final String CREATE_OVERLAP_PLAYER = "create-overlap-player";
    public static final String CREATE_SUCCESS = "create-success";
    public static final String DEBUG_ERROR_UPLOAD = "debug-error-upload";
    public static final String DELETE_ALL_TYPE_DENY = "delete-all-type-deny";
    public static final String DELETE_ALL_TYPE_SUCCESS = "delete-all-type-success";
    public static final String DELETE_ALL_TYPE_SUCCESS_WORLD = "delete-all-type-success-world";
    public static final String DELETE_ALL_TYPE_WARNING = "delete-all-type-warning";
    public static final String DELETE_ALL_TYPE_WARNING_WORLD = "delete-all-type-warning-world";
    public static final String DELETE_ALL_PLAYER_SUCCESS = "delete-all-player-success";
    public static final String DELETE_ALL_PLAYER_SUCCESS_WORLD = "delete-all-player-success-world";
    public static final String DELETE_ALL_PLAYER_WARNING = "delete-all-player-warning";
    public static final String DELETE_ALL_PLAYER_WARNING_WORLD = "delete-all-player-warning-world";
    public static final String DELETE_CLAIM_SUCCESS = "delete-claim-success";
    public static final String DELETE_CLAIM_WARNING = "delete-claim-warning";
    public static final String ECONOMY_BLOCK_AVAILABLE_PURCHASE_2D = "economy-block-available-purchase-2d";
    public static final String ECONOMY_BLOCK_AVAILABLE_PURCHASE_3D = "economy-block-available-purchase-3d";
    public static final String ECONOMY_BLOCK_PURCHASE_CONFIRMATION = "economy-block-purchase-confirmation";
    public static final String ECONOMY_BLOCK_PURCHASE_COST = "economy-block-purchase-cost";
    public static final String ECONOMY_BLOCK_PURCHASE_LIMIT = "economy-block-purchase-limit";
    public static final String ECONOMY_BLOCK_SALE_CONFIRMATION = "economy-block-sale-confirmation";
    public static final String ECONOMY_BLOCK_SELL_ERROR = "economy-block-sell-error";
    public static final String ECONOMY_BLOCK_TRANSFER_SUCCESS = "economy-block-transfer-success";
    public static final String ECONOMY_CLAIM_ABANDON_SUCCESS = "economy-claim-abandon-success";
    public static final String ECONOMY_CLAIM_ABANDON_SUCCESS_WORLD = "economy-claim-abandon-success-world";
    public static final String ECONOMY_CLAIM_BUY_CANCELLED = "economy-claim-buy-cancelled";
    public static final String ECONOMY_CLAIM_BUY_CONFIRMATION = "economy-claim-buy-confirmation";
    public static final String ECONOMY_CLAIM_BUY_CONFIRMED = "economy-claim-buy-confirmed";
    public static final String ECONOMY_CLAIM_BUY_NOT_ENOUGH_FUNDS = "economy-claim-buy-not-enough-funds";
    public static final String ECONOMY_CLAIM_BUY_TRANSFER_CANCELLED = "economy-claim-buy-transfer-cancelled";
    public static final String ECONOMY_CLAIM_RENT_CANCEL_CONFIRMATION_BALANCE = "economy-claim-rent-cancel-confirmation-balance";
    public static final String ECONOMY_CLAIM_RENT_CONFIRMATION = "economy-claim-rent-confirmation";
    public static final String ECONOMY_CLAIM_RENT_CONFIRMATION_MAX = "economy-claim-rent-confirmation-max";
    public static final String ECONOMY_CLAIM_RENT_CONFIRMATION_MIN = "economy-claim-rent-confirmation-min";
    public static final String ECONOMY_CLAIM_RENT_CONFIRMATION_MIN_MAX = "economy-claim-rent-confirmation-min-max";
    public static final String ECONOMY_CLAIM_RENT_CONFIRMED = "economy-claim-rent-confirmed";
    public static final String ECONOMY_CLAIM_RENT_END = "economy-claim-rent-end";
    public static final String ECONOMY_CLAIM_RENT_END_RESTORE = "economy-claim-rent-end-restore";
    public static final String ECONOMY_CLAIM_RENT_INVALID_PRICE = "economy-claim-rent-invalid-price";
    public static final String ECONOMY_CLAIM_RENT_NOT_ENOUGH_FUNDS = "economy-claim-rent-not-enough-funds";
    public static final String ECONOMY_CLAIM_RENT_PAYMENT_FAILURE = "economy-claim-rent-payment-failure";
    public static final String ECONOMY_CLAIM_RENT_PAYMENT_SUCCESS = "economy-claim-rent-payment-success";
    public static final String ECONOMY_CLAIM_RENT_RESTORE_CANCEL_CONFIRMATION_BALANCE = "economy-claim-rent-restore-cancel-confirmation-balance";
    public static final String ECONOMY_CLAIM_RENTED = "economy-claim-rented";
    public static final String ECONOMY_CLAIM_RENTED_ALREADY = "economy-claim-rented-already";
    public static final String ECONOMY_CLAIM_RENTED_EXPIRED = "economy-claim-rented-expired";
    public static final String ECONOMY_CLAIM_RENTER_CONFIRMATION = "economy-claim-renter-confirmation";
    public static final String ECONOMY_CLAIM_RENTER_CONFIRMATION_MAX = "economy-claim-renter-confirmation-max";
    public static final String ECONOMY_CLAIM_RENTER_CONFIRMATION_MIN = "economy-claim-renter-confirmation-min";
    public static final String ECONOMY_CLAIM_RENTER_CONFIRMATION_MIN_MAX = "economy-claim-renter-confirmation-min-max";
    public static final String ECONOMY_CLAIM_RENTER_CONFIRMED = "economy-claim-renter-confirmed";
    public static final String ECONOMY_CLAIM_RENTER_END = "economy-claim-renter-end";
    public static final String ECONOMY_CLAIM_RENTED_TIME_WARNING = "economy-claim-rented-time-warning";
    public static final String ECONOMY_CLAIM_RENTED_TIME_WARNING_RESTORE_ = "economy-claim-rented-time-warning-restore";
    public static final String ECONOMY_CLAIM_RENTED_NO_CANCEL = "economy-claim-rented-no-cancel";
    public static final String ECONOMY_CLAIM_SALE_CONFIRMATION = "economy-claim-sale-confirmation";
    public static final String ECONOMY_CLAIM_SALE_CONFIRMED = "economy-claim-sale-confirmed";
    public static final String ECONOMY_CLAIM_SALE_INVALID_PRICE = "economy-claim-sale-invalid-price";
    public static final String ECONOMY_CLAIM_SOLD = "economy-claim-sold";
    public static final String ECONOMY_DEPOSIT_ERROR = "economy-deposit-error";
    public static final String ECONOMY_MODE_BLOCK_SALE_CONFIRMATION = "economy-mode-block-sale-confirmation";
    public static final String ECONOMY_MODE_BLOCK_COST_NOT_SET = "economy-mode-block-cost-not-set";
    public static final String ECONOMY_MODE_RESIZE_SUCCESS_2D = "economy-mode-resize-success-2d";
    public static final String ECONOMY_MODE_RESIZE_SUCCESS_3D = "economy-mode-resize-success-3d";
    public static final String ECONOMY_NOT_ENOUGH_FUNDS = "economy-not-enough-funds";
    public static final String ECONOMY_PLAYER_NOT_FOUND = "economy-player-not-found";
    public static final String ECONOMY_SIGN_RENTED_LINE2 = "economy-sign-rented-line2";
    public static final String ECONOMY_SIGN_SOLD_LINE2 = "economy-sign-sold-line2";
    public static final String ECONOMY_WITHDRAW_ERROR = "economy-withdraw-error";
    public static final String FLAG_INVALID_CONTEXT = "flag-invalid-context";
    public static final String FLAG_INVALID_META = "flag-invalid-meta";
    public static final String FLAG_INVALID_TARGET = "flag-invalid-target";
    public static final String FLAG_NOT_ENABLED = "flag-not-enabled";
    public static final String FLAG_NOT_FOUND = "flag-not-found";
    public static final String FLAG_NOT_SET = "flag-not-set";
    public static final String FLAG_OVERRIDDEN = "flag-overridden";
    public static final String FLAG_OVERRIDE_NOT_SUPPORTED = "flag-override-not-supported";
    public static final String FLAG_SET_PERMISSION_TARGET = "flag-set-permission-target";
    public static final String FLAG_UI_CLICK_TOGGLE = "flag-ui-click-toggle";
    public static final String FLAG_UI_INHERIT_PARENT = "flag-ui-inherit-parent";
    public static final String FLAG_UI_OVERRIDE_PERMISSION = "flag-ui-override-permission";
    public static final String OPTION_APPLY_PLAYER_FLY_SPEED = "option-apply-player-fly-speed";
    public static final String OPTION_APPLY_PLAYER_GAMEMODE = "option-apply-player-gamemode";
    public static final String OPTION_APPLY_PLAYER_WALK_SPEED = "option-apply-player-walk-speed";
    public static final String OPTION_APPLY_PLAYER_WEATHER = "option-apply-player-weather";
    public static final String OPTION_APPLY_SPAWN_LIMIT = "option-apply-spawn-limit";
    public static final String OPTION_INVALID_CONTEXT = "option-invalid-context";
    public static final String OPTION_INVALID_TARGET = "option-invalid-target";
    public static final String OPTION_INVALID_VALUE = "option-invalid-value";
    public static final String OPTION_NOT_ENABLED = "option-not-enabled";
    public static final String OPTION_NOT_FOUND = "option-not-found";
    public static final String OPTION_NOT_SET = "option-not-set";
    public static final String OPTION_OVERRIDE_NOT_SUPPORTED = "option-override-not-supported";
    public static final String OPTION_REQUIRES_CONTEXTS = "option-requires-contexts";
    public static final String OPTION_RESET_SUCCESS = "option-reset-success";
    public static final String OPTION_SET_TARGET = "option-set-target";
    public static final String OPTION_UI_CLICK_TOGGLE = "option-ui-click-toggle";
    public static final String OPTION_UI_INHERIT_PARENT = "option-ui-inherit-parent";
    public static final String OPTION_UI_OVERRIDDEN = "option-ui-overridden";
    public static final String PERMISSION_ACCESS = "permission-access";
    public static final String PERMISSION_BAN_BLOCK = "permission-ban-block";
    public static final String PERMISSION_BAN_ENTITY = "permission-ban-entity";
    public static final String PERMISSION_BAN_ITEM = "permission-ban-item";
    public static final String PERMISSION_BUILD = "permission-build";
    public static final String PERMISSION_BUILD_NEAR_CLAIM = "permission-build-near-claim";
    public static final String PERMISSION_CLAIM_DELETE = "permission-claim-delete";
    public static final String PERMISSION_CLAIM_IGNORE = "permission-claim-ignore";
    public static final String PERMISSION_CLAIM_MANAGE = "permission-claim-manage";
    public static final String PERMISSION_CLAIM_RESET_FLAGS = "permission-claim-reset-flags";
    public static final String PERMISSION_INTERACT_BLOCK = "permission-interact-block";
    public static final String PERMISSION_INTERACT_ENTITY = "permission-interact-entity";
    public static final String PERMISSION_INTERACT_ITEM = "permission-interact-item";
    public static final String PERMISSION_INTERACT_ITEM_BLOCK = "permission-interact-item-block";
    public static final String PERMISSION_INTERACT_ITEM_ENTITY = "permission-interact-item-entity";
    public static final String PERMISSION_INVENTORY_OPEN = "permission-inventory-open";
    public static final String PERMISSION_ITEM_DROP = "permission-item-drop";
    public static final String PERMISSION_ITEM_USE = "permission-item-use";
    public static final String PERMISSION_PORTAL_FROM = "permission-portal-from";
    public static final String PERMISSION_PORTAL_TO = "permission-portal-to";
    public static final String PERMISSION_PROTECTED_PORTAL = "permission-protected-portal";
    public static final String PERMISSION_TELEPORT_FROM = "permission-teleport-from";
    public static final String PERMISSION_TELEPORT_TO = "permission-teleport-to";
    public static final String PERMISSION_TRUST = "permission-trust";
    public static final String PLAYER_ACCRUED_BLOCKS_EXCEEDED = "player-accrued-blocks-exceeded";
    public static final String PLAYER_NO_CLAIMS_TO_DELETE = "player-no-claims-to-delete";
    public static final String PLAYER_REMAINING_BLOCKS_2D = "player-remaining-blocks-2d";
    public static final String PLAYER_REMAINING_BLOCKS_3D = "player-remaining-blocks-3d";
    public static final String PLAYERINFO_UI_ABANDON_RETURN_RATIO = "playerinfo-ui-abandon-return-ratio";
    public static final String PLAYERINFO_UI_BLOCK_ACCRUED = "playerinfo-ui-block-accrued";
    public static final String PLAYERINFO_UI_BLOCK_BONUS = "playerinfo-ui-block-bonus";
    public static final String PLAYERINFO_UI_CLAIM_CREATION_LIMIT = "playerinfo-ui-claim-creation-limit";
    public static final String PLAYERINFO_UI_CLAIM_LEVEL = "playerinfo-ui-claim-level";
    public static final String PLAYERINFO_UI_CLAIM_SIZE_LIMIT = "playerinfo-ui-claim-size-limit";
    public static final String PLAYERINFO_UI_BLOCK_INITIAL = "playerinfo-ui-block-initial";
    public static final String PLAYERINFO_UI_BLOCK_MAX_ACCRUED = "playerinfo-ui-block-max-accrued";
    public static final String PLAYERINFO_UI_BLOCK_REMAINING = "playerinfo-ui-block-remaining";
    public static final String PLAYERINFO_UI_BLOCK_TOTAL = "playerinfo-ui-block-total";
    public static final String PLAYERINFO_UI_CHUNK_TOTAL = "playerinfo-ui-chunk-total";
    public static final String PLAYERINFO_UI_CLAIM_TOTAL = "playerinfo-ui-claim-total";
    public static final String PLAYERINFO_UI_ECONOMY_BLOCK_AVAILABLE_PURCHASE = "playerinfo-ui-economy-block-available-purchase";
    public static final String PLAYERINFO_UI_ECONOMY_BLOCK_COST = "playerinfo-ui-economy-block-cost";
    public static final String PLAYERINFO_UI_ECONOMY_BLOCK_SELL_RETURN = "playerinfo-ui-economy-block-sell-return";
    public static final String PLAYERINFO_UI_LAST_ACTIVE = "playerinfo-ui-last-active";
    public static final String PLAYERINFO_UI_TAX_CURRENT_RATE = "playerinfo-ui-tax-current-rate";
    public static final String PLAYERINFO_UI_TAX_GLOBAL_CLAIM_RATE = "playerinfo-ui-tax-global-claim-rate";
    public static final String PLAYERINFO_UI_TAX_GLOBAL_TOWN_RATE = "playerinfo-ui-tax-global-town-rate";
    public static final String PLAYERINFO_UI_TAX_TOTAL = "playerinfo-ui-tax-total";
    public static final String PLAYERINFO_UI_TITLE = "playerinfo-ui-title";
    public static final String PLAYERINFO_UI_UUID = "playerinfo-ui-uuid";
    public static final String PLAYERINFO_UI_WORLD = "playerinfo-ui-world";
    public static final String PLUGIN_COMMAND_NOT_FOUND = "plugin-command-not-found";
    public static final String PLUGIN_NOT_FOUND = "plugin-not-found";
    public static final String PVP_IN_COMBAT_NOT_ALLOWED = "pvp-in-combat-not-allowed";
    public static final String REGISTRY_BLOCK_NOT_FOUND = "registry-type-not-found";
    public static final String REGISTRY_ENTITY_NOT_FOUND = "registry-entity-not-found";
    public static final String REGISTRY_ITEM_NOT_FOUND = "registry-item-not-found";
    public static final String RESIZE_SUCCESS_2D = "resize-success-2d";
    public static final String RESIZE_SUCCESS_3D = "resize-success-3d";
    public static final String RESULT_TYPE_CHANGE_DENY = "result-type-change-deny";
    public static final String RESULT_TYPE_CHANGE_NOT_ADMIN = "result-type-change-not-admin";
    public static final String RESULT_TYPE_CHILD_SAME = "result-type-child-same";
    public static final String RESULT_TYPE_CREATE_DENY = "result-type-create-deny";
    public static final String RESULT_TYPE_NO_CHILDREN = "result-type-no-children";
    public static final String RESULT_TYPE_ONLY_SUBDIVISION = "result-type-only-subdivision";
    public static final String RESULT_TYPE_REQUIRES_OWNER = "result-type-requires-owner";
    public static final String SCHEMATIC_ABANDON_ALL_RESTORE_WARNING_WORLD = "schematic-abandon-all-restore-warning-world";
    public static final String SCHEMATIC_DELETED = "schematic-deleted";
    public static final String SCHEMATIC_NONE = "schematic-none";
    public static final String SCHEMATIC_RESTORE_CLICK = "schematic-restore-click"; 
    public static final String SCHEMATIC_RESTORE_CONFIRMATION = "schematic-restore-confirmation";
    public static final String SCHEMATIC_RESTORE_CONFIRMED = "schematic-restore-confirmed";
    public static final String SPAWN_SET_SUCCESS = "spawn-set-success";
    public static final String SPAWN_TELEPORT = "spawn-teleport";
    public static final String TAX_EXPIRED = "tax-expired";
    public static final String TAX_EXPIRED_BANK = "tax-expired-bank";
    public static final String TAX_PAID_BALANCE = "tax-paid-balance";
    public static final String TAX_PAID_PARTIAL = "tax-paid-partial";
    public static final String TAX_INFO = "tax-info";
    public static final String TAX_PAST_DUE = "tax-past-due";
    public static final String TAX_PAY_NO_FUNDS = "tax-pay-no-funds";
    public static final String TELEPORT_CONFIRM = "teleport-confirm";
    public static final String TELEPORT_DELAY_NOTICE = "teleport-delay-notice";
    public static final String TOOL_NOT_EQUIPPED = "tool-not-equipped";
    public static final String TOWN_CREATE_NOT_ENOUGH_FUNDS = "town-create-not-enough-funds";
    public static final String TOWN_NAME = "town-name";
    public static final String TOWN_TAG = "town-tag";
    public static final String TRUST_ALREADY_HAS = "trust-already-has";
    public static final String TRUST_GRANT = "trust-grant";
    public static final String TRUST_INDIVIDUAL_ALL_CLAIMS = "trust-individual-all-claims";
    public static final String TRUST_PLUGIN_CANCEL = "trust-plugin-cancel";
    public static final String TUTORIAL_CLAIM_BASIC = "tutorial-claim-basic";
    public static final String UI_CLICK_ADD_TARGET = "ui-click-add-target";
    public static final String UI_CLICK_FILTER_TYPE = "ui-click-filter-type";
    public static final String UI_CLICK_RETURN_COMMAND = "ui-click-return-command";
    public static final String UI_CLICK_VIEW = "ui-click-view";
    public static final String UNTRUST_INDIVIDUAL_ALL_CLAIMS = "untrust-individual-all-claims";
    public static final String UNTRUST_INDIVIDUAL_SINGLE_CLAIM = "untrust-individual-single-claim";
    public static final String UNTRUST_OWNER = "untrust-owner";
    public static final String VISUAL_UPDATE_IN_PROGRESS = "visual-update-in-progress";

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

            if (reload()) {
                save();
            }
        } catch (Exception e) {
            GriefDefenderPlugin.getInstance().getLogger().error("Failed to initialize configuration", e);
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
            GriefDefenderPlugin.getInstance().getLogger().error("Failed to save configuration", e);
        }
    }

    public boolean reload() {
        try {
            this.root = this.loader.load(ConfigurationOptions.defaults());
            this.configBase = this.configMapper.populate(this.root.getNode(GriefDefenderPlugin.MOD_ID));
            MESSAGE_DATA = this.configBase;
        } catch (Exception e) {
            GriefDefenderPlugin.getInstance().getLogger().error("Failed to load configuration", e);
            return false;
        }
        return true;
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
