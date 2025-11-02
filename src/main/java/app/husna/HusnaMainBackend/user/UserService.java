package app.husna.HusnaMainBackend.user;

import app.husna.HusnaMainBackend.constants.RoleName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * UserService implements MVP user management:
 * - Sign up with email or phone (username optional), uniqueness enforced.
 * - Mock OTP login flow (in-memory; not persisted/sent).
 * - Read/find helpers and pagination.
 * - Update contact info and username with normalization + uniqueness checks.
 * - Delete by id (hard delete).
 * - Role management for ORG_ADMIN and TEACHER (SUPER_ADMIN guarded elsewhere).
 *
 * Notes on repository expectations:
 * This service assumes the following methods exist in {@link UserAccountRepository}:
 *   Optional<UserAccount> findByEmail(String email);
 *   Optional<UserAccount> findByPhone(String phone);
 *   Optional<UserAccount> findByUsername(String username);
 *   boolean existsByEmail(String email);
 *   boolean existsByPhone(String phone);
 *   boolean existsByUsername(String username);
 *
 * If your repository uses case-insensitive variants or different signatures (e.g., findByEmailIgnoreCase),
 * add/adjust them in your repository; the service is written against the contract above.
 * // TODO: Ensure repository declares findByUsername(...) and existsByUsername(...)
 * // TODO: Ensure repository declares findByEmail(...) and existsByEmail(...) (non-IgnoreCase or adapt)
 * // TODO: Ensure repository id type is String to match entity (JpaRepository<UserAccount, String>)
 */
@Service
public class UserService {

    private final UserAccountRepository users;

    // In-memory store for mock OTPs: contact -> code
    private final Map<String, String> pendingOtps = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public UserService(UserAccountRepository users) {
        this.users = users;
    }

    // ===========
    // Create (Sign up)
    // ===========

    /**
     * Sign up a GENERAL_USER with email or phone (at least one required). Username is optional.
     * Enforces uniqueness on any provided contact/username. Normalizes all inputs.
     *
     * @param email    email address (nullable)
     * @param phone    phone number (nullable)
     * @param username username (nullable)
     * @return persisted UserAccount with GENERAL_USER role
     * @throws IllegalArgumentException if both email and phone are missing/blank
     * @throws IllegalStateException    if any provided field is already taken
     */
    @Transactional
    public UserAccount signUp(String email, String phone, String username) {
        String normEmail = normalizeEmail(email);
        String normPhone = normalizePhone(phone);
        String normUsername = normalizeUsername(username);

        if (isBlank(normEmail) && isBlank(normPhone)) {
            throw new IllegalArgumentException("contact_required");
        }

        ensureUniqueForCreate(normEmail, normPhone, normUsername);

        UserAccount ua = new UserAccount();
        ua.setEmail(normEmail);
        ua.setPhone(normPhone);
        ua.setUsername(normUsername);

        ensureGeneralUserRole(ua);
        return users.save(ua);
    }

    /**
     * Create a user as ORG_ADMIN (still includes GENERAL_USER).
     * Useful for seeding or admin flows.
     */
    @Transactional
    public UserAccount createOrgAdmin(String email, String phone, String username) {
        UserAccount ua = signUp(email, phone, username);
        addRoleIfMissing(ua, RoleName.ORG_ADMIN);
        ensureNoSuperAdminMutation(Collections.singleton(RoleName.ORG_ADMIN));
        return users.save(ua);
    }

    /**
     * Create a user as TEACHER (still includes GENERAL_USER).
     * Note: Teachers are out of MVP scope; provided for completeness.
     */
    @Transactional
    public UserAccount createTeacher(String email, String phone, String username) {
        UserAccount ua = signUp(email, phone, username);
        addRoleIfMissing(ua, RoleName.TEACHER);
        ensureNoSuperAdminMutation(Collections.singleton(RoleName.TEACHER));
        return users.save(ua);
    }

    // ===========
    // Mock OTP (Login)
    // ===========

    /**
     * Generate and store a 6-digit mock OTP for an existing user identified by contact (email or phone).
     * Returns the generated code (MVP mock behavior).
     *
     * @param contact email or phone (raw input)
     * @return 6-digit OTP string
     * @throws IllegalArgumentException if no user is found by the given contact
     */
    public String requestMockOtp(String contact) {
        ContactLookup lookup = resolveAndLookupContact(contact);
        if (lookup.user.isEmpty()) {
            throw new IllegalArgumentException("user_not_found");
        }
        String code = generateSixDigitCode();
        pendingOtps.put(lookup.normalizedContact, code);
        return code;
    }

