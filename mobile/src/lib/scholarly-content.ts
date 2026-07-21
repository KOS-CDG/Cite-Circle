/**
 * Ported from the Kotlin app's ScholarlyContent (core/designsystem/ScholarlyText.kt):
 * splits post/comment bodies into fenced-code / math / prose blocks, and prose
 * further splits into inline `code` and @paper:id citation spans.
 */

export type ContentBlock =
  | { type: 'prose'; text: string }
  | { type: 'code'; code: string; language: string }
  | { type: 'math'; text: string };

const FENCED_CODE = /```(\w*)\n([\s\S]*?)```/g;

/**
 * Math delimiters, deliberately strict so prices survive as prose: a
 * single-dollar span must not begin or end on whitespace, which is what keeps
 * "it cost $5 to $10" from being read as an equation.
 */
const MATH_PRESENT = /\$\$[\s\S]+?\$\$|\$(?!\s)[^$\n]+?(?<!\s)\$|\\\([\s\S]+?\\\)|\\\[[\s\S]+?\\\]/;

export function containsMath(text: string): boolean {
  return MATH_PRESENT.test(text);
}

/** Splits raw content into renderable blocks, preserving order. */
export function parseScholarlyContent(raw: string): ContentBlock[] {
  if (!raw.trim()) return [{ type: 'prose', text: raw }];

  const blocks: ContentBlock[] = [];
  let cursor = 0;
  FENCED_CODE.lastIndex = 0;
  let match: RegExpExecArray | null;
  while ((match = FENCED_CODE.exec(raw))) {
    if (match.index > cursor) addTextual(blocks, raw.slice(cursor, match.index));
    blocks.push({ type: 'code', code: match[2].replace(/\n+$/, ''), language: match[1].toLowerCase() });
    cursor = match.index + match[0].length;
  }
  if (cursor < raw.length) addTextual(blocks, raw.slice(cursor));

  return blocks.length ? blocks : [{ type: 'prose', text: raw }];
}

function addTextual(blocks: ContentBlock[], segment: string) {
  const trimmed = segment.replace(/^\n+|\n+$/g, '');
  if (!trimmed) return;
  blocks.push(containsMath(trimmed) ? { type: 'math', text: trimmed } : { type: 'prose', text: trimmed });
}

export type InlineSegment =
  | { type: 'text'; text: string }
  | { type: 'code'; text: string }
  | { type: 'citation'; text: string; paperId: string };

const INLINE_PATTERN = /`([^`\n]+)`|@paper:([A-Za-z0-9_-]+)/g;

/** Styles `inline code` spans and @paper:id citation tags within a prose block. */
export function parseInlineSegments(text: string): InlineSegment[] {
  if (!text.includes('`') && !text.includes('@paper:')) return [{ type: 'text', text }];

  const segments: InlineSegment[] = [];
  let cursor = 0;
  INLINE_PATTERN.lastIndex = 0;
  let match: RegExpExecArray | null;
  while ((match = INLINE_PATTERN.exec(text))) {
    if (match.index > cursor) segments.push({ type: 'text', text: text.slice(cursor, match.index) });
    if (match[1] !== undefined) {
      segments.push({ type: 'code', text: match[1] });
    } else if (match[2] !== undefined) {
      segments.push({ type: 'citation', text: match[0], paperId: match[2] });
    }
    cursor = match.index + match[0].length;
  }
  if (cursor < text.length) segments.push({ type: 'text', text: text.slice(cursor) });
  return segments;
}
