/**
 * Frontend enums for gamification system
 * Mirrors backend enums for consistency and type safety
 */

export enum TransactionReason {
  WELCOME_BONUS = "welcome-bonus",
  DAILY_LOGIN = "daily-login",
  BUG_RESOLUTION_CRASH = "bug-resolution-crash",
  BUG_RESOLUTION_CRITICAL = "bug-resolution-critical",
  BUG_RESOLUTION_HIGH = "bug-resolution-high",
  BUG_RESOLUTION_MEDIUM = "bug-resolution-medium",
  BUG_RESOLUTION_LOW = "bug-resolution-low",
  BUG_REOPENED = "bug-reopened"
}

export enum PointValue {
  WELCOME_BONUS = 1,
  DAILY_LOGIN = 1,
  BUG_RESOLUTION_CRASH = 100,
  BUG_RESOLUTION_CRITICAL = 75,
  BUG_RESOLUTION_HIGH = 50,
  BUG_RESOLUTION_MEDIUM = 25,
  BUG_RESOLUTION_LOW = 10,
  BUG_REOPENED_PENALTY = 10
}

/**
 * Helper functions for frontend transaction parsing
 */
export const getTransactionType = (reason: string): string => {
  if (reason.includes("bug-resolution")) return "bug-resolution";
  if (reason.includes("daily-login")) return "daily-login";
  if (reason.includes("welcome-bonus")) return "welcome-bonus";
  if (reason.includes("bug-reopened")) return "bug-reopened";
  return "unknown";
};

export const isBugResolution = (reason: string): boolean => {
  return reason.includes("bug-resolution");
};

export const isDailyLogin = (reason: string): boolean => {
  return reason.includes("daily-login");
};

export const isWelcomeBonus = (reason: string): boolean => {
  return reason.includes("welcome-bonus");
};

export const isBugReopened = (reason: string): boolean => {
  return reason.includes("bug-reopened");
};

/**
 * Extract priority from bug resolution reason
 * Format: "bug-resolution-crash - CRASH (+100 points) | Project: ..."
 */
export const extractBugPriority = (reason: string): string | null => {
  const match = reason.match(/bug-resolution-(\w+)/);
  return match ? match[1].toUpperCase() : null;
};

/**
 * Extract project name from enhanced reason
 * Format: "... | Project: ProjectName | ..."
 */
export const extractProjectName = (reason: string): string | null => {
  const match = reason.match(/Project: ([^|]+)/);
  return match ? match[1].trim() : null;
};

/**
 * Extract ticket number from enhanced reason
 * Format: "... | Ticket: #123"
 */
export const extractTicketNumber = (reason: string): string | null => {
  const match = reason.match(/Ticket: ([^|]+)/);
  return match ? match[1].trim() : null;
};
