import { ScrollView, Text, View } from 'react-native';

import { highlight } from '@/lib/syntax-highlighter';
import { useAppTheme } from './theme-provider';

interface CodeBlockProps {
  code: string;
  language: string;
}

export function CodeBlock({ code, language }: CodeBlockProps) {
  const { colorScheme } = useAppTheme();
  const isDark = colorScheme === 'dark';
  const tokens = highlight(code, language, isDark);

  return (
    <View
      style={{
        borderRadius: 10,
        padding: 12,
        backgroundColor: isDark ? '#0E1524' : '#F3F1EC',
      }}
    >
      {language ? (
        <Text
          style={{
            fontSize: 9,
            fontWeight: '700',
            color: isDark ? '#6B7A99' : '#8A94A6',
            marginBottom: 6,
          }}
        >
          {language.toUpperCase()}
        </Text>
      ) : null}
      <ScrollView horizontal showsHorizontalScrollIndicator={false}>
        <Text style={{ fontFamily: 'monospace', fontSize: 12, lineHeight: 18 }}>
          {tokens.map((token, index) => (
            <Text
              key={index}
              style={{
                color: token.color,
                fontWeight: token.bold ? '700' : '400',
                fontStyle: token.italic ? 'italic' : 'normal',
              }}
            >
              {token.text}
            </Text>
          ))}
        </Text>
      </ScrollView>
    </View>
  );
}
