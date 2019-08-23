/*
 * This file is part of GriefDefender, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
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
package com.griefdefender.text.action;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import org.bukkit.command.CommandSender;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class GDCallbackHolder {
    public static final String CALLBACK_COMMAND = "callback";
    public static final String CALLBACK_COMMAND_QUALIFIED = "/gd:" + CALLBACK_COMMAND;
    private static final GDCallbackHolder INSTANCE = new GDCallbackHolder();

    static final ConcurrentMap<UUID, Consumer<CommandSender>> reverseMap = new ConcurrentHashMap<>();
    private static final LoadingCache<Consumer<CommandSender>, UUID> callbackCache = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES)
            .removalListener(new RemovalListener<Consumer<CommandSender>, UUID>() {
                @Override
                public void onRemoval(RemovalNotification<Consumer<CommandSender>, UUID> notification) {
                    reverseMap.remove(notification.getValue(), notification.getKey());
                }
            })
            .build(new CacheLoader<Consumer<CommandSender>, UUID>() {
                @Override
                public UUID load(Consumer<CommandSender> key) throws Exception {
                    UUID ret = UUID.randomUUID();
                    reverseMap.putIfAbsent(ret, key);
                    return ret;
                }
            });


    public static GDCallbackHolder getInstance() {
        return INSTANCE;
    }


    public UUID getOrCreateIdForCallback(Consumer<CommandSender> callback) {
        return callbackCache.getUnchecked(checkNotNull(callback, "callback"));
    }

    public Optional<Consumer<CommandSender>> getCallbackForUUID(UUID id) {
        return Optional.of(reverseMap.get(id));
    }

    public String createCallbackRunCommand(Consumer<CommandSender> consumer) {
        UUID callbackId = getOrCreateIdForCallback(consumer);
        return CALLBACK_COMMAND_QUALIFIED + " " + callbackId;
    }
}
