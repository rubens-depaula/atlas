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
import com.atlas.command.CommandOutcome;
import com.atlas.command.CommandStatus;
import com.atlas.command.CommandTarget;
import com.atlas.device.Criticality;
import com.atlas.device.DeviceId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CommandExecutionServiceTest {

    @Test
    void shouldStartAcknowledgedCommandExecution() {

        OffsetDateTime now =
                OffsetDateTime.now();

        Command command =
                acknowledgedCommand(
                        "cmd_00000000000000000000000000",
                        now
                );

        InMemoryCommandRegistry registry =
                registered(command);

        CommandExecutionService service =
                new CommandExecutionService(registry);

        OffsetDateTime startedAt =
                now.plus(Duration.ofMillis(40));

        OffsetDateTime expectedCompletionAt =
                startedAt.plus(Duration.ofSeconds(2));

        service.startExecution(
                command.id(),
                startedAt,
                expectedCompletionAt
        );

        assertEquals(
                CommandStatus.EXECUTING,
                command.status()
        );

        assertEquals(
                startedAt,
                command.executingSince()
        );

        assertEquals(
                expectedCompletionAt,
                command.expectedCompletionAt()
        );

        assertEquals(
                CommandStatus.EXECUTING,
                registry.findById(command.id())
                        .orElseThrow()
                        .status()
        );
    }

    @Test
    void shouldCompleteExecutingCommand() {

        OffsetDateTime now =
                OffsetDateTime.now();

        Command command =
                acknowledgedCommand(
                        "cmd_00000000000000000000000001",
                        now
                );

        InMemoryCommandRegistry registry =
                registered(command);

        CommandExecutionService service =
                new CommandExecutionService(registry);

        OffsetDateTime startedAt =
                now.plus(Duration.ofMillis(40));

        service.startExecution(
                command.id(),
                startedAt,
                startedAt.plus(Duration.ofSeconds(2))
        );

        OffsetDateTime completedAt =
                now.plus(Duration.ofMillis(50));

        service.complete(
                command.id(),
                "adapter_message_complete",
                completedAt
        );

        assertEquals(
                CommandStatus.COMPLETED,
                command.status()
        );

        assertEquals(
                completedAt,
                command.completedAt()
        );

        assertNotNull(command.result());

        assertEquals(
                CommandOutcome.SUCCESS,
                command.result().outcome()
        );

        assertEquals(
                "adapter_message_complete",
                command.result().adapterMessageId()
        );

        assertEquals(
                CommandStatus.COMPLETED,
                registry.findById(command.id())
                        .orElseThrow()
                        .status()
        );
    }

    @Test
    void shouldConfirmExecutingCommand() {

        OffsetDateTime now =
                OffsetDateTime.now();

        Command command =
                acknowledgedCommand(
                        "cmd_00000000000000000000000002",
                        now
                );

        InMemoryCommandRegistry registry =
                registered(command);

        CommandExecutionService service =
                new CommandExecutionService(registry);

        OffsetDateTime startedAt =
                now.plus(Duration.ofMillis(40));

        service.startExecution(
                command.id(),
                startedAt,
                startedAt.plus(Duration.ofSeconds(2))
        );

        OffsetDateTime confirmedAt =
                now.plus(Duration.ofMillis(50));

        service.confirm(
                command.id(),
                "adapter_message_confirm",
                confirmedAt
        );

        assertEquals(
                CommandStatus.CONFIRMED,
                command.status()
        );

        assertEquals(
                confirmedAt,
                command.completedAt()
        );

        assertNotNull(command.result());

        assertEquals(
                CommandOutcome.SUCCESS,
                command.result().outcome()
        );

        assertEquals(
                "adapter_message_confirm",
                command.result().adapterMessageId()
        );

        assertEquals(
                CommandStatus.CONFIRMED,
                registry.findById(command.id())
                        .orElseThrow()
                        .status()
        );
    }

    @Test
    void shouldTimeoutAcknowledgedCommand() {

        OffsetDateTime now =
                OffsetDateTime.now();

        Command command =
                acknowledgedCommand(
                        "cmd_00000000000000000000000003",
                        now
                );

        InMemoryCommandRegistry registry =
                registered(command);

        CommandExecutionService service =
                new CommandExecutionService(registry);

        OffsetDateTime timedOutAt =
                now.plus(Duration.ofSeconds(5));

        service.timeout(
                command.id(),
                "command execution timed out",
                timedOutAt
        );

        assertEquals(
                CommandStatus.TIMEOUT,
                command.status()
        );

        assertEquals(
                timedOutAt,
                command.completedAt()
        );

        assertNotNull(command.result());

        assertEquals(
                CommandOutcome.UNKNOWN,
                command.result().outcome()
        );

        assertEquals(
                CommandStatus.TIMEOUT,
                registry.findById(command.id())
                        .orElseThrow()
                        .status()
        );
    }

    @Test
    void shouldMarkAcknowledgedCommandAsUnknownOutcome() {

        OffsetDateTime now =
                OffsetDateTime.now();

        Command command =
                acknowledgedCommand(
                        "cmd_00000000000000000000000004",
                        now
                );

        InMemoryCommandRegistry registry =
                registered(command);

        CommandExecutionService service =
                new CommandExecutionService(registry);

        OffsetDateTime unknownAt =
                now.plus(Duration.ofMillis(40));

        service.markUnknownOutcome(
                command.id(),
                "execution outcome is unknown",
                "adapter_message_unknown",
                unknownAt
        );

        assertEquals(
                CommandStatus.UNKNOWN_OUTCOME,
                command.status()
        );

        assertEquals(
                unknownAt,
                command.completedAt()
        );

        assertNotNull(command.result());

        assertEquals(
                CommandOutcome.UNKNOWN,
                command.result().outcome()
        );

        assertEquals(
                "adapter_message_unknown",
                command.result().adapterMessageId()
        );

        assertEquals(
                CommandStatus.UNKNOWN_OUTCOME,
                registry.findById(command.id())
                        .orElseThrow()
                        .status()
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

    private Command acknowledgedCommand(
            String id,
            OffsetDateTime now
    ) {
        Command command =
                new Command(
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
                        now,
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

        command.accept(
                now.plus(Duration.ofMillis(10))
        );

        command.markSent(
                now.plus(Duration.ofMillis(20))
        );

        command.acknowledge(
                now.plus(Duration.ofMillis(30))
        );

        return command;
    }
}
