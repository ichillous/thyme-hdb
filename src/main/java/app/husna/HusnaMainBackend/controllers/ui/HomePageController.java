package app.husna.HusnaMainBackend.controllers.ui;

import app.husna.HusnaMainBackend.auth.ActorContext;
import app.husna.HusnaMainBackend.constants.StateProvince;
import app.husna.HusnaMainBackend.event.Event;
import app.husna.HusnaMainBackend.event.EventRepository;
import app.husna.HusnaMainBackend.home.CityEventsService;
import app.husna.HusnaMainBackend.home.HomeService;
import app.husna.HusnaMainBackend.home.HomeService.CitySummary;
import app.husna.HusnaMainBackend.home.CityEventsService.DateFilter;
import app.husna.HusnaMainBackend.home.CityEventsService.SortKey;
import lombok.AllArgsConstructor;
import app.husna.HusnaMainBackend.profile.Profile;
import app.husna.HusnaMainBackend.profile.ProfileService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@AllArgsConstructor
public class HomePageController {

    private final HomeService home;
    private final CityEventsService citySvc;
    private final EventRepository eventRepository;
    private final ProfileService profiles;
    private final ObjectProvider<ActorContext> actorContext;

    @GetMapping({"/", "/home"})
    public String home(Model model) {
        List<CitySummary> cities = home.listActiveCities();
        model.addAttribute("cities", cities);
        return "home";
    }

    // (Optional) HTMX fragment kept for legacy inline list use; safe because different path
    @GetMapping("/home/cities/{city}/events")
    public String cityEvents(@PathVariable String city,
                             @RequestParam(required = false) String state,
                             @RequestParam(defaultValue = "true") boolean upcomingOnly,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "20") int size,
                             Model model) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), size);
        StateProvince stateProvince = parseState(state);
        Page<Event> events = home.listCityEvents(city, stateProvince, upcomingOnly, pageable);
        model.addAttribute("city", city);
        model.addAttribute("state", state);
        model.addAttribute("upcomingOnly", upcomingOnly);
        model.addAttribute("page", events);
        return "fragments/event_list :: list";
    }

    // ✅ Single authoritative route for the City Events page (search/sort/filter/paging=20)
    @GetMapping("/home/cities/{city}")
    public String cityPage(@PathVariable String city,
                           @RequestParam(required = false) String state,
                           @RequestParam(defaultValue = "ALL") DateFilter date,
                           @RequestParam(defaultValue = "NEWEST") SortKey sort,
                           @RequestParam(required = false) String q,
                           @RequestParam(defaultValue = "0") int page,
                           Model model) {
        StateProvince stateProvince = parseState(state);
        Page<Event> events = citySvc.searchCityEvents(city, stateProvince, q, date, sort, page);
        List<Event> content = events.getContent();
        List<String> eventIds = content.stream().map(Event::getEventId).toList();
        Map<String, Long> likeCounts = eventIds.isEmpty()
                ? Map.of()
                : this.eventRepository.countLikesByEventIds(eventIds).stream()
                .collect(Collectors.toMap(EventRepository.EventLikeCount::getEventId, EventRepository.EventLikeCount::getLikeCount));

        ActorContext ctx = actorContext.getIfAvailable();
        String actorUserId = ctx != null ? ctx.actorUserId().orElse(null) : null;
        Set<String> likedIds = (eventIds.isEmpty() || actorUserId == null)
                ? Set.of()
                : Set.copyOf(this.eventRepository.findLikedEventIds(actorUserId, eventIds));

        // Preload organizer profiles for location fallback (org address when event has no override)
        Map<String, Profile> ownerProfiles = content.stream()
                .map(Event::getOwnerOrgId)
                .distinct()
                .map(id -> profiles.findByUserId(id).orElse(null))
                .filter(p -> p != null)
                .collect(Collectors.toMap(Profile::getUserId, p -> p));

        model.addAttribute("city", city);
        model.addAttribute("state", state);
        model.addAttribute("q", q);
        model.addAttribute("date", date);
        model.addAttribute("sort", sort);
        model.addAttribute("page", events);
        model.addAttribute("likeCounts", likeCounts);
        model.addAttribute("likedEventIds", likedIds);
        model.addAttribute("ownerProfiles", ownerProfiles);
        return "events_city";
    }

    private StateProvince parseState(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return StateProvince.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
