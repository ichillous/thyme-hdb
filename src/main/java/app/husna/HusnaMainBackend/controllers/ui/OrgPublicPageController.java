package app.husna.HusnaMainBackend.controllers.ui;

import app.husna.HusnaMainBackend.event.Event;
import app.husna.HusnaMainBackend.event.EventRepository;
import app.husna.HusnaMainBackend.profile.Profile;
import app.husna.HusnaMainBackend.profile.ProfileJummahTime;
import app.husna.HusnaMainBackend.profile.ProfilePrayerTime;
import app.husna.HusnaMainBackend.profile.ProfileService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class OrgPublicPageController {

    private final ProfileService profiles;
    private final EventRepository events;

    public OrgPublicPageController(ProfileService profiles, EventRepository events) {
        this.profiles = profiles;
        this.events = events;
    }

    @GetMapping("/org/{orgUserId}")
    public String publicProfile(@PathVariable String orgUserId,
                                @RequestParam(defaultValue = "upcoming") String tab,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(required = false) String actorUserId,
                                Model model) {
        Profile profile = profiles.findByUserId(orgUserId)
                .orElseThrow(() -> new IllegalArgumentException("profile_not_found"));
        String normalizedTab = tab.equalsIgnoreCase("past") ? "past" : "upcoming";
        boolean upcoming = normalizedTab.equals("upcoming");
        Sort sort = upcoming
                ? Sort.by(Sort.Direction.ASC, "startAt")
                : Sort.by(Sort.Direction.DESC, "startAt");
        Pageable pageable = PageRequest.of(Math.max(page, 0), 20, sort);
        Page<Event> events = profiles.getOrgEvents(orgUserId, upcoming, pageable);
        List<ProfilePrayerTime> week = profiles.listPrayerTimesForOrg(orgUserId);
        List<ProfileJummahTime> jummah = profiles.listJummahTimesForOrg(orgUserId);

        Map<String, Long> likeCounts = events.getContent().stream()
                .collect(Collectors.toMap(Event::getEventId, e -> this.events.countLikes(e.getEventId())));
        Set<String> likedIds = java.util.Collections.emptySet();
        if (actorUserId != null) {
            likedIds = this.events.findByLikedBy_UserId(actorUserId, org.springframework.data.domain.Pageable.unpaged())
                    .stream().map(Event::getEventId).collect(Collectors.toSet());
        }

        model.addAttribute("profile", profile);
        model.addAttribute("tab", normalizedTab);
        model.addAttribute("page", events);
        model.addAttribute("week", week);
        model.addAttribute("jummah", jummah);
        model.addAttribute("orgUserId", orgUserId);
        model.addAttribute("actorUserId", actorUserId);
        model.addAttribute("likeCounts", likeCounts);
        model.addAttribute("likedIds", likedIds);
        return "org_public";
    }
}
