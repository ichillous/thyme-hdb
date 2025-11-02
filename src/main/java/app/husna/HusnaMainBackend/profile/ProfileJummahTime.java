package app.husna.HusnaMainBackend.profile;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "PROFILE_JUMMAH_TIME", indexes = @Index(columnList = "profile_id"))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ProfileJummahTime {

    @Id
    @Column(name = "id", length = 40)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private Profile profile;

    @Column(name = "start_time")
    private LocalTime startTime; // khutbah start (or prayer start if you prefer)

    @Column(name = "notes", length = 300)
    private String notes; // e.g., language or capacity

    @PrePersist
    void onCreate() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
    }
}
