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
package com.griefdefender.internal.pagination;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.MapMaker;
import com.griefdefender.command.CommandException;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.channel.MessageReceiver;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

public class GDPaginationHolder {

    private static final GDPaginationHolder INSTANCE = new GDPaginationHolder();

    private final ConcurrentMap<MessageReceiver, SourcePaginations> activePaginations = new MapMaker().weakKeys().makeMap();

    // We have a second active pagination system because of the way Players are handled by the server.
    // As Players are recreated every time they die in game, just storing the player in a weak map will
    // cause the player to be removed form the map upon death. Thus, player paginations get redirected
    // through to this cache instead, which last for 10 minutes from last access.
    private final Cache<UUID, SourcePaginations> playerActivePaginations = Caffeine.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES)
            .build();

    public ActivePagination getActivePagination(CommandSource src, String id) throws CommandException {
        SourcePaginations paginations = GDPaginationHolder.getInstance().getPaginationState(src, false);
        if (paginations == null) {
            return null;
        }

        UUID pageId = null;
        try {
            pageId = UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            throw new CommandException(TextComponent.of("Input was not a valid UUID!", TextColor.RED));
        }

        ActivePagination pagination = paginations.get(pageId);
        if (pagination == null) {
            throw new CommandException(TextComponent.of("No pagination registered for id " + id.toString(), TextColor.RED));
        }
        return pagination;
    }

    @Nullable
    SourcePaginations getPaginationState(MessageReceiver source, boolean create) {
        if (source instanceof Player) {
            return getPaginationStateForPlayer((Player) source, create);
        }

        return getPaginationStateForNonPlayer(source, create);
    }

    @Nullable
    SourcePaginations getPaginationStateForNonPlayer(MessageReceiver source, boolean create) {
        SourcePaginations ret = this.activePaginations.get(source);
        if (ret == null && create) {
            ret = new SourcePaginations();
            SourcePaginations existing = this.activePaginations.putIfAbsent(source, ret);
            if (existing != null) {
                ret = existing;
            }
        }
        return ret;
    }

    @Nullable
    SourcePaginations getPaginationStateForPlayer(Player source, boolean create) {
        return this.playerActivePaginations.get(source.getUniqueId(), k -> create ? new SourcePaginations() : null);
    }

    static class SourcePaginations {
        private final Map<UUID, ActivePagination> paginations = new ConcurrentHashMap<>();
        @Nullable private volatile UUID lastUuid;

        @Nullable public ActivePagination get(UUID uuid) {
            return this.paginations.get(uuid);
        }

        public void put(ActivePagination pagination) {
            synchronized (this.paginations) {
                this.paginations.put(pagination.getId(), pagination);
                this.lastUuid = pagination.getId();
            }
        }

        public Set<UUID> keys() {
            return this.paginations.keySet();
        }

        @Nullable
        public UUID getLastUuid() {
            return this.lastUuid;
        }
    }

    public static GDPaginationHolder getInstance() {
        return INSTANCE;
    }

    public Cache<UUID, SourcePaginations> getActivePaginationCache() {
        return this.playerActivePaginations;
    }
}
