# Frontend Logging System

This project now includes a comprehensive logging system that writes logs to both the browser console and a downloadable file for easier debugging.

## Features

- **Dual Output**: Logs appear in both console and file
- **Structured Logging**: Each log entry includes timestamp, level, component, and message
- **Data Logging**: Can log additional data objects and error stacks
- **File Download**: Download all logs as a text file
- **Global Access**: Access logger from browser console for debugging

## Usage

### In Components

```typescript
import { logDebug, logInfo, logWarn, logError } from "../utils/logger";

// Basic logging
logDebug("ComponentName", "Debug message");
logInfo("ComponentName", "Info message");
logWarn("ComponentName", "Warning message");
logError("ComponentName", "Error message", { additionalData: "value" });

// Logging with data
logDebug("ComponentName", "User action", { userId: 123, action: "click" });

// Logging errors
try {
	// some code
} catch (error) {
	logError(
		"ComponentName",
		"Operation failed",
		{ context: "user creation" },
		error
	);
}
```

### Downloading Logs

#### Method 1: UI Button

- Click the "📥 Download Logs (X)" button in the Create Bug modal
- Logs will automatically download as `frontend-logs.log`

#### Method 2: Browser Console

```javascript
// Download logs
window.frontendLogger.downloadLogs();

// View log count
window.frontendLogger.getLogCount();

// Clear logs
window.frontendLogger.clearLogs();

// Get logs as string
const logText = window.frontendLogger.getLogsAsString();
```

## Log Format

Each log entry follows this format:

```
[2024-01-15T10:30:45.123Z] DEBUG | ComponentName | Message | Data: {"key": "value"} | Stack: Error stack trace
```

## Log Levels

- **DEBUG**: Detailed information for debugging
- **INFO**: General information about application flow
- **WARN**: Warning messages for potential issues
- **ERROR**: Error messages with full error details

## Component Naming Convention

Use descriptive component names for better log organization:

- `CreateBugModal` - For bug creation modal
- `BugCard` - For individual bug cards
- `TeamAssignmentSection` - For team assignment functionality
- `DuplicateDetection` - For duplicate detection logic

## Benefits

1. **No More Console Copy-Paste**: Logs are automatically saved and downloadable
2. **Structured Debugging**: Easy to filter and search through logs
3. **Production Debugging**: Can collect logs from production builds
4. **Team Collaboration**: Share log files with team members
5. **Historical Analysis**: Keep logs for post-mortem analysis

## Example Usage in useEffect

```typescript
useEffect(() => {
	logDebug("ComponentName", "useEffect triggered", {
		dependency1: value1,
		dependency2: value2,
	});

	// Your effect logic here
}, [dependency1, dependency2]);
```

## Example Usage in Event Handlers

```typescript
const handleClick = useCallback(() => {
	logInfo("ComponentName", "Button clicked", {
		buttonId: "submit",
		timestamp: Date.now(),
	});

	// Handle the click
}, []);
```

## Performance Considerations

- Logs are buffered in memory (max 1000 entries)
- Old logs are automatically trimmed to prevent memory issues
- File download only happens when explicitly requested
- Console logging is immediate for real-time debugging

## Troubleshooting

If logs aren't appearing:

1. Check browser console for any errors
2. Verify the logger is imported correctly
3. Check if `window.frontendLogger` exists in console
4. Ensure the component name is a string, not a variable

## Future Enhancements

- Log level filtering
- Log persistence across page reloads
- Remote log aggregation
- Log rotation and archiving
- Performance metrics logging
