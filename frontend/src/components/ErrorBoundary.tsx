import React, { Component, ErrorInfo, ReactNode } from "react";
import { AlertTriangle, RefreshCw } from "lucide-react";
import { Button } from "./ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "./ui/card";

interface Props {
	children: ReactNode;
	fallback?: ReactNode;
}

interface State {
	hasError: boolean;
	error?: Error;
	errorInfo?: ErrorInfo;
}

export class ErrorBoundary extends Component<Props, State> {
	constructor(props: Props) {
		super(props);
		this.state = { hasError: false };
	}

	static getDerivedStateFromError(error: Error): State {
		return { hasError: true, error };
	}

	componentDidCatch(error: Error, errorInfo: ErrorInfo) {
		console.error("ErrorBoundary caught an error:", error, errorInfo);
		this.setState({ error, errorInfo });
	}

	handleRetry = () => {
		this.setState({ hasError: false, error: undefined, errorInfo: undefined });
	};

	render() {
		if (this.state.hasError) {
			if (this.props.fallback) {
				return this.props.fallback;
			}

			return (
				<div className="min-h-screen flex items-center justify-center p-4">
					<Card className="w-full max-w-md">
						<CardHeader className="text-center">
							<div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-red-100">
								<AlertTriangle className="h-6 w-6 text-red-600" />
							</div>
							<CardTitle className="text-lg font-semibold text-gray-900">
								Something went wrong
							</CardTitle>
						</CardHeader>
						<CardContent className="text-center">
							<p className="text-sm text-gray-600 mb-4">
								An unexpected error occurred. Please try refreshing the page.
							</p>
							{process.env.NODE_ENV === "development" && this.state.error && (
								<details className="mb-4 text-left">
									<summary className="cursor-pointer text-sm font-medium text-gray-700 mb-2">
										Error Details (Development)
									</summary>
									<pre className="text-xs text-red-600 bg-red-50 p-2 rounded overflow-auto max-h-32">
										{this.state.error.toString()}
										{this.state.errorInfo?.componentStack}
									</pre>
								</details>
							)}
							<div className="flex gap-2 justify-center">
								<Button onClick={this.handleRetry} variant="outline">
									<RefreshCw className="h-4 w-4 mr-2" />
									Try Again
								</Button>
								<Button onClick={() => window.location.reload()}>
									Refresh Page
								</Button>
							</div>
						</CardContent>
					</Card>
				</div>
			);
		}

		return this.props.children;
	}
}
