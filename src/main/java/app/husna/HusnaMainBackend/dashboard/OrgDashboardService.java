package app.husna.HusnaMainBackend.dashboard;

import app.husna.HusnaMainBackend.event.Event;
import app.husna.HusnaMainBackend.event.EventRepository;
import app.husna.HusnaMainBackend.profile.Profile;
import app.husna.HusnaMainBackend.profile.ProfileService;
import app.husna.HusnaMainBackend.user.UserAccount;
import app.husna.HusnaMainBackend.user.UserService;
import org.springframework.data.domain.*;
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

        boolean hasAddress = nonBlank(profile.getCity()) || nonBlank(profile.getStreet());
        boolean hasDonation = profile.isDonationEnabled() && nonBlank(profile.getDonationUrl());
        boolean hasBranding = nonBlank(profile.getLogoUrl()) || nonBlank(profile.getBannerUrl());

        return new Summary(
                total, published, drafts, upcoming, past, totalLikes,
                profile.getCity(), profile.getRegion(),
                hasAddress, hasDonation, hasBranding,
                profile.getPrayerTimesLastUpdated()
        );
    }

    public Page<EventRow> listEvents(String actorUserId, String status, Boolean publishedFilter, Pageable pageable) {
        UserAccount actor = users.getById(actorUserId);

        Page<Event> page = events.findByOwnerOrgId(actor.getUserId(), pageable);

        Instant now = Instant.now();
        List<Event> filtered = page.getContent().stream().filter(e -> {
            boolean statusOk = switch (status == null ? "all" : status.toLowerCase()) {
                case "upcoming" -> e.getStartAt().isAfter(now);
                case "past" -> !e.getStartAt().isAfter(now);
                default -> true;
            };
            boolean pubOk = (publishedFilter == null) || (e.isPublished() == publishedFilter);
            return statusOk && pubOk;
        }).sorted((a, b) -> {
            if (status != null && status.equalsIgnoreCase("past")) {
                return b.getStartAt().compareTo(a.getStartAt());
            }
            return a.getStartAt().compareTo(b.getStartAt());
        }).toList();

        Map<String, Long> likeCounts = events.countLikesByOrgEvents(actor.getUserId())
                .stream().collect(Collectors.toMap(EventRepository.EventLikeCount::getEventId,
                        EventRepository.EventLikeCount::getLikeCount));

        List<EventRow> rows = filtered.stream()
                .map(e -> new EventRow(e, likeCounts.getOrDefault(e.getEventId(), 0L)))
                .toList();

        // Keep original paging metadata but return filtered content
        return new PageImpl<>(rows, pageable, page.getTotalElements());
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
            String region,
            boolean hasAddress,
            boolean hasDonationSetup,
            boolean hasBranding,
            Instant prayerTimesLastUpdated
    ) {}

    public record EventRow(Event event, long likes) {}
}
