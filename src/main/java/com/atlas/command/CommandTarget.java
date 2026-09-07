package com.atlas.command;

import com.atlas.action.ActionKey;
import com.atlas.device.DeviceId;

public record CommandTarget(
        DeviceId deviceId,
        ActionKey action
) {

    public CommandTarget {
        if (deviceId == null) {
            throw new IllegalArgumentException(
                    "deviceId cannot be null"
            );
        }

        if (action == null) {
            throw new IllegalArgumentException(
                    "action cannot be null"
            );
        }
    }
}