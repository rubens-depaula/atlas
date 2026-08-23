package com.atlas.environment;

public record LocationId(String value) {

    public LocationId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "LocationId cannot be null or blank"
            );
        }
    }
}