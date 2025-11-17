# Husna Feature Checklist

Status key: ✅ complete · ⚠️ partial/incomplete · ❌ not started

> Last reviewed: leverage ProductAgent prompt analysis (assumptions tracked below).

## Home & City → Events Flow

| Item | Status | Notes |
| --- | --- | --- |
| Home shows only cities | ✅ | Implemented via `home.html:6-17` using `HomeService.listActiveCities()` (`home/HomeService.java:17-32`). |
| “No cities yet” empty state + CTA | ✅ | Added explanatory card with CTA linking to login → signup in `home.html:8-17`. |
| Define badge count logic | ✅ | Chips now count upcoming published events by filtering with `EventRepository.listActiveCities(Instant now)` and `HomeService.listActiveCities()` (`event/EventRepository.java:27-42`, `home/HomeService.java:20-29`). |

## City Events Page

| Item | Status | Notes |
| --- | --- | --- |
| Page size 20 with pager hide on single page | ✅ | `CityEventsService.searchCityEvents()` fixes size to 20; template hides pager when `totalPages > 1` (`events_city.html:28-71`). |
| Sort options (Newest/Soonest) | ✅ | Dropdown wired to `DateFilter` + `SortKey` enums (`events_city.html:12-25`, `CityEventsService.java:32-36`). |
| Date filter (Week/Month/All) | ✅ | Implemented in UI and specification builder (`events_city.html:12-25`, `CityEventsService.java:38-62`). |
| Search title/venue | ✅ | Query parameter `q` handled via `Specification` (`CityEventsService.java:41-67`). |
| Card actions: View + Heart/Unheart | ✅ | Cards preload the user’s liked state and total counts, reusing the HTMX toggle fragment for inline updates (`events_city.html:30-56`, `fragments/event_heart.html:5-23`). |
| Layout uses base template (root `th:replace`) | ✅ | `events_city.html` now keeps `th:replace` on the `<html>` element so the base layout/styles load correctly. (DONE) |
| Card layout polish (spacing/typography/accessibility) | ✅ | Media-style layout with consistent spacing, aria-labels, and improved actions alignment (`events_city.html:1-160`, `app.css`). (DONE) |

## Event Pages & “Heart” UX

| Item | Status | Notes |
| --- | --- | --- |
| Restrict detail page to published events | ✅ | `EventUiController` now returns 404 for drafts unless the viewer owns the event. (DONE) |
| Event detail content | ✅ | Detail page now links to Google Maps and organizer profile (`event_detail.html:6-32`, `controllers/ui/EventUiController.java:21-43`, `profile/ProfileService.java:72-77`). |
| Heart/Unheart (logged-in) | ✅ | HTMX toggle now receives initial liked/count data for both city cards and detail page (`events_city.html:30-56`, `event_detail.html:6-25`, `fragments/event_heart.html:5-23`). |
| Heart/Unheart (logged-out) | ✅ | Heart buttons launch the inline login modal and, after OTP verification, auto-toggle the heart via `nextTemplate` plumbing (`fragments/event_heart.html:6-23`, `fragments/auth.html:5-120`, `controllers/ui/AuthPageController.java:20-138`). |
| Display heart state/count on cards/detail | ✅ | List + detail views feed liked/count context to the shared fragment; HTMX toggle keeps counts in sync (`events_city.html:30-56`, `event_detail.html:6-25`, `EventUiController.java:21-43`). |

## Saved Events (General User)

| Item | Status | Notes |
| --- | --- | --- |
| My Saved page (Upcoming/Past sections) | ✅ | New `/saved` route displays upcoming/past tabs with heart controls (`controllers/ui/SavedEventsPageController.java`, `saved_events.html`). |
| Pagination 20/page with pager hide | ✅ | Saved events pager honors 20/page and hides buttons when not applicable (`saved_events.html:43-62`). |
| “Saved” nav link after login | ✅ | Header now drives Saved visibility off the session-backed `actorUserId` model attribute (`base.html:15-41`). |
| Batch like counts for list rows | ✅ | `/saved` preloads like counts via `countLikesByEventIds(...)` to avoid N+1 queries. (DONE) |

## Org Dashboard Enhancements

