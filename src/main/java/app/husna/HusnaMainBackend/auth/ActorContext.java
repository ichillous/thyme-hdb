package app.husna.HusnaMainBackend.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Optional;

@Component
@RequestScope
public class ActorContext {

    private final HttpServletRequest request;

    public ActorContext(HttpServletRequest request) {
        this.request = request;
    }

    public Optional<String> actorUserId() {
        Object attr = request.getAttribute(AuthSessionInterceptor.REQUEST_ATTR);
        if (attr instanceof String actor) {
            return Optional.of(actor);
        }
        return Optional.empty();
    }

    public String requireActorUserId() {
        return actorUserId().orElseThrow(() -> new MissingActorException("actor_required"));
    }
}
