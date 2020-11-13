/*
 * This file is part of GriefDefender, licensed under the MIT License (MIT).
 *
 * Copyright (c) bloodmc
 * Copyright (c) zml
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
package com.griefdefender.provider.permissionsex;

import ca.stellardrift.permissionsex.context.ContextDefinition;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import ca.stellardrift.permissionsex.util.Util;
import com.google.common.collect.ImmutableSet;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.permission.ContextKeys;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

class ClaimContextDefinition extends ContextDefinition<UUID> {

    ClaimContextDefinition() {
        super(ContextKeys.CLAIM);
    }

    @Override
    public void accumulateCurrentValues(CalculatedSubject calculatedSubject, Consumer<UUID> consumer) {
        Claim activeClaim = PermissionsExProvider.getClaimForSubject(calculatedSubject);

        if (activeClaim != null) {
            Claim parentClaim = activeClaim.getParent().orElse(null);
            if (parentClaim != null && activeClaim.getData().doesInheritParent()) {
                consumer.accept(parentClaim.getUniqueId());
            } else {
                consumer.accept(activeClaim.getUniqueId());
            }
        }
    }

    @Override
    public UUID deserialize(@NotNull String s) {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Unable to deserialize context: " + s + " is not a valid UUID");
        }
    }

    @Override
    public boolean matches(UUID uuid, UUID uuid2) {
        return uuid.equals(uuid2);
    }

    @Override
    @NotNull
    public Set<UUID> suggestValues(@NotNull CalculatedSubject subj) {
        Player ply = Util.castOptional(Optional.ofNullable(subj.getAssociatedObject()), Player.class).orElse(null);
        if (ply == null) {
            return ImmutableSet.of();
        }
        return GriefDefender.getCore().getClaimManager(ply.getWorld().getUID()).getWorldClaims().stream()
                .map(Claim::getUniqueId)
                .collect(Collectors.toSet());
    }

    @NotNull
    @Override
    public String serialize(UUID claim) {
        return claim.toString();
    }

}
