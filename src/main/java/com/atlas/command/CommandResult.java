package com.atlas.command;

import java.time.OffsetDateTime;

public record CommandResult(
        CommandOutcome outcome,
        CommandErrorCode errorCode,
        String errorMessage,
        String adapterMessageId,
        OffsetDateTime at
) {

    public CommandResult {
        if (outcome == null) {
            throw new IllegalArgumentException(
                    "outcome cannot be null"
            );
        }

        if (at == null) {
            throw new IllegalArgumentException(
                    "at cannot be null"
            );
        }

        if (errorMessage != null && errorMessage.isBlank()) {
            throw new IllegalArgumentException(
                    "errorMessage cannot be blank"
            );
        }

        if (adapterMessageId != null && adapterMessageId.isBlank()) {
            throw new IllegalArgumentException(
                    "adapterMessageId cannot be blank"
            );
        }
    }
}