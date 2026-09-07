package com.atlas.core.http;

import com.atlas.command.Command;
import com.atlas.command.CommandId;
import com.atlas.command.CommandRegistry;
import com.atlas.command.CommandResult;
import com.atlas.command.CommandStatusEntry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/commands")
public class CommandController {

    private final CommandRegistry commandRegistry;

    public CommandController(
            CommandRegistry commandRegistry
    ) {
        this.commandRegistry = commandRegistry;
    }

    @GetMapping
    public List<CommandResponse> findAll() {
        return commandRegistry
                .findAll()
                .stream()
                .map(CommandResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommandResponse> findById(
            @PathVariable String id
    ) {
        try {
            return commandRegistry
                    .findById(new CommandId(id))
                    .map(CommandResponse::from)
                    .map(ResponseEntity::ok)
                    .orElseGet(
                            () -> ResponseEntity
                                    .notFound()
                                    .build()
                    );

        } catch (IllegalArgumentException exception) {
            return ResponseEntity
                    .badRequest()
                    .build();
        }
    }

    public record CommandResponse(
            String id,
            String status,
            String actorType,
            String actorId,
            String originType,
            String originId,
            String deviceId,
            String action,
            Map<String, Object> parameters,
            String criticality,
            OffsetDateTime requestedAt,
            OffsetDateTime completedAt,
            List<StatusEntryResponse> statusHistory,
            ResultResponse result
    ) {

        public static CommandResponse from(
                Command command
        ) {
            return new CommandResponse(
                    command.id().value(),
                    command.status().name(),

                    command.actor()
                            .type()
                            .name(),

                    command.actor().id(),

                    command.origin()
                            .type()
                            .name(),

                    command.origin().id(),

                    command.target()
                            .deviceId()
                            .value(),

                    command.target()
                            .action()
                            .value(),

                    command.parameters(),

                    command.criticality()
                            .name(),

                    command.requestedAt(),
                    command.completedAt(),

                    command.statusHistory()
                            .stream()
                            .map(StatusEntryResponse::from)
                            .toList(),

                    ResultResponse.from(
                            command.result()
                    )
            );
        }
    }

    public record StatusEntryResponse(
            String status,
            OffsetDateTime at,
            String detail
    ) {

        public static StatusEntryResponse from(
                CommandStatusEntry entry
        ) {
            return new StatusEntryResponse(
                    entry.status().name(),
                    entry.at(),
                    entry.detail()
            );
        }
    }

    public record ResultResponse(
            String outcome,
            String errorCode,
            String message,
            OffsetDateTime at
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

                    result.errorMessage(),
                    result.at()
            );
        }
    }
}