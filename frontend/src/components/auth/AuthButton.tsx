import { Button } from "@/components/ui/button";
import { Loader2 } from "lucide-react";
import { ComponentProps } from "react";

interface AuthButtonProps extends ComponentProps<typeof Button> {
	loading?: boolean;
}

export default function AuthButton({
	children,
	loading = false,
	variant = "default",
	className = "",
	...props
}: AuthButtonProps) {
	return (
		<Button
			variant={variant}
			className={`w-full ${className}`}
			disabled={loading}
			{...props}
		>
			{loading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
			{children}
		</Button>
	);
}
