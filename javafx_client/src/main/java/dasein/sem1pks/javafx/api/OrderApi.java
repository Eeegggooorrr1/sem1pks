package dasein.sem1pks.javafx.api;

import dasein.sem1pks.javafx.model.OrderCreateRequest;
import dasein.sem1pks.javafx.model.OrderResponse;

import java.util.List;
import java.util.Map;

public final class OrderApi {
    private final HttpTransport http;

    OrderApi(HttpTransport http) {
        this.http = http;
    }

    public List<OrderResponse> find(String direction) {
        String query = HttpTransport.queryString(Map.of("direction", direction));
        return http.decodeList(http.request("GET", "/api/orders" + query, null, true, false), OrderResponse.class);
    }

    public OrderResponse create(long listingId) {
        return http.decode(http.request("POST", "/api/orders",
                new OrderCreateRequest(HttpTransport.positive(listingId)), true, false), OrderResponse.class);
    }

    public OrderResponse confirm(long orderId) {
        return changeStatus(orderId, "confirm");
    }

    public OrderResponse complete(long orderId) {
        return changeStatus(orderId, "complete");
    }

    public void cancel(long orderId) {
        http.request("PATCH", "/api/orders/" + HttpTransport.positive(orderId) + "/cancel", null, true, false);
    }

    private OrderResponse changeStatus(long id, String action) {
        return http.decode(http.request("PATCH", "/api/orders/" + HttpTransport.positive(id) + "/" + action,
                null, true, false), OrderResponse.class);
    }
}
