import ReactDOM from "react-dom/client";
import App from "./App.tsx";
import "./index.css";
import { BrowserRouter } from "react-router-dom";
import "react-toastify/dist/ReactToastify.css";

// Match vite.config `base` (default "/"). API host is VITE_API_BASE_URL — not the router basename.
const routerBasename =
	import.meta.env.BASE_URL === "/"
		? undefined
		: import.meta.env.BASE_URL.replace(/\/$/, "");

ReactDOM.createRoot(document.getElementById("root")!).render(
	<BrowserRouter basename={routerBasename}>
		<App />
	</BrowserRouter>
);
