package dasein.sem1pks.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record Version(
        @NotNull @PositiveOrZero Long version
) {}
