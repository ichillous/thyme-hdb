package app.husna.HusnaMainBackend.controllers.ui;

import app.husna.HusnaMainBackend.dashboard.OrgDashboardService;
import app.husna.HusnaMainBackend.dashboard.OrgDashboardService.EventRow;
import app.husna.HusnaMainBackend.dashboard.OrgDashboardService.Summary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/dashboard/org")
public class OrgDashboardPageController {

    private final OrgDashboardService svc;

    public OrgDashboardPageController(OrgDashboardService svc) { this.svc = svc; }

    // Shell page (HTMX will load fragments)
    @GetMapping
    public String dashboard(@RequestParam String actorUserId,
                            @RequestParam(required = false) String success,
                            Model model) {
        Summary summary = svc.summary(actorUserId);
        Page<EventRow> rows = svc.listEvents(actorUserId, null, null, PageRequest.of(0, 20));
        model.addAttribute("actorUserId", actorUserId);
        model.addAttribute("summary", summary);
        model.addAttribute("rows", rows);
        model.addAttribute("status", null);
        model.addAttribute("published", null);
        model.addAttribute("success", success);
        return "dashboard_org_page";
    }

    // HTMX partial: summary cards
    @GetMapping("/summary")
    public String summary(@RequestParam String actorUserId, Model model) {
        Summary s = svc.summary(actorUserId);
        model.addAttribute("s", s);
        return "fragments/dashboard_org :: summary";
    }

    // HTMX partial: events table (filters & pagination)
    @GetMapping("/events")
    public String events(@RequestParam String actorUserId,
                         @RequestParam(required = false) String status,        // all|upcoming|past
                         @RequestParam(required = false) Boolean published,    // true|false
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "20") int size,
                         Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<EventRow> rows = svc.listEvents(actorUserId, status, published, pageable);
        model.addAttribute("actorUserId", actorUserId);
        model.addAttribute("rows", rows);
        model.addAttribute("status", status);
        model.addAttribute("published", published);
        return "fragments/dashboard_org :: events";
    }
}
