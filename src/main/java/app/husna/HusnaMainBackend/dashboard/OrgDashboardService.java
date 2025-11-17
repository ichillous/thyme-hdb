package app.husna.HusnaMainBackend.dashboard;

import app.husna.HusnaMainBackend.constants.OrgType;
import app.husna.HusnaMainBackend.constants.StateProvince;
import app.husna.HusnaMainBackend.event.Event;
import app.husna.HusnaMainBackend.event.EventRepository;
import app.husna.HusnaMainBackend.profile.Profile;
import app.husna.HusnaMainBackend.profile.ProfileService;
import app.husna.HusnaMainBackend.user.UserAccount;
import app.husna.HusnaMainBackend.user.UserService;
import org.springframework.data.domain.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrgDashboardService {

    private final UserService users;
    private final ProfileService profiles;
    private final EventRepository events;

    public OrgDashboardService(UserService users, ProfileService profiles, EventRepository events) {
        this.users = users;
        this.profiles = profiles;
        this.events = events;
    }

    public Summary summary(String actorUserId) {
        UserAccount actor = users.getById(actorUserId);
        Profile profile = profiles.getMine(actorUserId);

        List<Event> all = events.findAllByOwnerOrgId(actor.getUserId());
        Instant now = Instant.now();

        long total = all.size();
        long published = all.stream().filter(Event::isPublished).count();
        long drafts = total - published;
        long upcoming = all.stream().filter(e -> e.isPublished() && e.getStartAt().isAfter(now)).count();
        long past = all.stream().filter(e -> e.isPublished() && !e.getStartAt().isAfter(now)).count();

        Map<String, Long> likeCounts = events.countLikesByOrgEvents(actor.getUserId())
                .stream().collect(Collectors.toMap(EventRepository.EventLikeCount::getEventId,
                        EventRepository.EventLikeCount::getLikeCount));
        long totalLikes = all.stream().mapToLong(e -> likeCounts.getOrDefault(e.getEventId(), 0L)).sum();

        boolean hasAddress = nonBlank(profile.getCity()) || nonBlank(profile.getAddressLine1());
        boolean hasDonation = profile.isDonationEnabled() && nonBlank(profile.getDonationUrl());
        boolean hasBranding = nonBlank(profile.getLogoUrl()) || nonBlank(profile.getBannerUrl());

        boolean isMosque = profile.getOrgType() == OrgType.MOSQUE;

        return new Summary(
                total, published, drafts, upcoming, past, totalLikes,
                profile.getCity(), profile.getStateProvince(),
                hasAddress, hasDonation, hasBranding,
                profile.getPrayerTimesLastUpdated(),
                isMosque
        );
    }

    public Page<EventRow> listEvents(String actorUserId, String status, Boolean publishedFilter, Pageable pageable) {
        UserAccount actor = users.getById(actorUserId);
        Instant now = Instant.now();

        Specification<Event> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("ownerOrgId"), actor.getUserId()));

            if (status != null) {
                String normalized = status.toLowerCase(Locale.ROOT);
                switch (normalized) {
                    case "upcoming" -> predicates.add(cb.greaterThan(root.get("startAt"), now));
                    case "past" -> predicates.add(cb.lessThanOrEqualTo(root.get("startAt"), now));
                    default -> {
                    }
                }
            }

            if (publishedFilter != null) {
                predicates.add(cb.equal(root.get("published"), publishedFilter));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };

        Pageable sortedPageable = ensureSort(pageable, status);
        Page<Event> page = events.findAll(spec, sortedPageable);

        Map<String, Long> likeCounts = events.countLikesByOrgEvents(actor.getUserId())
                .stream().collect(Collectors.toMap(EventRepository.EventLikeCount::getEventId,
                        EventRepository.EventLikeCount::getLikeCount));

        List<EventRow> rows = page.getContent().stream()
                .map(e -> new EventRow(e, likeCounts.getOrDefault(e.getEventId(), 0L)))
                .toList();

        return new PageImpl<>(rows, sortedPageable, page.getTotalElements());
    }

    private Pageable ensureSort(Pageable pageable, String status) {
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        boolean past = status != null && status.equalsIgnoreCase("past");
        Sort sort = past ? Sort.by(Sort.Order.desc("startAt")) : Sort.by(Sort.Order.asc("startAt"));
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    private boolean nonBlank(String s) { return s != null && !s.isBlank(); }

    // DTOs
    public record Summary(
            long totalEvents,
            long publishedEvents,
            long draftEvents,
            long upcomingEvents,
            long pastEvents,
            long totalLikes,
            String city,
            StateProvince stateProvince,
            boolean hasAddress,
            boolean hasDonationSetup,
            boolean hasBranding,
            Instant prayerTimesLastUpdated,
            boolean mosque
    ) {}

    public record EventRow(Event event, long likes) {}
}
