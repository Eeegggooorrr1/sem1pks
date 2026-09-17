package dasein.sem1pks.exception.conflict;

public class OrderCreateConflictException extends ConflictException {
    public OrderCreateConflictException(String message) {
        super("ORDER_CREATE_CONFLICT_EXCEPTION", message);
    }
}
