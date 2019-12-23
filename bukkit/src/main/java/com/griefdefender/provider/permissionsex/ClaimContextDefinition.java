package com.griefdefender.provider.permissionsex;

import ca.stellardrift.permissionsex.context.ContextDefinition;
import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import ca.stellardrift.permissionsex.util.Util;
import com.google.common.collect.ImmutableSet;
import com.griefdefender.api.GriefDefender;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.permission.ContextKeys;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

class ClaimContextDefinition extends ContextDefinition<UUID> {

    ClaimContextDefinition() {
        super(ContextKeys.CLAIM);
    }

    @Override
    public void accumulateCurrentValues(@NotNull CalculatedSubject calculatedSubject, @NotNull Function1<? super UUID, Unit> submitValue) {
        Claim activeClaim = PermissionsExProvider.getClaimForSubject(calculatedSubject);

        if (activeClaim != null) {
            Claim parentClaim = activeClaim.getParent().orElse(null);
            if (parentClaim != null && activeClaim.getData().doesInheritParent()) {
                submitValue.invoke(parentClaim.getUniqueId());
            } else {
                submitValue.invoke(activeClaim.getUniqueId());
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
    public boolean matches(@NotNull ContextValue<UUID> contextValue, UUID claim) {
        return contextValue.getParsedValue(this).equals(claim);
    }

    @Override
    public boolean matches(UUID uuid, UUID uuid2) {
        return uuid.equals(uuid2);
    }

    @Override
    @NotNull
    public Set<UUID> suggestValues(@NotNull CalculatedSubject subj) {
        Player ply = Util.castOptional(subj.getAssociatedObject(), Player.class).orElse(null);
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
