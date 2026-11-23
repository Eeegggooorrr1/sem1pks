package dasein.sem1pks.dto.response.csv;

import java.util.List;

public record ImportPreview(
        String confirmation,
        int validRows,
        int invalidRows,
        List<ImportError> errors
) {}
