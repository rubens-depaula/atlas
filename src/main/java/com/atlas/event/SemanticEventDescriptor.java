package com.atlas.event;

import com.atlas.action.ParameterDescriptor;

import java.util.List;

public record SemanticEventDescriptor(
        SemanticEventKey key,
        String name,
        List<ParameterDescriptor> payloadFields
) {

    public SemanticEventDescriptor {
        if (key == null) {
            throw new IllegalArgumentException(
                    "key cannot be null"
            );
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "name cannot be null or blank"
            );
        }

        if (payloadFields == null) {
            payloadFields = List.of();
        } else {
            payloadFields = List.copyOf(payloadFields);
        }
    }
}