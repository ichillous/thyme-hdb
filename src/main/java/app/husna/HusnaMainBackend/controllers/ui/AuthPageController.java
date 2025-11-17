package app.husna.HusnaMainBackend.controllers.ui;

import app.husna.HusnaMainBackend.auth.AuthSessionService;
import app.husna.HusnaMainBackend.user.UserAccount;
import app.husna.HusnaMainBackend.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

@Controller
public class AuthPageController {

    private final UserService users;
    private final AuthSessionService sessions;

    public AuthPageController(UserService users, AuthSessionService sessions) {
        this.users = users;
        this.sessions = sessions;
    }

    // Page shells (include the modal immediately)
    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String next,
                            @RequestParam(required = false) String nextTemplate,
                            @RequestParam(required = false) String afterEventId,
                            @RequestParam(required = false) String contact,
                            @RequestParam(required = false) String error,
                            Model model) {
        model.addAttribute("next", next);
        model.addAttribute("nextTemplate", nextTemplate);
        model.addAttribute("afterEventId", afterEventId);
        model.addAttribute("contact", contact);
        model.addAttribute("error", error);
        return "login";
    }

    @GetMapping("/signup")
    public String signupPage(@RequestParam(required = false) String next,
                             @RequestParam(required = false) String nextTemplate,
                             @RequestParam(required = false) String afterEventId,
                             @RequestParam(required = false) String error,
                             Model model) {
        model.addAttribute("next", next);
        model.addAttribute("nextTemplate", nextTemplate);
        model.addAttribute("afterEventId", afterEventId);
        model.addAttribute("error", error);
        return "signup";
    }

    // Fragments (HTMX fetch/replace)
    @GetMapping("/auth/login/form")
    public String loginForm(@RequestParam(required = false) String next,
                            @RequestParam(required = false) String nextTemplate,
                            @RequestParam(required = false) String afterEventId,
                            @RequestParam(required = false) String contact,
                            @RequestParam(required = false) String error,
                            Model model) {
        model.addAttribute("next", next);
        model.addAttribute("nextTemplate", nextTemplate);
        model.addAttribute("afterEventId", afterEventId);
        model.addAttribute("contact", contact);
        model.addAttribute("error", error);
        return "fragments/auth :: login";
    }

    @GetMapping("/auth/signup/form")
    public String signupForm(@RequestParam(required = false) String next,
                             @RequestParam(required = false) String nextTemplate,
                             @RequestParam(required = false) String afterEventId,
                             @RequestParam(required = false) String error,
                             Model model) {
        model.addAttribute("next", next);
        model.addAttribute("nextTemplate", nextTemplate);
        model.addAttribute("afterEventId", afterEventId);
        model.addAttribute("error", error);
        return "fragments/auth :: signup";
    }

    // === OTP flow (dev/simple) ===

    // Step 1: request OTP for contact (email or phone)
    @PostMapping("/auth/otp/request")
    public String requestOtp(@RequestParam String contact,
                             @RequestParam(required = false) String next,
                             @RequestParam(required = false) String nextTemplate,
                             @RequestParam(required = false) String afterEventId,
                             Model model) {
        try {
            String code = users.requestMockOtp(contact); // MVP: returns code
            model.addAttribute("contact", contact);
            model.addAttribute("devCode", code); // show for dev convenience
            model.addAttribute("next", next);
            model.addAttribute("nextTemplate", nextTemplate);
            model.addAttribute("afterEventId", afterEventId);
            return "fragments/auth :: verify";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", mapAuthError(ex.getMessage()));
            model.addAttribute("contact", contact);
            model.addAttribute("next", next);
            model.addAttribute("nextTemplate", nextTemplate);
            model.addAttribute("afterEventId", afterEventId);
            return "fragments/auth :: login";
        }
    }

    // Step 2: verify OTP; "log in" -> return success modal with userId
    @PostMapping("/auth/otp/verify")
    public String verifyOtp(@RequestParam String contact,
                            @RequestParam String code,
                            @RequestParam(required = false) String next,
                            @RequestParam(required = false) String nextTemplate,
                            @RequestParam(required = false) String afterEventId,
                            Model model,
                            HttpServletRequest request,
                            HttpServletResponse response) {
        try {
            UserAccount ua = users.verifyMockOtp(contact, code);
            startSession(ua, request, response);
            model.addAttribute("user", ua);
            model.addAttribute("next", resolveNext(next, nextTemplate, ua));
            model.addAttribute("afterEventId", afterEventId);
            return "fragments/auth :: login_success";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("contact", contact);
            model.addAttribute("next", next);
            model.addAttribute("nextTemplate", nextTemplate);
            model.addAttribute("afterEventId", afterEventId);
            model.addAttribute("error", mapAuthError(ex.getMessage()));
            return "fragments/auth :: verify";
        }
    }

    // === Signup ===
    @PostMapping("/auth/signup")
    public String signup(@RequestParam(required = false) String email,
                         @RequestParam(required = false) String phone,
                         @RequestParam(required = false) String username,
                         @RequestParam(defaultValue = "false") boolean org,
                         @RequestParam(required = false) String next,
                         @RequestParam(required = false) String nextTemplate,
                         @RequestParam(required = false) String afterEventId,
                         Model model) {
        try {
            UserAccount ua = org
                    ? users.createOrgAdmin(email, phone, username)
                    : users.signUp(email, phone, username);
            // prefer email as contact for OTP, else phone
            String contact = (ua.getEmail() != null && !ua.getEmail().isBlank())
                    ? ua.getEmail() : ua.getPhone();
            model.addAttribute("user", ua);
            model.addAttribute("contact", contact);
            model.addAttribute("next", next);
            model.addAttribute("nextTemplate", nextTemplate);
            model.addAttribute("afterEventId", afterEventId);
            return "fragments/auth :: signup_success";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("next", next);
            model.addAttribute("nextTemplate", nextTemplate);
            model.addAttribute("afterEventId", afterEventId);
            model.addAttribute("error", mapAuthError(ex.getMessage()));
            return "fragments/auth :: signup";
        }
    }

    private String mapAuthError(String code) {
        return switch (code == null ? "" : code) {
            case "user_not_found" -> "We couldn\u2019t find an account for that email or phone. Try again or sign up.";
            case "invalid_otp" -> "That code didn\u2019t match. Please try again.";
            case "contact_required" -> "Enter an email or phone number to continue.";
            case "email_taken" -> "That email is already in use.";
            case "phone_taken" -> "That phone number is already in use.";
            case "username_taken" -> "That username is already in use.";
            case "otp_expired" -> "That code expired. Request a new one.";
            case "otp_rate_limited" -> "You have requested too many codes. Please wait a moment before trying again.";
            case "otp_attempts_exceeded" -> "Too many incorrect attempts. Request a new code.";
            default -> "Something went wrong. Please try again.";
        };
    }

    private String resolveNext(String next, String nextTemplate, UserAccount ua) {
        if (next != null && !next.isBlank()) {
            return next;
        }
        if (nextTemplate != null && !nextTemplate.isBlank()) {
            return nextTemplate.replace("__USER__", ua.getUserId());
        }
        return null;
    }

    private void startSession(UserAccount ua, HttpServletRequest request, HttpServletResponse response) {
        String token = sessions.createSession(ua.getUserId());
        ResponseCookie cookie = ResponseCookie.from(AuthSessionService.SESSION_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(request.isSecure())
                .path("/")
                .sameSite("Lax")
                .maxAge(sessions.sessionTtl())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