| Item | Status | Notes |
| --- | --- | --- |
| Create/Edit/Delete Event forms | ✅ | Added create/edit/delete flows with publish/draft actions (`OrgEventFormController.java`, `dashboard_org_event_form.html`). |
| Publish/Draft toggles & success alerts | ✅ | Buttons set draft/publish states and dashboard shows success banners (`dashboard_org_event_form.html:44-52`, `dashboard_org_page.html:12-28`). |
| Profile management UI (bio, branding, address, services, prayer schedule, donation) | ✅ | Editable via `/dashboard/org/profile/edit` form (`OrgProfileFormController.java`, `dashboard_org_profile_form.html`). |
| Prayer schedule editor (weekly grid + Jummah rows) | ✅ | Manual schedule editor now collects a single set of times applied Saturday→Friday plus optional Jummah rows at `/dashboard/org/prayer-schedule` (`OrgPrayerScheduleController.java`, `dashboard_org_prayer_form.html`). (DONE) |
| Prayer schedule route no longer 500s | ✅ | Null-safe form builder prevents legacy/null mode from crashing GET; unauthorized remains 401 (`OrgPrayerScheduleController.java:65-112`). (DONE) |
| Mosque-only prayer schedule access | ✅ | Dashboard hides the CTA unless `OrgType.MOSQUE` and redirects non-mosques with an error message (`OrgDashboardService.java`, `dashboard_org_page.html`, `OrgPrayerScheduleController.java`). (DONE) |
| Dashboard nudges (“Next step” cards) | ✅ | Dashboard highlights missing setup areas with CTA cards (`dashboard_org_page.html:18-38`). |
| Server-side filtering + ownership guard | ✅ | Dashboard filters now run in the DB via `Specification` and edit forms load events through `EventService.getOrgOwnedEvent(...)` so only owners can view drafts. (DONE) |
| Require profile completion before events | ✅ | `ProfileService.requireOrgProfileComplete()` plus `/dashboard/org/profile/edit?notice=complete_profile` gating ensures orgs finish contact + address details before accessing event forms (`OrgEventFormController.java`, `ProfileService.java`). (DONE) |

## Public Org Profile Page

| Item | Status | Notes |
| --- | --- | --- |
| Route `/org/{orgUserId}` | ✅ | New public profile controller renders `/org/{orgUserId}` (`OrgPublicPageController.java`). |
| Sections (header, address, services, prayer times, donation, upcoming events) | ✅ | Template displays branding, contact info, prayer schedule, donation link, and paged events (`org_public.html`). |
| Guard + efficient likes lookup | ✅ | Org public pages now enforce `publicProfile`, reuse batched like queries, and `ProfileService.getOrgPublic()` only returns published events. (DONE) |
| Organizer profile link never 500s | ✅ | Missing org profiles are auto-bootstrapped and services load eagerly, so clicking an organizer’s name always works even before they customize their profile (`ProfileService.java`, `Profile.java`). (DONE) |
| Prayer/Jummah display for mosques | ✅ | Mosques with saved prayer or Jummah times now show them Saturday→Friday on their public profile, and the template guards against type mismatches when checking `OrgType` (`org_public.html`, `OrgPublicPageController.java`). (DONE) |

## Auth Modals (UX Polish)

| Item | Status | Notes |
| --- | --- | --- |
| In-place modal triggers from any page | ✅ | Header links now fetch modals with HTMX and keep the page in place (`src/main/resources/templates/base.html:23-36`). |
| Post-success navigation cues | ✅ | Login success dialog offers direct Saved/Dashboard buttons keyed to the signed-in user (`src/main/resources/templates/fragments/auth.html:88-118`). |
| Accessibility (focus trap, Esc, aria-modal, focus restore) | ✅ | Modals embed dialog semantics and a global helper traps focus, supports Esc, and restores the trigger (`src/main/resources/templates/fragments/auth.html:6-181`, `src/main/resources/templates/base.html:54-185`). |
| Error states (invalid OTP, duplicate signup, missing contact) | ✅ | Auth controller now surfaces inline errors for login/signup/verify, supports direct `/login` `/signup` pages, and keeps modals loaded (`controllers/ui/AuthPageController.java:18-118`, `fragments/auth.html`). |

## Authentication & Session

| Item | Status | Notes |
| --- | --- | --- |
| Persist login state without actorUserId query params | ✅ | Mock auth now stores a signed-in session cookie and exposes `actorUserId` via interceptor/model advice (`auth/AuthSessionService.java`, `auth/AuthSessionInterceptor.java`, `auth/ActorModelAdvice.java`). |
| OTP expiry and throttling | ✅ | OTP entries carry a 5-minute TTL, throttle after 3 requests/minute, and lock after repeated failures (`user/UserService.java:92-150`). |

## Navigation & States

| Item | Status | Notes |
| --- | --- | --- |
| Contextual nav (Saved / Org Dashboard visibility) | ✅ | Header links react to the session-derived `actorUserId` attribute instead of query params (`base.html:18-38`). |
| Breadcrumbs (Home → City → Event) | ✅ | Added semantic breadcrumbs on city and event detail templates linking back to Home (`events_city.html:1-82`, `event_detail.html:1-55`). |
| Empty states (no city events, no saved events, no org events) | ✅ | Saved and org views now show descriptive empty states with CTAs, and public org pages guide visitors when no events exist (`saved_events.html:35-67`, `dashboard_org.html:24-70`, `org_public.html:1-160`). |

