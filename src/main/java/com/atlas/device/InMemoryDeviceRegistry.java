package com.atlas.core.device;

import com.atlas.device.Device;
import com.atlas.device.DeviceId;
import com.atlas.device.DeviceRegistry;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryDeviceRegistry implements DeviceRegistry {

    private final ConcurrentMap<DeviceId, Device> devices =
            new ConcurrentHashMap<>();

    @Override
    public void register(Device device) {
        if (device == null) {
            throw new IllegalArgumentException(
                    "device cannot be null"
            );
        }

        Device previous = devices.putIfAbsent(
                device.id(),
                device
        );

        if (previous != null) {
            throw new IllegalArgumentException(
                    "device already registered: "
                            + device.id().value()
            );
        }
    }

    @Override
    public Optional<Device> findById(DeviceId id) {
        if (id == null) {
            throw new IllegalArgumentException(
                    "id cannot be null"
            );
        }

        return Optional.ofNullable(
                devices.get(id)
        );
    }

    @Override
    public List<Device> findAll() {
        return devices.values()
                .stream()
                .sorted(
                        Comparator.comparing(
                                device -> device.id().value()
                        )
                )
                .toList();
    }
}