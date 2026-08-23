package com.atlas.property;

import java.math.BigDecimal;
import java.util.List;

public record ValueDomain(
        BigDecimal min,
        BigDecimal max,
        BigDecimal step,
        List<String> allowedValues,
        boolean monotonic
) {

    public ValueDomain {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new IllegalArgumentException(
                    "min cannot be greater than max"
            );
        }

        if (step != null && step.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "step must be greater than zero"
            );
        }

        if (allowedValues != null) {
            allowedValues = List.copyOf(allowedValues);
        }
    }
}