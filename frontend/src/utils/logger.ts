/**
 * Comprehensive logging utility for frontend debugging
 * Writes logs to both console and file for easier debugging
 */

export enum LogLevel {
	DEBUG = "DEBUG",
	INFO = "INFO",
	WARN = "WARN",
	ERROR = "ERROR",
}

interface LogEntry {
	timestamp: string;
	level: LogLevel;
	component: string;
	message: string;
	data?: any;
	stack?: string;
}

class FrontendLogger {
	private logBuffer: LogEntry[] = [];
	private maxBufferSize = 1000;
	private isWriting = false;
	private logFileName = "frontend-logs.log";

	constructor() {
		// Initialize file download capability
		this.setupFileDownload();
		// Make logger globally accessible for debugging
		this.makeGlobal();
	}

	private makeGlobal() {
		if (typeof window !== "undefined") {
			// @ts-ignore - Intentionally making it global for debugging
			window.frontendLogger = this;
			console.log(
				"Frontend logger available globally as window.frontendLogger"
			);
			console.log("Use window.frontendLogger.downloadLogs() to download logs");
		}
	}

	private setupFileDownload() {
		// Create a hidden download link for log files
		if (typeof document !== "undefined") {
			const link = document.createElement("a");
			link.style.display = "none";
			link.download = this.logFileName;
			document.body.appendChild(link);
		}
	}

	private async writeToFile() {
		if (this.isWriting || this.logBuffer.length === 0) return;

		this.isWriting = true;

		try {
			// Create log content
			const logContent = this.logBuffer
				.map((entry) => {
					const dataStr = entry.data
						? ` | Data: ${JSON.stringify(entry.data, null, 2)}`
						: "";
					const stackStr = entry.stack ? ` | Stack: ${entry.stack}` : "";
					return `[${entry.timestamp}] ${entry.level} | ${entry.component} | ${entry.message}${dataStr}${stackStr}`;
				})
				.join("\n");

			// Create blob and download
			const blob = new Blob([logContent], { type: "text/plain" });
			const url = URL.createObjectURL(blob);

			// Trigger download
			if (typeof document !== "undefined") {
				const link = document.createElement("a");
				link.href = url;
				link.download = this.logFileName;
				document.body.appendChild(link);
				link.click();
				document.body.removeChild(link);
				URL.revokeObjectURL(url);
			}

			// Clear buffer after successful write
			this.logBuffer = [];
		} catch (error) {
			console.error("Failed to write logs to file:", error);
		} finally {
			this.isWriting = false;
		}
	}

	private log(
		level: LogLevel,
		component: string,
		message: string,
		data?: any,
		error?: Error
	) {
		const entry: LogEntry = {
			timestamp: new Date().toISOString(),
			level,
			component,
			message,
			data,
			stack: error?.stack,
		};

		// Add to buffer
		this.logBuffer.push(entry);

		// Keep buffer size manageable
		if (this.logBuffer.length > this.maxBufferSize) {
			this.logBuffer = this.logBuffer.slice(-this.maxBufferSize / 2);
		}

		// Also log to console for immediate visibility
		const consoleMethod =
			level === LogLevel.ERROR
				? "error"
				: level === LogLevel.WARN
				? "warn"
				: level === LogLevel.INFO
				? "info"
				: "log";

		const consoleMessage = `[${level}] ${component}: ${message}`;

		if (data) {
			console[consoleMethod](consoleMessage, data);
		} else {
			console[consoleMethod](consoleMessage);
		}

		if (error) {
			console.error("Error details:", error);
		}
	}

	debug(component: string, message: string, data?: any) {
		this.log(LogLevel.DEBUG, component, message, data);
	}

	info(component: string, message: string, data?: any) {
		this.log(LogLevel.INFO, component, message, data);
	}

	warn(component: string, message: string, data?: any) {
		this.log(LogLevel.WARN, component, message, data);
	}

	error(component: string, message: string, data?: any, error?: Error) {
		this.log(LogLevel.ERROR, component, message, data, error);
	}

	// Method to manually trigger file download
	downloadLogs() {
		this.writeToFile();
	}

	// Method to clear log buffer
	clearLogs() {
		this.logBuffer = [];
	}

	// Method to get current log count
	getLogCount() {
		return this.logBuffer.length;
	}

	// Method to get logs as string
	getLogsAsString(): string {
		return this.logBuffer
			.map((entry) => {
				const dataStr = entry.data
					? ` | Data: ${JSON.stringify(entry.data, null, 2)}`
					: "";
				const stackStr = entry.stack ? ` | Stack: ${entry.stack}` : "";
				return `[${entry.timestamp}] ${entry.level} | ${entry.component} | ${entry.message}${dataStr}${stackStr}`;
			})
			.join("\n");
	}
}

// Create singleton instance
export const logger = new FrontendLogger();

// Export convenience methods
export const logDebug = (component: string, message: string, data?: any) =>
	logger.debug(component, message, data);
export const logInfo = (component: string, message: string, data?: any) =>
	logger.info(component, message, data);
export const logWarn = (component: string, message: string, data?: any) =>
	logger.warn(component, message, data);
export const logError = (
	component: string,
	message: string,
	data?: any,
	error?: Error
) => logger.error(component, message, data, error);

// Export the logger instance for advanced usage
export default logger;
