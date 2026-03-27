/**
 * Component for displaying duplicate status information in bug detail pages.
 *
 * This component shows:
 * - Whether a bug is a duplicate
 * - Link to the original bug
 * - Count of other duplicates
 * - Navigation actions
 */

import React from "react";
import { DuplicateManagementPanel } from "./DuplicateManagementPanel";
import type { DuplicateInfoResponse } from "@/types/similarity";

interface DuplicateStatusBadgeProps {
	duplicateInfo: DuplicateInfoResponse;
	projectSlug: string;
	bugId: number;
	onViewOriginal?: () => void;
	onViewDuplicates?: () => void;
	onDuplicateRemoved?: () => void;
}

export const DuplicateStatusBadge: React.FC<DuplicateStatusBadgeProps> = ({
	duplicateInfo,
	projectSlug,
	bugId,
	onDuplicateRemoved,
}) => {
	// Use the new comprehensive DuplicateManagementPanel
	return (
		<DuplicateManagementPanel
			duplicateInfo={duplicateInfo}
			projectSlug={projectSlug}
			currentBugId={bugId}
			onDuplicateRemoved={onDuplicateRemoved}
		/>
	);
};
