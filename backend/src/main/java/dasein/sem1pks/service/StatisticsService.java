package dasein.sem1pks.service;

import dasein.sem1pks.dto.response.StatisticsResponse;
import dasein.sem1pks.repository.StatisticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final StatisticsRepository statisticsRepository;

    @Transactional(readOnly = true)
    public StatisticsResponse getStatistics() {
        StatisticsRepository.StatisticsProjection statistics = statisticsRepository.getStatistics();

        return new StatisticsResponse(
                statistics.getUsersCount(),
                statistics.getOrdersCount(),
                statistics.getListingsCount(),
                money(statisticsRepository.getAverageListingPrice()),
                money(statisticsRepository.getMaxListingPrice())
        );
    }

    private BigDecimal money(BigDecimal value) {
        return value == null
                ? BigDecimal.ZERO.setScale(2)
                : value.setScale(2, RoundingMode.HALF_UP);
    }
}
