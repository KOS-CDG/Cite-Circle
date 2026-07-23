# Prompt 4: Citation Graph & Co-Author Network Visualizer

```markdown
Role: Frontend Visualizations & Mobile Graph Engineer

Task:
Build an interactive Citation Graph and Co-Author Network visualizer for papers and researchers in Cite Circle, enabling researchers to visually explore literature lineages and co-authorships.

Features & Requirements:

1. Dynamic Node-Link Citation Graph:
   - Render interactive graph visualization for papers where:
     * Nodes = Papers (sized by citation count, colored by field/circle).
     * Edges = Citations (arrow pointing from citing paper to cited paper).
   - Touch Gestures: Smooth pan, pinch-to-zoom, node selection, and physics-based force layout simulation.

2. Co-Author Network Visualizer:
   - Visual network connecting researchers who have co-authored publications.
   - Cluster detection for research labs, institutions, and frequent collaborators.

3. Node Selection & Quick Inspection:
   - Tapping any node slides up a Paper / Author Detail Sheet displaying title, abstract preview, authors, year, citation count, and a button to view full paper or profile.
   - Filter Controls: Depth filter (1-hop, 2-hop citations), year range slider, and minimum citation threshold.

4. Researcher Impact Analytics Card:
   - Header summary metrics: Total Citations, h-index, i10-index, and Citation Velocity graph over time.

Deliverables:
1. Interactive canvas graph component (`CitationGraphView`).
2. Graph dataset generator API endpoints (`GET /papers/{id}/citation-graph` and `GET /users/{id}/coauthor-graph`).
3. Detail bottom sheet for node preview and navigation.
```
