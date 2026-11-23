package dasein.sem1pks.dto.response;

import java.util.List;
import java.util.UUID;

public record BulkResult(
        List<UUID> successful,
        List<BulkError> errors
) {}
