package dasein.sem1pks;

import dasein.sem1pks.service.AuthRateLimiter;
import dasein.sem1pks.exception.DomainException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class AuthRateLimiterTest {
    @Test void limitsAuthRequests() {
        var limiter = new AuthRateLimiter();
        for (int i = 0; i < 60; i++) limiter.check();
        assertThatThrownBy(limiter::check).isInstanceOfSatisfying(DomainException.class,
                error -> assertThat(error.getStatus().value()).isEqualTo(429));
    }
}
