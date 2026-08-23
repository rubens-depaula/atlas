package com.atlas.device;

public record DeviceSource(
        String adapterInstanceId,
        String nativeId
) {

    public DeviceSource {
        if (adapterInstanceId == null || adapterInstanceId.isBlank()) {
            throw new IllegalArgumentException(
                    "adapterInstanceId cannot be null or blank"
            );
        }

        if (nativeId == null || nativeId.isBlank()) {
            throw new IllegalArgumentException(
                    "nativeId cannot be null or blank"
            );
        }
    }
}