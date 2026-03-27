import { useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Input } from "@/components/ui/input";
import API from "@/services/api";
import AuthLayout from "@/components/auth/AuthLayout";
import AuthFormField from "@/components/auth/AuthFormField";
import AuthButton from "@/components/auth/AuthButton";

export default function ResetPasswordPage() {
	const [searchParams] = useSearchParams();
	const token = searchParams.get("token");
	const [newPassword, setNewPassword] = useState("");
	const [confirmPassword, setConfirmPassword] = useState("");
	const [isSubmitting, setIsSubmitting] = useState(false);
	const navigate = useNavigate();

	const handleSubmit = async (e: React.FormEvent) => {
		e.preventDefault();

		if (newPassword !== confirmPassword) {
			alert("Passwords don't match");
			return;
		}

		if (newPassword.length < 8) {
			alert("Password must be at least 8 characters");
			return;
		}

		setIsSubmitting(true);

		try {
			await API.post("/auth/reset-password", { token, newPassword });
			alert("Password reset successful!");
			navigate("/auth/login");
		} catch (error) {
			alert("Failed to reset password");
		} finally {
			setIsSubmitting(false);
		}
	};

	if (!token) {
		return (
			<AuthLayout
				title="Invalid Reset Link"
				subtitle="This reset link is invalid or has expired"
				showNavbar={true}
				showFooter={true}
			>
				<div className="text-center space-y-4">
					<div className="w-16 h-16 mx-auto bg-red-100 rounded-full flex items-center justify-center">
						<svg
							className="w-8 h-8 text-red-600"
							fill="none"
							stroke="currentColor"
							viewBox="0 0 24 24"
						>
							<path
								strokeLinecap="round"
								strokeLinejoin="round"
								strokeWidth={2}
								d="M6 18L18 6M6 6l12 12"
							/>
						</svg>
					</div>
					<p className="text-muted-foreground">
						Please request a new password reset link
					</p>
					<AuthButton onClick={() => navigate("/auth/forgot-password")}>
						Request New Reset Link
					</AuthButton>
				</div>
			</AuthLayout>
		);
	}

	return (
		<AuthLayout
			title="Reset Password"
			subtitle="Enter your new password below"
			showNavbar={true}
			showFooter={true}
		>
			<form onSubmit={handleSubmit} className="space-y-4">
				<AuthFormField label="New Password" required>
					<Input
						type="password"
						placeholder="Enter new password"
						value={newPassword}
						onChange={(e) => setNewPassword(e.target.value)}
						required
						minLength={8}
					/>
				</AuthFormField>

				<AuthFormField label="Confirm Password" required>
					<Input
						type="password"
						placeholder="Confirm new password"
						value={confirmPassword}
						onChange={(e) => setConfirmPassword(e.target.value)}
						required
					/>
				</AuthFormField>

				<AuthButton type="submit" loading={isSubmitting}>
					{isSubmitting ? "Resetting..." : "Reset Password"}
				</AuthButton>
			</form>
		</AuthLayout>
	);
}
