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
package com.griefdefender.permission;

import com.griefdefender.api.claim.TrustType;
import com.griefdefender.api.claim.TrustTypes;

public class GDPermissions {

    // Claims
    public static final String COMMAND_ABANDON_BASIC = "griefdefender.user.claim.command.abandon.basic";
    public static final String COMMAND_ABANDON_TOWN = "griefdefender.user.claim.command.abandon.town";
    public static final String COMMAND_ABANDON_ALL_CLAIMS = "griefdefender.user.claim.command.abandon-all";
    public static final String COMMAND_ABANDON_TOP_LEVEL_CLAIM = "griefdefender.user.claim.command.abandon-top-level";
    public static final String COMMAND_CUBOID_CLAIMS = "griefdefender.user.claim.command.cuboid";
    public static final String COMMAND_CLAIM_LIST_BASE = "griefdefender.user.claim.command.list.base";
    public static final String COMMAND_CLAIM_LIST_OTHERS = "griefdefender.user.claim.command.list.others";
    public static final String COMMAND_BASIC_MODE = "griefdefender.user.claim.command.basic-mode";
    public static final String COMMAND_GIVE_BLOCKS = "griefdefender.user.claim.command.give.blocks";
    public static final String COMMAND_GIVE_BOOK = "griefdefender.user.claim.command.give.book";
    public static final String COMMAND_GIVE_PET = "griefdefender.user.claim.command.give.pet";
    public static final String COMMAND_CLAIM_BANK = "griefdefender.user.claim.command.bank";
    public static final String COMMAND_CLAIM_BUY = "griefdefender.user.claim.command.buy";
    public static final String COMMAND_CLAIM_CONTRACT = "griefdefender.user.claim.command.contract";
    public static final String COMMAND_CLAIM_EXPAND = "griefdefender.user.claim.command.expand";
    public static final String COMMAND_CLAIM_INFO_OTHERS = "griefdefender.user.claim.command.info.others";
    public static final String COMMAND_CLAIM_INFO_BASE = "griefdefender.user.claim.command.info.base";
    public static final String COMMAND_CLAIM_INFO_TELEPORT_OTHERS = "griefdefender.user.claim.command.info.teleport.others";
    public static final String COMMAND_CLAIM_INFO_TELEPORT_BASE = "griefdefender.user.claim.command.info.teleport.base";
    public static final String COMMAND_CLAIM_MODE = "griefdefender.user.claim.command.claim-mode";
    public static final String COMMAND_CLAIM_OPTIONS_BASE = "griefdefender.user.claim.option.base";
    public static final String COMMAND_CLAIM_SELL = "griefdefender.user.claim.command.sell";
    public static final String COMMAND_CLAIM_SPAWN = "griefdefender.user.claim.command.spawn";
    public static final String COMMAND_CLAIM_SET_SPAWN = "griefdefender.user.claim.command.set-spawn";
    public static final String COMMAND_CLAIM_TAX = "griefdefender.user.claim.command.claim.tax";
    public static final String COMMAND_CLAIM_WORLDEDIT = "griefdefender.user.claim.command.worldedit-claim";
    public static final String COMMAND_SET_CLAIM_NAME = "griefdefender.user.claim.command.name";
    public static final String COMMAND_SET_CLAIM_FAREWELL = "griefdefender.user.claim.command.farewell";
    public static final String COMMAND_SET_CLAIM_GREETING = "griefdefender.user.claim.command.greeting";
    public static final String COMMAND_SUBDIVIDE_CLAIMS = "griefdefender.user.claim.command.subdivide-mode";
    public static final String COMMAND_TOWN_BANK = "griefdefender.user.town.command.bank";
    public static final String COMMAND_TOWN_CHAT = "griefdefender.user.town.command.chat";
    public static final String COMMAND_TOWN_INFO_BASE = "griefdefender.user.town.command.info.base";
    public static final String COMMAND_TOWN_INFO_OTHERS = "griefdefender.user.town.command.info.others";
    public static final String COMMAND_TOWN_INFO_TELEPORT_OTHERS = "griefdefender.user.town.command.info.teleport.others";
    public static final String COMMAND_TOWN_INFO_TELEPORT_BASE = "griefdefender.user.town.command.info.teleport.base";
    public static final String COMMAND_TOWN_NAME = "griefdefender.user.town.command.name";
    public static final String COMMAND_TOWN_TAG = "griefdefender.user.town.command.tag";
    public static final String COMMAND_TOWN_TAX = "griefdefender.user.town.command.tax";
    public static final String COMMAND_TOWN_MODE = "griefdefender.user.claim.command.town-mode";
    public static final String COMMAND_TRANSFER_CLAIM = "griefdefender.user.claim.command.transfer";
    public static final String COMMAND_BUY_CLAIM_BLOCKS = "griefdefender.user.claim.command.buy-blocks";
    public static final String COMMAND_SELL_CLAIM_BLOCKS = "griefdefender.user.claim.command.sell-blocks";
    public static final String COMMAND_LIST_CLAIM_FLAGS = "griefdefender.user.claim.command.list-flags";
    public static final String COMMAND_LIST_CLAIM_OPTIONS = "griefdefender.user.claim.command.list-options";
    public static final String COMMAND_BAN_ITEM = "griefdefender.user.claim.command.ban-item";
    public static final String COMMAND_UNBAN_ITEM = "griefdefender.user.claim.command.unban-item";
    public static final String COMMAND_CLAIM_INHERIT = "griefdefender.user.claim.command.inherit";
    public static final String CLAIM_CREATE = "griefdefender.user.claim.create.base";
    public static final String CLAIM_CREATE_BASIC = "griefdefender.user.claim.create.basic";
    public static final String CLAIM_CREATE_SUBDIVISION = "griefdefender.user.claim.create.subdivision";
    public static final String CLAIM_CREATE_TOWN = "griefdefender.user.claim.create.town";
    public static final String CLAIM_CUBOID_BASIC = "griefdefender.user.claim.create.cuboid.basic";
    public static final String CLAIM_CUBOID_SUBDIVISION = "griefdefender.user.claim.create.cuboid.subdivision";
    public static final String CLAIM_CUBOID_TOWN = "griefdefender.user.claim.create.cuboid.town";
    public static final String CLAIM_PVP_OVERRIDE = "griefdefender.user.claim.pvp-override";
    public static final String CLAIM_RESIZE = "griefdefender.user.claim.resize";
    public static final String CLAIM_SHOW_TUTORIAL = "griefdefender.user.claim.show-tutorial";
    public static final String VISUALIZE_CLAIMS = "griefdefender.user.claim.visualize.base";
    public static final String VISUALIZE_CLAIMS_NEARBY = "griefdefender.user.claim.visualize.nearby";
    public static final String COMMAND_PLAYER_INFO_BASE = "griefdefender.user.command.info.base";
    public static final String COMMAND_PLAYER_INFO_OTHERS = "griefdefender.user.command.info.others";
    public static final String COMMAND_VERSION = "griefdefender.user.command.version";

