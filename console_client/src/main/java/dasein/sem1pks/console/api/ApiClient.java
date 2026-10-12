package dasein.sem1pks.console.api;

import dasein.sem1pks.console.session.TokenStore;

public final class ApiClient {
    private final HttpTransport transport;
    private final AccountApi accounts;
    private final ListingApi listings;
    private final OrderApi orders;
    private final StatisticsApi statistics;
    private final AdminApi admin;

    public ApiClient(String baseUrl, TokenStore tokens) {
        transport = new HttpTransport(baseUrl, tokens);
        accounts = new AccountApi(transport, tokens);
        listings = new ListingApi(transport);
        orders = new OrderApi(transport);
        statistics = new StatisticsApi(transport);
        admin = new AdminApi(transport);
    }

    public String baseUrl() { return transport.baseUrl(); }
    public AccountApi accounts() { return accounts; }
    public ListingApi listings() { return listings; }
    public OrderApi orders() { return orders; }
    public StatisticsApi statistics() { return statistics; }
    public AdminApi admin() { return admin; }
}
