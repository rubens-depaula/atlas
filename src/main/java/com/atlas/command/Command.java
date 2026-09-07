package com.atlas.command;

import com.atlas.device.Criticality;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Command {

    private final CommandId id;
    private final CommandActor actor;
    private final CommandOrigin origin;
    private final CommandTarget target;
    private final Map<String, Object> parameters;
    private final Criticality criticality;

    private final OffsetDateTime requestedAt;
    private final Duration timeout;

    private final CommandIdempotencyKey idempotencyKey;
    private final CommandCausality causality;

    private final List<CommandStatusEntry> statusHistory =
            new ArrayList<>();

    private OffsetDateTime dispatchedAt;
    private OffsetDateTime executingSince;
    private OffsetDateTime expectedCompletionAt;
    private OffsetDateTime completedAt;

    private CommandResult result;

    public Command(
            CommandId id,
            CommandActor actor,
            CommandOrigin origin,
            CommandTarget target,
            Map<String, Object> parameters,
            Criticality criticality,
            OffsetDateTime requestedAt,
            Duration timeout,
            CommandIdempotencyKey idempotencyKey,
            CommandCausality causality
    ) {
        if (id == null) {
            throw new IllegalArgumentException(
                    "id cannot be null"
            );
        }

        if (actor == null) {
            throw new IllegalArgumentException(
                    "actor cannot be null"
            );
        }

        if (origin == null) {
            throw new IllegalArgumentException(
                    "origin cannot be null"
            );
        }

        if (target == null) {
            throw new IllegalArgumentException(
                    "target cannot be null"
            );
        }

        if (parameters == null) {
            throw new IllegalArgumentException(
                    "parameters cannot be null"
            );
        }

        if (criticality == null) {
            throw new IllegalArgumentException(
                    "criticality cannot be null"
            );
        }

        if (requestedAt == null) {
            throw new IllegalArgumentException(
                    "requestedAt cannot be null"
            );
        }

        if (timeout == null
                || timeout.isZero()
                || timeout.isNegative()) {
            throw new IllegalArgumentException(
                    "timeout must be greater than zero"
            );
        }

        if (idempotencyKey == null) {
            throw new IllegalArgumentException(
                    "idempotencyKey cannot be null"
            );
        }

        if (causality == null) {
            throw new IllegalArgumentException(
                    "causality cannot be null"
            );
        }

        if (actor.type() == CommandActorType.AUTOMATION
                && causality.cause() == null) {
            throw new IllegalArgumentException(
                    "AUTOMATION commands require a causality cause"
            );
        }

        this.id = id;
        this.actor = actor;
        this.origin = origin;
        this.target = target;

        this.parameters = Collections.unmodifiableMap(
                new LinkedHashMap<>(parameters)
        );

        this.criticality = criticality;
        this.requestedAt = requestedAt;
        this.timeout = timeout;
        this.idempotencyKey = idempotencyKey;
        this.causality = causality;

        statusHistory.add(
                new CommandStatusEntry(
                        CommandStatus.REQUESTED,
                        requestedAt,
                        null
                )
        );
    }

    public void accept(OffsetDateTime at) {
        requireStatus(CommandStatus.REQUESTED);

        transitionTo(
                CommandStatus.ACCEPTED,
                at,
                null
        );
    }

    public void reject(
            CommandErrorCode errorCode,
            String message,
            OffsetDateTime at
    ) {
        requireStatus(CommandStatus.REQUESTED);

        if (errorCode == null) {
            throw new IllegalArgumentException(
                    "errorCode cannot be null"
            );
        }

        transitionTo(
                CommandStatus.REJECTED,
                at,
                message
        );

        this.completedAt = at;

        this.result = new CommandResult(
                CommandOutcome.FAILURE,
                errorCode,
                message,
                null,
                at
        );
    }

    private void transitionTo(
            CommandStatus nextStatus,
            OffsetDateTime at,
            String detail
    ) {
        if (at == null) {
            throw new IllegalArgumentException(
                    "transition time cannot be null"
            );
        }

        CommandStatusEntry last =
                statusHistory.getLast();

        if (at.isBefore(last.at())) {
            throw new IllegalArgumentException(
                    "transition time cannot be before current status time"
            );
        }

        if (last.status().isTerminal()) {
            throw new IllegalStateException(
                    "terminal command cannot change status"
            );
        }

        statusHistory.add(
                new CommandStatusEntry(
                        nextStatus,
                        at,
                        detail
                )
        );
    }

    private void requireStatus(
            CommandStatus expected
    ) {
        if (status() != expected) {
            throw new IllegalStateException(
                    "expected status "
                            + expected
                            + " but was "
                            + status()
            );
        }
    }

    public CommandStatus status() {
        return statusHistory
                .getLast()
                .status();
    }

    public CommandId id() {
        return id;
    }

    public CommandActor actor() {
        return actor;
    }

    public CommandOrigin origin() {
        return origin;
    }

    public CommandTarget target() {
        return target;
    }

    public Map<String, Object> parameters() {
        return parameters;
    }

    public Criticality criticality() {
        return criticality;
    }

    public List<CommandStatusEntry> statusHistory() {
        return List.copyOf(statusHistory);
    }

    public OffsetDateTime requestedAt() {
        return requestedAt;
    }

    public OffsetDateTime dispatchedAt() {
        return dispatchedAt;
    }

    public OffsetDateTime executingSince() {
        return executingSince;
    }

    public OffsetDateTime expectedCompletionAt() {
        return expectedCompletionAt;
    }

    public OffsetDateTime completedAt() {
        return completedAt;
    }

    public Duration timeout() {
        return timeout;
    }

    public CommandIdempotencyKey idempotencyKey() {
        return idempotencyKey;
    }

    public CommandCausality causality() {
        return causality;
    }

    public CommandResult result() {
        return result;
    }
}