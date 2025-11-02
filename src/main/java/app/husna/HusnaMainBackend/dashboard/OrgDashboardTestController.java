package app.husna.HusnaMainBackend.controllers;

import app.husna.HusnaMainBackend.dashboard.OrgDashboardService;
import app.husna.HusnaMainBackend.dashboard.OrgDashboardService.EventRow;
import app.husna.HusnaMainBackend.dashboard.OrgDashboardService.Summary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test/dashboard/org")
public class OrgDashboardTestController {

    private final OrgDashboardService svc;

    public OrgDashboardTestController(OrgDashboardService svc) {
        this.svc = svc;
    }

    // Card-style numbers + quick health flags
    @GetMapping("/summary")
    public Summary summary(@RequestParam String actorUserId) {
        return svc.summary(actorUserId);
    }

    // Tabular list for events with like counts
    @GetMapping("/events")
    public Page<EventRow> events(@RequestParam String actorUserId,
                                 @RequestParam(required = false) String status,         // all|upcoming|past
                                 @RequestParam(required = false) Boolean published,     // true|false|null
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return svc.listEvents(actorUserId, status, published, pageable);
    }
}
