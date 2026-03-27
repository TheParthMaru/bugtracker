/**
 * LoginPage Component
 *
 * This component handles user authentication in the BugTracker application.
 * It provides a professional form for users to enter their credentials with comprehensive validation.
 *
 * Key Features:
 * - Enhanced email and password validation using Zod schema
 * - Professional form state management with react-hook-form
 * - Comprehensive error handling with field-specific feedback
 * - Success modal with celebration and smooth redirect
 * - JWT token storage upon successful authentication
 * - Responsive design consistent with RegisterPage styling
 *
 * Authentication Flow:
 * 1. User enters credentials with real-time validation
 * 2. Frontend validates input comprehensively
 * 3. Credentials sent to /api/bugtracker/v1/auth/login
 * 4. On success: Success modal → JWT stored → redirect to home
 * 5. On failure: Field-specific errors + toast notifications
 *
 */

import { useForm } from "react-hook-form";
import { z } from "zod";
import { zodResolver } from "@hookform/resolvers/zod";
import { useNavigate, Link } from "react-router-dom";
import { toast } from "react-toastify";
import { useState, useEffect } from "react";

import { Input } from "@/components/ui/input";
import { Footer } from "@/components/ui/footer";
import { Button } from "@/components/ui/button";
import Navbar from "@/components/Navbar";
import API from "@/services/api";
import { API_BASE_URL } from "@/config/constants";

// Enhanced validation schema matching backend requirements
const loginSchema = z.object({
	email: z
		.string()
		.min(1, "Email is required")
		.email("Enter a valid email address"),
	password: z
		.string()
		.min(1, "Password is required")
		.min(8, "Password must be at least 8 characters"),
});

type LoginFormData = z.infer<typeof loginSchema>;

