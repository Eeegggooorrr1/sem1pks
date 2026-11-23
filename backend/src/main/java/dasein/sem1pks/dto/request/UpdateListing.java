package dasein.sem1pks.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateListing(
        @NotNull @PositiveOrZero Long version,
        @NotNull @Valid ListingInput data
) {}
