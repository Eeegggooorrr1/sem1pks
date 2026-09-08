package dasein.sem1pks.controller;

import dasein.sem1pks.dto.request.AccountLoginRequest;
import dasein.sem1pks.dto.request.AccountRegisterRequest;
import dasein.sem1pks.dto.response.AuthResponse;
import dasein.sem1pks.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
public class AccountController {

    private final AccountService accountService;


    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AccountLoginRequest request) {
        AuthResponse authResponse = accountService.login(
                request.email(),
                request.password()
        );
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody AccountRegisterRequest request) {
        AuthResponse authResponse = accountService.createAccount(
                request.username(),
                request.email(),
                request.password()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
    }
}

