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
        Command command =
                requireCommand(commandId);

        if (startedAt == null) {
            throw new IllegalArgumentException(
                    "startedAt cannot be null"
            );
        }

        command.startExecution(
                startedAt,
                expectedCompletionAt
        );

        commandRegistry.save(command);
    }

    public void complete(
            CommandId commandId,
            String adapterMessageId,
            OffsetDateTime completedAt
    ) {
        Command command =
                requireCommand(commandId);

        if (completedAt == null) {
            throw new IllegalArgumentException(
                    "completedAt cannot be null"
            );
        }

        command.complete(
                adapterMessageId,
                completedAt
        );

        commandRegistry.save(command);
    }

    public void confirm(
            CommandId commandId,
            String adapterMessageId,
            OffsetDateTime confirmedAt
    ) {
        Command command =
                requireCommand(commandId);

        if (confirmedAt == null) {
            throw new IllegalArgumentException(
                    "confirmedAt cannot be null"
            );
        }

        command.confirm(
                adapterMessageId,
                confirmedAt
        );

        commandRegistry.save(command);
    }

    public void timeout(
            CommandId commandId,
            String message,
            OffsetDateTime timedOutAt
    ) {
        Command command =
                requireCommand(commandId);

        if (timedOutAt == null) {
            throw new IllegalArgumentException(
                    "timedOutAt cannot be null"
            );
        }

        command.timeout(
                message,
                timedOutAt
        );

        commandRegistry.save(command);
    }

    public void markUnknownOutcome(
            CommandId commandId,
            String message,
            String adapterMessageId,
            OffsetDateTime at
    ) {
        Command command =
                requireCommand(commandId);

        if (at == null) {
            throw new IllegalArgumentException(
                    "at cannot be null"
            );
        }

        command.markUnknownOutcome(
                message,
                adapterMessageId,
                at
        );

        commandRegistry.save(command);
    }

    private Command requireCommand(
            CommandId commandId
    ) {
        if (commandId == null) {
            throw new IllegalArgumentException(
                    "commandId cannot be null"
            );
        }

        return commandRegistry
                .findById(commandId)
                .orElseThrow(
                        () -> new IllegalStateException(
                                "command not found: "
                                        + commandId.value()
                        )
                );
    }
}
