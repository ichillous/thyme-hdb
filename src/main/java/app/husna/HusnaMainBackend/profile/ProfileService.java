package app.husna.HusnaMainBackend.profile;

import app.husna.HusnaMainBackend.constants.OrgType;
import app.husna.HusnaMainBackend.constants.PrayerTimesMode;
import app.husna.HusnaMainBackend.constants.RoleName;
import app.husna.HusnaMainBackend.constants.Services;
import app.husna.HusnaMainBackend.constants.StateProvince;
import app.husna.HusnaMainBackend.event.Event;
import app.husna.HusnaMainBackend.event.EventRepository;
import app.husna.HusnaMainBackend.user.UserAccount;
import app.husna.HusnaMainBackend.user.UserService;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalTime;
import java.util.*;
import java.util.Optional;

@Service
public class ProfileService {

    private final ProfileRepository profiles;
    private final ProfilePrayerTimeRepository dayTimes;
    private final ProfileJummahTimeRepository jummahs;
    private final UserService users;
    private final EventRepository events;

    public ProfileService(ProfileRepository profiles,
                          ProfilePrayerTimeRepository dayTimes,
                          ProfileJummahTimeRepository jummahs,
                          UserService users,
                          EventRepository events) {
        this.profiles = profiles;
        this.dayTimes = dayTimes;
        this.jummahs = jummahs;
        this.users = users;
        this.events = events;
    }

    // ===== Helpers =====

