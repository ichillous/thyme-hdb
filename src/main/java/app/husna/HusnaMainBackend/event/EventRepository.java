package app.husna.HusnaMainBackend.event;

import app.husna.HusnaMainBackend.constants.StateProvince;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
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
    Page<Event> findByPublishedTrueAndCityIgnoreCaseAndStateProvince(String city, StateProvince stateProvince, Pageable pageable);
    Page<Event> findByPublishedTrueAndCityIgnoreCaseAndStateProvinceAndStartAtAfter(String city, StateProvince stateProvince, Instant after, Pageable pageable);

    // --- Home page: list cities that currently have at least one published event ---
    interface CityAggregate {
        String getCity();
        StateProvince getStateProvince();
        long getCount();
    }

    @Query("""
      select e.city as city, e.stateProvince as stateProvince, count(e) as count
      from Event e
      where e.published = true
        and e.city is not null and e.city <> ''
        and e.startAt >= :now
      group by e.city, e.stateProvince
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
      select e.eventId as eventId, count(u) as likeCount
      from Event e
      left join e.likedBy u
      where e.eventId in :eventIds
      group by e.eventId
    """)
    List<EventLikeCount> countLikesByEventIds(@Param("eventIds") Collection<String> eventIds);

    @Query("""
      select e.eventId
      from Event e
      join e.likedBy u
      where u.userId = :userId and e.eventId in :eventIds
    """)
    List<String> findLikedEventIds(@Param("userId") String userId, @Param("eventIds") Collection<String> eventIds);

    @Query("""
       select count(u) from Event e
       left join e.likedBy u
       where e.eventId = :eventId
    """)
    long countLikes(@Param("eventId") String eventId);
}
