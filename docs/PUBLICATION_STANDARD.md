# Cite Circle Publication Standard

**Version 1.0 — Effective 2026-07-20**

The editorial criteria a manuscript must satisfy to be published in Cite Circle. This
document is the authority behind the AI review report rendered on the publish screen;
the scored dimensions here map one-to-one onto `AiReviewReport` in `backend/schemas.py`.

---

## 1. Scope and Accepted Genres

Cite Circle accepts **empirical research articles, systematic reviews, replication
studies, and registered reports**. Each is evaluated against the IMRaD framework
(Introduction, Methods, Results, Discussion) and APA 7 style.

The following are **out of scope** and must be desk-rejected rather than scored, because
the rubric's dimensions do not apply to them:

| Genre | Disposition |
|---|---|
| Opinion, editorial, blog-style commentary | Reject — no Methods or Results to assess |
| Science journalism / newsletters / magazine features | Reject — genre mismatch |
| Coursework, lab reports, problem sets | Reject unless restructured as IMRaD |
| Marketing or promotional content | Reject |
| Literature summaries without synthesis or protocol | Reject — not a systematic review |

Rationale: forcing a non-research genre through an IMRaD/APA rubric produces a
meaningless score. Genre screening happens **before** scoring.

---

## 2. Hard Gates (Pre-Scoring)

A submission failing **any** gate is returned without a numeric score. These are binary
and non-negotiable.

| # | Gate | Requirement |
|---|---|---|
| G1 | **Authorship & integrity** | All listed authors made substantive contributions. No ghost or gift authorship. |
| G2 | **Originality** | Not published elsewhere; not under concurrent review. Prior preprint must be disclosed. |
| G3 | **Plagiarism** | No unattributed text. Self-plagiarism disclosed and cited. |
| G4 | **Human/animal subjects ethics** | IRB/ethics approval number stated, or explicit statement that the study was exempt and why. |
| G5 | **Informed consent** | Documented for all human participants, including data-use and withdrawal terms. |
| G6 | **Data availability** | A data availability statement is present — whether data are open, restricted, or unavailable, with reason. |
| G7 | **Conflict of interest** | Declared, including funding sources. "None" is an acceptable declaration; silence is not. |
| G8 | **Fabrication** | No invented data, citations, or participants. Suspected fabrication escalates to human review, never auto-reject. |
| G9 | **Structural minimum** | Contains identifiable Introduction, Methods, Results, and Discussion sections. |
| G10 | **Reference list present** | A complete reference list exists and every in-text citation resolves to it. |

**G8 note:** fabrication suspicion must route to a human editor. An automated system
must not level an integrity accusation on its own.

---

## 3. Scored Dimensions

Four dimensions, each **0–100**, matching `structure`, `citations`, `clarity`, and
`originality` in the schema.

### 3.1 Structure (IMRaD Integrity) — weight 30%

Assesses whether the manuscript is organized as a research report and whether each
section performs its function.

| Band | Score | Criteria |
|---|---|---|
| Exemplary | 90–100 | All IMRaD sections present and correctly scoped. Explicit RQs/hypotheses. Methods replicable from the text alone. Results non-interpretive; interpretation confined to Discussion. Limitations section substantive. |
| Proficient | 75–89 | All sections present. RQs stated. Minor scope bleed (e.g. some interpretation in Results). Methods replicable with minor inference. |
| Developing | 60–74 | Sections present but unbalanced — thin Methods, or Discussion that restates Results without interpreting them. Hypotheses implied rather than stated. |
| Inadequate | 40–59 | A required section is missing or vestigial. Methods insufficient for replication. No limitations acknowledged. |
| Unacceptable | 0–39 | Not recognizably IMRaD. Fails gate G9. |

**Required in Methods for a score ≥75:** design named and justified; sampling procedure
and sample size determination; participant inclusion/exclusion criteria; demographics;
instrumentation with reliability evidence (Cronbach's α, McDonald's ω, or equivalent);
procedure; analytic plan. For quantitative designs, an *a priori* power analysis is
required for ≥85.

### 3.2 Citations (Attribution & APA 7 Reference Compliance) — weight 25%

| Band | Score | Criteria |
|---|---|---|
| Exemplary | 90–100 | Every claim attributed. All in-text citations resolve to the reference list and vice versa. APA 7 syntax correct throughout — `et al.` rule, narrative vs. parenthetical form, page locators on direct quotes. Hanging indents, sentence case for article titles, italicized journal names and volumes, DOIs as `https://doi.org/...`. Literature current and topically relevant. |
| Proficient | 75–89 | Attribution sound. Fewer than 5 formatting deviations. All DOIs present where they exist. |
| Developing | 60–74 | Several unattributed claims, or 5–15 formatting errors, or systematic DOI omission. Literature dated relative to the field. |
| Inadequate | 40–59 | Citation–reference mismatches. Pervasive formatting non-compliance. Over-reliance on secondary sources without `as cited in`. |
| Unacceptable | 0–39 | Citations unverifiable or absent. Fails gate G10. |

**Recency guidance:** ≥50% of references from the last 10 years for fast-moving fields;
seminal older works are exempt and expected.

### 3.3 Clarity (Expression & Presentation) — weight 20%

