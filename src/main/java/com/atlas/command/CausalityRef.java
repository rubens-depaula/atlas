package com.atlas.command;

public record CausalityRef(
        CausalityRefType type,
        String id
) {

    public CausalityRef {
        if (type == null) {
            throw new IllegalArgumentException(
                    "type cannot be null"
            );
        }

        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                    "id cannot be null or blank"
            );
        }
    }
}