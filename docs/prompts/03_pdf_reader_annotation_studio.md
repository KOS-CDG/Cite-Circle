# Prompt 3: In-App PDF Reader, Annotation Studio & AI Summarizer

```markdown
Role: Lead Mobile Engineer & AI UX Designer

Task:
Build an interactive PDF Reader and Annotation Studio for Cite Circle papers, allowing researchers to read preprints, highlight text, take marginal study notes, and generate AI section breakdowns.

Features & Requirements:

1. High-Performance Canvas PDF Renderer:
   - Render PDF pages smoothly with pinch-to-zoom, horizontal/vertical paging, page thumbnail scrub bar, and page jump search.
   - Cache rendered pages locally using Cloudinary thumbnail generator or PDF page rendering engines.

2. Interactive Highlight & Marginal Notes:
   - Text Selection: Allow users to select text passages on any PDF page.
   - Multi-Color Highlighter: Yellow (Key finding), Green (Methodology), Blue (Citation), Purple (Question/Critique).
   - Sticky Notes: Attach marginal commentary to exact page coordinates/offsets.
   - Persistence: Save annotations to backend (`POST /papers/{id}/annotations`) and sync across devices.

3. AI Executive Summarizer Drawer:
   - Floating Action Button to trigger AI paper analysis.
   - Section Breakdowns: Instant summarization tabs for "Abstract TL;DR", "Methodology & Setup", "Core Results", "Limitations & Future Work".
   - Key Takeaways Bullet List & Methodology Quality Index.

4. Social Quote Sharing ("Share to Feed"):
   - Highlight-to-Post: Tapping "Share Quote" on selected text pre-fills a new `PAPER_SHARE` post screen with the exact quote, page number citation, and paper attachment card.

Deliverables:
1. `PdfViewerScreen` UI component with overlay layer for text selection and highlight rendering.
2. `PaperAnnotation` data models & API endpoints for saving/fetching user notes.
3. `AiSummarizerBottomSheet` displaying AI section summaries.
4. Navigation link from Paper Detail screen to `PdfViewerRoute`.
```
