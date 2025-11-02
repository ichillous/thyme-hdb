# Husna — Community Events & Org Directory (MVP)

Husna is a minimal Spring Boot + Thymeleaf + HTMX app that helps people discover city-specific events from local organizations (mosques, non-profits, community centers). The MVP focuses on a clean **Cities → Events** discovery flow, lightweight “likes,” and transparent auth modals ready to wire to a real identity provider.

---

## Table of Contents

- [Features](#features)
- [Screens & Flows](#screens--flows)
- [Tech Stack](#tech-stack)
- [Run Locally](#run-locally)
- [UI Routes](#ui-routes)
- [Templates](#templates)
- [Event Query (Search / Sort / Filter / Paging)](#event-query-search--sort--filter--paging)
- [Likes (Hearts)](#likes-hearts)
- [Auth (UI Modals)](#auth-ui-modals)
- [Data Model (Concepts)](#data-model-concepts)
- [Roadmap](#roadmap)
- [Troubleshooting](#troubleshooting)
- [License](#license)

---

## Features

- **Home / Cities**: Shows active cities (derived from published events).
- **City Events**: Search, sort, filter, and paginate events for a selected city.
- **Event Details**: Full event page with time/venue details.
- **Likes/Hearts**: Toggle “saved” state with counts (HTMX partial swap).
- **Auth UX stubs**: Modal login/sign-up overlays (UI only for now).
- **Developer-friendly**: Server-rendered HTML, no SPA build step.

---

## Screens & Flows

1. **Discover**
   - Home → click a **City** chip → land on **City Events**.
   - Narrow via **search** (title/venue), **date filter** (This week/This month/All), **sort** (Newest/Soonest).
   - **Pagination**: 20 per page. Prev/Next only show when applicable.

2. **Inspect**
   - **View details** from the City Events list to see the full event page.

3. **Save**
   - **Heart/Unheart** on list or detail. Count updates in place via HTMX.

4. **Auth (UI only)**
   - Click **Log in** / **Sign up** in the header to open a **transparent floating modal**. Replace stubs with real auth later.

> Design note: The **home page does not show event cards**; the flow is _Home → City → Events_ for clarity.

---

## Tech Stack

- **Backend**: Spring Boot 3.x, Spring MVC, Spring Data JPA, H2 (in-memory), permissive Security for local dev.
- **Frontend**: Thymeleaf 3.1, HTMX.
- **Build**: Maven.
- **Styles**: `static/css/app.css` (dark theme by default via `<body class="app dark">`).

---

## Run Locally

```bash
./mvnw spring-boot:run
# App: http://localhost:8080
# H2 console (dev): http://localhost:8080/h2-console
# Swagger UI (if enabled): http://localhost:8080/swagger-ui.html
