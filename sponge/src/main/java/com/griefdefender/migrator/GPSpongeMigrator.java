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

import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.permission.ContextKeys;
import com.griefdefender.api.permission.flag.Flag;
import com.griefdefender.registry.FlagRegistryModule;
import com.griefdefender.util.PermissionUtil;
import org.apache.commons.io.FileUtils;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.util.Tristate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GPSpongeMigrator {

    final String regex = "^([^\\.]+).([^\\.]+).([^\\.]+)";
    final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);

    private static GPSpongeMigrator instance;
    public PermissionService permissionService = GriefDefenderPlugin.getInstance().permissionService;
    final Path GD_DATA_ROOT_PATH = GriefDefenderPlugin.getInstance().getConfigPath();
    final Path GD_GLOBAL_PLAYER_DATA_PATH = GD_DATA_ROOT_PATH.resolve("GlobalPlayerData");
    final Path GD_CLAIM_DATA_PATH = GD_DATA_ROOT_PATH.resolve("worlds");
    final Path GP_DATA_ROOT_PATH = GD_DATA_ROOT_PATH.getParent().resolve("griefprevention");
    final Path GP_GLOBAL_PLAYER_DATA_PATH = GP_DATA_ROOT_PATH.resolve("GlobalPlayerData");
    final Path GP_CLAIM_DATA_PATH = GP_DATA_ROOT_PATH.resolve("worlds");

    public void migrateData() {
        try {
            if (Files.exists(GP_GLOBAL_PLAYER_DATA_PATH)) {
                if (Files.isDirectory(GP_GLOBAL_PLAYER_DATA_PATH)) {
                    FileUtils.copyDirectory(GP_GLOBAL_PLAYER_DATA_PATH.toFile(), GD_GLOBAL_PLAYER_DATA_PATH.toFile(), true);
                } else {
                    // Support symlinks
                    FileUtils.copyFile(GP_GLOBAL_PLAYER_DATA_PATH.toFile(), GD_GLOBAL_PLAYER_DATA_PATH.toFile(), true);
                }
            }
            FileUtils.copyDirectory(GP_CLAIM_DATA_PATH.toFile(), GD_CLAIM_DATA_PATH.toFile(), true);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        final File dataPath = GD_CLAIM_DATA_PATH.toFile();
        final File[] files = dataPath.listFiles();

        GriefDefenderPlugin.getInstance().executor.execute(() -> {
            final CompletableFuture<Set<String>> future = permissionService.getGroupSubjects().getAllIdentifiers();
            future.thenAccept(groups -> {
                int count = 0;
                final int size = groups.size();
                List<Integer> printedSizes = new CopyOnWriteArrayList<>();
                for (String group : groups) {
                    count++;
                    final Subject groupSubject = permissionService.getGroupSubjects().getSubject(group).orElse(null);
                    final int current = count;
                    if (groupSubject == null) {
                        permissionService.getGroupSubjects().loadSubject(group).thenAccept(s -> {
                            migrateSubject(s, true);
                            final int printSize = (current*100/size);
                            if (!printedSizes.contains(printSize)) {
                                GriefDefenderPlugin.getInstance().getLogger().info("Performing group permission migration: " + printSize + "%");
                                printedSizes.add(printSize);
                            }
                        });
                    } else {
                        migrateSubject(groupSubject, true);
                        final int printSize = (current*100/size);
                        if (!printedSizes.contains(printSize)) {
                            GriefDefenderPlugin.getInstance().getLogger().info("Performing group permission migration: " + printSize + "%");
                            printedSizes.add(printSize);
                        }
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            final CompletableFuture<Set<String>> futureUsers = permissionService.getUserSubjects().getAllIdentifiers();
            futureUsers.thenAccept(users -> {
                int count = 0;
                final int size = users.size();
                List<Integer> printedSizes = new CopyOnWriteArrayList<>();
                for (String user : users) {
                    count++;
                    final Subject userSubject = permissionService.getUserSubjects().getSubject(user).orElse(null);
                    final int current = count;
                    if (userSubject == null) {
                        permissionService.getUserSubjects().loadSubject(user).thenAccept(s -> {
                            migrateSubject(s, false);
                            final int printSize = (current*100/size);
                            if (!printedSizes.contains(printSize)) {
                                GriefDefenderPlugin.getInstance().getLogger().info("Performing user permission migration: " + printSize + "%");
                                printedSizes.add(printSize);
                            }
                        });
                    } else {
                        migrateSubject(userSubject, false);
                        final int printSize = (current*100/size);
                        if (!printedSizes.contains(printSize) && printSize != 100) {
                            GriefDefenderPlugin.getInstance().getLogger().info("Performing user permission migration: " + printSize + "%");
                            printedSizes.add(printSize);
                        }
                    }
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                // migrate default user
                migrateSubject(GriefDefenderPlugin.getInstance().permissionService.getDefaults(), false, true);
                GriefDefenderPlugin.getInstance().getLogger().info("Performing user permission migration: 100%");
            });
        });
    }

    private void migrateFolderData(File[] files) {
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isDirectory()) {
                migrateFolderData(file.listFiles());
            }
        }
    }

    public Consumer<Subject> migrateSubjectPermissions(boolean isGroup) {
        return subject -> {
            migrateSubject(subject, isGroup);
        };
    }

    public void migrateSubject(Subject subject, boolean isGroup) {
        migrateSubject(subject, isGroup, false);
    }

    public void migrateSubject(Subject subject, boolean isGroup, boolean isDefault) {
        boolean migrated = false;
        try {
            for (Map.Entry<Set<Context>, Map<String, Boolean>> mapEntry : subject.getSubjectData().getAllPermissions().entrySet()) {
                final Set<Context> originalContexts = mapEntry.getKey();
                Iterator<Map.Entry<String, Boolean>> iterator = mapEntry.getValue().entrySet().iterator();
                Set<Context> gdContexts = new HashSet<>();
                for (Context context : originalContexts) {
                    if (context.getKey().equalsIgnoreCase("gp_claim_overrides")) {
                        gdContexts.add(new Context("gd_claim_override", context.getValue()));
                    } else if (context.getKey().equalsIgnoreCase("gp_claim_defaults")) {
                        gdContexts.add(new Context("gd_claim_default", context.getValue()));
                    } else if (context.getKey().equalsIgnoreCase("gp_claim")) {
                        gdContexts.add(new Context("gd_claim", context.getValue()));
                    } else {
                        gdContexts.add(context);
                    }
                }
                while (iterator.hasNext()) {
                    final Map.Entry<String, Boolean> entry = iterator.next();
                    final String originalPermission = entry.getKey();
                    if (!originalPermission.startsWith("griefprevention")) {
                        continue;
                    }
                    final String currentPermission = originalPermission.replace("griefprevention", "griefdefender").replace("fire-spread", "block-spread");
                    final Matcher matcher = pattern.matcher(currentPermission);
                    String flagBase = "";
                    if (matcher.find()) {
                        flagBase = matcher.group(0);
                    }
                    final Flag flag = FlagRegistryModule.getInstance().getById(flagBase).orElse(null);
                    if (flag == null) {
                        GriefDefenderPlugin.getInstance().getLogger().info("Detected legacy permission '" + originalPermission + "' on subject " + subject.getFriendlyIdentifier().orElse(subject.getIdentifier()) + "'. Migrating...");
                        if (isDefault) {
                            PermissionUtil.getInstance().setPermissionValue(GriefDefenderPlugin.DEFAULT_HOLDER, originalPermission, com.griefdefender.api.Tristate.UNDEFINED, this.getGDContexts(originalContexts));
                        } else {
                            subject.getSubjectData().setPermission(originalContexts, originalPermission, Tristate.UNDEFINED);
                        }
                        GriefDefenderPlugin.getInstance().getLogger().info("Removed legacy permission '" + originalPermission + "'.");
                        if (isDefault) {
                            PermissionUtil.getInstance().setPermissionValue(GriefDefenderPlugin.DEFAULT_HOLDER, currentPermission, com.griefdefender.api.Tristate.fromBoolean(entry.getValue()), this.getGDContexts(gdContexts));
                        } else {
                            subject.getSubjectData().setPermission(gdContexts, currentPermission, Tristate.fromBoolean(entry.getValue()));
                        }
                        GriefDefenderPlugin.getInstance().getLogger().info("Set new permission '" + currentPermission + "' with contexts " + gdContexts);
                        GriefDefenderPlugin.getInstance().getLogger().info("Successfully migrated permission " + currentPermission + " to " + currentPermission + " with contexts " + gdContexts);
                    } else {
                        GriefDefenderPlugin.getInstance().getLogger().info("Detected legacy flag permission '" + originalPermission + "' on subject " + subject.getFriendlyIdentifier().orElse(subject.getIdentifier()) + "'. Migrating...");
                        if (isDefault) {
                            PermissionUtil.getInstance().setPermissionValue(GriefDefenderPlugin.DEFAULT_HOLDER, originalPermission, com.griefdefender.api.Tristate.UNDEFINED, this.getGDContexts(originalContexts));
                        } else {
                            subject.getSubjectData().setPermission(originalContexts, originalPermission, Tristate.UNDEFINED);
                        }
                        GriefDefenderPlugin.getInstance().getLogger().info("Removed legacy flag permission '" + originalPermission + "'.");
                        Set<Context> newContextSet = new HashSet<>(gdContexts);
                        applyContexts(flagBase, currentPermission, newContextSet);
                        if (isDefault) {
                            PermissionUtil.getInstance().setPermissionValue(GriefDefenderPlugin.DEFAULT_HOLDER, flag.getPermission(), com.griefdefender.api.Tristate.fromBoolean(entry.getValue()), this.getGDContexts(newContextSet));
                        } else {
                            subject.getSubjectData().setPermission(newContextSet, flag.getPermission(), Tristate.fromBoolean(entry.getValue()));
                        }
                        GriefDefenderPlugin.getInstance().getLogger().info("Set new flag permission '" + flag.getPermission() + "' with contexts " + newContextSet);
                        GriefDefenderPlugin.getInstance().getLogger().info("Successfully migrated flag permission " + currentPermission + " to " + flag.getPermission() + " with contexts " + newContextSet);
                    }
                    migrated = true;
                }
            }
            for (Map.Entry<Set<Context>, Map<String, String>> mapEntry : subject.getSubjectData().getAllOptions().entrySet()) {
                final Set<Context> originalContexts = mapEntry.getKey();
                Iterator<Map.Entry<String, String>> iterator = mapEntry.getValue().entrySet().iterator();
                Set<Context> gdContexts = new HashSet<>();
                boolean hasClaimContext = false;
                for (Context context : originalContexts) {
                    if (context.getKey().equalsIgnoreCase("gp_claim_overrides")) {
                        gdContexts.add(new Context("gd_claim_override", context.getValue()));
                    } else if (context.getKey().equalsIgnoreCase("gp_claim_defaults")) {
                        gdContexts.add(new Context("gd_claim_default", context.getValue()));
                    } else if (context.getKey().equalsIgnoreCase("gp_claim")) {
                        gdContexts.add(new Context("gd_claim", context.getValue()));
                        hasClaimContext = true;
                    } else {
                        gdContexts.add(context);
                    }
                }
                while (iterator.hasNext()) {
                    final Set<Context> optionContexts = new HashSet<>(gdContexts);
                    final Map.Entry<String, String> entry = iterator.next();
                    final String originalPermission = entry.getKey();
                    if (!originalPermission.startsWith("griefprevention")) {
                        continue;
                    }
                    String currentPermission = originalPermission
                            .replace("griefprevention", "griefdefender")
                            .replace("claim-expiration-chest", "chest-expiration")
                            .replace("claim-expiration", "expiration")
                            .replace("claim-create-mode", "create-mode")
                            .replace("create-claim-limit", "create-limit")
                            .replace("initial-claim-blocks", "initial-blocks")
                            .replace("max-accrued-claim-blocks", "max-accrued-blocks")
                            .replace("min-claim-level", "min-level")
                            .replace("max-claim-level", "max-level")
                            .replace("min-claim-size", "min-size")
                            .replace("max-claim-size", "max-size")
                            .replaceAll("\\-basic[^-]*", "")
                            .replaceAll("\\-subdivision[^-]*", "")
                            .replaceAll("\\-town[^-]*", "");
                    if (!hasClaimContext) {
                        if (originalPermission.contains("-basic")) {
                            optionContexts.add(new Context("gd_claim_default", "basic"));
                        } else if (originalPermission.contains("-subdivision")) {
                            optionContexts.add(new Context("gd_claim_default", "subdivision"));
                        } else if (originalPermission.contains("-town")) {
                            optionContexts.add(new Context("gd_claim_default", "town"));
                        } else {
                            optionContexts.add(new Context("gd_claim_default", "global"));
                        }
                    }
                    GriefDefenderPlugin.getInstance().getLogger().info("Detected legacy option '" + originalPermission + "' on subject " + subject.getFriendlyIdentifier().orElse(subject.getIdentifier()) + "'. Migrating...");
                    if (isDefault) {
                        PermissionUtil.getInstance().setOptionValue(GriefDefenderPlugin.DEFAULT_HOLDER, originalPermission, "undefined", this.getGDContexts(originalContexts));
                    } else {
                        subject.getSubjectData().setOption(originalContexts, originalPermission, null);
                    }
                    GriefDefenderPlugin.getInstance().getLogger().info("Removed legacy option '" + originalPermission + "'.");
                    if (isDefault) {
                        PermissionUtil.getInstance().setOptionValue(GriefDefenderPlugin.DEFAULT_HOLDER, currentPermission, entry.getValue(), this.getGDContexts(optionContexts));
                    } else {
                        subject.getSubjectData().setOption(optionContexts, currentPermission, entry.getValue());
                    }
                    GriefDefenderPlugin.getInstance().getLogger().info("Removed legacy option '" + originalPermission + "'.");
                    GriefDefenderPlugin.getInstance().getLogger().info("Set new option '" + currentPermission + "' with contexts " + gdContexts);
                    GriefDefenderPlugin.getInstance().getLogger().info("Successfully migrated option " + currentPermission + " to " + currentPermission + " with contexts " + gdContexts);
                    migrated = true;
                }
            }
            if (migrated) {
                GriefDefenderPlugin.getInstance().getLogger().info("Finished migration of subject '" + subject.getIdentifier() + "'\n");
            }
            if (isGroup) {
                permissionService.getGroupSubjects().suggestUnload(subject.getIdentifier());
            } else {
                permissionService.getUserSubjects().suggestUnload(subject.getIdentifier());
            }
        } catch(Throwable t) {
            t.printStackTrace();
        }
    }

    private Set<com.griefdefender.api.permission.Context> getGDContexts(Set<Context> spongeContexts) {
        Set<com.griefdefender.api.permission.Context> gdContexts = new HashSet<>();
        for (Context context : spongeContexts) {
            gdContexts.add(new com.griefdefender.api.permission.Context(context.getKey(), context.getValue()));
        }
        return gdContexts;
    }

    private void applyContexts(String flagBase, String currentPermission, Set<Context> contexts) {
        final int permLength = currentPermission.split("\\.").length;
        if (permLength == 3) {
            // Only contains flag
            return;
        }

        String newPermission = currentPermission.replace(flagBase + ".", "");
        final String[] parts = newPermission.split("\\.source\\.");
        String targetPart = parts[0];
        String sourcePart = parts.length > 1 ? parts[1] : null;
        if (sourcePart != null) {
            // handle source
            String sourceParts[] = sourcePart.split("\\.");
            String sourceId = sourceParts[0];
            if (sourceParts.length > 1) {
                String value = sourceParts[1];
                int index = 2;
                while (index < sourceParts.length) {
                    value += "." + sourceParts[index];
                    index++;
                }
                if (value.equals("animal")) {
                    sourceId = "#animal";
                } else if (value.equals("monster")) {
                    sourceId = "#monster";
                } else {
                    if (value.contains("animal.")) {
                        value = value.replace("animal.", "");
                    } else if (value.contains("monster.")) {
                        value = value.replace("monster.", "");
                    }
                    sourceId += ":" + value;
                }
            } else {
                sourceId += ":any";
            }
            if (currentPermission.contains("interact") && GriefDefenderPlugin.ITEM_IDS.contains(sourceId)) {
                contexts.add(new Context("used_item", sourceId));
                GriefDefenderPlugin.getInstance().getLogger().info("Created new used_item context for '" + sourceId + "'.");
            } else {
                contexts.add(new Context("source", sourceId));
                GriefDefenderPlugin.getInstance().getLogger().info("Created new source context for '" + sourceId + "'.");
            }
        }

        // Handle target
        String targetParts[] = targetPart.split("\\.");
        String targetId = targetParts[0];
        if (targetParts.length > 1) {
            String value = targetParts[1];
            int index = 2;
            while (index < targetParts.length) {
                value += "." + targetParts[index];
                index++;
            }
            if (value.equals("animal")) {
                targetId = "#animal";
            } else if (value.equals("monster")) {
                targetId = "#monster";
            } else {
                if (value.contains("animal.")) {
                    value = value.replace("animal.", "");
                } else if (value.contains("monster.")) {
                    value = value.replace("monster.", "");
                }
                targetId += ":" + value;
            }
        } else {
            targetId += ":any";
        }
        contexts.add(new Context(ContextKeys.TARGET, targetId));
        GriefDefenderPlugin.getInstance().getLogger().info("Created new target context for '" + targetId + "'.");
    }

    public static GPSpongeMigrator getInstance() {
        return instance;
    }

    static {
        instance = new GPSpongeMigrator();
    }
}
