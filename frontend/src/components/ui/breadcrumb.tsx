import * as React from "react";
import { Slot } from "@radix-ui/react-slot";
import { ChevronRight, MoreHorizontal } from "lucide-react";
import { Link } from "react-router-dom";

import { cn } from "@/lib/utils";

// ============================================================================
// SHADCN UI BREADCRUMB COMPONENTS
// ============================================================================

function Breadcrumb({ ...props }: React.ComponentProps<"nav">) {
	return <nav aria-label="breadcrumb" data-slot="breadcrumb" {...props} />;
}

function BreadcrumbList({ className, ...props }: React.ComponentProps<"ol">) {
	return (
		<ol
			data-slot="breadcrumb-list"
			className={cn(
				"text-muted-foreground flex flex-wrap items-center gap-1.5 text-sm break-words sm:gap-2.5",
				className
			)}
			{...props}
		/>
	);
}

function BreadcrumbItem({ className, ...props }: React.ComponentProps<"li">) {
	return (
		<li
			data-slot="breadcrumb-item"
			className={cn("inline-flex items-center gap-1.5", className)}
			{...props}
		/>
	);
}

function BreadcrumbLink({
	asChild,
	className,
	...props
}: React.ComponentProps<"a"> & {
	asChild?: boolean;
}) {
	const Comp = asChild ? Slot : "a";

	return (
		<Comp
			data-slot="breadcrumb-link"
			className={cn("hover:text-foreground transition-colors", className)}
			{...props}
		/>
	);
}

function BreadcrumbPage({ className, ...props }: React.ComponentProps<"span">) {
	return (
		<span
			data-slot="breadcrumb-page"
			role="link"
			aria-disabled="true"
			aria-current="page"
			className={cn("text-foreground font-normal", className)}
			{...props}
		/>
	);
}

function BreadcrumbSeparator({
	children,
	className,
	...props
}: React.ComponentProps<"li">) {
	return (
		<li
			data-slot="breadcrumb-separator"
			role="presentation"
			aria-hidden="true"
			className={cn("[&>svg]:size-3.5", className)}
			{...props}
		>
			{children ?? <ChevronRight />}
		</li>
	);
}

function BreadcrumbEllipsis({
	className,
	...props
}: React.ComponentProps<"span">) {
	return (
		<span
			data-slot="breadcrumb-ellipsis"
			role="presentation"
			aria-hidden="true"
			className={cn("flex size-9 items-center justify-center", className)}
			{...props}
		>
			<MoreHorizontal className="size-4" />
			<span className="sr-only">More</span>
		</span>
	);
}

// ============================================================================
// CUSTOM BREADCRUMB COMPONENTS (Built with shadcn components above)
// ============================================================================

export interface CustomBreadcrumbProps {
	className?: string;
	onBackClick?: () => void;
}

/**
 * Project-specific breadcrumb component
 * Pattern: Home > Projects > [Project Name] > [Section] > [Current Page]
 */
export interface ProjectBreadcrumbProps extends CustomBreadcrumbProps {
	projectName: string;
	projectSlug: string;
	section?: string;
	sectionHref?: string;
	current?: string;
}

export function ProjectBreadcrumb({
	projectName,
	projectSlug,
	section,
	sectionHref,
	current,
	className = "mb-6",
}: ProjectBreadcrumbProps) {
	return (
		<Breadcrumb className={className}>
			<BreadcrumbList>
				<BreadcrumbItem>
					<BreadcrumbLink asChild>
						<Link to="/">Home</Link>
					</BreadcrumbLink>
				</BreadcrumbItem>
				<BreadcrumbSeparator />
				<BreadcrumbItem>
					<BreadcrumbLink asChild>
						<Link to="/projects">Projects</Link>
					</BreadcrumbLink>
				</BreadcrumbItem>
				<BreadcrumbSeparator />
				<BreadcrumbItem>
					<BreadcrumbLink asChild>
						<Link to={`/projects/${projectSlug}`}>{projectName}</Link>
					</BreadcrumbLink>
				</BreadcrumbItem>
				{section && (
					<>
						<BreadcrumbSeparator />
						<BreadcrumbItem>
							{sectionHref ? (
								<BreadcrumbLink asChild>
									<Link to={sectionHref}>{section}</Link>
								</BreadcrumbLink>
							) : (
								<BreadcrumbPage>{section}</BreadcrumbPage>
							)}
						</BreadcrumbItem>
					</>
				)}
				{current && (
					<>
						<BreadcrumbSeparator />
						<BreadcrumbItem>
							<BreadcrumbPage>{current}</BreadcrumbPage>
						</BreadcrumbItem>
					</>
				)}
			</BreadcrumbList>
		</Breadcrumb>
	);
}

