package com.citecircle.app.core.designsystem

import org.json.JSONObject

sealed interface ContentBlock {
    data class Prose(val text: String) : ContentBlock
    data class Code(val code: String, val language: String) : ContentBlock
    data class Math(val text: String) : ContentBlock
}

object ScholarlyContent {

    private val FENCED_CODE = Regex("```(\\w*)\\n([\\s\\S]*?)```")

    private val MATH_PRESENT = Regex(
        """\$\$[\s\S]+?\$\$""" +
            """|\$(?!\s)[^$\n]+?(?<!\s)\$""" +
            """|\\\([\s\S]+?\\\)""" +
            """|\\\[[\s\S]+?\\]"""
    )

    fun containsMath(text: String): Boolean = MATH_PRESENT.containsMatchIn(text)

    fun parse(raw: String): List<ContentBlock> {
        if (raw.isBlank()) return listOf(ContentBlock.Prose(raw))

        val blocks = mutableListOf<ContentBlock>()
        var cursor = 0

        for (match in FENCED_CODE.findAll(raw)) {
            if (match.range.first > cursor) {
                addTextual(blocks, raw.substring(cursor, match.range.first))
            }
            blocks += ContentBlock.Code(
                code = match.groupValues[2].trimEnd('\n'),
                language = match.groupValues[1].lowercase(),
            )
            cursor = match.range.last + 1
        }

        if (cursor < raw.length) addTextual(blocks, raw.substring(cursor))

        return blocks.ifEmpty { listOf(ContentBlock.Prose(raw)) }
    }

    private fun addTextual(blocks: MutableList<ContentBlock>, segment: String) {
        val trimmed = segment.trim('\n')
        if (trimmed.isEmpty()) return
        blocks += if (containsMath(trimmed)) ContentBlock.Math(trimmed) else ContentBlock.Prose(trimmed)
    }
}
