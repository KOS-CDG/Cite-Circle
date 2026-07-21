import { router } from 'expo-router';
import { useState } from 'react';
import { Pressable, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Button } from '@/components/ui/button';

interface OnboardingPage {
  title: string;
  description: string;
  emoji: string;
}

const onboardingPages: OnboardingPage[] = [
  {
    title: 'Connect with researchers worldwide',
    description: 'Find colleagues, track publications, and grow your academic network without institutional silos.',
    emoji: '🌐',
  },
  {
    title: 'Discuss ideas in Circles',
    description: 'Join field-specific communities. Ask questions, get feedback on drafts, and share resources.',
    emoji: '💬',
  },
  {
    title: 'Publish with an AI pre-reviewer',
    description: 'Upload drafts to get instant structural feedback, citation scans, and quality scores before peer review.',
    emoji: '🤖',
  },
];

function goToLogin() {
  router.replace('/(auth)/login');
}

export default function OnboardingScreen() {
  const [currentPage, setCurrentPage] = useState(0);
  const page = onboardingPages[currentPage];
  const isLastPage = currentPage === onboardingPages.length - 1;

  return (
    <View className="flex-1 bg-academic-paper dark:bg-[#15151A]">
      <SafeAreaView className="flex-1 px-6 py-4">
        <View className="h-9 flex-row justify-end">
          {!isLastPage ? (
            <Pressable onPress={goToLogin} className="px-2 py-2">
              <Text className="text-base font-medium text-academic-muted dark:text-[#A6A6AC]">Skip</Text>
            </Pressable>
          ) : null}
        </View>

        <View className="flex-1 items-center justify-center gap-6">
          <View className="h-40 w-40 items-center justify-center rounded-full bg-academic-parchment dark:bg-[#1F1F26]">
            <Text style={{ fontSize: 80 }}>{page.emoji}</Text>
          </View>

          <View className="items-center gap-3 px-2">
            <Text className="text-center font-serif text-3xl text-academic-ink dark:text-[#EFEAE0]">{page.title}</Text>
            <Text className="text-center text-base text-academic-muted dark:text-[#A6A6AC]">{page.description}</Text>
          </View>
        </View>

        <View className="flex-row items-center justify-center gap-2 pb-6">
          {onboardingPages.map((item, index) => (
            <View
              key={item.title}
              className={`h-2 rounded-full ${
                index === currentPage ? 'w-6 bg-academic-gold' : 'w-2 bg-academic-muted/40'
              }`}
            />
          ))}
        </View>

        <Button
          label={isLastPage ? 'Get Started' : 'Next'}
          onPress={() => (isLastPage ? goToLogin() : setCurrentPage((page) => page + 1))}
        />
      </SafeAreaView>
    </View>
  );
}
