package com.atlas.core.adapter;

import com.atlas.adapter.AdapterRegistry;
import com.atlas.adapter.DeviceAdapter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryAdapterRegistry
        implements AdapterRegistry {

    private final Map<String, DeviceAdapter> adapters;

    public InMemoryAdapterRegistry(
            List<DeviceAdapter> adapters
    ) {
        if (adapters == null) {
            throw new IllegalArgumentException(
                    "adapters cannot be null"
            );
        }

        Map<String, DeviceAdapter> indexed =
                new LinkedHashMap<>();

        for (DeviceAdapter adapter : adapters) {

            if (adapter == null) {
                throw new IllegalArgumentException(
                        "adapter cannot be null"
                );
            }

            String instanceId =
                    adapter.instanceId();

            if (instanceId == null
                    || instanceId.isBlank()) {
                throw new IllegalArgumentException(
                        "adapter instanceId cannot be null or blank"
                );
            }

            DeviceAdapter previous =
                    indexed.putIfAbsent(
                            instanceId,
                            adapter
                    );

            if (previous != null) {
                throw new IllegalArgumentException(
                        "duplicate adapter instanceId: "
                                + instanceId
                );
            }
        }

        this.adapters =
                Map.copyOf(indexed);
    }

    @Override
    public Optional<DeviceAdapter> findByInstanceId(
            String instanceId
    ) {
        if (instanceId == null
                || instanceId.isBlank()) {
            throw new IllegalArgumentException(
                    "instanceId cannot be null or blank"
            );
        }

        return Optional.ofNullable(
                adapters.get(instanceId)
        );
    }
}
