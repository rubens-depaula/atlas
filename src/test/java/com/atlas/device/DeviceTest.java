package com.atlas.device;

import com.atlas.action.ActionDescriptor;
import com.atlas.action.ActionKey;
import com.atlas.environment.EnvironmentId;
import com.atlas.property.FreshnessPolicy;
import com.atlas.property.PropertyDescriptor;
import com.atlas.property.PropertyKey;
import com.atlas.property.SemanticType;
import com.atlas.property.Unit;
import com.atlas.property.ValueType;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeviceTest {

    private final DeviceId deviceId = new DeviceId("dev_test");
    private final EnvironmentId environmentId = new EnvironmentId("env_test");

    @Test
    void shouldCreateValidDevice() {
        assertDoesNotThrow(() -> new Device(
                deviceId,
                "Test device",
                environmentId,
                null,
                null,
                DeviceClass.LIGHT,
                DeviceLifecycle.ACTIVE,
                Criticality.NORMAL,
                source(),
                List.of(),
                List.of(),
                List.of(),
                null,
                now(),
                now(),
                null
        ));
    }

    @Test
    void shouldRejectPropertyFromAnotherDevice() {
        PropertyDescriptor property = readOnlyProperty(
                new DeviceId("dev_other"),
                "power_state"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> device(
                        List.of(property),
                        List.of()
                )
        );
    }

    @Test
    void shouldRejectDuplicatePropertyKey() {
        PropertyDescriptor first = readOnlyProperty(
                deviceId,
                "power_state"
        );

        PropertyDescriptor second = readOnlyProperty(
                deviceId,
                "power_state"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> device(
                        List.of(first, second),
                        List.of()
                )
        );
    }

    @Test
    void shouldRejectUnknownWriteAction() {
        PropertyDescriptor property = new PropertyDescriptor(
                deviceId,
                new PropertyKey("power_state"),
                "Power state",
                SemanticType.POWER_STATE,
                ValueType.BOOLEAN,
                Unit.NONE,
                null,
                false,
                new ActionKey("turn_on"),
                FreshnessPolicy.OnChange.withoutHeartbeat()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> device(
                        List.of(property),
                        List.of()
                )
        );
    }

    @Test
    void shouldRejectActionReferencingUnknownProperty() {
        ActionDescriptor action = new ActionDescriptor(
                new ActionKey("turn_on"),
                "Turn on",
                List.of(),
                Criticality.NORMAL,
                false,
                null,
                null,
                null,
                List.of(new PropertyKey("power_state")),
                null
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> device(
                        List.of(),
                        List.of(action)
                )
        );
    }

    @Test
    void shouldRejectDurableCriticalActionWithoutCancelAction() {
        ActionDescriptor action = new ActionDescriptor(
                new ActionKey("start_pumping"),
                "Start pumping",
                List.of(),
                null,
                true,
                null,
                null,
                null,
                List.of(),
                null
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new Device(
                        deviceId,
                        "Water pump",
                        environmentId,
                        null,
                        null,
                        DeviceClass.PUMP,
                        DeviceLifecycle.ACTIVE,
                        Criticality.CRITICAL,
                        source(),
                        List.of(),
                        List.of(action),
                        List.of(),
                        null,
                        now(),
                        now(),
                        null
                )
        );
    }

    private Device device(
            List<PropertyDescriptor> properties,
            List<ActionDescriptor> actions
    ) {
        return new Device(
                deviceId,
                "Test device",
                environmentId,
                null,
                null,
                DeviceClass.LIGHT,
                DeviceLifecycle.ACTIVE,
                Criticality.NORMAL,
                source(),
                properties,
                actions,
                List.of(),
                null,
                now(),
                now(),
                null
        );
    }

    private PropertyDescriptor readOnlyProperty(
            DeviceId owner,
            String key
    ) {
        return new PropertyDescriptor(
                owner,
                new PropertyKey(key),
                "Property " + key,
                SemanticType.POWER_STATE,
                ValueType.BOOLEAN,
                Unit.NONE,
                null,
                true,
                null,
                FreshnessPolicy.OnChange.withoutHeartbeat()
        );
    }

    private DeviceSource source() {
        return new DeviceSource(
                "test.adapter",
                "device_01",
                now()
        );
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now();
    }
}