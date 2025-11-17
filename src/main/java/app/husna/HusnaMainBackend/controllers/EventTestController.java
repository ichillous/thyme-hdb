package app.husna.HusnaMainBackend.controllers;

import app.husna.HusnaMainBackend.constants.StateProvince;
import app.husna.HusnaMainBackend.constants.StateProvince;
import app.husna.HusnaMainBackend.event.Event;
import app.husna.HusnaMainBackend.event.EventService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/test/events")
public class EventTestController {

    private final EventService events;

    public EventTestController(EventService events) {
        this.events = events;
    }

    // ---- ORG CRUD (actorUserId simulates auth for MVP) ----

    @PostMapping
    public ResponseEntity<Event> create(
            @RequestParam String actorUserId,
            @RequestBody CreateEventRequest req) {
        Event created = events.createEvent(actorUserId, req.toServiceDto());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{eventId}")
    public Event get(@PathVariable String eventId) {
        return events.getEvent(eventId);
    }

    @PatchMapping("/{eventId}")
    public Event update(
            @RequestParam String actorUserId,
            @PathVariable String eventId,
            @RequestBody UpdateEventRequest req) {
        return events.updateEvent(actorUserId, eventId, req.toServiceDto());
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> delete(
            @RequestParam String actorUserId,
            @PathVariable String eventId) {
        events.deleteEvent(actorUserId, eventId);
        return ResponseEntity.noContent().build();
    }

    // ---- Listings ----

    @GetMapping
    public Page<Event> listPublic(Pageable pageable) {
        return events.listPublic(pageable);
    }

    @GetMapping("/upcoming")
    public Page<Event> listUpcoming(@RequestParam(required = false) Instant after, Pageable pageable) {
        Instant pivot = after != null ? after : Instant.now();
        return events.listUpcomingPublic(pivot, pageable);
    }

    @GetMapping("/owned")
    public Page<Event> listOwned(@RequestParam String actorUserId, Pageable pageable) {
        return events.listOwnedByOrg(actorUserId, pageable);
    }

    // ---- Hearts / Saved ----

    @PostMapping("/{eventId}/heart")
    public Event heart(@RequestParam String userId, @PathVariable String eventId) {
        return events.heart(userId, eventId);
    }

    @DeleteMapping("/{eventId}/heart")
    public Event unheart(@RequestParam String userId, @PathVariable String eventId) {
        return events.unheart(userId, eventId);
    }

    @GetMapping("/saved/by-user/{userId}")
    public Page<Event> saved(@PathVariable String userId, Pageable pageable) {
        return events.savedByUser(userId, pageable);
    }

    // ---- Errors ----

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badReq(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> conflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<Map<String, String>> forbidden(UnsupportedOperationException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
    }

    // ---- Request DTOs (controller-level) ----

    public record CreateEventRequest(
            String title,
            String description,
            String venueName,
            String addressLine,
            String city,
            StateProvince stateProvince,
            String postalCode,
            String timezone,
            Instant startAt,
            Instant endAt,
            Boolean published
    ) {
        public EventService.CreateEvent toServiceDto() {
            return new EventService.CreateEvent(
                    title, description, venueName, addressLine, city, stateProvince, postalCode, timezone, startAt, endAt, published
            );
        }
    }

    public record UpdateEventRequest(
            String title,
            String description,
            String venueName,
            String addressLine,
            String city,
            StateProvince stateProvince,
            String postalCode,
            String timezone,
            Instant startAt,
            Instant endAt,
            Boolean published
    ) {
        public EventService.UpdateEvent toServiceDto() {
            return new EventService.UpdateEvent(
                    title, description, venueName, addressLine, city, stateProvince, postalCode, timezone, startAt, endAt, published
            );
        }
    }
}
