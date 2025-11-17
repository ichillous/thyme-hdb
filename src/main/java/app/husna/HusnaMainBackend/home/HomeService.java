package app.husna.HusnaMainBackend.home;

import app.husna.HusnaMainBackend.constants.StateProvince;
import app.husna.HusnaMainBackend.event.Event;
import app.husna.HusnaMainBackend.event.EventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class HomeService {

    private final EventRepository events;

    public HomeService(EventRepository events) {
        this.events = events;
    }

    // Cities that have at least one published event (button list for home)
    public List<CitySummary> listActiveCities() {
        Instant now = Instant.now();
        return events.listActiveCities(now).stream()
                .map(c -> new CitySummary(c.getCity(), c.getStateProvince(), c.getCount()))
                .toList();
    }

    // All published events in city (optionally upcoming-only)
    public Page<Event> listCityEvents(String city, StateProvince state, boolean upcomingOnly, Pageable pageable) {
        Instant now = Instant.now();
        if (state != null) {
            return upcomingOnly
                    ? events.findByPublishedTrueAndCityIgnoreCaseAndStateProvinceAndStartAtAfter(city, state, now, pageable)
                    : events.findByPublishedTrueAndCityIgnoreCaseAndStateProvince(city, state, pageable);
        } else {
            return upcomingOnly
                    ? events.findByPublishedTrueAndCityIgnoreCaseAndStartAtAfter(city, now, pageable)
                    : events.findByPublishedTrueAndCityIgnoreCase(city, pageable);
        }
    }

    // DTO
    public record CitySummary(String city, StateProvince stateProvince, long eventCount) {}
}
