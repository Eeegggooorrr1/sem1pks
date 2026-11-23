package dasein.sem1pks.dto.request.note;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NoteInput(
        @NotBlank @Size(max=2000) String body,
        @NotNull Boolean pinned
) {}
