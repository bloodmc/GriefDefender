package com.griefdefender.provider.permissionsex;

import ca.stellardrift.permissionsex.context.ContextDefinition;
import ca.stellardrift.permissionsex.context.ContextValue;
import ca.stellardrift.permissionsex.subject.CalculatedSubject;
import com.griefdefender.api.CatalogType;
import com.griefdefender.api.registry.CatalogRegistryModule;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class MultiCatalogTypeContextDefinition extends ContextDefinition<CatalogType> {
    private final CatalogRegistryModule<?>[] registries;

    public MultiCatalogTypeContextDefinition(String key, CatalogRegistryModule<?>... registries) {
       super(key);
       this.registries = registries;
    }


    @Override
    public void accumulateCurrentValues(@NotNull CalculatedSubject calculatedSubject, @NotNull Function1<? super CatalogType, Unit> function1) {
    }

    @Override
    public CatalogType deserialize(@NotNull String s) {
        for (CatalogRegistryModule<?> reg : registries) {
            Optional<? extends CatalogType> possibility = reg.getById(s);
            if (possibility.isPresent()) {
                return possibility.get();
            }
        }
        throw new IllegalArgumentException("Provided value '" + s + "' was not a valid value in any of catalog types");
    }

    @Override
    public boolean matches(@NotNull ContextValue<CatalogType> contextValue, CatalogType t) {
        return contextValue.getParsedValue(this).equals(t);
    }

    @Override
    public boolean matches(CatalogType t, CatalogType t2) {
        return t.getId().equalsIgnoreCase(t2.getId());
    }

    @NotNull
    @Override
    public String serialize(CatalogType t) {
        return t.getId();
    }

    @NotNull
    @Override
    public Set<CatalogType> suggestValues(@NotNull CalculatedSubject subject) {
        return Arrays.stream(registries)
                .flatMap(reg -> reg.getAll().stream())
                .collect(Collectors.toSet());
    }
}
