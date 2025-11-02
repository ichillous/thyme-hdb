package app.husna.HusnaMainBackend.constants;

public enum PrayerTimesMode {
    DISABLED,        // default for non-mosques
    MANUAL_WEEKLY,   // org keeps weekly schedule up to date
    API_CALCULATED   // future: auto-calc via API; still requires periodic verification
}
