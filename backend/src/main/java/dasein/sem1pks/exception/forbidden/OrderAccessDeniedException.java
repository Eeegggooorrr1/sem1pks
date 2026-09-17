package dasein.sem1pks.exception.forbidden;

public class OrderAccessDeniedException extends ForbiddenException {
    public OrderAccessDeniedException(Long orderId) {
        super("ORDER_ACCESS_DENIED", "Нет прав для изменения заказа с id " + orderId);
    }
}
