package app.husna.HusnaMainBackend.controllers.ui;

import app.husna.HusnaMainBackend.event.Event;
import app.husna.HusnaMainBackend.event.EventService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/dashboard/org/events")
public class OrgEventFormController {

    private final EventService events;

    public OrgEventFormController(EventService events) {
        this.events = events;
    }

    @GetMapping("/new")
    public String newEvent(@RequestParam String actorUserId, Model model) {
        EventForm form = new EventForm();
        form.setTimezone("UTC");
        model.addAttribute("actorUserId", actorUserId);
        model.addAttribute("form", form);
        model.addAttribute("mode", "create");
        model.addAttribute("currentPublished", false);
        return "dashboard_org_event_form";
    }

    @PostMapping("/new")
    public String createEvent(@RequestParam String actorUserId,
                              @ModelAttribute("form") EventForm form,
                              @RequestParam(defaultValue = "draft") String action,
                              Model model) {
        try {
            events.createEvent(actorUserId, form.toCreateRequest(action));
            return "redirect:/dashboard/org?actorUserId=" + actorUserId + "&success=Event%20created";
        } catch (Exception ex) {
            model.addAttribute("actorUserId", actorUserId);
            model.addAttribute("mode", "create");
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("currentPublished", false);
            return "dashboard_org_event_form";
        }
    }

    @GetMapping("/{eventId}/edit")
    public String editEvent(@PathVariable String eventId,
                            @RequestParam String actorUserId,
                            Model model) {
        Event event = events.getEvent(eventId);
        EventForm form = EventForm.fromEvent(event);
        model.addAttribute("actorUserId", actorUserId);
        model.addAttribute("eventId", eventId);
        model.addAttribute("form", form);
        model.addAttribute("mode", "edit");
        model.addAttribute("currentPublished", event.isPublished());
        return "dashboard_org_event_form";
    }

    @PostMapping("/{eventId}/edit")
    public String updateEvent(@PathVariable String eventId,
                              @RequestParam String actorUserId,
                              @ModelAttribute("form") EventForm form,
                              @RequestParam(defaultValue = "draft") String action,
                              Model model) {
        try {
            events.updateEvent(actorUserId, eventId, form.toUpdateRequest(action));
            return "redirect:/dashboard/org?actorUserId=" + actorUserId + "&success=Event%20updated";
        } catch (Exception ex) {
            model.addAttribute("actorUserId", actorUserId);
            model.addAttribute("eventId", eventId);
            model.addAttribute("mode", "edit");
            model.addAttribute("error", ex.getMessage());
            Event event = events.getEvent(eventId);
            model.addAttribute("currentPublished", event.isPublished());
            return "dashboard_org_event_form";
        }
    }

    @PostMapping("/{eventId}/delete")
    public String deleteEvent(@PathVariable String eventId,
                              @RequestParam String actorUserId) {
        events.deleteEvent(actorUserId, eventId);
        return "redirect:/dashboard/org?actorUserId=" + actorUserId + "&success=Event%20deleted";
    }

    public static class EventForm {
        private static final DateTimeFormatter FORM_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        private String title;
        private String description;
        private String venueName;
        private String addressLine;
        private String city;
        private String region;
        private String postalCode;
        private String timezone;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private String startDateTime;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private String endDateTime;

        public EventService.CreateEvent toCreateRequest(String action) {
            boolean publish = "publish".equalsIgnoreCase(action);
            return new EventService.CreateEvent(
                    title,
                    description,
                    venueName,
                    addressLine,
                    city,
                    region,
                    postalCode,
                    timezone,
                    toInstant(startDateTime, timezone),
                    toInstant(endDateTime, timezone),
                    publish
            );
        }

        public EventService.UpdateEvent toUpdateRequest(String action) {
            Boolean publish = null;
            if ("publish".equalsIgnoreCase(action)) {
                publish = Boolean.TRUE;
            } else if ("draft".equalsIgnoreCase(action)) {
                publish = Boolean.FALSE;
            }
            return new EventService.UpdateEvent(
                    title,
                    description,
                    venueName,
                    addressLine,
                    city,
                    region,
                    postalCode,
                    timezone,
                    toInstant(startDateTime, timezone),
                    toInstant(endDateTime, timezone),
                    publish
            );
        }

        private static Instant toInstant(String value, String timezone) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("start_end_required");
            }
            ZoneId zone = (timezone == null || timezone.isBlank()) ? ZoneId.of("UTC") : ZoneId.of(timezone);
            LocalDateTime local = LocalDateTime.parse(value, FORM_FORMAT);
            return local.atZone(zone).toInstant();
        }

        public static EventForm fromEvent(Event e) {
            EventForm form = new EventForm();
            form.setTitle(e.getTitle());
            form.setDescription(e.getDescription());
            form.setVenueName(e.getVenueName());
            form.setAddressLine(e.getAddressLine());
            form.setCity(e.getCity());
            form.setRegion(e.getRegion());
            form.setPostalCode(e.getPostalCode());
            String tz = (e.getTimezone() != null && !e.getTimezone().isBlank()) ? e.getTimezone() : "UTC";
            form.setTimezone(tz);
            ZoneId zone = ZoneId.of(tz);
            form.setStartDateTime(FORM_FORMAT.format(LocalDateTime.ofInstant(e.getStartAt(), zone)));
            form.setEndDateTime(FORM_FORMAT.format(LocalDateTime.ofInstant(e.getEndAt(), zone)));
            return form;
        }

        // getters and setters

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getVenueName() { return venueName; }
        public void setVenueName(String venueName) { this.venueName = venueName; }
        public String getAddressLine() { return addressLine; }
        public void setAddressLine(String addressLine) { this.addressLine = addressLine; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        public String getPostalCode() { return postalCode; }
        public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
        public String getTimezone() { return timezone; }
        public void setTimezone(String timezone) { this.timezone = timezone; }
        public String getStartDateTime() { return startDateTime; }
        public void setStartDateTime(String startDateTime) { this.startDateTime = startDateTime; }
        public String getEndDateTime() { return endDateTime; }
        public void setEndDateTime(String endDateTime) { this.endDateTime = endDateTime; }
    }
}
