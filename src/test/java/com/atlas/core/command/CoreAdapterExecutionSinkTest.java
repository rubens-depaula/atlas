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

class CoreAdapterExecutionSinkTest {

    @Test
    void shouldRouteExecutionFactsToCommandLifecycle() {

        OffsetDateTime now =
                OffsetDateTime.now();

        Command command =
                new Command(
                        new CommandId(
                                "cmd_00000000000000000000000003"
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
                                "idem_sink_test",
                                Duration.ofSeconds(10)
                        ),
                        new CommandCausality(
                                "trace_sink_test",
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

        InMemoryCommandRegistry registry =
                new InMemoryCommandRegistry();

        registry.register(command);

        CommandExecutionService executionService =
                new CommandExecutionService(
                        registry
                );

        CoreAdapterExecutionSink sink =
                new CoreAdapterExecutionSink(
                        executionService
                );

        OffsetDateTime startedAt =
                now.plus(Duration.ofMillis(40));

        sink.executionStarted(
                command.id().value(),
                startedAt,
                startedAt.plus(Duration.ofSeconds(2))
        );

        assertEquals(
                CommandStatus.EXECUTING,
                command.status()
        );

        OffsetDateTime completedAt =
                now.plus(Duration.ofMillis(50));

        sink.executionCompleted(
                command.id().value(),
                "demo_execution_message",
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
                "demo_execution_message",
                command.result().adapterMessageId()
        );

        Command persisted =
                registry
                        .findById(command.id())
                        .orElseThrow();

        assertEquals(
                CommandStatus.COMPLETED,
                persisted.status()
        );
    }
}
