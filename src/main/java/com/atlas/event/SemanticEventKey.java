package com.atlas.event;

public record SemanticEventKey(String value) {

    public SemanticEventKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "SemanticEventKey cannot be null or blank"
            );
        }

        if (!value.matches("^[a-z][a-z0-9_.]{0,63}$")) {
            throw new IllegalArgumentException(
                    "SemanticEventKey must start with a lowercase letter " +
                            "and contain only lowercase letters, numbers, underscores or dots"
            );
        }
    }
}