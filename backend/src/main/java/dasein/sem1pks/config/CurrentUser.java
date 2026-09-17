package dasein.sem1pks.config;

import org.springframework.security.core.AuthenticatedPrincipal;

public record CurrentUser(Long id, String email) implements AuthenticatedPrincipal {

    @Override
    public String getName() {
        return email;
    }
}
