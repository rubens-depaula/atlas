package com.atlas.command;

public record CommandActor(
        CommandActorType type,
        String id
) {

    public CommandActor {
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