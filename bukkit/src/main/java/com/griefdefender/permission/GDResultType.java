package com.griefdefender.permission;

import com.griefdefender.api.permission.ResultType;
import net.kyori.text.Component;

public class GDResultType implements ResultType {

    private final String id;
    private final String name;
    private Component description;

    public GDResultType(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Component getDescription() {
        return this.description;
    }
}