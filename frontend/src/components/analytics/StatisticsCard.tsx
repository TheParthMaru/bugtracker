import React from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { TrendingUp, TrendingDown, Minus } from "lucide-react";
import { cn } from "@/lib/utils";

interface StatisticsCardProps {
	title: string;
	value: number | string;
	change?: number;
	changeType?: "increase" | "decrease" | "neutral";
	icon?: React.ReactNode;
	className?: string;
	description?: string;
}

export function StatisticsCard({
	title,
	value,
	change,
	changeType = "neutral",
	icon,
	className,
	description,
}: StatisticsCardProps) {
	const getChangeIcon = () => {
		switch (changeType) {
			case "increase":
				return <TrendingUp className="h-4 w-4 text-green-600" />;
			case "decrease":
				return <TrendingDown className="h-4 w-4 text-red-600" />;
			default:
				return <Minus className="h-4 w-4 text-gray-400" />;
		}
	};

	const getChangeColor = () => {
		switch (changeType) {
			case "increase":
				return "text-green-600";
			case "decrease":
				return "text-red-600";
			default:
				return "text-gray-600";
		}
	};

	const formatValue = (val: number | string) => {
		if (typeof val === "number") {
			return val.toLocaleString();
		}
		return val;
	};

	return (
		<Card className={cn("hover:shadow-md transition-shadow", className)}>
			<CardHeader className="flex flex-row items-center justify-between space-y-0 pb-1">
				<CardTitle className="text-sm font-medium text-muted-foreground">
					{title}
				</CardTitle>
				{icon && <div className="h-4 w-4 text-muted-foreground">{icon}</div>}
			</CardHeader>
			<CardContent className="pt-0">
				<div className="text-2xl font-bold">{formatValue(value)}</div>
				{description && (
					<p className="text-xs text-muted-foreground mt-1">{description}</p>
				)}
				{change !== undefined && (
					<div className="flex items-center space-x-1 mt-2">
						{getChangeIcon()}
						<span className={cn("text-xs font-medium", getChangeColor())}>
							{change > 0 ? "+" : ""}
							{change}%
						</span>
						<span className="text-xs text-muted-foreground">
							from last period
						</span>
					</div>
				)}
			</CardContent>
		</Card>
	);
}
