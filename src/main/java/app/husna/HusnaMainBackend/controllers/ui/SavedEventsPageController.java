package app.husna.HusnaMainBackend.controllers.ui;

import app.husna.HusnaMainBackend.event.Event;
import app.husna.HusnaMainBackend.event.EventRepository;
import app.husna.HusnaMainBackend.profile.Profile;
import app.husna.HusnaMainBackend.profile.ProfileService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class SavedEventsPageController {

    private final ProfileService profiles;
    private final EventRepository events;

    public SavedEventsPageController(ProfileService profiles, EventRepository events) {
        this.profiles = profiles;
        this.events = events;
    }

    @GetMapping("/saved")
    public String saved(@RequestParam String actorUserId,
                        @RequestParam(defaultValue = "upcoming") String tab,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        String normalizedTab = tab.equalsIgnoreCase("past") ? "past" : "upcoming";
        boolean upcoming = normalizedTab.equals("upcoming");
        Sort sort = upcoming
                ? Sort.by(Sort.Direction.ASC, "startAt")
                : Sort.by(Sort.Direction.DESC, "startAt");
        Pageable pageable = PageRequest.of(Math.max(page, 0), 20, sort);
        Page<Event> events = profiles.getSavedEvents(actorUserId, upcoming, pageable);
        Profile profile = profiles.getMine(actorUserId);

        model.addAttribute("actorUserId", actorUserId);
        model.addAttribute("profile", profile);
        model.addAttribute("tab", normalizedTab);
        model.addAttribute("page", events);
        model.addAttribute("likeCounts", events.getContent().stream()
                .collect(java.util.stream.Collectors.toMap(Event::getEventId, e -> this.events.countLikes(e.getEventId()))));
        return "saved_events";
    }
}
