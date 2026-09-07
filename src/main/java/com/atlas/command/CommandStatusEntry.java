package com.atlas.command;

import java.time.OffsetDateTime;

public record CommandStatusEntry(
        CommandStatus status,
        OffsetDateTime at,
        String detail
) {

    public CommandStatusEntry {
        if (status == null) {
            throw new IllegalArgumentException(
                    "status cannot be null"
            );
        }

        if (at == null) {
            throw new IllegalArgumentException(
                    "at cannot be null"
            );
        }

        if (detail != null && detail.isBlank()) {
            throw new IllegalArgumentException(
                    "detail cannot be blank"
            );
        }
    }
}