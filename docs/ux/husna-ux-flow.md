# Husna UX Flow Specification

Author: Codex (UX/engineering audit) · Date: 2025-11-17

## 1. Sitemap & Surface Inventory
| Route / Entry | Controller | Template / Fragment | Primary Role(s) | Notes |
| --- | --- | --- | --- | --- |
| `/` `/home` | `HomePageController.home` (`src/main/java/.../HomePageController.java:27-33`) | `home.html` (`src/main/resources/templates/home.html`) | Guest, Signed-in | Shows cities list + empty state CTA to auth flow. |
| `/home/cities/{city}` | `HomePageController.cityPage` (`lines 45-87`) | `events_city.html` | Guest, Signed-in | Full-featured listing: search, sort, date filters, likes, paging. |
| `/events/{eventId}` | `EventUiController.detail` (`lines 26-63`) | `event_detail.html` | Guest, Signed-in | Event detail card with location/time info and heart button. |
| `/saved` | `SavedEventsPageController` | `saved_events.html` | Signed-in general user | Upcoming/past tabs, empty states, hearts. |
| `/org/{orgUserId}` | `OrgPublicPageController` | `org_public.html` | Guest, Signed-in | Org profile with iconized sections, pill-style services, single-row prayer schedule, donation info, events. |
| `/login` `/signup` | `AuthPageController` | `login.html` / `signup.html` wrapping `fragments/auth.html` | All roles | Modal-first flow powered by HTMX, OTP mock. |
| `/dashboard/org` | `OrgDashboardPageController` | `dashboard_org_page.html` + fragments | Org admins | KPIs, events table, nudges, CTA to form routes. |
| `/dashboard/org/events/{new|{id}/edit}` | `OrgEventFormController` | `dashboard_org_event_form.html` | Org admins | Create/edit event form, delete action. |
| `/dashboard/org/profile/edit` | `OrgProfileFormController` | `dashboard_org_profile_form.html` | Org admins | Org profile edit, location + donation settings. |
| `/dashboard/org/prayer-schedule` | `OrgPrayerScheduleController` | `dashboard_org_prayer_form.html` | Org admins (mosque) | Weekly schedule + Jummah slots. |
| `/auth/login/form`, `/auth/signup/form`, `/auth/otp/*` | `AuthPageController` | `fragments/auth.html` | All roles | HTMX fragments for modal stack. |

Static assets and tokens live in `app.css` (`src/main/resources/static/css/app.css`).

## 2. Roles & Permissions
- **Guest visitor**: browse cities, org pages, event detail; hearts trigger auth modal (`fragments/event_heart.html`).
- **Signed-in attendee (GENERAL_USER)**: everything guest can do + toggle hearts, view `/saved`. Currently can load `/dashboard/org` (see issue log) but mutations enforce org role deeper in the stack.
- **Org admin (ORG_ADMIN)**: access dashboard, profile + event management, prayer schedule (`OrgEventFormController`, `OrgProfileFormController`, `OrgPrayerScheduleController`).
- **Future roles (TEACHER, SUPER_ADMIN)**: modelled in enums but not surfaced in UI.

## 3. Primary User Journeys
1. **Discover + Save (guest → attendee)**
   - Home chips → City listing (`events_city.html`) → heart button triggers login modal (`fragments/auth :: login`) → OTP verify → auto-toggle heart via `nextTemplate` (`fragments/auth.html:84-113`).
   - Non-happy: invalid OTP surfaces inline alert; still inside modal.
2. **Saved → Attend (attendee)**
   - `/saved` tabs switch upcoming/past (`saved_events.html:5-66`).
   - CTA to event detail or organizer profile; hearts remain interactive.
3. **Org Publish Flow (org admin)**
   - `/dashboard/org` summary + nudges → `Create event` CTA → event form (defaulting to profile address) → action buttons publish/draft (`dashboard_org_event_form.html:34-78`).
   - After publish, redirected back with `success` query flag to show toast/alert.
4. **Org Profile Maintenance**
   - Dashboard CTA → `/dashboard/org/profile/edit` (`dashboard_org_profile_form.html`) for branding/location/donations.
   - For mosques, prayer schedule CTA leads to weekly editor, stored through `ProfileService.replacePrayerSchedule`.

## 4. Screen Inventory & States
Each surface includes the default, empty, loading, error/guard, and responsive considerations.

