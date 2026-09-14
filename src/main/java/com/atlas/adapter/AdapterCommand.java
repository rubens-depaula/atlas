package com.atlas.adapter;

import java.util.LinkedHashMap;
import java.util.Map;

public record AdapterCommand(
        String commandId,
        String nativeId,
        String action,
        Map<String, Object> parameters
) {

    public AdapterCommand {
        if (commandId == null || commandId.isBlank()) {
            throw new IllegalArgumentException(
                    "commandId cannot be null or blank"
            );
        }

        if (nativeId == null || nativeId.isBlank()) {
            throw new IllegalArgumentException(
                    "nativeId cannot be null or blank"
            );
        }

        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException(
                    "action cannot be null or blank"
            );
        }

        if (parameters == null) {
            throw new IllegalArgumentException(
                    "parameters cannot be null"
            );
        }

        parameters = Map.copyOf(
                new LinkedHashMap<>(parameters)
        );
    }
}
