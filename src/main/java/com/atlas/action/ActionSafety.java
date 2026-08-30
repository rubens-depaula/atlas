package com.atlas.action;

import java.time.Duration;

public record ActionSafety(
        Duration maxExecution,
        Duration minIntervalBetweenExecutions,
        boolean requiresConfirmation,
        boolean deadmanSupported
) {

    public ActionSafety {
        if (maxExecution != null
                && (maxExecution.isZero() || maxExecution.isNegative())) {
            throw new IllegalArgumentException(
                    "maxExecution must be greater than zero"
            );
        }

        if (minIntervalBetweenExecutions != null
                && minIntervalBetweenExecutions.isNegative()) {
            throw new IllegalArgumentException(
                    "minIntervalBetweenExecutions cannot be negative"
            );
        }
    }
}