    // flags
    public static final String USER_CLAIM_FLAGS = "griefdefender.user.claim.flag";
    public static final String COMMAND_FLAGS_CLAIM = "griefdefender.user.claim.command.flag.base";
    public static final String COMMAND_FLAGS_CLAIM_ARG = "griefdefender.admin.claim.command.flag.arg";
    public static final String COMMAND_FLAGS_CLAIM_GUI = "griefdefender.user.claim.command.flag.gui";
    public static final String COMMAND_FLAGS_DEBUG = "griefdefender.user.claim.command.flag.debug";
    public static final String COMMAND_FLAGS_PLAYER = "griefdefender.user.claim.command.flag.player";
    public static final String COMMAND_FLAGS_GROUP = "griefdefender.user.claim.command.flag.group";
    public static final String COMMAND_FLAGS_RESET = "griefdefender.user.claim.command.flag.reset";

    public static final String FLAG_CUSTOM_ADMIN_BASE = "griefdefender.admin.custom.flag";
    public static final String FLAG_CUSTOM_USER_BASE = "griefdefender.user.custom.flag";

    // options
    public static final String USER_CLAIM_OPTIONS = "griefdefender.user.claim.option";
    public static final String COMMAND_OPTIONS_CLAIM = "griefdefender.user.claim.command.option.base";
    public static final String COMMAND_OPTIONS_PLAYER = "griefdefender.user.claim.command.option.player";
    public static final String COMMAND_OPTIONS_GROUP = "griefdefender.user.claim.command.option.group";
    public static final String USER_OPTION_PERK_OWNER_FLY_BASIC = "griefdefender.user.option.perk.owner-fly.basic";
    public static final String USER_OPTION_PERK_OWNER_FLY_TOWN = "griefdefender.user.option.perk.owner-fly.town";
    public static final String OPTION_BASE = "griefdefender";

