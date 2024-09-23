/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        primary: {
          50: "#dadedb",
          100: "#ccd6d0",
          200: "#98b099",
          500: "#1f5f30",
        },
        gray: {
          50: "#eee",
          100: "#dadada",
          400: "#aaa",
          500: "#888",
          800: "#222",
        },
        bg: {
          50: "#fafafa",
          100: "#f4f6f5,",
        },
      },
    },
  },
  plugins: [],
};
