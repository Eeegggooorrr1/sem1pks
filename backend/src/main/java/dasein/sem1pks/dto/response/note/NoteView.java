package dasein.sem1pks.dto.response.note;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NoteView(
        UUID id,
        UUID authorId,
        String authorName,
        String body,
        boolean pinned,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
