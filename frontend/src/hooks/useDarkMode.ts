import { useState, useEffect } from 'react';

export type Theme = 'light' | 'dark';

export const useDarkMode = () => {
  const [theme, setTheme] = useState<Theme>(() => {
    // Get theme from localStorage or system preference
    const stored = localStorage.getItem('theme') as Theme | null;
    if (stored) return stored;

    // Check system preference
    if (window.matchMedia('(prefers-color-scheme: dark)').matches) {
      return 'dark';
    }
    return 'light';
  });

  useEffect(() => {
    const root = window.document.documentElement;

    // Remove both classes first
    root.classList.remove('light', 'dark');

    // Add current theme
    root.classList.add(theme);

    // Save to localStorage
    localStorage.setItem('theme', theme);
  }, [theme]);

  const toggleTheme = () => {
    setTheme((prev) => (prev === 'light' ? 'dark' : 'light'));
  };

  const setLightTheme = () => setTheme('light');
  const setDarkTheme = () => setTheme('dark');

  return {
    theme,
    isDark: theme === 'dark',
    toggleTheme,
    setLightTheme,
    setDarkTheme,
  };
};
