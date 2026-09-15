package com.atlas.core.adapter;

import com.atlas.adapter.AdapterCommand;
import com.atlas.adapter.AdapterDispatchReceipt;
import com.atlas.adapter.AdapterExecutionSink;
import com.atlas.adapter.DeviceAdapter;

import java.time.OffsetDateTime;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class DemoDeviceAdapter
        implements DeviceAdapter {

    public static final String INSTANCE_ID =
            "demo.adapter";

    private final AdapterExecutionSink executionSink;
    private final ScheduledExecutorService executor;

    public DemoDeviceAdapter(
            AdapterExecutionSink executionSink,
            ScheduledExecutorService executor
    ) {
        if (executionSink == null) {
            throw new IllegalArgumentException(
                    "executionSink cannot be null"
            );
        }

        if (executor == null) {
            throw new IllegalArgumentException(
                    "executor cannot be null"
            );
        }

        this.executionSink = executionSink;
        this.executor = executor;
    }

    @Override
    public String instanceId() {
        return INSTANCE_ID;
    }

    @Override
    public AdapterDispatchReceipt dispatch(
            AdapterCommand command
    ) {
        if (command == null) {
            throw new IllegalArgumentException(
                    "command cannot be null"
            );
        }

        return new AdapterDispatchReceipt(
                "demo_message_"
                        + command.commandId(),
                OffsetDateTime.now()
        );
    }

    @Override
    public void afterAcknowledged(
            AdapterCommand command,
            AdapterDispatchReceipt receipt
    ) {
        if (command == null) {
            throw new IllegalArgumentException(
                    "command cannot be null"
            );
        }

        if (receipt == null) {
            throw new IllegalArgumentException(
                    "receipt cannot be null"
            );
        }

        if ("simulate_timeout".equals(
                command.action()
        )) {
            return;
        }

        executor.schedule(
                () -> startExecution(
                        command,
                        receipt
                ),
                500,
                TimeUnit.MILLISECONDS
        );
    }

    private void startExecution(
            AdapterCommand command,
            AdapterDispatchReceipt receipt
    ) {
        OffsetDateTime startedAt =
                OffsetDateTime.now();

        executionSink.executionStarted(
                command.commandId(),
                startedAt,
                startedAt.plusSeconds(2)
        );

        executor.schedule(
                () -> completeExecution(
                        command,
                        receipt
                ),
                1,
                TimeUnit.SECONDS
        );
    }

    private void completeExecution(
            AdapterCommand command,
            AdapterDispatchReceipt receipt
    ) {
        executionSink.executionCompleted(
                command.commandId(),
                receipt.adapterMessageId(),
                OffsetDateTime.now()
        );
    }
}
