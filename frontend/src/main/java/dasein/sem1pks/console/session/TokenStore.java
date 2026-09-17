package dasein.sem1pks.console.session;

public final class TokenStore {
    private String token;

    public String get() {
        return token;
    }

    public void set(String token) {
        this.token = token;
    }

    public void clear() {
        token = null;
    }
}
