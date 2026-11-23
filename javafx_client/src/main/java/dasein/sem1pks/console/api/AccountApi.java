package dasein.sem1pks.console.api;

import dasein.sem1pks.console.dto.account.*;
import dasein.sem1pks.console.session.TokenStore;

public final class AccountApi {
    private final HttpTransport http;
    private final TokenStore tokens;

    AccountApi(HttpTransport http, TokenStore tokens) {
        this.http = http;
        this.tokens = tokens;
    }

    public AccountResponse login(LoginRequest request) {
        return authenticate("/api/users/login", request);
    }

    public AccountResponse register(RegisterRequest request) {
        return authenticate("/api/users/register", request);
    }

    public void logout() {
        tokens.clear();
    }

    private AccountResponse authenticate(String path, Object request) {
        AuthResponse auth = http.decode(http.request("POST", path, request, false, false), AuthResponse.class);
        tokens.set(auth.accessToken());
        return auth.account();
    }
}
