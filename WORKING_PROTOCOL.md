# Husna Working Protocol

Read this protocol before making any change.

1. **Confirm context**
   - Review the latest `CHECKLIST.md`, `UXFLOWCHECKLIST.md` (for any UX/UI-related work), and `WORKING_PROTOCOL.md` entries.
   - Review `docs/ux/husna-ux-flow.md`, `docs/ux/backlog.md`, and `docs/ux/decision-log.md` for the latest UX rules, open questions, and decisions.
   - Note active IDE tabs and any outstanding errors reported by the user.
   - If the task involves UX or UI changes (new screens, flows, states, or visual polish), identify which parts of `UXFLOWCHECKLIST.md` apply before designing or coding.

2. **Collect requirements**
   - Parse the full user prompt; highlight requested functionality, constraints, and known issues or error logs.
   - Log open questions in `CHECKLIST.md` if clarification is needed.
   - For UX/UI tasks, restate the user type, job-to-be-done, and start/end of the flow as required by `UXFLOWCHECKLIST.md`.

3. **Plan approach**
   - Prioritize delivering a working prototype; use placeholders for integrations involving third-party APIs, security configuration changes, or any external dependencies. Implement those only after the core flow works.
   - Outline key steps and affected files before modifying code.
   - For UX/UI work, outline the end-to-end flow (entry → key screens/states → exit) and list the required states (default, empty, loading, error, permission) before implementing.

4. **Review existing logic**
   - Inspect controllers, templates, and services relevant to the task.
   - Identify current behavior and gaps against the requirement.
   - For UX flows, check existing navigation, empty states, and micro-interactions so new changes don’t introduce inconsistent patterns.

5. **Implement incrementally**
   - Make cohesive, minimal changes per iteration.
   - Run targeted builds/tests after significant edits (e.g., `./mvnw -q -DskipTests compile`).
   - For UX/UI changes, implement core happy-path flow first, then layer in empty/error/edge states and micro-interactions as described in `UXFLOWCHECKLIST.md`.

6. **Handle errors immediately**
   - Reproduce reported issues locally when possible.
   - Capture relevant stack traces or logs and address them before moving on.
   - If the error is a UX gap (missing state, confusing navigation, inconsistent microcopy), document the gap and fix it in line with `UXFLOWCHECKLIST.md`.

7. **Validate**
   - Ensure new behavior aligns with specs and doesn’t break existing flows.
   - Verify UX requirements (empty/loading states, accessibility, navigation, feedback/micro-interactions) when applicable.
   - Cross-check any UX/UI changes against `UXFLOWCHECKLIST.md`:
      - Context & goals (user type, job-to-be-done, start/end of flow).
      - Screen-by-screen states (default, empty, loading, error, permission, edge cases).
      - Navigation & wayfinding (entry, back paths, breadcrumbs, CTAs).
      - Interactions, content/microcopy, and accessibility notes.

8. **Document updates**
   - Update `CHECKLIST.md`, `UXFLOWCHECKLIST.md` (if patterns, rules, or assumptions change), or other documentation to reflect new status, decisions, or assumptions.
   - Note any new UX patterns, components, or conventions introduced so they can be reused consistently.
   - Summarize changes, test results, and next steps in the final response.

9. **Deferral rule**
   - ALL AND ANY code that requires third-party APIs, security configuration changes, or comparable integrations must be done last. Priority is delivering a working prototype with placeholders or mocks for those dependencies until final implementation.

10. **Final review**
   - Re-read `WORKING_PROTOCOL.md`, `CHECKLIST.md`, and (for UX/UI work) `UXFLOWCHECKLIST.md` to confirm compliance.
   - Deliver a clear summary with follow-up recommendations for the user, including any UX assumptions and open questions that should be revisited later.
   - Update `CHANGELOG.md` with what was updated/changed and why.
