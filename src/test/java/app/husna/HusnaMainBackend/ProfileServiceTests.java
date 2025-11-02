package app.husna.HusnaMainBackend;

import app.husna.HusnaMainBackend.constants.RoleName;
import app.husna.HusnaMainBackend.event.Event;
import app.husna.HusnaMainBackend.event.EventService;
import app.husna.HusnaMainBackend.profile.ProfileService;
import app.husna.HusnaMainBackend.user.UserAccount;
import app.husna.HusnaMainBackend.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ProfileServiceTests {

    @Autowired
    private ProfileService profiles;
    @Autowired
    private UserService users;
    @Autowired
    private EventService events;

    private String orgId;
    private String userId;
    private String upcomingEventId;
    private String pastEventId;

    @BeforeEach
    void setup() {
        UserAccount org = users.findByEmail("org@example.com")
                .orElseGet(() -> users.createOrgAdmin("org@example.com", null, "Org"));
        this.orgId = org.getUserId();

        UserAccount attendee = users.findByEmail("user@example.com")
                .orElseGet(() -> users.signUp("user@example.com", null, "User"));
        attendee.getRoles().add(RoleName.GENERAL_USER);
        userId = attendee.getUserId();

        Instant now = Instant.now();
        upcomingEventId = events.createEvent(orgId, new EventService.CreateEvent(
                "Future Event",
                "Soon",
                "Venue",
                "123 Main",
                "City",
                "Region",
                "90000",
                "UTC",
                now.plus(2, ChronoUnit.DAYS),
                now.plus(3, ChronoUnit.DAYS),
                true
        )).getEventId();

        pastEventId = events.createEvent(orgId, new EventService.CreateEvent(
                "Past Event",
                "Earlier",
                "Venue",
                "123 Main",
                "City",
                "Region",
                "90000",
                "UTC",
                now.minus(3, ChronoUnit.DAYS),
                now.minus(2, ChronoUnit.DAYS),
                true
        )).getEventId();

        events.heart(userId, upcomingEventId);
        events.heart(userId, pastEventId);
    }

    @Test
    void savedEventsUpcomingFiltersCorrectly() {
        Page<Event> upcoming = profiles.getSavedEvents(userId, true, PageRequest.of(0, 20));
        assertThat(upcoming.getContent()).extracting(Event::getEventId).containsExactly(upcomingEventId);

        Page<Event> past = profiles.getSavedEvents(userId, false, PageRequest.of(0, 20));
        assertThat(past.getContent()).extracting(Event::getEventId).containsExactly(pastEventId);
    }

    @Test
    void orgEventsSplitByTime() {
        Page<Event> upcoming = profiles.getOrgEvents(orgId, true, PageRequest.of(0, 20));
        assertThat(upcoming.getContent()).extracting(Event::getEventId).contains(upcomingEventId);

        Page<Event> past = profiles.getOrgEvents(orgId, false, PageRequest.of(0, 20));
        assertThat(past.getContent()).extracting(Event::getEventId).contains(pastEventId);
    }
}
