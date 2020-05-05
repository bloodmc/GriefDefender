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
package com.griefdefender.configuration.serializer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.google.common.reflect.TypeToken;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.ContextKeys;
import com.griefdefender.api.permission.flag.Flag;
import com.griefdefender.api.permission.flag.FlagData;
import com.griefdefender.api.permission.flag.FlagDefinition;
import com.griefdefender.permission.flag.GDFlagData;
import com.griefdefender.permission.flag.GDFlagDefinition;
import com.griefdefender.registry.FlagRegistryModule;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;

public class FlagDefinitionSerializer implements TypeSerializer<FlagDefinition> {

    @Override
    public FlagDefinition deserialize(TypeToken<?> type, ConfigurationNode node) throws ObjectMappingException {
        final String flagDisplayName = node.getKey().toString();
        final boolean enabled = node.getNode("enabled").getBoolean();
        final boolean adminGroup = node.getParent().getParent().getNode("admin-group").getBoolean();
        final String groupName = (String) node.getParent().getParent().getKey();
        final String descr = node.getNode("description").getString();
        Component description = TextComponent.empty();
        if (descr != null) {
            description = LegacyComponentSerializer.legacy().deserialize(descr, '&');
        }
        if (node.getNode("default-value").isVirtual()) {
            throw new ObjectMappingException("No 'default-value' found for flag definition '" + flagDisplayName + "' in group '" + groupName + "'. A default value is required and needs to be set to either 'true' or 'false'.");
        }
        final boolean defaultValue = node.getNode("default-value").getBoolean();

        List<String> contextList = node.getNode("contexts").getList(TypeToken.of(String.class));
        if (adminGroup && (contextList == null || contextList.isEmpty())) {
            throw new ObjectMappingException("No contexts found for admin flag definition '" + flagDisplayName + "' in group '" + groupName + "'. You must specify one of the following contexts :'gd_claim_default=<type>' , 'gd_claim_override=<type>', or 'gd_claim=claim'.");
        }
        List<String> permissionList = node.getNode("permissions").getList(TypeToken.of(String.class));
        if (permissionList == null) {
            throw new ObjectMappingException("No permissions found for flag definition '" + flagDisplayName + "'. You must specify at least 1 or more permissions.");
        }

        List<FlagData> flagDataList = new ArrayList<>();
        for (String permissionEntry : permissionList) {
            String permission = permissionEntry.replace(" ", "");
            String[] parts = permission.split(",");
            Flag linkedFlag = null;
            Set<Context> flagContexts = new HashSet<>();
            for (String part : parts) {
                String[] split =  part.split("=");
                String key = split[0];
                String value = split[1];
                // Handle linked Flag
                if (key.equalsIgnoreCase("flag")) {
                    final String flagName = value;
                    linkedFlag = FlagRegistryModule.getInstance().getById(flagName).orElse(null);
                    if (linkedFlag == null) {
                        throw new ObjectMappingException("Input '" + flagName + "' is not a valid GD flag to link to.");
                    }
                } else { //contexts
                    // validate context key
                    switch (key) {
                        case ContextKeys.SOURCE:
                        case ContextKeys.TARGET:
                            if (!value.contains(":") && !value.contains("#")) {
                                value = "minecraft:" + value;
                            }
                            flagContexts.add(new Context(key, value));
                            break;
                        case "used_item":
                        case "item_name":
                        case ContextKeys.STATE:
                            flagContexts.add(new Context(key, value));
                            break;
                        case "server":
                        case "world":
                        case ContextKeys.CLAIM_DEFAULT:
                        case ContextKeys.CLAIM_OVERRIDE:
                            // gd_claim contexts should always be set at the definition level
                            throw new ObjectMappingException("Invalid context '" + key + "' with value '" + value + "'.\nContext '" + key + "' can only be used for the definition.");
                        default:
                            throw new ObjectMappingException("Invalid context '" + key + "' with value '" + value + "'.");
                    }
                }
            }
            if (linkedFlag == null) {
                throw new ObjectMappingException("No linked flag specified. You need to specify 'flag=<flagname>'.");
            }

            final GDFlagData flagData = new GDFlagData(linkedFlag, flagContexts);
            flagDataList.add(flagData);
        }

        Set<Context> contexts = new HashSet<>();
        if (contextList != null) {
            for (String context : contextList) {
                final String parts[] = context.split("=");
                if (parts.length <= 1) {
                    throw new ObjectMappingException("Invalid context '" + context + "' for flag definition '" + flagDisplayName + "'. Skipping...");
                }
                final String key = parts[0];
                final String value = parts[1];
                if (key.equalsIgnoreCase("default") || key.equalsIgnoreCase("gd_claim_default")) {
                    if (!value.equalsIgnoreCase("global") && !value.equalsIgnoreCase("basic") && !value.equalsIgnoreCase("admin")
                            && !value.equalsIgnoreCase("subdivision") && !value.equalsIgnoreCase("town")) {
                        throw new ObjectMappingException("Invalid context '" + key + "' with value '" + value + "'.");
                    }
                    contexts.add(new Context("gd_claim_default", value));
                } else if (key.equalsIgnoreCase("override") || key.equalsIgnoreCase("gd_claim_override")) {
                    if (!value.equalsIgnoreCase("global") && !value.equalsIgnoreCase("basic") && !value.equalsIgnoreCase("admin")
                            && !value.equalsIgnoreCase("subdivision") && !value.equalsIgnoreCase("town")) {
                        // try UUID
                        if (value.length() == 36) {
                            try {
                                UUID.fromString(value);
                            } catch (IllegalArgumentException e) {
                                throw new ObjectMappingException("Invalid context '" + key + "' with value '" + value + "'.");
                            }
                        } else {
                            throw new ObjectMappingException("Invalid context '" + key + "' with value '" + value + "'.");
                        }
                    }
                    contexts.add(new Context("gd_claim_override", value));
                } else {
                    contexts.add(new Context(key, value));
                }
            }
        }

        final GDFlagDefinition flagDefinition = new GDFlagDefinition(flagDataList, flagDisplayName, description, groupName, adminGroup, contexts);
        flagDefinition.setIsEnabled(enabled);
        flagDefinition.setDefaultValue(Tristate.fromBoolean(defaultValue));
        return flagDefinition;
    }

