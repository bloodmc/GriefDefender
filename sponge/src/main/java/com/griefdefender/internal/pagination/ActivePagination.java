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

import com.griefdefender.command.CommandException;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.spongeapi.TextAdapter;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;
import org.spongepowered.api.command.CommandSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import javax.annotation.Nullable;

/**
 * Holds logic for an active pagination that is occurring.
 */
public abstract class ActivePagination {

    private static final Component SLASH_TEXT = TextComponent.of("/");
    private static final Component DIVIDER_TEXT = TextComponent.of(" ");
    private static final Component CONTINUATION_TEXT = TextComponent.of("...");
    private final Supplier<Optional<CommandSource>> src;
    private final UUID id = UUID.randomUUID();
    private final Component nextPageText;
    private final Component prevPageText;
    @Nullable
    private final Component title;
    @Nullable
    private final Component header;
    @Nullable
    private final Component footer;
    private int currentPage;
    private final int maxContentLinesPerPage;
    protected final GDPaginationCalculator calc;
    private final Component padding;

    public ActivePagination(Supplier<Optional<CommandSource>> src, GDPaginationCalculator calc, @Nullable Component title,
            @Nullable Component  header, @Nullable Component  footer, Component  padding) {
        this.src = src;
        this.calc = calc;
        this.title = title;
        this.header = header;
        this.footer = footer;
        this.padding = padding;
        this.nextPageText = TextComponent.builder("»")
                .color(TextColor.BLUE)
                .decoration(TextDecoration.UNDERLINED, true)
                .clickEvent(ClickEvent.runCommand("/gd:pagination " + this.id.toString() + " next"))
                .hoverEvent(HoverEvent.showText(TextComponent.of("/page next")))
                .insertion("/page next")
                .build();
        this.prevPageText = TextComponent.builder("«")
                .color(TextColor.BLUE)
                .decoration(TextDecoration.UNDERLINED, true)
                .clickEvent(ClickEvent.runCommand("/gd:pagination " + this.id.toString() + " prev"))
                .hoverEvent(HoverEvent.showText(TextComponent.of("/page prev")))
                .insertion("/page prev")
                .build();
        int maxContentLinesPerPage = calc.getLinesPerPage(src.get().get()) - 1;
        if (title != null) {
            maxContentLinesPerPage -= calc.getLines(title);
        }
        if (header != null) {
            maxContentLinesPerPage -= calc.getLines(header);
        }
        if (footer != null) {
            maxContentLinesPerPage -= calc.getLines(footer);
        }
        this.maxContentLinesPerPage = maxContentLinesPerPage;
    }

    public UUID getId() {
        return this.id;
    }

    protected abstract Iterable<Component> getLines(int page) throws CommandException;

    protected abstract boolean hasPrevious(int page);

    protected abstract boolean hasNext(int page);

    protected abstract int getTotalPages();

    public void nextPage() throws CommandException {
        specificPage(this.currentPage + 1);
    }

    public void previousPage() throws CommandException {
        specificPage(this.currentPage - 1);
    }

    public void currentPage() throws CommandException {
        specificPage(this.currentPage);
    }

    protected int getCurrentPage() {
        return this.currentPage;
    }

    protected int getMaxContentLinesPerPage() {
        return this.maxContentLinesPerPage;
    }

    public void specificPage(int page) throws CommandException {
        CommandSource src = this.src.get()
                .orElseThrow(() -> new CommandException(TextComponent.of(String.format("Source for pagination %s is no longer active!", getId()))));
        this.currentPage = page;

        List<Component> toSend = new ArrayList<>();
        Component title = this.title;
        if (title != null) {
            toSend.add(title);
        }
        if (this.header != null) {
            toSend.add(this.header);
        }

        for (Component line : getLines(page)) {
            toSend.add(line);
        }

        Component footer = calculateFooter(page);
        toSend.add(this.calc.center(footer, this.padding));
        if (this.footer != null) {
            toSend.add(this.footer);
        }
        for (Component component : toSend) {
            TextAdapter.sendComponent(src, component);
        }
    }

    protected Component calculateFooter(int currentPage) {
        boolean hasPrevious = hasPrevious(currentPage);
        boolean hasNext = hasNext(currentPage);

        TextComponent.Builder ret = null;
        if (hasPrevious) {
            ret = TextComponent.builder("").append(this.prevPageText).append(DIVIDER_TEXT);
        } else {
            ret = TextComponent.builder("«").append(DIVIDER_TEXT);
        }
        boolean needsDiv = false;
        int totalPages = getTotalPages();
        if (totalPages > 1) {
            ret.append(TextComponent.builder("")
                    .hoverEvent(HoverEvent.showText(TextComponent.of("/page " + currentPage)))
                    .clickEvent(ClickEvent.runCommand("/gd:pagination " + this.id + ' ' + currentPage))
                    .insertion("/page " + currentPage)
                    .append(TextComponent.of(String.valueOf(currentPage))).build())
            .append(SLASH_TEXT)
            .append(TextComponent.builder("")
                    .hoverEvent(HoverEvent.showText(TextComponent.of("/page " + totalPages)))
                    .clickEvent(ClickEvent.runCommand("/gd:pagination " + this.id + ' ' + totalPages))
                    .insertion("/page " + totalPages)
                    .append(TextComponent.of(String.valueOf(totalPages))).build());
            needsDiv = true;
        }
        if (hasNext) {
            if (needsDiv) {
                ret.append(DIVIDER_TEXT);
            }
            ret.append(this.nextPageText);
        } else {
            if (needsDiv) {
                ret.append(DIVIDER_TEXT);
            }
            ret.append(TextComponent.of("»"));
        }

        ret.color(this.padding.color());
        if (this.title != null) {
            ret.mergeDecorations(this.title);
        }
        return ret.build();
    }

    protected void padPage(final List<Component> currentPage, final int currentPageLines, final boolean addContinuation) {
        final int maxContentLinesPerPage = getMaxContentLinesPerPage();
        for (int i = currentPageLines; i < maxContentLinesPerPage; i++) {
            if (addContinuation && i == maxContentLinesPerPage - 1) {
                currentPage.add(CONTINUATION_TEXT);
            } else {
                currentPage.add(0, TextComponent.empty());
            }
        }
    }
}