### Home (`home.html`)
- **Default**: Stacked chips of cities, each showing state abbreviation and event count. Uses `HomeService.listActiveCities`.
- **Empty**: Card with CTA “Create an event” linking to login/signup.
- **Loading**: No skeleton; relies on base layout’s page load.
- **Error/guard**: None; consider offline banner if fetch fails.
- **Responsive**: Chip list wraps.

### City Listing (`events_city.html`)
- **Default**: Filter row (search + date + sort + submit), list of cards with actions + location fallback + time chunk.
- **Empty**: Inline message “No events match your filters” (line 71); propose linking back to `/home`.
- **Loading**: None; consider `data-skeleton` on list container for HTMX-driven pagination.
- **Errors**: Input validation relies on backend; invalid `state` query is ignored by `parseState`.
- **Permission**: Heart button uses modal for guests.
- **Responsive**: 2-column `.item` layout collapses via CSS grid.

### Event Detail (`event_detail.html`)
- **Default**: Breadcrumb -> card with title, schedule, location fallback to organizer profile, Google Maps link, organizer link, description, heart.
- **Empty states**: Not applicable; 404 thrown if event not found/unpublished.
- **Loading**: Full page load.
- **Error**: 404 for drafts to non-owners (controller lines 31-39).

### Saved Events (`saved_events.html`)
- **Default**: Tabs for upcoming/past, cards show metadata + hearts.
- **Empty**: Tab-specific copy + CTA to `/home` (lines 68-82).
- **Loading**: Page load only.
- **Permission**: Controller redirects guests to `/login?next=/saved`.

