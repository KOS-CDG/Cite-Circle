package com.citecircle.app.core.designsystem

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.webkit.WebView
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject

/**
 * Renders academic post/comment/message bodies: LaTeX math via KaTeX, fenced code
 * with syntax highlighting, and inline `code`.
 *
 * Plain prose — the overwhelming majority of content — never touches a WebView.
 * [ScholarlyContent.parse] only emits a [ContentBlock.Math] block when math
 * delimiters are actually present, and a whole block renders in ONE WebView
 * rather than one per formula, so a post with five equations still costs a
 * single view.
 */
@Composable
fun ScholarlyText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    val blocks = remember(text) { ScholarlyContent.parse(text) }

    // Fast path: nothing to lay out block-by-block.
    if (blocks.size == 1 && blocks[0] is ContentBlock.Prose) {
        Text(
            text = inlineCodeAnnotated((blocks[0] as ContentBlock.Prose).text, color),
            style = style,
            color = color,
            modifier = modifier,
        )
        return
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block ->
            when (block) {
                is ContentBlock.Prose -> Text(
                    text = inlineCodeAnnotated(block.text, color),
                    style = style,
                    color = color,
                    modifier = Modifier.fillMaxWidth(),
                )

                is ContentBlock.Code -> CodeBlock(
                    code = block.code,
                    language = block.language,
                    modifier = Modifier.fillMaxWidth(),
                )

                is ContentBlock.Math -> KaTeXBlock(
                    content = block.text,
                    textColor = color,
                    fontSizeSp = style.fontSize.takeIf { it != androidx.compose.ui.unit.TextUnit.Unspecified }
                        ?.value ?: 14f,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Parsing
// ──────────────────────────────────────────────────────────────────────────────



// ──────────────────────────────────────────────────────────────────────────────
// KaTeX
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Renders one text run containing LaTeX through KaTeX in an offline WebView.
 *
 * The user's text is injected with `textContent` and a JSON-encoded string —
 * never as HTML — so post content cannot inject markup or script into the
 * WebView, which runs with JavaScript enabled to drive KaTeX.
 */
@SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
@Composable
private fun KaTeXBlock(
    content: String,
    textColor: Color,
    fontSizeSp: Float,
    modifier: Modifier = Modifier,
) {
    var height by remember(content) { mutableStateOf<Dp?>(null) }
    val colorCss = remember(textColor) { textColor.toCssRgba() }
    val html = remember(content, colorCss, fontSizeSp) {
        katexDocument(content, colorCss, fontSizeSp)
    }

    // The page calls back once KaTeX has actually laid out, and again when the
    // web fonts land — the second report is what stops a formula from being
    // clipped, since KaTeX measures wider after its fonts swap in.
    val bridge = remember {
        object {
            var onHeight: (Float) -> Unit = {}

            @android.webkit.JavascriptInterface
            fun report(pixels: Float) {
                onHeight(pixels)
            }
        }
    }
    bridge.onHeight = { reported -> if (reported > 0f) height = reported.dp }

    AndroidView(
        modifier = modifier.then(height?.let { Modifier.height(it) } ?: Modifier.height(1.dp)),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                // KaTeX only needs the bundled assets; deny everything wider.
                settings.allowFileAccessFromFileURLs = false
                settings.allowUniversalAccessFromFileURLs = false
                settings.blockNetworkLoads = true
                settings.setSupportZoom(false)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                setBackgroundColor(AndroidColor.TRANSPARENT)
                addJavascriptInterface(bridge, "AndroidHeight")
            }
        },
        update = { webView ->
            // Reload only on a genuine content change. Loading unconditionally
            // would restart the render on every recomposition, including the one
            // our own height callback triggers.
            if (webView.tag != html) {
                webView.tag = html
                webView.loadDataWithBaseURL(KATEX_BASE_URL, html, "text/html", "utf-8", null)
            }
        },
    )
}

private const val KATEX_BASE_URL = "file:///android_asset/katex/"

/**
 * Builds the full page, content included.
 *
 * Rendering is driven by an inline script at the end of `<body>` rather than
 * from `onPageFinished` + `evaluateJavascript`: that callback can fire before
 * `#c` exists or before katex.min.js has executed, and the resulting failure
 * left the view collapsed with no way to recover.
 *
 * The body text is injected via `textContent` from a JSON-encoded literal, so
 * user content is never parsed as HTML. `<` is additionally escaped so a post
 * containing `</script>` cannot break out of the script element.
 */
private fun katexDocument(content: String, colorCss: String, fontSizeSp: Float): String {
    val payload = JSONObject.quote(content).replace("<", "\\u003c")
    return """
<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<link rel="stylesheet" href="katex.min.css">
<script src="katex.min.js"></script>
<script src="contrib/auto-render.min.js"></script>
<style>
  html, body {
    margin: 0; padding: 0;
    background: transparent;
    color: $colorCss;
    font-size: ${fontSizeSp}px;
    font-family: -apple-system, Roboto, sans-serif;
    line-height: 1.45;
    overflow-wrap: break-word;
  }
  #c { padding: 0; }
  .katex-display { margin: 0.5em 0; overflow-x: auto; overflow-y: hidden; }
  .katex { color: $colorCss; }
</style>
</head>
<body><div id="c"></div>
<script>
(function () {
  var el = document.getElementById('c');
  el.textContent = $payload;

  function report() {
    try {
      var h = Math.ceil(document.documentElement.getBoundingClientRect().height);
      if (h > 0 && window.AndroidHeight) AndroidHeight.report(h);
    } catch (e) {}
  }

  try {
    if (typeof renderMathInElement === 'function') {
      renderMathInElement(el, {
        delimiters: [
          { left: '${'$'}${'$'}', right: '${'$'}${'$'}', display: true  },
          { left: '\\[',          right: '\\]',          display: true  },
          { left: '${'$'}',       right: '${'$'}',       display: false },
          { left: '\\(',          right: '\\)',          display: false }
        ],
        throwOnError: false
      });
    }
  } catch (e) { /* malformed LaTeX stays visible as its source text */ }

  report();
  // KaTeX reflows once its web fonts resolve, so measure again then.
  if (document.fonts && document.fonts.ready) document.fonts.ready.then(report);
  if (window.ResizeObserver) new ResizeObserver(report).observe(el);
  window.addEventListener('load', report);
})();
</script>
</body>
</html>
""".trimIndent()
}

private fun Color.toCssRgba(): String =
    "rgba(${(red * 255).toInt()}, ${(green * 255).toInt()}, ${(blue * 255).toInt()}, $alpha)"

// ──────────────────────────────────────────────────────────────────────────────
// Code
// ──────────────────────────────────────────────────────────────────────────────

/** Fenced code block: monospace, horizontally scrollable, syntax highlighted. */
@Composable
fun CodeBlock(
    code: String,
    language: String,
    modifier: Modifier = Modifier,
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val highlighted = remember(code, language, isDark) {
        SyntaxHighlighter.highlight(code, language, isDark)
    }

    Surface(
        modifier = modifier.clip(RoundedCornerShape(10.dp)),
        color = if (isDark) Color(0xFF0E1524) else Color(0xFFF3F1EC),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (language.isNotBlank()) {
                Text(
                    text = language.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.ccColors.marginGray,
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
            Text(
                text = highlighted,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            )
        }
    }
}

/** Styles `inline code` spans without pulling in a full markdown renderer. */
private fun inlineCodeAnnotated(text: String, color: Color): AnnotatedString {
    if (!text.contains('`')) return AnnotatedString(text)

    return buildAnnotatedString {
        var cursor = 0
        val pattern = Regex("`([^`\\n]+)`")
        for (match in pattern.findAll(text)) {
            append(text.substring(cursor, match.range.first))
            withStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = color,
                    background = color.copy(alpha = 0.08f),
                )
            ) {
                append(match.groupValues[1])
            }
            cursor = match.range.last + 1
        }
        if (cursor < text.length) append(text.substring(cursor))
    }
}

private fun Color.luminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue
