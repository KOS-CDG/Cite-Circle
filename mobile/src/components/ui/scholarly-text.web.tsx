import renderMathInElement from 'katex/contrib/auto-render';
import { useEffect, useRef } from 'react';
import { View, type TextStyle } from 'react-native';

import { parseScholarlyContent } from '@/lib/scholarly-content';
import { CodeBlock } from './code-block';
import { InlineProse } from './scholarly-inline';
import { useAppTheme } from './theme-provider';

const KATEX_CSS_URL = 'https://cdn.jsdelivr.net/npm/katex@0.18.1/dist/katex.min.css';
const KATEX_CSS_ID = 'katex-css-cdn';

/**
 * Metro's web bundler can't resolve the local `url(fonts/...)` references
 * inside katex's own CSS ("Importing local resources in CSS is not
 * supported yet"), so the stylesheet loads from the CDN instead — same
 * approach the native WebView renderer already uses for its whole page.
 */
function ensureKatexCss() {
  if (typeof document === 'undefined' || document.getElementById(KATEX_CSS_ID)) return;
  const link = document.createElement('link');
  link.id = KATEX_CSS_ID;
  link.rel = 'stylesheet';
  link.href = KATEX_CSS_URL;
  document.head.appendChild(link);
}

interface ScholarlyTextProps {
  text: string;
  style?: TextStyle;
  color?: string;
}

/**
 * Web counterpart of scholarly-text.tsx: KaTeX runs directly in the DOM
 * (no WebView) via katex/contrib/auto-render, the same "scan a whole mixed
 * prose+math block" approach the native/Kotlin renderers use. Content is
 * injected with `textContent`, never HTML, so post text is never parsed as
 * markup.
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
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    ensureKatexCss();
    const el = ref.current;
    if (!el) return;
    el.textContent = content;
    try {
      renderMathInElement(el, {
        delimiters: [
          { left: '$$', right: '$$', display: true },
          { left: '\\[', right: '\\]', display: true },
          { left: '$', right: '$', display: false },
          { left: '\\(', right: '\\)', display: false },
        ],
        throwOnError: false,
      });
    } catch {
      // malformed LaTeX stays visible as its source text
    }
  }, [content]);

  return <div ref={ref} style={{ color, fontSize, lineHeight: 1.45, overflowX: 'auto', overflowWrap: 'break-word' }} />;
}
