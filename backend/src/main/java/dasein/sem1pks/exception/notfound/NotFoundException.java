package dasein.sem1pks.exception.notfound;

import dasein.sem1pks.exception.DomainException;
import org.springframework.http.HttpStatus;

public abstract class NotFoundException extends DomainException {
    protected NotFoundException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.NOT_FOUND);
    }
}