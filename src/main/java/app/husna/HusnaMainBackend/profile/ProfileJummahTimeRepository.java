package app.husna.HusnaMainBackend.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProfileJummahTimeRepository extends JpaRepository<ProfileJummahTime, String> {
    List<ProfileJummahTime> findByProfile_ProfileIdOrderByStartTime(String profileId);
    void deleteByProfile_ProfileId(String profileId);
}
