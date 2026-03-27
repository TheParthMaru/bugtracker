package com.pbm5.bugtracker.entity;

/**
 * Enum representing email notification frequency options.
 * 
 * Defines how often users receive email notifications:
 * - IMMEDIATE: Send emails immediately when events occur
 * - DAILY: Batch emails and send daily digest
 * - WEEKLY: Batch emails and send weekly summary
 */
public enum EmailFrequency {
    IMMEDIATE,
    DAILY,
    WEEKLY
}
