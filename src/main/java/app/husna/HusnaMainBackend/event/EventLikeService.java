// app/husna/HusnaMainBackend/event/EventLikeService.java
package app.husna.HusnaMainBackend.event;

import app.husna.HusnaMainBackend.user.UserAccount;
import app.husna.HusnaMainBackend.user.UserService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class EventLikeService {

    private final EventRepository events;
    private final UserService users;

    public EventLikeService(EventRepository events, UserService users) {
        this.events = events; this.users = users;
    }

    @Transactional
    public LikeState toggle(String actorUserId, String eventId) {
        UserAccount actor = users.getById(actorUserId);
        Event e = events.findById(eventId).orElseThrow(() -> new IllegalArgumentException("event_not_found"));
        boolean liked;
        if (e.getLikedBy().removeIf(u -> u.getUserId().equals(actor.getUserId()))) {
            liked = false;
        } else {
            e.getLikedBy().add(actor);
            liked = true;
        }
        events.save(e);
        long count = events.countLikes(eventId);
        return new LikeState(eventId, liked, count);
    }

    public record LikeState(String eventId, boolean liked, long count) {}
}
