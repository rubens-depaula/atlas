package com.atlas.adapter;

public interface DeviceAdapter {

    String instanceId();

    AdapterDispatchReceipt dispatch(
            AdapterCommand command
    );
}