| Band | Score | Criteria |
|---|---|---|
| Exemplary | 90–100 | Prose precise and economical. Title ≤12 words, empirically specific. Abstract 150–250 words covering problem, method, findings, implications, with keywords. Heading hierarchy correct across all five APA levels. Tables/figures numbered, titled in italic title case, called out in text, no vertical rules, `*Note.*` italicized. Statistical notation italicized (*M*, *SD*, *p*, *t*, *F*, *N*, *r*, *d*, *η*²), correct spacing (`*p* < .001`), leading zeros omitted for bounded values and included for unbounded. |
| Proficient | 75–89 | Readable and well-organized. Abstract within limits. Isolated notation or heading errors. |
| Developing | 60–74 | Comprehensible but uneven — verbose passages, abstract out of range, inconsistent headings, tables not called out in text. |
| Inadequate | 40–59 | Impedes comprehension. Systematic APA violations. Undefined jargon. |
| Unacceptable | 0–39 | Unintelligible or unformatted. |

### 3.4 Originality (Contribution & Significance) — weight 25%

| Band | Score | Criteria |
|---|---|---|
| Exemplary | 90–100 | Explicit, evidenced research gap. Clear theoretical or methodological advance. Findings meaningfully extend the literature reviewed in the Introduction. Implications specific and actionable. |
| Proficient | 75–89 | Identifiable contribution. Gap stated, if modestly. Incremental but genuine advance. Replications score here when well-executed and clearly framed. |
| Developing | 60–74 | Contribution unclear or overstated relative to evidence. Gap asserted rather than demonstrated. |
| Inadequate | 40–59 | Duplicates existing work without acknowledging it. No discernible gap. |
| Unacceptable | 0–39 | No contribution, or redundant with the author's own prior work undisclosed. |

**Replication studies are explicitly welcome** and must not be penalized on originality
for being replications. A rigorous direct replication with adequate power scores ≥80.

---

## 4. Overall Score and Verdict

```
overall_score = round(
    0.30 × structure +
    0.25 × citations  +
    0.20 × clarity    +
    0.25 × originality
)
```

Verdict thresholds, mapping to the schema's four enum values:

| Verdict | Overall | Additional condition |
|---|---|---|
| `ACCEPT` | ≥ 85 | No dimension below 75. No `NEEDS_ATTENTION` suggestions outstanding. |
| `MINOR_REVISIONS` | 70–84 | No dimension below 60. Deficiencies are stylistic or presentational, not methodological. |
| `MAJOR_REVISIONS` | 50–69 | Or **any** overall score where a methodological dimension (structure, originality) is below 60. |
| `REJECT` | < 50 | Or any hard-gate failure, or genre mismatch per §1. |

**Override rule:** a methodological flaw caps the verdict regardless of arithmetic. A
manuscript scoring 88 overall but 55 on structure is `MAJOR_REVISIONS`, not `ACCEPT`.
Presentation strengths never compensate for design weaknesses.

---

## 5. Suggestion Severity Taxonomy

Maps to `AiSuggestion.severity` (`MINOR` / `MODERATE` / `NEEDS_ATTENTION`).

| Severity | Definition | Examples |
|---|---|---|
| `MINOR` | Cosmetic. Does not affect validity or comprehension. | `p=0.04` → `*p* = .04`; missing DOI; `Smith et. al.` punctuation; heading case. |
| `MODERATE` | Affects clarity, completeness, or compliance. Should be fixed before publication. | Abstract over 250 words; table not called out in text; missing effect sizes; unstated inclusion criteria; dated literature. |
| `NEEDS_ATTENTION` | Threatens validity, replicability, or ethics. Blocks acceptance. | No IRB statement; sample size unjustified; analysis inappropriate to the RQ; conclusions unsupported by results; missing control condition; undisclosed conflict. |

Every `NEEDS_ATTENTION` suggestion must name the specific section and the concrete
remedy — not "improve the methodology" but "state the sampling frame and justify n = 48
with an a priori power analysis."

---

## 6. Reviewer Conduct

1. **Evaluate the manuscript, not the author.** No inference from institution, name, or
   nationality.
2. **Cite the passage.** Every criticism quotes or locates the text it concerns.
3. **Distinguish severity honestly.** Do not inflate stylistic nits to
   `NEEDS_ATTENTION`, and do not soften methodological flaws to `MODERATE`.
4. **Do not fabricate.** If a dimension cannot be assessed from the material provided,
   report it as *not assessable* rather than producing a number. A guessed score is
   worse than an absent one.
5. **Integrity concerns escalate to humans.** See gate G8.

---

## 7. Implementation Note — Current Gap

`POST /reviews` (`backend/main.py:1078-1092`) sends only `title` and `abstract` to the
model, then requests scores for `structure` and `citations`. Neither is derivable from an
abstract: there are no IMRaD sections and no reference list in the payload. Those two
scores — 55% of the weighted total — are currently unfounded.

To make this standard enforceable, one of the following is required:

- **Preferred:** extend `ReviewRequest` to carry full manuscript text (or the parsed
  section map plus reference list) and pass it to the model.
- **Interim:** restrict scoring to `clarity` and `originality`, which *are* assessable
  from a title and abstract, and surface the other two as "pending full-text review"
  rather than emitting a fabricated integer.

The interim option is honest; the current behavior is not.
