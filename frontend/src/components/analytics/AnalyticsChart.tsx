import React from "react";
import { ChartDataPoint } from "@/types/analytics";
import { cn } from "@/lib/utils";

interface AnalyticsChartProps {
	data: ChartDataPoint[];
	title: string;
	type: "pie" | "bar" | "line" | "doughnut";
	height?: number;
	width?: number;
	className?: string;
}

export function AnalyticsChart({
	data,
	title,
	type,
	height = 300,
	width,
	className,
}: AnalyticsChartProps) {
	const total = data.reduce((sum, item) => sum + item.value, 0);

	const renderPieChart = () => {
		let currentAngle = 0;

		return (
			<div className="relative" style={{ width: width || "100%", height }}>
				<svg width="100%" height="100%" viewBox="0 0 200 200">
					{data.map((item, index) => {
						const percentage = total > 0 ? item.value / total : 0;
						const angle = percentage * 360;
						const radius = 80;
						const centerX = 100;
						const centerY = 100;

						const startAngle = currentAngle;
						const endAngle = currentAngle + angle;

						const x1 =
							centerX + radius * Math.cos(((startAngle - 90) * Math.PI) / 180);
						const y1 =
							centerY + radius * Math.sin(((startAngle - 90) * Math.PI) / 180);
						const x2 =
							centerX + radius * Math.cos(((endAngle - 90) * Math.PI) / 180);
						const y2 =
							centerY + radius * Math.sin(((endAngle - 90) * Math.PI) / 180);

						const largeArcFlag = angle > 180 ? 1 : 0;

						const pathData = [
							`M ${centerX} ${centerY}`,
							`L ${x1} ${y1}`,
							`A ${radius} ${radius} 0 ${largeArcFlag} 1 ${x2} ${y2}`,
							"Z",
						].join(" ");

						currentAngle += angle;

						return (
							<path
								key={index}
								d={pathData}
								fill={item.color || "#3b82f6"}
								stroke="#fff"
								strokeWidth="2"
							/>
						);
					})}
				</svg>

				{/* Center text for doughnut chart */}
				{type === "doughnut" && (
					<div className="absolute inset-0 flex items-center justify-center">
						<div className="text-center">
							<div className="text-2xl font-bold text-foreground">{total}</div>
							<div className="text-sm font-medium text-foreground">Total</div>
						</div>
					</div>
				)}
			</div>
		);
	};

	const renderBarChart = () => {
		const maxValue = Math.max(...data.map((item) => item.value));

		return (
			<div className="space-y-4" style={{ height }}>
				{data.map((item, index) => {
					const percentage = maxValue > 0 ? (item.value / maxValue) * 100 : 0;

					return (
						<div key={index} className="space-y-2">
							<div className="flex items-center justify-between text-sm">
								<span className="font-medium">{item.label}</span>
								<span className="text-muted-foreground">{item.value}</span>
							</div>
							<div className="w-full bg-gray-200 rounded-full h-2">
								<div
									className="h-2 rounded-full transition-all duration-300"
									style={{
										width: `${percentage}%`,
										backgroundColor: item.color || "#3b82f6",
									}}
								/>
							</div>
						</div>
					);
				})}
			</div>
		);
	};

	const renderLineChart = () => {
		const maxValue = Math.max(...data.map((item) => item.value));
		const minValue = Math.min(...data.map((item) => item.value));
		const range = maxValue - minValue;

		const points = data
			.map((item, index) => {
				const x = (index / (data.length - 1)) * 100;
				const y =
					range > 0 ? 100 - ((item.value - minValue) / range) * 100 : 50;
				return `${x},${y}`;
			})
			.join(" ");

		return (
			<div style={{ width: width || "100%", height }}>
				<svg width="100%" height="100%" viewBox="0 0 100 100">
					<polyline
						fill="none"
						stroke={data[0]?.color || "#3b82f6"}
						strokeWidth="2"
						points={points}
					/>
					{data.map((item, index) => {
						const x = (index / (data.length - 1)) * 100;
						const y =
							range > 0 ? 100 - ((item.value - minValue) / range) * 100 : 50;

						return (
							<circle
								key={index}
								cx={x}
								cy={y}
								r="2"
								fill={item.color || "#3b82f6"}
							/>
						);
					})}
				</svg>
			</div>
		);
	};

	const renderChart = () => {
		switch (type) {
			case "pie":
			case "doughnut":
				return renderPieChart();
			case "bar":
				return renderBarChart();
			case "line":
				return renderLineChart();
			default:
				return <div>Unsupported chart type</div>;
		}
	};

	return (
		<div className={cn("space-y-4", className)}>
			<div className="text-center">
				<h3 className="text-lg font-semibold">{title}</h3>
			</div>
			{renderChart()}

			{/* Legend */}
			{type === "pie" || type === "doughnut" ? (
				<div className="grid grid-cols-2 gap-2 text-sm">
					{data.map((item, index) => (
						<div key={index} className="flex items-center space-x-2">
							<div
								className="w-3 h-3 rounded-full"
								style={{ backgroundColor: item.color || "#3b82f6" }}
							/>
							<span className="truncate">{item.label}</span>
							<span className="text-muted-foreground">
								({((item.value / total) * 100).toFixed(1)}%)
							</span>
						</div>
					))}
				</div>
			) : null}
		</div>
	);
}
