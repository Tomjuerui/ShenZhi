/** @type {import('tailwindcss').Config} */
module.exports = {
  darkMode: 'class',
  content: [
    './index.html',
    './src/**/*.{vue,js,ts,jsx,tsx}',
  ],
  theme: {
    extend: {
      colors: {
        ds: {
          bg: 'var(--ds-bg)',
          sidebar: 'var(--ds-bg-sidebar)',
          hover: 'var(--ds-bg-hover)',
          active: 'var(--ds-bg-active)',
          bubble: 'var(--ds-bg-bubble)',
          border: 'var(--ds-border)',
          text: 'var(--ds-text)',
          secondary: 'var(--ds-text-secondary)',
          muted: 'var(--ds-text-muted)',
          primary: 'var(--ds-primary)',
          'primary-hover': 'var(--ds-primary-hover)',
          'primary-soft': 'var(--ds-primary-soft)',
          'primary-border': 'var(--ds-primary-border)',
        },
      },
      borderRadius: {
        'ds-sm': 'var(--ds-radius-sm)',
        'ds-md': 'var(--ds-radius-md)',
        'ds-lg': 'var(--ds-radius-lg)',
        'ds-xl': 'var(--ds-radius-xl)',
      },
      maxWidth: {
        ds: 'var(--ds-content-max)',
      },
      fontSize: {
        'ds-greeting': ['28px', { lineHeight: '1.3', fontWeight: '700' }],
      },
      animation: {
        blink: 'blink 1.2s infinite steps(1, start)',
      },
      keyframes: {
        blink: {
          '0%, 100%': { 'background-color': 'currentColor' },
          '50%': { 'background-color': 'transparent' },
        },
      },
    },
  },
  plugins: [],
}
