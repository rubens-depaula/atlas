package com.atlas.core.adapter;

import com.atlas.adapter.AdapterCommand;
import com.atlas.adapter.AdapterDispatchReceipt;
import com.atlas.adapter.AdapterExecutionSink;
import com.atlas.adapter.DeviceAdapter;

import java.time.OffsetDateTime;

public final class DemoDeviceAdapter
        implements DeviceAdapter {

    public static final String INSTANCE_ID =
            "demo.adapter";

    private final AdapterExecutionSink executionSink;

    public DemoDeviceAdapter(
            AdapterExecutionSink executionSink
    ) {
        if (executionSink == null) {
            throw new IllegalArgumentException(
                    "executionSink cannot be null"
            );
        }

        this.executionSink = executionSink;
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

        OffsetDateTime startedAt =
                OffsetDateTime.now();

        executionSink.executionStarted(
                command.commandId(),
                startedAt,
                startedAt.plusSeconds(1)
        );

        OffsetDateTime completedAt =
                OffsetDateTime.now();

        executionSink.executionCompleted(
                command.commandId(),
                receipt.adapterMessageId(),
                completedAt
        );
    }
}
