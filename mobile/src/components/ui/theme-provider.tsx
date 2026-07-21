import { createContext, useContext, useMemo, useState, type PropsWithChildren } from 'react';
import { useColorScheme as useSystemColorScheme } from 'react-native';

import { Colors } from '@/constants/theme';

type ColorScheme = 'light' | 'dark';
type ThemePreference = ColorScheme | 'system';

type ThemeColors = { [K in keyof (typeof Colors)['light']]: string };

interface ThemeContextValue {
  colorScheme: ColorScheme;
  preference: ThemePreference;
  colors: ThemeColors;
  setPreference: (preference: ThemePreference) => void;
}

const ThemeContext = createContext<ThemeContextValue | null>(null);

export function AppThemeProvider({ children }: PropsWithChildren) {
  const systemScheme = useSystemColorScheme();
  const [preference, setPreference] = useState<ThemePreference>('system');

  const colorScheme: ColorScheme = preference === 'system' ? (systemScheme === 'dark' ? 'dark' : 'light') : preference;

  const value = useMemo<ThemeContextValue>(
    () => ({
      colorScheme,
      preference,
      colors: Colors[colorScheme],
      setPreference,
    }),
    [colorScheme, preference],
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useAppTheme() {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useAppTheme must be used within an AppThemeProvider');
  }
  return context;
}
