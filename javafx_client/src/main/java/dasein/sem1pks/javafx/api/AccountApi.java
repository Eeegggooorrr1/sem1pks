package dasein.sem1pks.javafx.api;

import dasein.sem1pks.javafx.model.*;
import dasein.sem1pks.javafx.session.Session;

public final class AccountApi {
    private final HttpTransport http;
    private final Session session;

    AccountApi(HttpTransport http, Session session) {
        this.http = http;
        this.session = session;
    }

    public AccountResponse login(String email, String password) {
        return authenticate("/api/users/login", new LoginRequest(email, password));
    }

    public AccountResponse register(String username, String email, String password) {
        return authenticate("/api/users/register", new RegisterRequest(username, email, password));
    }

    public void logout() {
        session.clear();
    }

    private AccountResponse authenticate(String path, Object request) {
        AuthResponse response = http.decode(http.request("POST", path, request, false, false), AuthResponse.class);
        session.open(response.accessToken(), response.account());
        return response.account();
    }
}
