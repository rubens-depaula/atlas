package com.atlas.core.command;

import com.atlas.adapter.AdapterCommand;
import com.atlas.adapter.AdapterDispatchReceipt;
import com.atlas.adapter.AdapterRegistry;
import com.atlas.adapter.DeviceAdapter;
import com.atlas.command.Command;
import com.atlas.command.CommandErrorCode;
import com.atlas.command.CommandRegistry;
import com.atlas.device.Device;
import com.atlas.device.DeviceRegistry;
import com.atlas.device.DeviceSource;

import java.time.OffsetDateTime;

public final class CommandDispatcher {

    private final DeviceRegistry deviceRegistry;
    private final CommandRegistry commandRegistry;
    private final AdapterRegistry adapterRegistry;

    public CommandDispatcher(
            DeviceRegistry deviceRegistry,
            CommandRegistry commandRegistry,
            AdapterRegistry adapterRegistry
    ) {
        if (deviceRegistry == null) {
            throw new IllegalArgumentException(
                    "deviceRegistry cannot be null"
            );
        }

        if (commandRegistry == null) {
            throw new IllegalArgumentException(
                    "commandRegistry cannot be null"
            );
        }

        if (adapterRegistry == null) {
            throw new IllegalArgumentException(
                    "adapterRegistry cannot be null"
            );
        }

        this.deviceRegistry = deviceRegistry;
        this.commandRegistry = commandRegistry;
        this.adapterRegistry = adapterRegistry;
    }

    public void dispatch(
            Command command,
            OffsetDateTime sentAt
    ) {
        if (command == null) {
            throw new IllegalArgumentException(
                    "command cannot be null"
            );
        }

        if (sentAt == null) {
            throw new IllegalArgumentException(
                    "sentAt cannot be null"
            );
        }

        Device device =
                deviceRegistry
                        .findById(
                                command.target().deviceId()
                        )
                        .orElse(null);

        if (device == null) {
            command.fail(
                    CommandErrorCode.DEVICE_UNREACHABLE,
                    "device not available in registry",
                    null,
                    sentAt
            );

            commandRegistry.save(command);
            return;
        }

        DeviceSource source =
                device.source();

        DeviceAdapter adapter =
                adapterRegistry
                        .findByInstanceId(
                                source.adapterInstanceId()
                        )
                        .orElse(null);

        if (adapter == null) {
            command.fail(
                    CommandErrorCode.ADAPTER_UNAVAILABLE,
                    "adapter unavailable: "
                            + source.adapterInstanceId(),
                    null,
                    sentAt
            );

            commandRegistry.save(command);
            return;
        }

        command.markSent(sentAt);
        commandRegistry.save(command);

        AdapterCommand adapterCommand =
                new AdapterCommand(
                        command.id().value(),
                        source.nativeId(),
                        command.target()
                                .action()
                                .value(),
                        command.parameters()
                );

        AdapterDispatchReceipt receipt;

        try {
            receipt =
                    adapter.dispatch(adapterCommand);

        } catch (RuntimeException exception) {

            command.markUnknownOutcome(
                    "adapter dispatch failed: "
                            + exception.getMessage(),
                    null,
                    sentAt
            );

            commandRegistry.save(command);
            return;
        }

        command.acknowledge(
                receipt.acknowledgedAt()
        );

        commandRegistry.save(command);
    }
}
