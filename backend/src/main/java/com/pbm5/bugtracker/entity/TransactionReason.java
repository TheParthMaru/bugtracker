package com.pbm5.bugtracker.entity;

/**
 * Enum for transaction reasons in the gamification system
 * Provides type-safe reason identifiers for point transactions
 */
public enum TransactionReason {
    WELCOME_BONUS("welcome-bonus"),
    DAILY_LOGIN("daily-login"),
    BUG_RESOLUTION_CRASH("bug-resolution-crash"),
    BUG_RESOLUTION_CRITICAL("bug-resolution-critical"),
    BUG_RESOLUTION_HIGH("bug-resolution-high"),
    BUG_RESOLUTION_MEDIUM("bug-resolution-medium"),
    BUG_RESOLUTION_LOW("bug-resolution-low"),
    BUG_REOPENED("bug-reopened");

    private final String value;

    TransactionReason(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static TransactionReason fromValue(String value) {
        for (TransactionReason reason : values()) {
            if (reason.value.equals(value)) {
                return reason;
            }
        }
        throw new IllegalArgumentException("Unknown transaction reason: " + value);
    }
}