package dasein.sem1pks.config;

import dasein.sem1pks.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final RestAuthenticationEntryPoint errorResponseWriter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length());
        if (token.isBlank()) {
            errorResponseWriter.writeError(response, "INVALID_TOKEN", "Некорректный токен", HttpStatus.UNAUTHORIZED);
            return;
        }

        try {
            JwtService.TokenData tokenData = jwtService.parseToken(token);
            if (tokenData.isBlocked()) {
                errorResponseWriter.writeError(response, "ACCOUNT_BLOCKED", "Аккаунт заблокирован", HttpStatus.FORBIDDEN);
                return;
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                var authority = new SimpleGrantedAuthority("ROLE_" + tokenData.role());
                var currentUser = new CurrentUser(tokenData.userId(), tokenData.email());
                var authentication = UsernamePasswordAuthenticationToken.authenticated(
                        currentUser, null, List.of(authority)
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (JwtException | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
            errorResponseWriter.writeError(response, "INVALID_TOKEN", "Некорректный токен", HttpStatus.UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
