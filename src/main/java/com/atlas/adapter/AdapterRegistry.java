package com.atlas.adapter;

import java.util.Optional;

public interface AdapterRegistry {

    Optional<DeviceAdapter> findByInstanceId(
            String instanceId
    );
}
