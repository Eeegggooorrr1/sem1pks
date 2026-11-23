package dasein.sem1pks.exception.forbidden;

import java.util.UUID;

public class OrderAccessDeniedException extends ForbiddenException {
    public OrderAccessDeniedException(UUID orderId) {
        super("ORDER_ACCESS_DENIED", "Нет прав для изменения заказа с id " + orderId);
    }
}
