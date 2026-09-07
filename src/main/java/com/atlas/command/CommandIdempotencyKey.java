package com.atlas.command;

import java.time.Duration;

public record CommandIdempotencyKey(
        String value,
        Duration window
) {

    public CommandIdempotencyKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "value cannot be null or blank"
            );
        }

        if (window == null
                || window.isZero()
                || window.isNegative()) {
            throw new IllegalArgumentException(
                    "window must be greater than zero"
            );
        }
    }
}