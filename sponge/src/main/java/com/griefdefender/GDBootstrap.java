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

import com.google.inject.Inject;
import com.griefdefender.util.BootstrapUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.spongepowered.api.MinecraftVersion;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Plugin(id = "griefdefender", name = "GriefDefender", version = "1.2.6", description = "Designed to defend servers from all forms of grief.",
    authors = {"bloodmc"}, dependencies = {@Dependency(id = "luckperms")})
public class GDBootstrap {

    @Inject public PluginContainer pluginContainer;
    @Inject private Logger logger;
    @Inject @ConfigDir(sharedRoot = false)
    private Path configPath;

    private Map<String, File> jarMap = new HashMap<>();
    private List<String> relocateList = new ArrayList<>();
    private static GDBootstrap instance;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.7 (KHTML, like Gecko) Chrome/16.0.912.75 Safari/535.7";

    public static GDBootstrap getInstance() {
        return instance;
    }

    @Listener(order = Order.LAST)
    public void onPreInit(GamePreInitializationEvent event) {
        instance = this;
        final JSONParser parser = new JSONParser();
        String bukkitJsonVersion = null;
        this.getLogger().info("Loading libraries...");
        final MinecraftVersion version = Sponge.getPlatform().getMinecraftVersion();
        if (Sponge.getPlatform().getMinecraftVersion().getName().contains("1.12.2")) {
            bukkitJsonVersion = "1.12.2";
        } else {
            this.getLogger().error("Detected unsupported version '" + version.getName() + "'. GriefDefender only 1.12.2. GriefDefender will NOT load.");
            return;
        }
        try {
            final InputStream in = getClass().getResourceAsStream("/" + bukkitJsonVersion + ".json");
            final BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            final JSONObject a = (JSONObject) parser.parse(reader);
            final JSONArray libraries = (JSONArray) a.get("libraries");
            if (libraries == null) {
                this.getLogger().error("Resource " + bukkitJsonVersion + ".json is corrupted!. Please contact author for assistance.");
                return;
            }
            final Path LIB_ROOT_PATH = instance.configPath.resolve("lib");
            final Iterator<JSONObject> iterator = libraries.iterator();
            while (iterator.hasNext()) {
                JSONObject lib = iterator.next();
                final String name = (String) lib.get("name");
                final String sha1 = (String) lib.get("sha1");
                final String path = (String) lib.get("path");
                final String relocate = (String) lib.get("relocate");
                final String url = (String) lib.get("url");
                final Path libPath = LIB_ROOT_PATH.resolve(path);
                downloadLibrary(name, relocate, sha1, url, libPath);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        // Inject jar-relocator and asm debug
        injectRelocatorDeps();
        // Relocate all GD dependencies and inject
        GDRelocator.getInstance().relocateJars(this.jarMap);
        // Boot GD
        GriefDefenderPlugin.getInstance().onPreInit(event, this.logger, this.configPath, this.pluginContainer);
        //Sponge.getEventManager().registerListeners(GriefDefenderPlugin.getInstance(), GriefDefenderPlugin.getInstance());
    }

    public List<String> getRelocateList() {
        return this.relocateList;
    }

    private void injectRelocatorDeps() {
        String name = "org.ow2.asm:asm-debug-all:5.2";
        File file = this.jarMap.get(name);
        BootstrapUtil.addUrlToClassLoader(name, file);
        name = "me.lucko:jar-relocator:1.3";
        file = this.jarMap.get(name);
        BootstrapUtil.addUrlToClassLoader(name, file);
        // inject reflect helper
        final String javaVersion = System.getProperty("java.version");
        if (getJavaVersion() >= 11) {
            name = "com.griefdefender:reflect-helper:2.0";
        } else {
            name = "com.griefdefender:reflect-helper:1.0";
        }
        file = this.jarMap.get(name);
        BootstrapUtil.addUrlToClassLoader(name, file);
    }

    public void downloadLibrary(String name, String relocate, String sha1, String url, Path libPath) {
        final File file = libPath.toFile();
        this.jarMap.put(name, file);
        if (relocate != null && !relocate.isEmpty() && relocate.contains(":")) {
            this.relocateList.add(relocate);
        }
        if (!Files.exists(libPath)) {
            this.getLogger().info("Downloading library " + name + " ...");
            try {
                URL website = new URL(url);
                URLConnection urlConnection = website.openConnection();
                // Some maven repos like nexus require a user agent so we just pass one to satisfy it
                urlConnection.setRequestProperty("User-Agent", USER_AGENT);
                ReadableByteChannel rbc = Channels.newChannel(urlConnection.getInputStream());
                if (!Files.exists(libPath)) {
                    file.getParentFile().mkdirs();
                }
                FileOutputStream fos = new FileOutputStream(file);
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                fos.close();
            } catch (IOException e) {
                this.getLogger().error("An error occured while downloading library '" + name + "'. Skipping...");
                e.printStackTrace();
                return;
            }

            
            final String hash = getLibraryHash(file);

            if (hash == null || !sha1.equals(hash)) {
                this.getLogger().error("Detected invalid hash '" + hash + "' for file '" + libPath + "'. Expected '" + sha1 + "'. Skipping...");
                try {
                    Files.delete(libPath);
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }

        this.jarMap.put(name, file);
    }

    private String getLibraryHash(File file) {
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA-1");
            final byte[] data = Files.readAllBytes(file.toPath());
            final byte[] b = md.digest(data);
            StringBuffer buffer = new StringBuffer();
            for (int i = 0; i < b.length; i++) {
                if ((0xff & b[i]) < 0x10) {
                    buffer.append("0" + Integer.toHexString((0xFF & b[i])));
                } else {
                    buffer.append(Integer.toHexString(0xFF & b[i]));
                }
            }
            return buffer.toString();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }

    public Logger getLogger() {
        return this.logger;
    }

    private static int getJavaVersion() {
        String version = System.getProperty("java.version");
        if(version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            final int dot = version.indexOf(".");
            if(dot != -1) { 
                version = version.substring(0, dot);
            }
        }
        return Integer.parseInt(version);
    }
}
