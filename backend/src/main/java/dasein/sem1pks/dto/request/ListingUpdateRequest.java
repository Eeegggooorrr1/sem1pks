package dasein.sem1pks.dto.request;

import dasein.sem1pks.domain.ListingCategory;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ListingUpdateRequest(
        @NotBlank(message = "Заголовок обязателен")
        @Size(max = 200, message = "Заголовок не должен превышать 200 символов")
        String title,

        @NotNull(message = "Цена обязательна")
        @Positive(message = "Цена должна быть положительной")
        @Digits(integer = 8, fraction = 2, message = "Цена должна иметь не более 8 цифр до запятой и 2 после")
        BigDecimal price,

        @NotNull(message = "Категория обязательна")
        ListingCategory category
) {
}
