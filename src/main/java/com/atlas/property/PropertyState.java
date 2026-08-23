package com.atlas.property;

import com.atlas.device.DeviceId;

import java.time.OffsetDateTime;

public record PropertyState(
        DeviceId deviceId,
        PropertyKey key,
        Object value,
        OffsetDateTime observedAt,
        OffsetDateTime receivedAt,
        Availability availability,
        Freshness freshness,
        Confidence confidence,
        String assumedFromCommandId
) {

    public PropertyState {
        if (deviceId == null) {
            throw new IllegalArgumentException("deviceId cannot be null");
        }

        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }

        if (availability == null) {
            throw new IllegalArgumentException("availability cannot be null");
        }

        if (freshness == null) {
            throw new IllegalArgumentException("freshness cannot be null");
        }

        if (confidence == null) {
            throw new IllegalArgumentException("confidence cannot be null");
        }

        if (confidence == Confidence.ASSUMED
                && (assumedFromCommandId == null || assumedFromCommandId.isBlank())) {
            throw new IllegalArgumentException(
                    "assumedFromCommandId is required when confidence is ASSUMED"
            );
        }
    }
}