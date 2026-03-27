/**
 * SimilarityConfigPanel Component
 *
 * Administration panel for configuring bug similarity detection algorithms.
 * Allows project administrators to customize algorithm weights, thresholds,
 * and enable/disable specific similarity algorithms.
 *
 * Features:
 * - Algorithm weight configuration
 * - Threshold adjustment
 * - Enable/disable toggles
 * - Configuration validation
 * - Reset to defaults
 * - Real-time configuration health check
 */

import React, { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import {
	Card,
	CardContent,
	CardDescription,
	CardHeader,
	CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import {
	Form,
	FormControl,
	FormField,
	FormItem,
	FormLabel,
	FormMessage,
} from "@/components/ui/form";
import {
	Settings,
	CheckCircle,
	AlertTriangle,
	RotateCcw,
	Info,
	TrendingUp,
} from "lucide-react";
import { bugService } from "@/services/bugService";
import type {
	SimilarityConfig,
	ConfigurationUpdateRequest,
	ConfigurationValidation,
} from "@/types/similarity";
import { SimilarityAlgorithm } from "@/types/similarity";

const configSchema = z.object({
	cosineWeight: z.number().min(0).max(1),
	cosineThreshold: z.number().min(0.1).max(0.99),
	cosineEnabled: z.boolean(),
	jaccardWeight: z.number().min(0).max(1),
	jaccardThreshold: z.number().min(0.1).max(0.99),
	jaccardEnabled: z.boolean(),
	levenshteinWeight: z.number().min(0).max(1),
	levenshteinThreshold: z.number().min(0.1).max(0.99),
	levenshteinEnabled: z.boolean(),
});

type ConfigFormData = z.infer<typeof configSchema>;

interface SimilarityConfigPanelProps {
	projectSlug: string;
	onConfigurationChange?: () => void;
}

const SimilarityConfigPanel: React.FC<SimilarityConfigPanelProps> = ({
	projectSlug,
	onConfigurationChange,
}) => {
	const [configurations, setConfigurations] = useState<SimilarityConfig[]>([]);
	const [validation, setValidation] = useState<ConfigurationValidation | null>(
		null
	);
	const [isLoading, setIsLoading] = useState(true);
	const [isSaving, setIsSaving] = useState(false);
	const [error, setError] = useState<string | null>(null);

	const form = useForm<ConfigFormData>({
		resolver: zodResolver(configSchema),
		defaultValues: {
			cosineWeight: 0.6,
			cosineThreshold: 0.75,
			cosineEnabled: true,
			jaccardWeight: 0.3,
			jaccardThreshold: 0.5,
			jaccardEnabled: true,
			levenshteinWeight: 0.1,
			levenshteinThreshold: 0.8,
			levenshteinEnabled: true,
		},
	});

	// Load configurations on mount
	useEffect(() => {
		loadConfigurations();
	}, [projectSlug]);

	const loadConfigurations = async () => {
		try {
			setIsLoading(true);
			setError(null);

			// Load configurations first
			const configs = await bugService.getSimilarityConfigurations(projectSlug);
			setConfigurations(configs);

			// Try to load validation, but don't fail if it errors
			try {
				const validationResult =
					await bugService.validateSimilarityConfigurations(projectSlug);
				setValidation(validationResult);
			} catch (validationError: any) {
				console.warn(
					"Validation failed, but configurations loaded:",
					validationError.message
				);
				// Don't set error for validation failure, just show warning
			}

			// Update form with loaded values
			const cosineConfig = configs.find((c) => c.algorithmName === "COSINE");
			const jaccardConfig = configs.find((c) => c.algorithmName === "JACCARD");
			const levenshteinConfig = configs.find(
				(c) => c.algorithmName === "LEVENSHTEIN"
			);

			form.reset({
				cosineWeight: cosineConfig?.weight || 0.6,
				cosineThreshold: cosineConfig?.threshold || 0.75,
				cosineEnabled: cosineConfig?.isEnabled || true,
				jaccardWeight: jaccardConfig?.weight || 0.3,
				jaccardThreshold: jaccardConfig?.threshold || 0.5,
				jaccardEnabled: jaccardConfig?.isEnabled || true,
				levenshteinWeight: levenshteinConfig?.weight || 0.1,
				levenshteinThreshold: levenshteinConfig?.threshold || 0.8,
				levenshteinEnabled: levenshteinConfig?.isEnabled || true,
			});
		} catch (err: any) {
			setError(err.message || "Failed to load configurations");
		} finally {
			setIsLoading(false);
		}
	};

	const handleInitialize = async () => {
		try {
			setIsSaving(true);
			setError(null);

			await bugService.initializeSimilarityConfigurations(projectSlug);
			await loadConfigurations();

			if (onConfigurationChange) {
				onConfigurationChange();
			}
		} catch (err: any) {
			setError(err.message || "Failed to initialize configurations");
		} finally {
			setIsSaving(false);
		}
	};

	const handleSubmit = async (data: ConfigFormData) => {
		try {
			setIsSaving(true);
			setError(null);

			// Update each algorithm configuration
			const updatePromises = [
				bugService.updateAlgorithmConfiguration(
					projectSlug,
					SimilarityAlgorithm.COSINE,
					{
						weight: data.cosineWeight,
						threshold: data.cosineThreshold,
						isEnabled: data.cosineEnabled,
					}
				),
				bugService.updateAlgorithmConfiguration(
					projectSlug,
					SimilarityAlgorithm.JACCARD,
					{
						weight: data.jaccardWeight,
						threshold: data.jaccardThreshold,
						isEnabled: data.jaccardEnabled,
					}
				),
				bugService.updateAlgorithmConfiguration(
					projectSlug,
					SimilarityAlgorithm.LEVENSHTEIN,
					{
						weight: data.levenshteinWeight,
						threshold: data.levenshteinThreshold,
						isEnabled: data.levenshteinEnabled,
					}
				),
			];

			await Promise.all(updatePromises);

			// Reload configurations to get updated values
			await loadConfigurations();

			if (onConfigurationChange) {
				onConfigurationChange();
			}
		} catch (err: any) {
			setError(err.message || "Failed to update configurations");
		} finally {
			setIsSaving(false);
		}
	};

	const handleReset = async () => {
		try {
			setIsSaving(true);
			setError(null);

			await bugService.resetSimilarityConfigurations(projectSlug);
			await loadConfigurations();

			if (onConfigurationChange) {
				onConfigurationChange();
			}
		} catch (err: any) {
			setError(err.message || "Failed to reset configurations");
		} finally {
			setIsSaving(false);
		}
	};

	if (isLoading) {
		return (
			<Card>
				<CardContent className="p-6">
					<div className="flex items-center space-x-2">
						<Settings className="h-5 w-5 animate-spin" />
						<span>Loading similarity configurations...</span>
					</div>
				</CardContent>
			</Card>
		);
	}

	const totalWeight =
		form.watch("cosineWeight") +
		form.watch("jaccardWeight") +
		form.watch("levenshteinWeight");
	const isWeightValid = Math.abs(totalWeight - 1.0) <= 0.1;

	return (
		<div className="space-y-6">
			{/* Error Display */}
			{error && (
				<Alert variant="destructive">
					<AlertTriangle className="h-4 w-4" />
					<AlertDescription>{error}</AlertDescription>
				</Alert>
			)}

			{/* No Configurations Found - Show Initialization */}
			{configurations.length === 0 && !isLoading && (
				<Card>
					<CardHeader>
						<CardTitle className="flex items-center gap-2">
							<AlertTriangle className="h-5 w-5 text-orange-500" />
							No Similarity Configurations Found
						</CardTitle>
						<CardDescription>
							This project doesn't have similarity detection configured yet.
							Click the button below to initialize with default settings.
						</CardDescription>
					</CardHeader>
					<CardContent>
						<Button
							onClick={handleInitialize}
							disabled={isSaving}
							className="gap-2"
						>
							{isSaving && <Settings className="h-4 w-4 animate-spin" />}
							<Settings className="h-4 w-4" />
							Initialize Default Configurations
						</Button>
					</CardContent>
				</Card>
			)}

			{/* Configuration Health Status */}
			{validation && (
				<Card>
					<CardHeader>
						<CardTitle className="flex items-center gap-2">
							{validation.isValid ? (
								<CheckCircle className="h-5 w-5 text-green-500" />
							) : (
								<AlertTriangle className="h-5 w-5 text-red-500" />
							)}
							Configuration Health
						</CardTitle>
					</CardHeader>
					<CardContent>
						<div className="space-y-3">
							<div className="flex items-center justify-between">
								<span className="text-sm font-medium">Status:</span>
								<Badge
									variant={validation.isValid ? "default" : "destructive"}
									className={
										validation.isValid
											? "bg-green-100 text-green-800 border-green-200"
											: "bg-red-100 text-red-800 border-red-200"
									}
								>
									{validation.isValid ? "Healthy" : "Issues Found"}
								</Badge>
							</div>
							{validation.recommendations &&
								validation.recommendations.length > 0 && (
									<div>
										<span className="text-sm font-medium">
											Recommendations:
										</span>
										<ul className="mt-2 space-y-1">
											{validation.recommendations.map((rec, index) => (
												<li
													key={index}
													className="text-sm text-muted-foreground"
												>
													• {rec}
												</li>
											))}
										</ul>
									</div>
								)}
						</div>
					</CardContent>
				</Card>
			)}

			{/* Validation Warning - When validation fails but configurations exist */}
			{configurations.length > 0 && !validation && (
				<Alert>
					<AlertTriangle className="h-4 w-4" />
					<AlertDescription>
						Configuration validation is temporarily unavailable, but your
						similarity settings are loaded. You can still modify and save your
						configurations.
					</AlertDescription>
				</Alert>
			)}

			{/* Configuration Form */}
			{configurations.length > 0 && (
				<Card>
					<CardHeader>
						<CardTitle className="flex items-center gap-2">
							<Settings className="h-5 w-5" />
							Algorithm Configuration
						</CardTitle>
						<CardDescription>
							Configure similarity algorithm weights, thresholds, and
							enable/disable status. Weights should sum to approximately 1.0 for
							optimal results.
						</CardDescription>
					</CardHeader>
					<CardContent>
						<Form {...form}>
							<form
								onSubmit={form.handleSubmit(handleSubmit)}
								className="space-y-6"
							>
								{/* Weight Summary */}
								<div className="p-4 bg-muted rounded-lg">
									<div className="flex items-center justify-between mb-2">
										<span className="text-sm font-medium">Total Weight:</span>
										<Badge
											variant={isWeightValid ? "secondary" : "destructive"}
										>
											{totalWeight.toFixed(2)}
										</Badge>
									</div>
									{!isWeightValid && (
										<p className="text-xs text-muted-foreground">
											Weights should sum to 1.0 for optimal results.
										</p>
									)}
								</div>

								{/* Cosine Similarity */}
								<div className="space-y-4">
									<div className="flex items-center justify-between">
										<h4 className="font-medium">Cosine Similarity</h4>
										<FormField
											control={form.control}
											name="cosineEnabled"
											render={({ field }) => (
												<FormItem className="flex items-center space-x-2">
													<FormControl>
														<Switch
															checked={field.value}
															onCheckedChange={field.onChange}
														/>
													</FormControl>
													<Label>Enabled</Label>
												</FormItem>
											)}
										/>
									</div>

									<div className="grid grid-cols-2 gap-4">
										<FormField
											control={form.control}
											name="cosineWeight"
											render={({ field }) => (
												<FormItem>
													<FormLabel>Weight</FormLabel>
													<FormControl>
														<Input
															type="number"
															step="0.1"
															min="0"
															max="1"
															{...field}
															onChange={(e) =>
																field.onChange(parseFloat(e.target.value) || 0)
															}
														/>
													</FormControl>
													<FormMessage />
												</FormItem>
											)}
										/>

										<FormField
											control={form.control}
											name="cosineThreshold"
											render={({ field }) => (
												<FormItem>
													<FormLabel>Threshold</FormLabel>
													<FormControl>
														<Input
															type="number"
															step="0.05"
															min="0.1"
															max="0.99"
															{...field}
															onChange={(e) =>
																field.onChange(parseFloat(e.target.value) || 0)
															}
														/>
													</FormControl>
													<FormMessage />
												</FormItem>
											)}
										/>
									</div>
								</div>

								<Separator />

								{/* Jaccard Similarity */}
								<div className="space-y-4">
									<div className="flex items-center justify-between">
										<h4 className="font-medium">Jaccard Similarity</h4>
										<FormField
											control={form.control}
											name="jaccardEnabled"
											render={({ field }) => (
												<FormItem className="flex items-center space-x-2">
													<FormControl>
														<Switch
															checked={field.value}
															onCheckedChange={field.onChange}
														/>
													</FormControl>
													<Label>Enabled</Label>
												</FormItem>
											)}
										/>
									</div>

									<div className="grid grid-cols-2 gap-4">
										<FormField
											control={form.control}
											name="jaccardWeight"
											render={({ field }) => (
												<FormItem>
													<FormLabel>Weight</FormLabel>
													<FormControl>
														<Input
															type="number"
															step="0.1"
															min="0"
															max="1"
															{...field}
															onChange={(e) =>
																field.onChange(parseFloat(e.target.value) || 0)
															}
														/>
													</FormControl>
													<FormMessage />
												</FormItem>
											)}
										/>

										<FormField
											control={form.control}
											name="jaccardThreshold"
											render={({ field }) => (
												<FormItem>
													<FormLabel>Threshold</FormLabel>
													<FormControl>
														<Input
															type="number"
															step="0.05"
															min="0.1"
															max="0.99"
															{...field}
															onChange={(e) =>
																field.onChange(parseFloat(e.target.value) || 0)
															}
														/>
													</FormControl>
													<FormMessage />
												</FormItem>
											)}
										/>
									</div>
								</div>

								<Separator />

								{/* Levenshtein Similarity */}
								<div className="space-y-4">
									<div className="flex items-center justify-between">
										<h4 className="font-medium">Levenshtein Similarity</h4>
										<FormField
											control={form.control}
											name="levenshteinEnabled"
											render={({ field }) => (
												<FormItem className="flex items-center space-x-2">
													<FormControl>
														<Switch
															checked={field.value}
															onCheckedChange={field.onChange}
														/>
													</FormControl>
													<Label>Enabled</Label>
												</FormItem>
											)}
										/>
									</div>

									<div className="grid grid-cols-2 gap-4">
										<FormField
											control={form.control}
											name="levenshteinWeight"
											render={({ field }) => (
												<FormItem>
													<FormLabel>Weight</FormLabel>
													<FormControl>
														<Input
															type="number"
															step="0.1"
															min="0"
															max="1"
															{...field}
															onChange={(e) =>
																field.onChange(parseFloat(e.target.value) || 0)
															}
														/>
													</FormControl>
													<FormMessage />
												</FormItem>
											)}
										/>

										<FormField
											control={form.control}
											name="levenshteinThreshold"
											render={({ field }) => (
												<FormItem>
													<FormLabel>Threshold</FormLabel>
													<FormControl>
														<Input
															type="number"
															step="0.05"
															min="0.1"
															max="0.99"
															{...field}
															onChange={(e) =>
																field.onChange(parseFloat(e.target.value) || 0)
															}
														/>
													</FormControl>
													<FormMessage />
												</FormItem>
											)}
										/>
									</div>
								</div>

								{/* Action Buttons */}
								<div className="flex justify-between pt-6">
									<Button
										type="button"
										variant="outline"
										onClick={handleReset}
										disabled={isSaving}
										className="gap-2"
									>
										<RotateCcw className="h-4 w-4" />
										Reset to Defaults
									</Button>

									<Button type="submit" disabled={isSaving} className="gap-2">
										{isSaving && <Settings className="h-4 w-4 animate-spin" />}
										Save Configuration
									</Button>
								</div>
							</form>
						</Form>
					</CardContent>
				</Card>
			)}

			{/* Configuration Help */}
			<Card>
				<CardHeader>
					<CardTitle className="flex items-center gap-2">
						<Info className="h-5 w-5" />
						Algorithm Information
					</CardTitle>
				</CardHeader>
				<CardContent className="space-y-4 text-sm">
					<div>
						<h5 className="font-medium">Cosine Similarity</h5>
						<p className="text-muted-foreground">
							Best for comparing semantic meaning and document similarity.
							Effective for finding bugs with similar topics and concepts.
						</p>
					</div>
					<div>
						<h5 className="font-medium">Jaccard Similarity</h5>
						<p className="text-muted-foreground">
							Compares sets of unique words. Good for finding bugs with
							overlapping keywords and terminology.
						</p>
					</div>
					<div>
						<h5 className="font-medium">Levenshtein Similarity</h5>
						<p className="text-muted-foreground">
							Character-level comparison. Useful for catching typos and similar
							text variations.
						</p>
					</div>
				</CardContent>
			</Card>
		</div>
	);
};

export default SimilarityConfigPanel;
