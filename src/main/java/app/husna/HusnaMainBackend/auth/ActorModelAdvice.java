package app.husna.HusnaMainBackend.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class ActorModelAdvice {

    @ModelAttribute("actorUserId")
    public String actorUserId(HttpServletRequest request) {
        Object attr = request.getAttribute(AuthSessionInterceptor.REQUEST_ATTR);
        return (attr instanceof String) ? (String) attr : null;
    }
}
