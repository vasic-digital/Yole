// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2025 Milos Vasic

import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./app/**/*.{ts,tsx,md,mdx}",
    "./components/**/*.{ts,tsx}",
  ],
  darkMode: "class",
  theme: {
    extend: {
      colors: {
        primary: {
          50: "#e3f2fd",
          100: "#bbdefb",
          200: "#90caf9",
          300: "#64b5f6",
          400: "#42a5f5",
          500: "#1976d2",
          600: "#1565c0",
          700: "#0d47a1",
          800: "#0a3d8f",
          900: "#07306e",
        },
      },
      fontFamily: {
        sans: ["Inter", "system-ui", "sans-serif"],
        mono: ["JetBrains Mono", "Fira Code", "monospace"],
      },
    },
  },
  plugins: [],
};

export default config;
