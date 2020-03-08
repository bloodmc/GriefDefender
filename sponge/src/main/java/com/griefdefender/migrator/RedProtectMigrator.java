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
package com.griefdefender.migrator;

import com.google.common.reflect.TypeToken;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.claim.ClaimTypes;
import com.griefdefender.configuration.ClaimDataConfig;
import com.griefdefender.configuration.ClaimStorageData;
import com.griefdefender.internal.util.BlockUtil;
import com.griefdefender.util.PermissionUtil;
import com.griefdefender.util.PlayerUtil;
import net.kyori.text.TextComponent;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RedProtectMigrator {

    private static final String USERNAME_PATTERN = "[a-zA-Z0-9_]+";

    public static void migrate(World world, Path redProtectFilePath, Path gpClaimDataPath) throws FileNotFoundException, ClassNotFoundException {
        if (!GriefDefenderPlugin.getGlobalConfig().getConfig().migrator.redProtectMigrator) {
            return;
        }

        int count = 0;
        try {
            GriefDefenderPlugin.getInstance().getLogger().info("Starting RedProtect region data migration for world " + world.getProperties().getWorldName() + "...");
            ConfigurationLoader<CommentedConfigurationNode> regionManager = HoconConfigurationLoader.builder().setPath(redProtectFilePath).build();
            CommentedConfigurationNode region = regionManager.load();
            GriefDefenderPlugin.getInstance().getLogger().info("Scanning RedProtect regions in world data file '" + redProtectFilePath + "'...");
            for (Object key:region.getChildrenMap().keySet()){
                String rname = key.toString();
                if (!region.getNode(rname).hasMapChildren()){
                    continue;
                }
                int maxX = region.getNode(rname,"maxX").getInt();
                int maxY = region.getNode(rname,"maxY").getInt(255);
                int maxZ = region.getNode(rname,"maxZ").getInt();
                int minX = region.getNode(rname,"minX").getInt();
                int minY = region.getNode(rname,"minY").getInt(0);
                int minZ = region.getNode(rname,"minZ").getInt();
                List<String> owners = new ArrayList<>(region.getNode(rname, "owners").getList(TypeToken.of(String.class)));

                List<String> members = new ArrayList<>(region.getNode(rname, "members").getList(TypeToken.of(String.class)));
                
                String creator = region.getNode(rname,"creator").getString();             
                String welcome = region.getNode(rname,"welcome").getString();                 

                // create GP claim data file
                GriefDefenderPlugin.getInstance().getLogger().info("Migrating RedProtect region data '" + rname + "'...");
                UUID ownerUniqueId = null;
                if (validate(creator)) {
                    try {
                        // check cache first
                        ownerUniqueId = PermissionUtil.getInstance().lookupUserUniqueId(creator);
                        if (ownerUniqueId == null) {
                            ownerUniqueId = UUID.fromString(getUUID(creator));
                        }
                    } catch (Throwable e) {
                        // assume admin claim
                    }
                }

                UUID claimUniqueId = UUID.randomUUID();
                Location<World> lesserBoundaryCorner = new Location<>(world, minX, minY, minZ);
                Location<World> greaterBoundaryCorner = new Location<>(world, maxX, maxY, maxZ);
                Path claimFilePath = gpClaimDataPath.resolve(claimUniqueId.toString());
                if (!Files.exists(claimFilePath)) {
                    Files.createFile(claimFilePath);
                }

                ClaimStorageData claimStorage = new ClaimStorageData(claimFilePath, world.getUniqueId());
                ClaimDataConfig claimDataConfig = claimStorage.getConfig();
                claimDataConfig.setName(TextComponent.of(rname));
                claimDataConfig.setWorldUniqueId(world.getUniqueId());
                claimDataConfig.setOwnerUniqueId(ownerUniqueId);
                claimDataConfig.setLesserBoundaryCorner(BlockUtil.getInstance().posToString(lesserBoundaryCorner));
                claimDataConfig.setGreaterBoundaryCorner(BlockUtil.getInstance().posToString(greaterBoundaryCorner));
                claimDataConfig.setDateLastActive(Instant.now());
                claimDataConfig.setType(ownerUniqueId == null ? ClaimTypes.ADMIN : ClaimTypes.BASIC);
                if (!welcome.equals("")) {
                    claimDataConfig.setGreeting(TextComponent.of(welcome));
                }
                List<String> rpUsers = new ArrayList<>(owners);
                rpUsers.addAll(members);
                List<UUID> builders = claimDataConfig.getBuilders();
                for (String builder : rpUsers) {
                    if (!validate(builder)) {
                        continue;
                    }

                    UUID builderUniqueId;
                    try {
                        builderUniqueId = PermissionUtil.getInstance().lookupUserUniqueId(creator);
                        if (builderUniqueId == null) {
                            builderUniqueId = UUID.fromString(getUUID(builder));
                        }
                    } catch (Throwable e) {
                        GriefDefenderPlugin.getInstance().getLogger().error("Could not locate a valid UUID for user '" + builder + "' in region '" + rname + 
                                "'. Skipping...");
                        continue;
                    }
                    if (!builders.contains(builderUniqueId) && ownerUniqueId != null && !builderUniqueId.equals(ownerUniqueId)) {
                        builders.add(builderUniqueId);
                    }
                }

                claimDataConfig.setRequiresSave(true);
                claimStorage.save();
                GriefDefenderPlugin.getInstance().getLogger().info("Successfully migrated RedProtect region data '" + rname + "' to '" + claimFilePath + "'");
                count++;
            }
            GriefDefenderPlugin.getInstance().getLogger().info("Finished RedProtect region data migration for world '" + world.getProperties().getWorldName() + "'."
                    + " Migrated a total of " + count + " regions.");
        } catch (IOException | ObjectMappingException e) {
            e.printStackTrace();
        }
    }

    private static boolean validate(final String username){
        Matcher matcher = Pattern.compile(USERNAME_PATTERN).matcher(username);
        return matcher.matches();
    }

    // Below taken from https://github.com/FabioZumbi12/Sponge-Redprotect-19/blob/master/src/main/java/br/net/fabiozumbi12/redprotect/MojangUUIDs.java
    public static String getUUID(String player) {
        try {
          URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + player);
          BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
          String line = in.readLine();
          if (line == null){
             return null;
          }
         // JSONObject jsonProfile = (JSONObject) new JSONParser().parse(line);
          String name = "";//(String) jsonProfile.get("id");
          return toUUID(name);
        } catch (Exception ex) {
           ex.printStackTrace();
        }
        return null;
    }
    
    private static String toUUID(String uuid){
        return uuid.substring(0, 8) + "-" + uuid.substring(8, 12) + "-"
                   + uuid.substring(12, 16) + "-" + uuid.substring(16, 20)
                   + "-" + uuid.substring(20, 32);
    }
}
