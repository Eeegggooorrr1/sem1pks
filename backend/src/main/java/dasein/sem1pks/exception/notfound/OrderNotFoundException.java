package dasein.sem1pks.exception.notfound;

public class OrderNotFoundException extends NotFoundException {
    public OrderNotFoundException(Long orderId) {
        super("ORDER_NOT_FOUND_EXCEPTION", "заказ с id " + orderId + " не найден");
    }
}
