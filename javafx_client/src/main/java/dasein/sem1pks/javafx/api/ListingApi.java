package dasein.sem1pks.javafx.api;

import dasein.sem1pks.javafx.model.ListingCreateRequest;
import dasein.sem1pks.javafx.model.ListingResponse;
import dasein.sem1pks.javafx.model.ListingUpdateRequest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ListingApi {
    private final HttpTransport http;

    ListingApi(HttpTransport http) {
        this.http = http;
    }

    public List<ListingResponse> search(String prefix, String category, String sortBy, String direction) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("prefix", prefix);
        query.put("category", category);
        query.put("sortBy", sortBy);
        query.put("direction", direction);
        return http.decodeList(http.request("GET", "/api/listings" + HttpTransport.queryString(query),
                null, false, false), ListingResponse.class);
    }

    public List<ListingResponse> mine() {
        return http.decodeList(http.request("GET", "/api/listings/my", null, true, false), ListingResponse.class);
    }

    public ListingResponse create(ListingCreateRequest request) {
        return http.decode(http.request("POST", "/api/listings", request, true, false), ListingResponse.class);
    }

    public ListingResponse update(long id, ListingUpdateRequest request) {
        return http.decode(http.request("PUT", "/api/listings/" + HttpTransport.positive(id),
                request, true, false), ListingResponse.class);
    }

    public ListingResponse markSold(long id) {
        return changeStatus(id, "sold");
    }

    public ListingResponse markActive(long id) {
        return changeStatus(id, "active");
    }

    public void close(long id) {
        http.request("PATCH", "/api/listings/" + HttpTransport.positive(id) + "/closed", null, true, false);
    }

    public void closeAsAdmin(long id) {
        http.request("PATCH", "/api/listings/" + HttpTransport.positive(id) + "/closed/admin", null, true, false);
    }

    private ListingResponse changeStatus(long id, String action) {
        return http.decode(http.request("PATCH", "/api/listings/" + HttpTransport.positive(id) + "/" + action,
                null, true, false), ListingResponse.class);
    }
}
