# Husna UX & Product Issue Log

Severity scale: S0 = blocker/data loss, S1 = critical path broken, S2 = major UX/security gap, S3 = polish/accessibility debt.

## Ranked Findings

### 1. Default profile nukes data on every boot (S0 · Environment)
- **Repro**: Start the app with the default configuration, create sample users/events, stop the app, then start it again. All tables are dropped before the app comes back up.
- **Expected vs actual**: Non-destructive schema migrations outside of explicit local/dev runs vs. every environment inheriting `application-local.yml` which forces `spring.sql.init.mode: always` and executes the `DROP TABLE` statements in `schema.sql` at startup.
- **Root cause**: `spring.profiles.active: local` (`src/main/resources/application.yml:1-3`) + `spring.sql.init.mode: always` in the local profile (`src/main/resources/application-local.yml:22-24`) combined with `DROP TABLE IF EXISTS …` at the top of `schema.sql` (`src/main/resources/schema.sql:1-4`).
- **Fix**: Remove the hard-coded active profile, gate destructive schema init behind an explicit `dev` profile, and move to migration tooling (Flyway/Liquibase) so prod/staging never run the drop statements.

### 2. User sign-up fails on relational databases (S1 · Data)
- **Repro**: Point Spring Boot at Postgres, call `/auth/signup`, and observe a persistence exception because the ID column cannot be generated.
- **Expected vs actual**: UUID primary keys for `USER_ACCOUNT` rows vs. a `@GeneratedValue(strategy = GenerationType.IDENTITY)` on a `String` column with no identity definition.
- **Root cause**: `UserAccount` declares a `String` `@Id` with `GenerationType.IDENTITY` (`src/main/java/app/husna/HusnaMainBackend/user/UserAccount.java:30-33`) while `schema.sql` relies on `DEFAULT RANDOM_UUID()` (not an identity). Hibernate cannot fetch generated keys and Postgres rejects IDENTITY on varchar.
- **Fix**: Switch to `@GeneratedValue(strategy = GenerationType.UUID)` or assign the UUID in `@PrePersist`, and drop the identity strategy so it matches the schema default.

### 3. Saved events leak unpublished content (S2 · Privacy/Content)
- **Repro**: Heart an event, have the organizer unpublish it, then open `/saved`. The DTO still includes the full title, description, address, and schedule even though the organizer intended to pull it from the public surface.
- **Expected vs actual**: Saved lists should only surface published events or clearly mark drafts as unavailable vs. the current repository calls returning any liked event regardless of publication state.
- **Root cause**: `ProfileService.getSavedEvents` delegates to `EventRepository.findByLikedBy_UserId…` without `published=true` filters (`src/main/java/app/husna/HusnaMainBackend/profile/ProfileService.java:99-105`).
- **Fix**: Add `findByLikedBy_UserIdAndPublishedTrue…` queries (upcoming/past variants) and fall back to 404 messaging when drafts slip through.

### 4. General users can open the org dashboard shell (S2 · Permissions)
- **Repro**: Sign up as a regular attendee, hit `/dashboard/org`, and you’ll see the dashboard, nudges, and event actions; the first mutation fails with an `org_only_operation` toast only after a failed form post.
- **Expected vs actual**: Immediate guard/redirect for non `ORG_ADMIN` roles vs. rendering the full dashboard before the service layer throws.
- **Root cause**: `OrgDashboardPageController.dashboard` only checks for login (`src/main/java/app/husna/HusnaMainBackend/controllers/ui/OrgDashboardPageController.java:27-47`) and `OrgDashboardService.summary` never asserts the actor’s role.
- **Fix**: Inject `ActorContext` -> fetch `UserAccount` -> verify `RoleName.ORG_ADMIN` before building the model; otherwise redirect to `/signup?org=true` or show an upsell state.

### 5. Org public listings lose location context when events rely on defaults (S3 · UX/Content)
- **Repro**: Create an event without overriding the venue (so it should inherit the org profile address) and visit `/org/{orgId}`. Each card shows blank venue/address fields even though the profile has the data.
- **Expected vs actual**: Event cards on org profiles should mirror the city/saved cards by falling back to `profile` when event overrides are empty vs. `org_public.html` only printing `e.venueName`/`e.addressLine` (`src/main/resources/templates/org_public.html:112-135`).
- **Fix**: Share the same `ownerProfiles` fallback logic used in `events_city.html`/`saved_events.html` or render from the already loaded `profile` object.

### 6. City filter form is invisible to assistive tech (S3 · Accessibility)
- **Repro**: Run VoiceOver/NVDA on `/home/cities/{city}` and focus the search/select controls. Screen readers announce “edit text blank” because there are no `<label>` or `aria-label` bindings.
- **Expected vs actual**: Every filter control needs an accessible name vs. plain `<input>`/`<select>` elements with placeholder text only (`src/main/resources/templates/events_city.html:12-23`).
- **Fix**: Wrap the inputs in `<label>` elements or add `aria-label` attributes, and ensure select options describe the filter scope.
- **Status**: ✅ Fixed (2025-11-17) by adding `.sr-only` labels in `events_city.html` and defining a reusable `.sr-only` helper in `app.css`.

### 7. HTTP security can never be tightened (S1 · Security)
- **Repro**: Try to secure a new controller by requiring authentication; Spring Security still serves it anonymously because the current filter chain whitelists `"/**"`.
- **Expected vs actual**: Ability to incrementally lock down endpoints vs. `SecurityConfig.apiChain` permitting every path (`src/main/java/app/husna/HusnaMainBackend/security/SecurityConfig.java:50-63`).
- **Root cause**: The `requestMatchers` include a global `"/**"` before `.anyRequest`, so the matcher short-circuits.
- **Fix**: Remove the `"/**"` matcher, restrict the dev-open policy to explicit static/docs paths, and re-enable session-based authentication for protected flows.

### 8. HTMX is fetched from an external CDN with no fallback (S3 · Resilience)
- **Repro**: Run the UI offline/behind a firewall—the `<script src="https://unpkg.com/htmx.org@1.9.12" defer>` never loads, so modals, skeletons, and heart toggles break silently.
- **Expected vs actual**: Frontend assets bundled locally per Husna SOP vs. CDN dependency defined in `base.html` (`src/main/resources/templates/base.html:8-13`) without Subresource Integrity or cache busting.
- **Fix**: Vendor the HTMX bundle into `src/main/resources/static/js`, reference it locally, and add SRI if a CDN copy remains as a fallback.
- **Status**: ✅ Fixed (2025-11-17) by bundling `/js/htmx.min.js` and adding a CDN fallback injector in `base.html`.
