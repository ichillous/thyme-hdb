package app.husna.HusnaMainBackend.controllers.ui;

import app.husna.HusnaMainBackend.constants.OrgType;
import app.husna.HusnaMainBackend.constants.Services;
import app.husna.HusnaMainBackend.profile.Profile;
import app.husna.HusnaMainBackend.profile.ProfileService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/dashboard/org/profile")
public class OrgProfileFormController {

    private final ProfileService profiles;

    public OrgProfileFormController(ProfileService profiles) {
        this.profiles = profiles;
    }

    @GetMapping("/edit")
    public String editProfile(@RequestParam String actorUserId, Model model) {
        Profile profile = profiles.getMine(actorUserId);
        ProfileForm form = ProfileForm.fromProfile(profile);
        model.addAttribute("actorUserId", actorUserId);
        model.addAttribute("form", form);
        model.addAttribute("orgTypes", OrgType.values());
        model.addAttribute("services", Services.values());
        return "dashboard_org_profile_form";
    }

    @PostMapping("/edit")
    public String updateProfile(@RequestParam String actorUserId,
                                @ModelAttribute("form") ProfileForm form,
                                Model model) {
        try {
            profiles.updateMine(actorUserId, form.toUpdateRequest());
            return "redirect:/dashboard/org?actorUserId=" + actorUserId + "&success=Profile%20updated";
        } catch (Exception ex) {
            model.addAttribute("actorUserId", actorUserId);
            model.addAttribute("orgTypes", OrgType.values());
            model.addAttribute("services", Services.values());
            model.addAttribute("error", ex.getMessage());
            return "dashboard_org_profile_form";
        }
    }

    public static class ProfileForm {
        private String displayName;
        private String bio;
        private String website;
        private String contactEmail;
        private String contactPhone;
        private String logoUrl;
        private String bannerUrl;
        private String street;
        private String city;
        private String region;
        private String postalCode;
        private String orgType;
        private Set<String> services;
        private String programsOffered;
        private String classesOffered;
        private boolean donationEnabled;
        private String donationProvider;
        private String donationUrl;

        public ProfileService.UpdateProfile toUpdateRequest() {
            Set<Services> serviceEnums = services == null ? null : services.stream()
                    .map(Services::valueOf)
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(Services.class)));
            OrgType type = null;
            if (orgType != null && !orgType.isBlank()) {
                type = OrgType.valueOf(orgType);
            }
            return new ProfileService.UpdateProfile(
                    displayName,
                    bio,
                    website,
                    contactEmail,
                    contactPhone,
                    logoUrl,
                    bannerUrl,
                    street,
                    city,
                    region,
                    postalCode,
                    type,
                    serviceEnums,
                    programsOffered,
                    classesOffered,
                    null,
                    donationEnabled,
                    donationProvider,
                    donationUrl
            );
        }

        public static ProfileForm fromProfile(Profile profile) {
            ProfileForm form = new ProfileForm();
            form.setDisplayName(profile.getDisplayName());
            form.setBio(profile.getBio());
            form.setWebsite(profile.getWebsite());
            form.setContactEmail(profile.getContactEmail());
            form.setContactPhone(profile.getContactPhone());
            form.setLogoUrl(profile.getLogoUrl());
            form.setBannerUrl(profile.getBannerUrl());
            form.setStreet(profile.getStreet());
            form.setCity(profile.getCity());
            form.setRegion(profile.getRegion());
            form.setPostalCode(profile.getPostalCode());
            form.setOrgType(profile.getOrgType() != null ? profile.getOrgType().name() : OrgType.OTHER.name());
            if (profile.getServices() != null) {
                form.setServices(profile.getServices().stream().map(Services::name).collect(Collectors.toSet()));
            }
            form.setProgramsOffered(profile.getProgramsOffered());
            form.setClassesOffered(profile.getClassesOffered());
            form.setDonationEnabled(profile.isDonationEnabled());
            form.setDonationProvider(profile.getDonationProvider());
            form.setDonationUrl(profile.getDonationUrl());
            return form;
        }

        // getters and setters

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getBio() { return bio; }
        public void setBio(String bio) { this.bio = bio; }
        public String getWebsite() { return website; }
        public void setWebsite(String website) { this.website = website; }
        public String getContactEmail() { return contactEmail; }
        public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
        public String getContactPhone() { return contactPhone; }
        public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
        public String getLogoUrl() { return logoUrl; }
        public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
        public String getBannerUrl() { return bannerUrl; }
        public void setBannerUrl(String bannerUrl) { this.bannerUrl = bannerUrl; }
        public String getStreet() { return street; }
        public void setStreet(String street) { this.street = street; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        public String getPostalCode() { return postalCode; }
        public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
        public String getOrgType() { return orgType; }
        public void setOrgType(String orgType) { this.orgType = orgType; }
        public Set<String> getServices() { return services; }
        public void setServices(Set<String> services) { this.services = services; }
        public String getProgramsOffered() { return programsOffered; }
        public void setProgramsOffered(String programsOffered) { this.programsOffered = programsOffered; }
        public String getClassesOffered() { return classesOffered; }
        public void setClassesOffered(String classesOffered) { this.classesOffered = classesOffered; }
        public boolean isDonationEnabled() { return donationEnabled; }
        public void setDonationEnabled(boolean donationEnabled) { this.donationEnabled = donationEnabled; }
        public String getDonationProvider() { return donationProvider; }
        public void setDonationProvider(String donationProvider) { this.donationProvider = donationProvider; }
        public String getDonationUrl() { return donationUrl; }
        public void setDonationUrl(String donationUrl) { this.donationUrl = donationUrl; }
    }
}
