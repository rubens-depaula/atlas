package com.atlas.property;

import com.atlas.device.DeviceId;

public record PropertyDescriptor(
        DeviceId deviceId,
        PropertyKey key,
        String name,
        SemanticType semanticType,
        ValueType valueType,
        Unit unit,
        ValueDomain valueDomain,
        boolean readOnly,
        String writeAction,
        FreshnessPolicy freshnessPolicy
) {

    public PropertyDescriptor {
        if (deviceId == null) {
            throw new IllegalArgumentException("deviceId cannot be null");
        }

        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name cannot be null or blank");
        }

        if (name.length() > 120) {
            throw new IllegalArgumentException(
                    "name cannot be longer than 120 characters"
            );
        }

        if (semanticType == null) {
            throw new IllegalArgumentException("semanticType cannot be null");
        }

        if (valueType == null) {
            throw new IllegalArgumentException("valueType cannot be null");
        }

        if (unit == null) {
            throw new IllegalArgumentException("unit cannot be null");
        }

        if (freshnessPolicy == null) {
            throw new IllegalArgumentException(
                    "freshnessPolicy cannot be null"
            );
        }

        if (!readOnly && (writeAction == null || writeAction.isBlank())) {
            throw new IllegalArgumentException(
                    "writeAction is required when property is writable"
            );
        }

        if (valueType == ValueType.ENUM && valueDomain == null) {
            throw new IllegalArgumentException(
                    "valueDomain is required for ENUM properties"
            );
        }
    }
}