package dasein.sem1pks.service;

import dasein.sem1pks.domain.User;
import dasein.sem1pks.dto.response.AccountResponse;
import dasein.sem1pks.dto.response.AuthResponse;
import dasein.sem1pks.exception.conflict.AccountAlreadyExistsException;
import dasein.sem1pks.exception.notfound.AccountNotFoundException;
import dasein.sem1pks.exception.unauthorized.InvalidCredentialsException;
import dasein.sem1pks.repository.UserRepository;
import dasein.sem1pks.util.EmailNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;


    public AuthResponse login(String email, String password) {
        String normalizedEmail = EmailNormalizer.normalize(email);
        try {
            authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(normalizedEmail, password)
            );
        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException();
        }
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new InvalidCredentialsException());

        String token = jwtService.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.isBlocked()
        );

        return new AuthResponse(token, toDto(user));
    }

    public AuthResponse createAccount(String username, String email, String password) {
        String normalizedEmail = EmailNormalizer.normalize(email);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new AccountAlreadyExistsException(normalizedEmail);
        }
        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setUsername(username.trim());

        User savedUser = userRepository.save(user);

        String token = jwtService.generateToken(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole().name(),
                savedUser.isBlocked()
        );

        return new AuthResponse(token, toDto(savedUser));

    }

    private AccountResponse toDto(User user) {
        return new AccountResponse(user.getId(), user.getEmail(), user.getUsername(), user.getRole());
    }

}
