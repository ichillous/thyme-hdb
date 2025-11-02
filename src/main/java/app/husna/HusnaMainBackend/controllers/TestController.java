package app.husna.HusnaMainBackend.controllers;

import app.husna.HusnaMainBackend.user.UserAccount;
import app.husna.HusnaMainBackend.user.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Lightweight controller to exercise the {@link UserService} endpoints in the MVP.
 * Endpoints are intentionally simple and return domain objects directly.
 *
 * Notes:
 * - @RestController returns JSON bodies by default. (See Spring guide)
 * - Pageable is accepted directly as a method parameter for pagination.
 * - Simple @ExceptionHandler mappings for clear HTTP statuses.
 *
 * Paths are under /api/test/users to keep this "test" surface distinct.
 */
@RestController
@RequestMapping("/api/test/users")
public class TestController {

    private final UserService users;

    public TestController(UserService users) {
        this.users = users;
    }

    // ---- Create (Sign up) ----

    @PostMapping("/signup")
    public ResponseEntity<UserAccount> signUp(@RequestBody SignUpRequest req) {
        UserAccount created = users.signUp(req.email(), req.phone(), req.username());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/signup/org-admin")
    public ResponseEntity<UserAccount> createOrgAdmin(@RequestBody SignUpRequest req) {
        UserAccount created = users.createOrgAdmin(req.email(), req.phone(), req.username());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/signup/teacher")
    public ResponseEntity<UserAccount> createTeacher(@RequestBody SignUpRequest req) {
        UserAccount created = users.createTeacher(req.email(), req.phone(), req.username());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ---- Mock OTP Login ----

    @PostMapping("/otp/request")
    public Map<String, String> requestOtp(@RequestBody ContactRequest req) {
        String code = users.requestMockOtp(req.contact());
        // Return the code (MVP mock behavior)
        return Map.of("contact", req.contact(), "code", code);
    }

    @PostMapping("/otp/verify")
    public UserAccount verifyOtp(@RequestBody VerifyOtpRequest req) {
        return users.verifyMockOtp(req.contact(), req.code());
    }

    // ---- Read / List ----

    @GetMapping("/{id}")
    public UserAccount getById(@PathVariable String id) {
        return users.getById(id);
    }

    @GetMapping
    public Page<UserAccount> list(Pageable pageable) {
        return users.list(pageable);
    }

    // ---- Updates ----

    @PatchMapping("/{id}/contact")
    public UserAccount updateContact(@PathVariable String id, @RequestBody UpdateContactRequest req) {
        return users.updateContact(id, req.email(), req.phone());
    }

    @PatchMapping("/{id}/username")
    public UserAccount updateUsername(@PathVariable String id, @RequestBody UpdateUsernameRequest req) {
        return users.updateUsername(id, req.username());
    }

    // ---- Roles (no SUPER_ADMIN here) ----

    @PostMapping("/{id}/roles/org-admin")
    public UserAccount grantOrgAdmin(@PathVariable String id) {
        return users.grantOrgAdmin(id);
    }

    @DeleteMapping("/{id}/roles/org-admin")
    public UserAccount revokeOrgAdmin(@PathVariable String id) {
        return users.revokeOrgAdmin(id);
    }

    @PostMapping("/{id}/roles/teacher")
    public UserAccount grantTeacher(@PathVariable String id) {
        return users.grantTeacher(id);
    }

    @DeleteMapping("/{id}/roles/teacher")
    public UserAccount revokeTeacher(@PathVariable String id) {
        return users.revokeTeacher(id);
    }

    // ---- Delete ----

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        users.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Simple exception-to-HTTP mapping ----

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<Map<String, String>> handleUnsupported(UnsupportedOperationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    // ---- Minimal request DTOs (kept here to avoid extra files) ----
    public record SignUpRequest(String email, String phone, String username) {}
    public record ContactRequest(String contact) {}
    public record VerifyOtpRequest(String contact, String code) {}
    public record UpdateContactRequest(String email, String phone) {}
    public record UpdateUsernameRequest(String username) {}
}

