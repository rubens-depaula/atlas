package com.atlas.core.http;

import com.atlas.action.ActionDescriptor;
import com.atlas.action.ParameterDescriptor;
import com.atlas.device.Criticality;
import com.atlas.device.Device;
import com.atlas.device.DeviceId;
import com.atlas.device.DeviceRegistry;
import com.atlas.property.PropertyDescriptor;
import com.atlas.property.ValueDomain;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
public class DeviceController {

    private final DeviceRegistry deviceRegistry;

    public DeviceController(DeviceRegistry deviceRegistry) {
        this.deviceRegistry = deviceRegistry;
    }

    @GetMapping("/api/devices")
    public List<DeviceSummaryResponse> findAll() {
        return deviceRegistry.findAll()
                .stream()
                .map(DeviceSummaryResponse::from)
                .toList();
    }

    @GetMapping("/api/devices/{id}")
    public ResponseEntity<DeviceDetailResponse> findById(
            @PathVariable String id
    ) {
        return deviceRegistry
                .findById(new DeviceId(id))
                .map(DeviceDetailResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record DeviceSummaryResponse(
            String id,
            String name,
            String deviceClass,
            String lifecycle
    ) {

        public static DeviceSummaryResponse from(Device device) {
            return new DeviceSummaryResponse(
                    device.id().value(),
                    device.name(),
                    device.deviceClass().name(),
                    device.lifecycle().name()
            );
        }
    }

    public record DeviceDetailResponse(
            String id,
            String name,
            String deviceClass,
            String lifecycle,
            String defaultCriticality,
            List<PropertyResponse> properties,
            List<ActionResponse> actions
    ) {

        public static DeviceDetailResponse from(Device device) {
            return new DeviceDetailResponse(
                    device.id().value(),
                    device.name(),
                    device.deviceClass().name(),
                    device.lifecycle().name(),
                    device.defaultCriticality().name(),

                    device.properties()
                            .stream()
                            .map(PropertyResponse::from)
                            .toList(),

                    device.actions()
                            .stream()
                            .map(action -> ActionResponse.from(
                                    action,
                                    device.defaultCriticality()
                            ))
                            .toList()
            );
        }
    }

    public record PropertyResponse(
            String key,
            String name,
            String semanticType,
            String valueType,
            String unit,
            boolean readOnly,
            String writeAction,
            ValueDomainResponse valueDomain
    ) {

        public static PropertyResponse from(
                PropertyDescriptor property
        ) {
            return new PropertyResponse(
                    property.key().value(),
                    property.name(),
                    property.semanticType().name(),
                    property.valueType().name(),
                    property.unit().name(),
                    property.readOnly(),
                    property.writeAction() != null
                            ? property.writeAction().value()
                            : null,
                    ValueDomainResponse.from(
                            property.valueDomain()
                    )
            );
        }
    }

    public record ActionResponse(
            String key,
            String name,
            String criticality,
            boolean durable,
            List<ParameterResponse> parameters,
            List<String> affectsProperties,
            String cancelAction
    ) {

        public static ActionResponse from(
                ActionDescriptor action,
                Criticality defaultCriticality
        ) {
            Criticality effectiveCriticality =
                    action.criticality() != null
                            ? action.criticality()
                            : defaultCriticality;

            return new ActionResponse(
                    action.key().value(),
                    action.name(),
                    effectiveCriticality.name(),
                    action.durable(),

                    action.parameters()
                            .stream()
                            .map(ParameterResponse::from)
                            .toList(),

                    action.affectsProperties()
                            .stream()
                            .map(property -> property.value())
                            .toList(),

                    action.cancelAction() != null
                            ? action.cancelAction().value()
                            : null
            );
        }
    }

    public record ParameterResponse(
            String key,
            String name,
            String valueType,
            String unit,
            boolean required,
            ValueDomainResponse valueDomain
    ) {

        public static ParameterResponse from(
                ParameterDescriptor parameter
        ) {
            return new ParameterResponse(
                    parameter.key(),
                    parameter.name(),
                    parameter.valueType().name(),
                    parameter.unit().name(),
                    parameter.required(),
                    ValueDomainResponse.from(
                            parameter.valueDomain()
                    )
            );
        }
    }

    public record ValueDomainResponse(
            BigDecimal min,
            BigDecimal max,
            BigDecimal step,
            List<String> allowedValues,
            boolean monotonic
    ) {

        public static ValueDomainResponse from(
                ValueDomain domain
        ) {
            if (domain == null) {
                return null;
            }

            return new ValueDomainResponse(
                    domain.min(),
                    domain.max(),
                    domain.step(),
                    domain.allowedValues(),
                    domain.monotonic()
            );
        }
    }
}