/**
 * Team-specific breadcrumb component
 * Pattern: Home > Teams > [Team Name] > [Current Page]
 */
export interface TeamBreadcrumbProps extends CustomBreadcrumbProps {
	teamName: string;
	teamSlug: string;
	current?: string;
}

export function TeamBreadcrumb({
	teamName,
	teamSlug,
	current,
	className = "mb-6",
}: TeamBreadcrumbProps) {
	return (
		<Breadcrumb className={className}>
			<BreadcrumbList>
				<BreadcrumbItem>
					<BreadcrumbLink asChild>
						<Link to="/">Home</Link>
					</BreadcrumbLink>
				</BreadcrumbItem>
				<BreadcrumbSeparator />
				<BreadcrumbItem>
					<BreadcrumbLink asChild>
						<Link to="/teams">Teams</Link>
					</BreadcrumbLink>
				</BreadcrumbItem>
				<BreadcrumbSeparator />
				<BreadcrumbItem>
					<BreadcrumbLink asChild>
						<Link to={`/teams/${teamSlug}`}>{teamName}</Link>
					</BreadcrumbLink>
				</BreadcrumbItem>
				{current && (
					<>
						<BreadcrumbSeparator />
						<BreadcrumbItem>
							<BreadcrumbPage>{current}</BreadcrumbPage>
						</BreadcrumbItem>
					</>
				)}
			</BreadcrumbList>
		</Breadcrumb>
	);
}

/**
 * Project Team breadcrumb component
 * Pattern: Home > Projects > [Project Name] > Teams > [Team Name] > [Current Page]
 */
export interface ProjectTeamBreadcrumbProps extends CustomBreadcrumbProps {
	projectName: string;
	projectSlug: string;
	teamName: string;
	teamSlug: string;
	current?: string;
}

export function ProjectTeamBreadcrumb({
	projectName,
	projectSlug,
	teamName,
	teamSlug,
	current,
	className = "mb-6",
}: ProjectTeamBreadcrumbProps) {
	return (
		<Breadcrumb className={className}>
			<BreadcrumbList>
				<BreadcrumbItem>
					<BreadcrumbLink asChild>
						<Link to="/">Home</Link>
					</BreadcrumbLink>
				</BreadcrumbItem>
				<BreadcrumbSeparator />
				<BreadcrumbItem>
					<BreadcrumbLink asChild>
						<Link to="/projects">Projects</Link>
					</BreadcrumbLink>
				</BreadcrumbItem>
				<BreadcrumbSeparator />
				<BreadcrumbItem>
					<BreadcrumbLink asChild>
						<Link to={`/projects/${projectSlug}`}>{projectName}</Link>
					</BreadcrumbLink>
				</BreadcrumbItem>
				<BreadcrumbSeparator />
				<BreadcrumbItem>
					<BreadcrumbLink asChild>
						<Link to={`/projects/${projectSlug}/teams`}>Teams</Link>
					</BreadcrumbLink>
				</BreadcrumbItem>
				<BreadcrumbSeparator />
				<BreadcrumbItem>
					<BreadcrumbLink asChild>
						<Link to={`/projects/${projectSlug}/teams/${teamSlug}`}>
							{teamName}
						</Link>
					</BreadcrumbLink>
				</BreadcrumbItem>
				{current && (
					<>
						<BreadcrumbSeparator />
						<BreadcrumbItem>
							<BreadcrumbPage>{current}</BreadcrumbPage>
						</BreadcrumbItem>
					</>
				)}
			</BreadcrumbList>
		</Breadcrumb>
	);
}

/**
 * Simple breadcrumb for pages like Projects List, Teams List, etc.
 * Pattern: Home > [Section] > [Current Page]
 */
export interface SimpleBreadcrumbProps extends CustomBreadcrumbProps {
	section: string;
	sectionHref?: string;
	current?: string;
}

