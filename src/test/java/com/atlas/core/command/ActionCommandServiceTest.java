package com.atlas.core.command;

import com.atlas.action.ActionDescriptor;
import com.atlas.action.ActionKey;
import com.atlas.action.ParameterDescriptor;
import com.atlas.command.Command;
import com.atlas.command.CommandActor;
import com.atlas.command.CommandActorType;
import com.atlas.command.CommandCausality;
import com.atlas.command.CommandErrorCode;
import com.atlas.command.CommandId;
import com.atlas.command.CommandIdempotencyKey;
import com.atlas.command.CommandOrigin;
import com.atlas.command.CommandOriginType;
import com.atlas.command.CommandStatus;
import com.atlas.device.Criticality;
import com.atlas.device.Device;
import com.atlas.device.DeviceClass;
import com.atlas.device.DeviceId;
import com.atlas.device.DeviceLifecycle;
import com.atlas.device.DeviceSource;
import com.atlas.environment.EnvironmentId;
import com.atlas.property.Unit;
import com.atlas.property.ValueDomain;
import com.atlas.property.ValueType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ActionCommandServiceTest {

    private final ActionCommandService service =
            new ActionCommandService();

    @Test
    void shouldAcceptValidBrightness() {
        Command command = submit(
                device(),
                "set_brightness",
                Map.of("value", 73)
        );

        assertEquals(
                CommandStatus.ACCEPTED,
                command.status()
        );

        assertNull(command.result());
    }

    @Test
    void shouldRejectBrightnessAboveMaximum() {
        Command command = submit(
                device(),
                "set_brightness",
                Map.of("value", 500)
        );

        assertEquals(
                CommandStatus.REJECTED,
                command.status()
        );

        assertNotNull(command.result());

        assertEquals(
                CommandErrorCode.INVALID_PARAMETER,
                command.result().errorCode()
        );
    }

    @Test
    void shouldRejectWrongParameterType() {
        Command command = submit(
                device(),
                "set_brightness",
                Map.of("value", "73")
        );

        assertEquals(
                CommandStatus.REJECTED,
                command.status()
        );

        assertEquals(
                CommandErrorCode.INVALID_PARAMETER,
                command.result().errorCode()
        );
    }

    @Test
    void shouldRejectMissingRequiredParameter() {
        Command command = submit(
                device(),
                "set_brightness",
                Map.of()
        );

        assertEquals(
                CommandStatus.REJECTED,
                command.status()
        );

        assertEquals(
                CommandErrorCode.INVALID_PARAMETER,
                command.result().errorCode()
        );
    }

    @Test
    void shouldRejectUnknownParameter() {
        Command command = submit(
                device(),
                "set_brightness",
                Map.of("abacaxi", 73)
        );

        assertEquals(
                CommandStatus.REJECTED,
                command.status()
        );

        assertEquals(
                CommandErrorCode.INVALID_PARAMETER,
                command.result().errorCode()
        );
    }

    @Test
    void shouldAcceptTurnOnWithoutParameters() {
        Command command = submit(
                device(),
                "turn_on",
                Map.of()
        );

        assertEquals(
                CommandStatus.ACCEPTED,
                command.status()
        );
    }

    @Test
    void shouldRejectUnsupportedAction() {
        Command command = submit(
                device(),
                "explode_everything",
                Map.of()
        );

        assertEquals(
                CommandStatus.REJECTED,
                command.status()
        );

        assertEquals(
                CommandErrorCode.UNSUPPORTED_ACTION,
                command.result().errorCode()
        );
    }

    private Command submit(
            Device device,
            String action,
            Map<String, Object> parameters
    ) {
        OffsetDateTime now =
                OffsetDateTime.now();

        return service.submit(
                new CommandId(
                        "cmd_00000000000000000000000000"
                ),
                device,
                new ActionKey(action),
                parameters,
                new CommandActor(
                        CommandActorType.USER,
                        "user_test"
                ),
                new CommandOrigin(
                        CommandOriginType.API,
                        "api_test"
                ),
                new CommandIdempotencyKey(
                        "idem_test",
                        Duration.ofSeconds(10)
                ),
                new CommandCausality(
                        "trace_test",
                        null,
                        0
                ),
                Duration.ofSeconds(5),
                now
        );
    }

    private Device device() {
        OffsetDateTime now =
                OffsetDateTime.now();

        ActionDescriptor setBrightness =
                new ActionDescriptor(
                        new ActionKey(
                                "set_brightness"
                        ),
                        "Definir intensidade",
                        List.of(
                                new ParameterDescriptor(
                                        "value",
                                        "Intensidade",
                                        ValueType.INTEGER,
                                        Unit.PERCENT,
                                        new ValueDomain(
                                                BigDecimal.ZERO,
                                                new BigDecimal("100"),
                                                BigDecimal.ONE,
                                                List.of(),
                                                false
                                        ),
                                        true
                                )
                        ),
                        Criticality.COSMETIC,
                        false,
                        null,
                        null,
                        null,
                        List.of(),
                        null
                );

        ActionDescriptor turnOn =
                new ActionDescriptor(
                        new ActionKey("turn_on"),
                        "Ligar",
                        List.of(),
                        Criticality.COSMETIC,
                        false,
                        null,
                        null,
                        null,
                        List.of(),
                        null
                );

        return new Device(
                new DeviceId(
                        "dev_demo_spot_01"
                ),
                "Spot da bancada 01",
                new EnvironmentId(
                        "env_apartamento"
                ),
                null,
                null,
                DeviceClass.LIGHT,
                DeviceLifecycle.ACTIVE,
                Criticality.COSMETIC,
                new DeviceSource(
                        "demo.adapter",
                        "spot_bancada_01",
                        now
                ),
                List.of(),
                List.of(
                        setBrightness,
                        turnOn
                ),
                List.of(),
                null,
                now,
                now,
                null
        );
    }
}