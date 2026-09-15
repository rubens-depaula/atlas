package com.atlas.core.command;

import com.atlas.command.Command;
import com.atlas.command.CommandRegistry;
import com.atlas.command.CommandStatus;

import java.time.OffsetDateTime;

public final class CommandTimeoutWatchdog {

    private final CommandRegistry commandRegistry;
    private final CommandExecutionService commandExecutionService;

    public CommandTimeoutWatchdog(
            CommandRegistry commandRegistry,
            CommandExecutionService commandExecutionService
    ) {
        if (commandRegistry == null) {
            throw new IllegalArgumentException(
                    "commandRegistry cannot be null"
            );
        }

        if (commandExecutionService == null) {
            throw new IllegalArgumentException(
                    "commandExecutionService cannot be null"
            );
        }

        this.commandRegistry = commandRegistry;
        this.commandExecutionService =
                commandExecutionService;
    }

    public int scan(
            OffsetDateTime now
    ) {
        if (now == null) {
            throw new IllegalArgumentException(
                    "now cannot be null"
            );
        }

        int timedOut = 0;

        for (Command command : commandRegistry.findAll()) {

            if (!isTimeoutEligible(command)) {
                continue;
            }

            OffsetDateTime deadline =
                    command.requestedAt()
                            .plus(command.timeout());

            if (now.isBefore(deadline)) {
                continue;
            }

            commandExecutionService.timeout(
                    command.id(),
                    "command timeout exceeded",
                    now
            );

            timedOut++;
        }

        return timedOut;
    }

    private boolean isTimeoutEligible(
            Command command
    ) {
        if (command.status().isTerminal()) {
            return false;
        }

        return switch (command.status()) {
            case ACCEPTED,
                 SENT,
                 ACKNOWLEDGED,
                 EXECUTING -> true;

            default -> false;
        };
    }
}
