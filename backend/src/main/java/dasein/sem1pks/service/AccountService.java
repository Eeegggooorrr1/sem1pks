package dasein.sem1pks.service;

import dasein.sem1pks.domain.User;
import dasein.sem1pks.dto.response.account.AccountResponse;
import dasein.sem1pks.dto.response.account.AuthResponse;
import dasein.sem1pks.exception.conflict.AccountAlreadyExistsException;
import dasein.sem1pks.exception.forbidden.AccountBlockedException;
import dasein.sem1pks.exception.unauthorized.InvalidCredentialsException;
import dasein.sem1pks.repository.UserRepository;
import dasein.sem1pks.util.EmailNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final RefreshSessionService refreshSessions;
    private final AuditService audit;

    @Transactional
    public AuthResponse login(String email, String password) {
        String normalizedEmail = EmailNormalizer.normalize(email);
        refreshSessions.lockAccount(normalizedEmail);
        try {
            authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(normalizedEmail, password));
        } catch (DisabledException e) {
            if (userRepository.findByEmail(normalizedEmail)
                            .filter(user -> passwordEncoder.matches(password, user.getPasswordHash()))
                            .isPresent()) {
                throw new AccountBlockedException();
            }
            throw new InvalidCredentialsException();
        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException();
        }
        User user = userRepository.findByEmail(normalizedEmail)
                            .orElseThrow(() -> new InvalidCredentialsException());

        String token = jwtService.generateToken(
                user.getId(), user.getEmail(), user.getRole().name(), user.isBlocked());

        audit.record(user.getId(), "LOGIN", "USER", user.getId(), "Вход в систему");
        return new AuthResponse(token, toDto(user), refreshSessions.issue(user.getId()));
    }

    @Transactional
    public AuthResponse createAccount(String username, String email, String password) {
        String normalizedEmail = EmailNormalizer.normalize(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new AccountAlreadyExistsException(normalizedEmail);
        }
        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setUsername(username.trim());

        User savedUser;
        try {
            savedUser = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            if (userRepository.existsByEmail(normalizedEmail)) {
                throw new AccountAlreadyExistsException(normalizedEmail);
            }
            throw e;
        }

        String token = jwtService.generateToken(
                savedUser.getId(), savedUser.getEmail(), savedUser.getRole().name(), savedUser.isBlocked());

        audit.record(savedUser.getId(), "REGISTER", "USER", savedUser.getId(), "Регистрация");
        return new AuthResponse(token, toDto(savedUser), refreshSessions.issue(savedUser.getId()));
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        UUID id = refreshSessions.consume(refreshToken);
        User user = userRepository.findById(id)
                            .filter(value -> !value.isBlocked())
                            .orElseThrow(InvalidCredentialsException::new);
        return new AuthResponse(
                jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name(), false),
                toDto(user), refreshSessions.issue(id));
    }

    public AccountResponse current(UUID userId) {
        return toDto(userRepository.findById(userId).orElseThrow(InvalidCredentialsException::new));
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        refreshSessions.lockAccount(userId);
        User user = userRepository.findById(userId).orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        refreshSessions.revokeUser(userId);
        audit.record(userId, "PASSWORD_CHANGE", "USER", userId, "Пароль изменен");
    }

    private AccountResponse toDto(User user) {
        return new AccountResponse(user.getId(), user.getEmail(), user.getUsername(), user.getRole());
    }
}
