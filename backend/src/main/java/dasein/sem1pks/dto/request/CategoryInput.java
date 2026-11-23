package dasein.sem1pks.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryInput(
        @NotBlank @Size(max = 20, message = "Не больше {max} символов.")
        @Pattern(
                regexp = "[A-Z][A-Z0-9_]*",
                message = "Начните код с заглавной латинской буквы. " +
                        "Допустимы также цифры и подчеркивание."
        ) String code,
        @NotBlank @Size(max = 100) String name,
        @NotNull Boolean enabled
) {}
