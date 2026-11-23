package dasein.sem1pks.controller;

import dasein.sem1pks.dto.response.Analytics;
import dasein.sem1pks.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AnalyticsController {
    private final AnalyticsService service;
    @GetMapping("/api/analytics")
    public Analytics get() {
        return service.get();
    }
}
