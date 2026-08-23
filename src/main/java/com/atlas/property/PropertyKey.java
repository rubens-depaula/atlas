package com.atlas.property;

public record PropertyKey(String value) {

    public PropertyKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "PropertyKey cannot be null or blank"
            );
        }
    }
}