package app.husna.HusnaMainBackend.controllers.ui;

import app.husna.HusnaMainBackend.auth.ActorContext;
import app.husna.HusnaMainBackend.event.Event;
import app.husna.HusnaMainBackend.event.EventLikeService;
import app.husna.HusnaMainBackend.event.EventRepository;
import app.husna.HusnaMainBackend.profile.Profile;
import app.husna.HusnaMainBackend.profile.ProfileService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Controller
public class EventUiController {

    private final EventRepository events;
    private final EventLikeService likes;
    private final ProfileService profiles;
    private final ObjectProvider<ActorContext> actorContext;

    public EventUiController(EventRepository events, EventLikeService likes, ProfileService profiles, ObjectProvider<ActorContext> actorContext) {
        this.events = events;
        this.likes = likes;
        this.profiles = profiles;
        this.actorContext = actorContext;
    }

    @GetMapping("/events/{eventId}")
    public String detail(@PathVariable String eventId,
                         Model model) {
        Event e = events.findById(eventId).orElseThrow(() -> new IllegalArgumentException("event_not_found"));
        ActorContext ctx = actorContext.getIfAvailable();
        String actorUserId = ctx != null ? ctx.actorUserId().orElse(null) : null;
        boolean isOwner = actorUserId != null && actorUserId.equals(e.getOwnerOrgId());
        if (!e.isPublished() && !isOwner) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "event_not_found");
        }
        model.addAttribute("e", e);
        model.addAttribute("likeCount", events.countLikes(eventId));
        model.addAttribute("actorUserId", actorUserId);
        boolean likedByActor = actorUserId != null
                && !events.findLikedEventIds(actorUserId, List.of(eventId)).isEmpty();
        model.addAttribute("likedByActor", likedByActor);
        Profile orgProfile = e.getOwnerOrgId() != null ? profiles.findByUserId(e.getOwnerOrgId()).orElse(null) : null;
        model.addAttribute("orgProfile", orgProfile);
        return "event_detail";
    }

    // HTMX target: returns just the heart button snippet
    @PostMapping("/ui/events/{eventId}/heart")
    public String toggleHeart(@PathVariable String eventId,
                              Model model) {
        String actorUserId = actorContext.getObject().requireActorUserId();
        var state = likes.toggle(actorUserId, eventId);
        model.addAttribute("eventId", state.eventId());
        model.addAttribute("liked", state.liked());
        model.addAttribute("count", state.count());
        model.addAttribute("actorUserId", actorUserId);
        return "fragments/event_heart :: button";
    }
}
