package dasein.sem1pks.dto.request.listing;

import dasein.sem1pks.domain.ListingStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record ListingInput(
        @NotBlank @Size(max = 64, message = "Не больше {max} символов.")
        @Pattern(
                regexp = "[A-Za-z0-9][A-Za-z0-9._-]*",
                message = "Начните код с латинской буквы или цифры. " +
                        "Допустимы также точка, дефис и подчеркивание."
        ) String code,
        @NotBlank @Size(max = 200, message = "Не больше {max} символов.") String title,
        @Size(max = 10000, message = "Не больше {max} символов.") String description,
        @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal price,
        @NotNull ListingStatus status,
        @NotNull UUID categoryId,
        @NotNull Boolean featured,
        @NotNull @Size(max = 20, message = "Укажите не больше {max} тегов.")
        Set<@NotBlank @Size(max = 40, message = "В теге может быть не больше {max} символов.")
            @Pattern(
                    regexp = "[\\p{L}\\p{N}_-]+",
                    message = "В тегах допустимы буквы, цифры, дефис и подчеркивание."
            ) String> tags
) {}
