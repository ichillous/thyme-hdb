package app.husna.HusnaMainBackend.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

@Component
public class AuthSessionInterceptor implements HandlerInterceptor {

    public static final String REQUEST_ATTR = "actorUserId";

    private final AuthSessionService sessions;

    public AuthSessionInterceptor(AuthSessionService sessions) {
        this.sessions = sessions;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = readCookie(request);
        if (token == null) {
            return true;
        }
        var resolved = sessions.resolve(token);
        if (resolved.isPresent()) {
            request.setAttribute(REQUEST_ATTR, resolved.get());
        } else {
            sessions.invalidate(token);
            clearCookie(request, response);
        }
        return true;
    }

    private String readCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> AuthSessionService.SESSION_COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private void clearCookie(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = new Cookie(AuthSessionService.SESSION_COOKIE_NAME, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());
        response.addCookie(cookie);
    }
}
