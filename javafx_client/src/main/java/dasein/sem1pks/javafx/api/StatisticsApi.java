package dasein.sem1pks.javafx.api;

import dasein.sem1pks.javafx.model.StatisticsResponse;

public final class StatisticsApi {
    private final HttpTransport http;

    StatisticsApi(HttpTransport http) {
        this.http = http;
    }

    public StatisticsResponse get() {
        return http.decode(http.request("GET", "/api/statistics", null, true, false), StatisticsResponse.class);
    }
}
