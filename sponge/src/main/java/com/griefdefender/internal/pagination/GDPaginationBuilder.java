/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
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
package com.griefdefender.internal.pagination;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;

import javax.annotation.Nullable;

public class GDPaginationBuilder implements PaginationList.Builder {

    @Nullable
    private Iterable<Component> contents;
    @Nullable
    private Component title;
    @Nullable
    private Component header;
    @Nullable
    private Component footer;
    private Component paginationSpacer = TextComponent.of("=");
    private int linesPerPage = 20;

    @Nullable
    private PaginationList paginationList;

    @Override
    public PaginationList.Builder contents(Iterable<Component> contents) {
        checkNotNull(contents, "The contents cannot be null!");
        this.contents = contents;
        this.paginationList = null;
        return this;
    }

    @Override
    public PaginationList.Builder contents(Component... contents) {
        checkNotNull(contents, "The contents cannot be null!");
        this.contents = ImmutableList.copyOf(contents);
        this.paginationList = null;
        return this;
    }

    @Override
    public PaginationList.Builder title(@Nullable Component title) {
        this.title = title;
        this.paginationList = null;
        return this;
    }

    @Override
    public PaginationList.Builder header(@Nullable Component header) {
        this.header = header;
        this.paginationList = null;
        return this;
    }

    @Override
    public PaginationList.Builder footer(@Nullable Component footer) {
        this.footer = footer;
        this.paginationList = null;
        return this;
    }

    @Override
    public PaginationList.Builder padding(Component padding) {
        checkNotNull(padding, "The padding cannot be null!");
        this.paginationSpacer = padding;
        this.paginationList = null;
        return this;
    }

    @Override
    public PaginationList.Builder linesPerPage(int linesPerPage) {
        this.linesPerPage = linesPerPage;
        return this;
    }

    @Override
    public PaginationList build() {
        checkState(this.contents != null, "The contents of the pagination list cannot be null!");

        if (this.paginationList == null) {
            this.paginationList = new GDPaginationList(this.contents, this.title, this.header, this.footer, this.paginationSpacer, this.linesPerPage);
        }
        return this.paginationList;
    }

    //@Override
    public PaginationList.Builder from(PaginationList list) {
        this.reset();
        this.contents = list.getContents();
        this.title = list.getTitle().orElse(null);
        this.header = list.getHeader().orElse(null);
        this.footer = list.getFooter().orElse(null);
        this.paginationSpacer = list.getPadding();

        this.paginationList = null;
        return this;
    }

    //@Override
    public PaginationList.Builder reset() {
        this.contents = null;
        this.title = null;
        this.header = null;
        this.footer = null;
        this.paginationSpacer = TextComponent.of("=");

        this.paginationList = null;
        return this;
    }
}
