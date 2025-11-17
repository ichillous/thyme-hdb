package app.husna.HusnaMainBackend.controllers.ui;

import app.husna.HusnaMainBackend.auth.ActorContext;
import app.husna.HusnaMainBackend.constants.PrayerTimesMode;
import app.husna.HusnaMainBackend.profile.Profile;
import app.husna.HusnaMainBackend.profile.ProfileJummahTime;
import app.husna.HusnaMainBackend.profile.ProfilePrayerTime;
import app.husna.HusnaMainBackend.profile.ProfileService;
import app.husna.HusnaMainBackend.profile.ProfileService.ReplacePrayerSchedule;
import org.springframework.beans.factory.ObjectProvider;
import app.husna.HusnaMainBackend.constants.OrgType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/dashboard/org/prayer-schedule")
public class OrgPrayerScheduleController {

    private final ProfileService profiles;
    private final ObjectProvider<ActorContext> actorContext;

    public OrgPrayerScheduleController(ProfileService profiles, ObjectProvider<ActorContext> actorContext) {
        this.profiles = profiles;
        this.actorContext = actorContext;
    }

    private static final List<Integer> DAY_ORDER = List.of(6, 7, 1, 2, 3, 4, 5); // Sat → Fri (ISO 8601 values)
    private static final String ALL_DAYS_LABEL = "All days (Sat-Fri)";

    private static List<ProfilePrayerTime> orderWeek(List<ProfilePrayerTime> week) {
        if (week == null || week.isEmpty()) {
            return List.of();
        }
        Map<Integer, ProfilePrayerTime> byDay = week.stream()
                .collect(Collectors.toMap(ProfilePrayerTime::getDayOfWeek, Function.identity()));
        List<ProfilePrayerTime> ordered = new ArrayList<>();
        for (int day : DAY_ORDER) {
            ProfilePrayerTime ptr = byDay.get(day);
            if (ptr != null) {
                ordered.add(ptr);
            }
        }
        return ordered;
    }

    private boolean ensureMosque(Profile profile) {
        return profile != null && profile.getOrgType() == OrgType.MOSQUE;
    }

    @GetMapping
    public String edit(Model model) {
        String actorUserId = actorContext.getObject().requireActorUserId();
        Profile profile = profiles.getMine(actorUserId);
        if (!ensureMosque(profile)) {
            return "redirect:/dashboard/org?error=Prayer%20schedule%20available%20for%20mosques%20only";
        }
        List<ProfilePrayerTime> week = orderWeek(profiles.listPrayerTimes(actorUserId));
        List<ProfileJummahTime> jummah = profiles.listJummahTimes(actorUserId);
        PrayerScheduleForm form = PrayerScheduleForm.from(profile, week, jummah);
        model.addAttribute("form", form);
        return "dashboard_org_prayer_form";
    }

    @PostMapping
    public String save(@ModelAttribute("form") PrayerScheduleForm form,
                       Model model) {
        String actorUserId = actorContext.getObject().requireActorUserId();
        try {
            Profile profile = profiles.getMine(actorUserId);
            if (!ensureMosque(profile)) {
                return "redirect:/dashboard/org?error=Prayer%20schedule%20available%20for%20mosques%20only";
            }
            profiles.updateMine(actorUserId, new ProfileService.UpdateProfile(
                    null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null,
                    null, null, null, PrayerTimesMode.MANUAL_WEEKLY,
                    null, null, null
            ));
            profiles.replacePrayerSchedule(actorUserId, form.toReplaceRequest());
            return "redirect:/dashboard/org?success=Prayer%20schedule%20updated";
        } catch (Exception ex) {
            form.ensureWeekRow();
            model.addAttribute("error", ex.getMessage());
            return "dashboard_org_prayer_form";
        }
    }

    public static class PrayerScheduleForm {
        private List<DayRow> week = new ArrayList<>();
        private List<JummahRow> jummah = new ArrayList<>();

        public ReplacePrayerSchedule toReplaceRequest() {
            DayRow weekRow = ensureWeekRow();
            List<ReplacePrayerSchedule.Day> days = new ArrayList<>();
            if (weekRow.hasTimes()) {
                LocalTime fajr = parse(weekRow.getFajr());
                LocalTime dhuhr = parse(weekRow.getDhuhr());
                LocalTime asr = parse(weekRow.getAsr());
                LocalTime maghrib = parse(weekRow.getMaghrib());
                LocalTime isha = parse(weekRow.getIsha());
                for (int day : DAY_ORDER) {
                    days.add(new ReplacePrayerSchedule.Day(day, fajr, dhuhr, asr, maghrib, isha));
                }
            }
            List<ReplacePrayerSchedule.Jummah> js = new ArrayList<>();
            for (JummahRow slot : jummah) {
                if (slot.getStartTime() != null && !slot.getStartTime().isBlank()) {
                    js.add(new ReplacePrayerSchedule.Jummah(parse(slot.getStartTime()), slot.getNotes()));
                }
            }
            return new ReplacePrayerSchedule(days, js);
        }

