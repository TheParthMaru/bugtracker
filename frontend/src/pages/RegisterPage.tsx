/**
 * RegisterPage Component
 *
 * This component handles user registration in the BugTracker application.
 * It provides a comprehensive form for users to create their accounts with role selection.
 *
 * Key Features:
 * - Complete form validation using Zod schema matching backend requirements
 * - Form state management with react-hook-form
 * - Password strength validation and confirmation matching
 * - Role selection with clear descriptions
 * - Skills input as optional comma-separated values
 * - Error handling and user feedback via toast notifications
 * - Responsive design consistent with LoginPage styling
 *
 * Registration Flow:
 * 1. User fills in registration form with all required fields
 * 2. Frontend validates input including password strength and confirmation
 * 3. Data sent to /api/bugtracker/v1/auth/register
 * 4. On success: User redirected to login page with success message
 * 5. On failure: Specific error messages displayed to user
 *
 */

import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useNavigate, Link } from "react-router-dom";
import { toast } from "react-toastify";
import { useState } from "react";

import { Input } from "@/components/ui/input";
import { Footer } from "@/components/ui/footer";
import { Button } from "@/components/ui/button";
import Navbar from "@/components/Navbar";
import API from "@/services/api";

// Validation schema matching backend requirements
const registerSchema = z
	.object({
		firstName: z.string().min(1, "First name is required").trim(),
		lastName: z.string().min(1, "Last name is required").trim(),
		email: z
			.string()
			.min(1, "Email is required")
			.email("Enter a valid email address")
			.regex(
				/^[A-Za-z0-9][A-Za-z0-9._-]*[A-Za-z0-9]@[A-Za-z0-9][A-Za-z0-9.-]*[A-Za-z0-9]\.[A-Za-z]{2,}$/,
				"Email must be a valid format with proper domain (e.g., user@example.com)"
			),
		password: z
			.string()
			.min(8, "Password must be at least 8 characters long")
			.regex(
				/.*[A-Z].*/,
				"Password must contain at least one uppercase letter (A-Z)"
			)
			.regex(
				/.*[a-z].*/,
				"Password must contain at least one lowercase letter (a-z)"
			)
			.regex(/.*\d.*/, "Password must contain at least one digit (0-9)")
			.regex(
				/.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?].*/,
				"Password must contain at least one special character"
			),
		confirmPassword: z.string().min(1, "Password confirmation is required"),
		role: z.enum(["ADMIN", "DEVELOPER", "REPORTER"], {
			required_error:
				"Role is required. Valid roles are: ADMIN, DEVELOPER, REPORTER",
		}),
		skills: z.string().optional(),
	})
	.refine((data) => data.password === data.confirmPassword, {
		message: "Passwords do not match",
		path: ["confirmPassword"],
	});

type RegisterFormData = z.infer<typeof registerSchema>;

