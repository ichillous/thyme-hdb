package app.husna.HusnaMainBackend.controllers;

import app.husna.HusnaMainBackend.constants.StateProvince;
import app.husna.HusnaMainBackend.event.Event;
import app.husna.HusnaMainBackend.home.HomeService;
import app.husna.HusnaMainBackend.home.HomeService.CitySummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/test/home")
public class HomeTestController {

    private final HomeService home;

    public HomeTestController(HomeService home) {
        this.home = home;
    }

    // 1) Buttons for cities on home page
    @GetMapping("/cities")
    public List<CitySummary> cities() {
        return home.listActiveCities();
    }

    // 2) Events page for a city (optionally filter by state and upcoming-only)
    @GetMapping("/cities/{city}/events")
    public Page<Event> cityEvents(@PathVariable String city,
                                  @RequestParam(required = false) String state,
                                  @RequestParam(defaultValue = "true") boolean upcomingOnly,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return home.listCityEvents(city, parseState(state), upcomingOnly, pageable);
    }

    // controllers/ui/HomePageController.java
    @GetMapping("/home/cities/{city}")
    public String cityPage(@PathVariable String city,
                           @RequestParam(required = false) String state,
                           @RequestParam(defaultValue = "true") boolean upcomingOnly,
                           @RequestParam(defaultValue = "0") int page,
                           Model model) {
        // always 20 per page
        Pageable pageable = PageRequest.of(Math.max(page, 0), 20);
        Page<Event> events = home.listCityEvents(city, parseState(state), upcomingOnly, pageable);
        model.addAttribute("city", city);
        model.addAttribute("state", state);
        model.addAttribute("upcomingOnly", upcomingOnly);
        model.addAttribute("page", events);
        return "events_city";
    }

    private StateProvince parseState(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return StateProvince.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

}
