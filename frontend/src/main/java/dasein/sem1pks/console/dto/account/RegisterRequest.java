package dasein.sem1pks.console.dto.account;

public record RegisterRequest(String username, String email, String password) {
    @Override
    public String toString() { return "RegisterRequest[username=" + username + ", email=" + email + ", password=<hidden>]"; }
}