    private boolean isOrg(UserAccount u) {
        return u.getRoles() != null && u.getRoles().contains(RoleName.ORG_ADMIN);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean hasOrgContact(Profile profile) {
        return hasText(profile.getContactEmail()) || hasText(profile.getContactPhone());
    }

    private boolean isProfileComplete(Profile profile) {
        return hasText(profile.getDisplayName())
                && hasText(profile.getAddressLine1())
                && hasText(profile.getCity())
                && profile.getStateProvince() != null
                && hasText(profile.getPostalCode())
                && hasOrgContact(profile);
    }

    private Profile bootstrapProfile(UserAccount owner) {
        Profile p = Profile.builder()
                .userId(owner.getUserId())
                .displayName(owner.getUsername() != null ? owner.getUsername() : owner.username())
                .publicProfile(isOrg(owner))         // orgs default public; general users default private
                .orgType(isOrg(owner) ? OrgType.OTHER : OrgType.NONE)
                .countryCode("US")
                .prayerTimesMode(PrayerTimesMode.DISABLED)
                .donationEnabled(false)
                .build();
        return profiles.save(p);
    }

    private Profile getOrCreateFor(UserAccount user) {
        return profiles.findByUserId(user.getUserId()).orElseGet(() -> bootstrapProfile(user));
    }

    public Optional<Profile> findByUserId(String userId) {
        return profiles.findByUserId(userId);
    }

    public boolean isOrgProfileComplete(String actorUserId) {
        UserAccount actor = users.getById(actorUserId);
        Profile profile = getOrCreateFor(actor);
        return isProfileComplete(profile);
    }

    public void requireOrgProfileComplete(String actorUserId) {
        if (!isOrgProfileComplete(actorUserId)) {
            throw new IllegalStateException("profile_incomplete");
        }
    }

    public Page<Event> getSavedEvents(String actorUserId, boolean upcoming, Pageable pageable) {
        UserAccount actor = users.getById(actorUserId);
        Instant now = Instant.now();
        return upcoming
                ? events.findByLikedBy_UserIdAndStartAtAfter(actor.getUserId(), now, pageable)
                : events.findByLikedBy_UserIdAndStartAtBefore(actor.getUserId(), now, pageable);
    }

    public List<ProfilePrayerTime> listPrayerTimes(String actorUserId) {
        Profile profile = getMine(actorUserId);
        return dayTimes.findByProfile_ProfileIdOrderByDayOfWeek(profile.getProfileId());
    }

    public List<ProfileJummahTime> listJummahTimes(String actorUserId) {
        Profile profile = getMine(actorUserId);
        return jummahs.findByProfile_ProfileIdOrderByStartTime(profile.getProfileId());
    }

    public List<ProfilePrayerTime> listPrayerTimesForOrg(String orgUserId) {
        Profile profile = profiles.findByUserId(orgUserId)
                .orElseThrow(() -> new IllegalArgumentException("profile_not_found"));
        return dayTimes.findByProfile_ProfileIdOrderByDayOfWeek(profile.getProfileId());
    }

    public List<ProfileJummahTime> listJummahTimesForOrg(String orgUserId) {
        Profile profile = profiles.findByUserId(orgUserId)
                .orElseThrow(() -> new IllegalArgumentException("profile_not_found"));
        return jummahs.findByProfile_ProfileIdOrderByStartTime(profile.getProfileId());
    }

    public Page<Event> getOrgEvents(String orgUserId, boolean upcoming, Pageable pageable) {
        Instant now = Instant.now();
        return upcoming
                ? events.findByOwnerOrgIdAndPublishedTrueAndStartAtAfter(orgUserId, now, pageable)
                : events.findByOwnerOrgIdAndPublishedTrueAndStartAtBefore(orgUserId, now, pageable);
    }

    // ===== CRUD =====

    /** Actor fetches their own profile (create if missing). */
    @Transactional
    public Profile getMine(String actorUserId) {
        UserAccount actor = users.getById(actorUserId);
        return getOrCreateFor(actor);
    }

    /** Public read: org profiles only. */
    public OrgProfileView getOrgPublic(String orgUserId, Pageable pageable) {
        UserAccount owner = users.getById(orgUserId);
        if (!isOrg(owner)) throw new UnsupportedOperationException("not_an_org");

        Profile p = profiles.findByUserId(orgUserId)
                .orElseGet(() -> bootstrapProfile(owner));
        if (!p.isPublicProfile()) {
            throw new UnsupportedOperationException("profile_not_public");
        }

        // org’s published events split by time (optional polish)
        Page<Event> owned = events.findByOwnerOrgIdAndPublishedTrue(orgUserId, pageable);
        Instant now = Instant.now();
        List<Event> upcoming = owned.getContent().stream().filter(e -> e.getStartAt().isAfter(now)).toList();
        List<Event> past     = owned.getContent().stream().filter(e -> !e.getStartAt().isAfter(now)).toList();

        List<ProfilePrayerTime> week = dayTimes.findByProfile_ProfileIdOrderByDayOfWeek(p.getProfileId());
        List<ProfileJummahTime> jts  = jummahs.findByProfile_ProfileIdOrderByStartTime(p.getProfileId());

        return OrgProfileView.from(p, week, jts, upcoming, past, owned.getNumber(), owned.getTotalPages());
    }

    /** General user private read w/ saved events. */
    public UserPrivateProfileView getUserPrivate(String actorUserId, Pageable pageable) {
        UserAccount actor = users.getById(actorUserId);
        Profile p = getOrCreateFor(actor);

        Instant now = Instant.now();
        Page<Event> saved = events.findByLikedBy_UserId(actor.getUserId(), pageable);
        List<Event> upcoming = saved.getContent().stream().filter(e -> e.getStartAt().isAfter(now)).toList();
        List<Event> past     = saved.getContent().stream().filter(e -> !e.getStartAt().isAfter(now)).toList();

        return UserPrivateProfileView.from(p, upcoming, past, saved.getNumber(), saved.getTotalPages());
    }

    /** Update profile. Orgs can update org-only fields; users can update safe personal bits. */
    @Transactional
    public Profile updateMine(String actorUserId, UpdateProfile req) {
        UserAccount actor = users.getById(actorUserId);
        Profile p = getOrCreateFor(actor);

        // Always allowed (owner)
        if (req.displayName() != null) p.setDisplayName(req.displayName());
        if (req.bio() != null) p.setBio(req.bio());
        if (req.website() != null) p.setWebsite(req.website());
        if (req.contactEmail() != null) p.setContactEmail(req.contactEmail());
        if (req.contactPhone() != null) p.setContactPhone(req.contactPhone());
        if (req.logoUrl() != null) p.setLogoUrl(req.logoUrl());
        if (req.bannerUrl() != null) p.setBannerUrl(req.bannerUrl());

        // Privacy: general users stay private
        boolean actorIsOrg = isOrg(actor);
        p.setPublicProfile(actorIsOrg);

        // ORG-only fields
        if (actorIsOrg) {
            if (req.addressLine1() != null) p.setAddressLine1(req.addressLine1());
            if (req.addressLine2() != null) p.setAddressLine2(req.addressLine2());
            if (req.city() != null) p.setCity(req.city());
            if (req.stateProvince() != null) p.setStateProvince(req.stateProvince());
            if (req.postalCode() != null) p.setPostalCode(req.postalCode());
            if (req.countryCode() != null) p.setCountryCode(req.countryCode().toUpperCase(Locale.ROOT));

            if (req.orgType() != null) p.setOrgType(req.orgType());

            if (req.services() != null) {
                p.getServices().clear();
                p.getServices().addAll(req.services());
            }

            if (req.programsOffered() != null) p.setProgramsOffered(req.programsOffered());
            if (req.classesOffered() != null) p.setClassesOffered(req.classesOffered());

            if (req.prayerTimesMode() != null) p.setPrayerTimesMode(req.prayerTimesMode());

            if (req.donationEnabled() != null) p.setDonationEnabled(req.donationEnabled());
            if (req.donationProvider() != null) p.setDonationProvider(req.donationProvider());
            if (req.donationUrl() != null) p.setDonationUrl(req.donationUrl());
        }

        return profiles.save(p);
    }

    /** Replace weekly times + jummah list (orgs only). */
    @Transactional
    public void replacePrayerSchedule(String actorUserId, ReplacePrayerSchedule req) {
        UserAccount actor = users.getById(actorUserId);
        if (!isOrg(actor)) throw new UnsupportedOperationException("org_only_operation");

        Profile p = profiles.findByUserId(actor.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("profile_not_found"));

        // Require a suitable mode
        if (p.getPrayerTimesMode() == PrayerTimesMode.DISABLED) {
            throw new IllegalStateException("prayer_times_disabled");
        }

        // Replace week
        dayTimes.deleteByProfile_ProfileId(p.getProfileId());
        if (req.week() != null) {
            for (ReplacePrayerSchedule.Day d : req.week()) {
                if (d.dayOfWeek() < 1 || d.dayOfWeek() > 7) {
                    throw new IllegalArgumentException("invalid_day_of_week");
                }
                ProfilePrayerTime row = ProfilePrayerTime.builder()
                        .profile(p)
                        .dayOfWeek(d.dayOfWeek())
                        .fajr(d.fajr())
                        .dhuhr(d.dhuhr())
                        .asr(d.asr())
                        .maghrib(d.maghrib())
                        .isha(d.isha())
                        .build();
                dayTimes.save(row);
            }
        }

        // Replace jummah
        jummahs.deleteByProfile_ProfileId(p.getProfileId());
        if (req.jummah() != null) {
            for (ReplacePrayerSchedule.Jummah j : req.jummah()) {
                ProfileJummahTime jt = ProfileJummahTime.builder()
                        .profile(p)
                        .startTime(j.startTime())
                        .notes(j.notes())
                        .build();
                jummahs.save(jt);
            }
        }

        p.setPrayerTimesLastUpdated(Instant.now());
        profiles.save(p);
    }

    // ===== DTOs =====

    public record UpdateProfile(
            String displayName,
            String bio,
            String website,
            String contactEmail,
            String contactPhone,
            String logoUrl,
            String bannerUrl,
            // ORG-only
            String addressLine1,
            String addressLine2,
            String city,
            StateProvince stateProvince,
            String postalCode,
            String countryCode,
            OrgType orgType,
            Set<Services> services,
            String programsOffered,
            String classesOffered,
            PrayerTimesMode prayerTimesMode,
            Boolean donationEnabled,
            String donationProvider,
            String donationUrl
    ) {}

    public record ReplacePrayerSchedule(
            List<Day> week,
            List<Jummah> jummah
    ) {
        public record Day(int dayOfWeek, LocalTime fajr, LocalTime dhuhr, LocalTime asr, LocalTime maghrib, LocalTime isha) {}
        public record Jummah(LocalTime startTime, String notes) {}
    }

    public record OrgProfileView(
            Profile profile,
            List<ProfilePrayerTime> weekly,
            List<ProfileJummahTime> jummahTimes,
            List<Event> upcomingEvents,
            List<Event> pastEvents,
            int page,
            int totalPages
    ) {
        static OrgProfileView from(Profile p,
                                   List<ProfilePrayerTime> week,
                                   List<ProfileJummahTime> jt,
                                   List<Event> upcoming,
                                   List<Event> past,
                                   int page, int totalPages) {
            return new OrgProfileView(p, week, jt, upcoming, past, page, totalPages);
        }
    }

    public record UserPrivateProfileView(
            Profile profile,
            List<Event> savedUpcoming,
            List<Event> savedPast,
            int page,
            int totalPages
    ) {
        static UserPrivateProfileView from(Profile p,
                                           List<Event> up,
                                           List<Event> past,
                                           int page, int totalPages) {
            return new UserPrivateProfileView(p, up, past, page, totalPages);
        }
    }
}