    // Admin
    public static final String ADVANCED_FLAGS = "griefdefender.admin.advanced-flags";
    public static final String BYPASS_BAN = "griefdefender.admin.bypass.ban";
    public static final String BYPASS_BORDER_CHECK = "griefdefender.admin.bypass.border-check";
    public static final String BYPASS_CLAIM_RESIZE = "griefdefender.admin.bypass.override.resize";
    public static final String BYPASS_CLAIM_LIMIT = "griefdefender.admin.bypass.override.limit";
    public static final String BYPASS_OPTION = "griefdefender.admin.bypass.option";
    public static final String CLAIM_CUBOID_ADMIN = "griefdefender.admin.claim.cuboid";
    public static final String CLAIM_RESIZE_ALL = "griefdefender.admin.claim.resize";
    public static final String CLAIM_RESIZE_ADMIN = "griefdefender.admin.claim.resize.admin";
    public static final String CLAIM_RESIZE_ADMIN_SUBDIVISION = "griefdefender.admin.claim.resize.admin.subdivision";
    public static final String CLAIM_RESIZE_BASIC = "griefdefender.admin.claim.resize.basic";
    public static final String CLAIM_RESIZE_BASIC_SUBDIVISION = "griefdefender.admin.claim.resize.basic.subdivision";
    public static final String CLAIM_RESIZE_TOWN = "griefdefender.admin.claim.resize.town";
    public static final String COMMAND_ADJUST_CLAIM_BLOCKS = "griefdefender.admin.claim.command.adjust-claim-blocks";
    public static final String COMMAND_ADMIN_CLAIMS = "griefdefender.admin.claim.command.admin-mode";
    public static final String COMMAND_ADMIN_DEBUG = "griefdefender.admin.claim.command.debug";
    public static final String COMMAND_CLAIM_BAN = "griefdefender.admin.claim.command.ban";
    public static final String COMMAND_CLAIM_CLEAR = "griefdefender.admin.claim.command.clear";
    public static final String COMMAND_CLAIM_PERMISSION_GROUP = "griefdefender.admin.claim.command.permission-group";
    public static final String COMMAND_CLAIM_PERMISSION_PLAYER = "griefdefender.admin.claim.command.permission-player";
    public static final String COMMAND_CLAIM_RESERVE = "griefdefender.admin.claim.command.reserve-name";
    public static final String COMMAND_CLAIM_SCHEMATIC = "griefdefender.admin.claim.command.schematic";
    public static final String COMMAND_IGNORE_CLAIMS = "griefdefender.admin.claim.command.ignore.base";
    public static final String COMMAND_DELETE_CLAIM_BASE = "griefdefender.admin.claim.command.delete.base";
    public static final String COMMAND_DELETE_CLAIMS = "griefdefender.admin.claim.command.delete-claims";
    public static final String COMMAND_DELETE_ADMIN_CLAIMS = "griefdefender.admin.command.delete-admin-claims";
    public static final String COMMAND_SET_ACCRUED_CLAIM_BLOCKS = "griefdefender.admin.command.set-accrued-claim-blocks";
    public static final String COMMAND_RESTORE_CLAIM = "griefdefender.admin.command.restore-claim.base";
    public static final String COMMAND_RESTORE_NATURE = "griefdefender.admin.command.restore-nature.base";
    public static final String COMMAND_RESTORE_NATURE_AGGRESSIVE = "griefdefender.admin.command.restore-nature.aggressive";
    public static final String COMMAND_RESTORE_NATURE_FILL = "griefdefender.admin.command.restore-nature.fill";
    public static final String COMMAND_RELOAD = "griefdefender.admin.command.reload";
    public static final String DELETE_CLAIM_BASIC = "griefdefender.admin.claim.command.delete.basic";
    public static final String DELETE_CLAIM_ADMIN = "griefdefender.admin.claim.command.delete.admin";
    public static final String EAVES_DROP_SIGNS = "griefdefender.admin.eavesdrop.signs";
    public static final String IGNORE_CLAIMS_BASIC = "griefdefender.admin.claim.command.ignore.basic";
    public static final String IGNORE_CLAIMS_ADMIN = "griefdefender.admin.claim.command.ignore.admin";
    public static final String IGNORE_CLAIMS_TOWN = "griefdefender.admin.claim.command.ignore.town";
    public static final String IGNORE_CLAIMS_WILDERNESS = "griefdefender.admin.claim.command.ignore.wilderness";
    public static final String LIST_ADMIN_CLAIMS = "griefdefender.admin.claim.list.admin";
    public static final String MANAGE_FLAG_DEFAULTS = "griefdefender.admin.flag-defaults";
    public static final String MANAGE_FLAG_OVERRIDES = "griefdefender.admin.flag-overrides";
    public static final String MANAGE_WILDERNESS = "griefdefender.admin.claim.wilderness";
    public static final String MANAGE_ADMIN_OPTIONS = "griefdefender.admin.claim.option.admin";
    public static final String MANAGE_GLOBAL_OPTIONS = "griefdefender.admin.claim.option.global";
    public static final String MANAGE_OVERRIDE_OPTIONS = "griefdefender.admin.claim.option.override";
    public static final String SET_ADMIN_FLAGS = "griefdefender.admin.claim.set-admin-flags";
    public static final String USE_RESERVED_CLAIM_NAMES = "griefdefender.admin.claim.use-reserved-names";

