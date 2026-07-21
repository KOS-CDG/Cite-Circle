import type { LucideIcon } from 'lucide-react-native';
import { ActivityIndicator, Pressable, Text, type PressableProps } from 'react-native';

import { useAppTheme } from '@/components/ui/theme-provider';

interface ButtonProps extends Omit<PressableProps, 'children'> {
  label: string;
  variant?: 'primary' | 'secondary';
  isLoading?: boolean;
  icon?: LucideIcon;
  className?: string;
}

export function Button({ label, variant = 'primary', isLoading, icon: Icon, disabled, className = '', ...rest }: ButtonProps) {
  const { colors } = useAppTheme();
  const isDisabled = disabled || isLoading;

  const base = 'flex-row items-center justify-center gap-2 rounded-xl px-5 py-3.5';
  const styleByVariant =
    variant === 'primary'
      ? 'bg-academic-maroon dark:bg-[#C6635E]'
      : 'border border-academic-gold/40 bg-academic-parchment dark:border-academic-gold/30 dark:bg-[#1F1F26]';

  return (
    <Pressable
      accessibilityRole="button"
      disabled={isDisabled}
      className={`${base} ${styleByVariant} ${isDisabled ? 'opacity-50' : ''} ${className}`}
      {...rest}
    >
      {isLoading ? (
        <ActivityIndicator color={variant === 'primary' ? '#FBF9F4' : colors.accent} />
      ) : (
        <>
          {Icon ? <Icon size={18} color={variant === 'primary' ? '#FBF9F4' : colors.accent} /> : null}
          <Text
            className={
              variant === 'primary'
                ? 'text-base font-semibold text-academic-paper'
                : 'text-base font-semibold text-academic-ink dark:text-[#EFEAE0]'
            }
          >
            {label}
          </Text>
        </>
      )}
    </Pressable>
  );
}
