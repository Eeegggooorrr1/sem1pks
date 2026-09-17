package dasein.sem1pks.console.api;

import dasein.sem1pks.console.dto.statistics.StatisticsResponse;

public final class StatisticsApi {
    private final HttpTransport http;

    StatisticsApi(HttpTransport http) { this.http = http; }

    public StatisticsResponse get() {
        return http.decode(http.request("GET", "/api/statistics", null, true, false), StatisticsResponse.class);
    }
}
