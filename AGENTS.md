# Husna MCP Agent Operating Procedure (SOP)

This SOP is the default working agreement for **all** MCP agents operating on the Husna repo during the prototype phase.

## Invariants (must always hold)
- **No third‑party API integration** and **no unit tests** until the project owner switches to the `integration` or `testing` profiles.
- Keep all work local. Assume **no outbound network**.
- Prefer small, reversible changes with clear commit messages.
- When in doubt, **re‑read this SOP and `WORKING_PROTOCOL.md`**.

## Required Flow (follow in order for every task)

1) **View `WORKING_PROTOCOL.md`**
   - Purpose: refresh guardrails and conventions.
   - Suggested tools: MCP filesystem `readFile` or editor `open`.

2) **View `CHECKLIST.md`**
   - Purpose: align on the current todo list and in‑progress items.

3) **Think of changes needed**
   - Produce a short plan: bullets of files to touch, functions to add/modify, data model impacts.

4) **Think of best way to implement**
   - Note risks, migrations, or local run steps. Keep scope minimal to satisfy the prompt.

5) **Think of edge cases / blind spots**
   - Consider: empty inputs, bad inputs, large inputs, race conditions, i18n/encoding, performance, permissions/auth, nulls, date/time, failure modes, rollback.

6) **Implement changes**
   - Edit code in small commits. Keep notes of what was changed and why.
   - Run locally and capture output logs when relevant.

7) **View `WORKING_PROTOCOL.md` again**
   - Re‑check naming, layout, and conventions.

8) **Verify `WORKING_PROTOCOL.md` was followed**
   - If any rule was bent, explain why and propose a follow‑up checklist item.

9) **Update `CHECKLIST.md`**
   - Append ` (DONE)` to items completed in this run. Add follow‑ups if needed.

## Definition of Done (DoD)
- Task plan + edge cases documented in the run output.
- Code builds locally with the current profile.
- `WORKING_PROTOCOL.md` re‑checked.
- `CHECKLIST.md` updated with `(DONE)` where applicable.

## Commit Message Template
```
<scope>: <short summary>

Why:
- <reason>

Changes:
- <file>: <what/why>

Notes:
- Edge cases handled: <list>
- Follow‑ups: <tickets or bullets>
```
