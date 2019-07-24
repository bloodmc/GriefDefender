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

    public static final String ABANDON_CLAIM_ADVERTISEMENT = "abandon-claim-advertisement";
    public static final String ABANDON_OTHER_SUCCESS = "abandon-other-success";
    public static final String ADJUST_BLOCKS_SUCCESS = "adjust-blocks-success";
    public static final String ADJUST_GROUP_BLOCKS_SUCCESS = "adjust-group-blocks-success";
    public static final String BLOCK_CLAIMED = "block-claimed";
    public static final String BLOCK_SALE_VALUE = "block-sale-value";
    public static final String BOOK_TOOLS = "book-tools";
    public static final String CLAIM_ABANDON_SUCCESS = "claim-abandon-success";
    public static final String CLAIM_ABOVE_LEVEL = "claim-above-level";
    public static final String CLAIM_BANK_INFO = "claim-bank-info";
    public static final String CLAIM_BELOW_LEVEL = "claim-below-level";
    public static final String CLAIM_SIZE_NEED_BLOCKS_2D = "claim-size-need-blocks-2d";
    public static final String CLAIM_SIZE_NEED_BLOCKS_3D = "claim-size-need-blocks-3d";
    public static final String CLAIM_RESIZE_SUCCESS_2D = "claim-resize-success-2d";
    public static final String CLAIM_RESIZE_SUCCESS_3D = "claim-resize-success-3d";
    public static final String CLAIM_SIZE_MAX_X = "claim-size-max-x";
    public static final String CLAIM_SIZE_MAX_Y = "claim-size-max-y";
    public static final String CLAIM_SIZE_MAX_Z = "claim-size-max-z";
    public static final String CLAIM_SIZE_MIN_X = "claim-size-min-x";
    public static final String CLAIM_SIZE_MIN_Y = "claim-size-min-y";
    public static final String CLAIM_SIZE_MIN_Z = "claim-size-min-z";
    public static final String CLAIM_SIZE_TOO_SMALL = "claim-size-too-small";
    public static final String CLAIM_CREATE_INSUFFICIENT_BLOCKS_2D = "claim-create-insufficient-blocks-2d";
    public static final String CLAIM_CREATE_INSUFFICIENT_BLOCKS_3D = "claim-create-insufficient-blocks-3d";
    public static final String ECONOMY_CLAIM_SALE_CONFIRMED = "economy-claim-sale-confirmed";
    public static final String ECONOMY_USER_NOT_FOUND = "economy-user-not-found";
    public static final String SCHEMATIC_RESTORE_CONFIRMED = "schematic-restore-confirmed";

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
            this.root = this.loader.load(ConfigurationOptions.defaults()
                    .setHeader(GriefDefenderPlugin.CONFIG_HEADER));
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
