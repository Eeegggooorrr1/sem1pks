package dasein.sem1pks.dto.request.admin;

import dasein.sem1pks.validation.Utf8Size;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record AdminUserCreateRequest(
        @NotNull @Valid AdminUserRequest user,
        @NotBlank @Size(min = 8, max = 72) @Utf8Size(max = 72) String password
) {}
