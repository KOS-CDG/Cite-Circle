/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{js,jsx,ts,tsx}'],
  presets: [require('nativewind/preset')],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        academic: {
          paper: '#FBF9F4',
          ink: '#1B1B1F',
          parchment: '#F2EDE0',
          maroon: '#7A2E2E',
          navy: '#1D3557',
          gold: '#B08D57',
          muted: '#6B6B6B',
        },
      },
      fontFamily: {
        serif: ['Georgia', 'Times New Roman', 'serif'],
      },
    },
  },
  plugins: [],
};
