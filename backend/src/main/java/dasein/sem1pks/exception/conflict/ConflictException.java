package dasein.sem1pks.exception.conflict;

import dasein.sem1pks.exception.DomainException;
import org.springframework.http.HttpStatus;

public abstract class ConflictException extends DomainException {
    protected ConflictException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.CONFLICT);
    }
}