package dasein.sem1pks.console.api;

import dasein.sem1pks.console.dto.order.*;
import java.util.List;
import java.util.Map;

public final class OrderApi {
    private final HttpTransport http;

    OrderApi(HttpTransport http) { this.http = http; }

    public List<OrderResponse> find(String direction) {
        return http.decodeList(http.request("GET", "/api/orders" + HttpTransport.queryString(Map.of("direction", direction)),
                null, true, false), OrderResponse.class);
    }

    public OrderResponse create(long listingId) {
        return http.decode(http.request("POST", "/api/orders", new OrderCreateRequest(HttpTransport.positive(listingId)),
                true, false), OrderResponse.class);
    }

    public OrderResponse changeStatus(long id, String action) {
        if (!List.of("confirm", "complete", "cancel").contains(action)) {
            throw new IllegalArgumentException("Неизвестное действие с заказом");
        }
        return http.decode(http.request("PATCH", "/api/orders/" + HttpTransport.positive(id) + "/" + action,
                null, true, false), OrderResponse.class);
    }
}
