package dasein.sem1pks.controller;

import dasein.sem1pks.config.CurrentUser;
import dasein.sem1pks.dto.request.AccountLoginRequest;
import dasein.sem1pks.dto.request.AccountRegisterRequest;
import dasein.sem1pks.dto.request.PasswordChangeRequest;
import dasein.sem1pks.dto.request.RefreshRequest;
import dasein.sem1pks.dto.response.AccountResponse;
import dasein.sem1pks.dto.response.AuthResponse;
import dasein.sem1pks.exception.forbidden.AccountBlockedException;
import dasein.sem1pks.exception.unauthorized.InvalidCredentialsException;
import dasein.sem1pks.service.AccountService;
import dasein.sem1pks.service.AuditService;
import dasein.sem1pks.service.AuthRateLimiter;
import dasein.sem1pks.service.RefreshSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
public class AccountController {
    private final AccountService accountService;
    private final RefreshSessionService refreshSessions;
    private final AuthRateLimiter rateLimiter;
    private final AuditService audit;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AccountLoginRequest request) {
        rateLimiter.check();
        try {
            return ResponseEntity.ok(accountService.login(request.email(), request.password()));
        } catch (InvalidCredentialsException | AccountBlockedException ex) {
            audit.record(null, "LOGIN_FAILED", "USER", null, "Неудачная попытка входа");
            throw ex;
        }
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody AccountRegisterRequest request) {
        rateLimiter.check();
        AuthResponse authResponse =
                accountService.createAccount(request.username(), request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        rateLimiter.check();
        return accountService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest request) {
        rateLimiter.check();
        refreshSessions.revoke(request.refreshToken());
    }

    @GetMapping("/me")
    public AccountResponse current(@AuthenticationPrincipal CurrentUser user) {
        return accountService.current(user.id());
    }

    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void password(
            @AuthenticationPrincipal CurrentUser user, @Valid @RequestBody PasswordChangeRequest request) {
        rateLimiter.check();
        accountService.changePassword(user.id(), request.currentPassword(), request.newPassword());
    }
}