### Org Public (`org_public.html`)
- **Default**: Branding card, address block, donation alert, services/programs/classes (with icons), single-row prayer schedule (mosques), and events tabs with icons.
- **Empty**: Services/programs sections hidden when data missing; events show tab-specific empties.
- **Loading**: Page load.
- **Permission**: Throws 404 when profile not public.
- **Known gap**: Event cards lack location fallback when no override (see Issue #5).

### Auth Modals (`fragments/auth.html`)
- **Default**: Login → OTP request → verify → success, with state stitched through HTMX swaps.
- **Empty/error states**: Inline alerts for missing/invalid contact or OTP; dev-only code display for MVP.
- **Loading**: Skeleton overlay via `data-skeleton="modal"` on modal root + `#hx-indicator` spinners.
- **Accessibility**: Base script traps focus, closes on ESC, and restores trigger focus.

### Org Dashboard (`dashboard_org_page.html` + fragments)
- **Default**: Hero + CTAs, alerts, nudges, summary KPIs (`fragments/dashboard_org :: summary`), events table (`::events`) with HTMX paging.
- **Empty**: Nudges for missing data, table row “No events yet.”
- **Loading**: `data-skeleton="table"` on the events fragment; global indicator for HTMX.
- **Permissions**: Should be restricted to org admins; currently missing guard (Issue #4).

### Event Form (`dashboard_org_event_form.html`)
- **Default**: Title/description inputs, venue toggle, timezone, start/end, action buttons.
- **Validation**: HTML `required` + service-level checks for timezone, start/end, state.
- **Empty**: Inline info alert explaining default venue behavior.
- **Errors**: `error` model attribute displayed at top when service throws.
- **Edge case**: Delete action uses confirm dialog.

### Org Profile Form / Prayer Schedule
- Similar patterns: stack layout, fieldsets with inputs, warnings for incomplete profile (`notice == complete_profile`).
- Prayer schedule ensures a single row for week (applies Sat→Fri) and up to three Jummah rows; instructions at top describe leaving blank to clear.

## 5. Navigation & Wayfinding
- **Global nav** (`base.html:15-38`): brand + Home + conditional Saved/Dashboard + login/signup triggers. No active state indicator; consider adding.
- **Breadcrumbs**: Only event detail uses `<nav class="breadcrumbs">` (line 5). Recommend expanding to dashboard forms.
- **Back paths**: Most forms link back to dashboard via “← Back to dashboard”. City listing links to org pages; event detail back references city.
- **Auth modal reentry**: `next` and `nextTemplate` allow continuing to the desired screen after login/OTP.

## 6. Forms & Validation Rules
| Form | Key Fields | Validation | Notes |
| --- | --- | --- | --- |
| Login/Signup modals | contact, OTP | server-side uses `UserService` normalization; OTP limited to 5 attempts | Mock OTP flows show dev code snippet. |
| Event form | title, description, venue override toggle, timezone, start/end | Title required, timezone required, start/end required with `end >= start`, state required when using override (`EventService.createEvent`). | `useDifferentVenue` toggles override block via inline JS. |
| Profile form | display name, contacts, location, donation | Display name required; restful ensures org-only fields when `ORG_ADMIN`. | `services` checkboxes map to enums. |
| Prayer schedule | Single weekly row + up to 3 Jummah entries | Accepts ISO times; blanks clear stored values; only mosques can access. |

## 7. Micro-interactions, Feedback & Loading
- **Toasts**: `window.husna.toast` stack defined in `base.html:125-189`; `fragments/event_heart` attaches `data-toast-*` for success/info messaging.
- **HTMX**: Bundled locally at `/js/htmx.min.js` with an automatic CDN fallback if the asset is missing; loading states still rely on `#hx-indicator` and the `data-skeleton` helper script (`base.html:190-266`).
- **Modal focus trap**: JS in `base.html:52-124` tracks triggers, traps TAB, handles ESC, and restores focus.
- **Form submit guard**: script (`base.html:267-282`) enforces explicit submit buttons for forms that set `data-require-explicit-submit` (event form) to prevent accidental Enter submissions.

## 8. Accessibility & Content Rules
- **ARIA/live regions**: Toast stack `aria-live="polite"`; indicator `aria-hidden="true"` by default.
- **Color tokens**: `app.css` defines `--bg`, `--fg`, `--accent`, etc. with sufficient contrast in dark theme.
- **Typography**: System font stack; `.muted` for explanatory copy.
- **Date/time**: All times render via `#temporals.format(e.startAtZoned, 'MMM d, yyyy HH:mm z')`, ensuring timezone from `Event.getResolvedZone` (UTC fallback).
- **Labels & visually-hidden helpers**: `.sr-only` class supports accessible labels on controls like the city filter; remaining gaps include reliance on color-only indicators for heart state (aria-pressed present, but consider extra screen reader copy).

## 9. Non-Happy Paths & Edge Cases
- Missing city: `/home/cities/{city}` with unknown name returns empty page dataset (should show “City not found”).
- Invalid OTP/contact: `AuthPageController` maps errors to friendly copy (`mapAuthError`).
- Draft event detail: returns 404 for non-owners (ResponseStatusException); saved list currently still surfaces those drafts.
- Dashboard forms: `profile_incomplete` triggers redirect to profile edit with `notice=complete_profile` banner.
- Prayer schedule: Non-mosque org hitting route gets redirect with `error` query.
- Large event sets: `OrgDashboardService.summary` loads all events to compute stats (perf risks for high volume); monitoring recommended.

## 10. Component & Token Specs
- **Buttons**: `.btn` uses `var(--chip)` background, 8px radius, border 1px `var(--line)`. Ghost variant transparent. Danger variant only on delete button (reuses `.btn danger` class? prone to add).
- **Cards**: `.card` background `var(--card)`, border radius `var(--radius) = 12px`, padding `var(--pad) = 14px`, box-shadow `var(--shadow)`.
- **Tabs**: `.tabs` bottom border + `.tab.active` accent border.
- **Chips/Badges**: `.chip` for cities, `.badge` for counts.
- **Stack/Grid utilities**: `.stack`, `.row`, `.grid.two/.three` used widely; responsive break at 800px (see `app.css:10-40`).
- **Alerts**: `.alert.{info|warn|error|success}` change border color; use for inline errors.
- **Utility helpers**: `.sr-only` class available for visually-hidden labels; continue to codify button sizes and disabled-state tokens next.

## 11. Open Questions & KPIs
- **Data governance**: When should drafts disappear from attendee surfaces? Need policy + instrumentation.
- **Org onboarding**: Should `/dashboard/org` be hidden until `ORG_ADMIN` role assigned? Define upgrade funnel.
- **KPIs**:
  1. City listing engagement: filter usage rate, heart conversion.
  2. Saved events retention: % of logged-in users with ≥1 saved event.
  3. Org health: Avg time from event draft to publish, % of orgs with complete profile/donation info.
  4. Prayer schedule adoption among mosques.
- **Pending decisions**:
  - Local vs CDN asset strategy (see Issue #8).
  - Search relevance improvements (title/venue only today; consider tags/services).
  - Mobile nav toggle (current nav assumes desktop width).

## 12. Proposed Follow-ups (linked to issue log)
- Add accessible labels to city filters (`events_city.html`) and replicate for saved filters.
- Add location fallback in `org_public.html` using `profile` data.
- Introduce proper role guards for dashboard routes and consider dedicated “Upgrade to organizer” CTA for general users.
- Bundle HTMX locally inside `/static/js` and document versioning in `CHANGELOG.md`.
- Replace destructive schema init with migrations to unblock persistent deployments.
