package com.atlas.adapter;

import java.time.OffsetDateTime;

public interface AdapterExecutionSink {

    void executionStarted(
            String commandId,
            OffsetDateTime startedAt,
            OffsetDateTime expectedCompletionAt
    );

    void executionCompleted(
            String commandId,
            String adapterMessageId,
            OffsetDateTime completedAt
    );

    void executionConfirmed(
            String commandId,
            String adapterMessageId,
            OffsetDateTime confirmedAt
    );
}
