package dasein.sem1pks.exception.badrequest;

import dasein.sem1pks.exception.DomainException;
import org.springframework.http.HttpStatus;

public abstract class BadRequestException extends DomainException {
    protected BadRequestException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.BAD_REQUEST);
    }
}