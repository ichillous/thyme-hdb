// app/husna/HusnaMainBackend/home/CityEventsService.java
package app.husna.HusnaMainBackend.home;

import app.husna.HusnaMainBackend.event.Event;
import app.husna.HusnaMainBackend.event.EventRepository;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.time.*;
import java.util.*;

@Service
public class CityEventsService {

    private final EventRepository events;

    public CityEventsService(EventRepository events) { this.events = events; }

    public Page<Event> searchCityEvents(String city,
                                        String region,
                                        String q,
                                        DateFilter dateFilter,
                                        SortKey sortKey,
                                        int page) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), 20, sort(sortKey));
        Specification<Event> spec = spec(city, region, q, dateFilter);
        return events.findAll(spec, pageable);
    }

    private Sort sort(SortKey key) {
        return (key == SortKey.SOONEST)
                ? Sort.by(Sort.Order.asc("startAt"))
                : Sort.by(Sort.Order.desc("startAt")); // NEWEST default
    }

    private Specification<Event> spec(String city, String region, String q, DateFilter df) {
        final String cityLc = city == null ? "" : city.toLowerCase(Locale.ROOT);
        final String regionLc = region == null ? null : region.toLowerCase(Locale.ROOT);
        final String like = q == null || q.isBlank() ? null : "%" + q.toLowerCase(Locale.ROOT) + "%";

        // date window
        Instant now = Instant.now();
        Instant end = switch (df == null ? DateFilter.ALL : df) {
            case WEEK -> now.plus(Duration.ofDays(7));
            case MONTH -> now.plus(Duration.ofDays(31));
            case ALL -> null;
        };

        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.isTrue(root.get("published")));
            p.add(cb.equal(cb.lower(root.get("city")), cityLc));
            if (regionLc != null && !regionLc.isBlank()) {
                p.add(cb.equal(cb.lower(root.get("region")), regionLc));
            }
            // upcoming-only behavior if using a date window; ALL = no lower bound
            if (end != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("startAt"), now));
                p.add(cb.lessThan(root.get("startAt"), end));
            }
            if (like != null) {
                p.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("venueName")), like)
                ));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    public enum SortKey { NEWEST, SOONEST }
    public enum DateFilter { WEEK, MONTH, ALL }
}
