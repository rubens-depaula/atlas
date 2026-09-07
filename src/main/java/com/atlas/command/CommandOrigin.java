package com.atlas.command;

public record CommandOrigin(
        CommandOriginType type,
        String id
) {

    public CommandOrigin {
        if (type == null) {
            throw new IllegalArgumentException(
                    "type cannot be null"
            );
        }

        if (id != null && id.isBlank()) {
            throw new IllegalArgumentException(
                    "id cannot be blank"
            );
        }
    }
}