# Husna UX Decision Log

| # | Decision | Context & Rationale | Follow-up |
| --- | --- | --- | --- |
| 1 | Prioritized an end-to-end UX specification instead of prototype changes | The repo already has functioning Thymeleaf flows; the immediate need (per brief + `WORKING_PROTOCOL.md`) is documentation and gap analysis. Building new UI without requirements would violate “working prototype first”. | Keep spec updated as flows evolve; tie future PRDs to this doc. |
| 2 | Ranked issues by severity before suggesting polish | Some problems (schema drops, broken IDs, missing role guards) block deployments, so the issue log (`docs/ux/issues.md`) leads with S0/S1 defects before S3 polish. | Tackle issues in severity order; convert into tickets. |
| 3 | Reused existing component tokens from `app.css` | Consistency matters more than inventing new styles; spec references the current tokens (cards, buttons, tabs) and proposes incremental improvements (e.g., sr-only helper). | When introducing new UI, extend `app.css` with documented tokens rather than ad-hoc styles. |
| 4 | Proposed local HTMX bundling instead of adding new CDN deps | Project SOP forbids external dependencies when possible; HTMX is the only external script. Recommendation is to vendor it in `static/js` to keep offline parity. | Add build step (or manual copy) and update `base.html` once issue is picked up. |
| 5 | Logged permissions + accessibility gaps as UX issues, not code fixes | Dashboard access and filter labels impact UX but need product alignment. Capturing them in documentation makes them traceable without overstepping current sprint scope. | Convert to stories when roadmap allows; coordinate with product/legal before changing access model. |
