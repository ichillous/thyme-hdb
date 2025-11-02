package app.husna.HusnaMainBackend.controllers.ui;

import app.husna.HusnaMainBackend.event.Event;
import app.husna.HusnaMainBackend.home.CityEventsService;
import app.husna.HusnaMainBackend.home.HomeService;
import app.husna.HusnaMainBackend.home.HomeService.CitySummary;
import app.husna.HusnaMainBackend.home.CityEventsService.DateFilter;
import app.husna.HusnaMainBackend.home.CityEventsService.SortKey;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@AllArgsConstructor
public class HomePageController {

    private final HomeService home;
    private final CityEventsService citySvc;

    @GetMapping({"/", "/home"})
    public String home(Model model) {
        List<CitySummary> cities = home.listActiveCities();
        model.addAttribute("cities", cities);
        return "home";
    }

    // (Optional) HTMX fragment kept for legacy inline list use; safe because different path
    @GetMapping("/home/cities/{city}/events")
    public String cityEvents(@PathVariable String city,
                             @RequestParam(required = false) String region,
                             @RequestParam(defaultValue = "true") boolean upcomingOnly,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "20") int size,
                             Model model) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), size);
        Page<Event> events = home.listCityEvents(city, region, upcomingOnly, pageable);
        model.addAttribute("city", city);
        model.addAttribute("region", region);
        model.addAttribute("upcomingOnly", upcomingOnly);
        model.addAttribute("page", events);
        return "fragments/event_list :: list";
    }

    // ✅ Single authoritative route for the City Events page (search/sort/filter/paging=20)
    @GetMapping("/home/cities/{city}")
    public String cityPage(@PathVariable String city,
                           @RequestParam(required = false) String region,
                           @RequestParam(defaultValue = "ALL") DateFilter date,
                           @RequestParam(defaultValue = "NEWEST") SortKey sort,
                           @RequestParam(required = false) String q,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(required = false) String actorUserId,
                           Model model) {
        Page<Event> events = citySvc.searchCityEvents(city, region, q, date, sort, page);
        model.addAttribute("city", city);
        model.addAttribute("region", region);
        model.addAttribute("q", q);
        model.addAttribute("date", date);
        model.addAttribute("sort", sort);
        model.addAttribute("page", events);
        model.addAttribute("actorUserId", actorUserId);
        return "events_city";
    }
}
