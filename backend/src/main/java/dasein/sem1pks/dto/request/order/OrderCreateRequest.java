package dasein.sem1pks.dto.request.order;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OrderCreateRequest(
        @NotNull
        UUID listingId
) {}