export function SimpleBreadcrumb({
	section,
	sectionHref,
	current,
	className = "mb-6",
}: SimpleBreadcrumbProps) {
	return (
		<Breadcrumb className={className}>
			<BreadcrumbList>
				<BreadcrumbItem>
					<BreadcrumbLink asChild>
						<Link to="/">Home</Link>
					</BreadcrumbLink>
				</BreadcrumbItem>
				<BreadcrumbSeparator />
				<BreadcrumbItem>
					{sectionHref ? (
						<BreadcrumbLink asChild>
							<Link to={sectionHref}>{section}</Link>
						</BreadcrumbLink>
					) : (
						<BreadcrumbPage>{section}</BreadcrumbPage>
					)}
				</BreadcrumbItem>
				{current && (
					<>
						<BreadcrumbSeparator />
						<BreadcrumbItem>
							<BreadcrumbPage>{current}</BreadcrumbPage>
						</BreadcrumbItem>
					</>
				)}
			</BreadcrumbList>
		</Breadcrumb>
	);
}

/**
 * Profile breadcrumb component
 * Pattern: Home > Profile > [Current Page]
 */
export interface ProfileBreadcrumbProps extends CustomBreadcrumbProps {
	current?: string;
}

export function ProfileBreadcrumb({
	current,
	className = "mb-6",
}: ProfileBreadcrumbProps) {
	return (
		<Breadcrumb className={className}>
			<BreadcrumbList>
				<BreadcrumbItem>
					<BreadcrumbLink asChild>
						<Link to="/">Home</Link>
					</BreadcrumbLink>
				</BreadcrumbItem>
				<BreadcrumbSeparator />
				<BreadcrumbItem>
					<BreadcrumbLink asChild>
						<Link to="/profile">Profile</Link>
					</BreadcrumbLink>
				</BreadcrumbItem>
				{current && (
					<>
						<BreadcrumbSeparator />
						<BreadcrumbItem>
							<BreadcrumbPage>{current}</BreadcrumbPage>
						</BreadcrumbItem>
					</>
				)}
			</BreadcrumbList>
		</Breadcrumb>
	);
}

// Convenience exports for backward compatibility
export function ProjectsListBreadcrumb() {
	return <SimpleBreadcrumb section="Projects" />;
}

export function TeamsListBreadcrumb() {
	return <SimpleBreadcrumb section="Teams" />;
}

// ============================================================================
// LEGACY BREADCRUMB COMPONENT (For backward compatibility)
// ============================================================================

export interface BreadcrumbItem {
	label: string;
	href?: string;
	current?: boolean;
	onClick?: () => void;
}

export interface LegacyBreadcrumbProps {
	items: BreadcrumbItem[];
	showBackButton?: boolean;
	backButtonText?: string;
	backButtonHref?: string;
	onBackClick?: () => void;
	className?: string;
}

export function LegacyBreadcrumb({
	items,
	showBackButton = false,
	backButtonText = "Back",
	backButtonHref,
	onBackClick,
	className = "mb-6",
}: LegacyBreadcrumbProps) {
	return (
		<Breadcrumb className={className}>
			<BreadcrumbList>
				{items.map((item, index) => {
					const isLast = index === items.length - 1;

					return (
						<React.Fragment key={index}>
							{index > 0 && <BreadcrumbSeparator />}

							{item.current ? (
								<BreadcrumbItem>
									<BreadcrumbPage>{item.label}</BreadcrumbPage>
								</BreadcrumbItem>
							) : item.href ? (
								<BreadcrumbItem>
									<BreadcrumbLink asChild>
										<Link to={item.href}>{item.label}</Link>
									</BreadcrumbLink>
								</BreadcrumbItem>
							) : item.onClick ? (
								<BreadcrumbItem>
									<BreadcrumbLink asChild>
										<button onClick={item.onClick}>{item.label}</button>
									</BreadcrumbLink>
								</BreadcrumbItem>
							) : (
								<BreadcrumbItem>
									<BreadcrumbPage>{item.label}</BreadcrumbPage>
								</BreadcrumbItem>
							)}
						</React.Fragment>
					);
				})}
			</BreadcrumbList>
		</Breadcrumb>
	);
}

// Export all shadcn components for direct use when needed
export {
	Breadcrumb,
	BreadcrumbList,
	BreadcrumbItem,
	BreadcrumbLink,
	BreadcrumbPage,
	BreadcrumbSeparator,
	BreadcrumbEllipsis,
};
