import { Pressable, Text } from 'react-native';

interface ChipProps {
  label: string;
  selected: boolean;
  onPress: () => void;
}

export function Chip({ label, selected, onPress }: ChipProps) {
  return (
    <Pressable
      accessibilityRole="button"
      onPress={onPress}
      className={`items-center rounded-full border px-4 py-2.5 ${
        selected
          ? 'border-academic-maroon bg-academic-maroon dark:border-[#C6635E] dark:bg-[#C6635E]'
          : 'border-academic-gold/30 bg-academic-parchment dark:border-academic-gold/20 dark:bg-[#1F1F26]'
      }`}
    >
      <Text
        className={
          selected
            ? 'text-sm font-medium text-academic-paper'
            : 'text-sm font-medium text-academic-ink dark:text-[#EFEAE0]'
        }
      >
        {label}
      </Text>
    </Pressable>
  );
}
