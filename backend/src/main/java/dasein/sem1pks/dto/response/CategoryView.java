package dasein.sem1pks.dto.response;

import java.util.UUID;

public record CategoryView(
        UUID id,
        String code,
        String name,
        boolean enabled
) {}
