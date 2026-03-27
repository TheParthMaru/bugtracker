module.exports = {
	root: true,
	env: {
		browser: true,
		es2021: true,
		node: true,
	},
	extends: [
		"eslint:recommended",
		"typescript-eslint",
		"plugin:react/recommended",
		"plugin:react-hooks/recommended",
	],
	parser: "@typescript-eslint/parser",
	parserOptions: {
		ecmaFeatures: {
			jsx: true,
		},
		ecmaVersion: "latest",
		sourceType: "module",
	},
	plugins: ["react", "react-hooks"],
	rules: {
		// React Hooks rules
		"react-hooks/rules-of-hooks": "error",
		"react-hooks/exhaustive-deps": "warn",

		// Function ordering rules
		"no-use-before-define": [
			"error",
			{
				functions: true,
				classes: true,
				variables: true,
			},
		],

		// React specific rules
		"react/react-in-jsx-scope": "off", // Not needed in React 17+
		"react/prop-types": "off", // Using TypeScript instead

		// General rules
		"no-console": "warn",
		"prefer-const": "error",
		"no-var": "error",
	},
	settings: {
		react: {
			version: "detect",
		},
	},
};
