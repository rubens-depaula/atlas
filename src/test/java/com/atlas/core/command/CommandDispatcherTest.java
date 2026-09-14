package com.atlas.core.command;

import com.atlas.action.ActionDescriptor;
import com.atlas.action.ActionKey;
import com.atlas.adapter.AdapterCommand;
import com.atlas.adapter.AdapterDispatchReceipt;
import com.atlas.adapter.AdapterRegistry;
import com.atlas.adapter.DeviceAdapter;
import com.atlas.command.Command;
import com.atlas.command.CommandActor;
import com.atlas.command.CommandActorType;
import com.atlas.command.CommandCausality;
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
import com.atlas.core.device.InMemoryDeviceRegistry;
import com.atlas.environment.EnvironmentId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CommandDispatcherTest {

    @Test
    void shouldDispatchAcceptedCommandToDeviceAdapter() {

        OffsetDateTime now =
                OffsetDateTime.now();

        Device device =
                device(now);

        InMemoryDeviceRegistry deviceRegistry =
                new InMemoryDeviceRegistry();

        deviceRegistry.register(device);

        InMemoryCommandRegistry commandRegistry =
                new InMemoryCommandRegistry();

        Command command =
                new ActionCommandService().submit(
                        new CommandId(
                                "cmd_00000000000000000000000000"
                        ),
                        device,
                        new ActionKey("turn_on"),
                        Map.of(),
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

        commandRegistry.register(command);

        OffsetDateTime sentAt =
                now.plus(Duration.ofMillis(10));

        OffsetDateTime acknowledgedAt =
                now.plus(Duration.ofMillis(20));

        FakeAdapter fakeAdapter =
                new FakeAdapter(acknowledgedAt);

        AdapterRegistry adapterRegistry =
                instanceId -> {
                    if ("demo.adapter".equals(instanceId)) {
                        return Optional.of(fakeAdapter);
                    }

                    return Optional.empty();
                };

        CommandDispatcher dispatcher =
                new CommandDispatcher(
                        deviceRegistry,
                        commandRegistry,
                        adapterRegistry
                );

        dispatcher.dispatch(
                command,
                sentAt
        );

        assertEquals(
                CommandStatus.ACKNOWLEDGED,
                command.status()
        );

        assertEquals(
                sentAt,
                command.dispatchedAt()
        );

        assertEquals(
                4,
                command.statusHistory().size()
        );

        assertNotNull(
                fakeAdapter.lastCommand
        );

        assertEquals(
                command.id().value(),
                fakeAdapter.lastCommand.commandId()
        );

        assertEquals(
                "spot_bancada_01",
                fakeAdapter.lastCommand.nativeId()
        );

        assertEquals(
                "turn_on",
                fakeAdapter.lastCommand.action()
        );

        Command persisted =
                commandRegistry
                        .findById(command.id())
                        .orElseThrow();

        assertEquals(
                CommandStatus.ACKNOWLEDGED,
                persisted.status()
        );
    }
        @Test
    void shouldMarkUnknownOutcomeWhenAdapterFailsAfterDispatch() {

        OffsetDateTime now =
                OffsetDateTime.now();

        Device device =
                device(now);

        InMemoryDeviceRegistry deviceRegistry =
                new InMemoryDeviceRegistry();

        deviceRegistry.register(device);

        InMemoryCommandRegistry commandRegistry =
                new InMemoryCommandRegistry();

        Command command =
                new ActionCommandService().submit(
                        new CommandId(
                                "cmd_00000000000000000000000000"
                        ),
                        device,
                        new ActionKey("turn_on"),
                        Map.of(),
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

        commandRegistry.register(command);

        DeviceAdapter failingAdapter =
                new DeviceAdapter() {

                    @Override
                    public String instanceId() {
                        return "demo.adapter";
                    }

                    @Override
                    public AdapterDispatchReceipt dispatch(
                            AdapterCommand adapterCommand
                    ) {
                        throw new IllegalStateException(
                                "simulated adapter failure"
                        );
                    }
                };

        AdapterRegistry adapterRegistry =
                instanceId ->
                        "demo.adapter".equals(instanceId)
                                ? Optional.of(failingAdapter)
                                : Optional.empty();

        CommandDispatcher dispatcher =
                new CommandDispatcher(
                        deviceRegistry,
                        commandRegistry,
                        adapterRegistry
                );

        OffsetDateTime sentAt =
                now.plus(Duration.ofMillis(10));

        dispatcher.dispatch(
                command,
                sentAt
        );

        assertEquals(
                CommandStatus.UNKNOWN_OUTCOME,
                command.status()
        );

        assertNotNull(
                command.result()
        );
    }

    private Device device(
            OffsetDateTime now
    ) {

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
                List.of(turnOn),
                List.of(),
                null,
                now,
                now,
                null
        );
    }

    private static final class FakeAdapter
            implements DeviceAdapter {

        private final OffsetDateTime acknowledgedAt;

        private AdapterCommand lastCommand;

        private FakeAdapter(
                OffsetDateTime acknowledgedAt
        ) {
            this.acknowledgedAt = acknowledgedAt;
        }

        @Override
        public String instanceId() {
            return "demo.adapter";
        }

        @Override
        public AdapterDispatchReceipt dispatch(
                AdapterCommand command
        ) {
            this.lastCommand = command;

            return new AdapterDispatchReceipt(
                    "adapter_message_test",
                    acknowledgedAt
            );
        }
    }
}
