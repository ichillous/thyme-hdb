package app.husna.HusnaMainBackend.controllers.ui;

import app.husna.HusnaMainBackend.auth.ActorContext;
import app.husna.HusnaMainBackend.event.Event;
import app.husna.HusnaMainBackend.event.EventRepository;
import app.husna.HusnaMainBackend.profile.Profile;
import app.husna.HusnaMainBackend.profile.ProfileService;
import org.springframework.beans.factory.ObjectProvider;
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
    private final ObjectProvider<ActorContext> actorContext;

    public SavedEventsPageController(ProfileService profiles, EventRepository events, ObjectProvider<ActorContext> actorContext) {
        this.profiles = profiles;
        this.events = events;
        this.actorContext = actorContext;
    }

    @GetMapping("/saved")
    public String saved(@RequestParam(defaultValue = "upcoming") String tab,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        ActorContext ctx = actorContext.getObject();
        var actorOpt = ctx.actorUserId();
        if (actorOpt.isEmpty()) {
            return "redirect:/login?next=/saved";
        }
        String actorUserId = actorOpt.get();
        String normalizedTab = tab.equalsIgnoreCase("past") ? "past" : "upcoming";
        boolean upcoming = normalizedTab.equals("upcoming");
        Sort sort = upcoming
                ? Sort.by(Sort.Direction.ASC, "startAt")
                : Sort.by(Sort.Direction.DESC, "startAt");
        Pageable pageable = PageRequest.of(Math.max(page, 0), 20, sort);
        Page<Event> events = profiles.getSavedEvents(actorUserId, upcoming, pageable);
        Profile profile = profiles.getMine(actorUserId);
        java.util.List<String> eventIds = events.getContent().stream().map(Event::getEventId).toList();
        java.util.Map<String, Long> likeCounts = eventIds.isEmpty()
                ? java.util.Collections.emptyMap()
                : this.events.countLikesByEventIds(eventIds).stream()
                .collect(java.util.stream.Collectors.toMap(EventRepository.EventLikeCount::getEventId,
                        EventRepository.EventLikeCount::getLikeCount));

        // Preload organizer profiles for location fallback in list rendering
        java.util.Map<String, Profile> ownerProfiles = events.getContent().stream()
                .map(Event::getOwnerOrgId)
                .distinct()
                .map(id -> profiles.findByUserId(id).orElse(null))
                .filter(p -> p != null)
                .collect(java.util.stream.Collectors.toMap(Profile::getUserId, p -> p));

        model.addAttribute("actorUserId", actorUserId);
        model.addAttribute("profile", profile);
        model.addAttribute("tab", normalizedTab);
        model.addAttribute("page", events);
        model.addAttribute("likeCounts", likeCounts);
        model.addAttribute("ownerProfiles", ownerProfiles);
        return "saved_events";
    }
}
