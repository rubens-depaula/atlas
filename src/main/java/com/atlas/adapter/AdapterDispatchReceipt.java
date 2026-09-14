package com.atlas.adapter;

import java.time.OffsetDateTime;

public record AdapterDispatchReceipt(
        String adapterMessageId,
        OffsetDateTime acknowledgedAt
) {

    public AdapterDispatchReceipt {
        if (adapterMessageId == null
                || adapterMessageId.isBlank()) {
            throw new IllegalArgumentException(
                    "adapterMessageId cannot be null or blank"
            );
        }

        if (acknowledgedAt == null) {
            throw new IllegalArgumentException(
                    "acknowledgedAt cannot be null"
            );
        }
    }
}
