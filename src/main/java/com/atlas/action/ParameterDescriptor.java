package com.atlas.action;

import com.atlas.property.Unit;
import com.atlas.property.ValueDomain;
import com.atlas.property.ValueType;

public record ParameterDescriptor(
        String key,
        String name,
        ValueType valueType,
        Unit unit,
        ValueDomain valueDomain,
        boolean required
) {

    public ParameterDescriptor {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException(
                    "key cannot be null or blank"
            );
        }

        if (!key.matches("^[a-z][a-z0-9_]{0,63}$")) {
            throw new IllegalArgumentException(
                    "key must start with a lowercase letter " +
                            "and contain only lowercase letters, numbers or underscores"
            );
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "name cannot be null or blank"
            );
        }

        if (valueType == null) {
            throw new IllegalArgumentException(
                    "valueType cannot be null"
            );
        }

        if (unit == null) {
            throw new IllegalArgumentException(
                    "unit cannot be null"
            );
        }

        if (valueType == ValueType.ENUM && valueDomain == null) {
            throw new IllegalArgumentException(
                    "valueDomain is required for ENUM parameters"
            );
        }
    }
}