        private static LocalTime parse(String value) {
            return (value == null || value.isBlank()) ? null : LocalTime.parse(value);
        }

        public static PrayerScheduleForm from(Profile profile,
                                              List<ProfilePrayerTime> week,
                                              List<ProfileJummahTime> jummah) {
            PrayerScheduleForm form = new PrayerScheduleForm();
            ProfilePrayerTime ptr = firstWithTimes(week);
            DayRow weekRow = form.ensureWeekRow();
            if (ptr != null) {
                weekRow.setFajr(toString(ptr.getFajr()));
                weekRow.setDhuhr(toString(ptr.getDhuhr()));
                weekRow.setAsr(toString(ptr.getAsr()));
                weekRow.setMaghrib(toString(ptr.getMaghrib()));
                weekRow.setIsha(toString(ptr.getIsha()));
            }

            List<ProfileJummahTime> safeJummah = (jummah != null) ? jummah : List.of();
            for (int i = 0; i < Math.max(3, safeJummah.size()); i++) {
                JummahRow slot = new JummahRow();
                if (i < safeJummah.size()) {
                    ProfileJummahTime jt = safeJummah.get(i);
                    slot.setStartTime(toString(jt.getStartTime()));
                    slot.setNotes(jt.getNotes());
                }
                form.getJummah().add(slot);
            }
            return form;
        }

        public DayRow ensureWeekRow() {
            return ensureWeekRowInternal();
        }

        private DayRow ensureWeekRowInternal() {
            if (week == null) {
                week = new ArrayList<>();
            }
            if (week.isEmpty()) {
                DayRow row = new DayRow();
                row.setLabel(ALL_DAYS_LABEL);
                week.add(row);
            } else {
                week.get(0).setLabel(ALL_DAYS_LABEL);
            }
            return week.get(0);
        }

        private static ProfilePrayerTime firstWithTimes(List<ProfilePrayerTime> week) {
            if (week == null || week.isEmpty()) {
                return null;
            }
            return week.stream()
                    .filter(PrayerScheduleForm::hasAnyTimes)
                    .findFirst()
                    .orElse(week.get(0));
        }

        private static boolean hasAnyTimes(ProfilePrayerTime time) {
            if (time == null) return false;
            return time.getFajr() != null || time.getDhuhr() != null || time.getAsr() != null
                    || time.getMaghrib() != null || time.getIsha() != null;
        }

        private static String toString(LocalTime time) {
            return time == null ? null : time.toString();
        }

        // getters and setters

        public List<DayRow> getWeek() { return week; }
        public void setWeek(List<DayRow> week) { this.week = week; }
        public List<JummahRow> getJummah() { return jummah; }
        public void setJummah(List<JummahRow> jummah) { this.jummah = jummah; }

        public static class DayRow {
            private String label;
            private String fajr;
            private String dhuhr;
            private String asr;
            private String maghrib;
            private String isha;

            public boolean hasTimes() {
                return streamNotBlank(fajr, dhuhr, asr, maghrib, isha);
            }

            private boolean streamNotBlank(String... values) {
                for (String v : values) {
                    if (v != null && !v.isBlank()) {
                        return true;
                    }
                }
                return false;
            }

            public String getLabel() { return label; }
            public void setLabel(String label) { this.label = label; }
            public String getFajr() { return fajr; }
            public void setFajr(String fajr) { this.fajr = fajr; }
            public String getDhuhr() { return dhuhr; }
            public void setDhuhr(String dhuhr) { this.dhuhr = dhuhr; }
            public String getAsr() { return asr; }
            public void setAsr(String asr) { this.asr = asr; }
            public String getMaghrib() { return maghrib; }
            public void setMaghrib(String maghrib) { this.maghrib = maghrib; }
            public String getIsha() { return isha; }
            public void setIsha(String isha) { this.isha = isha; }
        }

        public static class JummahRow {
            private String startTime;
            private String notes;

            public String getStartTime() { return startTime; }
            public void setStartTime(String startTime) { this.startTime = startTime; }
            public String getNotes() { return notes; }
            public void setNotes(String notes) { this.notes = notes; }
        }
    }
}
