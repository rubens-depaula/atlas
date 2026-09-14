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

    public void markSent(
            OffsetDateTime at
    ) {
        requireStatus(CommandStatus.ACCEPTED);

        transitionTo(
                CommandStatus.SENT,
                at,
                null
        );

        this.dispatchedAt = at;
    }

    public void acknowledge(
            OffsetDateTime at
    ) {
        requireStatus(CommandStatus.SENT);

        transitionTo(
                CommandStatus.ACKNOWLEDGED,
                at,
                null
        );
    }

    public void startExecution(
            OffsetDateTime at,
            OffsetDateTime expectedCompletionAt
    ) {
        requireStatus(
                CommandStatus.ACKNOWLEDGED
        );

        if (expectedCompletionAt != null
                && expectedCompletionAt.isBefore(at)) {
            throw new IllegalArgumentException(
                    "expectedCompletionAt cannot be before execution start"
            );
        }

        transitionTo(
                CommandStatus.EXECUTING,
                at,
                null
        );

        this.executingSince = at;
        this.expectedCompletionAt =
                expectedCompletionAt;
    }

    public void confirm(
            String adapterMessageId,
            OffsetDateTime at
    ) {
        requireStatus(
                CommandStatus.EXECUTING
        );

        transitionTo(
                CommandStatus.CONFIRMED,
                at,
                null
        );

        this.completedAt = at;

        this.result = new CommandResult(
                CommandOutcome.SUCCESS,
                null,
                null,
                adapterMessageId,
                at
        );
    }

    public void complete(
            String adapterMessageId,
            OffsetDateTime at
    ) {
        requireStatus(
                CommandStatus.EXECUTING
        );

        transitionTo(
                CommandStatus.COMPLETED,
                at,
                null
        );

        this.completedAt = at;

        this.result = new CommandResult(
                CommandOutcome.SUCCESS,
                null,
                null,
                adapterMessageId,
                at
        );
    }

    public void fail(
            CommandErrorCode errorCode,
            String message,
            String adapterMessageId,
            OffsetDateTime at
    ) {
        requireStatusOneOf(
                CommandStatus.ACCEPTED,
                CommandStatus.SENT,
                CommandStatus.ACKNOWLEDGED,
                CommandStatus.EXECUTING
        );

        if (errorCode == null) {
            throw new IllegalArgumentException(
                    "errorCode cannot be null"
            );
        }

        transitionTo(
                CommandStatus.FAILED,
                at,
                message
        );

        this.completedAt = at;

        this.result = new CommandResult(
                CommandOutcome.FAILURE,
                errorCode,
                message,
                adapterMessageId,
                at
        );
    }

    public void timeout(
            String message,
            OffsetDateTime at
    ) {
        requireStatusOneOf(
                CommandStatus.ACCEPTED,
                CommandStatus.SENT,
                CommandStatus.ACKNOWLEDGED,
                CommandStatus.EXECUTING
        );

        transitionTo(
                CommandStatus.TIMEOUT,
                at,
                message
        );

        this.completedAt = at;

        this.result = new CommandResult(
                CommandOutcome.UNKNOWN,
                null,
                message,
                null,
                at
        );
    }

    public void markUnknownOutcome(
            String message,
            String adapterMessageId,
            OffsetDateTime at
    ) {
        requireStatusOneOf(
                CommandStatus.SENT,
                CommandStatus.ACKNOWLEDGED,
                CommandStatus.EXECUTING
        );

        transitionTo(
                CommandStatus.UNKNOWN_OUTCOME,
                at,
                message
        );

        this.completedAt = at;

        this.result = new CommandResult(
                CommandOutcome.UNKNOWN,
                null,
                message,
                adapterMessageId,
                at
        );
    }
    public void cancel(
            String message,
            OffsetDateTime at
    ) {
        requireStatusOneOf(
                CommandStatus.REQUESTED,
                CommandStatus.ACCEPTED
        );

        transitionTo(
                CommandStatus.CANCELLED,
                at,
                message
        );

        this.completedAt = at;

        this.result = new CommandResult(
                CommandOutcome.CANCELLED,
                null,
                message,
                null,
                at
        );
    }

    public void expire(
            String message,
            OffsetDateTime at
    ) {
        requireStatusOneOf(
                CommandStatus.REQUESTED,
                CommandStatus.ACCEPTED
        );

        transitionTo(
                CommandStatus.EXPIRED,
                at,
                message
        );

        this.completedAt = at;

        this.result = new CommandResult(
                CommandOutcome.CANCELLED,
                null,
                message,
                null,
                at
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
    private void requireStatusOneOf(
            CommandStatus... expectedStatuses
    ) {
        CommandStatus current =
                status();

        for (CommandStatus expected : expectedStatuses) {
            if (current == expected) {
                return;
            }
        }

        throw new IllegalStateException(
                "status "
                        + current
                        + " is not allowed for this transition"
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
    public static Command rehydrate(
            CommandId id,
            CommandActor actor,
            CommandOrigin origin,
            CommandTarget target,
            Map<String, Object> parameters,
            Criticality criticality,
            OffsetDateTime requestedAt,
            Duration timeout,
            CommandIdempotencyKey idempotencyKey,
            CommandCausality causality,
            List<CommandStatusEntry> statusHistory,
            OffsetDateTime dispatchedAt,
            OffsetDateTime executingSince,
            OffsetDateTime expectedCompletionAt,
            OffsetDateTime completedAt,
            CommandResult result
    ) {
        if (statusHistory == null || statusHistory.isEmpty()) {
            throw new IllegalArgumentException(
                    "statusHistory cannot be null or empty"
            );
        }

        if (statusHistory.getFirst().status()
                != CommandStatus.REQUESTED) {
            throw new IllegalArgumentException(
                    "statusHistory must start with REQUESTED"
            );
        }

        OffsetDateTime previousAt = null;
        boolean terminalSeen = false;

        for (CommandStatusEntry entry : statusHistory) {
            if (entry == null) {
                throw new IllegalArgumentException(
                        "statusHistory cannot contain null entries"
                );
            }

            if (previousAt != null
                    && entry.at().isBefore(previousAt)) {
                throw new IllegalArgumentException(
                        "statusHistory must be chronological"
                );
            }

            if (terminalSeen) {
                throw new IllegalArgumentException(
                        "statusHistory cannot continue after a terminal status"
                );
            }

            terminalSeen = entry.status().isTerminal();
            previousAt = entry.at();
        }

        Command command = new Command(
                id,
                actor,
                origin,
                target,
                parameters,
                criticality,
                requestedAt,
                timeout,
                idempotencyKey,
                causality
        );

        command.statusHistory.clear();
        command.statusHistory.addAll(statusHistory);

        command.dispatchedAt = dispatchedAt;
        command.executingSince = executingSince;
        command.expectedCompletionAt = expectedCompletionAt;
        command.completedAt = completedAt;
        command.result = result;

        return command;
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
