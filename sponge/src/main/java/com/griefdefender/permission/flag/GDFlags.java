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
package com.griefdefender.permission.flag;

import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.permission.flag.Flags;

public class GDFlags {

    public static boolean BLOCK_BREAK;
    public static boolean BLOCK_GROW;
    public static boolean BLOCK_MODIFY;
    public static boolean BLOCK_PLACE;
    public static boolean BLOCK_SPREAD;
    public static boolean COLLIDE_BLOCK;
    public static boolean COLLIDE_ENTITY;
    public static boolean COMMAND_EXECUTE;
    public static boolean COMMAND_EXECUTE_PVP;
    public static boolean ENTER_CLAIM;
    public static boolean ENTITY_CHUNK_SPAWN;
    public static boolean ENTITY_DAMAGE;
    public static boolean ENTITY_RIDING;
    public static boolean ENTITY_SPAWN;
    public static boolean ENTITY_TELEPORT_FROM;
    public static boolean ENTITY_TELEPORT_TO;
    public static boolean EXIT_CLAIM;
    public static boolean EXPLOSION_BLOCK;
    public static boolean EXPLOSION_ENTITY;
    public static boolean INTERACT_BLOCK_PRIMARY;
    public static boolean INTERACT_BLOCK_SECONDARY;
    public static boolean INTERACT_ENTITY_PRIMARY;
    public static boolean INTERACT_ENTITY_SECONDARY;
    public static boolean INTERACT_ITEM_PRIMARY;
    public static boolean INTERACT_ITEM_SECONDARY;
    public static boolean INTERACT_INVENTORY;
    public static boolean INTERACT_INVENTORY_CLICK;
    public static boolean ITEM_DROP;
    public static boolean ITEM_PICKUP;
    public static boolean ITEM_SPAWN;
    public static boolean ITEM_USE;
    public static boolean LEAF_DECAY;
    public static boolean LIQUID_FLOW;
    public static boolean PORTAL_USE;
    public static boolean PROJECTILE_IMPACT_BLOCK;
    public static boolean PROJECTILE_IMPACT_ENTITY;

    public static void populateFlagStatus() {
        BLOCK_BREAK = GriefDefenderPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(Flags.BLOCK_BREAK.getName());
        BLOCK_GROW = GriefDefenderPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(Flags.BLOCK_GROW.getName());
        BLOCK_MODIFY = GriefDefenderPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(Flags.BLOCK_MODIFY.getName());
        BLOCK_PLACE = GriefDefenderPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(Flags.BLOCK_PLACE.getName());
        BLOCK_SPREAD  = GriefDefenderPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(Flags.BLOCK_SPREAD.getName());
        COLLIDE_BLOCK  = GriefDefenderPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(Flags.COLLIDE_BLOCK.getName());
        COLLIDE_ENTITY  = GriefDefenderPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(Flags.COLLIDE_ENTITY.getName());
        COMMAND_EXECUTE  = GriefDefenderPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(Flags.COMMAND_EXECUTE.getName());
        COMMAND_EXECUTE_PVP  = GriefDefenderPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(Flags.COMMAND_EXECUTE_PVP.getName());
        ENTER_CLAIM  = GriefDefenderPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(Flags.ENTER_CLAIM.getName());
        ENTITY_CHUNK_SPAWN  = GriefDefenderPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(Flags.ENTITY_CHUNK_SPAWN.getName());
        ENTITY_DAMAGE  = GriefDefenderPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(Flags.ENTITY_DAMAGE.getName());
        ENTITY_RIDING  = GriefDefenderPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(Flags.ENTITY_RIDING.getName());
        ENTITY_SPAWN  = GriefDefenderPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(Flags.ENTITY_SPAWN.getName());
        ENTITY_TELEPORT_FROM  = GriefDefenderPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(Flags.ENTITY_TELEPORT_FROM.getName());
        ENTITY_TELEPORT_TO  = GriefDefenderPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(Flags.ENTITY_TELEPORT_TO.getName());
        EXIT_CLAIM  = GriefDefenderPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(Flags.EXIT_CLAIM.getName());
        EXPLOSION_BLOCK  = GriefDefenderPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(Flags.EXPLOSION_BLOCK.getName());
        EXPLOSION_ENTITY  = GriefDefenderPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(Flags.EXPLOSION_ENTITY.getName());
        INTERACT_BLOCK_PRIMARY  = GriefDefenderPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(Flags.INTERACT_BLOCK_PRIMARY.getName());
        INTERACT_BLOCK_SECONDARY  = GriefDefenderPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(Flags.INTERACT_BLOCK_SECONDARY.getName());
        INTERACT_ENTITY_PRIMARY  = GriefDefenderPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(Flags.INTERACT_ENTITY_PRIMARY.getName());
        INTERACT_ENTITY_SECONDARY  = GriefDefenderPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(Flags.INTERACT_ENTITY_SECONDARY.getName());
        INTERACT_INVENTORY  = GriefDefenderPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(Flags.INTERACT_INVENTORY.getName());
        INTERACT_INVENTORY_CLICK  = GriefDefenderPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(Flags.INTERACT_INVENTORY_CLICK.getName());
        INTERACT_ITEM_PRIMARY  = GriefDefenderPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(Flags.INTERACT_ITEM_PRIMARY.getName());
        INTERACT_ITEM_SECONDARY  = GriefDefenderPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(Flags.INTERACT_ITEM_SECONDARY.getName());
        ITEM_DROP  = GriefDefenderPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(Flags.ITEM_DROP.getName());
        ITEM_PICKUP  = GriefDefenderPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(Flags.ITEM_PICKUP.getName());
        ITEM_SPAWN  = GriefDefenderPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(Flags.ITEM_SPAWN.getName());
        ITEM_USE  = GriefDefenderPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(Flags.ITEM_USE.getName());
        LEAF_DECAY  = GriefDefenderPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(Flags.LEAF_DECAY.getName());
        LIQUID_FLOW  = GriefDefenderPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(Flags.LIQUID_FLOW.getName());
        PORTAL_USE  = GriefDefenderPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(Flags.PORTAL_USE.getName());
        PROJECTILE_IMPACT_BLOCK  = GriefDefenderPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(Flags.PROJECTILE_IMPACT_BLOCK.getName());
        PROJECTILE_IMPACT_ENTITY  = GriefDefenderPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(Flags.PROJECTILE_IMPACT_ENTITY.getName());
    }
}
