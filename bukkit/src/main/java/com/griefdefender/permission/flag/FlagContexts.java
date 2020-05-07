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

import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.ContextKeys;

public class FlagContexts {

    public static final Context SOURCE_PLAYER = new Context(ContextKeys.SOURCE, "minecraft:player");
    public static final Context SOURCE_TNT = new Context(ContextKeys.SOURCE, "minecraft:tnt");
    public static final Context SOURCE_CREEPER = new Context(ContextKeys.SOURCE, "minecraft:creeper");
    public static final Context SOURCE_ENDERDRAGON = new Context(ContextKeys.SOURCE, "minecraft:enderdragon");
    public static final Context SOURCE_GHAST = new Context(ContextKeys.SOURCE, "minecraft:ghast");
    public static final Context SOURCE_ENDERMAN = new Context(ContextKeys.SOURCE, "minecraft:enderman");
    public static final Context SOURCE_SNOWMAN = new Context(ContextKeys.SOURCE, "minecraft:snowman");
    public static final Context SOURCE_WITHER = new Context(ContextKeys.SOURCE, "minecraft:wither");
    public static final Context SOURCE_LAVA_1_12 = new Context(ContextKeys.SOURCE, "minecraft:flowing_lava");
    public static final Context SOURCE_WATER_1_12 = new Context(ContextKeys.SOURCE, "minecraft:flowing_water");
    public static final Context SOURCE_LAVA = new Context(ContextKeys.SOURCE, "minecraft:lava");
    public static final Context SOURCE_WATER = new Context(ContextKeys.SOURCE, "minecraft:water");
    public static final Context SOURCE_LIGHTNING_BOLT = new Context(ContextKeys.SOURCE, "minecraft:lightning_bolt");
    public static final Context SOURCE_FALL = new Context(ContextKeys.SOURCE, "minecraft:fall");
    public static final Context SOURCE_FIRE = new Context(ContextKeys.SOURCE, "minecraft:fire");
    public static final Context SOURCE_FIREWORKS = new Context(ContextKeys.SOURCE, "minecraft:fireworks");
    public static final Context SOURCE_ICE = new Context(ContextKeys.SOURCE, "minecraft:ice");
    public static final Context SOURCE_PISTON = new Context(ContextKeys.SOURCE, "minecraft:piston");
    public static final Context SOURCE_VINE = new Context(ContextKeys.SOURCE, "minecraft:vine");
    public static final Context SOURCE_TYPE_MONSTER = new Context(ContextKeys.SOURCE, "#monster");

    // Block States
    public static final Context STATE_FARMLAND_DRY = new Context("state", "moisture:0");

    // Targets
    public static final Context TARGET_AIR = new Context(ContextKeys.TARGET, "minecraft:air");
    public static final Context TARGET_BED = new Context(ContextKeys.TARGET, "minecraft:bed");
    public static final Context TARGET_BOAT = new Context(ContextKeys.TARGET, "minecraft:boat");
    public static final Context TARGET_CHEST = new Context(ContextKeys.TARGET, "minecraft:chest");
    public static final Context TARGET_CHORUS_FRUIT = new Context(ContextKeys.TARGET, "minecraft:chorus_fruit");
    public static final Context TARGET_ENDERPEARL = new Context(ContextKeys.TARGET, "minecraft:enderpearl");
    public static final Context TARGET_FARMLAND = new Context(ContextKeys.TARGET, "minecraft:farmland");
    public static final Context TARGET_FLINTANDSTEEL = new Context(ContextKeys.TARGET, "minecraft:flint_and_steel");
    public static final Context TARGET_GRASS= new Context(ContextKeys.TARGET, "minecraft:grass");
    public static final Context TARGET_ITEM_FRAME = new Context(ContextKeys.TARGET, "minecraft:item_frame");
    public static final Context TARGET_LAVA_BUCKET = new Context(ContextKeys.TARGET, "minecraft:lava_bucket");
    public static final Context TARGET_MINECART = new Context(ContextKeys.TARGET, "minecraft:minecart");
    public static final Context TARGET_MYCELIUM = new Context(ContextKeys.TARGET, "minecraft:mycelium");
    public static final Context TARGET_PAINTING = new Context(ContextKeys.TARGET, "minecraft:painting");
    public static final Context TARGET_PISTON = new Context(ContextKeys.TARGET, "minecraft:piston");
    public static final Context TARGET_PLAYER = new Context(ContextKeys.TARGET, "minecraft:player");
    public static final Context TARGET_ICE_FORM = new Context(ContextKeys.TARGET, "minecraft:ice");
    public static final Context TARGET_ICE_MELT = new Context(ContextKeys.TARGET, "minecraft:water");
    public static final Context TARGET_SNOW_1_12 = new Context(ContextKeys.TARGET, "minecraft:snow_layer");
    public static final Context TARGET_SNOW = new Context(ContextKeys.TARGET, "minecraft:snow");
    public static final Context TARGET_TURTLE_EGG = new Context(ContextKeys.TARGET, "minecraft:turtle_egg");
    public static final Context TARGET_VINE = new Context(ContextKeys.TARGET, "minecraft:vine");
    public static final Context TARGET_WATER_BUCKET = new Context(ContextKeys.TARGET, "minecraft:water_bucket");
    public static final Context TARGET_XP_ORB = new Context(ContextKeys.TARGET, "minecraft:xp_orb");
    public static final Context TARGET_TYPE_ANIMAL = new Context(ContextKeys.TARGET, "#animal");
    public static final Context TARGET_TYPE_CROP = new Context(ContextKeys.TARGET, "#crop");
    public static final Context TARGET_TYPE_AMBIENT = new Context(ContextKeys.TARGET, "#ambient");
    public static final Context TARGET_TYPE_AQUATIC = new Context(ContextKeys.TARGET, "#aquatic");
    public static final Context TARGET_TYPE_MONSTER = new Context(ContextKeys.TARGET, "#monster");
    public static final Context TARGET_TYPE_MUSHROOM = new Context(ContextKeys.TARGET, "#mushroom");
    public static final Context TARGET_TYPE_PORTAL = new Context(ContextKeys.TARGET, "#portal");
    public static final Context TARGET_TYPE_VEHICLE = new Context(ContextKeys.TARGET, "#vehicle");

    public static final Context USED_ITEM_LAVA_BUCKET = new Context(ContextKeys.USED_ITEM, "minecraft:lava_bucket");
    public static final Context USED_ITEM_VEHICLE = new Context(ContextKeys.USED_ITEM, "#vehicle");
    public static final Context USED_ITEM_WATER_BUCKET = new Context(ContextKeys.USED_ITEM, "minecraft:water_bucket");
}
