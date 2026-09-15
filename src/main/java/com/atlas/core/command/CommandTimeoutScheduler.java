package com.atlas.core.command;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public final class CommandTimeoutScheduler {

    private final CommandTimeoutWatchdog watchdog;

    public CommandTimeoutScheduler(
            CommandTimeoutWatchdog watchdog
    ) {
        if (watchdog == null) {
            throw new IllegalArgumentException(
                    "watchdog cannot be null"
            );
        }

        this.watchdog = watchdog;
    }

    @Scheduled(
            fixedDelayString =
                    "${atlas.command-timeout.scan-interval-ms:1000}"
    )
    public void scan() {
        watchdog.scan(
                OffsetDateTime.now()
        );
    }
}
