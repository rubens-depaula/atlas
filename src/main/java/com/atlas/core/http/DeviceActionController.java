package com.atlas.core.http;

import com.atlas.action.ActionKey;
import com.atlas.command.Command;
import com.atlas.command.CommandActor;
import com.atlas.command.CommandActorType;
import com.atlas.command.CommandCausality;
import com.atlas.command.CommandId;
import com.atlas.command.CommandIdempotencyKey;
import com.atlas.command.CommandOrigin;
import com.atlas.command.CommandOriginType;
import com.atlas.command.CommandResult;
import com.atlas.command.CommandStatus;
import com.atlas.core.command.ActionCommandService;
import com.atlas.core.command.CommandIdGenerator;
import com.atlas.device.Device;
import com.atlas.device.DeviceId;
import com.atlas.device.DeviceRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/devices")
public class DeviceActionController {

    private final DeviceRegistry deviceRegistry;
    private final ActionCommandService commandService;
    private final CommandIdGenerator commandIdGenerator;

    public DeviceActionController(
            DeviceRegistry deviceRegistry,
            ActionCommandService commandService,
            CommandIdGenerator commandIdGenerator
    ) {
        this.deviceRegistry = deviceRegistry;
        this.commandService = commandService;
        this.commandIdGenerator = commandIdGenerator;
    }

    @PostMapping("/{deviceId}/actions/{action}")
    public ResponseEntity<CommandResponse> submit(
            @PathVariable String deviceId,
            @PathVariable String action,
            @RequestBody(required = false) ActionRequest request,
            @RequestHeader(
                    value = "Idempotency-Key",
                    required = false
            ) String idempotencyHeader
    ) {
        Device device =
                deviceRegistry
                        .findById(
                                new DeviceId(deviceId)
                        )
                        .orElse(null);

        if (device == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> parameters =
                request != null
                        && request.parameters() != null
                        ? request.parameters()
                        : Map.of();

        OffsetDateTime now =
                OffsetDateTime.now();

        CommandId commandId =
                commandIdGenerator.next();

        String idempotencyValue =
                idempotencyHeader != null
                        && !idempotencyHeader.isBlank()
                        ? idempotencyHeader
                        : commandId.value();

        Command command =
                commandService.submit(
                        commandId,
                        device,
                        new ActionKey(action),
                        parameters,

                        new CommandActor(
                                CommandActorType.USER,
                                null
                        ),

                        new CommandOrigin(
                                CommandOriginType.API,
                                "http"
                        ),

                        new CommandIdempotencyKey(
                                idempotencyValue,
                                Duration.ofSeconds(10)
                        ),

                        new CommandCausality(
                                "trace_"
                                        + commandId.value()
                                        .substring(4),
                                null,
                                0
                        ),

                        Duration.ofSeconds(5),
                        now
                );

        CommandResponse response =
                CommandResponse.from(command);

        if (command.status()
                == CommandStatus.ACCEPTED) {

            return ResponseEntity
                    .status(HttpStatus.ACCEPTED)
                    .body(response);
        }

        return ResponseEntity
                .status(
                        HttpStatus.UNPROCESSABLE_ENTITY
                )
                .body(response);
    }

    public record ActionRequest(
            Map<String, Object> parameters
    ) {
    }

    public record CommandResponse(
            String id,
            String status,
            String deviceId,
            String action,
            String criticality,
            ResultResponse result
    ) {

        public static CommandResponse from(
                Command command
        ) {
            return new CommandResponse(
                    command.id().value(),
                    command.status().name(),
                    command.target()
                            .deviceId()
                            .value(),
                    command.target()
                            .action()
                            .value(),
                    command.criticality().name(),
                    ResultResponse.from(
                            command.result()
                    )
            );
        }
    }

    public record ResultResponse(
            String outcome,
            String errorCode,
            String message
    ) {

        public static ResultResponse from(
                CommandResult result
        ) {
            if (result == null) {
                return null;
            }

            return new ResultResponse(
                    result.outcome().name(),

                    result.errorCode() != null
                            ? result.errorCode().name()
                            : null,

                    result.errorMessage()
            );
        }
    }
}