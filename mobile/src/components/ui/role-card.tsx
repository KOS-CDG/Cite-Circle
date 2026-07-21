import { Pressable, Text, View } from 'react-native';

import type { UserRole } from '@/types';

interface RoleCardProps {
  role: UserRole;
  title: string;
  description: string;
  emoji: string;
  isSelected: boolean;
  onPress: () => void;
}

export function RoleCard({ title, description, emoji, isSelected, onPress }: RoleCardProps) {
  return (
    <Pressable
      accessibilityRole="button"
      onPress={onPress}
      className={`w-full flex-row items-center gap-4 rounded-2xl border p-4 ${
        isSelected
          ? 'border-academic-maroon bg-academic-maroon/10 dark:border-[#C6635E] dark:bg-[#C6635E]/10'
          : 'border-academic-gold/30 bg-white dark:border-academic-gold/20 dark:bg-[#1F1F26]'
      }`}
    >
      <Text className="text-4xl">{emoji}</Text>
      <View className="flex-1">
        <Text className="text-base font-bold text-academic-ink dark:text-[#EFEAE0]">{title}</Text>
        <Text className="text-sm text-academic-muted dark:text-[#A6A6AC]">{description}</Text>
      </View>
    </Pressable>
  );
}
