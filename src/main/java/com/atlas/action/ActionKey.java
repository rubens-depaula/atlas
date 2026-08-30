package com.atlas.action;

public record ActionKey(String value) {

    public ActionKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "ActionKey cannot be null or blank"
            );
        }

        if (!value.matches("^[a-z][a-z0-9_]{0,63}$")) {
            throw new IllegalArgumentException(
                    "ActionKey must start with a lowercase letter " +
                            "and contain only lowercase letters, numbers or underscores"
            );
        }
    }
}