export default function LoginPage() {
	console.log("=== LoginPage component rendered ===");
	const navigate = useNavigate();
	const [loginSuccess, setLoginSuccess] = useState(false);
	const [hasError, setHasError] = useState(false);

	// Check if there's a stored token on component mount
	useEffect(() => {
		const token = localStorage.getItem("bugtracker_token");
		if (token) {
			navigate("/");
		}
	}, [navigate]);

	const {
		register,
		handleSubmit,
		setError,
		clearErrors,
		formState: { errors, isSubmitting },
	} = useForm<LoginFormData>({
		resolver: zodResolver(loginSchema),
		defaultValues: {
			email: "",
			password: "",
		},
		mode: "onSubmit",
	});

	console.log("=== React Hook Form initialized ===");
	console.log("handleSubmit function:", typeof handleSubmit);
	console.log("Current form errors:", errors);

	// Debug: Log validation errors
	useEffect(() => {
		if (Object.keys(errors).length > 0) {
			console.log("=== Form validation errors ===", errors);
		}
	}, [errors]);

	const onSubmit = async (data: LoginFormData) => {
		console.log("=== onSubmit function called ===");
		console.log("Form submitted with data:", {
			email: data.email,
			password: "***",
		});
		try {
			console.log("=== Making API call to backend ===");
			console.log("API URL:", `${API_BASE_URL}/auth/login`);
			console.log("Request data:", {
				email: data.email,
				password: "[REDACTED]",
			});
			const response = await API.post("/auth/login", data);

			// Extract token and daily login status from response
			const { token, dailyLoginAwarded } = response.data;
			localStorage.setItem("bugtracker_token", token);

			// Show daily login notification if awarded
			if (dailyLoginAwarded) {
				console.log("Daily login points awarded, showing notification");
				setTimeout(() => {
					// Import and use PointNotificationService
					import("@/services/pointNotificationService").then(
						({ PointNotificationService }) => {
							PointNotificationService.showDailyLogin();
						}
					);
				}, 1000);
			} else {
				console.log("No daily login points awarded today");
			}

			// Show success modal instead of immediate redirect
			setLoginSuccess(true);

			// Delay redirect to show success message
			setTimeout(() => {
				console.log("Redirecting to / after successful login");
				navigate("/");
			}, 2000);
		} catch (error: any) {
			// Reset login success state if error occurs
			setLoginSuccess(false);
			setHasError(true);

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

			if (error.response?.status === 401) {
				// Handle authentication errors - user not found or wrong password
				const errorMessage = error.response?.data;

				if (errorMessage === "User not found") {
					toast.error("User not found. Please check your credentials.");
					setError("email", {
						type: "server",
						message: "No account found with this email address",
					});
				} else if (errorMessage === "Invalid password") {
					toast.error("Invalid password. Please check your credentials.");
					setError("password", {
						type: "server",
						message: "Password is incorrect",
					});
				} else {
					// Fallback for other 401 errors
					toast.error(
						errorMessage ||
							"Authentication failed. Please check your credentials."
					);
					setError("email", {
						type: "server",
						message: "Please check your email address",
					});
					setError("password", {
						type: "server",
						message: "Please check your password",
					});
				}
			} else if (
				error.response?.status === 400 &&
				error.response?.data?.errors
			) {
				// Handle backend validation errors - set field-specific errors
				const backendErrors = error.response.data.errors;
				Object.entries(backendErrors).forEach(([field, message]) => {
					const formFieldName = field as keyof LoginFormData;
					if (["email", "password"].includes(field)) {
						setError(formFieldName, {
							type: "server",
							message: message as string,
						});
					} else {
						toast.error(message as string);
					}
				});
			} else if (error.response?.status === 404) {
				toast.error(
					"Login endpoint not found. Please check server configuration."
				);
			} else if (error.response?.status === 500) {
				toast.error("Server error. Please try again later.");
			} else if (error.response?.data?.message) {
				toast.error(error.response.data.message);
			} else if (
				error.response?.data &&
				typeof error.response.data === "string"
			) {
				toast.error(error.response.data);
			} else {
				toast.error("Login failed. Please try again.");
			}
		}
	};

	return (
		<>
			<Navbar />

			{/* Success Modal Overlay */}
			{loginSuccess && (
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
							Welcome Back! 🎉
						</h3>
						<p className="text-gray-600 text-sm mb-4">
							You have successfully logged into BugTracker.
						</p>

						{/* Loading indicator */}
						<div className="flex items-center justify-center space-x-2 text-sm text-gray-500">
							<div className="animate-spin rounded-full h-4 w-4 border-b-2 border-primary"></div>
							<span>Taking you to your dashboard...</span>
						</div>
					</div>
				</div>
			)}

			<div className="flex-1 flex justify-center items-center bg-gray-100 py-12 px-4 sm:px-6 lg:px-8">
				<form
					onSubmit={handleSubmit(onSubmit)}
					className="bg-white p-8 shadow-md rounded w-full max-w-md space-y-6"
					autoComplete="off"
				>
					<div className="text-center">
						<h2 className="text-2xl font-semibold">Welcome Back</h2>
						<p className="text-muted-foreground mt-1">
							Sign in to your BugTracker account
						</p>
					</div>

					{/* Email */}
					<div>
						<label className="block mb-1 text-sm font-medium">
							Email Address
						</label>
						<Input
							type="email"
							placeholder="Enter your email"
							{...register("email", {
								onChange: () => {
									if (hasError) {
										clearErrors("email");
										setHasError(false);
									}
								},
							})}
						/>
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
							placeholder="Enter your password"
							{...register("password", {
								onChange: () => {
									if (hasError) {
										clearErrors("password");
										setHasError(false);
									}
								},
							})}
						/>
						{errors.password && (
							<p className="text-red-500 text-sm mt-1">
								{errors.password.message}
							</p>
						)}
					</div>

					<Button
						type="submit"
						disabled={isSubmitting || loginSuccess}
						className="w-full"
					>
						{loginSuccess
							? "Login Successful!"
							: isSubmitting
							? "Signing in..."
							: "Sign In"}
					</Button>

					<div className="text-center text-sm">
						<span className="text-muted-foreground">
							Don't have an account?{" "}
						</span>
						<Link
							to="/auth/register"
							className="text-primary hover:underline font-medium"
						>
							Create account
						</Link>
						<div className="mt-2">
							<Link
								to="/auth/forgot-password"
								className="text-primary hover:underline text-sm"
							>
								Forgot your password?
							</Link>
						</div>
					</div>
				</form>
			</div>
			<Footer />
		</>
	);
}
