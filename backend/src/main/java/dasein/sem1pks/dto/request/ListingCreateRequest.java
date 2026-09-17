package dasein.sem1pks.dto.request;

import dasein.sem1pks.domain.ListingCategory;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ListingCreateRequest(

        @NotBlank(message = "Заголовок обязателен")
        @Size(max = 200, message = "Заголовок не должен превышать 200 символов")
        String title,

        @Size(max = 5000, message = "Описание не должно превышать 5000 символов")
        String description,

        @NotNull(message = "Цена обязательна")
        @Positive(message = "Цена должна быть положительной")
        @Digits(integer = 8, fraction = 2, message = "Цена должна иметь не более 8 цифр до запятой и 2 после")
        BigDecimal price,

        ListingCategory category
) {}