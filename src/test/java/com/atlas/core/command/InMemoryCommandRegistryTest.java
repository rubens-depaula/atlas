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
import com.atlas.command.CommandTarget;
import com.atlas.device.Criticality;
import com.atlas.device.DeviceId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryCommandRegistryTest {

    @Test
    void shouldRegisterAndFindCommand() {

        InMemoryCommandRegistry registry =
                new InMemoryCommandRegistry();

        Command command =
                command(
                        "cmd_00000000000000000000000000"
                );

        registry.register(command);

        assertTrue(
                registry
                        .findById(command.id())
                        .isPresent()
        );

        assertEquals(
                command,
                registry
                        .findById(command.id())
                        .orElseThrow()
        );
    }

    @Test
    void shouldRejectDuplicateCommandId() {

        InMemoryCommandRegistry registry =
                new InMemoryCommandRegistry();

        Command first =
                command(
                        "cmd_00000000000000000000000000"
                );

        Command duplicate =
                command(
                        "cmd_00000000000000000000000000"
                );

        registry.register(first);

        assertThrows(
                IllegalArgumentException.class,
                () -> registry.register(duplicate)
        );
    }

    @Test
    void shouldReturnAllCommands() {

        InMemoryCommandRegistry registry =
                new InMemoryCommandRegistry();

        registry.register(
                command(
                        "cmd_00000000000000000000000000"
                )
        );

        registry.register(
                command(
                        "cmd_00000000000000000000000001"
                )
        );

        assertEquals(
                2,
                registry.findAll().size()
        );
    }

    private Command command(
            String id
    ) {

        OffsetDateTime now =
                OffsetDateTime.now();

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
                        "idem_test_" + id,
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