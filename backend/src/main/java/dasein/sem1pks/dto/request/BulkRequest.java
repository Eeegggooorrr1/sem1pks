package dasein.sem1pks.dto.request;

import dasein.sem1pks.domain.ListingStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BulkRequest(
        @NotNull @Size(min=1,max=100) List<@NotNull @Valid BulkItem> items,
        @NotNull BulkAction action,
        ListingStatus status,
        @Size(max=40) @Pattern(regexp="[\\p{L}\\p{N}_-]+") String tag
) {}
