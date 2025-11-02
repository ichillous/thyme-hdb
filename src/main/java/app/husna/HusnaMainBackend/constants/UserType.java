package app.husna.HusnaMainBackend.constants;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserType {
    USER_PUBLIC("Public User"),
    AUTH_USER("Authorized User"),
    ADMIN("Husna"),
    ORG_USER("Non Profit User");

    private final String displayName;
}
