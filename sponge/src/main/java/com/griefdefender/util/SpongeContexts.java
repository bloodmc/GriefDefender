package com.griefdefender.util;

import com.griefdefender.api.claim.ClaimType;
import org.spongepowered.api.service.context.Context;

public class SpongeContexts {

    public static final Context GLOBAL_DEFAULT_CONTEXT = new Context("gd_claim_default", "global");
    public static final Context ADMIN_DEFAULT_CONTEXT = new Context("gd_claim_default", "admin");
    public static final Context BASIC_DEFAULT_CONTEXT = new Context("gd_claim_default", "basic");
    public static final Context SUBDIVISION_DEFAULT_CONTEXT = new Context("gd_claim_default", "subdivision");
    public static final Context TOWN_DEFAULT_CONTEXT = new Context("gd_claim_default", "town");
    public static final Context USER_DEFAULT_CONTEXT = new Context("gd_claim_default", "user");
    public static final Context WILDERNESS_DEFAULT_CONTEXT = new Context("gd_claim_default", "wilderness");
    public static final Context WORLD_DEFAULT_CONTEXT = new Context("gd_claim_default", "world");

    /**
     * Override contexts are used to force a permission to a {@link ClaimType}.
     */
    public static final Context GLOBAL_OVERRIDE_CONTEXT = new Context("gd_claim_override", "global");
    public static final Context ADMIN_OVERRIDE_CONTEXT = new Context("gd_claim_override", "admin");
    public static final Context BASIC_OVERRIDE_CONTEXT = new Context("gd_claim_override", "basic");
    /**
     * Used to override a single claim only.
     */
    //public static final Context CLAIM_OVERRIDE_CONTEXT = new Context("gd_claim_override", "CLAIM");
    public static final Context SUBDIVISION_OVERRIDE_CONTEXT = new Context("gd_claim_override", "subdivision");
    public static final Context TOWN_OVERRIDE_CONTEXT = new Context("gd_claim_override", "town");
    public static final Context USER_OVERRIDE_CONTEXT = new Context("gd_claim_override", "user");
    public static final Context WILDERNESS_OVERRIDE_CONTEXT = new Context("gd_claim_override", "wilderness");
    public static final Context WORLD_OVERRIDE_CONTEXT = new Context("gd_claim_override", "world");
}
