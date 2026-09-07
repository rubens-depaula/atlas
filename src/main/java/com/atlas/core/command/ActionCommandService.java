package com.atlas.core.command;

import com.atlas.action.ActionDescriptor;
import com.atlas.action.ActionKey;
import com.atlas.action.ParameterDescriptor;
import com.atlas.command.Command;
import com.atlas.command.CommandActor;
import com.atlas.command.CommandCausality;
import com.atlas.command.CommandErrorCode;
import com.atlas.command.CommandId;
import com.atlas.command.CommandIdempotencyKey;
import com.atlas.command.CommandOrigin;
import com.atlas.command.CommandTarget;
import com.atlas.device.Criticality;
import com.atlas.device.Device;
import com.atlas.device.DeviceLifecycle;
import com.atlas.property.ValueDomain;
import com.atlas.property.ValueType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;

public final class ActionCommandService {

    public Command submit(
            CommandId commandId,
            Device device,
            ActionKey actionKey,
            Map<String, Object> parameters,
            CommandActor actor,
            CommandOrigin origin,
            CommandIdempotencyKey idempotencyKey,
            CommandCausality causality,
            Duration timeout,
            OffsetDateTime requestedAt
    ) {
        if (device == null) {
            throw new IllegalArgumentException(
                    "device cannot be null"
            );
        }

        if (actionKey == null) {
            throw new IllegalArgumentException(
                    "actionKey cannot be null"
            );
        }

        if (parameters == null) {
            throw new IllegalArgumentException(
                    "parameters cannot be null"
            );
        }

        ActionDescriptor action =
                device.actions()
                        .stream()
                        .filter(candidate ->
                                candidate.key().equals(actionKey)
                        )
                        .findFirst()
                        .orElse(null);

        Criticality criticality =
                resolveCriticality(
                        device,
                        action
                );

        Command command = new Command(
                commandId,
                actor,
                origin,
                new CommandTarget(
                        device.id(),
                        actionKey
                ),
                parameters,
                criticality,
                requestedAt,
                timeout,
                idempotencyKey,
                causality
        );

        if (device.lifecycle()
                == DeviceLifecycle.RETIRED) {

            command.reject(
                    CommandErrorCode.DEVICE_RETIRED,
                    "device is retired",
                    requestedAt
            );

            return command;
        }

        if (action == null) {

            command.reject(
                    CommandErrorCode.UNSUPPORTED_ACTION,
                    "device does not support action: "
                            + actionKey.value(),
                    requestedAt
            );

            return command;
        }

        String validationError =
                validateParameters(
                        action,
                        parameters
                );

        if (validationError != null) {

            command.reject(
                    CommandErrorCode.INVALID_PARAMETER,
                    validationError,
                    requestedAt
            );

            return command;
        }

        command.accept(requestedAt);

        return command;
    }

    private Criticality resolveCriticality(
            Device device,
            ActionDescriptor action
    ) {
        if (action != null
                && action.criticality() != null) {
            return action.criticality();
        }

        return device.defaultCriticality();
    }

    private String validateParameters(
            ActionDescriptor action,
            Map<String, Object> parameters
    ) {

        for (String providedKey : parameters.keySet()) {

            boolean exists =
                    action.parameters()
                            .stream()
                            .anyMatch(parameter ->
                                    parameter.key()
                                            .equals(providedKey)
                            );

            if (!exists) {
                return "unknown parameter: "
                        + providedKey;
            }
        }

        for (ParameterDescriptor descriptor
                : action.parameters()) {

            Object value =
                    parameters.get(
                            descriptor.key()
                    );

            if (descriptor.required()
                    && value == null) {

                return "required parameter missing: "
                        + descriptor.key();
            }

            if (value == null) {
                continue;
            }

            if (!matchesType(
                    value,
                    descriptor.valueType()
            )) {
                return "parameter "
                        + descriptor.key()
                        + " must be "
                        + descriptor.valueType();
            }

            String domainError =
                    validateDomain(
                            descriptor,
                            value
                    );

            if (domainError != null) {
                return domainError;
            }
        }

        return null;
    }

    private boolean matchesType(
            Object value,
            ValueType valueType
    ) {
        return switch (valueType) {

            case BOOLEAN ->
                    value instanceof Boolean;

            case INTEGER ->
                    isInteger(value);

            case DECIMAL ->
                    toBigDecimal(value) != null;

            case STRING,
                 ENUM ->
                    value instanceof String;

            case DURATION ->
                    value instanceof Duration
                            || isDurationString(value);

            case TIMESTAMP ->
                    value instanceof OffsetDateTime
                            || isTimestampString(value);
        };
    }

    private String validateDomain(
            ParameterDescriptor descriptor,
            Object value
    ) {
        ValueDomain domain =
                descriptor.valueDomain();

        if (domain == null) {
            return null;
        }

        if (descriptor.valueType()
                == ValueType.INTEGER
                || descriptor.valueType()
                == ValueType.DECIMAL) {

            BigDecimal numeric =
                    toBigDecimal(value);

            if (numeric == null) {
                return "parameter "
                        + descriptor.key()
                        + " must be numeric";
            }

            if (domain.min() != null
                    && numeric.compareTo(
                    domain.min()
            ) < 0) {

                return "parameter "
                        + descriptor.key()
                        + " must be >= "
                        + domain.min();
            }

            if (domain.max() != null
                    && numeric.compareTo(
                    domain.max()
            ) > 0) {

                return "parameter "
                        + descriptor.key()
                        + " must be <= "
                        + domain.max();
            }

            if (domain.step() != null
                    && domain.step()
                    .compareTo(BigDecimal.ZERO) > 0) {

                BigDecimal base =
                        domain.min() != null
                                ? domain.min()
                                : BigDecimal.ZERO;

                BigDecimal remainder =
                        numeric
                                .subtract(base)
                                .remainder(
                                        domain.step()
                                );

                if (remainder.compareTo(
                        BigDecimal.ZERO
                ) != 0) {

                    return "parameter "
                            + descriptor.key()
                            + " does not match step "
                            + domain.step();
                }
            }
        }

        if (descriptor.valueType()
                == ValueType.ENUM
                && !domain.allowedValues().isEmpty()
                && !domain.allowedValues()
                .contains(value)) {

            return "parameter "
                    + descriptor.key()
                    + " must be one of "
                    + domain.allowedValues();
        }

        return null;
    }

    private boolean isInteger(Object value) {
        BigDecimal number =
                toBigDecimal(value);

        if (number == null) {
            return false;
        }

        return number
                .stripTrailingZeros()
                .scale() <= 0;
    }

    private BigDecimal toBigDecimal(
            Object value
    ) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }

        if (value instanceof BigInteger integer) {
            return new BigDecimal(integer);
        }

        if (value instanceof Byte
                || value instanceof Short
                || value instanceof Integer
                || value instanceof Long) {

            return BigDecimal.valueOf(
                    ((Number) value).longValue()
            );
        }

        if (value instanceof Float
                || value instanceof Double) {

            return BigDecimal.valueOf(
                    ((Number) value).doubleValue()
            );
        }

        return null;
    }

    private boolean isDurationString(
            Object value
    ) {
        if (!(value instanceof String text)) {
            return false;
        }

        try {
            Duration.parse(text);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    private boolean isTimestampString(
            Object value
    ) {
        if (!(value instanceof String text)) {
            return false;
        }

        try {
            OffsetDateTime.parse(text);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }
}
