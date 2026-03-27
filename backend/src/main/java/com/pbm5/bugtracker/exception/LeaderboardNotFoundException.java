package com.pbm5.bugtracker.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a leaderboard is not found
 */
public class LeaderboardNotFoundException extends GamificationException {

    public LeaderboardNotFoundException(String message) {
        super(message, "LEADERBOARD_NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    public LeaderboardNotFoundException(String message, Throwable cause) {
        super(message, "LEADERBOARD_NOT_FOUND", HttpStatus.NOT_FOUND, cause);
    }
}