    @Override
    public void serialize(TypeToken<?> type, FlagDefinition obj, ConfigurationNode node) throws ObjectMappingException {
        node.getNode("enabled").setValue(obj.isEnabled());
        node.getNode("default-value").setValue(obj.getDefaultValue().asBoolean());
        String description = "";
        if (obj.getDescription() != TextComponent.empty()) {
            description = LegacyComponentSerializer.legacy().serialize((Component) obj.getDescription(), '&');
            node.getNode("description").setValue(description);
        }

        if (!obj.getContexts().isEmpty()) {
            List<String> contextList = new ArrayList<>();
            ConfigurationNode contextNode = node.getNode("contexts");
            for (Context context : obj.getContexts()) {
                contextList.add(context.getKey().toLowerCase() + "=" + context.getValue().toLowerCase());
            }
            contextNode.setValue(contextList);
        }
        ConfigurationNode permissionNode = node.getNode("permissions");
        List<String> permissions = new ArrayList<>();
        for (FlagData flagData : obj.getFlagData()) {
            int count = 0;
            final Flag flag = flagData.getFlag();
            final Set<Context> dataContexts = flagData.getContexts();
            String permission = "";
            if (count > 0) {
                permission += ", ";
            }
            permission += "flag=" + flag.getName().toLowerCase();
            count++;

            for (Context context : dataContexts) {
                String key = context.getKey();
                permission += ", " + key + "=" + context.getValue();
            }

            permissions.add(permission);
        }
        permissionNode.setValue(permissions);
    }

}
