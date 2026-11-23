package dasein.sem1pks.dto.response;

import dasein.sem1pks.domain.UserRole;

import java.util.UUID;

public record AccountResponse(
        UUID id,
        String email,
        String username,
        UserRole role
) {}
