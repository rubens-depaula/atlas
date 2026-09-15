package com.atlas.core.command;

import com.atlas.action.ActionKey;
import com.atlas.command.Command;
import com.atlas.command.CommandActor;
import com.atlas.command.CommandActorType;
import com.atlas.command.CommandCausality;
import com.atlas.command.CommandId;
import com.atlas.command.CommandIdempotencyKey;
import com.atlas.command.CommandOrigin;
import com.atlas.command.CommandOriginType;
import com.atlas.command.CommandStatus;
import com.atlas.command.CommandTarget;
import com.atlas.device.Criticality;
import com.atlas.device.DeviceId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandTimeoutWatchdogTest {

    @Test
    void shouldIgnoreCommandStillInsideTimeoutWindow() {

        OffsetDateTime requestedAt =
                OffsetDateTime.parse(
                        "2026-09-15T20:00:00-03:00"
                );

        Command command =
                acceptedCommand(
                        "cmd_00000000000000000000000010",
                        requestedAt
                );

        InMemoryCommandRegistry registry =
                registered(command);

        CommandTimeoutWatchdog watchdog =
                watchdog(registry);

        int timedOut =
                watchdog.scan(
                        requestedAt.plusSeconds(4)
                );

        assertEquals(
                0,
                timedOut
        );

        assertEquals(
                CommandStatus.ACCEPTED,
                command.status()
        );
    }

    @Test
    void shouldTimeoutCommandExactlyAtDeadline() {

        OffsetDateTime requestedAt =
                OffsetDateTime.parse(
                        "2026-09-15T20:00:00-03:00"
                );

        Command command =
                acceptedCommand(
                        "cmd_00000000000000000000000011",
                        requestedAt
                );

        InMemoryCommandRegistry registry =
                registered(command);

        CommandTimeoutWatchdog watchdog =
                watchdog(registry);

        int timedOut =
                watchdog.scan(
                        requestedAt.plusSeconds(5)
                );

        assertEquals(
                1,
                timedOut
        );

        assertEquals(
                CommandStatus.TIMEOUT,
                command.status()
        );
    }

    @Test
    void shouldTimeoutExecutingCommandAfterDeadline() {

        OffsetDateTime requestedAt =
                OffsetDateTime.parse(
                        "2026-09-15T20:00:00-03:00"
                );

        Command command =
                acknowledgedCommand(
                        "cmd_00000000000000000000000012",
                        requestedAt
                );

        command.startExecution(
                requestedAt.plusSeconds(2),
                requestedAt.plusSeconds(3)
        );

        InMemoryCommandRegistry registry =
                registered(command);

        CommandTimeoutWatchdog watchdog =
                watchdog(registry);

        int timedOut =
                watchdog.scan(
                        requestedAt.plusSeconds(6)
                );

        assertEquals(
                1,
                timedOut
        );

        assertEquals(
                CommandStatus.TIMEOUT,
                command.status()
        );
    }

    @Test
    void shouldIgnoreCompletedCommandAfterDeadline() {

        OffsetDateTime requestedAt =
                OffsetDateTime.parse(
                        "2026-09-15T20:00:00-03:00"
                );

        Command command =
                acknowledgedCommand(
                        "cmd_00000000000000000000000013",
                        requestedAt
                );

        command.startExecution(
                requestedAt.plusSeconds(2),
                requestedAt.plusSeconds(3)
        );

        command.complete(
                "adapter_message_complete",
                requestedAt.plusSeconds(3)
        );

        InMemoryCommandRegistry registry =
                registered(command);

        CommandTimeoutWatchdog watchdog =
                watchdog(registry);

        int timedOut =
                watchdog.scan(
                        requestedAt.plusSeconds(10)
                );

        assertEquals(
                0,
                timedOut
        );

        assertEquals(
                CommandStatus.COMPLETED,
                command.status()
        );
    }

    private CommandTimeoutWatchdog watchdog(
            InMemoryCommandRegistry registry
    ) {
        CommandExecutionService executionService =
                new CommandExecutionService(
                        registry
                );

        return new CommandTimeoutWatchdog(
                registry,
                executionService
        );
    }

    private InMemoryCommandRegistry registered(
            Command command
    ) {
        InMemoryCommandRegistry registry =
                new InMemoryCommandRegistry();

        registry.register(command);

        return registry;
    }

    private Command acceptedCommand(
            String id,
            OffsetDateTime requestedAt
    ) {
        Command command =
                newCommand(
                        id,
                        requestedAt
                );

        command.accept(
                requestedAt.plus(
                        Duration.ofMillis(10)
                )
        );

        return command;
    }

    private Command acknowledgedCommand(
            String id,
            OffsetDateTime requestedAt
    ) {
        Command command =
                acceptedCommand(
                        id,
                        requestedAt
                );

        command.markSent(
                requestedAt.plus(
                        Duration.ofMillis(20)
                )
        );

        command.acknowledge(
                requestedAt.plus(
                        Duration.ofMillis(30)
                )
        );

        return command;
    }

    private Command newCommand(
            String id,
            OffsetDateTime requestedAt
    ) {
        return new Command(
                new CommandId(id),
                new CommandActor(
                        CommandActorType.USER,
                        "user_test"
                ),
                new CommandOrigin(
                        CommandOriginType.API,
                        "api_test"
                ),
                new CommandTarget(
                        new DeviceId(
                                "dev_demo_spot_01"
                        ),
                        new ActionKey(
                                "turn_on"
                        )
                ),
                Map.of(),
                Criticality.COSMETIC,
                requestedAt,
                Duration.ofSeconds(5),
                new CommandIdempotencyKey(
                        "idem_" + id,
                        Duration.ofSeconds(10)
                ),
                new CommandCausality(
                        "trace_" + id,
                        null,
                        0
                )
        );
    }
}
