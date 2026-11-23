package dasein.sem1pks.config;

import org.springframework.security.core.AuthenticatedPrincipal;

import java.util.UUID;

public record CurrentUser(
        UUID id,
        String email
) implements AuthenticatedPrincipal {

    @Override
    public String getName() {
        return email;
    }
}
