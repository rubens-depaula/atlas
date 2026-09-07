package com.atlas.device;

import java.util.List;
import java.util.Optional;

public interface DeviceRegistry {

    void register(Device device);

    Optional<Device> findById(DeviceId id);

    List<Device> findAll();
}