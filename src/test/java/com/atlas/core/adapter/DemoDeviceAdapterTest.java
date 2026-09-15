package com.atlas.core.adapter;

import com.atlas.adapter.AdapterCommand;
import com.atlas.adapter.AdapterDispatchReceipt;
import com.atlas.adapter.AdapterExecutionSink;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoDeviceAdapterTest {

    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor();

    @AfterEach
    void shutdownExecutor() {
        executor.shutdownNow();
    }

    @Test
    void executesCommandAsynchronouslyAfterAcknowledgement()
            throws Exception {

        RecordingExecutionSink sink =
                new RecordingExecutionSink();

        DemoDeviceAdapter adapter =
                new DemoDeviceAdapter(
                        sink,
                        executor
                );

        AdapterCommand command =
                new AdapterCommand(
                        "cmd_test_async",
                        "spot_bancada_01",
                        "turn_on",
                        Map.of()
                );

        AdapterDispatchReceipt receipt =
                adapter.dispatch(command);

        assertTimeout(
                Duration.ofMillis(200),
                () -> adapter.afterAcknowledged(
                        command,
                        receipt
                )
        );

        assertFalse(
                sink.started.await(
                        100,
                        TimeUnit.MILLISECONDS
                )
        );

        assertTrue(
                sink.started.await(
                        2,
                        TimeUnit.SECONDS
                )
        );

        assertTrue(
                sink.completed.await(
                        2,
                        TimeUnit.SECONDS
                )
        );
    }

    @Test
    void leavesSimulatedTimeoutCommandAcknowledged()
            throws Exception {

        RecordingExecutionSink sink =
                new RecordingExecutionSink();

        DemoDeviceAdapter adapter =
                new DemoDeviceAdapter(
                        sink,
                        executor
                );

        AdapterCommand command =
                new AdapterCommand(
                        "cmd_test_timeout",
                        "spot_bancada_01",
                        "simulate_timeout",
                        Map.of()
                );

        AdapterDispatchReceipt receipt =
                adapter.dispatch(command);

        adapter.afterAcknowledged(
                command,
                receipt
        );

        assertFalse(
                sink.started.await(
                        700,
                        TimeUnit.MILLISECONDS
                )
        );

        assertFalse(
                sink.completed.await(
                        100,
                        TimeUnit.MILLISECONDS
                )
        );
    }

    private static final class RecordingExecutionSink
            implements AdapterExecutionSink {

        private final CountDownLatch started =
                new CountDownLatch(1);

        private final CountDownLatch completed =
                new CountDownLatch(1);

        @Override
        public void executionStarted(
                String commandId,
                OffsetDateTime startedAt,
                OffsetDateTime expectedCompletionAt
        ) {
            started.countDown();
        }

        @Override
        public void executionCompleted(
                String commandId,
                String adapterMessageId,
                OffsetDateTime completedAt
        ) {
            completed.countDown();
        }

        @Override
        public void executionConfirmed(
                String commandId,
                String adapterMessageId,
                OffsetDateTime confirmedAt
        ) {
            // Not used by this demo flow.
        }
    }
}
