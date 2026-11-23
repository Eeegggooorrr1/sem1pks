package dasein.sem1pks.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

public record BulkItem(
        @NotNull UUID id,
        @NotNull @PositiveOrZero Long version
) {}
