import { router } from 'expo-router';
import { Text, type TextStyle } from 'react-native';

import { parseInlineSegments } from '@/lib/scholarly-content';

interface InlineProseProps {
  text: string;
  style?: TextStyle;
  color: string;
}

/** Renders one prose run, styling `inline code` and @paper:id citation tags. */
export function InlineProse({ text, style, color }: InlineProseProps) {
  const segments = parseInlineSegments(text);

  return (
    <Text style={[{ color }, style]}>
      {segments.map((segment, index) => {
        if (segment.type === 'code') {
          return (
            <Text
              key={index}
              style={{ fontFamily: 'monospace', fontSize: 13, backgroundColor: `${color}14` }}
            >
              {segment.text}
            </Text>
          );
        }
        if (segment.type === 'citation') {
          return (
            <Text
              key={index}
              style={{ color: '#8B3F3F', fontWeight: '700' }}
              onPress={() => router.push(`/paper/${segment.paperId}`)}
            >
              {segment.text}
            </Text>
          );
        }
        return <Text key={index}>{segment.text}</Text>;
      })}
    </Text>
  );
}
