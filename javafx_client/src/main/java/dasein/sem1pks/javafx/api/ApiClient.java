package dasein.sem1pks.javafx.api;

import dasein.sem1pks.javafx.session.Session;

public final class ApiClient {
    private final AccountApi accounts;
    private final ListingApi listings;
    private final OrderApi orders;
    private final StatisticsApi statistics;
    private final AdminApi admin;

    public ApiClient(String baseUrl, Session session) {
        HttpTransport transport = new HttpTransport(baseUrl, session);
        accounts = new AccountApi(transport, session);
        listings = new ListingApi(transport);
        orders = new OrderApi(transport);
        statistics = new StatisticsApi(transport);
        admin = new AdminApi(transport);
    }

    public AccountApi accounts() { return accounts; }
    public ListingApi listings() { return listings; }
    public OrderApi orders() { return orders; }
    public StatisticsApi statistics() { return statistics; }
    public AdminApi admin() { return admin; }
}
