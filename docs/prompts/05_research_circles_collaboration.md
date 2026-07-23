# Prompt 5: Research Circles & Co-Author Collaboration Workspaces

```markdown
Role: Full-Stack Mobile & Collaboration Architect

Task:
Upgrade the existing Circles feature into a full Collaborative Research Workspace, providing lab groups and research teams with shared draft editing, co-author review workflows, reading lists, and project pin boards.

Features & Requirements:

1. Circle Workspace Tabs:
   - Expand Circle screen to include 4 tabs: "Feed" (Discussions), "Papers" (Circle Publications), "Workspace" (Active Drafts & Collaboration), "Members" (Lab / Group Members).

2. Collaborative Draft Authoring & Peer Review:
   - Active Draft List: Members can upload working drafts (PDF/Docx/Markdown).
   - Co-Author Review Requests: Lead author can invite specific circle members or connection network to review draft sections prior to submission.
   - Inline Section Comments: Comment threads anchored to specific draft paragraphs/sections.

3. Shared Circle Reading Lists:
   - Shared Repository: Circle members can add papers from the master library into shared reading lists (e.g. "Weekly Lab Seminar Papers", "Lit Review 2026").
   - One-tap "Save to My Library" action for circle members.

4. Circle Membership & Role Management:
   - Roles: Circle Admin, Lead Researcher, Contributor, Guest Observer.
   - Private / Public Circle Access Controls: Invite code generation and membership request approval workflow.

Deliverables:
1. `backend/schemas.py` & DB tables for `circle_workspaces`, `circle_drafts`, `draft_comments`, `circle_reading_lists`.
2. Updated Circle Screen layout with tab navigation (`(CircleTabs)`).
3. Draft Review & Commenting UI screens.
```
