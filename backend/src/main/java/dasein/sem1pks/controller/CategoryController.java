package dasein.sem1pks.controller;

import dasein.sem1pks.config.CurrentUser;
import dasein.sem1pks.dto.request.CategoryInput;
import dasein.sem1pks.dto.response.CategoryView;
import dasein.sem1pks.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService service;

    @GetMapping
    public List<CategoryView> list() {
        return service.list();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryView create(
            @AuthenticationPrincipal CurrentUser user, @Valid @RequestBody CategoryInput data) {
        return service.create(user.id(), data);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryView update(@AuthenticationPrincipal CurrentUser user, @PathVariable UUID id,
            @Valid @RequestBody CategoryInput data) {
        return service.update(user.id(), id, data);
    }
}
