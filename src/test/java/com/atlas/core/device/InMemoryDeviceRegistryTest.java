package com.atlas.core.device;

import com.atlas.device.Criticality;
import com.atlas.device.Device;
import com.atlas.device.DeviceClass;
import com.atlas.device.DeviceId;
import com.atlas.device.DeviceLifecycle;
import com.atlas.device.DeviceSource;
import com.atlas.environment.EnvironmentId;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryDeviceRegistryTest {

    @Test
    void shouldRegisterAndFindDeviceById() {
        InMemoryDeviceRegistry registry =
                new InMemoryDeviceRegistry();

        Device device = device("dev_001");

        registry.register(device);

        assertTrue(
                registry.findById(device.id()).isPresent()
        );

        assertEquals(
                device,
                registry.findById(device.id()).orElseThrow()
        );
    }

    @Test
    void shouldRejectDuplicateDeviceId() {
        InMemoryDeviceRegistry registry =
                new InMemoryDeviceRegistry();

        registry.register(device("dev_001"));

        assertThrows(
                IllegalArgumentException.class,
                () -> registry.register(device("dev_001"))
        );
    }

    @Test
    void shouldReturnAllDevices() {
        InMemoryDeviceRegistry registry =
                new InMemoryDeviceRegistry();

        registry.register(device("dev_002"));
        registry.register(device("dev_001"));

        List<Device> devices = registry.findAll();

        assertEquals(2, devices.size());
        assertEquals(
                "dev_001",
                devices.get(0).id().value()
        );
        assertEquals(
                "dev_002",
                devices.get(1).id().value()
        );
    }

    private Device device(String id) {
        OffsetDateTime now = OffsetDateTime.now();

        return new Device(
                new DeviceId(id),
                "Test device",
                new EnvironmentId("env_test"),
                null,
                null,
                DeviceClass.LIGHT,
                DeviceLifecycle.ACTIVE,
                Criticality.NORMAL,
                new DeviceSource(
                        "test.adapter",
                        id,
                        now
                ),
                List.of(),
                List.of(),
                List.of(),
                null,
                now,
                now,
                null
        );
    }
}