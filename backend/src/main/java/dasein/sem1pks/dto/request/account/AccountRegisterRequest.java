package dasein.sem1pks.dto.request.account;

import dasein.sem1pks.validation.Utf8Size;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record AccountRegisterRequest(
        @NotBlank(message = "имя обязательно")
        @Size(min = 3, max = 20, message = "Имя должно быть от 3 до 20 символов")
        String username,
        @NotBlank(message = "Email обязателен")
        @Size(max = 254)
        @Email(message = "Некорректный формат email")
        String email,
        @NotBlank(message = "Пароль обязателен")
        @Size(min = 8, max = 72, message = "Пароль должен быть от 8 до 72 символов")
        @Utf8Size(max = 72)
        String password
) {
    public AccountRegisterRequest {
        username = username == null ? null : username.trim();
    }
}
