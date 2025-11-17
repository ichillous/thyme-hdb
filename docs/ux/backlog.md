# Husna Backlog (Derived from docs/ux/issues.md)

> Prioritization order: resolve S0 → S1 → S2 → S3. Ticket IDs map directly to the issue log sections for easy traceability.

## S0 — Blockers / Data Integrity

### UX-001 · Stop schema init from nuking data on boot
- **Severity**: S0 (Environment)
- **Source**: `docs/ux/issues.md#L7`
- **Summary**: `application.yml` defaults to the `local` profile which forces `spring.sql.init.mode: always`; schema.sql starts with `DROP TABLE` statements, so every app restart wipes data in any environment using the default config.
- **Repro**: Start app → seed data → restart → tables dropped before app returns.
- **Scope**: `src/main/resources/application.yml`, `application-local.yml`, `schema.sql`, build profile docs.
- **Proposed Fix**:
  1. Remove `spring.profiles.active: local` from the default `application.yml` and document how to opt-in via env vars.
  2. Gate destructive schema init (DROP statements) behind an explicit dev-only profile or move to Flyway/Liquibase migrations that never drop in prod/staging.
  3. Add regression check (manual ok) verifying data persists across restarts in default profile.
- **Definition of Done**:
  - Default startup no longer executes DROP statements.
  - Documentation explains how to run destructive schema init locally.
  - Smoke test proves user/event data persists after restart.

## S1 — Critical Path / Security

### UX-002 · Fix UserAccount ID generation for real databases
- **Severity**: S1 (Data)
- **Source**: `docs/ux/issues.md#L13`
- **Summary**: `UserAccount` uses `@GeneratedValue(strategy = GenerationType.IDENTITY)` on a `String` column while the schema expects UUIDs via `RANDOM_UUID()`. Postgres/MySQL cannot insert rows and login/signup flows fail.
- **Scope**: `src/main/java/.../user/UserAccount.java`, `schema.sql`, any seed data relying on IDs.
- **Proposed Fix**:
  1. Switch to `@GeneratedValue(strategy = GenerationType.UUID)` (Spring Boot 3) or assign UUIDs in `@PrePersist`.
  2. Align schema definition (remove IDENTITY semantics, ensure varchar(36) default).
  3. Add integration test covering `/auth/signup` hitting Postgres profile (or mocked) verifying insert success.
- **Definition of Done**: Sign-up/login works against Postgres; schema + entity definitions match.

### UX-003 · Remove `"/**"` wildcard from security allow list
- **Severity**: S1 (Security)
- **Source**: `docs/ux/issues.md#L41`
- **Summary**: `SecurityConfig.apiChain` permits everything due to `requestMatchers("/**")`, preventing any endpoint from being secured.
- **Scope**: `src/main/java/.../security/SecurityConfig.java` and any new auth-required routes.
- **Proposed Fix**:
  1. Limit the permitAll matcher list to explicit dev/static/documentation paths.
  2. Re-enable session or token-based auth for protected areas (dashboard, future APIs).
  3. Add regression test ensuring a sample secured endpoint returns 401 when not logged in.
- **Definition of Done**: Protected endpoints can require auth; manual/automated test confirms unauthorized requests fail.

## S2 — High Priority UX / Privacy

### UX-004 · Hide unpublished events from Saved lists
- **Severity**: S2 (Privacy/Content)
- **Source**: `docs/ux/issues.md#L19`
- **Summary**: `/saved` surfaces drafts/unpublished events because repository queries ignore `published=true`.
- **Scope**: `ProfileService.getSavedEvents`, `EventRepository`, saved template messaging.
- **Proposed Fix**: Add `findByLikedBy_UserIdAndPublishedTrue...` queries with upcoming/past filters; update service + controller; add empty-state messaging when saved item is withdrawn.
- **Definition of Done**: Saved list excludes drafts; integration test proves behavior.

### UX-005 · Guard `/dashboard/org` for org admins only
- **Severity**: S2 (Permissions)
- **Source**: `docs/ux/issues.md#L25`
- **Summary**: General users can open the dashboard shell because role checks happen only inside services when mutating.
- **Scope**: `OrgDashboardPageController`, `OrgDashboardService`, nav CTAs.
- **Proposed Fix**: Check actor roles before rendering; redirect/CTA to upgrade if not org admin; add controller test covering unauthorized access.
- **Definition of Done**: Non-org users cannot load dashboard; user-friendly upsell displayed instead.

## S3 — Polish / Resilience / Accessibility

### UX-006 · Restore venue fallback on org pages
- **Severity**: S3 (Content)
- **Source**: `docs/ux/issues.md#L31`
- **Summary**: `org_public.html` ignores organizer profile fields when event overrides are blank, unlike other lists.
- **Scope**: `org_public.html`, `OrgPublicPageController` model attributes.
- **Proposed Fix**: Reuse owner profile fallback logic from other templates or derive from `profile` object.
- **Definition of Done**: Org event cards always show location info even without per-event overrides.

### UX-007 · Label city filter controls for assistive tech
- **Severity**: S3 (Accessibility)
- **Source**: `docs/ux/issues.md#L35`
- **Summary**: Search input and selects on `/home/cities/{city}` lack `<label>` / `aria-label`, so screen readers announce “edit text blank”.
- **Scope**: `events_city.html` filter form.
- **Proposed Fix**: Add `<label>` wrappers or `aria-label` attributes, ensure selects describe purpose, and verify via VoiceOver/NVDA.
- **Definition of Done**: Accessibility audit confirms controls have readable names.
- **Status**: ✅ Completed (2025-11-17) — Added `.sr-only` labels and helper utility in `app.css` + `events_city.html`.

### UX-008 · Bundle HTMX locally (remove CDN dependency)
- **Severity**: S3 (Resilience)
- **Source**: `docs/ux/issues.md#L47`
- **Summary**: HTMX is loaded solely from `https://unpkg.com/...`; offline/corporate users lose all HTMX-driven flows.
- **Scope**: `base.html`, static asset pipeline, build docs.
- **Proposed Fix**: Vendor HTMX into `src/main/resources/static/js`, update template to load local copy with optional SRI-backed CDN fallback, document version bumps.
- **Definition of Done**: App works offline (HTMX available locally); documentation outlines upgrade procedure.
- **Status**: ✅ Completed (2025-11-17) — Bundled `htmx.min.js` under `/js`, referenced locally, and added automatic CDN fallback logic in `base.html`.
