package dasein.sem1pks.service;

import dasein.sem1pks.dto.response.StatisticsResponse;
import dasein.sem1pks.repository.StatisticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                statistics.getListingsCount()
        );
    }
}