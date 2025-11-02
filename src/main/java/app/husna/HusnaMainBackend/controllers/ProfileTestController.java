package app.husna.HusnaMainBackend.controllers;

import app.husna.HusnaMainBackend.profile.Profile;
import app.husna.HusnaMainBackend.profile.ProfileService;
import app.husna.HusnaMainBackend.profile.ProfileService.OrgProfileView;
import app.husna.HusnaMainBackend.profile.ProfileService.ReplacePrayerSchedule;
import app.husna.HusnaMainBackend.profile.ProfileService.UpdateProfile;
import app.husna.HusnaMainBackend.profile.ProfileService.UserPrivateProfileView;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/test/profiles")
public class ProfileTestController {

    private final ProfileService profiles;

    public ProfileTestController(ProfileService profiles) {
        this.profiles = profiles;
    }

    // ---- My profile (private) ----

    @GetMapping("/me")
    public Profile me(@RequestParam String actorUserId) {
        return profiles.getMine(actorUserId);
    }

    @PatchMapping("/me")
    public Profile updateMe(@RequestParam String actorUserId,
                            @RequestBody UpdateProfile req) {
        return profiles.updateMine(actorUserId, req);
    }

    @GetMapping("/me/saved")
    public UserPrivateProfileView mySaved(@RequestParam String actorUserId,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return profiles.getUserPrivate(actorUserId, pageable);
    }

    // ---- Org public profile ----

    @GetMapping("/org/{orgUserId}")
    public OrgProfileView orgPublic(@PathVariable String orgUserId,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return profiles.getOrgPublic(orgUserId, pageable);
    }

    // ---- Prayer schedule replace (org only) ----

    @PutMapping("/me/prayer-schedule")
    public ResponseEntity<Map<String, String>> replacePrayerSchedule(@RequestParam String actorUserId,
                                                                     @RequestBody ReplacePrayerSchedule req) {
        profiles.replacePrayerSchedule(actorUserId, req);
        return ResponseEntity.status(HttpStatus.OK).body(Map.of("status", "ok"));
    }

    // ---- Errors ----

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badReq(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> conflict(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<Map<String, String>> forbidden(UnsupportedOperationException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
    }
}