    // Misc
    public static final String COMMAND_HELP = "griefdefender.user.command.help";
    public static final String CHAT_CAPTURE = "griefdefender.user.chat.capture";

    // Trust
    public static final String COMMAND_TRUST_GROUP = "griefdefender.user.claim.command.trust.group";
    public static final String COMMAND_TRUST_PLAYER = "griefdefender.user.claim.command.trust.player";
    public static final String COMMAND_LIST_TRUST = "griefdefender.user.claim.command.trust.list";
    public static final String COMMAND_TRUSTALL_GROUP = "griefdefender.user.claim.command.trustall.group";
    public static final String COMMAND_TRUSTALL_PLAYER = "griefdefender.user.claim.command.trustall.player";
    public static final String COMMAND_UNTRUST_GROUP = "griefdefender.user.claim.command.untrust.group";
    public static final String COMMAND_UNTRUST_PLAYER = "griefdefender.user.claim.command.untrust.player";
    public static final String COMMAND_UNTRUSTALL_GROUP = "griefdefender.user.claim.command.untrustall.group";
    public static final String COMMAND_UNTRUSTALL_PLAYER = "griefdefender.user.claim.command.untrustall.player";
    public static final String GIVE_ACCESS_TRUST = "griefdefender.user.claim.trust.accessor";
    public static final String GIVE_CONTAINER_TRUST = "griefdefender.user.claim.trust.container";
    public static final String GIVE_BUILDER_TRUST = "griefdefender.user.claim.trust.builder";
    public static final String GIVE_MANAGER_TRUST = "griefdefender.user.claim.trust.manager";
    public static final String REMOVE_TRUST = "griefdefender.user.claim.trust.remove";
    public static final String TRUST_ACCESSOR = "griefdefender.trust.1.2.3.4";
    public static final String TRUST_CONTAINER = "griefdefender.trust.1.2.3";
    public static final String TRUST_BUILDER = "griefdefender.trust.1.2";
    public static final String TRUST_MANAGER = "griefdefender.trust.1";

    public static String getTrustPermission(TrustType type) {
        if (type == TrustTypes.ACCESSOR) {
            return GDPermissions.TRUST_ACCESSOR;
        }
        if (type == TrustTypes.BUILDER) {
            return GDPermissions.TRUST_BUILDER;
        }
        if (type == TrustTypes.CONTAINER) {
            return GDPermissions.TRUST_CONTAINER;
        }

        return GDPermissions.TRUST_MANAGER;
    }
}
