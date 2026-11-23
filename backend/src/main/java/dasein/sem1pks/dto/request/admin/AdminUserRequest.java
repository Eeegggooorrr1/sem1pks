package dasein.sem1pks.dto.request.admin;

import dasein.sem1pks.domain.UserRole;
import jakarta.validation.constraints.*;

public record AdminUserRequest(
        @NotBlank @Size(min = 3, max = 20) String username,
        @NotBlank @Email @Size(max = 254) String email,
        @NotNull UserRole role,
        @NotNull Boolean active
) {}
