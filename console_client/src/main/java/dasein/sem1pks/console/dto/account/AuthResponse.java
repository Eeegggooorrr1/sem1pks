package dasein.sem1pks.console.dto.account;

public record AuthResponse(String accessToken, AccountResponse account) {
    @Override
    public String toString() { return "AuthResponse[accessToken=<hidden>, account=" + account + "]"; }
}
