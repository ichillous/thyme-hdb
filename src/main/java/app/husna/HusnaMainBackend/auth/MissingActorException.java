package app.husna.HusnaMainBackend.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class MissingActorException extends RuntimeException {
    public MissingActorException(String message) {
        super(message);
    }
}
