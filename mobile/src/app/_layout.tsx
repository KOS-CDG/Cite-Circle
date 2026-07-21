import { useEffect } from 'react';
import { DarkTheme, DefaultTheme, Stack, ThemeProvider as NavigationThemeProvider } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { useColorScheme } from 'react-native';

import { AppThemeProvider } from '@/components/ui/theme-provider';
import { useAuthStore } from '@/store/auth-store';

SplashScreen.preventAutoHideAsync();

export default function RootLayout() {
  const colorScheme = useColorScheme();
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const needsProfileSetup = useAuthStore((state) => state.needsProfileSetup);
  const showTabs = isAuthenticated && !needsProfileSetup;

  useEffect(() => {
    SplashScreen.hideAsync().catch(() => {});
  }, []);

  return (
    <AppThemeProvider>
      <NavigationThemeProvider value={colorScheme === 'dark' ? DarkTheme : DefaultTheme}>
        <Stack screenOptions={{ headerShown: false }}>
          <Stack.Protected guard={!showTabs}>
            <Stack.Screen name="(auth)" />
          </Stack.Protected>
          <Stack.Protected guard={showTabs}>
            <Stack.Screen name="(tabs)" />
            <Stack.Screen name="circle/[id]" options={{ headerShown: true, title: '' }} />
            <Stack.Screen name="post/[id]" options={{ headerShown: true, title: 'Discussion' }} />
            <Stack.Screen name="paper/[id]" options={{ headerShown: true, title: 'Paper Details' }} />
            <Stack.Screen name="chat/[id]" options={{ headerShown: true, title: 'Chat' }} />
            <Stack.Screen name="profile/[id]" options={{ headerShown: true, title: 'Profile' }} />
            <Stack.Screen name="notifications" options={{ headerShown: true, title: 'Notifications' }} />
            <Stack.Screen name="network" options={{ headerShown: true, title: 'Network' }} />
          </Stack.Protected>
        </Stack>
      </NavigationThemeProvider>
    </AppThemeProvider>
  );
}

