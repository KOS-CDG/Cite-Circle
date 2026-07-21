/** Ported from the Kotlin app's SyntaxHighlighter (core/designsystem/SyntaxHighlighter.kt). */

export interface HighlightToken {
  text: string;
  color: string;
  bold?: boolean;
  italic?: boolean;
}

interface Palette {
  keyword: string;
  string: string;
  comment: string;
  number: string;
  function: string;
  plain: string;
}

const DARK: Palette = {
  keyword: '#C792EA',
  string: '#C3E88D',
  comment: '#6B7A99',
  number: '#F78C6C',
  function: '#82AAFF',
  plain: '#D6DEEB',
};

const LIGHT: Palette = {
  keyword: '#8B3FBF',
  string: '#2E7D32',
  comment: '#8A94A6',
  number: '#B35300',
  function: '#2B5CD9',
  plain: '#1B2A4A',
};

const COMMON = [
  'if', 'else', 'for', 'while', 'return', 'break', 'continue', 'class',
  'new', 'try', 'catch', 'finally', 'throw', 'import', 'true', 'false', 'null',
];

const KEYWORDS: Record<string, string[]> = {
  python: [...COMMON, 'def', 'elif', 'None', 'True', 'False', 'and', 'or', 'not', 'in', 'is', 'lambda', 'with', 'as', 'from', 'yield', 'async', 'await', 'pass', 'raise', 'self'],
  kotlin: [...COMMON, 'fun', 'val', 'var', 'when', 'object', 'data', 'suspend', 'override', 'private', 'internal', 'companion', 'init', 'sealed', 'interface', 'this', 'it'],
  java: [...COMMON, 'public', 'private', 'protected', 'static', 'void', 'int', 'double', 'boolean', 'final', 'extends', 'implements', 'this', 'package'],
  javascript: [...COMMON, 'function', 'const', 'let', 'var', '=>', 'async', 'await', 'typeof', 'undefined', 'this', 'export', 'default'],
  r: [...COMMON, 'function', 'NULL', 'NA', 'TRUE', 'FALSE', 'library', 'require'],
  sql: ['select', 'from', 'where', 'join', 'left', 'right', 'inner', 'outer', 'group', 'order', 'by', 'having', 'insert', 'update', 'delete', 'create', 'table', 'index', 'on', 'as', 'and', 'or', 'not', 'null', 'limit', 'distinct'],
  c: [...COMMON, 'include', 'define', 'struct', 'typedef', 'void', 'int', 'char', 'const'],
};

function keywordsFor(language: string): Set<string> {
  switch (language) {
    case 'py':
    case 'python':
      return new Set(KEYWORDS.python);
    case 'kt':
    case 'kotlin':
      return new Set(KEYWORDS.kotlin);
    case 'java':
      return new Set(KEYWORDS.java);
    case 'js':
    case 'javascript':
    case 'ts':
    case 'typescript':
      return new Set(KEYWORDS.javascript);
    case 'r':
      return new Set(KEYWORDS.r);
    case 'sql':
      return new Set(KEYWORDS.sql);
    case 'c':
    case 'cpp':
    case 'c++':
      return new Set(KEYWORDS.c);
    default:
      return new Set(COMMON);
  }
}

/** `#` comments in Python/R/shell/SQL-ish, `//` elsewhere; block comments for C-likes. */
function commentPattern(language: string): string {
  switch (language) {
    case 'py':
    case 'python':
    case 'r':
    case 'sh':
    case 'bash':
    case 'yaml':
    case 'yml':
      return '#[^\\n]*';
    case 'sql':
      return '--[^\\n]*';
    default:
      return '//[^\\n]*|/\\*[\\s\\S]*?\\*/';
  }
}

/** Lightweight single-pass tokeniser — not a parser, just enough for readability. */
export function highlight(code: string, language: string, isDark: boolean): HighlightToken[] {
  const palette = isDark ? DARK : LIGHT;
  const keywords = keywordsFor(language);

  const scanner = new RegExp(
    `(?<comment>${commentPattern(language)})` +
      '|(?<string>"""[\\s\\S]*?"""|"(?:\\\\.|[^"\\\\\\n])*"|\'(?:\\\\.|[^\'\\\\\\n])*\')' +
      '|(?<number>\\b\\d+(?:\\.\\d+)?\\b)' +
      '|(?<word>[A-Za-z_][A-Za-z0-9_]*)(?<call>\\s*\\()?',
    'g',
  );

  const tokens: HighlightToken[] = [];
  let cursor = 0;
  let match: RegExpExecArray | null;
  while ((match = scanner.exec(code))) {
    if (match.index > cursor) tokens.push({ text: code.slice(cursor, match.index), color: palette.plain });

    const groups = match.groups!;
    if (groups.comment !== undefined) {
      tokens.push({ text: groups.comment, color: palette.comment, italic: true });
    } else if (groups.string !== undefined) {
      tokens.push({ text: groups.string, color: palette.string });
    } else if (groups.number !== undefined) {
      tokens.push({ text: groups.number, color: palette.number });
    } else if (groups.word !== undefined) {
      const isCall = groups.call !== undefined;
      if (keywords.has(groups.word.toLowerCase())) {
        tokens.push({ text: groups.word, color: palette.keyword, bold: true });
      } else if (isCall) {
        tokens.push({ text: groups.word, color: palette.function });
      } else {
        tokens.push({ text: groups.word, color: palette.plain });
      }
      if (isCall) tokens.push({ text: groups.call, color: palette.plain });
    }

    cursor = match.index + match[0].length;
    if (match[0].length === 0) scanner.lastIndex += 1;
  }
  if (cursor < code.length) tokens.push({ text: code.slice(cursor), color: palette.plain });

  return tokens;
}
