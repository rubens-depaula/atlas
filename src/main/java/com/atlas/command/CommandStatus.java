package com.atlas.command;

public enum CommandStatus {

    REQUESTED,
    ACCEPTED,
    REJECTED,
    SENT,
    ACKNOWLEDGED,
    EXECUTING,
    CONFIRMED,
    COMPLETED,
    FAILED,
    TIMEOUT,
    CANCELLED,
    EXPIRED,
    UNKNOWN_OUTCOME;

    public boolean isTerminal() {
        return switch (this) {
            case CONFIRMED,
                 COMPLETED,
                 REJECTED,
                 FAILED,
                 TIMEOUT,
                 CANCELLED,
                 EXPIRED,
                 UNKNOWN_OUTCOME -> true;

            default -> false;
        };
    }
}