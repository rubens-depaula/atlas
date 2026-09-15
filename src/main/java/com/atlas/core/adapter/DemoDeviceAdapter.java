package com.atlas.core.adapter;

import com.atlas.adapter.AdapterCommand;
import com.atlas.adapter.AdapterDispatchReceipt;
import com.atlas.adapter.DeviceAdapter;

import java.time.OffsetDateTime;

public final class DemoDeviceAdapter
        implements DeviceAdapter {

    public static final String INSTANCE_ID =
            "demo.adapter";

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
}
