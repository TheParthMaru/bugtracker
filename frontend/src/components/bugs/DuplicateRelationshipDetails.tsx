import React from "react";
import { DuplicateRelationshipInfo, formatDate } from "@/types/similarity";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { User, Calendar, Info } from "lucide-react";

interface DuplicateRelationshipDetailsProps {
	relationshipInfo: DuplicateRelationshipInfo;
	className?: string;
}

/**
 * Component for displaying detailed duplicate relationship information.
 *
 * This component shows:
 * - Who marked the bug as duplicate
 * - When it was marked
 * - Additional context if available
 */
export const DuplicateRelationshipDetails: React.FC<
	DuplicateRelationshipDetailsProps
> = ({ relationshipInfo, className = "" }) => {
	if (!relationshipInfo) {
		return null;
	}

	return (
		<Card className={className}>
			<CardHeader className="pb-3">
				<CardTitle className="text-sm font-medium flex items-center gap-2">
					<Info className="w-4 h-4" />
					Duplicate Relationship Details
				</CardTitle>
			</CardHeader>
			<CardContent className="space-y-3">
				<div className="flex items-center gap-2 text-sm">
					<User className="w-4 h-4 text-muted-foreground" />
					<span className="text-muted-foreground">Marked by:</span>
					<Badge variant="outline" className="text-xs">
						{relationshipInfo.markedByUserName}
					</Badge>
				</div>

				<div className="flex items-center gap-2 text-sm">
					<Calendar className="w-4 h-4 text-muted-foreground" />
					<span className="text-muted-foreground">Marked on:</span>
					<span className="font-medium">
						{formatDate(relationshipInfo.markedAt)}
					</span>
				</div>
			</CardContent>
		</Card>
	);
};
