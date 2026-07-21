/**
 * Builds the offscreen HTML page a native WebView loads to render one
 * ScholarlyText math block. Ported from the Kotlin app's katexDocument
 * (core/designsystem/ScholarlyText.kt), with two differences: KaTeX loads
 * from a CDN instead of bundled offline assets (no asset-bundling step for
 * this port), and height is reported via `window.ReactNativeWebView.postMessage`
 * instead of an injected Android JS interface.
 *
 * The body text is injected via `textContent` from a JSON-encoded literal —
 * never as HTML — so post content can never be parsed as markup, and `<` is
 * escaped so a `</script>` in a post can't break out of the inline script.
 */
export function buildKatexHtml(content: string, colorCss: string, fontSizePx: number): string {
  const payload = JSON.stringify(content).replace(/</g, '\\u003c');

  return `<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.18.1/dist/katex.min.css">
<script src="https://cdn.jsdelivr.net/npm/katex@0.18.1/dist/katex.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/katex@0.18.1/dist/contrib/auto-render.min.js"></script>
<style>
  html, body {
    margin: 0; padding: 0;
    background: transparent;
    color: ${colorCss};
    font-size: ${fontSizePx}px;
    font-family: -apple-system, Roboto, sans-serif;
    line-height: 1.45;
    overflow-wrap: break-word;
  }
  #c { padding: 0; }
  .katex-display { margin: 0.5em 0; overflow-x: auto; overflow-y: hidden; }
  .katex { color: ${colorCss}; }
</style>
</head>
<body><div id="c"></div>
<script>
(function () {
  var el = document.getElementById('c');
  el.textContent = ${payload};

  function report() {
    try {
      var h = Math.ceil(document.documentElement.getBoundingClientRect().height);
      if (h > 0 && window.ReactNativeWebView) window.ReactNativeWebView.postMessage(String(h));
    } catch (e) {}
  }

  try {
    if (typeof renderMathInElement === 'function') {
      renderMathInElement(el, {
        delimiters: [
          { left: '$$', right: '$$', display: true },
          { left: '\\\\[', right: '\\\\]', display: true },
          { left: '$', right: '$', display: false },
          { left: '\\\\(', right: '\\\\)', display: false }
        ],
        throwOnError: false
      });
    }
  } catch (e) { /* malformed LaTeX stays visible as its source text */ }

  report();
  if (document.fonts && document.fonts.ready) document.fonts.ready.then(report);
  if (window.ResizeObserver) new ResizeObserver(report).observe(el);
  window.addEventListener('load', report);
})();
</script>
</body>
</html>`;
}
