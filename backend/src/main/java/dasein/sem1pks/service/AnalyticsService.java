package dasein.sem1pks.service;

import dasein.sem1pks.dto.response.analytics.Analytics;
import dasein.sem1pks.repository.AnalyticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private final AnalyticsRepository repository;

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Analytics get() {
        return repository.get();
    }
}