export default function RegisterPage() {
	const navigate = useNavigate();
	const [showPasswordRequirements, setShowPasswordRequirements] =
		useState(false);
	const [registrationSuccess, setRegistrationSuccess] = useState(false);

	const {
		register,
		handleSubmit,
		watch,
		setError,
		formState: { errors, isSubmitting },
	} = useForm<RegisterFormData>({
		resolver: zodResolver(registerSchema),
		defaultValues: {
			firstName: "",
			lastName: "",
			email: "",
			password: "",
			confirmPassword: "",
			role: "DEVELOPER" as const,
			skills: "",
		},
	});

	const password = watch("password");

	const onSubmit = async (data: RegisterFormData) => {
		try {
			// Transform skills string to array if provided
			const skillsString = data.skills?.trim() || "";
			const skillsArray = skillsString
				? skillsString
						.split(",")
						.map((skill) => skill.trim())
						.filter(Boolean)
				: [];

			const payload = {
				...data,
				skills: skillsArray.length > 0 ? skillsArray : undefined,
			};

			await API.post("/auth/register", payload);

			// Show success modal instead of immediate redirect
			setRegistrationSuccess(true);

			// Delay redirect to show success message
			setTimeout(() => {
				navigate("/auth/login");
			}, 2500);
		} catch (error: any) {
			console.log("Registration error:", error);

			// Reset registration success state if error occurs
			setRegistrationSuccess(false);

			// Handle network errors (backend not available)
			if (
				error.code === "ERR_NETWORK" ||
				error.message?.includes("Network Error")
			) {
				toast.error(
					"Unable to connect to the server. Please check if the backend is running."
				);
				return;
			}

			// Handle timeout errors
			if (error.code === "ECONNABORTED" || error.message?.includes("timeout")) {
				toast.error("Request timed out. Please try again.");
				return;
			}

			if (error.response?.status === 409) {
				// Handle user already exists - backend sends plain string "User already exists"
				const message =
					error.response?.data || "User already exists with this email address";
				toast.error(message);
				// Also set error on email field for better UX
				setError("email", {
					type: "server",
					message:
						"This email is already registered. Please use a different email or try logging in.",
				});
			} else if (
				error.response?.status === 400 &&
				error.response?.data?.errors
			) {
				// Handle backend validation errors - set field-specific errors
				const backendErrors = error.response.data.errors;
				Object.entries(backendErrors).forEach(([field, message]) => {
					// Map backend field names to form field names
					const formFieldName = field as keyof RegisterFormData;
					if (
						formFieldName in errors ||
						[
							"firstName",
							"lastName",
							"email",
							"password",
							"confirmPassword",
							"role",
							"skills",
						].includes(field)
					) {
						setError(formFieldName, {
							type: "server",
							message: message as string,
						});
					} else {
						// For general errors (like @PasswordsMatch class-level validation)
						toast.error(message as string);
					}
				});
			} else if (error.response?.status === 404) {
				toast.error(
					"Registration endpoint not found. Please check server configuration."
				);
			} else if (error.response?.status === 500) {
				toast.error("Server error. Please try again later.");
			} else if (error.response?.data?.message) {
				toast.error(error.response.data.message);
			} else if (
				error.response?.data &&
				typeof error.response.data === "string"
			) {
				// Handle cases where backend sends plain string responses
				toast.error(error.response.data);
			} else {
				toast.error("Registration failed. Please try again.");
			}
		}
	};

	return (
		<>
			<Navbar />

			{/* Success Modal Overlay */}
			{registrationSuccess && (
				<div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
					<div className="bg-white rounded-lg p-8 max-w-sm mx-4 text-center shadow-xl">
						{/* Success Icon */}
						<div className="w-16 h-16 mx-auto mb-4 bg-green-100 rounded-full flex items-center justify-center">
							<svg
								className="w-8 h-8 text-green-600"
								fill="none"
								stroke="currentColor"
								viewBox="0 0 24 24"
							>
								<path
									strokeLinecap="round"
									strokeLinejoin="round"
									strokeWidth={2}
									d="M5 13l4 4L19 7"
								/>
							</svg>
						</div>

						{/* Success Message */}
						<h3 className="text-lg font-semibold text-gray-900 mb-2">
							Account Created Successfully
						</h3>
						<p className="text-gray-600 text-sm mb-4">
							Welcome to BugTracker! You can now log in with your credentials.
						</p>

						{/* Loading indicator */}
						<div className="flex items-center justify-center space-x-2 text-sm text-gray-500">
							<div className="animate-spin rounded-full h-4 w-4 border-b-2 border-primary"></div>
							<span>Redirecting to login...</span>
						</div>
					</div>
				</div>
			)}

			<div className="flex-1 flex justify-center items-center bg-gray-100 py-12 px-4 sm:px-6 lg:px-8">
				<form
					onSubmit={handleSubmit(onSubmit)}
					className="bg-white p-8 shadow-md rounded w-full max-w-lg space-y-6"
				>
					<div className="text-center">
						<h2 className="text-2xl font-semibold">Create Account</h2>
						<p className="text-muted-foreground mt-1">
							Join BugTracker to start managing issues
						</p>
					</div>

					{/* Name Fields */}
					<div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
						<div>
							<label className="block mb-1 text-sm font-medium">
								First Name
							</label>
							<Input type="text" {...register("firstName")} />
							{errors.firstName && (
								<p className="text-red-500 text-sm mt-1">
									{errors.firstName.message}
								</p>
							)}
						</div>
						<div>
							<label className="block mb-1 text-sm font-medium">
								Last Name
							</label>
							<Input type="text" {...register("lastName")} />
							{errors.lastName && (
								<p className="text-red-500 text-sm mt-1">
									{errors.lastName.message}
								</p>
							)}
						</div>
					</div>

					{/* Email */}
					<div>
						<label className="block mb-1 text-sm font-medium">
							Email Address
						</label>
						<Input type="email" {...register("email")} />
						{errors.email && (
							<p className="text-red-500 text-sm mt-1">
								{errors.email.message}
							</p>
						)}
					</div>

					{/* Password */}
					<div>
						<label className="block mb-1 text-sm font-medium">Password</label>
						<Input
							type="password"
							{...register("password")}
							onFocus={() => setShowPasswordRequirements(true)}
							onBlur={() => setShowPasswordRequirements(false)}
						/>
						{errors.password && (
							<p className="text-red-500 text-sm mt-1">
								{errors.password.message}
							</p>
						)}
						{showPasswordRequirements && (
							<div className="mt-2 p-3 bg-gray-50 rounded-md text-sm">
								<p className="font-medium mb-1">Password Requirements:</p>
								<ul className="space-y-1 text-muted-foreground">
									<li className={password?.length >= 8 ? "text-green-600" : ""}>
										✓ At least 8 characters
									</li>
									<li
										className={
											/.*[A-Z].*/.test(password || "") ? "text-green-600" : ""
										}
									>
										✓ One uppercase letter
									</li>
									<li
										className={
											/.*[a-z].*/.test(password || "") ? "text-green-600" : ""
										}
									>
										✓ One lowercase letter
									</li>
									<li
										className={
											/.*\d.*/.test(password || "") ? "text-green-600" : ""
										}
									>
										✓ One number
									</li>
									<li
										className={
											/.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?].*/.test(
												password || ""
											)
												? "text-green-600"
												: ""
										}
									>
										✓ One special character
									</li>
								</ul>
							</div>
						)}
					</div>

					{/* Confirm Password */}
					<div>
						<label className="block mb-1 text-sm font-medium">
							Confirm Password
						</label>
						<Input type="password" {...register("confirmPassword")} />
						{errors.confirmPassword && (
							<p className="text-red-500 text-sm mt-1">
								{errors.confirmPassword.message}
							</p>
						)}
					</div>

					{/* Role Selection */}
					<div>
						<label className="block mb-1 text-sm font-medium">Role</label>
						<select
							{...register("role")}
							className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-base shadow-xs transition-colors file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50 md:text-sm"
						>
							<option value="DEVELOPER">
								Developer - Create and manage bug reports, assign tasks, view
								analytics
							</option>
							<option value="ADMIN">
								Admin - Full system access, manage users, projects, and settings
							</option>
							<option value="REPORTER">
								Reporter - Report bugs and issues, track status updates
							</option>
						</select>
						{errors.role && (
							<p className="text-red-500 text-sm mt-1">{errors.role.message}</p>
						)}
					</div>

					{/* Skills (Optional) */}
					<div>
						<label className="block mb-1 text-sm font-medium">
							Skills <span className="text-muted-foreground">(optional)</span>
						</label>
						<Input
							type="text"
							placeholder="e.g., JavaScript, Python, Testing (comma-separated)"
							{...register("skills")}
						/>
						<p className="text-xs text-muted-foreground mt-1">
							Enter your technical skills separated by commas
						</p>
					</div>

					<Button
						type="submit"
						disabled={isSubmitting || registrationSuccess}
						className="w-full"
					>
						{registrationSuccess
							? "Registration Successful!"
							: isSubmitting
							? "Creating Account..."
							: "Create Account"}
					</Button>

					<div className="text-center text-sm">
						<span className="text-muted-foreground">
							Already have an account?{" "}
						</span>
						<Link
							to="/auth/login"
							className="text-primary hover:underline font-medium"
						>
							Sign in
						</Link>
					</div>
				</form>
			</div>

			<Footer />
		</>
	);
}
