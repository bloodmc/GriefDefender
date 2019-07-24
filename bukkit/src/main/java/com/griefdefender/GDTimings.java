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
package com.griefdefender;

import co.aikar.timings.lib.MCTiming;

public class GDTimings {

    public static final MCTiming BLOCK_BREAK_EVENT = GriefDefenderPlugin.timing("onBlockBreak");
    public static final MCTiming BLOCK_COLLIDE_EVENT = GriefDefenderPlugin.timing("onBlockCollide");
    public static final MCTiming BLOCK_NOTIFY_EVENT = GriefDefenderPlugin.timing("onBlockNotify");
    public static final MCTiming BLOCK_PLACE_EVENT = GriefDefenderPlugin.timing("onBlockPlace");
    public static final MCTiming BLOCK_POST_EVENT = GriefDefenderPlugin.timing("onBlockPost");
    public static final MCTiming BLOCK_PRE_EVENT = GriefDefenderPlugin.timing("onBlockPre");
    public static final MCTiming ENTITY_EXPLOSION_PRE_EVENT = GriefDefenderPlugin.timing("onEntityExplosionPre");
    public static final MCTiming ENTITY_EXPLOSION_DETONATE_EVENT = GriefDefenderPlugin.timing("onEntityExplosionDetonate");
    public static final MCTiming ENTITY_ATTACK_EVENT = GriefDefenderPlugin.timing("onEntityAttack");
    public static final MCTiming ENTITY_COLLIDE_EVENT = GriefDefenderPlugin.timing("onEntityCollide");
    public static final MCTiming ENTITY_DAMAGE_EVENT = GriefDefenderPlugin.timing("onEntityDamage");
    public static final MCTiming ENTITY_DAMAGE_MONITOR_EVENT = GriefDefenderPlugin.timing("onEntityDamageMonitor");
    public static final MCTiming ENTITY_DEATH_EVENT = GriefDefenderPlugin.timing("onEntityDeath");
    public static final MCTiming ENTITY_DROP_ITEM_DEATH_EVENT = GriefDefenderPlugin.timing("onEntityDropDeathItem");
    public static final MCTiming ENTITY_MOUNT_EVENT = GriefDefenderPlugin.timing("onEntityMount");
    public static final MCTiming ENTITY_MOVE_EVENT = GriefDefenderPlugin.timing("onEntityMove");
    public static final MCTiming ENTITY_SPAWN_PRE_EVENT = GriefDefenderPlugin.timing("onEntitySpawnPre");
    public static final MCTiming ENTITY_SPAWN_EVENT = GriefDefenderPlugin.timing("onEntitySpawn");
    public static final MCTiming ENTITY_TELEPORT_EVENT = GriefDefenderPlugin.timing("onEntityTeleport");
    public static final MCTiming PLAYER_CHANGE_HELD_ITEM_EVENT = GriefDefenderPlugin.timing("onPlayerChangeHeldItem");
    public static final MCTiming PLAYER_CHAT_EVENT = GriefDefenderPlugin.timing("onPlayerChat");
    public static final MCTiming PLAYER_COMMAND_EVENT = GriefDefenderPlugin.timing("onPlayerCommand");
    public static final MCTiming PLAYER_DEATH_EVENT = GriefDefenderPlugin.timing("onPlayerDeath");
    public static final MCTiming PLAYER_DISPENSE_ITEM_EVENT = GriefDefenderPlugin.timing("onPlayerDispenseItem");
    public static final MCTiming PLAYER_LOGIN_EVENT = GriefDefenderPlugin.timing("onPlayerLogin");
    public static final MCTiming PLAYER_HANDLE_SHOVEL_ACTION = GriefDefenderPlugin.timing("onPlayerHandleShovelAction");
    public static final MCTiming PLAYER_INTERACT_BLOCK_PRIMARY_EVENT = GriefDefenderPlugin.timing("onPlayerInteractBlockPrimary");
    public static final MCTiming PLAYER_INTERACT_BLOCK_SECONDARY_EVENT = GriefDefenderPlugin.timing("onPlayerInteractBlockSecondary");
    public static final MCTiming PLAYER_INTERACT_ENTITY_PRIMARY_EVENT = GriefDefenderPlugin.timing("onPlayerInteractEntityPrimary");
    public static final MCTiming PLAYER_INTERACT_ENTITY_SECONDARY_EVENT = GriefDefenderPlugin.timing("onPlayerInteractEntitySecondary");
    public static final MCTiming PLAYER_INTERACT_INVENTORY_CLICK_EVENT = GriefDefenderPlugin.timing("onPlayerInteractInventoryClick");
    public static final MCTiming PLAYER_INTERACT_INVENTORY_CLOSE_EVENT = GriefDefenderPlugin.timing("onPlayerInteractInventoryClose");
    public static final MCTiming PLAYER_INTERACT_INVENTORY_OPEN_EVENT = GriefDefenderPlugin.timing("onPlayerInteractInventoryOpen");
    public static final MCTiming PLAYER_INVESTIGATE_CLAIM = GriefDefenderPlugin.timing("onPlayerInvestigateClaim");
    public static final MCTiming PLAYER_JOIN_EVENT = GriefDefenderPlugin.timing("onPlayerJoin");
    public static final MCTiming PLAYER_KICK_EVENT = GriefDefenderPlugin.timing("onPlayerKick");
    public static final MCTiming PLAYER_PICKUP_ITEM_EVENT = GriefDefenderPlugin.timing("onPlayerPickupItem");
    public static final MCTiming PLAYER_QUIT_EVENT = GriefDefenderPlugin.timing("onPlayerQuit");
    public static final MCTiming PLAYER_RESPAWN_EVENT = GriefDefenderPlugin.timing("onPlayerRespawn");
    public static final MCTiming PLAYER_USE_ITEM_EVENT = GriefDefenderPlugin.timing("onPlayerUseItem");
    public static final MCTiming SIGN_CHANGE_EVENT = GriefDefenderPlugin.timing("onSignChange");
    public static final MCTiming PROJECTILE_IMPACT_BLOCK_EVENT = GriefDefenderPlugin.timing("onProjectileImpactBlock");
    public static final MCTiming PROJECTILE_IMPACT_ENTITY_EVENT = GriefDefenderPlugin.timing("onProjectileImpactEntity");
    public static final MCTiming EXPLOSION_EVENT = GriefDefenderPlugin.timing("onExplosion");
    public static final MCTiming CLAIM_GETCLAIM = GriefDefenderPlugin.timing("getClaimAt");
    public static final MCTiming WORLD_LOAD_EVENT = GriefDefenderPlugin.timing("onWorldSave");
    public static final MCTiming WORLD_SAVE_EVENT = GriefDefenderPlugin.timing("onWorldSave");
    public static final MCTiming WORLD_UNLOAD_EVENT = GriefDefenderPlugin.timing("onWorldSave");
}
