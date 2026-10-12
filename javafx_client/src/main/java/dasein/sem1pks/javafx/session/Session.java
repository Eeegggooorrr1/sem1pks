package dasein.sem1pks.javafx.session;

import dasein.sem1pks.javafx.model.AccountResponse;

public final class Session {
    private String token;
    private AccountResponse account;

    public String token() {
        return token;
    }

    public AccountResponse account() {
        return account;
    }

    public boolean isAuthenticated() {
        return token != null && account != null;
    }

    public void open(String token, AccountResponse account) {
        this.token = token;
        this.account = account;
    }

    public void clear() {
        token = null;
        account = null;
    }
}
