package app.husna.HusnaMainBackend.profile;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "PROFILE_PRAYER_TIME",
        uniqueConstraints = @UniqueConstraint(columnNames = {"profile_id", "day_of_week"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ProfilePrayerTime {

    @Id
    @Column(name = "id", length = 40)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    /** ISO-8601 day of week: 1=Mon .. 7=Sun */
    @Column(name = "day_of_week", nullable = false)
    private int dayOfWeek;

    @Column(name = "fajr")
    private LocalTime fajr;

    @Column(name = "dhuhr")
    private LocalTime dhuhr;

    @Column(name = "asr")
    private LocalTime asr;

    @Column(name = "maghrib")
    private LocalTime maghrib;

    @Column(name = "isha")
    private LocalTime isha;

    @PrePersist
    void onCreate() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
    }
}
