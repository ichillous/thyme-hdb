package app.husna.HusnaMainBackend.controllers.ui;

import app.husna.HusnaMainBackend.auth.ActorContext;
import app.husna.HusnaMainBackend.constants.StateProvince;
import app.husna.HusnaMainBackend.event.Event;
import app.husna.HusnaMainBackend.event.EventService;
import app.husna.HusnaMainBackend.profile.ProfileService;
import org.springframework.beans.factory.ObjectProvider;
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
    private final ObjectProvider<ActorContext> actorContext;
    private final ProfileService profiles;

    public OrgEventFormController(EventService events, ObjectProvider<ActorContext> actorContext, ProfileService profiles) {
        this.events = events;
        this.actorContext = actorContext;
        this.profiles = profiles;
    }

    @GetMapping("/new")
    public String newEvent(Model model) {
        String actorUserId = actorContext.getObject().requireActorUserId();
        if (!profiles.isOrgProfileComplete(actorUserId)) {
            return "redirect:/dashboard/org/profile/edit?notice=complete_profile";
        }
        EventForm form = new EventForm();
        form.setTimezone("UTC");
        form.setUseDifferentVenue(false);
        model.addAttribute("form", form);
        model.addAttribute("mode", "create");
        model.addAttribute("currentPublished", false);
        model.addAttribute("states", StateProvince.values());
        return "dashboard_org_event_form";
    }

    @PostMapping("/new")
    public String createEvent(@ModelAttribute("form") EventForm form,
                              @RequestParam(defaultValue = "draft") String action,
                              Model model) {
        String actorUserId = actorContext.getObject().requireActorUserId();
        try {
            boolean useDifferentVenue = Boolean.TRUE.equals(form.getUseDifferentVenue());
            if (!useDifferentVenue) {
                var p = profiles.getMine(actorUserId);
                form.setVenueName(null);
                form.setAddressLine(p.getAddressLine1());
                form.setCity(p.getCity());
                form.setStateProvince(p.getStateProvince() != null ? p.getStateProvince().name() : null);
                form.setPostalCode(p.getPostalCode());
            }
            events.createEvent(actorUserId, form.toCreateRequest(action));
            return "redirect:/dashboard/org?success=Event%20created";
        } catch (IllegalStateException ex) {
            if ("profile_incomplete".equals(ex.getMessage())) {
                return "redirect:/dashboard/org/profile/edit?notice=complete_profile";
            }
            model.addAttribute("mode", "create");
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("currentPublished", false);
            model.addAttribute("states", StateProvince.values());
            return "dashboard_org_event_form";
        } catch (Exception ex) {
            model.addAttribute("mode", "create");
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("currentPublished", false);
            model.addAttribute("states", StateProvince.values());
            return "dashboard_org_event_form";
        }
    }

    @GetMapping("/{eventId}/edit")
    public String editEvent(@PathVariable String eventId,
                            Model model) {
        String actorUserId = actorContext.getObject().requireActorUserId();
        Event event = events.getOrgOwnedEvent(actorUserId, eventId);
        EventForm form = EventForm.fromEvent(event);
        model.addAttribute("eventId", eventId);
        model.addAttribute("form", form);
        model.addAttribute("mode", "edit");
        model.addAttribute("currentPublished", event.isPublished());
        model.addAttribute("states", StateProvince.values());
        return "dashboard_org_event_form";
    }

    @PostMapping("/{eventId}/edit")
    public String updateEvent(@PathVariable String eventId,
                              @ModelAttribute("form") EventForm form,
                              @RequestParam(defaultValue = "draft") String action,
                              Model model) {
        String actorUserId = actorContext.getObject().requireActorUserId();
        try {
            boolean useDifferentVenue = Boolean.TRUE.equals(form.getUseDifferentVenue());
            if (!useDifferentVenue) {
                var p = profiles.getMine(actorUserId);
                form.setVenueName(null);
                form.setAddressLine(p.getAddressLine1());
                form.setCity(p.getCity());
                form.setStateProvince(p.getStateProvince() != null ? p.getStateProvince().name() : null);
                form.setPostalCode(p.getPostalCode());
            }
            events.updateEvent(actorUserId, eventId, form.toUpdateRequest(action));
            return "redirect:/dashboard/org?success=Event%20updated";
        } catch (Exception ex) {
            model.addAttribute("eventId", eventId);
            model.addAttribute("mode", "edit");
            model.addAttribute("error", ex.getMessage());
            Event event = events.getOrgOwnedEvent(actorUserId, eventId);
            model.addAttribute("currentPublished", event.isPublished());
            model.addAttribute("states", StateProvince.values());
            return "dashboard_org_event_form";
        }
    }

    @PostMapping("/{eventId}/delete")
    public String deleteEvent(@PathVariable String eventId) {
        String actorUserId = actorContext.getObject().requireActorUserId();
        events.deleteEvent(actorUserId, eventId);
        return "redirect:/dashboard/org?success=Event%20deleted";
    }

    public static class EventForm {
        private static final DateTimeFormatter FORM_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        private String title;
        private String description;
        private String venueName;
        private String addressLine;
        private String city;
        private String stateProvince;
        private String postalCode;
        private String timezone;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private String startDateTime;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        private String endDateTime;
        private Boolean useDifferentVenue;

        public EventService.CreateEvent toCreateRequest(String action) {
            boolean publish = "publish".equalsIgnoreCase(action);
            return new EventService.CreateEvent(
                    title,
                    description,
                    venueName,
                    addressLine,
                    city,
                    stateProvince != null && !stateProvince.isBlank() ? StateProvince.valueOf(stateProvince) : null,
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
                    stateProvince != null && !stateProvince.isBlank() ? StateProvince.valueOf(stateProvince) : null,
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
            form.setStateProvince(e.getStateProvince() != null ? e.getStateProvince().name() : null);
            form.setPostalCode(e.getPostalCode());
            boolean hasOverride = (e.getVenueName() != null && !e.getVenueName().isBlank())
                    || (e.getAddressLine() != null && !e.getAddressLine().isBlank())
                    || (e.getCity() != null && !e.getCity().isBlank())
                    || (e.getStateProvince() != null)
                    || (e.getPostalCode() != null && !e.getPostalCode().isBlank());
            form.setUseDifferentVenue(hasOverride);
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
        public String getStateProvince() { return stateProvince; }
        public void setStateProvince(String stateProvince) { this.stateProvince = stateProvince; }
        public String getPostalCode() { return postalCode; }
        public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
        public String getTimezone() { return timezone; }
        public void setTimezone(String timezone) { this.timezone = timezone; }
        public String getStartDateTime() { return startDateTime; }
        public void setStartDateTime(String startDateTime) { this.startDateTime = startDateTime; }
        public String getEndDateTime() { return endDateTime; }
        public void setEndDateTime(String endDateTime) { this.endDateTime = endDateTime; }
        public Boolean getUseDifferentVenue() { return useDifferentVenue; }
        public void setUseDifferentVenue(Boolean useDifferentVenue) { this.useDifferentVenue = useDifferentVenue; }
    }
}
