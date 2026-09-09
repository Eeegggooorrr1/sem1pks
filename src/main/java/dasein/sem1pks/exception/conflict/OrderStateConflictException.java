package dasein.sem1pks.exception.conflict;

public class OrderStateConflictException extends ConflictException {
    public OrderStateConflictException() {
        super("ORDER_STATE_CONFLICT", "невозможно изменить статус заказа");
    }
}