    /**
     * Verify a previously generated mock OTP for contact. On success, clears the OTP and returns the user.
     *
     * @param contact email or phone (raw input used in requestMockOtp)
     * @param code    6-digit code
     * @return the matching UserAccount
     * @throws IllegalArgumentException if OTP is invalid or missing
     */
    public UserAccount verifyMockOtp(String contact, String code) {
        ContactLookup lookup = resolveAndLookupContact(contact);
        String key = lookup.normalizedContact;
        String expected = pendingOtps.get(key);
        if (expected == null || !Objects.equals(expected, code)) {
            throw new IllegalArgumentException("invalid_otp");
        }
        pendingOtps.remove(key);

        return lookup.user.orElseThrow(() -> new IllegalArgumentException("user_not_found"));
    }

    // ===========
    // Read / Find
    // ===========

    /**
     * Get a user by id.
     *
     * @throws IllegalArgumentException if not found
     */
    public UserAccount getById(String id) {
        return users.findById(id).orElseThrow(() -> new IllegalArgumentException("user_not_found"));
    }

    /**
     * Find a user by normalized email.
     */
    public Optional<UserAccount> findByEmail(String email) {
        String norm = normalizeEmail(email);
        if (isBlank(norm)) return Optional.empty();
        // TODO: repository must provide findByEmail(String)
        return users.findByEmail(norm);
    }

    /**
     * Find a user by normalized phone.
     */
    public Optional<UserAccount> findByPhone(String phone) {
        String norm = normalizePhone(phone);
        if (isBlank(norm)) return Optional.empty();
        // TODO: repository must provide findByPhone(String)
        return users.findByPhone(norm);
    }

    /**
     * Find a user by trimmed username.
     */
    public Optional<UserAccount> findByUsername(String username) {
        String norm = normalizeUsername(username);
        if (isBlank(norm)) return Optional.empty();
        // TODO: repository must provide findByUsername(String)
        return users.findByUsername(norm);
    }

    /**
     * List users with pagination.
     */
    public Page<UserAccount> list(Pageable pageable) {
        return users.findAll(pageable);
    }

    // ===========
    // Update
    // ===========

    /**
     * Update email and/or phone for a user. Normalizes and enforces uniqueness.
     * Pass null or blank to leave a field unchanged.
     */
    @Transactional
    public UserAccount updateContact(String id, String newEmail, String newPhone) {
        UserAccount ua = getById(id);

        String targetEmail = chooseUpdateValue(ua.getEmail(), normalizeEmail(newEmail));
        String targetPhone = chooseUpdateValue(ua.getPhone(), normalizePhone(newPhone));

        ensureUniqueForUpdate(ua, targetEmail, targetPhone, ua.getUsername());

        ua.setEmail(targetEmail);
        ua.setPhone(targetPhone);
        ensureGeneralUserRole(ua);
        return users.save(ua);
    }

    /**
     * Update username for a user. Normalizes and enforces uniqueness.
     */
    @Transactional
    public UserAccount updateUsername(String id, String newUsername) {
        UserAccount ua = getById(id);
        String targetUsername = chooseUpdateValue(ua.getUsername(), normalizeUsername(newUsername));

        ensureUniqueForUpdate(ua, ua.getEmail(), ua.getPhone(), targetUsername);

        ua.setUsername(targetUsername);
        ensureGeneralUserRole(ua);
        return users.save(ua);
    }

    // ===========
    // Delete
    // ===========

    /**
     * Hard delete a user by id (MVP).
     */
    @Transactional
    public void deleteById(String id) {
        users.deleteById(id);
    }

    // ===========
    // Role management (no SUPER_ADMIN here)
    // ===========

    /**
     * Grant ORG_ADMIN role to a user. GENERAL_USER is always kept.
     */
    @Transactional
    public UserAccount grantOrgAdmin(String id) {
        UserAccount ua = getById(id);
        ensureNoSuperAdminMutation(Collections.singleton(RoleName.ORG_ADMIN));
        addRoleIfMissing(ua, RoleName.ORG_ADMIN);
        ensureGeneralUserRole(ua);
        return users.save(ua);
    }

    /**
     * Revoke ORG_ADMIN role from a user. GENERAL_USER is always kept.
     */
    @Transactional
    public UserAccount revokeOrgAdmin(String id) {
        UserAccount ua = getById(id);
        ua.getRoles().remove(RoleName.ORG_ADMIN);
        ensureGeneralUserRole(ua);
        return users.save(ua);
    }

