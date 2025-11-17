package app.husna.HusnaMainBackend.auth;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthSessionService {

    public static final String SESSION_COOKIE_NAME = "husna_session";
    private static final Duration SESSION_TTL = Duration.ofHours(24);

    private final SecureRandom random = new SecureRandom();
    private final Map<String, SessionData> sessions = new ConcurrentHashMap<>();

    public Duration sessionTtl() {
        return SESSION_TTL;
    }

    public String createSession(String userId) {
        cleanupExpired();
        String token = generateToken();
        sessions.put(token, new SessionData(userId, Instant.now()));
        return token;
    }

    public Optional<String> resolve(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        SessionData data = sessions.get(token);
        Instant now = Instant.now();
        if (data == null) {
            return Optional.empty();
        }
        if (data.isExpired(now)) {
            sessions.remove(token);
            return Optional.empty();
        }
        data.touch(now);
        return Optional.of(data.userId);
    }

    public void invalidate(String token) {
        if (token != null && !token.isBlank()) {
            sessions.remove(token);
        }
    }

    private void cleanupExpired() {
        Instant now = Instant.now();
        sessions.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    private String generateToken() {
        byte[] buffer = new byte[32];
        random.nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }

    private static final class SessionData {
        private final String userId;
        private final Instant createdAt;
        private Instant lastSeen;

        SessionData(String userId, Instant createdAt) {
            this.userId = userId;
            this.createdAt = createdAt;
            this.lastSeen = createdAt;
        }

        void touch(Instant when) {
            this.lastSeen = when;
        }

        boolean isExpired(Instant now) {
            return createdAt.plus(SESSION_TTL).isBefore(now);
        }
    }
}
