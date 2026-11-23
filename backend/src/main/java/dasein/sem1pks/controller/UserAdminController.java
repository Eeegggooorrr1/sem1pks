package dasein.sem1pks.controller;

import dasein.sem1pks.config.CurrentUser;
import dasein.sem1pks.dto.request.AdminUserCreateRequest;
import dasein.sem1pks.dto.request.AdminUserRequest;
import dasein.sem1pks.dto.response.Page;
import dasein.sem1pks.dto.response.UserView;
import dasein.sem1pks.service.UserAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@Validated
@RequiredArgsConstructor
public class UserAdminController {
    private final UserAdminService users;

    @GetMapping
    public Page<UserView> list(@RequestParam(defaultValue = "0") @Min(0) @Max(1000000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return users.list(page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserView create(
            @AuthenticationPrincipal CurrentUser actor, @Valid @RequestBody AdminUserCreateRequest request) {
        return users.create(actor.id(), request.user(), request.password());
    }

    @PutMapping("/{id}")
    public UserView update(@AuthenticationPrincipal CurrentUser actor, @PathVariable UUID id,
            @Valid @RequestBody AdminUserRequest request) {
        return users.update(actor.id(), id, request);
    }
}
