package dasein.sem1pks.exception.unauthorized;

import dasein.sem1pks.exception.DomainException;
import org.springframework.http.HttpStatus;

public abstract class UnauthorizedException extends DomainException {
    protected UnauthorizedException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.UNAUTHORIZED);
    }
}