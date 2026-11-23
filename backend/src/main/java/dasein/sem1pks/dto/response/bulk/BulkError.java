package dasein.sem1pks.dto.response.bulk;

import java.util.UUID;

public record BulkError(
        UUID id,
        String code,
        String message
) {
    public String getUserMessage() { return message; }
}
