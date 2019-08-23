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
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.ContextKeys;
import com.griefdefender.api.permission.flag.Flag;
import com.griefdefender.permission.flag.CustomFlagData;
import com.griefdefender.permission.flag.GDCustomFlagDefinition;
import com.griefdefender.registry.FlagRegistryModule;

import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;

public class CustomFlagSerializer implements TypeSerializer<GDCustomFlagDefinition> {

    private static final String INVALID_GD_CONTEXT = "Invalid 'gd-context' specified."
            + "\nAccepted values are : "
            + "\ngd_claim:<UUID>"
            + "\ngd_claim_default:<type>"
            + "\ngd_claim_override:<type>|<UUID>";
    //private static final Pattern DATA_PATTERN = Pattern.compile("^?\\[ *((?:[\\w.-]+=[\\#\\w.-]+(?::[\\#\\w\\/.-]+)? *(?:, *(?!\\]$)|(?=\\]$)))+) *\\]$");
    //private static final Pattern DATA_PATTERN = Pattern.compile("^?\\( *((?:[\\w.-]+=[\\w.-]+(?::[\\w\\/.-]+)? *(?:, *(?!\\)$)|(?=\\)$)))+) *\\)$");

    @Override
    public GDCustomFlagDefinition deserialize(TypeToken<?> type, ConfigurationNode node) throws ObjectMappingException {
        final String flagDisplayName = node.getKey().toString();
        final boolean enabled = node.getNode("enabled").getBoolean();
        final String gdContext = node.getNode("gd-context").getString();
        final String descr = node.getNode("description").getString();
        Component description = TextComponent.empty();
        if (descr != null) {
            description = LegacyComponentSerializer.legacy().deserialize(descr, '&');
        }

        List<String> permissionList = node.getNode("permissions").getList(TypeToken.of(String.class));
        List<CustomFlagData> flagDataList = new ArrayList<>();
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
                        case ContextKeys.CLAIM_DEFAULT:
                        case ContextKeys.CLAIM_OVERRIDE:
                        case ContextKeys.STATE:
                            flagContexts.add(new Context(key, value));
                            break;
                        default:
                            throw new ObjectMappingException("Invalid context '" + key + "' with value '" + value + "'.");
                    }
                }
            }
            if (linkedFlag == null) {
                throw new ObjectMappingException("No linked flag specified. You need to specify 'flag=<flagname>'.");
            }

            flagDataList.add(new CustomFlagData(linkedFlag, flagContexts));
        }
        final GDCustomFlagDefinition flagDefinition = new GDCustomFlagDefinition(flagDataList, flagDisplayName, description);
        flagDefinition.setIsEnabled(enabled);
        if (gdContext != null) {
            final String parts[] = gdContext.split(":");
            if (parts.length <= 1) {
                throw new ObjectMappingException(INVALID_GD_CONTEXT);
            }
            final String key = parts[0];
            final String value = parts[1];
            switch (key) {
                case "gd_claim" :
                    UUID uuid = null;
                    try {
                        uuid = UUID.fromString(value);
                    } catch (IllegalArgumentException e) {
                        throw new ObjectMappingException(INVALID_GD_CONTEXT);
                    }
                    break;
                case "gd_claim_default" :
                    if (!value.equalsIgnoreCase("global") && !value.equalsIgnoreCase("basic") && !value.equalsIgnoreCase("admin")
                            && !value.equalsIgnoreCase("subdivision") && !value.equalsIgnoreCase("town")) {
                        throw new ObjectMappingException(INVALID_GD_CONTEXT);
                    }
                    break;
                case "gd_claim_override" :
                    if (!value.equalsIgnoreCase("global") && !value.equalsIgnoreCase("basic") && !value.equalsIgnoreCase("admin")
                            && !value.equalsIgnoreCase("subdivision") && !value.equalsIgnoreCase("town")) {
                        // try UUID
                        uuid = null;
                        try {
                            uuid = UUID.fromString(value);
                        } catch (IllegalArgumentException e) {
                            throw new ObjectMappingException(INVALID_GD_CONTEXT);
                        }
                    }
                    break;
                default : 
                    throw new ObjectMappingException(INVALID_GD_CONTEXT);
            }
            flagDefinition.setGDContext(new Context(key, value));
        }
        return flagDefinition;
    }

    @Override
    public void serialize(TypeToken<?> type, GDCustomFlagDefinition obj, ConfigurationNode node) throws ObjectMappingException {
        node.getNode("enabled").setValue(obj.isEnabled());
        String description = "";
        if (obj.getDescription() != TextComponent.empty()) {
            description = LegacyComponentSerializer.legacy().serialize((Component) obj.getDescription(), '&');
            node.getNode("description").setValue(description);
        }
        if (obj.getGDContext() != null) {
            node.getNode("gd-context").setValue(obj.getGDContext().getKey() + ":" + obj.getGDContext().getValue());
        }
        ConfigurationNode permissionNode = node.getNode("permissions");
        List<String> permissions = new ArrayList<>();
        for (CustomFlagData flagData : obj.getFlagData()) {
            int count = 0;
            final Flag flag = flagData.getFlag();
            final Set<Context> contexts = flagData.getContexts();
            String permission = "";
            if (count > 0) {
                permission += ", ";
            }
            permission += "flag=" + flag.getName().toLowerCase();
            count++;

            for (Context context : contexts) {
                String key = context.getKey();
                permission += ", " + key + "=" + context.getValue();
            }
            permissions.add(permission);
        }
        permissionNode.setValue(permissions);
    }

}
