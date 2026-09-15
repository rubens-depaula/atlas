package com.atlas.core.command;

import com.atlas.command.Command;
import com.atlas.command.CommandId;
import com.atlas.command.CommandRegistry;

import java.time.OffsetDateTime;

public final class CommandExecutionService {

    private final CommandRegistry commandRegistry;

    public CommandExecutionService(
            CommandRegistry commandRegistry
    ) {
        if (commandRegistry == null) {
            throw new IllegalArgumentException(
                    "commandRegistry cannot be null"
            );
        }

        this.commandRegistry = commandRegistry;
    }

    public void startExecution(
            CommandId commandId,
            OffsetDateTime startedAt,
            OffsetDateTime expectedCompletionAt
    ) {
        if (commandId == null) {
            throw new IllegalArgumentException(
                    "commandId cannot be null"
            );
        }

        if (startedAt == null) {
            throw new IllegalArgumentException(
                    "startedAt cannot be null"
            );
        }

        Command command =
                commandRegistry
                        .findById(commandId)
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "command not found: "
                                                + commandId.value()
                                )
                        );

        command.startExecution(
                startedAt,
                expectedCompletionAt
        );

        commandRegistry.save(command);
    }
}
