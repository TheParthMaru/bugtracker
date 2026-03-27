import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Input } from "@/components/ui/input";
import API from "@/services/api";
import AuthLayout from "@/components/auth/AuthLayout";
import AuthFormField from "@/components/auth/AuthFormField";
import AuthButton from "@/components/auth/AuthButton";

export default function ForgotPasswordPage() {
	const [email, setEmail] = useState("");
	const [isSubmitting, setIsSubmitting] = useState(false);
	const [isSuccess, setIsSuccess] = useState(false);
	const navigate = useNavigate();

	const handleSubmit = async (e: React.FormEvent) => {
		e.preventDefault();
		setIsSubmitting(true);

		try {
			await API.post("/auth/forgot-password", { email });
			setIsSuccess(true);
		} catch (error) {
			alert("Failed to send reset email");
		} finally {
			setIsSubmitting(false);
		}
	};

	if (isSuccess) {
		return (
			<AuthLayout
				title="Check Your Email"
				subtitle="We've sent a password reset link to your email"
				showNavbar={true}
				showFooter={true}
			>
				<div className="text-center space-y-4">
					<div className="w-16 h-16 mx-auto bg-green-100 rounded-full flex items-center justify-center">
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
					<p className="text-muted-foreground">
						Check your email for the reset link
					</p>
					<AuthButton onClick={() => navigate("/auth/login")}>
						Back to Login
					</AuthButton>
				</div>
			</AuthLayout>
		);
	}

	return (
		<AuthLayout
			title="Forgot Password"
			subtitle="Enter your email to receive a password reset link"
			showNavbar={true}
			showFooter={true}
		>
			<form onSubmit={handleSubmit} className="space-y-4">
				<AuthFormField label="Email Address" required>
					<Input
						type="email"
						placeholder="Enter your email"
						value={email}
						onChange={(e) => setEmail(e.target.value)}
						required
					/>
				</AuthFormField>

				<AuthButton type="submit" loading={isSubmitting}>
					{isSubmitting ? "Sending..." : "Send Reset Link"}
				</AuthButton>

				<AuthButton
					variant="outline"
					onClick={() => navigate("/auth/login")}
					type="button"
				>
					Back to Login
				</AuthButton>
			</form>
		</AuthLayout>
	);
}
