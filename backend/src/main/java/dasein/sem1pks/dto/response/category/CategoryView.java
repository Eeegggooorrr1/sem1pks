package dasein.sem1pks.dto.response.category;

import java.util.UUID;

public record CategoryView(
        UUID id,
        String code,
        String name,
        boolean enabled
) {}
