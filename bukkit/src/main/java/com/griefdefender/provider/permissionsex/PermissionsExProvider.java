package com.griefdefender.provider.permissionsex;

import ca.stellardrift.permissionsex.bukkit.PermissionsExPlugin;
import ca.stellardrift.permissionsex.context.ContextDefinition;
import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.data.Change;
import ca.stellardrift.permissionsex.data.ImmutableSubjectData;
import ca.stellardrift.permissionsex.subject.SubjectType;
import ca.stellardrift.permissionsex.util.Util;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.CatalogType;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.api.permission.Context;
import com.griefdefender.api.permission.ContextKeys;
import com.griefdefender.api.permission.PermissionResult;
import com.griefdefender.api.permission.ResultTypes;
import com.griefdefender.api.permission.flag.Flag;
import com.griefdefender.api.permission.option.Option;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.internal.registry.BlockTypeRegistryModule;
import com.griefdefender.internal.registry.EntityTypeRegistryModule;
import com.griefdefender.internal.registry.ItemTypeRegistryModule;
import com.griefdefender.permission.GDPermissionHolder;
import com.griefdefender.permission.GDPermissionResult;
import com.griefdefender.permission.GDPermissionUser;
import ca.stellardrift.permissionsex.PermissionsEx;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import com.griefdefender.provider.PermissionProvider;
import com.griefdefender.registry.ClaimTypeRegistryModule;
import com.griefdefender.registry.FlagRegistryModule;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PermissionsExProvider implements PermissionProvider {
    private static final ContextDefinition<UUID> CTX_CLAIM = new ClaimContextDefinition();
    private static final ContextDefinition<ClaimType> CTX_CLAIM_DEFAULT = new CatalogTypeContextDefinition<>(ContextKeys.CLAIM_DEFAULT, ClaimTypeRegistryModule.getInstance(), claimAttributeValue(GDClaim::getType));
    private static final ContextDefinition<ClaimType> CTX_CLAIM_OVERRIDE = new CatalogTypeContextDefinition<>(ContextKeys.CLAIM_OVERRIDE, ClaimTypeRegistryModule.getInstance());
    private static final ContextDefinition<Flag> CTX_FLAG = new CatalogTypeContextDefinition<>(ContextKeys.FLAG, FlagRegistryModule.getInstance());
    private static final ContextDefinition<CatalogType> CTX_SOURCE = new MultiCatalogTypeContextDefinition(ContextKeys.SOURCE, BlockTypeRegistryModule.getInstance(), ItemTypeRegistryModule.getInstance(), EntityTypeRegistryModule.getInstance());
    private static final ContextDefinition<CatalogType> CTX_STATE = new MultiCatalogTypeContextDefinition(ContextKeys.STATE);
    private static final ContextDefinition<CatalogType> CTX_TARGET = new MultiCatalogTypeContextDefinition(ContextKeys.TARGET);

    private final PermissionsEx<?> pex;

    public PermissionsExProvider(PermissionsEx<?> engine) {
        this.pex = engine;
        engine.registerContextDefinition(CTX_CLAIM);
        engine.registerContextDefinition(CTX_CLAIM_DEFAULT);
        engine.registerContextDefinition(CTX_CLAIM_OVERRIDE);
        engine.registerContextDefinition(CTX_FLAG);
        engine.registerContextDefinition(CTX_SOURCE);
        engine.registerContextDefinition(CTX_STATE);
        engine.registerContextDefinition(CTX_TARGET);
    }

    public static PermissionsExProvider initBukkit(Plugin pexPlugin) {
        if (pexPlugin instanceof PermissionsExPlugin) {
            return new PermissionsExProvider(((PermissionsExPlugin) pexPlugin).getManager());
        }
        throw new RuntimeException("Provided plugin " + pexPlugin + " was not a proper instance of PermissionsExPlugin");
    }

    private static <T> BiConsumer<CalculatedSubject, Function1<? super T, Unit>> claimAttributeValue(Function<GDClaim, T> claimFunc) {
        return (subj, collector) -> {
            GDClaim claim = getClaimForSubject(subj);
            if (claim != null) {
                T attr = claimFunc.apply(claim);
                if (attr != null) {
                    collector.invoke(attr);
                }
            }
        };
    }

    /**
     * Get the current applicable claim for a given subject for permissions purposes. This takes into account a claim being ignored as well
     *
     * @param subj The subject to get the active claim for
     * @return A claim if applicable, otherwise null
     */
    static GDClaim getClaimForSubject(CalculatedSubject subj) {
        Player ply = Util.castOptional(subj.getAssociatedObject(), Player.class).orElse(null);
        if (ply == null) {// not an online player
            return null;
        }
        GDPlayerData plyData = GriefDefenderPlugin.getInstance().dataStore.getPlayerData(ply.getWorld(), ply.getUniqueId());

        if (plyData != null && plyData.ignoreActiveContexts) {
            plyData.ignoreActiveContexts = false;
            return null;
        }

        GDClaim ret = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(plyData, ply.getLocation());
        if (plyData != null && !plyData.canIgnoreClaim(ret)) {
            return null;
        }

        GDClaim parentClaim = ret.parent;
        if (ret.getData().doesInheritParent() && parentClaim != null) {
            return parentClaim;
        } else {
            return ret;
        }
    }

    // - Data conversion

    private ContextValue<?> contextGDToPEX(Context gdCtx) {
        return new ContextValue<>(gdCtx.getKey(), gdCtx.getValue());
    }

    private Context contextPEXToGD(ContextValue<?> pexCtx) {
        return new Context(pexCtx.getKey(), pexCtx.getRawValue());
    }

    private Set<ContextValue<?>> contextsGDToPEX(Set<Context> gdCtxs) {
        return gdCtxs.stream()
                .map(this::contextGDToPEX)
                .collect(Collectors.toSet());

    }

    private Set<Context> contextsPEXToGD(Set<ContextValue<?>> ctxs) {
        return ctxs.stream()
                .map(this::contextPEXToGD)
                .collect(Collectors.toSet());
    }

    private CalculatedSubject holderToPEXSubject(GDPermissionHolder holder) {
        return pex.getSubjects(holder instanceof GDPermissionUser ? PermissionsEx.SUBJECTS_USER : PermissionsEx.SUBJECTS_GROUP).get(holder.getIdentifier()).join();
    }

    private <ValueType> Map<Set<Context>, ValueType> tKeys(Map<Set<ContextValue<?>>, ValueType> pexMap) {
        return tKeys(pexMap, Function.identity());
    }

    private <ValueType, InputValueType> Map<Set<Context>, ValueType> tKeys(Map<Set<ContextValue<?>>, InputValueType> pexMap, Function<InputValueType, ValueType> valueXform) {
        ImmutableMap.Builder<Set<Context>, ValueType> ret = ImmutableMap.builder();
        pexMap.forEach((key, val) -> ret.put(contextsPEXToGD(key), valueXform.apply(val)));
        return ret.build();
    }

    private Component textForError(Throwable t) { // TODO: is this already done somewhere else in GD?
        TextComponent.Builder build = TextComponent.builder(t.getMessage(), TextColor.RED);

        TextComponent.Builder stackTrace = TextComponent.builder();
        for (StackTraceElement el : t.getStackTrace()) {
            stackTrace.append(el.toString()).append("\n");
        }

        build.hoverEvent(HoverEvent.showText(stackTrace.build()));
        return build.build();
    }

    private CompletableFuture<PermissionResult> convertResult(CompletableFuture<Change<ImmutableSubjectData>> pexResult) {
        return pexResult.handle((res, err) -> {
            if (err != null) {
                return new GDPermissionResult(ResultTypes.FAILURE, textForError(err));
            } else {
                return new GDPermissionResult(ResultTypes.SUCCESS);
            }
        });
    }

    private Tristate tristateFromInt(int value) {
        if (value > 0) {
            return Tristate.TRUE;
        } else if (value < 0) {
            return Tristate.FALSE;
        }
        return Tristate.UNDEFINED;
    }

    private int intFromTristate(Tristate value) {
        switch (value) {
            case TRUE: return 1;
            case FALSE: return -1;
            case UNDEFINED: return 0;
            default: throw new IllegalArgumentException("unknown tristate value " + value);
        }
    }

    private int pValFromBool(@Nullable Boolean value) {
        if (value == null) {
            return 0;
        }
        return value ? 1 : -1;
    }

    private boolean pValIntegerToBool(@Nullable Integer value) {
        return value != null && value > 0;
    }

    // - Implement API

    @Override
    public String getServerName() {
        return pex.getConfig().getServerTags().get(0);
    }

    @Override
    public boolean hasGroupSubject(String identifier) {
        return pex.getSubjects(PermissionsEx.SUBJECTS_GROUP).isRegistered(identifier).join();
    }

    @Override
    public UUID lookupUserUniqueId(String name) {
        return Bukkit.getOfflinePlayer(name).getUniqueId(); // TODO: this is not a thing pex does, should be a platform thing
    }

    @Override
    public List<String> getAllLoadedPlayerNames() {
        return getAllLoadedSubjectNames(PermissionsEx.SUBJECTS_USER);
    }

    @Override
    public List<String> getAllLoadedGroupNames() {
        return getAllLoadedSubjectNames(PermissionsEx.SUBJECTS_GROUP);
    }

    private List<String> getAllLoadedSubjectNames(String subjectType) {
        return pex.getSubjects(subjectType).getActiveSubjects().stream()
                .map(subj -> subj.getIdentifier().getValue())
                .collect(Collectors.toList());
    }

    @Override
    public void addActiveContexts(Set<Context> contexts, GDPermissionHolder permissionHolder) {
        addActiveContexts(contexts, permissionHolder, null, null);
    }

    @Override
    public void addActiveContexts(Set<Context> contexts, GDPermissionHolder permissionHolder, GDPlayerData playerData, Claim claim) {
        contexts.addAll(contextsPEXToGD(holderToPEXSubject(permissionHolder).getActiveContexts()));
        // ??? TODO anything else
    }

    private ImmutableSubjectData clearContext(ImmutableSubjectData in, ContextValue<?> ctx) {
        for (Set<ContextValue<?>> ctxSet : in.getActiveContexts()) {
            if (ctxSet.contains(ctx)) {
                in = in.clearPermissions(ctxSet).clearOptions(ctxSet).clearParents(ctxSet);
            }
        }
        return in;

    }

    @Override
    public void clearPermissions(GDClaim claim) {
        ContextValue<UUID> claimContext = CTX_CLAIM.createValue(claim.getUniqueId());
        pex.performBulkOperation(() -> {
            List<CompletableFuture<?>> dataAwait = new LinkedList<>();
            pex.getRegisteredSubjectTypes().forEach(type -> {
                SubjectType subjects = pex.getSubjects(type);
                subjects.getAllIdentifiers().forEach(ident -> {
                    dataAwait.add(subjects.persistentData().getReference(ident).thenCombine(subjects.transientData().getReference(ident), (persist, trans) -> {
                        return CompletableFuture.allOf(persist.update(data -> clearContext(data, claimContext)),
                                trans.update(data -> clearContext(data, claimContext)));
                    }));
                });
            });
            return CompletableFuture.allOf(dataAwait.toArray(new CompletableFuture[0]));
        }).join();
    }

    @Override
    public void clearPermissions(GDPermissionHolder holder, Context context) {
        holderToPEXSubject(holder).data().update(data -> data.clearPermissions(ImmutableSet.of(contextGDToPEX(context))));
    }

    @Override
    public void clearPermissions(GDPermissionHolder holder, Set<Context> contexts) {
        holderToPEXSubject(holder).data().update(data -> data.clearPermissions(contextsGDToPEX(contexts)));
    }

    @Override
    public boolean holderHasPermission(GDPermissionHolder holder, String permission) {
        return holderToPEXSubject(holder).hasPermission(permission);
    }

    @Override
    public Map<String, Boolean> getPermissions(GDPermissionHolder holder, Set<Context> contexts) {
        return Maps.transformValues(holderToPEXSubject(holder).getPermissions(contextsGDToPEX(contexts)).asMap(), this::pValIntegerToBool);
    }

    @Override
    public Map<String, String> getOptions(GDPermissionHolder holder, Set<Context> contexts) {
        return holderToPEXSubject(holder).getOptions(contextsGDToPEX(contexts));
    }

    @Override
    public Map<Set<Context>, Map<String, Boolean>> getPermanentPermissions(GDPermissionHolder holder) {
        return tKeys(holderToPEXSubject(holder).data().get().getAllPermissions(), map -> Maps.transformValues(map, this::pValIntegerToBool));
    }

    @Override
    public Map<Set<Context>, Map<String, Boolean>> getTransientPermissions(GDPermissionHolder holder) {
        return tKeys(holderToPEXSubject(holder).transientData().get().getAllPermissions(), map -> Maps.transformValues(map, this::pValIntegerToBool));
    }

    @Override
    public Map<Set<Context>, Map<String, String>> getPermanentOptions(GDPermissionHolder holder) {
        return tKeys(holderToPEXSubject(holder).data().get().getAllOptions());
    }

    @Override
    public Map<Set<Context>, Map<String, String>> getTransientOptions(GDPermissionHolder holder) {
        return tKeys(holderToPEXSubject(holder).transientData().get().getAllOptions());
    }

    @Override
    public Map<String, String> getPermanentOptions(GDPermissionHolder holder, Set<Context> contexts) {
        return holderToPEXSubject(holder).data().get().getOptions(contextsGDToPEX(contexts));
    }

    @Override
    public Map<String, String> getTransientOptions(GDPermissionHolder holder, Set<Context> contexts) {
        return holderToPEXSubject(holder).transientData().get().getOptions(contextsGDToPEX(contexts));
    }

    @Override
    public Map<Set<Context>, Map<String, Boolean>> getAllPermissions(GDPermissionHolder holder) {
        final Map<Set<Context>, Map<String, Boolean>> allPermissions = new HashMap<>();
        holderToPEXSubject(holder).data().get().getAllPermissions().forEach((contexts, perms) ->
                allPermissions.put(contextsPEXToGD(contexts), new HashMap<>(Maps.transformValues(perms, this::pValIntegerToBool))));

        holderToPEXSubject(holder).transientData().get().getAllPermissions().forEach((contexts, perms) -> {
            Set<Context> gdContexts = contextsPEXToGD(contexts);
            if (allPermissions.containsKey(gdContexts)) {
                Map<String, Boolean> ctxPerms = allPermissions.get(gdContexts);
                perms.forEach((k, v) -> ctxPerms.put(k, v > 0));
            } else {
                allPermissions.put(gdContexts, Maps.transformValues(perms, this::pValIntegerToBool));
            }
        });
        return Collections.unmodifiableMap(allPermissions);
    }

    @Override
    public Tristate getPermissionValue(GDPermissionHolder holder, String permission) {
        return tristateFromInt(holderToPEXSubject(holder).getPermission(permission));
    }

    @Override
    public Tristate getPermissionValue(GDClaim claim, GDPermissionHolder holder, String permission, Set<Context> contexts) {
        return getPermissionValue(claim, holder, permission, contexts, true);
    }

    /*
     * The checkTransient value is ignored here -- we shouldn't need to use it since PEX already prioritizes
     * transient permissions appropriately based on the subject type
     */
    @Override
    public Tristate getPermissionValue(GDClaim claim, GDPermissionHolder holder, String permission, Set<Context> contexts, boolean checkTransient) {
        return tristateFromInt(holderToPEXSubject(holder).getPermission(contextsGDToPEX(contexts), permission));
    }

    @Override
    public Tristate getPermissionValue(GDPermissionHolder holder, String permission, Set<Context> contexts) {
        return tristateFromInt(holderToPEXSubject(holder).getPermission(contextsGDToPEX(contexts), permission));
    }

    @Override
    public Tristate getPermissionValueWithRequiredContexts(GDClaim claim, GDPermissionHolder holder, String permission,
            Set<Context> contexts, String contextFilter) {
        // TODO
        return tristateFromInt(holderToPEXSubject(holder).getPermission(contextsGDToPEX(contexts), permission));
    }

    @Override
    public String getOptionValue(GDPermissionHolder holder, Option option, Set<Context> contexts) {
        return holderToPEXSubject(holder).getOption(contextsGDToPEX(contexts), option.getPermission()).orElse(null);
    }

    @Override
    public List<String> getOptionValueList(GDPermissionHolder holder, Option option, Set<Context> contexts) {
        final List<String> valueList = new ArrayList<>();
        valueList.add(holderToPEXSubject(holder).getOption(contextsGDToPEX(contexts), option.getPermission()).orElse(null));
        return valueList;
    }

    @Override
    public PermissionResult setOptionValue(GDPermissionHolder holder, String permission, String value, Set<Context> contexts, boolean check) {
        return convertResult(holderToPEXSubject(holder).data().update(data -> data.setOption(contextsGDToPEX(contexts), permission, value))).join();
    }

    @Override
    public PermissionResult setTransientOption(GDPermissionHolder holder, String permission, String value, Set<Context> contexts) {
        return convertResult(holderToPEXSubject(holder).transientData().update(data -> data.setOption(contextsGDToPEX(contexts), permission, value))).join();
    }

    @Override
    public PermissionResult setTransientPermission(GDPermissionHolder holder, String permission, Tristate value, Set<Context> contexts) {
        return convertResult(holderToPEXSubject(holder).transientData().update(data -> data.setPermission(contextsGDToPEX(contexts), permission, pValFromBool(value.asBoolean())))).join();
    }

    @Override
    public void refreshCachedData(GDPermissionHolder holder) {
        holderToPEXSubject(holder).data().getCache().invalidate(holder.getIdentifier());
        holderToPEXSubject(holder).transientData().getCache().invalidate(holder.getIdentifier());
    }

    @Override
    public PermissionResult setPermissionValue(GDPermissionHolder holder, String permission, Tristate value, Set<Context> contexts, boolean check, boolean save) {
        return convertResult(holderToPEXSubject(holder).data().update(data -> data.setPermission(contextsGDToPEX(contexts), permission, intFromTristate(value)))).join();
    }

    @Override
    public CompletableFuture<Void> save(GDPermissionHolder holder) {
        // TODO
        return new CompletableFuture<>();
    }
}

