package app.husna.HusnaMainBackend.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Services {
    // services that orgs have + description
    PRAYER_TIMES("Prayer Times", "Daily prayer times, schedules & notifications"),
    LIVE_STREAMS("Live Streams", "Live khutbahs, lectures, and events"),
    CLASS_SCHEDULING("Class Scheduling", "Online and in-person class creation & enrollment"),
    DONATION_MANAGEMENT("Donation Management", "One-time and recurring donations"),
    ZAKAT_COLLECTION("Zakat Collection", "Special zakat campaigns and distributions"),
    EVENT_MANAGEMENT("Event Management", "Fundraisers, community gatherings, and special events"),
    MARRIAGE_SERVICES("Marriage Services", "Nikah arrangements, marriage counseling"),
    FUNERAL_SERVICES("Funeral Services", "Janazah prayer coordination and funeral support"),
    COUNSELING_SERVICES("Counseling Services", "Religious, marital, and youth counseling"),
    COMMUNITY_OUTREACH("Community Outreach", "Volunteering opportunities and charity distribution"),
    ISLAMIC_LIBRARY("Islamic Library", "Digital and physical Islamic books & resources"),
    RELIGIOUS_EDUCATION("Religious Education", "Quran, Tajweed, Hadith, and Fiqh classes"),
    YOUTH_PROGRAMS("Youth Programs", "Activities and mentorship for youth"),
    WOMENS_PROGRAMS("Women's Programs", "Women-focused events, support, and education"),
    NEW_MUSLIM_SUPPORT("New Muslim Support", "Mentorship and guidance for reverts"),
    HAJJ_UMRAH_ASSISTANCE("Hajj & Umrah Assistance", "Group trips and planning assistance"),
    CHARITY_PROJECTS("Charity Projects", "Community aid, food banks, and charitable campaigns"),
    ANALYTICS_DASHBOARD("Analytics Dashboard", "Track engagement, donations, and performance metrics");

    private final String displayName;
    private final String description;

}
