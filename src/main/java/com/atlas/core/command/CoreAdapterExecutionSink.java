package com.atlas.core.command;

import com.atlas.adapter.AdapterExecutionSink;
import com.atlas.command.CommandId;

import java.time.OffsetDateTime;

public final class CoreAdapterExecutionSink
        implements AdapterExecutionSink {

    private final CommandExecutionService executionService;

    public CoreAdapterExecutionSink(
            CommandExecutionService executionService
    ) {
        if (executionService == null) {
            throw new IllegalArgumentException(
                    "executionService cannot be null"
            );
        }

        this.executionService = executionService;
    }

    @Override
    public void executionStarted(
            String commandId,
            OffsetDateTime startedAt,
            OffsetDateTime expectedCompletionAt
    ) {
        executionService.startExecution(
                new CommandId(commandId),
                startedAt,
                expectedCompletionAt
        );
    }

    @Override
    public void executionCompleted(
            String commandId,
            String adapterMessageId,
            OffsetDateTime completedAt
    ) {
        executionService.complete(
                new CommandId(commandId),
                adapterMessageId,
                completedAt
        );
    }

    @Override
    public void executionConfirmed(
            String commandId,
            String adapterMessageId,
            OffsetDateTime confirmedAt
    ) {
        executionService.confirm(
                new CommandId(commandId),
                adapterMessageId,
                confirmedAt
        );
    }
}
