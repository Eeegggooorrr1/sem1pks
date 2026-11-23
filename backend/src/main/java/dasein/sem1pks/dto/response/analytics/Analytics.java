package dasein.sem1pks.dto.response.analytics;

import java.math.BigDecimal;
import java.util.List;

public record Analytics(
        long total,
        long active,
        long archived,
        long featured,
        BigDecimal averagePrice,
        List<Group> byCategory,
        List<Group> byStatus,
        List<Group> byPriority
) {}
