package com.atlas.property;

import java.time.Duration;
import java.util.Optional;

public sealed interface FreshnessPolicy
        permits FreshnessPolicy.Periodic,
        FreshnessPolicy.OnChange,
        FreshnessPolicy.OnDemand {

    FreshnessType mode();

    record Periodic(
            Duration expectedInterval,
            Duration staleAfter
    ) implements FreshnessPolicy {

        public Periodic {
            requirePositive(expectedInterval, "expectedInterval");
            requirePositive(staleAfter, "staleAfter");

            if (staleAfter.compareTo(expectedInterval) < 0) {
                throw new IllegalArgumentException(
                        "staleAfter cannot be shorter than expectedInterval"
                );
            }
        }

        @Override
        public FreshnessType mode() {
            return FreshnessType.PERIODIC;
        }
    }

    record OnChange(
            Optional<Duration> heartbeat
    ) implements FreshnessPolicy {

        public OnChange {
            if (heartbeat == null) {
                throw new IllegalArgumentException(
                        "heartbeat Optional cannot be null"
                );
            }

            heartbeat.ifPresent(value ->
                    requirePositive(value, "heartbeat")
            );
        }

        public static OnChange withoutHeartbeat() {
            return new OnChange(Optional.empty());
        }

        public static OnChange withHeartbeat(Duration heartbeat) {
            return new OnChange(Optional.of(heartbeat));
        }

        @Override
        public FreshnessType mode() {
            return FreshnessType.ON_CHANGE;
        }
    }

    record OnDemand(
            Duration staleAfter
    ) implements FreshnessPolicy {

        public OnDemand {
            requirePositive(staleAfter, "staleAfter");
        }

        @Override
        public FreshnessType mode() {
            return FreshnessType.ON_DEMAND;
        }
    }

    private static void requirePositive(Duration duration, String fieldName) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(
                    fieldName + " must be greater than zero"
            );
        }
    }
}