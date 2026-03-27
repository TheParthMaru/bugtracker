import React, { useState, useRef, useEffect } from "react";
import { Search, ChevronDown, X } from "lucide-react";
import { Button } from "./button";
import { Input } from "./input";
import { cn } from "@/lib/utils";
import { Project } from "@/types/project";

interface ProjectPickerProps {
	projects: Project[];
	selectedProjectSlug: string;
	onProjectSelect: (projectSlug: string) => void;
	placeholder?: string;
	disabled?: boolean;
	className?: string;
}

export const ProjectPicker: React.FC<ProjectPickerProps> = ({
	projects,
	selectedProjectSlug,
	onProjectSelect,
	placeholder = "Select a project",
	disabled = false,
	className,
}) => {
	const [isOpen, setIsOpen] = useState(false);
	const [searchTerm, setSearchTerm] = useState("");
	const [focusedIndex, setFocusedIndex] = useState(-1);
	const dropdownRef = useRef<HTMLDivElement>(null);
	const inputRef = useRef<HTMLInputElement>(null);

	const selectedProject = projects.find(
		(p) => p.projectSlug === selectedProjectSlug
	);

	// Filter projects based on search term
	const filteredProjects = projects.filter((project) =>
		project.name.toLowerCase().includes(searchTerm.toLowerCase())
	);

	// Handle keyboard navigation
	const handleKeyDown = (e: React.KeyboardEvent) => {
		if (!isOpen) {
			if (e.key === "Enter" || e.key === " ") {
				e.preventDefault();
				setIsOpen(true);
				setFocusedIndex(0);
			}
			return;
		}

		switch (e.key) {
			case "Escape":
				setIsOpen(false);
				setFocusedIndex(-1);
				break;
			case "ArrowDown":
				e.preventDefault();
				setFocusedIndex((prev) =>
					prev < filteredProjects.length - 1 ? prev + 1 : 0
				);
				break;
			case "ArrowUp":
				e.preventDefault();
				setFocusedIndex((prev) =>
					prev > 0 ? prev - 1 : filteredProjects.length - 1
				);
				break;
			case "Enter":
				e.preventDefault();
				if (focusedIndex >= 0 && focusedIndex < filteredProjects.length) {
					handleProjectSelect(filteredProjects[focusedIndex].projectSlug);
				}
				break;
		}
	};

	const handleProjectSelect = (projectSlug: string) => {
		onProjectSelect(projectSlug);
		setIsOpen(false);
		setSearchTerm("");
		setFocusedIndex(-1);
	};

	const handleToggle = () => {
		if (!disabled) {
			setIsOpen(!isOpen);
			if (!isOpen) {
				setFocusedIndex(0);
				setTimeout(() => inputRef.current?.focus(), 100);
			} else {
				setSearchTerm("");
				setFocusedIndex(-1);
			}
		}
	};

	const handleClear = (e: React.MouseEvent) => {
		e.stopPropagation();
		onProjectSelect("");
		setSearchTerm("");
		setFocusedIndex(-1);
	};

	// Close dropdown when clicking outside
	useEffect(() => {
		const handleClickOutside = (event: MouseEvent) => {
			if (
				dropdownRef.current &&
				!dropdownRef.current.contains(event.target as Node)
			) {
				setIsOpen(false);
				setSearchTerm("");
				setFocusedIndex(-1);
			}
		};

		if (isOpen) {
			document.addEventListener("mousedown", handleClickOutside);
		}

		return () => {
			document.removeEventListener("mousedown", handleClickOutside);
		};
	}, [isOpen]);

	return (
		<div className={cn("relative", className)} ref={dropdownRef}>
			<button
				type="button"
				onClick={handleToggle}
				onKeyDown={handleKeyDown}
				disabled={disabled}
				className={cn(
					"flex h-10 w-full items-center justify-between rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50",
					!selectedProject && "text-muted-foreground"
				)}
				aria-haspopup="listbox"
				aria-expanded={isOpen}
				aria-label={
					selectedProject
						? `Selected project: ${selectedProject.name}`
						: "Select a project"
				}
			>
				<span className="truncate">
					{selectedProject ? (
						<div>
							<div className="font-medium">{selectedProject.name}</div>
							<div className="text-xs text-gray-500">
								{selectedProject.projectSlug}
							</div>
						</div>
					) : (
						placeholder
					)}
				</span>
				<div className="flex items-center gap-2">
					{selectedProject && (
						<div
							role="button"
							tabIndex={0}
							onClick={handleClear}
							onKeyDown={(e) => {
								if (e.key === "Enter" || e.key === " ") {
									e.preventDefault();
									handleClear(e as any);
								}
							}}
							className="h-4 w-4 p-0 hover:bg-transparent cursor-pointer inline-flex items-center justify-center rounded-sm text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50"
							aria-label="Clear selection"
						>
							<X className="h-3 w-3" />
						</div>
					)}
					<ChevronDown
						className={cn(
							"h-4 w-4 transition-transform",
							isOpen && "rotate-180"
						)}
					/>
				</div>
			</button>

			{isOpen && (
				<div className="absolute top-full left-0 right-0 z-50 mt-1 bg-white border border-gray-200 rounded-md shadow-lg max-h-60 overflow-hidden">
					{/* Search Input */}
					<div className="p-2 border-b border-gray-200">
						<div className="relative">
							<Search className="absolute left-2 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
							<Input
								ref={inputRef}
								type="text"
								placeholder="Search projects..."
								value={searchTerm}
								onChange={(e) => setSearchTerm(e.target.value)}
								className="pl-8 border-0 focus-visible:ring-0 focus-visible:ring-offset-0"
								onKeyDown={handleKeyDown}
							/>
						</div>
					</div>

					{/* Project List */}
					<div
						className="max-h-48 overflow-y-auto"
						role="listbox"
						aria-label="Available projects"
					>
						{filteredProjects.length === 0 ? (
							<div className="px-3 py-2 text-sm text-gray-500 text-center">
								{searchTerm ? "No projects found" : "No projects available"}
							</div>
						) : (
							filteredProjects.map((project, index) => (
								<button
									key={project.id}
									type="button"
									role="option"
									aria-selected={project.projectSlug === selectedProjectSlug}
									onClick={() => handleProjectSelect(project.projectSlug)}
									onMouseEnter={() => setFocusedIndex(index)}
									className={cn(
										"w-full px-3 py-2 text-left text-sm hover:bg-gray-100 focus:bg-gray-100 focus:outline-none transition-colors",
										focusedIndex === index && "bg-gray-100",
										project.projectSlug === selectedProjectSlug &&
											"bg-blue-50 text-blue-700"
									)}
								>
									<div className="font-medium">{project.name}</div>
									<div className="text-xs text-gray-500">
										{project.projectSlug}
									</div>
								</button>
							))
						)}
					</div>
				</div>
			)}
		</div>
	);
};
