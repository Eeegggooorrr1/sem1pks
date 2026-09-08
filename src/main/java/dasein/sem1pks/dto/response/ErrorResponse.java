package dasein.sem1pks.dto.response;

import java.util.List;
import java.util.Map;

public record ErrorResponse(
        String errorCode,
        String message,
        int status,
        Map<String, List<String>> details
) {}
