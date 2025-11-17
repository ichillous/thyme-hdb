package app.husna.HusnaMainBackend.event;

import app.husna.HusnaMainBackend.constants.RoleName;
import app.husna.HusnaMainBackend.constants.StateProvince;
import app.husna.HusnaMainBackend.profile.ProfileService;
import app.husna.HusnaMainBackend.user.UserAccount;
import app.husna.HusnaMainBackend.user.UserService;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

@Service
public class EventService {

    private final EventRepository events;
    private final UserService users;
    private final ProfileService profiles;

    public EventService(EventRepository events, UserService users, ProfileService profiles) {
        this.events = events;
        this.users = users;
        this.profiles = profiles;
    }

    // ========= CRUD (ORG-ONLY) =========

    @Transactional
    public Event createEvent(String actorUserId, CreateEvent req) {
        UserAccount actor = users.getById(actorUserId);
        assertOrg(actor);
        profiles.requireOrgProfileComplete(actor.getUserId());

        validateTimes(req.startAt(), req.endAt());
        if (req.stateProvince() == null) {
            throw new IllegalArgumentException("state_required");
        }

        Event e = Event.builder()
                .ownerOrgId(actor.getUserId())
                .title(req.title())
                .description(nullToEmpty(req.description()))
                .venueName(req.venueName())
                .addressLine(req.addressLine())
                .city(req.city())
                .stateProvince(req.stateProvince())
                .postalCode(req.postalCode())
                .timezone(req.timezone())
                .startAt(req.startAt())
                .endAt(req.endAt())
                .published(Boolean.TRUE.equals(req.published()))
                .build();

        return events.save(e);
    }

    public Event getEvent(String eventId) {
        return events.findById(eventId).orElseThrow(() -> new IllegalArgumentException("event_not_found"));
    }

    /**
     * Fetch an event while ensuring the actor is an org admin and owns it. Useful for dashboard/edit forms.
     */
    public Event getOrgOwnedEvent(String actorUserId, String eventId) {
        UserAccount actor = users.getById(actorUserId);
        assertOrg(actor);
        Event e = getEvent(eventId);
        assertOwner(actor, e);
        return e;
    }

    @Transactional
    public Event updateEvent(String actorUserId, String eventId, UpdateEvent req) {
        UserAccount actor = users.getById(actorUserId);
        assertOrg(actor);

        Event e = getEvent(eventId);
        assertOwner(actor, e);

        // Patch semantics: only update when provided (non-null)
        if (req.title() != null) e.setTitle(req.title());
        if (req.description() != null) e.setDescription(req.description());
        if (req.venueName() != null) e.setVenueName(req.venueName());
        if (req.addressLine() != null) e.setAddressLine(req.addressLine());
        if (req.city() != null) e.setCity(req.city());
        if (req.stateProvince() != null) e.setStateProvince(req.stateProvince());
        if (req.postalCode() != null) e.setPostalCode(req.postalCode());
        if (req.timezone() != null) e.setTimezone(req.timezone());
        if (req.startAt() != null) e.setStartAt(req.startAt());
        if (req.endAt() != null) e.setEndAt(req.endAt());
        if (req.published() != null) e.setPublished(req.published());

        validateTimes(e.getStartAt(), e.getEndAt());

        return events.save(e);
    }

    @Transactional
    public void deleteEvent(String actorUserId, String eventId) {
        UserAccount actor = users.getById(actorUserId);
        assertOrg(actor);

        Event e = getEvent(eventId);
        assertOwner(actor, e);

        // Clean many-to-many to avoid orphan row surprises on some DBs
        e.getLikedBy().clear();
        events.delete(e);
    }

    // ========= LISTINGS =========

    public Page<Event> listPublic(Pageable pageable) {
        return events.findByPublishedTrue(pageable);
    }

    public Page<Event> listUpcomingPublic(Instant after, Pageable pageable) {
        return events.findByPublishedTrueAndStartAtAfter(after, pageable);
    }

    public Page<Event> listOwnedByOrg(String actorUserId, Pageable pageable) {
        UserAccount actor = users.getById(actorUserId);
        assertOrg(actor);
        return events.findByOwnerOrgId(actor.getUserId(), pageable);
    }

    // ========= HEARTS (ANY USER) =========

    @Transactional
    public Event heart(String userId, String eventId) {
        UserAccount user = users.getById(userId);
        Event e = getEvent(eventId);
        e.getLikedBy().add(user);
        return events.save(e);
    }

    @Transactional
    public Event unheart(String userId, String eventId) {
        UserAccount user = users.getById(userId);
        Event e = getEvent(eventId);
        e.getLikedBy().removeIf(u -> Objects.equals(u.getUserId(), user.getUserId()));
        return events.save(e);
    }

    public Page<Event> savedByUser(String userId, Pageable pageable) {
        return events.findByLikedBy_UserId(userId, pageable);
    }

    // ========= Helpers =========

    private void assertOrg(UserAccount actor) {
        Set<RoleName> roles = actor.getRoles() == null ? EnumSet.noneOf(RoleName.class) : actor.getRoles();
        if (roles.stream().noneMatch(r -> r == RoleName.ORG_ADMIN)) {
            throw new UnsupportedOperationException("org_only_operation");
        }
    }

    private void assertOwner(UserAccount actor, Event e) {
        if (!Objects.equals(e.getOwnerOrgId(), actor.getUserId())) {
            throw new UnsupportedOperationException("not_event_owner");
        }
    }

    private void validateTimes(Instant start, Instant end) {
        if (start == null || end == null) throw new IllegalArgumentException("start_end_required");
        if (end.isBefore(start)) throw new IllegalArgumentException("end_before_start");
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    // ====== DTOs (service-level) ======
    public record CreateEvent(
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
    ) {}

    public record UpdateEvent(
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
    ) {}
}
