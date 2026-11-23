package dasein.sem1pks.dto.request.account;

import dasein.sem1pks.validation.Utf8Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(
        @NotBlank @Size(max = 72) @Utf8Size(max = 72) String currentPassword,
        @NotBlank @Size(min = 8, max = 72) @Utf8Size(max = 72) String newPassword
) {}