    /**
     * Grant TEACHER role to a user.
     * Note: Teachers/classes are out of MVP scope; method provided for completeness.
     */
    @Transactional
    public UserAccount grantTeacher(String id) {
        UserAccount ua = getById(id);
        ensureNoSuperAdminMutation(Collections.singleton(RoleName.TEACHER));
        addRoleIfMissing(ua, RoleName.TEACHER);
        ensureGeneralUserRole(ua);
        return users.save(ua);
    }

    /**
     * Revoke TEACHER role from a user.
     * Note: Teachers/classes are out of MVP scope; method provided for completeness.
     */
    @Transactional
    public UserAccount revokeTeacher(String id) {
        UserAccount ua = getById(id);
        ua.getRoles().remove(RoleName.TEACHER);
        ensureGeneralUserRole(ua);
        return users.save(ua);
    }

    // ===========
    // Helpers
    // ===========

    private void ensureUniqueForCreate(String email, String phone, String username) {
        if (!isBlank(email) && users.existsByEmail(email)) { // TODO: ensure existsByEmail(String) exists
            throw new IllegalStateException("email_taken");
        }
        if (!isBlank(phone) && users.existsByPhone(phone)) { // TODO: ensure existsByPhone(String) exists
            throw new IllegalStateException("phone_taken");
        }
        if (!isBlank(username) && users.existsByUsername(username)) { // TODO: ensure existsByUsername(String) exists
            throw new IllegalStateException("username_taken");
        }
    }

    private void ensureUniqueForUpdate(UserAccount current,
                                       String email,
                                       String phone,
                                       String username) {
        if (!isBlank(email) && !email.equals(current.getEmail()) && users.existsByEmail(email)) {
            throw new IllegalStateException("email_taken");
        }
        if (!isBlank(phone) && !phone.equals(current.getPhone()) && users.existsByPhone(phone)) {
            throw new IllegalStateException("phone_taken");
        }
        if (!isBlank(username) && !username.equals(current.getUsername()) && users.existsByUsername(username)) {
            throw new IllegalStateException("username_taken");
        }
    }

    private void ensureGeneralUserRole(UserAccount ua) {
        if (ua.getRoles() == null) {
            ua.setRoles(new HashSet<>());
        }
        ua.getRoles().add(RoleName.GENERAL_USER);
    }

    private void addRoleIfMissing(UserAccount ua, RoleName role) {
        if (role == RoleName.SUPER_ADMIN) {
            throw new UnsupportedOperationException("super_admin_managed_elsewhere");
        }
        if (ua.getRoles() == null) {
            ua.setRoles(new HashSet<>());
        }
        ua.getRoles().add(role);
    }

    private void ensureNoSuperAdminMutation(Collection<RoleName> roles) {
        if (roles != null && roles.contains(RoleName.SUPER_ADMIN)) {
            throw new UnsupportedOperationException("super_admin_managed_elsewhere");
        }
    }

    private String normalizeEmail(String email) {
        if (email == null) return null;
        String trimmed = email.trim().toLowerCase(Locale.ROOT);
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizePhone(String phone) {
        if (phone == null) return null;
        String digitsOnly = phone.chars()
                .filter(Character::isDigit)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString()
                .trim();
        return digitsOnly.isEmpty() ? null : digitsOnly;
    }

    private String normalizeUsername(String username) {
        if (username == null) return null;
        String trimmed = username.trim(); // preserving case by default
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private String chooseUpdateValue(String existing, String proposed) {
        // If proposed is null or blank, keep existing; otherwise use proposed (may set to null if intended via blank-normalization).
        return proposed == null ? existing : proposed;
    }

    private String generateSixDigitCode() {
        int code = random.nextInt(1_000_000);
        return String.format("%06d", code);
    }

    private ContactLookup resolveAndLookupContact(String rawContact) {
        if (rawContact == null || rawContact.isBlank()) {
            return new ContactLookup("", Optional.empty());
        }
        String contact = rawContact.trim();
        boolean looksLikeEmail = contact.contains("@");

        if (looksLikeEmail) {
            String normEmail = normalizeEmail(contact);
            // TODO: repository must provide findByEmail(String)
            Optional<UserAccount> u = (normEmail == null) ? Optional.empty() : users.findByEmail(normEmail);
            return new ContactLookup(normEmail == null ? "" : normEmail, u);
        } else {
            String normPhone = normalizePhone(contact);
            // TODO: repository must provide findByPhone(String)
            Optional<UserAccount> u = (normPhone == null) ? Optional.empty() : users.findByPhone(normPhone);
            return new ContactLookup(normPhone == null ? "" : normPhone, u);
        }
    }

    private record ContactLookup(String normalizedContact, Optional<UserAccount> user) {}
}
