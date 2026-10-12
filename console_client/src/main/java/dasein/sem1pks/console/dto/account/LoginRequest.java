package dasein.sem1pks.console.dto.account;

public record LoginRequest(String email, String password) {
    @Override
    public String toString() { return "LoginRequest[email=" + email + ", password=<hidden>]"; }
}
