package com.citecircle.app.core.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The parser decides whether a post pays for a WebView, so both directions of
 * the math heuristic matter: missing real math breaks rendering, and false
 * positives spin up a WebView for ordinary prose.
 */
class ScholarlyContentTest {

    // ── Math detection: positives ────────────────────────────────────────────

    @Test
    fun `detects inline dollar math`() {
        assertTrue(ScholarlyContent.containsMath("The mass-energy relation ${'$'}E = mc^2${'$'} holds."))
    }

    @Test
    fun `detects display dollar math`() {
        assertTrue(ScholarlyContent.containsMath("Result:\n${'$'}${'$'}\\int_0^1 x dx = \\frac{1}{2}${'$'}${'$'}"))
    }

    @Test
    fun `detects latex paren delimiters`() {
        assertTrue(ScholarlyContent.containsMath("Given \\(x > 0\\) we proceed."))
    }

    @Test
    fun `detects latex bracket delimiters`() {
        assertTrue(ScholarlyContent.containsMath("\\[ \\sum_{i=1}^n i = \\frac{n(n+1)}{2} \\]"))
    }

    // ── Math detection: negatives (currency must stay prose) ─────────────────

    @Test
    fun `plain prose is not math`() {
        assertFalse(ScholarlyContent.containsMath("We replicated the study across three sites."))
    }

    @Test
    fun `currency range is not math`() {
        // The killer false positive: "$5 to $10" looks like $...$ to a naive regex.
        assertFalse(ScholarlyContent.containsMath("Reagents cost ${'$'}5 to ${'$'}10 per sample."))
    }

    @Test
    fun `single dollar amount is not math`() {
        assertFalse(ScholarlyContent.containsMath("The grant awarded ${'$'}50000 for equipment."))
    }

    @Test
    fun `dollar followed by space is not math`() {
        assertFalse(ScholarlyContent.containsMath("Budget: ${'$'} 500 total, ${'$'} 200 remaining."))
    }

    // ── Block parsing ────────────────────────────────────────────────────────

    @Test
    fun `plain post yields a single prose block`() {
        val blocks = ScholarlyContent.parse("Just a normal discussion post.")
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is ContentBlock.Prose)
    }

    @Test
    fun `fenced code becomes a code block with its language`() {
        val blocks = ScholarlyContent.parse("Try this:\n```python\nprint(42)\n```")
        assertEquals(2, blocks.size)
        assertTrue(blocks[0] is ContentBlock.Prose)
        val code = blocks[1] as ContentBlock.Code
        assertEquals("python", code.language)
        assertEquals("print(42)", code.code)
    }

    @Test
    fun `code fence without a language still parses`() {
        val blocks = ScholarlyContent.parse("```\nsome code\n```")
        val code = blocks.single() as ContentBlock.Code
        assertEquals("", code.language)
        assertEquals("some code", code.code)
    }

    @Test
    fun `math and code coexist in order`() {
        val raw = "Given ${'$'}f(x)=x^2${'$'} we compute:\n" +
            "```python\nf = lambda x: x**2\n```\n" +
            "which yields ${'$'}f(3)=9${'$'}."
        val blocks = ScholarlyContent.parse(raw)
        assertEquals(3, blocks.size)
        assertTrue("first should be math", blocks[0] is ContentBlock.Math)
        assertTrue("second should be code", blocks[1] is ContentBlock.Code)
        assertTrue("third should be math", blocks[2] is ContentBlock.Math)
    }

    @Test
    fun `math inside a fenced block is not treated as math`() {
        // LaTeX source shared as a code sample must render as code, not equations.
        val blocks = ScholarlyContent.parse("```latex\n${'$'}E=mc^2${'$'}\n```")
        assertTrue(blocks.single() is ContentBlock.Code)
    }

    @Test
    fun `multiple code blocks are all captured`() {
        val raw = "```python\na = 1\n```\ntext between\n```r\nb <- 2\n```"
        val blocks = ScholarlyContent.parse(raw)
        assertEquals(3, blocks.size)
        assertEquals("python", (blocks[0] as ContentBlock.Code).language)
        assertTrue(blocks[1] is ContentBlock.Prose)
        assertEquals("r", (blocks[2] as ContentBlock.Code).language)
    }

    @Test
    fun `blank content does not crash`() {
        assertEquals(1, ScholarlyContent.parse("").size)
        assertEquals(1, ScholarlyContent.parse("   ").size)
    }

    @Test
    fun `unterminated fence degrades to prose rather than eating the post`() {
        val blocks = ScholarlyContent.parse("Here we go:\n```python\nprint(1)")
        assertTrue(blocks.all { it is ContentBlock.Prose })
    }
}
