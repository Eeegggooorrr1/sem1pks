package dasein.sem1pks.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long started = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsedMs = (System.nanoTime() - started) / 1_000_000;
            int status = response.getStatus();
            String message = "{} {} -> {} ({} ms)";
            if (status >= 500) {
                log.error(message, request.getMethod(), request.getRequestURI(), status, elapsedMs);
            } else if (status >= 400) {
                log.warn(message, request.getMethod(), request.getRequestURI(), status, elapsedMs);
            } else {
                log.info(message, request.getMethod(), request.getRequestURI(), status, elapsedMs);
            }
        }
    }
}
