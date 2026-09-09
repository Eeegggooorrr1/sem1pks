package dasein.sem1pks.exception.forbidden;

import dasein.sem1pks.exception.DomainException;
import org.springframework.http.HttpStatus;

public abstract class ForbiddenException extends DomainException {

    protected ForbiddenException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.FORBIDDEN);
    }
}
