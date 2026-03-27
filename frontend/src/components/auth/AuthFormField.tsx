import { Label } from "@/components/ui/label";
import { Alert, AlertDescription } from "@/components/ui/alert";

interface AuthFormFieldProps {
	label: string;
	error?: string;
	children: React.ReactNode;
	required?: boolean;
}

export default function AuthFormField({
	label,
	error,
	children,
	required = false,
}: AuthFormFieldProps) {
	return (
		<div className="space-y-2">
			<Label className="text-sm font-medium">
				{label}
				{required && <span className="text-red-500 ml-1">*</span>}
			</Label>
			{children}
			{error && (
				<Alert variant="destructive" className="py-2">
					<AlertDescription className="text-xs">{error}</AlertDescription>
				</Alert>
			)}
		</div>
	);
}
