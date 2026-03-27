import Navbar from "@/components/Navbar";
import { Footer } from "@/components/ui/footer";

interface AuthLayoutProps {
	children: React.ReactNode;
	title: string;
	subtitle?: string;
	showNavbar?: boolean;
	showFooter?: boolean;
}

export default function AuthLayout({
	children,
	title,
	subtitle,
	showNavbar = true,
	showFooter = true,
}: AuthLayoutProps) {
	return (
		<>
			{showNavbar && <Navbar />}
			<div className="flex-1 flex justify-center items-center bg-gray-100 py-12 px-4 sm:px-6 lg:px-8">
				<div className="bg-white p-8 shadow-md rounded w-full max-w-lg space-y-6">
					<div className="text-center">
						<h2 className="text-2xl font-semibold">{title}</h2>
						{subtitle && (
							<p className="text-muted-foreground mt-1">{subtitle}</p>
						)}
					</div>
					{children}
				</div>
			</div>
			{showFooter && <Footer />}
		</>
	);
}
