package com.atlas.device;

public record DeviceMetadata(
        String manufacturer,
        String model,
        String firmwareVersion,
        String serialNumber,
        String notes
) {
}