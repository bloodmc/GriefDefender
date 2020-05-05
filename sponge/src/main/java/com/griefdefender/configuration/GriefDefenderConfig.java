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
import com.griefdefender.configuration.type.ConfigBase;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.Types;
import ninja.leaping.configurate.ValueType;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.commented.SimpleCommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMapper;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.util.ConfigurationNodeWalker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class GriefDefenderConfig<T extends ConfigBase> {

    private static final ConfigurationOptions LOADER_OPTIONS = ConfigurationOptions.defaults()
            .setHeader(GriefDefenderPlugin.CONFIG_HEADER);

    private final Path path;

    /**
     * The parent configuration - values are inherited from this
     */
    private final GriefDefenderConfig<?> parent;

    /**
     * The loader (mapped to a file) used to read/write the config to disk
     */
    private HoconConfigurationLoader loader;

    /**
     * A node representation of "whats actually in the file".
     */
    private CommentedConfigurationNode fileData = SimpleCommentedConfigurationNode.root(LOADER_OPTIONS);

    /**
     * A node representation of {@link #fileData}, merged with the data of {@link #parent}.
     */
    private CommentedConfigurationNode data = SimpleCommentedConfigurationNode.root(LOADER_OPTIONS);

    /**
     * The mapper instance used to populate the config instance
     */
    private ObjectMapper<T>.BoundInstance configMapper;

    public GriefDefenderConfig(Class<T> clazz, Path path, GriefDefenderConfig<?> parent) {
        this.parent = parent;
        this.path = path;

        try {
            if (Files.notExists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            if (Files.notExists(path)) {
                Files.createFile(path);
            }

            this.loader = HoconConfigurationLoader.builder().setPath(path).build();
            this.configMapper = ObjectMapper.forClass(clazz).bindToNew();

            load();
            // In order for the removeDuplicates method to function properly, it is extremely
            // important to avoid running save on parent BEFORE children save. Doing so will
            // cause duplicate nodes to not be removed properly as parent would have cleaned up
            // all duplicates prior.
            // To handle the above issue, we only call save for world configs during init.
            if (parent != null && parent.parent != null) {
                save();
            }
        } catch (Exception e) {
            GriefDefenderPlugin.getInstance().getLogger().error("Failed to load configuration at path " + path.toAbsolutePath(), e);
        }
    }

    public T getConfig() {
        return this.configMapper.getInstance();
    }

    public boolean save() {
        try {
            // save from the mapped object --> node
            CommentedConfigurationNode saveNode = SimpleCommentedConfigurationNode.root(LOADER_OPTIONS);
            this.configMapper.serialize(saveNode.getNode(GriefDefenderPlugin.MOD_ID));

            // before saving this config, remove any values already declared with the same value on the parent
            if (this.parent != null) {
                removeDuplicates(saveNode);
            }

            // save the data to disk
            this.loader.save(saveNode);

            // In order for the removeDuplicates method to function properly, it is extremely
            // important to avoid running save on parent BEFORE children save. Doing so will
            // cause duplicate nodes to not be removed as parent would have cleaned up
            // all duplicates prior.
            // To handle the above issue, we save AFTER saving child config.
            if (this.parent != null) {
                this.parent.save();
            }
            return true;
        } catch (IOException | ObjectMappingException e) {
            GriefDefenderPlugin.getInstance().getLogger().error("Failed to save configuration", e);
            return false;
        }
    }

    public void load() throws IOException, ObjectMappingException {
        // load settings from file
        CommentedConfigurationNode loadedNode = this.loader.load();

        // store "what's in the file" separately in memory
        this.fileData = loadedNode;

        // make a copy of the file data
        this.data = this.fileData.copy();

        // merge with settings from parent
        if (this.parent != null) {
            this.parent.load();
            this.data.mergeValuesFrom(this.parent.data);
        }

        // populate the config object
        populateInstance();
    }

    private void populateInstance() throws ObjectMappingException {
        this.configMapper.populate(this.data.getNode(GriefDefenderPlugin.MOD_ID));
    }

    /**
     * Traverses the given {@code root} config node, removing any values which
     * are also present and set to the same value on this configs "parent".
     *
     * @param root The node to process
     */
    private void removeDuplicates(CommentedConfigurationNode root) {
        if (this.parent == null) {
            throw new IllegalStateException("parent is null");
        }

        Iterator<ConfigurationNodeWalker.VisitedNode<CommentedConfigurationNode>> it = ConfigurationNodeWalker.DEPTH_FIRST_POST_ORDER.walkWithPath(root);
        while (it.hasNext()) {
            ConfigurationNodeWalker.VisitedNode<CommentedConfigurationNode> next = it.next();
            CommentedConfigurationNode node = next.getNode();

            // remove empty maps
            if (node.hasMapChildren()) {
                if (node.getChildrenMap().isEmpty()) {
                    node.setValue(null);
                }
                continue;
            }

            // ignore list values
            if (node.getParent() != null && node.getParent().getValueType() == ValueType.LIST) {
                continue;
            }

            // if the node already exists in the parent config, remove it
            CommentedConfigurationNode parentValue = this.parent.data.getNode(next.getPath().getArray());
            if (Objects.equals(node.getValue(), parentValue.getValue())) {
                node.setValue(null);
            } else {
                // Fix list bug
                if (parentValue.getValue() == null) {
                    if (node.getValueType() == ValueType.LIST) {
                        final List<?> nodeList = (List<?>) node.getValue();
                        if (nodeList.isEmpty()) {
                            node.setValue(null);
                        }
                        continue;
                    }
                }
                // Fix double bug
                final Double nodeVal = node.getValue(Types::asDouble);
                if (nodeVal != null) {
                    Double parentVal = parentValue.getValue(Types::asDouble);
                    if (parentVal == null && nodeVal.doubleValue() == 0 || (parentVal != null && nodeVal.doubleValue() == parentVal.doubleValue())) {
                        node.setValue(null);
                        continue;
                    }
                }
            }
        }
    }

    public CommentedConfigurationNode getRootNode() {
        return this.data.getNode(GriefDefenderPlugin.MOD_ID);
    }

    public Path getPath() {
        return this.path;
    }
}