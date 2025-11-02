package app.husna.HusnaMainBackend.user;

import app.husna.HusnaMainBackend.constants.RoleName;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "USER_ACCOUNT",
        indexes = {
            @Index(columnList = "userId", unique = true),
            @Index(columnList = "username", unique = true),
            @Index(columnList = "email", unique = true),
            @Index(columnList = "phone", unique = true)
        }
)
public class UserAccount {

    @Column(name = "user_id", nullable = false)
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String userId;

    @Column(name="email", length = 120, unique = true)
    private String email;

    @Column(name = "username" , length = 120, unique = true)
    private String username;

    @Column(name = "phone",length = 20, unique = true)
    private String phone;

    /* use role defaults to GENERAL_USER */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Set<RoleName> roles = new HashSet<>();


    @CreationTimestamp
    private Instant createdAt = Instant.now();
    @UpdateTimestamp
    private Instant updatedAt;


    public String username() {
        return (email != null && !email.isBlank()) ? email : phone;
    }
}