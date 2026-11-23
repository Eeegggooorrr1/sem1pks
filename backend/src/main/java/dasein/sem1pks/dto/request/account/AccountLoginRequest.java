package dasein.sem1pks.dto.request.account;

import dasein.sem1pks.validation.Utf8Size;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AccountLoginRequest(
        @NotBlank(message = "Email обязателен")
        @Email(message = "Некорректный формат email")
        @jakarta.validation.constraints.Size(max = 254)
        String email,
        @NotBlank(message = "Пароль обязателен")
        @Utf8Size(max = 72)
        String password
) {}
