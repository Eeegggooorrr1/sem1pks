package dasein.sem1pks.dto.response;

import dasein.sem1pks.domain.UserRole;

public record AccountResponse(Long id, String email, String username, UserRole role) {}
