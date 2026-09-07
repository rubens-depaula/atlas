package com.atlas.command;

public record CommandId(String value) {

    private static final String PATTERN =
            "^cmd_[0-9A-HJKMNP-TV-Z]{26}$";

    public CommandId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "CommandId cannot be null or blank"
            );
        }

        if (!value.matches(PATTERN)) {
            throw new IllegalArgumentException(
                    "CommandId must use the format cmd_<ULID>"
            );
        }
    }
}