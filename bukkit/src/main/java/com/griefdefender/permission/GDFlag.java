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

import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.api.claim.ClaimTypes;
import com.griefdefender.api.permission.flag.Flag;
import com.griefdefender.api.permission.flag.Flags;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;

public class GDFlag implements Flag {

    private final String id;
    private final String name;
    private Component description;

    public GDFlag(String id, String name) {
        this.id = id;
        this.name = name.toLowerCase();
    }

    @Override
    public String getId() {
        return this.id;
    }

    public String getPermission() {
        return "griefdefender.flag." + this.name.toLowerCase();
    }

    public String getName() {
        return this.name;
    }

    public Component getDescription() {
        if (this.description == null) {
            this.description = this.createDescription();
        }
        return this.description;
    }

    @Override
    public String toString() {
        return this.name;
    }

    private Component createDescription() {
        if (this == Flags.BLOCK_BREAK) {
            return TextComponent.builder("")
                    .append("Controls whether a block can be broken.\n")
                    .append("Example 1", TextColor.LIGHT_PURPLE)
                    .append(" : To prevent any source from breaking dirt blocks, enter\n")
                    .append("/cf block-break minecraft:dirt false\n", TextColor.GREEN)
                    .append("Note", TextColor.AQUA)
                    .append(" : minecraft represents the modid and dirt represents the block id.\n" +
                        "Specifying no modid will always default to minecraft.\n")
                    .append("Example 2", TextColor.LIGHT_PURPLE)
                    .append(" : To prevent players from breaking dirt blocks, enter\n")
                    .append("/cf block-break minecraft:player minecraft:dirt false\n", TextColor.GREEN)
                    .build();
        }
        if (this == Flags.BLOCK_GROW) {
            return TextComponent.builder("")
                    .append("Controls whether a block can grow.\n")
                    .append("Example 1", TextColor.LIGHT_PURPLE)
                    .append(" : To prevent a cactus from growing, enter\n")
                    .append("/cf block-grow cactus false\n", TextColor.GREEN)
                    .build();
        }
        if (this == Flags.BLOCK_MODIFY) {
            return TextComponent.builder("")
                    .append("Controls whether a block can be modified.\n")
                    .append("Example 1", TextColor.LIGHT_PURPLE)
                    .append(" : To prevent any source from igniting a block, enter\n")
                    .append("/cf block-modify minecraft:fire false\n", TextColor.GREEN)
                    .append("Note", TextColor.AQUA)
                    .append(" : minecraft represents the modid and fire represents the block id.\n" +
                        "Specifying no modid will always default to minecraft.\n")
                    .build();
        }
        if (this == Flags.BLOCK_PLACE) {
            return TextComponent.builder("")
                    .append("Controls whether a block can be placed.\n")
                    .append("Example 1", TextColor.LIGHT_PURPLE)
                    .append(" : To prevent any source from placing dirt blocks, enter\n")
                    .append("/cf block-place minecraft:dirt false\n", TextColor.GREEN)
                    .append("Note", TextColor.AQUA)
                    .append(" : minecraft represents the modid and dirt represents the block id.\n" +
                        "Specifying no modid will always default to minecraft.\n")
                    .append("Example 2", TextColor.LIGHT_PURPLE)
                    .append(" : To prevent players from placing dirt blocks, enter\n")
                    .append("/cf block-place minecraft:player minecraft:dirt false\n", TextColor.GREEN)
                    .build();
        }
        if (this == Flags.COLLIDE_BLOCK) {
            return TextComponent.builder("")
                    .append("Controls whether an entity can collide with a block.\n")
                    .append("Example", TextColor.LIGHT_PURPLE)
                    .append(" : To prevent entity collisions with dirt blocks, enter\n")
                    .append("/cf collide-block minecraft:dirt false", TextColor.GREEN)
                    .build();
        }
        if (this == Flags.COLLIDE_ENTITY) {
            return TextComponent.builder("")
                    .append("Controls whether an entity can collide with an entity.\n")
                    .append("Example", TextColor.LIGHT_PURPLE)
                    .append(" : To prevent entity collisions with item frames, enter\n")
                    .append("/cf collide-entity minecraft:itemframe false", TextColor.GREEN)
                    .build();
        }
        if (this == Flags.COMMAND_EXECUTE) {
            return TextComponent.builder("")
                    .append("Controls whether a command can be executed.\n")
                    .append("Example", TextColor.LIGHT_PURPLE)
                    .append(" : To prevent pixelmon's command '/shop select' from being run, enter\n")
                    .append("/cf command-execute pixelmon:shop[select] false\n", TextColor.GREEN)
                    .append("Note", TextColor.AQUA)
                    .append(" : ")
                    .append(TextComponent.of("pixelmon", TextColor.GOLD).decoration(TextDecoration.ITALIC, true))
                    .append(" represents the modid, ")
                    .append(TextComponent.of("shop", TextColor.GOLD).decoration(TextDecoration.ITALIC, true))
                    .append(" represents the base command, and ")
                    .append(TextComponent.of("select", TextColor.GOLD).decoration(TextDecoration.ITALIC, true))
                    .append(" represents the argument.\nSpecifying no modid will always default to minecraft.\n")
                    .build();
        }
        if (this == Flags.COMMAND_EXECUTE_PVP) {
            return TextComponent.builder("")
                    .append("Controls whether a command can be executed while engaged in ")
                    .append("PvP.\n", TextColor.RED)
                    .append("Example", TextColor.LIGHT_PURPLE)
                    .append(" : To prevent pixelmon's command '/shop select' from being run, enter\n")
                    .append("/cf command-execute pixelmon:shop[select] false\n", TextColor.GREEN)
                    .append("Note", TextColor.AQUA)
                    .append(" : ")
                    .append(TextComponent.of("pixelmon", TextColor.GOLD).decoration(TextDecoration.ITALIC, true))
                    .append(" represents the modid, ")
                    .append(TextComponent.of("shop", TextColor.GOLD).decoration(TextDecoration.ITALIC, true))
                    .append(" represents the base command, and ")
                    .append(TextComponent.of("select", TextColor.GOLD).decoration(TextDecoration.ITALIC, true))
                    .append(" represents the argument.\nSpecifying no modid will always default to minecraft.\n")
                    .build();
        }
        if (this == Flags.ENTER_CLAIM) {
            return TextComponent.builder("")
                    .append("Controls whether an entity can enter claim.\n")
                    .append("Note", TextColor.AQUA)
                    .append(" : If you want to use this for players, it is recommended to use \nthe '/cfg' command with the group the player is in.")
                    .build();
        }
        if (this == Flags.ENTITY_CHUNK_SPAWN) {
            return TextComponent.builder("")
                    .append("Controls whether an entity can be spawned during chunk load.\n")
                    .append("Note", TextColor.AQUA)
                    .append(" : This will remove all saved entities within a chunk after it loads.\n")
                    .append("Example", TextColor.LIGHT_PURPLE)
                    .append(" : To prevent horses from spawning in chunks enter\n")
                    .append("/cf entity-chunk-spawn minecraft:horse false", TextColor.GREEN)
                    .build();
        }
        if (this == Flags.ENTITY_DAMAGE) {
            return TextComponent.builder("")
                    .append("Controls whether an entity can be damaged.\n")
                    .append("Example 1", TextColor.LIGHT_PURPLE)
                    .append(" : To prevent horses from being damaged, enter\n")
                    .append("/cf entity-damage minecraft:horse false\n", TextColor.GREEN)
                    .append("Example 2", TextColor.LIGHT_PURPLE)
                    .append(" : To prevent all animals from being damaged, enter\n")
                    .append("/cf entity-damage minecraft:animals false", TextColor.GREEN)
                    .build();
        }
        if (this == Flags.ENTITY_RIDING) {
            return TextComponent.builder("")
                    .append("Controls whether an entity can be mounted.\n")
                    .append("Example", TextColor.LIGHT_PURPLE)
                    .append(" : To prevent horses from being mounted enter\n")
                    .append("/cf entity-riding minecraft:horse false", TextColor.GREEN)
                    .build();
        }
        if (this == Flags.ENTITY_SPAWN) {
            return TextComponent.builder("")
                    .append("Controls whether an entity can be spawned into the world.\n")
                    .append("Note", TextColor.AQUA)
                    .append(" : This does not include entity items. See item-spawn flag.\n")
                    .append("Example", TextColor.LIGHT_PURPLE)
                    .append(" : To prevent horses from spawning enter\n")
                    .append("/cf entity-spawn minecraft:horse false", TextColor.GREEN)
                    .build();
        }
        if (this == Flags.ENTITY_TELEPORT_FROM) {
            return TextComponent.builder("")
                    .append("Controls whether an entity can teleport from their current location.\n")
                    .append("Note", TextColor.AQUA)
                    .append(" : If you want to use this for players, it is recommended to use \n")
                    .append("the '/cfg' command with the group the player is in.")
                    .build();
        }
        if (this == Flags.ENTITY_TELEPORT_TO) {
            return TextComponent.builder("")
                    .append("Controls whether an entity can teleport to a location.\n")
                    .append("Example", TextColor.LIGHT_PURPLE)
                    .append(" : To prevent creepers from traveling and/or teleporting within your claim, enter\n")
                    .append("/cf entity-teleport-to minecraft:creeper false\n", TextColor.GREEN)
                    .append("Note", TextColor.AQUA)
                    .append(" : If you want to use this for players, it is recommended to use \n")
                    .append("the '/cfg' command with the group the player is in.")
                    .build();
        }
        if (this == Flags.EXIT_CLAIM) {
            return TextComponent.builder("")
                    .append("Controls whether an entity can exit claim.\n")
                    .append("Note", TextColor.AQUA)
                    .append(" : If you want to use this for players, it is recommended to use \n")
                    .append("the '/cfg' command with the group the player is in.")
                    .build();
        }
        if (this == Flags.EXPLOSION_BLOCK) {
            return TextComponent.builder("")
                    .append("Controls whether an explosion can damage blocks in the world.\n")
                    .append("Example", TextColor.LIGHT_PURPLE)
                    .append(" : To prevent an explosion from affecting blocks, enter\n")
                    .append("/cf explosion-block any false", TextColor.GREEN)
                    .build();
        }
        if (this == Flags.EXPLOSION_ENTITY) {
            return TextComponent.builder("")
                    .append("Controls whether an explosion can damage entities in the world.\n")
                    .append("Example", TextColor.LIGHT_PURPLE)
                    .append(" : To prevent an explosion from affecting entities, enter\n")
                    .append("/cf explosion-entity any false", TextColor.GREEN)
                    .build();
        }
        if (this == Flags.FIRE_SPREAD) {
            return TextComponent.builder("")
                    .append("Controls whether fire can spread in a world.\n")
                    .append("Note", TextColor.AQUA)
                    .append(" : This does not prevent the initial fire being placed, only spread.\n")
                    .append("Example", TextColor.LIGHT_PURPLE)
                    .append(" : To prevent fire from spreading, enter\n")
                    .append("/cf fire-spread any false", TextColor.GREEN)
                    .build();
        }
        if (this == Flags.INTERACT_BLOCK_PRIMARY) {
            return TextComponent.builder("")
                    .append("Controls whether a player can left-click(attack) a block.\n")
                    .append("Example", TextColor.LIGHT_PURPLE)
                    .append(" : To prevent players from left-clicking chests, enter\n")
                    .append("/cf interact-block-primary minecraft:chest false", TextColor.GREEN)
                    .build();
        }
        if (this == Flags.INTERACT_BLOCK_SECONDARY) {
            return TextComponent.builder("")
                    .append("Controls whether a player can right-click a block.\n")
                    .append("Example", TextColor.LIGHT_PURPLE)
                    .append(" : To prevent players from right-clicking(opening) chests, enter\n")
                    .append("/cf interact-block-secondary minecraft:chest false", TextColor.GREEN)
                    .build();
        }
        if (this == Flags.INTERACT_ENTITY_PRIMARY) {
            return TextComponent.builder("")
                    .append("Controls whether a player can left-click(attack) an entity.\n")
                    .append("Example", TextColor.LIGHT_PURPLE)
                    .append(" : To prevent players from left-clicking horses, enter\n")
                    .append("/cf interact-entity-primary minecraft:player minecraft:horse false\n", TextColor.GREEN)
                    .build();
        }
        if (this == Flags.INTERACT_ENTITY_SECONDARY) {
            return TextComponent.builder("")
                    .append("Controls whether a player can right-click on an entity.\n")
                    .append("Example", TextColor.LIGHT_PURPLE)
                    .append(" : To prevent horses from being mounted, enter\n")
                    .append("/cf interact-entity-secondary minecraft:horse false\n", TextColor.GREEN)
                    .append("Note", TextColor.AQUA)
                    .append(" : minecraft represents the modid and horse represents the entity id.\n")
                    .append("Specifying no modid will always default to minecraft.\n")
                    .build();
        }
        if (this == Flags.INTERACT_INVENTORY) {
            return TextComponent.builder("")
                    .append("Controls whether a player can right-click with a block that contains inventory such as a chest.\n")
                    .append("Example", TextColor.LIGHT_PURPLE)
                    .append(" : To prevent players from right-clicking any block that contains inventory, enter\n")
                    .append("/cf interact-inventory any false", TextColor.GREEN)
                    .build();
        }
        if (this == Flags.INTERACT_INVENTORY_CLICK) {
            return TextComponent.builder("")
                    .append("Controls whether a player can click on an inventory slot.\n")
                    .append("Example", TextColor.LIGHT_PURPLE)
                    .append(" : To prevent players from clicking an inventory slot that contains diamond, enter\n")
                    .append("/cf interact-inventory-click minecraft:diamond false", TextColor.GREEN)
                    .build();
        }
        if (this == Flags.INTERACT_ITEM_PRIMARY) {
            return TextComponent.builder("")
                    .append("Controls whether a player can left-click(attack) with an item.\n")
                    .append("Example", TextColor.LIGHT_PURPLE)
                    .append(" : To prevent players from left-clicking while holding a diamond sword, enter\n")
                    .append("/cf interact-item-primary minecraft:diamond_sword false", TextColor.GREEN)
                    .build();
        }
        if (this == Flags.INTERACT_ITEM_SECONDARY) {
            return TextComponent.builder("")
                    .append("Controls whether a player can right-click with an item.\n")
                    .append("Example", TextColor.LIGHT_PURPLE)
                    .append(" : To prevent players from right-clicking while holding a flint and steel, enter\n")
                    .append("/cf interact-item-secondary minecraft:flint_and_steel false", TextColor.GREEN)
                    .build();
        }
        if (this == Flags.ITEM_DROP) {
            return TextComponent.builder("")
                    .append("Controls whether an item can be dropped.\n")
                    .append("Example", TextColor.LIGHT_PURPLE)
                    .append(" : To prevent tnt from dropping in the world, enter\n")
                    .append("/cf item-drop minecraft:tnt false", TextColor.GREEN)
                    .build();
        }
        if (this == Flags.ITEM_PICKUP) {
            return TextComponent.builder("")
                    .append("Controls whether an item can be picked up.\n")
                    .append("Example", TextColor.LIGHT_PURPLE)
                    .append(" : To prevent tnt from dropping in the world, enter\n")
                    .append("/cf item-drop minecraft:tnt false", TextColor.GREEN)
                    .build();
        }
        if (this == Flags.ITEM_SPAWN) {
            return TextComponent.builder("")
                    .append("Controls whether an item can be spawned into the world up.\n")
                    .append("Example", TextColor.LIGHT_PURPLE)
                    .append(" : To prevent feather's from dropping in the world, enter\n")
                    .append("/cf item-drop minecraft:feather false", TextColor.GREEN)
                    .build();
        }
        if (this == Flags.ITEM_USE) {
            return TextComponent.builder("")
                    .append("Controls whether an item can be used.\n")
                    .append("Example", TextColor.LIGHT_PURPLE)
                    .append(" : To prevent usage of diamond swords, enter\n")
                    .append("/cf item-use minecraft:diamond_sword false", TextColor.GREEN)
                    .build();
        }
        if (this == Flags.LEAF_DECAY) {
            return TextComponent.builder("")
                    .append("Controls whether leaves can decay in a world.\n")
                    .append("Example", TextColor.LIGHT_PURPLE)
                    .append(" : To prevent leaves from decaying, enter\n")
                    .append("/cf leaf-decay any false", TextColor.GREEN)
                    .build();
        }
        if (this == Flags.LIQUID_FLOW) {
            return TextComponent.builder("")
                    .append("Controls whether liquid is allowed to flow.\n")
                    .append("Example", TextColor.LIGHT_PURPLE)
                    .append(" : To prevent liquid flow, enter\n")
                    .append("/cf liquid-flow any false", TextColor.GREEN)
                    .build();
        }
        if (this == Flags.PORTAL_USE) {
            return TextComponent.builder("")
                    .append("Controls whether a portal can be used.\n")
                    .append("Example 1", TextColor.LIGHT_PURPLE)
                    .append(" : To prevent any source from using portals, enter\n")
                    .append("/cf portal-use any false\n", TextColor.GREEN)
                    .append("Example 2", TextColor.LIGHT_PURPLE)
                    .append(" : To prevent only players from using portals, enter\n")
                    .append("/cf portal-use minecraft:player any false", TextColor.GREEN)
                    .build();
        }
        if (this == Flags.PROJECTILE_IMPACT_BLOCK) {
            return TextComponent.builder("")
                    .append("Controls whether a projectile can impact(collide) with a block.\n")
                    .append("Note", TextColor.AQUA)
                    .append(" : This involves things such as potions, arrows, throwables, pixelmon pokeballs, etc.\n")
                    .append("Example 1", TextColor.LIGHT_PURPLE)
                    .append(" : To prevent any projectile from impacting a block, enter\n")
                    .append("/cf projectile-impact-block any false\n", TextColor.GREEN)
                    .append("Example 2", TextColor.LIGHT_PURPLE)
                    .append(" : To allow pixelmon pokeball's to impact blocks, enter\n")
                    .append("/cf projectile-impact-block pixelmon:occupiedpokeball any true", TextColor.GREEN)
                    .build();
        }
        if (this == Flags.PROJECTILE_IMPACT_ENTITY) {
            return TextComponent.builder("")
                    .append("Controls whether a projectile can impact(collide) with an entity.\n")
                    .append("Note", TextColor.AQUA)
                    .append(" : This involves things such as potions, arrows, throwables, pixelmon pokeballs, etc.\n")
                    .append("Example 1", TextColor.LIGHT_PURPLE)
                    .append(" : To prevent any projectile from impacting an entity, enter\n")
                    .append("/cf projectile-impact-entity any false\n", TextColor.GREEN)
                    .append("Example 2", TextColor.LIGHT_PURPLE)
                    .append(" : To allow arrows to impact entities, enter\n")
                    .append("/cf projectile-impact-entity minecraft:arrow any true", TextColor.GREEN)
                    .build();
        }

        return TextComponent.of("Not defined.");
    }

    @Override
    public boolean getDefaultClaimTypeValue(ClaimType type) {
        if (type == ClaimTypes.ADMIN || type == ClaimTypes.BASIC || type == ClaimTypes.TOWN) {
            switch (this.name) {
                case "block-break" :
                case "block-modify" :
                case "block-place" :
                case "collide-block" :
                case "collide-entity" :
                case "explosion-block" :
                case "explosion-entity" :
                case "fire-spread" :
                case "interact-block-primary" :
                case "interact-block-secondary" :
                case "interact-entity-primary" :
                case "interact-inventory" : 
                case "item-spawn" :
                case "liquid-flow" : 
                case "projectile-impact-block" :
                case "projectile-impact-entity" : 
                    return false;
                default :
                    return true;
            }
        }
        if (type == ClaimTypes.WILDERNESS) {
            switch (this.name) {
                case "fire-spread" :
                    return false;

                default :
                    return true;
            }
        }

        return true;
    }
}
