package dasein.sem1pks.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AccountLoginRequest(

    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный формат email")
    String email,

    @NotBlank(message = "Пароль обязателен")
    String password
) {}