## Content & Formatting

| Item | Status | Notes |
| --- | --- | --- |
| Consistent timezone display | ✅ | `Event` exposes zoned accessors and all templates format `startAtZoned`/`endAtZoned`, so user-specified timezones render accurately. (DONE) |
| Truncate long descriptions in lists | ✅ | List cards share a `.clamp-3` utility to keep descriptions to three lines. (DONE) |
| Link org name on cards/details to profile | ✅ | City, saved, and detail views link to `/org/{orgUserId}` (`events_city.html:33-38`, `saved_events.html:24-28`, `event_detail.html:20-27`). |
| Component styles (tabs, alerts, nudges) | ✅ | `app.css` now defines `.tabs`, `.tab`, `.alert` variants, `.nudges`, utility gaps, `.center`, and `.clamp-3`. (DONE) |
| Sticky footer | ✅ | Flex column layout on body with `main` flex:1 keeps footer at bottom on short pages (`app.css:10-12`). (DONE) |

## Micro-interactions

| Item | Status | Notes |
| --- | --- | --- |
| Toasts/snackbars for actions | ✅ | Added reusable toast stack + HTMX hook in `base.html` and wired heart fragment metadata so likes/unlikes raise contextual alerts. (DONE) |
| Loading placeholders/skeletons for HTMX swaps | ✅ | HTMX targets now declare `data-skeleton` and a shared JS/CSS pipeline renders shimmering placeholders for dashboard tables + auth modals. (DONE) |

## Infrastructure & Config

| Item | Status | Notes |
| --- | --- | --- |
| Security filter chain ordering | ✅ | The H2-only chain now matches just `/h2-console/**` and `/localhost/**`, allowing the general chain (with CORS/session settings) to process the rest. (DONE) |
| Trim unused security deps | ✅ | Removed unused OAuth2/KMS/JWT dependencies from `pom.xml` to keep the surface lean. (DONE) |

## Open Questions & Assumptions

- City chip badge counts now reflect upcoming published events (`event/EventRepository.java:27-42`).
- Current session storage is in-memory (`AuthSessionService`); decide on persistent/session-store strategy for multi-node or restart scenarios.
- Confirm whether the auto-retoggle flow post-login is sufficient or if we should add user-visible confirmation/toast after hearting.
- Define product KPIs for Saved events and org dashboard adoption to guide micro-interactions (toasts, nudges) implementation.
- Auth modals now handle `/auth/otp/request` errors in-line; monitor for additional scenarios (e.g., rate limiting) once real auth is wired.

## UX Documentation & Audits

| Item | Status | Notes |
| --- | --- | --- |
| Repository-wide UX flow spec | ✅ (DONE) | Added `docs/ux/husna-ux-flow.md` capturing sitemap, roles, journeys, states, and accessibility notes so designers/devs share a single playbook. |
| Ranked issue log | ✅ (DONE) | Logged critical-to-polish findings in `docs/ux/issues.md` with severity, repro, root cause, and fix proposals tied to code locations. |
| Decision log | ✅ (DONE) | Documented key tradeoffs for this audit in `docs/ux/decision-log.md` to preserve rationale. |
| Follow-up: ticketize issues | ✅ (DONE) | Converted issue log into actionable tickets in `docs/ux/backlog.md`, ordered by severity with acceptance criteria. |


## Notes and bugs I noticed
- (DONE) Profiles must now be completed before an org accesses the event form or creates an event; incomplete orgs are redirected to `/dashboard/org/profile/edit`. 
- (DONE) Clicking an organizer profile no longer throws 500 errors because profiles are auto-created and load without lazy-init issues.
- (DONE) `/dashboard/org/prayer-schedule` 500 on GET fixed by adding null-safe defaults in form builder for legacy profiles without `prayerTimesMode`.

## Venue Model & UX

| Item | Status | Notes |
| --- | --- | --- |
| Default location = org address | ✅ | Create/edit forms default to the org profile address unless “Use different venue” is checked, and that override panel only appears when the box is checked (`dashboard_org_event_form.html`, `OrgEventFormController.java`). (DONE) |
| Override-only venue fields | ✅ | Templates fall back to org address when event fields are blank (`events_city.html`, `event_detail.html`). |
| Adapter (server-side prefill) | ✅ | Controller pre-fills request with org address when using default to satisfy validation (`OrgEventFormController.java:26-120`). |
| Follow-up: migration & tests | ⚠️ | Add migration to split override columns, and add integration tests under the testing profile. |
