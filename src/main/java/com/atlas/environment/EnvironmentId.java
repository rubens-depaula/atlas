package com.atlas.environment;

public record EnvironmentId(String value) {

    public EnvironmentId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "EnvironmentId cannot be null or blank"
            );
        }
    }
}