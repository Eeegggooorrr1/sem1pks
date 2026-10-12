package dasein.sem1pks.console.api;

import dasein.sem1pks.console.dto.listing.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ListingApi {
    private final HttpTransport http;

    ListingApi(HttpTransport http) { this.http = http; }

    public List<ListingResponse> search(String prefix, String category, String sortBy, String direction) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("prefix", prefix);
        query.put("category", category);
        query.put("sortBy", sortBy);
        query.put("direction", direction);
        return http.decodeList(http.request("GET", "/api/listings" + HttpTransport.queryString(query),
                null, true, false), ListingResponse.class);
    }

    public List<ListingResponse> mine() {
        return http.decodeList(http.request("GET", "/api/listings/my", null, true, false), ListingResponse.class);
    }

    public ListingResponse create(ListingCreateRequest request) {
        return http.decode(http.request("POST", "/api/listings", request, true, false), ListingResponse.class);
    }

    public ListingResponse changeStatus(long id, String action) {
        if (!List.of("sold", "active", "closed", "closed/admin").contains(action)) {
            throw new IllegalArgumentException("Неизвестное действие с объявлением");
        }
        return http.decode(http.request("PATCH", "/api/listings/" + HttpTransport.positive(id) + "/" + action,
                null, true, false), ListingResponse.class);
    }
}
