package com.griefdefender.claim;

import com.flowpowered.math.vector.Vector3i;
import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.api.claim.Town;
import com.griefdefender.api.data.TownData;
import org.spongepowered.api.world.World;

import java.util.UUID;

public class GDTown extends GDClaim implements Town {

    public GDTown(World world, Vector3i point1, Vector3i point2, ClaimType type, UUID ownerUniqueId, boolean cuboid) {
        super(world, point1, point2, type, ownerUniqueId, cuboid);
    }

    @Override
    public TownData getData() {
        return (TownData) this.claimData;
    }
}
