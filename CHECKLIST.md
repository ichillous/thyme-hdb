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

## Event Pages & “Heart” UX

| Item | Status | Notes |
| --- | --- | --- |
| Event detail content | ⚠️ | Title, times, venue, address, description render (`event_detail.html:6-25`), but missing maps link and org profile link. |
| Event detail content | ✅ | Detail page now links to Google Maps and organizer profile (`event_detail.html:6-32`, `controllers/ui/EventUiController.java:21-43`, `profile/ProfileService.java:72-77`). |
| Heart/Unheart (logged-in) | ✅ | HTMX toggle now receives initial liked/count data for both city cards and detail page (`events_city.html:30-56`, `event_detail.html:6-25`, `fragments/event_heart.html:5-23`). |
| Heart/Unheart (logged-out) | ✅ | Heart buttons launch the inline login modal and, after OTP verification, auto-toggle the heart via `nextTemplate` plumbing (`fragments/event_heart.html:6-23`, `fragments/auth.html:5-120`, `controllers/ui/AuthPageController.java:20-138`). |
| Display heart state/count on cards/detail | ✅ | List + detail views feed liked/count context to the shared fragment; HTMX toggle keeps counts in sync (`events_city.html:30-56`, `event_detail.html:6-25`, `EventUiController.java:21-43`). |

## Saved Events (General User)

| Item | Status | Notes |
| --- | --- | --- |
| My Saved page (Upcoming/Past sections) | ✅ | New `/saved` route displays upcoming/past tabs with heart controls (`controllers/ui/SavedEventsPageController.java`, `saved_events.html`). |
| Pagination 20/page with pager hide | ✅ | Saved events pager honors 20/page and hides buttons when not applicable (`saved_events.html:43-62`). |
| “Saved” nav link after login | ⚠️ | Nav shows Saved when `actorUserId` is present (`base.html:16-27`); still relies on query propagation. |

## Org Dashboard Enhancements

| Item | Status | Notes |
| --- | --- | --- |
| Create/Edit/Delete Event forms | ✅ | Added create/edit/delete flows with publish/draft actions (`OrgEventFormController.java`, `dashboard_org_event_form.html`). |
| Publish/Draft toggles & success alerts | ✅ | Buttons set draft/publish states and dashboard shows success banners (`dashboard_org_event_form.html:44-52`, `dashboard_org_page.html:12-28`). |
| Profile management UI (bio, branding, address, services, prayer schedule, donation) | ✅ | Editable via `/dashboard/org/profile/edit` form (`OrgProfileFormController.java`, `dashboard_org_profile_form.html`). |
| Prayer schedule editor (weekly grid + Jummah rows) | ✅ | Manual schedule editor with mode toggle and Jummah rows at `/dashboard/org/prayer-schedule` (`OrgPrayerScheduleController.java`, `dashboard_org_prayer_form.html`). |
| Dashboard nudges (“Next step” cards) | ✅ | Dashboard highlights missing setup areas with CTA cards (`dashboard_org_page.html:18-38`). |

## Public Org Profile Page

| Item | Status | Notes |
| --- | --- | --- |
| Route `/org/{orgUserId}` | ✅ | New public profile controller renders `/org/{orgUserId}` (`OrgPublicPageController.java`). |
| Sections (header, address, services, prayer times, donation, upcoming events) | ✅ | Template displays branding, contact info, prayer schedule, donation link, and paged events (`org_public.html`). |

## Auth Modals (UX Polish)

| Item | Status | Notes |
| --- | --- | --- |
| In-place modal triggers from any page | ❌ | Auth pages exist, but header links navigate instead of opening inline modals (`base.html:18-20`). |
| Post-success navigation cues | ❌ | No logic to show Saved link or Go to Dashboard CTA tied to login result. |
| Accessibility (focus trap, Esc, aria-modal, focus restore) | ❌ | Missing from modal markup (`fragments/auth.html`). |
| Error states (invalid OTP, duplicate signup, missing contact) | ✅ | Auth controller now surfaces inline errors for login/signup/verify, supports direct `/login` `/signup` pages, and keeps modals loaded (`controllers/ui/AuthPageController.java:18-118`, `fragments/auth.html`). |

## Navigation & States

| Item | Status | Notes |
| --- | --- | --- |
| Contextual nav (Saved / Org Dashboard visibility) | ⚠️ | Nav reveals Saved and Dashboard when `actorUserId` query param is present (`base.html:16-33`); still relies on URL propagation. |
| Breadcrumbs (Home → City → Event) | ❌ | Absent from city and event templates. |
| Empty states (no city events, no saved events, no org events) | ⚠️ | City events list shows “No events match” message; other contexts missing. |

## Content & Formatting

| Item | Status | Notes |
| --- | --- | --- |
| Consistent timezone display | ⚠️ | Templates use `#temporals.format(... 'z')`; ensure local vs tz logic aligns with spec. |
| Truncate long descriptions in lists | ❌ | City cards render full description (`events_city.html:32-34`). |
| Link org name on cards/details to profile | ✅ | City, saved, and detail views link to `/org/{orgUserId}` (`events_city.html:33-38`, `saved_events.html:24-28`, `event_detail.html:20-27`). |

## Micro-interactions

| Item | Status | Notes |
| --- | --- | --- |
| Toasts/snackbars for actions | ❌ | No toast UI or JS hooks. |
| Loading placeholders/skeletons for HTMX swaps | ❌ | Only spinner indicator (`base.html:35`); no skeleton components. |

## Open Questions & Assumptions

- City chip badge counts now reflect upcoming published events (`event/EventRepository.java:27-42`).
- What mechanism will persist post-OTP session state so the header can conditionally display “Saved” and “Org Dashboard” links?
- Confirm whether the auto-retoggle flow post-login is sufficient or if we should add user-visible confirmation/toast after hearting.
- Define product KPIs for Saved events and org dashboard adoption to guide micro-interactions (toasts, nudges) implementation.
- Auth modals now handle `/auth/otp/request` errors in-line; monitor for additional scenarios (e.g., rate limiting) once real auth is wired.
