package dasein.sem1pks.dto.response;

import dasein.sem1pks.domain.UserRole;

import java.util.UUID;

public record UserView(
        UUID id,
        String username,
        String email,
        UserRole role,
        boolean active
) {}
