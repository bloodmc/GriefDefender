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
package com.griefdefender.provider;

import com.griefdefender.GDBootstrap;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.listener.NucleusLegacyEventHandler;
import com.griefdefender.storage.BaseStorage;
import com.griefdefender.util.SpongeUtil;
import io.github.nucleuspowered.nucleus.api.NucleusAPI;
import io.github.nucleuspowered.nucleus.api.chat.NucleusChatChannel;
import io.github.nucleuspowered.nucleus.api.exceptions.PluginAlreadyRegisteredException;
import io.github.nucleuspowered.nucleus.api.service.NucleusMessageTokenService;
import net.kyori.text.Component;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.channel.MessageChannel;

import java.util.Optional;

public class NucleusProviderLegacy implements NucleusProvider {


    public NucleusProviderLegacy() {
        Sponge.getEventManager().registerListeners(GDBootstrap.getInstance(), new NucleusLegacyEventHandler());
        this.registerTokens();
    }

    public boolean isSocialSpy(Player player) {
        return NucleusAPI.getPrivateMessagingService().get().isSocialSpy(player);
    }

    public void registerTokens() {
        final NucleusMessageTokenService messageTokenService = Sponge.getServiceManager().provide(NucleusMessageTokenService.class).get();
        final PluginContainer pc = GDBootstrap.getInstance().pluginContainer;
        final BaseStorage dataStore = GriefDefenderPlugin.getInstance().dataStore;
        try {
            messageTokenService.register(GDBootstrap.getInstance().pluginContainer,
                    (tokenInput, commandSource, variables) -> {
                        // Each token will require something like this.

                        // This token, town, will give the name of the town the player is currently in.
                        // Will be registered in Nucleus as "{{pl:griefdefender:town}}", with the shortened version of "{{town}}"
                        // This will return the name of the town the player is currently in.
                        if (tokenInput.equalsIgnoreCase("town") && commandSource instanceof Player) {
                            Player player = (Player) commandSource;
                            final GDPlayerData data = dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());

                            // Shamelessly stolen from PlayerEventHandler
                            if (data.inTown) {
                                final Component component = dataStore.getClaimAtPlayer(data, player.getLocation()).getTownClaim().getTownData().getTownTag().orElse(null);
                                return Optional.ofNullable(SpongeUtil.getSpongeText(component));
                            }
                        }

                        return Optional.empty();
                    });
        } catch (PluginAlreadyRegisteredException ignored) {
            // already been done.
        }

        // register {{town}} from {{pl:griefdefender:town}}
        messageTokenService.registerPrimaryToken("town", pc, "town");
    }

    public boolean isChatChannel(MessageChannel channel) {
        return channel instanceof NucleusChatChannel;
    }
}
