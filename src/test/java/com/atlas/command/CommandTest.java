package com.atlas.command;

import java.util.List;

import com.atlas.action.ActionKey;
import com.atlas.device.Criticality;
import com.atlas.device.DeviceId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandTest {

    @Test
    void shouldStartAsRequested() {
        Command command = command(
                new CommandActor(
                        CommandActorType.USER,
                        "user_test"
                ),
                new CommandCausality(
                        "trace_test",
                        null,
                        0
                )
        );

        assertEquals(
                CommandStatus.REQUESTED,
                command.status()
        );

        assertEquals(
                1,
                command.statusHistory().size()
        );

        assertEquals(
                CommandStatus.REQUESTED,
                command.statusHistory().getFirst().status()
        );

        assertNull(command.result());
        assertNull(command.completedAt());
    }

    @Test
    void shouldAcceptRequestedCommand() {
        Command command = command(
                new CommandActor(
                        CommandActorType.USER,
                        "user_test"
                ),
                new CommandCausality(
                        "trace_test",
                        null,
                        0
                )
        );

        OffsetDateTime acceptedAt =
                command.requestedAt().plus(Duration.ofMillis(10));

        command.accept(acceptedAt);

        assertEquals(
                CommandStatus.ACCEPTED,
                command.status()
        );

        assertEquals(
                2,
                command.statusHistory().size()
        );

        assertEquals(
                CommandStatus.ACCEPTED,
                command.statusHistory().getLast().status()
        );

        assertNull(command.result());
        assertNull(command.completedAt());
    }

    @Test
    void shouldRejectWithResult() {
        Command command = command(
                new CommandActor(
                        CommandActorType.USER,
                        "user_test"
                ),
                new CommandCausality(
                        "trace_test",
                        null,
                        0
                )
        );

        OffsetDateTime rejectedAt =
                command.requestedAt().plus(Duration.ofMillis(10));

        command.reject(
                CommandErrorCode.INVALID_PARAMETER,
                "value must be between 0 and 100",
                rejectedAt
        );

        assertEquals(
                CommandStatus.REJECTED,
                command.status()
        );

        assertTrue(command.status().isTerminal());

        assertNotNull(command.result());

        assertEquals(
                CommandOutcome.FAILURE,
                command.result().outcome()
        );

        assertEquals(
                CommandErrorCode.INVALID_PARAMETER,
                command.result().errorCode()
        );

        assertEquals(
                rejectedAt,
                command.completedAt()
        );
    }

    @Test
    void shouldNotChangeAfterTerminalStatus() {
        Command command = command(
                new CommandActor(
                        CommandActorType.USER,
                        "user_test"
                ),
                new CommandCausality(
                        "trace_test",
                        null,
                        0
                )
        );

        OffsetDateTime rejectedAt =
                command.requestedAt().plus(Duration.ofMillis(10));

        command.reject(
                CommandErrorCode.INVALID_PARAMETER,
                "invalid value",
                rejectedAt
        );

        assertThrows(
                IllegalStateException.class,
                () -> command.accept(
                        rejectedAt.plus(Duration.ofMillis(10))
                )
        );

        assertEquals(
                CommandStatus.REJECTED,
                command.status()
        );
    }

    @Test
    void shouldRejectAutomationWithoutCause() {
        CommandActor actor =
                new CommandActor(
                        CommandActorType.AUTOMATION,
                        "automation_test"
                );

        CommandCausality causality =
                new CommandCausality(
                        "trace_test",
                        null,
                        0
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> command(actor, causality)
        );
    }

    @Test
    void shouldAllowAutomationWithCause() {
        CommandActor actor =
                new CommandActor(
                        CommandActorType.AUTOMATION,
                        "automation_test"
                );

        CommandCausality causality =
                new CommandCausality(
                        "trace_test",
                        new CausalityRef(
                                CausalityRefType.EVENT,
                                "evt_test"
                        ),
                        1
                );

        Command command =
                command(actor, causality);

        assertEquals(
                CommandStatus.REQUESTED,
                command.status()
        );

        assertNotNull(
                command.causality().cause()
        );

        assertEquals(
                CausalityRefType.EVENT,
                command.causality().cause().type()
        );
    }

    @Test
    void shouldRehydrateAcceptedCommand() {
        Command original = command(
                new CommandActor(
                        CommandActorType.USER,
                        "user_test"
                ),
                new CommandCausality(
                        "trace_test",
                        null,
                        0
                )
        );

        OffsetDateTime acceptedAt =
                original.requestedAt()
                        .plus(Duration.ofMillis(10));

        original.accept(acceptedAt);

        Command rehydrated = Command.rehydrate(
                original.id(),
                original.actor(),
                original.origin(),
                original.target(),
                original.parameters(),
                original.criticality(),
                original.requestedAt(),
                original.timeout(),
                original.idempotencyKey(),
                original.causality(),
                original.statusHistory(),
                original.dispatchedAt(),
                original.executingSince(),
                original.expectedCompletionAt(),
                original.completedAt(),
                original.result()
        );

        assertEquals(
                CommandStatus.ACCEPTED,
                rehydrated.status()
        );

        assertEquals(
                original.statusHistory(),
                rehydrated.statusHistory()
        );

        assertNull(rehydrated.result());
        assertNull(rehydrated.completedAt());
    }

    @Test
    void shouldRehydrateRejectedCommandWithResult() {
        Command original = command(
                new CommandActor(
                        CommandActorType.USER,
                        "user_test"
                ),
                new CommandCausality(
                        "trace_test",
                        null,
                        0
                )
        );

        OffsetDateTime rejectedAt =
                original.requestedAt()
                        .plus(Duration.ofMillis(10));

        original.reject(
                CommandErrorCode.INVALID_PARAMETER,
                "invalid value",
                rejectedAt
        );

        Command rehydrated = Command.rehydrate(
                original.id(),
                original.actor(),
                original.origin(),
                original.target(),
                original.parameters(),
                original.criticality(),
                original.requestedAt(),
                original.timeout(),
                original.idempotencyKey(),
                original.causality(),
                original.statusHistory(),
                original.dispatchedAt(),
                original.executingSince(),
                original.expectedCompletionAt(),
                original.completedAt(),
                original.result()
        );

        assertEquals(
                CommandStatus.REJECTED,
                rehydrated.status()
        );

        assertTrue(rehydrated.status().isTerminal());

        assertEquals(
                original.statusHistory(),
                rehydrated.statusHistory()
        );

        assertEquals(
                original.result(),
                rehydrated.result()
        );

        assertEquals(
                rejectedAt,
                rehydrated.completedAt()
        );
    }

    @Test
    void shouldProgressThroughConfirmedLifecycle() {

        Command command = command(
                new CommandActor(
                        CommandActorType.USER,
                        "user_test"
                ),
                new CommandCausality(
                        "trace_test",
                        null,
                        0
                )
        );

        OffsetDateTime acceptedAt =
                command.requestedAt()
                        .plus(Duration.ofMillis(10));

        OffsetDateTime sentAt =
                command.requestedAt()
                        .plus(Duration.ofMillis(20));

        OffsetDateTime acknowledgedAt =
                command.requestedAt()
                        .plus(Duration.ofMillis(30));

        OffsetDateTime executingAt =
                command.requestedAt()
                        .plus(Duration.ofMillis(40));

        OffsetDateTime expectedCompletionAt =
                executingAt.plus(Duration.ofSeconds(2));

        OffsetDateTime confirmedAt =
                command.requestedAt()
                        .plus(Duration.ofMillis(50));

        command.accept(acceptedAt);
        command.markSent(sentAt);
        command.acknowledge(acknowledgedAt);

        command.startExecution(
                executingAt,
                expectedCompletionAt
        );

        command.confirm(
                "adapter_message_test",
                confirmedAt
        );

        assertEquals(
                CommandStatus.CONFIRMED,
                command.status()
        );

        assertEquals(
                6,
                command.statusHistory().size()
        );

        assertEquals(
                sentAt,
                command.dispatchedAt()
        );

        assertEquals(
                executingAt,
                command.executingSince()
        );

        assertEquals(
                expectedCompletionAt,
                command.expectedCompletionAt()
        );

        assertEquals(
                confirmedAt,
                command.completedAt()
        );

        assertNotNull(
                command.result()
        );

        assertEquals(
                CommandOutcome.SUCCESS,
                command.result().outcome()
        );

        assertEquals(
                "adapter_message_test",
                command.result().adapterMessageId()
        );

        assertNull(
                command.result().errorCode()
        );
    }

    @Test
    void shouldProgressThroughCompletedLifecycle() {

        Command command = command(
                new CommandActor(
                        CommandActorType.USER,
                        "user_test"
                ),
                new CommandCausality(
                        "trace_test",
                        null,
                        0
                )
        );

        OffsetDateTime acceptedAt =
                command.requestedAt()
                        .plus(Duration.ofMillis(10));

        OffsetDateTime sentAt =
                command.requestedAt()
                        .plus(Duration.ofMillis(20));

        OffsetDateTime acknowledgedAt =
                command.requestedAt()
                        .plus(Duration.ofMillis(30));

        OffsetDateTime executingAt =
                command.requestedAt()
                        .plus(Duration.ofMillis(40));

        OffsetDateTime completedAt =
                command.requestedAt()
                        .plus(Duration.ofMillis(50));

        command.accept(acceptedAt);
        command.markSent(sentAt);
        command.acknowledge(acknowledgedAt);

        command.startExecution(
                executingAt,
                null
        );

        command.complete(
                "adapter_message_test",
                completedAt
        );

        assertEquals(
                CommandStatus.COMPLETED,
                command.status()
        );

        assertEquals(
                6,
                command.statusHistory().size()
        );

        assertEquals(
                completedAt,
                command.completedAt()
        );

        assertEquals(
                CommandOutcome.SUCCESS,
                command.result().outcome()
        );

        assertEquals(
                "adapter_message_test",
                command.result().adapterMessageId()
        );
    }

    @Test
    void shouldRejectExpectedCompletionBeforeExecutionStart() {

        Command command = command(
                new CommandActor(
                        CommandActorType.USER,
                        "user_test"
                ),
                new CommandCausality(
                        "trace_test",
                        null,
                        0
                )
        );

        OffsetDateTime acceptedAt =
                command.requestedAt()
                        .plus(Duration.ofMillis(10));

        OffsetDateTime sentAt =
                command.requestedAt()
                        .plus(Duration.ofMillis(20));

        OffsetDateTime acknowledgedAt =
                command.requestedAt()
                        .plus(Duration.ofMillis(30));

        OffsetDateTime executingAt =
                command.requestedAt()
                        .plus(Duration.ofMillis(40));

        command.accept(acceptedAt);
        command.markSent(sentAt);
        command.acknowledge(acknowledgedAt);

        assertThrows(
                IllegalArgumentException.class,
                () -> command.startExecution(
                        executingAt,
                        executingAt.minus(
                                Duration.ofMillis(1)
                        )
                )
        );

        assertEquals(
                CommandStatus.ACKNOWLEDGED,
                command.status()
        );

        assertNull(
                command.executingSince()
        );
    }

    @Test
    void shouldNotStartExecutionBeforeAcknowledgement() {

        Command command = command(
                new CommandActor(
                        CommandActorType.USER,
                        "user_test"
                ),
                new CommandCausality(
                        "trace_test",
                        null,
                        0
                )
        );

        OffsetDateTime acceptedAt =
                command.requestedAt()
                        .plus(Duration.ofMillis(10));

        OffsetDateTime executingAt =
                command.requestedAt()
                        .plus(Duration.ofMillis(20));

        command.accept(acceptedAt);

        assertThrows(
                IllegalStateException.class,
                () -> command.startExecution(
                        executingAt,
                        null
                )
        );

        assertEquals(
                CommandStatus.ACCEPTED,
                command.status()
        );

        assertNull(
                command.executingSince()
        );
    }

    @Test
    void shouldNotCompleteBeforeExecution() {

        Command command = command(
                new CommandActor(
                        CommandActorType.USER,
                        "user_test"
                ),
                new CommandCausality(
                        "trace_test",
                        null,
                        0
                )
        );

        OffsetDateTime acceptedAt =
                command.requestedAt()
                        .plus(Duration.ofMillis(10));

        OffsetDateTime sentAt =
                command.requestedAt()
                        .plus(Duration.ofMillis(20));

        OffsetDateTime completedAt =
                command.requestedAt()
                        .plus(Duration.ofMillis(30));

        command.accept(acceptedAt);
        command.markSent(sentAt);

        assertThrows(
                IllegalStateException.class,
                () -> command.complete(
                        "adapter_message_test",
                        completedAt
                )
        );

        assertEquals(
                CommandStatus.SENT,
                command.status()
        );

        assertNull(
                command.result()
        );

        assertNull(
                command.completedAt()
        );
    }

    @Test
    void shouldRejectTransitionBeforeCurrentStatusTime() {

        Command command = command(
                new CommandActor(
                        CommandActorType.USER,
                        "user_test"
                ),
                new CommandCausality(
                        "trace_test",
                        null,
                        0
                )
        );

        OffsetDateTime acceptedAt =
                command.requestedAt()
                        .plus(Duration.ofMillis(20));

        OffsetDateTime invalidSentAt =
                command.requestedAt()
                        .plus(Duration.ofMillis(10));

        command.accept(acceptedAt);

        assertThrows(
                IllegalArgumentException.class,
                () -> command.markSent(
                        invalidSentAt
                )
        );

        assertEquals(
                CommandStatus.ACCEPTED,
                command.status()
        );

        assertNull(
                command.dispatchedAt()
        );
    }

    @Test
    void shouldFailExecutingCommand() {

        Command command = command(
                new CommandActor(
                        CommandActorType.USER,
                        "user_test"
                ),
                new CommandCausality(
                        "trace_test",
                        null,
                        0
                )
        );

        OffsetDateTime acceptedAt =
                command.requestedAt()
                        .plus(Duration.ofMillis(10));

        OffsetDateTime sentAt =
                command.requestedAt()
                        .plus(Duration.ofMillis(20));

        OffsetDateTime acknowledgedAt =
                command.requestedAt()
                        .plus(Duration.ofMillis(30));

        OffsetDateTime executingAt =
                command.requestedAt()
                        .plus(Duration.ofMillis(40));

        OffsetDateTime failedAt =
                command.requestedAt()
                        .plus(Duration.ofMillis(50));

        command.accept(acceptedAt);
        command.markSent(sentAt);
        command.acknowledge(acknowledgedAt);

        command.startExecution(
                executingAt,
                null
        );

        command.fail(
                CommandErrorCode.DEVICE_UNREACHABLE,
                "device unreachable",
                "adapter_message_test",
                failedAt
        );

        assertEquals(
                CommandStatus.FAILED,
                command.status()
        );

        assertEquals(
                CommandOutcome.FAILURE,
                command.result().outcome()
        );

        assertEquals(
                CommandErrorCode.DEVICE_UNREACHABLE,
                command.result().errorCode()
        );

        assertEquals(
                "adapter_message_test",
                command.result().adapterMessageId()
        );

        assertEquals(
                failedAt,
                command.completedAt()
        );
    }

    @Test
    void shouldTimeoutSentCommandWithUnknownOutcome() {

        Command command = command(
                new CommandActor(
                        CommandActorType.USER,
                        "user_test"
                ),
                new CommandCausality(
                        "trace_test",
                        null,
                        0
                )
        );

        OffsetDateTime acceptedAt =
                command.requestedAt()
                        .plus(Duration.ofMillis(10));

        OffsetDateTime sentAt =
                command.requestedAt()
                        .plus(Duration.ofMillis(20));

        OffsetDateTime timeoutAt =
                command.requestedAt()
                        .plus(Duration.ofSeconds(5));

        command.accept(acceptedAt);
        command.markSent(sentAt);

        command.timeout(
                "command timed out",
                timeoutAt
        );

        assertEquals(
                CommandStatus.TIMEOUT,
                command.status()
        );

        assertEquals(
                CommandOutcome.UNKNOWN,
                command.result().outcome()
        );

        assertNull(
                command.result().errorCode()
        );

        assertEquals(
                timeoutAt,
                command.completedAt()
        );
    }

    @Test
    void shouldMarkUnknownOutcomeAfterDispatch() {

        Command command = command(
                new CommandActor(
                        CommandActorType.USER,
                        "user_test"
                ),
                new CommandCausality(
                        "trace_test",
                        null,
                        0
                )
        );

        OffsetDateTime acceptedAt =
                command.requestedAt()
                        .plus(Duration.ofMillis(10));

        OffsetDateTime sentAt =
                command.requestedAt()
                        .plus(Duration.ofMillis(20));

        OffsetDateTime unknownAt =
                command.requestedAt()
                        .plus(Duration.ofMillis(30));

        command.accept(acceptedAt);
        command.markSent(sentAt);

        command.markUnknownOutcome(
                "adapter response lost",
                "adapter_message_test",
                unknownAt
        );

        assertEquals(
                CommandStatus.UNKNOWN_OUTCOME,
                command.status()
        );

        assertEquals(
                CommandOutcome.UNKNOWN,
                command.result().outcome()
        );

        assertEquals(
                "adapter_message_test",
                command.result().adapterMessageId()
        );

        assertEquals(
                unknownAt,
                command.completedAt()
        );
    }

    @Test
    void shouldNotFailRequestedCommand() {

        Command command = command(
                new CommandActor(
                        CommandActorType.USER,
                        "user_test"
                ),
                new CommandCausality(
                        "trace_test",
                        null,
                        0
                )
        );

        assertThrows(
                IllegalStateException.class,
                () -> command.fail(
                        CommandErrorCode.INTERNAL_ERROR,
                        "failure",
                        null,
                        command.requestedAt()
                                .plus(Duration.ofMillis(10))
                )
        );

        assertEquals(
                CommandStatus.REQUESTED,
                command.status()
        );

        assertNull(
                command.result()
        );
    }

    @Test
    void shouldNotMarkUnknownOutcomeBeforeDispatch() {

        Command command = command(
                new CommandActor(
                        CommandActorType.USER,
                        "user_test"
                ),
                new CommandCausality(
                        "trace_test",
                        null,
                        0
                )
        );

        OffsetDateTime acceptedAt =
                command.requestedAt()
                        .plus(Duration.ofMillis(10));

        command.accept(acceptedAt);

        assertThrows(
                IllegalStateException.class,
                () -> command.markUnknownOutcome(
                        "unknown",
                        null,
                        acceptedAt.plus(
                                Duration.ofMillis(10)
                        )
                )
        );

        assertEquals(
                CommandStatus.ACCEPTED,
                command.status()
        );

        assertNull(
                command.result()
        );
    }
    @Test
    void shouldCancelRequestedCommand() {

        Command command = command(
                new CommandActor(
                        CommandActorType.USER,
                        "user_test"
                ),
                new CommandCausality(
                        "trace_test",
                        null,
                        0
                )
        );

        OffsetDateTime cancelledAt =
                command.requestedAt()
                        .plus(Duration.ofMillis(10));

        command.cancel(
                "cancelled by user",
                cancelledAt
        );

        assertEquals(
                CommandStatus.CANCELLED,
                command.status()
        );

        assertEquals(
                CommandOutcome.CANCELLED,
                command.result().outcome()
        );

        assertEquals(
                cancelledAt,
                command.completedAt()
        );
    }

    @Test
    void shouldExpireAcceptedCommand() {

        Command command = command(
                new CommandActor(
                        CommandActorType.USER,
                        "user_test"
                ),
                new CommandCausality(
                        "trace_test",
                        null,
                        0
                )
        );

        OffsetDateTime acceptedAt =
                command.requestedAt()
                        .plus(Duration.ofMillis(10));

        OffsetDateTime expiredAt =
                command.requestedAt()
                        .plus(Duration.ofSeconds(5));

        command.accept(acceptedAt);

        command.expire(
                "command validity window expired",
                expiredAt
        );

        assertEquals(
                CommandStatus.EXPIRED,
                command.status()
        );

        assertEquals(
                CommandOutcome.CANCELLED,
                command.result().outcome()
        );

        assertEquals(
                expiredAt,
                command.completedAt()
        );
    }

    @Test
    void shouldNotCancelAfterDispatch() {

        Command command = command(
                new CommandActor(
                        CommandActorType.USER,
                        "user_test"
                ),
                new CommandCausality(
                        "trace_test",
                        null,
                        0
                )
        );

        OffsetDateTime acceptedAt =
                command.requestedAt()
                        .plus(Duration.ofMillis(10));

        OffsetDateTime sentAt =
                command.requestedAt()
                        .plus(Duration.ofMillis(20));

        command.accept(acceptedAt);
        command.markSent(sentAt);

        assertThrows(
                IllegalStateException.class,
                () -> command.cancel(
                        "too late",
                        sentAt.plus(
                                Duration.ofMillis(10)
                        )
                )
        );

        assertEquals(
                CommandStatus.SENT,
                command.status()
        );

        assertNull(
                command.result()
        );
    }

    @Test
    void shouldNotExpireAfterDispatch() {

        Command command = command(
                new CommandActor(
                        CommandActorType.USER,
                        "user_test"
                ),
                new CommandCausality(
                        "trace_test",
                        null,
                        0
                )
        );

        OffsetDateTime acceptedAt =
                command.requestedAt()
                        .plus(Duration.ofMillis(10));

        OffsetDateTime sentAt =
                command.requestedAt()
                        .plus(Duration.ofMillis(20));

        command.accept(acceptedAt);
        command.markSent(sentAt);

        assertThrows(
                IllegalStateException.class,
                () -> command.expire(
                        "too late to expire",
                        sentAt.plus(
                                Duration.ofMillis(10)
                        )
                )
        );

        assertEquals(
                CommandStatus.SENT,
                command.status()
        );

        assertNull(
                command.result()
        );
    }
        @Test
    void shouldRejectInvalidTransitionDuringRehydration() {

        Command original = command(
                new CommandActor(
                        CommandActorType.USER,
                        "user_test"
                ),
                new CommandCausality(
                        "trace_test",
                        null,
                        0
                )
        );

        OffsetDateTime executingAt =
                original.requestedAt()
                        .plus(Duration.ofMillis(10));

        List<CommandStatusEntry> invalidHistory =
                List.of(
                        new CommandStatusEntry(
                                CommandStatus.REQUESTED,
                                original.requestedAt(),
                                null
                        ),
                        new CommandStatusEntry(
                                CommandStatus.EXECUTING,
                                executingAt,
                                null
                        )
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> Command.rehydrate(
                        original.id(),
                        original.actor(),
                        original.origin(),
                        original.target(),
                        original.parameters(),
                        original.criticality(),
                        original.requestedAt(),
                        original.timeout(),
                        original.idempotencyKey(),
                        original.causality(),
                        invalidHistory,
                        null,
                        executingAt,
                        null,
                        null,
                        null
                )
        );
    }

    @Test
    void shouldRehydrateCompleteValidLifecycle() {

        Command original = command(
                new CommandActor(
                        CommandActorType.USER,
                        "user_test"
                ),
                new CommandCausality(
                        "trace_test",
                        null,
                        0
                )
        );

        OffsetDateTime acceptedAt =
                original.requestedAt()
                        .plus(Duration.ofMillis(10));

        OffsetDateTime sentAt =
                original.requestedAt()
                        .plus(Duration.ofMillis(20));

        OffsetDateTime acknowledgedAt =
                original.requestedAt()
                        .plus(Duration.ofMillis(30));

        OffsetDateTime executingAt =
                original.requestedAt()
                        .plus(Duration.ofMillis(40));

        OffsetDateTime completedAt =
                original.requestedAt()
                        .plus(Duration.ofMillis(50));

        original.accept(acceptedAt);
        original.markSent(sentAt);
        original.acknowledge(acknowledgedAt);

        original.startExecution(
                executingAt,
                null
        );

        original.complete(
                "adapter_message_test",
                completedAt
        );

        Command rehydrated = Command.rehydrate(
                original.id(),
                original.actor(),
                original.origin(),
                original.target(),
                original.parameters(),
                original.criticality(),
                original.requestedAt(),
                original.timeout(),
                original.idempotencyKey(),
                original.causality(),
                original.statusHistory(),
                original.dispatchedAt(),
                original.executingSince(),
                original.expectedCompletionAt(),
                original.completedAt(),
                original.result()
        );

        assertEquals(
                CommandStatus.COMPLETED,
                rehydrated.status()
        );

        assertEquals(
                original.statusHistory(),
                rehydrated.statusHistory()
        );

        assertEquals(
                original.dispatchedAt(),
                rehydrated.dispatchedAt()
        );

        assertEquals(
                original.executingSince(),
                rehydrated.executingSince()
        );

        assertEquals(
                original.completedAt(),
                rehydrated.completedAt()
        );

        assertEquals(
                CommandOutcome.SUCCESS,
                rehydrated.result().outcome()
        );
    }

    private Command command(
            CommandActor actor,
            CommandCausality causality
    ) {
        OffsetDateTime now =
                OffsetDateTime.now();

        return new Command(
                new CommandId(
                        "cmd_00000000000000000000000000"
                ),
                actor,
                new CommandOrigin(
                        CommandOriginType.API,
                        "api_test"
                ),
                new CommandTarget(
                        new DeviceId(
                                "dev_demo_spot_01"
                        ),
                        new ActionKey(
                                "set_brightness"
                        )
                ),
                Map.of(
                        "value",
                        73
                ),
                Criticality.COSMETIC,
                now,
                Duration.ofSeconds(5),
                new CommandIdempotencyKey(
                        "idem_test",
                        Duration.ofSeconds(10)
                ),
                causality
        );
    }
}
