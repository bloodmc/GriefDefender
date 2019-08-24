package com.griefdefender.provider.permissionsex;

import ca.stellardrift.permissionsex.context.ContextDefinition;
import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import com.google.common.collect.ImmutableSet;
import com.griefdefender.api.CatalogType;
import com.griefdefender.api.registry.CatalogRegistryModule;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.function.BiConsumer;

public class CatalogTypeContextDefinition<T extends CatalogType> extends ContextDefinition<T> {
    private final BiConsumer<CalculatedSubject, Function1<? super T, Unit>> currentValueAccumulator;

    private final CatalogRegistryModule<T> registry;

    public CatalogTypeContextDefinition(String key, CatalogRegistryModule<T> registry) {
       this(key, registry, null);
    }

    public CatalogTypeContextDefinition(String key, CatalogRegistryModule<T> registry, BiConsumer<CalculatedSubject, Function1<? super T, Unit>> currentValueAccumulator) {
        super(key);
        this.registry = registry;
        this.currentValueAccumulator = currentValueAccumulator == null ? (x, y) -> {} : currentValueAccumulator;
    }

    @Override
    public void accumulateCurrentValues(@NotNull CalculatedSubject calculatedSubject, @NotNull Function1<? super T, Unit> function1) {
        currentValueAccumulator.accept(calculatedSubject, function1);
    }

    @Override
    public T deserialize(@NotNull String s) {
        return registry.getById(s).orElseThrow(() -> new IllegalArgumentException("Provided value '" + s + "' was not a valid value in catalog type " + registry.getClass().getSimpleName()));
    }

    @Override
    public boolean matches(@NotNull ContextValue<T> contextValue, T t) {
        return contextValue.getParsedValue(this).equals(t);
    }

    @NotNull
    @Override
    public String serialize(T t) {
        return t.getId();
    }

    @NotNull
    @Override
    public Set<T> suggestValues(@NotNull CalculatedSubject subject) {
        return ImmutableSet.copyOf(registry.getAll());
    }
}
