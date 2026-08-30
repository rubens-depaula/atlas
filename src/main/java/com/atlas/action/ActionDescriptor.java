package com.atlas.action;

import com.atlas.device.Criticality;
import com.atlas.property.PropertyKey;

import java.time.Duration;
import java.util.List;

public record ActionDescriptor(
        ActionKey key,
        String name,
        List<ParameterDescriptor> parameters,
        Criticality criticality,
        boolean durable,
        Duration expectedDuration,
        String durationParameter,
        ActionKey cancelAction,
        List<PropertyKey> affectsProperties,
        ActionSafety safety
) {

    public ActionDescriptor {
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

        if (parameters == null) {
            throw new IllegalArgumentException(
                    "parameters cannot be null"
            );
        }

        parameters = List.copyOf(parameters);

        if (affectsProperties == null) {
            throw new IllegalArgumentException(
                    "affectsProperties cannot be null"
            );
        }

        affectsProperties = List.copyOf(affectsProperties);

        if (expectedDuration != null
                && (expectedDuration.isZero() || expectedDuration.isNegative())) {
            throw new IllegalArgumentException(
                    "expectedDuration must be greater than zero"
            );
        }

        if (expectedDuration != null && durationParameter != null) {
            throw new IllegalArgumentException(
                    "use either expectedDuration or durationParameter, not both"
            );
        }

        if (durationParameter != null && durationParameter.isBlank()) {
            throw new IllegalArgumentException(
                    "durationParameter cannot be blank"
            );
        }

        if (durable
                && criticality == Criticality.CRITICAL
                && cancelAction == null) {
            throw new IllegalArgumentException(
                    "cancelAction is required for durable CRITICAL actions"
            );
        }
    }
}