package dasein.sem1pks.controller;

import dasein.sem1pks.config.CurrentUser;
import dasein.sem1pks.dto.request.admin.AdminUserCreateRequest;
import dasein.sem1pks.dto.request.admin.AdminUserRequest;
import dasein.sem1pks.dto.response.admin.UserView;
import dasein.sem1pks.dto.response.common.Page;
import dasein.sem1pks.service.ExportService;
import dasein.sem1pks.service.UserAdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@Validated
@RequiredArgsConstructor
public class AdminController {
    private final ExportService exportService;
    private final UserAdminService users;

    @GetMapping("/users")
    public Page<UserView> listUsers(
            @RequestParam(defaultValue = "0") @Min(0) @Max(1000000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return users.list(page, size);
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserView createUser(
            @AuthenticationPrincipal CurrentUser actor,
            @Valid @RequestBody AdminUserCreateRequest request
    ) {
        return users.create(actor.id(), request.user(), request.password());
    }

    @PutMapping("/users/{id}")
    public UserView updateUser(
            @AuthenticationPrincipal CurrentUser actor,
            @PathVariable UUID id,
            @Valid @RequestBody AdminUserRequest request
    ) {
        return users.update(actor.id(), id, request);
    }

    @GetMapping(value = "/export", produces = "application/zip")
    public ResponseEntity<byte[]> export() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"domain-export.zip\"")
                .cacheControl(CacheControl.noStore())
                .body(exportService.export());
    }
}
