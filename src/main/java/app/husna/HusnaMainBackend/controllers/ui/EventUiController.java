// app/husna/HusnaMainBackend/controllers/ui/EventUiController.java
package app.husna.HusnaMainBackend.controllers.ui;

import app.husna.HusnaMainBackend.event.Event;
import app.husna.HusnaMainBackend.event.EventLikeService;
import app.husna.HusnaMainBackend.event.EventRepository;
import app.husna.HusnaMainBackend.profile.Profile;
import app.husna.HusnaMainBackend.profile.ProfileService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class EventUiController {

    private final EventRepository events;
    private final EventLikeService likes;
    private final ProfileService profiles;

    public EventUiController(EventRepository events, EventLikeService likes, ProfileService profiles) {
        this.events = events; this.likes = likes; this.profiles = profiles;
    }

    @GetMapping("/events/{eventId}")
    public String detail(@PathVariable String eventId,
                         @RequestParam(required = false) String actorUserId,
                         Model model) {
        Event e = events.findById(eventId).orElseThrow(() -> new IllegalArgumentException("event_not_found"));
        model.addAttribute("e", e);
        model.addAttribute("likeCount", events.countLikes(eventId));
        model.addAttribute("actorUserId", actorUserId);
        Profile orgProfile = e.getOwnerOrgId() != null ? profiles.findByUserId(e.getOwnerOrgId()).orElse(null) : null;
        model.addAttribute("orgProfile", orgProfile);
        return "event_detail";
    }

    // HTMX target: returns just the heart button snippet
    @PostMapping("/ui/events/{eventId}/heart")
    public String toggleHeart(@PathVariable String eventId,
                              @RequestParam String actorUserId,
                              Model model) {
        var state = likes.toggle(actorUserId, eventId);
        model.addAttribute("eventId", state.eventId());
        model.addAttribute("liked", state.liked());
        model.addAttribute("count", state.count());
        model.addAttribute("actorUserId", actorUserId);
        return "fragments/event_heart :: button";
    }
}
