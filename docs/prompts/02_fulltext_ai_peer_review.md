# Prompt 2: Full-Text AI Peer Review & Fireworks.ai Engine

```markdown
Role: Full-Stack AI Engineer & Backend Architect

Task:
Implement a full-manuscript AI Peer Review system for Cite Circle, upgrading the existing single-abstract endpoint (`POST /papers/review`) to analyze full IMRaD manuscript sections against `PUBLICATION_STANDARD.md` using the Fireworks.ai REST Chat Completions API.

Features & Requirements:

1. Full Manuscript Parsing (Backend):
   - Extend `ReviewRequest` schema in `backend/schemas.py` to support `full_text` or structured section maps (`introduction`, `methods`, `results`, `discussion`, `references`).
   - Extract and validate sections prior to scoring. If non-IMRaD text or out-of-scope genres (opinion piece, promotional) are submitted, return early desk rejection without scoring.

2. Hard Gate Ethical Pre-Screening:
   - Implement binary gate checks:
     * G4: Human/Animal Ethics & IRB approval statement check.
     * G5: Informed consent declaration check.
     * G6: Data availability statement check.
     * G7: Conflict of interest statement presence.
     * G10: Reference list completeness & citation resolution.

3. Weighted Multi-Dimensional Rubric:
   - Calculate `overall_score` based on exact weights in `PUBLICATION_STANDARD.md`:
     * Structure (IMRaD integrity): 30%
     * Citations (APA 7 formatting & coverage): 25%
     * Clarity (Expression & statistical notation): 20%
     * Originality (Gap & methodology contribution): 25%
   - Override Rule: Enforce major revision capping if any core methodological score (Structure or Originality) is below 60.

4. Fireworks.ai Structured JSON Output Integration:
   - Model: `accounts/fireworks/models/llama-v3p1-70b-instruct`
   - Request JSON Mode: `"response_format": {"type": "json_object"}`.
   - Enforce system prompt demanding JSON payload containing:
     * Overall verdict (`ACCEPT`, `MINOR_REVISIONS`, `MAJOR_REVISIONS`, `REJECT`).
     * Numeric scores for all 4 dimensions.
     * Actionable suggestions categorized by severity (`MINOR`, `MODERATE`, `NEEDS_ATTENTION`), quoting specific passages.

5. Client UX & Progressive Milestone Updates (Android / Expo):
   - Stream progressive review stages in client (`AiReviewStage` Flow):
     1. "Parsing manuscript sections & references..."
     2. "Evaluating ethical hard gates..."
     3. "Analyzing structure & citations via Fireworks.ai..."
     4. "Formatting recommendations & verdict..."
     5. "Complete" (delivering report) or "Error" (graceful fallback).

Deliverables:
1. `backend/schemas.py` updated schemas (`ReviewRequest`, `AiReviewReport`, `AiSuggestion`).
2. `backend/main.py` updated `POST /papers/review` endpoint logic.
3. `FireworksAiReviewRepository` Retrofit network implementation for client.
4. UI component for displaying detailed review breakdowns with expandable suggestions.
```
