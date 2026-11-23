package dasein.sem1pks.service;

import dasein.sem1pks.exception.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class AuthRateLimiter {
    private long windowStart = System.nanoTime();
    private int requests;

    public synchronized void check() {
        long now = System.nanoTime();
        if (now - windowStart >= 60_000_000_000L) {
            windowStart = now;
            requests = 0;
        }
        if (++requests > 60) {
            throw new DomainException("AUTH_RATE_LIMIT", "Слишком много запросов авторизации. Повторите через минуту",
                    HttpStatus.TOO_MANY_REQUESTS) {};
        }
    }
}
