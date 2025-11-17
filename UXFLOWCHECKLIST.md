# Husna UX Flow Checklist

Use this checklist **whenever adding or changing UX/UI** in Husna. The goal is to design full flows, not isolated screens.

---

## A. Confirm Context

Before proposing any UI:

- **User type**
    - Who is this for?
        - e.g. `Guest visitor`, `Signed-in user`, `Org admin`, `Super admin`.
- **Job-to-be-done**
    - Phrase as:
        - “User wants to **[goal]** so that **[outcome]**.”
- **Flow boundaries**
    - Where does the flow start?
        - Entry points (URL, nav item, button, modal trigger).
    - Where should it end?
        - What does “done” look like for the user (success state)?
- **User state**
    - Logged-in or logged-out?
    - Org profile complete or incomplete?
    - Has events / has saved events / has no data?

> If any of the above are unknown, explicitly list them as **assumptions** or **open questions** before designing.

---

## B. Map the End-to-End Flow

Describe the full journey, not just one screen.

For this feature/flow:

- **Step list**
    - Step 1 → Step 2 → Step 3 … until success or exit.
- For each step, specify:
    - **Screen/View name**
    - **User actions**
        - What can they do here? (view, filter, heart, edit, delete, cancel, go back)
    - **System changes**
        - What data or state changes after each action?
        - e.g. “Heart event → creates Like record + updates count; shows toast; keeps scroll position.”

---

## C. Required UI States (Per Screen/Component)

For every screen or key component touched, define:

- **Default / happy path**
    - Normal state with valid data.
- **Empty state**
    - No cities, no events, no saved items, no org profile, no search results, etc.
    - Include:
        - Short explanation of what’s happening.
        - Clear CTA (e.g. “Browse cities”, “Create event”, “Update profile”).
- **Loading state**
    - Skeletons, spinners, or disabled buttons/links.
    - For HTMX/async flows: what shows while the fragment is loading?
- **Error states**
    - Validation errors (forms).
    - System errors (unexpected failure, but expressed as friendly UI).
    - Connectivity/timeouts (retry options, explanatory text).
- **Permission/guard states**
    - Not logged in → auth modal or redirect.
    - Logged in but missing required setup (e.g. org profile incomplete).
    - Forbidden/unauthorized access.
- **Edge cases**
    - Past vs upcoming events.
    - Deleted/unpublished/hidden items.
    - Pagination edges (only one page, last page, no results).
- **Responsive behavior**
    - Minimum: describe differences for **mobile vs desktop**:
        - Navigation behavior.
        - Card vs table layout.
        - Modals vs full-screen overlays.

---

## D. Navigation & Wayfinding

For any new or changed flow:

- **Entry**
    - From which pages/routes can the user start this flow?
    - Are we exposing it via:
        - Global nav?
        - Local links/buttons?
        - Inline CTA from empty states?
- **Back paths**
    - How does the user:
        - Go back to where they came from?
        - Return to `Home`, `City`, `Org profile`, or `Dashboard`?
    - Do we need:
        - Breadcrumbs (e.g. `Home → City → Event`)?
        - Back links (e.g. “Back to events”)?
- **Cross-linking**
    - Cards → detail pages.
    - Detail pages → org profiles.
    - Empty states → relevant flows (create/edit/complete profile).

The user should never feel “stuck” or lost in the flow.

---

## E. Interaction & Micro-interactions

For each interactive element (button, link, toggle, HTMX fragment, etc.):

- **States**
    - Normal
    - Hover/focus
    - Disabled
    - Loading (showing something while action is in progress).
- **Feedback**
    - Toast/snackbar vs inline messages.
    - Counter/badge updates (e.g. like counts).
    - Visual confirmation of success or failure.
- **Async behavior**
    - For HTMX/async calls:
        - What fragment updates?
        - What loading indicator shows?
        - Do we optimistically update UI or wait for response?
- **Double-submit protection**
    - Disable buttons / show loading state while request is in flight.
    - Avoid duplicate actions on rapid clicks.

Reuse existing Husna patterns:
- Toast stack.
- Skeleton loading states.
- Tabs, alerts, nudges, sticky footer, etc.

---

## F. Content & Microcopy

For each flow/screen:

- **Headings & labels**
    - Clear, concise, and task-oriented.
    - Match existing tone in Husna.
- **Empty-state copy**
    - Explain:
        - What’s going on (e.g. “You don’t have any saved events yet”).
        - What the user can do next (CTA).
- **Error copy**
    - Include:
        - What went wrong.
        - How the user can fix it (if applicable).
- **Buttons & links**
    - Use action verbs that match user intent:
        - “Save event”, “Create event”, “Update profile”, “Go to dashboard”.
- **Consistency**
    - Prefer reusing existing wording patterns before inventing new ones.

---

## G. Accessibility

For each new/changed flow:

- **Keyboard access**
    - All interactive elements are reachable and usable via keyboard.
- **Focus management**
    - Modals:
        - Trap focus inside.
        - Close with ESC.
        - Restore focus to triggering element on close.
    - On validation error:
        - Focus the first invalid field or error summary.
- **Semantics**
    - Proper heading structure (h1 → h2 → h3).
    - Correct roles (dialogs, alerts, navigation, etc.).
- **Color & contrast**
    - Don’t use color alone to convey meaning.
    - Ensure sufficient contrast for text and important controls.

---

## H. Validation Against Existing Husna Protocol

Before finalizing changes:

- **Check `WORKING_PROTOCOL.md`**
    - Confirm:
        - You’ve respected the “working prototype first” rule.
        - External integrations / security changes are not blocking core UX.
- **Check `CHECKLIST.md`**
    - Update or reference:
        - Any related items (empty states, sticky footer, venue model, auth modals, etc.).
- **Impact**
    - List affected layers:
        - Routes/controllers.
        - Templates/partials.
        - Services/repositories (if behavior changes).
- **Risks & assumptions**
    - Note:
        - Potential 500/edge-case conditions (e.g., missing data, legacy state).
        - Any assumptions made due to missing requirements.
        - Open questions to clarify later.

---

**Rule of thumb:**  
If a UX change only defines a pretty screen without its **context, states, navigation, and feedback**, it is **not done**.
