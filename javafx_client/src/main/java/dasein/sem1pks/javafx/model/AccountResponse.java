package dasein.sem1pks.javafx.model;

public record AccountResponse(long id, String email, String username, String role) {
    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
