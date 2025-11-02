package app.husna.HusnaMainBackend.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, String> {

    // === Methods expected by UserService (normalized lookups) ===
    Optional<UserAccount> findByEmail(String email);
    Optional<UserAccount> findByPhone(String phone);
    Optional<UserAccount> findByUsername(String username);

    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    boolean existsByUsername(String username);

    // === Optional convenience (email case-insensitive) ===
    Optional<UserAccount> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
}
