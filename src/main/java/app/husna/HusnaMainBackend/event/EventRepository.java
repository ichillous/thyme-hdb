package app.husna.HusnaMainBackend.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, String>, JpaSpecificationExecutor<Event> {

    Page<Event> findByOwnerOrgId(String ownerOrgId, Pageable pageable);
    Page<Event> findByOwnerOrgIdAndPublishedTrue(String ownerOrgId, Pageable pageable);
    Page<Event> findByOwnerOrgIdAndPublishedTrueAndStartAtAfter(String ownerOrgId, Instant after, Pageable pageable);
    Page<Event> findByOwnerOrgIdAndPublishedTrueAndStartAtBefore(String ownerOrgId, Instant before, Pageable pageable);
    List<Event> findAllByOwnerOrgId(String ownerOrgId);
    Page<Event> findByLikedBy_UserId(String userId, Pageable pageable);
    Page<Event> findByLikedBy_UserIdAndStartAtAfter(String userId, Instant after, Pageable pageable);
    Page<Event> findByLikedBy_UserIdAndStartAtBefore(String userId, Instant before, Pageable pageable);

    Page<Event> findByPublishedTrue(Pageable pageable);
    Page<Event> findByPublishedTrueAndStartAtAfter(Instant after, Pageable pageable);

    Page<Event> findByPublishedTrueAndCityIgnoreCase(String city, Pageable pageable);
    Page<Event> findByPublishedTrueAndCityIgnoreCaseAndStartAtAfter(String city, Instant after, Pageable pageable);
    Page<Event> findByPublishedTrueAndCityIgnoreCaseAndRegionIgnoreCase(String city, String region, Pageable pageable);
    Page<Event> findByPublishedTrueAndCityIgnoreCaseAndRegionIgnoreCaseAndStartAtAfter(String city, String region, Instant after, Pageable pageable);

    // --- Home page: list cities that currently have at least one published event ---
    interface CityAggregate {
        String getCity();
        String getRegion();
        long getCount();
    }

    @Query("""
      select e.city as city, e.region as region, count(e) as count
      from Event e
      where e.published = true
        and e.city is not null and e.city <> ''
        and e.startAt >= :now
      group by e.city, e.region
      order by lower(e.city)
    """)
    List<CityAggregate> listActiveCities(@Param("now") Instant now);

    // --- Dashboard: like counts by event for this org ---
    interface EventLikeCount {
        String getEventId();
        long getLikeCount();
    }

    @Query("""
      select e.eventId as eventId, count(u) as likeCount
      from Event e
      left join e.likedBy u
      where e.ownerOrgId = :orgId
      group by e.eventId
    """)
    List<EventLikeCount> countLikesByOrgEvents(@Param("orgId") String orgId);

    @Query("""
       select count(u) from Event e
       left join e.likedBy u
       where e.eventId = :eventId
    """)
    long countLikes(@Param("eventId") String eventId);
}
