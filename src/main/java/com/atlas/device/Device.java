package com.atlas.device;

import com.atlas.action.ActionDescriptor;
import com.atlas.action.ActionKey;
import com.atlas.environment.EnvironmentId;
import com.atlas.environment.LocationId;
import com.atlas.event.SemanticEventDescriptor;
import com.atlas.property.PropertyDescriptor;
import com.atlas.property.PropertyKey;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Device {

    private final DeviceId id;
    private String name;

    private final EnvironmentId environmentId;
    private LocationId locationId;
    private DeviceId parentDeviceId;

    private final DeviceClass deviceClass;
    private DeviceLifecycle lifecycle;
    private final Criticality defaultCriticality;

    private DeviceSource source;

    private final List<PropertyDescriptor> properties;
    private final List<ActionDescriptor> actions;
    private final List<SemanticEventDescriptor> events;

    private DeviceMetadata metadata;

    private final OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime retiredAt;

    public Device(
            DeviceId id,
            String name,
            EnvironmentId environmentId,
            LocationId locationId,
            DeviceId parentDeviceId,
            DeviceClass deviceClass,
            DeviceLifecycle lifecycle,
            Criticality defaultCriticality,
            DeviceSource source,
            List<PropertyDescriptor> properties,
            List<ActionDescriptor> actions,
            List<SemanticEventDescriptor> events,
            DeviceMetadata metadata,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            OffsetDateTime retiredAt
    ) {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }

        validateName(name);

        if (environmentId == null) {
            throw new IllegalArgumentException(
                    "environmentId cannot be null"
            );
        }

        if (parentDeviceId != null && parentDeviceId.equals(id)) {
            throw new IllegalArgumentException(
                    "device cannot be its own parent"
            );
        }

        if (deviceClass == null) {
            throw new IllegalArgumentException(
                    "deviceClass cannot be null"
            );
        }

        if (lifecycle == null) {
            throw new IllegalArgumentException(
                    "lifecycle cannot be null"
            );
        }

        if (defaultCriticality == null) {
            throw new IllegalArgumentException(
                    "defaultCriticality cannot be null"
            );
        }

        if (source == null) {
            throw new IllegalArgumentException(
                    "source cannot be null"
            );
        }

        if (properties == null) {
            throw new IllegalArgumentException(
                    "properties cannot be null"
            );
        }

        if (actions == null) {
            throw new IllegalArgumentException(
                    "actions cannot be null"
            );
        }

        if (events == null) {
            throw new IllegalArgumentException(
                    "events cannot be null"
            );
        }

        if (createdAt == null) {
            throw new IllegalArgumentException(
                    "createdAt cannot be null"
            );
        }

        if (updatedAt == null) {
            throw new IllegalArgumentException(
                    "updatedAt cannot be null"
            );
        }

        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException(
                    "updatedAt cannot be before createdAt"
            );
        }

        this.id = id;
        this.name = name;
        this.environmentId = environmentId;
        this.locationId = locationId;
        this.parentDeviceId = parentDeviceId;
        this.deviceClass = deviceClass;
        this.lifecycle = lifecycle;
        this.defaultCriticality = defaultCriticality;
        this.source = source;

        this.properties = List.copyOf(properties);
        this.actions = List.copyOf(actions);
        this.events = List.copyOf(events);

        this.metadata = metadata;

        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.retiredAt = retiredAt;

        validateStructure();
    }

    private void validateStructure() {
        Set<PropertyKey> propertyKeys = new HashSet<>();
        Set<ActionKey> actionKeys = new HashSet<>();

        for (PropertyDescriptor property : properties) {
            if (!property.deviceId().equals(id)) {
                throw new IllegalArgumentException(
                        "property belongs to another device: "
                                + property.key().value()
                );
            }

            if (!propertyKeys.add(property.key())) {
                throw new IllegalArgumentException(
                        "duplicate property key: "
                                + property.key().value()
                );
            }
        }

        for (ActionDescriptor action : actions) {
            if (!actionKeys.add(action.key())) {
                throw new IllegalArgumentException(
                        "duplicate action key: "
                                + action.key().value()
                );
            }
        }

        for (PropertyDescriptor property : properties) {
            if (!property.readOnly()
                    && !actionKeys.contains(property.writeAction())) {
                throw new IllegalArgumentException(
                        "writeAction does not exist: "
                                + property.writeAction().value()
                );
            }
        }

        for (ActionDescriptor action : actions) {
            for (PropertyKey affected : action.affectsProperties()) {
                if (!propertyKeys.contains(affected)) {
                    throw new IllegalArgumentException(
                            "action references unknown property: "
                                    + affected.value()
                    );
                }
            }

            if (action.cancelAction() != null
                    && !actionKeys.contains(action.cancelAction())) {
                throw new IllegalArgumentException(
                        "cancelAction does not exist: "
                                + action.cancelAction().value()
                );
            }

            Criticality effectiveCriticality =
                    action.criticality() != null
                            ? action.criticality()
                            : defaultCriticality;

            if (action.durable()
                    && effectiveCriticality == Criticality.CRITICAL
                    && action.cancelAction() == null) {
                throw new IllegalArgumentException(
                        "durable CRITICAL action requires cancelAction"
                );
            }
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "name cannot be null or blank"
            );
        }

        if (name.length() > 120) {
            throw new IllegalArgumentException(
                    "name cannot be longer than 120 characters"
            );
        }
    }

    public void rename(String newName, OffsetDateTime changedAt) {
        validateName(newName);
        requireValidChangeTime(changedAt);

        this.name = newName;
        this.updatedAt = changedAt;
    }

    public void relocate(LocationId newLocationId, OffsetDateTime changedAt) {
        requireValidChangeTime(changedAt);

        this.locationId = newLocationId;
        this.updatedAt = changedAt;
    }

    public void rebind(DeviceSource newSource, OffsetDateTime changedAt) {
        if (newSource == null) {
            throw new IllegalArgumentException(
                    "newSource cannot be null"
            );
        }

        requireValidChangeTime(changedAt);

        this.source = newSource;
        this.updatedAt = changedAt;
    }

    public void retire(OffsetDateTime retiredAt) {
        requireValidChangeTime(retiredAt);

        this.lifecycle = DeviceLifecycle.RETIRED;
        this.retiredAt = retiredAt;
        this.updatedAt = retiredAt;
    }

    private void requireValidChangeTime(OffsetDateTime changedAt) {
        if (changedAt == null) {
            throw new IllegalArgumentException(
                    "changedAt cannot be null"
            );
        }

        if (changedAt.isBefore(updatedAt)) {
            throw new IllegalArgumentException(
                    "changedAt cannot be before updatedAt"
            );
        }
    }

    public DeviceId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public EnvironmentId environmentId() {
        return environmentId;
    }

    public LocationId locationId() {
        return locationId;
    }

    public DeviceId parentDeviceId() {
        return parentDeviceId;
    }

    public DeviceClass deviceClass() {
        return deviceClass;
    }

    public DeviceLifecycle lifecycle() {
        return lifecycle;
    }

    public Criticality defaultCriticality() {
        return defaultCriticality;
    }

    public DeviceSource source() {
        return source;
    }

    public List<PropertyDescriptor> properties() {
        return properties;
    }

    public List<ActionDescriptor> actions() {
        return actions;
    }

    public List<SemanticEventDescriptor> events() {
        return events;
    }

    public DeviceMetadata metadata() {
        return metadata;
    }

    public OffsetDateTime createdAt() {
        return createdAt;
    }

    public OffsetDateTime updatedAt() {
        return updatedAt;
    }

    public OffsetDateTime retiredAt() {
        return retiredAt;
    }
}