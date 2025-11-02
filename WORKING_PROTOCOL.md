# Husna Working Protocol

Read this protocol before making any change.

1. **Confirm context**
   - Review the latest `CHECKLIST.md` and `WORKING_PROTOCOL.md` entries.
   - Note active IDE tabs and any outstanding errors reported by the user.
2. **Collect requirements**
   - Parse the full user prompt; highlight requested functionality, constraints, and known issues or error logs.
   - Log open questions in `CHECKLIST.md` if clarification is needed.
3. **Plan approach**
   - Prioritize delivering a working prototype; use placeholders for integrations involving third-party APIs, security configuration changes, or any external dependencies. Implement those only after the core flow works.
   - Outline key steps and affected files before modifying code.
4. **Review existing logic**
   - Inspect controllers, templates, and services relevant to the task.
   - Identify current behavior and gaps against the requirement.
5. **Implement incrementally**
   - Make cohesive, minimal changes per iteration.
   - Run targeted builds/tests after significant edits (e.g., `./mvnw -q -DskipTests compile`).
6. **Handle errors immediately**
   - Reproduce reported issues locally when possible.
   - Capture relevant stack traces or logs and address them before moving on.
7. **Validate**
   - Ensure new behavior aligns with specs and doesn’t break existing flows.
   - Verify UX requirements (empty/loading states, accessibility, etc.) when applicable.
8. **Document updates**
   - Update `CHECKLIST.md` or other documentation to reflect new status, decisions, or assumptions.
   - Summarize changes, test results, and next steps in the final response.
9. **Deferral rule**
   - ALL AND ANY code that requires third-party APIs, security configuration changes, or comparable integrations must be done last. Priority is delivering a working prototype with placeholders or mocks for those dependencies until final implementation.
10. **Final review**
    - Re-read `WORKING_PROTOCOL.md` to confirm compliance.
    - Deliver a clear summary with follow-up recommendations for the user.
