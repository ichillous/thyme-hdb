package app.husna.HusnaMainBackend.controllers.ui;

import app.husna.HusnaMainBackend.constants.PrayerTimesMode;
import app.husna.HusnaMainBackend.profile.Profile;
import app.husna.HusnaMainBackend.profile.ProfileJummahTime;
import app.husna.HusnaMainBackend.profile.ProfilePrayerTime;
import app.husna.HusnaMainBackend.profile.ProfileService;
import app.husna.HusnaMainBackend.profile.ProfileService.ReplacePrayerSchedule;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/dashboard/org/prayer-schedule")
public class OrgPrayerScheduleController {

    private final ProfileService profiles;

    public OrgPrayerScheduleController(ProfileService profiles) {
        this.profiles = profiles;
    }

    @GetMapping
    public String edit(@RequestParam String actorUserId, Model model) {
        Profile profile = profiles.getMine(actorUserId);
        List<ProfilePrayerTime> week = profiles.listPrayerTimes(actorUserId);
        List<ProfileJummahTime> jummah = profiles.listJummahTimes(actorUserId);
        PrayerScheduleForm form = PrayerScheduleForm.from(profile, week, jummah);
        model.addAttribute("actorUserId", actorUserId);
        model.addAttribute("form", form);
        model.addAttribute("modes", PrayerTimesMode.values());
        return "dashboard_org_prayer_form";
    }

    @PostMapping
    public String save(@RequestParam String actorUserId,
                       @ModelAttribute("form") PrayerScheduleForm form,
                       Model model) {
        try {
            PrayerTimesMode mode = PrayerTimesMode.valueOf(form.getMode());
            profiles.updateMine(actorUserId, form.toUpdateProfile(mode));
            if (mode == PrayerTimesMode.MANUAL_WEEKLY) {
                profiles.replacePrayerSchedule(actorUserId, form.toReplaceRequest());
            }
            return "redirect:/dashboard/org?actorUserId=" + actorUserId + "&success=Prayer%20schedule%20updated";
        } catch (Exception ex) {
            model.addAttribute("actorUserId", actorUserId);
            model.addAttribute("modes", PrayerTimesMode.values());
            model.addAttribute("error", ex.getMessage());
            return "dashboard_org_prayer_form";
        }
    }

    public static class PrayerScheduleForm {
        private String mode;
        private List<DayRow> week = new ArrayList<>();
        private List<JummahRow> jummah = new ArrayList<>();

        public ReplacePrayerSchedule toReplaceRequest() {
            List<ReplacePrayerSchedule.Day> days = new ArrayList<>();
            for (DayRow row : week) {
                if (row.hasTimes()) {
                    days.add(new ReplacePrayerSchedule.Day(
                            row.getDayOfWeek(),
                            parse(row.getFajr()),
                            parse(row.getDhuhr()),
                            parse(row.getAsr()),
                            parse(row.getMaghrib()),
                            parse(row.getIsha())
                    ));
                }
            }
            List<ReplacePrayerSchedule.Jummah> js = new ArrayList<>();
            for (JummahRow row : jummah) {
                if (row.getStartTime() != null && !row.getStartTime().isBlank()) {
                    js.add(new ReplacePrayerSchedule.Jummah(parse(row.getStartTime()), row.getNotes()));
                }
            }
            return new ReplacePrayerSchedule(days, js);
        }

        public ProfileService.UpdateProfile toUpdateProfile(PrayerTimesMode mode) {
            return new ProfileService.UpdateProfile(
                    null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null,
                    null, mode, null, null, null
            );
        }

        private static LocalTime parse(String value) {
            return (value == null || value.isBlank()) ? null : LocalTime.parse(value);
        }

        public static PrayerScheduleForm from(Profile profile,
                                              List<ProfilePrayerTime> week,
                                              List<ProfileJummahTime> jummah) {
            PrayerScheduleForm form = new PrayerScheduleForm();
            form.setMode(profile.getPrayerTimesMode().name());
            for (int i = 1; i <= 7; i++) {
                final int day = i;
                ProfilePrayerTime ptr = week.stream().filter(p -> p.getDayOfWeek() == day).findFirst().orElse(null);
                DayRow row = new DayRow();
                row.setDayOfWeek(i);
                if (ptr != null) {
                    row.setFajr(toString(ptr.getFajr()));
                    row.setDhuhr(toString(ptr.getDhuhr()));
                    row.setAsr(toString(ptr.getAsr()));
                    row.setMaghrib(toString(ptr.getMaghrib()));
                    row.setIsha(toString(ptr.getIsha()));
                }
                form.getWeek().add(row);
            }
            for (int i = 0; i < Math.max(3, jummah.size()); i++) {
                JummahRow row = new JummahRow();
                if (i < jummah.size()) {
                    ProfileJummahTime jt = jummah.get(i);
                    row.setStartTime(toString(jt.getStartTime()));
                    row.setNotes(jt.getNotes());
                }
                form.getJummah().add(row);
            }
            return form;
        }

        private static String toString(LocalTime time) {
            return time == null ? null : time.toString();
        }

        // getters and setters

        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
        public List<DayRow> getWeek() { return week; }
        public void setWeek(List<DayRow> week) { this.week = week; }
        public List<JummahRow> getJummah() { return jummah; }
        public void setJummah(List<JummahRow> jummah) { this.jummah = jummah; }

        public static class DayRow {
            private int dayOfWeek;
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

            public int getDayOfWeek() { return dayOfWeek; }
            public void setDayOfWeek(int dayOfWeek) { this.dayOfWeek = dayOfWeek; }
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
