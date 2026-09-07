package com.atlas.command;

public record CommandCausality(
        String traceId,
        CausalityRef cause,
        int depth
) {

    public CommandCausality {
        if (traceId == null || traceId.isBlank()) {
            throw new IllegalArgumentException(
                    "traceId cannot be null or blank"
            );
        }

        if (depth < 0) {
            throw new IllegalArgumentException(
                    "depth cannot be negative"
            );
        }
    }
}