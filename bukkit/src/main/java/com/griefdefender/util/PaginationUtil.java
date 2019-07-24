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
package com.griefdefender.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// A bad hack at attempting to track active pages
public class PaginationUtil {

    private static PaginationUtil instance;

    public static PaginationUtil getInstance() {
        return instance;
    }

    static {
        instance = new PaginationUtil();
    }

    // The active page resets when a new command is entered by same player
    private final Map<UUID, Integer> activePageMap = new HashMap<>();

    public void updateActiveCommand(UUID uuid, String command, String args) {
        if (command.equalsIgnoreCase("callback") || command.equalsIgnoreCase("gpreload")) {
            // ignore
            return;
        }

        if (command.equalsIgnoreCase("page")) {
            final Integer activePage = this.activePageMap.get(uuid);
            if (activePage != null) {
                try {
                    final Integer page = Integer.parseInt(args);
                    this.activePageMap.put(uuid, page);
                } catch (Throwable t) {
                    
                }
            }
            return;
        }

        if (command.equalsIgnoreCase("pagination")) {
            final Integer activePage = this.activePageMap.get(uuid);
            if (activePage != null) {
                final boolean isNext = args.contains("next");
                if (isNext) {
                    this.activePageMap.put(uuid, activePage + 1);
                } else if (activePage != 1) {
                    this.activePageMap.put(uuid, activePage - 1);
                }
            }
            return;
        }

        resetActivePage(uuid);
    }

    public void resetActivePage(UUID uuid) {
        this.activePageMap.put(uuid, 1);
    }

    public Integer getActivePage(UUID uuid) {
        return this.activePageMap.get(uuid);
    }

    public void removeActivePageData(UUID uuid) {
        this.activePageMap.remove(uuid);
    }
}
