import { router } from 'expo-router';
import { BookOpen, Sparkles } from 'lucide-react-native';
import { useEffect, useRef, useState } from 'react';
import { Animated, Easing, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { useAppTheme } from '@/components/ui/theme-provider';
import { apiClient } from '@/lib/api-client';
import { clearAuthToken, getAuthToken } from '@/lib/token-storage';
import { useAuthStore } from '@/store/auth-store';
import type { User } from '@/types';

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

export default function SplashScreen() {
  const { colors } = useAppTheme();
  const setUser = useAuthStore((state) => state.setUser);
  const [progress, setProgress] = useState(0);
  const [stageText, setStageText] = useState('Restoring scholar credentials...');
  const spin = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    const loop = Animated.loop(
      Animated.timing(spin, { toValue: 1, duration: 3000, easing: Easing.linear, useNativeDriver: true }),
    );
    loop.start();
    return () => loop.stop();
  }, [spin]);

  useEffect(() => {
    let cancelled = false;

    async function bootstrap() {
      setProgress(0.35);
      await sleep(400);
      if (cancelled) return;
      setStageText('Syncing research circles & papers...');
      setProgress(0.75);
      await sleep(400);
      if (cancelled) return;
      setStageText('Initializing AI Copilot engine...');

      let authenticated = false;
      const token = await getAuthToken();
      if (token) {
        try {
          const user = await apiClient.get<User>('/users/me');
          if (!cancelled) {
            setUser(user);
            authenticated = true;
          }
        } catch {
          await clearAuthToken();
        }
      }

      if (cancelled) return;
      setProgress(1);
      setStageText('Workspace ready!');
      await sleep(300);
      if (cancelled) return;

      if (!authenticated) {
        router.replace('/(auth)/onboarding');
      }
      // If authenticated, the root layout's Stack.Protected guard reacts to
      // setUser() above and swaps to (tabs) on its own — no navigation needed.
    }

    bootstrap();
    return () => {
      cancelled = true;
    };
  }, [setUser]);

  const rotate = spin.interpolate({ inputRange: [0, 1], outputRange: ['0deg', '360deg'] });

  return (
    <View className="flex-1 bg-academic-paper dark:bg-[#15151A]">
      <SafeAreaView className="flex-1 items-center justify-center gap-6 px-8">
        <View className="items-center justify-center">
          <Animated.View
            style={{ transform: [{ rotate }] }}
            className="h-36 w-36 items-center justify-center rounded-full border-4 border-academic-gold/25"
          >
            <View className="absolute h-3 w-3 rounded-full bg-academic-gold" style={{ top: 0 }} />
          </Animated.View>
          <View className="absolute h-20 w-20 items-center justify-center rounded-full bg-academic-navy dark:bg-[#5D82AE]">
            <BookOpen size={32} color="#FBF9F4" />
          </View>
        </View>

        <View className="items-center gap-1">
          <Text className="font-serif text-4xl italic text-academic-navy dark:text-[#5D82AE]">Cite-Circle</Text>
          <Text className="text-base text-academic-muted dark:text-[#A6A6AC]">Where ideas connect</Text>
        </View>

        <View className="w-full rounded-2xl bg-white p-5 shadow-sm dark:bg-[#1F1F26]">
          <View className="flex-row items-center justify-center gap-2">
            <Sparkles size={18} color={colors.accentSecondary} />
            <Text className="text-sm font-bold text-academic-ink dark:text-[#EFEAE0]">
              Prepping workspace {Math.round(progress * 100)}%
            </Text>
          </View>

          <View className="mt-3 h-2 w-full overflow-hidden rounded-full bg-academic-navy/10 dark:bg-[#5D82AE]/15">
            <View className="h-full rounded-full bg-academic-gold" style={{ width: `${progress * 100}%` }} />
          </View>

          <Text className="mt-3 text-center text-sm text-academic-muted dark:text-[#A6A6AC]">{stageText}</Text>
        </View>
      </SafeAreaView>
    </View>
  );
}
