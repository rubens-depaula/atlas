package com.atlas.command;

import com.atlas.action.ActionKey;
import com.atlas.device.Criticality;
import com.atlas.device.DeviceId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandTest {

    @Test
    void shouldStartAsRequested() {
        Command command = command(
                new CommandActor(
                        CommandActorType.USER,
                        "user_test"
                ),
                new CommandCausality(
                        "trace_test",
                        null,
                        0
                )
        );

        assertEquals(
                CommandStatus.REQUESTED,
                command.status()
        );

        assertEquals(
                1,
                command.statusHistory().size()
        );

        assertEquals(
                CommandStatus.REQUESTED,
                command.statusHistory().getFirst().status()
        );

        assertNull(command.result());
        assertNull(command.completedAt());
    }

    @Test
    void shouldAcceptRequestedCommand() {
        Command command = command(
                new CommandActor(
                        CommandActorType.USER,
                        "user_test"
                ),
                new CommandCausality(
                        "trace_test",
                        null,
                        0
                )
        );

        OffsetDateTime acceptedAt =
                command.requestedAt().plus(Duration.ofMillis(10));

        command.accept(acceptedAt);

        assertEquals(
                CommandStatus.ACCEPTED,
                command.status()
        );

        assertEquals(
                2,
                command.statusHistory().size()
        );

        assertEquals(
                CommandStatus.ACCEPTED,
                command.statusHistory().getLast().status()
        );

        assertNull(command.result());
        assertNull(command.completedAt());
    }

    @Test
    void shouldRejectWithResult() {
        Command command = command(
                new CommandActor(
                        CommandActorType.USER,
                        "user_test"
                ),
                new CommandCausality(
                        "trace_test",
                        null,
                        0
                )
        );

        OffsetDateTime rejectedAt =
                command.requestedAt().plus(Duration.ofMillis(10));

        command.reject(
                CommandErrorCode.INVALID_PARAMETER,
                "value must be between 0 and 100",
                rejectedAt
        );

        assertEquals(
                CommandStatus.REJECTED,
                command.status()
        );

        assertTrue(command.status().isTerminal());

        assertNotNull(command.result());

        assertEquals(
                CommandOutcome.FAILURE,
                command.result().outcome()
        );

        assertEquals(
                CommandErrorCode.INVALID_PARAMETER,
                command.result().errorCode()
        );

        assertEquals(
                rejectedAt,
                command.completedAt()
        );
    }

    @Test
    void shouldNotChangeAfterTerminalStatus() {
        Command command = command(
                new CommandActor(
                        CommandActorType.USER,
                        "user_test"
                ),
                new CommandCausality(
                        "trace_test",
                        null,
                        0
                )
        );

        OffsetDateTime rejectedAt =
                command.requestedAt().plus(Duration.ofMillis(10));

        command.reject(
                CommandErrorCode.INVALID_PARAMETER,
                "invalid value",
                rejectedAt
        );

        assertThrows(
                IllegalStateException.class,
                () -> command.accept(
                        rejectedAt.plus(Duration.ofMillis(10))
                )
        );

        assertEquals(
                CommandStatus.REJECTED,
                command.status()
        );
    }

    @Test
    void shouldRejectAutomationWithoutCause() {
        CommandActor actor =
                new CommandActor(
                        CommandActorType.AUTOMATION,
                        "automation_test"
                );

        CommandCausality causality =
                new CommandCausality(
                        "trace_test",
                        null,
                        0
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> command(actor, causality)
        );
    }

    @Test
    void shouldAllowAutomationWithCause() {
        CommandActor actor =
                new CommandActor(
                        CommandActorType.AUTOMATION,
                        "automation_test"
                );

        CommandCausality causality =
                new CommandCausality(
                        "trace_test",
                        new CausalityRef(
                                CausalityRefType.EVENT,
                                "evt_test"
                        ),
                        1
                );

        Command command =
                command(actor, causality);

        assertEquals(
                CommandStatus.REQUESTED,
                command.status()
        );

        assertNotNull(
                command.causality().cause()
        );

        assertEquals(
                CausalityRefType.EVENT,
                command.causality().cause().type()
        );
    }

    private Command command(
            CommandActor actor,
            CommandCausality causality
    ) {
        OffsetDateTime now =
                OffsetDateTime.now();

        return new Command(
                new CommandId(
                        "cmd_00000000000000000000000000"
                ),
                actor,
                new CommandOrigin(
                        CommandOriginType.API,
                        "api_test"
                ),
                new CommandTarget(
                        new DeviceId(
                                "dev_demo_spot_01"
                        ),
                        new ActionKey(
                                "set_brightness"
                        )
                ),
                Map.of(
                        "value",
                        73
                ),
                Criticality.COSMETIC,
                now,
                Duration.ofSeconds(5),
                new CommandIdempotencyKey(
                        "idem_test",
                        Duration.ofSeconds(10)
                ),
                causality
        );
    }
}