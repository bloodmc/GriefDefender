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
package com.griefdefender.event;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Queues;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.event.EventCause;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.internal.util.NMSUtil;

import java.util.Deque;

import org.bukkit.OfflinePlayer;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class GDCauseStackManager {

    private int tick_stored;

    private static GDCauseStackManager instance;

    private final Deque<Object> cause = Queues.newArrayDeque();

    @Nullable private EventCause cached_cause;

    public EventCause getCurrentCause() {
        if (NMSUtil.getInstance().getRunningServerTicks() != tick_stored) {
            this.cached_cause = null;
            this.cause.clear();
        }
        if (this.cached_cause == null) {
            if (this.cause.isEmpty()) {
                this.cached_cause = EventCause.of(GriefDefenderPlugin.getInstance());
            } else {
                this.cached_cause = EventCause.of(this.cause);
            }
        }
        return this.cached_cause;
    }

    public GDCauseStackManager pushCause(Object obj) {
        checkNotNull(obj, "obj");
        if (NMSUtil.getInstance().getRunningServerTicks() != tick_stored) {
            this.cached_cause = null;
            this.cause.clear();
        }
        if (obj instanceof OfflinePlayer) {
            obj = PermissionHolderCache.getInstance().getOrCreateUser((OfflinePlayer) obj);
        }
        /*if (tick_stored == NMSUtil.getInstance().getRunningServerTicks()) {
            this.cause.push(obj);
            return this;
        }*/

        tick_stored = NMSUtil.getInstance().getRunningServerTicks();
        this.cached_cause = null;
        this.cause.push(obj);
        return this;
    }

    public Object popCause() {
        this.cached_cause = null;
        if (this.cause.isEmpty()) {
            return null;
        }
        return this.cause.pop();
    }

    public Object peekCause() {
        return this.cause.peek();
    }

    public static GDCauseStackManager getInstance() {
        return instance;
    }

    static {
        instance = new GDCauseStackManager();
    }
}
