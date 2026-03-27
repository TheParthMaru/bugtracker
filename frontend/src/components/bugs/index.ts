/**
 * Bug Components Index
 *
 * Central export file for all bug-related components.
 * Follows the same pattern as projects and teams components.
 */

// Badge components
export {
	BugStatusBadge,
	BugPriorityBadge,
	BugTypeBadge,
	BugLabelBadge,
	isOpenStatus,
	isFixedStatus,
	isClosedStatus,
	isReopenedStatus,
	canTransitionToStatus,
	getValidStatusTransitions,
} from "./BugStatusBadge";

// Card components
export { BugCard, BugCardSkeleton } from "./BugCard";

// Table components
export { BugsTable } from "./BugsTable";

// Filter components
export { BugFilters } from "./BugFilters";

// Attachment components
export { BugAttachmentUpload } from "./BugAttachmentUpload";

export { BugAttachmentViewer } from "./BugAttachmentViewer";

// Comment components
export { BugCommentForm } from "./BugCommentForm";

export { BugCommentThread } from "./BugCommentThread";

// Page components
export { BugDetailPage } from "./BugDetailPage";
