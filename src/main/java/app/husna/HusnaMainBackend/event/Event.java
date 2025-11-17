package app.husna.HusnaMainBackend.event;

import app.husna.HusnaMainBackend.constants.StateProvince;
import app.husna.HusnaMainBackend.user.UserAccount;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "EVENT",
        indexes = {
                @Index(columnList = "event_id", unique = true),
                @Index(columnList = "owner_org_id"),
                @Index(columnList = "start_at"),
                @Index(columnList = "end_at")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Event {

    @Id
    @Column(name = "event_id", nullable = false, length = 40)
    private String eventId;

    /** The org (user with ORG_ADMIN role) that owns this event */
    @Column(name = "owner_org_id", nullable = false, length = 40)
    private String ownerOrgId;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "description", length = 4000)
    private String description;

    @Column(name = "venue_name", length = 200)
    private String venueName;

    @Column(name = "address_line", length = 300)
    private String addressLine;

    @Column(name = "city", length = 120)
    private String city;

    @Enumerated(EnumType.STRING)
    @Column(name = "state_province", length = 2)
    private StateProvince stateProvince;

    @Column(name = "postal_code", length = 40)
    private String postalCode;

    @Column(name = "tz", length = 60)
    private String timezone;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "published", nullable = false)
    private boolean published;

    // Hearts (saved events). Any user can heart.
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "EVENT_HEART",
            joinColumns = @JoinColumn(name = "event_id", referencedColumnName = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    )
    @Builder.Default
    private Set<UserAccount> likedBy = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void assignId() {
        if (eventId == null || eventId.isBlank()) {
            eventId = UUID.randomUUID().toString();
        }
    }

    @Transient
    public ZoneId getResolvedZone() {
        if (timezone != null && !timezone.isBlank()) {
            try {
                return ZoneId.of(timezone);
            } catch (DateTimeException ignored) {
            }
        }
        return ZoneId.of("UTC");
    }

    @Transient
    public ZonedDateTime getStartAtZoned() {
        return startAt == null ? null : ZonedDateTime.ofInstant(startAt, getResolvedZone());
    }

    @Transient
    public ZonedDateTime getEndAtZoned() {
        return endAt == null ? null : ZonedDateTime.ofInstant(endAt, getResolvedZone());
    }
}
