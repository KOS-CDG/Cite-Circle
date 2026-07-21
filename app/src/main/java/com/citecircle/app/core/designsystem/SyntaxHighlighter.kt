package com.citecircle.app.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Lightweight tokeniser for code shared in posts and comments.
 *
 * Deliberately not a parser: research posts carry short snippets, and a
 * single-pass regex scan costs far less than a grammar-driven highlighter for
 * output nobody compiles. Unknown languages still get strings, comments and
 * numbers, which is most of the readability win.
 */
object SyntaxHighlighter {

    private data class Palette(
        val keyword: Color,
        val string: Color,
        val comment: Color,
        val number: Color,
        val function: Color,
        val plain: Color,
    )

    private val Dark = Palette(
        keyword = Color(0xFFC792EA),
        string = Color(0xFFC3E88D),
        comment = Color(0xFF6B7A99),
        number = Color(0xFFF78C6C),
        function = Color(0xFF82AAFF),
        plain = Color(0xFFD6DEEB),
    )

    private val Light = Palette(
        keyword = Color(0xFF8B3FBF),
        string = Color(0xFF2E7D32),
        comment = Color(0xFF8A94A6),
        number = Color(0xFFB35300),
        function = Color(0xFF2B5CD9),
        plain = Color(0xFF1B2A4A),
    )

    private val COMMON = setOf(
        "if", "else", "for", "while", "return", "break", "continue", "class",
        "new", "try", "catch", "finally", "throw", "import", "true", "false", "null",
    )

    private val KEYWORDS: Map<String, Set<String>> = mapOf(
        "python" to COMMON + setOf(
            "def", "elif", "None", "True", "False", "and", "or", "not", "in", "is",
            "lambda", "with", "as", "from", "yield", "async", "await", "pass", "raise", "self",
        ),
        "kotlin" to COMMON + setOf(
            "fun", "val", "var", "when", "object", "data", "suspend", "override",
            "private", "internal", "companion", "init", "sealed", "interface", "this", "it",
        ),
        "java" to COMMON + setOf(
            "public", "private", "protected", "static", "void", "int", "double",
            "boolean", "final", "extends", "implements", "this", "package",
        ),
        "javascript" to COMMON + setOf(
            "function", "const", "let", "var", "=>", "async", "await", "typeof",
            "undefined", "this", "export", "default",
        ),
        "r" to COMMON + setOf("function", "NULL", "NA", "TRUE", "FALSE", "library", "require"),
        "sql" to setOf(
            "select", "from", "where", "join", "left", "right", "inner", "outer",
            "group", "order", "by", "having", "insert", "update", "delete", "create",
            "table", "index", "on", "as", "and", "or", "not", "null", "limit", "distinct",
        ),
        "c" to COMMON + setOf("include", "define", "struct", "typedef", "void", "int", "char", "const"),
    )

    private fun keywordsFor(language: String): Set<String> = when (language) {
        "py", "python" -> KEYWORDS.getValue("python")
        "kt", "kotlin" -> KEYWORDS.getValue("kotlin")
        "java" -> KEYWORDS.getValue("java")
        "js", "javascript", "ts", "typescript" -> KEYWORDS.getValue("javascript")
        "r" -> KEYWORDS.getValue("r")
        "sql" -> KEYWORDS.getValue("sql")
        "c", "cpp", "c++" -> KEYWORDS.getValue("c")
        else -> COMMON
    }

    /** `#` comments in Python/R/shell/SQL-ish, `//` elsewhere; `/* */` for C-likes. */
    private fun commentPattern(language: String): String = when (language) {
        "py", "python", "r", "sh", "bash", "yaml", "yml" -> "#[^\\n]*"
        "sql" -> "--[^\\n]*"
        else -> "//[^\\n]*|/\\*[\\s\\S]*?\\*/"
    }

    fun highlight(code: String, language: String, isDark: Boolean): AnnotatedString {
        val palette = if (isDark) Dark else Light
        val keywords = keywordsFor(language)

        // One alternation, scanned left to right, so a keyword inside a string or
        // comment is never re-tokenised — the outer match consumes it first.
        val scanner = Regex(
            "(?<comment>${commentPattern(language)})" +
                "|(?<string>\"\"\"[\\s\\S]*?\"\"\"|\"(?:\\\\.|[^\"\\\\\\n])*\"|'(?:\\\\.|[^'\\\\\\n])*')" +
                "|(?<number>\\b\\d+(?:\\.\\d+)?\\b)" +
                "|(?<word>[A-Za-z_][A-Za-z0-9_]*)(?<call>\\s*\\()?"
        )

        return buildAnnotatedString {
            var cursor = 0
            for (match in scanner.findAll(code)) {
                if (match.range.first > cursor) {
                    withStyle(SpanStyle(color = palette.plain)) {
                        append(code.substring(cursor, match.range.first))
                    }
                }

                val comment = match.groups["comment"]?.value
                val string = match.groups["string"]?.value
                val number = match.groups["number"]?.value
                val word = match.groups["word"]?.value

                when {
                    comment != null -> withStyle(
                        SpanStyle(color = palette.comment, fontStyle = FontStyle.Italic)
                    ) { append(comment) }

                    string != null -> withStyle(SpanStyle(color = palette.string)) { append(string) }

                    number != null -> withStyle(SpanStyle(color = palette.number)) { append(number) }

                    word != null -> {
                        val isCall = match.groups["call"] != null
                        val style = when {
                            word.lowercase() in keywords ->
                                SpanStyle(color = palette.keyword, fontWeight = FontWeight.Bold)
                            isCall -> SpanStyle(color = palette.function)
                            else -> SpanStyle(color = palette.plain)
                        }
                        withStyle(style) { append(word) }
                        // The trailing "(" is part of the match but not of the name.
                        if (isCall) {
                            withStyle(SpanStyle(color = palette.plain)) {
                                append(match.groups["call"]!!.value)
                            }
                        }
                    }
                }
                cursor = match.range.last + 1
            }

            if (cursor < code.length) {
                withStyle(SpanStyle(color = palette.plain)) { append(code.substring(cursor)) }
            }
        }
    }
}
