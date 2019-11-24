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

public class PlayerStorageData {

    private HoconConfigurationLoader loader;
    private CommentedConfigurationNode root = SimpleCommentedConfigurationNode.root(ConfigurationOptions.defaults());
    private ObjectMapper<PlayerDataConfig>.BoundInstance configMapper;
    private PlayerDataConfig configBase;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public PlayerStorageData(Path path) {

        try {
            if (Files.notExists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            if (Files.notExists(path)) {
                Files.createFile(path);
            }

            this.loader = HoconConfigurationLoader.builder().setPath(path).build();
            this.configMapper = (ObjectMapper.BoundInstance) ObjectMapper.forClass(PlayerDataConfig.class).bindToNew();
            this.root = this.loader.load(ConfigurationOptions.defaults());
            CommentedConfigurationNode rootNode = this.root.getNode(GriefDefenderPlugin.MOD_ID);
            // Check if server is using existing Sponge GP data
            if (rootNode.isVirtual()) {
                // check GriefPrevention
                CommentedConfigurationNode gpRootNode = this.root.getNode("GriefPrevention");
                if (!gpRootNode.isVirtual()) {
                    rootNode.setValue(gpRootNode.getValue());
                    gpRootNode.setValue(null);
                }
            }
            this.configBase = this.configMapper.populate(rootNode);
            save();
        } catch (Exception e) {
            GriefDefenderPlugin.getInstance().getLogger().error("Failed to initialize configuration", e);
        }
    }

    public PlayerDataConfig getConfig() {
        return this.configBase;
    }

    public void save() {
        try {
            if (this.configBase != null) {
                if (this.configBase.requiresSave()) {
                    this.configMapper.serialize(this.root.getNode(GriefDefenderPlugin.MOD_ID));
                    this.loader.save(this.root);
                    this.configBase.setRequiresSave(false);
                }
            }
        } catch (IOException | ObjectMappingException e) {
            GriefDefenderPlugin.getInstance().getLogger().error("Failed to save configuration", e);
        }
    }

    public boolean reload() {
        try {
            this.root = this.loader.load(ConfigurationOptions.defaults());
            this.configBase = this.configMapper.populate(this.root.getNode(GriefDefenderPlugin.MOD_ID));
        } catch (Exception e) {
            GriefDefenderPlugin.getInstance().getLogger().error("Failed to load configuration", e);
            return false;
        }
        return true;
    }

    public CommentedConfigurationNode getRootNode() {
        return this.root.getNode(GriefDefenderPlugin.MOD_ID);
    }
}
