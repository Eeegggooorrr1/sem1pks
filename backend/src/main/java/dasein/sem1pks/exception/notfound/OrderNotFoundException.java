package dasein.sem1pks.exception.notfound;

import java.util.UUID;

public class OrderNotFoundException extends NotFoundException {
    public OrderNotFoundException(UUID orderId) {
        super("ORDER_NOT_FOUND_EXCEPTION", "заказ с id " + orderId + " не найден");
    }
}
