package app.husna.HusnaMainBackend.controllers.ui;

import app.husna.HusnaMainBackend.auth.ActorContext;
import app.husna.HusnaMainBackend.event.Event;
import app.husna.HusnaMainBackend.event.EventRepository;
import app.husna.HusnaMainBackend.profile.Profile;
import app.husna.HusnaMainBackend.profile.ProfileJummahTime;
import app.husna.HusnaMainBackend.profile.ProfilePrayerTime;
import app.husna.HusnaMainBackend.profile.ProfileService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class OrgPublicPageController {

    private final ProfileService profiles;
    private final EventRepository events;
    private final ObjectProvider<ActorContext> actorContext;
    private static final List<String> DAY_NAMES = List.of("Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday");
    private static final List<Integer> DAY_ORDER = List.of(6, 7, 1, 2, 3, 4, 5);

    public OrgPublicPageController(ProfileService profiles, EventRepository events, ObjectProvider<ActorContext> actorContext) {
        this.profiles = profiles;
        this.events = events;
        this.actorContext = actorContext;
    }

    @ModelAttribute("dayNames")
    public List<String> dayNames() {
        return DAY_NAMES;
    }

    @GetMapping("/org/{orgUserId}")
    public String publicProfile(@PathVariable String orgUserId,
                                @RequestParam(defaultValue = "upcoming") String tab,
                                @RequestParam(defaultValue = "0") int page,
                                Model model) {
        Profile profile = profiles.findByUserId(orgUserId)
                .orElseThrow(() -> new IllegalArgumentException("profile_not_found"));
        if (!profile.isPublicProfile()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "profile_not_public");
        }
        String normalizedTab = tab.equalsIgnoreCase("past") ? "past" : "upcoming";
        boolean upcoming = normalizedTab.equals("upcoming");
        Sort sort = upcoming
                ? Sort.by(Sort.Direction.ASC, "startAt")
                : Sort.by(Sort.Direction.DESC, "startAt");
        Pageable pageable = PageRequest.of(Math.max(page, 0), 20, sort);
        Page<Event> events = profiles.getOrgEvents(orgUserId, upcoming, pageable);
        List<ProfilePrayerTime> week = profiles.listPrayerTimesForOrg(orgUserId);
        List<ProfileJummahTime> jummah = profiles.listJummahTimesForOrg(orgUserId);
        week = orderWeek(week);
        List<String> eventIds = events.getContent().stream().map(Event::getEventId).toList();
        Map<String, Long> likeCounts = eventIds.isEmpty()
                ? Map.of()
                : this.events.countLikesByEventIds(eventIds).stream()
                .collect(Collectors.toMap(EventRepository.EventLikeCount::getEventId,
                        EventRepository.EventLikeCount::getLikeCount));
        ActorContext ctx = actorContext.getIfAvailable();
        String actorUserId = ctx != null ? ctx.actorUserId().orElse(null) : null;
        Set<String> likedIds = (actorUserId != null && !eventIds.isEmpty())
                ? Set.copyOf(this.events.findLikedEventIds(actorUserId, eventIds))
                : Set.of();

        model.addAttribute("profile", profile);
        model.addAttribute("tab", normalizedTab);
        model.addAttribute("page", events);
        model.addAttribute("week", week);
        model.addAttribute("jummah", jummah);
        model.addAttribute("orgUserId", orgUserId);
        model.addAttribute("likeCounts", likeCounts);
        model.addAttribute("likedIds", likedIds);
        return "org_public";
    }

    private static List<ProfilePrayerTime> orderWeek(List<ProfilePrayerTime> week) {
        if (week == null || week.isEmpty()) {
            return List.of();
        }
        Map<Integer, ProfilePrayerTime> byDay = week.stream()
                .collect(Collectors.toMap(ProfilePrayerTime::getDayOfWeek, ptr -> ptr, (a, b) -> a));
        List<ProfilePrayerTime> ordered = new ArrayList<>();
        for (int day : DAY_ORDER) {
            ProfilePrayerTime ptr = byDay.get(day);
            if (ptr != null) {
                ordered.add(ptr);
            }
        }
        return ordered;
    }
}
