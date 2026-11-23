package dasein.sem1pks.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.slf4j.event.Level;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Correlation-ID";
    private static final String MDC_KEY = "correlationId";
    private static final Pattern VALID_ID = Pattern.compile("[a-zA-Z0-9_-]{1,64}");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long started = System.nanoTime();
        String correlationId = resolveId(request);
        MDC.put(MDC_KEY, correlationId);

        try {
            response.setHeader(HEADER, correlationId);
            filterChain.doFilter(request, response);
        } finally {
            try {
                logRequest(request, response, started, correlationId);
            } finally {
                MDC.remove(MDC_KEY);
            }
        }
    }

    private String resolveId(HttpServletRequest request) {
        String id = request.getHeader(HEADER);
        return id != null && VALID_ID.matcher(id).matches() ? id : UUID.randomUUID().toString();
    }

    private void logRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            long started,
            String correlationId
    ) {
        long elapsedMs = (System.nanoTime() - started) / 1_000_000;
        int status = response.getStatus();

        Level level = Level.INFO;
        if (status >= 500) {
            level = Level.ERROR;
        } else if (status >= 400) {
            level = Level.WARN;
        }

        log.atLevel(level).log("{} {} -> {} ({} ms) [{}]",
                request.getMethod(), request.getRequestURI(), status, elapsedMs, correlationId);
    }
}
