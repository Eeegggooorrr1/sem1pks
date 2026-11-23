package dasein.sem1pks.controller;

import dasein.sem1pks.dto.request.AuditFilter.Direction;
import dasein.sem1pks.dto.request.AuditFilter.SortBy;
import dasein.sem1pks.dto.request.AuditFilter;
import dasein.sem1pks.dto.response.AuditEvent;
import dasein.sem1pks.dto.response.Page;
import dasein.sem1pks.service.AuditService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit")
@Validated
@RequiredArgsConstructor
public class AuditController {

    private final AuditService audit;

    @GetMapping
    public Page<AuditEvent> list(
            @RequestParam(defaultValue = "") @Size(max = 200) String search,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1000000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "DESC") Direction direction,
            @RequestParam(defaultValue = "CREATED_AT") SortBy sortBy
    ) {
        return audit.search(new AuditFilter(search, page, size, direction, sortBy));
    }
}
