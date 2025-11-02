package app.husna.HusnaMainBackend.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProfilePrayerTimeRepository extends JpaRepository<ProfilePrayerTime, String> {
    List<ProfilePrayerTime> findByProfile_ProfileIdOrderByDayOfWeek(String profileId);
    void deleteByProfile_ProfileId(String profileId);
}
