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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.griefdefender.command.CommandException;
import net.kyori.text.Component;
import net.kyori.text.adapter.spongeapi.TextAdapter;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.source.ProxySource;
import org.spongepowered.api.entity.living.player.Player;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nullable;

public class GDPaginationList implements PaginationList {

    private final Iterable<Component> contents;
    private final Optional<Component> title;
    private final Optional<Component> header;
    private final Optional<Component> footer;
    private final Component paginationSpacer;
    private final int linesPerPage;

    public GDPaginationList(Iterable<Component> contents, @Nullable Component title, @Nullable Component header,
            @Nullable Component footer, Component paginationSpacer, int linesPerPage) {
        this.contents = contents;
        this.title = Optional.ofNullable(title);
        this.header = Optional.ofNullable(header);
        this.footer = Optional.ofNullable(footer);
        this.paginationSpacer = paginationSpacer;
        this.linesPerPage = linesPerPage;
    }

    @Override
    public Iterable<Component> getContents() {
        return this.contents;
    }

    @Override
    public Optional<Component> getTitle() {
        return this.title;
    }

    @Override
    public Optional<Component> getHeader() {
        return this.header;
    }

    @Override
    public Optional<Component> getFooter() {
        return this.footer;
    }

    @Override
    public Component getPadding() {
        return this.paginationSpacer;
    }

    @Override
    public int getLinesPerPage() {
        return this.linesPerPage;
    }

    @Override
    public void sendTo(final CommandSource receiver, int page) {
        checkNotNull(receiver, "The message receiver cannot be null!");

        CommandSource realSource = receiver;
        while (realSource instanceof ProxySource) {
            realSource = ((ProxySource)realSource).getOriginalSource();
        }
        final GDPaginationCalculator calculator = new GDPaginationCalculator(this.linesPerPage);
        Iterable<Map.Entry<Component, Integer>> counts = StreamSupport.stream(this.contents.spliterator(), false).map(input -> {
            int lines = calculator.getLines(input);
            return Maps.immutableEntry(input, lines);
        }).collect(Collectors.toList());

        Component title = this.title.orElse(null);
        if (title != null) {
            title = calculator.center(title, this.paginationSpacer);
        }

        // If the MessageReceiver is a Player, then upon death, they will become a different MessageReceiver object.
        // Thus, we use a supplier to supply the player from the server, if required.
        Supplier<Optional<CommandSource>> messageReceiverSupplier;
        if (receiver instanceof Player) {
            final UUID playerUuid = ((Player) receiver).getUniqueId();
            messageReceiverSupplier = () -> Optional.ofNullable(Sponge.getServer().getPlayer(playerUuid).orElse(null)).map(x -> (Player) x);
        } else {
            WeakReference<CommandSource> srcReference = new WeakReference<>(receiver);
            messageReceiverSupplier = () -> Optional.ofNullable(srcReference.get());
        }

        ActivePagination pagination;
        if (this.contents instanceof List) { // If it started out as a list, it's probably reasonable to copy it to another list
            pagination = new ListPagination(messageReceiverSupplier, calculator, ImmutableList.copyOf(counts), title, this.header.orElse(null),
                    this.footer.orElse(null), this.paginationSpacer);
        } else {
            pagination = new IterablePagination(messageReceiverSupplier, calculator, counts, title, this.header.orElse(null),
                    this.footer.orElse(null), this.paginationSpacer);
        }

        GDPaginationHolder.getInstance().getPaginationState(receiver, true).put(pagination);
        try {
            pagination.specificPage(page);
        } catch (CommandException e) {
            TextAdapter.sendComponent(receiver, e.getText());
        }
    }
}
