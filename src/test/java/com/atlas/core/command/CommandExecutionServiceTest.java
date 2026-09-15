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

class CommandExecutionServiceTest {

    @Test
    void shouldStartAcknowledgedCommandExecution() {

        OffsetDateTime now =
                OffsetDateTime.now();

        Command command =
                new Command(
                        new CommandId(
                                "cmd_00000000000000000000000000"
                        ),
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
                                "idem_test",
                                Duration.ofSeconds(10)
                        ),
                        new CommandCausality(
                                "trace_test",
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

        InMemoryCommandRegistry commandRegistry =
                new InMemoryCommandRegistry();

        commandRegistry.register(command);

        CommandExecutionService service =
                new CommandExecutionService(
                        commandRegistry
                );

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

        Command persisted =
                commandRegistry
                        .findById(command.id())
                        .orElseThrow();

        assertEquals(
                CommandStatus.EXECUTING,
                persisted.status()
        );
    }
}
