import { useState } from 'react';
import { View, type TextStyle } from 'react-native';
import { WebView } from 'react-native-webview';

import { buildKatexHtml } from '@/lib/katex-html';
import { parseScholarlyContent } from '@/lib/scholarly-content';
import { CodeBlock } from './code-block';
import { InlineProse } from './scholarly-inline';
import { useAppTheme } from './theme-provider';

interface ScholarlyTextProps {
  text: string;
  style?: TextStyle;
  color?: string;
}

/**
 * Renders academic post/comment bodies: LaTeX math via KaTeX (in an offscreen
 * WebView), fenced code with syntax highlighting, inline `code`, and
 * clickable @paper:id citation tags. Plain prose — the overwhelming majority
 * of content — never touches a WebView.
 */
export function ScholarlyText({ text, style, color }: ScholarlyTextProps) {
  const { colors } = useAppTheme();
  const resolvedColor = color ?? colors.text;
  const blocks = parseScholarlyContent(text);

  if (blocks.length === 1 && blocks[0].type === 'prose') {
    return <InlineProse text={blocks[0].text} style={style} color={resolvedColor} />;
  }

  return (
    <View style={{ gap: 8 }}>
      {blocks.map((block, index) => {
        if (block.type === 'code') return <CodeBlock key={index} code={block.code} language={block.language} />;
        if (block.type === 'math') {
          return (
            <KaTeXBlock
              key={index}
              content={block.text}
              color={resolvedColor}
              fontSize={typeof style?.fontSize === 'number' ? style.fontSize : 14}
            />
          );
        }
        return <InlineProse key={index} text={block.text} style={style} color={resolvedColor} />;
      })}
    </View>
  );
}

function KaTeXBlock({ content, color, fontSize }: { content: string; color: string; fontSize: number }) {
  const [height, setHeight] = useState(1);
  const html = buildKatexHtml(content, color, fontSize);

  return (
    <View style={{ height, width: '100%' }}>
      <WebView
        originWhitelist={['*']}
        source={{ html }}
        onMessage={(event) => {
          const reported = Number(event.nativeEvent.data);
          if (reported > 0) setHeight(reported);
        }}
        style={{ backgroundColor: 'transparent' }}
        scrollEnabled={false}
        javaScriptEnabled
      />
    </View>
  );
}
