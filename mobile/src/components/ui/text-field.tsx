import { Text, TextInput, View, type TextInputProps } from 'react-native';

import { useAppTheme } from '@/components/ui/theme-provider';

interface TextFieldProps extends TextInputProps {
  label: string;
  errorMessage?: string | null;
}

export function TextField({ label, errorMessage, className = '', ...rest }: TextFieldProps) {
  const { colors } = useAppTheme();

  return (
    <View className="w-full gap-1.5">
      <Text className="text-sm font-medium text-academic-ink dark:text-[#EFEAE0]">{label}</Text>
      <TextInput
        placeholderTextColor={colors.textSecondary}
        className={`w-full rounded-xl border px-4 py-3 text-base text-academic-ink dark:text-[#EFEAE0] ${
          errorMessage ? 'border-red-500' : 'border-academic-gold/30 dark:border-academic-gold/20'
        } bg-academic-parchment dark:bg-[#1F1F26] ${className}`}
        {...rest}
      />
      {errorMessage ? <Text className="text-sm text-red-500">{errorMessage}</Text> : null}
    </View>
  );
}
