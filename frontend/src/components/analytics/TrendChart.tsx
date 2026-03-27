import React from "react";
import type { TrendDataPoint } from "@/types/analytics";
import { cn } from "@/lib/utils";

interface TrendChartProps {
	data: TrendDataPoint[];
	title: string;
	color?: string;
	height?: number;
	width?: number;
	className?: string;
}

export function TrendChart({
	data,
	title,
	color = "#3b82f6",
	height = 300,
	width,
	className,
}: TrendChartProps) {
	if (!data || data.length === 0) {
		return (
			<div
				className={cn("flex items-center justify-center", className)}
				style={{ height }}
			>
				<div className="text-center text-muted-foreground">
					<p>No data available</p>
				</div>
			</div>
		);
	}

	const maxValue = Math.max(...data.map((item) => item.value));
	const minValue = Math.min(...data.map((item) => item.value));
	const range = maxValue - minValue;

	// Create SVG path for the line
	const points = data
		.map((item, index) => {
			const x = (index / (data.length - 1)) * 100;
			const y = range > 0 ? 100 - ((item.value - minValue) / range) * 100 : 50;
			return `${x},${y}`;
		})
		.join(" ");

	// Create area path for fill
	const areaPoints = [
		...points.split(" ").map((point) => point.replace(",", " ")),
		`100 ${range > 0 ? 100 - ((minValue - minValue) / range) * 100 : 50}`,
		`0 ${range > 0 ? 100 - ((minValue - minValue) / range) * 100 : 50}`,
	].join(" L ");

	const formatDate = (dateString: string) => {
		const date = new Date(dateString);
		return date.toLocaleDateString("en-US", {
			month: "short",
			day: "numeric",
		});
	};

	return (
		<div className={cn("space-y-4", className)}>
			<div className="text-center">
				<h3 className="text-lg font-semibold">{title}</h3>
			</div>

			<div className="relative" style={{ width: width || "100%", height }}>
				<svg
					width="100%"
					height="100%"
					viewBox="0 0 100 100"
					className="overflow-visible"
				>
					{/* Grid lines */}
					<defs>
						<pattern
							id="grid"
							width="10"
							height="10"
							patternUnits="userSpaceOnUse"
						>
							<path
								d="M 10 0 L 0 0 0 10"
								fill="none"
								stroke="#f3f4f6"
								strokeWidth="0.5"
							/>
						</pattern>
					</defs>
					<rect width="100" height="100" fill="url(#grid)" />

					{/* Area fill */}
					<path
						d={`M ${areaPoints}`}
						fill={color}
						fillOpacity="0.1"
						stroke="none"
					/>

					{/* Line */}
					<polyline
						fill="none"
						stroke={color}
						strokeWidth="2"
						points={points}
						strokeLinecap="round"
						strokeLinejoin="round"
					/>

					{/* Data points */}
					{data.map((item, index) => {
						const x = (index / (data.length - 1)) * 100;
						const y =
							range > 0 ? 100 - ((item.value - minValue) / range) * 100 : 50;

						return (
							<circle
								key={index}
								cx={x}
								cy={y}
								r="3"
								fill={color}
								stroke="#fff"
								strokeWidth="2"
								className="transition-all duration-200 hover:r-4"
							/>
						);
					})}
				</svg>

				{/* X-axis labels */}
				<div className="flex justify-between text-xs text-muted-foreground mt-2">
					{data.map((item, index) => {
						if (
							index === 0 ||
							index === data.length - 1 ||
							index % Math.ceil(data.length / 4) === 0
						) {
							return (
								<span key={index} className="truncate">
									{formatDate(item.date)}
								</span>
							);
						}
						return null;
					})}
				</div>
			</div>

			{/* Summary stats */}
			<div className="grid grid-cols-3 gap-4 text-center text-sm">
				<div>
					<div className="font-semibold">{data.length}</div>
					<div className="text-muted-foreground">Data points</div>
				</div>
				<div>
					<div className="font-semibold">{maxValue}</div>
					<div className="text-muted-foreground">Peak</div>
				</div>
				<div>
					<div className="font-semibold">
						{(
							data.reduce((sum, item) => sum + item.value, 0) / data.length
						).toFixed(1)}
					</div>
					<div className="text-muted-foreground">Average</div>
				</div>
			</div>
		</div>
	);
}
