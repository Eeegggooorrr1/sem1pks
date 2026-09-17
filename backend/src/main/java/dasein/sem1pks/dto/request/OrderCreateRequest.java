package dasein.sem1pks.dto.request;

import jakarta.validation.constraints.NotNull;

public record OrderCreateRequest(
        @NotNull
        Long listingId
) {}