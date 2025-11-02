package app.husna.HusnaMainBackend.profile;

import app.husna.HusnaMainBackend.constants.OrgType;
import app.husna.HusnaMainBackend.constants.PrayerTimesMode;
import app.husna.HusnaMainBackend.constants.Services;
import app.husna.HusnaMainBackend.user.UserAccount;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "PROFILE",
        indexes = {
                @Index(columnList = "profile_id", unique = true),
                @Index(columnList = "user_id", unique = true),
                @Index(columnList = "public_profile"),
                @Index(columnList = "org_type")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Profile {

    @Id
    @Column(name = "profile_id", nullable = false, length = 40)
    private String profileId;

    /** owner of this profile (1:1 with user) */
    @Column(name = "user_id", nullable = false, length = 40, unique = true)
    private String userId;

    // Basic presentation
    @Column(name = "display_name", length = 160)
    private String displayName;

    @Column(name = "bio", length = 4000)
    private String bio;

    /** General-user profiles are private by default; orgs are public by default */
    @Column(name = "public_profile", nullable = false)
    private boolean publicProfile;

    // Contact / socials (optional but handy)
    @Column(name = "website", length = 300)
    private String website;

    @Column(name = "contact_email", length = 200)
    private String contactEmail;

    @Column(name = "contact_phone", length = 40)
    private String contactPhone;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "banner_url", length = 500)
    private String bannerUrl;

    // Address (displayed for orgs)
    @Column(name = "street", length = 300)
    private String street;

    @Column(name = "city", length = 120)
    private String city;

    @Column(name = "region", length = 120)
    private String region;

    @Column(name = "postal_code", length = 40)
    private String postalCode;

    // Organization flags
    @Enumerated(EnumType.STRING)
    @Column(name = "org_type", nullable = false)
    private OrgType orgType;

    // Services (orgs)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "PROFILE_SERVICES", joinColumns = @JoinColumn(name = "profile_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "service_name", nullable = false)
    @Builder.Default
    private Set<Services> services = new HashSet<>();

    // Programs / classes (simple MVP text; can become richer models later)
    @Column(name = "programs_offered", length = 4000)
    private String programsOffered;

    @Column(name = "classes_offered", length = 4000)
    private String classesOffered;

    // Prayer times config (orgs)
    @Enumerated(EnumType.STRING)
    @Column(name = "prayer_times_mode", nullable = false)
    private PrayerTimesMode prayerTimesMode;

    @Column(name = "prayer_times_last_updated")
    private Instant prayerTimesLastUpdated;

    // Donations (placeholder for Stripe later)
    @Column(name = "donation_enabled", nullable = false)
    private boolean donationEnabled;

    @Column(name = "donation_provider", length = 80)
    private String donationProvider; // e.g., "STRIPE"

    @Column(name = "donation_url", length = 500)
    private String donationUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (profileId == null || profileId.isBlank()) {
            profileId = UUID.randomUUID().toString();
        }
    }
}
