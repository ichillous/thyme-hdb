package app.husna.HusnaMainBackend;

import app.husna.HusnaMainBackend.constants.PrayerTimesMode;
import app.husna.HusnaMainBackend.event.EventService;
import app.husna.HusnaMainBackend.profile.ProfileService;
import app.husna.HusnaMainBackend.user.UserAccount;
import app.husna.HusnaMainBackend.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@AutoConfigureMockMvc
class SavedAndOrgPagesTests {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UserService users;
    @Autowired
    private EventService events;
    @Autowired
    private ProfileService profiles;

    private String orgId;
    private String userId;
    private String futureEventId;

    @BeforeEach
    void init() {
        UserAccount org = users.findByEmail("org2@example.com")
                .orElseGet(() -> users.createOrgAdmin("org2@example.com", null, "Org Two"));
        this.orgId = org.getUserId();
        UserAccount user = users.findByEmail("saved@example.com")
                .orElseGet(() -> users.signUp("saved@example.com", null, "Saver"));
        this.userId = user.getUserId();

        Instant now = Instant.now();
        futureEventId = events.createEvent(orgId, new EventService.CreateEvent(
                "Community Dinner",
                "Details",
                "Hall",
                "456 Elm",
                "Metro",
                "ST",
                "00000",
                "UTC",
                now.plus(5, ChronoUnit.DAYS),
                now.plus(6, ChronoUnit.DAYS),
                true
        )).getEventId();

        events.heart(userId, futureEventId);

        profiles.updateMine(orgId, new ProfileService.UpdateProfile(
                "Org Two",
                "Serving the community",
                "https://example.org",
                "info@example.org",
                "555-111-2222",
                "https://img/logo.png",
                "https://img/banner.png",
                "123 Center St",
                "Metro",
                "ST",
                "00000",
                null,
                null,
                "Programs",
                "Classes",
                PrayerTimesMode.MANUAL_WEEKLY,
                true,
                "Stripe",
                "https://donate.example.org"
        ));

        profiles.replacePrayerSchedule(orgId, new ProfileService.ReplacePrayerSchedule(List.of(), List.of()));

        profiles.replacePrayerSchedule(orgId, new ProfileService.ReplacePrayerSchedule(
                List.of(new ProfileService.ReplacePrayerSchedule.Day(1, LocalTime.of(5, 30), LocalTime.NOON,
                        LocalTime.of(15, 30), LocalTime.of(18, 0), LocalTime.of(19, 30))),
                List.of(new ProfileService.ReplacePrayerSchedule.Jummah(LocalTime.of(13, 0), "First Khutbah"))
        ));
    }

    @Test
    void savedPageShowsUpcomingEvent() throws Exception {
        mvc.perform(get("/saved").param("actorUserId", userId))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Community Dinner")));
    }

    @Test
    void orgPublicPageRendersSections() throws Exception {
        mvc.perform(get("/org/" + orgId).param("actorUserId", userId))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Org Two")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Support this organization")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Community Dinner")));
    }
}
