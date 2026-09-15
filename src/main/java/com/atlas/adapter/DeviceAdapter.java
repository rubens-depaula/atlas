package com.atlas.adapter;

public interface DeviceAdapter {

    String instanceId();

    AdapterDispatchReceipt dispatch(
            AdapterCommand command
    );

    default void afterAcknowledged(
            AdapterCommand command,
            AdapterDispatchReceipt receipt
    ) {
        // Optional lifecycle hook.
    }
}
